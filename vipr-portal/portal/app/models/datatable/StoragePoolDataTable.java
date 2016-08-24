/*
 * Copyright (c) 2015 EMC Corporation
 * All Rights Reserved
 */
package models.datatable;

import static com.emc.vipr.client.core.util.ResourceUtils.name;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.WordUtils;

import com.emc.storageos.model.pools.StoragePoolRestRep;
import com.emc.storageos.model.systems.StorageSystemRestRep;
import com.emc.vipr.client.core.util.CachedResources;

import models.SupportedResourceTypes;
import util.MessagesUtils;
import util.datatable.DataTable;

public class StoragePoolDataTable extends DataTable {

	protected static final String OBJECT = "object";
    public StoragePoolDataTable() {
        addColumn("name");
        addColumn("registrationStatus").setRenderFunction("render.registrationStatus");
        addColumn("storageSystem");
        addColumn("status").hidden();
        addColumn("volumeTypes");
	addColumn("protocols");
        addColumn("driveTypes");
        addColumn("compressionEnabled").setRenderFunction("render.boolean");
        addColumn("numOfDataCenters").hidden();
        addColumn("freeCapacity").setRenderFunction("render.sizeInGb");
        addColumn("subscribedCapacity").setRenderFunction("render.sizeInGb");
        addColumn("totalCapacity").setRenderFunction("render.sizeInGb");
        addColumn("assigned").hidden().setRenderFunction("render.boolean");
        setDefaultSort("name", "asc");
        sortAllExcept("id");
    }

    public static class StoragePoolInfo {
        public String id;
        public String name;
        public String storageSystem;
        public String status;
	public String protocols;
        public String driveTypes;
        public Long freeCapacity;
        public Integer numOfDataCenters;
        public Long subscribedCapacity;
        public Long totalCapacity;
        public String registrationStatus;
        public String volumeTypes;
        public String network;
        public boolean assigned;
        public boolean compressionEnabled;

        public StoragePoolInfo(StoragePoolRestRep storagePool) {
            this(storagePool, (String) null);
        }

        public StoragePoolInfo(StoragePoolRestRep storagePool, CachedResources<StorageSystemRestRep> storageSystems) {
            this(storagePool, name(storageSystems.get(storagePool.getStorageSystem())));
        }

        public StoragePoolInfo(StoragePoolRestRep storagePool, String storageSystemName) {
            this.id = storagePool.getId().toString();
            this.name = storagePool.getPoolName();
            this.storageSystem = StringUtils.defaultIfEmpty(storageSystemName, MessagesUtils.get("StoragePoolDataTable.notApplicable"));
            this.status = StringUtils.isNotEmpty(storagePool.getOperationalStatus()) ?
                    WordUtils.capitalizeFully(storagePool.getOperationalStatus()) :
                    MessagesUtils.get("StoragePoolDataTable.notApplicable");
            this.driveTypes = StringUtils.join(storagePool.getDriveTypes(), ", ");
	    this.protocols = StringUtils.join(storagePool.getProtocols(), ", ");
            this.freeCapacity = storagePool.getFreeCapacity();
            this.subscribedCapacity = storagePool.getSubscribedCapacity();
            if (storagePool.getPoolServiceType().equals(OBJECT)) {
            	this.subscribedCapacity = storagePool.getUsedCapacity();
            	this.numOfDataCenters = storagePool.getDataCenters();
            }
            this.totalCapacity = storagePool.getTotalCapacity();
            this.registrationStatus = storagePool.getRegistrationStatus();
            this.volumeTypes = SupportedResourceTypes.getDisplayValue(storagePool.getSupportedResourceTypes());
            this.compressionEnabled = storagePool.getCompressionEnabled();
        }
    }
}
