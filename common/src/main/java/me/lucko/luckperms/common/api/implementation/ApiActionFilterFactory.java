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

package me.lucko.luckperms.common.api.implementation;

import me.lucko.luckperms.common.actionlog.filter.ActionFilters;
import net.luckperms.api.actionlog.filter.ActionFilter;
import net.luckperms.api.actionlog.filter.ActionFilterFactory;

import java.util.Objects;
import java.util.UUID;

public final class ApiActionFilterFactory implements ActionFilterFactory {
    public static final ApiActionFilterFactory INSTANCE = new ApiActionFilterFactory();

    private ApiActionFilterFactory() {

    }

    @Override
    public ActionFilter any() {
        return new ApiActionFilter(ActionFilters.all());
    }

    @Override
    public ActionFilter source(UUID uniqueId) {
        Objects.requireNonNull(uniqueId, "uniqueId");
        return new ApiActionFilter(ActionFilters.source(uniqueId));
    }

    @Override
    public ActionFilter user(UUID uniqueId) {
        Objects.requireNonNull(uniqueId, "uniqueId");
        return new ApiActionFilter(ActionFilters.user(uniqueId));
    }

    @Override
    public ActionFilter group(String name) {
        Objects.requireNonNull(name, "name");
        return new ApiActionFilter(ActionFilters.group(name));
    }

    @Override
    public ActionFilter track(String name) {
        Objects.requireNonNull(name, "name");
        return new ApiActionFilter(ActionFilters.track(name));
    }

    @Override
    public ActionFilter search(String query) {
        Objects.requireNonNull(query, "query");
        return new ApiActionFilter(ActionFilters.search(query));
    }

}
