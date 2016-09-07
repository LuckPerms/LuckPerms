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

package me.lucko.luckperms.utils;

import com.google.common.collect.ImmutableMap;
import lombok.*;
import me.lucko.luckperms.api.Tristate;
import me.lucko.luckperms.constants.Patterns;

import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * An immutable permission node
 */
@SuppressWarnings({"WeakerAccess", "unused"})
@ToString
@EqualsAndHashCode
public class Node implements me.lucko.luckperms.api.Node {
    private static final Pattern PREFIX_PATTERN = Pattern.compile("(?i)prefix\\.-?\\d+\\..*");
    private static final Pattern SUFFIX_PATTERN = Pattern.compile("(?i)suffix\\.-?\\d+\\..*");

    public static me.lucko.luckperms.api.Node fromSerialisedNode(String s, Boolean b) {
        return builderFromSerialisedNode(s, b).build();
    }

    public static me.lucko.luckperms.api.Node.Builder builderFromSerialisedNode(String s, Boolean b) {
        if (s.contains("/")) {
            String[] parts = Patterns.SERVER_DELIMITER.split(s, 2);
            // 0=server(+world)   1=node

            // WORLD SPECIFIC
            if (parts[0].contains("-")) {
                String[] serverParts = Patterns.WORLD_DELIMITER.split(parts[0], 2);
                // 0=server   1=world

                if (parts[1].contains("$")) {
                    String[] tempParts = Patterns.TEMP_DELIMITER.split(parts[1], 2);
                    return new Node.Builder(tempParts[0], true).setServerRaw(serverParts[0]).setWorld(serverParts[1])
                            .setExpiry(Long.parseLong(tempParts[1])).setValue(b);
                } else {
                    return new Node.Builder(parts[1], true).setServerRaw(serverParts[0]).setWorld(serverParts[1]).setValue(b);
                }

            } else {
                // SERVER BUT NOT WORLD SPECIFIC
                if (parts[1].contains("$")) {
                    String[] tempParts = Patterns.TEMP_DELIMITER.split(parts[1], 2);
                    return new Node.Builder(tempParts[0], true).setServerRaw(parts[0]).setExpiry(Long.parseLong(tempParts[1])).setValue(b);
                } else {
                    return new Node.Builder(parts[1], true).setServerRaw(parts[0]).setValue(b);
                }
            }
        } else {
            // NOT SERVER SPECIFIC
            if (s.contains("$")) {
                String[] tempParts = Patterns.TEMP_DELIMITER.split(s, 2);
                return new Node.Builder(tempParts[0], true).setExpiry(Long.parseLong(tempParts[1])).setValue(b);
            } else {
                return new Node.Builder(s, true).setValue(b);
            }
        }
    }

    @Getter
    private final String permission;

    @Getter
    private Boolean value;

    @Getter
    private boolean override;

    private String server = null;
    private String world = null;

    private long expireAt = 0L;

    private final Map<String, String> extraContexts = new HashMap<>();

    /**
     * Make an immutable node instance
     * @param permission the actual permission node
     * @param value the value (if it's *not* negated)
     * @param expireAt the time when the node will expire
     * @param server the server this node applies on
     * @param world the world this node applies on
     * @param extraContexts any additional contexts applying to this node
     */
    public Node(String permission, boolean value, boolean override, long expireAt, String server, String world, Map<String, String> extraContexts) {
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

        if (extraContexts != null) {
            this.extraContexts.putAll(extraContexts);
        }
    }

    @Override
    public Tristate getTristate() {
        return Tristate.fromBoolean(value);
    }

    public boolean isNegated() {
        return !value;
    }

    public Optional<String> getServer() {
        return Optional.ofNullable(server);
    }

    public Optional<String> getWorld() {
        return Optional.ofNullable(world);
    }

    public boolean isServerSpecific() {
        return getServer().isPresent() && !getServer().get().equalsIgnoreCase("global");
    }

    public boolean isWorldSpecific() {
        return getWorld().isPresent();
    }

    @Override
    public boolean shouldApplyOnServer(String server, boolean includeGlobal, boolean applyRegex) {
        if (server == null || server.equals("")) {
            return true;
        }

        if (isServerSpecific()) {
            return shouldApply(server, applyRegex, this.server);
        } else {
            return includeGlobal;
        }
    }

    @Override
    public boolean shouldApplyOnWorld(String world, boolean includeGlobal, boolean applyRegex) {
        if (world == null || world.equals("")) {
            return true;
        }

        if (isWorldSpecific()) {
            return shouldApply(world, applyRegex, this.world);
        } else {
            return includeGlobal;
        }
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
            String[] parts = Patterns.VERTICAL_BAR.split(bits);

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
    public boolean shouldApplyWithContext(Map<String, String> context) {
        if (context == null || context.isEmpty()) {
            return true;
        }

        for (Map.Entry<String, String> c : context.entrySet()) {
            if (c.getKey().equals("server")) {
                if (shouldApplyOnServer(c.getValue(), false, false)) {
                    return false;
                }
            }

            if (c.getKey().equals("world")) {
                if (shouldApplyOnWorld(c.getValue(), false, false)) {
                    return false;
                }
            }

            if (!getExtraContexts().containsKey(c.getKey())) {
                return false;
            }

            if (!getExtraContexts().get(c.getKey()).equalsIgnoreCase(c.getValue())) {
                return false;
            }
        }

        return true;
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
        if (!Patterns.SHORTHAND_NODE.matcher(getPermission()).find()) {
            return Collections.emptyList();
        }

        if (!getPermission().contains(".")) {
            return Collections.emptyList();
        }

        String[] parts = Patterns.DOT.split(getPermission());
        List<Set<String>> nodeParts = new ArrayList<>();

        for (String s : parts) {
            if ((!s.startsWith("(") || !s.endsWith(")")) || (!s.contains("|") && !s.contains("-"))) {
                nodeParts.add(Collections.singleton(s));
                continue;
            }

            final String bits = s.substring(1, s.length() - 1);
            if (s.contains("|")) {
                nodeParts.add(new HashSet<>(Arrays.asList(Patterns.VERTICAL_BAR.split(bits))));
            } else {
                String[] range = Patterns.WORLD_DELIMITER.split(bits, 2);
                if (isChar(range[0], range[1])) {
                    nodeParts.add(getCharRange(range[0].charAt(0), range[1].charAt(0)));
                } else if (isInt(range[0], range[1])) {
                    nodeParts.add(IntStream.rangeClosed(Integer.parseInt(range[0]), Integer.parseInt(range[1])).boxed()
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

        return new ArrayList<>(nodes);
    }

    public boolean isTemporary() {
        return expireAt != 0L;
    }

    public boolean isPermanent() {
        return !isTemporary();
    }

    public long getExpiryUnixTime(){
        return expireAt;
    }

    public Date getExpiry() {
        return new Date(expireAt * 1000L);
    }

    public long getSecondsTilExpiry() {
        return expireAt - (System.currentTimeMillis() / 1000L);
    }

    public boolean hasExpired() {
        return isTemporary() && expireAt < (System.currentTimeMillis() / 1000L);
    }

    public Map<String, String> getExtraContexts() {
        return ImmutableMap.copyOf(extraContexts);
    }

    public String toSerializedNode() {
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

        if (!extraContexts.isEmpty()) {
            builder.append("(");
            for (Map.Entry<String, String> entry : extraContexts.entrySet()) {
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
    public boolean isGroupNode() {
        return Patterns.GROUP_MATCH.matcher(getPermission()).matches();
    }

    @Override
    public String getGroupName() {
        if (!isGroupNode()) {
            throw new IllegalStateException("This is not a group node");
        }

        return getPermission().substring("group.".length());
    }

    @Override
    public boolean isWildcard() {
        return getPermission().endsWith(".*");
    }

    @Override
    public int getWildcardLevel() {
        return (int) getPermission().chars().filter(num -> num == Character.getNumericValue('.')).count();
    }

    @Override
    public boolean isMeta() {
        return getPermission().matches("meta\\..*\\..*");
    }

    @Override
    public Map.Entry<String, String> getMeta() {
        if (!isMeta()) {
            throw new IllegalStateException();
        }

        String[] metaPart = getPermission().substring("meta.".length()).split("\\.", 2);
        return new AbstractMap.SimpleEntry<>(metaPart[0], metaPart[1]);
    }

    @Override
    public boolean isPrefix() {
        return PREFIX_PATTERN.matcher(getPermission()).matches();
    }

    @Override
    public Map.Entry<Integer, String> getPrefix() {
        if (!isPrefix()) {
            throw new IllegalStateException();
        }

        String[] prefixPart = Patterns.DOT.split(getPermission().substring("prefix.".length()), 2);
        Integer i = Integer.parseInt(prefixPart[0]);

        return new AbstractMap.SimpleEntry<>(i, prefixPart[1]);
    }

    @Override
    public boolean isSuffix() {
        return SUFFIX_PATTERN.matcher(getPermission()).matches();
    }

    @Override
    public Map.Entry<Integer, String> getSuffix() {
        if (!isSuffix()) {
            throw new IllegalStateException();
        }

        String[] suffixPart = Patterns.DOT.split(getPermission().substring("suffix.".length()), 2);
        Integer i = Integer.parseInt(suffixPart[0]);

        return new AbstractMap.SimpleEntry<>(i, suffixPart[1]);
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

        if (!other.getExtraContexts().equals(this.getExtraContexts())) {
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

        if (!other.getExtraContexts().equals(this.getExtraContexts())) {
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

        if (!other.getExtraContexts().equals(this.getExtraContexts())) {
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

    @RequiredArgsConstructor
    public static class Builder implements me.lucko.luckperms.api.Node.Builder {
        private final String permission;
        private Boolean value = true;
        private boolean override = false;
        private String server = null;
        private String world = null;
        private long expireAt = 0L;

        private final Map<String, String> extraContexts = new HashMap<>();

        Builder(String permission, boolean shouldConvertContexts) {
            if (!shouldConvertContexts) {
                this.permission = permission;
            } else {
                if (!Patterns.NODE_CONTEXTS.matcher(permission).matches()) {
                    this.permission = permission;
                } else {
                    String[] contextParts = permission.substring(1).split("\\)", 2);
                    // 0 = context, 1 = node
                    this.permission = contextParts[1];

                    for (String s : contextParts[0].split("\\,")) {
                        if (!s.contains("=")) {
                            // Not valid
                            continue;
                        }

                        String[] context = s.split("\\=", 2);
                        extraContexts.put(context[0], context[1]);
                    }
                }
            }
        }

        @Override
        public me.lucko.luckperms.api.Node.Builder setNegated(boolean negated) {
            value = !negated;
            return this;
        }

        @Override
        public me.lucko.luckperms.api.Node.Builder setValue(boolean value) {
            this.value = value;
            return this;
        }

        @Override
        public me.lucko.luckperms.api.Node.Builder setOverride(boolean override) {
            this.override = override;
            return this;
        }

        @Override
        public me.lucko.luckperms.api.Node.Builder setExpiry(long expireAt) {
            this.expireAt = expireAt;
            return this;
        }

        @Override
        public me.lucko.luckperms.api.Node.Builder setWorld(String world) {
            this.world = world;
            return this;
        }

        @Override
        public me.lucko.luckperms.api.Node.Builder setServer(String server) {
            if (server != null && ArgumentChecker.checkServer(server)) {
                throw new IllegalArgumentException("Server name invalid.");
            }

            this.server = server;
            return this;
        }

        public me.lucko.luckperms.api.Node.Builder setServerRaw(String server) {
            this.server = server;
            return this;
        }

        @Override
        public me.lucko.luckperms.api.Node.Builder withExtraContext(@NonNull String key, @NonNull String value) {
            switch (key) {
                case "server":
                    setServer(value);
                    break;
                case "world":
                    setWorld(value);
                    break;
                default:
                    this.extraContexts.put(key, value);
                    break;
            }

            return this;
        }

        @Override
        public me.lucko.luckperms.api.Node.Builder withExtraContext(Map<String, String> map) {
            map.entrySet().forEach(this::withExtraContext);
            return this;
        }

        @Override
        public me.lucko.luckperms.api.Node.Builder withExtraContext(Map.Entry<String, String> entry) {
            withExtraContext(entry.getKey(), entry.getValue());
            return this;
        }

        @Override
        public me.lucko.luckperms.api.Node build() {
            return new Node(permission, value, override, expireAt, server, world, extraContexts);
        }
    }

}
