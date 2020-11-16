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

import com.google.common.collect.Maps;

import me.lucko.luckperms.common.plugin.LuckPermsPlugin;

import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.translation.GlobalTranslator;
import net.kyori.adventure.translation.TranslationRegistry;
import net.kyori.adventure.translation.Translator;
import net.kyori.adventure.util.UTF8ResourceBundleControl;

import org.checkerframework.checker.nullness.qual.Nullable;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.PropertyResourceBundle;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class TranslationManager {
    /** The default locale used by LuckPerms messages */
    public static final Locale DEFAULT_LOCALE = Locale.ENGLISH;

    private final LuckPermsPlugin plugin;
    private final Path translationsDirectory;
    private final Set<Locale> installed = ConcurrentHashMap.newKeySet();
    private TranslationRegistry registry;

    public TranslationManager(LuckPermsPlugin plugin) {
        this.plugin = plugin;
        this.translationsDirectory = this.plugin.getBootstrap().getConfigDirectory().resolve("translations");
    }

    public Path getTranslationsDirectory() {
        return this.translationsDirectory;
    }

    public Set<Locale> getInstalledLocales() {
        return Collections.unmodifiableSet(this.installed);
    }

    public void reload() {
        // remove any previous registry
        if (this.registry != null) {
            GlobalTranslator.get().removeSource(this.registry);
            this.installed.clear();
        }

        // create a translation registry
        this.registry = TranslationRegistry.create(Key.key("luckperms", "main"));
        this.registry.defaultLocale(DEFAULT_LOCALE);

        // load custom translations first, then the base (built-in) translations after.
        loadCustom();
        loadBase();

        // register it to the global source, so our translations can be picked up by adventure-platform
        GlobalTranslator.get().addSource(this.registry);
    }

    /**
     * Loads the base (English) translations from the jar file.
     */
    private void loadBase() {
        ResourceBundle bundle = ResourceBundle.getBundle("luckperms", DEFAULT_LOCALE, UTF8ResourceBundleControl.get());
        try {
            this.registry.registerAll(DEFAULT_LOCALE, bundle, false);
        } catch (IllegalArgumentException e) {
            this.plugin.getLogger().warn("Error loading default locale file", e);
        }
    }

    /**
     * Loads custom translations (in any language) from the plugin configuration folder.
     */
    public void loadCustom() {
        List<Path> translationFiles;
        try (Stream<Path> stream = Files.list(this.translationsDirectory)) {
            translationFiles = stream.filter(path -> path.getFileName().toString().endsWith(".properties")).collect(Collectors.toList());
        } catch (IOException e) {
            translationFiles = Collections.emptyList();
        }

        Map<Locale, ResourceBundle> loaded = new HashMap<>();
        for (Path translationFile : translationFiles) {
            try {
                Map.Entry<Locale, ResourceBundle> result = loadCustomTranslationFile(translationFile);
                if (result != null) {
                    loaded.put(result.getKey(), result.getValue());
                }
            } catch (Exception e) {
                this.plugin.getLogger().warn("Error loading locale file: " + translationFile.getFileName(), e);
            }
        }

        // try registering the locale without a country code - if we don't already have a registration for that
        loaded.forEach((locale, bundle) -> {
            Locale localeWithoutCountry = new Locale(locale.getLanguage());
            if (!locale.equals(localeWithoutCountry) && !localeWithoutCountry.equals(DEFAULT_LOCALE) && this.installed.add(localeWithoutCountry)) {
                this.registry.registerAll(localeWithoutCountry, bundle, false);
            }
        });
    }

    private Map.Entry<Locale, ResourceBundle> loadCustomTranslationFile(Path translationFile) {
        String fileName = translationFile.getFileName().toString();
        String localeString = fileName.substring(0, fileName.length() - ".properties".length());
        Locale locale = parseLocale(localeString);

        if (locale == null) {
            this.plugin.getLogger().warn("Unknown locale '" + localeString + "' - unable to register.");
            return null;
        }

        PropertyResourceBundle bundle;
        try (BufferedReader reader = Files.newBufferedReader(translationFile, StandardCharsets.UTF_8)) {
            bundle = new PropertyResourceBundle(reader);
        } catch(IOException e) {
            this.plugin.getLogger().warn("Error loading locale file: " + localeString, e);
            return null;
        }

        this.registry.registerAll(locale, bundle, false);
        this.installed.add(locale);
        return Maps.immutableEntry(locale, bundle);
    }

    public static Component render(Component component) {
        return render(component, Locale.getDefault());
    }

    public static Component render(Component component, @Nullable String locale) {
        return render(component, parseLocale(locale));
    }

    public static Component render(Component component, @Nullable Locale locale) {
        if (locale == null) {
            locale = Locale.getDefault();
            if (locale == null) {
                locale = DEFAULT_LOCALE;
            }
        }
        return GlobalTranslator.render(component, locale);
    }

    public static @Nullable Locale parseLocale(@Nullable String locale) {
        return locale == null ? null : Translator.parseLocale(locale);
    }

    public static String localeDisplayName(Locale locale) {
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

}
