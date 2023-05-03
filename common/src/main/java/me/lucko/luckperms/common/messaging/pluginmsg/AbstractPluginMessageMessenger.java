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

package me.lucko.luckperms.common.messaging.pluginmsg;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import net.luckperms.api.messenger.IncomingMessageConsumer;
import net.luckperms.api.messenger.Messenger;
import net.luckperms.api.messenger.message.OutgoingMessage;
import org.checkerframework.checker.nullness.qual.NonNull;

/**
 * Abstract implementation of {@link Messenger} using Minecraft's
 * 'plugin messaging channels' packet.
 *
 * <p>The {@link OutgoingMessage#asEncodedString() encoded string} format
 * is used to transmit messages. {@link java.io.DataOutput#writeUTF(String)} is
 * used to encode the string into raw bytes.</p>
 */
public abstract class AbstractPluginMessageMessenger implements Messenger {

    /**
     * The identifier of the channel used by LuckPerms for all messages.
     */
    public static final String CHANNEL = "luckperms:update";

    /**
     * The {@link IncomingMessageConsumer} used by the messenger.
     */
    private final IncomingMessageConsumer consumer;

    protected AbstractPluginMessageMessenger(IncomingMessageConsumer consumer) {
        this.consumer = consumer;
    }

    @Override
    public final void sendOutgoingMessage(@NonNull OutgoingMessage outgoingMessage) {
        ByteArrayDataOutput dataOutput = ByteStreams.newDataOutput();
        dataOutput.writeUTF(outgoingMessage.asEncodedString());

        byte[] buf = dataOutput.toByteArray();

        sendOutgoingMessage(buf);
    }

    protected abstract void sendOutgoingMessage(byte[] buf);

    protected boolean handleIncomingMessage(byte[] buf) {
        ByteArrayDataInput dataInput = ByteStreams.newDataInput(buf);
        String decodedString = dataInput.readUTF();
        return this.consumer.consumeIncomingMessageAsString(decodedString);
    }

}
