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

import lombok.Getter;
import lombok.NonNull;
import lombok.ToString;

import com.google.common.collect.ImmutableList;

import me.lucko.luckperms.api.metastacking.MetaStackDefinition;
import me.lucko.luckperms.api.metastacking.MetaStackElement;

import java.util.List;

@Getter
@ToString
public final class SimpleMetaStackDefinition implements MetaStackDefinition {

    private final List<MetaStackElement> elements;
    private final String startSpacer;
    private final String middleSpacer;
    private final String endSpacer;

    // cache hashcode - this class is immutable, and used an index in MetaContexts
    private final int hashCode;

    public SimpleMetaStackDefinition(@NonNull List<MetaStackElement> elements, @NonNull String startSpacer, @NonNull String middleSpacer, @NonNull String endSpacer) {
        this.elements = ImmutableList.copyOf(elements);
        this.startSpacer = startSpacer;
        this.middleSpacer = middleSpacer;
        this.endSpacer = endSpacer;
        this.hashCode = calculateHashCode();
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) return true;
        if (!(o instanceof SimpleMetaStackDefinition)) return false;
        final SimpleMetaStackDefinition other = (SimpleMetaStackDefinition) o;

        return this.getElements().equals(other.getElements()) &&
                this.getStartSpacer().equals(other.getStartSpacer()) &&
                this.getMiddleSpacer().equals(other.getMiddleSpacer()) &&
                this.getEndSpacer().equals(other.getEndSpacer());
    }

    private int calculateHashCode() {
        final int PRIME = 59;
        int result = 1;
        result = result * PRIME + this.getElements().hashCode();
        result = result * PRIME + this.getStartSpacer().hashCode();
        result = result * PRIME + this.getMiddleSpacer().hashCode();
        result = result * PRIME + this.getEndSpacer().hashCode();
        return result;
    }

    @Override
    public int hashCode() {
        return hashCode;
    }
}
