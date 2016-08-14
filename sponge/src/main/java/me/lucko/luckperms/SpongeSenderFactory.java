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

package me.lucko.luckperms;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import me.lucko.luckperms.commands.SenderFactory;
import me.lucko.luckperms.constants.Constants;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.text.serializer.TextSerializers;

import java.util.UUID;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class SpongeSenderFactory extends SenderFactory<CommandSource> {
    private static SpongeSenderFactory instance = null;
    public static SpongeSenderFactory get() {
        if (instance == null){
            instance = new SpongeSenderFactory();
        }
        return instance;
    }

    @Override
    protected String getName(CommandSource source) {
        if (source instanceof Player) {
            return source.getName();
        }
        return Constants.getConsoleName();
    }

    @Override
    protected UUID getUuid(CommandSource source) {
        if (source instanceof Player) {
            return ((Player) source).getUniqueId();
        }
        return Constants.getConsoleUUID();
    }

    @SuppressWarnings("deprecation")
    @Override
    protected void sendMessage(CommandSource source, String s) {
        source.sendMessage(TextSerializers.LEGACY_FORMATTING_CODE.deserialize(s));
    }

    @Override
    protected boolean hasPermission(CommandSource source, String node) {
        return source.hasPermission(node);
    }
}
