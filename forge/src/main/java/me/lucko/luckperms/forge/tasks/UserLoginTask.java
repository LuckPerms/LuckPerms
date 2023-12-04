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

package me.lucko.luckperms.forge.tasks;

import java.util.function.Consumer;

import com.mojang.authlib.GameProfile;

import me.lucko.luckperms.common.config.ConfigKeys;
import me.lucko.luckperms.common.locale.Message;
import me.lucko.luckperms.common.locale.TranslationManager;
import me.lucko.luckperms.common.model.User;
import me.lucko.luckperms.common.plugin.util.AbstractConnectionListener;
import me.lucko.luckperms.forge.ForgeSenderFactory;
import me.lucko.luckperms.forge.LPForgePlugin;
import net.kyori.adventure.text.Component;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.login.ClientboundLoginDisconnectPacket;
import net.minecraft.server.network.ConfigurationTask;
import net.minecraft.server.network.ConfigurationTask.Type;
import net.minecraft.server.network.ServerConfigurationPacketListenerImpl;
import net.minecraftforge.network.ChannelListManager;
import net.minecraftforge.network.config.ConfigurationTaskContext;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class UserLoginTask extends AbstractConnectionListener implements ConfigurationTask {
    private final LPForgePlugin plugin;
    private static final Type TYPE = new Type("luckperms:user_login");

    public UserLoginTask(LPForgePlugin plugin) {
        super(plugin);
        this.plugin = plugin;
    }

    @Override
    public void start(ConfigurationTaskContext ctx) {
        final GameProfile gameProfile = ((ServerConfigurationPacketListenerImpl) ctx.getConnection().getPacketListener()).getOwner();
        String username = gameProfile.getName();
        UUID uniqueId = gameProfile.getId();

        try {
            CompletableFuture.runAsync(() -> {
                onPlayerNegotiationAsync(ctx.getConnection(), uniqueId, username);
            }, this.plugin.getBootstrap().getScheduler().async()).get();
        } catch (Throwable ex) {
            this.plugin.getLogger().severe("Exception occurred while loading user data for " + uniqueId + " - " + username, ex.getCause());
        }
        ctx.finish(type());
    }

    private void onPlayerNegotiationAsync(Connection connection, UUID uniqueId, String username) {
        if (this.plugin.getConfiguration().get(ConfigKeys.DEBUG_LOGINS)) {
            this.plugin.getLogger().info("Processing pre-login (async phase) for " + uniqueId + " - " + username);
        }

        /* Actually process the login for the connection.
           We do this here to delay the login until the data is ready.
           If the login gets cancelled later on, then this will be cleaned up.

           This includes:
           - loading uuid data
           - loading permissions
           - creating a user instance in the UserManager for this connection.
           - setting up cached data. */
        try {
            User user = loadUser(uniqueId, username);
            recordConnection(uniqueId);
            this.plugin.getEventDispatcher().dispatchPlayerLoginProcess(uniqueId, username, user);
        } catch (Exception ex) {
            this.plugin.getLogger().severe("Exception occurred whilst loading data for " + uniqueId + " - " + username, ex);
            
            if (this.plugin.getConfiguration().get(ConfigKeys.CANCEL_FAILED_LOGINS)) {
                Component component = TranslationManager.render(Message.LOADING_DATABASE_ERROR.build());
                connection.send(new ClientboundLoginDisconnectPacket(ForgeSenderFactory.toNativeText(component)));
                connection.disconnect(ForgeSenderFactory.toNativeText(component));
                this.plugin.getEventDispatcher().dispatchPlayerLoginProcess(uniqueId, username, null);
            }
        }
    }

    @Override
    public void start(Consumer<Packet<?>> send) {
        throw new IllegalStateException("This should never be called");
    }

    @Override
    public Type type() {
        return TYPE;
    }
}
