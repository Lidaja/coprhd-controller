/*
 * Copyright (c) 2016 EMC Corporation
 * All Rights Reserved
 */

package com.emc.storageos.driver.denali;

import com.emc.storageos.storagedriver.DriverTask;

/**
 * Default implementation of DriverTask.
 */
public class DenaliTask extends DriverTask {

    public DenaliTask(String taskId) {
        super(taskId);
    }

    @Override
    public DriverTask abort(DriverTask task) {
        DriverTask abortTaskTask = new DriverTask("AbortTask_"+ task.getTaskId()) {
            public DriverTask abort(DriverTask task) {
                throw new UnsupportedOperationException("Cannot abort abort task");
            }
        };
        abortTaskTask.setStatus(TaskStatus.FAILED);
        abortTaskTask.setMessage("abort operation is not supported for default tasks.");
        return abortTaskTask;
    }
}
