package me.lucko.luckperms.common.storage.dao.cassandra;

import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.exceptions.DriverException;
import com.google.common.base.Strings;

public class CassandraConnectionManager implements AutoCloseable {
    private final Cluster cluster;
    private final Session session;

    public CassandraConnectionManager(CassandraConfig config) {
        Cluster.Builder builder = Cluster.builder().addContactPointsWithPorts(config.getNodes());
        if (config.isSsl()) {
            builder.withSSL();
        }
        String username = config.getUsername();
        String password = config.getPassword();
        if (!Strings.isNullOrEmpty(username) && !Strings.isNullOrEmpty(password)) {
            builder.withCredentials(username, password);
        }

        this.cluster = builder.build();
        this.session = this.cluster.connect();
    }

    protected Session getSession() {
        return this.session;
    }

    protected Cluster getCluster() {
        return this.cluster;
    }

    @Override
    public void close() throws DriverException {
        this.cluster.close();
    }
}
