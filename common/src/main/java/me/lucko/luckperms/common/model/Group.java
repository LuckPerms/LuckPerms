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

package me.lucko.luckperms.common.model;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import me.lucko.luckperms.common.api.delegates.GroupDelegate;
import me.lucko.luckperms.common.config.ConfigKeys;
import me.lucko.luckperms.common.plugin.LuckPermsPlugin;
import me.lucko.luckperms.common.references.GroupReference;
import me.lucko.luckperms.common.references.Identifiable;

@ToString(of = {"name"})
@EqualsAndHashCode(of = {"name"}, callSuper = false)
public class Group extends PermissionHolder implements Identifiable<String> {

    /**
     * The name of the group
     */
    @Getter
    private final String name;

    @Getter
    private final GroupDelegate delegate = new GroupDelegate(this);

    public Group(String name, LuckPermsPlugin plugin) {
        super(name, plugin);
        this.name = name;
    }

    @Override
    public String getId() {
        return name.toLowerCase();
    }

    public String getRawDisplayName() {
        return getPlugin().getConfiguration().get(ConfigKeys.GROUP_NAME_REWRITES).getOrDefault(name, name);
    }

    public String getDisplayName() {
        String dn = getRawDisplayName();
        return dn.equals(name) ? name : name + " (" + dn + ")";
    }

    @Override
    public String getFriendlyName() {
        return getDisplayName();
    }

    @Override
    public GroupReference toReference() {
        return GroupReference.of(getId());
    }
}
