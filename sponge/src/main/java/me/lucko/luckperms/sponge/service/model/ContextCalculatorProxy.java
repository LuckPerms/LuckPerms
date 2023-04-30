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

package me.lucko.luckperms.sponge.service.model;

import me.lucko.luckperms.common.context.calculator.ForwardingContextCalculator;
import net.luckperms.api.context.ContextConsumer;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.spongepowered.api.event.Cause;
import org.spongepowered.api.event.EventContext;
import org.spongepowered.api.event.EventContextKeys;
import org.spongepowered.api.service.context.Context;
import org.spongepowered.api.service.context.ContextCalculator;
import org.spongepowered.api.service.permission.Subject;

import java.util.function.Consumer;

public class ContextCalculatorProxy implements ForwardingContextCalculator<Subject> {
    private final ContextCalculator delegate;

    public ContextCalculatorProxy(ContextCalculator delegate) {
        this.delegate = delegate;
    }

    @Override
    public void calculate(@NonNull Subject subject, @NonNull ContextConsumer consumer) {
        EventContext eventContext = EventContext.builder()
                .add(EventContextKeys.SUBJECT, subject)
                .build();

        Cause cause = Cause.builder()
                .append(subject)
                .build(eventContext);

        calculate(cause, consumer);
    }

    public void calculate(@NonNull Cause cause, @NonNull ContextConsumer consumer) {
        this.delegate.accumulateContexts(cause, new ForwardingContextConsumer(consumer));
    }

    @Override
    public Object delegate() {
        return this.delegate;
    }

    private static final class ForwardingContextConsumer implements Consumer<Context> {
        private final ContextConsumer consumer;

        private ForwardingContextConsumer(ContextConsumer consumer) {
            this.consumer = consumer;
        }

        @Override
        public void accept(Context context) {
            if (net.luckperms.api.context.Context.isValidKey(context.getKey()) &&
                    net.luckperms.api.context.Context.isValidValue(context.getValue())) {
                this.consumer.accept(context.getKey(), context.getValue());
            }
        }
    }

}
