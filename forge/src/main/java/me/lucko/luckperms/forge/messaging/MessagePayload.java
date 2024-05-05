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

package me.lucko.luckperms.forge.messaging;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record MessagePayload(ResourceLocation id, io.netty.buffer.ByteBuf data) implements CustomPacketPayload {

    public static <T extends FriendlyByteBuf> StreamCodec<T, MessagePayload> codec(ResourceLocation id, int i) {
        return CustomPacketPayload.codec((message, bytebuf) -> {
            bytebuf.writeBytes(message.data);
        }, (bytebuf) -> {
            int j = bytebuf.readableBytes();

            if (j >= 0 && j <= i) {
                return new MessagePayload(id, bytebuf.readBytes(j));
            } else {
                throw new IllegalArgumentException("Payload may not be larger than " + i + " bytes");
            }
        });
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return new CustomPacketPayload.Type<>(this.id);
    }
}