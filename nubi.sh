NAME=$(rpm -qa | grep storageos)
rpm -e $NAME
make clobber BUILD_TYPE=oss rpm
rpm -Uvh /tmp/coprhd-controller/build/RPMS/x86_64/*.rpm
sleep 3
cd /opt/storageos/conf
sed -i "12i <entry key=\"denaliStorageDriver\" value=\"denali\"/>" driver-conf.xml
sed -i "21i <value>denali</value>" driver-conf.xml
sed -i "45i <value>denali</value>" driver-conf.xml
sed -i "399i <entry key=\"denali\" value-ref=\"denaliStorageDriver\"/>" controller-conf.xml
sed -i "19i <entry key=\"denali\" value-ref=\"denaliStorageDriver\"/>" discovery-externaldevice-context.xml
jar cf /opt/storageos/lib/denaliStorageDriver.jar /tmp/coprhd-controller/internalLibraries/storagedriver/src/main/java/com/emc/storageos/storagedriver/DenaliDriver.java
cd /opt/storageos/bin
sed -i "6d" controllersvc
sed -i "6i export CLASSPATH=$CLASSPATH:$\{LIB_DIR\}/denaliStorageDriver.jar" controllersvc
sudo /etc/storageos/storageos restart
/opt/ADG/conf/configure.sh waitStorageOS
