#!/bin/bash
#
# Copyright 2016 EMC Corporation
# All Rights Reserved
#

function installSystem
{
  rm /etc/legal
  cat << EOF | passwd root
ChangeMe
ChangeMe
EOF

  cat > /etc/motd << EOF
Have a lot of fun...
EOF

  # Allow root SSH login
  if [ -f /etc/ssh/sshd_config ]; then
    sed -i -e "s/without-password/yes/g" /etc/ssh/sshd_config
  fi

  echo "127.0.0.1 localhost" >> /etc/hosts
  echo "127.0.0.1 localhost.localdomain" >> /etc/hosts

  sed -i -e "s/^#deb/deb/g" /etc/apt/sources.list
  sed -i -e "/^deb file/d" /etc/apt/sources.list

  mkdir -p /workspace
  chmod 1777 /workspace

  cp -f /etc/inputrc /etc/inputrc-original
  LineNo=$(grep -A 5 -n ".*Normal keypad and cursor of xterm" /etc/inputrc | grep history-search-backward | cut -d- -f1)
  LineNo="$LineNo "$(grep -A 5 -n ".*Normal keypad and cursor of xterm" /etc/inputrc | grep set-mark | cut -d- -f1)
  for ln in $LineNo; do sed -i "$ln s/.*/#&/" /etc/inputrc; done
}

function installJava
{
  java=$2
  [ ! -z "${java}" ] || java=8

  update-alternatives --set java /usr/lib/jvm/java-${java}-openjdk-amd64/jre/bin/java
  update-alternatives --set javac /usr/lib/jvm/java-${java}-openjdk-amd64/bin/javac
}

function installNginx
{
  if [ -f /tmp/templates/nginx-1.6.2.tar.gz -a -f /tmp/templates/v0.3.0.tar.gz -a -f /tmp/templates/v0.25.tar.gz ]; then
    mkdir -p /tmp/nginx
    mv /tmp/templates/nginx-1.6.2.tar.gz /tmp/nginx/
    mv /tmp/templates/v0.3.0.tar.gz /tmp/nginx/
    mv /tmp/templates/v0.25.tar.gz /tmp/nginx/
    tar --directory=/tmp/nginx -xzf /tmp/nginx/nginx-1.6.2.tar.gz
    tar --directory=/tmp/nginx -xzf /tmp/nginx/v0.3.0.tar.gz
    tar --directory=/tmp/nginx -xzf /tmp/nginx/v0.25.tar.gz
    patch --directory=/tmp/nginx/nginx-1.6.2 -p1 < /tmp/nginx/nginx_upstream_check_module-0.3.0/check_1.5.12+.patch
    bash -c "cd /tmp/nginx/nginx-1.6.2; ./configure --add-module=/tmp/nginx/nginx_upstream_check_module-0.3.0 --add-module=/tmp/nginx/headers-more-nginx-module-0.25 --with-http_ssl_module --prefix=/usr --conf-path=/etc/nginx/nginx.conf"
    make --directory=/tmp/nginx/nginx-1.6.2
    make --directory=/tmp/nginx/nginx-1.6.2 install
    rm -fr /tmp/nginx
  fi
}

function installStorageOS
{
  ln -s /bin/cp /usr/bin/cp
  cat > /etc/rc.status << EOF
#!/bin/bash
function rc_reset {
  /bin/true
}
function rc_failed {
  /bin/true
}
function rc_status {
  /bin/true
}
function rc_exit {
  /bin/true
}
EOF
  chmod u+x /etc/rc.status
  getent group storageos || groupadd -g 444 storageos
  getent passwd storageos || useradd -r -d /opt/storageos -c "StorageOS" -g 444 -u 444 -s /bin/bash storageos
  chown storageos:storageos /etc/rc.status
  [ ! -d /opt/storageos ] || chown -R storageos:storageos /opt/storageos
  [ ! -d /data ] || chown -R storageos:storageos /data
}

function enableStorageOS
{
  dpkg -s storageos &> /dev/null
  if [ $? -eq 0 ]; then
    rm /etc/init/keepalived.override
    rm /etc/init/nginx.override
    rm /etc/init/boot-ovfenv.override
    /etc/storageos/storageos enable
    /etc/storageos/boot-ovfenv start
    service keepalived start
    start nginx
    /etc/storageos/storageos start
  fi
}

function disableStorageOS
{
  dpkg -s storageos &> /dev/null
  if [ $? -eq 0 ]; then
    echo "manual" > /etc/init/boot-ovfenv.override
    echo "manual" > /etc/init/nginx.override
    echo "manual" > /etc/init/keepalived.override
    if [ -f /etc/storageos/storageos ]; then
      /etc/storageos/storageos stop
      /etc/storageos/storageos disable
    fi
    /etc/storageos/boot-ovfenv stop
    stop nginx
    service keepalived stop
  fi
}

function waitStorageOS
{
  source /etc/ovfenv.properties
  while [ ! -f /opt/storageos/logs/coordinatorsvc.log ]; do
    echo "Warning: coordinatorsvc unavailable. Waiting..."
    sleep 1
  done
  while [ ! -f /opt/storageos/logs/apisvc.log ]; do
    echo "Warning: apisvc unavailable. Waiting..."
    sleep 35
  done
  until $(curl --silent --insecure https://${network_1_ipaddr}:4443/formlogin | grep --quiet "Authorized Users Only" &>/dev/null); do
    echo "Warning: service unavailable. Waiting..."
    sleep 25
  done
  echo "UI ready on: https://${network_1_ipaddr}"
}

function installVagrant
{
  getent group vagrant || groupadd vagrant
  getent passwd vagrant || useradd -m -g vagrant -s /bin/bash -d /home/vagrant vagrant
  mkdir -p /home/vagrant/.ssh
  echo "ssh-rsa AAAAB3NzaC1yc2EAAAABIwAAAQEA6NF8iallvQVp22WDkTkyrtvp9eWW6A8YVr+kz4TjGYe7gHzIw+niNltGEFHzD8+v1I2YJ6oXevct1YeS0o9HZyN1Q9qgCgzUFtdOKLv6IedplqoPkcmF0aYet2PkEDo3MlTBckFXPITAMzF8dJSIFo9D8HfdOV0IAdx4O7PtixWKn5y2hMNG0zQPyUecp4pzC6kivAIhyfHilFR61RGL+GPXQ2MWZWFYbAGjyiYJnAmCP3NOTd0jMZEnDkbUvxhMmBYSdETk1rRgm+R4LOzFUGaHqHDLKLX+FIPKcF96hrucXzcWyLbIbEgE98OHlnVYCzRdK8jlqm8tehUc9c9WhQ== vagrant insecure public key" > /home/vagrant/.ssh/authorized_keys
  grep --quiet vagrant /etc/sudoers || echo "vagrant ALL=(ALL) NOPASSWD: ALL" >> /etc/sudoers
  chown -R vagrant:vagrant /home/vagrant
}

function installNetwork
{
  chmod 755 /etc/init/ovf-network.conf
  chmod 755 /opt/ADG/conf/ovf-network.sh
  rm /etc/network/interfaces
  #echo -e "/*" >> /etc/network/interfaces
}

function installNetworkConfigurationFile
{
  eth=$2
  gateway=$3
  netmask=$4
  [ ! -z "${eth}" ] || eth=1
  [ ! -z "${gateway}" ] || gateway=$(route -n | grep 'UG[ \t]' | awk '{print $2}')
  [ ! -z "${netmask}" ] || netmask='255.255.255.0'
  ipaddr=$(ifconfig | awk '/inet addr/{print substr($2,6)}' | head -n ${eth} | tail -n 1)
  cat > /etc/ovfenv.properties <<EOF
network_1_ipaddr6=::0
network_1_ipaddr=${ipaddr}
network_gateway6=::0
network_gateway=${gateway}
network_netmask=${netmask}
network_prefix_length=64
network_vip6=::0
network_vip=${ipaddr}
node_count=1
node_id=vipr1
EOF
}

$1 "$@"
