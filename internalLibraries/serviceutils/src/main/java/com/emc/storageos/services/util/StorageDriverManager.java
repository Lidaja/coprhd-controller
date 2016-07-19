/*
 * Copyright (c) 2015 EMC Corporation
 * All Rights Reserved
 */

package com.emc.storageos.services.util;


import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

/**
 * This class contains data structures with storage system types of installed drivers.
 */

public class StorageDriverManager implements ApplicationContextAware {

    public static final String EXTERNAL_STORAGE_DEVICE = "externalBlockStorageDevice";
    public static final String STORAGE_DRIVER_MANAGER = "storageDriverManager";
    private static Logger log = LoggerFactory.getLogger(StorageDriverManager.class);

    private static ApplicationContext _context;
    @Override
    public void setApplicationContext(ApplicationContext appContext)
            throws BeansException {
        _context = appContext;
    }

    public static ApplicationContext getApplicationContext() {
        log.info("Context value: {}", _context);
        return _context;
    }

    private Map<String, String> storageSystemsMap = new HashMap<>();
    private Map<String, String> storageProvidersMap = new HashMap<>();
    private Set<String>  blockSystems = new HashSet<>();
    private Set<String>  fileSystems = new HashSet<>();
    private Set<String>  providerManaged = new HashSet<>();
    private Set<String>  directlyManaged = new HashSet<>();

    public boolean isBlockStorageSystem(String storageSystemType) {
        return blockSystems.contains(storageSystemType);
    }

    public boolean isFileStorageSystem(String storageSystemType) {
        return fileSystems.contains(storageSystemType);
    }

    public boolean isDriverManaged(String type) {
        return storageSystemsMap.values().contains(type) || storageProvidersMap.values().contains(type);
    }

    public boolean isProvider(String type) {
        return storageProvidersMap.values().contains(type);
    }

    public boolean isProviderStorageSystem(String storageSystemType) {
        return providerManaged.contains(storageSystemType);
    }

    public boolean isDirectlyManagedStorageSystem(String storageSystemType) {
        return directlyManaged.contains(storageSystemType);
    }

    public Map<String, String>  getStorageSystemsMap() {
        return storageSystemsMap;
    }

    public void setStorageSystemsMap(Map<String, String> storageSystems) {
        this.storageSystemsMap = storageSystems;
    }

    public Set<String> getBlockSystems() {
        return blockSystems;
    }

    public void setBlockSystems(Set<String> blockSystems) {
        this.blockSystems = blockSystems;
    }

    public Set<String> getFileSystems() {
        return fileSystems;
    }

    public void setFileSystems(Set<String> fileSystems) {
        this.fileSystems = fileSystems;
    }

    public Set<String> getProviderManaged() {
        return providerManaged;
    }

    public void setProviderManaged(Set<String> providerManaged) {
        this.providerManaged = providerManaged;
    }

    public Set<String> getDirectlyManaged() {
        return directlyManaged;
    }

    public void setDirectlyManaged(Set<String> directlyManaged) {
        this.directlyManaged = directlyManaged;
    }

    public void setStorageProvidersMap(Map<String, String> storageProvidersMap) {
        this.storageProvidersMap = storageProvidersMap;
    }

    public Map<String, String> getStorageProvidersMap() {
        return storageProvidersMap;
    }
}
