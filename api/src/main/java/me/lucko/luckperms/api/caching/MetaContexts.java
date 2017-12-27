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

import com.google.common.base.Preconditions;

import me.lucko.luckperms.api.Contexts;
import me.lucko.luckperms.api.metastacking.MetaStackDefinition;

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
        this.contexts = Preconditions.checkNotNull(contexts, "contexts");
        this.prefixStackDefinition = Preconditions.checkNotNull(prefixStackDefinition, "prefixStackDefinition");
        this.suffixStackDefinition = Preconditions.checkNotNull(suffixStackDefinition, "suffixStackDefinition");
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
        final MetaContexts other = (MetaContexts) o;
        return this.getContexts().equals(other.getContexts()) &&
                this.getPrefixStackDefinition().equals(other.getPrefixStackDefinition()) &&
                this.getSuffixStackDefinition().equals(other.getSuffixStackDefinition());
    }

    private int calculateHashCode() {
        final int PRIME = 59;
        int result = 1;
        result = result * PRIME + this.getContexts().hashCode();
        result = result * PRIME + this.getPrefixStackDefinition().hashCode();
        result = result * PRIME + this.getSuffixStackDefinition().hashCode();
        return result;
    }

    @Override
    public int hashCode() {
        return hashCode;
    }
}
