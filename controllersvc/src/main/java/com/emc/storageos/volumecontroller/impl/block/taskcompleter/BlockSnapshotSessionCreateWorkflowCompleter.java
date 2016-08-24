/*
 * Copyright (c) 2012 EMC Corporation
 * All Rights Reserved
 */
package com.emc.storageos.volumecontroller.impl.block.taskcompleter;

import java.net.URI;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.emc.storageos.db.client.DbClient;
import com.emc.storageos.db.client.URIUtil;
import com.emc.storageos.db.client.model.BlockObject;
import com.emc.storageos.db.client.model.BlockSnapshot;
import com.emc.storageos.db.client.model.BlockSnapshotSession;
import com.emc.storageos.db.client.model.Operation;
import com.emc.storageos.db.client.model.StringSet;
import com.emc.storageos.exceptions.DeviceControllerException;
import com.emc.storageos.services.OperationTypeEnum;
import com.emc.storageos.svcs.errorhandling.model.ServiceCoded;

/**
 * Task completer invoked when a workflow creating new BlockSnapshotSession
 * instances completes.
 */
@SuppressWarnings("serial")
public class BlockSnapshotSessionCreateWorkflowCompleter extends BlockSnapshotSessionCompleter {

    // Message constants.
    public static final String SNAPSHOT_SESSION_CREATE_SUCCESS_MSG = "Block Snapshot Session %s created for source %s";
    public static final String SNAPSHOT_SESSION_CREATE_FAIL_MSG = "Failed to create Block Snapshot Session %s for source %s";

    // A map of the BlockSnapshot instances linked to the session for the request.
    private final List<List<URI>> _sessionSnapshotURIs;

    // A logger.
    private static final Logger s_logger = LoggerFactory.getLogger(BlockSnapshotSessionCreateWorkflowCompleter.class);

    /**
     * Constructor
     * 
     * @param snapSessionURI The URI of the BlockSnapshotSession instance created in the request.
     * @param sessionSnapshotURIs A map of the BlockSnapshot URIs linked to the session for the request.
     * @param taskId The unique task identifier.
     */
    public BlockSnapshotSessionCreateWorkflowCompleter(URI snapSessionURI, List<List<URI>> sessionSnapshotURIs, String taskId) {
        super(snapSessionURI, taskId);
        _sessionSnapshotURIs = sessionSnapshotURIs;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void complete(DbClient dbClient, Operation.Status status, ServiceCoded coded) throws DeviceControllerException {
        try {
            BlockSnapshotSession snapSession = dbClient.queryObject(BlockSnapshotSession.class, getId());
            List<BlockObject> allSources = getAllSources(snapSession, dbClient);

            // Record the results.
            recordBlockSnapshotSessionOperation(dbClient, OperationTypeEnum.CREATE_SNAPSHOT_SESSION,
                    status, snapSession, allSources.get(0));

            // Update the status map of the snapshot session.
            switch (status) {
                case error:
                    // For those BlockSnapshot instances representing linked targets that
                    // were not successfully created and linked to the array snapshot
                    // represented by the BlockSnapshotSession instance, mark them inactive.
                    for (List<URI> snapshotURIs : _sessionSnapshotURIs) {
                        for (URI snapshotURI : snapshotURIs) {
                            // Successfully linked targets will be in the list of linked
                            // targets for the session.
                            StringSet linkedTargets = snapSession.getLinkedTargets();
                            if ((linkedTargets == null) || (!linkedTargets.contains(snapshotURI.toString()))) {
                                BlockSnapshot snapshot = dbClient.queryObject(BlockSnapshot.class, snapshotURI);
                                // If rollback was successful the snapshot would already have been
                                // marked inactive, so be sure to check for null.
                                if ((snapshot != null) && (!snapshot.getInactive())) {
                                    snapshot.setInactive(true);
                                    dbClient.updateObject(snapshot);
                                }
                            }
                        }
                    }

                    setErrorOnDataObject(dbClient, BlockSnapshotSession.class, getId(), coded);
                    for (BlockObject source : allSources) {
                        setErrorOnDataObject(dbClient, URIUtil.getModelClass(source.getId()), source.getId(), coded);
                    }
                    break;
                case ready:
                    setReadyOnDataObject(dbClient, BlockSnapshotSession.class, getId());
                    for (BlockObject source : allSources) {
                        setReadyOnDataObject(dbClient, URIUtil.getModelClass(source.getId()), source);
                    }
                    break;
                case suspended_error:
                    setSuspendedErrorOnDataObject(dbClient, BlockSnapshotSession.class, getId(), coded);
                    for (BlockObject source : allSources) {
                        setSuspendedErrorOnDataObject(dbClient, URIUtil.getModelClass(source.getId()), source, coded);
                    }
                    break;
                case suspended_no_error:
                    setSuspendedNoErrorOnDataObject(dbClient, BlockSnapshotSession.class, getId());
                    for (BlockObject source : allSources) {
                        setSuspendedNoErrorOnDataObject(dbClient, URIUtil.getModelClass(source.getId()), source);
                    }
                    break;
                default:
                    String errMsg = String.format("Unexpected status %s for completer for task %s", status.name(), getOpId());
                    s_logger.info(errMsg);
                    throw DeviceControllerException.exceptions.unexpectedCondition(errMsg);
            }

            s_logger.info("Done snapshot session create task {} with status: {}", getOpId(), status.name());
        } catch (Exception e) {
            s_logger.error("Failed updating status for snapshot session create task {}", getOpId(), e);
        } finally {
            super.complete(dbClient, status, coded);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected String getDescriptionOfResults(Operation.Status status, BlockObject sourceObj, BlockSnapshotSession snapSession) {
        return (status == Operation.Status.ready) ?
                String.format(SNAPSHOT_SESSION_CREATE_SUCCESS_MSG, snapSession.getLabel(), sourceObj.getLabel()) :
                String.format(SNAPSHOT_SESSION_CREATE_FAIL_MSG, snapSession.getLabel(), sourceObj.getLabel());
    }
}
