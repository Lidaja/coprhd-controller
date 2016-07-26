/*
 * Copyright (c) 2015 EMC Corporation
 * All Rights Reserved
 */
package controllers;

import static com.emc.vipr.client.core.util.ResourceUtils.id;
import static com.emc.vipr.client.core.util.ResourceUtils.uri;
import static com.emc.vipr.client.core.util.ResourceUtils.uris;
import static controllers.Common.backToReferrer;
import static controllers.Common.getUserMessage;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import jobs.vipr.TenantsCall;
import models.ConnectivityTypes;
import models.RegistrationStatus;
import models.datatable.NetworksDataTable;
import models.datatable.StoragePoolDataTable;
import models.datatable.StoragePoolDataTable.StoragePoolInfo;
import models.datatable.StoragePortDataTable;
import models.datatable.StoragePortDataTable.StoragePortInfo;
import models.datatable.StorageSystemDataTable;
import models.datatable.StorageSystemDataTable.StorageSystemInfo;
import models.datatable.VirtualArrayDataTable;
import models.datatable.VirtualArrayDataTable.VirtualArrayInfo;
import models.datatable.VirtualPoolDataTable;
import models.datatable.VirtualPoolDataTable.VirtualPoolInfo;

import org.apache.commons.lang.StringUtils;

import play.Logger;
import play.data.binding.As;
import play.data.validation.MaxSize;
import play.data.validation.MinSize;
import play.data.validation.Required;
import play.data.validation.Validation;
import play.i18n.Messages;
import play.mvc.Util;
import play.mvc.With;
import util.BourneUtil;
import util.MessagesUtils;
import util.NetworkUtils;
import util.StoragePoolUtils;
import util.StoragePortUtils;
import util.StorageSystemUtils;
import util.TenantUtils;
import util.VirtualArrayUtils;
import util.VirtualPoolUtils;
import util.builders.ACLUpdateBuilder;
import util.datatable.DataTablesSupport;
import com.emc.storageos.model.NamedRelatedResourceRep;
import com.emc.storageos.model.auth.ACLEntry;
import com.emc.storageos.model.pools.StoragePoolRestRep;
import com.emc.storageos.model.pools.StoragePoolUpdate;
import com.emc.storageos.model.pools.VirtualArrayAssignmentChanges;
import com.emc.storageos.model.pools.VirtualArrayAssignments;
import com.emc.storageos.model.ports.StoragePortRestRep;
import com.emc.storageos.model.ports.StoragePortUpdate;
import com.emc.storageos.model.systems.StorageSystemRestRep;
import com.emc.storageos.model.varray.BlockSettings;
import com.emc.storageos.model.varray.NetworkRestRep;
import com.emc.storageos.model.varray.NetworkUpdate;
import com.emc.storageos.model.varray.VirtualArrayRestRep;
import com.emc.storageos.model.vpool.VirtualPoolCommonRestRep;
import com.emc.vipr.client.core.util.CachedResources;
import com.emc.vipr.client.core.util.ResourceUtils;
import com.emc.vipr.client.exceptions.ServiceErrorException;
import com.emc.vipr.client.exceptions.ViPRException;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import controllers.arrays.Networks;
import controllers.deadbolt.Restrict;
import controllers.deadbolt.Restrictions;
import controllers.util.FlashException;
import controllers.util.ViprResourceController;

@With(Common.class)
@Restrictions({ @Restrict("SYSTEM_ADMIN"), @Restrict("RESTRICTED_SYSTEM_ADMIN") })
public class VirtualArrays extends ViprResourceController {
    protected static final String SAVED_SUCCESS = "VirtualArrays.save.success";
    protected static final String SAVED_ERROR = "VirtualArrays.save.error";
    protected static final String DELETED_SUCCESS = "VirtualArrays.delete.success";
    protected static final String DELETED_ERROR = "VirtualArrays.delete.error";
    protected static final String UNKNOWN = "VirtualArrays.unknown";

    /**
     * Simple create and save operation that takes only the name.
     * 
     * @param name
     *            Name of the virtual array
     */
    @FlashException("list")
    public static void createSimple(String name) {
        VirtualArrayForm virtualArray = new VirtualArrayForm();
        virtualArray.name = name;
        virtualArray.validate("virtualArray");
        if (Validation.hasErrors()) {
            flash.error(MessagesUtils.get(SAVED_ERROR, virtualArray.name));
            list();
        }

        VirtualArrayRestRep varray = virtualArray.save();
        flash.success(MessagesUtils.get(SAVED_SUCCESS, virtualArray.name));
        virtualArray.load(varray);
        edit(virtualArray.id);
    }

    /**
     * Displays the page for editing an existing virtual array.
     * 
     * @param id
     *            the virtual array ID.
     */
    public static void edit(String id) {
        VirtualArrayRestRep virtualArray = getVirtualArray(id);
        VirtualArrayForm form = new VirtualArrayForm();
        form.load(virtualArray);
        edit(form);
    }

    /**
     * Gets the given virtual array. If the array cannot be found, an error is show and redirects back to the referrer
     * or to the list page.
     * 
     * @param id
     *            the virtual array ID.
     * @return the virtual array.
     */
    @Util
    public static VirtualArrayRestRep getVirtualArray(String id) {
        VirtualArrayRestRep virtualArray = VirtualArrayUtils.getVirtualArray(id);
        if (virtualArray == null) {
            flash.error(MessagesUtils.get(UNKNOWN, id));
            backToReferrer();
            list();
        }
        return virtualArray;
    }

    /**
     * Shows the edit page for a virtual array form.
     * 
     * @param virtualArray
     *            the virtual array form.
     */
    private static void edit(VirtualArrayForm virtualArray) {
        Map<Boolean, String> autoSanZoningOptions = Maps.newHashMap();
        autoSanZoningOptions.put(Boolean.TRUE, Messages.get("virtualArray.autoSanZoning.true"));
        autoSanZoningOptions.put(Boolean.FALSE, Messages.get("virtualArray.autoSanZoning.false"));
        renderArgs.put("autoSanZoningOptions", autoSanZoningOptions);
        renderArgs.put("storageSystems", new VirtualArrayStorageSystemsDataTable());
        renderArgs.put("virtualPools", new VirtualArrayVirtualPoolsDataTable());

        if (TenantUtils.canReadAllTenants() && VirtualArrayUtils.canUpdateACLs()) {
            renderArgs.put("tenantOptions", dataObjectOptions(await(new TenantsCall().asPromise())));
        }

        // Numbers for networks, ports and pools
        if (!virtualArray.isNew()) {
            renderArgs.put("networksCount", NetworkUtils.getNetworksByVirtualArray(virtualArray.id).size());
            renderArgs.put("storagePortsCount", StoragePortUtils.getStoragePortsByVirtualArray(uri(virtualArray.id))
                    .size());
            renderArgs.put("storagePoolsCount", StoragePoolUtils.getStoragePoolsAssignedToVirtualArray(virtualArray.id)
                    .size());
        }

        render("@edit", virtualArray);
    }

    /**
     * Saves a virtual array.
     * 
     * @param virtualArray
     *            the virtual array.
     */
    @FlashException(referrer = { "createSimple", "edit", "list" })
    public static void save(VirtualArrayForm virtualArray) {
        if (virtualArray == null) {
            list();
        }
        virtualArray.validate("virtualArray");
        if (Validation.hasErrors()) {
            Common.handleError();
        }

        VirtualArrayRestRep varray = virtualArray.save();
        if (TenantUtils.canReadAllTenants() && VirtualArrayUtils.canUpdateACLs()) {
            saveTenantACLs(varray.getId().toString(), virtualArray.tenants);
        }
        flash.success(MessagesUtils.get(SAVED_SUCCESS, virtualArray.name));
        backToReferrer();
        list();
    }

    /**
     * Saves tenant ACLs on the virtual array.
     * 
     * @param virtualArrayId
     *            the virtual array ID.
     * @param tenants
     *            the tenant ACLs.
     */
    private static void saveTenantACLs(String virtualArrayId, List<String> tenants) {
        Set<String> tenantIds = Sets.newHashSet();
        if (tenants != null) {
            tenantIds.addAll(tenants);
        }
        ACLUpdateBuilder builder = new ACLUpdateBuilder(VirtualArrayUtils.getACLs(virtualArrayId));
        builder.setTenants(tenantIds);

        try {
            VirtualArrayUtils.updateACLs(virtualArrayId, builder.getACLUpdate());
        } catch (ViPRException e) {
            Logger.error(e, "Failed to update Virtual Array ACLs");
            String errorDesc = e.getMessage();
            if (e instanceof ServiceErrorException) {
                errorDesc = ((ServiceErrorException) e).getDetailedMessage();
            }
            flash.error(MessagesUtils.get("varrays.updateVarrayACLs.failed", errorDesc));
        }
    }

    /**
     * Displays the virtual array list page.
     */
    public static void list() {
        VirtualArrayDataTable dataTable = new VirtualArrayDataTable();
        render(dataTable);
    }

    /**
     * Lists the virtual arrays and renders the result using JSON.
     */
    public static void listJson() {
        try {
            performListJson(VirtualArrayUtils.getVirtualArrays(), new JsonItemOperation());
        } catch (Exception e) {
            renderJSON(DataTablesSupport.createJSON(Collections.emptyList(), params, getUserMessage(e)));
        }
    }

    /**
     * Renders the details of a single virtual array.
     * 
     * @param id
     *            the virtual array ID.
     */
    public static void itemDetails(String id) {
        VirtualArrayRestRep virtualArray = VirtualArrayUtils.getVirtualArray(id);
        if (virtualArray == null) {
            error(MessagesUtils.get(UNKNOWN, id));
        }
        Map<String, Set<NamedRelatedResourceRep>> connectivityMap = VirtualArrayUtils.getConnectivityMap(id);
        // Do not display RP/VPLEX connectivity separately
        connectivityMap.remove(ConnectivityTypes.RP_VPLEX);
        Map<String, Set<String>> attributes = VirtualArrayUtils.getAvailableAttributes(uris(id));
        render(virtualArray, connectivityMap, attributes);
    }

    /**
     * Deletes the specified virtual arrays.
     * 
     * @param ids
     *            the IDs of the virtual arrays to delete.
     */
    public static void delete(@As(",") String[] ids) {
        delete(uris(ids));
    }

    /**
     * Deletes the specified virtual arrays and redirects back to the list page.
     * 
     * @param ids
     *            the list of IDs.
     */
    private static void delete(List<URI> ids) {
        performSuccessFail(ids, new DeactivateOperation(), DELETED_SUCCESS, DELETED_ERROR);
        list();
    }

    /**
     * Creates a change to add a virtual array.
     * 
     * @param virtualArray
     *            the virtual array to add.
     * @return the virtual array assignment changes.
     */
    private static VirtualArrayAssignmentChanges addVirtualArray(VirtualArrayRestRep virtualArray) {
        VirtualArrayAssignmentChanges changes = new VirtualArrayAssignmentChanges();
        changes.setAdd(new VirtualArrayAssignments(Sets.newHashSet(stringId(virtualArray))));
        return changes;
    }

    /**
     * Creates a change to remove a virtual array.
     * 
     * @param virtualArray
     *            the virtual array to remove.
     * @return the virtual array assignment changes.
     */
    private static VirtualArrayAssignmentChanges removeVirtualArray(VirtualArrayRestRep virtualArray) {
        VirtualArrayAssignmentChanges changes = new VirtualArrayAssignmentChanges();
        changes.setRemove(new VirtualArrayAssignments(Sets.newHashSet(stringId(virtualArray))));
        return changes;
    }

    /**
     * Displays the networks page for the given virtual array.
     * 
     * @param id
     *            the virtual array ID.
     */
    public static void networks(String id) {
        VirtualArrayRestRep virtualArray = getVirtualArray(id);
        VirtualArrayNetworksDataTable dataTable = new VirtualArrayNetworksDataTable();
        render(virtualArray, dataTable);
    }

    /**
     * Renders the list of networks for a given virtual array as JSON.
     * 
     * @param id
     *            the virtual array ID.
     */
    public static void networksJson(String id) {
        List<NetworkInfo> items = Lists.newArrayList();
        List<NetworkRestRep> networks = NetworkUtils.getNetworksByVirtualArray(id);
        for (NetworkRestRep network : networks) {
            items.add(new NetworkInfo(network, id));
        }
        renderJSON(DataTablesSupport.createJSON(items, params));
    }

    /**
     * Renders the list of networks available to add to the given virtual array as JSON.
     * 
     * @param id
     *            the virtual array ID.
     */
    public static void availableNetworksJson(String id) {
        List<NetworkInfo> items = Lists.newArrayList();
        List<NetworkRestRep> networks = NetworkUtils.getNetworksAssignableToVirtualArray(id);
        for (NetworkRestRep network : networks) {
            items.add(new NetworkInfo(network, id));
        }
        renderJSON(DataTablesSupport.createJSON(items, params));
    }

    /**
     * Registers a number of networks and redisplays the networks page.
     * 
     * @param virtualArrayId
     *            the virtual array ID.
     * @param ids
     *            the network IDs.
     */
    @FlashException
    public static void registerNetworks(String virtualArrayId, @As(",") String[] ids) {
        if (ids != null) {
            Networks.registerNetworks(uris(ids));
        }
        networks(virtualArrayId);
    }

    /**
     * De-registers a number of networks and redisplays the networks page.
     * 
     * @param virtualArrayId
     *            the virtual array ID.
     * @param ids
     *            the network IDs.
     */
    @FlashException
    public static void deregisterNetworks(String virtualArrayId, @As(",") String[] ids) {
        if (ids != null) {
            Networks.deregisterNetworks(uris(ids));
        }
        networks(virtualArrayId);
    }

    /**
     * Adds a number of networks to the given virtual array, and redisplays the networks page.
     * 
     * @param virtualArrayId
     *            the virtual array ID.
     * @param ids
     *            the IDs of the networks to add.
     */
    @FlashException
    public static void addNetworks(String virtualArrayId, @As(",") String[] ids) {
        if ((ids == null) || (ids.length == 0)) {
            networks(virtualArrayId);
        }

        VirtualArrayRestRep virtualArray = getVirtualArray(virtualArrayId);
        updateNetworks(uris(ids), addVirtualArray(virtualArray));
        networks(virtualArrayId);
    }

    /**
     * Removes a number of networks from the given virtual array, and redisplays the networks page.
     * 
     * @param virtualArrayId
     *            the virtual array ID.
     * @param ids
     *            the IDs of the networks to remove.
     */
    @FlashException
    public static void removeNetworks(String virtualArrayId, @As(",") String[] ids) {
        if ((ids == null) || (ids.length == 0)) {
            networks(virtualArrayId);
        }

        VirtualArrayRestRep virtualArray = getVirtualArray(virtualArrayId);
        updateNetworks(uris(ids), removeVirtualArray(virtualArray));
        networks(virtualArrayId);
    }

    /**
     * Updates the given networks with the virtual array assignment changes.
     * 
     * @param ids
     *            the network IDs.
     * @param changes
     *            the virtual array changes.
     */
    private static void updateNetworks(List<URI> ids, VirtualArrayAssignmentChanges changes) {
        if (ids.isEmpty()) {
            return;
        }
        List<NetworkRestRep> networks = NetworkUtils.getNetworks(ids);
        for (NetworkRestRep network : networks) {
            NetworkUpdate update = new NetworkUpdate();
            update.setVarrayChanges(changes);
            NetworkUtils.update(network.getId(), update);
        }
    }

    /**
     * Displays the storage ports page for the given virtual array.
     * 
     * @param id
     *            the virtual array ID.
     */
    public static void storagePorts(String id) {
        VirtualArrayRestRep virtualArray = getVirtualArray(id);
        VirtualArrayStoragePortsDataTable dataTable = new VirtualArrayStoragePortsDataTable();
        render(virtualArray, dataTable);
    }

    /**
     * Renders the list of storage ports for a given virtual array as JSON.
     * 
     * @param id
     *            the virtual array ID.
     */
    public static void storagePortsJson(String id) {
        List<StoragePortInfo> items = Lists.newArrayList();
        CachedResources<StorageSystemRestRep> storageSystems = StorageSystemUtils.createCache();
        List<StoragePortRestRep> storagePorts = StoragePortUtils.getStoragePortsByVirtualArray(uri(id));

        Map<URI, String> networks = NetworkUtils.getNetworkNamesByVirtualArray(id);
        for (StoragePortRestRep storagePort : storagePorts) {
            StoragePortInfo item = new StoragePortInfo(storagePort, storageSystems.get(storagePort.getStorageDevice()));
            item.assigned = VirtualArrayUtils.isAssigned(storagePort, id);
            item.network = networks.get(id(storagePort.getNetwork()));
            items.add(item);
        }
        renderJSON(DataTablesSupport.createJSON(items, params));
    }

    /**
     * Renders the list of storage ports that are available to be assigned to the given virtual array.
     * 
     * @param id
     *            the virtual array ID.
     */
    public static void availableStoragePortsJson(String id) {
        List<StoragePortInfo> items = Lists.newArrayList();
        CachedResources<StorageSystemRestRep> storageSystems = StorageSystemUtils.createCache();
        Map<URI, String> networks = NetworkUtils.getNetworkNames();
        List<StoragePortRestRep> storagePorts = StoragePortUtils.getStoragePortsAssignableToVirtualArray(uri(id));
        for (StoragePortRestRep storagePort : storagePorts) {
            StoragePortInfo item = new StoragePortInfo(storagePort, storageSystems.get(storagePort.getStorageDevice()));
            item.network = networks.get(id(storagePort.getNetwork()));
            items.add(item);
        }
        renderJSON(DataTablesSupport.createJSON(items, params));
    }

    /**
     * Adds a number of storage ports to the given virtual array, and redisplays the storage ports page.
     * 
     * @param virtualArrayId
     *            the virtual array ID.
     * @param ids
     *            the IDs of the storage ports to add.
     */
    @FlashException
    public static void addStoragePorts(String virtualArrayId, @As(",") String[] ids) {
        if ((ids != null) && (ids.length > 0)) {
            VirtualArrayRestRep virtualArray = getVirtualArray(virtualArrayId);
            updateStoragePorts(uris(ids), addVirtualArray(virtualArray));
        }
        storagePorts(virtualArrayId);
    }

    /**
     * Removes a number of storage ports from the given virtual array, and redisplays the storage ports page.
     * 
     * @param virtualArrayId
     *            the virtual array ID.
     * @param ids
     *            the IDs of the storage ports to remove.
     */
    @FlashException
    public static void removeStoragePorts(String virtualArrayId, @As(",") String[] ids) {
        if ((ids != null) && (ids.length > 0)) {
            VirtualArrayRestRep virtualArray = getVirtualArray(virtualArrayId);
            updateStoragePorts(uris(ids), removeVirtualArray(virtualArray));
        }
        storagePorts(virtualArrayId);
    }

    /**
     * Updates the given storage ports with the virtual array assignment changes.
     * 
     * @param ids
     *            the storage port IDs.
     * @param changes
     *            the virtual array changes.
     */
    private static void updateStoragePorts(List<URI> ids, VirtualArrayAssignmentChanges changes) {
        if (ids.isEmpty()) {
            return;
        }
        List<StoragePortRestRep> storagePorts = StoragePortUtils.getStoragePorts(ids);
        for (StoragePortRestRep storagePort : storagePorts) {
            StoragePortUpdate update = new StoragePortUpdate();
            update.setVarrayChanges(changes);
            StoragePortUtils.update(storagePort.getId(), update);
        }
    }

    /**
     * Displays a page listing all storage pools associated with the given virtual array.
     * 
     * @param id
     *            the virtual array ID.
     */
    public static void storagePools(String id) {
        VirtualArrayRestRep virtualArray = getVirtualArray(id);
        VirtualArrayStoragePoolsDataTable dataTable = new VirtualArrayStoragePoolsDataTable();
        render(virtualArray, dataTable);
    }

    /**
     * Renders the list of storage pools for a given virtual array as JSON.
     * 
     * @param id
     *            the virtual array ID.
     */
    public static void storagePoolsJson(String id) {
        List<StoragePoolInfo> items = Lists.newArrayList();
        CachedResources<StorageSystemRestRep> storageSystems = StorageSystemUtils.createCache();
        for (StoragePoolRestRep storagePool : StoragePoolUtils.getStoragePoolsAssignedToVirtualArray(id)) {
            StoragePoolInfo item = new StoragePoolInfo(storagePool, storageSystems);
            item.assigned = VirtualArrayUtils.isAssigned(storagePool, id);
            items.add(item);
        }
        renderJSON(DataTablesSupport.createJSON(items, params));
    }

    /**
     * Renders the list of storage pools available for the given virtual array as JSON.
     * 
     * @param id
     *            the virtual array ID.
     */
    public static void availableStoragePoolsJson(String id) {
        List<StoragePoolInfo> items = Lists.newArrayList();
        CachedResources<StorageSystemRestRep> storageSystems = StorageSystemUtils.createCache();
        for (StoragePoolRestRep storagePool : StoragePoolUtils.getStoragePoolsAssignableToVirtualArray(id)) {
            items.add(new StoragePoolInfo(storagePool, storageSystems));
        }
        renderJSON(DataTablesSupport.createJSON(items, params));
    }

    /**
     * Registers a number of storage pools and redisplays the storage pools page.
     * 
     * @param virtualArrayId
     *            the virtual array ID.
     * @param ids
     *            the storage pools IDs.
     */
    @FlashException
    public static void registerStoragePools(String virtualArrayId, @As(",") String[] ids) {
        if ((ids != null) && (ids.length > 0)) {
            for (StoragePoolRestRep pool : StoragePoolUtils.getStoragePools(uris(ids))) {
                if (RegistrationStatus.isUnregistered(pool.getRegistrationStatus())) {
                    StoragePoolUtils.register(id(pool), id(pool.getStorageSystem()));
                }
            }
        }
        storagePools(virtualArrayId);
    }

    /**
     * De-registers a number of storage pools and redisplays the storage pools page.
     * 
     * @param virtualArrayId
     *            the virtual array ID.
     * @param ids
     *            the storage pool IDs.
     */
    @FlashException
    public static void deregisterStoragePools(String virtualArrayId, @As(",") String[] ids) {
        if ((ids != null) && (ids.length > 0)) {
            for (StoragePoolRestRep pool : StoragePoolUtils.getStoragePools(uris(ids))) {
                if (RegistrationStatus.isRegistered(pool.getRegistrationStatus())) {
                    StoragePoolUtils.deregister(id(pool));
                }
            }
        }
        storagePools(virtualArrayId);
    }

    /**
     * Adds a number of storage pools to the given virtual array, and redisplays the storage pools page.
     * 
     * @param virtualArrayId
     *            the virtual array ID.
     * @param ids
     *            the IDs of the storage pools to add.
     */
    @FlashException
    public static void addStoragePools(String virtualArrayId, @As(",") String[] ids) {
        if ((ids != null) && (ids.length > 0)) {
            VirtualArrayRestRep virtualArray = getVirtualArray(virtualArrayId);
            updateStoragePools(uris(ids), addVirtualArray(virtualArray));
        }
        storagePools(virtualArrayId);
    }

    /**
     * Removes a number of storage pools from the given virtual array, and redisplays the storage pools page.
     * 
     * @param virtualArrayId
     *            the virtual array ID.
     * @param ids
     *            the IDs of the storage pools to remove.
     */
    @FlashException
    public static void removeStoragePools(String virtualArrayId, @As(",") String[] ids) {
        if ((ids != null) && (ids.length > 0)) {
            VirtualArrayRestRep virtualArray = getVirtualArray(virtualArrayId);
            updateStoragePools(uris(ids), removeVirtualArray(virtualArray));
        }
        storagePools(virtualArrayId);
    }

    /**
     * Updates the given storage pools with the virtual array assignment changes.
     * 
     * @param ids
     *            the storage pool IDs.
     * @param changes
     *            the virtual array changes.
     */
    private static void updateStoragePools(List<URI> ids, VirtualArrayAssignmentChanges changes) {
        if (ids.isEmpty()) {
            return;
        }
        List<StoragePoolRestRep> storagePools = StoragePoolUtils.getStoragePools(ids);
        for (StoragePoolRestRep storagePool : storagePools) {
            StoragePoolUpdate update = new StoragePoolUpdate();
            update.setVarrayChanges(changes);
            StoragePoolUtils.update(storagePool.getId(), update);
        }
    }

    public static void storageSystemsJson(String id) {
        List<StorageSystemInfo> items = Lists.newArrayList();
        for (StorageSystemRestRep storageSystem : StorageSystemUtils.getStorageSystemsByVirtualArray(id)) {
            items.add(new StorageSystemInfo(storageSystem));
        }
        renderJSON(DataTablesSupport.createJSON(items, params));
    }

    /**
     * Adds all ports of the given storage systems to the virtual array.
     * 
     * @param virtualArrayId
     *            the virtual array ID.
     * @param ids
     *            the storage system IDs.
     */
    @FlashException(referrer = { "edit", "list" })
    public static void addStorageSystems(String virtualArrayId, @As(",") String[] ids) {
        List<URI> storagePorts = Lists.newArrayList();
        for (URI storageSystemId : uris(ids)) {
            List<StoragePortRestRep> ports = StoragePortUtils.getStoragePortsByStorageSystem(storageSystemId);
            storagePorts.addAll(ResourceUtils.ids(ports));
        }
        if (!storagePorts.isEmpty()) {
            VirtualArrayRestRep virtualArray = getVirtualArray(virtualArrayId);
            updateStoragePorts(storagePorts, addVirtualArray(virtualArray));
        }
        edit(virtualArrayId);
    }

    /**
     * Gets the list of storage systems that may be associated to a virtual array.
     * 
     * @param id
     *            the virtual array ID.
     */
    public static void addStorageSystemsJson(String id) {
        List<StorageSystemInfo> items = Lists.newArrayList();
        for (StorageSystemRestRep storageSystem : StorageSystemUtils.getStorageSystems()) {
            items.add(new StorageSystemInfo(storageSystem));
        }
        renderJSON(DataTablesSupport.createJSON(items, params));
    }

    public static void virtualPoolsJson(String id) {
        List<VirtualPoolInfo> items = Lists.newArrayList();
        for (VirtualPoolCommonRestRep virtualPool : VirtualPoolUtils.getVirtualPoolsForVirtualArray(uri(id))) {
            items.add(new VirtualPoolInfo(virtualPool));
        }
        renderJSON(DataTablesSupport.createJSON(items, params));
    }

    protected static class JsonItemOperation implements ResourceValueOperation<VirtualArrayInfo, VirtualArrayRestRep> {
        @Override
        public VirtualArrayInfo performOperation(VirtualArrayRestRep virtualArray) throws Exception {
            return new VirtualArrayInfo(virtualArray);
        }
    }

    protected static class DeactivateOperation implements ResourceIdOperation<Void> {
        @Override
        public Void performOperation(URI id) throws Exception {
            VirtualArrayUtils.deactivate(id);
            return null;
        }
    }

    /**
     * Virtual array create/edit form.
     */
    public static class VirtualArrayForm {
        public String id;
        @Required
        @MaxSize(128)
        @MinSize(2)
        public String name;
        public Boolean autoSanZoning = Boolean.TRUE;
        public Boolean enableTenants = Boolean.FALSE;
        public List<String> tenants = new ArrayList<String>();

        public boolean isNew() {
            return StringUtils.isBlank(id);
        }

        public void load(VirtualArrayRestRep virtualArray) {
            this.id = stringId(virtualArray);
            this.name = virtualArray.getName();
            if (virtualArray.getBlockSettings() != null) {
                BlockSettings settings = virtualArray.getBlockSettings();
                this.autoSanZoning = settings.getAutoSanZoning();
            }
            loadTenant(virtualArray);
        }

        private void loadTenant(VirtualArrayRestRep virtualArray) {
            List<ACLEntry> acls = BourneUtil.getViprClient().varrays().getACLs(virtualArray.getId());
            for (ACLEntry acl : acls) {
                if (acl.getTenant() != null) {
                    this.tenants.add(acl.getTenant());
                }
            }

            if (!tenants.isEmpty()) {
                this.enableTenants = true;
            }
        }

        public VirtualArrayRestRep save() {
            if (isNew()) {
                return VirtualArrayUtils.create(name, Boolean.TRUE.equals(autoSanZoning));
            }
            else {
                return VirtualArrayUtils.update(id, name, Boolean.TRUE.equals(autoSanZoning));
            }
        }

        public void validate(String name) {
            Validation.valid(name, this);
            validateTenant(name);
        }

        protected void validateTenant(String formName) {
            if (enableTenants != null && enableTenants) {
                Validation.required(String.format("%s.tenants", formName), this.tenants);
            }
        }
    }

    public static class VirtualArrayNetworksDataTable extends NetworksDataTable {
        public VirtualArrayNetworksDataTable() {
            alterColumn("virtualArrayNames").hidden();
            addColumn("assigned").setRenderFunction("render.boolean");
            sortAll();
        }
    }

    public static class NetworkInfo extends NetworksDataTable.NetworkInfo {
        public boolean assigned;

        public NetworkInfo(NetworkRestRep network, String virtualArrayId) {
            super(network);
            assigned = (network.getAssignedVirtualArrays() != null)
                    && network.getAssignedVirtualArrays().contains(virtualArrayId);
        }
    }

    public static class VirtualArrayStoragePortsDataTable extends StoragePortDataTable {
        public VirtualArrayStoragePortsDataTable() {
            alterColumn("storageSystem").setVisible(true);
            alterColumn("networkIdentifier").setRenderFunction("render.networkIdentifier");
            alterColumn("iqn").hidden();
            addColumn("assigned").setRenderFunction("render.boolean");
            setSortable("assigned");
            setDefaultSortField("storageSystem");
        }
    }

    public static class VirtualArrayStoragePoolsDataTable extends StoragePoolDataTable {
        public VirtualArrayStoragePoolsDataTable() {
            alterColumn("assigned").setVisible(true);
        }
    }

    public static class VirtualArrayStorageSystemsDataTable extends StorageSystemDataTable {
        public VirtualArrayStorageSystemsDataTable() {
            alterColumn("name").setRenderFunction(null);
            alterColumn("registrationStatus").hidden();
            //alterColumn("host").hidden();
        }
    }

    public static class VirtualArrayVirtualPoolsDataTable extends VirtualPoolDataTable {
        public VirtualArrayVirtualPoolsDataTable() {
            alterColumn("name").setRenderFunction(null);
            alterColumn("description").setVisible(false);
            alterColumn("provisionedAs").setVisible(false);
            alterColumn("storagePoolAssignment").setVisible(false);
            alterColumn("protocols").setVisible(false);
        }
    }
}
