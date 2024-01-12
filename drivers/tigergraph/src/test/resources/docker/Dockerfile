FROM tigergraph/tigergraph:3.9.3-1
ENTRYPOINT /usr/sbin/sshd && \
    su - tigergraph bash -c "/home/tigergraph/tigergraph/app/cmd/gadmin start all && tail -f /dev/null"