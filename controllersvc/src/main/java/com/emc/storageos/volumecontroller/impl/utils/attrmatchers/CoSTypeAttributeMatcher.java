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

import com.emc.storageos.db.client.model.StoragePool;
import com.emc.storageos.volumecontroller.AttributeMatcher;
import com.google.common.base.Joiner;

/**
 * CoSTypeAttributeMatcher is responsible to match pools.
 * In case of CoS, filter the pools by its type.
 * 
 */
public class CoSTypeAttributeMatcher extends AttributeMatcher {

    private static final Logger _logger = LoggerFactory
            .getLogger(CoSTypeAttributeMatcher.class);

    /**
     * Matches all the pools against CoS Type attribute.
     * 
     * @param pools : Storage Pools
     * @return list of pools matching.
     */
    @Override
    public List<StoragePool> matchStoragePoolsWithAttributeOn(List<StoragePool> pools, Map<String, Object> attributeMap,
            StringBuffer errorMessage) {
        _logger.info("Pools Matching VPool Type Started: {}", Joiner.on("\t").join(getNativeGuidFromPools(pools)));
        List<StoragePool> matchedPools = new ArrayList<StoragePool>();
        Iterator<StoragePool> poolIterator = pools.iterator();
        String vpoolType = attributeMap.get(Attributes.vpool_type.toString()).toString();
        while (poolIterator.hasNext()) {
            StoragePool pool = poolIterator.next();
            
            if (null != pool
                    && pool.getPoolServiceType().contains(vpoolType)) {
                matchedPools.add(pool);
            }
        }
        if (CollectionUtils.isEmpty(matchedPools)) {
            errorMessage.append(String.format("No matching pools found with Virtual Pool Type attribute : %s. ", vpoolType));
            _logger.error(errorMessage.toString());
        }
        _logger.info("Pools Matching VPoolType Ended: {}", Joiner.on("\t").join(getNativeGuidFromPools(matchedPools)));
        return matchedPools;
    }

    @Override
    protected boolean isAttributeOn(Map<String, Object> attributeMap) {
        return attributeMap.containsKey(Attributes.vpool_type.toString());
    }
}
