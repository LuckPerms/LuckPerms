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

package me.lucko.luckperms.neoforge.attachments;

import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import net.neoforged.neoforge.registries.RegisterEvent;

import java.util.function.Consumer;

public final class UserAttachmentRegister implements Consumer<RegisterEvent> {
    @Override
    public void accept(RegisterEvent event) {
        if (event.getRegistryKey() == NeoForgeRegistries.Keys.ATTACHMENT_TYPES) {
            /*
             * Register our attachment type.
             * Our attachment does not need serialisation, so we do not call serialize(...) here.
             * copyOnDeath(), copyHandler() are also for serialisable attachments only, so we do not use them.
             * Non-serialisable attachments are safe for server-only mods.
             * Attachments are lazily evaluated; they are not created until first getData() call.
             * One pitfall is that, you can call getData(UserAttachment.TYPE) on all entities, or even non-entities.
             * There are no wau to prevent creating UserAttachment on non-player.
             * To avoid issue, we need to make sure we only ever call ServerPlayer.getData().
             */
            event.register(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, UserAttachment.IDENTIFIER,
                    () -> AttachmentType.<UserAttachment>builder(UserAttachmentImpl::new).build());
        }
    }
}
