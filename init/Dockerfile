FROM bitnami/minideb:stretch
ENV JAVA_OPTS=""
ADD build/graal/kees-init-linux-x86_64 /kees-init
RUN chmod +x /kees-init
CMD exec /kees-init $JAVA_OPTS