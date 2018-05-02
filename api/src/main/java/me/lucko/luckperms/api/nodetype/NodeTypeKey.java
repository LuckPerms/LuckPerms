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

package me.lucko.luckperms.api.nodetype;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

import javax.annotation.Nonnull;

/**
 * Marks an instance used as a key for a {@link NodeType}.
 *
 * <p>A single instance of this interface is created and stored statically for
 * each sub-interface of {@link NodeType}.</p>
 *
 * @param <N> the type of the {@link NodeType} being indexed by this key
 * @since 4.2
 */
public interface NodeTypeKey<N extends NodeType> {

    /**
     * Gets the {@link Class#getSimpleName() class name} of the represented type.
     *
     * @return the name of the represented type
     */
    @Nonnull
    default String getTypeName() {
        ParameterizedType thisType = (ParameterizedType) getClass().getGenericSuperclass();
        Type nodeType = thisType.getActualTypeArguments()[0];
        return ((Class) nodeType).getSimpleName();
    }

}
