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

package me.lucko.luckperms.library;

import me.lucko.luckperms.common.locale.TranslationManager;
import me.lucko.luckperms.common.sender.SenderFactory;
import me.lucko.luckperms.library.sender.LibrarySender;
import net.kyori.adventure.text.Component;
import net.luckperms.api.util.Tristate;

import java.util.UUID;

public class LibrarySenderFactory extends SenderFactory<LPLibraryPlugin, LibrarySender> {

    public LibrarySenderFactory(LPLibraryPlugin plugin) {
        super(plugin);
    }

    @Override
    protected String getName(LibrarySender sender) {
        return sender.getName();
    }

    @Override
    protected UUID getUniqueId(LibrarySender sender) {
        return sender.getUniqueId();
    }

    @Override
    protected void sendMessage(LibrarySender sender, Component message) {
        Component rendered = TranslationManager.render(message, sender.getLocale());
        sender.sendMessage(rendered);
    }

    @Override
    protected Tristate getPermissionValue(LibrarySender sender, String node) {
        return sender.getPermissionValue(node);
    }

    @Override
    protected boolean hasPermission(LibrarySender sender, String node) {
        return sender.hasPermission(node);
    }

    @Override
    protected void performCommand(LibrarySender sender, String command) {
        sender.performCommand(command);
    }

    @Override
    protected boolean isConsole(LibrarySender sender) {
        return sender.isConsole();
    }

    @Override
    protected boolean shouldSplitNewlines(LibrarySender sender) {
        return sender.shouldSplitNewlines();
    }
}
