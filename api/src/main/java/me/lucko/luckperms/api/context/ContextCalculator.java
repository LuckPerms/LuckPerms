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

package me.lucko.luckperms.api.context;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * A simple implementation of the listener aspects of {@link IContextCalculator}
 * @param <T> the subject type
 */
public abstract class ContextCalculator<T> implements IContextCalculator<T> {
    private final List<ContextListener<T>> listeners = new CopyOnWriteArrayList<>();

    /**
     * Pushes an update to all registered {@link ContextListener}s.
     * Make sure any changes are applied internally before this method is called.
     * @param subject the subject that changed
     * @param before the context state before the change
     * @param current the context state after the change (now)
     * @throws NullPointerException if any parameters are null
     */
    protected void pushUpdate(T subject, Map.Entry<String, String> before, Map.Entry<String, String> current) {
        if (subject == null) {
            throw new NullPointerException("subject");
        }
        if (before == null) {
            throw new NullPointerException("before");
        }
        if (current == null) {
            throw new NullPointerException("current");
        }

        for (ContextListener<T> listener : listeners) {
            try {
                listener.onContextChange(subject, before, current);
            } catch (Exception e) {
                System.out.println("Exception whilst passing context change to listener: " + listener);
                e.printStackTrace();
            }
        }
    }

    @Override
    public void addListener(ContextListener<T> listener) {
        if (listener == null) {
            throw new NullPointerException("listener");
        }

        listeners.add(listener);
    }

}
