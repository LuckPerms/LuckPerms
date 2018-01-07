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

package me.lucko.luckperms.api.caching;

import me.lucko.luckperms.api.Contexts;
import me.lucko.luckperms.api.metastacking.MetaStackDefinition;

import java.util.Objects;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.Immutable;

/**
 * Encapsulates the options and settings for a meta lookup.
 *
 * <p>Consists of a standard {@link Contexts} element, plus options to define how
 * the meta stack should be constructed.</p>
 *
 * @since 3.2
 */
@Immutable
public final class MetaContexts {

    /**
     * Creates a new meta contexts instance
     *
     * @param contexts the standard contexts for the query
     * @param prefixStackDefinition the prefix stack definition to be used
     * @param suffixStackDefinition the suffix stack definition to be used
     * @return the new instance
     */
    public static MetaContexts of(@Nonnull Contexts contexts, @Nonnull MetaStackDefinition prefixStackDefinition, @Nonnull MetaStackDefinition suffixStackDefinition) {
        return new MetaContexts(contexts, prefixStackDefinition, suffixStackDefinition);
    }

    private final Contexts contexts;
    private final MetaStackDefinition prefixStackDefinition;
    private final MetaStackDefinition suffixStackDefinition;

    // cache hashcode - this class is immutable, and is used as an index in the permission cache.
    private final int hashCode;

    /**
     * Creates a new meta contexts instance
     *
     * @param contexts the standard contexts for the query
     * @param prefixStackDefinition the prefix stack definition to be used
     * @param suffixStackDefinition the suffix stack definition to be used
     */
    public MetaContexts(@Nonnull Contexts contexts, @Nonnull MetaStackDefinition prefixStackDefinition, @Nonnull MetaStackDefinition suffixStackDefinition) {
        this.contexts = Objects.requireNonNull(contexts, "contexts");
        this.prefixStackDefinition = Objects.requireNonNull(prefixStackDefinition, "prefixStackDefinition");
        this.suffixStackDefinition = Objects.requireNonNull(suffixStackDefinition, "suffixStackDefinition");
        this.hashCode = calculateHashCode();
    }

    @Nonnull
    public Contexts getContexts() {
        return this.contexts;
    }

    @Nonnull
    public MetaStackDefinition getPrefixStackDefinition() {
        return this.prefixStackDefinition;
    }

    @Nonnull
    public MetaStackDefinition getSuffixStackDefinition() {
        return this.suffixStackDefinition;
    }

    @Override
    @Nonnull
    public String toString() {
        return "MetaContexts(" +
                "contexts=" + this.getContexts() + ", " +
                "prefixStackDefinition=" + this.getPrefixStackDefinition() + ", " +
                "suffixStackDefinition=" + this.getSuffixStackDefinition() +
                ")";
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) return true;
        if (!(o instanceof MetaContexts)) return false;
        final MetaContexts that = (MetaContexts) o;
        return this.contexts.equals(that.contexts) &&
                this.prefixStackDefinition.equals(that.prefixStackDefinition) &&
                this.suffixStackDefinition.equals(that.suffixStackDefinition);
    }

    private int calculateHashCode() {
        return Objects.hash(this.contexts, this.prefixStackDefinition, this.suffixStackDefinition);
    }

    @Override
    public int hashCode() {
        return this.hashCode;
    }
}
