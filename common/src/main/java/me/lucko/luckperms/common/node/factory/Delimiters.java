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

public final class Delimiters {
    private Delimiters() {}

    /**
     * The characters which are delimited when serializing meta/prefix/suffix strings
     */
    private static final String[] GENERIC_DELIMITERS = new String[]{".", "/", "-", "$"};

    static String escapeCharacters(String s) {
        if (s == null) {
            throw new NullPointerException();
        }

        return escapeDelimiters(s, GENERIC_DELIMITERS);
    }

    public static String unescapeCharacters(String s) {
        if (s == null) {
            throw new NullPointerException();
        }

        return unescapeDelimiters(s, GENERIC_DELIMITERS);
    }

    private static String escapeDelimiters(String s, String... delimiters) {
        if (s == null) {
            return null;
        }

        for (String d : delimiters) {
            s = s.replace(d, "\\" + d);
        }
        return s;
    }

    private static String unescapeDelimiters(String s, String... delimiters) {
        if (s == null) {
            return null;
        }

        for (String d : delimiters) {
            s = s.replace("\\" + d, d);
        }
        return s;
    }

}
