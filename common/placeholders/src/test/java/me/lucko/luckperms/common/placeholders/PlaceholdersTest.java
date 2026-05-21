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

package me.lucko.luckperms.common.placeholders;

import net.luckperms.api.LuckPerms;
import net.luckperms.api.cacheddata.CachedDataManager;
import net.luckperms.api.cacheddata.CachedMetaData;
import net.luckperms.api.cacheddata.CachedPermissionData;
import net.luckperms.api.model.user.User;
import net.luckperms.api.query.QueryOptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.time.temporal.ChronoUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class PlaceholdersTest {

    @Mock private LuckPerms api;
    @Mock private User user;
    @Mock private QueryOptions queryOptions;

    @Mock private CachedDataManager cachedDataManager;
    @Mock private CachedPermissionData cachedPermissionData;
    @Mock private CachedMetaData cachedMetaData;

    private PlaceholderContext ctx;

    @BeforeEach
    public void setupMocks() {
        lenient().when(this.user.getCachedData()).thenReturn(this.cachedDataManager);
        lenient().when(this.cachedDataManager.getPermissionData(this.queryOptions)).thenReturn(this.cachedPermissionData);
        lenient().when(this.cachedDataManager.getMetaData(this.queryOptions)).thenReturn(this.cachedMetaData);

        this.ctx = new PlaceholderContext(this.api, this.user, this.queryOptions);
    }

    // test some of the basic / simple / most used placeholders - the others are too difficult to test
    // using mocks only.

    @Test
    public void testPrefix() {
        when(this.cachedMetaData.getPrefix()).thenReturn("test prefix");
        assertEquals("test prefix", Placeholders.PREFIX.resolve(this.ctx));
    }

    @Test
    public void testSuffix() {
        when(this.cachedMetaData.getSuffix()).thenReturn("test suffix");
        assertEquals("test suffix", Placeholders.SUFFIX.resolve(this.ctx));
    }

    @Test
    public void testMeta() {
        when(this.cachedMetaData.getMetaValue("test_key")).thenReturn("hello");
        assertEquals("hello", Placeholders.META.resolve(this.ctx.withArgument("test_key")));
    }

    @Test
    public void testFormatDuration() {
        Duration duration = ChronoUnit.YEARS.getDuration().multipliedBy(5)
                .plus(ChronoUnit.MONTHS.getDuration().multipliedBy(4))
                .plus(ChronoUnit.WEEKS.getDuration().multipliedBy(3))
                .plusDays(2)
                .plusHours(1)
                .plusMinutes(6)
                .plusSeconds(7);

        assertEquals("5y 4mo 3w 2d 1h 6m 7s", Placeholders.formatDuration(duration));
        assertEquals("1m 10s", Placeholders.formatDuration(Duration.ofMinutes(1).plusSeconds(10)));
        assertEquals("0s", Placeholders.formatDuration(Duration.ZERO));
    }

}
