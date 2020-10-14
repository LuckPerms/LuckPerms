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

package me.lucko.luckperms.common.locale;

import me.lucko.luckperms.common.plugin.LuckPermsPlugin;

import net.kyori.adventure.key.Key;
import net.kyori.adventure.translation.GlobalTranslator;
import net.kyori.adventure.translation.TranslationRegistry;
import net.kyori.adventure.translation.Translator;
import net.kyori.adventure.util.UTF8ResourceBundleControl;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class TranslationManager {
    /** The default locale used by LuckPerms messages */
    public static final Locale DEFAULT_LOCALE = Locale.ENGLISH;

    private final LuckPermsPlugin plugin;
    private final TranslationRegistry registry;

    public TranslationManager(LuckPermsPlugin plugin) {
        this.plugin = plugin;

        // create a translation registry for luckperms
        this.registry = TranslationRegistry.create(Key.key("luckperms", "main"));
        this.registry.defaultLocale(DEFAULT_LOCALE);

        // register it to the global source, so our translations can be picked up by adventure-platform
        GlobalTranslator.get().addSource(this.registry);
    }

    public void load() {
        // load custom translations first, then the base (built-in) translations after.
        loadCustom();
        loadBase();
    }

    /**
     * Loads the base (English) translations from the jar file.
     */
    private void loadBase() {
        ResourceBundle bundle = ResourceBundle.getBundle("luckperms", DEFAULT_LOCALE, UTF8ResourceBundleControl.get());
        this.registry.registerAll(DEFAULT_LOCALE, bundle, false);
    }

    /**
     * Loads custom translations (in any language) from the plugin configuration folder.
     */
    public void loadCustom() {
        List<Path> translationFiles;
        try (Stream<Path> stream = Files.list(this.plugin.getBootstrap().getConfigDirectory().resolve("translations"))) {
            translationFiles = stream.filter(path -> path.getFileName().toString().endsWith(".properties")).collect(Collectors.toList());
        } catch (IOException e) {
            translationFiles = Collections.emptyList();
        }

        for (Path translationFile : translationFiles) {
            try {
                loadCustomTranslationFile(translationFile);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void loadCustomTranslationFile(Path translationFile) {
        String fileName = translationFile.getFileName().toString();
        String localeString = fileName.substring(0, fileName.length() - ".properties".length());
        Locale locale = parseLocale(localeString, null);

        if (locale == null) {
            this.plugin.getLogger().warn("Unknown locale '" + localeString + "' - unable to register.");
            return;
        }

        this.registry.registerAll(locale, translationFile, true);
        this.plugin.getLogger().info("Registered additional translations for " + locale.toLanguageTag());
    }

    public static Locale parseLocale(String locale, Locale defaultLocale) {
        if (locale == null) {
            return defaultLocale;
        }

        Locale parsed = Translator.parseLocale(locale);
        return parsed != null ? parsed : defaultLocale;
    }

}
