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
import me.lucko.luckperms.common.node.types.Inheritance;
import me.lucko.luckperms.standalone.app.integration.CommandExecutor;
import me.lucko.luckperms.standalone.app.integration.SingletonPlayer;
import me.lucko.luckperms.standalone.utils.CommandTester;
import me.lucko.luckperms.standalone.utils.TestPluginProvider;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.luckperms.api.event.log.LogNotifyEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.testcontainers.shaded.com.google.common.collect.ImmutableMap;
import org.testcontainers.shaded.com.google.common.collect.ImmutableSet;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class CommandsIntegrationTest {
    
    private static final Map<String, String> CONFIG = ImmutableMap.<String, String>builder()
            .put("log-notify", "false")
            .build();

    @Test
    public void testGroupCommands(@TempDir Path tempDir) {
        TestPluginProvider.use(tempDir, CONFIG, (app, bootstrap, plugin) -> {
            CommandExecutor executor = app.getCommandExecutor();

            new CommandTester(plugin, executor)
                    .givenHasPermissions("luckperms.creategroup")
                    .whenRunCommand("creategroup test")
                    .thenExpect("[LP] test was successfully created.")

                    .givenHasPermissions("luckperms.creategroup")
                    .whenRunCommand("creategroup test2")
                    .thenExpect("[LP] test2 was successfully created.")

                    .givenHasPermissions("luckperms.deletegroup")
                    .whenRunCommand("deletegroup test2")
                    .thenExpect("[LP] test2 was successfully deleted.")

                    .givenHasPermissions("luckperms.listgroups")
                    .whenRunCommand("listgroups")
                    .thenExpect("""
                            [LP] Showing group entries:    (page 1 of 1 - 2 entries)
                            [LP] Groups: (name, weight, tracks)
                            [LP] -  default - 0
                            [LP] -  test - 0
                            """
                    )

                    .givenHasPermissions("luckperms.group.info")
                    .whenRunCommand("group test info")
                    .thenExpect("""
                            [LP] > Group Info: test
                            [LP] - Display Name: test
                            [LP] - Weight: None
                            [LP] - Contextual Data: (mode: server)
                            [LP]     Prefix: None
                            [LP]     Suffix: None
                            [LP]     Meta: None
                            """
                    )

                    .givenHasAllPermissions()
                    .whenRunCommand("group test meta set hello world")
                    .clearMessageBuffer()

                    .givenHasPermissions("luckperms.group.setweight")
                    .whenRunCommand("group test setweight 10")
                    .thenExpect("[LP] Set weight to 10 for group test.")

                    .givenHasPermissions("luckperms.group.setweight")
                    .whenRunCommand("group test setweight 100")
                    .thenExpect("[LP] Set weight to 100 for group test.")

                    .givenHasPermissions("luckperms.group.setdisplayname")
                    .whenRunCommand("group test setdisplayname Test")
                    .thenExpect("[LP] Set display name to Test for group test in context global.")

                    .givenHasPermissions("luckperms.group.setdisplayname")
                    .whenRunCommand("group test setdisplayname Dummy")
                    .thenExpect("[LP] Set display name to Dummy for group test in context global.")

                    .givenHasPermissions("luckperms.group.info")
                    .whenRunCommand("group Dummy info")
                    .thenExpect("""
                            [LP] > Group Info: test
                            [LP] - Display Name: Dummy
                            [LP] - Weight: 100
                            [LP] - Contextual Data: (mode: server)
                            [LP]     Prefix: None
                            [LP]     Suffix: None
                            [LP]     Meta: (weight=100) (hello=world)
                            """
                    )

                    .givenHasPermissions("luckperms.group.clone")
                    .whenRunCommand("group test clone testclone")
                    .thenExpect("[LP] test (Dummy) was successfully cloned onto testclone (Dummy).")

                    .givenHasPermissions("luckperms.group.info")
                    .whenRunCommand("group testclone info")
                    .thenExpect("""
                            [LP] > Group Info: testclone
                            [LP] - Display Name: Dummy
                            [LP] - Weight: 100
                            [LP] - Contextual Data: (mode: server)
                            [LP]     Prefix: None
                            [LP]     Suffix: None
                            [LP]     Meta: (weight=100) (hello=world)
                            """
                    )

                    .givenHasPermissions("luckperms.group.rename")
                    .whenRunCommand("group test rename test2")
                    .thenExpect("[LP] test (Dummy) was successfully renamed to test2 (Dummy).")

                    .givenHasPermissions("luckperms.group.info")
                    .whenRunCommand("group test2 info")
                    .thenExpect("""
                            [LP] > Group Info: test2
                            [LP] - Display Name: Dummy
                            [LP] - Weight: 100
                            [LP] - Contextual Data: (mode: server)
                            [LP]     Prefix: None
                            [LP]     Suffix: None
                            [LP]     Meta: (weight=100) (hello=world)
                            """
                    )

                    .givenHasPermissions("luckperms.listgroups")
                    .whenRunCommand("listgroups")
                    .thenExpect("""
                            [LP] Showing group entries:    (page 1 of 1 - 3 entries)
                            [LP] Groups: (name, weight, tracks)
                            [LP] -  test2 (Dummy) - 100
                            [LP] -  testclone (Dummy) - 100
                            [LP] -  default - 0
                            """
                    );
        });
    }

    @Test
    public void testGroupPermissionCommands(@TempDir Path tempDir) {
        TestPluginProvider.use(tempDir, CONFIG, (app, bootstrap, plugin) -> {
            CommandExecutor executor = app.getCommandExecutor();

            new CommandTester(plugin, executor)
                    .whenRunCommand("creategroup test")
                    .clearMessageBuffer()

                    .givenHasPermissions("luckperms.group.permission.set")
                    .whenRunCommand("group test permission set test.node true")
                    .thenExpect("[LP] Set test.node to true for test in context global.")

                    .givenHasPermissions("luckperms.group.permission.set")
                    .whenRunCommand("group test permission set test.node.other false server=test")
                    .thenExpect("[LP] Set test.node.other to false for test in context server=test.")

                    .givenHasPermissions("luckperms.group.permission.set")
                    .whenRunCommand("group test permission set test.node.other false server=test world=test2")
                    .thenExpect("[LP] Set test.node.other to false for test in context server=test, world=test2.")

                    .givenHasPermissions("luckperms.group.permission.settemp")
                    .whenRunCommand("group test permission settemp abc true 1h")
                    .thenExpect("[LP] Set abc to true for test for a duration of 1 hour in context global.")

                    .givenHasPermissions("luckperms.group.permission.settemp")
                    .whenRunCommand("group test permission settemp abc true 2h replace")
                    .thenExpect("[LP] Set abc to true for test for a duration of 2 hours in context global.")

                    .givenHasPermissions("luckperms.group.permission.unsettemp")
                    .whenRunCommand("group test permission unsettemp abc")
                    .thenExpect("[LP] Unset temporary permission abc for test in context global.")

                    .givenHasPermissions("luckperms.group.permission.info")
                    .whenRunCommand("group test permission info")
                    .thenExpect("""
                            [LP] test's Permissions:  (page 1 of 1 - 3 entries)
                            > test.node.other (server=test) (world=test2)
                            > test.node.other (server=test)
                            > test.node
                            """
                    )

                    .givenHasPermissions("luckperms.group.permission.unset")
                    .whenRunCommand("group test permission unset test.node")
                    .thenExpect("[LP] Unset test.node for test in context global.")

                    .givenHasPermissions("luckperms.group.permission.unset")
                    .whenRunCommand("group test permission unset test.node.other")
                    .thenExpect("[LP] test does not have test.node.other set in context global.")

                    .givenHasPermissions("luckperms.group.permission.unset")
                    .whenRunCommand("group test permission unset test.node.other server=test")
                    .thenExpect("[LP] Unset test.node.other for test in context server=test.")

                    .givenHasPermissions("luckperms.group.permission.check")
                    .whenRunCommand("group test permission check test.node.other")
                    .thenExpect("""
                            [LP] Permission information for test.node.other:
                            [LP] - test has test.node.other set to false in context server=test, world=test2.
                            [LP] - test does not inherit test.node.other.
                            [LP]
                            [LP] Permission check for test.node.other:
                            [LP]     Result: undefined
                            [LP]     Processor: None
                            [LP]     Cause: None
                            [LP]     Context: None
                            """
                    )

                    .givenHasPermissions("luckperms.group.permission.clear")
                    .whenRunCommand("group test permission clear server=test world=test2")
                    .thenExpect("[LP] test's permissions were cleared in context server=test, world=test2. (1 node was removed.)")

                    .givenHasPermissions("luckperms.group.permission.info")
                    .whenRunCommand("group test permission info")
                    .thenExpect("[LP] test does not have any permissions set.");
        });
    }

    @Test
    public void testGroupParentCommands(@TempDir Path tempDir) {
        TestPluginProvider.use(tempDir, CONFIG, (app, bootstrap, plugin) -> {
            CommandExecutor executor = app.getCommandExecutor();

            new CommandTester(plugin, executor)
                    .whenRunCommand("creategroup test")
                    .whenRunCommand("creategroup test2")
                    .whenRunCommand("creategroup test3")
                    .clearMessageBuffer()

                    .givenHasPermissions("luckperms.group.parent.add")
                    .whenRunCommand("group test parent add default")
                    .thenExpect("[LP] test now inherits permissions from default in context global.")

                    .givenHasPermissions("luckperms.group.parent.add")
                    .whenRunCommand("group test parent add test2 server=test")
                    .thenExpect("[LP] test now inherits permissions from test2 in context server=test.")

                    .givenHasPermissions("luckperms.group.parent.add")
                    .whenRunCommand("group test parent add test3 server=test")
                    .thenExpect("[LP] test now inherits permissions from test3 in context server=test.")

                    .givenHasPermissions("luckperms.group.parent.addtemp")
                    .whenRunCommand("group test parent addtemp test2 1d server=hello")
                    .thenExpect("[LP] test now inherits permissions from test2 for a duration of 1 day in context server=hello.")

                    .givenHasPermissions("luckperms.group.parent.removetemp")
                    .whenRunCommand("group test parent removetemp test2 server=hello")
                    .thenExpect("[LP] test no longer temporarily inherits permissions from test2 in context server=hello.")

                    .givenHasPermissions("luckperms.group.parent.info")
                    .whenRunCommand("group test parent info")
                    .thenExpect("""
                            [LP] test's Parents:  (page 1 of 1 - 3 entries)
                            > test2 (server=test)
                            > test3 (server=test)
                            > default
                            """
                    )

                    .givenHasPermissions("luckperms.group.parent.set")
                    .whenRunCommand("group test parent set test2 server=test")
                    .thenExpect("[LP] test had their existing parent groups cleared, and now only inherits test2 in context server=test.")

                    .givenHasPermissions("luckperms.group.parent.remove")
                    .whenRunCommand("group test parent remove test2 server=test")
                    .thenExpect("[LP] test no longer inherits permissions from test2 in context server=test.")

                    .givenHasPermissions("luckperms.group.parent.clear")
                    .whenRunCommand("group test parent clear")
                    .thenExpect("[LP] test's parents were cleared in context global. (1 node was removed.)")

                    .givenHasPermissions("luckperms.group.parent.info")
                    .whenRunCommand("group test parent info")
                    .thenExpect("[LP] test does not have any parents defined.");
        });
    }

    @Test
    public void testGroupMetaCommands(@TempDir Path tempDir) {
        TestPluginProvider.use(tempDir, CONFIG, (app, bootstrap, plugin) -> {
            CommandExecutor executor = app.getCommandExecutor();

            new CommandTester(plugin, executor)
                    .whenRunCommand("creategroup test")
                    .clearMessageBuffer()

                    .givenHasPermissions("luckperms.group.meta.info")
                    .whenRunCommand("group test meta info")
                    .thenExpect("""
                            [LP] test has no prefixes.
                            [LP] test has no suffixes.
                            [LP] test has no meta.
                            """
                    )

                    .givenHasPermissions("luckperms.group.meta.set")
                    .whenRunCommand("group test meta set hello world")
                    .thenExpect("[LP] Set meta key 'hello' to 'world' for test in context global.")


                    .givenHasPermissions("luckperms.group.meta.set")
                    .whenRunCommand("group test meta set hello world2 server=test")
                    .thenExpect("[LP] Set meta key 'hello' to 'world2' for test in context server=test.")

                    .givenHasPermissions("luckperms.group.meta.addprefix")
                    .whenRunCommand("group test meta addprefix 10 \"&ehello world\"")
                    .thenExpect("[LP] test had prefix 'hello world' set at a priority of 10 in context global.")

                    .givenHasPermissions("luckperms.group.meta.addsuffix")
                    .whenRunCommand("group test meta addsuffix 100 \"&ehi\"")
                    .thenExpect("[LP] test had suffix 'hi' set at a priority of 100 in context global.")

                    .givenHasPermissions("luckperms.group.meta.addsuffix")
                    .whenRunCommand("group test meta addsuffix 1 \"&6no\"")
                    .thenExpect("[LP] test had suffix 'no' set at a priority of 1 in context global.")

                    .givenHasPermissions("luckperms.group.meta.settemp")
                    .whenRunCommand("group test meta settemp abc xyz 1d server=hello")
                    .thenExpect("[LP] Set meta key 'abc' to 'xyz' for test for a duration of 1 day in context server=hello.")

                    .givenHasPermissions("luckperms.group.meta.addtempprefix")
                    .whenRunCommand("group test meta addtempprefix 1000 abc 1d server=hello")
                    .thenExpect("[LP] test had prefix 'abc' set at a priority of 1000 for a duration of 1 day in context server=hello.")

                    .givenHasPermissions("luckperms.group.meta.addtempsuffix")
                    .whenRunCommand("group test meta addtempsuffix 1000 xyz 3d server=hello")
                    .thenExpect("[LP] test had suffix 'xyz' set at a priority of 1000 for a duration of 3 days in context server=hello.")

                    .givenHasPermissions("luckperms.group.meta.unsettemp")
                    .whenRunCommand("group test meta unsettemp abc server=hello")
                    .thenExpect("[LP] Unset temporary meta key 'abc' for test in context server=hello.")

                    .givenHasPermissions("luckperms.group.meta.removetempprefix")
                    .whenRunCommand("group test meta removetempprefix 1000 abc server=hello")
                    .thenExpect("[LP] test had temporary prefix 'abc' at priority 1000 removed in context server=hello.")

                    .givenHasPermissions("luckperms.group.meta.removetempsuffix")
                    .whenRunCommand("group test meta removetempsuffix 1000 xyz server=hello")
                    .thenExpect("[LP] test had temporary suffix 'xyz' at priority 1000 removed in context server=hello.")

                    .givenHasPermissions("luckperms.group.meta.info")
                    .whenRunCommand("group test meta info")
                    .thenExpect("""
                            [LP] test's Prefixes
                            [LP] -> 10 - 'hello world' (inherited from self)
                            [LP] test's Suffixes
                            [LP] -> 100 - 'hi' (inherited from self)
                            [LP] -> 1 - 'no' (inherited from self)
                            [LP] test's Meta
                            [LP] -> hello = 'world2' (inherited from self) (server=test)
                            [LP] -> hello = 'world' (inherited from self)
                            """
                    )

                    .givenHasPermissions("luckperms.group.info")
                    .whenRunCommand("group test info")
                    .thenExpect("""
                            [LP] > Group Info: test
                            [LP] - Display Name: test
                            [LP] - Weight: None
                            [LP] - Contextual Data: (mode: server)
                            [LP]     Prefix: "hello world"
                            [LP]     Suffix: "hi"
                            [LP]     Meta: (hello=world)
                            """
                    )

                    .givenHasPermissions("luckperms.group.meta.unset")
                    .whenRunCommand("group test meta unset hello")
                    .thenExpect("[LP] Unset meta key 'hello' for test in context global.")

                    .givenHasPermissions("luckperms.group.meta.unset")
                    .whenRunCommand("group test meta unset hello server=test")
                    .thenExpect("[LP] Unset meta key 'hello' for test in context server=test.")

                    .givenHasPermissions("luckperms.group.meta.removeprefix")
                    .whenRunCommand("group test meta removeprefix 10")
                    .thenExpect("[LP] test had all prefixes at priority 10 removed in context global.")

                    .givenHasPermissions("luckperms.group.meta.removesuffix")
                    .whenRunCommand("group test meta removesuffix 100")
                    .thenExpect("[LP] test had all suffixes at priority 100 removed in context global.")

                    .givenHasPermissions("luckperms.group.meta.removesuffix")
                    .whenRunCommand("group test meta removesuffix 1")
                    .thenExpect("[LP] test had all suffixes at priority 1 removed in context global.")

                    .givenHasPermissions("luckperms.group.meta.info")
                    .whenRunCommand("group test meta info")
                    .thenExpect("""
                            [LP] test has no prefixes.
                            [LP] test has no suffixes.
                            [LP] test has no meta.
                            """
                    );
        });
    }

    @Test
    public void testUserCommands(@TempDir Path tempDir) {
        TestPluginProvider.use(tempDir, CONFIG, (app, bootstrap, plugin) -> {
            CommandExecutor executor = app.getCommandExecutor();

            plugin.getStorage().savePlayerData(UUID.fromString("c1d60c50-70b5-4722-8057-87767557e50d"), "Luck").join();
            plugin.getStorage().savePlayerData(UUID.fromString("069a79f4-44e9-4726-a5be-fca90e38aaf5"), "Notch").join();

            new CommandTester(plugin, executor)
                    .givenHasPermissions("luckperms.user.info")
                    .whenRunCommand("user Luck info")
                    .thenExpect("""
                            [LP] > User Info: luck
                            [LP] - UUID: c1d60c50-70b5-4722-8057-87767557e50d
                            [LP]     (type: official)
                            [LP] - Status: Offline
                            [LP] - Parent Groups:
                            [LP]     > default
                            [LP] - Contextual Data: (mode: server)
                            [LP]     Contexts: None
                            [LP]     Prefix: None
                            [LP]     Suffix: None
                            [LP]     Primary Group: default
                            [LP]     Meta: (primarygroup=default)
                            """
                    )

                    .givenHasPermissions("luckperms.user.info")
                    .whenRunCommand("user c1d60c50-70b5-4722-8057-87767557e50d info")
                    .thenExpect("""
                            [LP] > User Info: luck
                            [LP] - UUID: c1d60c50-70b5-4722-8057-87767557e50d
                            [LP]     (type: official)
                            [LP] - Status: Offline
                            [LP] - Parent Groups:
                            [LP]     > default
                            [LP] - Contextual Data: (mode: server)
                            [LP]     Contexts: None
                            [LP]     Prefix: None
                            [LP]     Suffix: None
                            [LP]     Primary Group: default
                            [LP]     Meta: (primarygroup=default)
                            """
                    )

                    .givenHasAllPermissions()
                    .whenRunCommand("creategroup admin")
                    .whenRunCommand("user Luck parent set admin")
                    .clearMessageBuffer()

                    .givenHasPermissions("luckperms.user.clone")
                    .whenRunCommand("user Luck clone Notch")
                    .thenExpect("[LP] luck was successfully cloned onto notch.")

                    .givenHasPermissions("luckperms.user.parent.info")
                    .whenRunCommand("user Notch parent info")
                    .thenExpect("""
                            [LP] notch's Parents:  (page 1 of 1 - 1 entries)
                            > admin
                            """
                    );
        });
    }

    @Test
    public void testUserPermissionCommands(@TempDir Path tempDir) {
        TestPluginProvider.use(tempDir, CONFIG, (app, bootstrap, plugin) -> {
            CommandExecutor executor = app.getCommandExecutor();

            plugin.getStorage().savePlayerData(UUID.fromString("c1d60c50-70b5-4722-8057-87767557e50d"), "Luck").join();

            new CommandTester(plugin, executor)
                    .givenHasPermissions("luckperms.user.permission.set")
                    .whenRunCommand("user Luck permission set test.node true")
                    .thenExpect("[LP] Set test.node to true for luck in context global.")

                    .givenHasPermissions("luckperms.user.permission.set")
                    .whenRunCommand("user Luck permission set test.node.other false server=test")
                    .thenExpect("[LP] Set test.node.other to false for luck in context server=test.")

                    .givenHasPermissions("luckperms.user.permission.set")
                    .whenRunCommand("user Luck permission set test.node.other false server=test world=test2")
                    .thenExpect("[LP] Set test.node.other to false for luck in context server=test, world=test2.")

                    .givenHasPermissions("luckperms.user.permission.settemp")
                    .whenRunCommand("user Luck permission settemp abc true 1h")
                    .thenExpect("[LP] Set abc to true for luck for a duration of 1 hour in context global.")

                    .givenHasPermissions("luckperms.user.permission.settemp")
                    .whenRunCommand("user Luck permission settemp abc true 2h replace")
                    .thenExpect("[LP] Set abc to true for luck for a duration of 2 hours in context global.")

                    .givenHasPermissions("luckperms.user.permission.unsettemp")
                    .whenRunCommand("user Luck permission unsettemp abc")
                    .thenExpect("[LP] Unset temporary permission abc for luck in context global.")

                    .givenHasPermissions("luckperms.user.permission.info")
                    .whenRunCommand("user Luck permission info")
                    .thenExpect("""
                            [LP] luck's Permissions:  (page 1 of 1 - 3 entries)
                            > test.node.other (server=test) (world=test2)
                            > test.node.other (server=test)
                            > test.node
                            """
                    )

                    .givenHasPermissions("luckperms.user.permission.unset")
                    .whenRunCommand("user Luck permission unset test.node")
                    .thenExpect("[LP] Unset test.node for luck in context global.")

                    .givenHasPermissions("luckperms.user.permission.unset")
                    .whenRunCommand("user Luck permission unset test.node.other")
                    .thenExpect("[LP] luck does not have test.node.other set in context global.")

                    .givenHasPermissions("luckperms.user.permission.unset")
                    .whenRunCommand("user Luck permission unset test.node.other server=test")
                    .thenExpect("[LP] Unset test.node.other for luck in context server=test.")

                    .givenHasPermissions("luckperms.user.permission.check")
                    .whenRunCommand("user Luck permission check test.node.other")
                    .thenExpect("""
                            [LP] Permission information for test.node.other:
                            [LP] - luck has test.node.other set to false in context server=test, world=test2.
                            [LP] - luck does not inherit test.node.other.
                            [LP]
                            [LP] Permission check for test.node.other:
                            [LP]     Result: undefined
                            [LP]     Processor: None
                            [LP]     Cause: None
                            [LP]     Context: None
                            """
                    )

                    .givenHasPermissions("luckperms.user.permission.clear")
                    .whenRunCommand("user Luck permission clear server=test world=test2")
                    .thenExpect("[LP] luck's permissions were cleared in context server=test, world=test2. (1 node was removed.)")

                    .givenHasPermissions("luckperms.user.permission.info")
                    .whenRunCommand("user Luck permission info")
                    .thenExpect("[LP] luck does not have any permissions set.");
        });
    }

    @Test
    public void testUserParentCommands(@TempDir Path tempDir) {
        TestPluginProvider.use(tempDir, CONFIG, (app, bootstrap, plugin) -> {
            CommandExecutor executor = app.getCommandExecutor();

            plugin.getStorage().savePlayerData(UUID.fromString("c1d60c50-70b5-4722-8057-87767557e50d"), "Luck").join();

            new CommandTester(plugin, executor)
                    .whenRunCommand("creategroup test2")
                    .whenRunCommand("creategroup test3")
                    .clearMessageBuffer()

                    .givenHasPermissions("luckperms.user.parent.add")
                    .whenRunCommand("user Luck parent add default")
                    .thenExpect("[LP] luck already inherits from default in context global.")

                    .givenHasPermissions("luckperms.user.parent.add")
                    .whenRunCommand("user Luck parent add test2 server=test")
                    .thenExpect("[LP] luck now inherits permissions from test2 in context server=test.")

                    .givenHasPermissions("luckperms.user.parent.add")
                    .whenRunCommand("user Luck parent add test3 server=test")
                    .thenExpect("[LP] luck now inherits permissions from test3 in context server=test.")

                    .givenHasPermissions("luckperms.user.parent.addtemp")
                    .whenRunCommand("user Luck parent addtemp test2 1d server=hello")
                    .thenExpect("[LP] luck now inherits permissions from test2 for a duration of 1 day in context server=hello.")

                    .givenHasPermissions("luckperms.user.parent.removetemp")
                    .whenRunCommand("user Luck parent removetemp test2 server=hello")
                    .thenExpect("[LP] luck no longer temporarily inherits permissions from test2 in context server=hello.")

                    .givenHasPermissions("luckperms.user.parent.info")
                    .whenRunCommand("user Luck parent info")
                    .thenExpect("""
                            [LP] luck's Parents:  (page 1 of 1 - 3 entries)
                            > test2 (server=test)
                            > test3 (server=test)
                            > default
                            """
                    )

                    .givenHasPermissions("luckperms.user.parent.set")
                    .whenRunCommand("user Luck parent set test2 server=test")
                    .thenExpect("[LP] luck had their existing parent groups cleared, and now only inherits test2 in context server=test.")

                    .givenHasPermissions("luckperms.user.parent.remove")
                    .whenRunCommand("user Luck parent remove test2 server=test")
                    .thenExpect("[LP] luck no longer inherits permissions from test2 in context server=test.")

                    .givenHasPermissions("luckperms.user.parent.clear")
                    .whenRunCommand("user Luck parent clear")
                    .thenExpect("[LP] luck's parents were cleared in context global. (0 nodes were removed.)")

                    .givenHasPermissions("luckperms.user.parent.info")
                    .whenRunCommand("user Luck parent info")
                    .thenExpect("""
                            [LP] luck's Parents:  (page 1 of 1 - 1 entries)
                            > default
                            """
                    );
        });
    }

    @Test
    public void testUserMetaCommands(@TempDir Path tempDir) {
        TestPluginProvider.use(tempDir, CONFIG, (app, bootstrap, plugin) -> {
            CommandExecutor executor = app.getCommandExecutor();

            plugin.getStorage().savePlayerData(UUID.fromString("c1d60c50-70b5-4722-8057-87767557e50d"), "Luck").join();

            new CommandTester(plugin, executor)
                    .givenHasPermissions("luckperms.user.meta.info")
                    .whenRunCommand("user Luck meta info")
                    .thenExpect("""
                            [LP] luck has no prefixes.
                            [LP] luck has no suffixes.
                            [LP] luck has no meta.
                            """
                    )

                    .givenHasPermissions("luckperms.user.meta.set")
                    .whenRunCommand("user Luck meta set hello world")
                    .thenExpect("[LP] Set meta key 'hello' to 'world' for luck in context global.")

                    .givenHasPermissions("luckperms.user.meta.set")
                    .whenRunCommand("user Luck meta set hello world2 server=test")
                    .thenExpect("[LP] Set meta key 'hello' to 'world2' for luck in context server=test.")

                    .givenHasPermissions("luckperms.user.meta.addprefix")
                    .whenRunCommand("user Luck meta addprefix 10 \"&ehello world\"")
                    .thenExpect("[LP] luck had prefix 'hello world' set at a priority of 10 in context global.")

                    .givenHasPermissions("luckperms.user.meta.addsuffix")
                    .whenRunCommand("user Luck meta addsuffix 100 \"&ehi\"")
                    .thenExpect("[LP] luck had suffix 'hi' set at a priority of 100 in context global.")

                    .givenHasPermissions("luckperms.user.meta.addsuffix")
                    .whenRunCommand("user Luck meta addsuffix 1 \"&6no\"")
                    .thenExpect("[LP] luck had suffix 'no' set at a priority of 1 in context global.")

                    .givenHasPermissions("luckperms.user.meta.settemp")
                    .whenRunCommand("user Luck meta settemp abc xyz 1d server=hello")
                    .thenExpect("[LP] Set meta key 'abc' to 'xyz' for luck for a duration of 1 day in context server=hello.")

                    .givenHasPermissions("luckperms.user.meta.addtempprefix")
                    .whenRunCommand("user Luck meta addtempprefix 1000 abc 1d server=hello")
                    .thenExpect("[LP] luck had prefix 'abc' set at a priority of 1000 for a duration of 1 day in context server=hello.")

                    .givenHasPermissions("luckperms.user.meta.addtempsuffix")
                    .whenRunCommand("user Luck meta addtempsuffix 1000 xyz 3d server=hello")
                    .thenExpect("[LP] luck had suffix 'xyz' set at a priority of 1000 for a duration of 3 days in context server=hello.")

                    .givenHasPermissions("luckperms.user.meta.unsettemp")
                    .whenRunCommand("user Luck meta unsettemp abc server=hello")
                    .thenExpect("[LP] Unset temporary meta key 'abc' for luck in context server=hello.")

                    .givenHasPermissions("luckperms.user.meta.removetempprefix")
                    .whenRunCommand("user Luck meta removetempprefix 1000 abc server=hello")
                    .thenExpect("[LP] luck had temporary prefix 'abc' at priority 1000 removed in context server=hello.")

                    .givenHasPermissions("luckperms.user.meta.removetempsuffix")
                    .whenRunCommand("user Luck meta removetempsuffix 1000 xyz server=hello")
                    .thenExpect("[LP] luck had temporary suffix 'xyz' at priority 1000 removed in context server=hello.")

                    .givenHasPermissions("luckperms.user.meta.info")
                    .whenRunCommand("user Luck meta info")
                    .thenExpect("""
                            [LP] luck's Prefixes
                            [LP] -> 10 - 'hello world' (inherited from self)
                            [LP] luck's Suffixes
                            [LP] -> 100 - 'hi' (inherited from self)
                            [LP] -> 1 - 'no' (inherited from self)
                            [LP] luck's Meta
                            [LP] -> hello = 'world2' (inherited from self) (server=test)
                            [LP] -> hello = 'world' (inherited from self)
                            """
                    )

                    .givenHasPermissions("luckperms.user.info")
                    .whenRunCommand("user Luck info")
                    .thenExpect("""
                            [LP] > User Info: luck
                            [LP] - UUID: c1d60c50-70b5-4722-8057-87767557e50d
                            [LP]     (type: official)
                            [LP] - Status: Offline
                            [LP] - Parent Groups:
                            [LP]     > default
                            [LP] - Contextual Data: (mode: server)
                            [LP]     Contexts: None
                            [LP]     Prefix: "hello world"
                            [LP]     Suffix: "hi"
                            [LP]     Primary Group: default
                            [LP]     Meta: (hello=world) (primarygroup=default)
                            """
                    )

                    .givenHasPermissions("luckperms.user.meta.unset")
                    .whenRunCommand("user Luck meta unset hello")
                    .thenExpect("[LP] Unset meta key 'hello' for luck in context global.")

                    .givenHasPermissions("luckperms.user.meta.unset")
                    .whenRunCommand("user Luck meta unset hello server=test")
                    .thenExpect("[LP] Unset meta key 'hello' for luck in context server=test.")

                    .givenHasPermissions("luckperms.user.meta.removeprefix")
                    .whenRunCommand("user Luck meta removeprefix 10")
                    .thenExpect("[LP] luck had all prefixes at priority 10 removed in context global.")

                    .givenHasPermissions("luckperms.user.meta.removesuffix")
                    .whenRunCommand("user Luck meta removesuffix 100")
                    .thenExpect("[LP] luck had all suffixes at priority 100 removed in context global.")

                    .givenHasPermissions("luckperms.user.meta.removesuffix")
                    .whenRunCommand("user Luck meta removesuffix 1")
                    .thenExpect("[LP] luck had all suffixes at priority 1 removed in context global.")

                    .givenHasPermissions("luckperms.user.meta.info")
                    .whenRunCommand("user Luck meta info")
                    .thenExpect("""
                            [LP] luck has no prefixes.
                            [LP] luck has no suffixes.
                            [LP] luck has no meta.
                            """
                    );
        });
    }

    @Test
    public void testTrackCommands(@TempDir Path tempDir) {
        TestPluginProvider.use(tempDir, CONFIG, (app, bootstrap, plugin) -> {
            CommandExecutor executor = app.getCommandExecutor();

            new CommandTester(plugin, executor)
                    .givenHasPermissions("luckperms.createtrack")
                    .whenRunCommand("createtrack test1")
                    .thenExpect("[LP] test1 was successfully created.")

                    .givenHasPermissions("luckperms.createtrack")
                    .whenRunCommand("createtrack test2")
                    .thenExpect("[LP] test2 was successfully created.")

                    .givenHasPermissions("luckperms.listtracks")
                    .whenRunCommand("listtracks")
                    .thenExpect("[LP] Tracks: test1, test2")

                    .givenHasPermissions("luckperms.deletetrack")
                    .whenRunCommand("deletetrack test2")
                    .thenExpect("[LP] test2 was successfully deleted.")

                    .givenHasAllPermissions()
                    .whenRunCommand("creategroup aaa")
                    .whenRunCommand("creategroup bbb")
                    .whenRunCommand("creategroup ccc")
                    .clearMessageBuffer()

                    .givenHasPermissions("luckperms.track.append")
                    .whenRunCommand("track test1 append bbb")
                    .thenExpect("[LP] Group bbb was appended to track test1.")

                    .givenHasPermissions("luckperms.track.insert")
                    .whenRunCommand("track test1 insert aaa 1")
                    .thenExpect("""
                            [LP] Group aaa was inserted into track test1 at position 1.
                            [LP] aaa ---> bbb
                            """
                    )

                    .givenHasPermissions("luckperms.track.insert")
                    .whenRunCommand("track test1 insert ccc 3")
                    .thenExpect("""
                            [LP] Group ccc was inserted into track test1 at position 3.
                            [LP] aaa ---> bbb ---> ccc
                            """
                    )

                    .givenHasPermissions("luckperms.track.info")
                    .whenRunCommand("track test1 info")
                    .thenExpect("""
                            [LP] > Showing Track: test1
                            [LP] - Path: aaa ---> bbb ---> ccc
                            """
                    )

                    .givenHasPermissions("luckperms.track.clone")
                    .whenRunCommand("track test1 clone testclone")
                    .thenExpect("[LP] test1 was successfully cloned onto testclone.")

                    .givenHasPermissions("luckperms.track.info")
                    .whenRunCommand("track testclone info")
                    .thenExpect("""
                            [LP] > Showing Track: testclone
                            [LP] - Path: aaa ---> bbb ---> ccc
                            """
                    )

                    .givenHasPermissions("luckperms.track.rename")
                    .whenRunCommand("track test1 rename test2")
                    .thenExpect("[LP] test1 was successfully renamed to test2.")

                    .givenHasPermissions("luckperms.listtracks")
                    .whenRunCommand("listtracks")
                    .thenExpect("[LP] Tracks: test2, testclone")

                    .givenHasPermissions("luckperms.track.info")
                    .whenRunCommand("track test2 info")
                    .thenExpect("""
                            [LP] > Showing Track: test2
                            [LP] - Path: aaa ---> bbb ---> ccc
                            """
                    )

                    .givenHasPermissions("luckperms.group.showtracks")
                    .whenRunCommand("group aaa showtracks")
                    .thenExpect("""
                            [LP] aaa's Tracks:
                            > test2:
                            (aaa ---> bbb ---> ccc)
                            > testclone:
                            (aaa ---> bbb ---> ccc)
                            """
                    )

                    .givenHasPermissions("luckperms.track.remove")
                    .whenRunCommand("track test2 remove bbb")
                    .thenExpect("""
                            [LP] Group bbb was removed from track test2.
                            [LP] aaa ---> ccc
                            """
                    )

                    .givenHasPermissions("luckperms.track.clear")
                    .whenRunCommand("track test2 clear")
                    .thenExpect("[LP] test2's groups track was cleared.");
        });
    }

    @Test
    public void testUserTrackCommands(@TempDir Path tempDir) {
        TestPluginProvider.use(tempDir, CONFIG, (app, bootstrap, plugin) -> {
            CommandExecutor executor = app.getCommandExecutor();

            plugin.getStorage().savePlayerData(UUID.fromString("c1d60c50-70b5-4722-8057-87767557e50d"), "Luck").join();

            new CommandTester(plugin, executor)
                    .whenRunCommand("createtrack staff")
                    .whenRunCommand("createtrack premium")

                    .whenRunCommand("creategroup mod")
                    .whenRunCommand("creategroup admin")

                    .whenRunCommand("creategroup vip")
                    .whenRunCommand("creategroup vip+")
                    .whenRunCommand("creategroup mvp")
                    .whenRunCommand("creategroup mvp+")

                    .whenRunCommand("track staff append mod")
                    .whenRunCommand("track staff append admin")
                    .whenRunCommand("track premium append vip")
                    .whenRunCommand("track premium append vip+")
                    .whenRunCommand("track premium append mvp")
                    .whenRunCommand("track premium append mvp+")

                    .clearMessageBuffer()

                    .givenHasPermissions("luckperms.user.promote")
                    .whenRunCommand("user Luck promote staff")
                    .thenExpect("[LP] luck isn't in any groups on staff, so they were added to the first group, mod in context global.")

                    .givenHasPermissions("luckperms.user.promote")
                    .whenRunCommand("user Luck promote staff")
                    .thenExpect("""
                            [LP] Promoting luck along track staff from mod to admin in context global.
                            [LP] mod ---> admin
                            """
                    )

                    .givenHasPermissions("luckperms.user.promote")
                    .whenRunCommand("user Luck promote staff")
                    .thenExpect("[LP] The end of track staff was reached, unable to promote luck.")

                    .givenHasPermissions("luckperms.user.demote")
                    .whenRunCommand("user Luck demote staff")
                    .thenExpect("""
                            [LP] Demoting luck along track staff from admin to mod in context global.
                            [LP] mod <--- admin
                            """
                    )

                    .givenHasPermissions("luckperms.user.demote")
                    .whenRunCommand("user Luck demote staff")
                    .thenExpect("[LP] The end of track staff was reached, so luck was removed from mod.")

                    .givenHasPermissions("luckperms.user.demote")
                    .whenRunCommand("user Luck demote staff")
                    .thenExpect("[LP] luck isn't already in any groups on staff.")

                    .givenHasPermissions("luckperms.user.promote")
                    .whenRunCommand("user Luck promote premium server=test1")
                    .thenExpect("[LP] luck isn't in any groups on premium, so they were added to the first group, vip in context server=test1.")

                    .givenHasPermissions("luckperms.user.promote")
                    .whenRunCommand("user Luck promote premium server=test2")
                    .thenExpect("[LP] luck isn't in any groups on premium, so they were added to the first group, vip in context server=test2.")

                    .givenHasPermissions("luckperms.user.promote")
                    .whenRunCommand("user Luck promote premium server=test1")
                    .thenExpect("""
                            [LP] Promoting luck along track premium from vip to vip+ in context server=test1.
                            [LP] vip ---> vip+ ---> mvp ---> mvp+
                            """
                    )

                    .givenHasPermissions("luckperms.user.promote")
                    .whenRunCommand("user Luck promote premium server=test2")
                    .thenExpect("""
                            [LP] Promoting luck along track premium from vip to vip+ in context server=test2.
                            [LP] vip ---> vip+ ---> mvp ---> mvp+
                            """
                    )

                    .givenHasPermissions("luckperms.user.parent.info")
                    .whenRunCommand("user Luck parent info")
                    .thenExpect("""
                            [LP] luck's Parents:  (page 1 of 1 - 3 entries)
                            > vip+ (server=test2)
                            > vip+ (server=test1)
                            > default
                            """
                    )

                    .givenHasPermissions("luckperms.user.showtracks")
                    .whenRunCommand("user Luck showtracks")
                    .thenExpect("""
                            [LP] luck's Tracks:
                            > premium: (server=test2)
                            (vip ---> vip+ ---> mvp ---> mvp+)
                            > premium: (server=test1)
                            (vip ---> vip+ ---> mvp ---> mvp+)
                            """
                    )

                    .givenHasPermissions("luckperms.user.demote")
                    .whenRunCommand("user Luck demote premium server=test1")
                    .thenExpect("""
                            [LP] Demoting luck along track premium from vip+ to vip in context server=test1.
                            [LP] vip <--- vip+ <--- mvp <--- mvp+
                            """
                    )

                    .givenHasPermissions("luckperms.user.demote")
                    .whenRunCommand("user Luck demote premium server=test2")
                    .thenExpect("""
                            [LP] Demoting luck along track premium from vip+ to vip in context server=test2.
                            [LP] vip <--- vip+ <--- mvp <--- mvp+
                            """
                    )

                    .givenHasPermissions("luckperms.user.demote")
                    .whenRunCommand("user Luck demote premium server=test1")
                    .thenExpect("[LP] The end of track premium was reached, so luck was removed from vip.")

                    .givenHasPermissions("luckperms.user.demote")
                    .whenRunCommand("user Luck demote premium server=test2")
                    .thenExpect("[LP] The end of track premium was reached, so luck was removed from vip.")

                    .givenHasPermissions("luckperms.user.parent.info")
                    .whenRunCommand("user Luck parent info")
                    .thenExpect("""
                            [LP] luck's Parents:  (page 1 of 1 - 1 entries)
                            > default
                            """
                    );
        });
    }

    @Test
    public void testSearchCommand(@TempDir Path tempDir) {
        TestPluginProvider.use(tempDir, CONFIG, (app, bootstrap, plugin) -> {
            CommandExecutor executor = app.getCommandExecutor();

            plugin.getStorage().savePlayerData(UUID.fromString("c1d60c50-70b5-4722-8057-87767557e50d"), "Luck").join();

            new CommandTester(plugin, executor)
                    .whenRunCommand("creategroup test")
                    .whenRunCommand("user Luck permission set hello.world true server=survival")
                    .whenRunCommand("group test permission set hello.world true world=nether")
                    .whenRunCommand("user Luck parent add test")
                    .clearMessageBuffer()

                    .givenHasPermissions("luckperms.search")
                    .whenRunCommand("search hello.world")
                    .thenExpect("""
                            [LP] Searching for users and groups with permissions == hello.world...
                            [LP] Found 2 entries from 1 users and 1 groups.
                            [LP] Showing user entries:    (page 1 of 1 - 1 entries)
                            > luck - true (server=survival)
                            [LP] Showing group entries:    (page 1 of 1 - 1 entries)
                            > test - true (world=nether)
                            """
                    )

                    .givenHasPermissions("luckperms.search")
                    .whenRunCommand("search ~~ group.%")
                    .thenExpect("""
                                [LP] Searching for users and groups with permissions ~~ group.%...
                                [LP] Found 2 entries from 2 users and 0 groups.
                                [LP] Showing user entries:    (page 1 of 1 - 2 entries)
                                > luck - (group.test) - true
                                > luck - (group.default) - true
                                """
                    );
        });
    }

    @Test
    public void testBulkUpdate(@TempDir Path tempDir) throws InterruptedException {
        Map<String, String> config = new HashMap<>(CONFIG);
        config.put("skip-bulkupdate-confirmation", "true");

        TestPluginProvider.use(tempDir, config, (app, bootstrap, plugin) -> {
            CommandExecutor executor = app.getCommandExecutor();

            plugin.getStorage().savePlayerData(UUID.fromString("c1d60c50-70b5-4722-8057-87767557e50d"), "Luck").join();
            plugin.getStorage().savePlayerData(UUID.fromString("069a79f4-44e9-4726-a5be-fca90e38aaf5"), "Notch").join();

            CountDownLatch completed = new CountDownLatch(1);

            SingletonPlayer.INSTANCE.addMessageSink(component -> {
                String plain = PlainTextComponentSerializer.plainText().serialize(component);
                if (plain.contains("Bulk update completed successfully")) {
                    completed.countDown();
                }
            });

            new CommandTester(plugin, executor)
                    .whenRunCommand("creategroup moderator")
                    .whenRunCommand("creategroup admin")
                    .whenRunCommand("user Luck parent add moderator server=survival")
                    .whenRunCommand("user Notch parent add moderator")
                    .whenRunCommand("group admin parent add moderator")
                    .whenRunCommand("group moderator rename mod")
                    .clearMessageBuffer()

                    .givenHasPermissions("luckperms.bulkupdate")
                    .whenRunCommand("bulkupdate all update permission group.mod \"permission == group.moderator\"")
                    .thenExpectStartsWith("[LP] Running bulk update.");

            assertTrue(completed.await(15, TimeUnit.SECONDS), "operation did not complete in the allotted time");

            Group adminGroup = plugin.getGroupManager().getIfLoaded("admin");
            assertNotNull(adminGroup);
            assertEquals(ImmutableSet.of(Inheritance.builder("mod").build()), adminGroup.normalData().asSet());

            User luckUser = plugin.getStorage().loadUser(UUID.fromString("c1d60c50-70b5-4722-8057-87767557e50d"), null).join();
            assertNotNull(luckUser);
            assertEquals(
                    ImmutableSet.of(
                            Inheritance.builder("default").build(),
                            Inheritance.builder("mod").withContext("server", "survival").build()
                    ),
                    luckUser.normalData().asSet()
            );

            User notchUser = plugin.getStorage().loadUser(UUID.fromString("069a79f4-44e9-4726-a5be-fca90e38aaf5"), null).join();
            assertNotNull(notchUser);
            assertEquals(
                    ImmutableSet.of(
                            Inheritance.builder("default").build(),
                            Inheritance.builder("mod").build()
                    ),
                    notchUser.normalData().asSet()
            );
        });
    }

    @Test
    public void testInvalidCommands(@TempDir Path tempDir) {
        TestPluginProvider.use(tempDir, CONFIG, (app, bootstrap, plugin) -> {
            CommandExecutor executor = app.getCommandExecutor();

            new CommandTester(plugin, executor)
                    .givenHasPermissions("luckperms.user.info")
                    .whenRunCommand("user unknown info")
                    .thenExpect("[LP] A user for unknown could not be found.")

                    .givenHasPermissions("luckperms.user.info")
                    .whenRunCommand("user unknown unknown")
                    .thenExpect("[LP] Command not recognised.")

                    .givenHasPermissions("luckperms.group.info")
                    .whenRunCommand("group unknown info")
                    .thenExpect("[LP] A group named unknown could not be found.")

                    .givenHasPermissions("luckperms.group.info")
                    .whenRunCommand("group unknown unknown")
                    .thenExpect("[LP] Command not recognised.")

                    .givenHasPermissions("luckperms.track.info")
                    .whenRunCommand("track unknown info")
                    .thenExpect("[LP] A track named unknown could not be found.")

                    .givenHasPermissions("luckperms.track.info")
                    .whenRunCommand("track unknown unknown")
                    .thenExpect("[LP] Command not recognised.");
        });
    }

    @Test
    public void testNoPermissions(@TempDir Path tempDir) {
        TestPluginProvider.use(tempDir, CONFIG, (app, bootstrap, plugin) -> {
            CommandExecutor executor = app.getCommandExecutor();
            String version = "v" + bootstrap.getVersion();

            new CommandTester(plugin, executor)
                    .givenHasPermissions(/* empty */)

                    .whenRunCommand("")
                    .thenExpect("""
                                [LP] Running LuckPerms %s.
                                [LP] It seems that no permissions have been setup yet!
                                [LP] Before you can use any of the LuckPerms commands in-game, you need to use the console to give yourself access.
                                [LP] Open your console and run:
                                [LP]  > lp user StandaloneUser permission set luckperms.* true
                                [LP] After you've done this, you can begin to define your permission assignments and groups.
                                [LP] Don't know where to start? Check here: https://luckperms.net/wiki/Usage
                                """.formatted(version)
                    )

                    .whenRunCommand("help")
                    .thenExpect("[LP] Running LuckPerms %s.".formatted(version))

                    .whenRunCommand("group default info")
                    .thenExpect("[LP] Running LuckPerms %s.".formatted(version));
        });
    }

    @Test
    public void testLogNotify(@TempDir Path tempDir) {
        Map<String, String> config = new HashMap<>(CONFIG);
        config.put("log-notify", "true");

        TestPluginProvider.use(tempDir, config, (app, bootstrap, plugin) -> {
            CommandExecutor executor = app.getCommandExecutor();

            // by default, notifications are not sent to the user who initiated the event - override that
            app.getApi().getEventBus().subscribe(LogNotifyEvent.class, e -> e.setCancelled(false));

            new CommandTester(plugin, executor)
                    .givenHasPermissions("luckperms.group.permission.set", "luckperms.log.notify")
                    .whenRunCommand("group default permission set hello.world true server=test")
                    .thenExpect("""
                                [LP] Set hello.world to true for default in context server=test.
                                [LP] LOG > (StandaloneUser) [G] (default)
                                [LP] LOG > permission set hello.world true server=test
                                """
                    );
        });
    }

    @Test
    public void testArgumentBasedCommandPermissions(@TempDir Path tempDir) {
        Map<String, String> config = new HashMap<>(CONFIG);
        config.put("argument-based-command-permissions", "true");

        TestPluginProvider.use(tempDir, config, (app, bootstrap, plugin) -> {
            CommandExecutor executor = app.getCommandExecutor();

            new CommandTester(plugin, executor)
                    .givenHasPermissions("luckperms.group.permission.set")
                    .whenRunCommand("group default permission set hello.world true server=test")
                    .thenExpect("[LP] You do not have permission to use this command!")

                    .givenHasPermissions(
                            "luckperms.group.permission.set",
                            "luckperms.group.permission.set.modify.default",
                            "luckperms.group.permission.set.usecontext.global",
                            "luckperms.group.permission.set.test.permission"
                    )
                    .whenRunCommand("group default permission set test.permission")
                    .thenExpect("[LP] Set test.permission to true for default in context global.")

                    .givenHasPermissions(
                            "luckperms.group.permission.unset",
                            "luckperms.group.permission.unset.modify.default",
                            "luckperms.group.permission.unset.usecontext.global",
                            "luckperms.group.permission.unset.test.permission"
                    )
                    .whenRunCommand("group default permission unset test.permission")
                    .thenExpect("[LP] Unset test.permission for default in context global.")

                    .givenHasPermissions(
                            "luckperms.group.permission.set",
                            "luckperms.group.permission.set.modify.default",
                            "luckperms.group.permission.set.usecontext.server.test",
                            "luckperms.group.permission.set.hello.world"
                    )
                    .whenRunCommand("group default permission set hello.world true server=test")
                    .thenExpect("[LP] Set hello.world to true for default in context server=test.")

                    .givenHasPermissions("luckperms.group.permission.info")
                    .whenRunCommand("group default permission info")
                    .thenExpect("[LP] You do not have permission to use this command!")

                    .givenHasPermissions(
                            "luckperms.group.permission.info",
                            "luckperms.group.permission.info.view.default"
                    )
                    .whenRunCommand("group default permission info")
                    .thenExpect("""
                                [LP] default's Permissions:  (page 1 of 1 - 1 entries)
                                > hello.world (server=test)
                                """
                    );
        });
    }

}
