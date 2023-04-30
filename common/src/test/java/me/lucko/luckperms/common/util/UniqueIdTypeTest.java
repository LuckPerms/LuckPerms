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

package me.lucko.luckperms.common.util;

import me.lucko.luckperms.common.event.EventDispatcher;
import me.lucko.luckperms.common.plugin.LuckPermsPlugin;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.AdditionalAnswers.returnsSecondArg;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class UniqueIdTypeTest {

    @Mock private LuckPermsPlugin plugin;
    @Mock private EventDispatcher dispatcher;

    @BeforeEach
    public void setupMocks() {
        when(this.plugin.getEventDispatcher()).thenReturn(this.dispatcher);
        when(this.dispatcher.dispatchUniqueIdDetermineType(any(), anyString())).then(returnsSecondArg());
    }

    @ParameterizedTest
    @CsvSource({
            "797a99ba-c040-4f04-8cfc-6b01a4890d2f, authenticated",
            "5d41402a-bc4b-3a76-b971-9d911017c592, unauthenticated",
            "cfa4fcb1-a786-23fc-b956-33914c7bb373, npc",
            "00000000-0000-0000-0000-000000000000, unknown"
    })
    public void testParse(UUID uuid, String expectedType) {
        UniqueIdType uniqueIdType = UniqueIdType.determineType(uuid, this.plugin);
        assertEquals(expectedType, uniqueIdType.getType());
    }

}
