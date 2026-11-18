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

package me.lucko.luckperms.common.storage.implementation.file;

import me.lucko.luckperms.common.actionlog.ActionJsonSerializer;
import me.lucko.luckperms.common.actionlog.LogPage;
import me.lucko.luckperms.common.actionlog.LoggedAction;
import me.lucko.luckperms.common.actionlog.filter.ActionFilters;
import me.lucko.luckperms.common.plugin.LuckPermsPlugin;
import net.luckperms.api.actionlog.Action;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;

public class FileActionLoggerTest {

    private static final UUID SOURCE = UUID.fromString("725d585e-4ff1-4f18-acca-6ac538364080");
    private static final UUID TARGET = UUID.fromString("c1d60c50-70b5-4722-8057-87767557e50d");

    // Between the 5.0 serialization changes and the fix for #1843, the target's
    // uniqueId and name were written next to the "target" object instead of inside it.
    @Test
    public void testMisplacedTargetEntriesAreSkipped(@TempDir Path dir) throws Exception {
        LoggedAction valid = LoggedAction.build()
                .timestamp(Instant.ofEpochSecond(1700000000L))
                .source(SOURCE)
                .sourceName("Luck")
                .targetType(Action.Target.Type.USER)
                .target(TARGET)
                .targetName("Notch")
                .description("permission set test true")
                .build();

        Path contentFile = dir.resolve("actions.txt");
        Files.write(contentFile, Arrays.asList(
                "{\"timestamp\":1575000000," +
                        "\"source\":{\"uniqueId\":\"" + SOURCE + "\",\"name\":\"Luck\"}," +
                        "\"target\":{\"type\":\"USER\"}," +
                        "\"uniqueId\":\"" + TARGET + "\",\"name\":\"Notch\"," +
                        "\"description\":\"permission set test true\"}",
                "{\"timestamp\":1575000001," +
                        "\"source\":{\"uniqueId\":\"" + SOURCE + "\",\"name\":\"Luck\"}," +
                        "\"target\":{\"type\":\"GROUP\"}," +
                        "\"name\":\"admin\"," +
                        "\"description\":\"permission set test true\"}",
                ActionJsonSerializer.serialize(valid).toString()
        ), StandardCharsets.UTF_8);

        FileActionLogger logger = new FileActionLogger(mock(LuckPermsPlugin.class, RETURNS_DEEP_STUBS));
        logger.init(contentFile, dir.resolve("actions.json"));

        PrintStream err = System.err;
        ByteArrayOutputStream errContent = new ByteArrayOutputStream();
        LogPage page;
        try {
            System.setErr(new PrintStream(errContent, true, "UTF-8"));
            page = logger.getLogPage(ActionFilters.all(), null);
        } finally {
            System.setErr(err);
        }

        assertEquals(Collections.singletonList(valid), page.getContent());
        assertEquals("", errContent.toString("UTF-8"));
    }

}
