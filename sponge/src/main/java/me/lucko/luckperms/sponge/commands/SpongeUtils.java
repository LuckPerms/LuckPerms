/*
 * Copyright (c) 2016 Lucko (Luck) <luck@lucko.me>
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

package me.lucko.luckperms.sponge.commands;

import lombok.experimental.UtilityClass;

import com.google.common.collect.Maps;

import me.lucko.luckperms.api.context.ContextSet;
import me.lucko.luckperms.common.commands.utils.ArgumentUtils;

import org.spongepowered.api.service.context.Context;
import org.spongepowered.api.service.permission.Subject;
import org.spongepowered.api.util.Tristate;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@UtilityClass
public class SpongeUtils {

    public static Tristate parseTristate(int index, List<String> args) throws ArgumentUtils.ArgumentException {
        String s = args.get(index).toLowerCase();
        if (s.equals("1") || s.equals("true") || s.equals("t")) {
            return Tristate.TRUE;
        }
        if (s.equals("0") || s.equals("null") || s.equals("none") || s.equals("undefined") || s.equals("undef")) {
            return Tristate.UNDEFINED;
        }
        if (s.equals("-1") || s.equals("false") || s.equals("f")) {
            return Tristate.FALSE;
        }
        throw new ArgumentUtils.DetailedUsageException();
    }

    public static String nodesToString(Map<String, Boolean> nodes) {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, Boolean> e : nodes.entrySet()) {
            sb.append("&3> ")
                    .append(e.getValue() ? "&a" : "&c")
                    .append(e.getKey())
                    .append("\n");
        }
        return sb.toString();
    }

    public static String optionsToString(Map<String, String> options) {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> e : options.entrySet()) {
            sb.append("&3> &a")
                    .append(e.getKey())
                    .append(" &f= \"")
                    .append(e.getKey())
                    .append("&f\"\n");
        }
        return sb.toString();
    }

    public static String parentsToString(List<Subject> parents) {
        StringBuilder sb = new StringBuilder();
        for (Subject s : parents) {
            sb.append("&3> &a")
                    .append(s.getIdentifier())
                    .append(" &bfrom collection &a")
                    .append(s.getContainingCollection().getIdentifier())
                    .append("&b.\n");
        }
        return sb.toString();
    }

    public static String contextToString(ContextSet set) {
        if (set.isEmpty()) {
            return "&bGLOBAL";
        }

        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> e : set.toSet()) {
            sb.append("&f").append(e.getKey()).append("&7=&f").append(e.getValue()).append("&7, ");
        }
        return sb.delete(sb.length() - 2, sb.length()).toString();
    }

    public static ContextSet convertContexts(Set<Context> contexts) {
        return ContextSet.fromEntries(contexts.stream().map(c -> Maps.immutableEntry(c.getKey(), c.getValue())).collect(Collectors.toSet()));
    }

    public static Set<Context> convertContexts(ContextSet contexts) {
        return contexts.toSet().stream().map(e -> new Context(e.getKey(), e.getValue())).collect(Collectors.toSet());
    }

}
