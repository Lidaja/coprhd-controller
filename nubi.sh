NAME=$(rpm -qa | grep storageos)
rpm -e $NAME
make clobber BUILD_TYPE=oss rpm
rpm -Uvh /tmp/coprhd-controller/build/RPMS/x86_64/*.rpm
sleep 3
cd /opt/storageos/conf
sed -i "12i <entry key=\"denaliStorageDriver\" value=\"denali\"/>" driver-conf.xml
sed -i "21i <value>denali</value>" driver-conf.xml
sed -i "45i <value>denali</value>" driver-conf.xml
sed -i "442i <entry key=\"denali\" value-ref=\"denaliStorageDriver\"/>" controller-conf.xml
sed -i "1042d" controller-conf.xml
sed -i "19i <entry key=\"denali\" value-ref=\"denaliStorageDriver\"/>" discovery-externaldevice-context.xml
jar cf /opt/storageos/lib/denaliStorageDriver.jar /tmp/coprhd-controller/internalLibraries/storagedriver/src/main/java/com/emc/storageos/storagedriver/DenaliDriver.java
cd /opt/storageos/bin
sed -i "6d" controllersvc
sed -i "6i export CLASSPATH=\"/opt/storageos/conf:\${LIB_DIR}:\${LIB_DIR}/storageos-controllersvc.jar:\${LIB_DIR}/jython.jar:\${LIB_DIR}/gson-2.1.jar:\${LIB_DIR}/commons-httpclient-3.1.jar:\${LIB_DIR}/commons-codec-1.7.jar:\${LIB_DIR}/jsch-0.1.51.jar:\${LIB_DIR}/com.iwave.ext.vmware.vcenter.jar:\${LIB_DIR}/com.iwave.ext.windows.jar:\${LIB_DIR}/com.iwave.ext.linux.jar:\${LIB_DIR}/storageos-dbsvc.jar:\${LIB_DIR}/storageos-processmonitor.jar:\${LIB_DIR}/storageos-security.jar:\${LIB_DIR}/storageos-cimadapter.jar:\${LIB_DIR}/storageos-discoveryplugins.jar:\${LIB_DIR}/storageos-isilon.jar:\${LIB_DIR}/storageos-datadomain.jar:\${LIB_DIR}/storageos-vplex.jar:\${LIB_DIR}/storageos-vnx.jar:\${LIB_DIR}/storageos-vnxe.jar:\${LIB_DIR}/storageos-netapp.jar:\${LIB_DIR}/storageos-netappc.jar:\${LIB_DIR}/storageos-recoverpoint.jar:\${LIB_DIR}/storageos-compute.jar:\${LIB_DIR}/storageos-cinder.jar:\${LIB_DIR}/storageos-glance.jar:\${LIB_DIR}/storageos-hds.jar:\${LIB_DIR}/storageos-scaleio.jar:\${LIB_DIR}/storageos-xtremio.jar:\${LIB_DIR}/storageos-ecs.jar:\${LIB_DIR}/storageos-ceph.jar:\${LIB_DIR}/storageos-storagedriver.jar:\${LIB_DIR}/storageos-storagedriversimulator.jar:\${LIB_DIR}/slf4j-api-1.7.2.jar:\${LIB_DIR}/slf4j-log4j12-1.7.2.jar:\${LIB_DIR}/jul-to-slf4j-1.7.2.jar:\${LIB_DIR}/log4j-1.2.16.jar:\${LIB_DIR}/apache-log4j-extras-1.1.jar:\${LIB_DIR}/joda-time-2.1.jar:\${LIB_DIR}/jna-4.0.0.jar:\${LIB_DIR}/jna-platform-4.0.0.jar:\${LIB_DIR}/netty-all-4.0.23.Final.jar:\${LIB_DIR}/super-csv-2.1.0.jar:\${LIB_DIR}/antlr-3.5.2.jar:\${LIB_DIR}/antlr-runtime-3.5.2.jar:\${LIB_DIR}/uuid-3.2.jar:\${LIB_DIR}/cassandra-clientutil-2.1.11.jar:\${LIB_DIR}/cassandra-thrift-2.1.11.jar:\${LIB_DIR}/cassandra-all-2.1.11.jar:\${LIB_DIR}/commons-lang3-3.1.jar:\${LIB_DIR}/commons-math3-3.2.jar:\${LIB_DIR}/guava-16.0.jar:\${LIB_DIR}/compress-lzf-0.8.4.jar:\${LIB_DIR}/high-scale-lib-1.0.6.jar:\${LIB_DIR}/jackson-mapper-asl-1.9.2.jar:\${LIB_DIR}/jackson-core-asl-1.9.2.jar:\${LIB_DIR}/jamm-0.3.0.jar:\${LIB_DIR}/jbcrypt-0.3m.jar:\${LIB_DIR}/jline-1.0.jar:\${LIB_DIR}/json-simple-1.1.jar:\${LIB_DIR}/lz4-1.2.0.jar:\${LIB_DIR}/metrics-core-2.2.0.jar:\${LIB_DIR}/servlet-api-2.5.jar:\${LIB_DIR}/snakeyaml-1.11.jar:\${LIB_DIR}/snaptree-0.1.jar:\${LIB_DIR}/snappy-java-1.0.5.jar:\${LIB_DIR}/concurrentlinkedhashmap-lru-1.3.jar:\${LIB_DIR}/commons-cli-1.1.jar:\${LIB_DIR}/libthrift-0.9.2.jar:\${LIB_DIR}/astyanax-queue-1.56.49.jar:\${LIB_DIR}/astyanax-recipes-1.56.49.jar:\${LIB_DIR}/astyanax-cassandra-1.56.49.jar:\${LIB_DIR}/astyanax-core-1.56.49.jar:\${LIB_DIR}/astyanax-entity-mapper-1.56.49.jar:\${LIB_DIR}/astyanax-thrift-1.56.49.jar:\${LIB_DIR}/commons-lang-2.6.jar:\${LIB_DIR}/stream-2.5.2.jar:\${LIB_DIR}/reporter-config-2.1.0.jar:\${LIB_DIR}/thrift-server-0.3.7.jar:\${LIB_DIR}/airline-0.6.jar:\${LIB_DIR}/commons-io-2.4.jar:\${LIB_DIR}/vijava-5.1.jar:\${LIB_DIR}/httpclient-4.3.3.jar:\${LIB_DIR}/httpcore-4.3.2.jar:\${LIB_DIR}/commons-logging-1.1.1.jar:\${LIB_DIR}/commons-logging-adapters-1.1.jar:\${LIB_DIR}/commons-logging-api-1.1.jar:\${LIB_DIR}/com.iwave.ext.command.jar:\${LIB_DIR}/com.iwave.ext.ssh.jar:\${LIB_DIR}/commons-beanutils-1.8.3.jar:\${LIB_DIR}/cglib-nodep-2.2.2.jar:\${LIB_DIR}/storageos-dbclient.jar:\${LIB_DIR}/storageos-errorhandling.jar:\${LIB_DIR}/spring-security-aspects-3.1.0.RELEASE.jar:\${LIB_DIR}/spring-security-config-3.1.0.RELEASE.jar:\${LIB_DIR}/spring-security-core-3.1.0.RELEASE.jar:\${LIB_DIR}/spring-security-crypto-3.1.0.RELEASE.jar:\${LIB_DIR}/spring-security-ldap-3.1.0.RELEASE.jar:\${LIB_DIR}/spring-security-openid-3.1.0.RELEASE.jar:\${LIB_DIR}/spring-security-remoting-3.1.0.RELEASE.jar:\${LIB_DIR}/spring-security-taglibs-3.1.0.RELEASE.jar:\${LIB_DIR}/spring-security-web-3.1.0.RELEASE.jar:\${LIB_DIR}/spring-ldap-core-1.3.1.RELEASE.jar:\${LIB_DIR}/jetty-server-7.6.4.v20120524.jar:\${LIB_DIR}/jetty-util-7.6.4.v20120524.jar:\${LIB_DIR}/jetty-servlet-7.6.4.v20120524.jar:\${LIB_DIR}/jetty-servlets-7.6.4.v20120524.jar:\${LIB_DIR}/jetty-http-7.6.4.v20120524.jar:\${LIB_DIR}/jetty-security-7.6.4.v20120524.jar:\${LIB_DIR}/jetty-io-7.6.4.v20120524.jar:\${LIB_DIR}/jetty-continuation-7.6.4.v20120524.jar:\${LIB_DIR}/jetty-deploy-7.6.4.v20120524.jar:\${LIB_DIR}/jetty-webapp-7.6.4.v20120524.jar:\${LIB_DIR}/jetty-xml-7.6.4.v20120524.jar:\${LIB_DIR}/esapi-2.1.0.jar:\${LIB_DIR}/jsoup-1.8.2.jar:\${LIB_DIR}/storageos-geomodels.jar:\${LIB_DIR}/storageos-coordinatorsvc.jar:\${LIB_DIR}/storageos-keystone.jar:\${LIB_DIR}/mail-1.4.3.jar:\${LIB_DIR}/sblim-cim-client-2.2.3-vipr1.jar:\${LIB_DIR}/storageos-serviceutils.jar:\${LIB_DIR}/vnxfile-xmlapi-0.jar:\${LIB_DIR}/com.iwave.ext.netapp-2.4.1.0.4878.78d5471.jar:\${LIB_DIR}/com.iwave.ext.netappc-2.4.1.0.4878.78d5471.jar:\${LIB_DIR}/asm-3.1.jar:\${LIB_DIR}/jackson-jaxrs-1.9.2.jar:\${LIB_DIR}/jackson-xc-1.9.2.jar:\${LIB_DIR}/jersey-client-1.12.jar:\${LIB_DIR}/jersey-core-1.12.jar:\${LIB_DIR}/jersey-json-1.12.jar:\${LIB_DIR}/jersey-server-1.12.jar:\${LIB_DIR}/jersey-servlet-1.12.jar:\${LIB_DIR}/jettison-1.1.jar:\${LIB_DIR}/jsr311-api-1.1.1.jar:\${LIB_DIR}/jersey-apache-client-1.12.jar:\${LIB_DIR}/jersey-apache-client4-1.12.jar:\${LIB_DIR}/jersey-multipart-1.12.jar:\${LIB_DIR}/fapi-4.1.0.26.d7f820a.jar:\${LIB_DIR}/aopalliance-1.0.jar:\${LIB_DIR}/spring-aop-3.1.0.RELEASE.jar:\${LIB_DIR}/spring-asm-3.1.0.RELEASE.jar:\${LIB_DIR}/spring-aspects-3.1.0.RELEASE.jar:\${LIB_DIR}/spring-beans-3.1.0.RELEASE.jar:\${LIB_DIR}/spring-context-3.1.0.RELEASE.jar:\${LIB_DIR}/spring-context-support-3.1.0.RELEASE.jar:\${LIB_DIR}/spring-core-3.1.0.RELEASE.jar:\${LIB_DIR}/spring-expression-3.1.0.RELEASE.jar:\${LIB_DIR}/spring-instrument-3.1.0.RELEASE.jar:\${LIB_DIR}/spring-instrument-tomcat-3.1.0.RELEASE.jar:\${LIB_DIR}/spring-jdbc-3.1.0.RELEASE.jar:\${LIB_DIR}/spring-jms-3.1.0.RELEASE.jar:\${LIB_DIR}/spring-orm-3.1.0.RELEASE.jar:\${LIB_DIR}/spring-oxm-3.1.0.RELEASE.jar:\${LIB_DIR}/spring-test-3.1.0.RELEASE.jar:\${LIB_DIR}/spring-tx-3.1.0.RELEASE.jar:\${LIB_DIR}/spring-web-3.1.0.RELEASE.jar:\${LIB_DIR}/spring-webmvc-portlet-3.1.0.RELEASE.jar:\${LIB_DIR}/spring-webmvc-3.1.0.RELEASE.jar:\${LIB_DIR}/spring-struts-3.1.0.RELEASE.jar:\${LIB_DIR}/ucs-schema-3.0.1c.25.5dcde79.jar:\${LIB_DIR}/milyn-smooks-all-1.5.1.jar:\${LIB_DIR}/jaxen-1.1.1.jar:\${LIB_DIR}/rados-0.3.0.jar:\${LIB_DIR}/dom4j-1.6.1.jar:\${LIB_DIR}/javassist-3.18.0-GA.jar:\${LIB_DIR}/storageos-jmx.jar:\${LIB_DIR}/storageos-models.jar:\${LIB_DIR}/netty-3.2.8.Final.jar:\${LIB_DIR}/zookeeper-3.4.6.jar:\${LIB_DIR}/curator-client-2.10.0.jar:\${LIB_DIR}/curator-framework-2.10.0.jar:\${LIB_DIR}/curator-recipes-2.10.0.jar:\${LIB_DIR}/curator-test-2.10.0.jar:\${LIB_DIR}/curator-x-discovery-2.10.0.jar:\${LIB_DIR}/curator-x-discovery-server-2.10.0.jar:\${LIB_DIR}/activation-1.1.jar:\${LIB_DIR}/json-sanitizer-1.0.jar:\${LIB_DIR}/jmdns-3.4.1.jar:\${LIB_DIR}/xml-apis-1.0.b2.jar:\${LIB_DIR}/tools.jar:\${LIB_DIR}/denaliStorageDriver.jar\"" controllersvc
sudo /etc/storageos/storageos restart
/opt/ADG/conf/configure.sh waitStorageOS
