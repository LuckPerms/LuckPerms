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

package me.lucko.luckperms.common.contexts;

import lombok.AllArgsConstructor;

import com.google.common.collect.Maps;

import me.lucko.luckperms.api.context.ContextCalculator;
import me.lucko.luckperms.api.context.MutableContextSet;
import me.lucko.luckperms.common.config.ConfigKeys;
import me.lucko.luckperms.common.config.LPConfiguration;

import java.util.Map;

@AllArgsConstructor
public class ServerCalculator<T> extends ContextCalculator<T> {
    private final LPConfiguration config;

    @Override
    public MutableContextSet giveApplicableContext(T subject, MutableContextSet accumulator) {
        accumulator.add(Maps.immutableEntry("server", config.get(ConfigKeys.SERVER)));
        return accumulator;
    }

    @Override
    public boolean isContextApplicable(T subject, Map.Entry<String, String> context) {
        return context.getKey().equals("server") && config.get(ConfigKeys.SERVER).equalsIgnoreCase(context.getValue());
    }
}
