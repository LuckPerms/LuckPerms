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

package me.lucko.luckperms.common.node;

import lombok.Getter;
import lombok.ToString;

import com.google.common.base.Preconditions;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;

import me.lucko.luckperms.api.MetaUtils;
import me.lucko.luckperms.api.Node;
import me.lucko.luckperms.api.context.ContextSet;
import me.lucko.luckperms.api.context.ImmutableContextSet;
import me.lucko.luckperms.api.context.MutableContextSet;
import me.lucko.luckperms.common.utils.DateUtil;
import me.lucko.luckperms.common.utils.PatternCache;

import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * An immutable permission node
 */
@ToString(of = {"permission", "value", "override", "server", "world", "expireAt", "contexts"})
public final class ImmutableNode implements Node {

    @Getter
    private final String permission;

    @Getter
    private final Boolean value;

    @Getter
    private boolean override;

    // nullable
    private final String server;
    // nullable
    private final String world;

    // 0L for no expiry
    private final long expireAt;

    @Getter
    private final ImmutableContextSet contexts;

    @Getter
    private final ImmutableContextSet fullContexts;

    // Cached state

    // these save on lots of instance creation when comparing nodes
    private final Optional<String> optServer;
    private final Optional<String> optWorld;

    private final boolean isGroup;
    private String groupName;

    private final boolean isWildcard;
    private final int wildcardLevel;

    private final boolean isMeta;
    private Map.Entry<String, String> meta;

    private final boolean isPrefix;
    private Map.Entry<Integer, String> prefix;

    private final boolean isSuffix;
    private Map.Entry<Integer, String> suffix;

    private final List<String> resolvedShorthand;

    private final String serializedNode;

    /**
     * Make an immutable node instance
     *
     * @param permission the actual permission node
     * @param value      the value (if it's *not* negated)
     * @param expireAt   the time when the node will expire
     * @param server     the server this node applies on
     * @param world      the world this node applies on
     * @param contexts   any additional contexts applying to this node
     */
    @SuppressWarnings("deprecation")
    public ImmutableNode(String permission, boolean value, boolean override, long expireAt, String server, String world, ContextSet contexts) {
        if (permission == null || permission.equals("")) {
            throw new IllegalArgumentException("Empty permission");
        }

        // standardize server/world values.
        if (server != null) {
            server = server.toLowerCase();
        }
        if (world != null) {
            world = world.toLowerCase();
        }
        if (server != null && (server.equals("global") || server.equals(""))) {
            server = null;
        }
        if (world != null && (world.equals("global") || world.equals(""))) {
            world = null;
        }

        this.permission = NodeFactory.unescapeDelimiters(permission, "/", "-", "$", "(", ")", "=", ",");
        this.value = value;
        this.override = override;
        this.expireAt = expireAt;
        this.server = NodeFactory.unescapeDelimiters(server, "/", "-");
        this.world = NodeFactory.unescapeDelimiters(world, "/", "-");
        this.contexts = contexts == null ? ContextSet.empty() : contexts.makeImmutable();

        // Setup state
        isGroup = this.permission.toLowerCase().startsWith("group.");
        if (isGroup) {
            groupName = this.permission.substring("group.".length()).toLowerCase();
        }

        isWildcard = this.permission.endsWith(".*");
        wildcardLevel = (int) this.permission.chars().filter(num -> num == Character.getNumericValue('.')).count();

        isMeta = NodeFactory.isMetaNode(this.permission);
        if (isMeta) {
            List<String> metaPart = Splitter.on(PatternCache.compileDelimitedMatcher(".", "\\")).limit(2).splitToList(getPermission().substring("meta.".length()));
            meta = Maps.immutableEntry(MetaUtils.unescapeCharacters(metaPart.get(0)), MetaUtils.unescapeCharacters(metaPart.get(1)));
        }

        isPrefix = NodeFactory.isPrefixNode(this.permission);
        if (isPrefix) {
            List<String> prefixPart = Splitter.on(PatternCache.compileDelimitedMatcher(".", "\\")).limit(2).splitToList(getPermission().substring("prefix.".length()));
            Integer i = Integer.parseInt(prefixPart.get(0));
            prefix = Maps.immutableEntry(i, MetaUtils.unescapeCharacters(prefixPart.get(1)));
        }

        isSuffix = NodeFactory.isSuffixNode(this.permission);
        if (isSuffix) {
            List<String> suffixPart = Splitter.on(PatternCache.compileDelimitedMatcher(".", "\\")).limit(2).splitToList(getPermission().substring("suffix.".length()));
            Integer i = Integer.parseInt(suffixPart.get(0));
            suffix = Maps.immutableEntry(i, MetaUtils.unescapeCharacters(suffixPart.get(1)));
        }

        resolvedShorthand = ImmutableList.copyOf(ShorthandParser.parseShorthand(getPermission()));
        serializedNode = calculateSerializedNode();

        MutableContextSet fullContexts = this.contexts.mutableCopy();
        if (isServerSpecific()) {
            fullContexts.add("server", this.server);
        }
        if (isWorldSpecific()) {
            fullContexts.add("world", this.world);
        }

        this.fullContexts = fullContexts.makeImmutable();
        this.optServer = Optional.ofNullable(this.server);
        this.optWorld = Optional.ofNullable(this.world);
    }

    @Override
    public Optional<String> getServer() {
        return optServer;
    }

    @Override
    public Optional<String> getWorld() {
        return optWorld;
    }

    @Override
    public boolean isServerSpecific() {
        return server != null;
    }

    @Override
    public boolean isWorldSpecific() {
        return world != null;
    }

    @Override
    public boolean appliesGlobally() {
        return server == null && world == null && contexts.isEmpty();
    }

    @Override
    public boolean hasSpecificContext() {
        return server != null || world != null || !contexts.isEmpty();
    }

    @Override
    public boolean isTemporary() {
        return expireAt != 0L;
    }

    @Override
    public long getExpiryUnixTime() {
        Preconditions.checkState(isTemporary(), "Node does not have an expiry time.");
        return expireAt;
    }

    @Override
    public Date getExpiry() {
        Preconditions.checkState(isTemporary(), "Node does not have an expiry time.");
        return new Date(expireAt * 1000L);
    }

    @Override
    public long getSecondsTilExpiry() {
        Preconditions.checkState(isTemporary(), "Node does not have an expiry time.");
        return expireAt - DateUtil.unixSecondsNow();
    }

    @Override
    public boolean hasExpired() {
        return isTemporary() && expireAt < DateUtil.unixSecondsNow();
    }

    @Override
    public boolean isGroupNode() {
        return isGroup;
    }

    @Override
    public String getGroupName() {
        Preconditions.checkState(isGroupNode(), "Node is not a group node");
        return groupName;
    }

    @Override
    public boolean isWildcard() {
        return isWildcard;
    }

    @Override
    public int getWildcardLevel() {
        return wildcardLevel;
    }

    @Override
    public boolean isMeta() {
        return isMeta;
    }

    @Override
    public Map.Entry<String, String> getMeta() {
        Preconditions.checkState(isMeta(), "Node is not a meta node");
        return meta;
    }

    @Override
    public boolean isPrefix() {
        return isPrefix;
    }

    @Override
    public Map.Entry<Integer, String> getPrefix() {
        Preconditions.checkState(isPrefix(), "Node is not a prefix node");
        return prefix;
    }

    @Override
    public boolean isSuffix() {
        return isSuffix;
    }

    @Override
    public Map.Entry<Integer, String> getSuffix() {
        Preconditions.checkState(isSuffix(), "Node is not a suffix node");
        return suffix;
    }

    @Override
    public boolean shouldApply(boolean includeGlobal, boolean includeGlobalWorld, String server, String world, ContextSet context, boolean applyRegex) {
        return shouldApplyOnServer(server, includeGlobal, applyRegex) && shouldApplyOnWorld(world, includeGlobalWorld, applyRegex) && shouldApplyWithContext(context, false);
    }

    @Override
    public boolean shouldApplyOnServer(String server, boolean includeGlobal, boolean applyRegex) {
        if (server == null || server.equals("") || server.equalsIgnoreCase("global")) {
            return !isServerSpecific();
        }

        return isServerSpecific() ? shouldApply(server, applyRegex, this.server) : includeGlobal;
    }

    @Override
    public boolean shouldApplyOnWorld(String world, boolean includeGlobal, boolean applyRegex) {
        if (world == null || world.equals("") || world.equalsIgnoreCase("null")) {
            return !isWorldSpecific();
        }

        return isWorldSpecific() ? shouldApply(world, applyRegex, this.world) : includeGlobal;
    }

    @Override
    public boolean shouldApplyWithContext(ContextSet context, boolean worldAndServer) {
        if (contexts.isEmpty() && !isServerSpecific() && !isWorldSpecific()) {
            return true;
        }

        if (worldAndServer) {
            if (isWorldSpecific()) {
                if (context == null) return false;
                if (!context.hasIgnoreCase("world", world)) return false;
            }

            if (isServerSpecific()) {
                if (context == null) return false;
                if (!context.hasIgnoreCase("server", server)) return false;
            }
        }

        if (!contexts.isEmpty()) {
            if (context == null) return false;

            for (Map.Entry<String, String> c : contexts.toSet()) {
                if (!context.hasIgnoreCase(c.getKey(), c.getValue())) return false;
            }
        }

        return true;
    }

    @Override
    public boolean shouldApplyWithContext(ContextSet context) {
        return shouldApplyWithContext(context, true);
    }

    @Override
    public boolean shouldApplyOnAnyServers(List<String> servers, boolean includeGlobal) {
        for (String s : servers) {
            if (shouldApplyOnServer(s, includeGlobal, false)) {
                return true;
            }
        }

        return false;
    }

    @Override
    public boolean shouldApplyOnAnyWorlds(List<String> worlds, boolean includeGlobal) {
        for (String s : worlds) {
            if (shouldApplyOnWorld(s, includeGlobal, false)) {
                return true;
            }
        }

        return false;
    }

    @Override
    public List<String> resolveWildcard(List<String> possibleNodes) {
        if (!isWildcard() || possibleNodes == null) {
            return Collections.emptyList();
        }

        String match = getPermission().substring(0, getPermission().length() - 2);
        return possibleNodes.stream().filter(pn -> pn.startsWith(match)).collect(Collectors.toList());
    }

    @Override
    public List<String> resolveShorthand() {
        return resolvedShorthand;
    }

    @Override
    public String toSerializedNode() {
        return serializedNode;
    }

    private String calculateSerializedNode() {
        StringBuilder builder = new StringBuilder();

        if (server != null) {
            builder.append(NodeFactory.escapeDelimiters(server, "/", "-"));

            if (world != null) {
                builder.append("-").append(NodeFactory.escapeDelimiters(world, "/", "-"));
            }
            builder.append("/");
        } else {
            if (world != null) {
                builder.append("global-").append(NodeFactory.escapeDelimiters(world, "/", "-")).append("/");
            }
        }

        if (!contexts.isEmpty()) {
            builder.append("(");
            for (Map.Entry<String, String> entry : contexts.toSet()) {
                builder.append(NodeFactory.escapeDelimiters(entry.getKey(), "=", "(", ")", ",")).append("=").append(NodeFactory.escapeDelimiters(entry.getValue(), "=", "(", ")", ",")).append(",");
            }

            builder.deleteCharAt(builder.length() - 1);
            builder.append(")");
        }

        builder.append(NodeFactory.escapeDelimiters(permission, "/", "-", "$", "(", ")", "=", ","));

        if (expireAt != 0L) {
            builder.append("$").append(expireAt);
        }

        return builder.toString();
    }

    public boolean equals(Object o) {
        if (o == this) return true;
        if (!(o instanceof Node)) return false;
        final Node other = (Node) o;

        if (!this.permission.equals(other.getPermission())) return false;
        if (!this.getValue().equals(other.getValue())) return false;
        if (this.override != other.isOverride()) return false;

        final String thisServer = this.getServer().orElse(null);
        final String otherServer = other.getServer().orElse(null);
        if (thisServer == null ? otherServer != null : !thisServer.equals(otherServer)) return false;

        final String thisWorld = this.getWorld().orElse(null);
        final String otherWorld = other.getWorld().orElse(null);
        if (thisWorld == null ? otherWorld != null : !thisWorld.equals(otherWorld)) return false;

        final long thisExpireAt = this.isTemporary() ? this.getExpiryUnixTime() : 0L;
        final long otherExpireAt = other.isTemporary() ? other.getExpiryUnixTime() : 0L;

        return thisExpireAt == otherExpireAt && this.getContexts().equals(other.getContexts());
    }

    public int hashCode() {
        final int PRIME = 59;
        int result = 1;

        result = result * PRIME + this.permission.hashCode();
        result = result * PRIME + Boolean.hashCode(this.value);
        result = result * PRIME + (this.override ? 79 : 97);

        final String server = this.getServer().orElse(null);
        result = result * PRIME + (server == null ? 43 : server.hashCode());

        final String world = this.getWorld().orElse(null);
        result = result * PRIME + (world == null ? 43 : world.hashCode());

        result = result * PRIME + (int) (this.expireAt >>> 32 ^ this.expireAt);
        result = result * PRIME + this.contexts.hashCode();

        return result;
    }

    @Override
    public boolean equalsIgnoringValue(Node other) {
        if (!other.getPermission().equalsIgnoreCase(this.getPermission())) {
            return false;
        }

        if (other.isTemporary() != this.isTemporary()) {
            return false;
        }

        if (this.isTemporary()) {
            if (other.getExpiryUnixTime() != this.getExpiryUnixTime()) {
                return false;
            }
        }

        if (other.getServer().isPresent() == this.getServer().isPresent()) {
            if (other.getServer().isPresent()) {
                if (!other.getServer().get().equalsIgnoreCase(this.getServer().get())) {
                    return false;
                }
            }
        } else {
            return false;
        }

        if (other.getWorld().isPresent() == this.getWorld().isPresent()) {
            if (other.getWorld().isPresent()) {
                if (!other.getWorld().get().equalsIgnoreCase(this.getWorld().get())) {
                    return false;
                }
            }
        } else {
            return false;
        }

        return other.getContexts().equals(this.getContexts());
    }

    @Override
    public boolean almostEquals(Node other) {
        if (!other.getPermission().equalsIgnoreCase(this.getPermission())) {
            return false;
        }

        if (other.isTemporary() != this.isTemporary()) {
            return false;
        }

        if (other.getServer().isPresent() == this.getServer().isPresent()) {
            if (other.getServer().isPresent()) {
                if (!other.getServer().get().equalsIgnoreCase(this.getServer().get())) {
                    return false;
                }
            }
        } else {
            return false;
        }

        if (other.getWorld().isPresent() == this.getWorld().isPresent()) {
            if (other.getWorld().isPresent()) {
                if (!other.getWorld().get().equalsIgnoreCase(this.getWorld().get())) {
                    return false;
                }
            }
        } else {
            return false;
        }

        return other.getContexts().equals(this.getContexts());
    }

    @Override
    public boolean equalsIgnoringValueOrTemp(Node other) {
        if (!other.getPermission().equalsIgnoreCase(this.getPermission())) {
            return false;
        }

        if (other.getServer().isPresent() == this.getServer().isPresent()) {
            if (other.getServer().isPresent()) {
                if (!other.getServer().get().equalsIgnoreCase(this.getServer().get())) {
                    return false;
                }
            }
        } else {
            return false;
        }

        if (other.getWorld().isPresent() == this.getWorld().isPresent()) {
            if (other.getWorld().isPresent()) {
                if (!other.getWorld().get().equalsIgnoreCase(this.getWorld().get())) {
                    return false;
                }
            }
        } else {
            return false;
        }

        return other.getContexts().equals(this.getContexts());
    }

    @Override
    public Boolean setValue(Boolean value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getKey() {
        return getPermission();
    }

    private static boolean shouldApply(String str, boolean applyRegex, String thisStr) {
        if (str.equalsIgnoreCase(thisStr)) {
            return true;
        }

        Set<String> expandedStr = ShorthandParser.parseShorthand(str, false);
        Set<String> expandedThisStr = ShorthandParser.parseShorthand(thisStr, false);

        if (str.toLowerCase().startsWith("r=") && applyRegex) {
            Pattern p = PatternCache.compile(str.substring(2));
            if (p == null) {
                return false;
            }

            for (String s : expandedThisStr) {
                if (p.matcher(s).matches()) {
                    return true;
                }
            }
            return false;
        }

        if (thisStr.toLowerCase().startsWith("r=") && applyRegex) {
            Pattern p = PatternCache.compile(thisStr.substring(2));
            if (p == null) {
                return false;
            }

            for (String s : expandedStr) {
                if (p.matcher(s).matches()) {
                    return true;
                }
            }
            return false;
        }

        if (expandedStr.size() <= 1 && expandedThisStr.size() <= 1) {
            return false;
        }

        for (String t : expandedThisStr) {
            for (String s : expandedStr) {
                if (t.equalsIgnoreCase(s)) {
                    return true;
                }
            }
        }
        return false;
    }

}
