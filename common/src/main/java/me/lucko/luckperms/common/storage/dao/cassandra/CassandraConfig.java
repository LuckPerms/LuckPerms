package me.lucko.luckperms.common.storage.dao.cassandra;

import java.net.InetSocketAddress;
import java.util.Set;

public class CassandraConfig {
    private final Set<InetSocketAddress> nodes;
    private final boolean ssl;
    private final String keyspace;
    private final String username;
    private final String password;
    private final String prefix;

    public CassandraConfig(Set<InetSocketAddress> nodes, boolean ssl, String keyspace, String username, String password, String prefix) {
        this.nodes = nodes;
        this.ssl = ssl;
        this.keyspace = keyspace;
        this.username = username;
        this.password = password;
        this.prefix = prefix;
    }

    public Set<InetSocketAddress> getNodes() {
        return this.nodes;
    }

    public boolean isSsl() {
        return this.ssl;
    }

    public String getKeyspace() {
        return this.keyspace;
    }

    public String getUsername() {
        return this.username;
    }

    public String getPassword() {
        return this.password;
    }

    public String getPrefix() {
        return this.prefix;
    }
}
