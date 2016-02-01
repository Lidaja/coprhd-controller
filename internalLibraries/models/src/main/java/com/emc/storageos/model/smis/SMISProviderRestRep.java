/*
 * Copyright (c) 2008-2011 EMC Corporation
 * All Rights Reserved
 */

package com.emc.storageos.model.smis;

import javax.xml.bind.annotation.*;

import com.emc.storageos.model.DataObjectRestRep;
import com.emc.storageos.model.RelatedResourceRep;

import java.util.ArrayList;
import java.util.List;

@XmlRootElement(name = "smis_provider")
@XmlAccessorType(XmlAccessType.PROPERTY)
public class SMISProviderRestRep extends DataObjectRestRep {
    private String iPAddress;
    private Integer portNumber;
    private List<RelatedResourceRep> storageSystems;
    private String description;
    private String manufacturer;
    private String versionString;
    private String providerID;
    private String connectionStatus;
    private String userName;
    private Boolean useSSL;
    private String scanStatus;
    private String lastScanStatusMessage;
    private Long lastScanTime;
    private Long nextScanTime;
    private Long successScanTime;
    private String compatibilityStatus;
    private String registrationStatus;

    public SMISProviderRestRep() {
    }

    /**
     * Status of the connection.
     * Valid values:
     *  CONNECTED
     *  NOTCONNECTED
     */
    @XmlElement(name = "connection_status")
    public String getConnectionStatus() {
        return connectionStatus;
    }

    public void setConnectionStatus(String connectionStatus) {
        this.connectionStatus = connectionStatus;
    }

    /**
     * Information relevant to the SMIS provider software.
     * 
     */
    @XmlElement(name = "description")
    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * SMIS provider's IP address.
     * 
     */
    @XmlElement(name = "ip_address")
    public String getIPAddress() {
        return iPAddress;
    }

    public void setIPAddress(String iPAddress) {
        this.iPAddress = iPAddress;
    }

    /**
     * Name of the manufacturer.
     * 
     */
    @XmlElement(name = "manufacturer")
    public String getManufacturer() {
        return manufacturer;
    }

    public void setManufacturer(String manufacturer) {
        this.manufacturer = manufacturer;
    }

    /**
     * The port number used to connect with the SMIS
     * provider, typically 5988 or 5989.
     * 
     */
    @XmlElement(name = "port_number")
    public Integer getPortNumber() {
        return portNumber;
    }

    public void setPortNumber(Integer portNumber) {
        this.portNumber = portNumber;
    }

    /**
     * A combination of the provider's IP address and the port
     * number, used as an ID.
     * 
     */
    @XmlElement(name = "provider_id")
    public String getProviderID() {
        return providerID;
    }

    public void setProviderID(String providerID) {
        this.providerID = providerID;
    }

    @XmlElementWrapper(name = "storage_systems")
    /**
     * List of URIs representing the storage systems accessible 
     * through this provider.
     */
    @XmlElement(name = "storage_system")
    public List<RelatedResourceRep> getStorageSystems() {
        if (storageSystems == null) {
            storageSystems = new ArrayList<RelatedResourceRep>();
        }
        return storageSystems;
    }

    public void setStorageSystems(List<RelatedResourceRep> storageSystems) {
        this.storageSystems = storageSystems;
    }

    /**
     * Login credential at the provider.
     * 
     */
    @XmlElement(name = "user_name")
    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    /**
     * Whether or not secure SSL connection is used.
     * 
     */
    @XmlElement(name = "use_ssl")
    public Boolean getUseSSL() {
        return useSSL;
    }

    public void setUseSSL(Boolean useSSL) {
        this.useSSL = useSSL;
    }

    /**
     * SMIS provider software revision number.
     * 
     */
    @XmlElement(name = "version_string")
    public String getVersionString() {
        return versionString;
    }

    public void setVersionString(String versionString) {
        this.versionString = versionString;
    }

    /**
     * Status of the provider scan job. 
     * Valid values:
     *  CREATED
     *  IN_PROGRESS
     *  COMPLETE
     *  ERROR
     * 
     */
    @XmlElement(name = "job_scan_status")
    public String getScanStatus() {
        return scanStatus;
    }

    public void setScanStatus(String scanStatus) {
        this.scanStatus = scanStatus;
    }

    /**
     * Status message from the last scan.
     * 
     */
    @XmlElement(name = "last_scan_status_message")
    public String getLastScanStatusMessage() {
        return lastScanStatusMessage;
    }

    public void setLastScanStatusMessage(String statusMessage) {
        lastScanStatusMessage = statusMessage;
    }

    /**
     * Time the last scan occurred.
     * 
     */
    @XmlElement(name = "last_scan_time")
    public Long getLastScanTime() {
        return lastScanTime;
    }

    public void setLastScanTime(Long lastScanTime) {
        this.lastScanTime = lastScanTime;
    }

    /**
     * Time for which the next scan job is scheduled.
     * 
     */
    @XmlElement(name = "next_scan_time")
    public Long getNextScanTime() {
        return nextScanTime;
    }

    public void setNextScanTime(Long nextScanTime) {
        this.nextScanTime = nextScanTime;
    }

    /**
     * The latest timestamp when the system run scanning successfully
     * 
     */
    @XmlElement(name = "success_scan_time")
    public Long getSuccessScanTime() {
        return successScanTime;
    }

    public void setSuccessScanTime(Long successScanTime) {
        this.successScanTime = successScanTime;
    }

    /**
     * Registration status of the provider
     * Valid values:
     *  REGISTERED
     *  UNREGISTERED
     */
    @XmlElement(name = "registration_status")
    public String getRegistrationStatus() {
        return registrationStatus;
    }

    public void setRegistrationStatus(String registrationStatus) {
        this.registrationStatus = registrationStatus;
    }

    /**
     * Whether or not the provider software is compatible with
     * ViPR.
     * Valid values:
     *  COMPATIBLE
     *  INCOMPATIBLE
     *  UNKNOWN
     */
    @XmlElement(name = "compatibility_status")
    public String getCompatibilityStatus() {
        return compatibilityStatus;
    }

    public void setCompatibilityStatus(String compatibilityStatus) {
        this.compatibilityStatus = compatibilityStatus;
    }
}
