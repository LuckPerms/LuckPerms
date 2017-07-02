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

package me.lucko.luckperms.common.verbose;

import lombok.RequiredArgsConstructor;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;

import me.lucko.luckperms.api.Tristate;
import me.lucko.luckperms.common.commands.sender.Sender;
import me.lucko.luckperms.common.locale.Message;
import me.lucko.luckperms.common.utils.DateUtil;
import me.lucko.luckperms.common.utils.PasteUtils;
import me.lucko.luckperms.common.utils.Scripting;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.script.ScriptEngine;

@RequiredArgsConstructor
public class VerboseListener {
    private static final int DATA_TRUNCATION = 10000;
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss z");
    private static final Function<Tristate, String> TRISTATE_COLOR = tristate -> {
        switch (tristate) {
            case TRUE:
                return "&2";
            case FALSE:
                return "&c";
            default:
                return "&7";
        }
    };

    private final long startTime = System.currentTimeMillis();

    private final String pluginVersion;
    private final Sender holder;
    private final String filter;
    private final boolean notify;

    private final AtomicInteger counter = new AtomicInteger(0);
    private final AtomicInteger matchedCounter = new AtomicInteger(0);
    private final List<CheckData> results = new ArrayList<>();

    public void acceptData(CheckData data) {
        counter.incrementAndGet();
        if (!matches(data, filter)) {
            return;
        }
        matchedCounter.incrementAndGet();

        if (results.size() < DATA_TRUNCATION) {
            results.add(data);
        }

        if (notify) {
            Message.VERBOSE_LOG.send(holder, "&a" + data.getChecked() + "&7 -- &a" + data.getNode() + "&7 -- " + TRISTATE_COLOR.apply(data.getValue()) + data.getValue().name().toLowerCase() + "");
        }
    }

    private static boolean matches(CheckData data, String filter) {
        if (filter.equals("")) {
            return true;
        }

        ScriptEngine engine = Scripting.getScriptEngine();
        if (engine == null) {
            return false;
        }

        StringTokenizer tokenizer = new StringTokenizer(filter, " |&()!", true);
        StringBuilder expression = new StringBuilder();

        while (tokenizer.hasMoreTokens()) {
            String token = tokenizer.nextToken();
            if (!isDelim(token)) {
                boolean b = data.getChecked().equalsIgnoreCase(token) ||
                        data.getNode().toLowerCase().startsWith(token.toLowerCase()) ||
                        data.getValue().name().equalsIgnoreCase(token);

                token = "" + b;
            }

            expression.append(token);
        }

        try {
            String exp = expression.toString().replace("&", "&&").replace("|", "||");
            String result = engine.eval(exp).toString();
            if (!result.equals("true") && !result.equals("false")) {
                throw new IllegalArgumentException(exp + " - " + result);
            }

            return Boolean.parseBoolean(result);

        } catch (Throwable t) {
            t.printStackTrace();
        }

        return false;
    }

    public static boolean isValidFilter(String filter) {
        if (filter.equals("")) {
            return true;
        }

        ScriptEngine engine = Scripting.getScriptEngine();
        if (engine == null) {
            return false;
        }

        StringTokenizer tokenizer = new StringTokenizer(filter, " |&()!", true);
        StringBuilder expression = new StringBuilder();

        while (tokenizer.hasMoreTokens()) {
            String token = tokenizer.nextToken();
            if (!isDelim(token)) {
                token = "true"; // dummy result
            }

            expression.append(token);
        }

        try {
            String exp = expression.toString().replace("&", "&&").replace("|", "||");
            String result = engine.eval(exp).toString();
            if (!result.equals("true") && !result.equals("false")) {
                throw new IllegalArgumentException(exp + " - " + result);
            }

            return true;

        } catch (Throwable t) {
            return false;
        }
    }

    private static boolean isDelim(String token) {
        return token.equals(" ") || token.equals("|") || token.equals("&") || token.equals("(") || token.equals(")") || token.equals("!");
    }

    public String uploadPasteData() {
        long now = System.currentTimeMillis();
        String startDate = DATE_FORMAT.format(new Date(startTime));
        String endDate = DATE_FORMAT.format(new Date(now));
        long secondsTaken = (now - startTime) / 1000L;
        String duration = DateUtil.formatTime(secondsTaken);
        String filter = this.filter;
        if (filter == null || filter.equals("")){
            filter = "any";
        } else {
            filter = "`" + filter + "`";
        }

        ImmutableList.Builder<String> output = ImmutableList.<String>builder()
                .add("## Verbose Checking Output")
                .add("#### This file was automatically generated by [LuckPerms](https://github.com/lucko/LuckPerms) " + pluginVersion)
                .add("")
                .add("### Metadata")
                .add("| Key | Value |")
                .add("|-----|-------|")
                .add("| Start Time | " + startDate + " |")
                .add("| End Time | " + endDate + " |")
                .add("| Duration | " + duration +" |")
                .add("| Count | **" + matchedCounter.get() + "** / " + counter + " |")
                .add("| User | " + holder.getName() + " |")
                .add("| Filter | " + filter + " |")
                .add("");

        if (matchedCounter.get() > results.size()) {
            output.add("**WARN:** Result set exceeded max size of " + DATA_TRUNCATION + ". The output below was truncated to " + DATA_TRUNCATION + " entries.");
            output.add("");
        }

        output.add("### Output")
                .add("Format: `<checked>` `<permission>` `<value>`")
                .add("")
                .add("___")
                .add("");

        ImmutableList.Builder<String> data = ImmutableList.<String>builder()
                .add("User,Permission,Result");

        results.stream()
                .peek(c -> output.add("`" + c.getChecked() + "` - " + c.getNode() + " - **" + c.getValue().toString() + "**   "))
                .forEach(c -> data.add(escapeCommas(c.getChecked()) + "," + escapeCommas(c.getNode()) + "," + c.getValue().name().toLowerCase()));

        results.clear();

        List<Map.Entry<String, String>> content = ImmutableList.of(
                Maps.immutableEntry("luckperms-verbose.md", output.build().stream().collect(Collectors.joining("\n"))),
                Maps.immutableEntry("raw-data.csv", data.build().stream().collect(Collectors.joining("\n")))
        );

        return PasteUtils.paste("LuckPerms Verbose Checking Output", content);
    }

    private static String escapeCommas(String s) {
        return s.contains(",") ? "\"" + s + "\"" : s;
    }

}
