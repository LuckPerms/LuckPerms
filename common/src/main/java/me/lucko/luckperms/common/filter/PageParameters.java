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

package me.lucko.luckperms.common.filter;

import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

public class PageParameters {

    private final int pageSize;
    private final int pageNumber;

    public PageParameters(int pageSize, int pageNumber) {
        if (pageSize < 1) {
            throw new IllegalArgumentException("pageSize cannot be less than 1: " + pageSize);
        }
        if (pageNumber < 1) {
            throw new IllegalArgumentException("pageNumber cannot be less than 1: " + pageNumber);
        }

        this.pageSize = pageSize;
        this.pageNumber = pageNumber;
    }

    public int pageSize() {
        return this.pageSize;
    }

    public int pageNumber() {
        return this.pageNumber;
    }

    public <T> List<T> paginate(List<T> input) {
        int fromIndex = this.pageSize * (this.pageNumber - 1);
        if (fromIndex >= input.size()) {
            return Collections.emptyList();
        }

        int toIndex = Math.min(fromIndex + this.pageSize, input.size());
        return input.subList(fromIndex, toIndex);
    }

    public <T> Stream<T> paginate(Stream<T> input) {
        return input.skip((long) this.pageSize * (this.pageNumber - 1)).limit(this.pageSize);
    }

    public int getMaxPage(int totalEntries) {
        if (totalEntries == 0) {
            return 0;
        }

        return (totalEntries + this.pageSize - 1) / this.pageSize;
    }

}
