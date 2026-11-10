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

package me.lucko.luckperms.bukkit.util;

import com.google.gson.JsonElement;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import org.bukkit.command.CommandSender;

import java.lang.reflect.Method;

public final class PaperAdventureBridge {

    public static final PaperAdventureBridge INSTANCE;
    static {
        PaperAdventureBridge bridge = null;
        try {
            bridge = new PaperAdventureBridge();
        } catch (Exception e) {
            // ignore
        }
        INSTANCE = bridge;
    }

    private final Method serializerDeserialize;
    private final Method sendMessage;
    private final Object serializerInstance;

    private PaperAdventureBridge() throws Exception {
        String adventurePkg = "net.kyo".concat("ri.adventure.");
        Class<?> audienceClass = Class.forName(adventurePkg + "audience.Audience");
        Class<?> componentClass = Class.forName(adventurePkg + "text.Component");
        Class<?> serializerClass = Class.forName(adventurePkg + "text.serializer.gson.GsonComponentSerializer");

        if (!audienceClass.isAssignableFrom(CommandSender.class)) {
            throw new IllegalStateException("CommandSender does not implement Audience");
        }

        this.serializerDeserialize = serializerClass.getMethod("deserializeFromTree", JsonElement.class);
        this.sendMessage = audienceClass.getMethod("sendMessage", componentClass);
        this.serializerInstance = serializerClass.getMethod("gson").invoke(null);
    }

    public Object toPlatformComponent(Component component) {
        JsonElement json = GsonComponentSerializer.gson().serializeToTree(component);
        try {
            return this.serializerDeserialize.invoke(this.serializerInstance, json);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    public void sendMessage(CommandSender audience, Component message) {
        try {
            this.sendMessage.invoke(audience, toPlatformComponent(message));
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

}
