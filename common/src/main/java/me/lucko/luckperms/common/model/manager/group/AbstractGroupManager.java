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

package me.lucko.luckperms.common.model.manager.group;

import me.lucko.luckperms.common.model.Group;
import me.lucko.luckperms.common.model.manager.AbstractManager;

import java.util.Optional;

public abstract class AbstractGroupManager<T extends Group> extends AbstractManager<String, Group, T> implements GroupManager<T> {

    @Override
    public T getByDisplayName(String name) {
        // try to get an exact match first
        T g = getIfLoaded(name);
        if (g != null) {
            return g;
        }

        // then try exact display name matches
        for (T group : getAll().values()) {
            Optional<String> displayName = group.getDisplayName();
            if (displayName.isPresent() && displayName.get().equals(name)) {
                return group;
            }
        }

        // then try case insensitive name matches
        for (T group : getAll().values()) {
            Optional<String> displayName = group.getDisplayName();
            if (displayName.isPresent() && displayName.get().equalsIgnoreCase(name)) {
                return group;
            }
        }

        return null;
    }

    @Override
    protected String sanitizeIdentifier(String s) {
        return s.toLowerCase();
    }

    @Override
    public void invalidateAllGroupCaches() {
        getAll().values().forEach(g -> g.getCachedData().invalidate());
    }

    @Override
    public void invalidateAllPermissionCalculators() {
        getAll().values().forEach(g -> g.getCachedData().invalidatePermissionCalculators());
    }
}