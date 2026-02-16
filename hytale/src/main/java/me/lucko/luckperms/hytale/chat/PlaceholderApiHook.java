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

package me.lucko.luckperms.hytale.chat;

import com.hypixel.hytale.server.core.universe.PlayerRef;

import java.util.regex.Pattern;

/**
 * Integration with PlaceholderAPI.
 */
public interface PlaceholderApiHook {

    Pattern PERCENT_PLACEHOLDER_PATTERN = Pattern.compile("[%]([^%]+)[%]");
    Pattern BRACKET_PLACEHOLDER_PATTERN = Pattern.compile("[{]([^{}]+)[}]");

    /**
     * Checks if the input string contains any placeholders.
     *
     * @param input the string to check
     * @return true if the string contains placeholders, false otherwise
     */
    static boolean containsPlaceholders(String input) {
        return PERCENT_PLACEHOLDER_PATTERN.matcher(input).find() || BRACKET_PLACEHOLDER_PATTERN.matcher(input).find();
    }

    /**
     * Transforms the format string, replacing placeholders in {@code %placeholder%}
     * or {@code {placeholder}} format with {@code <papi:'placeholder'>}.
     *
     * <p>This allows the placeholders to be resolved by the MiniMessage parser.</p>
     *
     * @param input the string to transform
     * @return the transformed string
     */
    static String transformFormat(String input) {
        input = PERCENT_PLACEHOLDER_PATTERN.matcher(input).replaceAll("<papi:'$1'>");
        input = BRACKET_PLACEHOLDER_PATTERN.matcher(input).replaceAll("<papi:'$1'>");
        return input;
    }

    static PlaceholderApiHook init() {
        try {
            Class.forName("at.helpch.placeholderapi.PlaceholderAPI");
            return new PlaceholderApiHookImpl();
        } catch (ClassNotFoundException e) {
            return null;
        }
    }

    String resolvePlaceholder(PlayerRef playerRef, String placeholder);

}
