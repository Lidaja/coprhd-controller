/*
 * Copyright (c) 2015 EMC Corporation
 * All Rights Reserved
 */

package com.emc.storageos.systemservices.impl.security;


import com.emc.storageos.security.ipsec.IPsecConfig;
import com.emc.storageos.svcs.errorhandling.resources.ServiceUnavailableException;
import com.emc.storageos.systemservices.impl.ipsec.IPsecManager;
import com.emc.storageos.systemservices.impl.upgrade.CoordinatorClientExt;
import org.apache.commons.lang.StringUtils;
import org.apache.curator.framework.recipes.locks.InterProcessLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class IPSecInitialRotate implements Runnable {

    private static final Logger log = LoggerFactory.getLogger(IPSecInitialRotate.class);

    private IPsecManager ipsecMgr;
    private CoordinatorClientExt coordinator;
    private IPsecConfig ipsecConfig;

    private int IPSEC_ROTATION_RETRY_INTERVAL = 5;  //seconds

    @Override
    public void run() {

        while (true) {
            try {
                InterProcessLock lock = null;
                try {
                    lock = coordinator.getCoordinatorClient().getSiteLocalLock("ipseclock");
                    lock.acquire();
                    log.info("Acquired the lock {}", "ipseclock");

                    ipsecMgr.verifyClusterIsStable();

                    String preSharedKey = ipsecConfig.getPreSharedKeyFromZK();
                    if (StringUtils.isBlank(preSharedKey)) {
                        log.info("No pre shared key in zk, generate a new key");
                        ipsecMgr.rotateKey(true);
                        return;
                    } else {
                        log.info("First ipsec key found in zk. No need to regenerate it");
                        return;
                    }
                } finally {
                    lock.release();
                }
            } catch (ServiceUnavailableException suex) {
                log.warn("cluster is not stable currently.");
            } catch (Exception ex) {
                log.warn("error when run ipsec initial rotation: " + ex.getMessage());
            }

            try {
                log.info("sleep for " + IPSEC_ROTATION_RETRY_INTERVAL + " seconds before retrying ipsec rotation.");
                Thread.sleep(IPSEC_ROTATION_RETRY_INTERVAL * 1000);
            } catch (InterruptedException iex) {
                log.warn("interrupted ipsec initial ");
            }
        }
    }


    public IPsecConfig getIpsecConfig() {
        return ipsecConfig;
    }

    public void setIpsecConfig(IPsecConfig ipsecConfig) {
        this.ipsecConfig = ipsecConfig;
    }

    public CoordinatorClientExt getCoordinator() {
        return coordinator;
    }

    public void setCoordinator(CoordinatorClientExt coordinator) {
        this.coordinator = coordinator;
    }

    public IPsecManager getIpsecMgr() {
        return ipsecMgr;
    }

    public void setIpsecMgr(IPsecManager ipsecMgr) {
        this.ipsecMgr = ipsecMgr;
    }
}