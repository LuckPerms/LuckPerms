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

package me.lucko.luckperms.standalone;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import me.lucko.luckperms.common.commands.misc.ExportCommand;
import me.lucko.luckperms.common.commands.misc.ImportCommand;
import me.lucko.luckperms.common.model.Group;
import me.lucko.luckperms.common.model.Track;
import me.lucko.luckperms.common.model.User;
import me.lucko.luckperms.common.node.types.Inheritance;
import me.lucko.luckperms.common.node.types.Permission;
import me.lucko.luckperms.standalone.app.integration.CommandExecutor;
import me.lucko.luckperms.standalone.utils.TestPluginProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.zip.GZIPInputStream;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

public class ImportExportIntegrationTest {

    @Test
    public void testRoundTrip(@TempDir Path tempDirA, @TempDir Path tempDirB) throws IOException {
        Path path = tempDirA.resolve("testfile.json.gz");

        // run an export on environment A
        TestPluginProvider.use(tempDirA, (app, bootstrap, plugin) -> {
            CommandExecutor executor = app.getCommandExecutor();

            plugin.getStorage().savePlayerData(UUID.fromString("c1d60c50-70b5-4722-8057-87767557e50d"), "Luck").join();

            executor.execute("creategroup test").join();
            executor.execute("group test permission set test.permission true").join();
            executor.execute("createtrack test").join();
            executor.execute("track test append default").join();
            executor.execute("user Luck permission set hello").join();

            executor.execute("export testfile").join();

            ExportCommand exportCommand = (ExportCommand) plugin.getCommandManager().getMainCommands().get("export");
            await().atMost(10, TimeUnit.SECONDS).until(() -> !exportCommand.isRunning());
        });

        // check the export contains the expected data
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new GZIPInputStream(Files.newInputStream(path))))) {
            JsonObject obj = new Gson().fromJson(reader, JsonObject.class);
            assertEquals(2, obj.get("groups").getAsJsonObject().size());
            assertEquals(1, obj.get("users").getAsJsonObject().size());
            assertEquals(1, obj.get("tracks").getAsJsonObject().size());
        }

        // copy the export file from environment A to environment B
        Files.copy(path, tempDirB.resolve("testfile.json.gz"));

        // import the file on environment B
        TestPluginProvider.use(tempDirB, (app, bootstrap, plugin) -> {
            CommandExecutor executor = app.getCommandExecutor();
            assertNull(plugin.getGroupManager().getIfLoaded("test"));

            executor.execute("import testfile").join();

            ImportCommand importCommand = (ImportCommand) plugin.getCommandManager().getMainCommands().get("import");
            await().atMost(10, TimeUnit.SECONDS).until(() -> !importCommand.isRunning());

            // assert that the expected objects exist
            Group testGroup = plugin.getGroupManager().getIfLoaded("test");
            assertNotNull(testGroup);
            assertEquals(ImmutableList.of(Permission.builder().permission("test.permission").build()), testGroup.normalData().asList());

            Track testTrack = plugin.getTrackManager().getIfLoaded("test");
            assertNotNull(testTrack);
            assertEquals(ImmutableList.of("default"), testTrack.getGroups());

            User testUser = plugin.getStorage().loadUser(UUID.fromString("c1d60c50-70b5-4722-8057-87767557e50d"), null).join();
            assertNotNull(testUser);
            assertEquals("luck", testUser.getUsername().orElse(null));
            assertEquals(ImmutableSet.of(
                    Permission.builder().permission("hello").build(),
                    Inheritance.builder().group("default").build()
            ), testUser.normalData().asSet());
        });
    }
}
