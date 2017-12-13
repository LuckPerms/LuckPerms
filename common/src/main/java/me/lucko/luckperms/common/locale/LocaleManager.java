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

import java.io.File;

/**
 * Manages translations
 */
public interface LocaleManager {

    /**
     * Tries to load from a locale file, and logs via the plugin if successful.
     *
     * @param plugin the plugin to log to
     * @param file the file to load from
     */
    void tryLoad(LuckPermsPlugin plugin, File file);

    /**
     * Loads a locale file
     *
     * @param file the file to load from
     * @throws Exception if the process fails
     */
    void loadFromFile(File file) throws Exception;

    /**
     * Gets the size of loaded translations
     *
     * @return the size of the loaded translations
     */
    int getSize();

    /**
     * Gets a translation for a given message key
     *
     * @param key the key
     * @return the translation, or null if there isn't any translation available.
     */
    String getTranslation(Message key);

    /**
     * Gets a translation for a given command spec key
     *
     * @param key the key
     * @return the translation data, or null if there isn't any translation available.
     */
    CommandSpecData getTranslation(CommandSpec key);

}
