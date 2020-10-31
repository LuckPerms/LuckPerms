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

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import me.lucko.luckperms.common.command.CommandResult;
import me.lucko.luckperms.common.command.abstraction.SingleCommand;
import me.lucko.luckperms.common.command.access.CommandPermission;
import me.lucko.luckperms.common.command.spec.CommandSpec;
import me.lucko.luckperms.common.command.utils.ArgumentList;
import me.lucko.luckperms.common.http.UnsuccessfulRequestException;
import me.lucko.luckperms.common.locale.Message;
import me.lucko.luckperms.common.locale.TranslationManager;
import me.lucko.luckperms.common.plugin.LuckPermsPlugin;
import me.lucko.luckperms.common.sender.Sender;
import me.lucko.luckperms.common.util.MoreFiles;
import me.lucko.luckperms.common.util.Predicates;
import me.lucko.luckperms.common.util.gson.GsonProvider;

import net.kyori.adventure.text.Component;

import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public class TranslationsCommand extends SingleCommand {
    private static final String TRANSLATIONS_INFO_ENDPOINT = "https://metadata.luckperms.net/data/translations";
    private static final String TRANSLATIONS_DOWNLOAD_ENDPOINT = "https://metadata.luckperms.net/translation/";

    public TranslationsCommand() {
        super(CommandSpec.TRANSLATIONS, "Translations", CommandPermission.TRANSLATIONS, Predicates.notInRange(0, 1));
    }

    @Override
    public CommandResult execute(LuckPermsPlugin plugin, Sender sender, ArgumentList args, String label) {
        Message.TRANSLATIONS_SEARCHING.send(sender);

        List<LanguageInfo> availableTranslations;
        try {
            availableTranslations = getAvailableTranslations(plugin);
        } catch (IOException | UnsuccessfulRequestException e) {
            Message.TRANSLATIONS_SEARCHING_ERROR.send(sender);
            plugin.getLogger().warn("Unable to obtain a list of available translations", e);
            return CommandResult.FAILURE;
        }

        if (args.size() >= 1 && args.get(0).equalsIgnoreCase("install")) {
            Message.TRANSLATIONS_INSTALLING.send(sender);

            downloadTranslations(plugin, availableTranslations, sender);
            plugin.getTranslationManager().reload();

            Message.TRANSLATIONS_INSTALL_COMPLETE.send(sender);
            return CommandResult.SUCCESS;
        }

        Message.INSTALLED_TRANSLATIONS.send(sender, plugin.getTranslationManager().getInstalledLocales().stream().map(Locale::toString).collect(Collectors.toList()));

        Message.AVAILABLE_TRANSLATIONS_HEADER.send(sender);
        for (LanguageInfo language : availableTranslations) {
            Message.AVAILABLE_TRANSLATIONS_ENTRY.send(sender, language.locale.toString(), localeDisplayName(language.locale), language.progress, language.contributors);
        }
        sender.sendMessage(Message.prefixed(Component.empty()));
        Message.TRANSLATIONS_DOWNLOAD_PROMPT.send(sender, label);
        return CommandResult.SUCCESS;
    }

    private static void downloadTranslations(LuckPermsPlugin plugin, List<LanguageInfo> languages, Sender sender) {
        try {
            MoreFiles.createDirectoriesIfNotExists(plugin.getTranslationManager().getTranslationsDirectory());
        } catch (IOException e) {
            // ignore
        }

        for (LanguageInfo language : languages) {
            Message.TRANSLATIONS_INSTALLING_SPECIFIC.send(sender, language.locale.toString());

            Request request = new Request.Builder()
                    .header("User-Agent", plugin.getBytebin().getUserAgent())
                    .url(TRANSLATIONS_DOWNLOAD_ENDPOINT + language.id)
                    .build();

            Path file = plugin.getTranslationManager().getTranslationsDirectory().resolve(language.locale.toString() + ".properties");

            try (Response response = plugin.getBytebin().makeHttpRequest(request)) {
                try (ResponseBody responseBody = response.body()) {
                    if (responseBody == null) {
                        throw new RuntimeException("No response");
                    }

                    try (InputStream inputStream = responseBody.byteStream()) {
                        Files.copy(inputStream, file, StandardCopyOption.REPLACE_EXISTING);
                    }
                }
            } catch (UnsuccessfulRequestException | IOException e) {
                Message.TRANSLATIONS_DOWNLOAD_ERROR.send(sender, language.locale.toString());
                plugin.getLogger().warn("Unable to download translations", e);
            }
        }
    }

    public static List<LanguageInfo> getAvailableTranslations(LuckPermsPlugin plugin) throws IOException, UnsuccessfulRequestException {
        Request request = new Request.Builder()
                .header("User-Agent", plugin.getBytebin().getUserAgent())
                .url(TRANSLATIONS_INFO_ENDPOINT)
                .build();

        JsonObject jsonResponse;
        try (Response response = plugin.getBytebin().makeHttpRequest(request)) {
            try (ResponseBody responseBody = response.body()) {
                if (responseBody == null) {
                    throw new RuntimeException("No response");
                }

                try (InputStream inputStream = responseBody.byteStream()) {
                    try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
                        jsonResponse = GsonProvider.normal().fromJson(reader, JsonObject.class);
                    }
                }
            }
        }

        List<LanguageInfo> languages = new ArrayList<>();
        for (Map.Entry<String, JsonElement> language : jsonResponse.get("languages").getAsJsonObject().entrySet()) {
            languages.add(new LanguageInfo(language.getKey(), language.getValue().getAsJsonObject()));
        }
        languages.removeIf(language -> language.progress <= 0);
        return languages;
    }

    private static String localeDisplayName(Locale locale) {
        if (locale.getLanguage().equals("zh")) {
            if (locale.getCountry().equals("CN")) {
                return "简体中文"; // Chinese (Simplified)
            } else if (locale.getCountry().equals("TW")) {
                return "繁體中文"; // Chinese (Traditional)
            }
            return locale.getDisplayCountry(locale) + locale.getDisplayLanguage(locale);
        }

        if (locale.getLanguage().equals("en") && locale.getCountry().equals("PT")) {
            return "Pirate";
        }

        return locale.getDisplayLanguage(locale);
    }

    private static final class LanguageInfo {
        private final String id;
        private final String name;
        private final Locale locale;
        private final int progress;
        private final List<String> contributors;

        LanguageInfo(String id, JsonObject data) {
            this.id = id;
            this.name = data.get("name").getAsString();
            this.locale = Objects.requireNonNull(TranslationManager.parseLocale(data.get("localeTag").getAsString()));
            this.progress = data.get("progress").getAsInt();
            this.contributors = new ArrayList<>();
            for (JsonElement contributor : data.get("contributors").getAsJsonArray()) {
                this.contributors.add(contributor.getAsJsonObject().get("name").getAsString());
            }
        }
    }
}
