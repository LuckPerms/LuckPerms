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

package me.lucko.luckperms.contexts;

import me.lucko.luckperms.api.context.ContextListener;
import me.lucko.luckperms.api.context.IContextCalculator;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

public class ContextManager<T> {

    private final List<IContextCalculator<T>> calculators = new CopyOnWriteArrayList<>();
    private final List<ContextListener<T>> listeners = new CopyOnWriteArrayList<>();

    public void registerCalculator(IContextCalculator<T> calculator) {
        listeners.forEach(calculator::addListener);
        calculators.add(calculator);
    }

    public void registerListener(ContextListener<T> listener) {
        for (IContextCalculator<T> calculator : calculators) {
            calculator.addListener(listener);
        }

        listeners.add(listener);
    }

    public Map<String, String> giveApplicableContext(T subject, Map<String, String> accumulator) {
        for (IContextCalculator<T> calculator : calculators) {
            calculator.giveApplicableContext(subject, accumulator);
        }
        return accumulator;
    }

    public boolean isContextApplicable(T subject, Map.Entry<String, String> context) {
        for (IContextCalculator<T> calculator : calculators) {
            if (calculator.isContextApplicable(subject, context)) {
                return true;
            }
        }
        return false;
    }
}
