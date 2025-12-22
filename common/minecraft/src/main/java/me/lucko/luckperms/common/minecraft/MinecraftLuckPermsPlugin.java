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

package me.lucko.luckperms.common.minecraft;

import me.lucko.luckperms.common.calculator.CalculatorFactory;
import me.lucko.luckperms.common.locale.TranslationManager;
import me.lucko.luckperms.common.minecraft.calculator.MinecraftCalculatorFactory;
import me.lucko.luckperms.common.minecraft.context.MinecraftContextManager;
import me.lucko.luckperms.common.model.User;
import me.lucko.luckperms.common.model.manager.group.StandardGroupManager;
import me.lucko.luckperms.common.model.manager.track.StandardTrackManager;
import me.lucko.luckperms.common.model.manager.user.StandardUserManager;
import me.lucko.luckperms.common.plugin.AbstractLuckPermsPlugin;
import me.lucko.luckperms.common.sender.DummyConsoleSender;
import me.lucko.luckperms.common.sender.Sender;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.luckperms.api.query.QueryOptions;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.players.PlayerList;

import java.util.Optional;
import java.util.stream.Stream;

public abstract class MinecraftLuckPermsPlugin<P extends MinecraftLuckPermsPlugin<P, B>, B extends MinecraftLuckPermsBootstrap> extends AbstractLuckPermsPlugin {
    protected final B bootstrap;

    private StandardUserManager userManager;
    private StandardGroupManager groupManager;
    private StandardTrackManager trackManager;

    protected MinecraftLuckPermsPlugin(B bootstrap) {
        this.bootstrap = bootstrap;
    }

    @Override
    public final B getBootstrap() {
        return this.bootstrap;
    }

    @Override
    public abstract MinecraftContextManager getContextManager();

    public abstract MinecraftSenderFactory<P> getSenderFactory();

    @Override
    protected void setupManagers() {
        this.userManager = new StandardUserManager(this);
        this.groupManager = new StandardGroupManager(this);
        this.trackManager = new StandardTrackManager(this);
    }

    @Override
    protected CalculatorFactory provideCalculatorFactory() {
        return new MinecraftCalculatorFactory(this);
    }

    @Override
    public final Optional<QueryOptions> getQueryOptionsForUser(User user) {
        return this.bootstrap.getPlayer(user.getUniqueId()).map(player -> getContextManager().getQueryOptions(player));
    }

    @Override
    public final Stream<Sender> getOnlineSenders() {
        return Stream.concat(
                Stream.of(getConsoleSender()),
                this.bootstrap.getServer()
                        .map(MinecraftServer::getPlayerList)
                        .map(PlayerList::getPlayers)
                        .stream()
                        .flatMap(players -> players.stream()
                                .map(player -> getSenderFactory().wrap(player.createCommandSourceStack()))
                        )
        );
    }

    @Override
    public final Sender getConsoleSender() {
        return this.bootstrap.getServer()
                .map(server -> getSenderFactory().wrap(server.createCommandSourceStack()))
                .orElseGet(() -> new DummyConsoleSender(this) {
                    @Override
                    public void sendMessage(Component message) {
                        MinecraftLuckPermsPlugin.this.bootstrap.getPluginLogger().info(PlainTextComponentSerializer.plainText().serialize(TranslationManager.render(message)));
                    }
                });
    }

    @Override
    public StandardUserManager getUserManager() {
        return this.userManager;
    }

    @Override
    public StandardGroupManager getGroupManager() {
        return this.groupManager;
    }

    @Override
    public StandardTrackManager getTrackManager() {
        return this.trackManager;
    }

}
