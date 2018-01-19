package me.lucko.luckperms.common.storage.dao.cassandra;

import com.datastax.driver.core.*;
import com.datastax.driver.core.exceptions.DriverException;
import me.lucko.luckperms.api.HeldPermission;
import me.lucko.luckperms.api.LogEntry;
import me.lucko.luckperms.api.Node;
import me.lucko.luckperms.common.actionlog.ExtendedLogEntry;
import me.lucko.luckperms.common.actionlog.Log;
import me.lucko.luckperms.common.bulkupdate.BulkUpdate;
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
    private final CassandraConfig config;
    private CassandraConnectionManager connectionManager;

    private PreparedStatement ACTION_INSERT;
    private PreparedStatement ACTION_SELECT_ALL;

    private PreparedStatement USER_SELECT_ALL_UUID;
    private PreparedStatement USER_SELECT_PERMISSIONS;
    private PreparedStatement USER_SELECT;
    private PreparedStatement USER_DELETE;
    private PreparedStatement USER_INSERT;
    private PreparedStatement USER_RENAME;
    private PreparedStatement USER_UPDATE_PERMISSIONS;

    private PreparedStatement GROUP_SELECT_ALL;
    private PreparedStatement GROUP_SELECT;
    private PreparedStatement GROUP_INSERT;
    private PreparedStatement GROUP_DELETE;

    private PreparedStatement TRACK_SELECT;
    private PreparedStatement TRACK_SELECT_ALL;
    private PreparedStatement TRACK_INSERT;
    private PreparedStatement TRACK_DELETE;

    private PreparedStatement UUID_TO_NAME_SELECT;
    private PreparedStatement NAME_TO_UUID_SELECT;
    private PreparedStatement UUID_TO_NAME_INSERT;
    private PreparedStatement NAME_TO_UUID_INSERT;

    public CassandraDao(LuckPermsPlugin plugin, CassandraConfig config) {
        super(plugin, "Cassandra");
        this.prefix = str -> str.replace("{prefix}", config.getPrefix());
        this.config = config;
    }

    @Override
    public void init() {
        this.connectionManager = new CassandraConnectionManager(config);
        Session session = connectionManager.getSession();
        Cluster cluster = connectionManager.getCluster();
        if (cluster.getMetadata().getKeyspace(config.getKeyspace()) == null) {
            cluster.connect().execute("CREATE KEYSPACE IF NOT EXISTS " + config.getKeyspace() + " WITH REPLICATION = {'class':'SimpleStrategy', 'replication_factor':1};");
            session.execute("USE " + config.getKeyspace());
        } else {
            session.execute("USE " + config.getKeyspace());
        }
        KeyspaceMetadata keyspace = session.getCluster().getMetadata().getKeyspace(config.getKeyspace());
        UserType testTable = keyspace.getUserType(prefix.apply("permission"));
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
                        if (line.endsWith(";")) {
                            String result = prefix.apply(sb.toString());
                            if(!result.isEmpty()) session.execute(result);
                            sb = new StringBuilder();
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                plugin.getLog().severe("Error occurred whilst initialising the database.");
                shutdown();
            }
        }

        TypeCodec<UDTValue> codec = CodecRegistry.DEFAULT_INSTANCE.codecFor(keyspace.getUserType("permission"));
        NodeCodec nodeCodec = new NodeCodec(codec);
        CodecRegistry.DEFAULT_INSTANCE.register(nodeCodec);

        ACTION_INSERT = session.prepare(this.prefix.apply("INSERT INTO {prefix}actions(time, actor_uuid, actor_name, type, acted_uuid, acted_name, action) VALUES(?, ?, ?, ?, ?, ?, ?)"));
        ACTION_SELECT_ALL = session.prepare(this.prefix.apply("SELECT * FROM {prefix}actions"));

        USER_SELECT_ALL_UUID = session.prepare(this.prefix.apply("SELECT uuid FROM {prefix}users"));
        USER_SELECT_PERMISSIONS = session.prepare(this.prefix.apply("SELECT uuid, permissions FROM {prefix}users"));
        USER_SELECT = session.prepare(this.prefix.apply("SELECT * FROM {prefix}users WHERE uuid=?"));
        USER_DELETE = session.prepare(this.prefix.apply("DELETE FROM {prefix}users WHERE uuid=?"));
        USER_INSERT = session.prepare(this.prefix.apply("INSERT INTO {prefix}users(uuid, name, permissions, primaryGroup) VALUES(?, ?, ?, ?)"));
        USER_RENAME = session.prepare(this.prefix.apply("UPDATE {prefix}users SET name=? WHERE uuid=?"));
        USER_UPDATE_PERMISSIONS = session.prepare(this.prefix.apply("UPDATE {prefix}users SET permissions=? WHERE uuid=?"));

        GROUP_SELECT_ALL = session.prepare(this.prefix.apply("SELECT * FROM {prefix}groups"));
        GROUP_SELECT = session.prepare(this.prefix.apply("SELECT * FROM {prefix}groups WHERE name=?"));
        GROUP_INSERT = session.prepare(this.prefix.apply("INSERT INTO {prefix}groups(name, permissions) VALUES(?, ?)"));
        GROUP_DELETE = session.prepare(this.prefix.apply("DELETE FROM {prefix}groups WHERE name=?"));

        TRACK_SELECT = session.prepare(this.prefix.apply("SELECT * FROM {prefix}tracks WHERE name=?"));
        TRACK_SELECT_ALL = session.prepare(this.prefix.apply("SELECT * FROM {prefix}tracks"));
        TRACK_INSERT = session.prepare(this.prefix.apply("INSERT INTO {prefix}tracks(name, groups) VALUES(?, ?)"));
        TRACK_DELETE = session.prepare(this.prefix.apply("DELETE FROM {prefix}tracks WHERE name=?"));

        UUID_TO_NAME_SELECT = session.prepare(this.prefix.apply("SELECT name FROM {prefix}uuid_to_name WHERE uuid=?"));
        NAME_TO_UUID_SELECT = session.prepare(this.prefix.apply("SELECT uuid FROM {prefix}name_to_uuid WHERE name=?"));
        UUID_TO_NAME_INSERT = session.prepare(this.prefix.apply("INSERT INTO {prefix}uuid_to_name(uuid, name) VALUES(?, ?)"));
        NAME_TO_UUID_INSERT = session.prepare(this.prefix.apply("INSERT INTO {prefix}name_to_uuid(name, uuid) VALUES(?, ?)"));
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
    public void logAction(LogEntry entry) throws DriverException {
        BoundStatement bind = ACTION_INSERT.bind(entry.getTimestamp(),
                entry.getActor(),
                entry.getActorName(),
                entry.getType().getCode(),
                entry.getActed().orElse(null),
                entry.getActedName(),
                entry.getAction());
        connectionManager.getSession().execute(bind);
    }

    @Override
    public Log getLog() throws DriverException {
        Log.Builder builder = Log.builder();
        BoundStatement bind = ACTION_SELECT_ALL.bind();
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
    }

    @Override
    public void applyBulkUpdate(BulkUpdate bulkUpdate) {
        if(bulkUpdate.getDataType().isIncludingUsers()) {
            BoundStatement bind = USER_SELECT_PERMISSIONS.bind();
            ResultSet rs = connectionManager.getSession().execute(bind);
            for (Row row : rs) {
                Set<NodeModel> permissions = row.getSet("permissions", NodeModel.class);
                Set<NodeModel> collect = permissions.stream()
                        .map(bulkUpdate::apply)
                        .filter(Objects::nonNull)
                        .collect(Collectors.toSet());
                if(!collect.equals(permissions)) {
                    BoundStatement bs = USER_UPDATE_PERMISSIONS.bind(row.getUUID("uuid"), collect);
                    connectionManager.getSession().execute(bs);
                }
            }
        }

        if(bulkUpdate.getDataType().isIncludingGroups()) {
            BoundStatement bind = GROUP_SELECT_ALL.bind();
            ResultSet rs = connectionManager.getSession().execute(bind);
            for (Row row : rs) {
                Set<NodeModel> permissions = row.getSet("permissions", NodeModel.class);
                Set<NodeModel> collect = permissions.stream()
                        .map(bulkUpdate::apply)
                        .filter(Objects::nonNull)
                        .collect(Collectors.toSet());
                if(!collect.equals(permissions)) {
                    BoundStatement bs = GROUP_INSERT.bind(row.getString("name"), collect);
                    connectionManager.getSession().execute(bs);
                }
            }
        }
    }

    @Override
    public User loadUser(UUID uuid, String username) {
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
                if (this.plugin.getUserManager().shouldSave(user)) {
                    user.clearNodes();
                    user.getPrimaryGroup().setStoredValue(null);
                    plugin.getUserManager().giveDefaultIfNeeded(user, false);
                }
            }
        } finally {
            user.getIoLock().unlock();
        }
        user.getRefreshBuffer().requestDirectly();
        return user;
    }

    @Override
    public void saveUser(User user) {
        user.getIoLock().lock();
        try {
            if (!this.plugin.getUserManager().shouldSave(user)) {
                BoundStatement bind = USER_DELETE.bind(user.getUuid());
                connectionManager.getSession().execute(bind);
            } else {
                Set<NodeModel> nodes = user.getEnduringNodes().values().stream()
                        .map(NodeModel::fromNode).collect(Collectors.toSet());
                BoundStatement bind = USER_INSERT.bind(user.getUuid(),
                        user.getName().orElse("null"),
                        nodes,
                        user.getPrimaryGroup().getStoredValue().orElse("default"));
                connectionManager.getSession().execute(bind);
            }
        } finally {
            user.getIoLock().unlock();
        }
    }

    @Override
    public Set<UUID> getUniqueUsers() {
        Set<UUID> ids = new HashSet<>();
        BoundStatement bind = USER_SELECT_ALL_UUID.bind();
        ResultSet rs = connectionManager.getSession().execute(bind);
        for (Row row : rs) {
            ids.add(row.getUUID("uuid"));
        }
        return ids;
    }

    @Override
    public List<HeldPermission<UUID>> getUsersWithPermission(String permission) {
        //https://issues.apache.org/jira/browse/CASSANDRA-7396
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
    }

    @Override
    public Group createAndLoadGroup(String name) {
        Group group = plugin.getGroupManager().getOrMake(name);
        group.getIoLock().lock();
        try {
            // if store contains group, load from store else save
            BoundStatement bind = GROUP_SELECT.bind(name);
            ResultSet rs = connectionManager.getSession().execute(bind);
            Row row = rs.one();
            if(row != null) {
                Set<NodeModel> permissions = row.getSet("permissions", NodeModel.class);
                group.setEnduringNodes(permissions.stream().map(NodeModel::toNode).collect(Collectors.toSet()));
            } else {
                saveGroup(group);
            }
            group.getRefreshBuffer().requestDirectly();
        } finally {
            group.getIoLock().unlock();
        }
        return group;
    }

    @Override
    public Optional<Group> loadGroup(String name) {
        Group group = null;
        try {
            // if store contains group, load from store else save
            BoundStatement bind = GROUP_SELECT.bind(name);
            ResultSet rs = connectionManager.getSession().execute(bind);
            Row row = rs.one();
            if(row != null) {
                group = plugin.getGroupManager().getOrMake(name);
                group.getIoLock().lock();
                Set<NodeModel> permissions = row.getSet("permissions", NodeModel.class);
                group.setEnduringNodes(permissions.stream().map(NodeModel::toNode).collect(Collectors.toSet()));
                group.getRefreshBuffer().requestDirectly();
            }
        } finally {
            if(group != null) group.getIoLock().unlock();
        }
        return Optional.ofNullable(group);
    }

    @Override
    public void loadAllGroups() {
        BoundStatement bind = GROUP_SELECT_ALL.bind();
        ResultSet rs = connectionManager.getSession().execute(bind);
        for (Row row : rs) {
            String name = row.getString("name");
            Set<Node> permissions = row.getSet("permissions", NodeModel.class).stream()
                    .map(NodeModel::toNode)
                    .collect(Collectors.toSet());

            Group group = plugin.getGroupManager().getOrMake(name);
            group.getIoLock().lock();
            group.setEnduringNodes(permissions);
            group.getIoLock().unlock();
        }
    }

    @Override
    public void saveGroup(Group group) {
        group.getIoLock().lock();
        try {
            String name = group.getName();
            Set<NodeModel> permissions = group.getEnduringNodes().values().stream()
                    .map(NodeModel::fromNode)
                    .collect(Collectors.toSet());
            BoundStatement bind = GROUP_INSERT.bind(name, permissions);
            connectionManager.getSession().execute(bind);
        } finally {
            group.getIoLock().unlock();
        }
    }

    @Override
    public void deleteGroup(Group group) {
        group.getIoLock().lock();
        try {
            BoundStatement bind = GROUP_DELETE.bind(group.getName());
            connectionManager.getSession().execute(bind);
        } finally {
            group.getIoLock().unlock();
        }
    }

    @Override
    public List<HeldPermission<String>> getGroupsWithPermission(String permission) {
        //https://issues.apache.org/jira/browse/CASSANDRA-7396
        List<HeldPermission<String>> list = new ArrayList<>();
        BoundStatement bind = GROUP_SELECT_ALL.bind();
        ResultSet rs = connectionManager.getSession().execute(bind);
        for (Row row : rs) {
            String name = row.getString("name");
            Set<NodeModel> permissions = row.getSet("permissions", NodeModel.class);
            for (NodeModel perm : permissions) {
                if(perm.getPermission().equalsIgnoreCase(permission)) {
                    list.add(NodeHeldPermission.of(name, perm));
                }
            }
        }
        return Collections.unmodifiableList(list);
    }

    @Override
    public Track createAndLoadTrack(String name) {
        Track track = plugin.getTrackManager().getOrMake(name);
        track.getIoLock().lock();
        try {
            BoundStatement bind = TRACK_SELECT.bind(name);
            ResultSet rs = connectionManager.getSession().execute(bind);
            Row row = rs.one();
            if(row != null) {
                List<String> groups = row.getList("groups", String.class);
                track.setGroups(groups);
            } else {
                saveTrack(track);
            }
        } finally {
            track.getIoLock().unlock();
        }
        return track;
    }

    @Override
    public Optional<Track> loadTrack(String name) {
        Track track = plugin.getTrackManager().getOrMake(name);
        track.getIoLock().lock();
        try {
            BoundStatement bind = TRACK_SELECT.bind(name);
            ResultSet rs = connectionManager.getSession().execute(bind);
            Row row = rs.one();
            if(row != null) {
                List<String> groups = row.getList("groups", String.class);
                track.setGroups(groups);
            }
        } finally {
            track.getIoLock().unlock();
        }
        return Optional.of(track);
    }

    @Override
    public void loadAllTracks() {
        BoundStatement bind = TRACK_SELECT_ALL.bind();
        ResultSet rs = connectionManager.getSession().execute(bind);
        for (Row row : rs) {
            String name = row.getString("name");
            List<String> groups = row.getList("groups", String.class);
            Track track = plugin.getTrackManager().getOrMake(name);
            track.getIoLock().lock();
            track.setGroups(groups);
            track.getIoLock().unlock();
        }
    }

    @Override
    public void saveTrack(Track track) {
        track.getIoLock().lock();
        try {
            BoundStatement bind = TRACK_INSERT.bind(track.getName(), track.getGroups());
            connectionManager.getSession().execute(bind);
        } finally {
            track.getIoLock().unlock();
        }
    }

    @Override
    public void deleteTrack(Track track) {
        track.getIoLock().lock();
        try {
            BoundStatement bind = TRACK_DELETE.bind(track.getName());
            connectionManager.getSession().execute(bind);
        } finally {
            track.getIoLock().unlock();
        }
    }

    @Override
    public void saveUUIDData(UUID uuid, String username) {
        BatchStatement bs = new BatchStatement();
        BoundStatement bind = UUID_TO_NAME_INSERT.bind(uuid, username);
        BoundStatement bind1 = NAME_TO_UUID_INSERT.bind(username, uuid);
        bs.add(bind).add(bind1);
        connectionManager.getSession().execute(bs);
    }

    @Override
    public UUID getUUID(String username) {
        BoundStatement bind = NAME_TO_UUID_SELECT.bind(username);
        ResultSet rs = connectionManager.getSession().execute(bind);
        Row row = rs.one();
        return row != null ? row.getUUID("uuid") : null;
    }

    @Override
    public String getName(UUID uuid) {
        BoundStatement bind = UUID_TO_NAME_SELECT.bind(uuid);
        ResultSet rs = connectionManager.getSession().execute(bind);
        Row row = rs.one();
        return row != null ? row.getString("name") : null;
    }
}
