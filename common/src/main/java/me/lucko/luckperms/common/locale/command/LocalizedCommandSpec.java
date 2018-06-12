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

package me.lucko.luckperms.common.locale.command;

import com.google.common.collect.ImmutableList;

import me.lucko.luckperms.common.locale.LocaleManager;

import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;

/**
 * Represents a localized instance of a {@link CommandSpec}.
 */
public class LocalizedCommandSpec {
    private final LocaleManager localeManager;
    private final CommandSpec spec;

    public LocalizedCommandSpec(CommandSpec spec, LocaleManager localeManager) {
        this.localeManager = localeManager;
        this.spec = spec;
    }

    public String description() {
        CommandSpecData translation = this.localeManager.getTranslation(this.spec);
        if (translation != null && translation.getDescription() != null) {
            return translation.getDescription();
        }

        // fallback
        return this.spec.getDescription();
    }

    public String usage() {
        CommandSpecData translation = this.localeManager.getTranslation(this.spec);
        if (translation != null && translation.getUsage() != null) {
            return translation.getUsage();
        }

        // fallback
        return this.spec.getUsage();
    }

    public List<Argument> args() {
        CommandSpecData translation = this.localeManager.getTranslation(this.spec);
        if (translation == null || translation.getArgs() == null) {
            // fallback
            return this.spec.getArgs();
        }

        List<Argument> args = new ArrayList<>(this.spec.getArgs());
        ListIterator<Argument> it = args.listIterator();
        while (it.hasNext()) {
            Argument next = it.next();
            String s = translation.getArgs().get(next.getName());

            // if a translation for the given arg key is present, apply the new description.
            if (s != null) {
                it.set(Argument.create(next.getName(), next.isRequired(), s));
            }
        }

        return ImmutableList.copyOf(args);
    }

    public LocaleManager getLocaleManager() {
        return this.localeManager;
    }
}
