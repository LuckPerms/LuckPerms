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

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;

import me.lucko.luckperms.api.Tristate;
import me.lucko.luckperms.common.commands.CommandManager;
import me.lucko.luckperms.common.commands.sender.Sender;
import me.lucko.luckperms.common.commands.utils.CommandUtils;
import me.lucko.luckperms.common.locale.Message;
import me.lucko.luckperms.common.utils.DateUtil;
import me.lucko.luckperms.common.utils.PasteUtils;
import me.lucko.luckperms.common.utils.TextUtils;

import net.kyori.text.TextComponent;
import net.kyori.text.event.HoverEvent;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * Accepts and processes {@link CheckData}, passed from the {@link VerboseHandler}.
 */
@RequiredArgsConstructor
public class VerboseListener {
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss z");

    // how much data should we store before stopping.
    private static final int DATA_TRUNCATION = 10000;

    // how many traces should we add
    private static final int TRACE_DATA_TRUNCATION = 250;

    // the time when the listener was first registered
    private final long startTime = System.currentTimeMillis();

    // the version of the plugin. (used when we paste data to gist)
    private final String pluginVersion;

    // the sender to notify each time the listener processes a check which passes the filter
    @Getter
    private final Sender notifiedSender;

    // the filter string
    private final String filter;

    // if we should notify the sender
    private final boolean notify;

    // the number of checks we have processed
    private final AtomicInteger counter = new AtomicInteger(0);

    // the number of checks we have processed and accepted, based on the filter rules for this
    // listener
    private final AtomicInteger matchedCounter = new AtomicInteger(0);

    // the checks which passed the filter, up to a max size of #DATA_TRUNCATION
    private final List<CheckData> results = new ArrayList<>(DATA_TRUNCATION / 10);

    /**
     * Accepts and processes check data.
     *
     * @param data the data to process
     */
    public void acceptData(CheckData data) {
        // increment handled counter
        counter.incrementAndGet();

        // check if the data passes our filters
        if (!VerboseFilter.passesFilter(data, filter)) {
            return;
        }

        // increment the matched filter
        matchedCounter.incrementAndGet();

        // record the check, if we have space for it
        if (results.size() < DATA_TRUNCATION) {
            results.add(data);
        }

        // handle notifications
        if (notify) {
            StringBuilder msgContent = new StringBuilder();

            if (notifiedSender.isConsole()) {
                msgContent.append("&8[&2")
                        .append(data.getCheckOrigin().getCode())
                        .append("&8] ");
            }

            msgContent.append("&a")
                    .append(data.getCheckTarget())
                    .append("&7 - &a")
                    .append(data.getPermission())
                    .append("&7 - ")
                    .append(getTristateColor(data.getResult()))
                    .append(data.getResult().name().toLowerCase());

            if (notifiedSender.isConsole()) {
                // just send as a raw message
                Message.VERBOSE_LOG.send(notifiedSender, msgContent.toString());
            } else {

                // form a hoverevent from the check trace
                TextComponent textComponent = TextUtils.fromLegacy(Message.VERBOSE_LOG.asString(notifiedSender.getPlatform().getLocaleManager(), msgContent.toString()));

                // build the text
                List<String> hover = new ArrayList<>();
                hover.add("&bOrigin: &2" + data.getCheckOrigin().name());
                hover.add("&bContext: &r" + CommandUtils.contextSetToString(data.getCheckContext()));
                hover.add("&bTrace: &r");

                int overflow = readStack(data, 15, e -> hover.add("&7" + e.getClassName() + "." + e.getMethodName() + (e.getLineNumber() >= 0 ? ":" + e.getLineNumber() : "")));
                if (overflow != 0) {
                    hover.add("&f... and " + overflow + " more");
                }

                // send the message
                HoverEvent e = new HoverEvent(HoverEvent.Action.SHOW_TEXT, TextUtils.fromLegacy(TextUtils.joinNewline(hover.stream()), CommandManager.AMPERSAND_CHAR));
                TextComponent msg = textComponent.toBuilder().applyDeep(comp -> comp.hoverEvent(e)).build();
                notifiedSender.sendMessage(msg);
            }
        }
    }

    /**
     * Uploads the captured data in this listener to a paste and returns the url
     *
     * @param showTraces if stack traces should be included in the output
     * @param attachRaw if the rawdata should be attached to the gist
     * @return the url
     * @see PasteUtils#paste(String, List)
     */
    public String uploadPasteData(boolean showTraces, boolean attachRaw) {

        // retrieve variables
        long now = System.currentTimeMillis();
        String startDate = DATE_FORMAT.format(new Date(startTime));
        String endDate = DATE_FORMAT.format(new Date(now));
        long secondsTaken = (now - startTime) / 1000L;
        String duration = DateUtil.formatTimeShort(secondsTaken);

        String filter = this.filter;
        if (filter == null || filter.equals("")){
            filter = "any";
        } else {
            filter = "`" + filter + "`";
        }

        // start building the message output
        ImmutableList.Builder<String> prettyOutput = ImmutableList.<String>builder()
                .add("## Verbose Checking Output")
                .add("#### This file was automatically generated by [LuckPerms](https://github.com/lucko/LuckPerms) " + pluginVersion)
                .add("")
                .add("### Metadata")
                .add("| Key | Value |")
                .add("|-----|-------|")
                .add("| Start Time | " + startDate + " |")
                .add("| End Time | " + endDate + " |")
                .add("| Duration | " + duration +" |")
                .add("| Count | **" + matchedCounter.get() + "** / " + counter.get() + " |")
                .add("| User | " + notifiedSender.getNameWithLocation() + " |")
                .add("| Filter | " + filter + " |")
                .add("| Include traces | " + showTraces + " |")
                .add("");

        // warn if data was truncated
        if (matchedCounter.get() > results.size()) {
            prettyOutput.add("**WARN:** Result set exceeded max size of " + DATA_TRUNCATION + ". The output below was truncated to " + DATA_TRUNCATION + " entries.");
            prettyOutput.add("");
        }

        // explain why some traces may be missing
        if (showTraces && results.size() > TRACE_DATA_TRUNCATION) {
            prettyOutput.add("**WARN:** Result set exceeded size of " + TRACE_DATA_TRUNCATION + ". The traced output below was truncated to " + TRACE_DATA_TRUNCATION + " entries.   ");
            prettyOutput.add("Either refine the query using a more specific filter, or disable tracing by adding '--slim' to the end of the paste command.");
            prettyOutput.add("");
        }

        // print the format of the output
        prettyOutput.add("### Output")
                .add("Format: `<checked>` `<permission>` `<value>`")
                .add("")
                .add("___")
                .add("");

        // build the csv output - will only be appended to if this is enabled.
        ImmutableList.Builder<String> csvOutput = ImmutableList.<String>builder()
                .add("User,Permission,Result");

        // how many instances have been printed so far
        AtomicInteger printedCount = new AtomicInteger(0);

        for (CheckData c : results) {
            if (!showTraces) {

                // if traces aren't being shown, just append using raw markdown
                prettyOutput.add("`" + c.getCheckTarget() + "` - " + c.getPermission() + " - " + getTristateSymbol(c.getResult()) + "   ");

            } else if (printedCount.incrementAndGet() > TRACE_DATA_TRUNCATION) {

                // if we've gone over the trace truncation, just append the raw info.
                // we still have to use html, as the rest of this section is still using it.
                prettyOutput.add("<br><code>" + c.getCheckTarget() + "</code> - " + c.getPermission() + " - " + getTristateSymbol(c.getResult()));

            } else {

                // append the full output.
                prettyOutput.add("<details><summary><code>" + c.getCheckTarget() + "</code> - " + c.getPermission() + " - " + getTristateSymbol(c.getResult()) + "</summary><p>");

                // append the spoiler text
                prettyOutput.add("<br><b>Origin:</b> <code>" + c.getCheckOrigin().name() + "</code>");
                prettyOutput.add("<br><b>Context:</b> <code>" + CommandUtils.stripColor(CommandUtils.contextSetToString(c.getCheckContext())) + "</code>");
                prettyOutput.add("<br><b>Trace:</b><pre>");

                int overflow = readStack(c, 30, e -> prettyOutput.add(e.getClassName() + "." + e.getMethodName() + (e.getLineNumber() >= 0 ? ":" + e.getLineNumber() : "")));
                if (overflow != 0) {
                    prettyOutput.add("... and " + overflow + " more");
                }

                prettyOutput.add("</pre></p></details>");
            }

            // if we're including a raw csv output, append that too
            if (attachRaw) {
                csvOutput.add(escapeCommas(c.getCheckTarget()) + "," + escapeCommas(c.getPermission()) + "," + c.getResult().name().toLowerCase());
            }
        }
        results.clear();

        ImmutableList.Builder<Map.Entry<String, String>> content = ImmutableList.builder();
        content.add(Maps.immutableEntry("luckperms-verbose.md", prettyOutput.build().stream().collect(Collectors.joining("\n"))));

        if (attachRaw) {
            content.add(Maps.immutableEntry("raw-data.csv", csvOutput.build().stream().collect(Collectors.joining("\n"))));
        }

        return PasteUtils.paste("LuckPerms Verbose Checking Output", content.build());
    }

    /**
     * Reads a stack trace from a {@link CheckData} instance.
     *
     * @param data the data to read from
     * @param truncateLength the length when we should stop reading the stack
     * @param consumer the element consumer
     * @return how many elements were left unread, or 0 if everything was read
     */
    private static int readStack(CheckData data, int truncateLength, Consumer<StackTraceElement> consumer) {
        StackTraceElement[] stack = data.getCheckTrace();

        // how many lines have been printed
        int count = 0;
        // if we're printing elements yet
        boolean printing = false;

        for (StackTraceElement e : stack) {
            // start printing when we escape LP internals code
            boolean shouldStartPrinting = !printing && (
                    (data.getCheckOrigin() == CheckOrigin.API || data.getCheckOrigin() == CheckOrigin.INTERNAL) || (
                            !e.getClassName().startsWith("me.lucko.luckperms.") &&
                            // all used within the checking impl somewhere
                            !e.getClassName().equals("java.util.concurrent.CompletableFuture") &&
                            !e.getClassName().startsWith("com.github.benmanes.caffeine") &&
                            !e.getClassName().equals("java.util.concurrent.ConcurrentHashMap")
                    )
            );

            if (shouldStartPrinting) {
                printing = true;
            }

            if (!printing) continue;
            if (count >= truncateLength) break;

            consumer.accept(e);
            count++;
        }

        if (stack.length > truncateLength) {
            return stack.length - truncateLength;
        }
        return 0;
    }

    private static String escapeCommas(String s) {
        return s.contains(",") ? "\"" + s + "\"" : s;
    }

    private static String getTristateColor(Tristate tristate) {
        switch (tristate) {
            case TRUE:
                return "&2";
            case FALSE:
                return "&c";
            default:
                return "&7";
        }
    }

    private static String getTristateSymbol(Tristate tristate) {
        switch (tristate) {
            case TRUE:
                return "✔️";
            case FALSE:
                return "❌";
            default:
                return "❔";
        }
    }

}
