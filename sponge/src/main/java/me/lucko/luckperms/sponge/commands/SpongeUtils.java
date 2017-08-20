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

package me.lucko.luckperms.sponge.commands;

import lombok.experimental.UtilityClass;

import me.lucko.luckperms.api.Tristate;
import me.lucko.luckperms.api.context.ContextSet;
import me.lucko.luckperms.common.commands.utils.ArgumentUtils;
import me.lucko.luckperms.common.commands.utils.Util;
import me.lucko.luckperms.sponge.service.model.SubjectReference;

import java.util.List;
import java.util.Map;

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

    public static String parentsToString(Iterable<SubjectReference> parents) {
        StringBuilder sb = new StringBuilder();
        for (SubjectReference s : parents) {
            sb.append("&3> &a")
                    .append(s.getSubjectIdentifier())
                    .append(" &bfrom collection &a")
                    .append(s.getCollectionIdentifier())
                    .append("&b.\n");
        }
        return sb.toString();
    }

    public static String contextToString(ContextSet set) {
        return Util.contextSetToString(set);
    }

}
