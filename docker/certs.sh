#!/bin/sh
mkdir -p /etc/ssl/certs
echo "" > /etc/ssl/certs/dummy-instance.pem
(cd /etc/ssl/certs; for file in *${CERT_FILTER:-instance}*.pem; do openssl x509 -outform der -in "$file" -out /tmp/certificate.der | true; keytool -import -alias "$file" -keystore $JAVA_HOME/lib/security/cacerts -file /tmp/certificate.der -deststorepass changeit -noprompt | true ; done);