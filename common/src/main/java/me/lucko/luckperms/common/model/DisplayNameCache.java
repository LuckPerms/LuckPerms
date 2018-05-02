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

import me.lucko.luckperms.api.Node;
import me.lucko.luckperms.api.nodetype.types.DisplayNameType;
import me.lucko.luckperms.common.buffers.Cache;
import me.lucko.luckperms.common.config.ConfigKeys;

import java.util.Optional;

import javax.annotation.Nonnull;

/**
 * Cache instance to supply the display name of a {@link Group}.
 */
public class DisplayNameCache extends Cache<Optional<String>> {
    private final Group group;

    public DisplayNameCache(Group group) {
        this.group = group;
    }

    @Nonnull
    @Override
    protected Optional<String> supply() {
        // query for a displayname node
        for (Node n : this.group.getOwnNodes(this.group.getPlugin().getContextManager().getStaticContext())) {
            Optional<DisplayNameType> displayName = n.getTypeData(DisplayNameType.KEY);
            if (displayName.isPresent()) {
                return Optional.of(displayName.get().getDisplayName());
            }
        }

        // fallback to config
        String name = this.group.getPlugin().getConfiguration().get(ConfigKeys.GROUP_NAME_REWRITES).get(this.group.getObjectName());
        return name == null || name.equals(this.group.getObjectName()) ? Optional.empty() : Optional.of(name);
    }
}
