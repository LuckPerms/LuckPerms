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

import me.lucko.luckperms.standalone.app.integration.CommandExecutor;
import me.lucko.luckperms.standalone.utils.CommandTester;
import me.lucko.luckperms.standalone.utils.TestPluginProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.UUID;

public class CommandsIntegrationTest {

    @Test
    public void testGroupCommands(@TempDir Path tempDir) {
        TestPluginProvider.use(tempDir, (app, bootstrap, plugin) -> {
            CommandExecutor executor = app.getCommandExecutor();

            new CommandTester(executor)
                    .givenCommand("creategroup test")
                    .thenExpect("[LP] test was successfully created.")

                    .givenCommand("creategroup test2")
                    .thenExpect("[LP] test2 was successfully created.")

                    .givenCommand("deletegroup test2")
                    .thenExpect("[LP] test2 was successfully deleted.")

                    .givenCommand("listgroups")
                    .thenExpect("""
                            [LP] Showing group entries:    (page 1 of 1 - 2 entries)
                            [LP] Groups: (name, weight, tracks)
                            [LP] -  default - 0
                            [LP] -  test - 0
                            """
                    )

                    .givenCommand("group test info")
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

                    .givenCommand("group test meta set hello world")
                    .clearMessageBuffer()

                    .givenCommand("group test setweight 10")
                    .thenExpect("[LP] Set weight to 10 for group test.")

                    .givenCommand("group test setweight 100")
                    .thenExpect("[LP] Set weight to 100 for group test.")

                    .givenCommand("group test setdisplayname Test")
                    .thenExpect("[LP] Set display name to Test for group test in context global.")

                    .givenCommand("group test setdisplayname Dummy")
                    .thenExpect("[LP] Set display name to Dummy for group test in context global.")

                    .givenCommand("group Dummy info")
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

                    .givenCommand("group test clone testclone")
                    .thenExpect("[LP] test (Dummy) was successfully cloned onto testclone (Dummy).")

                    .givenCommand("group testclone info")
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

                    .givenCommand("group test rename test2")
                    .thenExpect("[LP] test (Dummy) was successfully renamed to test2 (Dummy).")

                    .givenCommand("group test2 info")
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

                    .givenCommand("listgroups")
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
        TestPluginProvider.use(tempDir, (app, bootstrap, plugin) -> {
            CommandExecutor executor = app.getCommandExecutor();

            new CommandTester(executor)
                    .givenCommand("creategroup test")
                    .clearMessageBuffer()

                    .givenCommand("group test permission set test.node true")
                    .thenExpect("[LP] Set test.node to true for test in context global.")

                    .givenCommand("group test permission set test.node.other false server=test")
                    .thenExpect("[LP] Set test.node.other to false for test in context server=test.")

                    .givenCommand("group test permission set test.node.other false server=test world=test2")
                    .thenExpect("[LP] Set test.node.other to false for test in context server=test, world=test2.")

                    .givenCommand("group test permission settemp abc true 1h")
                    .thenExpect("[LP] Set abc to true for test for a duration of 1 hour in context global.")

                    .givenCommand("group test permission settemp abc true 2h replace")
                    .thenExpect("[LP] Set abc to true for test for a duration of 2 hours in context global.")

                    .givenCommand("group test permission unsettemp abc")
                    .thenExpect("[LP] Unset temporary permission abc for test in context global.")

                    .givenCommand("group test permission info")
                    .thenExpect("""
                            [LP] test's Permissions:  (page 1 of 1 - 3 entries)
                            > test.node.other (server=test) (world=test2)
                            > test.node.other (server=test)
                            > test.node
                            """
                    )

                    .givenCommand("group test permission unset test.node")
                    .thenExpect("[LP] Unset test.node for test in context global.")

                    .givenCommand("group test permission unset test.node.other")
                    .thenExpect("[LP] test does not have test.node.other set in context global.")

                    .givenCommand("group test permission unset test.node.other server=test")
                    .thenExpect("[LP] Unset test.node.other for test in context server=test.")

                    .givenCommand("group test permission check test.node.other")
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

                    .givenCommand("group test permission clear server=test world=test2")
                    .thenExpect("[LP] test's permissions were cleared in context server=test, world=test2. (1 node was removed.)")

                    .givenCommand("group test permission info")
                    .thenExpect("[LP] test does not have any permissions set.");
        });
    }

    @Test
    public void testGroupParentCommands(@TempDir Path tempDir) {
        TestPluginProvider.use(tempDir, (app, bootstrap, plugin) -> {
            CommandExecutor executor = app.getCommandExecutor();

            new CommandTester(executor)
                    .givenCommand("creategroup test")
                    .givenCommand("creategroup test2")
                    .givenCommand("creategroup test3")
                    .clearMessageBuffer()

                    .givenCommand("group test parent add default")
                    .thenExpect("[LP] test now inherits permissions from default in context global.")

                    .givenCommand("group test parent add test2 server=test")
                    .thenExpect("[LP] test now inherits permissions from test2 in context server=test.")

                    .givenCommand("group test parent add test3 server=test")
                    .thenExpect("[LP] test now inherits permissions from test3 in context server=test.")

                    .givenCommand("group test parent addtemp test2 1d server=hello")
                    .thenExpect("[LP] test now inherits permissions from test2 for a duration of 1 day in context server=hello.")

                    .givenCommand("group test parent removetemp test2 server=hello")
                    .thenExpect("[LP] test no longer temporarily inherits permissions from test2 in context server=hello.")

                    .givenCommand("group test parent info")
                    .thenExpect("""
                            [LP] test's Parents:  (page 1 of 1 - 3 entries)
                            > test2 (server=test)
                            > test3 (server=test)
                            > default
                            """
                    )

                    .givenCommand("group test parent set test2 server=test")
                    .thenExpect("[LP] test had their existing parent groups cleared, and now only inherits test2 in context server=test.")

                    .givenCommand("group test parent remove test2 server=test")
                    .thenExpect("[LP] test no longer inherits permissions from test2 in context server=test.")

                    .givenCommand("group test parent clear")
                    .thenExpect("[LP] test's parents were cleared in context global. (1 node was removed.)")

                    .givenCommand("group test parent info")
                    .thenExpect("[LP] test does not have any parents defined.");
        });
    }

    @Test
    public void testGroupMetaCommands(@TempDir Path tempDir) {
        TestPluginProvider.use(tempDir, (app, bootstrap, plugin) -> {
            CommandExecutor executor = app.getCommandExecutor();

            new CommandTester(executor)
                    .givenCommand("creategroup test")
                    .clearMessageBuffer()

                    .givenCommand("group test meta info")
                    .thenExpect("""
                            [LP] test has no prefixes.
                            [LP] test has no suffixes.
                            [LP] test has no meta.
                            """
                    )

                    .givenCommand("group test meta set hello world")
                    .thenExpect("[LP] Set meta key 'hello' to 'world' for test in context global.")

                    .givenCommand("group test meta set hello world2 server=test")
                    .thenExpect("[LP] Set meta key 'hello' to 'world2' for test in context server=test.")

                    .givenCommand("group test meta addprefix 10 \"&ehello world\"")
                    .thenExpect("[LP] test had prefix 'hello world' set at a priority of 10 in context global.")

                    .givenCommand("group test meta addsuffix 100 \"&ehi\"")
                    .thenExpect("[LP] test had suffix 'hi' set at a priority of 100 in context global.")

                    .givenCommand("group test meta addsuffix 1 \"&6no\"")
                    .thenExpect("[LP] test had suffix 'no' set at a priority of 1 in context global.")

                    .givenCommand("group test meta settemp abc xyz 1d server=hello")
                    .thenExpect("[LP] Set meta key 'abc' to 'xyz' for test for a duration of 1 day in context server=hello.")

                    .givenCommand("group test meta addtempprefix 1000 abc 1d server=hello")
                    .thenExpect("[LP] test had prefix 'abc' set at a priority of 1000 for a duration of 1 day in context server=hello.")

                    .givenCommand("group test meta addtempsuffix 1000 xyz 3d server=hello")
                    .thenExpect("[LP] test had suffix 'xyz' set at a priority of 1000 for a duration of 3 days in context server=hello.")

                    .givenCommand("group test meta unsettemp abc server=hello")
                    .thenExpect("[LP] Unset temporary meta key 'abc' for test in context server=hello.")

                    .givenCommand("group test meta removetempprefix 1000 abc server=hello")
                    .thenExpect("[LP] test had temporary prefix 'abc' at priority 1000 removed in context server=hello.")

                    .givenCommand("group test meta removetempsuffix 1000 xyz server=hello")
                    .thenExpect("[LP] test had temporary suffix 'xyz' at priority 1000 removed in context server=hello.")

                    .givenCommand("group test meta info")
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

                    .givenCommand("group test info")
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

                    .givenCommand("group test meta unset hello")
                    .thenExpect("[LP] Unset meta key 'hello' for test in context global.")

                    .givenCommand("group test meta unset hello server=test")
                    .thenExpect("[LP] Unset meta key 'hello' for test in context server=test.")

                    .givenCommand("group test meta removeprefix 10")
                    .thenExpect("[LP] test had all prefixes at priority 10 removed in context global.")

                    .givenCommand("group test meta removesuffix 100")
                    .thenExpect("[LP] test had all suffixes at priority 100 removed in context global.")

                    .givenCommand("group test meta removesuffix 1")
                    .thenExpect("[LP] test had all suffixes at priority 1 removed in context global.")

                    .givenCommand("group test meta info")
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
        TestPluginProvider.use(tempDir, (app, bootstrap, plugin) -> {
            CommandExecutor executor = app.getCommandExecutor();

            plugin.getStorage().savePlayerData(UUID.fromString("c1d60c50-70b5-4722-8057-87767557e50d"), "Luck").join();
            plugin.getStorage().savePlayerData(UUID.fromString("069a79f4-44e9-4726-a5be-fca90e38aaf5"), "Notch").join();

            new CommandTester(executor)
                    .givenCommand("user Luck info")
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

                    .givenCommand("user c1d60c50-70b5-4722-8057-87767557e50d info")
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

                    .givenCommand("creategroup admin")
                    .givenCommand("user Luck parent set admin")
                    .clearMessageBuffer()

                    .givenCommand("user Luck clone Notch")
                    .thenExpect("[LP] luck was successfully cloned onto notch.")

                    .givenCommand("user Notch parent info")
                    .thenExpect("""
                            [LP] notch's Parents:  (page 1 of 1 - 1 entries)
                            > admin
                            """
                    );
        });
    }

    @Test
    public void testUserPermissionCommands(@TempDir Path tempDir) {
        TestPluginProvider.use(tempDir, (app, bootstrap, plugin) -> {
            CommandExecutor executor = app.getCommandExecutor();

            plugin.getStorage().savePlayerData(UUID.fromString("c1d60c50-70b5-4722-8057-87767557e50d"), "Luck").join();

            new CommandTester(executor)
                    .givenCommand("user Luck permission set test.node true")
                    .thenExpect("[LP] Set test.node to true for luck in context global.")

                    .givenCommand("user Luck permission set test.node.other false server=test")
                    .thenExpect("[LP] Set test.node.other to false for luck in context server=test.")

                    .givenCommand("user Luck permission set test.node.other false server=test world=test2")
                    .thenExpect("[LP] Set test.node.other to false for luck in context server=test, world=test2.")

                    .givenCommand("user Luck permission settemp abc true 1h")
                    .thenExpect("[LP] Set abc to true for luck for a duration of 1 hour in context global.")

                    .givenCommand("user Luck permission settemp abc true 2h replace")
                    .thenExpect("[LP] Set abc to true for luck for a duration of 2 hours in context global.")

                    .givenCommand("user Luck permission unsettemp abc")
                    .thenExpect("[LP] Unset temporary permission abc for luck in context global.")

                    .givenCommand("user Luck permission info")
                    .thenExpect("""
                            [LP] luck's Permissions:  (page 1 of 1 - 3 entries)
                            > test.node.other (server=test) (world=test2)
                            > test.node.other (server=test)
                            > test.node
                            """
                    )

                    .givenCommand("user Luck permission unset test.node")
                    .thenExpect("[LP] Unset test.node for luck in context global.")

                    .givenCommand("user Luck permission unset test.node.other")
                    .thenExpect("[LP] luck does not have test.node.other set in context global.")

                    .givenCommand("user Luck permission unset test.node.other server=test")
                    .thenExpect("[LP] Unset test.node.other for luck in context server=test.")

                    .givenCommand("user Luck permission check test.node.other")
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

                    .givenCommand("user Luck permission clear server=test world=test2")
                    .thenExpect("[LP] luck's permissions were cleared in context server=test, world=test2. (1 node was removed.)")

                    .givenCommand("user Luck permission info")
                    .thenExpect("[LP] luck does not have any permissions set.");
        });
    }

    @Test
    public void testUserParentCommands(@TempDir Path tempDir) {
        TestPluginProvider.use(tempDir, (app, bootstrap, plugin) -> {
            CommandExecutor executor = app.getCommandExecutor();

            plugin.getStorage().savePlayerData(UUID.fromString("c1d60c50-70b5-4722-8057-87767557e50d"), "Luck").join();

            new CommandTester(executor)
                    .givenCommand("creategroup test2")
                    .givenCommand("creategroup test3")
                    .clearMessageBuffer()

                    .givenCommand("user Luck parent add default")
                    .thenExpect("[LP] luck already inherits from default in context global.")

                    .givenCommand("user Luck parent add test2 server=test")
                    .thenExpect("[LP] luck now inherits permissions from test2 in context server=test.")

                    .givenCommand("user Luck parent add test3 server=test")
                    .thenExpect("[LP] luck now inherits permissions from test3 in context server=test.")

                    .givenCommand("user Luck parent addtemp test2 1d server=hello")
                    .thenExpect("[LP] luck now inherits permissions from test2 for a duration of 1 day in context server=hello.")

                    .givenCommand("user Luck parent removetemp test2 server=hello")
                    .thenExpect("[LP] luck no longer temporarily inherits permissions from test2 in context server=hello.")

                    .givenCommand("user Luck parent info")
                    .thenExpect("""
                            [LP] luck's Parents:  (page 1 of 1 - 3 entries)
                            > test2 (server=test)
                            > test3 (server=test)
                            > default
                            """
                    )

                    .givenCommand("user Luck parent set test2 server=test")
                    .thenExpect("[LP] luck had their existing parent groups cleared, and now only inherits test2 in context server=test.")

                    .givenCommand("user Luck parent remove test2 server=test")
                    .thenExpect("[LP] luck no longer inherits permissions from test2 in context server=test.")

                    .givenCommand("user Luck parent clear")
                    .thenExpect("[LP] luck's parents were cleared in context global. (0 nodes were removed.)")

                    .givenCommand("user Luck parent info")
                    .thenExpect("""
                            [LP] luck's Parents:  (page 1 of 1 - 1 entries)
                            > default
                            """
                    );
        });
    }

    @Test
    public void testUserMetaCommands(@TempDir Path tempDir) {
        TestPluginProvider.use(tempDir, (app, bootstrap, plugin) -> {
            CommandExecutor executor = app.getCommandExecutor();

            plugin.getStorage().savePlayerData(UUID.fromString("c1d60c50-70b5-4722-8057-87767557e50d"), "Luck").join();

            new CommandTester(executor)
                    .givenCommand("user Luck meta info")
                    .thenExpect("""
                            [LP] luck has no prefixes.
                            [LP] luck has no suffixes.
                            [LP] luck has no meta.
                            """
                    )

                    .givenCommand("user Luck meta set hello world")
                    .thenExpect("[LP] Set meta key 'hello' to 'world' for luck in context global.")

                    .givenCommand("user Luck meta set hello world2 server=test")
                    .thenExpect("[LP] Set meta key 'hello' to 'world2' for luck in context server=test.")

                    .givenCommand("user Luck meta addprefix 10 \"&ehello world\"")
                    .thenExpect("[LP] luck had prefix 'hello world' set at a priority of 10 in context global.")

                    .givenCommand("user Luck meta addsuffix 100 \"&ehi\"")
                    .thenExpect("[LP] luck had suffix 'hi' set at a priority of 100 in context global.")

                    .givenCommand("user Luck meta addsuffix 1 \"&6no\"")
                    .thenExpect("[LP] luck had suffix 'no' set at a priority of 1 in context global.")

                    .givenCommand("user Luck meta settemp abc xyz 1d server=hello")
                    .thenExpect("[LP] Set meta key 'abc' to 'xyz' for luck for a duration of 1 day in context server=hello.")

                    .givenCommand("user Luck meta addtempprefix 1000 abc 1d server=hello")
                    .thenExpect("[LP] luck had prefix 'abc' set at a priority of 1000 for a duration of 1 day in context server=hello.")

                    .givenCommand("user Luck meta addtempsuffix 1000 xyz 3d server=hello")
                    .thenExpect("[LP] luck had suffix 'xyz' set at a priority of 1000 for a duration of 3 days in context server=hello.")

                    .givenCommand("user Luck meta unsettemp abc server=hello")
                    .thenExpect("[LP] Unset temporary meta key 'abc' for luck in context server=hello.")

                    .givenCommand("user Luck meta removetempprefix 1000 abc server=hello")
                    .thenExpect("[LP] luck had temporary prefix 'abc' at priority 1000 removed in context server=hello.")

                    .givenCommand("user Luck meta removetempsuffix 1000 xyz server=hello")
                    .thenExpect("[LP] luck had temporary suffix 'xyz' at priority 1000 removed in context server=hello.")

                    .givenCommand("user Luck meta info")
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

                    .givenCommand("user Luck info")
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

                    .givenCommand("user Luck meta unset hello")
                    .thenExpect("[LP] Unset meta key 'hello' for luck in context global.")

                    .givenCommand("user Luck meta unset hello server=test")
                    .thenExpect("[LP] Unset meta key 'hello' for luck in context server=test.")

                    .givenCommand("user Luck meta removeprefix 10")
                    .thenExpect("[LP] luck had all prefixes at priority 10 removed in context global.")

                    .givenCommand("user Luck meta removesuffix 100")
                    .thenExpect("[LP] luck had all suffixes at priority 100 removed in context global.")

                    .givenCommand("user Luck meta removesuffix 1")
                    .thenExpect("[LP] luck had all suffixes at priority 1 removed in context global.")

                    .givenCommand("user Luck meta info")
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
        TestPluginProvider.use(tempDir, (app, bootstrap, plugin) -> {
            CommandExecutor executor = app.getCommandExecutor();

            new CommandTester(executor)
                    .givenCommand("createtrack test1")
                    .thenExpect("[LP] test1 was successfully created.")

                    .givenCommand("createtrack test2")
                    .thenExpect("[LP] test2 was successfully created.")

                    .givenCommand("listtracks")
                    .thenExpect("[LP] Tracks: test1, test2")

                    .givenCommand("deletetrack test2")
                    .thenExpect("[LP] test2 was successfully deleted.")

                    .givenCommand("creategroup aaa")
                    .givenCommand("creategroup bbb")
                    .givenCommand("creategroup ccc")
                    .clearMessageBuffer()

                    .givenCommand("track test1 append bbb")
                    .thenExpect("[LP] Group bbb was appended to track test1.")

                    .givenCommand("track test1 insert aaa 1")
                    .thenExpect("""
                            [LP] Group aaa was inserted into track test1 at position 1.
                            [LP] aaa ---> bbb
                            """
                    )

                    .givenCommand("track test1 insert ccc 3")
                    .thenExpect("""
                            [LP] Group ccc was inserted into track test1 at position 3.
                            [LP] aaa ---> bbb ---> ccc
                            """
                    )

                    .givenCommand("track test1 info")
                    .thenExpect("""
                            [LP] > Showing Track: test1
                            [LP] - Path: aaa ---> bbb ---> ccc
                            """
                    )

                    .givenCommand("track test1 clone testclone")
                    .thenExpect("[LP] test1 was successfully cloned onto testclone.")

                    .givenCommand("track testclone info")
                    .thenExpect("""
                            [LP] > Showing Track: testclone
                            [LP] - Path: aaa ---> bbb ---> ccc
                            """
                    )

                    .givenCommand("track test1 rename test2")
                    .thenExpect("[LP] test1 was successfully renamed to test2.")

                    .givenCommand("listtracks")
                    .thenExpect("[LP] Tracks: test2, testclone")

                    .givenCommand("track test2 info")
                    .thenExpect("""
                            [LP] > Showing Track: test2
                            [LP] - Path: aaa ---> bbb ---> ccc
                            """
                    )

                    .givenCommand("group aaa showtracks")
                    .thenExpect("""
                            [LP] aaa's Tracks:
                            > test2:
                            (aaa ---> bbb ---> ccc)
                            > testclone:
                            (aaa ---> bbb ---> ccc)
                            """
                    )

                    .givenCommand("track test2 remove bbb")
                    .thenExpect("""
                            [LP] Group bbb was removed from track test2.
                            [LP] aaa ---> ccc
                            """
                    )

                    .givenCommand("track test2 clear")
                    .thenExpect("[LP] test2's groups track was cleared.");
        });
    }

    @Test
    public void testUserTrackCommands(@TempDir Path tempDir) {
        TestPluginProvider.use(tempDir, (app, bootstrap, plugin) -> {
            CommandExecutor executor = app.getCommandExecutor();

            plugin.getStorage().savePlayerData(UUID.fromString("c1d60c50-70b5-4722-8057-87767557e50d"), "Luck").join();

            new CommandTester(executor)
                    .givenCommand("createtrack staff")
                    .givenCommand("createtrack premium")

                    .givenCommand("creategroup mod")
                    .givenCommand("creategroup admin")

                    .givenCommand("creategroup vip")
                    .givenCommand("creategroup vip+")
                    .givenCommand("creategroup mvp")
                    .givenCommand("creategroup mvp+")

                    .givenCommand("track staff append mod")
                    .givenCommand("track staff append admin")
                    .givenCommand("track premium append vip")
                    .givenCommand("track premium append vip+")
                    .givenCommand("track premium append mvp")
                    .givenCommand("track premium append mvp+")

                    .clearMessageBuffer()

                    .givenCommand("user Luck promote staff")
                    .thenExpect("[LP] luck isn't in any groups on staff, so they were added to the first group, mod in context global.")

                    .givenCommand("user Luck promote staff")
                    .thenExpect("""
                            [LP] Promoting luck along track staff from mod to admin in context global.
                            [LP] mod ---> admin
                            """
                    )

                    .givenCommand("user Luck promote staff")
                    .thenExpect("[LP] The end of track staff was reached, unable to promote luck.")

                    .givenCommand("user Luck demote staff")
                    .thenExpect("""
                            [LP] Demoting luck along track staff from admin to mod in context global.
                            [LP] mod <--- admin
                            """
                    )

                    .givenCommand("user Luck demote staff")
                    .thenExpect("[LP] The end of track staff was reached, so luck was removed from mod.")

                    .givenCommand("user Luck demote staff")
                    .thenExpect("[LP] luck isn't already in any groups on staff.")

                    .givenCommand("user Luck promote premium server=test1")
                    .thenExpect("[LP] luck isn't in any groups on premium, so they were added to the first group, vip in context server=test1.")

                    .givenCommand("user Luck promote premium server=test2")
                    .thenExpect("[LP] luck isn't in any groups on premium, so they were added to the first group, vip in context server=test2.")

                    .givenCommand("user Luck promote premium server=test1")
                    .thenExpect("""
                            [LP] Promoting luck along track premium from vip to vip+ in context server=test1.
                            [LP] vip ---> vip+ ---> mvp ---> mvp+
                            """
                    )

                    .givenCommand("user Luck promote premium server=test2")
                    .thenExpect("""
                            [LP] Promoting luck along track premium from vip to vip+ in context server=test2.
                            [LP] vip ---> vip+ ---> mvp ---> mvp+
                            """
                    )

                    .givenCommand("user Luck parent info")
                    .thenExpect("""
                            [LP] luck's Parents:  (page 1 of 1 - 3 entries)
                            > vip+ (server=test2)
                            > vip+ (server=test1)
                            > default
                            """
                    )

                    .givenCommand("user Luck showtracks")
                    .thenExpect("""
                            [LP] luck's Tracks:
                            > premium: (server=test2)
                            (vip ---> vip+ ---> mvp ---> mvp+)
                            > premium: (server=test1)
                            (vip ---> vip+ ---> mvp ---> mvp+)
                            """
                    )

                    .givenCommand("user Luck demote premium server=test1")
                    .thenExpect("""
                            [LP] Demoting luck along track premium from vip+ to vip in context server=test1.
                            [LP] vip <--- vip+ <--- mvp <--- mvp+
                            """
                    )

                    .givenCommand("user Luck demote premium server=test2")
                    .thenExpect("""
                            [LP] Demoting luck along track premium from vip+ to vip in context server=test2.
                            [LP] vip <--- vip+ <--- mvp <--- mvp+
                            """
                    )

                    .givenCommand("user Luck demote premium server=test1")
                    .thenExpect("[LP] The end of track premium was reached, so luck was removed from vip.")

                    .givenCommand("user Luck demote premium server=test2")
                    .thenExpect("[LP] The end of track premium was reached, so luck was removed from vip.")

                    .givenCommand("user Luck parent info")
                    .thenExpect("""
                            [LP] luck's Parents:  (page 1 of 1 - 1 entries)
                            > default
                            """
                    );
        });
    }

}
