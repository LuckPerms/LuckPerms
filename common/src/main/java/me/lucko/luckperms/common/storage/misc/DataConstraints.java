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

package me.lucko.luckperms.common.storage.misc;

import java.util.function.Predicate;
import java.util.regex.Pattern;

public final class DataConstraints {
    private DataConstraints() {}

    public static final int MAX_PERMISSION_LENGTH = 200;

    public static final int MAX_TRACK_NAME_LENGTH = 36;
    public static final int MAX_GROUP_NAME_LENGTH = 36;

    public static final int MAX_PLAYER_USERNAME_LENGTH = 16;
    public static final Pattern PLAYER_USERNAME_INVALID_CHAR_MATCHER = Pattern.compile("[^A-Za-z0-9_]");

    public static final int MAX_SERVER_LENGTH = 36;
    public static final int MAX_WORLD_LENGTH = 36;

    public static final Predicate<String> PERMISSION_TEST = s -> !s.isEmpty() && s.length() <= MAX_PERMISSION_LENGTH;

    public static final Predicate<String> PLAYER_USERNAME_TEST = s -> !s.isEmpty() && s.length() <= MAX_PLAYER_USERNAME_LENGTH && !PLAYER_USERNAME_INVALID_CHAR_MATCHER.matcher(s).find();

    public static final Predicate<String> PLAYER_USERNAME_TEST_LENIENT = s -> !s.isEmpty() && s.length() <= MAX_PLAYER_USERNAME_LENGTH;

    public static final Predicate<String> GROUP_NAME_TEST = s -> !s.isEmpty() && s.length() <= MAX_GROUP_NAME_LENGTH && !s.contains(" ");

    public static final Predicate<String> GROUP_NAME_TEST_ALLOW_SPACE = s -> !s.isEmpty() && s.length() <= MAX_GROUP_NAME_LENGTH;

    public static final Predicate<String> TRACK_NAME_TEST = s -> !s.isEmpty() && s.length() <= MAX_TRACK_NAME_LENGTH && !s.contains(" ");

    public static final Predicate<String> TRACK_NAME_TEST_ALLOW_SPACE = s -> !s.isEmpty() && s.length() <= MAX_TRACK_NAME_LENGTH;

    public static final Predicate<String> SERVER_NAME_TEST = s -> !s.isEmpty() && s.length() <= MAX_SERVER_LENGTH && !s.contains(" ");

    public static final Predicate<String> WORLD_NAME_TEST = s -> !s.isEmpty() && s.length() <= MAX_WORLD_LENGTH;

}
