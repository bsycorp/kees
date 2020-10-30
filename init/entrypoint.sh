#!/bin/bash
mkdir -p /etc/ssl/certs
mkdir -p $JAVA_HOME/lib/security/
echo "" > /etc/ssl/certs/dummy-instance.pem
/make-jks -import -dir /etc/ssl/certs -keystore $JAVA_HOME/lib/security/cacerts -storepass changeit
exec $@