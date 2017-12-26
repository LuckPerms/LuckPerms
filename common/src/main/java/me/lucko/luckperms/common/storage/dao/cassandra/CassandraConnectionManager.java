package me.lucko.luckperms.common.storage.dao.cassandra;

import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.Statement;
import com.datastax.driver.core.querybuilder.QueryBuilder;

public class CassandraConnectionManager implements AutoCloseable {
    private final Cluster cluster;
    private final Session session;

    public CassandraConnectionManager(CassandraConfig config) {
        Cluster.Builder builder = Cluster.builder().addContactPointsWithPorts(config.getNodes());
        if(config.isSsl()) builder.withSSL();
        String username = config.getUsername();
        String password = config.getPassword();
        if(isNotEmpty(username) && isNotEmpty(password)) builder.withCredentials(username, password);
        this.cluster = builder.build();
        this.session = cluster.connect();
    }

    protected Session getSession() {
        return session;
    }

    protected Cluster getCluster() {
        return cluster;
    }

    @Override
    public void close() throws Exception {
        cluster.close();
    }

    private static boolean isNotEmpty(String str) {
        return str != null && str.length() != 0;
    }
}
