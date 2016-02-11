/*
 * Copyright (c) 2012-2015 iWave Software LLC
 * All Rights Reserved
 */
package com.emc.sa.asset.providers;

import static com.emc.sa.asset.providers.BlockProviderUtils.isLocalMirrorSupported;
import static com.emc.sa.asset.providers.BlockProviderUtils.isLocalSnapshotSupported;
import static com.emc.sa.asset.providers.BlockProviderUtils.isRPSourceVolume;
import static com.emc.sa.asset.providers.BlockProviderUtils.isRPTargetVolume;
import static com.emc.sa.asset.providers.BlockProviderUtils.isRemoteSnapshotSupported;
import static com.emc.sa.asset.providers.BlockProviderUtils.isSnapshotSessionSupportedForCG;
import static com.emc.sa.asset.providers.BlockProviderUtils.isSnapshotSessionSupportedForVolume;
import static com.emc.sa.asset.providers.BlockProviderUtils.isVpoolProtectedByVarray;
import static com.emc.vipr.client.core.util.ResourceUtils.name;
import static com.emc.vipr.client.core.util.ResourceUtils.stringId;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.stereotype.Component;

import com.emc.sa.asset.AssetOptionsContext;
import com.emc.sa.asset.AssetOptionsUtils;
import com.emc.sa.asset.BaseAssetOptionsProvider;
import com.emc.sa.asset.annotation.Asset;
import com.emc.sa.asset.annotation.AssetDependencies;
import com.emc.sa.asset.annotation.AssetNamespace;
import com.emc.sa.machinetags.KnownMachineTags;
import com.emc.sa.machinetags.MachineTagUtils;
import com.emc.sa.service.vipr.block.BlockStorageUtils;
import com.emc.sa.util.ResourceType;
import com.emc.sa.util.StringComparator;
import com.emc.storageos.db.client.model.BlockConsistencyGroup;
import com.emc.storageos.db.client.model.BlockConsistencyGroup.Types;
import com.emc.storageos.db.client.model.Volume.ReplicationState;
import com.emc.storageos.model.BulkIdParam;
import com.emc.storageos.model.NamedRelatedResourceRep;
import com.emc.storageos.model.RelatedResourceRep;
import com.emc.storageos.model.StringHashMapEntry;
import com.emc.storageos.model.VirtualArrayRelatedResourceRep;
import com.emc.storageos.model.application.VolumeGroupList;
import com.emc.storageos.model.application.VolumeGroupRestRep;
import com.emc.storageos.model.block.BlockConsistencyGroupRestRep;
import com.emc.storageos.model.block.BlockObjectRestRep;
import com.emc.storageos.model.block.BlockSnapshotRestRep;
import com.emc.storageos.model.block.BlockSnapshotSessionRestRep;
import com.emc.storageos.model.block.NamedVolumesList;
import com.emc.storageos.model.block.VolumeDeleteTypeEnum;
import com.emc.storageos.model.block.VolumeRestRep;
import com.emc.storageos.model.block.VolumeRestRep.ProtectionRestRep;
import com.emc.storageos.model.block.export.ExportBlockParam;
import com.emc.storageos.model.block.export.ExportGroupRestRep;
import com.emc.storageos.model.block.export.ITLRestRep;
import com.emc.storageos.model.host.HostRestRep;
import com.emc.storageos.model.project.ProjectRestRep;
import com.emc.storageos.model.protection.ProtectionSetRestRep;
import com.emc.storageos.model.systems.StorageSystemRestRep;
import com.emc.storageos.model.varray.VirtualArrayRestRep;
import com.emc.storageos.model.vpool.BlockVirtualPoolRestRep;
import com.emc.storageos.model.vpool.VirtualPoolChangeOperationEnum;
import com.emc.storageos.model.vpool.VirtualPoolChangeRep;
import com.emc.storageos.model.vpool.VirtualPoolCommonRestRep;
import com.emc.vipr.client.ViPRCoreClient;
import com.emc.vipr.client.core.filters.BlockVolumeConsistencyGroupFilter;
import com.emc.vipr.client.core.filters.ConsistencyGroupFilter;
import com.emc.vipr.client.core.filters.DefaultResourceFilter;
import com.emc.vipr.client.core.filters.ExportHostOrClusterFilter;
import com.emc.vipr.client.core.filters.ExportVirtualArrayFilter;
import com.emc.vipr.client.core.filters.FilterChain;
import com.emc.vipr.client.core.filters.RecoverPointPersonalityFilter;
import com.emc.vipr.client.core.filters.ResourceFilter;
import com.emc.vipr.client.core.filters.SRDFSourceFilter;
import com.emc.vipr.client.core.filters.SRDFTargetFilter;
import com.emc.vipr.client.core.filters.SourceTargetVolumesFilter;
import com.emc.vipr.client.core.filters.VplexVolumeFilter;
import com.emc.vipr.client.core.filters.VplexVolumeVirtualPoolFilter;
import com.emc.vipr.client.core.util.CachedResources;
import com.emc.vipr.client.core.util.ResourceUtils;
import com.emc.vipr.model.catalog.AssetOption;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

@Component
@AssetNamespace("vipr")
public class BlockProvider extends BaseAssetOptionsProvider {
    public static final String EXCLUSIVE_STORAGE = "exclusive";
    public static final String SHARED_STORAGE = "shared";
    public static final String RECOVERPOINT_BOOKMARK_SNAPSHOT_TYPE_VALUE = "rp";
    public static final String LOCAL_ARRAY_SNAPSHOT_TYPE_VALUE = "local";
    public static final String SNAPSHOT_SESSION_TYPE_VALUE = "session";
    public static final String CG_SNAPSHOT_TYPE_VALUE = "cg";
    public static final String CG_SNAPSHOT_SESSION_TYPE_VALUE = "cgsession";

    public static final String VOLUME_OPTION_KEY = "volume";
    public static final String CONSISTENCY_GROUP_OPTION_KEY = "consistencygroup";

    public static final String LINKED_SNAPSHOT_COPYMODE_VALUE = "copy";
    public static final String LINKED_SNAPSHOT_NOCOPYMODE_VALUE = "nocopy";

    private static final AssetOption VOLUME_OPTION = newAssetOption(VOLUME_OPTION_KEY, "block.storage.type.volume");
    private static final AssetOption CONSISTENCY_GROUP_OPTION = newAssetOption(CONSISTENCY_GROUP_OPTION_KEY,
            "block.storage.type.consistencygroup");

    // Failover option keys
    public static final String LATEST_IMAGE_OPTION_KEY = "latest";
    public static final String PIT_IMAGE_OPTION_KEY = "pit";

    private static final AssetOption LATEST_IMAGE_OPTION = newAssetOption(LATEST_IMAGE_OPTION_KEY, "failover.image.type.latest");
    private static final AssetOption PIT_IMAGE_OPTION = newAssetOption(PIT_IMAGE_OPTION_KEY, "failover.image.type.pit");

    private static final AssetOption EXCLUSIVE_STORAGE_OPTION = newAssetOption(EXCLUSIVE_STORAGE, "block.storage.type.exclusive");
    private static final AssetOption SHARED_STORAGE_OPTION = newAssetOption(SHARED_STORAGE, "block.storage.type.shared");
    private static final AssetOption RECOVERPOINT_BOOKMARK_SNAPSHOT_TYPE_OPTION = newAssetOption(RECOVERPOINT_BOOKMARK_SNAPSHOT_TYPE_VALUE,
            "block.snapshot.type.rp_bookmark");
    private static final AssetOption LOCAL_ARRAY_SNAPSHOT_TYPE_OPTION = newAssetOption(LOCAL_ARRAY_SNAPSHOT_TYPE_VALUE,
            "block.snapshot.type.local");
    private static final AssetOption SNAPSHOT_SESSION_TYPE_OPTION = newAssetOption(SNAPSHOT_SESSION_TYPE_VALUE,
            "block.snapshot.type.session");
    private static final AssetOption CG_SNAPSHOT_TYPE_OPTION = newAssetOption(CG_SNAPSHOT_TYPE_VALUE,
            "block.snapshot.type.cg");
    private static final AssetOption CG_SNAPSHOT_SESSION_TYPE_OPTION = newAssetOption(CG_SNAPSHOT_SESSION_TYPE_VALUE,
            "block.snapshot.type.cg.session");
    private static final AssetOption LINKED_SNAPSHOT_COPYMODE_OPTION = newAssetOption(LINKED_SNAPSHOT_COPYMODE_VALUE,
            "block.snapshot.linked.copymode");
    private static final AssetOption LINKED_SNAPSHOT_NOCOPYMODE_OPTION = newAssetOption(LINKED_SNAPSHOT_NOCOPYMODE_VALUE,
            "block.snapshot.linked.nocopymode");

    private static List<AssetOption> NTFS_OPTIONS = Lists.newArrayList(newAssetOption("DEFAULT", "Default"),
            newAssetOption("512", "512"),
            newAssetOption("1024", "1k"),
            newAssetOption("2048", "2k"),
            newAssetOption("4096", "4k"),
            newAssetOption("8192", "8k"),
            newAssetOption("16k", "16k"),
            newAssetOption("32k", "32k"),
            newAssetOption("64k", "64k"));

    private static List<AssetOption> FAT32_OPTIONS = Lists.newArrayList(newAssetOption("DEFAULT", "Default"),
            newAssetOption("512", "512"),
            newAssetOption("1024", "1k"),
            newAssetOption("2048", "2k"),
            newAssetOption("4096", "4k"),
            newAssetOption("8192", "8k"));

    private static List<AssetOption> ALL_BLOCKSIZE_OPTIONS = Lists.newArrayList(NTFS_OPTIONS);

    private static List<AssetOption> WINDOWS_FILESYSTEM_TYPES = Lists.newArrayList(
            newAssetOption("ntfs", "ntfs"),
            newAssetOption("fat32", "fat32"));

    private static final String BLOCK_CONSISTENCY_GROUP_TYPE = "BlockConsistencyGroup";

    private static final String VOLUME_TYPE = "Volume";

    private static final String NONE_TYPE = "None";

    public static boolean isExclusiveStorage(String storageType) {
        return EXCLUSIVE_STORAGE.equals(storageType);
    }

    public static boolean isSharedStorage(String storageType) {
        return SHARED_STORAGE.equals(storageType);
    }

    public static boolean isVolumeType(String type) {
        return VOLUME_OPTION_KEY.equals(type);
    }

    public static boolean isConsistencyGroupType(String type) {
        return CONSISTENCY_GROUP_OPTION_KEY.equals(type);
    }

    public static boolean checkTypeConsistency(URI volumeId, String volumeOrConsistencyType) {
        if (volumeId == null || volumeOrConsistencyType == null) {
            return false;
        } else if (isVolumeType(volumeOrConsistencyType) && !BlockProviderUtils.isType(volumeId, VOLUME_TYPE)) {
            return false;
        } else if (isConsistencyGroupType(volumeOrConsistencyType) && !BlockProviderUtils.isType(volumeId, BLOCK_CONSISTENCY_GROUP_TYPE)) {
            return false;
        }
        return true;
    }

    static boolean isConsistencyGroupType(URI urlId) {
        return BlockProviderUtils.isType(urlId, BLOCK_CONSISTENCY_GROUP_TYPE);
    }

    protected static boolean isInConsistencyGroup(BlockObjectRestRep volume) {
        return volume.getConsistencyGroup() != null;
    }

    protected static boolean hasXIO3XVolumes(VolumeRestRep volume) {
        return volume.getHasXIO3XVolumes() != null && volume.getHasXIO3XVolumes() == true;
    }

    @Asset("blockVolumeOrConsistencyType")
    @AssetDependencies("project")
    public List<AssetOption> getblockVolumeOrConsistencyType(AssetOptionsContext ctx, URI project) {
        return Lists.newArrayList(VOLUME_OPTION, CONSISTENCY_GROUP_OPTION);
    }

    @Asset("imageToAccess")
    @AssetDependencies("protectedBlockVolume")
    public List<AssetOption> getImageToAccess(AssetOptionsContext ctx, URI protectedBlockVolume) {
        List<AssetOption> options = Lists.newArrayList();
        options = Lists.newArrayList(LATEST_IMAGE_OPTION, PIT_IMAGE_OPTION);

        return options;
    }

    @Asset("blockStorageType")
    public List<AssetOption> getStorageTypes(AssetOptionsContext ctx) {
        return Lists.newArrayList(EXCLUSIVE_STORAGE_OPTION, SHARED_STORAGE_OPTION);
    }

    @Asset("sourceBlockVolume")
    @AssetDependencies("project")
    public List<AssetOption> getSourceVolumes(AssetOptionsContext ctx, URI project) {
        debug("getting source block volumes (project=%s)", project);
        return createVolumeOptions(null, listSourceVolumes(api(ctx), project));
    }

    @Asset("sourceBlockVolumeForAddToApplication")
    @AssetDependencies({ "consistencyGroupAll" })
    public List<AssetOption> getApplicationSourceVolumes(AssetOptionsContext ctx, URI cg) {
        
        final ViPRCoreClient client = api(ctx);
        VolumeGroupList applications = client.application().getApplications();
        List<URI> applicationIds = new ArrayList<URI>();
        for (NamedRelatedResourceRep volumeGroup : applications.getVolumeGroups()) {
            VolumeGroupRestRep vg = client.application().getApplication(volumeGroup.getId());
            if (vg.getRoles().contains("COPY")) {
                applicationIds.add(volumeGroup.getId());
            }
        }
        
        ResourceFilter<VolumeRestRep> cgFilter = new BlockVolumeConsistencyGroupFilter(cg, false);
        
        List<ProjectRestRep> projects = api(ctx).projects().getByTenant(ctx.getTenant());
        List<VolumeRestRep> volumes = new ArrayList<VolumeRestRep>();
        for (ProjectRestRep project : projects) {
            volumes.addAll(listSourceVolumes(api(ctx), project.getId(), cgFilter));
        }
        List<VolumeRestRep> volumesNotInApplication = new ArrayList<VolumeRestRep>();
        for (VolumeRestRep volume : volumes) {
            if (volume.getVolumeGroups()!= null && !volume.getVolumeGroups().isEmpty()) {
                boolean volumeIsInApplication = false;
                for (RelatedResourceRep rep : volume.getVolumeGroups()) {
                    if (applicationIds.contains(rep.getId())) {
                        volumeIsInApplication = true;
                        break;
                    }
                }
                if (!volumeIsInApplication) {
                    volumesNotInApplication.add(volume);
                }
            } else {
                volumesNotInApplication.add(volume);
            }
        }

        return createVolumeOptions(null, volumesNotInApplication);
    }

    @Asset("sourceBlockVolumeInConsistencyGroup")
    @AssetDependencies({ "project", "consistencyGroup" })
    public List<AssetOption> getSourceVolumesWithoutConsistencyGroup(AssetOptionsContext ctx, URI project, URI consistencyGroup) {
        debug("getting source block volumes in consistency group or no consistency group (project=%s, consistency group=%s)", project,
                consistencyGroup);
        return createVolumeOptions(null,
                listSourceVolumes(api(ctx), project, new BlockVolumeConsistencyGroupFilter(consistencyGroup, true)));
    }

    /**
     * Get source volumes for a specific project. If the deletionType is VIPR_ONLY, create
     * a filter that only retrieves Volumes with Host Exports
     *
     * @param ctx
     * @param project
     * @param deletionType
     * @return
     */
    @Asset("sourceBlockVolumeWithDeletion")
    @AssetDependencies({ "project", "deletionType" })
    public List<AssetOption> getSourceVolumesWithDeletion(final AssetOptionsContext ctx, URI project, String deletionType) {
        debug("getting source block volumes (project=%s)", project);
        List<VolumeRestRep> volumes = listSourceVolumes(api(ctx), project);
        return createVolumeOptions(null, volumes);
    }

    @Asset("exportedBlockVolume")
    @AssetDependencies("project")
    public List<AssetOption> getExportedVolumes(AssetOptionsContext ctx, URI project) {
        debug("getting source block volumes (project=%s)", project);
        final ViPRCoreClient client = api(ctx);
        // Filter volumes that are not RP Metadata
        List<URI> volumeIds = Lists.newArrayList();
        for (ExportGroupRestRep export : client.blockExports().findByProject(project)) {
            for (ExportBlockParam resource : export.getVolumes()) {
                if (ResourceType.isType(ResourceType.VOLUME, resource.getId())) {
                    volumeIds.add(resource.getId());
                }
            }
        }
        FilterChain<VolumeRestRep> filter = new FilterChain<VolumeRestRep>(RecoverPointPersonalityFilter.METADATA.not());
        List<VolumeRestRep> volumes = client.blockVolumes().getByIds(volumeIds, filter);
        return createVolumeWithVarrayOptions(client, volumes);
    }

    @Asset("deletionType")
    public List<AssetOption> getDeletionType(AssetOptionsContext ctx) {
        List<AssetOption> options = new ArrayList<>();

        options.add(newAssetOption(VolumeDeleteTypeEnum.FULL.toString(), "block.deletion.type.full"));
        options.add(newAssetOption(VolumeDeleteTypeEnum.VIPR_ONLY.toString(), "block.deletion.type.vipr_only"));

        return options;
    }

    @Asset("fullOnlyDeletionType")
    public List<AssetOption> getFullOnlyDeletionType(AssetOptionsContext ctx) {
        List<AssetOption> options = new ArrayList<>();

        options.add(newAssetOption(VolumeDeleteTypeEnum.FULL.toString(), "block.deletion.type.full"));

        return options;
    }

    @Asset("vplexMigrationChangeOperation")
    public List<AssetOption> getVplexMigrationChangeOperations(AssetOptionsContext ctx) {
        List<AssetOption> options = Lists.newArrayList();
        options.add(newAssetOption(VirtualPoolChangeOperationEnum.VPLEX_DATA_MIGRATION.name(), "virtualPoolChange.operation."
                + VirtualPoolChangeOperationEnum.VPLEX_DATA_MIGRATION.name()));
        options.add(newAssetOption(VirtualPoolChangeOperationEnum.VPLEX_LOCAL_TO_DISTRIBUTED.name(), "virtualPoolChange.operation."
                + VirtualPoolChangeOperationEnum.VPLEX_LOCAL_TO_DISTRIBUTED.name()));
        return options;
    }

    @Asset("virtualPoolChangeOperation")
    public List<AssetOption> getVirtualPoolchangeOperations(AssetOptionsContext ctx) {
        List<AssetOption> options = Lists.newArrayList();
        for (VirtualPoolChangeOperationEnum operation : VirtualPoolChangeOperationEnum.values()) {
            options.add(newAssetOption(operation.name(), "virtualPoolChange.operation." + operation.name()));
        }
        return options;
    }

    @Asset("virtualPoolChangeVolumeWithSource")
    @AssetDependencies({ "project", "blockVirtualPool" })
    public List<AssetOption> getVpoolChangeVolumes(AssetOptionsContext ctx, URI projectId, URI virtualPoolId) {
        return createVolumeOptions(api(ctx), listSourceVolumes(api(ctx), projectId, new VirtualPoolFilter(virtualPoolId)));
    }

    @Asset("virtualPoolChangeVolumeWithSourceFilter")
    @AssetDependencies({ "project", "blockVirtualPool", "sourceVolumeFilter" })
    public List<AssetOption> getVpoolChangeVolumes(AssetOptionsContext ctx, URI projectId, URI virtualPoolId, int volumePage) {
        List<AssetOption> options = createVolumeOptions(api(ctx),
                listSourceVolumes(api(ctx), projectId, new VirtualPoolFilter(virtualPoolId)));
        return VirtualDataCenterProvider.getVolumeSublist(volumePage, options);
    }

    @Asset("sourceVolumeFilter")
    @AssetDependencies({ "project", "blockVirtualPool" })
    public List<AssetOption> getVolumeFilter(AssetOptionsContext ctx, URI projectId, URI virtualPoolId) {
        List<String> volumeNames = Lists.newArrayList();
        for (VolumeRestRep volume : listSourceVolumes(api(ctx), projectId, new VirtualPoolFilter(virtualPoolId))) {
            volumeNames.add(volume.getName());
        }
        Collections.sort(volumeNames, new StringComparator(false));
        return VirtualDataCenterProvider.getVolumeFilterOptions(volumeNames);
    }

    @Asset("virtualArrayChangeVolume")
    @AssetDependencies({ "project", "targetVirtualArray" })
    public List<AssetOption> getVirtualArrayChangeCandidateVolumes(AssetOptionsContext ctx, URI projectId, URI varrayId) {
        ViPRCoreClient client = api(ctx);
        NamedVolumesList volumeList = client.blockVolumes().listVirtualArrayChangeCandidates(projectId, varrayId);
        List<VolumeRestRep> volumes = client.blockVolumes().getByRefs(volumeList.getVolumes());
        return createBaseResourceOptions(volumes);
    }

    @Asset("targetVirtualArray")
    @AssetDependencies("project")
    public List<AssetOption> getTargetVplexVolumeVirtualArrays(AssetOptionsContext ctx, URI projectId) {
        ViPRCoreClient client = api(ctx);
        List<AssetOption> targets = Lists.newArrayList();
        for (VirtualArrayRestRep varray : client.varrays().getAll()) {
            targets.add(createBaseResourceOption(varray));
        }
        return targets;
    }

    @Asset("migrationTargetVirtualPool")
    @AssetDependencies({ "project", "blockVirtualPool", "vplexMigrationChangeOperation" })
    public List<AssetOption> getMigrationTargetVirtualPools(AssetOptionsContext ctx, URI projectId, URI virtualPoolId,
            String vpoolChangeOperation) {
        return getTargetVirtualPoolsForVpool(ctx, projectId, virtualPoolId, vpoolChangeOperation);
    }

    @Asset("journalCopyName")
    @AssetDependencies("rpConsistencyGroupByProject")
    public List<AssetOption> getCopyNameByConsistencyGroup(AssetOptionsContext ctx, URI consistencyGroupId) {
        ViPRCoreClient client = api(ctx);
        List<RelatedResourceRep> volumes = client.blockConsistencyGroups().get(consistencyGroupId).getVolumes();
        Set<String> copyNames = Sets.newHashSet();

        for (RelatedResourceRep volume : volumes) {
            VolumeRestRep volumeRep = client.blockVolumes().get(volume);
            if (volumeRep.getProtection() != null && volumeRep.getProtection().getRpRep() != null) {
                if (volumeRep.getProtection().getRpRep().getCopyName() != null) {
                    String copyName = volumeRep.getProtection().getRpRep().getCopyName();
                    copyNames.add(copyName);
                }
                if (volumeRep.getHaVolumes() != null) {
                    List<RelatedResourceRep> haVolumes = volumeRep.getHaVolumes();
                    for (RelatedResourceRep haVolume : haVolumes) {
                        VolumeRestRep haVolumeRep = client.blockVolumes().get(haVolume);
                        if (haVolumeRep.getProtection() != null && haVolumeRep.getProtection().getRpRep() != null
                                && haVolumeRep.getProtection().getRpRep().getCopyName() != null) {
                            String copyName = haVolumeRep.getProtection().getRpRep().getCopyName();
                            copyNames.add(copyName);
                        }

                    }
                }
                if (volumeRep.getProtection().getRpRep().getRpTargets() != null) {
                    List<VirtualArrayRelatedResourceRep> targetVolumes = volumeRep.getProtection().getRpRep().getRpTargets();
                    for (VirtualArrayRelatedResourceRep target : targetVolumes) {
                        VolumeRestRep targetVolume = client.blockVolumes().get(target.getId());
                        String copyName = targetVolume.getProtection().getRpRep().getCopyName();
                        copyNames.add(copyName);
                    }
                }
            }
        }

        List<AssetOption> copyNameAssets = Lists.newArrayList();
        for (String copyName : copyNames) {
            copyNameAssets.add(newAssetOption(copyName, copyName));
        }
        AssetOptionsUtils.sortOptionsByLabel(copyNameAssets);
        return copyNameAssets;
    }

    @Asset("virtualArrayByConsistencyGroup")
    @AssetDependencies("rpConsistencyGroupByProject")
    public List<AssetOption> getVirtualArrayByConsistencyGroup(AssetOptionsContext ctx, URI consistencyGroupId) {
        ViPRCoreClient client = api(ctx);
        List<RelatedResourceRep> volumes = client.blockConsistencyGroups().get(consistencyGroupId).getVolumes();
        List<AssetOption> targets = Lists.newArrayList();

        if (!volumes.isEmpty()) {
            RelatedResourceRep varrayId = client.blockVolumes().get(volumes.get(0)).getVirtualArray();
            VirtualArrayRestRep virtualArray = client.varrays().get(varrayId);
            targets.add(createBaseResourceOption(virtualArray));
        }
        return targets;
    }

    @Asset("targetVirtualPool")
    @AssetDependencies({ "sourceBlockVolume", "virtualPoolChangeOperation" })
    public List<AssetOption> getTargetVirtualPools(AssetOptionsContext ctx, URI volumeId, String vpoolChangeOperation) {
        List<VirtualPoolChangeRep> vpoolChanges = api(ctx).blockVolumes().listVirtualPoolChangeCandidates(volumeId);
        return createVpoolChangeOptions(vpoolChangeOperation, vpoolChanges);
    }

    @Asset("targetVirtualPool")
    @AssetDependencies({ "project", "blockVirtualPool", "virtualPoolChangeOperation" })
    public List<AssetOption> getTargetVirtualPoolsForVpool(AssetOptionsContext ctx, URI projectId, URI virtualPoolId,
            String vpoolChangeOperation) {
        List<VolumeRestRep> volumes = listSourceVolumes(api(ctx), projectId, new VirtualPoolFilter(virtualPoolId));
        List<URI> volumeIds = Lists.newArrayList();
        for (VolumeRestRep volume : volumes) {
            volumeIds.add(volume.getId());
        }
        if (CollectionUtils.isNotEmpty(volumeIds)) {
            BulkIdParam input = new BulkIdParam();
            input.setIds(volumeIds);
            List<VirtualPoolChangeRep> vpoolChanges = api(ctx).blockVpools().listVirtualPoolChangeCandidates(virtualPoolId, input);
            return createVpoolChangeOptions(vpoolChangeOperation, vpoolChanges);
        }
        return Collections.emptyList();
    }

    @Asset("volumeExport")
    @AssetDependencies("blockVolume")
    public List<AssetOption> getExportsForVolume(AssetOptionsContext ctx, URI volumeId) {
        Set<NamedRelatedResourceRep> exports = getUniqueExportsForVolume(ctx, volumeId);
        return createNamedResourceOptions(exports);
    }

    @Asset("snapshotExport")
    @AssetDependencies("exportedBlockSnapshot")
    public List<AssetOption> getExportsForSnapshot(AssetOptionsContext ctx, URI snapshotId) {
        Set<NamedRelatedResourceRep> exports = getUniqueExportsForSnapshot(ctx, snapshotId);
        return createNamedResourceOptions(exports);
    }

    @Asset("volumeExport")
    @AssetDependencies("sourceBlockVolume")
    public List<AssetOption> getExportsForSourceVolume(AssetOptionsContext ctx, URI volumeId) {
        Set<NamedRelatedResourceRep> exports = getUniqueExportsForVolume(ctx, volumeId);
        return createNamedResourceOptions(exports);
    }

    @Asset("volumeExport")
    @AssetDependencies("exportedBlockVolume")
    public List<AssetOption> getExportsForExportedVolume(AssetOptionsContext ctx, URI volumeId) {
        final ViPRCoreClient client = api(ctx);
        Set<NamedRelatedResourceRep> exports = getUniqueExportsForVolume(ctx, volumeId);
        return createExportWithVarrayOptions(client, client.blockExports().getByRefs(exports));
    }

    /**
     * Gets the set of unique exports for a given volume.
     *
     * @param ctx
     *            the asset options context
     * @param volumeId
     *            the volume id.
     * @return the export resource IDs.
     */
    protected Set<NamedRelatedResourceRep> getUniqueExportsForVolume(AssetOptionsContext ctx, URI volumeId) {
        return convertITLListToRelatedResourceRestReps(api(ctx).blockVolumes().getExports(volumeId));
    }

    /** Gets the set of unique exports for a given snapshot */
    protected Set<NamedRelatedResourceRep> getUniqueExportsForSnapshot(AssetOptionsContext ctx, URI snapshotId) {
        return convertITLListToRelatedResourceRestReps(api(ctx).blockSnapshots().listExports(snapshotId));
    }

    /** Find the unique exports for the given */
    protected Set<NamedRelatedResourceRep> convertITLListToRelatedResourceRestReps(List<ITLRestRep> exports) {
        Set<NamedRelatedResourceRep> relatedRestReps = Sets.newHashSet();
        for (ITLRestRep export : exports) {
            relatedRestReps.add(export.getExport());
        }
        return relatedRestReps;
    }

    protected static List<AssetOption> createExportWithVarrayOptions(ViPRCoreClient client,
            Collection<? extends ExportGroupRestRep> exportObjects) {
        List<URI> varrayIds = getExportVirtualArrayIds(exportObjects);
        Map<URI, VirtualArrayRestRep> varrayNames = getVirutalArrayNames(client, varrayIds);
        List<AssetOption> options = Lists.newArrayList();
        for (ExportGroupRestRep export : exportObjects) {
            options.add(createExportWithVarrayOption(export, varrayNames));
        }
        AssetOptionsUtils.sortOptionsByLabel(options);
        return options;
    }

    protected static AssetOption createExportWithVarrayOption(ExportGroupRestRep export, Map<URI, VirtualArrayRestRep> varrayNames) {
        String varrayName = varrayNames.get(export.getVirtualArray().getId()).getName();
        String label = getMessage("block.unexport.export", export.getName(), varrayName);
        return new AssetOption(export.getId(), label);
    }

    private static List<URI> getExportVirtualArrayIds(Collection<? extends ExportGroupRestRep> exportObjects) {
        List<URI> varrayIds = Lists.newArrayList();
        for (ExportGroupRestRep export : exportObjects) {
            varrayIds.add(export.getVirtualArray().getId());
        }
        return varrayIds;
    }

    @Asset("unassignedBlockVolume")
    @AssetDependencies({ "host", "project" })
    public List<AssetOption> getBlockVolumes(AssetOptionsContext ctx, URI hostOrClusterId, final URI projectId) {
        ViPRCoreClient client = api(ctx);
        Set<URI> exportedBlockResources = BlockProvider.getExportedVolumes(api(ctx), projectId, hostOrClusterId, null);
        UnexportedBlockResourceFilter<VolumeRestRep> unexportedFilter = new UnexportedBlockResourceFilter<VolumeRestRep>(
                exportedBlockResources);
        SourceTargetVolumesFilter sourceTargetVolumesFilter = new SourceTargetVolumesFilter();
        List<VolumeRestRep> volumes = client.blockVolumes().findByProject(projectId, unexportedFilter.and(sourceTargetVolumesFilter));

        return createBaseResourceOptions(volumes);
    }

    @Asset("unassignedVplexBlockVolume")
    @AssetDependencies({ "project", "host", "virtualArray" })
    public List<AssetOption> getBlockVolumes(AssetOptionsContext ctx, final URI projectId, URI hostOrClusterId, URI virtualArrayId) {
        // get a list of all 'source' volumes that :
        // - are in this project and not exported to the given host/cluster
        // - are a 'source' volume
        // - are vplex volumes
        ViPRCoreClient client = api(ctx);
        Set<URI> exportedBlockResources = BlockProvider.getExportedVolumes(client, projectId, hostOrClusterId, virtualArrayId);
        UnexportedBlockResourceFilter<VolumeRestRep> unexportedFilter = new UnexportedBlockResourceFilter<VolumeRestRep>(
                exportedBlockResources);
        SourceTargetVolumesFilter sourceTargetVolumesFilter = new SourceTargetVolumesFilter();
        VplexVolumeFilter vplexVolumeFilter = new VplexVolumeFilter();

        CachedResources<BlockVirtualPoolRestRep> blockVpools = new CachedResources<>(client.blockVpools());
        VplexVolumeVirtualPoolFilter virtualPoolFilter = new VplexVolumeVirtualPoolFilter(blockVpools);

        FilterChain<VolumeRestRep> filter = sourceTargetVolumesFilter.and(unexportedFilter).and(vplexVolumeFilter.or(virtualPoolFilter));

        // perform the query and apply the filter
        List<VolumeRestRep> volumes = client.blockVolumes().findByProject(projectId, filter);

        // get a list of all volumes from our list that are in the target VArray, or use the target VArray for protection
        List<BlockObjectRestRep> acceptedVolumes = getVPlexVolumesInTargetVArray(client, virtualArrayId, volumes);

        return createBaseResourceOptions(acceptedVolumes);
    }

    @Asset("unassignedBlockSnapshot")
    @AssetDependencies({ "host", "project" })
    public List<AssetOption> getBlockSnapshots(AssetOptionsContext ctx, URI hostOrClusterId, URI projectId) {
        // get a list of all snapshots that are in this project that are not exported to the given host/cluster
        Set<URI> exportedBlockResources = getExportedVolumes(api(ctx), projectId, hostOrClusterId, null);
        UnexportedBlockResourceFilter<BlockSnapshotRestRep> unexportedSnapshotFilter =
                new UnexportedBlockResourceFilter<BlockSnapshotRestRep>(exportedBlockResources);
        return getSnapshotOptionsForProject(ctx, projectId, unexportedSnapshotFilter);
    }

    @Asset("vplexVolumeWithSnapshots")
    @AssetDependencies({ "project", "blockVolumeOrConsistencyType" })
    public List<AssetOption> getVplexSnapshotVolumes(AssetOptionsContext ctx, URI project, String volumeOrConsistencyType) {
        final ViPRCoreClient client = api(ctx);
        if (isVolumeType(volumeOrConsistencyType)) {
            List<VolumeRestRep> volumes = client.blockVolumes().findByProject(project, new DefaultResourceFilter<VolumeRestRep>() {
                @Override
                public boolean accept(VolumeRestRep volume) {
                    if (volume.getHaVolumes() != null && !volume.getHaVolumes().isEmpty()
                            && !client.blockSnapshots().getByVolume(volume.getId()).isEmpty() && !isInConsistencyGroup(volume)) {
                        return true;
                    } else {
                        return false;
                    }
                }
            });
            return createVolumeOptions(client, volumes);
        } else {
            List<BlockConsistencyGroupRestRep> consistencyGroups = client.blockConsistencyGroups().findByProject(project,
                    new DefaultResourceFilter<BlockConsistencyGroupRestRep>() {
                        @Override
                        public boolean accept(BlockConsistencyGroupRestRep cg) {
                            if (cg.getTypes() != null && cg.getTypes().contains(Types.VPLEX.name())) {
                                return true;
                            } else {
                                return false;
                            }
                        }

                    });
            return createBaseResourceOptions(consistencyGroups);
        }
    }

    @Asset("vplexBlockSnapshot")
    @AssetDependencies({ "project", "blockVolumeOrConsistencyType", "vplexVolumeWithSnapshots" })
    public List<AssetOption> getVplexBlockSnapshots(AssetOptionsContext ctx, URI projectId, String type, URI volumeOrCGId) {
        if (isVolumeType(type) && BlockProviderUtils.isType(volumeOrCGId, VOLUME_TYPE)) {
            List<BlockSnapshotRestRep> snapshots = api(ctx).blockSnapshots().getByVolume(volumeOrCGId);
            return constructSnapshotOptions(api(ctx), projectId, snapshots);
        } else if (isConsistencyGroupType(type) && BlockProviderUtils.isType(volumeOrCGId, BLOCK_CONSISTENCY_GROUP_TYPE)) {
            return getConsistencyGroupSnapshots(ctx, volumeOrCGId);
        } else {
            return new ArrayList<AssetOption>();
        }
    }

    public static class UnexportedBlockResourceFilter<T extends BlockObjectRestRep> extends DefaultResourceFilter<T> {

        /** The list of block resources ids that have been exported to this host/cluster */
        private final Set<URI> exportedBlockResources;

        public UnexportedBlockResourceFilter(Set<URI> exportedBlockResources) {
            this.exportedBlockResources = exportedBlockResources;
        }

        @Override
        public boolean acceptId(URI resourceId) {
            // accept this volume if it hasn't been exported to this host/cluster
            return !exportedBlockResources.contains(resourceId);
        }

    }

    @Asset("blockVirtualPool")
    public List<AssetOption> getBlockVirtualPools(AssetOptionsContext ctx) {
        debug("getting blockVirtualPools");
        return createBaseResourceOptions(api(ctx).blockVpools().getAll());
    }

    @Asset("blockVirtualPoolFilter")
    public List<AssetOption> getBlockVirtualPoolFilters(AssetOptionsContext ctx) {
        List<AssetOption> options = createBaseResourceOptions(api(ctx).blockVpools().getAll());
        options.add(0, new AssetOption("All", "All"));
        return options;
    }

    /**
     * Returns the virtual pools for a given virtualArray (initially added for the Create Volume service)
     *
     * @param ctx
     * @param virtualArray
     * @return
     */
    @Asset("blockVirtualPool")
    @AssetDependencies({ "virtualArray" })
    public List<AssetOption> getVirtualPoolsForVirtualArray(AssetOptionsContext ctx, URI virtualArray) {
        debug("getting virtualPoolsForVirtualArray(virtualArray=%s)", virtualArray);
        List<BlockVirtualPoolRestRep> virtualPools = api(ctx).blockVpools().getByVirtualArray(virtualArray);
        return createVirtualPoolResourceOptions(virtualPools);
    }

    @Asset("blockVirtualPool")
    @AssetDependencies({ "virtualArrayByConsistencyGroup" })
    public List<AssetOption> getVirtualPoolsForVirtualArrayByCG(AssetOptionsContext ctx, URI virtualArray) {
        return getVirtualPoolsForVirtualArray(ctx, virtualArray);
    }

    @Asset("blockVolume")
    @AssetDependencies("project")
    public List<AssetOption> getVolumes(AssetOptionsContext ctx, URI project) {
        debug("getting volumes (project=%s)", project);
        ViPRCoreClient client = api(ctx);
        return createVolumeOptions(client, listVolumes(client, project));
    }

    @Asset("blockVolumeByType")
    @AssetDependencies({ "project", "blockVolumeOrConsistencyType" })
    public List<AssetOption> getVolumesByType(AssetOptionsContext ctx, URI project, String type) {
        debug("getting volumes (project=%s)", project);
        final ViPRCoreClient client = api(ctx);
        if (isVolumeType(type)) {
            return createVolumeOptions(client, listVolumesWithoutConsistencyGroup(client, project));
        } else {
            List<BlockConsistencyGroupRestRep> consistencyGroups = api(ctx).blockConsistencyGroups()
                    .search()
                    .byProject(project)
                    .run();
            return createBaseResourceOptions(consistencyGroups);
        }
    }

    @Asset("protectedBlockVolume")
    @AssetDependencies({ "project", "blockVolumeOrConsistencyType" })
    public List<AssetOption> getProtectedVolumes(AssetOptionsContext ctx, URI project, String volumeOrConsistencyType) {
        if (isVolumeType(volumeOrConsistencyType)) {
            debug("getting protected volumes (project=%s)", project);
            // Allow recoverpoint or SRDF sources
            ResourceFilter<VolumeRestRep> filter = RecoverPointPersonalityFilter.SOURCE.or(new SRDFSourceFilter());
            ViPRCoreClient client = api(ctx);
            List<VolumeRestRep> volumes = client.blockVolumes().findByProject(project, filter);
            return createVolumeOptions(client, volumes);
        } else {
            debug("getting protected consistency groups (project=%s)", project);
            // Allow recoverpoint or SRDF sources
            ResourceFilter<BlockConsistencyGroupRestRep> filter = new ConsistencyGroupFilter(BlockConsistencyGroup.Types.RP.name(),
                    false).or(new ConsistencyGroupFilter(BlockConsistencyGroup.Types.SRDF.name(),
                    false));
            List<BlockConsistencyGroupRestRep> consistencyGroups = api(ctx).blockConsistencyGroups()
                    .search()
                    .byProject(project)
                    .filter(filter)
                    .run();

            return createBaseResourceOptions(consistencyGroups);
        }
    }

    @Asset("failoverTarget")
    @AssetDependencies("protectedBlockVolume")
    public List<AssetOption> getFailoverTarget(AssetOptionsContext ctx, URI protectedBlockVolume) {
        if (protectedBlockVolume != null) {
            ViPRCoreClient client = api(ctx);

            if (BlockProviderUtils.isType(protectedBlockVolume, VOLUME_TYPE)) {
                debug("getting failoverTargets (protectedBlockVolume=%s)", protectedBlockVolume);
                VolumeRestRep volume = client.blockVolumes().get(protectedBlockVolume);

                ProtectionRestRep protection = volume.getProtection();
                if (protection != null) {
                    // RecoverPoint protection
                    if (protection.getRpRep() != null && protection.getRpRep().getProtectionSet() != null) {
                        return getRpFailoverTargets(client, volume);
                    }
                    // VMAX SRDF protection
                    if (protection.getSrdfRep() != null && protection.getSrdfRep().getSRDFTargetVolumes() != null
                            && !protection.getSrdfRep().getSRDFTargetVolumes().isEmpty()) {
                        return getSrdfFailoverTargets(client, volume);
                    }
                }
            } else if (BlockProviderUtils.isType(protectedBlockVolume, BLOCK_CONSISTENCY_GROUP_TYPE)) {
                debug("getting failoverTargets for consistency group %s", protectedBlockVolume);
                BlockConsistencyGroupRestRep cg = client.blockConsistencyGroups().get(protectedBlockVolume);

                List<VolumeRestRep> srcVolumes = null;
                // Get RP source volumes
                if (cg.getTypes().contains(BlockConsistencyGroup.Types.RP.name())) {
                    srcVolumes = client.blockVolumes().getByRefs(cg.getVolumes(), RecoverPointPersonalityFilter.SOURCE);
                }
                // Get SRDF source volumes
                if (cg.getTypes().contains(BlockConsistencyGroup.Types.SRDF.name())) {
                    srcVolumes = client.blockVolumes().getByRefs(cg.getVolumes(), new SRDFSourceFilter());
                }

                if (srcVolumes != null && !srcVolumes.isEmpty()) {
                    // Get the first source volume and obtain its target references
                    VolumeRestRep srcVolume = srcVolumes.get(0);

                    if (cg.getTypes() != null) {
                        Map<String, String> targetVolumes = Maps.newLinkedHashMap();
                        CachedResources<VirtualArrayRestRep> virtualArrays = new CachedResources<VirtualArrayRestRep>(client.varrays());
                        List<VirtualArrayRelatedResourceRep> targets = new ArrayList<VirtualArrayRelatedResourceRep>();

                        // Process the RP targets
                        if (cg.getTypes().contains(BlockConsistencyGroup.Types.RP.name())) {
                            targets = srcVolume.getProtection().getRpRep().getRpTargets();
                        }
                        // Process the SRDF targets
                        if (cg.getTypes().contains(BlockConsistencyGroup.Types.SRDF.name())) {
                            targets = srcVolume.getProtection().getSrdfRep().getSRDFTargetVolumes();
                        }

                        for (VolumeRestRep targetVolume : client.blockVolumes().getByRefs(targets)) {
                            VirtualArrayRestRep virtualArray = virtualArrays.get(targetVolume.getVirtualArray());
                            String label = getMessage(name(virtualArray));
                            targetVolumes.put(stringId(virtualArray), label);
                        }

                        List<AssetOption> options = Lists.newArrayList();
                        for (Map.Entry<String, String> entry : targetVolumes.entrySet()) {
                            options.add(new AssetOption(entry.getKey(), entry.getValue()));
                        }
                        AssetOptionsUtils.sortOptionsByLabel(options);
                        return options;
                    }
                }
            }
        }

        return Lists.newArrayList();
    }

    protected List<AssetOption> getRpFailoverTargets(ViPRCoreClient client, VolumeRestRep volume) {
        Map<String, String> targetVolumes = Maps.newLinkedHashMap();
        URI protectionSetId = volume.getProtection().getRpRep().getProtectionSet().getId();
        ProtectionSetRestRep localProtectionSet = client.blockVolumes().getProtectionSet(volume.getId(),
                protectionSetId);
        String sourceSiteName = volume.getProtection().getRpRep().getInternalSiteName();
        CachedResources<VirtualArrayRestRep> virtualArrays = new CachedResources<VirtualArrayRestRep>(client.varrays());

        List<RelatedResourceRep> rpTargets = localProtectionSet.getVolumes();
        for (VolumeRestRep protectionSetVolume : client.blockVolumes().getByRefs(rpTargets, RecoverPointPersonalityFilter.TARGET)) {
            String targetSiteName = protectionSetVolume.getProtection().getRpRep().getInternalSiteName();
            boolean isLocal = StringUtils.equals(sourceSiteName, targetSiteName);
            String rpType = isLocal ? "local" : "remote";
            VirtualArrayRestRep virtualArray = virtualArrays.get(protectionSetVolume.getVirtualArray());
            String label = getMessage("recoverpoint.target", name(protectionSetVolume), rpType, name(virtualArray));
            targetVolumes.put(stringId(protectionSetVolume), label);
        }

        List<AssetOption> options = Lists.newArrayList();
        for (Map.Entry<String, String> entry : targetVolumes.entrySet()) {
            options.add(new AssetOption(entry.getKey(), entry.getValue()));
        }
        AssetOptionsUtils.sortOptionsByLabel(options);
        return options;
    }

    protected List<AssetOption> getSrdfFailoverTargets(ViPRCoreClient client, VolumeRestRep volume) {
        Map<String, String> targetVolumes = Maps.newLinkedHashMap();
        CachedResources<VirtualArrayRestRep> virtualArrays = new CachedResources<VirtualArrayRestRep>(client.varrays());

        List<VirtualArrayRelatedResourceRep> srdfTargets = volume.getProtection().getSrdfRep().getSRDFTargetVolumes();
        for (VolumeRestRep protectionSetVolume : client.blockVolumes().getByRefs(srdfTargets, new SRDFTargetFilter())) {
            VirtualArrayRestRep virtualArray = virtualArrays.get(protectionSetVolume.getVirtualArray());
            String label = getMessage("srdf.target", name(protectionSetVolume), name(virtualArray));
            targetVolumes.put(stringId(protectionSetVolume), label);
        }

        List<AssetOption> options = Lists.newArrayList();
        for (Map.Entry<String, String> entry : targetVolumes.entrySet()) {
            options.add(new AssetOption(entry.getKey(), entry.getValue()));
        }
        AssetOptionsUtils.sortOptionsByLabel(options);
        return options;
    }

    @Asset("blockSnapshot")
    @AssetDependencies({ "project" })
    public List<AssetOption> getBlockSnapshotsByVolume(AssetOptionsContext ctx, URI project) {
        debug("getting blockSnapshots (project=%s)", project);
        return getSnapshotOptionsForProject(ctx, project);
    }

    @Asset("exportedBlockSnapshot")
    @AssetDependencies({ "project" })
    public List<AssetOption> getExportedBlockSnapshotsByVolume(AssetOptionsContext ctx, URI project) {
        debug("getting exported blockSnapshots (project=%s)", project);
        final ViPRCoreClient client = api(ctx);
        List<URI> snapshotIds = Lists.newArrayList();
        for (ExportGroupRestRep export : client.blockExports().findByProject(project)) {
            for (ExportBlockParam resource : export.getVolumes()) {
                if (ResourceType.isType(ResourceType.BLOCK_SNAPSHOT, resource.getId())) {
                    snapshotIds.add(resource.getId());
                }
            }
        }
        List<BlockSnapshotRestRep> snapshots = client.blockSnapshots().getByIds(snapshotIds);
        return createVolumeWithVarrayOptions(client, snapshots);
    }

    private List<AssetOption> getVolumeSnapshotOptionsForProject(AssetOptionsContext ctx, URI project) {
        final ViPRCoreClient client = api(ctx);
        List<BlockSnapshotRestRep> snapshots = client.blockSnapshots().findByProject(project,
                new DefaultResourceFilter<BlockSnapshotRestRep>() {
                    @Override
                    public boolean accept(BlockSnapshotRestRep snapshot) {
                        VolumeRestRep parentVolume = client.blockVolumes().get(snapshot.getParent().getId());
                        return (isRPSourceVolume(parentVolume) || !isInConsistencyGroup(snapshot) || hasXIO3XVolumes(parentVolume));
                    }
                });

        return constructSnapshotOptions(client, project, snapshots);
    }

    private List<AssetOption> getVolumeSnapshotSessionOptionsForProject(AssetOptionsContext ctx, URI project) {
        final ViPRCoreClient client = api(ctx);
        List<BlockSnapshotSessionRestRep> snapshotSessions = client.blockSnapshotSessions().findByProject(project);
        return constructSnapshotSessionOptions(client, project, snapshotSessions);
    }

    @Asset("blockSnapshotOrConsistencyGroup")
    @AssetDependencies({ "project", "blockVolumeOrConsistencyType", "blockSnapshotType", "consistencyGroupByProjectAndType" })
    public List<AssetOption> getBlockSnapshotsByVolume(AssetOptionsContext ctx, URI project, String storageType, String snapshotType,
            URI consistencyGroupId) {
        if (NONE_TYPE.equals(storageType)) {
            return new ArrayList<AssetOption>();
        } else {
            if (CG_SNAPSHOT_TYPE_VALUE.equals(snapshotType)
                    || CG_SNAPSHOT_SESSION_TYPE_VALUE.equals(snapshotType)) {
                if (consistencyGroupId == null) {
                    error("Consistency type invalid : %s", consistencyGroupId);
                    return new ArrayList<AssetOption>();
                }
                if (!BlockProviderUtils.isType(consistencyGroupId, BLOCK_CONSISTENCY_GROUP_TYPE)) {
                    error("Consistency Group field is required for Storage Type [%s, %s]", storageType, consistencyGroupId);
                    return new ArrayList<AssetOption>();
                }
                if (CG_SNAPSHOT_SESSION_TYPE_VALUE.equals(snapshotType)) {
                    return getConsistencyGroupSnapshotSessions(ctx, consistencyGroupId);
                } else {
                    return getConsistencyGroupSnapshots(ctx, consistencyGroupId);
                }
            } else if (SNAPSHOT_SESSION_TYPE_VALUE.equals(snapshotType)) {
                info("getting blockSnapshotSessions (project=%s)", project);
                return getVolumeSnapshotSessionOptionsForProject(ctx, project);
            } else {
                info("getting blockSnapshots (project=%s)", project);
                return getVolumeSnapshotOptionsForProject(ctx, project);
            }
        }
    }

    @Asset("snapshotAvailableForResynchronize")
    @AssetDependencies({ "blockVolumeWithSnapshot", "blockVolumeOrConsistencyType" })
    public List<AssetOption> getSnapshotSynchronize(AssetOptionsContext ctx, URI volumeId, String volumeOrConsistencyType) {
        if (!checkTypeConsistency(volumeId, volumeOrConsistencyType)) {
            warn("Inconsistent types, %s and %s, return empty results", volumeId, volumeOrConsistencyType);
            return new ArrayList<AssetOption>();
        }
        final ViPRCoreClient client = api(ctx);
        if (isVolumeType(volumeOrConsistencyType)) {
            List<BlockSnapshotRestRep> snapshots = client.blockSnapshots().getByVolume(volumeId,
                    new DefaultResourceFilter<BlockSnapshotRestRep>() {

                        @Override
                        public boolean accept(BlockSnapshotRestRep snapshot) {
                            String replicaState = snapshot.getReplicaState();
                            return replicaState != null &&
                                    !(replicaState.equals(ReplicationState.DETACHED.name())) &&
                                    !(replicaState.equals(ReplicationState.INACTIVE.name()));
                        }
                    });

            return constructSnapshotOptions(snapshots);
        } else {
            return getConsistencyGroupSnapshots(ctx, volumeId);
        }
    }

    @Asset("consistencyGroupByProjectAndType")
    @AssetDependencies({ "project", "blockVolumeOrConsistencyType" })
    public List<AssetOption> getAllConsistencyGroups(final AssetOptionsContext ctx, URI projectId, String type) {

        if (isConsistencyGroupType(type)) {
            List<BlockConsistencyGroupRestRep> consistencyGroups = api(ctx).blockConsistencyGroups()
                    .search()
                    .byProject(projectId)
                    .run();
            return createBaseResourceOptions(consistencyGroups);
        } else {
            return Lists.newArrayList(newAssetOption(NONE_TYPE, "None"));
        }
    }

    @Asset("blockSnapshotType")
    @AssetDependencies({"blockVolumeOrConsistencyType", "snapshotBlockVolume"})
    public List<AssetOption> getBlockSnapshotType(AssetOptionsContext ctx, String storageType, URI blockVolumeOrCG) {
        // These are hard coded values for now. In the future, this may be available through an API
        List<AssetOption> options = Lists.newArrayList();
        if (isConsistencyGroupType(blockVolumeOrCG)) {                        
            options.add(CG_SNAPSHOT_TYPE_OPTION);
            // Only add cg session option if the CG selected supports it
            ViPRCoreClient client = api(ctx);
            BlockConsistencyGroupRestRep cg = client.blockConsistencyGroups().get(blockVolumeOrCG);
            if (isSnapshotSessionSupportedForCG(cg)) {
                options.add(CG_SNAPSHOT_SESSION_TYPE_OPTION);
            } 
        } else {
            debug("getting blockSnapshotTypes (blockVolume=%s)", blockVolumeOrCG);
            ViPRCoreClient client = api(ctx);
            VolumeRestRep volume = client.blockVolumes().get(blockVolumeOrCG);
            BlockVirtualPoolRestRep virtualPool = client.blockVpools().get(volume.getVirtualPool());
            if (isLocalSnapshotSupported(virtualPool)) {
                options.add(LOCAL_ARRAY_SNAPSHOT_TYPE_OPTION);
            }

            if (isRPSourceVolume(volume)) {
                options.add(RECOVERPOINT_BOOKMARK_SNAPSHOT_TYPE_OPTION);
            }
            
            if (isSnapshotSessionSupportedForVolume(volume)) {
                options.add(SNAPSHOT_SESSION_TYPE_OPTION);
            }
        }
        return options;
    }

    @Asset("blockSnapshotType")
    @AssetDependencies("blockVolumeOrConsistencyType")
    public List<AssetOption> getBlockSnapshotType(AssetOptionsContext ctx, String storageType) {
        // These are hard coded values for now. In the future, this may be available through an API
        List<AssetOption> options = Lists.newArrayList();

        if (isConsistencyGroupType(storageType)) {
            options.add(CG_SNAPSHOT_TYPE_OPTION);
            options.add(CG_SNAPSHOT_SESSION_TYPE_OPTION);
        } else {
            options.add(LOCAL_ARRAY_SNAPSHOT_TYPE_OPTION);
            options.add(RECOVERPOINT_BOOKMARK_SNAPSHOT_TYPE_OPTION);
            options.add(SNAPSHOT_SESSION_TYPE_OPTION);
        }
        return options;
    }

    @Asset("linkedSnapshotsForVolume")
    @AssetDependencies({ "snapshotSessionBlockVolume", "blockVolumeOrConsistencyType" })
    public List<AssetOption> getLinkedSnapshotsForSnapshotSessionVolume(AssetOptionsContext ctx, URI volumeOrCGId, String volumeOrConsistencyType) {
        if (!checkTypeConsistency(volumeOrCGId, volumeOrConsistencyType)) {
            warn("Inconsistent types, %s and %s, return empty results", volumeOrCGId, volumeOrConsistencyType);
            return new ArrayList<AssetOption>();
        }
        List<BlockSnapshotRestRep> snapshots = new ArrayList<BlockSnapshotRestRep>();
        List<BlockSnapshotSessionRestRep> snapshotSessions =  new ArrayList<BlockSnapshotSessionRestRep>();
        final ViPRCoreClient client = api(ctx);
        if (isVolumeType(volumeOrConsistencyType)) {
            snapshots = client.blockSnapshots().getByVolume(volumeOrCGId, new DefaultResourceFilter<BlockSnapshotRestRep>());
            snapshotSessions = client.blockSnapshotSessions().getByVolume(volumeOrCGId, new DefaultResourceFilter<BlockSnapshotSessionRestRep>());            
        } else {
            snapshots = client.blockSnapshots().getByConsistencyGroup(volumeOrCGId, new DefaultResourceFilter<BlockSnapshotRestRep>());
            snapshotSessions = client.blockSnapshotSessions().getByConsistencyGroup(volumeOrCGId, new DefaultResourceFilter<BlockSnapshotSessionRestRep>());            
        }
        return constructSnapshotWithSnapshotSessionOptions(snapshots, snapshotSessions);
    }

    @Asset("linkedSnapshotCopyMode")
    public List<AssetOption> getLinkedSnapshotCopyMode(AssetOptionsContext ctx) {
        // These are hard coded values for now. In the future, this may be available through an API
        List<AssetOption> options = Lists.newArrayList();
        options.add(LINKED_SNAPSHOT_COPYMODE_OPTION);
        options.add(LINKED_SNAPSHOT_NOCOPYMODE_OPTION);
        return options;
    }

    private List<AssetOption> getBlockVolumesForHost(ViPRCoreClient client, URI tenant, URI host, boolean mounted) {
        return createVolumeOptions(client, null, host, BlockProviderUtils.getBlockVolumes(client, tenant, host, mounted));
    }

    private List<AssetOption> getBlockVolumesForHostDatastore(ViPRCoreClient client, URI tenant, URI host, String datastore) {
        return createVolumeOptions(client, null, host, BlockProviderUtils.getBlockVolumesForDatastore(client, tenant, host, datastore));
    }

    private List<AssetOption> getProjectBlockVolumesForHost(ViPRCoreClient client, URI project, URI host,
            boolean mounted) {
        return createVolumeOptions(client, project, host, getProjectBlockVolumes(client, host, project, mounted));
    }

    private List<AssetOption> getBlockResourcesForHost(ViPRCoreClient client, URI tenant, URI host, boolean mounted) {
        return getBlockResourcesForHost(client, tenant, host, mounted, null);
    }

    private List<AssetOption> getProjectBlockResourcesForHost(ViPRCoreClient client, URI project, URI host,
            boolean mounted) {
        return getProjectBlockResourcesForHost(client, project, host, mounted, null);
    }

    @SuppressWarnings("unchecked")
    private List<AssetOption> getBlockResourcesForHost(ViPRCoreClient client, URI tenant, URI host, boolean mounted,
            ResourceFilter<BlockObjectRestRep> filter) {
        List<? extends BlockObjectRestRep> resources = BlockProviderUtils.getBlockResources(client, tenant, host, mounted);
        ResourceUtils.applyFilter((List<BlockObjectRestRep>) resources, filter);
        return createVolumeOptions(client, null, host, resources);
    }

    @SuppressWarnings("unchecked")
    private List<AssetOption> getProjectBlockResourcesForHost(ViPRCoreClient client, URI project, URI host,
            boolean mounted, ResourceFilter<BlockObjectRestRep> filter) {
        List<? extends BlockObjectRestRep> resources = getProjectBlockResources(client, host, project, mounted);
        ResourceUtils.applyFilter((List<BlockObjectRestRep>) resources, filter);
        return createVolumeOptions(client, project, host, resources);
    }

    @Asset("mountedBlockVolume")
    @AssetDependencies("host")
    public List<AssetOption> getMountedBlockVolumesForHost(AssetOptionsContext context, URI host) {
        return getBlockVolumesForHost(api(context), context.getTenant(), host, true);
    }

    @Asset("mountedBlockVolume")
    @AssetDependencies({ "host", "project" })
    public List<AssetOption> getMountedBlockVolumesForHost(AssetOptionsContext context, URI host, URI project) {
        return getProjectBlockVolumesForHost(api(context), project, host, true);
    }

    @Asset("mountedBlockVolume")
    @AssetDependencies("linuxHost")
    public List<AssetOption> getMountedBlockVolumesForLinuxHost(AssetOptionsContext context, URI host) {
        return getBlockVolumesForHost(api(context), context.getTenant(), host, true);
    }

    @Asset("mountedBlockVolume")
    @AssetDependencies({ "linuxHost", "project" })
    public List<AssetOption> getMountedBlockVolumesForLinuxHost(AssetOptionsContext context, URI host, URI project) {
        return getProjectBlockVolumesForHost(api(context), project, host, true);
    }

    @Asset("mountedBlockVolume")
    @AssetDependencies("hpuxHost")
    public List<AssetOption> getMountedBlockVolumesForHpuxHost(AssetOptionsContext context, URI host) {
        return getBlockVolumesForHost(api(context), context.getTenant(), host, true);
    }

    @Asset("mountedBlockVolume")
    @AssetDependencies({ "hpuxHost", "project" })
    public List<AssetOption> getMountedBlockVolumesForHpuxHost(AssetOptionsContext context, URI host, URI project) {
        return getProjectBlockVolumesForHost(api(context), project, host, true);
    }

    @Asset("mountedBlockVolume")
    @AssetDependencies("windowsHost")
    public List<AssetOption> getMountedBlockVolumesForWindowsHost(AssetOptionsContext context, URI host) {
        return getBlockVolumesForHost(api(context), context.getTenant(), host, true);
    }

    @Asset("mountedBlockVolume")
    @AssetDependencies({ "windowsHost", "project" })
    public List<AssetOption> getMountedBlockVolumesForWindowsHost(AssetOptionsContext context, URI host, URI project) {
        return getProjectBlockVolumesForHost(api(context), project, host, true);
    }

    @Asset("fileSystemType")
    public List<AssetOption> getWindowsFilesystemTypes(AssetOptionsContext context) {
        return WINDOWS_FILESYSTEM_TYPES;
    }

    @Asset("blockSize")
    public List<AssetOption> getAllWindowsBlockSize(AssetOptionsContext context) {
        return ALL_BLOCKSIZE_OPTIONS;
    }

    @Asset("blockSize")
    @AssetDependencies({ "fileSystemType" })
    public List<AssetOption> getWindowsBlockSize(AssetOptionsContext context, String fileSystemType) {

        if ("ntfs".equals(fileSystemType)) {
            return NTFS_OPTIONS;
        } else {
            return FAT32_OPTIONS;
        }
    }

    @Asset("mountedBlockVolume")
    @AssetDependencies("esxHost")
    public List<AssetOption> getMountedBlockVolumesForEsxHost(AssetOptionsContext context, URI host) {
        return getBlockVolumesForHost(api(context), context.getTenant(), host, true);
    }

    @Asset("mountedBlockVolumeDatastore")
    @AssetDependencies({ "esxHost", "blockdatastore" })
    public List<AssetOption> getMountedBlockVolumesForEsxHostDatastore(AssetOptionsContext context, URI host, String datastore) {
        return getBlockVolumesForHostDatastore(api(context), context.getTenant(), host, datastore);
    }

    @Asset("mountedBlockVolume")
    @AssetDependencies({ "esxHost", "project" })
    public List<AssetOption> getMountedBlockVolumesForEsxHost(AssetOptionsContext context, URI host, URI project) {
        return getProjectBlockVolumesForHost(api(context), project, host, true);
    }

    @Asset("unmountedBlockVolume")
    @AssetDependencies("host")
    public List<AssetOption> getUnmountedBlockVolumesForHost(AssetOptionsContext context, URI host) {
        return getBlockVolumesForHost(api(context), context.getTenant(), host, false);
    }

    @Asset("unmountedBlockVolume")
    @AssetDependencies({ "host", "project" })
    public List<AssetOption> getUnmountedBlockVolumesForHost(AssetOptionsContext context, URI host, URI project) {
        return getProjectBlockVolumesForHost(api(context), project, host, false);
    }

    @Asset("unmountedBlockVolume")
    @AssetDependencies("linuxHost")
    public List<AssetOption> getUnmountedBlockVolumesForLinuxHost(AssetOptionsContext context, URI host) {
        return getBlockVolumesForHost(api(context), context.getTenant(), host, false);
    }

    @Asset("unmountedBlockVolume")
    @AssetDependencies({ "linuxHost", "project" })
    public List<AssetOption> getUnmountedBlockVolumesForLinuxHost(AssetOptionsContext context, URI host, URI project) {
        return getProjectBlockVolumesForHost(api(context), project, host, false);
    }

    @Asset("unmountedBlockVolume")
    @AssetDependencies("windowsHost")
    public List<AssetOption> getUnmountedBlockVolumesForWindowsHost(AssetOptionsContext context, URI host) {
        return getBlockVolumesForHost(api(context), context.getTenant(), host, false);
    }

    @Asset("unmountedBlockVolume")
    @AssetDependencies({ "windowsHost", "project" })
    public List<AssetOption> getUnmountedBlockVolumesForWindowsHost(AssetOptionsContext context, URI host, URI project) {
        return getProjectBlockVolumesForHost(api(context), project, host, false);
    }

    @Asset("unmountedBlockVolume")
    @AssetDependencies("esxHost")
    public List<AssetOption> getUnmountedBlockVolumesForEsxHost(AssetOptionsContext context, URI host) {
        return getBlockVolumesForHost(api(context), context.getTenant(), host, false);
    }

    @Asset("unmountedBlockVolume")
    @AssetDependencies({ "esxHost", "project" })
    public List<AssetOption> getUnmountedBlockVolumesForEsxHost(AssetOptionsContext context, URI host, URI project) {
        return getProjectBlockVolumesForHost(api(context), project, host, false);
    }

    @Asset("mountedBlockResource")
    @AssetDependencies("host")
    public List<AssetOption> getMountedBlockResources(AssetOptionsContext context, URI host) {
        return getBlockResourcesForHost(api(context), context.getTenant(), host, true);
    }

    @Asset("mountedBlockResourceNoTargets")
    @AssetDependencies("host")
    public List<AssetOption> getMountedBlockResourcesNoTargets(AssetOptionsContext context, URI host) {
        return getBlockResourcesForHost(api(context), context.getTenant(), host, true, new BlockObjectSRDFTargetFilter().not());
    }

    @Asset("unmountedBlockResourceNoTargets")
    @AssetDependencies({ "host" })
    public List<AssetOption> getUnmountedBlockResourcesNoTargets(AssetOptionsContext context, URI host) {
        return getBlockResourcesForHost(api(context), context.getTenant(), host, false, new BlockObjectSRDFTargetFilter().not());
    }

    @Asset("unmountedBlockResource")
    @AssetDependencies({ "host" })
    public List<AssetOption> getUnmountedBlockResources(AssetOptionsContext context, URI host) {
        return getBlockResourcesForHost(api(context), context.getTenant(), host, false);
    }

    @Asset("unmountedBlockResource")
    @AssetDependencies({ "host", "project" })
    public List<AssetOption> getUnmountedBlockResources(AssetOptionsContext context, URI host, URI project) {
        return getProjectBlockResourcesForHost(api(context), project, host, false);
    }

    @Asset("mountedBlockResource")
    @AssetDependencies("linuxHost")
    public List<AssetOption> getLinuxMountedBlockResources(AssetOptionsContext context, URI linuxHost) {
        return getBlockResourcesForHost(api(context), context.getTenant(), linuxHost, true);
    }

    @Asset("mountedBlockResourceNoTargets")
    @AssetDependencies("linuxHost")
    public List<AssetOption> getLinuxMountedBlockResourcesNoTargets(AssetOptionsContext context, URI linuxHost) {
        return getBlockResourcesForHost(api(context), context.getTenant(), linuxHost, true, new BlockObjectSRDFTargetFilter().not());
    }

    @Asset("unmountedBlockResource")
    @AssetDependencies({ "linuxHost" })
    public List<AssetOption> getLinuxUnmountedBlockResources(AssetOptionsContext context, URI linuxHost) {
        return getBlockResourcesForHost(api(context), context.getTenant(), linuxHost, false);
    }

    @Asset("unmountedBlockResource")
    @AssetDependencies({ "linuxHost", "project" })
    public List<AssetOption> getLinuxUnmountedBlockResources(AssetOptionsContext context, URI linuxHost, URI project) {
        return getProjectBlockResourcesForHost(api(context), project, linuxHost, false);
    }

    @Asset("mountedBlockResourcePath")
    @AssetDependencies("linuxHost")
    public List<AssetOption> getLinuxMountedBlockResourcePaths(AssetOptionsContext context, URI linuxHost) {
        List<? extends BlockObjectRestRep> volumes = BlockProviderUtils.getBlockResources(api(context), context.getTenant(), linuxHost,
                true);
        return createStringOptions(getTagValues(KnownMachineTags.getHostMountPointTagName(linuxHost), volumes));
    }

    @Asset("unmountedBlockResource")
    @AssetDependencies({ "aixHost" })
    public List<AssetOption> getAixUnmountedBlockResources(AssetOptionsContext context, URI aixHost) {
        return getBlockResourcesForHost(api(context), context.getTenant(), aixHost, false);
    }

    @Asset("unmountedBlockResource")
    @AssetDependencies({ "aixHost", "project" })
    public List<AssetOption> getAixUnmountedBlockResources(AssetOptionsContext context, URI aixHost, URI project) {
        return getProjectBlockResourcesForHost(api(context), project, aixHost, false);
    }

    @Asset("mountedBlockResource")
    @AssetDependencies("aixHost")
    public List<AssetOption> getAixMountedBlockResources(AssetOptionsContext context, URI aixHost) {
        return getBlockResourcesForHost(api(context), context.getTenant(), aixHost, true);
    }

    @Asset("mountedBlockResourceNoTargets")
    @AssetDependencies("aixHost")
    public List<AssetOption> getAixMountedBlockResourcesNoTargets(AssetOptionsContext context, URI aixHost) {
        return getBlockResourcesForHost(api(context), context.getTenant(), aixHost, true, new BlockObjectSRDFTargetFilter().not());
    }

    @Asset("unmountedBlockResource")
    @AssetDependencies({ "hpuxHost" })
    public List<AssetOption> getHpuxUnmountedBlockResources(AssetOptionsContext context, URI hpuxHost) {
        return getBlockResourcesForHost(api(context), context.getTenant(), hpuxHost, false);
    }

    @Asset("unmountedBlockResource")
    @AssetDependencies({ "hpuxHost", "project" })
    public List<AssetOption> getHpuxUnmountedBlockResources(AssetOptionsContext context, URI hpuxHost, URI project) {
        return getProjectBlockResourcesForHost(api(context), project, hpuxHost, false);
    }

    @Asset("mountedBlockResource")
    @AssetDependencies("hpuxHost")
    public List<AssetOption> getHpuxMountedBlockResources(AssetOptionsContext context, URI hpuxHost) {
        return getBlockResourcesForHost(api(context), context.getTenant(), hpuxHost, true);
    }

    @Asset("mountedBlockResourceNoTargets")
    @AssetDependencies("hpuxHost")
    public List<AssetOption> getHpuxMountedBlockResourcesNoTargets(AssetOptionsContext context, URI hpuxHost) {
        return getBlockResourcesForHost(api(context), context.getTenant(), hpuxHost, true, new BlockObjectSRDFTargetFilter().not());
    }

    @Asset("mountedBlockResource")
    @AssetDependencies("windowsHost")
    public List<AssetOption> getWindowsMountedBlockResources(AssetOptionsContext context, URI windowsHost) {
        return getBlockResourcesForHost(api(context), context.getTenant(), windowsHost, true);
    }

    @Asset("mountedBlockResourceNoTargets")
    @AssetDependencies("windowsHost")
    public List<AssetOption> getWindowsMountedBlockResourcesNoTargets(AssetOptionsContext context, URI windowsHost) {
        return getBlockResourcesForHost(api(context), context.getTenant(), windowsHost, true, new BlockObjectSRDFTargetFilter().not());
    }

    @Asset("unmountedBlockResource")
    @AssetDependencies({ "windowsHost" })
    public List<AssetOption> getWindowsUnmountedBlockResources(AssetOptionsContext context, URI windowsHost) {
        return getBlockResourcesForHost(api(context), context.getTenant(), windowsHost, false);
    }

    @Asset("unmountedBlockResource")
    @AssetDependencies({ "windowsHost", "project" })
    public List<AssetOption> getWindowsUnmountedBlockResources(AssetOptionsContext context, URI windowsHost, URI project) {
        return getProjectBlockResourcesForHost(api(context), project, windowsHost, false);
    }

    @Asset("mountedBlockResource")
    @AssetDependencies({ "esxHost" })
    public List<AssetOption> getESXMountedBlockResources(AssetOptionsContext context, URI esxHost) {
        return getBlockResourcesForHost(api(context), context.getTenant(), esxHost, true);
    }

    @Asset("unmountedBlockResource")
    @AssetDependencies({ "esxHost" })
    public List<AssetOption> getESXUnmountedBlockResources(AssetOptionsContext context, URI esxHost) {
        return getBlockResourcesForHost(api(context), context.getTenant(), esxHost, false);
    }

    @Asset("unmountedBlockResource")
    @AssetDependencies({ "esxHost", "project" })
    public List<AssetOption> getESXUnmountedBlockResources(AssetOptionsContext context, URI esxHost, URI project) {
        return getProjectBlockResourcesForHost(api(context), project, esxHost, false);
    }

    @Asset("snapshotBlockVolume")
    @AssetDependencies({ "project", "blockVolumeOrConsistencyType" })
    public List<AssetOption> getSnapshotBlockVolumes(AssetOptionsContext context, URI project, String storageType) {
        final ViPRCoreClient client = api(context);
        if (isVolumeType(storageType)) {
            List<VolumeRestRep> volumes = listVolumesNonBulk(client, project);
            List<VolumeDetail> volumeDetails = getVolumeDetails(client, volumes);
            Map<URI, VolumeRestRep> volumeNames = ResourceUtils.mapById(volumes);

            List<AssetOption> options = Lists.newArrayList();
            for (VolumeDetail detail : volumeDetails) {

                boolean localSnapSupported = isLocalSnapshotSupported(detail.vpool);
                boolean isRPTargetVolume = isRPTargetVolume(detail.volume);
                boolean isRPSourceVolume = isRPSourceVolume(detail.volume);
                boolean isInConsistencyGroup = BlockProvider.isInConsistencyGroup(detail.volume);
                boolean isXio3XVolume = hasXIO3XVolumes(detail.volume);

                debug("filter[ localSnapSupported=%s, isRPTargetVolume=%s, isRPSourceVolume=%s, isInConsistencyGroup=%s, isXio3XVolume=%s ]",
                        localSnapSupported, isRPTargetVolume, isRPSourceVolume, isInConsistencyGroup, isXio3XVolume);

                if (isRPSourceVolume || (localSnapSupported && (!isInConsistencyGroup || isRPTargetVolume || isXio3XVolume))) {
                    options.add(createVolumeOption(client, null, detail.volume, volumeNames));
                }
            }
            return options;
        } else {
            List<BlockConsistencyGroupRestRep> consistencyGroups = client.blockConsistencyGroups().search().byProject(project).run();
            return createBaseResourceOptions(consistencyGroups);
        }
    }

    @Asset("snapshotSessionBlockVolume")
    @AssetDependencies({ "project", "blockVolumeOrConsistencyType" })
    public List<AssetOption> getSnapshotSessionBlockVolumes(AssetOptionsContext context, URI project, String storageType) {
        final ViPRCoreClient client = api(context);
        if (isVolumeType(storageType)) {
            List<VolumeRestRep> volumes = listVolumesNonBulk(client, project);
            List<VolumeDetail> volumeDetails = getVolumeDetails(client, volumes);
            Map<URI, VolumeRestRep> volumeNames = ResourceUtils.mapById(volumes);

            List<AssetOption> options = Lists.newArrayList();
            for (VolumeDetail detail : volumeDetails) {
                boolean localSnapSupported = isLocalSnapshotSupported(detail.vpool);
                boolean isRPTargetVolume = isRPTargetVolume(detail.volume);
                boolean isRPSourceVolume = isRPSourceVolume(detail.volume);
                boolean isInConsistencyGroup = BlockProvider.isInConsistencyGroup(detail.volume);
                boolean isSnapshotSessionSupported = isSnapshotSessionSupportedForVolume(detail.volume);

                debug("filter[ localSnapSupported=%s, isRPTargetVolume=%s, isRPSourceVolume=%s, isInConsistencyGroup=%s, isXio3XVolume=%s ]",
                        localSnapSupported, isRPTargetVolume, isRPSourceVolume, isInConsistencyGroup, isSnapshotSessionSupported);

                if (isSnapshotSessionSupported && localSnapSupported && !isInConsistencyGroup) {
                    options.add(createVolumeOption(client, null, detail.volume, volumeNames));
                }
            }
            return options;
        } else {
            List<BlockConsistencyGroupRestRep> consistencyGroups = client.blockConsistencyGroups().search().byProject(project).run();
            return createBaseResourceOptions(consistencyGroups);
        }
    }

    @Asset("snapshotSessionsByVolume")
    @AssetDependencies({ "project", "blockVolumeOrConsistencyType", "snapshotSessionBlockVolume" })
    public List<AssetOption> getSnapshotSessionsByVolume(AssetOptionsContext ctx, URI projectId, String type, URI volumeOrCGId) {
        if (isVolumeType(type) && BlockProviderUtils.isType(volumeOrCGId, VOLUME_TYPE)) {
            List<BlockSnapshotSessionRestRep> snapshotSessions = api(ctx).blockSnapshotSessions().getByVolume(volumeOrCGId);
            return constructSnapshotSessionOptions(api(ctx), projectId, snapshotSessions);
        } else if (isConsistencyGroupType(type) && BlockProviderUtils.isType(volumeOrCGId, BLOCK_CONSISTENCY_GROUP_TYPE)) {            
            return getConsistencyGroupSnapshotSessions(ctx, volumeOrCGId);
        } else {
            return new ArrayList<AssetOption>();
        }
    }

    @Asset("blockVolumeWithSnapshot")
    @AssetDependencies({ "project", "blockVolumeOrConsistencyType" })
    public List<AssetOption> getBlockVolumesWithSnapshot(AssetOptionsContext context, URI project, String volumeOrConsistencyType) {
        final ViPRCoreClient client = api(context);
        if (isVolumeType(volumeOrConsistencyType)) {
            List<VolumeRestRep> volumes = client.blockVolumes().findByProject(project, new DefaultResourceFilter<VolumeRestRep>() {
                @Override
                public boolean accept(VolumeRestRep volume) {
                    if (!(client.blockSnapshots().getByVolume(volume.getId())).isEmpty() && !isInConsistencyGroup(volume)) {
                        return true;
                    } else {
                        return false;
                    }
                }
            });
            return createVolumeOptions(client, volumes);
        } else {
            List<BlockConsistencyGroupRestRep> consistencyGroups = client.blockConsistencyGroups()
                    .search()
                    .byProject(project)
                    .run();
            return createBaseResourceOptions(consistencyGroups);
        }
    }

    @Asset("localSnapshotBlockVolume")
    @AssetDependencies({ "project" })
    public List<AssetOption> getLocalSnapshotBlockVolumes(AssetOptionsContext context, URI project) {
        final ViPRCoreClient client = api(context);
        List<VolumeRestRep> volumes = listVolumes(client, project);
        List<VolumeDetail> volumeDetails = getVolumeDetails(client, volumes);
        Map<URI, VolumeRestRep> volumeNames = ResourceUtils.mapById(volumes);

        List<AssetOption> options = Lists.newArrayList();
        for (VolumeDetail detail : volumeDetails) {
            if (isLocalSnapshotSupported(detail.vpool)) {
                options.add(createVolumeOption(client, null, detail.volume, volumeNames));
            }
        }
        return options;
    }

    @Asset("remoteSnapshotBlockVolume")
    @AssetDependencies({ "project" })
    public List<AssetOption> getRemoteSnapshotBlockVolumes(AssetOptionsContext context, URI project) {
        final ViPRCoreClient client = api(context);
        List<VolumeRestRep> volumes = listVolumes(client, project);
        List<VolumeDetail> volumeDetails = getVolumeDetails(client, volumes);
        Map<URI, VolumeRestRep> volumeNames = ResourceUtils.mapById(volumes);

        List<AssetOption> options = Lists.newArrayList();
        for (VolumeDetail detail : volumeDetails) {
            if (isRemoteSnapshotSupported(detail.volume)) {
                options.add(createVolumeOption(client, null, detail.volume, volumeNames));
            }
        }
        return options;
    }

    @Asset("localMirrorBlockVolume")
    @AssetDependencies({ "project" })
    public List<AssetOption> getLocalMirrorBlockVolumes(AssetOptionsContext context, URI project) {
        final ViPRCoreClient client = api(context);
        List<VolumeRestRep> volumes = listVolumes(client, project);
        List<VolumeDetail> volumeDetails = getVolumeDetails(client, volumes);
        Map<URI, VolumeRestRep> volumeNames = getProjectVolumeNames(client, project);

        List<AssetOption> options = Lists.newArrayList();
        for (VolumeDetail detail : volumeDetails) {
            if (isLocalMirrorSupported(detail.vpool)) {
                options.add(createVolumeOption(client, null, detail.volume, volumeNames));
            }
        }
        return options;
    }

    @Asset("volumeWithContinuousCopies")
    @AssetDependencies("project")
    public List<AssetOption> getVolumesWithContinuousCopies(AssetOptionsContext ctx, URI project) {
        final ViPRCoreClient client = api(ctx);
        List<VolumeRestRep> volumes = client.blockVolumes().findByProject(project, new SourceTargetVolumesFilter() {
            @Override
            public boolean acceptId(URI id) {
                return !client.blockVolumes().getContinuousCopies(id).isEmpty();
            }
        });
        return createVolumeOptions(client, volumes);
    }

    @Asset("continuousCopies")
    @AssetDependencies("volumeWithContinuousCopies")
    public List<AssetOption> getContinuousCopies(AssetOptionsContext ctx, URI volume) {
        return createBaseResourceOptions(api(ctx).blockVolumes().getContinuousCopies(volume));
    }

    @Asset("blockJournalSize")
    @AssetDependencies("rpConsistencyGroupByProject")
    public List<AssetOption> getBlockJournalSize(AssetOptionsContext ctx, URI consistencyGroup) {

        String minimumSize = null;

        BlockConsistencyGroupRestRep cg = api(ctx).blockConsistencyGroups().get(consistencyGroup);
        for (RelatedResourceRep vol : cg.getVolumes()) {
            VolumeRestRep volume = api(ctx).blockVolumes().get(vol);
            if (volume.getProtection() != null && volume.getProtection().getRpRep() != null
                    && volume.getProtection().getRpRep().getProtectionSet() != null) {
                RelatedResourceRep protectionSetId = volume.getProtection().getRpRep().getProtectionSet();
                ProtectionSetRestRep protectionSet = api(ctx).blockVolumes().getProtectionSet(volume.getId(), protectionSetId.getId());
                for (RelatedResourceRep protectionVolume : protectionSet.getVolumes()) {
                    VolumeRestRep vol1 = api(ctx).blockVolumes().get(protectionVolume);
                    if (vol1.getProtection().getRpRep().getPersonality().equalsIgnoreCase("METADATA")) {
                        String capacity = api(ctx).blockVolumes().get(protectionVolume).getCapacity();
                        if (minimumSize == null || Float.parseFloat(capacity) < Float.parseFloat(minimumSize)) {
                            minimumSize = capacity;
                        }
                    }
                }
            }
        }

        if (minimumSize == null) {
            return Lists.newArrayList();
        } else {
            return Lists.newArrayList(newAssetOption(minimumSize, minimumSize));
        }
    }

    @Asset("volumeWithoutConsistencyGroup")
    @AssetDependencies("project")
    public List<AssetOption> getVolumesWithoutConsistencyGroup(AssetOptionsContext ctx, URI project) {
        debug("getting volumes that don't belong to a consistency group (project=%s)", project);
        ViPRCoreClient client = api(ctx);
        List<VolumeRestRep> volumes = client.blockVolumes().findByProject(project);
        Map<URI, VolumeRestRep> volumeNames = getProjectVolumeNames(client, project);
        List<AssetOption> options = Lists.newArrayList();
        for (VolumeRestRep volume : volumes) {
            if (volume.getConsistencyGroup() == null) {
                options.add(createVolumeOption(client, null, volume, volumeNames));
            }
        }
        return options;
    }

    @Asset("volumeWithFullCopies")
    @AssetDependencies({ "project", "blockVolumeOrConsistencyType" })
    public List<AssetOption> getVolumesWithFullCopies(AssetOptionsContext ctx, URI project, String volumeOrConsistencyType) {
        final ViPRCoreClient client = api(ctx);
        if (isVolumeType(volumeOrConsistencyType)) {
            List<VolumeRestRep> volumes = client.blockVolumes().findByProject(project, new DefaultResourceFilter<VolumeRestRep>() {
                @Override
                public boolean accept(VolumeRestRep volume) {
                    if (!client.blockVolumes().getFullCopies(volume.getId()).isEmpty() && !isInConsistencyGroup(volume)) {
                        return true;
                    } else {
                        return false;
                    }
                }
            });
            return createVolumeOptions(client, volumes);
        } else {
            List<BlockConsistencyGroupRestRep> consistencyGroups = api(ctx).blockConsistencyGroups()
                    .search()
                    .byProject(project)
                    .run();
            return createBaseResourceOptions(consistencyGroups);
        }
    }

    @Asset("fullCopy")
    @AssetDependencies({ "volumeWithFullCopies", "blockVolumeOrConsistencyType" })
    public List<AssetOption> getFullCopies(AssetOptionsContext ctx, URI volumeId, String volumeOrConsistencyType) {

        final ViPRCoreClient client = api(ctx);
        if (isVolumeType(volumeOrConsistencyType)) {
            if (!BlockProviderUtils.isType(volumeId, VOLUME_TYPE)) {
                warn("Inconsistent types, %s and %s, return empty results", volumeId, volumeOrConsistencyType);
                return new ArrayList<AssetOption>();
            }

            return createBaseResourceOptions(client.blockVolumes().getFullCopies(volumeId));
        } else {
            if (!BlockProviderUtils.isType(volumeId, BLOCK_CONSISTENCY_GROUP_TYPE)) {
                warn("Inconsistent types, %s and %s, return empty results", volumeId, volumeOrConsistencyType);
                return new ArrayList<AssetOption>();
            }
            return getConsistencyGroupFullCopies(ctx, volumeId);
        }
    }

    @Asset("fullCopyAvailableForDetach")
    @AssetDependencies({ "volumeWithFullCopies", "blockVolumeOrConsistencyType" })
    public List<AssetOption> getFullCopiedDetach(AssetOptionsContext ctx, URI volumeId, String volumeOrConsistencyType) {
        if (!checkTypeConsistency(volumeId, volumeOrConsistencyType)) {
            warn("Inconsistent types, %s and %s, return empty results", volumeId, volumeOrConsistencyType);
            return new ArrayList<AssetOption>();
        }
        final ViPRCoreClient client = api(ctx);
        if (isVolumeType(volumeOrConsistencyType)) {
            List<VolumeRestRep> volumes = client.blockVolumes().getFullCopies(volumeId, new DefaultResourceFilter<VolumeRestRep>() {
                @Override
                public boolean accept(VolumeRestRep volume) {
                    String replicaState = volume.getProtection().getFullCopyRep().getReplicaState();
                    return replicaState != null &&
                            !(replicaState.equals(ReplicationState.DETACHED.name()));
                }
            });
            return createVolumeOptions(client, volumes);
        } else {
            return getConsistencyGroupFullCopies(ctx, volumeId);
        }
    }

    private List<AssetOption> getConsistencyGroupFullCopies(AssetOptionsContext ctx, URI consistencyGroupId) {
        return createNamedResourceOptions(api(ctx).blockConsistencyGroups().getFullCopies(consistencyGroupId));
    }

    private List<AssetOption> getConsistencyGroupSnapshots(AssetOptionsContext ctx, URI consistencyGroupId) {
        return createNamedResourceOptions(api(ctx).blockConsistencyGroups().getSnapshots(consistencyGroupId));
    }
    
    private List<AssetOption> getConsistencyGroupSnapshotSessions(AssetOptionsContext ctx, URI consistencyGroupId) {
        return createNamedResourceOptions(api(ctx).blockConsistencyGroups().getSnapshotSessions(consistencyGroupId));
    }

    @Asset("fullCopyAvailableForResynchronize")
    @AssetDependencies({ "volumeWithFullCopies", "blockVolumeOrConsistencyType" })
    public List<AssetOption> getFullCopiedSynchronize(AssetOptionsContext ctx, URI volumeId, String volumeOrConsistencyType) {
        if (!checkTypeConsistency(volumeId, volumeOrConsistencyType)) {
            warn("Inconsistent types, %s and %s, return empty results", volumeId, volumeOrConsistencyType);
            return new ArrayList<AssetOption>();
        }
        final ViPRCoreClient client = api(ctx);
        if (isVolumeType(volumeOrConsistencyType)) {
            List<VolumeRestRep> volumes = client.blockVolumes().getFullCopies(volumeId, new DefaultResourceFilter<VolumeRestRep>() {
                @Override
                public boolean accept(VolumeRestRep volume) {
                    String replicaState = volume.getProtection().getFullCopyRep().getReplicaState();
                    return replicaState != null &&
                            !(replicaState.equals(ReplicationState.DETACHED.name())) &&
                            !(replicaState.equals(ReplicationState.INACTIVE.name()));
                }
            });
            return createVolumeOptions(client, volumes);
        } else {
            return getConsistencyGroupFullCopies(ctx, volumeId);
        }
    }

    @Asset("fullCopyAvailableForRestore")
    @AssetDependencies({ "volumeWithFullCopies", "blockVolumeOrConsistencyType" })
    public List<AssetOption> getFullCopiedRestore(AssetOptionsContext ctx, URI volumeId, String volumeOrConsistencyType) {
        if (!checkTypeConsistency(volumeId, volumeOrConsistencyType)) {
            warn("Inconsistent types, %s and %s, return empty results", volumeId, volumeOrConsistencyType);
            return new ArrayList<AssetOption>();
        }

        final ViPRCoreClient client = api(ctx);
        if (isVolumeType(volumeOrConsistencyType)) {
            List<VolumeRestRep> volumes = client.blockVolumes().getFullCopies(volumeId, new DefaultResourceFilter<VolumeRestRep>() {
                @Override
                public boolean accept(VolumeRestRep volume) {
                    String replicaState = volume.getProtection().getFullCopyRep().getReplicaState();
                    return replicaState != null &&
                            !(replicaState.equals(ReplicationState.RESTORED.name())) &&
                            !(replicaState.equals(ReplicationState.INACTIVE.name())) &&
                            !(replicaState.equals(ReplicationState.DETACHED.name()));
                }
            });
            return createVolumeOptions(client, volumes);
        } else {
            return getConsistencyGroupFullCopies(ctx, volumeId);
        }
    }

    @Asset("application")
    public List<AssetOption> getApplications(AssetOptionsContext ctx) {
        final ViPRCoreClient client = api(ctx);
        VolumeGroupList applications = client.application().getApplications();
        return createNamedResourceOptions(applications.getVolumeGroups());
    }

    private boolean isRPTargetReplicationGroup(String rg) {
        if (rg != null) {
            String[] parts = StringUtils.split(rg, '-');
            if (parts.length > 1 && parts[1].equals("RPTARGET")) {
                return true;
            }
        }
        return false;
    }

    @Asset("applicationBlockVolume")
    @AssetDependencies("application")
    public List<AssetOption> getApplications(AssetOptionsContext ctx, URI application) {
        final ViPRCoreClient client = api(ctx);
        List<NamedRelatedResourceRep> volList = client.application().listVolumes(application);
        List<AssetOption> options = new ArrayList<AssetOption>();
        if (volList != null && !volList.isEmpty()) {
            List<VolumeRestRep> allVols = client.blockVolumes().getByRefs(volList);
            Map<String, List<VolumeRestRep>> repGrpVolMap = new HashMap<String, List<VolumeRestRep>>();
            for (VolumeRestRep vol : allVols) {
                if (!isRPTargetReplicationGroup(vol.getReplicationGroupInstance())) {
                    if (repGrpVolMap.get(vol.getReplicationGroupInstance()) == null) {
                        repGrpVolMap.put(vol.getReplicationGroupInstance(), new ArrayList<VolumeRestRep>());
                    }
                    repGrpVolMap.get(vol.getReplicationGroupInstance()).add(vol);
                }
            }
            for (Entry<String, List<VolumeRestRep>> entry : repGrpVolMap.entrySet()) {
                for (VolumeRestRep vol : entry.getValue()) {
                    options.add(new AssetOption(vol.getId(),
                            getMessage("application.volumes.replication_group", vol.getName(), vol.getReplicationGroupInstance())));
                }
            }
        }
        return options;
    }

    @Asset("replicationGroup")
    @AssetDependencies("application")
    public List<AssetOption> getApplicationReplicationGroups(AssetOptionsContext ctx, URI applicationId) {
        final ViPRCoreClient client = api(ctx);
        VolumeGroupRestRep application = client.application().getApplication(applicationId);
        return createStringOptions(application.getReplicationGroupNames());
    }

    @Asset("fullCopyName")
    @AssetDependencies("application")
    public List<AssetOption> getApplicationFullCopyNames(AssetOptionsContext ctx, URI applicationId) {
        final ViPRCoreClient client = api(ctx);
        NamedVolumesList volList = client.application().getFullCopiesByApplication(applicationId);
        Set<String> fullCopyNames = new HashSet<String>();
        if (volList != null && volList.getVolumes() != null && !volList.getVolumes().isEmpty()) {
            List<URI> ids = new ArrayList<URI>();
            for (NamedRelatedResourceRep volId : volList.getVolumes()) {
                ids.add(volId.getId());
            }
            List<VolumeRestRep> fullCopies = client.blockVolumes().getByIds(ids);
            for (VolumeRestRep vol : fullCopies) {
                if (vol != null && vol.getProtection() != null && vol.getProtection().getFullCopyRep() != null &&
                        vol.getProtection().getFullCopyRep().getFullCopySetName() != null) {
                    fullCopyNames.add(vol.getProtection().getFullCopyRep().getFullCopySetName());
                }
            }
        }
        return createStringOptions(fullCopyNames);
    }

    @Asset("applicationVirtualArray")
    @AssetDependencies("application")
    public List<AssetOption> getApplicationVirtualArrays(AssetOptionsContext ctx, URI applicationId) {
        final ViPRCoreClient client = api(ctx);
        List<NamedRelatedResourceRep> volList = client.application().listVolumes(applicationId);
        List<AssetOption> options = new ArrayList<AssetOption>();
        boolean isVplex = false;
        boolean isRP = false;
        boolean isDistributed = false;
        URI sourceVarrayId = null;
        URI haVarrayId = null;
        List<VolumeRestRep> allVols = null;

        if (volList != null && !volList.isEmpty()) {
            allVols = client.blockVolumes().getByRefs(volList);
            VolumeRestRep vol = client.blockVolumes().get(volList.get(0));
        	StorageSystemRestRep sys = client.storageSystems().get(vol.getStorageController());
        	if (sys.getSystemType().equals("vplex")) {
        		isVplex = true;
                sourceVarrayId = vol.getVirtualArray().getId();
                List<RelatedResourceRep> assocVols = vol.getHaVolumes();
                if (assocVols.size() > 1) {
                    isDistributed = true;
                    for (RelatedResourceRep backingVolRef : assocVols) {
                        VolumeRestRep backingVol = client.blockVolumes().get(backingVolRef.getId());
                        if (!backingVol.getVirtualArray().getId().equals(sourceVarrayId)) {
                            haVarrayId = backingVol.getVirtualArray().getId();
                            break;
                        }
                    }
                }
        	}
        	if (vol.getProtection() != null) {
        		isRP = true;
                for (VolumeRestRep rpvol : allVols) {
                    if (rpvol.getProtection() != null && rpvol.getProtection().getRpRep().getPersonality().equals("SOURCE")) {
                        sourceVarrayId = rpvol.getVirtualArray().getId();
                        break;
                    }
                }
        	}
        } else {
            return options;
        }

        // if the volumes are vplex or RP display source as an option
        if (isVplex || isRP) {
            options.add(newAssetOption(sourceVarrayId, "protection.site.type.source"));
        }
        // if the volumes are vplex distributed display HA as an option
        if (isVplex && isDistributed) {
            options.add(newAssetOption(haVarrayId, "protection.site.type.ha"));
        }
        // if the volumes are RP (vplex or not) add the RP targets as options
        if (isRP) {
            Set<URI> cgIds = new HashSet<URI>();
        	for (VolumeRestRep vol : allVols) {
                cgIds.add(vol.getConsistencyGroup().getId());
        	}

            List<AssetOption> targetOptions = new ArrayList<AssetOption>();
            for (URI cgId : cgIds) {
                targetOptions.addAll(getFailoverTarget(ctx, cgId));
        	}

            // sort the targets
            AssetOptionsUtils.sortOptionsByLabel(targetOptions);

            // remove duplicates
            AssetOption previous = null;
            List<AssetOption> tgtOptions = new ArrayList<AssetOption>();
            for (AssetOption option : targetOptions) {
                if (previous == null || !option.key.equals(previous.key)) {
                    tgtOptions.add(newAssetOption(option.key, "protection.site.type.target", option.value));
                }
            }

            options.addAll(tgtOptions);

        }
        return options;
    }

    class VirtualPoolFilter extends DefaultResourceFilter<VolumeRestRep> {

        private final URI virtualPoolId;

        public VirtualPoolFilter(URI virtualPoolId) {
            this.virtualPoolId = virtualPoolId;
        }

        @Override
        public boolean accept(VolumeRestRep item) {
            return ObjectUtils.equals(ResourceUtils.id(item.getVirtualPool()), virtualPoolId);
        }

    }

    @SafeVarargs
    public static List<VolumeRestRep> listSourceVolumes(ViPRCoreClient client, URI project, ResourceFilter<VolumeRestRep>... filters) {
        // Filter volumes that are not SRDF Targets, not RP Targets and not RP Metadata
        FilterChain<VolumeRestRep> filter = new FilterChain<VolumeRestRep>(new SRDFTargetFilter().not());
        filter.and(RecoverPointPersonalityFilter.TARGET.not());
        filter.and(RecoverPointPersonalityFilter.METADATA.not());
        for (ResourceFilter<VolumeRestRep> additionalFilter : filters) {
            filter.and(additionalFilter);
        }
        return client.blockVolumes().findByProject(project, filter);
    }

    protected static List<VolumeRestRep> listVolumes(ViPRCoreClient client, URI project) {
        return client.blockVolumes().findByProject(project);
    }

    /**
     * @deprecated
     *             This should not be used, as it calls the bulk api and then individually loads the
     *             full VolumeRestRep in order to gain access to special fields on the object( HasXIO3XVolumes ).
     *             This should be viewed as a temporary patch to a problem with the bulk block volume api service.
     *
     * @param client
     * @param project
     * @return
     */
    @Deprecated
    protected static List<VolumeRestRep> listVolumesNonBulk(ViPRCoreClient client, URI project) {
        List<VolumeRestRep> volumes = new ArrayList<VolumeRestRep>();
        for (VolumeRestRep volume : listVolumes(client, project)) {
            volumes.add(client.blockVolumes().get(volume.getId()));
        }
        return volumes;
    }

    protected List<VolumeRestRep> listVolumesWithoutConsistencyGroup(ViPRCoreClient client, URI project) {
        return client.blockVolumes().findByProject(project, new DefaultResourceFilter<VolumeRestRep>() {

            @Override
            public boolean accept(VolumeRestRep item) {
                return !isInConsistencyGroup(item);
            }
        });
    }

    protected static List<AssetOption> createVolumeWithVarrayOptions(ViPRCoreClient client,
            Collection<? extends BlockObjectRestRep> blockObjects) {
        List<URI> varrayIds = getVirtualArrayIds(blockObjects);
        Map<URI, VirtualArrayRestRep> varrayNames = getVirutalArrayNames(client, varrayIds);
        List<AssetOption> options = Lists.newArrayList();
        for (BlockObjectRestRep blockObject : blockObjects) {
            options.add(createVolumeWithVarrayOption(blockObject, varrayNames));
        }
        AssetOptionsUtils.sortOptionsByLabel(options);
        return options;
    }

    protected static AssetOption createVolumeWithVarrayOption(BlockObjectRestRep blockObject, Map<URI, VirtualArrayRestRep> varrayNames) {
        String label = getBlockObjectLabelWithVarray(blockObject, varrayNames);
        String name = getBlockObjectName(null, blockObject);
        if (StringUtils.isNotBlank(name)) {
            label = String.format("%s: %s", name, label);
        }
        return new AssetOption(blockObject.getId(), label);
    }

    private static String getBlockObjectLabelWithVarray(BlockObjectRestRep blockObject, Map<URI, VirtualArrayRestRep> varrayNames) {
        if (blockObject instanceof VolumeRestRep) {
            VolumeRestRep volume = (VolumeRestRep) blockObject;
            String varrayName = varrayNames.get(volume.getVirtualArray().getId()).getName();
            return getMessage("block.unexport.volume", volume.getName(), volume.getProvisionedCapacity(), varrayName);
        }
        return blockObject.getName();
    }

    private static List<URI> getVirtualArrayIds(Collection<? extends BlockObjectRestRep> blockObjects) {
        List<URI> varrayIds = Lists.newArrayList();
        for (BlockObjectRestRep blockObject : blockObjects) {
            if (blockObject instanceof VolumeRestRep) {
                VolumeRestRep volume = (VolumeRestRep) blockObject;
                varrayIds.add(volume.getVirtualArray().getId());
            }
        }
        return varrayIds;
    }

    protected static Map<URI, VirtualArrayRestRep> getVirutalArrayNames(ViPRCoreClient client, List<URI> varrays) {
        if (varrays == null) {
            return Collections.emptyMap();
        }
        return ResourceUtils.mapById(client.varrays().getByIds(varrays));
    }

    protected static List<AssetOption> createVolumeOptions(ViPRCoreClient client, Collection<? extends BlockObjectRestRep> blockObjects) {
        return createVolumeOptions(client, null, null, blockObjects);
    }

    protected static List<AssetOption> createVolumeOptions(ViPRCoreClient client, URI project, URI hostId,
            Collection<? extends BlockObjectRestRep> blockObjects) {
        Map<URI, VolumeRestRep> volumeNames = getProjectVolumeNames(client, project);
        List<AssetOption> options = Lists.newArrayList();
        for (BlockObjectRestRep blockObject : blockObjects) {
            options.add(createVolumeOption(client, hostId, blockObject, volumeNames));
        }
        AssetOptionsUtils.sortOptionsByLabel(options);
        return options;
    }

    protected static AssetOption createVolumeOption(ViPRCoreClient client, URI hostId, BlockObjectRestRep blockObject,
            Map<URI, VolumeRestRep> volumeNames) {
        String label = getBlockObjectLabel(client, blockObject, volumeNames);
        String name = getBlockObjectName(hostId, blockObject);
        if (StringUtils.isNotBlank(name)) {
            label = String.format("%s: %s", name, label);
        }
        return new AssetOption(blockObject.getId(), label);
    }

    protected static Map<URI, VolumeRestRep> getProjectVolumeNames(ViPRCoreClient client, URI project) {
        if (project == null) {
            return Collections.emptyMap();
        }
        return ResourceUtils.mapById(client.blockVolumes().findByProject(project));
    }

    protected static String getBlockObjectName(URI hostId, BlockObjectRestRep blockObject) {
        if (hostId != null) {
            String mountPoint = KnownMachineTags.getBlockVolumeMountPoint(hostId, blockObject);
            if (StringUtils.isNotBlank(mountPoint)) {
                return mountPoint;
            }
        }

        String datastore = KnownMachineTags.getBlockVolumeVMFSDatastore(hostId, blockObject);
        if (StringUtils.isNotBlank(datastore)) {
            return datastore;
        }
        return null;
    }

    protected boolean isMounted(Collection<URI> hostIds, VolumeRestRep volume) {
        for (URI hostId : hostIds) {
            if (BlockProviderUtils.isMounted(hostId, volume)) {
                return true;
            }
        }
        return false;
    }

    private static String getBlockObjectLabel(ViPRCoreClient client, BlockObjectRestRep blockObject, Map<URI, VolumeRestRep> volumeNames) {
        if (blockObject instanceof VolumeRestRep) {
            VolumeRestRep volume = (VolumeRestRep) blockObject;
            return getMessage("block.volume", volume.getName(), volume.getProvisionedCapacity());
        }
        else if (blockObject instanceof BlockSnapshotRestRep) {
            BlockSnapshotRestRep snapshot = (BlockSnapshotRestRep) blockObject;
            return getMessage("block.snapshot.label", snapshot.getName(), getBlockSnapshotParentVolumeName(volumeNames, snapshot));
        }
        else if (blockObject instanceof BlockSnapshotSessionRestRep) {
            BlockSnapshotSessionRestRep snapshotSession = (BlockSnapshotSessionRestRep) blockObject;
            return getMessage("block.snapshotsession.label", snapshotSession.getName(),
                    getBlockSnapshotSessionParentVolumeName(volumeNames, snapshotSession));
        }
        return blockObject.getName();
    }

    private static String getBlockSnapshotLinkedLabel(BlockSnapshotRestRep snapshot, Map<URI, String> linkedSnapshotToSnapshotSessionMap) {
        String snapshotSessionName = linkedSnapshotToSnapshotSessionMap.get(snapshot.getId());
        if (snapshotSessionName != null && !snapshotSessionName.isEmpty()) {
            return getMessage("block.snapshot.linked", snapshot.getName(), snapshotSessionName);
        }
        return getMessage("block.snapshot.linked.unknown", snapshot.getName());
    }

    protected static String getBlockSnapshotParentVolumeName(Map<URI, VolumeRestRep> volumeNames, BlockSnapshotRestRep snapshot) {
        if (snapshot.getParent() != null &&
                snapshot.getParent().getId() != null &&
                volumeNames != null &&
                volumeNames.get(snapshot.getParent().getId()) != null) {
            return ResourceUtils.name(volumeNames.get(snapshot.getParent().getId()));
        }
        return getMessage("block.snapshot.unknown.volume");
    }

    protected static String getBlockSnapshotSessionParentVolumeName(Map<URI, VolumeRestRep> volumeNames,
            BlockSnapshotSessionRestRep snapshotSession) {
        if (snapshotSession.getParent() != null &&
                snapshotSession.getParent().getId() != null &&
                volumeNames != null &&
                volumeNames.get(snapshotSession.getParent().getId()) != null) {
            return ResourceUtils.name(volumeNames.get(snapshotSession.getParent().getId()));
        }
        return getMessage("block.snapshot.unknown.volume");
    }

    private List<AssetOption>
            getSnapshotOptionsForProject(AssetOptionsContext ctx, URI project, ResourceFilter<BlockSnapshotRestRep> filter) {
        ViPRCoreClient client = api(ctx);
        List<BlockSnapshotRestRep> snapshots = client.blockSnapshots().findByProject(project, filter);
        return constructSnapshotOptions(client, project, snapshots);
    }

    private List<AssetOption> getSnapshotOptionsForProject(AssetOptionsContext ctx, URI project) {
        ViPRCoreClient client = api(ctx);
        List<BlockSnapshotRestRep> snapshots = client.blockSnapshots().findByProject(project);
        return constructSnapshotOptions(client, project, snapshots);
    }

    protected List<AssetOption> constructSnapshotOptions(ViPRCoreClient client, URI project, List<BlockSnapshotRestRep> snapshots) {
        List<AssetOption> options = Lists.newArrayList();
        Map<URI, VolumeRestRep> volumeNames = getProjectVolumeNames(client, project);
        for (BlockSnapshotRestRep snapshot : snapshots) {
            options.add(new AssetOption(snapshot.getId(), getBlockObjectLabel(client, snapshot, volumeNames)));
        }
        AssetOptionsUtils.sortOptionsByLabel(options);
        return options;
    }

    protected List<AssetOption> constructSnapshotOptions(List<BlockSnapshotRestRep> snapshots) {
        List<AssetOption> options = Lists.newArrayList();
        for (BlockSnapshotRestRep snapshot : snapshots) {
            options.add(new AssetOption(snapshot.getId(), snapshot.getName()));
        }
        AssetOptionsUtils.sortOptionsByLabel(options);
        return options;
    }

    protected List<AssetOption> constructSnapshotWithSnapshotSessionOptions(List<BlockSnapshotRestRep> snapshots,
            List<BlockSnapshotSessionRestRep> snapshotSessions) {
        List<AssetOption> options = Lists.newArrayList();
        // Create a map of linked target URIs to snapshot session names for convenience when creating
        // the option labels.
        Map<URI, String> linkedSnapshotToSnapshotSessionMap = new HashMap<URI, String>();
        for (BlockSnapshotSessionRestRep snapshotSession : snapshotSessions) {
            for (RelatedResourceRep linkedTarget : snapshotSession.getLinkedTarget()) {
                linkedSnapshotToSnapshotSessionMap.put(linkedTarget.getId(), snapshotSession.getName());
            }
        }
        for (BlockSnapshotRestRep snapshot : snapshots) {
            options.add(new AssetOption(snapshot.getId(), getBlockSnapshotLinkedLabel(snapshot, linkedSnapshotToSnapshotSessionMap)));
        }
        AssetOptionsUtils.sortOptionsByLabel(options);
        return options;
    }
    
    protected List<AssetOption> constructSnapshotSessionOptions(ViPRCoreClient client, URI project, List<BlockSnapshotSessionRestRep> snapshotSessions) {
        List<AssetOption> options = Lists.newArrayList();
        Map<URI, VolumeRestRep> volumeNames = getProjectVolumeNames(client, project);
        for (BlockSnapshotSessionRestRep snapshotSession : snapshotSessions) {
            options.add(new AssetOption(snapshotSession.getId(), getBlockObjectLabel(client, snapshotSession, volumeNames)));
        }
        AssetOptionsUtils.sortOptionsByLabel(options);
        return options;
    }

    /**
     * Gets the set of volume IDs associated with the given exports.
     *
     * @param exports
     *            the export groups.
     * @return the set of volume IDs.
     */
    protected static Set<URI> getBlockVolumeIdsForExports(List<ExportGroupRestRep> exports) {
        Set<URI> ids = Sets.newHashSet();
        for (ExportGroupRestRep export : exports) {
            if (export.getVolumes() != null) {
                for (ExportBlockParam volume : export.getVolumes()) {
                    ids.add(volume.getId());
                }
            }
        }
        return ids;
    }

    /**
     * Gets the set of volume IDs for volumes in the given project that are exported to the given host/cluster
     *
     * @param client An instance of the ViPRCoreClient
     * @param projectId The ViPR ID of the project
     * @param hostOrClusterId The ViPR ID of the host/cluster
     *
     * @return The set of Volume IDs
     */
    protected static Set<URI> getExportedVolumes(ViPRCoreClient client, URI projectId, URI hostOrClusterId, URI virtualArrayId) {
        // determine host and cluster id
        URI hostId = BlockStorageUtils.isHost(hostOrClusterId) ? hostOrClusterId : null;
        URI clusterId = BlockStorageUtils.isCluster(hostOrClusterId) ? hostOrClusterId : null;

        // get a list of all the exports in this project that expose resources to this host/cluster
        ResourceFilter<ExportGroupRestRep> filter;
        if (virtualArrayId == null) {
            filter = new ExportHostOrClusterFilter(hostId, clusterId);
        }
        else {
            filter = new ExportHostOrClusterFilter(hostId, clusterId).and(new ExportVirtualArrayFilter(virtualArrayId));
        }
        List<ExportGroupRestRep> exports = client.blockExports().findByProject(projectId, filter);

        return getBlockVolumeIdsForExports(exports);
    }

    /**
     * Gets the value of the specified tag from the given volumes.
     *
     * @param tagName
     *            the tag name.
     * @param volumes
     *            the volumes.
     * @return the tag values.
     */
    public static Set<String> getTagValues(String tagName, Collection<? extends BlockObjectRestRep> volumes) {
        Set<String> values = Sets.newHashSet();
        for (BlockObjectRestRep volume : volumes) {
            String value = MachineTagUtils.getBlockVolumeTag(volume, tagName);
            if (value != null) {
                values.add(value);
            }
        }
        return values;
    }

    /**
     * Returns a list of virtual pools where volume can be moved
     */
    protected List<BlockVirtualPoolRestRep> listTargetVirtualPools(AssetOptionsContext ctx, URI volumeId,
            ResourceFilter<BlockVirtualPoolRestRep> filter) {
        ViPRCoreClient client = api(ctx);
        List<VirtualPoolChangeRep> vpoolChanges = client.blockVolumes().listVirtualPoolChangeCandidates(volumeId);

        List<URI> vpoolIds = Lists.newArrayList();
        for (VirtualPoolChangeRep change : vpoolChanges) {
            if (change.getAllowed()) {
                vpoolIds.add(change.getId());
            }
        }
        return client.blockVpools().getByIds(vpoolIds, filter);
    }

    /**
     * Gets the volume details for a collection of volumes.
     *
     * @param client the bourne client.
     * @param volumes the collection of volumes.
     * @return the volume details.
     */
    protected static List<VolumeDetail> getVolumeDetails(ViPRCoreClient client, Collection<VolumeRestRep> volumes) {
        Set<URI> blockVirtualPoolIds = getBlockVirtualPoolIdsForVolumes(volumes);
        Map<URI, BlockVirtualPoolRestRep> virtualPoolMap = getBlockVirtualPools(client, blockVirtualPoolIds);

        List<VolumeDetail> details = Lists.newArrayList();
        for (VolumeRestRep volume : volumes) {
            BlockVirtualPoolRestRep virtualPool = virtualPoolMap.get(volume.getVirtualPool().getId());
            details.add(new VolumeDetail(volume, virtualPool));
        }
        return details;
    }

    /**
     * Gets the unique set of BlockVirtualPool IDs for the given volumes.
     *
     * @param volumes the volumes.
     * @return the block virtual pool IDs.
     */
    protected static Set<URI> getBlockVirtualPoolIdsForVolumes(Collection<VolumeRestRep> volumes) {
        Set<URI> ids = Sets.newHashSet();
        for (VolumeRestRep volume : volumes) {
            ids.add(volume.getVirtualPool().getId());
        }
        return ids;
    }

    /**
     * Gets all BlockVirtualPools mapped by ID.
     *
     * @param client the ViPR client instance.
     * @param ids the IDs.
     * @return the mapping of ID->BlockVirtualPoolRestRep
     */
    public static Map<URI, BlockVirtualPoolRestRep> getBlockVirtualPools(ViPRCoreClient client, Collection<URI> ids) {
        Map<URI, BlockVirtualPoolRestRep> virtualPools = Maps.newHashMap();
        for (BlockVirtualPoolRestRep virtualPool : client.blockVpools().getByIds(ids)) {
            virtualPools.put(virtualPool.getId(), virtualPool);
        }
        return virtualPools;
    }

    /**
     * Gets all BlockVirtualPools mapped by ID.
     *
     * @param client the ViPR client instance.
     * @param volumes the volumes for which we need the VPool information.
     * @return the mapping of ID->BlockVirtualPoolRestRep
     */
    public static Map<URI, BlockVirtualPoolRestRep> getVpoolsForVolumes(ViPRCoreClient client, List<VolumeRestRep> volumes) {
        // collect the id of each vpool used by a volume in the given list
        Set<URI> vpoolIds = Sets.newHashSet();
        for (VolumeRestRep volume : volumes) {
            vpoolIds.add(volume.getVirtualPool().getId());
        }

        // build up a map of URI to VPool
        return getBlockVirtualPools(client, vpoolIds);
    }

    /**
     * Gets all {@link VolumeRestRep}s that are either in the target VArray or use the target VArray for protection
     *
     * @param client the ViPR client instance.
     * @param targetVArrayId the target VArray ID.
     * @param volumes the volumes we are concerned with. (These should be VPlex volumes)
     * @return List of {@link VolumeRestRep}s that are VPlex volumes that are in the target VArray
     */
    public static List<BlockObjectRestRep> getVPlexVolumesInTargetVArray(ViPRCoreClient client, URI targetVArrayId,
            List<VolumeRestRep> volumes) {
        // collect vpools used by these volumes
        Map<URI, BlockVirtualPoolRestRep> vpools = getVpoolsForVolumes(client, volumes);

        // sift through the volumes to find ones with the correct VArray
        List<BlockObjectRestRep> acceptedVolumes = Lists.newArrayList();
        for (VolumeRestRep volume : volumes) {
            if (volume.getVirtualArray().getId().equals(targetVArrayId)) {
                addVolume(acceptedVolumes, volume);
            }
            else {
                // if this volume's HA type is 'distributed' and its distributed VArray matches the target VArray we can accept the volume
                URI vpoolId = volume.getVirtualPool().getId();
                BlockVirtualPoolRestRep volumeVpool = vpools.get(vpoolId);
                if (volumeVpool != null && isVpoolProtectedByVarray(volumeVpool, targetVArrayId)) {
                    addVolume(acceptedVolumes, volume);
                }
            }
        }

        return acceptedVolumes;
    }

    /**
     * Class for holding all volume detail information, currently the volume and virtual pool.
     *
     * @author jonnymiller
     */
    public static class VolumeDetail {
        public VolumeRestRep volume;
        public BlockVirtualPoolRestRep vpool;

        public VolumeDetail(VolumeRestRep volume) {
            this.volume = volume;
        }

        public VolumeDetail(VolumeRestRep volume, BlockVirtualPoolRestRep vpool) {
            this.volume = volume;
            this.vpool = vpool;
        }
    }

    public static List<AssetOption> createVirtualPoolResourceOptions(Collection<? extends VirtualPoolCommonRestRep> virtualPools) {

        List<AssetOption> options = Lists.newArrayList();

        for (VirtualPoolCommonRestRep virtualPool : virtualPools) {
            options.add(createVirtualPoolResourceOption(virtualPool));
        }
        AssetOptionsUtils.sortOptionsByLabel(options);
        return options;
    }

    public static AssetOption createVirtualPoolResourceOption(VirtualPoolCommonRestRep virtualPool) {
        boolean hasPools = (virtualPool.getUseMatchedPools() && !CollectionUtils.isEmpty(virtualPool.getMatchedStoragePools()) ||
                (!virtualPool.getUseMatchedPools() && !CollectionUtils.isEmpty(virtualPool.getAssignedStoragePools())));

        String label = virtualPool.getName();
        if (!hasPools) {
            label = getMessage("block.virtualPool.noStorage", virtualPool.getName());
        }

        return new AssetOption(virtualPool.getId(), label);
    }

    protected List<AssetOption> createVpoolChangeOptions(String vpoolChangeOperation, List<VirtualPoolChangeRep> vpoolChanges) {
        List<AssetOption> options = Lists.newArrayList();
        for (VirtualPoolChangeRep vpoolChange : vpoolChanges) {
            if (vpoolChange.getAllowedChangeOperations() != null) {
                for (StringHashMapEntry allowedChangeOperation : vpoolChange.getAllowedChangeOperations()) {
                    String operation = allowedChangeOperation.getName();
                    boolean isCorrectOperation =
                            StringUtils.isNotBlank(operation) &&
                                    operation.equalsIgnoreCase(vpoolChangeOperation);
                    if (isCorrectOperation) {
                        options.add(new AssetOption(vpoolChange.getId(), vpoolChange.getName()));
                    }
                }
            }
        }
        return options;
    }

    protected List<BlockObjectRestRep>
            getProjectBlockResources(ViPRCoreClient client, URI hostOrClusterId, URI project, boolean onlyMounted) {
        List<BlockObjectRestRep> blockObjects = Lists.newArrayList();

        // the list of host ids that the volume could be mounted to
        List<URI> hostIds = getHostIds(client, hostOrClusterId, onlyMounted);

        // get a list of all volumes and snapshots in the project
        List<VolumeRestRep> projectVolumes = client.blockVolumes().findByProject(project);
        List<BlockSnapshotRestRep> projectSnapshots = client.blockSnapshots().findByProject(project);

        // cycle through every volume in the project and add the volume and its snapshots
        for (VolumeRestRep volume : projectVolumes) {
            boolean isMounted = isMounted(hostIds, volume);
            if ((onlyMounted && isMounted) || (!onlyMounted && !isMounted)) {
                addVolume(blockObjects, volume, projectSnapshots);
            }
        }

        // remaining snapshots can be added now. We don't know their parent volume but we need them in the list anyway
        blockObjects.addAll(projectSnapshots);

        return blockObjects;
    }

    protected List<VolumeRestRep> getProjectBlockVolumes(ViPRCoreClient client, URI hostOrClusterId, URI project, boolean onlyMounted) {
        List<VolumeRestRep> volumes = Lists.newArrayList();

        // the list of host ids that the volume could be mounted to
        List<URI> hostIds = getHostIds(client, hostOrClusterId, onlyMounted);

        // get a list of all volumes and snapshots in the project
        List<VolumeRestRep> projectVolumes = client.blockVolumes().findByProject(project);

        // cycle through every volume in the project and add those that match
        for (VolumeRestRep volume : projectVolumes) {
            boolean isMounted = isMounted(hostIds, volume);
            if ((onlyMounted && isMounted) || (!onlyMounted && !isMounted)) {
                volumes.add(volume);
            }
        }
        return volumes;
    }

    protected List<URI> getHostIds(ViPRCoreClient client, URI hostOrClusterId,
            boolean onlyMounted) {
        List<URI> hostIds = Lists.newArrayList();

        if (onlyMounted) {
            hostIds.add(hostOrClusterId);
        }
        else {
            hostIds.addAll(HostProvider.getHostIds(client, hostOrClusterId));

            // Add Cluster ID to the end of the host ID's
            if (BlockStorageUtils.isCluster(hostOrClusterId)) {
                hostIds.add(hostOrClusterId);
            }
            else {
                HostRestRep host = client.hosts().get(hostOrClusterId);
                if (host.getCluster() != null) {
                    hostIds.add(host.getCluster().getId());
                }
            }
        }
        return hostIds;
    }

    protected static void addVolume(List<BlockObjectRestRep> blockObjects, VolumeRestRep volume) {
        addVolume(blockObjects, volume, null);
    }

    /**
     * Add the volume and it's snapshots to the 'blockObjects' list.
     *
     * When the method completes the snapshots that have been added to the blockObjects list will be removed from the snapshots list.
     */
    protected static void addVolume(List<BlockObjectRestRep> blockObjects, VolumeRestRep volume, List<BlockSnapshotRestRep> snapshots) {
        blockObjects.add(volume);
        if (CollectionUtils.isNotEmpty(snapshots)) {
            // use an iterator so we can do proper removes
            Iterator<BlockSnapshotRestRep> snapshotIter = snapshots.iterator();
            while (snapshotIter.hasNext()) {
                BlockSnapshotRestRep snap = snapshotIter.next();
                if (ResourceUtils.idEquals(snap.getParent(), volume)) {
                    blockObjects.add(snap);
                    snapshotIter.remove();
                }
            }
        }
    }

    /**
     * SRDF filter for block objects.
     */
    private static class BlockObjectSRDFTargetFilter extends DefaultResourceFilter<BlockObjectRestRep> {
        private final SRDFTargetFilter filter = new SRDFTargetFilter();

        @Override
        public boolean accept(BlockObjectRestRep item) {
            if (item instanceof VolumeRestRep) {
                return filter.accept((VolumeRestRep) item);
            }
            return false;
        }
    }
}
