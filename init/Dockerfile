FROM bitnami/minideb:stretch
ADD build/graal/kees-init-linux-x86_64 /kees-init
RUN chmod +x /kees-init
CMD ["/kees-init"]