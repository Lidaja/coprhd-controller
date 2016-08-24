/*
 *  Copyright (c) 2016 EMC Corporation
 * All Rights Reserved
 */
package com.emc.storageos.volumecontroller.impl.plugins.discovery.smis.processor.export;

import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.cim.CIMInstance;
import javax.cim.CIMObjectPath;
import javax.cim.UnsignedInteger32;
import javax.wbem.CloseableIterator;
import javax.wbem.client.EnumerateResponse;
import javax.wbem.client.WBEMClient;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.emc.storageos.db.client.DbClient;
import com.emc.storageos.db.client.model.ExportGroup;
import com.emc.storageos.db.client.model.Host;
import com.emc.storageos.db.client.model.Initiator;
import com.emc.storageos.db.client.util.NullColumnValueGetter;
import com.emc.storageos.db.client.util.WWNUtility;
import com.emc.storageos.db.client.util.iSCSIUtility;
import com.emc.storageos.plugins.AccessProfile;
import com.emc.storageos.plugins.BaseCollectionException;
import com.emc.storageos.plugins.common.Constants;
import com.emc.storageos.plugins.common.PartitionManager;
import com.emc.storageos.plugins.common.Processor;
import com.emc.storageos.plugins.common.domainmodel.Operation;
import com.emc.storageos.util.NetworkUtil;
import com.emc.storageos.volumecontroller.impl.plugins.SMICommunicationInterface;
import com.emc.storageos.volumecontroller.impl.smis.SmisConstants;

/**
 * Processor used for retrieving masking constructs and populating data structures for array affinity.
 */
public class ArrayAffinityExportProcessor extends Processor {

    private final Logger _logger = LoggerFactory.getLogger(ArrayAffinityExportProcessor.class);
    private AccessProfile _profile;
    private Map<String, Object> _keyMap;
    private DbClient _dbClient;
    private List<Object> _args;

    private final String ISCSI_PATTERN = "^(iqn|IQN|eui).*$";
    private static int MAX_OBJECT_COUNT = 100;

    private static final String HOST = "Host";
    private static final int BATCH_SIZE = 100;

    private Map<URI, Set<String>> _hostToExportMasksMap = null;
    private Map<String, Set<URI>> _exportMaskToHostsMap = null;
    private Map<String, Set<String>> _maskToVolumesMap = null;
    private Map<String, Set<URI>> _maskToStoragePoolsMap = null;
    private Map<String, URI> _volumeToStoragePoolMap = null;

    private PartitionManager _partitionManager;

    /**
     * Method for setting the partition manager via injection.
     *
     * @param partitionManager the partition manager instance
     */
    public void setPartitionManager(PartitionManager partitionManager) {
        _partitionManager = partitionManager;
    }

    /**
     * Initialize the Processor. Child classes should call
     * super.initialize if they want the various convenience getter
     * methods to work.
     *
     * @param operation
     * @param resultObj
     * @param keyMap
     */
    protected void initialize(Operation operation, Object resultObj,
            Map<String, Object> keyMap) {
        _keyMap = keyMap;
        _dbClient = (DbClient) keyMap.get(Constants.dbClient);
        _profile = (AccessProfile) keyMap.get(Constants.ACCESSPROFILE);
    }

    /*
     * (non-Javadoc)
     *
     * @see com.emc.storageos.plugins.common.Processor#processResult(com.emc.storageos.plugins.common.domainmodel.Operation,
     * java.lang.Object, java.util.Map)
     */
    @SuppressWarnings("unchecked")
    @Override
    public void processResult(Operation operation, Object resultObj,
            Map<String, Object> keyMap) throws BaseCollectionException {

        initialize(operation, resultObj, keyMap);
        CloseableIterator<CIMInstance> it = null;
        EnumerateResponse<CIMInstance> response = null;
        WBEMClient client = SMICommunicationInterface.getCIMClient(keyMap);

        try {
            // get lun masking view CIM path
            CIMObjectPath path = getObjectPathfromCIMArgument(_args, keyMap);
            _logger.info("looking at lun masking view: " + path.toString());
            response = (EnumerateResponse<CIMInstance>) resultObj;
            processVolumesAndInitiatorsPaths(response.getResponses(), path.toString(), client);

            while (!response.isEnd()) {
                _logger.info("Processing next Chunk");
                response = client.getInstancesWithPath(Constants.MASKING_PATH, response.getContext(),
                        new UnsignedInteger32(MAX_OBJECT_COUNT));
                processVolumesAndInitiatorsPaths(response.getResponses(), path.toString(), client);
            }
        } catch (Exception e) {
            _logger.error("Processing lun maksing view failed", e);
        } finally {
            if (it != null) {
                it.close();
            }

            wrapUp();

            if (response != null) {
                try {
                    client.closeEnumeration(Constants.MASKING_PATH, response.getContext());
                } catch (Exception e) {
                    _logger.debug("Exception occurred while closing enumeration", e);
                }
            }
        }
    }

    /**
     * Gets the Map of host to maskingViewPaths that is being tracked in the keyMap.
     *
     * @return a Map of host to maskingViewPaths
     */
    private Map<URI, Set<String>> getHostToExportMasksMap() {
        // find or create the host -> maskingViewPaths tracking data structure in the key map
        _hostToExportMasksMap = (Map<URI, Set<String>>) _keyMap.get(Constants.HOST_EXPORT_MASKS_MAP);
        if (_hostToExportMasksMap == null) {
            _hostToExportMasksMap = new HashMap<URI, Set<String>>();
            _keyMap.put(Constants.HOST_EXPORT_MASKS_MAP, _hostToExportMasksMap);
        }

        return _hostToExportMasksMap;
    }

    /**
     * Gets the Map of maskingViewPath to hosts that is being tracked in the keyMap.
     *
     * @return a Map of maskingViewPath to hosts
     */
    private Map<String, Set<URI>> getExportMaskToHostsMap() {
        // find or create the maskingViewPath -> hosts tracking data structure in the key map
        _exportMaskToHostsMap = (Map<String, Set<URI>>) _keyMap.get(Constants.EXPORT_MASK_HOSTS_MAP);
        if (_exportMaskToHostsMap == null) {
            _exportMaskToHostsMap = new HashMap<String, Set<URI>>();
            _keyMap.put(Constants.EXPORT_MASK_HOSTS_MAP, _exportMaskToHostsMap);
        }

        return _exportMaskToHostsMap;
    }

    /**
     * Gets the Map of maskingViewPath to volumes that is being tracked in the keyMap.
     *
     * @return a Map of maskingViewPath to volumes
     */
    private Map<String, Set<String>> getExportMaskToVolumesMap() {
        // find or create the maskingViewPath -> volumes tracking data structure in the key map
        _maskToVolumesMap = (Map<String, Set<String>>) _keyMap.get(Constants.EXPORT_MASK_VOLUMES_MAP);
        if (_maskToVolumesMap == null) {
            _maskToVolumesMap = new HashMap<String, Set<String>>();
            _keyMap.put(Constants.EXPORT_MASK_VOLUMES_MAP, _maskToVolumesMap);
        }

        return _maskToVolumesMap;
    }

    /**
     * Gets the Map of maskingViewPath to StoragePools that is being tracked in the keyMap.
     *
     * @return a Map of maskingViewPath to StoragePools
     */
    private Map<String, Set<URI>> getMaskToStoragePoolsMap() {
        // find or create the maskingViewPath -> StoragePools tracking data structure in the key map
        _maskToStoragePoolsMap = (Map<String, Set<URI>>) _keyMap.get(Constants.EXPORT_MASK_STORAGE_POOLS_MAP);
        if (_maskToStoragePoolsMap == null) {
            _maskToStoragePoolsMap = new HashMap<String, Set<URI>>();
            _keyMap.put(Constants.EXPORT_MASK_STORAGE_POOLS_MAP, _maskToStoragePoolsMap);
        }

        return _maskToStoragePoolsMap;
    }

    /**
     * Gets the Map of volume to StoragePool that is being tracked in the keyMap.
     *
     * @return a Map of volume to StoragePool
     */
    private Map<String, URI> getVolumeToStoragePoolMap() {
        // find or create the volume -> StoragePool tracking data structure in the key map
        _volumeToStoragePoolMap = (Map<String, URI>) _keyMap.get(Constants.VOLUME_STORAGE_POOL_MAP);
        if (_volumeToStoragePoolMap == null) {
            _volumeToStoragePoolMap = new HashMap<String, URI>();
            _keyMap.put(Constants.VOLUME_STORAGE_POOL_MAP, _volumeToStoragePoolMap);
        }

        return _volumeToStoragePoolMap;
    }

    /**
     * Update preferredPools for hosts
     */
    protected void wrapUp() {

        Integer currentCommandIndex = this.getCurrentCommandIndex(_args);
        List maskingViews = (List) _keyMap.get(Constants.MASKING_VIEWS);
        _logger.info("ArrayAffinityExportProcessor current index is " + currentCommandIndex);
        _logger.info("ArrayAffinityExportProcessor maskingViews size is " + maskingViews.size());
        if ((maskingViews != null) && (maskingViews.size() == (currentCommandIndex + 1))) {
            _logger.info("this is the last time ArrayAffinityExportProcessor will be called, cleaning up...");
            updatePreferredPools();
        } else {
            _logger.info("no need to wrap up yet...");
        }
    }

    private void processVolumesAndInitiatorsPaths(CloseableIterator<CIMInstance> it, String maskingViewPath, WBEMClient client) {
        while (it.hasNext()) {
            CIMInstance instance = it.next();

            _logger.info("looking at classname: " + instance.getClassName());
            switch (instance.getClassName()) {

                // process initiators
                case SmisConstants.CP_SE_STORAGE_HARDWARE_ID:

                    String initiatorNetworkId = this.getCIMPropertyValue(instance, SmisConstants.CP_STORAGE_ID);
                    _logger.info("looking at initiator network id " + initiatorNetworkId);
                    if (WWNUtility.isValidNoColonWWN(initiatorNetworkId)) {
                        initiatorNetworkId = WWNUtility.getWWNWithColons(initiatorNetworkId);
                        _logger.info("   wwn normalized to " + initiatorNetworkId);
                    } else if (WWNUtility.isValidWWN(initiatorNetworkId)) {
                        initiatorNetworkId = initiatorNetworkId.toUpperCase();
                        _logger.info("   wwn normalized to " + initiatorNetworkId);
                    } else if (initiatorNetworkId.matches(ISCSI_PATTERN)
                            && (iSCSIUtility.isValidIQNPortName(initiatorNetworkId) || iSCSIUtility
                                    .isValidEUIPortName(initiatorNetworkId))) {
                        _logger.info("   iSCSI storage port normalized to " + initiatorNetworkId);
                    } else {
                        _logger.warn("   this is not a valid FC or iSCSI network id format, skipping");
                        continue;
                    }

                    // check if a host initiator exists for this id
                    Initiator knownInitiator = NetworkUtil.getInitiator(initiatorNetworkId, _dbClient);
                    URI hostId = null;
                    if (knownInitiator != null) {
                        _logger.info("Found an initiator in ViPR on host " + knownInitiator.getHostName());
                        hostId = knownInitiator.getHost();
                    } else {
                        _logger.info("No hosts in ViPR found configured for initiator " + initiatorNetworkId);
                    }

                    if (hostId == null) {
                        hostId = NullColumnValueGetter.getNullURI();
                    }

                    // add to map of host to export masks, and map of mask to hosts
                    Set<String> maskingViewPaths = getHostToExportMasksMap().get(hostId);
                    if (maskingViewPaths == null) {
                        maskingViewPaths = new HashSet<String>();
                        _logger.info("Creating mask set for host {}" + hostId);
                        getHostToExportMasksMap().put(hostId, maskingViewPaths);
                    }
                    maskingViewPaths.add(maskingViewPath);

                    Set<URI> hosts = getExportMaskToHostsMap().get(maskingViewPath);
                    if (hosts == null) {
                         _logger.info("Initial host count for mask {}" + maskingViewPath);
                         hosts = new HashSet<URI>();
                         getExportMaskToHostsMap().put(maskingViewPath, hosts);
                    }

                    hosts.add(hostId);

                    break;

                // process storage volumes
                case _symmvolume:
                case _clarvolume:

                    CIMObjectPath volumePath = instance.getObjectPath();
                    _logger.info("volumePath is " + volumePath.toString());
                    URI poolURI = null;
                    if (ArrayAffinityDiscoveryUtils.isUnmanagedVolume(volumePath, _dbClient)) {
                        poolURI = ArrayAffinityDiscoveryUtils.getStoragePool(volumePath, client, _dbClient);

                        if (!NullColumnValueGetter.isNullURI(poolURI)) {
                            Set<String> volumes = getExportMaskToVolumesMap().get(maskingViewPath);
                            if (volumes == null) {
                                volumes = new HashSet<String>();
                                _logger.info("Creating volume set for mask {}" + maskingViewPath);
                                getExportMaskToVolumesMap().put(maskingViewPath, volumes);
                            }

                            volumes.add(volumePath.toString());

                            Set<URI> pools = getMaskToStoragePoolsMap().get(maskingViewPath);
                            if (pools == null) {
                                pools = new HashSet<URI>();
                                _logger.info("Creating pool set for mask {}" + maskingViewPath);
                                getMaskToStoragePoolsMap().put(maskingViewPath, pools);
                            }

                            pools.add(poolURI);

                            if (!getVolumeToStoragePoolMap().containsKey(volumePath)) {
                                getVolumeToStoragePoolMap().put(volumePath.toString(), poolURI);
                            }
                        }
                    }

                    break;

                default:
                    break;
            }
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see com.emc.storageos.plugins.common.Processor#setPrerequisiteObjects(java.util.List)
     */
    @Override
    protected void setPrerequisiteObjects(List<Object> inputArgs)
            throws BaseCollectionException {
        this._args = inputArgs;
    }

    private void updatePreferredPools() {
        Map<URI, Set<String>> hostToExportMasks = getHostToExportMasksMap();
        Map<String, Set<URI>> exportToMaskHostCount = getExportMaskToHostsMap();
        Map<String, Set<String>> maskToVolumes = getExportMaskToVolumesMap();
        Map<String, Set<URI>> maskToStroagePools = getMaskToStoragePoolsMap();
        Map<String, URI> volumeToStoragePool = getVolumeToStoragePoolMap();

        String systemIdsStr = _profile.getProps().get(Constants.SYSTEM_IDS);
        String[] systemIds = systemIdsStr.split(Constants.ID_DELIMITER);
        Set<String> systemIdSet = new HashSet<String>(Arrays.asList(systemIds));
        List<Host> hostsToUpdate = new ArrayList<Host>();

        Map<URI, Set<String>> hostToVolumes = new HashMap<URI, Set<String>>();
        Map<String, Set<URI>> volumeToHosts = new HashMap<String, Set<URI>>();
        // populate hostToVolumes and volumeToHosts maps
        for (Map.Entry<URI, Set<String>> entry : hostToExportMasks.entrySet()) {
            URI host = entry.getKey();
            for (String mask : entry.getValue()) {
                Set<String> volumes = maskToVolumes.get(mask);
                if (volumes != null) {
                    Set<String> hostVols = hostToVolumes.get(host);
                    if (hostVols == null) {
                        hostVols = new HashSet<String>();
                        hostToVolumes.put(host, hostVols);
                    }

                    hostVols.addAll(volumes);

                    for (String volume : volumes) {
                        Set<URI> hosts = volumeToHosts.get(volume);
                        if (hosts == null) {
                            hosts = new HashSet<URI>();
                            volumeToHosts.put(volume, hosts);
                        }

                        hosts.add(host);
                    }
                }
            }
        }

        try {
            List<URI> hostURIs = _dbClient.queryByType(Host.class, true);
            Iterator<Host> hosts = _dbClient.queryIterativeObjectFields(Host.class, ArrayAffinityDiscoveryUtils.HOST_PROPERTIES, hostURIs);
            while (hosts.hasNext()) {
                Host host = hosts.next();
                if (host != null) {
                    _logger.info("Processing host {}", host.getLabel());
                    // check masks
                    Map<String, String> preferredPoolMap = new HashMap<String, String>();
                    Set<String> masks = hostToExportMasks.get(host.getId());
                    if (masks != null && !masks.isEmpty()) {
                        for (String mask : masks) {
                            Set<URI> pools = maskToStroagePools.get(mask);
                            String exportType = exportToMaskHostCount.get(mask).size() > 1 ? ExportGroup.ExportGroupType.Cluster.name()
                                    : ExportGroup.ExportGroupType.Host.name();
                            if (pools != null && !pools.isEmpty()) {
                                for (URI pool : pools) {
                                    ArrayAffinityDiscoveryUtils.addPoolToPreferredPoolMap(preferredPoolMap, pool.toString(), exportType);
                                }
                            }
                        }
                    }

                    // check volumes
                    Set<String> volumes = hostToVolumes.get(host.getId());
                    if (volumes != null && !volumes.isEmpty()) {
                        for (String volume : volumes) {
                            URI pool = volumeToStoragePool.get(volume);

                            if (pool != null) {
                                String exportType = volumeToHosts.get(volume).size() > 1 ? ExportGroup.ExportGroupType.Cluster.name()
                                        : ExportGroup.ExportGroupType.Host.name();
                                ArrayAffinityDiscoveryUtils.addPoolToPreferredPoolMap(preferredPoolMap, pool.toString(), exportType);
                            }
                        }
                    }

                    if (ArrayAffinityDiscoveryUtils.updatePreferredPools(host, systemIdSet, _dbClient, preferredPoolMap)) {
                        hostsToUpdate.add(host);
                    }
                }

                // if hostsToUpdate size reaches BATCH_SIZE, persist to db
                if (hostsToUpdate.size() >= BATCH_SIZE) {
                    _partitionManager.updateInBatches(hostsToUpdate, BATCH_SIZE, _dbClient, HOST);
                    hostsToUpdate.clear();
                }
            }

            if (!hostsToUpdate.isEmpty()) {
                _partitionManager.updateInBatches(hostsToUpdate, BATCH_SIZE, _dbClient, HOST);
            }
        } catch (Exception e) {
            _logger.warn("Exception on updatePreferredPools", e);
        }
    }
}
