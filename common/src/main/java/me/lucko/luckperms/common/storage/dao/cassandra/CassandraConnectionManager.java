package me.lucko.luckperms.common.storage.dao.cassandra;

import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.Session;

import java.net.InetSocketAddress;
import java.util.List;

public class CassandraConnectionManager implements AutoCloseable {

    private final Cluster cluster;
    private final Session session;

    public CassandraConnectionManager(List<InetSocketAddress> socketAddresses, boolean ssl, String username, String password, String keyspace) {
        Cluster.Builder builder = Cluster.builder().addContactPointsWithPorts(socketAddresses);
        if(ssl) builder.withSSL();
        if(username != null && username.length() > 0 &&
                password != null && password.length() > 0) builder.withCredentials(username, password);
        this.cluster = builder.build();
        this.session = cluster.connect(keyspace);
    }

    public Session getSession() {
        return session;
    }

    protected Cluster getCluster() {
        return cluster;
    }

    @Override
    public void close() throws Exception {
        cluster.close();
    }
}
