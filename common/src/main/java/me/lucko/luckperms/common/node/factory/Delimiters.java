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

package me.lucko.luckperms.common.node.factory;

import com.google.common.base.Splitter;
import me.lucko.luckperms.common.cache.PatternCache;
import me.lucko.luckperms.common.node.AbstractNode;

import java.util.regex.Pattern;

/**
 * Used to add/remove a delimiter character for the {@link AbstractNode#NODE_SEPARATOR} character.
 */
public final class Delimiters {
    private Delimiters() {}

    public static final char DELIMITER = '\\';

    // used to split prefix/suffix/meta nodes
    public static final Splitter SPLIT_BY_NODE_SEPARATOR_IN_TWO = Splitter.on(PatternCache.compile("(?<!" + Pattern.quote(String.valueOf(DELIMITER)) + ")" + Pattern.quote(AbstractNode.NODE_SEPARATOR_STRING))).limit(2);

    private static boolean isDelimitedCharacter(char c) {
        return c == AbstractNode.NODE_SEPARATOR;
    }

    private static boolean isLegacyDelimitedCharacter(char c) {
        return c == AbstractNode.NODE_SEPARATOR || c == '/' || c == '-' || c == '$';
    }

    public static String escapeCharacters(String s) {
        if (s == null) {
            throw new NullPointerException();
        }

        char[] chars = s.toCharArray();

        int count = 0;
        for (char c : chars) {
            if (isDelimitedCharacter(c)) {
                count++;
            }
        }

        if (count == 0) {
            return s;
        }

        StringBuilder sb = new StringBuilder(chars.length + count);
        for (char c : chars) {
            if (isDelimitedCharacter(c)) {
                sb.append(DELIMITER);
            }
            sb.append(c);
        }

        return sb.toString();
    }

    public static String unescapeCharacters(String s) {
        if (s == null) {
            throw new NullPointerException();
        }

        char[] chars = s.toCharArray();

        int count = 0;
        for (int i = 0, j = chars.length - 1; i < j; i++) {
            if (chars[i] == DELIMITER && isLegacyDelimitedCharacter(chars[i + 1])) {
                count++;
            }
        }

        if (count == 0) {
            return s;
        }

        StringBuilder sb = new StringBuilder(chars.length - count);
        int i = 0;
        while (i < chars.length) {
            if (i < chars.length - 1 && chars[i] == DELIMITER && isLegacyDelimitedCharacter(chars[i + 1])) {
                sb.append(chars[i + 1]);
                i += 2;
            } else {
                sb.append(chars[i]);
                i++;
            }
        }

        return sb.toString();
    }

}
