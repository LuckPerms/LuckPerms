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

package me.lucko.luckperms.nukkit.inject;

import cn.nukkit.permission.Permission;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Represents the possible default values for permissions
 */
public enum PermissionDefault {
    TRUE("true") {
        @Override
        public boolean getValue(boolean op) {
            return true;
        }
    },
    FALSE("false") {
        @Override
        public boolean getValue(boolean op) {
            return false;
        }
    },
    OP("op", "isop", "operator", "isoperator", "admin", "isadmin") {
        @Override
        public boolean getValue(boolean op) {
            return op;
        }
    },
    NOT_OP("!op", "notop", "!operator", "notoperator", "!admin", "notadmin") {
        @Override
        public boolean getValue(boolean op) {
            return !op;
        }
    };

    private final String[] names;
    private static final Map<String, PermissionDefault> LOOKUP = new HashMap<>();

    PermissionDefault(String... names) {
        this.names = names;
    }

    /**
     * Calculates the value of this PermissionDefault for the given operator
     * value
     *
     * @param op If the target is op
     * @return True if the default should be true, or false
     */
    public abstract boolean getValue(boolean op);

    /**
     * Looks up a PermissionDefault by name
     *
     * @param name Name of the default
     * @return Specified value, or null if not found
     */
    public static @Nullable PermissionDefault getByName(String name) {
        return LOOKUP.get(name.toLowerCase(Locale.ROOT).replaceAll("[^a-z!]", ""));
    }

    public static @Nullable PermissionDefault fromPermission(@Nullable Permission permission) {
        if (permission == null) {
            return null;
        }
        return getByName(permission.getDefault());
    }

    @Override
    public String toString() {
        return this.names[0];
    }

    static {
        for (PermissionDefault value : values()) {
            for (String name : value.names) {
                LOOKUP.put(name, value);
            }
        }
    }
}