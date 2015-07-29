/*
 * Copyright 2015 EMC Corporation
 * All Rights Reserved
 */
/**
 * Copyright (c) 2014 EMC Corporation
 * All Rights Reserved
 *
 * This software contains the intellectual property of EMC Corporation
 * or is licensed to EMC Corporation from third parties.  Use of this
 * software and the intellectual property contained therein is expressly
 * limited to the terms and conditions of the License Agreement under which
 * it is provided by or on behalf of EMC.
 */

package com.emc.storageos.vnxe.requests;

import java.util.List;

import com.emc.storageos.vnxe.models.VNXeStorageProcessor;

public class StorageProcessorListRequest extends KHRequests<VNXeStorageProcessor> {
    private static final String URL = "/api/types/storageProcessor/instances";

    public StorageProcessorListRequest(KHClient client) {
        super(client);
        _url = URL;
    }

    public List<VNXeStorageProcessor> get() {
        return getDataForObjects(VNXeStorageProcessor.class);

    }

}
