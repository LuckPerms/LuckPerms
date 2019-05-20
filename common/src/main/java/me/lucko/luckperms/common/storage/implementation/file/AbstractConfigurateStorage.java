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

package me.lucko.luckperms.common.storage.implementation.file;

import com.google.common.base.Throwables;
import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;

import me.lucko.luckperms.api.actionlog.Action;
import me.lucko.luckperms.api.context.ImmutableContextSet;
import me.lucko.luckperms.api.model.PlayerSaveResult;
import me.lucko.luckperms.api.node.ChatMetaType;
import me.lucko.luckperms.api.node.Node;
import me.lucko.luckperms.api.node.NodeType;
import me.lucko.luckperms.api.node.types.ChatMetaNode;
import me.lucko.luckperms.api.node.types.InheritanceNode;
import me.lucko.luckperms.api.node.types.MetaNode;
import me.lucko.luckperms.common.actionlog.Log;
import me.lucko.luckperms.common.bulkupdate.BulkUpdate;
import me.lucko.luckperms.common.context.ContextSetConfigurateSerializer;
import me.lucko.luckperms.common.model.Group;
import me.lucko.luckperms.common.model.NodeMapType;
import me.lucko.luckperms.common.model.Track;
import me.lucko.luckperms.common.model.User;
import me.lucko.luckperms.common.model.UserIdentifier;
import me.lucko.luckperms.common.node.factory.NodeFactory;
import me.lucko.luckperms.common.node.model.NodeDataContainer;
import me.lucko.luckperms.common.plugin.LuckPermsPlugin;
import me.lucko.luckperms.common.storage.implementation.StorageImplementation;
import me.lucko.luckperms.common.storage.implementation.file.loader.ConfigurateLoader;
import me.lucko.luckperms.common.storage.implementation.file.loader.JsonLoader;
import me.lucko.luckperms.common.storage.implementation.file.loader.YamlLoader;
import me.lucko.luckperms.common.util.ImmutableCollectors;
import me.lucko.luckperms.common.util.MoreFiles;

import ninja.leaping.configurate.ConfigurationNode;
import ninja.leaping.configurate.SimpleConfigurationNode;
import ninja.leaping.configurate.Types;

import java.io.IOException;
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
public abstract class AbstractConfigurateStorage implements StorageImplementation {

    protected final LuckPermsPlugin plugin;
    private final String implementationName;

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

    protected AbstractConfigurateStorage(LuckPermsPlugin plugin, String implementationName, ConfigurateLoader loader, String dataDirectoryName) {
        this.plugin = plugin;
        this.implementationName = implementationName;
        this.loader = loader;
        this.dataDirectoryName = dataDirectoryName;
        this.actionLogger = new FileActionLogger(plugin);
    }

    @Override
    public LuckPermsPlugin getPlugin() {
        return this.plugin;
    }

    @Override
    public String getImplementationName() {
        return this.implementationName;
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
        MoreFiles.createDirectoriesIfNotExists(this.dataDirectory);

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
    public void logAction(Action entry) {
        this.actionLogger.logAction(entry);
    }

    @Override
    public Log getLog() throws IOException {
        return this.actionLogger.getLog();
    }

    protected ConfigurationNode processBulkUpdate(BulkUpdate bulkUpdate, ConfigurationNode node) {
        Set<NodeDataContainer> nodes = readNodes(node);
        Set<NodeDataContainer> results = nodes.stream()
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

                Set<Node> nodes = readNodes(object).stream().map(NodeDataContainer::toNode).collect(Collectors.toSet());
                user.setNodes(NodeMapType.ENDURING, nodes);
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
                    user.clearEnduringNodes();
                    user.getPrimaryGroup().setStoredValue(null);
                    this.plugin.getUserManager().giveDefaultIfNeeded(user, false);
                }
            }
        } catch (Exception e) {
            throw reportException(uuid.toString(), e);
        } finally {
            user.getIoLock().unlock();
        }
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
                if (this instanceof SeparatedConfigurateStorage) {
                    data.getNode("uuid").setValue(user.getUuid().toString());
                }
                data.getNode("name").setValue(user.getName().orElse("null"));
                data.getNode(this.loader instanceof JsonLoader ? "primaryGroup" : "primary-group").setValue(user.getPrimaryGroup().getStoredValue().orElse(NodeFactory.DEFAULT_GROUP_NAME));

                Set<NodeDataContainer> nodes = user.enduringData().immutable().values().stream().map(NodeDataContainer::fromNode).collect(Collectors.toCollection(LinkedHashSet::new));
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
                Set<Node> nodes = readNodes(object).stream().map(NodeDataContainer::toNode).collect(Collectors.toSet());
                group.setNodes(NodeMapType.ENDURING, nodes);
            } else {
                ConfigurationNode data = SimpleConfigurationNode.root();
                if (this instanceof SeparatedConfigurateStorage) {
                    data.getNode("name").setValue(group.getName());
                }

                Set<NodeDataContainer> nodes = group.enduringData().immutable().values().stream().map(NodeDataContainer::fromNode).collect(Collectors.toCollection(LinkedHashSet::new));
                writeNodes(data, nodes);

                saveFile(StorageLocation.GROUP, name, data);
            }
        } catch (Exception e) {
            throw reportException(name, e);
        } finally {
            group.getIoLock().unlock();
        }
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

            Set<NodeDataContainer> data = readNodes(object);
            Set<Node> nodes = data.stream().map(NodeDataContainer::toNode).collect(Collectors.toSet());
            group.setNodes(NodeMapType.ENDURING, nodes);

        } catch (Exception e) {
            throw reportException(name, e);
        } finally {
            if (group != null) {
                group.getIoLock().unlock();
            }
        }
        return Optional.of(group);
    }

    @Override
    public void saveGroup(Group group) {
        group.getIoLock().lock();
        try {
            ConfigurationNode data = SimpleConfigurationNode.root();
            if (this instanceof SeparatedConfigurateStorage) {
                data.getNode("name").setValue(group.getName());
            }

            Set<NodeDataContainer> nodes = group.enduringData().immutable().values().stream().map(NodeDataContainer::fromNode).collect(Collectors.toCollection(LinkedHashSet::new));
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
                if (this instanceof SeparatedConfigurateStorage) {
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
            if (this instanceof SeparatedConfigurateStorage) {
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

    private static NodeDataContainer readMetaAttributes(ConfigurationNode attributes, Function<ConfigurationNode, String> permissionFunction) {
        String server = attributes.getNode("server").getString("global");
        String world = attributes.getNode("world").getString("global");
        long expiry = attributes.getNode("expiry").getLong(0L);

        ImmutableContextSet context = ImmutableContextSet.empty();
        ConfigurationNode contextMap = attributes.getNode("context");
        if (!contextMap.isVirtual() && contextMap.hasMapChildren()) {
            context = ContextSetConfigurateSerializer.deserializeContextSet(contextMap).immutableCopy();
        }

        return NodeDataContainer.of(permissionFunction.apply(attributes), true, server, world, expiry, context);
    }

    private static Collection<NodeDataContainer> readAttributes(ConfigurationNode attributes, String permission) {
        boolean value = attributes.getNode("value").getBoolean(true);
        String server = attributes.getNode("server").getString("global");
        String world = attributes.getNode("world").getString("global");
        long expiry = attributes.getNode("expiry").getLong(0L);

        ImmutableContextSet context = ImmutableContextSet.empty();
        ConfigurationNode contextMap = attributes.getNode("context");
        if (!contextMap.isVirtual() && contextMap.hasMapChildren()) {
            context = ContextSetConfigurateSerializer.deserializeContextSet(contextMap).immutableCopy();
        }

        ConfigurationNode batchAttribute = attributes.getNode("permissions");
        if (permission.startsWith("luckperms.batch") && !batchAttribute.isVirtual() && batchAttribute.hasListChildren()) {
            List<NodeDataContainer> nodes = new ArrayList<>();
            for (ConfigurationNode element : batchAttribute.getChildrenList()) {
                nodes.add(NodeDataContainer.of(element.getString(), value, server, world, expiry, context));
            }
            return nodes;
        } else {
            return Collections.singleton(NodeDataContainer.of(permission, value, server, world, expiry, context));
        }
    }

    private static Map.Entry<String, ConfigurationNode> parseEntry(ConfigurationNode appended, String keyFieldName) {
        if (!appended.hasMapChildren()) {
            return null;
        }

        Map<Object, ? extends ConfigurationNode> children = appended.getChildrenMap();
        if (children.isEmpty()) {
            return null;
        }

        // if children.size == 1 and the only entry doesn't have a key called "permission" - assume
        // the key refers to the name of the permission
        if (children.size() == 1) {
            Map.Entry<Object, ? extends ConfigurationNode> entry = Iterables.getFirst(children.entrySet(), null);
            if (entry != null) {
                String permission = entry.getKey().toString();
                ConfigurationNode attributes = entry.getValue();

                if (!permission.equals(keyFieldName)) {
                    return Maps.immutableEntry(permission, attributes);
                }
            }
        }

        // assume 'appended' is the actual entry.
        String permission = children.get(keyFieldName).getString(null);
        if (permission == null) {
            return null;
        }

        return Maps.immutableEntry(permission, appended);
    }

    protected static Set<NodeDataContainer> readNodes(ConfigurationNode data) {
        Set<NodeDataContainer> nodes = new HashSet<>();

        if (data.getNode("permissions").hasListChildren()) {
            List<? extends ConfigurationNode> children = data.getNode("permissions").getChildrenList();
            for (ConfigurationNode appended : children) {
                String plainValue = appended.getValue(Types::strictAsString);
                if (plainValue != null) {
                    nodes.add(NodeDataContainer.of(plainValue));
                    continue;
                }

                Map.Entry<String, ConfigurationNode> entry = parseEntry(appended, "permission");
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
                    nodes.add(NodeDataContainer.of(NodeFactory.groupNode(stringValue)));
                    continue;
                }

                Map.Entry<String, ConfigurationNode> entry = parseEntry(appended, "group");
                if (entry == null) {
                    continue;
                }
                nodes.add(readMetaAttributes(entry.getValue(), c -> NodeFactory.groupNode(entry.getKey())));
            }
        }

        for (ChatMetaType chatMetaType : ChatMetaType.values()) {
            String keyName = chatMetaType.toString() + "es";
            if (data.getNode(keyName).hasListChildren()) {
                List<? extends ConfigurationNode> children = data.getNode(keyName).getChildrenList();
                for (ConfigurationNode appended : children) {
                    Map.Entry<String, ConfigurationNode> entry = parseEntry(appended, chatMetaType.toString());
                    if (entry == null) {
                        continue;
                    }
                    nodes.add(readMetaAttributes(entry.getValue(), c -> NodeFactory.chatMetaNode(chatMetaType, c.getNode("priority").getInt(0), entry.getKey())));
                }
            }
        }

        if (data.getNode("meta").hasListChildren()) {
            List<? extends ConfigurationNode> children = data.getNode("meta").getChildrenList();
            for (ConfigurationNode appended : children) {
                Map.Entry<String, ConfigurationNode> entry = parseEntry(appended, "key");
                if (entry == null) {
                    continue;
                }
                nodes.add(readMetaAttributes(entry.getValue(), c -> NodeFactory.metaNode(entry.getKey(), c.getNode("value").getString("null"))));
            }
        }

        return nodes;
    }

    private static void writeAttributesTo(ConfigurationNode attributes, NodeDataContainer node, boolean writeValue) {
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

    private static boolean isPlain(NodeDataContainer node) {
        return node.getValue() &&
                node.getServer().equalsIgnoreCase("global") &&
                node.getWorld().equalsIgnoreCase("global") &&
                node.getExpiry() == 0L &&
                node.getContexts().isEmpty();
    }

    private void appendNode(ConfigurationNode base, String key, ConfigurationNode attributes, String keyFieldName) {
        if (this.loader instanceof YamlLoader) {
            // create a map node with a single entry of key --> attributes
            ConfigurationNode appended = base.getAppendedNode();
            appended.getNode(key).setValue(attributes);
        } else {
            // include the attributes and key in the same map
            ConfigurationNode appended = base.getAppendedNode();
            appended.getNode(keyFieldName).setValue(key);
            appended.mergeValuesFrom(attributes);
        }
    }

    private void writeNodes(ConfigurationNode to, Set<NodeDataContainer> nodes) {
        ConfigurationNode permissionsSection = SimpleConfigurationNode.root();
        ConfigurationNode parentsSection = SimpleConfigurationNode.root();
        ConfigurationNode prefixesSection = SimpleConfigurationNode.root();
        ConfigurationNode suffixesSection = SimpleConfigurationNode.root();
        ConfigurationNode metaSection = SimpleConfigurationNode.root();

        for (NodeDataContainer node : nodes) {
            Node n = node.toNode();

            // just add a string to the list.
            if (this.loader instanceof YamlLoader && isPlain(node)) {
                if (n instanceof InheritanceNode) {
                    parentsSection.getAppendedNode().setValue(((InheritanceNode) n).getGroupName());
                    continue;
                }
                if (!NodeType.META_OR_CHAT_META.matches(n)) {
                    permissionsSection.getAppendedNode().setValue(node.getPermission());
                    continue;
                }
            }

            if (n instanceof ChatMetaNode<?, ?> && n.getValue()) {
                // handle prefixes / suffixes
                ChatMetaNode<?, ?> chatMeta = (ChatMetaNode<?, ?>) n;

                ConfigurationNode attributes = SimpleConfigurationNode.root();
                attributes.getNode("priority").setValue(chatMeta.getPriority());
                writeAttributesTo(attributes, node, false);

                switch (chatMeta.getType()) {
                    case PREFIX:
                        appendNode(prefixesSection, chatMeta.getMetaValue(), attributes, "prefix");
                        break;
                    case SUFFIX:
                        appendNode(suffixesSection, chatMeta.getMetaValue(), attributes, "suffix");
                        break;
                    default:
                        throw new AssertionError();
                }
            } else if (n instanceof MetaNode && n.getValue()) {
                // handle meta nodes
                MetaNode meta = (MetaNode) n;

                ConfigurationNode attributes = SimpleConfigurationNode.root();
                attributes.getNode("value").setValue(meta.getMetaValue());
                writeAttributesTo(attributes, node, false);

                appendNode(metaSection, meta.getMetaKey(), attributes, "key");
            } else if (n instanceof InheritanceNode && n.getValue()) {
                // handle group nodes
                InheritanceNode inheritance = (InheritanceNode) n;

                ConfigurationNode attributes = SimpleConfigurationNode.root();
                writeAttributesTo(attributes, node, false);

                appendNode(parentsSection, inheritance.getGroupName(), attributes, "group");
            } else {
                // handle regular permissions and negated meta+prefixes+suffixes
                ConfigurationNode attributes = SimpleConfigurationNode.root();
                writeAttributesTo(attributes, node, true);

                appendNode(permissionsSection, n.getKey(), attributes, "permission");
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
