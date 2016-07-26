/*
 * Copyright (c) 2015 EMC Corporation
 * All Rights Reserved
 */
package models.datatable;

import models.StorageSystemTypes;

import org.apache.commons.lang.StringUtils;
import java.io.*;
import util.MessagesUtils;
import util.datatable.DataTable;

import com.emc.storageos.model.systems.StorageSystemRestRep;
import com.emc.vipr.client.core.util.ResourceUtils;

public class StorageSystemDataTable extends DataTable {

    protected static final String NAME_NOT_AVAILABLE = "StorageSystems.nameNotAvailable";

    public StorageSystemDataTable() {
        addColumn("name").setRenderFunction("renderLink");
        addColumn("registrationStatus").setRenderFunction("render.registrationStatus");
        addColumn("range");
	addColumn("vipAddress");
	addColumn("numNodes");
        addColumn("type");
	addColumn("deviceType");
        addColumn("version").hidden();
        addColumn("userName").hidden();
        StorageSystemInfo.addDiscoveryColumns(this);
        sortAll();
        setDefaultSort("name", "asc");
    }

    public static class StorageSystemInfo extends DiscoveredSystemInfo {
        public String id;
        public String name;
        public String host;
	public String range;
	public String vipAddress;
	public String numNodes;
	public String[] nums;
	public String last;
	public String deviceType;
        public String userName;
        public String type;
        public String version;
        public String registrationStatus;

        public StorageSystemInfo() {
        }

        public StorageSystemInfo(StorageSystemRestRep storageSystem) {
            super(storageSystem);
            this.id = storageSystem.getId().toString();
            this.name = StringUtils.defaultIfEmpty(
                    StringUtils.defaultIfEmpty(storageSystem.getName(), storageSystem.getSerialNumber()),
                    MessagesUtils.get(NAME_NOT_AVAILABLE));
            if (ResourceUtils.id(storageSystem.getActiveProvider()) != null) {
                this.host = storageSystem.getSmisProviderIP();
                this.userName = storageSystem.getSmisUserName();
            }
            else {
                this.host = storageSystem.getIpAddress();
                this.userName = storageSystem.getUsername();
            }
	    this.vipAddress = storageSystem.getVipAddress();
	    this.numNodes = storageSystem.getNumNodes();
	    this.nums = this.host.split("\\.");
	    this.last = this.nums[this.nums.length-1];
	    this.range = this.nums[0].concat(".").concat(this.nums[1]).concat(".").concat(this.nums[2]).concat(".").concat(this.last).concat("-").concat(this.nums[0]).concat(".").concat(this.nums[1]).concat(".").concat(this.nums[2]).concat(".").concat(Integer.toString(Integer.parseInt(this.last)+Integer.parseInt(this.numNodes)-1));
	    this.deviceType = storageSystem.getDeviceType();


            this.type = StorageSystemTypes.getDisplayValue(storageSystem.getSystemType());
            this.version = storageSystem.getFirmwareVersion();
            this.registrationStatus = storageSystem.getRegistrationStatus();
        }
    }
}
