/*
 * Copyright (c) 2008-2014 EMC Corporation
 * All Rights Reserved
 */

package com.emc.storageos.volumecontroller.impl.utils.attrmatchers;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.CollectionUtils;

import com.emc.storageos.db.client.model.DiscoveredDataObject.CompatibilityStatus;
import com.emc.storageos.db.client.model.StoragePool;
import com.emc.storageos.volumecontroller.AttributeMatcher;
import com.google.common.base.Joiner;

public class CompatiblePoolMatcher extends AttributeMatcher {

    private static final Logger _logger = LoggerFactory.getLogger(CompatiblePoolMatcher.class);

    @Override
    protected boolean isAttributeOn(Map<String, Object> attributeMap) {
        // Since this is a defaultMatcher, it should always return true.
        return true;
    }

    @Override
    protected List<StoragePool> matchStoragePoolsWithAttributeOn(
            List<StoragePool> allPools, Map<String, Object> attributeMap,
            StringBuffer errorMessage) {
        List<StoragePool> matchedPools = new ArrayList<StoragePool>();
        // Filter out incompatible pools.
        _logger.info("Compatible Pools Matcher Started : {}", Joiner.on("\t").join(getNativeGuidFromPools(allPools)));
        Iterator<StoragePool> poolIterator = allPools.iterator();
        while (poolIterator.hasNext()) {
            StoragePool pool = poolIterator.next();
            if (null == pool) {
                continue;
            } else if (!CompatibilityStatus.INCOMPATIBLE.name().equalsIgnoreCase(pool.getCompatibilityStatus())) {
                matchedPools.add(pool);
            }

        }
        _logger.info("Compatible Pools Matcher Ended : {}", Joiner.on("\t").join(getNativeGuidFromPools(matchedPools)));

        if (CollectionUtils.isEmpty(matchedPools)) {
            errorMessage.append("No matching compatible stoarge pool found. ");
            _logger.error(errorMessage.toString());
        }
        return matchedPools;
    }

}
