/*
 * Copyright (c) 2015 EMC Corporation
 * All Rights Reserved
 */
package com.emc.storageos.volumecontroller.impl.utils.attrmatchers;

import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.emc.storageos.db.client.model.StoragePool;
import com.emc.storageos.volumecontroller.AttributeMatcher;
import com.google.common.base.Joiner;

/**
 * Matcher is responsible for picking initial placement tier in VMAX FAST.
 * 
 */
public class VmaxFastInitialPlacementSelectionMatcher extends ConditionalAttributeMatcher {

    private static final Logger _logger = LoggerFactory
            .getLogger(VmaxFastInitialPlacementSelectionMatcher.class);

    private AttributeMatcher driveTypeMatcher;
    private AttributeMatcher raidLevelMatcher;

    public void setDriveTypeMatcher(AttributeMatcher driveTypeMatcher) {
        this.driveTypeMatcher = driveTypeMatcher;
    }

    public void setRaidLevelMatcher(AttributeMatcher raidLevelMatcher) {
        this.raidLevelMatcher = raidLevelMatcher;
    }

    @Override
    protected boolean isAttributeOn(Map<String, Object> attributeMap) {
        // If Raid level or Drive Type is not configured, then user hasn't selected
        // Initial Placement Tier, hence skip this matcher.
        if (!isRaidLevelOrDriveTypeConfigured(attributeMap)) {
            _logger.info("Raid or Drive Type Not configured");
            return false;
        }
        return isAutoTieringPolicyOn(attributeMap);
    }

    @Override
    protected List<StoragePool> matchStoragePoolsWithAttributeOn(
            List<StoragePool> allPools, Map<String, Object> attributeMap,
            StringBuffer errorMessage) {
        // run drive Type and Raid Level Matcher
        _logger.info("Finding Initial tier Placement with Pools {} Started", Joiner
                .on("\t").join(getNativeGuidFromPools(allPools)));
        List<StoragePool> filteredPools = raidLevelMatcher.runMatchStoragePools(allPools, attributeMap, errorMessage);
        filteredPools = driveTypeMatcher.runMatchStoragePools(filteredPools, attributeMap, errorMessage);
        // if matching pools is 0, then return all the Pools, randomly a tier will be chosen as initial placement tier.
        if (filteredPools.isEmpty()) {
            _logger.info("No Pools found matching initial placement Criteria ,returning all  Pools : {} ", Joiner
                    .on("\t").join(getNativeGuidFromPools(allPools)));
            return allPools;
        }
        // if matching pools, then return filtered Pools.
        _logger.info("{} Pools found matching initial placement Criteria ", Joiner
                .on("\t").join(getNativeGuidFromPools(filteredPools)));
        return filteredPools;

    }

}
