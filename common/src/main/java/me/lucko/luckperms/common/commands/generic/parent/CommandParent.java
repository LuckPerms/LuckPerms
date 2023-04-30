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

package me.lucko.luckperms.common.commands.generic.parent;

import com.google.common.collect.ImmutableList;
import me.lucko.luckperms.common.command.abstraction.GenericChildCommand;
import me.lucko.luckperms.common.command.abstraction.GenericParentCommand;
import me.lucko.luckperms.common.command.spec.CommandSpec;
import me.lucko.luckperms.common.model.HolderType;
import me.lucko.luckperms.common.model.PermissionHolder;

import java.util.Collections;

public class CommandParent<T extends PermissionHolder> extends GenericParentCommand<T> {
    public CommandParent(HolderType type) {
        super(CommandSpec.PARENT, "Parent", type, ImmutableList.<GenericChildCommand>builder()
                .add(new ParentInfo())
                .add(new ParentSet())
                .add(new ParentAdd())
                .add(new ParentRemove())
                .add(new ParentSetTrack())
                .add(new ParentAddTemp())
                .add(new ParentRemoveTemp())
                .add(new ParentClear())
                .add(new ParentClearTrack())
                .addAll(type == HolderType.USER ? Collections.singleton(new UserSwitchPrimaryGroup()) : Collections.emptySet())
                .build());
    }
}
