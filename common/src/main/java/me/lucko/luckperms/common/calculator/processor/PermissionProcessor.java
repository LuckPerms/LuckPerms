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

package me.lucko.luckperms.common.calculator.processor;

import me.lucko.luckperms.common.calculator.PermissionCalculator;
import me.lucko.luckperms.common.calculator.result.TristateResult;

import java.util.Map;

/**
 * A processor within a {@link PermissionCalculator}.
 *
 * <p>Processors should not implement any sort of caching. This is handled in
 * the parent calculator.</p>
 */
public interface PermissionProcessor {

    /**
     * Returns the permission value determined by this calculator.
     *
     * @param prev the result of the previous calculator in the chain
     * @param permission the permission
     * @return a tristate
     */
    TristateResult hasPermission(TristateResult prev, String permission);

    /**
     * Sets the source permissions which should be used by this processor
     *
     * @param sourceMap the source map
     */
    default void setSource(Map<String, Boolean> sourceMap) {

    }

    /**
     * Called after a change has been made to the source map
     */
    default void refresh() {

    }

    /**
     * Called after the parent calculator has been invalidated
     */
    default void invalidate() {

    }

}
