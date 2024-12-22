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

package me.lucko.luckperms.common.cacheddata.metastack;

import com.google.common.collect.ImmutableList;
import net.luckperms.api.metastacking.DuplicateRemovalFunction;
import net.luckperms.api.metastacking.MetaStackDefinition;
import net.luckperms.api.metastacking.MetaStackElement;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.util.List;
import java.util.Objects;

public final class SimpleMetaStackDefinition implements MetaStackDefinition {

    private final List<MetaStackElement> elements;
    private final DuplicateRemovalFunction duplicateRemovalFunction;
    private final String startSpacer;
    private final String middleSpacer;
    private final String endSpacer;

    // cache hashcode - this class is immutable, and used an index in MetaContexts
    private final int hashCode;

    public SimpleMetaStackDefinition(List<MetaStackElement> elements, DuplicateRemovalFunction duplicateRemovalFunction, String startSpacer, String middleSpacer, String endSpacer) {
        this.elements = ImmutableList.copyOf(Objects.requireNonNull(elements, "elements"));
        this.duplicateRemovalFunction = Objects.requireNonNull(duplicateRemovalFunction, "duplicateRemovalFunction");
        this.startSpacer = Objects.requireNonNull(startSpacer, "startSpacer");
        this.middleSpacer = Objects.requireNonNull(middleSpacer, "middleSpacer");
        this.endSpacer = Objects.requireNonNull(endSpacer, "endSpacer");
        this.hashCode = calculateHashCode();
    }

    @Override
    public @NonNull List<MetaStackElement> getElements() {
        return this.elements;
    }

    @Override
    public @NonNull DuplicateRemovalFunction getDuplicateRemovalFunction() {
        return this.duplicateRemovalFunction;
    }

    @Override
    public @NonNull String getStartSpacer() {
        return this.startSpacer;
    }

    @Override
    public @NonNull String getMiddleSpacer() {
        return this.middleSpacer;
    }

    @Override
    public @NonNull String getEndSpacer() {
        return this.endSpacer;
    }

    public int getHashCode() {
        return this.hashCode;
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) return true;
        if (!(o instanceof SimpleMetaStackDefinition)) return false;
        final SimpleMetaStackDefinition that = (SimpleMetaStackDefinition) o;

        return this.elements.equals(that.elements) &&
                this.duplicateRemovalFunction.equals(that.duplicateRemovalFunction) &&
                this.startSpacer.equals(that.startSpacer) &&
                this.middleSpacer.equals(that.middleSpacer) &&
                this.endSpacer.equals(that.endSpacer);
    }

    private int calculateHashCode() {
        return Objects.hash(this.elements, this.duplicateRemovalFunction, this.startSpacer, this.middleSpacer, this.elements);
    }

    @Override
    public int hashCode() {
        return this.hashCode;
    }

    @Override
    public String toString() {
        return "SimpleMetaStackDefinition(" +
                "elements=" + this.elements + ", " +
                "duplicateRemovalFunction=" + this.duplicateRemovalFunction + ", " +
                "startSpacer=" + this.startSpacer + ", " +
                "middleSpacer=" + this.middleSpacer + ", " +
                "endSpacer=" + this.endSpacer + ")";
    }
}
