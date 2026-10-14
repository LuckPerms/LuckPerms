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

package me.lucko.luckperms.common.inheritance;

import me.lucko.luckperms.common.model.Group;
import me.lucko.luckperms.common.model.HolderType;
import me.lucko.luckperms.common.model.PermissionHolder;
import me.lucko.luckperms.common.model.PrimaryGroupHolder;
import me.lucko.luckperms.common.model.User;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Comparator;
import java.util.List;
import java.util.OptionalInt;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

public class InheritanceComparatorTest {

    @Test
    public void testUserAlwaysFirst() {
        User user = makeUser("root", "admin");
        List<PermissionHolder> holders = List.of(
                makeGroup("admin", 0),
                makeGroup("moderator", 0),
                user
        );

        Comparator<? super PermissionHolder> comparator = InheritanceComparator.getFor(user);

        List<String> results = holders.stream().sorted(comparator).map(PermissionHolder::getPlainDisplayName).collect(Collectors.toList());
        assertEquals(List.of("root", "admin", "moderator"), results);
    }

    @Test
    public void testPrimaryGroupOrdering() {
        User user = makeUser("root", "admin");
        List<PermissionHolder> holders = List.of(
                makeGroup("vip", 0),
                makeGroup("admin", 0),
                makeGroup("moderator", 0)
        );

        Comparator<? super PermissionHolder> comparator = InheritanceComparator.getFor(user);

        List<String> results = holders.stream().sorted(comparator).map(PermissionHolder::getPlainDisplayName).collect(Collectors.toList());
        assertEquals(List.of("admin", "vip", "moderator"), results);
    }

    @Test
    public void testWeightOrdering() {
        User user = makeUser("root", "vip");
        List<PermissionHolder> holders = List.of(
                makeGroup("vip", 3),
                makeGroup("admin", 10),
                makeGroup("moderator", 5)
        );

        Comparator<? super PermissionHolder> comparator = InheritanceComparator.getFor(user);

        List<String> results = holders.stream().sorted(comparator).map(PermissionHolder::getPlainDisplayName).collect(Collectors.toList());
        assertEquals(List.of("admin", "moderator", "vip"), results);
    }

    private static Group makeGroup(String name, int weight) {
        Group group = Mockito.mock(Group.class);
        when(group.getType()).thenReturn(HolderType.GROUP);
        when(group.getName()).thenReturn(name);
        when(group.getPlainDisplayName()).thenReturn(name);
        when(group.getWeight()).thenReturn(OptionalInt.of(weight));
        return group;
    }

    private static User makeUser(String name, String primaryGroup) {
        User user = Mockito.mock(User.class);
        when(user.getType()).thenReturn(HolderType.USER);
        when(user.getPlainDisplayName()).thenReturn(name);

        PrimaryGroupHolder primaryGroupHolder = new PrimaryGroupHolder.Stored(user);
        if (primaryGroup != null) {
            primaryGroupHolder.setStoredValue(primaryGroup);
        }

        when(user.getPrimaryGroup()).thenReturn(primaryGroupHolder);
        return user;
    }
}
