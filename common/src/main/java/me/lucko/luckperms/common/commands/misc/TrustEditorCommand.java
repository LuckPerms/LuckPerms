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

package me.lucko.luckperms.common.commands.misc;

import me.lucko.luckperms.common.command.abstraction.SingleCommand;
import me.lucko.luckperms.common.command.access.CommandPermission;
import me.lucko.luckperms.common.command.spec.CommandSpec;
import me.lucko.luckperms.common.command.utils.ArgumentList;
import me.lucko.luckperms.common.locale.Message;
import me.lucko.luckperms.common.plugin.LuckPermsPlugin;
import me.lucko.luckperms.common.sender.Sender;
import me.lucko.luckperms.common.util.Predicates;
import me.lucko.luckperms.common.webeditor.socket.WebEditorSocket;

public class TrustEditorCommand extends SingleCommand {
    public TrustEditorCommand() {
        super(CommandSpec.TRUST_EDITOR, "TrustEditor", CommandPermission.TRUST_EDITOR, Predicates.not(1));
    }

    @Override
    public void execute(LuckPermsPlugin plugin, Sender sender, ArgumentList args, String label) {
        String id = args.get(0);

        if (id.isEmpty()) {
            Message.APPLY_EDITS_INVALID_CODE.send(sender, id);
            return;
        }

        WebEditorSocket socket = plugin.getWebEditorStore().sockets().getSocket(sender);
        if (socket == null) {
            Message.EDITOR_SOCKET_TRUST_FAILURE.send(sender);
            return;
        }

        if (socket.trustConnection(id)) {
            Message.EDITOR_SOCKET_TRUST_SUCCESS.send(sender);
        } else {
            Message.EDITOR_SOCKET_TRUST_FAILURE.send(sender);
        }
    }

    @Override
    public boolean shouldDisplay() {
        return false;
    }
}
