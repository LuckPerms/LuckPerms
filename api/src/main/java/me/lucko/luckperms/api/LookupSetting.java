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

import java.util.EnumSet;
import java.util.Set;

/**
 * The various lookup setting flags for {@link Contexts}.
 *
 * @since 4.2
 */
public enum LookupSetting {

    /**
     * If the target subject is OP
     */
    IS_OP,

    /**
     * If global or non-server-specific nodes should be applied
     */
    INCLUDE_NODES_SET_WITHOUT_SERVER,

    /**
     * If global or non-world-specific nodes should be applied
     */
    INCLUDE_NODES_SET_WITHOUT_WORLD,

    /**
     * If parent groups should be resolved
     */
    RESOLVE_INHERITANCE,

    /**
     * If global or non-server-specific group memberships should be applied
     */
    APPLY_PARENTS_SET_WITHOUT_SERVER,

    /**
     * If global or non-world-specific group memberships should be applied
     */
    APPLY_PARENTS_SET_WITHOUT_WORLD;



    /* bitwise utility methods */

    static boolean isSet(byte b, LookupSetting setting) {
        return ((b >> setting.ordinal()) & 1) == 1;
    }

    static byte createFlag(LookupSetting... settings) {
        byte b = 0;
        for (LookupSetting setting : settings) {
            b |= (1 << setting.ordinal());
        }
        return b;
    }

    static byte createFlag(Set<LookupSetting> settings) {
        byte b = 0;
        for (LookupSetting setting : settings) {
            b |= (1 << setting.ordinal());
        }
        return b;
    }

    static Set<LookupSetting> createSetFromFlag(byte b) {
        EnumSet<LookupSetting> settings = EnumSet.noneOf(LookupSetting.class);
        for (LookupSetting setting : LookupSetting.values()) {
            if (((b >> setting.ordinal()) & 1) == 1) {
                settings.add(setting);
            }
        }
        return settings;
    }

    static byte createFlag(boolean includeNodesSetWithoutServer, boolean includeNodesSetWithoutWorld, boolean resolveInheritance, boolean applyParentsWithoutServer, boolean applyParentsWithoutWorld, boolean isOp) {
        byte b = 0;
        if (includeNodesSetWithoutServer) {
            b |= (1 << LookupSetting.INCLUDE_NODES_SET_WITHOUT_SERVER.ordinal());
        }
        if (includeNodesSetWithoutWorld) {
            b |= (1 << LookupSetting.INCLUDE_NODES_SET_WITHOUT_WORLD.ordinal());
        }
        if (resolveInheritance) {
            b |= (1 << LookupSetting.RESOLVE_INHERITANCE.ordinal());
        }
        if (applyParentsWithoutServer) {
            b |= (1 << LookupSetting.APPLY_PARENTS_SET_WITHOUT_SERVER.ordinal());
        }
        if (applyParentsWithoutWorld) {
            b |= (1 << LookupSetting.APPLY_PARENTS_SET_WITHOUT_WORLD.ordinal());
        }
        if (isOp) {
            b |= (1 << LookupSetting.IS_OP.ordinal());
        }
        return b;
    }
}
