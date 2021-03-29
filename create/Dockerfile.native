FROM bitnami/minideb:stretch
ADD build/graal/kees-create-linux-x86_64 /kees-create
RUN chmod +x /kees-create
CMD ["/kees-create"]