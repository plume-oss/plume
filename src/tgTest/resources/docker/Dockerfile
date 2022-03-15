FROM docker.tigergraph.com/tigergraph:3.5.0
ENTRYPOINT /usr/sbin/sshd && \
    su - tigergraph bash -c "/home/tigergraph/tigergraph/app/cmd/gadmin start all && tail -f /dev/null"