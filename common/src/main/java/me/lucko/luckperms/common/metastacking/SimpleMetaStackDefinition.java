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

package me.lucko.luckperms.common.metastacking;

import com.google.common.collect.ImmutableList;

import me.lucko.luckperms.api.metastacking.MetaStackDefinition;
import me.lucko.luckperms.api.metastacking.MetaStackElement;

import org.checkerframework.checker.nullness.qual.NonNull;

import java.util.List;
import java.util.Objects;

public final class SimpleMetaStackDefinition implements MetaStackDefinition {

    private final List<MetaStackElement> elements;
    private final String startSpacer;
    private final String middleSpacer;
    private final String endSpacer;

    // cache hashcode - this class is immutable, and used an index in MetaContexts
    private final int hashCode;

    public SimpleMetaStackDefinition(List<MetaStackElement> elements, String startSpacer, String middleSpacer, String endSpacer) {
        this.elements = ImmutableList.copyOf(Objects.requireNonNull(elements, "elements"));
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

        return this.getElements().equals(that.getElements()) &&
                this.getStartSpacer().equals(that.getStartSpacer()) &&
                this.getMiddleSpacer().equals(that.getMiddleSpacer()) &&
                this.getEndSpacer().equals(that.getEndSpacer());
    }

    private int calculateHashCode() {
        return Objects.hash(getElements(), getStartSpacer(), getMiddleSpacer(), getEndSpacer());
    }

    @Override
    public int hashCode() {
        return this.hashCode;
    }

    @Override
    public String toString() {
        return "SimpleMetaStackDefinition(elements=" + this.getElements() + ", startSpacer=" + this.getStartSpacer() + ", middleSpacer=" + this.getMiddleSpacer() + ", endSpacer=" + this.getEndSpacer() + ", hashCode=" + this.getHashCode() + ")";
    }
}
