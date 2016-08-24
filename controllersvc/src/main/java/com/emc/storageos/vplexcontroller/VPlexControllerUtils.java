/*
 * Copyright (c) 2015 EMC Corporation
 * All Rights Reserved
 */
package com.emc.storageos.vplexcontroller;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.emc.storageos.cinder.CinderConstants;
import com.emc.storageos.db.client.DbClient;
import com.emc.storageos.db.client.URIUtil;
import com.emc.storageos.db.client.constraint.AlternateIdConstraint;
import com.emc.storageos.db.client.constraint.URIQueryResultList;
import com.emc.storageos.db.client.model.DataObject;
import com.emc.storageos.db.client.model.DiscoveredDataObject;
import com.emc.storageos.db.client.model.ExportMask;
import com.emc.storageos.db.client.model.Initiator;
import com.emc.storageos.db.client.model.StoragePort;
import com.emc.storageos.db.client.model.StorageProvider;
import com.emc.storageos.db.client.model.StorageSystem;
import com.emc.storageos.db.client.model.StringMap;
import com.emc.storageos.db.client.model.Volume;
import com.emc.storageos.db.client.util.CommonTransformerFunctions;
import com.emc.storageos.db.client.util.NullColumnValueGetter;
import com.emc.storageos.db.exceptions.DatabaseException;
import com.emc.storageos.exceptions.DeviceControllerException;
import com.emc.storageos.networkcontroller.impl.NetworkDeviceController;
import com.emc.storageos.recoverpoint.utils.WwnUtils;
import com.emc.storageos.util.ConnectivityUtil;
import com.emc.storageos.util.ExportUtils;
import com.emc.storageos.util.NetworkUtil;
import com.emc.storageos.util.VPlexUtil;
import com.emc.storageos.volumecontroller.ControllerException;
import com.emc.storageos.volumecontroller.impl.utils.ExportMaskUtils;
import com.emc.storageos.vplex.api.VPlexApiClient;
import com.emc.storageos.vplex.api.VPlexApiException;
import com.emc.storageos.vplex.api.VPlexApiFactory;
import com.emc.storageos.vplex.api.VPlexPortInfo;
import com.emc.storageos.vplex.api.VPlexResourceInfo;
import com.emc.storageos.vplex.api.VPlexStorageViewInfo;
import com.emc.storageos.vplex.api.VPlexStorageVolumeInfo;
import com.emc.storageos.vplex.api.VPlexTargetInfo;
import com.google.common.base.Joiner;
import com.google.common.collect.Collections2;

public class VPlexControllerUtils {
    // logger reference.
    private static final Logger log = LoggerFactory
            .getLogger(VPlexControllerUtils.class);

    private static final String VPLEX = "vplex";

    /**
     * Get a DataObject. Throw exception if not found or inactive.
     * 
     * @param clazz
     * @param id
     * @return
     * @throws ControllerException
     */
    protected static <T extends DataObject> T getDataObject(Class<T> clazz, URI id, DbClient dbClient)
            throws DeviceControllerException {
        try {
            T object = null;

            if (id != null) {
                object = dbClient.queryObject(clazz, id);
                if (object == null) {
                    throw VPlexApiException.exceptions.getDataObjectFailedNotFound(
                            clazz.getSimpleName(), id.toString());
                }
                if (object.getInactive()) {
                    log.info("database object is inactive: " + object.getId().toString());
                    throw VPlexApiException.exceptions.getDataObjectFailedInactive(id.toString());
                }
            }
            return object;
        } catch (DatabaseException ex) {
            throw VPlexApiException.exceptions.getDataObjectFailedExc(id.toString(), ex);
        }
    }

    /**
     * Get the HTTP client for making requests to the passed VPlex management server.
     * 
     * @param vplexApiFactory A reference to the VPlex API factory.
     * @param vplexMnmgtSvr A VPlex management server.
     * @param dbClient A reference to a DB client.
     * 
     * @return A reference to the VPlex API HTTP client.
     * @throws URISyntaxException
     */
    public static VPlexApiClient getVPlexAPIClient(VPlexApiFactory vplexApiFactory,
            StorageProvider vplexMnmgtSvr, DbClient dbClient) throws URISyntaxException {
        URI vplexEndpointURI = new URI("https", null, vplexMnmgtSvr.getIPAddress(),
                vplexMnmgtSvr.getPortNumber(), "/", null, null);

        VPlexApiClient client = vplexApiFactory.getClient(vplexEndpointURI,
                vplexMnmgtSvr.getUserName(), vplexMnmgtSvr.getPassword());
        return client;
    }

    /**
     * Get the HTTP client for making requests to the passed VPlex storage system.
     * 
     * @param vplexApiFactory A reference to the VPlex API factory.
     * @param vplexSystem The VPlex storage system.
     * @param dbClient A reference to a DB client.
     * 
     * @return A reference to the VPlex API HTTP client.
     * @throws URISyntaxException
     */
    public static VPlexApiClient getVPlexAPIClient(VPlexApiFactory vplexApiFactory,
            StorageSystem vplexSystem, DbClient dbClient) throws URISyntaxException {
        // Create the URI to access the VPlex Management Server based
        // on the IP and port for the active provider for the passed
        // VPlex system.
        StorageProvider activeMgmntSvr = null;
        URI activeMgmntSvrURI = vplexSystem.getActiveProviderURI();
        if (!NullColumnValueGetter.isNullURI(activeMgmntSvrURI)) {
            activeMgmntSvr = dbClient.queryObject(StorageProvider.class, activeMgmntSvrURI);
        }

        if (activeMgmntSvr == null) {
            log.error("No active management server for VPLEX system {}", vplexSystem.getId());
            throw VPlexApiException.exceptions.connectionFailure(vplexSystem.getId().toString());
        }

        return getVPlexAPIClient(vplexApiFactory, activeMgmntSvr, dbClient);
    }

    /**
     * Get the HTTP client for making requests to the passed VPlex storage system URI.
     * Performs some helpful validation on the requested VPLEX system before returning
     * successfully.
     * 
     * @param vplexApiFactory A reference to the VPlex API factory.
     * @param vplexSystemUri The VPlex storage system URI.
     * @param dbClient A reference to a DB client.
     * 
     * @return A reference to the VPlex API HTTP client.
     * @throws URISyntaxException
     */
    public static VPlexApiClient getVPlexAPIClient(VPlexApiFactory vplexApiFactory,
            URI vplexUri, DbClient dbClient) throws URISyntaxException {

        if (vplexUri == null) {
            log.error("The provided VPLEX Storage System URI was null.");
            throw VPlexApiException.exceptions.vplexUriIsNull();
        }

        StorageSystem vplex = dbClient.queryObject(StorageSystem.class, vplexUri);

        if (vplex == null) {
            log.error("No VPLEX Storage System was found with URI {}.", vplexUri.toString());
            throw VPlexApiException.exceptions.vplexSystemNotFound(vplexUri.toString());
        }

        if (!vplex.getSystemType().equals(DiscoveredDataObject.Type.vplex.name())) {
            log.error("The Storage System (of type {}) with URI {} is not a VPLEX system.",
                    vplex.getSystemType(), vplexUri.toString());
            throw VPlexApiException.exceptions.invalidStorageSystemType(
                    vplex.getSystemType(), vplexUri.toString());
        }

        return getVPlexAPIClient(vplexApiFactory, vplex, dbClient);
    }

    /**
     * Determines the cluster name for a VPlex virtual volume based on the
     * volume's virtual array.
     * 
     * @param dbClient db client
     * @param vplexVolume The VPlex volume whose cluster we want to find.
     * @return The VPlex cluster name
     * @throws Exception
     * @throws URISyntaxException
     */
    public static String getVPlexClusterName(DbClient dbClient, Volume vplexVolume) throws Exception {
        // Get the virtual array from the vplex virtual volume. This will be used
        // to determine the volume's vplex cluster.
        URI vaURI = vplexVolume.getVirtualArray();
        URI vplexURI = vplexVolume.getStorageController();
        return getVPlexClusterName(dbClient, vaURI, vplexURI);
    }
    
    /**
     * Determines the cluster name based on the volume's virtual array.
     * 
     * @param dbClient db client
     * @param vplexURI The vplex system URI
     * @param vaURI The virtual array URI
     * @return The VPlex cluster name
     * @throws Exception
     * @throws URISyntaxException
     */
    public static String getVPlexClusterName(DbClient dbClient, URI vaURI, URI vplexURI) throws Exception {
        String clusterName = null;
        
        // Get the vplex storage system so we can a handle on the vplex client
        StorageSystem vplexSystem = getDataObject(StorageSystem.class, vplexURI, dbClient);
        VPlexApiClient client = null;

        try {
            client = VPlexControllerUtils.getVPlexAPIClient(VPlexApiFactory.getInstance(), vplexSystem, dbClient);
        } catch (URISyntaxException e) {
            throw VPlexApiException.exceptions.connectionFailure(vplexURI.toString());
        }

        String vplexCluster = ConnectivityUtil.getVplexClusterForVarray(vaURI, vplexSystem.getId(), dbClient);
        if (vplexCluster.equals(ConnectivityUtil.CLUSTER_UNKNOWN)) {
            throw new Exception("Unable to find VPLEX cluster for the varray " + vaURI);
        }

        clusterName = client.getClusterName(vplexCluster);

        return clusterName;
    }

    /**
     * Returns the cluster name (free form, user-configurable)
     * for a given VPLEX cluster id (either "1" or "2").
     * 
     * @param clusterId the cluster id (either "1" or "2")
     * @param vplexUri URI of the VPLEX to look at
     * @param dbClient a database client instance
     * 
     * @return the cluster name as configured by the user, or null
     *         if it couldn't be determined
     */
    public static String getClusterNameForId(String clusterId, URI vplexUri, DbClient dbClient) {

        String clusterName = null;
        VPlexApiClient client = null;

        try {
            VPlexApiFactory vplexApiFactory = VPlexApiFactory.getInstance();
            client = VPlexControllerUtils.getVPlexAPIClient(vplexApiFactory, vplexUri, dbClient);
        } catch (URISyntaxException e) {
            log.error("cannot load vplex api client", e);
        }

        if (null != client) {
            clusterName = client.getClusterName(clusterId);
        }

        log.info("VPLEX cluster name for cluster id {} is {}", clusterId, clusterName);
        return clusterName;
    }

    /**
     * Returns a VPlexResourceInfo object for the given device name based
     * on its virtual volume type (local or distributed).
     * 
     * @param deviceName the name of the device
     * @param virtualVolumeType the type of virtual volume (local or distributed)
     * @param vplexUri the URI of the VPLEX system
     * @param dbClient a reference to the database client
     * 
     * @return a VPlexResourceInfo object for the device name
     * @throws VPlexApiException
     */
    public static VPlexResourceInfo getDeviceInfo(String deviceName, String virtualVolumeType,
            URI vplexUri, DbClient dbClient) throws VPlexApiException {
        VPlexResourceInfo device = null;
        VPlexApiClient client = null;

        try {
            VPlexApiFactory vplexApiFactory = VPlexApiFactory.getInstance();
            client = VPlexControllerUtils.getVPlexAPIClient(vplexApiFactory, vplexUri, dbClient);
        } catch (URISyntaxException e) {
            log.error("cannot load vplex api client", e);
        }

        if (null != client) {
            device = client.getDeviceStructure(deviceName, virtualVolumeType);
        }

        return device;
    }

    /**
     * Returns a Map of lowest-level storage-volume resource's WWN to its VPlexStorageVolumeInfo
     * object for a given device name, virtual volume type, and cluster name. If
     * hasMirror is true, this indicates the top-level device is composed of a
     * RAID-1 mirror, so there's an extra layers of components to traverse in finding
     * the lowest-level storage-volume resources.
     * 
     * @param deviceName the name of the top-level device to look at
     * @param virtualVolumeType the type of virtual volume (local or distributed)
     * @param clusterName the cluster name
     * @param hasMirror indicates if the top-level device is a RAID-1 mirror
     * @param vplexUri the URI of the VPLEX system
     * @param dbClient a reference to the database client
     * 
     * @return a map of WWNs to VPlexStorageVolumeInfo objects
     * @throws VPlexApiException
     */
    public static Map<String, VPlexStorageVolumeInfo> getStorageVolumeInfoForDevice(
            String deviceName, String virtualVolumeType, String clusterName,
            boolean hasMirror, URI vplexUri, DbClient dbClient) throws VPlexApiException {

        Map<String, VPlexStorageVolumeInfo> storageVolumeInfo = null;
        VPlexApiClient client = null;

        try {
            VPlexApiFactory vplexApiFactory = VPlexApiFactory.getInstance();
            client = VPlexControllerUtils.getVPlexAPIClient(vplexApiFactory, vplexUri, dbClient);
        } catch (URISyntaxException e) {
            log.error("cannot load vplex api client", e);
        }

        if (null != client) {
            storageVolumeInfo = client.getStorageVolumeInfoForDevice(
                    deviceName, virtualVolumeType, clusterName, hasMirror);
        }

        log.info("Backend storage volume wwns for {} are {}", deviceName, storageVolumeInfo);
        return storageVolumeInfo;
    }

    /**
     * Returns the top-level supporting device name for a given storage volume native id,
     * wwn, and backend array serial number.
     * 
     * @param volumeNativeId the storage volume's native id
     * @param wwn the storage volume's wwn
     * @param backendArraySerialNum the serial number of the backend array
     * @param vplexUri the URI of the VPLEX device
     * @param dbClient a reference to the database client
     * 
     * @return the name of the top level device for the given storage volume
     * @throws VPlexApiException
     */
    @Deprecated
    public static String getDeviceNameForStorageVolume(String volumeNativeId,
            String wwn, String backendArraySerialNum, URI vplexUri, DbClient dbClient)
            throws VPlexApiException {

        String deviceName = null;
        VPlexApiClient client = null;

        try {
            VPlexApiFactory vplexApiFactory = VPlexApiFactory.getInstance();
            client = VPlexControllerUtils.getVPlexAPIClient(vplexApiFactory, vplexUri, dbClient);
        } catch (URISyntaxException e) {
            log.error("cannot load vplex api client", e);
        }

        if (null != client) {
            deviceName = client.getDeviceForStorageVolume(volumeNativeId, wwn, backendArraySerialNum);
        }

        log.info("Device name for storage volume {} is {}", volumeNativeId, deviceName);
        return deviceName;
    }

    /**
     * Gets the list of ITLs from the volume extensions
     * 
     * @param volume
     * @return
     */
    public static List<String> getVolumeITLs(Volume volume) {
        List<String> itlList = new ArrayList<>();

        StringMap extensions = volume.getExtensions();
        if (null != extensions) {
            Set<String> keys = extensions.keySet();
            for (String key : keys)
            {
                if (key.startsWith(CinderConstants.PREFIX_ITL)) {
                    itlList.add(extensions.get(key));
                }
            }
        }

        return itlList;
    }

    /**
     * Returns true if the Initiator object represents a VPLEX StoragePort.
     * 
     * @param initiator the Initiator to test
     * @param dbClient a reference to the database client
     * 
     * @return true if the Initiator object represents a VPLEX StoragePort
     */
    public static boolean isVplexInitiator(Initiator initiator, DbClient dbClient) {
        StoragePort port = NetworkUtil.getStoragePort(initiator.getInitiatorPort(), dbClient);
        if (null != port) {
            StorageSystem vplex = dbClient.queryObject(StorageSystem.class, port.getStorageDevice());
            if (null != vplex && VPLEX.equals(vplex.getSystemType())) {
                return true;
            }
        }

        return false;
    }

    /**
     * Returns a Map of distributed device component context
     * paths from the VPLEX API to VPLEX cluster names.
     * 
     * @param vplexUri the VPLEX to query
     * @param dbClient a reference to the database client
     * @return a Map of distributed device component context
     *         paths from the VPLEX API to VPLEX cluster names
     * 
     * @throws VPlexApiException
     */
    public static Map<String, String> getDistributedDevicePathToClusterMap(
            URI vplexUri, DbClient dbClient) throws VPlexApiException {
        VPlexApiClient client = null;

        try {
            VPlexApiFactory vplexApiFactory = VPlexApiFactory.getInstance();
            client = VPlexControllerUtils.getVPlexAPIClient(vplexApiFactory, vplexUri, dbClient);
        } catch (URISyntaxException e) {
            log.error("cannot load vplex api client", e);
        }

        Map<String, String> distributedDevicePathToClusterMap = Collections.emptyMap();
        if (null != client) {
            distributedDevicePathToClusterMap =
                    client.getDistributedDevicePathToClusterMap();
        }

        return distributedDevicePathToClusterMap;
    }
    
    /**
     * Validates that the underlying structure of the given device name
     * satisfies the constraints for compatibility with ViPR.  Used for
     * validating unmanaged VPLEX volumes before ingestion.
     * 
     * @param deviceName the device to validate
     * @param vplexUri the VPLEX to query
     * @param dbClient a reference to the database client
     * @throws VPlexApiException if the device structure is incompatible with ViPR
     */
    public static void validateSupportingDeviceStructure(String deviceName, 
            URI vplexUri, DbClient dbClient) throws VPlexApiException {
        VPlexApiClient client = null;

        try {
            VPlexApiFactory vplexApiFactory = VPlexApiFactory.getInstance();
            client = VPlexControllerUtils.getVPlexAPIClient(vplexApiFactory, vplexUri, dbClient);
        } catch (URISyntaxException e) {
            log.error("cannot load vplex api client", e);
        }

        if (null != client) {
            String drillDownResponse = client.getDrillDownInfoForDevice(deviceName);
            if (!VPlexUtil.isDeviceStructureValid(deviceName, drillDownResponse)) {
                throw VPlexApiException.exceptions.deviceStructureIsIncompatibleForIngestion(drillDownResponse);
            }
        } else {
            throw VPlexApiException.exceptions.failedToExecuteDrillDownCommand(
                    deviceName, "cannot load vplex api client");
        }
    }

    /**
     * Creates a Map of target-port to port-wwn values for a VPLEX device
     * For example: target port - P0000000046E01E80-A0-FC02 
     *              port wwn    - 0x50001442601e8002
     * 
     * @param client a reference to the VPlexApiClient to query for port info
     * @param clusterName the cluster for this port name search; port names can potentially be different
     *                    across clusters.
     * @return a Map of target-port to port-wwn values for a VPLEX device
     */
    public static Map<String, String> getTargetPortToPwwnMap(VPlexApiClient client, String clusterName) {

        Map<String, String> targetPortToPwwnMap = new HashMap<String, String>();
        List<VPlexTargetInfo> targetInfos = client.getTargetInfoForCluster(clusterName);
        if (targetInfos != null) {
            for (VPlexTargetInfo vplexTargetPortInfo : targetInfos) {
                if (null != vplexTargetPortInfo.getPortWwn()) {
                    targetPortToPwwnMap.put(vplexTargetPortInfo.getName(), vplexTargetPortInfo.getPortWwn());
                }
            }
        }

        log.info("target port map for cluster {} is {}", clusterName, targetPortToPwwnMap);
        return targetPortToPwwnMap;
    }

    /**
     * Refreshes the given ExportMask with the latest export information from the VPLEX.
     * 
     * This method follows the same basic logic as VmaxExportOperations.refreshExportMask, 
     * but adjusted for VPLEX storage views and other concepts.
     * 
     * @param dbClient a reference to the database client
     * @param storageView a reference to the VPlexStorageViewInfo for the ExportMask's mask name
     * @param exportMask the ExportMask to refresh
     * @param vplexClusterName the VPLEX cluster name on which to find the ExportMask
     * @param targetPortToPwwnMap a Map of VPLEX target port names to WWNs
     * @param networkDeviceController the NetworkDeviceController, used for refreshing the zoning map
     */
    public static void refreshExportMask(DbClient dbClient, VPlexStorageViewInfo storageView, 
            ExportMask exportMask, Map<String, String> targetPortToPwwnMap, NetworkDeviceController networkDeviceController) {
        refreshExportMask(dbClient, storageView, exportMask, targetPortToPwwnMap, networkDeviceController, false);
    }

    /**
     * Refreshes the given ExportMask with the latest export information from the VPLEX, with
     * an option to indicate whether or not the caller is a remove volume or initiator operation.
     * If removing volumes or initiators, we will not remove anything from the existing collections
     * so that any logic for data unavailability prevention can be made on the state before ViPR
     * attempts to make any changes to the storage view.
     * 
     * This method follows the same basic logic as VmaxExportOperations.refreshExportMask, 
     * but adjusted for VPLEX storage views and other concepts.
     * 
     * @param dbClient a reference to the database client
     * @param storageView a reference to the VPlexStorageViewInfo for the ExportMask's mask name
     * @param exportMask the ExportMask to refresh
     * @param vplexClusterName the VPLEX cluster name on which to find the ExportMask
     * @param targetPortToPwwnMap a Map of VPLEX target port names to WWNs
     * @param networkDeviceController the NetworkDeviceController, used for refreshing the zoning map
     * @param isRemoveOperation flag to indicate whether the caller is a operation that removes inits or vols
     */
    public static void refreshExportMask(DbClient dbClient, VPlexStorageViewInfo storageView, 
            ExportMask exportMask, Map<String, String> targetPortToPwwnMap, 
            NetworkDeviceController networkDeviceController, boolean isRemoveOperation) {
        try {

            if (null == exportMask || null == storageView || null == targetPortToPwwnMap || targetPortToPwwnMap.isEmpty()) {
                int portNameMapEntryCount = targetPortToPwwnMap != null ? targetPortToPwwnMap.size() : 0;
                String message = String.format("export mask was %s, storage view was %s, and port name to wwn map had %d entries",
                        exportMask, storageView, portNameMapEntryCount);
                log.error(message);
                if (null == storageView) {
                    throw new IllegalArgumentException("storage view could not be found on vplex device; " + message);
                } else {
                    throw new IllegalArgumentException("export mask refresh arguments are invalid: " + message);
                }
            }

            // Get volumes and initiators for the masking instance
            Map<String, Integer> discoveredVolumes = storageView.getWwnToHluMap();
            List<String> discoveredInitiators = storageView.getInitiatorPwwns();

            Set<String> existingVolumes = (exportMask.getExistingVolumes() != null) ?
                    exportMask.getExistingVolumes().keySet() : Collections.emptySet();
            Set<String> existingInitiators = (exportMask.getExistingInitiators() != null) ?
                    exportMask.getExistingInitiators() : Collections.emptySet();

            List<String> viprVolumes = new ArrayList<String>();
            if (exportMask.getVolumes() != null) {
                List<Volume> vols = dbClient.queryObject(Volume.class, URIUtil.toURIList(exportMask.getVolumes().keySet()));
                for (Volume volume : vols) {
                    viprVolumes.add(volume.getWWN());
                }
            }
            List<String> viprInits = new ArrayList<String>();
            if (exportMask.getInitiators() != null) {
                List<Initiator> inits = dbClient.queryObject(Initiator.class, URIUtil.toURIList(exportMask.getInitiators()));
                for (Initiator init : inits) {
                    viprInits.add(Initiator.normalizePort(init.getInitiatorPort()));
                }
            }

            String name = exportMask.getMaskName();

            log.info(String.format("%nExportMask %s in the ViPR database: ViPR Vols:{%s} ViPR Inits:{%s} Existing Inits:{%s} Existing Vols:{%s}%n", name,
                    Joiner.on(',').join(viprVolumes),
                    Joiner.on(',').join(viprInits),
                    Joiner.on(',').join(existingInitiators),
                    Joiner.on(',').join(existingVolumes)));

            log.info(String.format("StorageView %s discovered on VPLEX: Inits:{%s} Vols:{%s}%n", name,
                    Joiner.on(',').join(discoveredInitiators),
                    Joiner.on(',').join(discoveredVolumes.keySet())));

            // Check the initiators and update the lists as necessary
            boolean addInitiators = false;
            List<String> initiatorsToAdd = new ArrayList<String>();
            List<Initiator> initiatorIdsToAdd = new ArrayList<Initiator>();
            for (String port : discoveredInitiators) {
                String normalizedPort = Initiator.normalizePort(port);
                if (!exportMask.hasExistingInitiator(normalizedPort) &&
                        !exportMask.hasUserInitiator(normalizedPort)) {
                    initiatorsToAdd.add(normalizedPort);
                    Initiator existingInitiator = ExportUtils.getInitiator(Initiator.toPortNetworkId(port), dbClient);
                    if (existingInitiator != null) {
                        initiatorIdsToAdd.add(existingInitiator);
                    }
                    addInitiators = true;
                }
            }

            boolean removeInitiators = false;
            List<String> initiatorsToRemove = new ArrayList<String>();
            List<URI> initiatorIdsToRemove = new ArrayList<URI>();
            if (exportMask.getExistingInitiators() != null &&
                    !exportMask.getExistingInitiators().isEmpty()) {
                initiatorsToRemove.addAll(exportMask.getExistingInitiators());
                initiatorsToRemove.removeAll(discoveredInitiators);
            }

            // if init is in userAddedInitiators now, but also in existing initiators,
            // we should remove it from existing initiators
            List<String> initiatorsToRemoveFromExisting = new ArrayList<String>();
            if (!isRemoveOperation) {
                for (String initWwn : discoveredInitiators) {
                    Initiator managedInitiator = ExportUtils.getInitiator(Initiator.toPortNetworkId(initWwn), dbClient);
                    if ((exportMask.hasUserInitiator(initWwn) || 
                            (managedInitiator != null && exportMask.hasInitiator(managedInitiator.getId().toString())))
                            && exportMask.hasExistingInitiator(initWwn)) {
                        log.info("\texisting initiators contain id {}, but it is also in "
                                + "user added inits, removing from existing inits", initWwn);
                        initiatorsToRemoveFromExisting.add(initWwn);
                    }
                }
            }

            if (exportMask.getInitiators() != null &&
                    !exportMask.getInitiators().isEmpty()) {
                initiatorIdsToRemove.addAll(Collections2.transform(exportMask.getInitiators(),
                        CommonTransformerFunctions.FCTN_STRING_TO_URI));
                for (String port : discoveredInitiators) {
                    Initiator existingInitiator = ExportUtils.getInitiator(Initiator.toPortNetworkId(port), dbClient);
                    if (existingInitiator != null) {
                        initiatorIdsToRemove.remove(existingInitiator.getId());
                    }
                }
            }

            removeInitiators = !initiatorsToRemove.isEmpty() || !initiatorIdsToRemove.isEmpty()
                    || !initiatorsToRemoveFromExisting.isEmpty();

            // Check the volumes and update the lists as necessary
            Map<String, Integer> volumesToAdd = ExportMaskUtils.diffAndFindNewVolumes(exportMask, discoveredVolumes);
            boolean addVolumes = !volumesToAdd.isEmpty();

            boolean removeVolumes = false;
            List<String> volumesToRemoveFromExisting = new ArrayList<String>();
            if (exportMask.getExistingVolumes() != null &&
                    !exportMask.getExistingVolumes().isEmpty()) {
                volumesToRemoveFromExisting.addAll(exportMask.getExistingVolumes().keySet());
                volumesToRemoveFromExisting.removeAll(discoveredVolumes.keySet());
            }

            // if volume is in userAddedVolumes now, but also in existing volumes,
            // we should remove it from existing volumes
            if (!isRemoveOperation) {
                for (String wwn : discoveredVolumes.keySet()) {
                    if (exportMask.hasExistingVolume(wwn)) {
                        URIQueryResultList volumeList = new URIQueryResultList();
                        dbClient.queryByConstraint(AlternateIdConstraint.Factory.getVolumeWwnConstraint(wwn), volumeList);
                        if (volumeList.iterator().hasNext()) {
                            URI volumeURI = volumeList.iterator().next();
                            if (exportMask.hasVolume(volumeURI)) {
                                log.info("\texisting volumes contain wwn {}, but it is also in the "
                                        + "export mask's volumes, so removing from existing volumes", wwn);
                                volumesToRemoveFromExisting.add(wwn);
                            }
                        }
                    }
                }
            }
            removeVolumes = !volumesToRemoveFromExisting.isEmpty();

            // Grab the storage ports that have been allocated for this
            // existing mask and update them.
            List<String> storagePorts = storageView.getPorts();
            List<String> portWwns = new ArrayList<String>();
            for (String storagePort : storagePorts) {
                if (targetPortToPwwnMap.keySet().contains(storagePort)) {
                    portWwns.add(WwnUtils.convertWWN(targetPortToPwwnMap.get(storagePort), WwnUtils.FORMAT.COLON));
                }
            }
            List<String> storagePortURIs = ExportUtils.storagePortNamesToURIs(dbClient, portWwns);

            // Check the storagePorts and update the lists as necessary
            boolean addStoragePorts = false;
            List<String> storagePortsToAdd = new ArrayList<String>();
            if (exportMask.getStoragePorts() == null) {
                exportMask.setStoragePorts(new ArrayList<String>());
            }

            for (String portID : storagePortURIs) {
                if (!exportMask.getStoragePorts().contains(portID)) {
                    storagePortsToAdd.add(portID);
                    addStoragePorts = true;
                }
            }

            boolean removeStoragePorts = false;
            List<String> storagePortsToRemove = new ArrayList<String>();
            if (exportMask.getStoragePorts() != null &&
                    !exportMask.getStoragePorts().isEmpty()) {
                storagePortsToRemove.addAll(exportMask.getStoragePorts());
                storagePortsToRemove.removeAll(storagePortURIs);
                removeStoragePorts = !storagePortsToRemove.isEmpty();
            }

            log.info(
                    String.format("ExportMask %s refresh initiators; addToExisting:{%s} removeAndUpdateZoning:{%s} removeFromExistingOnly:{%s}%n",
                            name, Joiner.on(',').join(initiatorsToAdd),
                            Joiner.on(',').join(initiatorsToRemove), 
                            Joiner.on(',').join(initiatorsToRemoveFromExisting)));
            log.info(
                    String.format("ExportMask %s refresh volumes; addToExisting:{%s} removeFromExistingOnly:{%s}%n",
                            name, Joiner.on(',').join(volumesToAdd.keySet()),
                            Joiner.on(',').join(volumesToRemoveFromExisting)));
            log.info(
                    String.format("ExportMask %s refresh ports; add:{%s} remove:{%s}%n",
                            name, Joiner.on(',').join(storagePortsToAdd),
                            Joiner.on(',').join(storagePortsToRemove)));

            // Any changes indicated, then update the mask and persist it
            if (addInitiators || removeInitiators || addVolumes ||
                    removeVolumes || addStoragePorts || removeStoragePorts) {
                log.info("ExportMask refresh: There are changes to mask, updating it...\n");
                exportMask.removeFromExistingInitiators(initiatorsToRemove);
                // keeping this separate from initiatorsToRemove because we don't want a zoning update
                exportMask.removeFromExistingInitiators(initiatorsToRemoveFromExisting);
                if (initiatorIdsToRemove != null && !initiatorIdsToRemove.isEmpty()) {
                    exportMask.removeInitiators(dbClient.queryObject(Initiator.class, initiatorIdsToRemove));
                }
                List<Initiator> userAddedInitiators =
                        ExportMaskUtils.findIfInitiatorsAreUserAddedInAnotherMask(exportMask, initiatorIdsToAdd, dbClient);
                exportMask.addToUserCreatedInitiators(userAddedInitiators);
                exportMask.addToExistingInitiatorsIfAbsent(initiatorsToAdd);
                exportMask.addInitiators(initiatorIdsToAdd);
                exportMask.removeFromExistingVolumes(volumesToRemoveFromExisting);
                exportMask.addToExistingVolumesIfAbsent(volumesToAdd);
                exportMask.getStoragePorts().addAll(storagePortsToAdd);
                exportMask.getStoragePorts().removeAll(storagePortsToRemove);
                ExportMaskUtils.sanitizeExportMaskContainers(dbClient, exportMask);
                dbClient.updateObject(exportMask);
                log.info("ExportMask is now:\n" + exportMask.toString());
            } else {
                log.info("ExportMask refresh: There are no changes to the mask\n");
            }
            networkDeviceController.refreshZoningMap(exportMask,
                    initiatorsToRemove, Collections.emptyList(),
                    (addInitiators || removeInitiators), true);
        } catch (Exception ex) {
            log.error("Failed to refresh VPLEX Storage View: " + ex.getLocalizedMessage(), ex);
            String storageViewName = exportMask != null ? exportMask.getMaskName() : "unknown";
            throw VPlexApiException.exceptions.failedToRefreshVplexStorageView(storageViewName, ex.getLocalizedMessage());
        }
    }
}
