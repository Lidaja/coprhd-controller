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
import util.StringOption;
//import util.IntOption;

import com.emc.storageos.db.server.impl.StorageSystemTypesInitUtils;
import com.emc.storageos.model.storagesystem.type.StorageSystemTypeList;
import com.emc.storageos.model.storagesystem.type.StorageSystemTypeRestRep;
import com.google.common.collect.Lists;

import util.StorageSystemTypeUtils;
import util.StringOption;

public class StorageSystemTypes {
    private static final String OPTION_PREFIX = "StorageSystemType";
    public static final String NONE = "NONE";
    public static final String DENALI = "denali";
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

    public static final String BLOCK = "Block";
    public static final String FILE = "File";
    public static final String OBJECT = "Object";

    public static final String[] BLOCK_TYPES = {DENALI, VMAX, VNX_BLOCK, VPLEX, HITACHI, OPENSTACK, SCALEIO, SCALEIOAPI, XTREMIO, VNXe, IBMXIV, CEPH, UNITY };
    public static final String[] FILE_TYPES = { ISILON, VNX_FILE, NETAPP, DATA_DOMAIN, VNXe, UNITY, NETAPPC };
    public static final String[] STORAGE_PROVIDER_TYPES = { SMIS, VNX_BLOCK, HITACHI, VPLEX, OPENSTACK, SCALEIO, SCALEIOAPI, DATA_DOMAIN, IBMXIV, XTREMIO, CEPH };
    public static final String[] NON_SMIS_TYPES = { ISILON, VNX_FILE, NETAPP, XTREMIO, VNXe, UNITY, NETAPPC, ECS };


    public static final StringOption[] OPTIONS = {
	    option(DENALI)
            //option(ISILON),
            //option(VNX_FILE),
            //option(NETAPP),
            //option(VNXe),
            //option(UNITY),
            //option(NETAPPC),
            //option(ECS),
	    //new StringOption(DENALI, getDisplayValue(STORAGE_PROVIDER_DENALI)),
            //new StringOption(VMAX, getDisplayValue(STORAGE_PROVIDER_VMAX)),
            //new StringOption(VPLEX, getDisplayValue(STORAGE_PROVIDER_VPLEX)),
            //new StringOption(HITACHI, getDisplayValue(STORAGE_PROVIDER_HITACHI)),
            //new StringOption(OPENSTACK, getDisplayValue(STORAGE_PROVIDER_OPENSTACK)),
            //new StringOption(SCALEIOAPI, getDisplayValue(STORAGE_PROVIDER_SCALEIOAPI)),
            //new StringOption(DATA_DOMAIN, getDisplayValue(STORAGE_PROVIDER_DATA_DOMAIN)),
            //new StringOption(IBMXIV, getDisplayValue(STORAGE_PROVIDER_IBMXIV)),
            //new StringOption(XTREMIO, getDisplayValue(STORAGE_PROVIDER_XTREMIO)),
            //new StringOption(CEPH, getDisplayValue(STORAGE_PROVIDER_CEPH))
    };

    public static final StringOption[] DEVICES = {
	   option(BLOCK),
	   option(FILE),
	   option(OBJECT)
    };

    public static final StringOption[] NODES = {
	   option("1"),
	   option("2"),
	   option("3"),
	   option("4"),
	   option("5"),
	   option("6"),
   	   option("7"),
	   option("8"),
	   option("9"), 
	   option("10"),
	   option("11"),
	   option("12"),
	   option("13"),
	   option("14"),
           option("15"),
	   option("16"),
	   option("17"),
	   option("18"),
	   option("19"),
           option("20"),
 	   option("21"),
	   option("22"),
	   option("23"),
	   option("24"),
	   option("25"),
	   option("26"),
   	   option("27"),
	   option("28"),
	   option("29"), 
	   option("30"),
	   option("31"),
	   option("32"),
	   option("33"),
	   option("34"),
           option("35"),
	   option("36"),
	   option("37"),
	   option("38"),
	   option("39"),
 	   option("40"),
	   option("41"),
	   option("42"),
	   option("43"),
	   option("44"),
	   option("45"),
	   option("46"),
   	   option("47"),
	   option("48"),
	   option("49"), 
	   option("50"),
	   option("51"),
	   option("52"),
	   option("53"),
	   option("54"),
           option("55"),
	   option("56"),
	   option("57"),
	   option("58"),
	   option("59"),
	   option("60"),
 	   option("61"),
	   option("62"),
	   option("63"),
	   option("64"),
	   option("65"),
	   option("66"),
   	   option("67"),
	   option("68"),
	   option("69"), 
	   option("70"),
	   option("71"),
	   option("72"),
	   option("73"),
	   option("74"),
           option("75"),
	   option("76"),
	   option("77"),
	   option("78"),
	   option("79"),
           option("80"),
           option("81"),
 	   option("82"),
 	   option("83"),
	   option("84"),
	   option("85"),
	   option("86"),
   	   option("87"),
	   option("88"),
	   option("89"), 
	   option("90"),
	   option("91"),
	   option("92"),
	   option("93"),
	   option("94"),
           option("95"),
	   option("96"),
	   option("97"),
	   option("98"),
	   option("99"),
           option("100")
    };

    public static final StringOption[] SMIS_OPTIONS = StringOption.options(STORAGE_PROVIDER_TYPES, OPTION_PREFIX);
    public static final StringOption[] NON_SMIS_OPTIONS = StringOption.options(NON_SMIS_TYPES, OPTION_PREFIX);
    public static final StringOption[] SSL_DEFAULT_OPTIONS = StringOption.options(new String[] { VNX_BLOCK, VMAX, SCALEIOAPI, VPLEX, VNX_FILE, VNXe, 
            UNITY,  IBMXIV }, OPTION_PREFIX);
    public static final StringOption[] NON_SSL_OPTIONS = StringOption.options(new String[] { SCALEIO, XTREMIO, CEPH });
    public static final StringOption[] MDM_DEFAULT_OPTIONS = StringOption.options(new String[] { SCALEIO, SCALEIOAPI });
    public static final StringOption[] MDM_ONLY_OPTIONS = StringOption.options(new String[] {SCALEIOAPI});
    public static final StringOption[] ELEMENT_MANAGER_OPTIONS = StringOption.options(new String[] { SCALEIO });
    public static final StringOption[] SECRET_KEY_OPTIONS = StringOption.options(new String[] { CEPH });

    public static boolean isNone(String type) {
        return NONE.equals(type);
    }
    
    public static boolean isDenali(String type) {
	return DENALI.equals(type);
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
