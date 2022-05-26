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

package me.lucko.luckperms.forge.calculator;

import me.lucko.luckperms.common.cacheddata.result.TristateResult;
import me.lucko.luckperms.common.calculator.processor.AbstractPermissionProcessor;
import me.lucko.luckperms.common.calculator.processor.PermissionProcessor;

import net.luckperms.api.util.Tristate;

/**
 * Permission processor which is added to the owner of an Integrated server to
 * simply return true if no other processors match.
 */
public class ServerOwnerProcessor extends AbstractPermissionProcessor implements PermissionProcessor {
    private static final TristateResult TRUE_RESULT = new TristateResult.Factory(ServerOwnerProcessor.class).result(Tristate.TRUE);

    public static final ServerOwnerProcessor INSTANCE = new ServerOwnerProcessor();

    private ServerOwnerProcessor() {

    }

    @Override
    public TristateResult hasPermission(String permission) {
        return TRUE_RESULT;
    }

}
