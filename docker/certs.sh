#!/bin/sh
mkdir -p /etc/ssl/certs
echo "" > /etc/ssl/certs/dummy-instance.pem
java -jar keyutil.jar -e /etc/ssl/certs/*instance*.pem -F -f $JAVA_HOME/lib/security/cacerts -i -p changeit -d