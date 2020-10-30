FROM bitnami/minideb:stretch
RUN install_packages ca-certificates
ADD https://github.com/bsycorp/make-jks/releases/download/0.2.3/make-jks-linux-x64 /make-jks
RUN chmod +x /make-jks
ADD entrypoint.sh /entrypoint.sh
RUN chmod +x /entrypoint.sh
ADD build/graal/kees-create-linux-x86_64 /kees-create
RUN chmod +x /kees-create
ENTRYPOINT ["/entrypoint.sh"]
CMD ["/kees-create"]