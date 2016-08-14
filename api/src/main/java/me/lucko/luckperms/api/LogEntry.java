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

package me.lucko.luckperms.api;

import lombok.*;

import java.util.UUID;

@Getter
@Builder
@ToString
@EqualsAndHashCode
@AllArgsConstructor
public final class LogEntry implements Comparable<LogEntry> {
    private static final String FORMAT = "&8(&e%s&8) [&a%s&8] (&b%s&8) &7--> &f%s";

    @NonNull private final long timestamp;
    @NonNull private final UUID actor;
    @NonNull private final String actorName;
    @NonNull private final char type;
    private final UUID acted;
    @NonNull private final String actedName;
    @NonNull private final String action;

    @Override
    public int compareTo(LogEntry o) {
        return Long.compare(timestamp, o.getTimestamp());
    }

    public boolean matchesSearch(String query) {
        query = query.toLowerCase();
        return actorName.toLowerCase().contains(query) || actedName.toLowerCase().contains(query)
                || action.toLowerCase().contains(query);
    }

    public String getFormatted() {
        return String.format(FORMAT,
                getActorName(),
                Character.toString(getType()),
                getActedName(),
                getAction()
        );
    }
}
