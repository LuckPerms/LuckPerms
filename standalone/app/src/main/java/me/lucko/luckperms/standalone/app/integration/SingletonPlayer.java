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

package me.lucko.luckperms.standalone.app.integration;

import me.lucko.luckperms.standalone.app.LuckPermsApplication;
import me.lucko.luckperms.standalone.app.utils.AnsiUtils;

import net.kyori.adventure.text.Component;

import java.util.UUID;

/**
 * Dummy/singleton player class used by the standalone plugin.
 *
 * In various places (ContextManager, SenderFactory, ..) the platform "player" type is used
 * as a generic parameter. This class acts as this type for the standalone plugin.
 */
public class SingletonPlayer {
    public static final SingletonPlayer INSTANCE = new SingletonPlayer();

    private static final UUID UUID = new UUID(0, 0);

    public String getName() {
        return "StandaloneUser";
    }

    public UUID getUniqueId() {
        return UUID;
    }

    public void printStdout(Component component) {
        LuckPermsApplication.LOGGER.info(AnsiUtils.format(component));
    }

}
