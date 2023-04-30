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

import com.google.gson.JsonObject;
import me.lucko.luckperms.common.command.abstraction.SingleCommand;
import me.lucko.luckperms.common.command.access.CommandPermission;
import me.lucko.luckperms.common.command.spec.CommandSpec;
import me.lucko.luckperms.common.command.utils.ArgumentList;
import me.lucko.luckperms.common.http.UnsuccessfulRequestException;
import me.lucko.luckperms.common.locale.Message;
import me.lucko.luckperms.common.plugin.LuckPermsPlugin;
import me.lucko.luckperms.common.sender.Sender;
import me.lucko.luckperms.common.util.Predicates;
import me.lucko.luckperms.common.webeditor.WebEditorResponse;

import java.io.IOException;

public class ApplyEditsCommand extends SingleCommand {
    public ApplyEditsCommand() {
        super(CommandSpec.APPLY_EDITS, "ApplyEdits", CommandPermission.APPLY_EDITS, Predicates.notInRange(1, 2));
    }

    @Override
    public void execute(LuckPermsPlugin plugin, Sender sender, ArgumentList args, String label) {
        boolean ignoreSessionWarning = args.remove("--force");

        String code = args.get(0);

        if (code.isEmpty()) {
            Message.APPLY_EDITS_INVALID_CODE.send(sender, code);
            return;
        }

        JsonObject data;
        try {
            data = plugin.getBytebin().getJsonContent(code).getAsJsonObject();
        } catch (UnsuccessfulRequestException e) {
            Message.EDITOR_HTTP_REQUEST_FAILURE.send(sender, e.getResponse().code(), e.getResponse().message());
            return;
        } catch (IOException e) {
            plugin.getLogger().warn("Error reading data from bytebin", e);
            Message.EDITOR_HTTP_UNKNOWN_FAILURE.send(sender);
            return;
        }

        if (data == null) {
            Message.APPLY_EDITS_UNABLE_TO_READ.send(sender, code);
            return;
        }

        new WebEditorResponse(code, data).apply(plugin, sender, null, label, ignoreSessionWarning);
    }

    @Override
    public boolean shouldDisplay() {
        return false;
    }
}
