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

import com.google.common.collect.Iterables;

import me.lucko.luckperms.common.actionlog.Log;
import me.lucko.luckperms.common.bulkupdate.BulkUpdate;
import me.lucko.luckperms.common.context.ContextSetConfigurateSerializer;
import me.lucko.luckperms.common.context.contextset.ImmutableContextSetImpl;
import me.lucko.luckperms.common.model.Group;
import me.lucko.luckperms.common.model.HolderType;
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
import me.lucko.luckperms.common.util.MoreFiles;

import net.luckperms.api.actionlog.Action;
import net.luckperms.api.context.DefaultContextKeys;
import net.luckperms.api.context.ImmutableContextSet;
import net.luckperms.api.model.PlayerSaveResult;
import net.luckperms.api.node.Node;
import net.luckperms.api.node.NodeBuilder;
import net.luckperms.api.node.NodeType;
import net.luckperms.api.node.types.ChatMetaNode;
import net.luckperms.api.node.types.InheritanceNode;
import net.luckperms.api.node.types.MetaNode;

import ninja.leaping.configurate.ConfigurationNode;
import ninja.leaping.configurate.Types;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Abstract storage implementation using Configurate {@link ConfigurationNode}s to
 * serialize and deserialize data.
 */
public abstract class AbstractConfigurateStorage implements StorageImplementation {
    /** The plugin instance */
    protected final LuckPermsPlugin plugin;

    /** The name of this implementation */
    private final String implementationName;

    /** The Configurate loader used to read/write data */
    protected final ConfigurateLoader loader;

    /* The data directory */
    protected Path dataDirectory;
    private final String dataDirectoryName;

    /* The UUID cache */
    private final FileUuidCache uuidCache;
    private Path uuidCacheFile;

    /** The action logger */
    private final FileActionLogger actionLogger;

    protected AbstractConfigurateStorage(LuckPermsPlugin plugin, String implementationName, ConfigurateLoader loader, String dataDirectoryName) {
        this.plugin = plugin;
        this.implementationName = implementationName;
        this.loader = loader;
        this.dataDirectoryName = dataDirectoryName;

        this.uuidCache = new FileUuidCache();
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

    @Override
    public void init() throws IOException {
        // init the data directory and ensure it exists
        this.dataDirectory = this.plugin.getBootstrap().getDataDirectory().resolve(this.dataDirectoryName);
        MoreFiles.createDirectoriesIfNotExists(this.dataDirectory);

        // setup the uuid cache
        this.uuidCacheFile = MoreFiles.createFileIfNotExists(this.dataDirectory.resolve("uuidcache.txt"));
        this.uuidCache.load(this.uuidCacheFile);

        // setup the action logger
        this.actionLogger.init(this.dataDirectory.resolve("actions.txt"), this.dataDirectory.resolve("actions.json"));
    }

    @Override
    public void shutdown() {
        this.uuidCache.save(this.uuidCacheFile);
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

    @Override
    public User loadUser(UUID uniqueId, String username) throws IOException {
        User user = this.plugin.getUserManager().getOrMake(uniqueId, username);
        try {
            ConfigurationNode file = readFile(StorageLocation.USERS, uniqueId.toString());
            if (file != null) {
                String name = file.getNode("name").getString();
                String primaryGroup = file.getNode(this.loader instanceof JsonLoader ? "primaryGroup" : "primary-group").getString();

                user.getPrimaryGroup().setStoredValue(primaryGroup);
                user.setUsername(name, true);

                user.loadNodesFromStorage(readNodes(file));
                this.plugin.getUserManager().giveDefaultIfNeeded(user);

                boolean updatedUsername = user.getUsername().isPresent() && (name == null || !user.getUsername().get().equalsIgnoreCase(name));
                if (updatedUsername | user.auditTemporaryNodes()) {
                    saveUser(user);
                }
            } else {
                if (this.plugin.getUserManager().isNonDefaultUser(user)) {
                    user.loadNodesFromStorage(Collections.emptyList());
                    user.getPrimaryGroup().setStoredValue(null);
                    this.plugin.getUserManager().giveDefaultIfNeeded(user);
                }
            }
        } catch (Exception e) {
            throw new FileIOException(uniqueId.toString(), e);
        }
        return user;
    }

    @Override
    public void saveUser(User user) throws IOException {
        user.normalData().discardChanges();
        try {
            if (!this.plugin.getUserManager().isNonDefaultUser(user)) {
                saveFile(StorageLocation.USERS, user.getUniqueId().toString(), null);
            } else {
                ConfigurationNode file = ConfigurationNode.root();
                if (this instanceof SeparatedConfigurateStorage) {
                    file.getNode("uuid").setValue(user.getUniqueId().toString());
                }

                String name = user.getUsername().orElse("null");
                String primaryGroup = user.getPrimaryGroup().getStoredValue().orElse(GroupManager.DEFAULT_GROUP_NAME);

                file.getNode("name").setValue(name);
                file.getNode(this.loader instanceof JsonLoader ? "primaryGroup" : "primary-group").setValue(primaryGroup);

                writeNodes(file, user.normalData().asList());
                saveFile(StorageLocation.USERS, user.getUniqueId().toString(), file);
            }
        } catch (Exception e) {
            throw new FileIOException(user.getUniqueId().toString(), e);
        }
    }

    @Override
    public Group createAndLoadGroup(String name) throws IOException {
        Group group = this.plugin.getGroupManager().getOrMake(name);
        try {
            ConfigurationNode file = readFile(StorageLocation.GROUPS, name);

            if (file != null) {
                group.loadNodesFromStorage(readNodes(file));
            } else {
                file = ConfigurationNode.root();
                if (this instanceof SeparatedConfigurateStorage) {
                    file.getNode("name").setValue(group.getName());
                }

                writeNodes(file, group.normalData().asList());
                saveFile(StorageLocation.GROUPS, name, file);
            }
        } catch (Exception e) {
            throw new FileIOException(name, e);
        }
        return group;
    }

    @Override
    public Optional<Group> loadGroup(String name) throws IOException {
        try {
            ConfigurationNode file = readFile(StorageLocation.GROUPS, name);
            if (file == null) {
                return Optional.empty();
            }

            Group group = this.plugin.getGroupManager().getOrMake(name);
            group.loadNodesFromStorage(readNodes(file));
            return Optional.of(group);
        } catch (Exception e) {
            throw new FileIOException(name, e);
        }
    }

    @Override
    public void saveGroup(Group group) throws IOException {
        group.normalData().discardChanges();
        try {
            ConfigurationNode file = ConfigurationNode.root();
            if (this instanceof SeparatedConfigurateStorage) {
                file.getNode("name").setValue(group.getName());
            }

            writeNodes(file, group.normalData().asList());
            saveFile(StorageLocation.GROUPS, group.getName(), file);
        } catch (Exception e) {
            throw new FileIOException(group.getName(), e);
        }
    }

    @Override
    public void deleteGroup(Group group) throws IOException {
        try {
            saveFile(StorageLocation.GROUPS, group.getName(), null);
        } catch (Exception e) {
            throw new FileIOException(group.getName(), e);
        }
        this.plugin.getGroupManager().unload(group.getName());
    }

    @Override
    public Track createAndLoadTrack(String name) throws IOException {
        Track track = this.plugin.getTrackManager().getOrMake(name);
        try {
            ConfigurationNode file = readFile(StorageLocation.TRACKS, name);
            if (file != null) {
                track.setGroups(file.getNode("groups").getList(Types::asString));
            } else {
                file = ConfigurationNode.root();
                if (this instanceof SeparatedConfigurateStorage) {
                    file.getNode("name").setValue(name);
                }
                file.getNode("groups").setValue(track.getGroups());
                saveFile(StorageLocation.TRACKS, name, file);
            }
        } catch (Exception e) {
            throw new FileIOException(name, e);
        }
        return track;
    }

    @Override
    public Optional<Track> loadTrack(String name) throws IOException {
        try {
            ConfigurationNode file = readFile(StorageLocation.TRACKS, name);
            if (file == null) {
                return Optional.empty();
            }

            Track track = this.plugin.getTrackManager().getOrMake(name);
            track.setGroups(file.getNode("groups").getList(Types::asString));
            return Optional.of(track);
        } catch (Exception e) {
            throw new FileIOException(name, e);
        }
    }

    @Override
    public void saveTrack(Track track) throws IOException {
        try {
            ConfigurationNode file = ConfigurationNode.root();
            if (this instanceof SeparatedConfigurateStorage) {
                file.getNode("name").setValue(track.getName());
            }
            file.getNode("groups").setValue(track.getGroups());
            saveFile(StorageLocation.TRACKS, track.getName(), file);
        } catch (Exception e) {
            throw new FileIOException(track.getName(), e);
        }
    }

    @Override
    public void deleteTrack(Track track) throws IOException {
        try {
            saveFile(StorageLocation.TRACKS, track.getName(), null);
        } catch (Exception e) {
            throw new FileIOException(track.getName(), e);
        }
        this.plugin.getTrackManager().unload(track.getName());
    }

    @Override
    public PlayerSaveResult savePlayerData(UUID uniqueId, String username) {
        return this.uuidCache.addMapping(uniqueId, username);
    }

    @Override
    public void deletePlayerData(UUID uniqueId) {
        this.uuidCache.removeMapping(uniqueId);
    }

    @Override
    public UUID getPlayerUniqueId(String username) {
        return this.uuidCache.lookupUuid(username);
    }

    @Override
    public String getPlayerName(UUID uniqueId) {
        return this.uuidCache.lookupUsername(uniqueId);
    }

    protected boolean processBulkUpdate(BulkUpdate bulkUpdate, ConfigurationNode node, HolderType holderType) {
        Set<Node> nodes = readNodes(node);
        Set<Node> results = bulkUpdate.apply(nodes, holderType);

        if (results == null) {
            return false;
        }

        writeNodes(node, results);
        return true;
    }

    private static ImmutableContextSet readContexts(ConfigurationNode attributes) {
        ImmutableContextSet.Builder contextBuilder = new ImmutableContextSetImpl.BuilderImpl();
        ConfigurationNode contextMap = attributes.getNode("context");
        if (!contextMap.isVirtual() && contextMap.isMap()) {
            contextBuilder.addAll(ContextSetConfigurateSerializer.deserializeContextSet(contextMap));
        }

        String server = attributes.getNode("server").getString("global");
        contextBuilder.add(DefaultContextKeys.SERVER_KEY, server);

        String world = attributes.getNode("world").getString("global");
        contextBuilder.add(DefaultContextKeys.WORLD_KEY, world);

        return contextBuilder.build();
    }

    private static Node readAttributes(NodeBuilder<?, ?> builder, ConfigurationNode attributes) {
        long expiryVal = attributes.getNode("expiry").getLong(0L);
        Instant expiry = expiryVal == 0L ? null : Instant.ofEpochSecond(expiryVal);
        ImmutableContextSet context = readContexts(attributes);

        return builder.expiry(expiry).context(context).build();
    }

    private static final class NodeEntry {
        final String key;
        final ConfigurationNode attributes;

        private NodeEntry(String key, ConfigurationNode attributes) {
            this.key = key;
            this.attributes = attributes;
        }
    }

    private static NodeEntry parseNode(ConfigurationNode configNode, String keyFieldName) {
        Map<Object, ? extends ConfigurationNode> children = configNode.getChildrenMap();
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
                    return new NodeEntry(permission, attributes);
                }
            }
        }

        // assume 'configNode' is the actual entry.
        String permission = children.get(keyFieldName).getString(null);
        if (permission == null) {
            return null;
        }

        return new NodeEntry(permission, configNode);
    }

    protected static Set<Node> readNodes(ConfigurationNode data) {
        Set<Node> nodes = new HashSet<>();

        for (ConfigurationNode appended : data.getNode("permissions").getChildrenList()) {
            String plainValue = appended.getValue(Types::strictAsString);
            if (plainValue != null) {
                nodes.add(NodeBuilders.determineMostApplicable(plainValue).build());
                continue;
            }

            NodeEntry entry = parseNode(appended, "permission");
            if (entry == null) {
                continue;
            }

            nodes.add(readAttributes(
                    NodeBuilders.determineMostApplicable(entry.key).value(entry.attributes.getNode("value").getBoolean(true)),
                    entry.attributes
            ));
        }

        for (ConfigurationNode appended : data.getNode("parents").getChildrenList()) {
            String plainValue = appended.getValue(Types::strictAsString);
            if (plainValue != null) {
                nodes.add(Inheritance.builder(plainValue).build());
                continue;
            }

            NodeEntry entry = parseNode(appended, "group");
            if (entry == null) {
                continue;
            }

            nodes.add(readAttributes(
                    Inheritance.builder(entry.key),
                    entry.attributes
            ));
        }

        for (ConfigurationNode appended : data.getNode("prefixes").getChildrenList()) {
            NodeEntry entry = parseNode(appended, "prefix");
            if (entry == null) {
                continue;
            }

            nodes.add(readAttributes(
                    Prefix.builder(entry.key, entry.attributes.getNode("priority").getInt(0)),
                    entry.attributes
            ));
        }

        for (ConfigurationNode appended : data.getNode("suffixes").getChildrenList()) {
            NodeEntry entry = parseNode(appended, "suffix");
            if (entry == null) {
                continue;
            }

            nodes.add(readAttributes(
                    Suffix.builder(entry.key, entry.attributes.getNode("priority").getInt(0)),
                    entry.attributes
            ));
        }

        for (ConfigurationNode appended : data.getNode("meta").getChildrenList()) {
            NodeEntry entry = parseNode(appended, "key");
            if (entry == null) {
                continue;
            }

            nodes.add(readAttributes(
                    Meta.builder(entry.key, entry.attributes.getNode("value").getString("null")),
                    entry.attributes
            ));
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
        ConfigurationNode appended = base.appendListNode();
        if (this.loader instanceof YamlLoader) {
            // create a map node with a single entry of key --> attributes
            appended.getNode(key).setValue(attributes);
        } else {
            // include the attributes and key in the same map
            appended.getNode(keyFieldName).setValue(key);
            appended.mergeValuesFrom(attributes);
        }
    }

    private void writeNodes(ConfigurationNode to, Collection<Node> nodes) {
        ConfigurationNode permissionsSection = ConfigurationNode.root();

        // ensure for CombinedConfigurateStorage that there's at least *something*
        // to save to the file even if it's just an empty list.
        if (this instanceof CombinedConfigurateStorage) {
            permissionsSection.setValue(Collections.emptyList());
        }

        ConfigurationNode parentsSection = ConfigurationNode.root();
        ConfigurationNode prefixesSection = ConfigurationNode.root();
        ConfigurationNode suffixesSection = ConfigurationNode.root();
        ConfigurationNode metaSection = ConfigurationNode.root();

        for (Node n : nodes) {
            // just add a string to the list.
            if (this.loader instanceof YamlLoader && isPlain(n)) {
                if (n instanceof InheritanceNode) {
                    parentsSection.appendListNode().setValue(((InheritanceNode) n).getGroupName());
                    continue;
                }
                if (!NodeType.META_OR_CHAT_META.matches(n)) {
                    permissionsSection.appendListNode().setValue(n.getKey());
                    continue;
                }
            }

            if (n instanceof ChatMetaNode<?, ?> && n.getValue()) {
                // handle prefixes / suffixes
                ChatMetaNode<?, ?> chatMeta = (ChatMetaNode<?, ?>) n;

                ConfigurationNode attributes = ConfigurationNode.root();
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

                ConfigurationNode attributes = ConfigurationNode.root();
                attributes.getNode("value").setValue(meta.getMetaValue());
                writeAttributesTo(attributes, n, false);

                appendNode(metaSection, meta.getMetaKey(), attributes, "key");
            } else if (n instanceof InheritanceNode && n.getValue()) {
                // handle group nodes
                InheritanceNode inheritance = (InheritanceNode) n;

                ConfigurationNode attributes = ConfigurationNode.root();
                writeAttributesTo(attributes, n, false);

                appendNode(parentsSection, inheritance.getGroupName(), attributes, "group");
            } else {
                // handle regular permissions and negated meta+prefixes+suffixes
                ConfigurationNode attributes = ConfigurationNode.root();
                writeAttributesTo(attributes, n, true);

                appendNode(permissionsSection, n.getKey(), attributes, "permission");
            }
        }

        if (permissionsSection.isList() || this instanceof CombinedConfigurateStorage) {
            to.getNode("permissions").setValue(permissionsSection);
        } else {
            to.removeChild("permissions");
        }

        if (parentsSection.isList()) {
            to.getNode("parents").setValue(parentsSection);
        } else {
            to.removeChild("parents");
        }

        if (prefixesSection.isList()) {
            to.getNode("prefixes").setValue(prefixesSection);
        } else {
            to.removeChild("prefixes");
        }

        if (suffixesSection.isList()) {
            to.getNode("suffixes").setValue(suffixesSection);
        } else {
            to.removeChild("suffixes");
        }

        if (metaSection.isList()) {
            to.getNode("meta").setValue(metaSection);
        } else {
            to.removeChild("meta");
        }
    }
}
