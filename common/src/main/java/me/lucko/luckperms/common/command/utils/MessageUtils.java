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

package me.lucko.luckperms.common.command.utils;

import me.lucko.luckperms.common.locale.LocaleManager;
import me.lucko.luckperms.common.locale.message.Message;

import net.luckperms.api.context.Context;
import net.luckperms.api.context.ContextSet;
import net.luckperms.api.node.Node;
import net.luckperms.api.util.Tristate;

import java.util.Collection;
import java.util.List;

public final class MessageUtils {
    private MessageUtils() {}

    public static String toCommaSep(Collection<String> strings) {
        if (strings.isEmpty()) {
            return "&bNone";
        }

        StringBuilder sb = new StringBuilder();
        strings.forEach(s -> sb.append("&3").append(s).append("&7, "));
        return sb.delete(sb.length() - 2, sb.length()).toString();
    }

    public static String listToArrowSep(Collection<String> strings, String highlight) {
        if (strings.isEmpty()) {
            return "&bNone";
        }

        StringBuilder sb = new StringBuilder();
        strings.forEach(s -> sb.append(s.equalsIgnoreCase(highlight) ? "&b" : "&3").append(s).append("&7 ---> "));
        return sb.delete(sb.length() - 6, sb.length()).toString();
    }

    public static String listToArrowSep(Collection<String> strings, String highlightFirst, String highlightSecond, boolean reversed) {
        if (strings.isEmpty()) {
            return "&6None";
        }

        StringBuilder sb = new StringBuilder();
        for (String s : strings) {
            if (s.equalsIgnoreCase(highlightFirst)) {
                sb.append("&b").append(s).append("&4");
            } else if (s.equalsIgnoreCase(highlightSecond)) {
                sb.append("&b").append(s).append("&7");
            } else {
                sb.append("&3").append(s).append("&7");
            }

            sb.append(reversed ? " <--- " : " ---> ");
        }
        return sb.delete(sb.length() - 6, sb.length()).toString();
    }

    public static String listToArrowSep(List<String> strings) {
        if (strings.isEmpty()) {
            return "&6None";
        }

        StringBuilder sb = new StringBuilder();
        strings.forEach(s -> sb.append("&3").append(s).append("&b ---> "));
        return sb.delete(sb.length() - 6, sb.length()).toString();
    }

    /**
     * Formats a boolean to a colored string
     *
     * @param b the boolean value
     * @return a formatted boolean string
     */
    public static String formatBoolean(boolean b) {
        return b ? "&atrue" : "&cfalse";
    }

    /**
     * Formats a tristate to a colored string
     *
     * @param t the tristate value
     * @return a formatted tristate string
     */
    public static String formatTristate(Tristate t) {
        switch (t) {
            case TRUE:
                return "&atrue";
            case FALSE:
                return "&cfalse";
            default:
                return "&cundefined";
        }
    }

    /**
     * Produces a string representing a Nodes context, suitable for appending onto another message line.
     *
     * @param localeManager the locale manager
     * @param node the node to query context from
     * @return a string representing the nodes context, or an empty string if the node applies globally.
     */
    public static String getAppendableNodeContextString(LocaleManager localeManager, Node node) {
        StringBuilder sb = new StringBuilder();
        for (Context c : node.getContexts()) {
            sb.append(" ").append(contextToString(localeManager, c.getKey(), c.getValue()));
        }
        return sb.toString();
    }

    /**
     * Converts a context pair to a formatted string, surrounded by (  ) brackets.
     *
     *
     * @param localeManager the locale manager
     * @param key the context key
     * @param value the context value
     * @return a formatted string
     */
    public static String contextToString(LocaleManager localeManager, String key, String value) {
        return Message.CONTEXT_PAIR.asString(localeManager, key, value);
    }

    public static String contextSetToString(LocaleManager localeManager, ContextSet set) {
        if (set.isEmpty()) {
            return Message.CONTEXT_PAIR__GLOBAL_INLINE.asString(localeManager);
        }

        StringBuilder sb = new StringBuilder();

        for (Context e : set) {
            sb.append(Message.CONTEXT_PAIR_INLINE.asString(localeManager, e.getKey(), e.getValue()));
            sb.append(Message.CONTEXT_PAIR_SEP.asString(localeManager));
        }

        return sb.delete(sb.length() - Message.CONTEXT_PAIR_SEP.asString(localeManager).length(), sb.length()).toString();
    }

}
