package me.lucko.luckperms.common.storage.dao.cassandra;

import java.net.InetSocketAddress;
import java.util.Set;

public class CassandraConfig {
    private final Set<InetSocketAddress> nodes;
    private final boolean ssl;
    private final String keyspace;
    private final String username;
    private final String password;

    public CassandraConfig(Set<InetSocketAddress> nodes, boolean ssl, String keyspace, String username, String password) {
        this.nodes = nodes;
        this.ssl = ssl;
        this.keyspace = keyspace;
        this.username = username;
        this.password = password;
    }

    public Set<InetSocketAddress> getNodes() {
        return nodes;
    }

    public boolean isSsl() {
        return ssl;
    }

    public String getKeyspace() {
        return keyspace;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }
}
