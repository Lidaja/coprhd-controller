/*
 * Copyright (c) 2012-2015 iWave Software LLC
 * All Rights Reserved
 */
package com.emc.sa.service.vmware;

import static com.emc.sa.service.ServiceParams.DATACENTER;
import static com.emc.sa.service.ServiceParams.HOST;
import static com.emc.sa.service.ServiceParams.VCENTER;

import java.net.URI;
import java.util.List;
import java.util.Set;

import com.emc.sa.engine.ExecutionUtils;
import com.emc.sa.engine.bind.Param;
import com.emc.sa.service.vipr.ViPRService;
import com.emc.sa.service.vipr.block.BlockStorageUtils;
import com.emc.storageos.db.client.model.Cluster;
import com.emc.storageos.db.client.model.Host;
import com.emc.storageos.db.client.model.VcenterDataCenter;
import com.google.common.collect.Sets;
import com.vmware.vim25.mo.ClusterComputeResource;
import com.vmware.vim25.mo.HostSystem;

public abstract class VMwareHostService extends ViPRService {
    @Param(VCENTER)
    protected URI vcenterId;
    @Param(DATACENTER)
    protected URI datacenterId;
    @Param(HOST)
    protected URI hostId;

    protected VMwareSupport vmware = new VMwareSupport();
    protected VcenterDataCenter datacenter;
    protected Host esxHost;
    protected Cluster hostCluster;
    protected HostSystem host;
    protected ClusterComputeResource cluster;

    private void initHost() {
        datacenter = vmware.getDatacenter(datacenterId);

        if (BlockStorageUtils.isHost(hostId)) {
            esxHost = getModelClient().hosts().findById(hostId);

            if (esxHost == null) {
                throw new IllegalArgumentException("Host " + hostId + " not found");
            }

            logInfo("vmware.service.target.host", esxHost.getLabel());
        }
        else {
            hostCluster = getModelClient().clusters().findById(hostId);
            if (hostCluster == null) {
                throw new IllegalArgumentException("Cluster " + hostId + " not found");
            }

            List<Host> hosts = getModelClient().hosts().findByCluster(hostId);
            if (hosts.isEmpty()) {
                throw new IllegalArgumentException("Cluster '" + hostCluster.getLabel() + "' [" + hostId
                        + "] contains no hosts");
            }

            esxHost = hosts.get(0);
            cluster = vmware.getCluster(datacenter.getLabel(), hostCluster.getLabel());

            logInfo("vmware.service.target.cluster", hostCluster.getLabel(), hosts.size());
        }

        host = vmware.getHostSystem(datacenter.getLabel(), esxHost.getLabel());
    }

    protected void connectAndInitializeHost() {
        vmware.connect(vcenterId);
        initHost();
    }

    @Override
    public void precheck() throws Exception {
        super.precheck();
        connectAndInitializeHost();
        validateClusterHosts();
    }

    @Override
    public void destroy() {
        super.destroy();
        vmware.disconnect();
    }

    protected void acquireHostLock() {
        acquireHostLock(esxHost, hostCluster);
    }

    /**
     * Validates the vCenter cluster hosts match the same hosts we have in our database for the cluster. If there is a mismatch the check
     * will fail the order.
     */
    protected void validateClusterHosts() {
        if (hostCluster != null) {
            VcenterDataCenter datacenter = getModelClient().datacenters().findById(datacenterId);
            Cluster cluster = getModelClient().clusters().findById(hostCluster.getId());

            ClusterComputeResource vcenterCluster = vmware.getCluster(datacenter.getLabel(), cluster.getLabel());

            if (vcenterCluster == null) {
                ExecutionUtils.fail("failTask.vmware.cluster.notfound", args(), args(cluster.getLabel()));
            }

            Set<String> vCenterHostUuids = Sets.newHashSet();
            for (HostSystem hostSystem : vcenterCluster.getHosts()) {
                if (hostSystem.getHardware() != null && hostSystem.getHardware().systemInfo != null) {
                    vCenterHostUuids.add(hostSystem.getHardware().systemInfo.uuid);
                }
            }

            List<Host> dbHosts = getModelClient().hosts().findByCluster(hostCluster.getId());
            Set<String> dbHostUuids = Sets.newHashSet();
            for (Host host : dbHosts) {
                dbHostUuids.add(host.getUuid());
            }

            if (!vCenterHostUuids.equals(dbHostUuids)) {
                ExecutionUtils.fail("failTask.vmware.cluster.mismatch", args(), args(cluster.getLabel()));
            } else {
                info("Hosts in cluster %s matches correctly", cluster.getLabel());
            }

        }
    }
}
