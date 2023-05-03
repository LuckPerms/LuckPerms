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

package me.lucko.luckperms.common.api;

import com.google.common.base.Preconditions;
import me.lucko.luckperms.common.config.ConfigKeys;
import me.lucko.luckperms.common.plugin.LuckPermsPlugin;
import me.lucko.luckperms.common.storage.misc.DataConstraints;

import java.util.Locale;
import java.util.function.Predicate;

public final class ApiUtils {
    private ApiUtils() {}

    public static String checkUsername(String s, LuckPermsPlugin plugin) {
        if (s == null) {
            return null;
        }

        Predicate<String> test = plugin.getConfiguration().get(ConfigKeys.ALLOW_INVALID_USERNAMES) ?
                DataConstraints.PLAYER_USERNAME_TEST_LENIENT :
                DataConstraints.PLAYER_USERNAME_TEST;

        Preconditions.checkArgument(test.test(s), "Invalid username entry: " + s);
        return s;
    }

    public static String checkName(String s) {
        if (s == null) {
            return null;
        }

        Preconditions.checkArgument(DataConstraints.GROUP_NAME_TEST.test(s), "Invalid name entry: " + s);
        return s.toLowerCase(Locale.ROOT);
    }

}
