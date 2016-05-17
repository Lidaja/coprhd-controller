NAME=$(rpm -qa | grep storageos)
rpm -e $NAME
make clobber BUILD_TYPE=oss rpm
rpm -Uvh /tmp/coprhd-controller/build/RPMS/x86_64/*.rpm
sleep 5
./relaunch.sh
