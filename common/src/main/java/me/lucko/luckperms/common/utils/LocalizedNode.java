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

package me.lucko.luckperms.common.utils;

import lombok.*;
import me.lucko.luckperms.api.Node;
import me.lucko.luckperms.api.Tristate;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Holds a Node and where it was inherited from
 */
@Getter
@ToString
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class LocalizedNode implements me.lucko.luckperms.api.LocalizedNode {
    public static LocalizedNode of(@NonNull me.lucko.luckperms.api.Node node, @NonNull String location) {
        return new LocalizedNode(node, location);
    }

    private final me.lucko.luckperms.api.Node node;
    private final String location;

    @Override
    public int hashCode() {
        return node.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        return node.equals(obj);
    }

    @Override
    public String getPermission() {
        return node.getPermission();
    }

    @Override
    public String getKey() {
        return node.getKey();
    }

    @Override
    public Boolean getValue() {
        return node.getValue();
    }

    @Override
    public Boolean setValue(Boolean value) {
        return node.setValue(value);
    }

    @Override
    public Tristate getTristate() {
        return node.getTristate();
    }

    @Override
    public boolean isNegated() {
        return node.isNegated();
    }

    @Override
    public boolean isOverride() {
        return node.isOverride();
    }

    @Override
    public Optional<String> getServer() {
        return node.getServer();
    }

    @Override
    public Optional<String> getWorld() {
        return node.getWorld();
    }

    @Override
    public boolean isServerSpecific() {
        return node.isServerSpecific();
    }

    @Override
    public boolean isWorldSpecific() {
        return node.isWorldSpecific();
    }

    @Override
    public boolean shouldApplyOnServer(String server, boolean includeGlobal, boolean applyRegex) {
        return node.shouldApplyOnServer(server, includeGlobal, applyRegex);
    }

    @Override
    public boolean shouldApplyOnWorld(String world, boolean includeGlobal, boolean applyRegex) {
        return node.shouldApplyOnWorld(world, includeGlobal, applyRegex);
    }

    @Override
    public boolean shouldApplyWithContext(Map<String, String> context, boolean worldAndServer) {
        return node.shouldApplyWithContext(context, worldAndServer);
    }

    @Override
    public boolean shouldApplyWithContext(Map<String, String> context) {
        return node.shouldApplyWithContext(context);
    }

    @Override
    public boolean shouldApplyOnAnyServers(List<String> servers, boolean includeGlobal) {
        return node.shouldApplyOnAnyServers(servers, includeGlobal);
    }

    @Override
    public boolean shouldApplyOnAnyWorlds(List<String> worlds, boolean includeGlobal) {
        return node.shouldApplyOnAnyWorlds(worlds, includeGlobal);
    }

    @Override
    public List<String> resolveWildcard(List<String> possibleNodes) {
        return node.resolveWildcard(possibleNodes);
    }

    @Override
    public List<String> resolveShorthand() {
        return node.resolveShorthand();
    }

    @Override
    public boolean isTemporary() {
        return node.isTemporary();
    }

    @Override
    public boolean isPermanent() {
        return node.isPermanent();
    }

    @Override
    public long getExpiryUnixTime() {
        return node.getExpiryUnixTime();
    }

    @Override
    public Date getExpiry() {
        return node.getExpiry();
    }

    @Override
    public long getSecondsTilExpiry() {
        return node.getSecondsTilExpiry();
    }

    @Override
    public boolean hasExpired() {
        return node.hasExpired();
    }

    @Override
    public Map<String, String> getExtraContexts() {
        return node.getExtraContexts();
    }

    @Override
    public String toSerializedNode() {
        return node.toSerializedNode();
    }

    @Override
    public boolean isGroupNode() {
        return node.isGroupNode();
    }

    @Override
    public String getGroupName() {
        return node.getGroupName();
    }

    @Override
    public boolean isWildcard() {
        return node.isWildcard();
    }

    @Override
    public int getWildcardLevel() {
        return node.getWildcardLevel();
    }

    @Override
    public boolean isMeta() {
        return node.isMeta();
    }

    @Override
    public Map.Entry<String, String> getMeta() {
        return node.getMeta();
    }

    @Override
    public boolean isPrefix() {
        return node.isPrefix();
    }

    @Override
    public Map.Entry<Integer, String> getPrefix() {
        return node.getPrefix();
    }

    @Override
    public boolean isSuffix() {
        return node.isSuffix();
    }

    @Override
    public Map.Entry<Integer, String> getSuffix() {
        return node.getSuffix();
    }

    @Override
    public boolean equalsIgnoringValue(Node other) {
        return node.equalsIgnoringValue(other);
    }

    @Override
    public boolean almostEquals(Node other) {
        return node.almostEquals(other);
    }

    @Override
    public boolean equalsIgnoringValueOrTemp(Node other) {
        return node.equalsIgnoringValueOrTemp(other);
    }
}
