FROM bitnami/java:1.8-prod
ENV JAVA_OPTS=""
ADD build/distributions/create.tar /app/
CMD ["/app/create/bin/create"]