/*
 * Copyright (c) 2013 EMC Corporation
 * All Rights Reserved
 */

package com.emc.storageos.volumecontroller.impl.block.taskcompleter;

import java.net.URI;
import java.util.List;

import com.emc.storageos.db.client.DbClient;
import com.emc.storageos.db.client.model.Operation;
import com.emc.storageos.exceptions.DeviceControllerException;
import com.emc.storageos.svcs.errorhandling.model.ServiceCoded;
import com.emc.storageos.volumecontroller.TaskCompleter;

public abstract class AbstractZoneCompleter extends TaskCompleter {

    public AbstractZoneCompleter(Class clazz, URI uri, String opId) {
        super(clazz, uri, opId);
    }

    public AbstractZoneCompleter(Class clazz, List<URI> uris, String opId) {
        super(clazz, uris, opId);
    }

    @Override
    protected void complete(DbClient dbClient, Operation.Status status, ServiceCoded coded) throws DeviceControllerException {
        updateWorkflowStatus(status, coded);
    }
}
