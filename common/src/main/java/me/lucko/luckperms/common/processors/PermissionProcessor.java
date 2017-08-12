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

package me.lucko.luckperms.common.processors;

import me.lucko.luckperms.api.Tristate;

import java.util.Map;

/**
 * A processor within a {@link me.lucko.luckperms.common.calculators.PermissionCalculator}.
 */
public interface PermissionProcessor {

    /**
     * Returns the permission value determined by this calculator.
     *
     * @param permission the permission
     * @return a tristate
     */
    Tristate hasPermission(String permission);

    /**
     * Called each time the permission calculators backing is updated.
     *
     * <p>Note that the same map instance is passed to this method on each update,
     * so, if no processing is needed on the backing map, this can be cached on
     * the first invocation.</p>
     *
     * @param map the map
     */
    void updateBacking(Map<String, Boolean> map);

}
