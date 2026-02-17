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

package me.lucko.luckperms.common.command.access;

import com.google.common.collect.ImmutableSet;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class CommandPermissionTest {

    private static final ImmutableSet<CommandPermission> ALLOWED_READ_ONLY = ImmutableSet.of(
            CommandPermission.SYNC,
            CommandPermission.INFO,
            CommandPermission.EDITOR,
            CommandPermission.VERBOSE,
            CommandPermission.TREE,
            CommandPermission.SEARCH,
            CommandPermission.EXPORT,
            CommandPermission.RELOAD_CONFIG,
            CommandPermission.TRANSLATIONS,
            CommandPermission.LIST_GROUPS,
            CommandPermission.LIST_TRACKS,
            CommandPermission.USER_INFO,
            CommandPermission.USER_PERM_INFO,
            CommandPermission.USER_PERM_CHECK,
            CommandPermission.USER_PARENT_INFO,
            CommandPermission.USER_META_INFO,
            CommandPermission.USER_EDITOR,
            CommandPermission.USER_SHOW_TRACKS,
            CommandPermission.GROUP_INFO,
            CommandPermission.GROUP_PERM_INFO,
            CommandPermission.GROUP_PERM_CHECK,
            CommandPermission.GROUP_PARENT_INFO,
            CommandPermission.GROUP_META_INFO,
            CommandPermission.GROUP_EDITOR,
            CommandPermission.GROUP_LIST_MEMBERS,
            CommandPermission.GROUP_SHOW_TRACKS,
            CommandPermission.TRACK_INFO,
            CommandPermission.TRACK_EDITOR,
            CommandPermission.LOG_RECENT,
            CommandPermission.LOG_USER_HISTORY,
            CommandPermission.LOG_GROUP_HISTORY,
            CommandPermission.LOG_TRACK_HISTORY,
            CommandPermission.LOG_SEARCH,
            CommandPermission.LOG_NOTIFY,
            CommandPermission.SPONGE_PERMISSION_INFO,
            CommandPermission.SPONGE_PARENT_INFO,
            CommandPermission.SPONGE_OPTION_INFO
    );

    @ParameterizedTest
    @EnumSource(CommandPermission.class)
    public void testReadOnly(CommandPermission permission) {
        String name = permission.name();
        assertEquals(ALLOWED_READ_ONLY.contains(permission), permission.isReadOnly(), name);
    }

}
