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

import me.lucko.luckperms.common.model.Group;
import me.lucko.luckperms.common.model.User;
import me.lucko.luckperms.common.node.types.DisplayName;
import me.lucko.luckperms.common.node.types.Prefix;
import me.lucko.luckperms.common.node.types.Suffix;
import me.lucko.luckperms.standalone.app.integration.CommandExecutor;
import me.lucko.luckperms.standalone.utils.TestPluginProvider;
import me.lucko.luckperms.standalone.utils.TestSender;
import net.luckperms.api.node.NodeType;
import net.luckperms.api.util.Tristate;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ChatMetaTabCompletionTest {

    private static final Map<String, String> CONFIG = Map.of(
            "log-notify", "false",
            "commands-rate-limit", "false",
            "server", "survival"
    );

    @Test
    public void testUserCompletionByNameAndUniqueId(@TempDir Path tempDir) {
        TestPluginProvider.use(tempDir, CONFIG, (app, bootstrap, plugin) -> {
            CommandExecutor executor = app.getCommandExecutor();
            UUID uniqueId = UUID.randomUUID();
            User user = plugin.getUserManager().getOrMake(uniqueId, "TestUser");
            user.normalData().add(Prefix.builder("&8[&#ECC97DMember&8] ", 1).build());
            user.normalData().add(Prefix.builder("&7Admin", 2).build());
            user.normalData().add(Suffix.builder(" &aSuffix", 1).build());

            assertEquals(Set.of("1", "2"),
                    Set.copyOf(executor.tabComplete("user testuser meta setprefix ")));
            assertEquals(List.of("\"&8[&#ECC97DMember&8] \" "),
                    executor.tabComplete("user " + uniqueId + " meta setprefix 1 "));
            assertEquals(List.of("2"),
                    executor.tabComplete("user TestUser meta setprefix 2"));
            assertEquals(List.of("\"&7Admin\" "),
                    executor.tabComplete("user TestUser meta setprefix 2 "));
            assertEquals(List.of("\"&7Admin\" "),
                    executor.tabComplete("user TestUser meta setprefix 2 \"&7a"));
            assertEquals(List.of("\" &aSuffix\" "),
                    executor.tabComplete("user TestUser meta setsuffix "));
            assertEquals(List.of(), executor.tabComplete("user TestUser meta setprefix 99 "));
        });
    }

    @Test
    public void testGroupCompletionAndQuotedValueExecution(@TempDir Path tempDir) {
        TestPluginProvider.use(tempDir, CONFIG, (app, bootstrap, plugin) -> {
            CommandExecutor executor = app.getCommandExecutor();
            Group group = plugin.getGroupManager().getOrMake("test");
            group.normalData().add(DisplayName.builder("TestGroup").build());
            group.normalData().add(Prefix.builder("&aMember Rank ", 10).build());
            group.normalData().add(Prefix.builder("&cLeader", 20).build());
            group.normalData().add(Suffix.builder(" &bSuffix", 10).build());

            List<String> prefixes = executor.tabComplete("group test meta setprefix ");
            assertEquals(Set.of("10", "20"), Set.copyOf(prefixes));
            List<String> prefixValue = executor.tabComplete("group test meta setprefix 10 ");
            assertEquals(List.of("\"&aMember Rank \" "), prefixValue);
            assertEquals(prefixValue, executor.tabComplete("group test meta setprefix 10 \"&amember R"));
            assertEquals(List.of("\" &bSuffix\" "),
                    executor.tabComplete("group test meta setsuffix 10 "));

            executor.execute("group test meta setprefix " + prefixes.get(0) + " " + prefixValue.get(0)).join();
            assertEquals(true, group.normalData().asList().stream()
                    .filter(NodeType.PREFIX::matches)
                    .map(NodeType.PREFIX::cast)
                    .anyMatch(n -> n.getMetaValue().equals("&aMember Rank ") && n.getPriority() == 10));
        });
    }

    @Test
    public void testContextsAndOtherCommandsStillComplete(@TempDir Path tempDir) {
        TestPluginProvider.use(tempDir, CONFIG, (app, bootstrap, plugin) -> {
            CommandExecutor executor = app.getCommandExecutor();
            plugin.getGroupManager().getOrMake("test");
            plugin.getTrackManager().getOrMake("staff");

            assertEquals(List.of("server=survival"),
                    executor.tabComplete("group test meta setprefix \"Member Rank\" server="));
            assertEquals(List.of("server=survival"),
                    executor.tabComplete("group test meta setprefix 10 \"Member Rank\" server="));
            assertEquals(List.of("server=survival"),
                    executor.tabComplete("group test setdisplayname Test server="));
            assertEquals(List.of("staff"), executor.tabComplete("user MissingUser promote "));
            assertEquals(List.of("true", "false"),
                    executor.tabComplete("group test permission set example.permission "));
        });
    }

    @Test
    public void testMissingTargetsAndUnauthorizedSenders(@TempDir Path tempDir) {
        TestPluginProvider.use(tempDir, CONFIG, (app, bootstrap, plugin) -> {
            CommandExecutor executor = app.getCommandExecutor();
            Group group = plugin.getGroupManager().getOrMake("test");
            group.normalData().add(Prefix.builder("&aMember", 1).build());
            TestSender sender = new TestSender();
            sender.setPermissionChecker(permission -> Tristate.FALSE);

            assertEquals(List.of(), executor.tabComplete("user MissingUser meta setprefix "));
            assertEquals(List.of(), executor.tabComplete("user " + UUID.randomUUID() + " meta setprefix "));
            assertEquals(List.of(), executor.tabComplete("group missing meta setprefix "));
            assertEquals(List.of(), executor.tabComplete(sender, "group test meta setprefix "));
        });
    }
}
