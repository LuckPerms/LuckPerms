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

package me.lucko.luckperms.sponge.contexts;

import lombok.AllArgsConstructor;

import me.lucko.luckperms.api.context.ContextCalculator;
import me.lucko.luckperms.api.context.MutableContextSet;
import me.lucko.luckperms.sponge.service.model.CompatibilityUtil;

import org.spongepowered.api.service.context.Context;
import org.spongepowered.api.service.permission.Subject;

import java.util.HashSet;
import java.util.Set;

@AllArgsConstructor
public class SpongeCalculatorLink implements ContextCalculator<Subject> {
    private final org.spongepowered.api.service.context.ContextCalculator<Subject> delegate;

    @Override
    public MutableContextSet giveApplicableContext(Subject subject, MutableContextSet accumulator) {
        Set<Context> contexts = new HashSet<Context>() {

            // don't allow null elements
            @Override
            public boolean add(Context context) {
                if (context == null) {
                    throw new NullPointerException("context");
                }

                return super.add(context);
            }
        };

        try {
            delegate.accumulateContexts(subject, contexts);
            accumulator.addAll(CompatibilityUtil.convertContexts(contexts));
        } catch (Exception e) {
            new RuntimeException("Exception thrown by delegate Sponge calculator: " + delegate.getClass().getName(), e).printStackTrace();
        }

        return accumulator;
    }
}
