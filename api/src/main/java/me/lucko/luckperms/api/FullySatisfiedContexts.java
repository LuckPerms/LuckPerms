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

package me.lucko.luckperms.api;

import me.lucko.luckperms.api.caching.MetaContexts;
import me.lucko.luckperms.api.caching.UserData;
import me.lucko.luckperms.api.context.ContextSet;

import javax.annotation.Nonnull;

/**
 * A special instance of {@link Contexts}, which when passed to:
 *
 * <p></p>
 * <ul>
 *     <li>{@link UserData#getPermissionData(Contexts)}</li>
 *     <li>{@link UserData#getMetaData(Contexts)}</li>
 *     <li>{@link UserData#getMetaData(MetaContexts)}</li>
 * </ul>
 *
 * <p>... will always satisfy all contextual requirements.</p>
 *
 * <p>This effectively allows you to do lookups which ignore context.</p>
 *
 * @since 3.3
 */
public final class FullySatisfiedContexts extends Contexts {
    private static final FullySatisfiedContexts INSTANCE = new FullySatisfiedContexts();

    @Nonnull
    public static Contexts getInstance() {
        return INSTANCE;
    }

    private FullySatisfiedContexts() {
        super(ContextSet.empty(), true, true, true, true, true, false);
    }

    @Nonnull
    @Override
    public String toString() {
        return "FullySatisfiedContexts";
    }

    @Override
    public boolean equals(Object o) {
        return o == this;
    }

    @Override
    public int hashCode() {
        return System.identityHashCode(this);
    }
}
