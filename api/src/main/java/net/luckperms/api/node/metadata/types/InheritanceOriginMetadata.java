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

package net.luckperms.api.node.metadata.types;

import net.luckperms.api.model.PermissionHolder;
import net.luckperms.api.node.Node;
import net.luckperms.api.node.metadata.NodeMetadataKey;

import org.checkerframework.checker.nullness.qual.NonNull;

/**
 * Node metadata indicating where a node was inherited from.
 */
public interface InheritanceOriginMetadata {

    /**
     * The {@link NodeMetadataKey} for {@link InheritanceOriginMetadata}.
     */
    NodeMetadataKey<InheritanceOriginMetadata> KEY = NodeMetadataKey.of("inheritanceorigin", InheritanceOriginMetadata.class);

    /**
     * Gets the location where the {@link Node} is inherited from.
     *
     * <p>The resultant string is the {@link PermissionHolder.Identifier#getName() object name} of the
     * permission holder the node was inherited from.</p>
     *
     * <p>If the node was not inherited, the {@link PermissionHolder.Identifier#getName() object name}
     * of the permission holder itself (the one that defined the node) will be returned.</p>
     *
     * @return where the node was inherited from.
     */
    PermissionHolder.@NonNull Identifier getOrigin();

}
