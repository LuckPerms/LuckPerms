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
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.ansi.ANSIComponentSerializer;
import net.kyori.ansi.ColorLevel;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.function.Consumer;

/**
 * Dummy/singleton player class used by the standalone plugin.
 *
 * <p>In various places (ContextManager, SenderFactory, ..) the platform "player" type is used
 * as a generic parameter. This class acts as this type for the standalone plugin.</p>
 */
public class SingletonPlayer {

    /** Empty UUID used by the singleton player. */
    private static final UUID UUID = new UUID(0, 0);

    /** A message sink that prints the component to stdout */
    private static final Consumer<Component> PRINT_TO_STDOUT = component -> LuckPermsApplication.LOGGER.info(ANSIComponentSerializer.ansi().serialize(component));

    /** Singleton instance */
    public static final SingletonPlayer INSTANCE = new SingletonPlayer();

    /** A set of message sinks that messages are delivered to */
    private final Set<Consumer<Component>> messageSinks;

    private SingletonPlayer() {
        this.messageSinks = new CopyOnWriteArraySet<>();
        this.messageSinks.add(PRINT_TO_STDOUT);
    }

    public String getName() {
        return "StandaloneUser";
    }

    public UUID getUniqueId() {
        return UUID;
    }

    public void sendMessage(Component component) {
        for (Consumer<Component> sink : this.messageSinks) {
            sink.accept(component);
        }
    }

    public void addMessageSink(Consumer<Component> sink) {
        this.messageSinks.add(sink);
    }

    public void removeMessageSink(Consumer<Component> sink) {
        this.messageSinks.remove(sink);
    }

}
