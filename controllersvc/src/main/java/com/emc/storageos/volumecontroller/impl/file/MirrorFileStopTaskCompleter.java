/*
 * Copyright (c) 2015-2016 EMC Corporation
 * All Rights Reserved
 */
package com.emc.storageos.volumecontroller.impl.file;

import java.net.URI;
import java.util.Collection;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.emc.storageos.db.client.DbClient;
import com.emc.storageos.db.client.model.FileShare;
import com.emc.storageos.db.client.model.NamedURI;
import com.emc.storageos.db.client.model.Operation.Status;
import com.emc.storageos.db.client.util.NullColumnValueGetter;
import com.emc.storageos.exceptions.DeviceControllerException;
import com.emc.storageos.services.OperationTypeEnum;
import com.emc.storageos.svcs.errorhandling.model.ServiceCoded;

public class MirrorFileStopTaskCompleter extends MirrorFileTaskCompleter {
    private static final Logger _log = LoggerFactory.getLogger(MirrorFileStopTaskCompleter.class);
    private Collection<FileShare> srcfileshares;
    private Collection<FileShare> tgtfileshares;

    public MirrorFileStopTaskCompleter(Class clazz, List<URI> ids, String opId) {
        super(clazz, ids, opId);
    }

    public MirrorFileStopTaskCompleter(Class clazz, URI id, String opId) {
        super(clazz, id, opId);
    }

    public MirrorFileStopTaskCompleter(URI sourceURI, URI targetURI, String opId) {
        super(sourceURI, targetURI, opId);
    }

    public void setFileShares(Collection<FileShare> srcfileshares, Collection<FileShare> tgtfileshares) {
        this.srcfileshares = srcfileshares;
        this.tgtfileshares = tgtfileshares;
    }

    @Override
    protected void complete(DbClient dbClient, Status status, ServiceCoded coded) throws DeviceControllerException {
        try {
            setDbClient(dbClient);

            switch (status) {

                case ready:

                    if (null != srcfileshares && null != tgtfileshares && !srcfileshares.isEmpty() && !tgtfileshares.isEmpty()) {
                        for (FileShare sourceFS : srcfileshares) {
                            sourceFS.setPersonality(NullColumnValueGetter.getNullStr());
                            sourceFS.setAccessState(FileShare.FileAccessState.READWRITE.name());
                            if (null != sourceFS.getMirrorfsTargets()) {
                                sourceFS.getMirrorfsTargets().clear();
                            }

                            dbClient.updateObject(sourceFS);
                        }

                        for (FileShare target : tgtfileshares) {
                            target.setPersonality(NullColumnValueGetter.getNullStr());
                            target.setAccessState(FileShare.FileAccessState.READWRITE.name());
                            target.setParentFileShare(new NamedURI(NullColumnValueGetter.getNullURI(), NullColumnValueGetter.getNullStr()));

                            dbClient.updateAndReindexObject(target);
                        }

                        FileShare target = tgtfileshares.iterator().next();
                        FileShare source = srcfileshares.iterator().next();

                        recordMirrorOperation(dbClient, OperationTypeEnum.STOP_FILE_MIRROR, status,
                                getSourceFileShare().getId().toString(),
                                getTargetFileShare().getId().toString());

                    }
                default:
                    _log.info("Unable to handle File Mirror Stop Operational status: {}", status);
            }

            recordMirrorOperation(dbClient, OperationTypeEnum.STOP_FILE_MIRROR, status, getSourceFileShare().getId().toString(),
                    getTargetFileShare().getId().toString());

        } catch (Exception e) {
            _log.error("Failed updating status. MirrorSessionStop {}, for task " + getOpId(), getId(), e);
        } finally {
            super.complete(dbClient, status, coded);
        }
    }

    @Override
    protected FileShare.MirrorStatus getFileMirrorStatusForSuccess() {
        return this.mirrorSyncStatus = FileShare.MirrorStatus.DETACHED;
    }

}