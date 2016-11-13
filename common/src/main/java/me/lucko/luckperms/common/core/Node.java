/*
 * Copyright (c) 2016 Lucko (Luck) <luck@lucko.me>
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

package me.lucko.luckperms.common.core;

import com.google.common.base.Preconditions;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import me.lucko.luckperms.api.MetaUtils;
import me.lucko.luckperms.api.Tristate;
import me.lucko.luckperms.api.context.ContextSet;
import me.lucko.luckperms.common.constants.Patterns;

import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * An immutable permission node
 */
@SuppressWarnings("OptionalGetWithoutIsPresent")
@ToString(of = {"permission", "value", "override", "server", "world", "expireAt", "contexts"})
@EqualsAndHashCode(of = {"permission", "value", "override", "server", "world", "expireAt", "contexts"})
public class Node implements me.lucko.luckperms.api.Node {
    private static final Pattern PREFIX_PATTERN = Pattern.compile("(?i)prefix\\.-?\\d+\\..*");
    private static final Pattern SUFFIX_PATTERN = Pattern.compile("(?i)suffix\\.-?\\d+\\..*");
    private static final Pattern META_PATTERN = Pattern.compile("meta\\..*\\..*");

    @Getter
    private final String permission;

    @Getter
    private Boolean value;

    @Getter
    private boolean override;

    private String server = null;
    private String world = null;

    private long expireAt = 0L;

    @Getter
    private final ContextSet contexts;

    // Cached state
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
     * @param permission the actual permission node
     * @param value the value (if it's *not* negated)
     * @param expireAt the time when the node will expire
     * @param server the server this node applies on
     * @param world the world this node applies on
     * @param contexts any additional contexts applying to this node
     */
    public Node(String permission, boolean value, boolean override, long expireAt, String server, String world, ContextSet contexts) {
        if (permission == null || permission.equals("")) {
            throw new IllegalArgumentException("Empty permission");
        }

        if (server != null && (server.equalsIgnoreCase("global") || server.equals(""))) {
            server = null;
        }

        if (world != null && world.equals("")) {
            world = null;
        }

        if (world != null && server == null) {
            server = "global";
        }

        this.permission = permission;
        this.value = value;
        this.override = override;
        this.expireAt = expireAt;
        this.server = server;
        this.world = world;
        this.contexts = contexts == null ? ContextSet.empty() : contexts.makeImmutable();

        // Setup state
        isGroup = permission.toLowerCase().startsWith("group.");
        if (isGroup) {
            groupName = permission.substring("group.".length());
        }

        isWildcard = permission.endsWith(".*");
        wildcardLevel = (int) permission.chars().filter(num -> num == Character.getNumericValue('.')).count();

        isMeta = META_PATTERN.matcher(permission).matches();
        if (isMeta) {
            List<String> metaPart = Splitter.on('.').limit(2).splitToList(getPermission().substring("meta.".length()));
            meta = Maps.immutableEntry(MetaUtils.unescapeCharacters(metaPart.get(0)), MetaUtils.unescapeCharacters(metaPart.get(1)));
        }

        isPrefix = PREFIX_PATTERN.matcher(permission).matches();
        if (isPrefix) {
            List<String> prefixPart = Splitter.on('.').limit(2).splitToList(getPermission().substring("prefix.".length()));
            Integer i = Integer.parseInt(prefixPart.get(0));
            prefix = Maps.immutableEntry(i, MetaUtils.unescapeCharacters(prefixPart.get(1)));
        }

        isSuffix = SUFFIX_PATTERN.matcher(permission).matches();
        if (isSuffix) {
            List<String> suffixPart = Splitter.on('.').limit(2).splitToList(getPermission().substring("suffix.".length()));
            Integer i = Integer.parseInt(suffixPart.get(0));
            suffix = Maps.immutableEntry(i, MetaUtils.unescapeCharacters(suffixPart.get(1)));
        }

        resolvedShorthand = calculateShorthand();
        serializedNode = calculateSerializedNode();
    }

    @Override
    public Tristate getTristate() {
        return Tristate.fromBoolean(value);
    }

    @Override
    public boolean isNegated() {
        return !value;
    }

    @Override
    public Optional<String> getServer() {
        return Optional.ofNullable(server);
    }

    @Override
    public Optional<String> getWorld() {
        return Optional.ofNullable(world);
    }

    @Override
    public boolean isServerSpecific() {
        return server != null && !server.equalsIgnoreCase("global");
    }

    @Override
    public boolean isWorldSpecific() {
        return world != null;
    }

    @Override
    public boolean isTemporary() {
        return expireAt != 0L;
    }

    @Override
    public boolean isPermanent() {
        return !isTemporary();
    }

    @Override
    public long getExpiryUnixTime(){
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
        return expireAt - (System.currentTimeMillis() / 1000L);
    }

    @Override
    public boolean hasExpired() {
        return isTemporary() && expireAt < (System.currentTimeMillis() / 1000L);
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

    private static boolean shouldApply(String world, boolean applyRegex, String thisWorld) {
        if (world.toLowerCase().startsWith("r=") && applyRegex) {
            Pattern p = Patterns.compile(world.substring(2));
            if (p == null) {
                return false;
            }
            return p.matcher(thisWorld).matches();
        }

        if (world.startsWith("(") && world.endsWith(")") && world.contains("|")) {
            final String bits = world.substring(1, world.length() - 1);
            Iterable<String> parts = Splitter.on('|').split(bits);

            for (String s : parts) {
                if (s.equalsIgnoreCase(thisWorld)) {
                    return true;
                }
            }

            return false;
        }

        return thisWorld.equalsIgnoreCase(world);
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

    @SuppressWarnings("ResultOfMethodCallIgnored")
    private static boolean isInt(String a, String b) {
        try {
            Integer.parseInt(a);
            Integer.parseInt(b);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private static boolean isChar(String a, String b) {
        return a.length() == 1 && b.length() == 1;
    }

    private static Set<String> getCharRange(char a, char b) {
        Set<String> s = new HashSet<>();
        for (char c = a; c <= b; c++) {
            s.add(Character.toString(c));
        }
        return s;
    }

    @Override
    public List<String> resolveShorthand() {
        return resolvedShorthand;
    }

    @Override
    public String toSerializedNode() {
        return serializedNode;
    }

    private List<String> calculateShorthand() {
        if (!Patterns.SHORTHAND_NODE.matcher(getPermission()).find()) {
            return ImmutableList.of();
        }

        if (!getPermission().contains(".")) {
            return ImmutableList.of();
        }

        Iterable<String> parts = Splitter.on('.').split(getPermission());
        List<Set<String>> nodeParts = new ArrayList<>();

        for (String s : parts) {
            if ((!s.startsWith("(") || !s.endsWith(")")) || (!s.contains("|") && !s.contains("-"))) {
                nodeParts.add(Collections.singleton(s));
                continue;
            }

            final String bits = s.substring(1, s.length() - 1);
            if (s.contains("|")) {
                nodeParts.add(new HashSet<>(Splitter.on('|').splitToList(bits)));
            } else {
                List<String> range = Splitter.on('-').limit(2).splitToList(bits);
                if (isChar(range.get(0), range.get(1))) {
                    nodeParts.add(getCharRange(range.get(0).charAt(0), range.get(1).charAt(0)));
                } else if (isInt(range.get(0), range.get(1))) {
                    nodeParts.add(IntStream.rangeClosed(Integer.parseInt(range.get(0)), Integer.parseInt(range.get(1))).boxed()
                            .map(i -> "" + i)
                            .collect(Collectors.toSet())
                    );
                } else {
                    // Fallback
                    nodeParts.add(Collections.singleton(s));
                }
            }
        }

        Set<String> nodes = new HashSet<>();
        for (Set<String> set : nodeParts) {
            final Set<String> newNodes = new HashSet<>();
            if (nodes.isEmpty()) {
                newNodes.addAll(set);
            } else {
                nodes.forEach(str -> newNodes.addAll(set.stream()
                        .map(add -> str + "." + add)
                        .collect(Collectors.toList()))
                );
            }
            nodes = newNodes;
        }

        return ImmutableList.copyOf(nodes);
    }

    private String calculateSerializedNode() {
        StringBuilder builder = new StringBuilder();

        if (server != null) {
            builder.append(server);

            if (world != null) {
                builder.append("-").append(world);
            }
            builder.append("/");
        } else {
            if (world != null) {
                builder.append("global-").append(world).append("/");
            }
        }

        if (!contexts.isEmpty()) {
            builder.append("(");
            for (Map.Entry<String, String> entry : contexts.toSet()) {
                builder.append(entry.getKey()).append("=").append(entry.getValue()).append(",");
            }

            builder.deleteCharAt(builder.length() - 1);
            builder.append(")");
        }

        builder.append(permission);

        if (expireAt != 0L) {
            builder.append("$").append(expireAt);
        }

        return builder.toString();
    }

    @Override
    public boolean equalsIgnoringValue(me.lucko.luckperms.api.Node other) {
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

        if (!other.getContexts().equals(this.getContexts())) {
            return false;
        }

        return true;
    }

    @Override
    public boolean almostEquals(me.lucko.luckperms.api.Node other) {
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

        if (!other.getContexts().equals(this.getContexts())) {
            return false;
        }

        return true;
    }

    @Override
    public boolean equalsIgnoringValueOrTemp(me.lucko.luckperms.api.Node other) {
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

        if (!other.getContexts().equals(this.getContexts())) {
            return false;
        }

        return true;
    }

    @Override
    public Boolean setValue(Boolean value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getKey() {
        return getPermission();
    }
}
