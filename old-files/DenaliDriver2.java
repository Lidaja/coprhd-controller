package com.emc.storageos.storagedriver;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.python.util.PythonInterpreter;
import org.python.core.*;

import com.emc.storageos.storagedriver.BlockStorageDriver;
import com.emc.storageos.storagedriver.model.StorageHostComponent;
import org.apache.commons.lang.mutable.MutableBoolean;
import org.apache.commons.lang.mutable.MutableInt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.emc.storageos.storagedriver.AbstractStorageDriver;
import com.emc.storageos.storagedriver.DriverTask;
import com.emc.storageos.storagedriver.DenaliTask;
import com.emc.storageos.storagedriver.RegistrationData;
//import com.emc.storageos.storagedriver.model.ITL;
import com.emc.storageos.storagedriver.model.Initiator;
import com.emc.storageos.storagedriver.model.StorageObject;
import com.emc.storageos.storagedriver.model.StoragePool;
import com.emc.storageos.storagedriver.model.StoragePort;
import com.emc.storageos.storagedriver.model.StorageSystem;
import com.emc.storageos.storagedriver.model.StorageProvider;
import com.emc.storageos.storagedriver.model.StorageVolume;
import com.emc.storageos.storagedriver.model.VolumeClone;
import com.emc.storageos.storagedriver.model.VolumeConsistencyGroup;
import com.emc.storageos.storagedriver.model.VolumeMirror;
import com.emc.storageos.storagedriver.model.VolumeSnapshot;
import com.emc.storageos.storagedriver.storagecapabilities.CapabilityInstance;
import com.emc.storageos.storagedriver.storagecapabilities.CapabilityDefinition;
import com.emc.storageos.storagedriver.storagecapabilities.StorageCapabilities;

public class DenaliDriver extends AbstractStorageDriver implements BlockStorageDriver {

	private static final Logger _log = LoggerFactory.getLogger(DenaliDriver.class);
	private static final String DRIVER_NAME = "DenaliDriver";
	private static final String STORAGE_DEVICE_ID = "DenaliStorage-420";
	private static Integer portIndex = 0;
	private static Integer numPools = 3;
	private static Integer numPorts = 3;
	private static Integer numNetPorts = 3;
	private static Long maxThick = 1000000L;
	private static Long minThick = 300L;
	private static Long maxThin = 2000000L;
	private static Long minThin = 300L;
	private static Long subCap = 50000000L;
	private static Long freeCap = 50000000L;
	private static Long totCap = 48000000L;	
	private static Map<String, Integer> systemNameToPortIndexName = new HashMap<>();

	private static PythonInterpreter interp = new PythonInterpreter();

    	/**
     	* Get list of supported storage system types. Ex. vmax, vnxblock, hitachi, etc...
     	* @return list of supported storage system types
     	*/
    	public List<String> getSystemTypes(){
		List<String> systemTypes = new ArrayList<String>();
		systemTypes.add("denali");
		return systemTypes;
	}


    	/**
     	* Return driver task with a given id.
     	*
     	* @param taskId
     	* @return
     	*/
    	public DriverTask getTask(String taskId){
		DriverTask Task = new DenaliTask(taskId);
		return Task;
	}



    	/**
     	* Get storage object with a given type with specified native ID which belongs to specified storage system
     	*
     	* @param storageSystemId storage system native id
     	* @param objectId object native id
     	* @param type  class instance
     	* @param <T> storage object type
     	* @return storage object or null if does not exist
     	*
     	* Example of usage:
     	*    StorageVolume volume = StorageDriver.getStorageObject("vmax-12345", "volume-1234", StorageVolume.class);
     	*/
	
    	public <T extends StorageObject> T getStorageObject(String storageSystemId, String objectId, Class<T> type){
		try{
			T S = type.newInstance();
			return S;
		} catch (InstantiationException e){
			System.out.println("oopsies");
			return null;
		} catch (IllegalAccessException e){
			System.out.println("oopsies again");
			return null;
		}
	}



	// Block Volume operations
    	/**
     	* Create storage volumes with a given set of capabilities.
	* Before completion of the request, set all required data for provisioned volumes in "volumes" parameter.
	*
	* @param volumes Input/output argument for volumes.
	* @param capabilities Input argument for capabilities. Defines storage capabilities of volumes to create.
	* @return task
	*/
	
	@Override
	public DriverTask createVolumes(List<StorageVolume> volumes, StorageCapabilities capabilities){
		Set<String> newVolumes = new HashSet<>();
		for (StorageVolume volume : volumes){
			volume.setNativeId("DenaliVolume" + UUID.randomUUID().toString());
			volume.setAccessStatus(StorageVolume.AccessStatus.READ_WRITE);
			volume.setProvisionedCapacity(volume.getRequestedCapacity());
			volume.setAllocatedCapacity(volume.getRequestedCapacity());
			volume.setDeviceLabel(volume.getNativeId());
			volume.setWwn(String.format("%s%s", volume.getStorageSystemId(),volume.getNativeId()));
			newVolumes.add(volume.getNativeId());
		}
		String taskType = "create-storage-volumes";
		String taskId = String.format("%s+%s+%s", DRIVER_NAME, taskType,UUID.randomUUID().toString());
		DriverTask Task = new DenaliTask(taskId);
		Task.setStatus(DenaliTask.TaskStatus.READY);
       		String msg = String.format("StorageDriver: createVolumes information for storage system %s, volume nativeIds %s - end",volumes.get(0).getStorageSystemId(), newVolumes.toString());
        	_log.info(msg);
		_log.info("TEST MESSAGE");
		_log.debug("Another Test");
        	Task.setMessage(msg);
        	return Task;

	}
    	/**
     	* Expand volume.
     	* Before completion of the request, set all required data for expanded volume in "volume" parameter.
     	* This includes update for capacity properties based on the new volume size:
     	*                                         requestedCapacity, provisionedCapacity, allocatedCapacity.
     	*
     	* @param volume  Volume to expand. Type: Input/Output argument.
     	* @param newCapacity  Requested capacity in bytes. Type: input argument.
     	* @return task
     	*/
    	public DriverTask expandVolume(StorageVolume volume, long newCapacity){
        	String taskType = "expand-storage-volumes";
        	String taskId = String.format("%s+%s+%s", DRIVER_NAME, taskType,UUID.randomUUID().toString());
        	volume.setRequestedCapacity(newCapacity);
        	volume.setProvisionedCapacity(newCapacity);
        	volume.setAllocatedCapacity(newCapacity);
        	DriverTask Task = new DenaliTask(taskId);
        	Task.setStatus(DriverTask.TaskStatus.READY);
        	_log.info("StorageDriver: expandVolume information for storage system {},volume nativeId {}, " + "new capacity {} - end", volume.getStorageSystemId(), volume.toString(), volume.getRequestedCapacity());
        	return Task;
	}

    	/**
     	* Delete volumes.
     	* @param volumes Volumes to delete.
     	* @return task
     	*/
	@Override
    	public DriverTask deleteVolumes(List<StorageVolume> volumes){
		for (StorageVolume volume : volumes){
			//volume = null;
		}
		//volumes = null;
        	String taskType = "delete-storage-volumes";
        	String taskId = String.format("%s+%s+%s", DRIVER_NAME, taskType, UUID.randomUUID().toString());
        	DriverTask Task = new DenaliTask(taskId);
        	Task.setStatus(DriverTask.TaskStatus.READY);
        	_log.info("StorageDriver:  deleteVolumes information for storage system {},volume nativeIds {} - end", volumes.get(0).getStorageSystemId(),volumes.toString());
		_log.info("TEST DELETE");
		_log.debug("TEST DELETE AGAIN");
        	return Task;
	}




    	// Block Snapshot operations

    	/**
     	* Create volume snapshots.
     	*
     	* @param snapshots Type: Input/Output.
     	* @param capabilities capabilities required from snapshots. Type: Input.
     	* @return task
     	*/
	
	@Override
    	public DriverTask createVolumeSnapshot(List<VolumeSnapshot> snapshots, StorageCapabilities capabilities){
		String snapTimestamp = Long.toString(System.currentTimeMillis());
		for (VolumeSnapshot snapshot : snapshots){
			snapshot.setCustomCapabilities(capabilities.getCustomCapabilities());
			snapshot.setCommonCapabilities(capabilities.getCommonCapabilities());
			snapshot.setNativeId("snap-" + snapshot.getParentId() + UUID.randomUUID().toString());
			//snapshot.setTimestamp(snapTimestamp);
		}
		String taskType = "create-volume-snapshot";
		String taskId = String.format("%s+%s+%s", DRIVER_NAME, taskType, UUID.randomUUID().toString());
		DriverTask Task = new DenaliTask(taskId);
		Task.setStatus(DriverTask.TaskStatus.READY);
		_log.info("StorageDriver: createVolumeSnapshot informatiion for storage system {}, snapshots native Ids {} - end", snapshots.get(0).getStorageSystemId(), snapshots.toString());
		return Task;
	}



    	/**
     	* Restore volume to snapshot state.
     	* Implementation should check if the volume is part of consistency group and restore
     	* all volumes in the consistency group to the same consistency group snapshot (as defined
     	* by the snapshot parameter).
     	* If the volume is not part of consistency group, restore this volume to the snapshot.
    	*
     	* @param volume Type: Input/Output.
     	* @param snapshot  Type: Input.
     	* @return task
     	*/
	
    	/**
     	* Delete snapshots.
     	* @param snapshots Type: Input.

     	* @return task
     	*/
	
	@Override
    	public DriverTask deleteVolumeSnapshot(List<VolumeSnapshot> snapshots){
		for (VolumeSnapshot snapshot : snapshots){
			//snapshot = null;
		}
		//snapshots = null;
		String taskType = "delete-volume-snapshot";
		String taskId = String.format("%s+%s+%s", DRIVER_NAME, taskType, UUID.randomUUID().toString());
		DriverTask Task = new DenaliTask(taskId);
		Task.setStatus(DriverTask.TaskStatus.READY);
		String msg = String.format("StorageDriver: deleteVolumeSnapshot for storage system %s, " + " snapshots nativeId %s - end", snapshots.get(0).getStorageSystemId(), snapshots.toString());
		_log.info(msg);
		Task.setMessage(msg);
		return Task;
	}



	// Block clone operations

	/**
     	* Clone volume clones.
     	* @param clones  Type: Input/Output.
     	* @param capabilities capabilities of clones. Type: Input.
     	* @return task
     	*/
	@Override
    	public DriverTask createVolumeClone(List<VolumeClone> clones, StorageCapabilities capabilities){
		for (VolumeClone clone : clones) { 
			clone.setNativeId("clone-" + clone.getParentId() + clone.getDisplayName());
			clone.setWwn(String.format("%s%s", clone.getStorageSystemId(), clone.getNativeId()));
			clone.setReplicationState(VolumeClone.ReplicationState.SYNCHRONIZED);
			clone.setDeviceLabel(clone.getNativeId());
		}
		String taskType = "create-volume-clone";
		String taskId = String.format("%s+%s+%s", DRIVER_NAME, taskType,UUID.randomUUID().toString());
		DriverTask Task = new DenaliTask(taskId);
		Task.setStatus(DriverTask.TaskStatus.READY);
		
		String msg = String.format("StorageDriver: createVolumeClone information for storage system %s, clone nativeIds %s - end", clones.get(0).getStorageSystemId(), clones.toString());
		_log.info(msg);
		Task.setMessage(msg);
		return Task;
	}



	/**
     	* Detach volume clones.
     	* It is implementation responsibility to validate consistency of this operation
     	* when clones belong to consistency groups.
     	*
     	* @param clones Type: Input/Output.
     	* @return task
     	*/
	@Override
    	public DriverTask detachVolumeClone(List<VolumeClone> clones){
		for (VolumeClone clone : clones){
			//DETACH CLONE
		}
		String taskType = "detach-volume-clone";
		String taskId = String.format("%s+%s+%s", DRIVER_NAME, taskType, UUID.randomUUID().toString());
		DriverTask Task = new DenaliTask(taskId);
		Task.setStatus(DriverTask.TaskStatus.READY);
		String msg = String.format("StorageDriver: detachVolumeClone for storage system %s, " +  "clones nativeId %s - end", clones.get(0).getStorageSystemId(), clones.toString());
		_log.info(msg);
		Task.setMessage(msg);
		return Task;
	}

	/**
     	* Delete volume clones.
     	*
     	* @param clones clones to delete. Type: Input.
     	* @return
     	*/
    	public DriverTask deleteVolumeClone(List<VolumeClone> clones){
		for (VolumeClone clone : clones){
			//clone = null;
		}
		//clones = null;
	        String taskType = "delete-volume-clone";
        	String taskId = String.format("%s+%s+%s", DRIVER_NAME, taskType, UUID.randomUUID().toString());
	        DriverTask Task = new DenaliTask(taskId);
        	Task.setStatus(DriverTask.TaskStatus.READY);
        	String msg = String.format("StorageDriver: deleteVolumeClone for storage system %s, " + "clones nativeId %s - end", clones.get(0).getStorageSystemId(), clones.toString());
	        _log.info(msg);
        	Task.setMessage(msg);
        	return Task;
	}


    	/**
     	* Restore from clone.
     	*
     	* It is implementation responsibility to validate consistency of this operation
     	* when clones belong to consistency groups.
     	*
     	* @param clones   Clones to restore from. Type: Input/Output.
     	* @return task
     	*/
	@Override
    	public DriverTask restoreFromClone(List<VolumeClone> clones){
        String taskType = "restore-volume-clones";
        String taskId = String.format("%s+%s+%s", DRIVER_NAME, taskType, UUID.randomUUID().toString());
        DriverTask Task = new DenaliTask(taskId);
        Task.setStatus(DriverTask.TaskStatus.READY);
        String msg = String.format("StorageDriver: restoreFromClone : clones %s ", clones);
        for (VolumeClone clone : clones) {
            clone.setReplicationState(VolumeClone.ReplicationState.RESTORED);
        }
        _log.info(msg);
        Task.setMessage(msg);
        return Task;
	}



	// Block Mirror operations

    	/**
     	* Create volume mirrors.
     	*
     	* @param mirrors  Type: Input/Output.
     	* @param capabilities capabilities of mirrors. Type: Input.
     	* @return task
     	*/
	@Override
    	public DriverTask createVolumeMirror(List<VolumeMirror> mirrors, StorageCapabilities capabilities){
		for (VolumeMirror mirror : mirrors){
			mirror.setCustomCapabilities(capabilities.getCustomCapabilities());
			mirror.setCommonCapabilities(capabilities.getCommonCapabilities());
		}
                String taskType = "create-volume-mirrors";
                String taskId = String.format("%s+%s+%s", DRIVER_NAME, taskType,UUID.randomUUID().toString());
                DriverTask Task = new DenaliTask(taskId);
                Task.setStatus(DenaliTask.TaskStatus.READY);
                String msg = String.format("StorageDriver: createVolumeMirrors information for storage system %s, volume nativeIds %s - end",mirrors.get(0).getStorageSystemId(), mirrors.toString());
                _log.info(msg);
                Task.setMessage(msg);
                return Task;
	}



    	/**
     	* Delete mirrors.
     	*
     	* @param mirrors mirrors to delete. Type: Input.
     	* @return task
     	*/
	@Override
    	public DriverTask deleteVolumeMirror(List<VolumeMirror> mirrors){
		for (VolumeMirror mirror : mirrors){
			//mirror = null;
		}
		//mirrors = null;
                String taskType = "delete-volume-mirror";
                String taskId = String.format("%s+%s+%s", DRIVER_NAME, taskType, UUID.randomUUID().toString());
		DriverTask Task = new DenaliTask(taskId);
		String msg = String.format("StorageDriver: deleteVolumeMirror for strorage system %s, " + "mirrors nativeId %s - end", mirrors.get(0).getStorageSystemId(), mirrors.toString());
		_log.info(msg);
		Task.setMessage(msg);
		return Task;
	}



	/**
     	* Split mirrors
     	* @param mirrors  Type: Input/Output.
     	* @return task
     	*/
	@Override
    	public DriverTask splitVolumeMirror(List<VolumeMirror> mirrors){
		for (VolumeMirror mirror : mirrors){
			//SPLIT MIRROR
		}
                String taskType = "split-volume-mirror";
                String taskId = String.format("%s+%s+%s", DRIVER_NAME, taskType, UUID.randomUUID().toString());
                DriverTask Task = new DenaliTask(taskId);
                String msg = String.format("StorageDriver: splitVolumeMirror for strorage system %s, " + "mirrors native Id %s - end", mirrors.get(0).getStorageSystemId(), mirrors.toString());
                _log.info(msg);
                Task.setMessage(msg);
                return Task;
	}



    	/**
     	* Resume mirrors after split
     	*
     	* @param mirrors  Type: Input/Output.
     	* @return task
     	*/
	@Override
    	public DriverTask resumeVolumeMirror(List<VolumeMirror> mirrors){
		for (VolumeMirror mirror : mirrors){
			//RESUME MIRROR
		}
                String taskType = "resume-volume-mirror";
                String taskId = String.format("%s+%s+%s", DRIVER_NAME, taskType, UUID.randomUUID().toString());
                DriverTask Task = new DenaliTask(taskId);
                String msg = String.format("StorageDriver: resumeVolumeMirror for strorage system %s, " + "mirrors nativeId %s - end", mirrors.get(0).getStorageSystemId(), mirrors.toString());
                _log.info(msg);
                Task.setMessage(msg);
                return Task;
	}

    	// Block Export operations

	/**
     	* Get export masks for a given set of initiators.
     	*
     	* @param storageSystem Storage system to get ITLs from. Type: Input.
     	* @param initiators Type: Input.
     	* @return list of export masks
     	*/
	/*
	@Override
    	public List<ITL> getITL(StorageSystem storageSystem, List<Initiator> initiators){
		List<ITL> ITLS = new ArrayList<ITL>();
		return ITLS;
	}
	*/
	/**
     	* Export volumes to initiators through a given set of ports. If ports are not provided,
     	* use port requirements from ExportPathsServiceOption storage capability
     	*
     	* @param initiators Type: Input.
     	* @param volumes    Type: Input.
     	* @param volumeToHLUMap map of volume nativeID to requested HLU. HLU value of -1 means that HLU is not defined and will be assigned by array.
     	*                       Type: Input/Output.
     	* @param recommendedPorts list of storage ports recommended for the export. Optional. Type: Input.
     	* @param availablePorts list of ports available for the export. Type: Input.
     	* @param capabilities storage capabilities. Type: Input.
     	* @param usedRecommendedPorts true if driver used recommended and only recommended ports for the export, false otherwise. Type: Output.
     	* @param selectedPorts ports selected for the export (if recommended ports have not been used). Type: Output.
     	* @return task
     	*/
	@Override
    	public DriverTask exportVolumesToInitiators(List<Initiator> initiators, List<StorageVolume> volumes, Map<String, String> volumeToHLUMap, List<StoragePort> recommendedPorts, List<StoragePort> availablePorts, StorageCapabilities capabilities, MutableBoolean usedRecommendedPorts, List<StoragePort> selectedPorts){
		usedRecommendedPorts.setValue(true);
		selectedPorts.addAll(recommendedPorts);
		String taskType = "export-volumes-to-initiators";
		String taskId = String.format("%s+%s+%s", DRIVER_NAME, taskType, UUID.randomUUID().toString());
		DriverTask Task = new DenaliTask(taskId);
		Task.setStatus(DriverTask.TaskStatus.READY);
		String msg = String.format("StorageDriver: exportVolumesToInitiators - end");
		_log.info(msg);
		Task.setMessage(msg);
		return Task;
	}


	/**
     	* Unexport volumes from initiators
     	*
     	* @param initiators  Type: Input.
     	* @param volumes     Type: Input.
     	* @return task
     	*/
	@Override
    	public DriverTask unexportVolumesFromInitiators(List<Initiator> initiators, List<StorageVolume> volumes){
        	String taskType = "unexport-volumes-from-initiators";
        	String taskId = String.format("%s+%s+%s", DRIVER_NAME, taskType, UUID.randomUUID().toString());
	        DriverTask Task = new DenaliTask(taskId);
	        Task.setStatus(DriverTask.TaskStatus.READY);
        	String msg = String.format("StorageDriver: unexportVolumesFromInitiators - end");
        	_log.info(msg);
        	Task.setMessage(msg);
        	return Task;
	}

    	// Consistency group operations.
    	/**
     	* Create block consistency group.
     	* @param consistencyGroup input/output
     	* @return
     	*/
	@Override
    	public DriverTask createConsistencyGroup(VolumeConsistencyGroup consistencyGroup){
		consistencyGroup.setNativeId(consistencyGroup.getDisplayName());
		consistencyGroup.setDeviceLabel(consistencyGroup.getDisplayName());
		String taskType = "create-volume-cg";
		String taskId = String.format("%s+%s+%s", DRIVER_NAME, taskType, UUID.randomUUID().toString());
		DriverTask Task = new DenaliTask(taskId);
        	Task.setStatus(DriverTask.TaskStatus.READY);
        	_log.info("StorageDriver: createConsistencyGroup information for storage system {}, consistencyGroup nativeId {} - end", consistencyGroup.getStorageSystemId(), consistencyGroup.getNativeId());
		return Task;
	}



	/**
     	* Delete block consistency group.
     	* @param consistencyGroup Input
     	* @return
     	*/
	@Override
    	public DriverTask deleteConsistencyGroup(VolumeConsistencyGroup consistencyGroup){
        	String taskType = "delete-volume-cg";
        	String taskId = String.format("%s+%s+%s", DRIVER_NAME, taskType, UUID.randomUUID().toString());
        	DriverTask Task = new DenaliTask(taskId);
        	Task.setStatus(DriverTask.TaskStatus.READY);
        	String msg = String.format("StorageDriver: deleteConsistencyGroup information for storage system %s, consistencyGroup nativeId %s - end", consistencyGroup.getStorageSystemId(), consistencyGroup.getNativeId());
        	_log.info(msg);
        	Task.setMessage(msg);
        	return Task;
	}

    	/**
     	* Create snapshot of consistency group.
     	* @param consistencyGroup input parameter
     	* @param snapshots   input/output parameter
    	* @param capabilities Capabilities of snapshots. Type: Input.
     	* @return
     	*/
	@Override
    	public DriverTask createConsistencyGroupSnapshot(VolumeConsistencyGroup consistencyGroup, List<VolumeSnapshot> snapshots,
                                            					 List<CapabilityInstance> capabilities){
	        String snapTimestamp = Long.toString(System.currentTimeMillis());
        	for (VolumeSnapshot snapshot : snapshots) {
            		snapshot.setNativeId("snap-" + snapshot.getParentId() + consistencyGroup.getDisplayName() + UUID.randomUUID().toString());
            		//snapshot.setTimestamp(snapTimestamp);
        	}
        	String taskType = "create-group-snapshot";
        	String taskId = String.format("%s+%s+%s", DRIVER_NAME, taskType, UUID.randomUUID().toString());
        	DriverTask Task = new DenaliTask(taskId);
        	Task.setStatus(DriverTask.TaskStatus.READY);
        	Task.setMessage("Created snapshots for consistency group " + snapshots.get(0).getConsistencyGroup());
        	_log.info("StorageDriver: createGroupSnapshot information for storage system {}, snapshots nativeIds {} - end", snapshots.get(0).getStorageSystemId(), snapshots.toString());
        	return Task;
	}



    	/**
     	* Delete snapshot.
     	* @param snapshots  Input.
     	* @return
     	*/
	@Override
    	public DriverTask deleteConsistencyGroupSnapshot(List<VolumeSnapshot> snapshots){
		DriverTask Task = new DenaliTask("Delete");
		return Task;
	}



    	/**
     	* Create clone of consistency group.
     	* It is implementation responsibility to validate consistency of this group operation.
     	*
     	* @param consistencyGroup input
     	* @param clones input/output
     	* @param capabilities Capabilities of clones. Type: Input.
     	* @return
     	*/
	@Override
    	public DriverTask createConsistencyGroupClone(VolumeConsistencyGroup consistencyGroup, List<VolumeClone> clones,        									List<CapabilityInstance> capabilities){
        	consistencyGroup.setNativeId(consistencyGroup.getDisplayName());
        	consistencyGroup.setDeviceLabel(consistencyGroup.getDisplayName());
        	String taskType = "create-volume-cg";
        	String taskId = String.format("%s+%s+%s", DRIVER_NAME, taskType, UUID.randomUUID().toString());
        	DriverTask Task = new DenaliTask(taskId);
        	Task.setStatus(DriverTask.TaskStatus.READY);
        	_log.info("StorageDriver: createConsistencyGroup information for storage system {}, consistencyGroup nativeId {} - end", consistencyGroup.getStorageSystemId(), consistencyGroup.getNativeId());
        	return Task;
	}



	public RegistrationData getRegistrationData(){
		RegistrationData Data = new RegistrationData("test","test",null);
                return Data;
	}
    	/**
     	*  Get driver registration data.
     	*/
   	public RegistrationData getRegistrationData(String name, String type,Set<CapabilityDefinition> capabilities){
		RegistrationData Data = new RegistrationData(name,type,capabilities);
		return Data;
	}

    	/**
     	* Discover storage systems and their capabilities
     	*
     	* @param storageSystems StorageSystems to discover. Type: Input/Output.
     	* @return
     	*/
	@Override
    	public DriverTask discoverStorageSystem(List<StorageSystem> storageSystems){
		StorageSystem storageSystem = storageSystems.get(0);
		_log.info("StorageDriver: discoverStorageSystem informtion for storage system {}, name {} - start", storageSystem.getIpAddress(), storageSystem.getSystemName());
		String taskType = "discover-storage-system";
		String taskId = String.format("%s+%s+%s",DRIVER_NAME, taskType, UUID.randomUUID().toString());
		DriverTask Task = new DenaliTask(taskId);
        	try {
            		storageSystem.setSerialNumber(storageSystem.getSystemName());
            		storageSystem.setNativeId(storageSystem.getSystemName());
		        storageSystem.setFirmwareVersion("2.4-3.12");
            		storageSystem.setIsSupportedVersion(true);
            		// Support both, element and group replicas.
            		Set<StorageSystem.SupportedReplication> supportedReplications = new HashSet<>();
            		supportedReplications.add(StorageSystem.SupportedReplication.elementReplica);
            		supportedReplications.add(StorageSystem.SupportedReplication.groupReplica);
            		storageSystem.setSupportedReplications(supportedReplications);

            		Task.setStatus(DriverTask.TaskStatus.READY);
            		_log.info("StorageDriver: discoverStorageSystem information for storage system {}, nativeId {} - end", storageSystem.getIpAddress(), storageSystem.getNativeId());
            		return Task;
        	} catch (Exception e) {
            		Task.setStatus(DriverTask.TaskStatus.FAILED);
            		e.printStackTrace();
        	}
        	return Task;

	}

    	/**
     	* Discover storage pools and their capabilities.
     	* @param storageSystem Type: Input.
     	* @param storagePools  Type: Output.
     	* @return
     	*/
   	@Override
 	public DriverTask discoverStoragePools(StorageSystem storageSystem, List<StoragePool> storagePools){
		_log.info("Discover of storage pools for storage system {} .", storageSystem.getNativeId());
		String taskType = "discover-storage-pools";
		String taskId = String.format("%s+%s+%s",DRIVER_NAME, taskType, UUID.randomUUID().toString());
		DriverTask Task = new DenaliTask(taskId);
		try {
			Map<String, List<String>> connectionInfo = driverRegistry.getDriverAttributesForKey("DenaliDriver",storageSystem.getNativeId());
			_log.info("Storage system connection info: {} : {}", storageSystem.getNativeId(), connectionInfo);
			for (int i = 0; i <= numPools; i++) {
				StoragePool pool = new StoragePool();
				pool.setNativeId("DenaliPool" + i + storageSystem.getNativeId());
				pool.setStorageSystemId(storageSystem.getNativeId());
				_log.info("Discovered Pool {}, storageSystem {}", pool.getNativeId(), pool.getStorageSystemId());
				pool.setDeviceLabel("er-pool" + i + storageSystem.getNativeId());
				pool.setPoolName(pool.getDeviceLabel());
				Set<StoragePool.Protocols> protocols = new HashSet<>();
				protocols.add(StoragePool.Protocols.iSCSI);
				pool.setProtocols(protocols);
				pool.setPoolServiceType(StoragePool.PoolServiceType.block);
				pool.setMaximumThickVolumeSize(maxThick);
				pool.setMinimumThickVolumeSize(minThick);
				pool.setMaximumThinVolumeSize(maxThin);
				pool.setMinimumThinVolumeSize(minThin);
				pool.setSupportedResourceType(StoragePool.SupportedResourceType.THIN_AND_THICK);
				pool.setSubscribedCapacity(subCap);
				pool.setFreeCapacity(freeCap);
				pool.setTotalCapacity(totCap);
				pool.setOperationalStatus(StoragePool.PoolOperationalStatus.READY);
				Set<StoragePool.SupportedDriveTypes> supportedDriveTypes = new HashSet<>();
				supportedDriveTypes.add(StoragePool.SupportedDriveTypes.SSD);
				pool.setSupportedDriveTypes(supportedDriveTypes);
				storagePools.add(pool);
			}
			Task.setStatus(DriverTask.TaskStatus.READY);
			_log.info("StorageDriver: discoverStoragePools information for storage system {}, nativeId {} - end", storageSystem.getIpAddress(), storageSystem.getNativeId());
		} catch (Exception e) {
			Task.setStatus(DriverTask.TaskStatus.FAILED);
			e.printStackTrace();
		}
		return Task;
	}

    	/**
     	* Discover storage ports and their capabilities
     	* @param storageSystem Type: Input.
     	* @param storagePorts  Type: Output.
     	* @return
     	*/
	@Override
    	public DriverTask discoverStoragePorts(StorageSystem storageSystem, List<StoragePort> storagePorts){
		_log.info("Discovery of storage ports for storage system {} .", storageSystem.getNativeId()); 
		Integer index = systemNameToPortIndexName.get(storageSystem.getNativeId());
		if(index == null){
			index = ++portIndex;
			systemNameToPortIndexName.put(storageSystem.getNativeId(), index);
		}
		//Ports with network
		for (int i = 0; i < numNetPorts ; i++){
			StoragePort port = new StoragePort();
			port.setNativeId("DenaliPort-" + i + storageSystem.getNativeId());
			port.setStorageSystemId(storageSystem.getNativeId());
			_log.info("Discovered Port {}, storageSystem {}", port.getNativeId(), port.getStorageSystemId());	
			port.setDeviceLabel("er-DenaliPort-" +i+storageSystem.getNativeId());
			port.setPortName(port.getDeviceLabel());
			port.setNetworkId("DenaliNetwork"+storageSystem.getNativeId());
			port.setTransportType(StoragePort.TransportType.IP);
			port.setPortNetworkId("6" + Integer.toHexString(index) + ":FE:FE:FE:FE:FE:FE:1" + i);
			port.setOperationalStatus(StoragePort.OperationalStatus.OK);
			port.setPortHAZone("zone-"+1);
			storagePorts.add(port);
		}
		//Ports without network
		for (int i = 0; i < numPorts; i++){
			StoragePort port = new StoragePort();
			port.setNativeId("DenaliPort-"+i+storageSystem.getNativeId());
       			port.setStorageSystemId(storageSystem.getNativeId());
            		_log.info("Discovered Port {}, storageSystem {}", port.getNativeId(), port.getStorageSystemId());
            		port.setDeviceLabel("er-DenaliPort-" + i+ storageSystem.getNativeId());
            		port.setPortName(port.getDeviceLabel());
		        port.setTransportType(StoragePort.TransportType.IP);
            		port.setPortNetworkId("6" + Integer.toHexString(index) + ":FE:FE:FE:FE:FE:FE:1" + i);
            		port.setOperationalStatus(StoragePort.OperationalStatus.OK);
            		port.setPortHAZone("zone-with-many-ports");
            		storagePorts.add(port);
		}
		String taskType = "Discover-storage-ports";
		String taskId = String.format("%s+%s+%s", DRIVER_NAME, taskType, UUID.randomUUID().toString());
		DriverTask Task = new DenaliTask(taskId);
		Task.setStatus(DriverTask.TaskStatus.READY);
		_log.info("StorageDriver: discoverStoragePorts information for storage system {}, nativeId {} - end", storageSystem.getIpAddress(), storageSystem.getNativeId());
		return Task;
	}


    	/**
     	* Discover host components which are part of storage system
     	*
     	* @param storageSystem Type: Input.
     	* @param embeddedStorageHostComponents Type: Output.
     	* @return
     	*/
	@Override
    	public DriverTask discoverStorageHostComponents(StorageSystem storageSystem, List<StorageHostComponent> embeddedStorageHostComponents){
		DriverTask Task = new DenaliTask("Discover");
		return Task;
	}


    	/**
     	* Discover storage volumes
     	* @param storageSystem  Type: Input.
     	* @param storageVolumes Type: Output.
     	* @param token used for paging. Input 0 indicates that the first page should be returned. Output 0 indicates
     	*              that last page was returned. Type: Input/Output.
     	* @return
     	*/
	@Override
    	public DriverTask getStorageVolumes(StorageSystem storageSystem, List<StorageVolume> storageVolumes, MutableInt token){
		DriverTask Task = new DenaliTask("get");
		return Task;
	}

    	public DriverTask discoverStorageProvider(StorageProvider storageProvider, List<StorageSystem> storageSystems){
		DriverTask Task = new DenaliTask("Discover");
		return Task;
	}


}
