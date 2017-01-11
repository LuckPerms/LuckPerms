/*
 * Copyright (c) 2016 Lucko (Luck) <luck@lucko.me>
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

package me.lucko.luckperms.common.storage.holder;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;

import com.google.common.collect.Multimap;

import me.lucko.luckperms.api.Node;
import me.lucko.luckperms.common.storage.backing.utils.NodeDataHolder;

import java.util.Optional;
import java.util.OptionalLong;

@Getter
@EqualsAndHashCode
@AllArgsConstructor(staticName = "of")
public class NodeHeldPermission<T> implements HeldPermission<T> {
    public static <T> NodeHeldPermission<T> of(T holder, NodeDataHolder nodeDataHolder) {
        return of(holder, nodeDataHolder.toNode());
    }

    private final T holder;
    private final Node node;

    @Override
    public String getPermission() {
        return node.getPermission();
    }

    @Override
    public boolean getValue() {
        return node.getValue();
    }

    @Override
    public Optional<String> getServer() {
        return node.getServer();
    }

    @Override
    public Optional<String> getWorld() {
        return node.getWorld();
    }

    @Override
    public OptionalLong getExpiry() {
        return node.isTemporary() ? OptionalLong.of(node.getExpiryUnixTime()) : OptionalLong.empty();
    }

    @Override
    public Multimap<String, String> getContext() {
        return node.getContexts().toMultimap();
    }
}
