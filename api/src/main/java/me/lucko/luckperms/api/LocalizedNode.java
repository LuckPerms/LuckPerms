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

import javax.annotation.Nonnull;
import javax.annotation.concurrent.Immutable;

/**
 * An extension of {@link Node}, providing information about
 * where the node originated from.
 *
 * @since 2.11
 */
@Immutable
public interface LocalizedNode extends Node {

    /**
     * Gets the delegate node.
     *
     * <p>Result is never another {@link LocalizedNode} instance.</p>
     *
     * @return the node this instance is representing
     */
    @Nonnull
    Node getNode();

    /**
     * Gets the location where the {@link Node} is inherited from.
     *
     * <p>The resultant string is the {@link PermissionHolder#getObjectName() object name} of the
     * permission holder the node was inherited from.</p>
     *
     * <p>If the node was not inherited, the {@link PermissionHolder#getObjectName() object name}
     * of the permission holder itself (the one that defined the node) will be returned.</p>
     *
     * @return where the node was inherited from.
     */
    @Nonnull
    String getLocation();

}
