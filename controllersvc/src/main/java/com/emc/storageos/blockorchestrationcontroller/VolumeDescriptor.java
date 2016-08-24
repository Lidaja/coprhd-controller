/*
 * Copyright (c) 2015 EMC Corporation
 * All Rights Reserved
 */
package com.emc.storageos.blockorchestrationcontroller;

import java.io.Serializable;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.emc.storageos.db.client.model.Volume;
import com.emc.storageos.volumecontroller.impl.utils.VirtualPoolCapabilityValuesWrapper;

@SuppressWarnings("serial")
public class VolumeDescriptor implements Serializable {
    public enum Type {
        /*
         * ******************************
         * The ordering of these are important for the sortByType() method,
         * be mindful when adding/removing/changing the list.
         * Especially the RP Values, keep them in sequential order.
         * ******************************
         */
        BLOCK_DATA(1), // user's data volume
        BLOCK_MIRROR(2), // array level mirror
        BLOCK_SNAPSHOT(3), // array level snapshot
        RP_EXISTING_PROTECTED_SOURCE(4), // RecoverPoint existing source volume that has protection already
        RP_EXISTING_SOURCE(5), // RecoverPoint existing source volume
        RP_VPLEX_VIRT_SOURCE(6), // RecoverPoint + VPLEX Virtual source
        RP_SOURCE(7), // RecoverPoint source
        RP_TARGET(8), // RecoverPoint target
        RP_VPLEX_VIRT_TARGET(9), // RecoverPoint + VPLEX Virtual target
        RP_VPLEX_VIRT_JOURNAL(10), // RecoverPoint + VPLEX Virtual journal
        RP_JOURNAL(11), // RecoverPoint journal
        VPLEX_VIRT_VOLUME(12), // VPLEX Virtual Volume
        VPLEX_LOCAL_MIRROR(13), // VPLEX local mirror
        VPLEX_IMPORT_VOLUME(14), // VPLEX existing Volume to be imported
        SRDF_SOURCE(15), // SRDF remote mirror source
        SRDF_TARGET(16), // SRDF remote mirror target
        SRDF_EXISTING_SOURCE(17), // SRDF existing source volume
        VPLEX_MIGRATE_VOLUME(18),
        BLOCK_SNAPSHOT_SESSION(19), // snapshot session
        DUMMY_MIGRATE(20); // Used to pass through without migrating 

        private final int order;

        private Type(int order) {
            this.order = order;
        }

        public int getOrder() {
            return order;
        }
    };

    private Type type; // The type of this volume
    private URI deviceURI; // Device this volume will be created on
    private URI volumeURI; // The volume id or BlockObject id to be created
    private URI poolURI; // The pool id to be used for creation
    private VirtualPoolCapabilityValuesWrapper capabilitiesValues; // Non-volume-specific RP policy is stored in here
    private URI consistencyGroup; // The consistency group this volume belongs to
    private Long volumeSize; // Used to separate multi-volume create requests
    private URI migrationId; // Reference to the migration object for this volume
    private URI computeResource; // Host/Cluster to which the volume will be exported to, as part of the provisioning.
    private List<List<URI>> snapSessionSnapshotURIs; // list of snapshot id's to link sessions to

    // Layer/device specific parameters (key/value) for this volume (serializable!)
    private Map<String, Object> parameters = new HashMap<String, Object>();

    public static final String PARAM_VARRAY_CHANGE_NEW_VAARAY_ID = "varrayChangeNewVArrayId";
    public static final String PARAM_VPOOL_CHANGE_EXISTING_VOLUME_ID = "vpoolChangeExistingVolumeId";
    public static final String PARAM_VPOOL_CHANGE_NEW_VPOOL_ID = "vpoolChangeNewVpoolId";
    public static final String PARAM_VPOOL_CHANGE_OLD_VPOOL_ID = "vpoolChangeOldVpoolId";
    public static final String PARAM_IS_COPY_SOURCE_ID = "isCopySourceId";
    public static final String PARAM_DO_NOT_DELETE_VOLUME = "doNotDeleteVolume";
    public static final String PARAM_MIGRATION_SUSPEND_BEFORE_COMMIT = "migrationSuspendBeforeCommit";
    public static final String PARAM_MIGRATION_SUSPEND_BEFORE_DELETE_SOURCE = "migrationSuspendBeforeDeleteSource";

    public VolumeDescriptor(Type type,
            URI deviceURI, URI volumeURI, URI poolURI, URI consistencyGroupURI,
            VirtualPoolCapabilityValuesWrapper capabilities, Long volumeSize) {
        this(type, deviceURI, volumeURI, poolURI, consistencyGroupURI, capabilities);
        this.volumeSize = volumeSize;
    }

    public VolumeDescriptor(Type type,
            URI deviceURI, URI volumeURI, URI poolURI, URI consistencyGroupURI, URI migrationId,
            VirtualPoolCapabilityValuesWrapper capabilities) {
        this(type, deviceURI, volumeURI, poolURI, consistencyGroupURI, capabilities);
        setMigrationId(migrationId);
    }

    public VolumeDescriptor(Type type,
            URI deviceURI, URI volumeURI, URI poolURI, URI consistencyGroupURI,
            VirtualPoolCapabilityValuesWrapper capabilities) {
        this.type = type;
        this.deviceURI = deviceURI;
        this.volumeURI = volumeURI;
        this.poolURI = poolURI;
        this.capabilitiesValues = capabilities;
        this.consistencyGroup = consistencyGroupURI;
    }

    public VolumeDescriptor(Type type,
            URI deviceURI, URI volumeURI, URI poolURI,
            VirtualPoolCapabilityValuesWrapper capabilities) {
        this(type, deviceURI, volumeURI, poolURI, null, capabilities);
    }

    /**
     * constructor for snapshot session volume descriptor
     * 
     * @param type type of volume desccriptor (snapshot session)
     * @param deviceURI storage controller id
     * @param volumeURI BlockSnapshotSession id
     * @param poolURI virtual pool id
     * @param consistencyGroupURI consistency group id
     * @param capabilities capabilities object
     * @param snapSessionSnapshotURIs list of snapshot ids for create and link to snapshot
     */
    public VolumeDescriptor(Type type,
            URI deviceURI, URI volumeURI, URI poolURI, URI consistencyGroupURI,
            VirtualPoolCapabilityValuesWrapper capabilities, List<List<URI>> snapSessionSnapshotURIs) {
        this(type, deviceURI, volumeURI, poolURI, consistencyGroupURI, capabilities);
        this.setSnapSessionSnapshotURIs(snapSessionSnapshotURIs);
    }

    /**
     * Returns all the descriptors of a given type.
     * 
     * @param descriptors
     *            List<VolumeDescriptor> input list
     * @param type
     *            enum Type
     * @return returns list elements matching given type
     */
    static public List<VolumeDescriptor> getDescriptors(List<VolumeDescriptor> descriptors, Type type) {
        List<VolumeDescriptor> list = new ArrayList<VolumeDescriptor>();
        for (VolumeDescriptor descriptor : descriptors) {
            if (descriptor.getType() == type) {
                list.add(descriptor);
            }
        }
        return list;
    }

    /**
     * Return a map of device URI to a list of descriptors in that device.
     * 
     * @param descriptors
     *            List<VolumeDescriptors>
     * @return Map of device URI to List<VolumeDescriptors> in that device
     */
    static public Map<URI, List<VolumeDescriptor>> getDeviceMap(List<VolumeDescriptor> descriptors) {
        HashMap<URI, List<VolumeDescriptor>> poolMap = new HashMap<URI, List<VolumeDescriptor>>();
        for (VolumeDescriptor desc : descriptors) {
            if (poolMap.get(desc.getDeviceURI()) == null) {
                poolMap.put(desc.getDeviceURI(), new ArrayList<VolumeDescriptor>());
            }
            poolMap.get(desc.getDeviceURI()).add(desc);
        }
        return poolMap;
    }

    /**
     * Return a map of pool URI to a list of descriptors in that pool.
     * 
     * @param descriptors
     *            List<VolumeDescriptors>
     * @return Map of pool URI to List<VolumeDescriptors> in that pool
     */
    static public Map<URI, List<VolumeDescriptor>> getPoolMap(List<VolumeDescriptor> descriptors) {
        HashMap<URI, List<VolumeDescriptor>> poolMap = new HashMap<URI, List<VolumeDescriptor>>();
        for (VolumeDescriptor desc : descriptors) {
            if (poolMap.get(desc.getPoolURI()) == null) {
                poolMap.put(desc.getPoolURI(), new ArrayList<VolumeDescriptor>());
            }
            poolMap.get(desc.getPoolURI()).add(desc);
        }
        return poolMap;
    }

    /**
     * Return a map of pool URI to a list of descriptors in that pool of each size.
     * 
     * @param descriptors
     *            List<VolumeDescriptors>
     * @return Map of pool URI to a map of identical sized volumes to List<VolumeDescriptors> in that pool of that size
     */
    static public Map<URI, Map<Long, List<VolumeDescriptor>>> getPoolSizeMap(List<VolumeDescriptor> descriptors) {
        Map<URI, Map<Long, List<VolumeDescriptor>>> poolSizeMap = new HashMap<URI, Map<Long, List<VolumeDescriptor>>>();
        for (VolumeDescriptor desc : descriptors) {

            // If the outside pool map doesn't exist, create it.
            if (poolSizeMap.get(desc.getPoolURI()) == null) {
                poolSizeMap.put(desc.getPoolURI(), new HashMap<Long, List<VolumeDescriptor>>());
            }

            // If the inside size map doesn't exist, create it.
            if (poolSizeMap.get(desc.getPoolURI()).get(desc.getVolumeSize()) == null) {
                poolSizeMap.get(desc.getPoolURI()).put(desc.getVolumeSize(), new ArrayList<VolumeDescriptor>());
            }

            // Add volume to the list
            poolSizeMap.get(desc.getPoolURI()).get(desc.getVolumeSize()).add(desc);
        }

        return poolSizeMap;
    }

    /**
     * Return a List of URIs for the volumes.
     * 
     * @param descriptors
     *            List<VolumeDescriptors>
     * @return List<URI> of volumes in the input list
     */
    public static List<URI> getVolumeURIs(List<VolumeDescriptor> descriptors) {
        Set<URI> volumeURIs = new HashSet<>();
        for (VolumeDescriptor desc : descriptors) {
            volumeURIs.add(desc.getVolumeURI());
        }
        List<URI> volumeList = new ArrayList<>();
        volumeList.addAll(volumeURIs);
        return volumeList;
    }

    /**
     * Filter a list of VolumeDescriptors by type(s).
     * 
     * @param descriptors
     *            -- Original list.
     * @param inclusive
     *            -- Types to be included (or null if not used).
     * @param exclusive
     *            -- Types to be excluded (or null if not used).
     * @return List<VolumeDescriptor>
     */
    public static List<VolumeDescriptor> filterByType(
            List<VolumeDescriptor> descriptors,
            Type[] inclusive, Type[] exclusive) {
        List<VolumeDescriptor> result = new ArrayList<VolumeDescriptor>();
        if (descriptors == null) {
            return result;
        }

        HashSet<Type> included = new HashSet<Type>();
        if (inclusive != null) {
            included.addAll(Arrays.asList(inclusive));
        }
        HashSet<Type> excluded = new HashSet<Type>();
        if (exclusive != null) {
            excluded.addAll(Arrays.asList(exclusive));
        }
        for (VolumeDescriptor desc : descriptors) {
            if (excluded.contains(desc.getType())) {
                continue;
            }
            if (included.isEmpty() || included.contains(desc.getType())) {
                result.add(desc);
            }
        }
        return result;
    }

    public static List<VolumeDescriptor> filterByType(
            List<VolumeDescriptor> descriptors,
            Type... inclusive) {
        return filterByType(descriptors, inclusive, null);
    }

    /**
     * Helper method to retrieve the vpool change volume hiding in the volume descriptors
     * 
     * @param descriptors
     *            list of volumes
     * @return URI of the vpool change volume
     */
    public static URI getVirtualPoolChangeVolume(List<VolumeDescriptor> descriptors) {
        if (descriptors != null) {
            for (VolumeDescriptor volumeDescriptor : descriptors) {
                if (volumeDescriptor.getParameters() != null) {
                    if ((URI) volumeDescriptor.getParameters().get(VolumeDescriptor.PARAM_VPOOL_CHANGE_EXISTING_VOLUME_ID) != null) {
                        return (URI) volumeDescriptor.getParameters().get(VolumeDescriptor.PARAM_VPOOL_CHANGE_EXISTING_VOLUME_ID);
                    }
                }
            }
        }
        return null;
    }

    /**
     * Helper method to find the change vpool using a single descriptor
     * 
     * @param descriptor
     *            Volume descriptor to use
     * @return URI of the change vpool
     */
    public static URI getVirtualPoolChangeVolume(VolumeDescriptor descriptor) {
        if (descriptor != null) {
            List<VolumeDescriptor> descriptors = new ArrayList<VolumeDescriptor>();
            descriptors.add(descriptor);
            return getVirtualPoolChangeVolume(descriptors);
        }
        return null;
    }

    /**
     * Helper method to retrieve the change vpool volume hiding in the volume descriptors and
     * to find the old vpool.
     * 
     * @param descriptors
     *            list of volumes
     * @return Map<URI,URI> of the vpool change volume and the old vpool associated to it.
     */
    public static Map<URI, URI> createVolumeToOldVpoolMap(List<VolumeDescriptor> descriptors) {
        Map<URI, URI> volumesToOldVpoolMap = new HashMap<URI, URI>();
        if (descriptors != null) {
            for (VolumeDescriptor volumeDescriptor : descriptors) {
                if (volumeDescriptor.getParameters() != null) {
                    if (volumeDescriptor.getParameters().get(VolumeDescriptor.PARAM_VPOOL_CHANGE_EXISTING_VOLUME_ID) != null) {
                        URI volumeURI = (URI) volumeDescriptor.getParameters().get(VolumeDescriptor.PARAM_VPOOL_CHANGE_EXISTING_VOLUME_ID);
                        URI oldVpoolURI = (URI) volumeDescriptor.getParameters().get(VolumeDescriptor.PARAM_VPOOL_CHANGE_OLD_VPOOL_ID);
                        volumesToOldVpoolMap.put(volumeURI, oldVpoolURI);
                    }
                }
            }
        }
        return volumesToOldVpoolMap;
    }
    
    /**
     * Helper method to retrieve the change vpool volume hiding in the volume descriptors and
     * to find the new vpool.
     * 
     * @param descriptors list of volumes
     * @return Map<URI,URI> of the vpool change volume and the old vpool associated to it.
     */
    public static Map<URI, URI> createVolumeToNewVpoolMap(List<VolumeDescriptor> descriptors) {
        Map<URI, URI> volumesToNewVpoolMap = new HashMap<URI, URI>();
        if (descriptors != null) {
            for (VolumeDescriptor volumeDescriptor : descriptors) {
                if (volumeDescriptor.getParameters() != null) {
                    if (volumeDescriptor.getParameters().get(VolumeDescriptor.PARAM_VPOOL_CHANGE_EXISTING_VOLUME_ID) != null) {
                        URI volumeURI = (URI) volumeDescriptor.getParameters().get(VolumeDescriptor.PARAM_VPOOL_CHANGE_EXISTING_VOLUME_ID);
                        URI newVpoolURI = (URI) volumeDescriptor.getParameters().get(VolumeDescriptor.PARAM_VPOOL_CHANGE_NEW_VPOOL_ID);
                        volumesToNewVpoolMap.put(volumeURI, newVpoolURI);
                    }
                }
            }
        }
        return volumesToNewVpoolMap;
    }

    /**
     * Helper method to retrieve the suspension setting hiding in the volume descriptors.
     * 
     * @param descriptors
     *            list of volumes
     * @return boolean if we should suspend before commit
     */
    public static boolean getMigrationSuspendBeforeCommit(List<VolumeDescriptor> descriptors) {
        return isPropertyEnabled(descriptors, VolumeDescriptor.PARAM_MIGRATION_SUSPEND_BEFORE_COMMIT);
    }

    /**
     * Helper method to retrieve the suspension setting hiding in the volume descriptors.
     * 
     * @param descriptors
     *            list of volumes
     * @return boolean if we should suspend before deleting the source volumes
     */
    public static boolean getMigrationSuspendBeforeDeleteSource(List<VolumeDescriptor> descriptors) {
        return isPropertyEnabled(descriptors, VolumeDescriptor.PARAM_MIGRATION_SUSPEND_BEFORE_DELETE_SOURCE);
    }

    /**
     * Helper that checks any boolean property in the volume descriptor. False is default response.
     * 
     * @param descriptors
     *            descriptor list
     * @param param
     *            string of parameter to check
     * @return true if the property is set and true, false otherwise
     */
    private static boolean isPropertyEnabled(List<VolumeDescriptor> descriptors, String param) {
        if (descriptors != null) {
            for (VolumeDescriptor volumeDescriptor : descriptors) {
                if (volumeDescriptor.getParameters() != null) {
                    if (volumeDescriptor.getParameters().get(param) != null) {
                        return (Boolean) volumeDescriptor.getParameters().get(param);
                    }
                }
            }
        }
        return false;
    }

    /**
     * Sorts the descriptors using the natural order of the enum type
     * defined at the top of the class.
     * 
     * @param descriptors
     *            VolumeDescriptors to sort
     */
    public static void sortByType(List<VolumeDescriptor> descriptors) {
        Collections.sort(descriptors, new Comparator<VolumeDescriptor>() {
            @Override
            public int compare(VolumeDescriptor vd1, VolumeDescriptor vd2) {
                return vd1.getType().getOrder() - vd2.getType().getOrder();
            }
        });
    }

    /**
     * Returns all descriptors that have the PARAM_DO_NOT_DELETE_VOLUME flag set to true.
     * 
     * @param descriptors
     *            List of descriptors to check
     * @return all descriptors that have the PARAM_DO_NOT_DELETE_VOLUME flag set to true
     */
    public static List<VolumeDescriptor> getDoNotDeleteDescriptors(List<VolumeDescriptor> descriptors) {
        List<VolumeDescriptor> doNotDeleteDescriptors = new ArrayList<VolumeDescriptor>();
        if (descriptors != null && !descriptors.isEmpty()) {
            for (VolumeDescriptor descriptor : descriptors) {
                if (descriptor.getParameters() != null
                        && descriptor.getParameters().get(VolumeDescriptor.PARAM_DO_NOT_DELETE_VOLUME) != null
                        && descriptor.getParameters().get(VolumeDescriptor.PARAM_DO_NOT_DELETE_VOLUME).equals(Boolean.TRUE)) {
                    doNotDeleteDescriptors.add(descriptor);
                }
            }
        }
        return doNotDeleteDescriptors;
    }

    @Override
    public String toString() {
        return "VolumeDescriptor [_type=" + getType() + ", _deviceURI="
                + getDeviceURI() + ", _volumeURI=" + getVolumeURI() + ", _poolURI="
                + getPoolURI() + ", _consistencyGroup=" + getConsistencyGroupURI() +
                ", _capabilitiesValues=" + getCapabilitiesValues() + ", parameters="
                + parameters + ", size=" + getVolumeSize() + "]";
    }

    public String toString(Volume volume) {
        return "VolumeDescriptor [_type=" + getType() + ", _deviceURI="
                + getDeviceURI() + ", _poolURI="
                + getPoolURI() + ", _consistencyGroup=" + getConsistencyGroupURI() +
                ", _capabilitiesValues=" + getCapabilitiesValues()
                + ", parameters=" + parameters + ", volume=" +
                volume.toString() + ", size=" + getVolumeSize() + "]";
    }

    public Type getType() {
        return type;
    }

    public void setType(Type type) {
        this.type = type;
    }

    public URI getDeviceURI() {
        return deviceURI;
    }

    public void setDeviceURI(URI deviceURI) {
        this.deviceURI = deviceURI;
    }

    public URI getVolumeURI() {
        return volumeURI;
    }

    public void setVolumeURI(URI volumeURI) {
        this.volumeURI = volumeURI;
    }

    public URI getPoolURI() {
        return poolURI;
    }

    public void setPoolURI(URI poolURI) {
        this.poolURI = poolURI;
    }

    public VirtualPoolCapabilityValuesWrapper getCapabilitiesValues() {
        return capabilitiesValues;
    }

    public void setCapabilitiesValues(VirtualPoolCapabilityValuesWrapper capabilitiesValues) {
        this.capabilitiesValues = capabilitiesValues;
    }

    public Map<String, Object> getParameters() {
        return parameters;
    }

    public void setParameters(Map<String, Object> parameters) {
        this.parameters = parameters;
    }

    public URI getConsistencyGroupURI() {
        return consistencyGroup;
    }

    public void setConsistencyGroupURI(URI consistencyGroupURI) {
        this.consistencyGroup = consistencyGroupURI;
    }

    public Long getVolumeSize() {
        return volumeSize;
    }

    public void setVolumeSize(Long _volumeSize) {
        this.volumeSize = _volumeSize;
    }

    public URI getMigrationId() {
        return migrationId;
    }

    public void setMigrationId(URI _migrationId) {
        this.migrationId = _migrationId;
    }

    public URI getComputeResource() {
        return computeResource;
    }

    public void setComputeResource(URI _computeResource) {
        this.computeResource = _computeResource;
    }

    /**
     * @return the snapSessionSnapshotURIs
     */
    public List<List<URI>> getSnapSessionSnapshotURIs() {
        return snapSessionSnapshotURIs;
}

    /**
     * @param snapSessionSnapshotURIs the snapSessionSnapshotURIs to set
     */
    public void setSnapSessionSnapshotURIs(List<List<URI>> snapSessionSnapshotURIs) {
        this.snapSessionSnapshotURIs = snapSessionSnapshotURIs;
    }
}
