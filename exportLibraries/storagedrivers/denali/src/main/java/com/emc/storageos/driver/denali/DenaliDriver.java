

package com.emc.storageos.driver.denali;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import java.io.PrintWriter;
import java.io.IOException;

import org.apache.commons.lang.mutable.MutableBoolean;
import org.apache.commons.lang.mutable.MutableInt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.emc.storageos.storagedriver.BlockStorageDriver;
import com.emc.storageos.storagedriver.DefaultStorageDriver;
import com.emc.storageos.storagedriver.DriverTask;
import com.emc.storageos.storagedriver.HostExportInfo;
import com.emc.storageos.storagedriver.RegistrationData;



import com.emc.storageos.storagedriver.model.Initiator;
import com.emc.storageos.storagedriver.model.StorageBlockObject;
import com.emc.storageos.storagedriver.model.StorageHostComponent;
import com.emc.storageos.storagedriver.model.StorageObject;
import com.emc.storageos.storagedriver.model.StoragePool;
import com.emc.storageos.storagedriver.model.StoragePool.Protocols;
import com.emc.storageos.storagedriver.model.StoragePool.RaidLevels;
import com.emc.storageos.storagedriver.model.StoragePort;
import com.emc.storageos.storagedriver.model.StorageProvider;
import com.emc.storageos.storagedriver.model.StorageSystem;
import com.emc.storageos.storagedriver.model.StorageVolume;
import com.emc.storageos.storagedriver.model.VolumeClone;
import com.emc.storageos.storagedriver.model.VolumeConsistencyGroup;
import com.emc.storageos.storagedriver.model.VolumeMirror;
import com.emc.storageos.storagedriver.model.VolumeSnapshot;

import com.emc.storageos.storagedriver.storagecapabilities.AutoTieringPolicyCapabilityDefinition;
import com.emc.storageos.storagedriver.storagecapabilities.CapabilityInstance;
import com.emc.storageos.storagedriver.storagecapabilities.StorageCapabilities;

public class DenaliDriver extends DefaultStorageDriver implements BlockStorageDriver {
    private static final Logger _log = LoggerFactory.getLogger(DenaliDriver.class);
    public static final String DRIVER_NAME = "denali";
    
    @Override
    public RegistrationData getRegistrationData() {
        RegistrationData registrationData = new RegistrationData("DenaliDriver", "denali", null);
        return registrationData;
    }

    public void setConnInfoToRegistry(String systemNativeId, String ipAddress, int port, String username, String password) {
        Map<String, List<String>> attributes = new HashMap<>();
        List<String> listIP = new ArrayList<>();
        List<String> listPort = new ArrayList<>();
        List<String> listUserName = new ArrayList<>();
        List<String> listPwd = new ArrayList<>();

        listIP.add(ipAddress);
        attributes.put("IP_ADDRESS", listIP);
        listPort.add(Integer.toString(port));
        attributes.put("PORT_NUMBER", listPort);
        listUserName.add(username);
        attributes.put("USER_NAME", listUserName);
        listPwd.add(password);
        attributes.put("PASSWORD", listPwd);
        _log.info(String.format("StorageDriver: setting connection information for %s, attributes: %s ", systemNativeId, attributes));
        this.driverRegistry.setDriverAttributesForKey("Denali Storage Driver", systemNativeId, attributes);
    }

    @Override    
    public DriverTask discoverStorageSystem(StorageSystem storageSystem) {
        _log.info("StorageDriver: discoverStorageSystem information for storage system {}, name {} - start", storageSystem.getIpAddress(), storageSystem.getSystemName());
        String taskType = "discover-storage-system";
        String taskId = String.format("%s+%s+%s", DRIVER_NAME, taskType, UUID.randomUUID().toString());
        DriverTask task = new DenaliTask(taskId);
        try {
            if (storageSystem.getSerialNumber() == null) {
            	storageSystem.setSerialNumber(storageSystem.getSystemName());
            }
	    
	    System.out.println(storageSystem.getNativeId());
	    //storageSystem.setNativeId("denali");
            storageSystem.setFirmwareVersion("2.4-3.12");
            storageSystem.setIsSupportedVersion(true);
            setConnInfoToRegistry(storageSystem.getNativeId(), storageSystem.getIpAddress(), storageSystem.getPortNumber(), storageSystem.getUsername(), storageSystem.getPassword());
	    List<String> protocols = new ArrayList<String>();
	    protocols.add(Protocols.iSCSI.toString());
	    storageSystem.setProtocols(protocols);
            task.setStatus(DriverTask.TaskStatus.READY);
            _log.info("StorageDriver: discoverStorageSystem information for storage system {}, nativeId {} - end", storageSystem.getIpAddress(), storageSystem.getNativeId());
        } catch (Exception e) {
            task.setStatus(DriverTask.TaskStatus.FAILED);
            e.printStackTrace();
        }
        return task;
    }
    @Override
    public DriverTask discoverStoragePools(StorageSystem storageSystem, List<StoragePool> storagePools) {
        _log.info("Discovery of storage pools for storage system {} .", storageSystem.getNativeId());
        String taskType = "discover-storage-pools";
        String taskId = String.format("%s+%s+%s", DRIVER_NAME, taskType, UUID.randomUUID().toString());
        DriverTask task = new DenaliTask(taskId);
        task.setStatus(DriverTask.TaskStatus.READY);
        _log.info("StorageDriver: discoverStoragePools information for storage system {}, nativeId {} - end", storageSystem.getIpAddress(), storageSystem.getNativeId());
    	StoragePool pool = new StoragePool();
    	// Set other storage pool properties
        
    	// Add auto tiering policy capabilities
    	AutoTieringPolicyCapabilityDefinition capabilityDefinition = new AutoTieringPolicyCapabilityDefinition();
    	Map<String, List<String>> props = new HashMap<>();
    	List<CapabilityInstance> capabilities = new ArrayList<>();
    	String policyId = "Auto-Tier-Policy-1";
    	props.put(AutoTieringPolicyCapabilityDefinition.PROPERTY_NAME.POLICY_ID.name(), Arrays.asList(policyId));
    	String provisioningType = StoragePool.AutoTieringPolicyProvisioningType.ThinlyProvisioned.name();
    	props.put(AutoTieringPolicyCapabilityDefinition.PROPERTY_NAME.PROVISIONING_TYPE.name(), Arrays.asList(provisioningType));
    	CapabilityInstance capabilityInstance = new CapabilityInstance(capabilityDefinition.getId(), policyId, props);
    	capabilities.add(capabilityInstance);
        
    	// Add capabilities for other auto tiering policies supported by the storage pool.
    	// Set the capabilities for the storage pool.
    	pool.setCapabilities(capabilities);
        
	// Add the storage pool to the list of pools to be returned.
    	storagePools.add(pool);
        return task;
    }

}
