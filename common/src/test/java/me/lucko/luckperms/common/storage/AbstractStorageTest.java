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

package me.lucko.luckperms.common.storage;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import me.lucko.luckperms.common.actionlog.LogPage;
import me.lucko.luckperms.common.actionlog.LoggedAction;
import me.lucko.luckperms.common.actionlog.filter.ActionFilters;
import me.lucko.luckperms.common.config.ConfigKeys;
import me.lucko.luckperms.common.config.LuckPermsConfiguration;
import me.lucko.luckperms.common.event.EventDispatcher;
import me.lucko.luckperms.common.filter.PageParameters;
import me.lucko.luckperms.common.model.Group;
import me.lucko.luckperms.common.model.PrimaryGroupHolder;
import me.lucko.luckperms.common.model.User;
import me.lucko.luckperms.common.model.manager.group.GroupManager;
import me.lucko.luckperms.common.model.manager.group.StandardGroupManager;
import me.lucko.luckperms.common.model.manager.user.StandardUserManager;
import me.lucko.luckperms.common.model.manager.user.UserManager;
import me.lucko.luckperms.common.node.types.Inheritance;
import me.lucko.luckperms.common.node.types.Permission;
import me.lucko.luckperms.common.plugin.LuckPermsPlugin;
import me.lucko.luckperms.common.plugin.bootstrap.LuckPermsBootstrap;
import me.lucko.luckperms.common.plugin.scheduler.SchedulerAdapter;
import me.lucko.luckperms.common.storage.implementation.StorageImplementation;
import net.luckperms.api.actionlog.Action;
import net.luckperms.api.model.PlayerSaveResult;
import net.luckperms.api.model.PlayerSaveResult.Outcome;
import net.luckperms.api.model.data.DataType;
import net.luckperms.api.node.Node;
import net.luckperms.api.node.types.InheritanceNode;
import net.luckperms.api.node.types.PermissionNode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.AdditionalAnswers.answer;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public abstract class AbstractStorageTest {

    @Mock protected LuckPermsPlugin plugin;
    @Mock protected LuckPermsBootstrap bootstrap;
    @Mock protected LuckPermsConfiguration configuration;

    protected StorageImplementation storage;

    @BeforeEach
    public final void setupMocksAndStorage() throws Exception {
        lenient().when(this.plugin.getBootstrap()).thenReturn(this.bootstrap);
        lenient().when(this.plugin.getConfiguration()).thenReturn(this.configuration);
        lenient().when(this.plugin.getEventDispatcher()).thenReturn(mock(EventDispatcher.class));
        lenient().when(this.bootstrap.getScheduler()).thenReturn(mock(SchedulerAdapter.class));
        lenient().when(this.configuration.get(ConfigKeys.PRIMARY_GROUP_CALCULATION)).thenReturn(PrimaryGroupHolder.AllParentsByWeight::new);
        lenient().when(this.configuration.get(ConfigKeys.PRIMARY_GROUP_CALCULATION_METHOD)).thenReturn("parents-by-weight");
        lenient().when(this.bootstrap.getResourceStream(anyString()))
                .then(answer((String path) -> AbstractStorageTest.class.getClassLoader().getResourceAsStream(path)));
        lenient().when(this.plugin.getEventDispatcher()).thenReturn(mock(EventDispatcher.class));

        this.storage = makeStorage(this.plugin);
        this.storage.init();
    }

    protected abstract StorageImplementation makeStorage(LuckPermsPlugin plugin) throws Exception;

    protected void cleanupResources() {
        // do nothing
    }

    @AfterEach
    public final void shutdownStorage() {
        this.storage.shutdown();
        cleanupResources();
    }

    @Test
    public void testActionLog() throws Exception {
        UUID sourceUuid = UUID.randomUUID();
        UUID targetUuid = UUID.randomUUID();

        Instant baseTime = Instant.now();

        Function<Integer, LoggedAction> mockAction = i -> LoggedAction.build()
                .source(i % 2 == 0 ? sourceUuid : UUID.randomUUID())
                .sourceName("Test Source")
                .targetType(Action.Target.Type.USER)
                .target(targetUuid)
                .targetName("Test Target")
                .description("hello " + i)
                .timestamp(baseTime.plusSeconds(i))
                .build();

        for (int i = 0; i < 100; i++) {
            this.storage.logAction(mockAction.apply(-i));
        }
        for (int i = 0; i < 100; i++) {
            this.storage.logAction(mockAction.apply(i));
        }
        for (int i = 100; i < 200; i++) {
            this.storage.logAction(mockAction.apply(-i));
        }

        for (int i = 0; i < 10; i++) {
            this.storage.logAction(LoggedAction.build()
                    .source(UUID.randomUUID())
                    .sourceName("Test Source")
                    .targetType(Action.Target.Type.GROUP)
                    .targetName(i % 2 == 0 ? "test_group" : "dummy")
                    .description("group test " + i)
                    .timestamp(baseTime)
                    .build());
        }

        for (int i = 0; i < 10; i++) {
            this.storage.logAction(LoggedAction.build()
                    .source(UUID.randomUUID())
                    .sourceName("Test Source")
                    .targetType(Action.Target.Type.TRACK)
                    .targetName(i % 2 == 0 ? "test_track" : "dummy")
                    .description("track test " + i)
                    .timestamp(baseTime)
                    .build());
        }

        LogPage page = this.storage.getLogPage(ActionFilters.source(sourceUuid), new PageParameters(5, 1));
        assertEquals(ImmutableList.of(
                mockAction.apply(98),
                mockAction.apply(96),
                mockAction.apply(94),
                mockAction.apply(92),
                mockAction.apply(90)
        ), page.getContent());
        List<Integer> positions = page.getNumberedContent().stream().map(LogPage.Entry::position).collect(Collectors.toList());
        assertEquals(ImmutableList.of(1, 2, 3, 4, 5), positions);
        assertEquals(150, page.getTotalEntries());

        page = this.storage.getLogPage(ActionFilters.source(sourceUuid), new PageParameters(5, 3));
        assertEquals(ImmutableList.of(
                mockAction.apply(78),
                mockAction.apply(76),
                mockAction.apply(74),
                mockAction.apply(72),
                mockAction.apply(70)
        ), page.getContent());
        positions = page.getNumberedContent().stream().map(LogPage.Entry::position).collect(Collectors.toList());
        assertEquals(ImmutableList.of(11, 12, 13, 14, 15), positions);
        assertEquals(150, page.getTotalEntries());

        page = this.storage.getLogPage(ActionFilters.source(sourceUuid), new PageParameters(5, 31));
        assertEquals(150, page.getTotalEntries());
        assertEquals(0, page.getContent().size());

        page = this.storage.getLogPage(ActionFilters.source(sourceUuid), new PageParameters(500, 1));
        assertEquals(150, page.getTotalEntries());
        assertEquals(150, page.getContent().size());

        page = this.storage.getLogPage(ActionFilters.source(sourceUuid), null);
        assertEquals(150, page.getTotalEntries());
        assertEquals(150, page.getContent().size());

        page = this.storage.getLogPage(ActionFilters.all(), null);
        assertEquals(320, page.getTotalEntries());
        assertEquals(320, page.getContent().size());

        page = this.storage.getLogPage(ActionFilters.user(targetUuid), new PageParameters(5, 1));
        assertEquals(300, page.getTotalEntries());

        page = this.storage.getLogPage(ActionFilters.group("test_group"), new PageParameters(10, 1));
        assertEquals(5, page.getContent().size());
        assertEquals(
                ImmutableList.of("group test 8", "group test 6", "group test 4", "group test 2", "group test 0"),
                page.getContent().stream().map(LoggedAction::getDescription).collect(Collectors.toList())
        );

        page = this.storage.getLogPage(ActionFilters.track("test_track"), new PageParameters(10, 1));
        assertEquals(5, page.getContent().size());
        assertEquals(
                ImmutableList.of("track test 8", "track test 6", "track test 4", "track test 2", "track test 0"),
                page.getContent().stream().map(LoggedAction::getDescription).collect(Collectors.toList())
        );

        page = this.storage.getLogPage(ActionFilters.search("hello"), new PageParameters(500, 1));
        assertEquals(300, page.getContent().size());
    }

    @Test
    public void testSavePlayerData() throws Exception {
        UUID uniqueId = UUID.randomUUID();

        // clean insert
        PlayerSaveResult r1 = this.storage.savePlayerData(uniqueId, "Player1");
        assertEquals(ImmutableSet.of(Outcome.CLEAN_INSERT), r1.getOutcomes());
        assertNull(r1.getOtherUniqueIds());
        assertNull(r1.getPreviousUsername());

        // no change expected
        PlayerSaveResult r2 = this.storage.savePlayerData(uniqueId, "Player1");
        assertEquals(ImmutableSet.of(Outcome.NO_CHANGE), r2.getOutcomes());
        assertNull(r2.getOtherUniqueIds());
        assertNull(r2.getPreviousUsername());

        // changed username
        PlayerSaveResult r3 = this.storage.savePlayerData(uniqueId, "Player2");
        assertEquals(ImmutableSet.of(Outcome.USERNAME_UPDATED), r3.getOutcomes());
        assertNull(r3.getOtherUniqueIds());
        assertTrue("Player1".equalsIgnoreCase(r3.getPreviousUsername()));

        // changed uuid
        UUID newUniqueId = UUID.randomUUID();
        PlayerSaveResult r4 = this.storage.savePlayerData(newUniqueId, "Player2");
        assertEquals(ImmutableSet.of(Outcome.CLEAN_INSERT, Outcome.OTHER_UNIQUE_IDS_PRESENT_FOR_USERNAME), r4.getOutcomes());
        assertNotNull(r4.getOtherUniqueIds());
        assertEquals(ImmutableSet.of(uniqueId), r4.getOtherUniqueIds());
        assertNull(r2.getPreviousUsername());
    }

    @Test
    public void testGetPlayerUniqueIdAndName() throws Exception {
        UUID uniqueId = UUID.randomUUID();
        String username = "Player1";

        this.storage.savePlayerData(uniqueId, username);

        assertEquals(uniqueId, this.storage.getPlayerUniqueId("Player1"));
        assertTrue(username.equalsIgnoreCase(this.storage.getPlayerName(uniqueId)));
    }

    @Test
    public void testGetPlayerUniqueIdAndNameNull() throws Exception {
        assertNull(this.storage.getPlayerUniqueId("Player1"));
        assertNull(this.storage.getPlayerName(UUID.randomUUID()));
    }

    @Test
    public void testSaveAndLoadGroup() throws Exception {
        StandardGroupManager groupManager = new StandardGroupManager(this.plugin);

        //noinspection unchecked,rawtypes
        lenient().when(this.plugin.getGroupManager()).thenReturn((GroupManager) groupManager);

        Group group = this.storage.createAndLoadGroup("test");

        group.normalData().add(Permission.builder()
                .permission("test.1")
                .withContext("server", "test")
                .build()
        );
        group.normalData().add(Permission.builder()
                .permission("test.2")
                .withContext("world", "test")
                .build()
        );
        group.normalData().add(Permission.builder()
                .permission("test.3")
                .expiry(1, TimeUnit.HOURS)
                .withContext("server", "test")
                .withContext("world", "test")
                .withContext("hello", "test")
                .build()
        );

        Set<Node> nodes = group.normalData().asSet();
        assertEquals(3, nodes.size());

        this.storage.saveGroup(group);
        groupManager.unload("test");

        Group loaded = this.storage.loadGroup("test").orElse(null);
        assertNotNull(loaded);
        assertNotSame(group, loaded);
        assertEquals(nodes, loaded.normalData().asSet());
    }

    @Test
    public void testSaveAndDeleteUser() throws Exception {
        StandardUserManager userManager = new StandardUserManager(this.plugin);

        //noinspection unchecked,rawtypes
        when(this.plugin.getUserManager()).thenReturn((UserManager) userManager);

        UUID exampleUniqueId = UUID.fromString("069a79f4-44e9-4726-a5be-fca90e38aaf5");
        String exampleUsername = "Notch";
        PermissionNode examplePermission = Permission.builder()
                .permission("test.1")
                .withContext("server", "test")
                .build();
        InheritanceNode defaultGroupNode = Inheritance.builder(GroupManager.DEFAULT_GROUP_NAME).build();

        // create a default user, assert that is doesn't appear in unique users list
        this.storage.savePlayerData(exampleUniqueId, exampleUsername);
        assertFalse(this.storage.getUniqueUsers().contains(exampleUniqueId));

        // give the user a node, assert that it does appear in unique users list
        User user = this.storage.loadUser(exampleUniqueId, exampleUsername);
        user.setNode(DataType.NORMAL, examplePermission, true);
        this.storage.saveUser(user);
        assertTrue(this.storage.getUniqueUsers().contains(exampleUniqueId));

        // clear all nodes (reset to default) and assert that it does not appear in unique users list
        user.clearNodes(DataType.NORMAL, null, true);
        this.storage.saveUser(user);
        assertFalse(this.storage.getUniqueUsers().contains(exampleUniqueId));
        assertEquals(ImmutableSet.of(defaultGroupNode), user.normalData().asSet());

        // give it a node again, assert that it shows as a unique user
        user.setNode(DataType.NORMAL, examplePermission, true);
        this.storage.saveUser(user);
        assertTrue(this.storage.getUniqueUsers().contains(exampleUniqueId));
        assertEquals(ImmutableSet.of(defaultGroupNode, examplePermission), user.normalData().asSet());

        // reload user data from the db and assert that it is unchanged
        user = this.storage.loadUser(exampleUniqueId, exampleUsername);
        assertEquals(ImmutableSet.of(defaultGroupNode, examplePermission), user.normalData().asSet());
    }

}
