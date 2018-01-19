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

import me.lucko.luckperms.api.Node;
import me.lucko.luckperms.api.Tristate;
import me.lucko.luckperms.api.context.ContextSet;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import javax.annotation.Nonnull;

/**
 * Holds a Node and plus an owning object. All calls are passed onto the contained Node instance.
 */
public final class ImmutableTransientNode implements Node {
    public static ImmutableTransientNode of(Node node, Object owner) {
        Objects.requireNonNull(node, "node");
        Objects.requireNonNull(owner, "owner");
        return new ImmutableTransientNode(node, owner);
    }

    private final Node node;
    private final Object owner;

    private ImmutableTransientNode(Node node, Object owner) {
        this.node = node;
        this.owner = owner;
    }

    @Override
    public int hashCode() {
        return this.node.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        return this == obj || this.node.equals(obj);
    }

    public Node getNode() {
        return this.node;
    }

    public Object getOwner() {
        return this.owner;
    }

    @Nonnull
    @Override
    public Optional<String> getServer() {
        return this.node.getServer();
    }

    @Override
    public boolean getValuePrimitive() {
        return this.node.getValuePrimitive();
    }

    @Override
    public String getKey() {
        return this.node.getKey();
    }

    @Nonnull
    @Override
    public Map.Entry<String, String> getMeta() throws IllegalStateException {
        return this.node.getMeta();
    }

    @Override
    public boolean isServerSpecific() {
        return this.node.isServerSpecific();
    }

    @Nonnull
    @Override
    public Tristate getTristate() {
        return this.node.getTristate();
    }

    @Override
    public boolean hasExpired() {
        return this.node.hasExpired();
    }

    @Override
    public boolean isWildcard() {
        return this.node.isWildcard();
    }

    @Override
    public boolean equalsIgnoringValueOrTemp(@Nonnull Node other) {
        return this.node.equalsIgnoringValueOrTemp(other);
    }

    @Nonnull
    @Override
    public List<String> resolveShorthand() {
        return this.node.resolveShorthand();
    }

    @Override
    public boolean almostEquals(@Nonnull Node other) {
        return this.node.almostEquals(other);
    }

    @Nonnull
    @Override
    public ContextSet getFullContexts() {
        return this.node.getFullContexts();
    }

    @Override
    public long getSecondsTilExpiry() throws IllegalStateException {
        return this.node.getSecondsTilExpiry();
    }

    @Nonnull
    @Override
    public String getPermission() {
        return this.node.getPermission();
    }

    @Nonnull
    @Override
    public Map.Entry<Integer, String> getSuffix() throws IllegalStateException {
        return this.node.getSuffix();
    }

    @Override
    public boolean isWorldSpecific() {
        return this.node.isWorldSpecific();
    }

    @Override
    public boolean equalsIgnoringValue(@Nonnull Node other) {
        return this.node.equalsIgnoringValue(other);
    }

    @Override
    public long getExpiryUnixTime() throws IllegalStateException {
        return this.node.getExpiryUnixTime();
    }

    @Override
    public boolean isGroupNode() {
        return this.node.isGroupNode();
    }

    @Override
    public Boolean setValue(Boolean value) {
        return this.node.setValue(value);
    }

    @Override
    public boolean isPrefix() {
        return this.node.isPrefix();
    }

    @Nonnull
    @Override
    public String getGroupName() throws IllegalStateException {
        return this.node.getGroupName();
    }

    @Nonnull
    @Override
    public Date getExpiry() throws IllegalStateException {
        return this.node.getExpiry();
    }

    @Override
    public boolean isNegated() {
        return this.node.isNegated();
    }

    @Override
    public boolean hasSpecificContext() {
        return this.node.hasSpecificContext();
    }

    @Override
    public int getWildcardLevel() throws IllegalStateException {
        return this.node.getWildcardLevel();
    }

    @Override
    public boolean isTemporary() {
        return this.node.isTemporary();
    }

    @Nonnull
    @Override
    public Map.Entry<Integer, String> getPrefix() throws IllegalStateException {
        return this.node.getPrefix();
    }

    @Override
    public boolean isMeta() {
        return this.node.isMeta();
    }

    @Override
    public boolean isPermanent() {
        return this.node.isPermanent();
    }

    @Nonnull
    @Override
    public Optional<String> getWorld() {
        return this.node.getWorld();
    }

    @Nonnull
    @Override
    public Boolean getValue() {
        return this.node.getValue();
    }

    @Override
    public boolean isOverride() {
        return this.node.isOverride();
    }

    @Override
    public boolean isSuffix() {
        return this.node.isSuffix();
    }

    @Nonnull
    @Override
    public ContextSet getContexts() {
        return this.node.getContexts();
    }

    @Override
    public boolean appliesGlobally() {
        return this.node.appliesGlobally();
    }

    @Override
    public boolean shouldApplyWithContext(@Nonnull ContextSet context) {
        return this.node.shouldApplyWithContext(context);
    }

    @Override
    public String toString() {
        return "ImmutableTransientNode(node=" + this.getNode() + ", owner=" + this.getOwner() + ")";
    }
}
