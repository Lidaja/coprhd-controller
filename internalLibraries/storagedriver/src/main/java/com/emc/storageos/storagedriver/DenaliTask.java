package com.emc.storageos.storagedriver;

import java.util.Calendar;

public class DenaliTask extends DriverTask {
	private String key;
	public DenaliTask(String TaskId){
		super(TaskId);
	}
    	public DriverTask abort(DriverTask task) {
        	DriverTask abortTask = new DriverTask("Abort task: 1234") {
            		public DriverTask abort(DriverTask task) {
                		return null;
            		}
	        };
        	abortTask.setStatus(TaskStatus.FAILED);
	        abortTask.setMessage("Operation is not supported.");
        	return abortTask;
    	}
}

