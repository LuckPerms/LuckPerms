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

package me.lucko.luckperms.fabric.placeholder;

import eu.pb4.placeholders.api.ParserContext;
import eu.pb4.placeholders.api.PlaceholderResult;
import eu.pb4.placeholders.api.Placeholders;
import eu.pb4.placeholders.api.ServerPlaceholderContext;
import eu.pb4.placeholders.api.parsers.NodeParser;
import me.lucko.luckperms.common.model.User;
import me.lucko.luckperms.common.placeholders.Placeholder;
import me.lucko.luckperms.common.placeholders.PlaceholderContext;
import me.lucko.luckperms.common.placeholders.PlaceholderRegistry;
import me.lucko.luckperms.fabric.LPFabricPlugin;
import net.luckperms.api.query.QueryOptions;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;

public class FabricPlaceholderApiIntegration {
    private final LPFabricPlugin plugin;

    public FabricPlaceholderApiIntegration(LPFabricPlugin plugin) {
        this.plugin = plugin;
    }

    public void register() {
        for (Placeholder placeholder : PlaceholderRegistry.getAll()) {
            Placeholders.registerServer(
                    Identifier.fromNamespaceAndPath("luckperms", placeholder.id()),
                    new Handler(this.plugin, placeholder)
            );
        }
    }

    private static final NodeParser NODE_PARSER = NodeParser.builder().legacyAll().simplifiedTextFormat().quickText().build();

    private record Handler(LPFabricPlugin plugin, Placeholder placeholder) implements eu.pb4.placeholders.api.Placeholder.Handler<ServerPlaceholderContext, String> {
        @Override
        public PlaceholderResult onPlaceholderRequest(ServerPlaceholderContext context, String argument) {
            ServerPlayer player = context.serverPlayer();
            User user = this.plugin.getUserManager().getIfLoaded(player.getUUID());
            if (user == null) {
                return PlaceholderResult.invalid("Unable to find corresponding user for UUID: " + player.getUUID());
            }

            QueryOptions queryOptions = this.plugin.getContextManager().getQueryOptions(player);
            PlaceholderContext ctx = new PlaceholderContext(this.plugin.getApiProvider(), user.getApiProxy(), queryOptions);

            String result;
            if (this.placeholder instanceof Placeholder.Basic basic) {
                result = basic.resolve(ctx);
            } else if (this.placeholder instanceof Placeholder.UsingArgument usingArg) {
                result = usingArg.resolve(ctx.withArgument(argument == null ? "" : argument));
            } else {
                throw new IllegalStateException("Unknown placeholder type: " + this.placeholder.getClass());
            }

            return toResult(parseText(result));
        }

        private Component parseText(String input) {
            if (input == null) {
                return null;
            }
            return NODE_PARSER.parseComponent(input, ParserContext.of());
        }

        private static PlaceholderResult toResult(Component component) {
            return component == null
                    ? PlaceholderResult.invalid()
                    : PlaceholderResult.value(component);
        }
    }
}
