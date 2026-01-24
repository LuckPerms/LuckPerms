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

package me.lucko.luckperms.hytale.calculator;

import me.lucko.luckperms.common.cacheddata.result.TristateResult;
import me.lucko.luckperms.common.calculator.PermissionLookupCache;
import me.lucko.luckperms.common.calculator.processor.AbstractPermissionProcessor;
import me.lucko.luckperms.common.calculator.processor.PermissionProcessor;
import me.lucko.luckperms.common.verbose.event.CheckOrigin;

/**
 * Permission Processor for Hytale "virtual groups".
 */
public class HytaleVirtualGroupProcessor extends AbstractPermissionProcessor implements PermissionProcessor {
    public static final TristateResult.Factory RESULT_FACTORY = new TristateResult.Factory(HytaleVirtualGroupProcessor.class);

    private final PermissionLookupCache lookup;

    public HytaleVirtualGroupProcessor(PermissionLookupCache lookup) {
        this.lookup = lookup;
    }

    @Override
    public TristateResult hasPermission(String permission) {
        return RESULT_FACTORY.result(this.lookup.checkPermission(permission, CheckOrigin.INTERNAL).result());
    }

}
