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

package me.lucko.luckperms.common.commands.misc;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import me.lucko.luckperms.api.Contexts;
import me.lucko.luckperms.api.LookupSetting;
import me.lucko.luckperms.api.caching.MetaContexts;
import me.lucko.luckperms.api.context.ContextCalculator;
import me.lucko.luckperms.api.context.StaticContextCalculator;
import me.lucko.luckperms.api.metastacking.MetaStackDefinition;
import me.lucko.luckperms.api.metastacking.MetaStackElement;
import me.lucko.luckperms.common.caching.type.MetaCache;
import me.lucko.luckperms.common.caching.type.PermissionCache;
import me.lucko.luckperms.common.command.CommandResult;
import me.lucko.luckperms.common.command.abstraction.SingleCommand;
import me.lucko.luckperms.common.command.access.CommandPermission;
import me.lucko.luckperms.common.contexts.ContextSetJsonSerializer;
import me.lucko.luckperms.common.contexts.ProxiedContextCalculator;
import me.lucko.luckperms.common.locale.LocaleManager;
import me.lucko.luckperms.common.locale.command.CommandSpec;
import me.lucko.luckperms.common.locale.message.Message;
import me.lucko.luckperms.common.model.User;
import me.lucko.luckperms.common.plugin.LuckPermsPlugin;
import me.lucko.luckperms.common.processors.PermissionProcessor;
import me.lucko.luckperms.common.sender.Sender;
import me.lucko.luckperms.common.utils.Predicates;
import me.lucko.luckperms.common.utils.gson.JArray;
import me.lucko.luckperms.common.utils.gson.JObject;
import me.lucko.luckperms.common.web.Pastebin;
import me.lucko.luckperms.common.web.StandardPastebin;

import net.kyori.text.Component;
import net.kyori.text.TextComponent;
import net.kyori.text.event.ClickEvent;
import net.kyori.text.event.HoverEvent;
import net.kyori.text.format.TextColor;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

public class DebugCommand extends SingleCommand {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    public DebugCommand(LocaleManager locale) {
        super(CommandSpec.DEBUG.localize(locale), "Debug", CommandPermission.DEBUG, Predicates.alwaysFalse());
    }

    @Override
    public CommandResult execute(LuckPermsPlugin plugin, Sender sender, List<String> args, String label) {
        Message.DEBUG_START.send(sender);

        StringBuilder sb = new StringBuilder();
        sb.append("LuckPerms Debug Output\n\n\n");

        BiConsumer<String, JObject> builder = (name, content) -> {
            sb.append("-- ").append(name).append(" --\n");
            sb.append(GSON.toJson(content.toJson()));
            sb.append("\n\n");
        };

        builder.accept("platform.json", getPlatformData(plugin));
        builder.accept("storage.json", getStorageData(plugin));
        builder.accept("context.json", getContextData(plugin));
        builder.accept("players.json", getPlayersData(plugin));

        Pastebin.Paste paste = StandardPastebin.HASTEBIN.postPlain(sb.toString());

        Message.DEBUG_URL.send(sender);

        Component message = TextComponent.builder(paste.url()).color(TextColor.AQUA)
                .clickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, String.valueOf(paste.url())))
                .hoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, TextComponent.of("Click to open the debugging data.").color(TextColor.GRAY)))
                .build();

        sender.sendMessage(message);
        return CommandResult.SUCCESS;
    }

    private static JObject getPlatformData(LuckPermsPlugin plugin) {
        return new JObject()
                .add("type", plugin.getBootstrap().getType().name())
                .add("version", new JObject()
                        .add("api", String.valueOf(plugin.getApiProvider().getPlatformInfo().getApiVersion()))
                        .add("plugin", plugin.getBootstrap().getVersion())
                )
                .add("server", new JObject()
                        .add("brand", plugin.getBootstrap().getServerBrand())
                        .add("version", plugin.getBootstrap().getServerVersion())
                );
    }

    private static JObject getStorageData(LuckPermsPlugin plugin) {
        return new JObject()
                .add("storage", new JObject()
                        .add("name", plugin.getStorage().getName())
                        .add("type", plugin.getStorage().getDao().getClass().getName())
                        .add("meta", () -> {
                            JObject metaObject = new JObject();
                            Map<String, String> meta = plugin.getStorage().getMeta();
                            for (Map.Entry<String, String> entry : meta.entrySet()) {
                                metaObject.add(entry.getKey(), entry.getValue());
                            }
                            return metaObject;
                        }))
                .add("messaging", () -> {
                    JObject messaging = new JObject();
                    plugin.getMessagingService().ifPresent(ms -> {
                        messaging.add("name", ms.getName());
                        messaging.add("implementation", new JObject()
                                .add("messenger", ms.getMessenger().getClass().getName())
                                .add("provider", ms.getMessengerProvider().getClass().getName())
                        );
                    });
                    return messaging;
                });
    }

    private static JObject getContextData(LuckPermsPlugin plugin) {
        return new JObject()
                .add("staticContext", ContextSetJsonSerializer.serializeContextSet(plugin.getContextManager().getStaticContext()))
                .add("calculators", () -> {
                    JArray calculators = new JArray();
                    for (ContextCalculator<?> calculator : plugin.getContextManager().getCalculators()) {
                        String name = calculator.getClass().getName();
                        if (calculator instanceof ProxiedContextCalculator) {
                            name = ((ProxiedContextCalculator) calculator).getDelegate().getClass().getName();
                        }
                        calculators.add(name);
                    }
                    return calculators;
                })
                .add("staticCalculators", () -> {
                    JArray staticCalculators = new JArray();
                    for (StaticContextCalculator calculator : plugin.getContextManager().getStaticCalculators()) {
                        staticCalculators.add(calculator.getClass().getName());
                    }
                    return staticCalculators;
                });
    }

    private static JObject getPlayersData(LuckPermsPlugin plugin) {
        JObject ret = new JObject();

        Set<UUID> onlinePlayers = plugin.getBootstrap().getOnlinePlayers().collect(Collectors.toSet());
        ret.add("count", onlinePlayers.size());

        JArray playerArray = new JArray();
        for (UUID uuid : onlinePlayers) {
            User user = plugin.getUserManager().getIfLoaded(uuid);
            if (user == null) {
                playerArray.add(new JObject()
                        .add("uniqueId", uuid.toString())
                        .add("loaded", false)
                );
                continue;
            }

            playerArray.add(new JObject()
                    .add("uniqueId", uuid.toString())
                    .add("loaded", true)
                    .add("username", user.getName().orElse("null"))
                    .add("primaryGroup", new JObject()
                            .add("type", user.getPrimaryGroup().getClass().getName())
                            .add("value", user.getPrimaryGroup().getValue())
                            .add("storedValue", user.getPrimaryGroup().getStoredValue().orElse("null"))
                    )
                    .add("activeContext", () -> {
                        JObject obj = new JObject();
                        Contexts contexts = plugin.getContextForUser(user).orElse(null);
                        if (contexts != null) {
                            MetaContexts metaContexts = plugin.getContextManager().formMetaContexts(contexts);
                            obj.add("data", new JObject()
                                            .add("permissions", serializePermissionsData(user.getCachedData().getPermissionData(contexts)))
                                            .add("meta", serializeMetaData(user.getCachedData().getMetaData(metaContexts)))
                                    )
                                    .add("contextSet", ContextSetJsonSerializer.serializeContextSet(contexts.getContexts()))
                                    .add("settings", serializeContextsSettings(contexts))
                                    .add("metaSettings", serializeMetaContextsSettings(metaContexts));
                        }
                        return obj;
                    })
            );
        }
        ret.add("players", playerArray);
        return ret;
    }

    private static JArray serializeContextsSettings(Contexts contexts) {
        JArray array = new JArray();
        for (LookupSetting setting : contexts.getSettings()) {
            array.add(setting.name());
        }
        return array;
    }

    private static JObject serializeMetaContextsSettings(MetaContexts metaContexts) {
        return new JObject()
                .add("prefixStack", serializeMetaStackData(metaContexts.getPrefixStackDefinition()))
                .add("suffixStack", serializeMetaStackData(metaContexts.getSuffixStackDefinition()));
    }

    private static JObject serializePermissionsData(PermissionCache permissionData) {
        return new JObject()
                .add("processors", () -> {
                    JArray processors = new JArray();
                    for (PermissionProcessor processor : permissionData.getCalculator().getProcessors()) {
                        processors.add(processor.getClass().getName());
                    }
                    return processors;
                });
    }

    private static JObject serializeMetaData(MetaCache metaData) {
        return new JObject()
                .add("prefix", metaData.getPrefix())
                .add("suffix", metaData.getSuffix())
                .add("prefixes", () -> {
                    JArray prefixes = new JArray();
                    for (Map.Entry<Integer, String> entry : metaData.getPrefixes().entrySet()) {
                        prefixes.add(new JObject()
                                .add("weight", entry.getKey())
                                .add("value", entry.getValue())
                        );
                    }
                    return prefixes;
                })
                .add("suffixes", () -> {
                    JArray suffixes = new JArray();
                    for (Map.Entry<Integer, String> entry : metaData.getSuffixes().entrySet()) {
                        suffixes.add(new JObject()
                                .add("weight", entry.getKey())
                                .add("value", entry.getValue())
                        );
                    }
                    return suffixes;
                })
                .add("meta", () -> {
                    JObject metaMap = new JObject();
                    for (Map.Entry<String, String> entry : metaData.getMeta().entrySet()) {
                        metaMap.add(entry.getKey(), entry.getValue());
                    }
                    return metaMap;
                })
                .add("metaMap", () -> {
                    JObject metaMultimap = new JObject();
                    for (Map.Entry<String, Collection<String>> entry : metaData.getMetaMultimap().asMap().entrySet()) {
                        JArray values = new JArray();
                        for (String v : entry.getValue()) {
                            values.add(v);
                        }
                        metaMultimap.add(entry.getKey(), values);
                    }
                    return metaMultimap;
                });
    }

    private static JObject serializeMetaStackData(MetaStackDefinition definition) {
        return new JObject()
                .add("type", definition.getClass().getName())
                .add("startSpacer", definition.getStartSpacer())
                .add("middleSpacer", definition.getMiddleSpacer())
                .add("endSpacer", definition.getEndSpacer())
                .add("elements", () -> {
                    JArray elements = new JArray();
                    for (MetaStackElement element : definition.getElements()) {
                        elements.add(new JObject()
                                .add("type", element.getClass().getName())
                                .add("info", element.toString())
                        );
                    }
                    return elements;
                });
    }

}
