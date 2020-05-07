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

package me.lucko.luckperms.common.node.matcher;

import me.lucko.luckperms.common.bulkupdate.comparison.Constraint;

import net.luckperms.api.node.Node;
import net.luckperms.api.node.matcher.NodeMatcher;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Abstract implementation of {@link NodeMatcher} backed by a {@link Constraint}.
 */
public abstract class ConstraintNodeMatcher<T extends Node> implements NodeMatcher<T> {
    private final Constraint constraint;

    protected ConstraintNodeMatcher(Constraint constraint) {
        this.constraint = constraint;
    }

    public Constraint getConstraint() {
        return this.constraint;
    }

    public abstract @Nullable T filterConstraintMatch(@NonNull Node node);

    public @Nullable T match(Node node) {
        return getConstraint().eval(node.getKey()) ? filterConstraintMatch(node) : null;
    }

    @Override
    public boolean test(@NonNull Node node) {
        return match(node) != null;
    }
}
