/*
 * Copyright (c) 2015 EMC Corporation
 * All Rights Reserved
 */
package com.emc.storageos.volumecontroller.impl.utils.attrmatchers;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.CollectionUtils;

import com.emc.storageos.db.client.model.RemoteDirectorGroup.SupportedCopyModes;
import com.emc.storageos.db.client.model.StoragePool;
import com.emc.storageos.db.client.model.StoragePool.CopyTypes;
import com.emc.storageos.volumecontroller.AttributeMatcher;
import com.google.common.base.Joiner;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;

/**
 * FileCopyModeMatcher - is an attribute matcher to select storage pools
 * with given replication copy mode.
 * 
 */

public class FileCopyModeMatcher extends AttributeMatcher {
    private static final Logger _logger = LoggerFactory.getLogger(FileCopyModeMatcher.class);

    @Override
    protected boolean isAttributeOn(Map<String, Object> attributeMap) {
        return (null != attributeMap && attributeMap.containsKey(Attributes.remote_copy_mode.toString()));
    }

    @Override
    protected List<StoragePool> matchStoragePoolsWithAttributeOn(
            List<StoragePool> allPools, Map<String, Object> attributeMap,
            StringBuffer errorMessage) {

        _logger.info("Pools matching file replication copy mode  Started :  {} ",
                Joiner.on("\t").join(getNativeGuidFromPools(allPools)));

        // Group the storage pools by storage system
        List<StoragePool> matchedPools = new ArrayList<StoragePool>();
        String copyMode = SupportedCopyModes.ASYNCHRONOUS.toString();
        if (attributeMap.get(Attributes.remote_copy_mode.toString()) != null) {
            copyMode = (String) attributeMap.get(Attributes.file_replication_copy_mode.toString());
        }
        String copyType = getPoolCopyTypeFromCopyModes(copyMode);
        String sourceSystem = null;
        if (attributeMap.get(Attributes.source_storage_system.toString()) != null) {
            sourceSystem = (String) attributeMap.get(Attributes.source_storage_system.toString());
        }

        ListMultimap<URI, StoragePool> storageToPoolMap = ArrayListMultimap.create();
        for (StoragePool pool : allPools) {
            storageToPoolMap.put(pool.getStorageDevice(), pool);
        }

        for (StoragePool pool : allPools) {
            // Ignore the storage pools from same source system!!
            if (pool.getStorageDevice().toString().equalsIgnoreCase(sourceSystem)) {
                continue;
            }
            if (pool.getSupportedCopyTypes() != null && pool.getSupportedCopyTypes().contains(copyType)) {
                matchedPools.add(pool);
            }
        }
        
        if(CollectionUtils.isEmpty(matchedPools)){
            errorMessage.append(String.format("No matching storage pool found for copy mode %s and copy type %s. ", copyMode, copyType));
            _logger.error(errorMessage.toString());
        }
        _logger.info("Pools matching file replication copy mode  Ended: {}", Joiner.on("\t").join(getNativeGuidFromPools(matchedPools)));
        return matchedPools;
    }

    private String getPoolCopyTypeFromCopyModes(String supportedCopyMode) {
        String copyType = CopyTypes.ASYNC.name();
        if (SupportedCopyModes.SYNCHRONOUS.name().equalsIgnoreCase(supportedCopyMode)) {
            copyType = CopyTypes.SYNC.name();
        }
        return copyType;
    }
}