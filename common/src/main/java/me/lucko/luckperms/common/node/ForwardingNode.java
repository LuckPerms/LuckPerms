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
import me.lucko.luckperms.api.StandardNodeEquality;
import me.lucko.luckperms.api.Tristate;
import me.lucko.luckperms.api.context.ContextSet;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.annotation.Nonnull;

public abstract class ForwardingNode implements Node {

    protected abstract Node delegate();

    @Override
    public int hashCode() {
        return delegate().hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        return this == obj || delegate().equals(obj);
    }

    @Nonnull
    @Override
    public String getPermission() {
        return delegate().getPermission();
    }

    @Nonnull
    @Override
    public Boolean getValue() {
        return delegate().getValue();
    }

    @Override
    public boolean getValuePrimitive() {
        return delegate().getValuePrimitive();
    }

    @Nonnull
    @Override
    public Tristate getTristate() {
        return delegate().getTristate();
    }

    @Override
    public boolean isNegated() {
        return delegate().isNegated();
    }

    @Override
    public boolean isOverride() {
        return delegate().isOverride();
    }

    @Nonnull
    @Override
    public Optional<String> getServer() {
        return delegate().getServer();
    }

    @Nonnull
    @Override
    public Optional<String> getWorld() {
        return delegate().getWorld();
    }

    @Override
    public boolean isServerSpecific() {
        return delegate().isServerSpecific();
    }

    @Override
    public boolean isWorldSpecific() {
        return delegate().isWorldSpecific();
    }

    @Override
    public boolean appliesGlobally() {
        return delegate().appliesGlobally();
    }

    @Override
    public boolean hasSpecificContext() {
        return delegate().hasSpecificContext();
    }

    @Override
    public boolean shouldApplyWithContext(@Nonnull ContextSet context) {
        return delegate().shouldApplyWithContext(context);
    }

    @Nonnull
    @Override
    public List<String> resolveShorthand() {
        return delegate().resolveShorthand();
    }

    @Override
    public boolean isTemporary() {
        return delegate().isTemporary();
    }

    @Override
    public boolean isPermanent() {
        return delegate().isPermanent();
    }

    @Override
    public long getExpiryUnixTime() throws IllegalStateException {
        return delegate().getExpiryUnixTime();
    }

    @Nonnull
    @Override
    public Date getExpiry() throws IllegalStateException {
        return delegate().getExpiry();
    }

    @Override
    public long getSecondsTilExpiry() throws IllegalStateException {
        return delegate().getSecondsTilExpiry();
    }

    @Override
    public boolean hasExpired() {
        return delegate().hasExpired();
    }

    @Nonnull
    @Override
    public ContextSet getContexts() {
        return delegate().getContexts();
    }

    @Nonnull
    @Override
    public ContextSet getFullContexts() {
        return delegate().getFullContexts();
    }

    @Override
    public boolean isGroupNode() {
        return delegate().isGroupNode();
    }

    @Nonnull
    @Override
    public String getGroupName() throws IllegalStateException {
        return delegate().getGroupName();
    }

    @Override
    public boolean isWildcard() {
        return delegate().isWildcard();
    }

    @Override
    public int getWildcardLevel() throws IllegalStateException {
        return delegate().getWildcardLevel();
    }

    @Override
    public boolean isMeta() {
        return delegate().isMeta();
    }

    @Nonnull
    @Override
    public Map.Entry<String, String> getMeta() throws IllegalStateException {
        return delegate().getMeta();
    }

    @Override
    public boolean isPrefix() {
        return delegate().isPrefix();
    }

    @Nonnull
    @Override
    public Map.Entry<Integer, String> getPrefix() throws IllegalStateException {
        return delegate().getPrefix();
    }

    @Override
    public boolean isSuffix() {
        return delegate().isSuffix();
    }

    @Nonnull
    @Override
    public Map.Entry<Integer, String> getSuffix() throws IllegalStateException {
        return delegate().getSuffix();
    }

    @Override
    public boolean standardEquals(Node other, StandardNodeEquality equalityPredicate) {
        return delegate().standardEquals(other, equalityPredicate);
    }

    @Override
    public Builder toBuilder() {
        return delegate().toBuilder();
    }
}
