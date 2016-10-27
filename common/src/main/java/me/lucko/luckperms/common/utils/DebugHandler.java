/*
 * Copyright (c) 2016 Lucko (Luck) <luck@lucko.me>
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

package me.lucko.luckperms.common.utils;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import me.lucko.luckperms.api.Tristate;
import me.lucko.luckperms.common.commands.Sender;
import me.lucko.luckperms.common.constants.Message;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class DebugHandler {
    private final Map<Reciever, String> listeners = new ConcurrentHashMap<>();

    public void printOutput(String checked, String node, Tristate value) {
        for (Map.Entry<Reciever, String> e : listeners.entrySet()) {
            if (node.toLowerCase().startsWith(e.getValue().toLowerCase())) {
                Message.LOG.send(e.getKey().getSender(), "&7Checking &a" + checked + "&7 for: &a" + node + " &f(&7" + value.toString() + "&f)");
            }
        }
    }

    public void register(Sender sender, String s) {
        listeners.put(new Reciever(sender.getUuid(), sender), s);
    }

    public void unregister(UUID uuid) {
        listeners.remove(new Reciever(uuid, null));
    }

    @Getter
    @EqualsAndHashCode(of = "uuid")
    @AllArgsConstructor
    private static final class Reciever {
        private final UUID uuid;
        private final Sender sender;
    }
}
