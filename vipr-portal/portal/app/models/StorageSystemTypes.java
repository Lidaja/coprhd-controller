/*
 * Copyright (c) 2015 EMC Corporation
 * All Rights Reserved
 */
package models;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;

import com.emc.storageos.db.server.impl.StorageSystemTypesInitUtils;
import com.emc.storageos.model.storagesystem.type.StorageSystemTypeList;
import com.emc.storageos.model.storagesystem.type.StorageSystemTypeRestRep;
import com.google.common.collect.Lists;

import util.StorageSystemTypeUtils;
import util.StringOption;

public class StorageSystemTypes {
    private static final String OPTION_PREFIX = "StorageSystemType";
    public static final String NONE = "NONE";
    public static final String ISILON = "isilon";
    public static final String VNX_BLOCK = "vnxblock";
    public static final String VNXe = "vnxe";
    public static final String UNITY = "unity";
    public static final String VNX_FILE = "vnxfile";
    public static final String VMAX = "vmax";
    public static final String NETAPP = "netapp";
    public static final String NETAPPC = "netappc";
    public static final String HITACHI = "hds";
    public static final String IBMXIV = "ibmxiv";
    public static final String VPLEX = "vplex";
    public static final String OPENSTACK = "openstack";
    public static final String SCALEIO = "scaleio";
    public static final String SCALEIOAPI = "scaleioapi";
    public static final String XTREMIO = "xtremio";
    public static final String DATA_DOMAIN = "datadomain";
    public static final String ECS = "ecs";
    public static final String CEPH = "ceph";
    private static final String SMIS = "smis";
    
    public static final String STORAGE_PROVIDER_VMAX = "STORAGE_PROVIDER.vmax";
    public static final String STORAGE_PROVIDER_HITACHI = "STORAGE_PROVIDER.hds";
    public static final String STORAGE_PROVIDER_VPLEX = "STORAGE_PROVIDER.vplex";
    public static final String STORAGE_PROVIDER_OPENSTACK = "STORAGE_PROVIDER.cinder";
    public static final String STORAGE_PROVIDER_SCALEIO = "STORAGE_PROVIDER.scaleio";
    public static final String STORAGE_PROVIDER_SCALEIOAPI = "STORAGE_PROVIDER.scaleioapi";
    public static final String STORAGE_PROVIDER_DATA_DOMAIN = "STORAGE_PROVIDER.ddmc";
    public static final String STORAGE_PROVIDER_IBMXIV = "STORAGE_PROVIDER.ibmxiv";
    public static final String STORAGE_PROVIDER_XTREMIO = "STORAGE_PROVIDER.xtremio";
    public static final String STORAGE_PROVIDER_CEPH = "STORAGE_PROVIDER.ceph";

    public static final String[] BLOCK_TYPES = { VMAX, VNX_BLOCK, VPLEX, HITACHI, OPENSTACK, SCALEIO, SCALEIOAPI, XTREMIO, VNXe, IBMXIV, CEPH, UNITY };
    public static final String[] FILE_TYPES = { ISILON, VNX_FILE, NETAPP, DATA_DOMAIN, VNXe, UNITY, NETAPPC };
    public static final String[] STORAGE_PROVIDER_TYPES = { SMIS, VNX_BLOCK, HITACHI, VPLEX, OPENSTACK, SCALEIO, SCALEIOAPI, DATA_DOMAIN, IBMXIV, XTREMIO, CEPH };
    public static final String[] NON_SMIS_TYPES = { ISILON, VNX_FILE, NETAPP, XTREMIO, VNXe, UNITY, NETAPPC, ECS };

    public static boolean isNone(String type) {
        return NONE.equals(type);
    }

    public static boolean isIsilon(String type) {
        return ISILON.equals(type);
    }

    public static boolean isVnxBlock(String type) {
        return VNX_BLOCK.equals(type);
    }

    public static boolean isVnxFile(String type) {
        return VNX_FILE.equals(type);
    }

    public static boolean isVmax(String type) {
        return VMAX.equals(type);
    }

    public static boolean isNetapp(String type) {
        return NETAPP.equals(type);
    }

    public static boolean isNetappc(String type) {
        return NETAPPC.equals(type);
    }

    public static boolean isVplex(String type) {
        return VPLEX.equals(type);
    }

    public static boolean isScaleIO(String type) {
        return SCALEIO.equals(type);
    }
    
    public static boolean isScaleIOApi(String type) {
    	return SCALEIOAPI.equals(type);
    }

    public static boolean isCeph(String type) {
    	return CEPH.equals(type);
    }

    public static boolean isXtremIO(String type) {
        return XTREMIO.equals(type);
    }

    public static boolean isVNXe(String type) {
        return VNXe.equals(type);
    }

    public static boolean isUnity(String type) {
        return UNITY.equals(type);
    }

    public static boolean isECS(String type) {
    	return ECS.equals(type);
    }
    
    public static boolean isFileStorageSystem(String type) {
        return contains(FILE_TYPES, type);
    }

    public static boolean isBlockStorageSystem(String type) {
        return contains(BLOCK_TYPES, type);
    }

    public static boolean isStorageProvider(String type) {
        return contains(STORAGE_PROVIDER_TYPES, type);
    }

    private static boolean contains(String[] systemTypes, String type) {
        for (String systemType : systemTypes) {
            if (systemType.equals(type)) {
                return true;
            }
        }
        return false;
    }

    public static StringOption option(String type) {
        return new StringOption(type, getDisplayValue(type));
    }

    public static List<StringOption> options(String... types) {
        List<StringOption> options = Lists.newArrayList();
        for (String type : types) {
            options.add(option(type));
        }
        return options;
    }

    public static String getDisplayValue(String type) {
        return StringOption.getDisplayValue(type, OPTION_PREFIX);
    }

    /**
     * Inside structure of StringOption is "storage type name: provider name (or storage type display name)
     */
    public static List<StringOption> getStorageTypeOptions() {
        Map<String, String> arrayProviderMap = StorageSystemTypesInitUtils.getProviderDsiplayNameMap();
        List<StringOption> options = new ArrayList<StringOption>();
        StorageSystemTypeList typeList = StorageSystemTypeUtils.getAllStorageSystemTypes(StorageSystemTypeUtils.ALL_TYPE);

        for (StorageSystemTypeRestRep type : typeList.getStorageSystemTypes()) {
            String typeName = type.getStorageTypeName();
            // ignore SMIS providers except VPLEX, SCALEIO, IBMXIV, XTREMIO
            if (type.getIsSmiProvider() && !StringUtils.equals(VPLEX, typeName)
                    && !StringUtils.equals(SCALEIOAPI, typeName)
                    && !StringUtils.equals(IBMXIV, typeName)
                    && !StringUtils.equals(XTREMIO, typeName)
                    && !StringUtils.equals(CEPH, typeName)) {
                continue;
            }

            String provider = arrayProviderMap.get(typeName);
            if (provider != null) {
                if (StringUtils.equals(VMAX, typeName)) {
                    options.add(new StringOption(SMIS, provider));
                } else {
                    options.add(new StringOption(typeName, provider));
                }
            } else if (!StringUtils.equals(VNX_BLOCK, typeName)) { // VNX block is covered by VMAX
                options.add(new StringOption(typeName, type.getStorageTypeDispName()));
            }
        }
        return options;
    }

    public static List<StringOption> getStorageDeviceTypeOptions() {
        List<StringOption> options = new ArrayList<StringOption>();
	options.add(new StringOption("Block"));
	options.add(new StringOption("File"));
	options.add(new StringOption("Object"));
        return options;
    }


    public static List<StringOption> getStorageNumNodesOptions() {
        List<StringOption> options = new ArrayList<StringOption>();
	for (int i = 1; i <= 100; i++){
		options.add(new StringOption(Integer.toString(i)));
	}
        return options;
    }


    public static List<StringOption> getBlockStorageOptions() {
        List<StringOption> options = new ArrayList<StringOption>(Arrays.asList(StringOption.NONE_OPTION));
        StorageSystemTypeList typeList = StorageSystemTypeUtils.getAllStorageSystemTypes(StorageSystemTypeUtils.ALL_TYPE);
        for (StorageSystemTypeRestRep type : typeList.getStorageSystemTypes()) {
            // ignore those whose type is not block
            if (!StorageSystemTypeUtils.BLOCK_TYPE.equalsIgnoreCase(type.getMetaType())
                    && !StorageSystemTypeUtils.BLOCK_AND_FILE_TYPE.equalsIgnoreCase(type.getMetaType())) {
                continue;
            }
            // no need further check for non-SMIS providers
            if (!type.getIsSmiProvider()) {
                options.add(new StringOption(type.getStorageTypeName(), type.getStorageTypeDispName()));
                continue;
            }
            if ((StringUtils.equals(SCALEIO, type.getStorageTypeName())
                    || StringUtils.equals(IBMXIV, type.getStorageTypeName())
                    || StringUtils.equals(XTREMIO, type.getStorageTypeName()))
                    || StringUtils.equals(CEPH, type.getStorageTypeName())) {
                options.add(new StringOption(type.getStorageTypeName(), type.getStorageTypeDispName()));
            }
        }
        return options;
    }

    public static List<StringOption> getFileStorageOptions() {
        List<StringOption> options = new ArrayList<StringOption>(Arrays.asList(StringOption.NONE_OPTION));
        StorageSystemTypeList typeList = StorageSystemTypeUtils.getAllStorageSystemTypes(StorageSystemTypeUtils.ALL_TYPE);
        for (StorageSystemTypeRestRep type : typeList.getStorageSystemTypes()) {
            if (!StorageSystemTypeUtils.FILE_TYPE.equalsIgnoreCase(type.getMetaType())
                    && !StorageSystemTypeUtils.BLOCK_AND_FILE_TYPE.equalsIgnoreCase(type.getMetaType())) {
                continue;
            }
            if (type.getIsSmiProvider()) {
                continue;
            }
            options.add(new StringOption(type.getStorageTypeName(), type.getStorageTypeDispName()));
        }
        return options;
    }

    public static List<StringOption> getObjectStorageOptions() {
        List<StringOption> options = new ArrayList<StringOption>(Arrays.asList(StringOption.NONE_OPTION));
        StorageSystemTypeList typeList = StorageSystemTypeUtils.getAllStorageSystemTypes(StorageSystemTypeUtils.ALL_TYPE);
        for (StorageSystemTypeRestRep type : typeList.getStorageSystemTypes()) {
            if (!StorageSystemTypeUtils.OBJECT_TYPE.equalsIgnoreCase(type.getMetaType())) {
                continue;
            }
            options.add(new StringOption(type.getStorageTypeName(), type.getStorageTypeDispName()));
        }
        return options;
    }

    public static List<StringOption> getProvidersWithSSL() {
        String alltypes = "all";
        List<StringOption> allproviders = new ArrayList<StringOption>();
        StorageSystemTypeList storagetypelist = StorageSystemTypeUtils
                .getAllStorageSystemTypes(alltypes);
        for (StorageSystemTypeRestRep storagetypeRest : storagetypelist
                .getStorageSystemTypes()) {
            if (storagetypeRest.getIsDefaultSsl()) {
                allproviders.add(new StringOption(storagetypeRest
                        .getStorageTypeName(), storagetypeRest
                        .getStorageTypeDispName()));
            }
        }

        return allproviders;
    }

    public static List<StringOption> getProvidersWithoutSSL() {
        String alltypes = "all";
        List<StringOption> allproviders = new ArrayList<StringOption>();
        StorageSystemTypeList storagetypelist = StorageSystemTypeUtils
                .getAllStorageSystemTypes(alltypes);
        for (StorageSystemTypeRestRep storagetypeRest : storagetypelist
                .getStorageSystemTypes()) {
            if (!storagetypeRest.getIsDefaultSsl()) {
                allproviders.add(new StringOption(storagetypeRest
                        .getStorageTypeName(), storagetypeRest
                        .getStorageTypeDispName()));
            }
        }

        return allproviders;
    }

    public static List<StringOption> getProvidersWithMDM() {
        String alltypes = "all";
        List<StringOption> allproviders = new ArrayList<StringOption>();
        StorageSystemTypeList storagetypelist = StorageSystemTypeUtils
                .getAllStorageSystemTypes(alltypes);
        for (StorageSystemTypeRestRep storagetypeRest : storagetypelist
                .getStorageSystemTypes()) {
            if (storagetypeRest.getIsDefaultMDM()) {
                allproviders.add(new StringOption(storagetypeRest
                        .getStorageTypeName(), storagetypeRest
                        .getStorageTypeDispName()));
            }
        }

        return allproviders;
    }

    public static List<StringOption> getProvidersWithOnlyMDM() {
        String alltypes = "all";
        List<StringOption> allproviders = new ArrayList<StringOption>();
        StorageSystemTypeList storagetypelist = StorageSystemTypeUtils
                .getAllStorageSystemTypes(alltypes);
        for (StorageSystemTypeRestRep storagetypeRest : storagetypelist
                .getStorageSystemTypes()) {
            if (storagetypeRest.getIsOnlyMDM()) {
                allproviders.add(new StringOption(storagetypeRest
                        .getStorageTypeName(), storagetypeRest
                        .getStorageTypeDispName()));
            }
        }

        return allproviders;
    }

    public static List<StringOption> getProvidersWithEMS() {
        String alltypes = "all";
        List<StringOption> allproviders = new ArrayList<StringOption>();
        StorageSystemTypeList storagetypelist = StorageSystemTypeUtils
                .getAllStorageSystemTypes(alltypes);
        for (StorageSystemTypeRestRep storagetypeRest : storagetypelist
                .getStorageSystemTypes()) {
            if (storagetypeRest.getIsElementMgr()) {
                allproviders.add(new StringOption(storagetypeRest
                        .getStorageTypeName(), storagetypeRest
                        .getStorageTypeDispName()));
            }
        }

        return allproviders;
    }

    public static List<StringOption> getProvidersWithSecretKey() {
        String alltypes = "all";
        List<StringOption> allproviders = new ArrayList<StringOption>();
        StorageSystemTypeList storagetypelist = StorageSystemTypeUtils
                .getAllStorageSystemTypes(alltypes);
        for (StorageSystemTypeRestRep storagetypeRest : storagetypelist
                .getStorageSystemTypes()) {
            if (storagetypeRest.getIsSecretKey()) {
                allproviders.add(new StringOption(storagetypeRest
                        .getStorageTypeName(), storagetypeRest
                        .getStorageTypeDispName()));
            }
        }

        return allproviders;
    }
}
