/*
 * Copyright (c) 2016 EMC Corporation
 * All Rights Reserved
 */
package com.emc.storageos.storagedriver;

import com.emc.storageos.storagedriver.model.Initiator;
import com.emc.storageos.storagedriver.model.StorageHostComponent;
import com.emc.storageos.storagedriver.model.StorageObject;
import com.emc.storageos.storagedriver.model.StoragePool;
import com.emc.storageos.storagedriver.model.StoragePort;
import com.emc.storageos.storagedriver.model.StorageProvider;
import com.emc.storageos.storagedriver.model.StorageSystem;
import com.emc.storageos.storagedriver.model.StorageVolume;
import com.emc.storageos.storagedriver.model.VolumeClone;
import com.emc.storageos.storagedriver.model.VolumeConsistencyGroup;
import com.emc.storageos.storagedriver.model.VolumeMirror;
import com.emc.storageos.storagedriver.model.VolumeSnapshot;
import com.emc.storageos.storagedriver.storagecapabilities.CapabilityInstance;
import com.emc.storageos.storagedriver.storagecapabilities.StorageCapabilities;
import org.apache.commons.lang.mutable.MutableBoolean;
import org.apache.commons.lang.mutable.MutableInt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;
import java.net.HttpURLConnection;

import java.io.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Default, not-supported, implementation of SDK driver methods.
 * Can be use as a base class for SDK storage drivers.
 */
public class DenaliDriver extends AbstractStorageDriver implements BlockStorageDriver {

    private static final Logger _log = LoggerFactory.getLogger(DenaliDriver.class);
    private Integer numPools = 2;

    @Override
    public DriverTask createVolumes(List<StorageVolume> volumes, StorageCapabilities capabilities) {
        String taskType = "create-storage-volumes";
        String driverName = this.getClass().getSimpleName();
        String taskId = String.format("%s+%s+%s", driverName, taskType, UUID.randomUUID().toString());
        DriverTask task = new DenaliTask(taskId);
        task.setStatus(DriverTask.TaskStatus.READY);
        String msg = String.format("%s: %s --- operation is supported.", driverName, "createVolumes");
        _log.warn(msg);
        task.setMessage(msg);
	/*
	String vol_name = "Volume Name";
	String ip = "10.10.30.235";
	String size = "1G";
	String tag = "tag";
	String pool_name = "Test";
        try{
                URL url = new URL("http://localhost:5000/volume");
                HttpURLConnection conn = (HttpURLConnection)url.openConnection();
                conn.setDoOutput(true);
                conn.setRequestMethod( "POST" );
                conn.addRequestProperty("Content-Type", "application/json");
                OutputStream os = conn.getOutputStream();
                OutputStreamWriter osw = new OutputStreamWriter(os, "UTF-8");
                String message = "{\"ip\":\""+ip+"\",\"pool\":\""+pool_name+"\",\"size\":\""+size+"\",\"tag\":\""+tag+"\",\"name\":\""+vol_name+"\"}";
                System.out.println(message);
                osw.write(message);
                osw.flush();
                osw.close();
                System.out.println(conn.getResponseCode());
                StringBuilder sb = getStringBuilder(conn);
                System.out.println(sb.toString());
        } catch (IOException e){
                e.getStackTrace();
                System.out.println("error");
        }*/
        return task;
    }


    @Override
    public DriverTask getStorageVolumes(StorageSystem storageSystem, List<StorageVolume> storageVolumes, MutableInt token) {
        String taskType = "get-storage-volumes";
        String driverName = this.getClass().getSimpleName();
        String taskId = String.format("%s+%s+%s", driverName, taskType, UUID.randomUUID().toString());
        DriverTask task = new DenaliTask(taskId);
        task.setStatus(DriverTask.TaskStatus.FAILED);

        String msg = String.format("%s: %s --- operation is not supported.", driverName, "getStorageVolumes");
        _log.warn(msg);
        task.setMessage(msg);
        return task;
    }

    public static StringBuilder getStringBuilder(HttpURLConnection con){
          StringBuilder sb = new StringBuilder();
          try{

                BufferedReader br = new BufferedReader(new InputStreamReader(con.getInputStream(), "UTF-8"));
                String line = null;
                while ((line = br.readLine()) != null){
                        sb.append(line+"\n");
                }
                br.close();
         } catch(IOException e){
                 e.printStackTrace();
         }
         return sb;
    }


    @Override
    public List<VolumeSnapshot> getVolumeSnapshots(StorageVolume volume) {
        String driverName = this.getClass().getSimpleName();
        String msg = String.format("%s: %s --- operation is not supported.", driverName, "getVolumeSnapshots");
        _log.warn(msg);
        throw new UnsupportedOperationException(msg);
    }

    @Override
    public List<VolumeClone> getVolumeClones(StorageVolume volume) {
        String driverName = this.getClass().getSimpleName();
        String msg = String.format("%s: %s --- operation is not supported.", driverName, "getVolumeClones");
        _log.warn(msg);
        throw new UnsupportedOperationException(msg);
    }

    @Override
    public List<VolumeMirror> getVolumeMirrors(StorageVolume volume) {
        String driverName = this.getClass().getSimpleName();
        String msg = String.format("%s: %s --- operation is not supported.", driverName, "getVolumeMirrors");
        _log.warn(msg);
        throw new UnsupportedOperationException(msg);
    }

    @Override
    public DriverTask expandVolume(StorageVolume volume, long newCapacity) {
        String taskType = "expandVolume";
        String driverName = this.getClass().getSimpleName();
        String taskId = String.format("%s+%s+%s", driverName, taskType, UUID.randomUUID().toString());
        DriverTask task = new DenaliTask(taskId);
        task.setStatus(DriverTask.TaskStatus.FAILED);

        String msg = String.format("%s: %s --- operation is not supported.", driverName, "expandVolume");
        _log.warn(msg);
        task.setMessage(msg);
        return task;
    }

    @Override
    public DriverTask deleteVolumes(List<StorageVolume> volumes) {
        String taskType = "delete-storage-volumes";
        String driverName = this.getClass().getSimpleName();
        String taskId = String.format("%s+%s+%s", driverName, taskType, UUID.randomUUID().toString());
        DriverTask task = new DenaliTask(taskId);
        task.setStatus(DriverTask.TaskStatus.FAILED);

        String msg = String.format("%s: %s --- operation is not supported.", driverName, "deleteVolumes");
        _log.warn(msg);
        task.setMessage(msg);
        return task;
    }

    @Override
    public DriverTask createVolumeSnapshot(List<VolumeSnapshot> snapshots, StorageCapabilities capabilities) {
        String taskType = "create-volume-snapshot";
        String driverName = this.getClass().getSimpleName();
        String taskId = String.format("%s+%s+%s", driverName, taskType, UUID.randomUUID().toString());
        DriverTask task = new DenaliTask(taskId);
        task.setStatus(DriverTask.TaskStatus.FAILED);

        String msg = String.format("%s: %s --- operation is not supported.", driverName, "createVolumeSnapshot");
        _log.warn(msg);
        task.setMessage(msg);
        return task;
    }

    @Override
    public DriverTask restoreSnapshot(List<VolumeSnapshot> snapshots) {
        String driverName = this.getClass().getSimpleName();
        String taskId = String.format("%s+%s+%s", driverName, "restoreSnapshot", UUID.randomUUID().toString());
        DriverTask task = new DenaliTask(taskId);
        task.setStatus(DriverTask.TaskStatus.FAILED);

        String msg = String.format("%s: %s --- operation is not supported.", driverName, "restoreSnapshot");
        _log.warn(msg);
        task.setMessage(msg);
        return task;
    }
    
    @Override
    public DriverTask stopManagement(StorageSystem driverStorageSystem){
    	_log.info("Stopping management for StorageSystem {}", driverStorageSystem.getNativeId());
    	String driverName = this.getClass().getSimpleName();
        String taskId = String.format("%s+%s+%s", driverName, "stopManagement", UUID.randomUUID().toString());
        DriverTask task = new DenaliTask(taskId);
        task.setStatus(DriverTask.TaskStatus.FAILED);
        
        String msg = String.format("%s: %s --- operation is not supported.", driverName, "stopManagement");
        _log.warn(msg);
        task.setMessage(msg);
        return task;
    }

    @Override
    public DriverTask deleteVolumeSnapshot(List<VolumeSnapshot> snapshots) {
        String driverName = this.getClass().getSimpleName();
        String taskId = String.format("%s+%s+%s", driverName, "deleteVolumeSnapshot", UUID.randomUUID().toString());
        DriverTask task = new DenaliTask(taskId);
        task.setStatus(DriverTask.TaskStatus.FAILED);

        String msg = String.format("%s: %s --- operation is not supported.", driverName, "deleteVolumeSnapshot");
        _log.warn(msg);
        task.setMessage(msg);
        return task;
    }

    @Override
    public DriverTask createVolumeClone(List<VolumeClone> clones, StorageCapabilities capabilities) {
        String driverName = this.getClass().getSimpleName();
        String taskId = String.format("%s+%s+%s", driverName, "createVolumeClone", UUID.randomUUID().toString());
        DriverTask task = new DenaliTask(taskId);
        task.setStatus(DriverTask.TaskStatus.FAILED);

        String msg = String.format("%s: %s --- operation is not supported.", driverName, "createVolumeClone");
        _log.warn(msg);
        task.setMessage(msg);
        return task;
    }

    @Override
    public DriverTask detachVolumeClone(List<VolumeClone> clones) {
        String driverName = this.getClass().getSimpleName();
        String taskId = String.format("%s+%s+%s", driverName, "detachVolumeClone", UUID.randomUUID().toString());
        DriverTask task = new DenaliTask(taskId);
        task.setStatus(DriverTask.TaskStatus.FAILED);

        String msg = String.format("%s: %s --- operation is not supported.", driverName, "detachVolumeClone");
        _log.warn(msg);
        task.setMessage(msg);
        return task;
    }

    @Override
    public DriverTask restoreFromClone(List<VolumeClone> clones) {
        String driverName = this.getClass().getSimpleName();
        String taskId = String.format("%s+%s+%s", driverName, "restoreFromClone", UUID.randomUUID().toString());
        DriverTask task = new DenaliTask(taskId);
        task.setStatus(DriverTask.TaskStatus.FAILED);

        String msg = String.format("%s: %s --- operation is not supported.", driverName, "restoreFromClone");
        _log.warn(msg);
        task.setMessage(msg);
        return task;
    }

    @Override
    public DriverTask deleteVolumeClone(List<VolumeClone> clones) {
        String driverName = this.getClass().getSimpleName();
        String taskId = String.format("%s+%s+%s", driverName, "deleteVolumeClone", UUID.randomUUID().toString());
        DriverTask task = new DenaliTask(taskId);
        task.setStatus(DriverTask.TaskStatus.FAILED);

        String msg = String.format("%s: %s --- operation is not supported.", driverName, "deleteVolumeClone");
        _log.warn(msg);
        task.setMessage(msg);
        return task;
    }

    @Override
    public DriverTask createVolumeMirror(List<VolumeMirror> mirrors, StorageCapabilities capabilities) {
        String driverName = this.getClass().getSimpleName();
        String taskId = String.format("%s+%s+%s", driverName, "createVolumeMirror", UUID.randomUUID().toString());
        DriverTask task = new DenaliTask(taskId);
        task.setStatus(DriverTask.TaskStatus.FAILED);

        String msg = String.format("%s: %s --- operation is not supported.", driverName, "createVolumeMirror");
        _log.warn(msg);
        task.setMessage(msg);
        return task;
    }

    @Override
    public DriverTask createConsistencyGroupMirror(VolumeConsistencyGroup consistencyGroup, List<VolumeMirror> mirrors, List<CapabilityInstance> capabilities) {
        String driverName = this.getClass().getSimpleName();
        String taskId = String.format("%s+%s+%s", driverName, "createConsistencyGroupMirror", UUID.randomUUID().toString());
        DriverTask task = new DenaliTask(taskId);
        task.setStatus(DriverTask.TaskStatus.FAILED);

        String msg = String.format("%s: %s --- operation is not supported.", driverName, "createConsistencyGroupMirror");
        _log.warn(msg);
        task.setMessage(msg);
        return task;
    }

    @Override
    public DriverTask deleteVolumeMirror(List<VolumeMirror> mirrors) {
        String driverName = this.getClass().getSimpleName();
        String taskId = String.format("%s+%s+%s", driverName, "deleteVolumeMirror", UUID.randomUUID().toString());
        DriverTask task = new DenaliTask(taskId);
        task.setStatus(DriverTask.TaskStatus.FAILED);

        String msg = String.format("%s: %s --- operation is not supported.", driverName, "deleteVolumeMirror");
        _log.warn(msg);
        task.setMessage(msg);
        return task;
    }

    @Override
    public DriverTask deleteConsistencyGroupMirror(List<VolumeMirror> mirrors) {
        String driverName = this.getClass().getSimpleName();
        String taskId = String.format("%s+%s+%s", driverName, "deleteConsistencyGroupMirror", UUID.randomUUID().toString());
        DriverTask task = new DenaliTask(taskId);
        task.setStatus(DriverTask.TaskStatus.FAILED);

        String msg = String.format("%s: %s --- operation is not supported.", driverName, "deleteConsistencyGroupMirror");
        _log.warn(msg);
        task.setMessage(msg);
        return task;
    }
    
    @Override
    public DriverTask addVolumesToConsistencyGroup (List<StorageVolume> volumes, StorageCapabilities capabilities){
    	_log.info("addVolumesToConsistencyGroup : unsupported operation.");
    	String driverName = this.getClass().getSimpleName();
        String taskType = "add-volumes-to-consistency-groupd";
        String taskId = String.format("%s+%s+%s", driverName, taskType, UUID.randomUUID().toString());
        DriverTask task = new DenaliTask(taskId);
        task.setStatus(DriverTask.TaskStatus.FAILED);
        
        String msg = String.format("addVolumesToConsistencyGroup: unsupported operation");
        _log.info(msg);
        task.setMessage(msg);
        
        return task;
    }
    
    @Override
    public DriverTask removeVolumesFromConsistencyGroup(List<StorageVolume> volumes,  StorageCapabilities capabilities){
    	_log.info("removeVolumesFromConsistencyGroup : unsupported operation.");
        String taskType = "remove-volumes-to-consistency-groupd";
        String driverName = this.getClass().getSimpleName();
        String taskId = String.format("%s+%s+%s", driverName, taskType, UUID.randomUUID().toString());
        DriverTask task = new DenaliTask(taskId);
        task.setStatus(DriverTask.TaskStatus.FAILED);
        
        String msg = String.format("removeVolumesFromConsistencyGroup: unsupported operation");
        _log.info(msg);
        task.setMessage(msg);
        
        return task;
    }

    @Override
    public DriverTask splitVolumeMirror(List<VolumeMirror> mirrors) {
        String driverName = this.getClass().getSimpleName();
        String taskId = String.format("%s+%s+%s", driverName, "splitVolumeMirror", UUID.randomUUID().toString());
        DriverTask task = new DenaliTask(taskId);
        task.setStatus(DriverTask.TaskStatus.FAILED);

        String msg = String.format("%s: %s --- operation is not supported.", driverName, "splitVolumeMirror");
        _log.warn(msg);
        task.setMessage(msg);
        return task;
    }

    @Override
    public DriverTask resumeVolumeMirror(List<VolumeMirror> mirrors) {
        String driverName = this.getClass().getSimpleName();
        String taskId = String.format("%s+%s+%s", driverName, "resumeVolumeMirror", UUID.randomUUID().toString());
        DriverTask task = new DenaliTask(taskId);
        task.setStatus(DriverTask.TaskStatus.FAILED);

        String msg = String.format("%s: %s --- operation is not supported.", driverName, "resumeVolumeMirror");
        _log.warn(msg);
        task.setMessage(msg);
        return task;
    }

    @Override
    public DriverTask restoreVolumeMirror(List<VolumeMirror> mirrors) {
        String driverName = this.getClass().getSimpleName();
        String taskId = String.format("%s+%s+%s", driverName, "restoreVolumeMirror", UUID.randomUUID().toString());
        DriverTask task = new DenaliTask(taskId);
        task.setStatus(DriverTask.TaskStatus.FAILED);

        String msg = String.format("%s: %s --- operation is not supported.", driverName, "restoreVolumeMirror");
        _log.warn(msg);
        task.setMessage(msg);
        return task;
    }

    @Override
    public Map<String, HostExportInfo> getVolumeExportInfoForHosts(StorageVolume volume) {
        String driverName = this.getClass().getSimpleName();
        String msg = String.format("%s: %s --- operation is not supported.", driverName, "getVolumeExportInfoForHosts");
        _log.warn(msg);
        throw new UnsupportedOperationException(msg);
    }

    @Override
    public Map<String, HostExportInfo> getSnapshotExportInfoForHosts(VolumeSnapshot snapshot) {
        String driverName = this.getClass().getSimpleName();
        String msg = String.format("%s: %s --- operation is not supported.", driverName, "getSnapshotExportInfoForHosts");
        _log.warn(msg);
        throw new UnsupportedOperationException(msg);
    }

    @Override
    public Map<String, HostExportInfo> getCloneExportInfoForHosts(VolumeClone clone) {
        String driverName = this.getClass().getSimpleName();
        String msg = String.format("%s: %s --- operation is not supported.", driverName, "getCloneExportInfoForHosts");
        _log.warn(msg);
        throw new UnsupportedOperationException(msg);
    }

    @Override
    public Map<String, HostExportInfo> getMirrorExportInfoForHosts(VolumeMirror mirror) {
        String driverName = this.getClass().getSimpleName();
        String msg = String.format("%s: %s --- operation is not supported.", driverName, "getMirrorExportInfoForHosts");
        _log.warn(msg);
        throw new UnsupportedOperationException(msg);
    }

    @Override
    public DriverTask exportVolumesToInitiators(List<Initiator> initiators, List<StorageVolume> volumes, Map<String, String> volumeToHLUMap, List<StoragePort> recommendedPorts, List<StoragePort> availablePorts, StorageCapabilities capabilities, MutableBoolean usedRecommendedPorts, List<StoragePort> selectedPorts) {
        String driverName = this.getClass().getSimpleName();
        String taskId = String.format("%s+%s+%s", driverName, "exportVolumesToInitiators", UUID.randomUUID().toString());
        DriverTask task = new DenaliTask(taskId);
        task.setStatus(DriverTask.TaskStatus.FAILED);

        String msg = String.format("%s: %s --- operation is not supported.", driverName, "exportVolumesToInitiators");
        _log.warn(msg);
        task.setMessage(msg);
        return task;
    }

    @Override
    public DriverTask unexportVolumesFromInitiators(List<Initiator> initiators, List<StorageVolume> volumes) {
        String driverName = this.getClass().getSimpleName();
        String taskId = String.format("%s+%s+%s", driverName, "unexportVolumesFromInitiators", UUID.randomUUID().toString());
        DriverTask task = new DenaliTask(taskId);
        task.setStatus(DriverTask.TaskStatus.FAILED);

        String msg = String.format("%s: %s --- operation is not supported.", driverName, "unexportVolumesFromInitiators");
        _log.warn(msg);
        task.setMessage(msg);
        return task;
    }

    @Override
    public DriverTask createConsistencyGroup(VolumeConsistencyGroup consistencyGroup) {
        String driverName = this.getClass().getSimpleName();
        String taskId = String.format("%s+%s+%s", driverName, "createConsistencyGroup", UUID.randomUUID().toString());
        DriverTask task = new DenaliTask(taskId);
        task.setStatus(DriverTask.TaskStatus.FAILED);

        String msg = String.format("%s: %s --- operation is not supported.", driverName, "createConsistencyGroup");
        _log.warn(msg);
        task.setMessage(msg);
        return task;
    }

    @Override
    public DriverTask deleteConsistencyGroup(VolumeConsistencyGroup consistencyGroup) {
        String driverName = this.getClass().getSimpleName();
        String taskId = String.format("%s+%s+%s", driverName, "deleteConsistencyGroup", UUID.randomUUID().toString());
        DriverTask task = new DenaliTask(taskId);
        task.setStatus(DriverTask.TaskStatus.FAILED);

        String msg = String.format("%s: %s --- operation is not supported.", driverName, "deleteConsistencyGroup");
        _log.warn(msg);
        task.setMessage(msg);
        return task;
    }

    @Override
    public DriverTask createConsistencyGroupSnapshot(VolumeConsistencyGroup consistencyGroup, List<VolumeSnapshot> snapshots, List<CapabilityInstance> capabilities) {
        String driverName = this.getClass().getSimpleName();
        String taskId = String.format("%s+%s+%s", driverName, "createConsistencyGroupSnapshot", UUID.randomUUID().toString());
        DriverTask task = new DenaliTask(taskId);
        task.setStatus(DriverTask.TaskStatus.FAILED);

        String msg = String.format("%s: %s --- operation is not supported.", driverName, "createConsistencyGroupSnapshot");
        _log.warn(msg);
        task.setMessage(msg);
        return task;
    }

    @Override
    public DriverTask deleteConsistencyGroupSnapshot(List<VolumeSnapshot> snapshots) {
        String driverName = this.getClass().getSimpleName();
        String taskId = String.format("%s+%s+%s", driverName, "deleteConsistencyGroupSnapshot", UUID.randomUUID().toString());
        DriverTask task = new DenaliTask(taskId);
        task.setStatus(DriverTask.TaskStatus.FAILED);

        String msg = String.format("%s: %s --- operation is not supported.", driverName, "deleteConsistencyGroupSnapshot");
        _log.warn(msg);
        task.setMessage(msg);
        return task;
    }

    @Override
    public DriverTask createConsistencyGroupClone(VolumeConsistencyGroup consistencyGroup, List<VolumeClone> clones, List<CapabilityInstance> capabilities) {
        String driverName = this.getClass().getSimpleName();
        String taskId = String.format("%s+%s+%s", driverName, "createConsistencyGroupClone", UUID.randomUUID().toString());
        DriverTask task = new DenaliTask(taskId);
        task.setStatus(DriverTask.TaskStatus.FAILED);

        String msg = String.format("%s: %s --- operation is not supported.", driverName, "createConsistencyGroupClone");
        _log.warn(msg);
        task.setMessage(msg);
        return task;
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
        this.driverRegistry.setDriverAttributesForKey("StorageDriverSimulator", systemNativeId, attributes);
    }

    @Override
    public DriverTask discoverStorageSystem(StorageSystem storageSystem) {
        String driverName = this.getClass().getSimpleName();
        String taskId = String.format("%s+%s+%s", driverName, "discoverStorageSystem", UUID.randomUUID().toString());
        DriverTask task = new DenaliTask(taskId);
        task.setStatus(DriverTask.TaskStatus.READY);
	try {
        	if (storageSystem.getSerialNumber() == null) {
            		storageSystem.setSerialNumber(storageSystem.getSystemName());
            	}
            	if (storageSystem.getNativeId() == null) {
            		storageSystem.setNativeId(storageSystem.getSystemName());
            	}
            	storageSystem.setFirmwareVersion("2.4-3.12");
            	storageSystem.setIsSupportedVersion(true);
            	setConnInfoToRegistry(storageSystem.getNativeId(), storageSystem.getIpAddress(), storageSystem.getPortNumber(), storageSystem.getUsername(), storageSystem.getPassword());
            	// Support both, element and group replicas.
            	Set<StorageSystem.SupportedReplication> supportedReplications = new HashSet<>();
            	supportedReplications.add(StorageSystem.SupportedReplication.elementReplica);
            	supportedReplications.add(StorageSystem.SupportedReplication.groupReplica);
            	storageSystem.setSupportedReplications(supportedReplications);
        } catch (Exception e) {
            	task.setStatus(DriverTask.TaskStatus.FAILED);
            	e.printStackTrace();
        }
        String msg = String.format("%s: %s --- operation is not supported.", driverName, "discoverStorageSystem");
        _log.warn(msg);
        task.setMessage(msg);
        return task;
    }

    @Override
    public DriverTask discoverStoragePools(StorageSystem storageSystem, List<StoragePool> storagePools) {
        String driverName = this.getClass().getSimpleName();
        String taskId = String.format("%s+%s+%s", driverName, "discoverStoragePools", UUID.randomUUID().toString());
        DriverTask task = new DenaliTask(taskId);
        task.setStatus(DriverTask.TaskStatus.READY);
        try {
            // Get connection information.
            Map<String, List<String>> connectionInfo = driverRegistry.getDriverAttributesForKey("StorageDriverSimulator", storageSystem.getNativeId());
            _log.info("Storage system connection info: {} : {}", storageSystem.getNativeId(), connectionInfo);
            for (int i = 0; i < numPools; i++ ) {
                StoragePool pool = new StoragePool();
                pool.setNativeId("Denali-pool-"+ i +"-"+ storageSystem.getNativeId());
                pool.setStorageSystemId(storageSystem.getNativeId());
                _log.info("Discovered Pool {}, storageSystem {}", pool.getNativeId(), pool.getStorageSystemId());
                pool.setDeviceLabel("Denali-pool" + i +"-"+ storageSystem.getNativeId());
                pool.setPoolName(pool.getDeviceLabel());
                Set<StoragePool.Protocols> protocols = new HashSet<>();
                protocols.add(StoragePool.Protocols.iSCSI);
                pool.setProtocols(protocols);
                pool.setPoolServiceType(StoragePool.PoolServiceType.block);
                pool.setMaximumThickVolumeSize(3000000L);
                pool.setMinimumThickVolumeSize(1000L);
                pool.setMaximumThinVolumeSize(5000000L);
                pool.setMinimumThinVolumeSize(1000L);
                pool.setSupportedResourceType(StoragePool.SupportedResourceType.THIN_AND_THICK);

                pool.setSubscribedCapacity(5000000L);
                pool.setFreeCapacity(45000000L);
                pool.setTotalCapacity(48000000L);
                pool.setOperationalStatus(StoragePool.PoolOperationalStatus.READY);
                Set<StoragePool.SupportedDriveTypes> supportedDriveTypes = new HashSet<>();
		supportedDriveTypes.add(StoragePool.SupportedDriveTypes.SSD);
                pool.setSupportedDriveTypes(supportedDriveTypes);

                Set<StoragePool.RaidLevels> raidLevels = new HashSet<>();
                raidLevels.add(StoragePool.RaidLevels.RAID1);
                raidLevels.add(StoragePool.RaidLevels.RAID2);
		raidLevels.add(StoragePool.RaidLevels.RAID50);
		raidLevels.add(StoragePool.RaidLevels.RAID60);
                pool.setSupportedRaidLevels(raidLevels);

                storagePools.add(pool);
            }
	} catch (Exception e) {
            task.setStatus(DriverTask.TaskStatus.FAILED);
            e.printStackTrace();
        }

	String msg = String.format("%s: %s --- operation is not supported.", driverName, "discoverStoragePools");
        _log.warn(msg);
        task.setMessage(msg);
        return task;
    }

    @Override
    public DriverTask discoverStoragePorts(StorageSystem storageSystem, List<StoragePort> storagePorts) {
	_log.info("Discovery of storage ports for storage system {} .", storageSystem.getNativeId());
        int index = 0;
        // Get "portIndexes" attribute map
        Map<String, List<String>> portIndexes = driverRegistry.getDriverAttributesForKey("simulatordriver", "portIndexes");
        if (portIndexes != null) {
            List<String>  indexes = portIndexes.get(storageSystem.getNativeId());
            if (indexes != null) {
                index = Integer.parseInt(indexes.get(0));
                _log.info("Storage ports index for storage system {} is {} .", storageSystem.getNativeId(), index);
            }
        }

        if (index == 0) {
            // no index for this system in the registry
            // get the last used index and increment by 1 to generate an index
            if (portIndexes != null) {
                List<String> indexes = portIndexes.get("lastIndex");
                if (indexes != null) {
                    index = Integer.parseInt(indexes.get(0)) + 1;
                } else {
                    index ++;
                }
            } else {
                index ++;
            }
            // set this index for the system in registry
            driverRegistry.addDriverAttributeForKey("simulatordriver", "portIndexes", storageSystem.getNativeId(), Collections.singletonList(String.valueOf(index)));
            driverRegistry.addDriverAttributeForKey("simulatordriver", "portIndexes", "lastIndex", Collections.singletonList(String.valueOf(index)));
            _log.info("Storage ports index for storage system {} is {} .", storageSystem.getNativeId(), index);
        }
	/*
        // Create ports with network
        for (int i =0; i <= 2; i++ ) {
            StoragePort port = new StoragePort();
            port.setNativeId("port-denali-" + i + storageSystem.getNativeId());
            port.setStorageSystemId(storageSystem.getNativeId());
            _log.info("Discovered Port {}, storageSystem {}", port.getNativeId(), port.getStorageSystemId());

            port.setDeviceLabel("er-port-denali-" + i + storageSystem.getNativeId());
            port.setPortName(port.getDeviceLabel());
            port.setNetworkId("11");
            port.setTransportType(StoragePort.TransportType.IP);
            port.setPortNetworkId("6" + Integer.toHexString(index) + ":FE:FE:FE:FE:FE:FE:1" + i);
            port.setOperationalStatus(StoragePort.OperationalStatus.OK);
            port.setPortHAZone("zone-"+i);
            storagePorts.add(port);
        }
	*/
        // Create ports without network
        for (int i =0; i <= 3; i++ ) {
            StoragePort port = new StoragePort();
            port.setNativeId("port-denali-" + i+ storageSystem.getNativeId());
            port.setStorageSystemId(storageSystem.getNativeId());
            _log.info("Discovered Port {}, storageSystem {}", port.getNativeId(), port.getStorageSystemId());

            port.setDeviceLabel("er-port-denali" + i+ storageSystem.getNativeId());
            port.setPortName(port.getDeviceLabel());
            port.setTransportType(StoragePort.TransportType.FC);
            port.setPortNetworkId("6" + Integer.toHexString(index) + ":FE:FE:FE:FE:FE:FE:1" + i);
            port.setOperationalStatus(StoragePort.OperationalStatus.OK);
            port.setPortHAZone("zone-with-many-ports");
            storagePorts.add(port);
        }

        String taskType = "discover-storage-ports";
        String taskId = String.format("%s+%s+%s", "DenaliDriver", taskType, UUID.randomUUID().toString());
        DriverTask task = new DenaliTask(taskId);
        task.setStatus(DriverTask.TaskStatus.READY);
        _log.info("StorageDriver: discoverStoragePorts information for storage system {}, nativeId {} - end", storageSystem.getIpAddress(), storageSystem.getNativeId());
        return task;
    }        

    @Override
    public DriverTask discoverStorageHostComponents(StorageSystem storageSystem, List<StorageHostComponent> embeddedStorageHostComponents) {
        String driverName = this.getClass().getSimpleName();
        String taskId = String.format("%s+%s+%s", driverName, "discoverStorageHostComponents", UUID.randomUUID().toString());
        DriverTask task = new DenaliTask(taskId);
        task.setStatus(DriverTask.TaskStatus.FAILED);

        String msg = String.format("%s: %s --- operation is not supported.", driverName, "discoverStorageHostComponents");
        _log.warn(msg);
        task.setMessage(msg);
        return task;
    }

    @Override
    public DriverTask discoverStorageProvider(StorageProvider storageProvider, List<StorageSystem> storageSystems) {
        String driverName = this.getClass().getSimpleName();
        String taskId = String.format("%s+%s+%s", driverName, "discover-storage-provider", UUID.randomUUID().toString());
        DriverTask task = new DenaliTask(taskId);
        task.setStatus(DriverTask.TaskStatus.FAILED);

        String msg = String.format("%s: %s --- operation is not supported.", driverName, "discoverStorageProvider");
        _log.warn(msg);
        task.setMessage(msg);
        return task;
    }

    @Override
    public RegistrationData getRegistrationData() {
        String driverName = this.getClass().getSimpleName();
        String msg = String.format("%s: %s --- operation is not supported.", driverName, "getRegistrationData");
        _log.warn(msg);
        throw new UnsupportedOperationException(msg);
    }

    @Override
    public DriverTask getTask(String taskId) {
        String driverName = this.getClass().getSimpleName();
        String msg = String.format("%s: %s --- operation is not supported.", driverName, "getTask");
        _log.warn(msg);
        throw new UnsupportedOperationException(msg);
    }

    @Override
    public <T extends StorageObject> T getStorageObject(String storageSystemId, String objectId, Class<T> type) {
        String driverName = this.getClass().getSimpleName();
        String msg = String.format("%s: %s --- operation is not supported.", driverName, "getStorageObject");
        _log.warn(msg);
        throw new UnsupportedOperationException(msg);
    }

    @Override
    public boolean validateStorageProviderConnection(StorageProvider storageProvider) {
        String driverName = this.getClass().getSimpleName();
        String msg = String.format("%s: %s --- operation is not supported.", driverName, "validateStorageProviderConnection");
        _log.warn(msg);
        throw new UnsupportedOperationException(msg);
    }
}
