/*
 * Copyright (c) 2013 EMC Corporation
 * All Rights Reserved
 */

package com.emc.storageos.volumecontroller.impl.block.taskcompleter;

import com.emc.storageos.db.client.DbClient;
import com.emc.storageos.db.client.model.BlockObject;
import com.emc.storageos.db.client.model.ExportGroup;
import com.emc.storageos.db.client.model.ExportMask;
import com.emc.storageos.db.client.model.Operation;
import com.emc.storageos.exceptions.DeviceControllerException;
import com.emc.storageos.svcs.errorhandling.model.ServiceCoded;
import com.emc.storageos.util.ExportUtils;
import com.emc.storageos.volumecontroller.impl.utils.ExportOperationContext;
import com.emc.storageos.workflow.WorkflowService;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.emc.storageos.volumecontroller.impl.smis.vmax.VmaxExportOperationContext.OPERATION_ADD_VOLUMES_TO_STORAGE_GROUP;

@SuppressWarnings("serial")
public class ExportMaskAddVolumeCompleter extends ExportTaskCompleter {
    private static final org.slf4j.Logger _log = LoggerFactory.getLogger(ExportMaskAddVolumeCompleter.class);
    private final List<URI> _volumes;
    private final Map<URI, Integer> _volumeMap;

    public ExportMaskAddVolumeCompleter(URI egUri, URI emUri, Map<URI, Integer> volumes,
            String task) {
        super(ExportGroup.class, egUri, emUri, task);
        _volumes = new ArrayList<>();
        _volumes.addAll(volumes.keySet());
        _volumeMap = new HashMap<>();
        _volumeMap.putAll(volumes);
    }

    @Override
    protected void complete(DbClient dbClient, Operation.Status status, ServiceCoded coded) throws DeviceControllerException {
        try {
            ExportGroup exportGroup = dbClient.queryObject(ExportGroup.class, getId());
            ExportMask exportMask = (getMask() != null) ?
                    dbClient.queryObject(ExportMask.class, getMask()) : null;

            if (exportMask == null) {
                _log.warn("Export mask was null for task {}", getOpId());
                return;
            }

            if (shouldUpdateDatabase(status)) {
                for (URI volumeURI : _volumes) {
                    BlockObject volume = BlockObject.fetch(dbClient, volumeURI);
                    _log.info(String.format("Done ExportMaskAddVolume - Id: %s, OpId: %s, status: %s",
                            getId().toString(), getOpId(), status.name()));
                    exportMask.removeFromExistingVolumes(volume);
                    exportMask.addToUserCreatedVolumes(volume);
                }

                exportMask.setCreatedBySystem(true);
                exportMask.addVolumes(_volumeMap);
                exportGroup.addExportMask(exportMask.getId());
                ExportUtils.reconcileHLUs(dbClient, exportGroup, exportMask, _volumeMap);

                dbClient.updateObject(exportMask);
                dbClient.updateObject(exportGroup);
            }

        } catch (Exception e) {
            _log.error(String.format("Failed updating status for ExportMaskAddVolume - Id: %s, OpId: %s",
                    getId().toString(), getOpId()), e);
        } finally {
            super.complete(dbClient, status, coded);
        }
    }

    /**
     * This completer may complete with a ready or error status.  In the case of an error status,
     * we can check the {@link ExportOperationContext} to see if the volumes were added and perform
     * the necessary database updates.
     *
     * @param status    Status of the operation.
     * @return          true, if the status is ready or the volumes were added despite error status.
     */
    private boolean shouldUpdateDatabase(Operation.Status status) {
        return wereVolumesAdded() || status == Operation.Status.ready;
    }

    private boolean wereVolumesAdded() {
        ExportOperationContext context = (ExportOperationContext) WorkflowService.getInstance().loadStepData(getOpId());

        if (context != null) {
            List<ExportOperationContext.ExportOperationContextOperation> operations = context.getOperations();

            if (operations != null) {
                for (ExportOperationContext.ExportOperationContextOperation operation : operations) {
                    // VMAX check
                    if (OPERATION_ADD_VOLUMES_TO_STORAGE_GROUP.equalsIgnoreCase(operation.getOperation())) {
                        // TODO Check arguments.
                        return true;
                    }
                }
            }
        }

        return false;
    }

}
