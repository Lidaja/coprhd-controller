/*
 * Copyright (c) 2013 EMC Corporation
 * All Rights Reserved
 */
package com.emc.storageos.volumecontroller.impl.utils.attrmatchers;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.CollectionUtils;

import com.emc.storageos.db.client.model.StoragePool;
import com.emc.storageos.db.client.model.StorageSystem;
import com.emc.storageos.volumecontroller.AttributeMatcher;

public class NotificationLimitMatcher extends AttributeMatcher {
    private static final Logger _logger = LoggerFactory
            .getLogger(NotificationLimitMatcher.class);

    @Override
    protected boolean isAttributeOn(Map<String, Object> attributeMap) {
        return (null != attributeMap && attributeMap.containsKey(Attributes.support_notification_limit
                .toString()));
    }

    @Override
    protected List<StoragePool> matchStoragePoolsWithAttributeOn(List<StoragePool> allPools, Map<String, Object> attributeMap,
            StringBuffer errorMessage) {
        List<StoragePool> filteredPools = new ArrayList<StoragePool>();
        _logger.info("started matching pools with notification limit.");
        for (StoragePool pool : allPools) {
            StorageSystem system = _objectCache.getDbClient().queryObject(StorageSystem.class, pool.getStorageDevice());
            if (system.getSupportNotificationLimit() != null
                    && system.getSupportNotificationLimit().equals(attributeMap.get(Attributes.support_notification_limit.name()))) {
                filteredPools.add(pool);
            }
        }
        if (CollectionUtils.isEmpty(filteredPools)) {
            errorMessage.append("No Matching pools found with support notification limit. ");
            _logger.error(errorMessage.toString());
        }
        return filteredPools;
    }

}
