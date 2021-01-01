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
import me.lucko.luckperms.common.locale.TranslationManager;
import me.lucko.luckperms.common.sender.Sender;
import me.lucko.luckperms.common.sender.SenderFactory;
import me.lucko.luckperms.fabric.model.MixinUser;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import net.luckperms.api.util.Tristate;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import java.util.Locale;
import java.util.UUID;

public class FabricSenderFactory extends SenderFactory<LPFabricPlugin, ServerCommandSource> {
    private final LPFabricPlugin plugin;

    public FabricSenderFactory(LPFabricPlugin plugin) {
        super(plugin);
        this.plugin = plugin;
    }

    @Override
    protected LPFabricPlugin getPlugin() {
        return this.plugin;
    }

    @Override
    protected UUID getUniqueId(ServerCommandSource commandSource) {
        if (commandSource.getEntity() != null) {
            return commandSource.getEntity().getUuid();
        }
        return Sender.CONSOLE_UUID;
    }

    @Override
    protected String getName(ServerCommandSource commandSource) {
        String name = commandSource.getName();
        if (commandSource.getEntity() != null && name.equals("Server")) {
            return Sender.CONSOLE_NAME;
        }
        return name;
    }

    @Override
    protected void sendMessage(ServerCommandSource sender, Component message) {
        Locale locale = null;
        if (sender.getEntity() instanceof ServerPlayerEntity) {
            locale = ((MixinUser) sender.getEntity()).getCachedLocale();
        }
        sender.sendFeedback(toNativeText(TranslationManager.render(message, locale)), false);
    }

    @Override
    protected Tristate getPermissionValue(ServerCommandSource commandSource, String node) {
        switch (Permissions.getPermissionValue(commandSource, node)) {
            case TRUE:
                return Tristate.TRUE;
            case FALSE:
                return Tristate.FALSE;
            case DEFAULT:
                return Tristate.UNDEFINED;
            default:
                throw new AssertionError();
        }
    }

    @Override
    protected boolean hasPermission(ServerCommandSource commandSource, String node) {
        return getPermissionValue(commandSource, node).asBoolean();
    }

    @Override
    protected void performCommand(ServerCommandSource sender, String command) {
        sender.getMinecraftServer().getCommandManager().execute(sender, command);
    }

    public static Text toNativeText(Component component) {
        return Text.Serializer.fromJson(GsonComponentSerializer.gson().serialize(component));
    }
}
