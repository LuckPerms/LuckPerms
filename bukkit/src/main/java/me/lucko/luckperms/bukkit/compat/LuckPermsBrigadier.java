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

package me.lucko.luckperms.bukkit.compat;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;

import me.lucko.commodore.Commodore;
import me.lucko.commodore.CommodoreProvider;
import me.lucko.luckperms.bukkit.LPBukkitPlugin;
import me.lucko.luckperms.common.sender.Sender;

import org.bukkit.command.Command;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandSendEvent;
import org.bukkit.plugin.PluginManager;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.GZIPInputStream;

public class LuckPermsBrigadier {

    public static void register(LPBukkitPlugin plugin, Command pluginCommand) throws Exception {
        // register completions with commodore
        Commodore commodore = CommodoreProvider.getCommodore(plugin.getBootstrap());
        try (InputStream is = plugin.getBootstrap().getResourceStream("luckperms-brigadier.json.gz")) {
            if (is == null) {
                throw new Exception("Brigadier command data missing from jar!");
            }

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(new GZIPInputStream(is), StandardCharsets.UTF_8))) {
                JsonObject data = new JsonParser().parse(reader).getAsJsonObject();
                LiteralArgumentBuilder command = deserializeLiteral(data);
                commodore.register(pluginCommand, command);
            }
        }

        // add event listener to prevent completions from being send to players without permission
        // to use any LP commands.
        PluginManager pluginManager = plugin.getBootstrap().getServer().getPluginManager();
        pluginManager.registerEvents(new PermissionListener(plugin, pluginCommand), plugin.getBootstrap());
    }

    private static final class PermissionListener implements Listener {
        private final LPBukkitPlugin plugin;
        private final List<String> aliases;

        private PermissionListener(LPBukkitPlugin plugin, Command pluginCommand) {
            this.plugin = plugin;
            this.aliases = Commodore.getAliases(pluginCommand).stream()
                    .flatMap(alias -> Stream.of(alias, "minecraft:" + alias))
                    .collect(Collectors.toList());
        }

        @EventHandler
        public void onCommandSend(PlayerCommandSendEvent e) {
            Sender playerAsSender = this.plugin.getSenderFactory().wrap(e.getPlayer());
            if (!this.plugin.getCommandManager().hasPermissionForAny(playerAsSender)) {
                e.getCommands().removeAll(this.aliases);
            }
        }
    }

    private static ArgumentBuilder deserialize(JsonObject data) {
        String type = data.get("type").getAsString();
        switch (type) {
            case "literal": {
                return deserializeLiteral(data);
            }
            case "argument": {
                return deserializeArgument(data);
            }
            default:
                throw new IllegalArgumentException("type: " + type);
        }
    }

    private static LiteralArgumentBuilder deserializeLiteral(JsonObject data) {
        String name = data.get("name").getAsString();
        LiteralArgumentBuilder arg = LiteralArgumentBuilder.literal(name);
        return deserializeChildren(data, arg);
    }

    private static RequiredArgumentBuilder deserializeArgument(JsonObject data) {
        String name = data.get("name").getAsString();
        ArgumentType argumentType = deserializeArgumentType(data);

        //noinspection unchecked
        RequiredArgumentBuilder arg = RequiredArgumentBuilder.argument(name, argumentType);
        return deserializeChildren(data, arg);
    }

    private static ArgumentType deserializeArgumentType(JsonObject data) {
        String parser = data.get("parser").getAsString();
        String properties = null;
        if (data.has("properties")) {
            properties = data.get("properties").getAsString();
        }

        switch (parser) {
            case "brigadier:string": {
                Objects.requireNonNull(properties, "string properties");
                switch (properties) {
                    case "SINGLE_WORD":
                        return StringArgumentType.word();
                    case "QUOTABLE_PHRASE":
                        return StringArgumentType.string();
                    case "GREEDY_PHRASE":
                        return StringArgumentType.greedyString();
                    default:
                        throw new IllegalArgumentException("string property: " + properties);
                }
            }
            case "brigadier:bool":
                return BoolArgumentType.bool();
            case "brigadier:integer":
                return IntegerArgumentType.integer();
            default:
                throw new IllegalArgumentException("parser: " + parser);
        }
    }

    private static <T extends ArgumentBuilder> T deserializeChildren(JsonObject data, T builder) {
        if (data.has("children")) {
            JsonArray children = data.get("children").getAsJsonArray();
            for (JsonElement child : children) {
                //noinspection unchecked
                builder.then(deserialize(child.getAsJsonObject()));
            }
        }
        return builder;
    }

}
