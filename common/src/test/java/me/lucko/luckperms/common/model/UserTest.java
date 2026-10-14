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

package me.lucko.luckperms.common.model;

import me.lucko.luckperms.common.config.ConfigKeys;
import me.lucko.luckperms.common.config.LuckPermsConfiguration;
import me.lucko.luckperms.common.event.EventDispatcher;
import me.lucko.luckperms.common.plugin.LuckPermsPlugin;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
public class UserTest {

    @Mock private LuckPermsPlugin plugin;
    @Mock private LuckPermsConfiguration configuration;

    @BeforeEach
    public void setupMocks() {
        lenient().when(this.plugin.getEventDispatcher()).thenReturn(mock(EventDispatcher.class));
        lenient().when(this.plugin.getConfiguration()).thenReturn(this.configuration);
        lenient().when(this.configuration.get(ConfigKeys.PRIMARY_GROUP_CALCULATION)).thenReturn(u -> null);
    }

    @Test
    public void testGetDisplayName() {
        UUID uniqueId = UUID.randomUUID();
        User user = new User(uniqueId, this.plugin);

        // no username set
        assertEquals(uniqueId.toString(), user.getPlainDisplayName());

        // username set
        user.setUsername("Luck", true);
        assertEquals("Luck", user.getPlainDisplayName());
    }

    @Test
    public void testSetUsername() {
        User user = new User(UUID.randomUUID(), this.plugin);

        // fail, over 16 chars
        boolean res = user.setUsername("123456789123456789", false);
        assertFalse(res);
        assertEquals(Optional.empty(), user.getUsername());

        // succeed - none set already
        res = user.setUsername("luck", true);
        assertTrue(res);
        assertEquals(Optional.of("luck"), user.getUsername());

        // fail - weak=true and username set already
        res = user.setUsername("example", true);
        assertFalse(res);
        assertEquals(Optional.of("luck"), user.getUsername());

        // succeed - weak=true and only change in case
        res = user.setUsername("Luck", true);
        assertFalse(res);
        assertEquals(Optional.of("Luck"), user.getUsername());

        // succeed - change in case
        res = user.setUsername("LUCK", false);
        assertFalse(res);
        assertEquals(Optional.of("LUCK"), user.getUsername());

        // succeed - change in value
        res = user.setUsername("example", false);
        assertTrue(res);
        assertEquals(Optional.of("example"), user.getUsername());

        // succeed - set to null
        res = user.setUsername("", false);
        assertTrue(res);
        assertEquals(Optional.empty(), user.getUsername());
    }
}
