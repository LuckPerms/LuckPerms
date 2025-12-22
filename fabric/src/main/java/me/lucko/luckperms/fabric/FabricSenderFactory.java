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

package me.lucko.luckperms.fabric;

import me.lucko.fabric.api.permissions.v0.Permissions;
import me.lucko.luckperms.common.minecraft.MinecraftSenderFactory;
import me.lucko.luckperms.fabric.mixin.CommandSourceStackAccessor;
import net.luckperms.api.util.Tristate;
import net.minecraft.commands.CommandSource;
import net.minecraft.commands.CommandSourceStack;

public class FabricSenderFactory extends MinecraftSenderFactory<LPFabricPlugin> {
    public FabricSenderFactory(LPFabricPlugin plugin) {
        super(plugin);
    }

    @Override
    protected CommandSource getSource(CommandSourceStack sender) {
        return ((CommandSourceStackAccessor) sender).getSource();
    }

    @Override
    protected Tristate getPermissionValue(CommandSourceStack commandSource, String node) {
        return switch (Permissions.getPermissionValue(commandSource, node)) {
            case TRUE -> Tristate.TRUE;
            case FALSE -> Tristate.FALSE;
            case DEFAULT -> Tristate.UNDEFINED;
        };
    }
}
