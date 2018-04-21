/*
 * This file is part of LuckPerms, licensed under the MIT License.
 *
 *  Copyright (c) lucko (Luck) <luck@lucko.me>
 *  Copyright (c) contributors
 *
 *  Permission is hereby granted, free of charge, to any person obtaining a copy
 *  of this software and associated documentation files (the "Software"), to deal
 *  in the Software without restriction, including without limitation the rights
 *  to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *  copies of the Software, and to permit persons to whom the Software is
 *  furnished to do so, subject to the following conditions:
 *
 *  The above copyright notice and this permission notice shall be included in all
 *  copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 *  SOFTWARE.
 */

package me.lucko.luckperms.common.storage.dao.file;

import com.google.common.base.Throwables;
import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;

import me.lucko.luckperms.api.ChatMetaType;
import me.lucko.luckperms.api.LogEntry;
import me.lucko.luckperms.api.Node;
import me.lucko.luckperms.api.context.ImmutableContextSet;
import me.lucko.luckperms.common.actionlog.Log;
import me.lucko.luckperms.common.bulkupdate.BulkUpdate;
import me.lucko.luckperms.common.contexts.ContextSetConfigurateSerializer;
import me.lucko.luckperms.common.model.Group;
import me.lucko.luckperms.common.model.Track;
import me.lucko.luckperms.common.model.User;
import me.lucko.luckperms.common.node.MetaType;
import me.lucko.luckperms.common.node.NodeFactory;
import me.lucko.luckperms.common.node.NodeModel;
import me.lucko.luckperms.common.plugin.LuckPermsPlugin;
import me.lucko.luckperms.common.references.UserIdentifier;
import me.lucko.luckperms.common.storage.PlayerSaveResult;
import me.lucko.luckperms.common.storage.dao.AbstractDao;
import me.lucko.luckperms.common.storage.dao.file.loader.ConfigurateLoader;
import me.lucko.luckperms.common.storage.dao.file.loader.JsonLoader;
import me.lucko.luckperms.common.utils.ImmutableCollectors;
import me.lucko.luckperms.common.utils.MoreFiles;

import ninja.leaping.configurate.ConfigurationNode;
import ninja.leaping.configurate.SimpleConfigurationNode;
import ninja.leaping.configurate.Types;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Abstract implementation using configurate {@link ConfigurationNode}s to serialize and deserialize
 * data.
 */
public abstract class AbstractConfigurateDao extends AbstractDao {

    // the loader responsible for i/o
    protected final ConfigurateLoader loader;

    // the name of the data directory
    private final String dataDirectoryName;
    // the data directory
    protected Path dataDirectory;

    // the uuid cache instance
    private final FileUuidCache uuidCache = new FileUuidCache();
    // the action logger instance
    private final FileActionLogger actionLogger;

    // the file used to store uuid data
    private Path uuidDataFile;

    protected AbstractConfigurateDao(LuckPermsPlugin plugin, ConfigurateLoader loader, String name, String dataDirectoryName) {
        super(plugin, name);
        this.loader = loader;
        this.dataDirectoryName = dataDirectoryName;
        this.actionLogger = new FileActionLogger(plugin);
    }

    /**
     * Reads a configuration node from the given location
     *
     * @param location the location
     * @param name the name of the object
     * @return the node
     * @throws IOException if an io error occurs
     */
    protected abstract ConfigurationNode readFile(StorageLocation location, String name) throws IOException;

    /**
     * Saves a configuration node to the given location
     *
     * @param location the location
     * @param name the name of the object
     * @param node the node
     * @throws IOException if an io error occurs
     */
    protected abstract void saveFile(StorageLocation location, String name, ConfigurationNode node) throws IOException;

    // used to report i/o exceptions which took place in a specific file
    protected RuntimeException reportException(String file, Exception ex) throws RuntimeException {
        this.plugin.getLogger().warn("Exception thrown whilst performing i/o: " + file);
        ex.printStackTrace();
        throw Throwables.propagate(ex);
    }

    @Override
    public void init() throws IOException {
        this.dataDirectory = this.plugin.getBootstrap().getDataDirectory().resolve(this.dataDirectoryName);
        Files.createDirectories(this.dataDirectory);

        this.uuidDataFile = MoreFiles.createFileIfNotExists(this.dataDirectory.resolve("uuidcache.txt"));
        this.uuidCache.load(this.uuidDataFile);

        this.actionLogger.init(this.dataDirectory.resolve("actions.json"));
    }

    @Override
    public void shutdown() {
        this.uuidCache.save(this.uuidDataFile);
        this.actionLogger.flush();
    }

    @Override
    public void logAction(LogEntry entry) {
        this.actionLogger.logAction(entry);
    }

    @Override
    public Log getLog() throws IOException {
        return this.actionLogger.getLog();
    }

    protected ConfigurationNode processBulkUpdate(BulkUpdate bulkUpdate, ConfigurationNode node) {
        Set<NodeModel> nodes = readNodes(node);
        Set<NodeModel> results = nodes.stream()
                .map(bulkUpdate::apply)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        if (nodes.equals(results)) {
            return null;
        }

        writeNodes(node, results);
        return node;
    }

    @Override
    public User loadUser(UUID uuid, String username) {
        User user = this.plugin.getUserManager().getOrMake(UserIdentifier.of(uuid, username));
        user.getIoLock().lock();
        try {
            ConfigurationNode object = readFile(StorageLocation.USER, uuid.toString());
            if (object != null) {
                String name = object.getNode("name").getString();
                user.getPrimaryGroup().setStoredValue(object.getNode(this.loader instanceof JsonLoader ? "primaryGroup" : "primary-group").getString());

                Set<Node> nodes = readNodes(object).stream().map(NodeModel::toNode).collect(Collectors.toSet());
                user.setEnduringNodes(nodes);
                user.setName(name, true);

                boolean save = this.plugin.getUserManager().giveDefaultIfNeeded(user, false);
                if (user.getName().isPresent() && (name == null || !user.getName().get().equalsIgnoreCase(name))) {
                    save = true;
                }

                if (save | user.auditTemporaryPermissions()) {
                    saveUser(user);
                }
            } else {
                if (this.plugin.getUserManager().shouldSave(user)) {
                    user.clearNodes();
                    user.getPrimaryGroup().setStoredValue(null);
                    this.plugin.getUserManager().giveDefaultIfNeeded(user, false);
                }
            }
        } catch (Exception e) {
            throw reportException(uuid.toString(), e);
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
                saveFile(StorageLocation.USER, user.getUuid().toString(), null);
            } else {
                ConfigurationNode data = SimpleConfigurationNode.root();
                if (this instanceof SeparatedConfigurateDao) {
                    data.getNode("uuid").setValue(user.getUuid().toString());
                }
                data.getNode("name").setValue(user.getName().orElse("null"));
                data.getNode(this.loader instanceof JsonLoader ? "primaryGroup" : "primary-group").setValue(user.getPrimaryGroup().getStoredValue().orElse(NodeFactory.DEFAULT_GROUP_NAME));

                Set<NodeModel> nodes = user.getEnduringNodes().values().stream().map(NodeModel::fromNode).collect(Collectors.toCollection(LinkedHashSet::new));
                writeNodes(data, nodes);

                saveFile(StorageLocation.USER, user.getUuid().toString(), data);
            }
        } catch (Exception e) {
            throw reportException(user.getUuid().toString(), e);
        } finally {
            user.getIoLock().unlock();
        }
    }

    @Override
    public Group createAndLoadGroup(String name) {
        Group group = this.plugin.getGroupManager().getOrMake(name);
        group.getIoLock().lock();
        try {
            ConfigurationNode object = readFile(StorageLocation.GROUP, name);

            if (object != null) {
                Set<Node> nodes = readNodes(object).stream().map(NodeModel::toNode).collect(Collectors.toSet());
                group.setEnduringNodes(nodes);
            } else {
                ConfigurationNode data = SimpleConfigurationNode.root();
                if (this instanceof SeparatedConfigurateDao) {
                    data.getNode("name").setValue(group.getName());
                }

                Set<NodeModel> nodes = group.getEnduringNodes().values().stream().map(NodeModel::fromNode).collect(Collectors.toCollection(LinkedHashSet::new));
                writeNodes(data, nodes);

                saveFile(StorageLocation.GROUP, name, data);
            }
        } catch (Exception e) {
            throw reportException(name, e);
        } finally {
            group.getIoLock().unlock();
        }
        group.getRefreshBuffer().requestDirectly();
        return group;
    }

    @Override
    public Optional<Group> loadGroup(String name) {
        Group group = this.plugin.getGroupManager().getIfLoaded(name);
        if (group != null) {
            group.getIoLock().lock();
        }

        try {
            ConfigurationNode object = readFile(StorageLocation.GROUP, name);

            if (object == null) {
                return Optional.empty();
            }

            if (group == null) {
                group = this.plugin.getGroupManager().getOrMake(name);
                group.getIoLock().lock();
            }

            Set<NodeModel> data = readNodes(object);
            Set<Node> nodes = data.stream().map(NodeModel::toNode).collect(Collectors.toSet());
            group.setEnduringNodes(nodes);

        } catch (Exception e) {
            throw reportException(name, e);
        } finally {
            if (group != null) {
                group.getIoLock().unlock();
            }
        }
        group.getRefreshBuffer().requestDirectly();
        return Optional.of(group);
    }

    @Override
    public void saveGroup(Group group) {
        group.getIoLock().lock();
        try {
            ConfigurationNode data = SimpleConfigurationNode.root();
            if (this instanceof SeparatedConfigurateDao) {
                data.getNode("name").setValue(group.getName());
            }

            Set<NodeModel> nodes = group.getEnduringNodes().values().stream().map(NodeModel::fromNode).collect(Collectors.toCollection(LinkedHashSet::new));
            writeNodes(data, nodes);

            saveFile(StorageLocation.GROUP, group.getName(), data);
        } catch (Exception e) {
            throw reportException(group.getName(), e);
        } finally {
            group.getIoLock().unlock();
        }
    }

    @Override
    public void deleteGroup(Group group) {
        group.getIoLock().lock();
        try {
            saveFile(StorageLocation.GROUP, group.getName(), null);
        } catch (Exception e) {
            throw reportException(group.getName(), e);
        } finally {
            group.getIoLock().unlock();
        }
        this.plugin.getGroupManager().unload(group);
    }

    @Override
    public Track createAndLoadTrack(String name) {
        Track track = this.plugin.getTrackManager().getOrMake(name);
        track.getIoLock().lock();
        try {
            ConfigurationNode object = readFile(StorageLocation.TRACK, name);

            if (object != null) {
                List<String> groups = object.getNode("groups").getChildrenList().stream()
                        .map(ConfigurationNode::getString)
                        .collect(ImmutableCollectors.toList());

                track.setGroups(groups);
            } else {
                ConfigurationNode data = SimpleConfigurationNode.root();
                if (this instanceof SeparatedConfigurateDao) {
                    data.getNode("name").setValue(name);
                }
                data.getNode("groups").setValue(track.getGroups());
                saveFile(StorageLocation.TRACK, name, data);
            }

        } catch (Exception e) {
            throw reportException(name, e);
        } finally {
            track.getIoLock().unlock();
        }
        return track;
    }

    @Override
    public Optional<Track> loadTrack(String name) {
        Track track = this.plugin.getTrackManager().getIfLoaded(name);
        if (track != null) {
            track.getIoLock().lock();
        }

        try {
            ConfigurationNode object = readFile(StorageLocation.TRACK, name);

            if (object == null) {
                return Optional.empty();
            }

            if (track == null) {
                track = this.plugin.getTrackManager().getOrMake(name);
                track.getIoLock().lock();
            }

            List<String> groups = object.getNode("groups").getChildrenList().stream()
                    .map(ConfigurationNode::getString)
                    .collect(ImmutableCollectors.toList());

            track.setGroups(groups);

        } catch (Exception e) {
            throw reportException(name, e);
        } finally {
            if (track != null) {
                track.getIoLock().unlock();
            }
        }
        return Optional.of(track);
    }

    @Override
    public void saveTrack(Track track) {
        track.getIoLock().lock();
        try {
            ConfigurationNode data = SimpleConfigurationNode.root();
            if (this instanceof SeparatedConfigurateDao) {
                data.getNode("name").setValue(track.getName());
            }
            data.getNode("groups").setValue(track.getGroups());
            saveFile(StorageLocation.TRACK, track.getName(), data);
        } catch (Exception e) {
            throw reportException(track.getName(), e);
        } finally {
            track.getIoLock().unlock();
        }
    }

    @Override
    public void deleteTrack(Track track) {
        track.getIoLock().lock();
        try {
            saveFile(StorageLocation.TRACK, track.getName(), null);
        } catch (Exception e) {
            throw reportException(track.getName(), e);
        } finally {
            track.getIoLock().unlock();
        }
        this.plugin.getTrackManager().unload(track);
    }

    @Override
    public PlayerSaveResult savePlayerData(UUID uuid, String username) {
        return this.uuidCache.addMapping(uuid, username);
    }

    @Override
    public UUID getPlayerUuid(String username) {
        return this.uuidCache.lookupUuid(username);
    }

    @Override
    public String getPlayerName(UUID uuid) {
        return this.uuidCache.lookupUsername(uuid);
    }

    private static NodeModel readAttributes(ConfigurationNode attributes, Function<ConfigurationNode, String> permissionFunction) {
        boolean value = attributes.getNode("value").getBoolean(true);
        String server = attributes.getNode("server").getString("global");
        String world = attributes.getNode("world").getString("global");
        long expiry = attributes.getNode("expiry").getLong(0L);

        ImmutableContextSet context = ImmutableContextSet.empty();
        ConfigurationNode contextMap = attributes.getNode("context");
        if (!contextMap.isVirtual() && contextMap.hasMapChildren()) {
            context = ContextSetConfigurateSerializer.deserializeContextSet(contextMap).makeImmutable();
        }

        return NodeModel.of(permissionFunction.apply(attributes), value, server, world, expiry, context);
    }

    private static Collection<NodeModel> readAttributes(ConfigurationNode attributes, String permission) {
        boolean value = attributes.getNode("value").getBoolean(true);
        String server = attributes.getNode("server").getString("global");
        String world = attributes.getNode("world").getString("global");
        long expiry = attributes.getNode("expiry").getLong(0L);

        ImmutableContextSet context = ImmutableContextSet.empty();
        ConfigurationNode contextMap = attributes.getNode("context");
        if (!contextMap.isVirtual() && contextMap.hasMapChildren()) {
            context = ContextSetConfigurateSerializer.deserializeContextSet(contextMap).makeImmutable();
        }

        ConfigurationNode batchAttribute = attributes.getNode("permissions");
        if (permission.startsWith("luckperms.batch") && !batchAttribute.isVirtual() && batchAttribute.hasListChildren()) {
            List<NodeModel> nodes = new ArrayList<>();
            for (ConfigurationNode element : batchAttribute.getChildrenList()) {
                nodes.add(NodeModel.of(element.getString(), value, server, world, expiry, context));
            }
            return nodes;
        } else {
            return Collections.singleton(NodeModel.of(permission, value, server, world, expiry, context));
        }
    }

    private static Map.Entry<String, ConfigurationNode> parseEntry(ConfigurationNode appended) {
        if (!appended.hasMapChildren()) {
            return null;
        }
        Map.Entry<Object, ? extends ConfigurationNode> entry = Iterables.getFirst(appended.getChildrenMap().entrySet(), null);
        if (entry == null || !entry.getValue().hasMapChildren()) {
            return null;
        }

        return Maps.immutableEntry(entry.getKey().toString(), entry.getValue());
    }

    protected static Set<NodeModel> readNodes(ConfigurationNode data) {
        Set<NodeModel> nodes = new HashSet<>();

        if (data.getNode("permissions").hasListChildren()) {
            List<? extends ConfigurationNode> children = data.getNode("permissions").getChildrenList();
            for (ConfigurationNode appended : children) {
                String plainValue = appended.getValue(Types::strictAsString);
                if (plainValue != null) {
                    nodes.add(NodeModel.of(plainValue));
                    continue;
                }

                Map.Entry<String, ConfigurationNode> entry = parseEntry(appended);
                if (entry == null) {
                    continue;
                }
                nodes.addAll(readAttributes(entry.getValue(), entry.getKey()));
            }
        }

        if (data.getNode("parents").hasListChildren()) {
            List<? extends ConfigurationNode> children = data.getNode("parents").getChildrenList();
            for (ConfigurationNode appended : children) {
                String stringValue = appended.getValue(Types::strictAsString);
                if (stringValue != null) {
                    nodes.add(NodeModel.of(NodeFactory.groupNode(stringValue)));
                    continue;
                }

                Map.Entry<String, ConfigurationNode> entry = parseEntry(appended);
                if (entry == null) {
                    continue;
                }
                nodes.add(readAttributes(entry.getValue(), c -> NodeFactory.groupNode(entry.getKey())));
            }
        }

        for (ChatMetaType chatMetaType : ChatMetaType.values()) {
            String keyName = chatMetaType.toString() + "es";
            if (data.getNode(keyName).hasListChildren()) {
                List<? extends ConfigurationNode> children = data.getNode(keyName).getChildrenList();
                for (ConfigurationNode appended : children) {
                    Map.Entry<String, ConfigurationNode> entry = parseEntry(appended);
                    if (entry == null) {
                        continue;
                    }
                    nodes.add(readAttributes(entry.getValue(), c -> NodeFactory.chatMetaNode(chatMetaType, c.getNode("priority").getInt(0), entry.getKey())));
                }
            }
        }

        if (data.getNode("meta").hasListChildren()) {
            List<? extends ConfigurationNode> children = data.getNode("meta").getChildrenList();
            for (ConfigurationNode appended : children) {
                Map.Entry<String, ConfigurationNode> entry = parseEntry(appended);
                if (entry == null) {
                    continue;
                }
                nodes.add(readAttributes(entry.getValue(), c -> NodeFactory.metaNode(entry.getKey(), entry.getValue().getNode("value").getString("null"))));
            }
        }

        return nodes;
    }

    private static void writeAttributesTo(ConfigurationNode attributes, NodeModel node, boolean writeValue) {
        if (writeValue) {
            attributes.getNode("value").setValue(node.getValue());
        }

        if (!node.getServer().equals("global")) {
            attributes.getNode("server").setValue(node.getServer());
        }

        if (!node.getWorld().equals("global")) {
            attributes.getNode("world").setValue(node.getWorld());
        }

        if (node.getExpiry() != 0L) {
            attributes.getNode("expiry").setValue(node.getExpiry());
        }

        if (!node.getContexts().isEmpty()) {
            attributes.getNode("context").setValue(ContextSetConfigurateSerializer.serializeContextSet(node.getContexts()));
        }
    }

    private static boolean isPlain(NodeModel node) {
        return node.getValue() &&
                node.getServer().equalsIgnoreCase("global") &&
                node.getWorld().equalsIgnoreCase("global") &&
                node.getExpiry() == 0L &&
                node.getContexts().isEmpty();
    }

    private static void writeNodes(ConfigurationNode to, Set<NodeModel> nodes) {
        ConfigurationNode permissionsSection = SimpleConfigurationNode.root();
        ConfigurationNode parentsSection = SimpleConfigurationNode.root();
        ConfigurationNode prefixesSection = SimpleConfigurationNode.root();
        ConfigurationNode suffixesSection = SimpleConfigurationNode.root();
        ConfigurationNode metaSection = SimpleConfigurationNode.root();

        for (NodeModel node : nodes) {
            Node n = node.toNode();

            // just add a string to the list.
            if (isPlain(node)) {
                if (n.isGroupNode()) {
                    parentsSection.getAppendedNode().setValue(n.getGroupName());
                    continue;
                }
                if (!MetaType.ANY.matches(n)) {
                    permissionsSection.getAppendedNode().setValue(node.getPermission());
                    continue;
                }
            }

            ChatMetaType chatMetaType = ChatMetaType.ofNode(n).orElse(null);
            if (chatMetaType != null && n.getValuePrimitive()) {
                // handle prefixes / suffixes
                Map.Entry<Integer, String> entry = chatMetaType.getEntry(n);

                ConfigurationNode attributes = SimpleConfigurationNode.root();
                attributes.getNode("priority").setValue(entry.getKey());
                writeAttributesTo(attributes, node, false);

                ConfigurationNode appended = SimpleConfigurationNode.root();
                appended.getNode(entry.getValue()).setValue(attributes);

                switch (chatMetaType) {
                    case PREFIX:
                        prefixesSection.getAppendedNode().setValue(appended);
                        break;
                    case SUFFIX:
                        suffixesSection.getAppendedNode().setValue(appended);
                        break;
                    default:
                        throw new AssertionError();
                }
            } else if (n.isMeta() && n.getValuePrimitive()) {
                // handle meta nodes
                Map.Entry<String, String> meta = n.getMeta();

                ConfigurationNode attributes = SimpleConfigurationNode.root();
                attributes.getNode("value").setValue(meta.getValue());
                writeAttributesTo(attributes, node, false);

                ConfigurationNode appended = SimpleConfigurationNode.root();
                appended.getNode(meta.getKey()).setValue(attributes);

                metaSection.getAppendedNode().setValue(appended);
            } else if (n.isGroupNode() && n.getValuePrimitive()) {
                // handle group nodes
                ConfigurationNode attributes = SimpleConfigurationNode.root();
                writeAttributesTo(attributes, node, false);

                ConfigurationNode appended = SimpleConfigurationNode.root();
                appended.getNode(n.getGroupName()).setValue(attributes);

                parentsSection.getAppendedNode().setValue(appended);
            } else {
                // handle regular permissions and negated meta+prefixes+suffixes
                ConfigurationNode attributes = SimpleConfigurationNode.root();
                writeAttributesTo(attributes, node, true);

                ConfigurationNode appended = SimpleConfigurationNode.root();
                appended.getNode(n.getPermission()).setValue(attributes);

                permissionsSection.getAppendedNode().setValue(appended);
            }
        }

        if (permissionsSection.hasListChildren()) {
            to.getNode("permissions").setValue(permissionsSection);
        }
        if (parentsSection.hasListChildren()) {
            to.getNode("parents").setValue(parentsSection);
        }
        if (prefixesSection.hasListChildren()) {
            to.getNode("prefixes").setValue(prefixesSection);
        }
        if (suffixesSection.hasListChildren()) {
            to.getNode("suffixes").setValue(suffixesSection);
        }
        if (metaSection.hasListChildren()) {
            to.getNode("meta").setValue(metaSection);
        }
    }
}
