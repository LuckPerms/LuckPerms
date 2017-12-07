package me.lucko.luckperms.common.storage.dao.cassandra;

import com.datastax.driver.core.*;
import com.datastax.driver.core.exceptions.DriverException;
import me.lucko.luckperms.api.HeldPermission;
import me.lucko.luckperms.api.LogEntry;
import me.lucko.luckperms.common.actionlog.ExtendedLogEntry;
import me.lucko.luckperms.common.actionlog.Log;
import me.lucko.luckperms.common.bulkupdate.BulkUpdate;
import me.lucko.luckperms.common.managers.GenericUserManager;
import me.lucko.luckperms.common.model.Group;
import me.lucko.luckperms.common.model.Track;
import me.lucko.luckperms.common.model.User;
import me.lucko.luckperms.common.node.NodeHeldPermission;
import me.lucko.luckperms.common.node.NodeModel;
import me.lucko.luckperms.common.plugin.LuckPermsPlugin;
import me.lucko.luckperms.common.references.UserIdentifier;
import me.lucko.luckperms.common.storage.dao.AbstractDao;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public class CassandraDao extends AbstractDao {

    private final Function<String, String> prefix;
    private final CassandraConnectionManager connectionManager;

    private final PreparedStatement USER_SELECT_ALL_UUID;
    private final PreparedStatement USER_SELECT_PERMISSIONS;
    private final PreparedStatement USER_SELECT;
    private final PreparedStatement USER_DELETE;
    private final PreparedStatement USER_INSERT;
    private final PreparedStatement USER_RENAME;

    private final PreparedStatement ACTION_INSERT;
    private final PreparedStatement ACTION_SELECT_ALL;

    protected CassandraDao(LuckPermsPlugin plugin, CassandraConnectionManager connectionManager, String prefix) {
        super(plugin, "Cassandra");
        this.prefix = str -> str.replace("{prefix}", prefix);
        this.connectionManager = connectionManager;

        Session cluster = connectionManager.getSession();

        USER_SELECT_ALL_UUID = cluster.prepare(this.prefix.apply("SELECT uuid FROM {prefix}users"));
        USER_SELECT_PERMISSIONS = cluster.prepare(this.prefix.apply("SELECT uuid, permissions FROM {prefix}users"));
        USER_SELECT = cluster.prepare(this.prefix.apply("SELECT * FROM {prefix}users WHERE uuid=?"));
        USER_DELETE = cluster.prepare(this.prefix.apply("DELETE FROM {prefix}users WHERE uuid=?"));
        USER_INSERT = cluster.prepare(this.prefix.apply("INSERT INTO {prefix}users(uuid, name, permissions, primaryGroup) VALUES(?, ?, ?, ?)"));
        USER_RENAME = cluster.prepare(this.prefix.apply("UPDATE {prefix}users SET name=? WHERE uuid=?"));

        ACTION_INSERT = cluster.prepare(this.prefix.apply("INSERT INTO {prefix}actions(time, actor_uuid, actor_name, type, acted_uuid, acted_name, action) VALUES(?, ?, ?, ?, ?, ?, ?)"));
        ACTION_SELECT_ALL = cluster.prepare(this.prefix.apply("SELECT * FROM {prefix}actions"));
    }

    @Override
    public void init() {
        Session session = connectionManager.getSession();
        String keyspace = session.getLoggedKeyspace();
        TableMetadata testTable = session.getCluster()
                .getMetadata()
                .getKeyspace(keyspace)
                .getTable(prefix.apply("user_permissions"));
        if(testTable == null) {
            // create tables
            String schemaFileName = "schema/cassandra.cql";
            try (InputStream is = plugin.getResourceStream(schemaFileName)) {
                if (is == null) {
                    throw new Exception("Couldn't locate schema file for cassandra");
                }

                try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        if (line.startsWith("--") || line.startsWith("#")) continue;
                        sb.append(line);
                    }
                    session.execute(prefix.apply(sb.toString()));
                }
            } catch (Exception e) {
                e.printStackTrace();
                plugin.getLog().severe("Error occurred whilst initialising the database.");
                shutdown();
            }
        }
    }

    @Override
    public void shutdown() {
        try {
            connectionManager.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean logAction(LogEntry entry) {
        BoundStatement bind = ACTION_INSERT.bind(entry.getTimestamp(),
                entry.getActor(),
                entry.getActorName(),
                entry.getType().getCode(),
                entry.getActed().orElse(null),
                entry.getActedName(),
                entry.getAction());
        try {
            connectionManager.getSession().execute(bind);
        } catch (DriverException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    @Override
    public Log getLog() {
        Log.Builder builder = Log.builder();
        BoundStatement bind = ACTION_SELECT_ALL.bind();
        try {
            ResultSet rs = connectionManager.getSession().execute(bind);
            for (Row row : rs) {
                ExtendedLogEntry e = ExtendedLogEntry.build()
                        .timestamp(row.getTimestamp("time").getTime())
                        .actor(row.getUUID("actor_uuid"))
                        .actorName(row.getString("actor_name"))
                        .type(LogEntry.Type.valueOf(row.getString("type").charAt(0)))
                        .acted(row.getUUID("acted_uuid"))
                        .actedName(row.getString("acted_name"))
                        .action(row.getString("action"))
                        .build();
                builder.add(e);
            }
            return builder.build();
        } catch (DriverException e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public boolean applyBulkUpdate(BulkUpdate bulkUpdate) {
        return false; //todo: implement
    }

    @SuppressWarnings("Duplicates")
    @Override
    public boolean loadUser(UUID uuid, String username) {
        User user = plugin.getUserManager().getOrMake(UserIdentifier.of(uuid, username));
        user.getIoLock().lock();
        try {
            BoundStatement getPermissions = USER_SELECT.bind(uuid);
            ResultSet rs = connectionManager.getSession().execute(getPermissions);
            Row row = rs.one();
            if (row != null) {
                List<NodeModel> permissions = row.getList("permissions", NodeModel.class);
                String group = row.getString("primaryGroup");
                String name = row.getString("name");
                user.getPrimaryGroup().setStoredValue(group);
                user.setName(name, true);
                user.setEnduringNodes(permissions.stream().map(NodeModel::toNode).collect(Collectors.toSet()));
                if(user.getName().isPresent() && !user.getName().get().equalsIgnoreCase(name)) {
                    BoundStatement bind = USER_RENAME.bind(name, user.getUuid());
                    connectionManager.getSession().execute(bind);
                }
            } else {
                if (GenericUserManager.shouldSave(user)) {
                    user.clearNodes();
                    user.getPrimaryGroup().setStoredValue(null);
                    plugin.getUserManager().giveDefaultIfNeeded(user, false);
                }
            }
        } catch (DriverException e) {
            e.printStackTrace();
            return false;
        } finally {
            user.getIoLock().unlock();
        }
        user.getRefreshBuffer().requestDirectly();
        return true;
    }

    @SuppressWarnings("Duplicates")
    @Override
    public boolean saveUser(User user) {
        user.getIoLock().lock();
        try {
            if (!GenericUserManager.shouldSave(user)) {
                BoundStatement bind = USER_DELETE.bind(user.getUuid());
                connectionManager.getSession().execute(bind);
                return true;
            } else {
                Set<NodeModel> nodes = user.getEnduringNodes().values().stream()
                        .map(NodeModel::fromNode).collect(Collectors.toSet());
                BoundStatement bind = USER_INSERT.bind(user.getUuid(),
                        user.getName().orElse("null"),
                        nodes,
                        user.getPrimaryGroup().getStoredValue().orElse("default"));
                connectionManager.getSession().execute(bind);
                return true;
            }

        } catch (DriverException e) {
            e.printStackTrace();
            return false;
        } finally {
            user.getIoLock().unlock();
        }
    }

    @Override
    public Set<UUID> getUniqueUsers() {
        try {
            BoundStatement bind = USER_SELECT_ALL_UUID.bind();
            ResultSet rs = connectionManager.getSession().execute(bind);
            Set<UUID> ids = new HashSet<>();
            for (Row row : rs) {
                ids.add(row.getUUID("uuid"));
            }
            return ids;
        } catch (DriverException e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public List<HeldPermission<UUID>> getUsersWithPermission(String permission) {
        //https://issues.apache.org/jira/browse/CASSANDRA-7396
        try {
            List<HeldPermission<UUID>> list = new ArrayList<>();
            BoundStatement bind = USER_SELECT_PERMISSIONS.bind();
            ResultSet rs = connectionManager.getSession().execute(bind);
            for (Row row : rs) {
                UUID uuid = row.getUUID("uuid");
                Set<NodeModel> permissions = row.getSet("permissions", NodeModel.class);
                for (NodeModel perm : permissions) {
                    if(perm.getPermission().equalsIgnoreCase(permission)) {
                        list.add(NodeHeldPermission.of(uuid, perm));
                    }
                }
            }
            return Collections.unmodifiableList(list);
        } catch (DriverException e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public boolean createAndLoadGroup(String name) {
        Group group = plugin.getGroupManager().getOrMake(name);
        group.getIoLock().lock();
        try {
            // if store contains group, load from store else save
        } catch (DriverException e) {
            e.printStackTrace();
            return false;
        } finally {
            group.getIoLock().unlock();
        }
        group.getRefreshBuffer().requestDirectly();
        return false;
    }

    @Override
    public boolean loadGroup(String name) {
        return false;
    }

    @Override
    public boolean loadAllGroups() {
        return false;
    }

    @Override
    public boolean saveGroup(Group group) {
        return false;
    }

    @Override
    public boolean deleteGroup(Group group) {
        return false;
    }

    @Override
    public List<HeldPermission<String>> getGroupsWithPermission(String permission) {
        return null;
    }

    @Override
    public boolean createAndLoadTrack(String name) {
        return false;
    }

    @Override
    public boolean loadTrack(String name) {
        return false;
    }

    @Override
    public boolean loadAllTracks() {
        return false;
    }

    @Override
    public boolean saveTrack(Track track) {
        return false;
    }

    @Override
    public boolean deleteTrack(Track track) {
        return false;
    }

    @Override
    public boolean saveUUIDData(UUID uuid, String username) {
        return false;
    }

    @Override
    public UUID getUUID(String username) {
        return null;
    }

    @Override
    public String getName(UUID uuid) {
        return null;
    }
}
