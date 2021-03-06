/*
 * Copyright (c) 2015-2016 EMC Corporation
 * All Rights Reserved
 */

package com.emc.storageos.volumecontroller.impl.file;

import java.net.URI;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.emc.storageos.db.client.DbClient;
import com.emc.storageos.db.client.model.FileShare;
import com.emc.storageos.db.client.model.Operation;
import com.emc.storageos.db.client.model.Operation.Status;
import com.emc.storageos.exceptions.DeviceControllerException;
import com.emc.storageos.services.OperationTypeEnum;
import com.emc.storageos.svcs.errorhandling.model.ServiceCoded;

public class MirrorFileModifyRPOTaskCompleter extends MirrorFileTaskCompleter {

    private static final long serialVersionUID = 1L;
    private static final Logger _log = LoggerFactory.getLogger(MirrorFileModifyRPOTaskCompleter.class);

    public MirrorFileModifyRPOTaskCompleter(Class<?> clazz, URI id, String opId) {
        super(clazz, id, opId);
    }

    @Override
    protected void complete(DbClient dbClient, Status status, ServiceCoded coded) throws DeviceControllerException {
        try {
            setDbClient(dbClient);
            recordMirrorOperation(dbClient, OperationTypeEnum.MODIFY_FILE_MIRROR_RPO, status, getSourceFileShare().getId().toString());
        } catch (Exception e) {
            _log.error("Failed updating status. MirrorSessionRefresh {}, for task " + getOpId(), getId(), e);
        } finally {
            setStatus(dbClient, status, coded);
            updateFileSystemStatus(dbClient, status);
        }
    }

    @Override
    protected void updateFileSystemStatus(DbClient dbClient, Operation.Status status) {
        try {
            if (Operation.Status.ready.equals(status)) {
                List<FileShare> fileshares = dbClient.queryObject(FileShare.class, getIds());
                dbClient.updateObject(fileshares);
                _log.info("Updated Mirror status for fileshares: {}", getIds());
            }
        } catch (Exception e) {
            _log.info("Not updating fileshare mirror link status for fileshares: {}", getIds(), e);
        }
    }
}
