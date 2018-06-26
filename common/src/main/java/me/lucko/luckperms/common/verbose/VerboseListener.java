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

import com.google.gson.JsonObject;

import me.lucko.luckperms.api.Tristate;
import me.lucko.luckperms.common.command.CommandManager;
import me.lucko.luckperms.common.command.utils.MessageUtils;
import me.lucko.luckperms.common.locale.message.Message;
import me.lucko.luckperms.common.sender.Sender;
import me.lucko.luckperms.common.utils.DurationFormatter;
import me.lucko.luckperms.common.utils.StackTracePrinter;
import me.lucko.luckperms.common.utils.TextUtils;
import me.lucko.luckperms.common.utils.gson.JArray;
import me.lucko.luckperms.common.utils.gson.JObject;
import me.lucko.luckperms.common.web.StandardPastebin;

import net.kyori.text.TextComponent;
import net.kyori.text.event.HoverEvent;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

/**
 * Accepts and processes {@link CheckData}, passed from the {@link VerboseHandler}.
 */
public class VerboseListener {
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss z");

    // how much data should we store before stopping.
    private static final int DATA_TRUNCATION = 10000;
    // how many lines should we include in each stack trace send as a chat message
    private static final int STACK_TRUNCATION_CHAT = 15;
    // how many lines should we include in each stack trace in the web output
    private static final int STACK_TRUNCATION_WEB = 30;

    private static final StackTracePrinter FILTERING_PRINTER = StackTracePrinter.builder()
            .ignoreClassStartingWith("me.lucko.luckperms.")
            .ignoreClassStartingWith("com.github.benmanes.caffeine")
            .ignoreClass("java.util.concurrent.CompletableFuture")
            .ignoreClass("java.util.concurrent.ConcurrentHashMap")
            .build();

    private static final StackTracePrinter CHAT_FILTERED_PRINTER = FILTERING_PRINTER.toBuilder()
            .truncateLength(STACK_TRUNCATION_CHAT)
            .build();

    private static final StackTracePrinter CHAT_UNFILTERED_PRINTER = StackTracePrinter.builder()
            .truncateLength(STACK_TRUNCATION_CHAT)
            .build();

    private static final StackTracePrinter WEB_FILTERED_PRINTER = FILTERING_PRINTER.toBuilder()
            .truncateLength(STACK_TRUNCATION_WEB)
            .build();

    private static final StackTracePrinter WEB_UNFILTERED_PRINTER = StackTracePrinter.builder()
            .truncateLength(STACK_TRUNCATION_WEB)
            .build();

    // the time when the listener was first registered
    private final long startTime = System.currentTimeMillis();
    // the sender to notify each time the listener processes a check which passes the filter
    private final Sender notifiedSender;
    // the filter
    private final VerboseFilter filter;
    // if we should notify the sender
    private final boolean notify;
    // the number of checks we have processed
    private final AtomicInteger counter = new AtomicInteger(0);
    // the number of checks we have processed and accepted, based on the filter rules for this
    // listener
    private final AtomicInteger matchedCounter = new AtomicInteger(0);
    // the checks which passed the filter, up to a max size of #DATA_TRUNCATION
    private final List<CheckData> results = new ArrayList<>(DATA_TRUNCATION / 10);

    public VerboseListener(Sender notifiedSender, VerboseFilter filter, boolean notify) {
        this.notifiedSender = notifiedSender;
        this.filter = filter;
        this.notify = notify;
    }

    /**
     * Accepts and processes check data.
     *
     * @param data the data to process
     */
    public void acceptData(CheckData data) {
        // increment handled counter
        this.counter.incrementAndGet();

        // check if the data passes our filter
        if (!this.filter.evaluate(data)) {
            return;
        }

        // increment the matched filter
        this.matchedCounter.incrementAndGet();

        // record the check, if we have space for it
        if (this.results.size() < DATA_TRUNCATION) {
            this.results.add(data);
        }

        // handle notifications
        if (this.notify) {
            sendNotification(data);
        }
    }

    private void sendNotification(CheckData data) {
        if (this.notifiedSender.isConsole()) {
            // just send as a raw message
            Message.VERBOSE_LOG.send(this.notifiedSender,
                    data.getCheckTarget(),
                    data.getPermission(),
                    getTristateColor(data.getResult()),
                    data.getResult().name().toLowerCase()
            );
            return;
        }

        // form a hoverevent from the check trace
        TextComponent textComponent = Message.VERBOSE_LOG.asComponent(this.notifiedSender.getPlugin().getLocaleManager(),
                data.getCheckTarget(),
                data.getPermission(),
                getTristateColor(data.getResult()),
                data.getResult().name().toLowerCase()
        );

        // build the text
        List<String> hover = new ArrayList<>();
        hover.add("&bOrigin: &2" + data.getCheckOrigin().name());
        hover.add("&bContext: &r" + MessageUtils.contextSetToString(this.notifiedSender.getPlugin().getLocaleManager(), data.getCheckContext()));
        hover.add("&bTrace: &r");

        Consumer<StackTraceElement> printer = StackTracePrinter.elementToString(str -> hover.add("&7" + str));
        int overflow;
        if (data.getCheckOrigin() == CheckOrigin.API || data.getCheckOrigin() == CheckOrigin.INTERNAL) {
            overflow = CHAT_UNFILTERED_PRINTER.process(data.getCheckTrace(), printer);
        } else {
            overflow = CHAT_FILTERED_PRINTER.process(data.getCheckTrace(), printer);
        }
        if (overflow != 0) {
            hover.add("&f... and " + overflow + " more");
        }

        // send the message
        HoverEvent hoverEvent = new HoverEvent(HoverEvent.Action.SHOW_TEXT, TextUtils.fromLegacy(TextUtils.joinNewline(hover.stream()), CommandManager.AMPERSAND_CHAR));
        TextComponent text = textComponent.toBuilder().applyDeep(comp -> comp.hoverEvent(hoverEvent)).build();
        this.notifiedSender.sendMessage(text);
    }

    /**
     * Uploads the captured data in this listener to a paste and returns the url
     *
     * @return the url
     */
    public String uploadPasteData() {

        // retrieve variables
        long now = System.currentTimeMillis();
        String startDate = DATE_FORMAT.format(new Date(this.startTime));
        String endDate = DATE_FORMAT.format(new Date(now));
        long secondsTaken = (now - this.startTime) / 1000L;
        String duration = DurationFormatter.CONCISE.format(secondsTaken);

        String filter;
        if (this.filter.isBlank()){
            filter = "any";
        } else {
            filter = this.filter.toString();
        }

        boolean truncated = this.matchedCounter.get() > this.results.size();

        JObject metadata = new JObject()
                .add("startTime", startDate)
                .add("endTime", endDate)
                .add("duration", duration)
                .add("count", new JObject()
                        .add("matched", this.matchedCounter.get())
                        .add("total", this.counter.get())
                )
                .add("uploader", new JObject()
                        .add("name", this.notifiedSender.getNameWithLocation())
                        .add("uuid", this.notifiedSender.getUuid().toString())
                )
                .add("filter", filter)
                .add("truncated", truncated);

        JArray data = new JArray();
        for (CheckData c : this.results) {
            if (c.getCheckOrigin() == CheckOrigin.API || c.getCheckOrigin() == CheckOrigin.INTERNAL) {
                data.add(c.toJson(WEB_UNFILTERED_PRINTER));
            } else {
                data.add(c.toJson(WEB_FILTERED_PRINTER));
            }
        }
        this.results.clear();

        JsonObject payload = new JObject()
                .add("metadata", metadata)
                .add("data", data)
                .toJson();

        return StandardPastebin.BYTEBIN.postJson(payload, true).id();
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

    public Sender getNotifiedSender() {
        return this.notifiedSender;
    }
}
