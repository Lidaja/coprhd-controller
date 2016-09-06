package com.emc.storageos.driver.denali;

import com.emc.storageos.storagedriver.DriverTask;

public class DenaliTask extends DriverTask {

    public DenaliTask(String taskId) {
        super(taskId);
    }

    public DriverTask abort(DriverTask task) {
        DriverTask abortTaskTask = new DriverTask("AbortTask_"+getTaskId()) {
            public DriverTask abort(DriverTask task) {
                throw new UnsupportedOperationException("Cannot abort abort task");
            }
        };
        abortTaskTask.setStatus(TaskStatus.FAILED);
        abortTaskTask.setMessage("Operation is not supported for simulator tasks.");
        return abortTaskTask;
    }
}
