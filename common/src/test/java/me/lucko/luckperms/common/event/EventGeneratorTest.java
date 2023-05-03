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

package me.lucko.luckperms.common.event;

import me.lucko.luckperms.common.event.gen.GeneratedEventClass;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.event.LuckPermsEvent;
import net.luckperms.api.event.player.PlayerDataSaveEvent;
import net.luckperms.api.event.sync.PreSyncEvent;
import net.luckperms.api.model.PlayerSaveResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
public class EventGeneratorTest {

    @Mock public LuckPerms luckPermsApi;

    @Test
    public void testGenerateAll() {
        GeneratedEventClass.preGenerate();
    }

    @Test
    public void testSimple() throws Throwable {
        UUID randomUniqueId = UUID.randomUUID();
        String randomUsername = "random";
        PlayerSaveResult mockResult = mock(PlayerSaveResult.class);

        GeneratedEventClass eventClass = GeneratedEventClass.generate(PlayerDataSaveEvent.class);
        LuckPermsEvent rawEvent = eventClass.newInstance(this.luckPermsApi, randomUniqueId, randomUsername, mockResult);

        assertTrue(rawEvent instanceof PlayerDataSaveEvent);
        PlayerDataSaveEvent event = (PlayerDataSaveEvent) rawEvent;

        assertEquals(PlayerDataSaveEvent.class, event.getEventType());
        assertSame(randomUniqueId, event.getUniqueId());
        assertSame(randomUsername, event.getUsername());
        assertSame(mockResult, event.getResult());
        assertSame(this.luckPermsApi, event.getLuckPerms());
    }

    @Test
    public void testDefaultMethods() throws Throwable {
        AtomicBoolean state = new AtomicBoolean(false);
        GeneratedEventClass eventClass = GeneratedEventClass.generate(PreSyncEvent.class);
        PreSyncEvent event = (PreSyncEvent) eventClass.newInstance(this.luckPermsApi, state);

        assertFalse(event.isCancelled());
        assertTrue(event.isNotCancelled());
        assertEquals("PreSyncEvent{cancellationState=false}", event.toString());

        assertFalse(event.setCancelled(true));

        assertTrue(event.isCancelled());
        assertFalse(event.isNotCancelled());
        assertEquals("PreSyncEvent{cancellationState=true}", event.toString());
    }

}
