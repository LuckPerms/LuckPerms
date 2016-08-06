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

package me.lucko.luckperms.groups;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import me.lucko.luckperms.LuckPermsPlugin;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@RequiredArgsConstructor
public class GroupManager {
    private final LuckPermsPlugin plugin;

    /**
     * A {@link Map} containing all loaded groups
     */
    @Getter
    private final Map<String, Group> groups = new ConcurrentHashMap<>();

    /**
     * Get a group object by name
     * @param name The name to search by
     * @return a {@link Group} object if the group is loaded, returns null if the group is not loaded
     */
    public Group getGroup(String name) {
        return groups.get(name);
    }

    /**
     * Add a group to the loaded groups map
     * @param group The group to add
     */
    public void setGroup(Group group) {
        groups.put(group.getName(), group);
    }

    /**
     * Updates (or sets if the group wasn't already loaded) a group in the groups map
     * @param group The group to update or set
     */
    public void updateOrSetGroup(Group group) {
        if (!isLoaded(group.getName())) {
            // The group isn't already loaded
            groups.put(group.getName(), group);
        } else {
            groups.get(group.getName()).setNodes(group.getNodes());
        }
    }

    /**
     * Check to see if a group is loaded or not
     * @param name The name of the group
     * @return true if the group is loaded
     */
    public boolean isLoaded(String name) {
        return groups.containsKey(name);
    }

    /**
     * Removes and unloads the group from the plugins internal storage
     * @param group The group to unload
     */
    public void unloadGroup(Group group) {
        if (group != null) {
            groups.remove(group.getName());
        }
    }

    /**
     * Unloads all groups from the manager
     */
    public void unloadAll() {
        groups.clear();
    }

    /**
     * Makes a new group object
     * @param name The name of the group
     * @return a new {@link Group} object
     */
    public Group makeGroup(String name) {
        return new Group(name, plugin);
    }
}