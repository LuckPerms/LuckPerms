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
import com.google.common.collect.ImmutableMap;
import net.luckperms.api.metastacking.MetaStackElement;
import net.luckperms.api.node.ChatMetaType;
import net.luckperms.api.node.types.ChatMetaNode;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public final class FluentMetaStackElement implements MetaStackElement {

    public static Builder builder(String name) {
        return new Builder(name);
    }

    private final List<MetaStackElement> subElements;
    private final String toString;

    private FluentMetaStackElement(String name, Map<String, String> params, List<MetaStackElement> subElements) {
        this.subElements = ImmutableList.copyOf(subElements);
        this.toString = formToString(name, params);
    }

    @Override
    public boolean shouldAccumulate(@NonNull ChatMetaType type, @NonNull ChatMetaNode<?, ?> node, @Nullable ChatMetaNode<?, ?> current) {
        for (MetaStackElement element : this.subElements) {
            if (!element.shouldAccumulate(type, node, current)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FluentMetaStackElement that = (FluentMetaStackElement) o;
        return this.subElements.equals(that.subElements);
    }

    @Override
    public int hashCode() {
        return this.subElements.hashCode();
    }

    @Override
    public String toString() {
        return this.toString;
    }

    private static String formToString(String name, Map<String, String> params) {
        return name + "(" + params.entrySet().stream().map(p -> p.getKey() + "=" + p.getValue()).collect(Collectors.joining(", ")) + ")";
    }

    public static final class Builder {
        private final String name;
        private final ImmutableMap.Builder<String, String> params = ImmutableMap.builder();
        private final ImmutableList.Builder<MetaStackElement> elements = ImmutableList.builder();

        Builder(String name) {
            this.name = name;
        }

        public Builder with(MetaStackElement check) {
            this.elements.add(check);
            return this;
        }

        public Builder param(String name, String value) {
            this.params.put(name, value);
            return this;
        }

        public MetaStackElement build() {
            return new FluentMetaStackElement(this.name, this.params.build(), this.elements.build());
        }
    }
}

