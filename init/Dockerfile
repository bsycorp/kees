FROM bitnami/java:1.8-prod
ENV JAVA_OPTS=""
ADD build/distributions/init.tar /app/
CMD ["/app/init/bin/init"]