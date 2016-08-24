/*
 * Copyright 2016 Intel Corporation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package com.emc.storageos.keystone.restapi.utils;

import com.emc.storageos.cinder.CinderConstants;
import com.emc.storageos.db.client.DbClient;
import com.emc.storageos.db.client.model.*;
import com.emc.storageos.keystone.KeystoneConstants;
import com.emc.storageos.keystone.restapi.KeystoneApiClient;
import com.emc.storageos.keystone.restapi.KeystoneRestClientFactory;
import com.emc.storageos.keystone.restapi.model.response.*;
import com.emc.storageos.model.tenant.TenantCreateParam;
import com.emc.storageos.model.tenant.UserMappingAttributeParam;
import com.emc.storageos.model.tenant.UserMappingParam;
import com.emc.storageos.svcs.errorhandling.resources.APIException;
import com.emc.vipr.model.sys.ipreconfig.ClusterIpv4Setting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Keystone API Utils class.
 */
public class KeystoneUtils {

    private static final Logger _log = LoggerFactory.getLogger(KeystoneUtils.class);
    public static final String OPENSTACK_CINDER_V2_NAME = "cinderv2";
    public static final String OPENSTACK_CINDER_V1_NAME = "cinder";
    public static final String OPENSTACK_TENANT_ID = "tenant_id";
    public static final String OPENSTACK_DEFAULT_REGION = "RegionOne";
    public static final String TENANT_ID = "tenant_id";
    public static final String VALUES = "values";
    // Default excluded option for OSTenant.
    private static final boolean DEFAULT_EXCLUDED_TENANT_OPTION = false;

    private KeystoneRestClientFactory _keystoneApiFactory;
    private Properties _ovfProperties;
    private DbClient _dbClient;

    public void setDbClient(DbClient dbClient) {
        this._dbClient = dbClient;
    }

    public void setKeystoneFactory(KeystoneRestClientFactory factory) {
        this._keystoneApiFactory = factory;
    }

    public void setOvfProperties(Properties ovfProps) {
        _ovfProperties = ovfProps;
    }

    /**
     * Delete endpoint for the service with given ID.
     *
     * @param keystoneApi KeystoneApiClient.
     * @param serviceId OpenStack service ID.
     */
    public void deleteKeystoneEndpoint(KeystoneApiClient keystoneApi, String serviceId) {
        _log.debug("START - deleteKeystoneEndpoint");

        if (serviceId != null) {

            // Get Keystone endpoints from Keystone API.
            EndpointResponse endpoints = keystoneApi.getKeystoneEndpoints();
            // Find endpoint to delete.
            EndpointV2 endpointToDelete = findEndpoint(endpoints, serviceId);
            // Do not execute delete call when endpoint does not exist.
            if (endpointToDelete != null) {
                // Delete endpoint using Keystone API.
                keystoneApi.deleteKeystoneEndpoint(endpointToDelete.getId());
                _log.debug("Keystone endpoint deleted");
            }
        }

        _log.debug("END - deleteKeystoneEndpoint");
    }

    /**
     * Retrieves OpenStack endpoint related to given service ID.
     *
     * @param serviceId Service ID.
     * @return OpenStack endpoint for the given service ID.
     */
    public EndpointV2 findEndpoint(EndpointResponse response, String serviceId) {
        _log.debug("START - findEndpoint");

        if (serviceId == null) {
            _log.error("serviceId is null");
            throw APIException.internalServerErrors.targetIsNullOrEmpty("Service id");
        }

        for (EndpointV2 endpoint : response.getEndpoints()) {
            if (endpoint.getServiceId().equals(serviceId)) {
                _log.debug("END - findEndpoint");
                return endpoint;
            }
        }

        _log.warn("Missing endpoint for service {}", serviceId);
        // Return null if there is no endpoints for given service.
        return null;
    }

    /**
     * Retrieves OpenStack service ID with given service name.
     *
     * @param keystoneApi Keystone Api client.
     * @param serviceName Name of a service to retrieve.
     * @return ID of service with given name.
     */
    public String findServiceId(KeystoneApiClient keystoneApi, String serviceName) {
        _log.debug("START - findServiceId");

        if (serviceName == null) {
            _log.error("serviceName is null");
            throw APIException.internalServerErrors.targetIsNullOrEmpty("Service name");
        }

        // Get Keystone services from Keystone API.
        ServiceResponse services = keystoneApi.getKeystoneServices();

        for (ServiceV2 service : services.getServices()) {
            if (service.getName().equals(serviceName)) {
                _log.debug("END - findServiceId");
                return service.getId();
            }
        }
        _log.warn("Missing service {}", serviceName);
        // Return null if service is missing.
        return null;
    }

    /**
     * Retrieves URI from server urls.
     *
     * @param serverUrls Set of strings representing server urls.
     * @return First URI from server urls param.
     */
    public URI retrieveUriFromServerUrls(Set<String> serverUrls) {
        URI authUri = null;
        for (String uri : serverUrls) {
            authUri = URI.create(uri);
            break; // There will be single URL only
        }
        return authUri;
    }

    /**
     * Get region for the service with given ID.
     *
     * @param keystoneApi KeystoneApiClient.
     * @param serviceId OpenStack service ID.
     */
    public String getRegionForService(KeystoneApiClient keystoneApi, String serviceId) {
        _log.debug("START - getRegionForService");

        if (serviceId == null) {
            _log.error("serviceId is null");
            throw APIException.internalServerErrors.targetIsNullOrEmpty("Service id");
        }

        // Get Keystone endpoints from Keystone API.
        EndpointResponse endpoints = keystoneApi.getKeystoneEndpoints();
        // Find endpoint for the service.
        EndpointV2 endpoint = findEndpoint(endpoints, serviceId);
        // Return null if endpoint is null, otherwise return region name.
        if (endpoint != null) {
            _log.debug("END - getRegionForService");
            // Return region name.
            return endpoint.getRegion();
        }
        _log.warn("Endpoint missing for a service with ID: {}", serviceId);

        return null;
    }

    /**
     * Get Keystone API client.
     *
     * @param authUri URI pointing to Keystone server.
     * @param username OpenStack username.
     * @param usernamePassword OpenStack password.
     * @param tenantName OpenStack tenantname.
     * @return keystoneApi KeystoneApiClient.
     */
    public KeystoneApiClient getKeystoneApi(URI authUri, String username, String usernamePassword, String tenantName) {

        // Get Keystone API Client.
        KeystoneApiClient keystoneApi = (KeystoneApiClient) _keystoneApiFactory.getRESTClient(
                authUri, username, usernamePassword);
        keystoneApi.setTenantName(tenantName);

        return keystoneApi;
    }

    /**
     * Retrieves username and tenantname from the AuthnProvider.
     *
     * @param managerDN of a Authentication Provider.
     * @return StringMap containing username and tenantname keys with values.
     */
    private StringMap getUsernameAndTenant(String managerDN) {

        String username = managerDN.split(",")[0].split("=")[1];
        String tenantName = managerDN.split(",")[1].split("=")[1];
        StringMap map = new StringMap();
        map.put(CinderConstants.USERNAME, username);
        map.put(CinderConstants.TENANTNAME, tenantName);

        return map;
    }

    /**
     * Get Keystone API client.
     *
     * @param managerDN of an Authentication Provider.
     * @param serverUrls of an Authentication Provider
     * @param managerPassword of an Authentication Provider
     * @return keystoneApi KeystoneApiClient.
     */
    public KeystoneApiClient getKeystoneApi(String managerDN, StringSet serverUrls, String managerPassword) {

        URI authUri = retrieveUriFromServerUrls(serverUrls);

        StringMap usernameAndTenantMap = getUsernameAndTenant(managerDN);
        String username = usernameAndTenantMap.get(CinderConstants.USERNAME);
        String tenantName = usernameAndTenantMap.get(CinderConstants.TENANTNAME);

        // Get Keystone API Client.
        return getKeystoneApi(authUri, username, managerPassword, tenantName);
    }

    /**
     * Register CoprHD in Keystone.
     * Creates an endpoint pointing to CoprHd instead to Cinder.
     *
     * @param managerDN of an Authentication Provider.
     * @param serverUrls of an Authentication Provider
     * @param managerPassword of an Authentication Provider
     */
    public void registerCoprhdInKeystone(String managerDN, StringSet serverUrls, String managerPassword) {
        _log.debug("START - register CoprHD in Keystone");

        // Create a new KeystoneAPI.
        KeystoneApiClient keystoneApi = getKeystoneApi(managerDN, serverUrls, managerPassword);
        // Find Id of cinderv2 service.
        String cinderv2ServiceId = findServiceId(keystoneApi, KeystoneUtils.OPENSTACK_CINDER_V2_NAME);
        // Find Id of cinderv1 service.
        String cinderServiceId = findServiceId(keystoneApi, KeystoneUtils.OPENSTACK_CINDER_V1_NAME);

        // Create service when cinderv2 service is missing.
        if (cinderv2ServiceId == null) {
            ServiceV2 service = prepareNewCinderService(true);
            CreateServiceResponse response = keystoneApi.createKeystoneService(service);
            cinderv2ServiceId = response.getService().getId();
        } else {
            // Delete old endpoint for cinderv2 service.
            deleteKeystoneEndpoint(keystoneApi, cinderv2ServiceId);
        }

        // Create service when cinder service is missing.
        if (cinderServiceId == null) {
            ServiceV2 service = prepareNewCinderService(false);
            CreateServiceResponse response = keystoneApi.createKeystoneService(service);
            cinderServiceId = response.getService().getId();
        } else {
            // Delete old endpoint for cinderv1 service.
            deleteKeystoneEndpoint(keystoneApi, cinderServiceId);
        }

        // Get region name for a cinderv2 service.
        String region = getRegionForService(keystoneApi, cinderv2ServiceId);

        // Set default region in case when endpoint is not present.
        if (region == null) {
            region = KeystoneUtils.OPENSTACK_DEFAULT_REGION;
        }

        // Prepare new endpoint for cinderv2 service.
        EndpointV2 newEndpointV2 = prepareNewCinderEndpoint(region, cinderv2ServiceId, true);
        // Prepare new endpoint for cinderv1 service.
        EndpointV2 newEndpointV1 = prepareNewCinderEndpoint(region, cinderServiceId, false);
        // Create a new endpoint pointing to CoprHD for cinderv2 using Keystone API.
        keystoneApi.createKeystoneEndpoint(newEndpointV2);
        // Create a new endpoint pointing to CoprHD for cinderv1 using Keystone API.
        keystoneApi.createKeystoneEndpoint(newEndpointV1);

        _log.debug("END - register CoprHD in Keystone");
    }

    /**
     * Prepare a new service (cinder or cinderv2).
     *
     * @param isCinderv2 Boolean that holds information about version of a service.
     * @return Service filled with necessary information.
     */
    public ServiceV2 prepareNewCinderService(Boolean isCinderv2) {

        ServiceV2 service = new ServiceV2();

        if (isCinderv2) {
            service.setName(KeystoneUtils.OPENSTACK_CINDER_V2_NAME);
            service.setType(CinderConstants.SERVICE_TYPE_VOLUMEV2);
        } else {
            service.setName(KeystoneUtils.OPENSTACK_CINDER_V1_NAME);
            service.setType(CinderConstants.SERVICE_TYPE_VOLUME);
        }

        service.setDescription(CinderConstants.SERVICE_DESCRIPTION);

        return service;
    }

    /**
     * Prepare a new endpoint for cinder or cinderv2 service.
     *
     * @param region Region assigned to the endpoint.
     * @param serviceId Cinder service ID.
     * @param isCinderv2 Boolean that holds information about version of a service.
     * @return Endpoint filled with necessary information.
     */
    public EndpointV2 prepareNewCinderEndpoint(String region, String serviceId, Boolean isCinderv2) {

        String url = "";

        String clusterVIP = getVIP();

        if (clusterVIP == null) {
            _log.error("Could not retrieve cluster Virtual IP");
            throw APIException.internalServerErrors.targetIsNullOrEmpty("Virtual IP");
        }

        _log.debug("Cluster VIP: {}", clusterVIP);

        // Checks whether url should point to cinderv2 or to cinder service.
        if (isCinderv2) {
            url = CinderConstants.HTTPS_URL + clusterVIP + CinderConstants.COPRHD_URL_V2;
        } else {
            url = CinderConstants.HTTPS_URL + clusterVIP + CinderConstants.COPRHD_URL_V1;
        }

        EndpointV2 endpoint = new EndpointV2();
        endpoint.setRegion(region);
        endpoint.setServiceId(serviceId);
        endpoint.setPublicURL(url);
        endpoint.setAdminURL(url);
        endpoint.setInternalURL(url);

        return endpoint;
    }

    /**
     * Returns Virtual IP of CoprHD.
     *
     * @return CoprHD VIP.
     */
    public String getVIP() {

        // Get cluster Virtual IP.
        Map<String, String> ovfprops = (Map) _ovfProperties;
        ClusterIpv4Setting ipv4Setting = new ClusterIpv4Setting();
        ipv4Setting.loadFromPropertyMap(ovfprops);
        return ipv4Setting.getNetworkVip();
    }

    /**
     * Populate or Modify the keystone token
     * in authentication provider.
     *
     * @param managerDN of an Authentication Provider.
     * @param serverUrls of an Authentication Provider
     * @param password of an Authentication Provider
     *
     * @return StringMap containing keystone authentication keys.
     */
    public StringMap populateKeystoneToken(StringSet serverUrls, String managerDN, String password) {

        URI authUri = retrieveUriFromServerUrls(serverUrls);

        StringMap usernameAndTenantMap = getUsernameAndTenant(managerDN);
        String username = usernameAndTenantMap.get(CinderConstants.USERNAME);
        String tenantName = usernameAndTenantMap.get(CinderConstants.TENANTNAME);
        KeystoneApiClient keystoneApi = getKeystoneApi(authUri, username, password, tenantName);
        keystoneApi.authenticate_keystone();
        StringMap keystoneAuthKeys = new StringMap();
        keystoneAuthKeys.put(KeystoneConstants.AUTH_TOKEN, keystoneApi.getAuthToken());
        return keystoneAuthKeys;
    }

    /**
     * Delete endpoints for cinder service.
     *
     * @param managerDN of an Authentication Provider.
     * @param serverUrls of an Authentication Provider
     * @param managerPassword of an Authentication Provider
     */
    public void deleteCinderEndpoints(String managerDN, StringSet serverUrls, String managerPassword) {

        // Create a new KeystoneAPI.
        KeystoneApiClient keystoneApi = getKeystoneApi(managerDN, serverUrls, managerPassword);
        // Get a cinderv2 service id.
        String serviceIdV2 = findServiceId(keystoneApi, KeystoneUtils.OPENSTACK_CINDER_V2_NAME);
        // Get a cinderv1 service id.
        String serviceIdV1 = findServiceId(keystoneApi, KeystoneUtils.OPENSTACK_CINDER_V1_NAME);

        // Delete endpoint when cinderv2 service exist.
        if (serviceIdV2 != null) {

            // Delete endpoint for cinderv2 service.
            deleteKeystoneEndpoint(keystoneApi, serviceIdV2);
        }

        // Delete endpoint when cinder service exist.
        if (serviceIdV1 != null) {

            // Delete endpoint for cinder service.
            deleteKeystoneEndpoint(keystoneApi, serviceIdV1);
        }
    }

    /**
     * Retrieves Keystone Authentication Provider from CoprHD.
     *
     * @return Keystone Authentication Provider.
     */
    public AuthnProvider getKeystoneProvider() {

        List<URI> authnProviderURI = _dbClient.queryByType(AuthnProvider.class, true);
        Iterator<AuthnProvider> providerIter = _dbClient.queryIterativeObjects(AuthnProvider.class, authnProviderURI);
        // Iterate over Providers and return Keystone Provider.
        while (providerIter.hasNext()) {
            AuthnProvider provider = providerIter.next();
            if (AuthnProvider.ProvidersType.keystone.toString().equalsIgnoreCase(provider.getMode())) {
                return provider;
            }
        }

        // Return null whether Keystone Provider is missing in CoprHD.
        return null;
    }

    /**
     * Retrieves OpenStack Tenants from Keystone.
     *
     * @return List of OpenStack Tenants.
     */
    public List<KeystoneTenant> getOpenStackTenants() {

        AuthnProvider keystoneProvider = getKeystoneProvider();

        if (keystoneProvider == null) {
            throw APIException.internalServerErrors.targetIsNullOrEmpty("Keystone provider");
        }

        // Get Keystone API client.
        KeystoneApiClient keystoneApiClient = getKeystoneApi(keystoneProvider.getManagerDN(),
                keystoneProvider.getServerUrls(), keystoneProvider.getManagerPassword());

        // Get OpenStack Tenants.
        // You cannot remove or add elements dynamically to Arrays (Arrays.asList) that is why this needs to be wrapped in a new list.
        return new ArrayList<>(Arrays.asList(keystoneApiClient.getKeystoneTenants().getTenants()));
    }

    /**
     * Retrieves OpenStack Tenant with given id.
     *
     * @param id Tenant ID.
     *
     * @return OpenStack Tenant.
     */
    public KeystoneTenant getTenantWithId(String id) {

        AuthnProvider keystoneProvider = getKeystoneProvider();

        if (keystoneProvider == null) {
            throw APIException.internalServerErrors.targetIsNullOrEmpty("Keystone provider");
        }

        // Get Keystone API client.
        KeystoneApiClient keystoneApiClient = getKeystoneApi(keystoneProvider.getManagerDN(),
                keystoneProvider.getServerUrls(), keystoneProvider.getManagerPassword());

        for (KeystoneTenant tenant : keystoneApiClient.getKeystoneTenants().getTenants()) {
            if (tenant.getId().equals(id)) {
                return tenant;
            }
        }

        return null;
    }

    /**
     * Retrieves CoprHD Tenants with OpenStack ID parameter.
     *
     * @return List of CoprHD Tenants.
     */
    public List<TenantOrg> getCoprhdTenantsWithOpenStackId() {

        List<URI> coprhdTenantsURI = _dbClient.queryByType(TenantOrg.class, true);
        Iterator<TenantOrg> tenantsIter = _dbClient.queryIterativeObjects(TenantOrg.class, coprhdTenantsURI);
        List<TenantOrg> tenants = new ArrayList<>();

        // Iterate over CoprHD Tenants and get only those that contain tenant_id parameter.
        while (tenantsIter.hasNext()) {
            TenantOrg tenant = tenantsIter.next();
            if (getCoprhdTenantUserMapping(tenant) != null) {
                tenants.add(tenant);
            }
        }

        return tenants;
    }

    /**
     * Retrieves CoprHD Tenant with given OpenStack ID.
     *
     * @return CoprHD Tenant.
     */
    public TenantOrg getCoprhdTenantWithOpenstackId(String id) {

        if (id != null && !id.isEmpty()) {
            List<TenantOrg> tenants = getCoprhdTenantsWithOpenStackId();

            for (TenantOrg tenant : tenants) {
                String userMapping = getCoprhdTenantUserMapping(tenant);
                if (userMapping != null && userMapping.contains(id)) {
                    return tenant;
                }
            }
        }

        return null;
    }

    /**
     * Retrieves UserMapping having "tenant_id" as attribute in CoprHD Tenant.
     *
     * @param tenant CoprHD Tenant.
     * @return User Mapping for given Tenant.
     */
    public String getCoprhdTenantUserMapping(TenantOrg tenant) {

        StringSetMap userMappings = tenant.getUserMappings();
        // Ignore root Tenant.
        if (!TenantOrg.isRootTenant(tenant) && userMappings != null) {
            // Return mapping that contains tenant_id.
            for (AbstractChangeTrackingSet<String> userMappingSet : userMappings.values()) {
                for (String mapping : userMappingSet) {
                    if (mapping.contains(TENANT_ID)) {
                        return mapping;
                    }
                }
            }
        }

        // Return null whether Tenant has no mapping with tenant_id parameter.
        return null;
    }

    /**
     * Retrieves OpenStack Tenant ID from UserMapping.
     * UserMapping string contains 3 main subgroups: domain, attributes and groups. Openstack tenant id is located in attributes subgroup
     * assigned to 'values' string. Method splits userMapping string and looks for 'values' and then retrieves tenant id from it.
     *
     * @param userMapping CoprHD UserMapping.
     * @return OpenStack Tenant ID.
     */
    public String getTenantIdFromUserMapping(String userMapping) {

        // Create split pattern.
        Pattern p = Pattern.compile("\\[(.*?)\\]");
        // Apply pattern.
        Matcher m = p.matcher(userMapping);
        String result;

        while (m.find()) {

            if (m.group().contains(VALUES)) {
                result = m.group().split(":")[1];

                return result.substring(2, result.length() - 2);
            }
        }

        return "";
    }

    /**
     * Finds CoprHD representation of OpenStack Tenant with given OS ID.
     *
     * @param tenantId OpenStack Tenant ID.
     * @return CoprHD Tenant.
     */
    public OSTenant findOpenstackTenantInCoprhd(String tenantId) {

        List<URI> osTenantURI = _dbClient.queryByType(OSTenant.class, true);
        Iterator<OSTenant> osTenantIter = _dbClient.queryIterativeObjects(OSTenant.class, osTenantURI);

        while (osTenantIter.hasNext()) {
            OSTenant osTenant = osTenantIter.next();
            if (osTenant.getOsId().equals(tenantId)) {
                return osTenant;
            }
        }

        return null;
    }

    /**
     * Prepares userMappings with OpenStack Tenant ID for CoprHD Tenant.
     *
     * @param openstackId OpenStack Tenant ID.
     * @return UserMappings.
     */
    public List<UserMappingParam> prepareUserMappings(String openstackId) {

        AuthnProvider provider = getKeystoneProvider();

        // Create mapping rules
        List<UserMappingParam> userMappings = new ArrayList<>();
        List<String> values = new ArrayList<>();
        values.add(openstackId);

        List<UserMappingAttributeParam> attributes = new ArrayList<>();
        attributes.add(new UserMappingAttributeParam(KeystoneUtils.OPENSTACK_TENANT_ID, values));

        userMappings.add(new UserMappingParam(provider.getDomains().iterator().next(), attributes, new ArrayList<String>()));

        return userMappings;
    }

    /**
     * Prepares TenantCreateParam class filled with information from given Tenant.
     *
     * @param tenant OpenStack Tenant.
     * @return TenantCreateParam.
     */
    public TenantCreateParam prepareTenantParam(KeystoneTenant tenant) {

        TenantCreateParam param = new TenantCreateParam(CinderConstants.TENANT_NAME_PREFIX + " " + tenant.getName(),
                prepareUserMappings(tenant.getId()));

        param.setDescription(getProperTenantDescription(tenant.getDescription()));

        return param;
    }

    /**
     * Tags Project with given Openstack Tenant ID.
     *
     * @param projectId CoprHD Project ID.
     * @param osTenantId OpenStack Tenant ID.
     * @param coprhdTenantId CoprHD Tenant ID.
     * @return Updated project.
     */
    public Project tagProjectWithOpenstackId(URI projectId, String osTenantId, String coprhdTenantId) {

        // Tag project with OpenStack tenant_id
        Project project = _dbClient.queryObject(Project.class, projectId);
        if (project != null) {
            ScopedLabelSet tagSet = new ScopedLabelSet();
            ScopedLabel tagLabel = new ScopedLabel(coprhdTenantId, osTenantId);
            tagSet.add(tagLabel);
            project.setTag(tagSet);
            _dbClient.updateObject(project);
            return project;
        }

        throw APIException.internalServerErrors.targetIsNullOrEmpty("Project with id: " + projectId);
    }

    /**
     * Maps Openstack KeystoneTenant to OSTenant.
     *
     * @param tenant OpenStack Tenant to map.
     * @return OSTenant.
     */
    public OSTenant mapToOsTenant(KeystoneTenant tenant) {

        if (tenant != null) {
            OSTenant osTenant = new OSTenant();
            osTenant.setOsId(tenant.getId());
            // Check whether description is null or empty.
            osTenant.setDescription(getProperTenantDescription(tenant.getDescription()));
            osTenant.setName(tenant.getName());
            osTenant.setEnabled(Boolean.parseBoolean(tenant.getEnabled()));
            osTenant.setExcluded(DEFAULT_EXCLUDED_TENANT_OPTION);

            return osTenant;
        }

        throw APIException.internalServerErrors.targetIsNullOrEmpty("KeystoneTenant");
    }

    /**
     * Checks whether given Tenant description is null or empty and returns appropriate description.
     *
     * @param description Tenant description.
     * @return String.
     */
    public String getProperTenantDescription(String description) {

        if (description != null && !description.isEmpty()) {
            return description;
        }

        return CinderConstants.TENANT_NAME_PREFIX;
    }
}
