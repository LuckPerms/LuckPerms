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

package me.lucko.luckperms.standalone.app.utils;

import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.flattener.ComponentFlattener;
import net.kyori.adventure.text.flattener.FlattenerListener;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.ansi.ANSIComponentRenderer;
import net.kyori.ansi.StyleOps;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Range;

/**
 * Utility to format a {@link Component} as an ANSI string.
 */
public final class AnsiUtils {
    private AnsiUtils() {}

    public static String format(Component component) {
        ANSIComponentRenderer.ToString<Style> formatter = ANSIComponentRenderer.toString(AdventureStyleOps.INSTANCE);
        ComponentFlattener.basic().flatten(component, new AnsiFlattenerListener(formatter));
        return formatter.asString();
    }

    private static final class AnsiFlattenerListener implements FlattenerListener {
        private final ANSIComponentRenderer.ToString<Style> formatter;

        AnsiFlattenerListener(ANSIComponentRenderer.ToString<Style> formatter) {
            this.formatter = formatter;
        }

        @Override
        public void pushStyle(@NotNull Style style) {
            this.formatter.pushStyle(style);
        }

        @Override
        public void component(@NotNull String text) {
            this.formatter.text(text);
        }

        @Override
        public void popStyle(@NotNull Style style) {
            this.formatter.popStyle(style);
        }
    }

    private static final class AdventureStyleOps implements StyleOps<Style> {
        private static final AdventureStyleOps INSTANCE = new AdventureStyleOps();

        @Override
        public State bold(@NotNull Style style) {
            return state(style.decoration(TextDecoration.BOLD));
        }

        @Override
        public State italics(@NotNull Style style) {
            return state(style.decoration(TextDecoration.ITALIC));
        }

        @Override
        public State underlined(@NotNull Style style) {
            return state(style.decoration(TextDecoration.UNDERLINED));
        }

        @Override
        public State strikethrough(@NotNull Style style) {
            return state(style.decoration(TextDecoration.STRIKETHROUGH));
        }

        @Override
        public State obfuscated(@NotNull Style style) {
            return state(style.decoration(TextDecoration.OBFUSCATED));
        }

        @Override
        public @Range(from = -1L, to = 16777215L) int color(@NotNull Style style) {
            TextColor color = style.color();
            return color == null ? StyleOps.COLOR_UNSET : color.value();
        }

        @Override
        public @Nullable String font(@NotNull Style style) {
            Key font = style.font();
            return font == null ? null : font.asString();
        }

        private static State state(TextDecoration.State state) {
            switch (state) {
                case TRUE:
                    return State.TRUE;
                case FALSE:
                    return State.FALSE;
                case NOT_SET:
                    return State.UNSET;
                default:
                    throw new AssertionError();
            }
        }
    }

}
