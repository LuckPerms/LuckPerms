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

import me.lucko.luckperms.common.actionlog.Log;
import me.lucko.luckperms.common.bulkupdate.BulkUpdate;
import me.lucko.luckperms.common.context.ContextSetConfigurateSerializer;
import me.lucko.luckperms.common.context.contextset.ImmutableContextSetImpl;
import me.lucko.luckperms.common.model.Group;
import me.lucko.luckperms.common.model.Track;
import me.lucko.luckperms.common.model.User;
import me.lucko.luckperms.common.model.manager.group.GroupManager;
import me.lucko.luckperms.common.node.factory.NodeBuilders;
import me.lucko.luckperms.common.node.types.Inheritance;
import me.lucko.luckperms.common.node.types.Meta;
import me.lucko.luckperms.common.node.types.Prefix;
import me.lucko.luckperms.common.node.types.Suffix;
import me.lucko.luckperms.common.plugin.LuckPermsPlugin;
import me.lucko.luckperms.common.storage.implementation.StorageImplementation;
import me.lucko.luckperms.common.storage.implementation.file.loader.ConfigurateLoader;
import me.lucko.luckperms.common.storage.implementation.file.loader.JsonLoader;
import me.lucko.luckperms.common.storage.implementation.file.loader.YamlLoader;
import me.lucko.luckperms.common.util.ImmutableCollectors;
import me.lucko.luckperms.common.util.MoreFiles;

import net.luckperms.api.actionlog.Action;
import net.luckperms.api.context.DefaultContextKeys;
import net.luckperms.api.context.ImmutableContextSet;
import net.luckperms.api.model.PlayerSaveResult;
import net.luckperms.api.model.data.DataType;
import net.luckperms.api.node.Node;
import net.luckperms.api.node.NodeBuilder;
import net.luckperms.api.node.NodeType;
import net.luckperms.api.node.types.ChatMetaNode;
import net.luckperms.api.node.types.InheritanceNode;
import net.luckperms.api.node.types.MetaNode;

import ninja.leaping.configurate.ConfigurationNode;
import ninja.leaping.configurate.SimpleConfigurationNode;
import ninja.leaping.configurate.Types;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
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

        this.actionLogger.init(this.dataDirectory.resolve("actions.txt"), this.dataDirectory.resolve("actions.json"));
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
        Set<Node> nodes = readNodes(node);
        Set<Node> results = nodes.stream()
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
    public User loadUser(UUID uniqueId, String username) {
        User user = this.plugin.getUserManager().getOrMake(uniqueId, username);
        user.getIoLock().lock();
        try {
            ConfigurationNode object = readFile(StorageLocation.USER, uniqueId.toString());
            if (object != null) {
                String name = object.getNode("name").getString();
                user.getPrimaryGroup().setStoredValue(object.getNode(this.loader instanceof JsonLoader ? "primaryGroup" : "primary-group").getString());

                user.setNodes(DataType.NORMAL, readNodes(object));
                user.setUsername(name, true);

                boolean save = this.plugin.getUserManager().giveDefaultIfNeeded(user, false);
                if (user.getUsername().isPresent() && (name == null || !user.getUsername().get().equalsIgnoreCase(name))) {
                    save = true;
                }

                if (save | user.auditTemporaryNodes()) {
                    saveUser(user);
                }
            } else {
                if (this.plugin.getUserManager().shouldSave(user)) {
                    user.clearNodes(DataType.NORMAL, null, true);
                    user.getPrimaryGroup().setStoredValue(null);
                    this.plugin.getUserManager().giveDefaultIfNeeded(user, false);
                }
            }
        } catch (Exception e) {
            throw reportException(uniqueId.toString(), e);
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
                saveFile(StorageLocation.USER, user.getUniqueId().toString(), null);
            } else {
                ConfigurationNode data = SimpleConfigurationNode.root();
                if (this instanceof SeparatedConfigurateStorage) {
                    data.getNode("uuid").setValue(user.getUniqueId().toString());
                }
                data.getNode("name").setValue(user.getUsername().orElse("null"));
                data.getNode(this.loader instanceof JsonLoader ? "primaryGroup" : "primary-group").setValue(user.getPrimaryGroup().getStoredValue().orElse(GroupManager.DEFAULT_GROUP_NAME));

                writeNodes(data, user.normalData().immutable().values());
                saveFile(StorageLocation.USER, user.getUniqueId().toString(), data);
            }
        } catch (Exception e) {
            throw reportException(user.getUniqueId().toString(), e);
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
                group.setNodes(DataType.NORMAL, readNodes(object));
            } else {
                ConfigurationNode data = SimpleConfigurationNode.root();
                if (this instanceof SeparatedConfigurateStorage) {
                    data.getNode("name").setValue(group.getName());
                }

                writeNodes(data, group.normalData().immutable().values());
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

            group.setNodes(DataType.NORMAL, readNodes(object));

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

            writeNodes(data, group.normalData().immutable().values());
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
        this.plugin.getGroupManager().unload(group.getName());
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
        this.plugin.getTrackManager().unload(track.getName());
    }

    @Override
    public PlayerSaveResult savePlayerData(UUID uniqueId, String username) {
        return this.uuidCache.addMapping(uniqueId, username);
    }

    @Override
    public UUID getPlayerUniqueId(String username) {
        return this.uuidCache.lookupUuid(username);
    }

    @Override
    public String getPlayerName(UUID uniqueId) {
        return this.uuidCache.lookupUsername(uniqueId);
    }

    private static ImmutableContextSet readContexts(ConfigurationNode attributes) {
        ImmutableContextSet.Builder contextBuilder = new ImmutableContextSetImpl.BuilderImpl();
        ConfigurationNode contextMap = attributes.getNode("context");
        if (!contextMap.isVirtual() && contextMap.hasMapChildren()) {
            contextBuilder.addAll(ContextSetConfigurateSerializer.deserializeContextSet(contextMap));
        }

        String server = attributes.getNode("server").getString("global");
        if (!server.equals("global")) {
            contextBuilder.add(DefaultContextKeys.SERVER_KEY, server);
        }

        String world = attributes.getNode("world").getString("global");
        if (!world.equals("global")) {
            contextBuilder.add(DefaultContextKeys.WORLD_KEY, world);
        }

        return contextBuilder.build();
    }

    private static Node readMetaAttributes(ConfigurationNode attributes, Function<ConfigurationNode, NodeBuilder<?, ?>> permissionFunction) {
        boolean value = attributes.getNode("value").getBoolean(true);
        long expiryVal = attributes.getNode("expiry").getLong(0L);
        Instant expiry = expiryVal == 0L ? null : Instant.ofEpochSecond(expiryVal);
        ImmutableContextSet context = readContexts(attributes);

        return permissionFunction.apply(attributes)
                .value(value)
                .expiry(expiry)
                .context(context)
                .build();
    }

    private static Collection<Node> readAttributes(ConfigurationNode attributes, String permission) {
        boolean value = attributes.getNode("value").getBoolean(true);
        long expiryVal = attributes.getNode("expiry").getLong(0L);
        Instant expiry = expiryVal == 0L ? null : Instant.ofEpochSecond(expiryVal);
        ImmutableContextSet context = readContexts(attributes);

        ConfigurationNode batchAttribute = attributes.getNode("permissions");
        if (permission.startsWith("luckperms.batch") && !batchAttribute.isVirtual() && batchAttribute.hasListChildren()) {
            List<Node> nodes = new ArrayList<>();
            for (ConfigurationNode element : batchAttribute.getChildrenList()) {
                Node node = NodeBuilders.determineMostApplicable(element.getString())
                        .value(value)
                        .expiry(expiry)
                        .context(context)
                        .build();
                nodes.add(node);
            }
            return nodes;
        } else {
            Node node = NodeBuilders.determineMostApplicable(permission)
                    .value(value)
                    .expiry(expiry)
                    .context(context)
                    .build();
            return Collections.singleton(node);
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

    protected static Set<Node> readNodes(ConfigurationNode data) {
        Set<Node> nodes = new HashSet<>();

        if (data.getNode("permissions").hasListChildren()) {
            List<? extends ConfigurationNode> children = data.getNode("permissions").getChildrenList();
            for (ConfigurationNode appended : children) {
                String plainValue = appended.getValue(Types::strictAsString);
                if (plainValue != null) {
                    nodes.add(NodeBuilders.determineMostApplicable(plainValue).build());
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
                String plainValue = appended.getValue(Types::strictAsString);
                if (plainValue != null) {
                    nodes.add(Inheritance.builder(plainValue).build());
                    continue;
                }

                Map.Entry<String, ConfigurationNode> entry = parseEntry(appended, "group");
                if (entry == null) {
                    continue;
                }
                nodes.add(readMetaAttributes(entry.getValue(), c -> Inheritance.builder(entry.getKey())));
            }
        }

        if (data.getNode("prefixes").hasListChildren()) {
            List<? extends ConfigurationNode> children = data.getNode("prefixes").getChildrenList();
            for (ConfigurationNode appended : children) {
                Map.Entry<String, ConfigurationNode> entry = parseEntry(appended, "prefix");
                if (entry == null) {
                    continue;
                }
                nodes.add(readMetaAttributes(entry.getValue(), c -> Prefix.builder(entry.getKey(), c.getNode("priority").getInt(0))));
            }
        }

        if (data.getNode("suffixes").hasListChildren()) {
            List<? extends ConfigurationNode> children = data.getNode("suffixes").getChildrenList();
            for (ConfigurationNode appended : children) {
                Map.Entry<String, ConfigurationNode> entry = parseEntry(appended, "suffix");
                if (entry == null) {
                    continue;
                }
                nodes.add(readMetaAttributes(entry.getValue(), c -> Suffix.builder(entry.getKey(), c.getNode("priority").getInt(0))));
            }
        }

        if (data.getNode("meta").hasListChildren()) {
            List<? extends ConfigurationNode> children = data.getNode("meta").getChildrenList();
            for (ConfigurationNode appended : children) {
                Map.Entry<String, ConfigurationNode> entry = parseEntry(appended, "key");
                if (entry == null) {
                    continue;
                }
                nodes.add(readMetaAttributes(entry.getValue(), c -> Meta.builder(entry.getKey(), c.getNode("value").getString("null"))));
            }
        }

        return nodes;
    }

    private static void writeAttributesTo(ConfigurationNode attributes, Node node, boolean writeValue) {
        if (writeValue) {
            attributes.getNode("value").setValue(node.getValue());
        }

        if (node.hasExpiry()) {
            attributes.getNode("expiry").setValue(node.getExpiry().getEpochSecond());
        }

        if (!node.getContexts().isEmpty()) {
            attributes.getNode("context").setValue(ContextSetConfigurateSerializer.serializeContextSet(node.getContexts()));
        }
    }

    private static boolean isPlain(Node node) {
        return node.getValue() && !node.hasExpiry() && node.getContexts().isEmpty();
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

    private void writeNodes(ConfigurationNode to, Collection<Node> nodes) {
        ConfigurationNode permissionsSection = SimpleConfigurationNode.root();
        ConfigurationNode parentsSection = SimpleConfigurationNode.root();
        ConfigurationNode prefixesSection = SimpleConfigurationNode.root();
        ConfigurationNode suffixesSection = SimpleConfigurationNode.root();
        ConfigurationNode metaSection = SimpleConfigurationNode.root();

        for (Node n : nodes) {
            // just add a string to the list.
            if (this.loader instanceof YamlLoader && isPlain(n)) {
                if (n instanceof InheritanceNode) {
                    parentsSection.getAppendedNode().setValue(((InheritanceNode) n).getGroupName());
                    continue;
                }
                if (!NodeType.META_OR_CHAT_META.matches(n)) {
                    permissionsSection.getAppendedNode().setValue(n.getKey());
                    continue;
                }
            }

            if (n instanceof ChatMetaNode<?, ?> && n.getValue()) {
                // handle prefixes / suffixes
                ChatMetaNode<?, ?> chatMeta = (ChatMetaNode<?, ?>) n;

                ConfigurationNode attributes = SimpleConfigurationNode.root();
                attributes.getNode("priority").setValue(chatMeta.getPriority());
                writeAttributesTo(attributes, n, false);

                switch (chatMeta.getMetaType()) {
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
                writeAttributesTo(attributes, n, false);

                appendNode(metaSection, meta.getMetaKey(), attributes, "key");
            } else if (n instanceof InheritanceNode && n.getValue()) {
                // handle group nodes
                InheritanceNode inheritance = (InheritanceNode) n;

                ConfigurationNode attributes = SimpleConfigurationNode.root();
                writeAttributesTo(attributes, n, false);

                appendNode(parentsSection, inheritance.getGroupName(), attributes, "group");
            } else {
                // handle regular permissions and negated meta+prefixes+suffixes
                ConfigurationNode attributes = SimpleConfigurationNode.root();
                writeAttributesTo(attributes, n, true);

                appendNode(permissionsSection, n.getKey(), attributes, "permission");
            }
        }

        if (permissionsSection.hasListChildren() || this instanceof CombinedConfigurateStorage) {
            // ensure for CombinedConfigurateStorage that there's at least *something* to save to the file
            // even if it's just an empty list.
            if (!permissionsSection.hasListChildren()) {
                permissionsSection.setValue(Collections.emptyList());
            }
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
