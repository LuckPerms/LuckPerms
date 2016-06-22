package me.lucko.luckperms.data.methods;

import com.zaxxer.hikari.HikariDataSource;
import me.lucko.luckperms.LuckPermsPlugin;
import me.lucko.luckperms.data.MySQLConfiguration;

import java.sql.Connection;
import java.sql.SQLException;

public class MySQLDatastore extends SQLDatastore {

    private static final String CREATETABLE_UUID = "CREATE TABLE IF NOT EXISTS `lp_uuid` (`name` VARCHAR(16) NOT NULL, `uuid` VARCHAR(36) NOT NULL, PRIMARY KEY (`name`)) ENGINE=InnoDB DEFAULT CHARSET=latin1;";
    private static final String CREATETABLE_USERS = "CREATE TABLE IF NOT EXISTS `lp_users` (`uuid` VARCHAR(36) NOT NULL, `name` VARCHAR(16) NOT NULL, `perms` TEXT NOT NULL, PRIMARY KEY (`uuid`)) ENGINE=InnoDB DEFAULT CHARSET=latin1;";
    private static final String CREATETABLE_GROUPS = "CREATE TABLE IF NOT EXISTS `lp_groups` (`name` VARCHAR(36) NOT NULL, `perms` TEXT NULL, PRIMARY KEY (`name`)) ENGINE=InnoDB DEFAULT CHARSET=latin1;";

    private MySQLConfiguration configuration;
    private HikariDataSource hikari;

    public MySQLDatastore(LuckPermsPlugin plugin, MySQLConfiguration configuration) {
        super(plugin, "MySQL");
        this.configuration = configuration;
    }

    @Override
    public void init() {
        hikari = new HikariDataSource();

        final String address = configuration.getAddress();
        final String database = configuration.getDatabase();
        final String username = configuration.getUsername();
        final String password = configuration.getPassword();

        hikari.setMaximumPoolSize(10);
        hikari.setDataSourceClassName("com.mysql.jdbc.jdbc2.optional.MysqlDataSource");
        hikari.addDataSourceProperty("serverName", address.split(":")[0]);
        hikari.addDataSourceProperty("port", address.split(":")[1]);
        hikari.addDataSourceProperty("databaseName", database);
        hikari.addDataSourceProperty("user", username);
        hikari.addDataSourceProperty("password", password);

        setupTables(CREATETABLE_UUID, CREATETABLE_USERS, CREATETABLE_GROUPS);
    }

    @Override
    public void shutdown() {
        if (hikari != null) {
            hikari.shutdown();
        }
    }

    @Override
    Connection getConnection() throws SQLException {
        return hikari.getConnection();
    }
}
