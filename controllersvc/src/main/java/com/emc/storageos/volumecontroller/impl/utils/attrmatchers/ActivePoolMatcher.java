/*
 * Copyright (c) 2013 EMC Corporation
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

import com.emc.storageos.db.client.model.DiscoveredDataObject.DiscoveryStatus;
import com.emc.storageos.db.client.model.DiscoveredDataObject.RegistrationStatus;
import com.emc.storageos.db.client.model.StoragePool;
import com.emc.storageos.volumecontroller.AttributeMatcher;
import com.google.common.base.Joiner;

/**
 * ActivePoolMatcher is responsible to check pool activeness, ready state
 * and its registration status.
 * 
 */
public class ActivePoolMatcher extends AttributeMatcher {

    private static final Logger _logger = LoggerFactory
            .getLogger(ActivePoolMatcher.class);

    @Override
    public List<StoragePool> matchStoragePoolsWithAttributeOn(List<StoragePool> pools, Map<String, Object> attributeMap,
            StringBuffer errorMessage) {
        List<StoragePool> matchedPools = new ArrayList<StoragePool>();
        // Filter out inactive/unregistered/non-ready pools.
        _logger.info("Active Pools Matcher Started : {}", Joiner.on("\t").join(getNativeGuidFromPools(pools)));
        Iterator<StoragePool> poolIterator = pools.iterator();
        while (poolIterator.hasNext()) {
            StoragePool pool = poolIterator.next();
            if (null == pool) {
                continue;
            } else if (!pool.getInactive()
                    && RegistrationStatus.REGISTERED.toString()
                            .equalsIgnoreCase(pool.getRegistrationStatus())
                    && StoragePool.PoolOperationalStatus.READY.toString()
                            .equalsIgnoreCase(pool.getOperationalStatus())
                    && !DiscoveryStatus.NOTVISIBLE.toString()
                            .equalsIgnoreCase(pool.getDiscoveryStatus())) {
                matchedPools.add(pool);
            }
        }

        if (CollectionUtils.isEmpty(matchedPools)) {
            errorMessage.append("No active storage pools are available. ");
            _logger.error(errorMessage.toString());
        }
        _logger.info("Active Pools Matcher Ended : {}", Joiner.on("\t").join(getNativeGuidFromPools(matchedPools)));
        return matchedPools;
    }

    @Override
    protected boolean isAttributeOn(Map<String, Object> attributeMap) {
        // Since this is a defaultMatcher, it should always return true.
        return true;
    }
}
