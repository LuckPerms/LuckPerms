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

package me.lucko.luckperms.common.util;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

public final class Iterators {
    private Iterators() {}

    public static <E> void tryIterate(Iterable<E> iterable, Consumer<E> action) {
        for (E element : iterable) {
            try {
                action.accept(element);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public static <I, O> void tryIterate(Iterable<I> iterable, Function<I, O> mapping, Consumer<O> action) {
        for (I element : iterable) {
            try {
                action.accept(mapping.apply(element));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public static <E> void tryIterate(E[] array, Consumer<E> action) {
        for (E element : array) {
            try {
                action.accept(element);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public static <I, O> void tryIterate(I[] array, Function<I, O> mapping, Consumer<O> action) {
        for (I element : array) {
            try {
                action.accept(mapping.apply(element));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public static <E> List<List<E>> divideIterable(Iterable<E> source, int size) {
        List<List<E>> lists = new ArrayList<>();
        Iterator<E> it = source.iterator();
        while (it.hasNext()) {
            List<E> subList = new ArrayList<>();
            for (int i = 0; it.hasNext() && i < size; i++) {
                subList.add(it.next());
            }
            lists.add(subList);
        }
        return lists;
    }

}
