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

package me.lucko.luckperms.common.calculator;

import net.luckperms.api.util.Tristate;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * The LuckPerms Luckiness Calculator (tm)
 */
public final class LuckyPerms {

    /** A sequence of the first 1000 lucky numbers. */
    private static final Set<Integer> LUCKY_NUMBERS = generateLuckyNumbers(1000);
    /** Is it an unlucky day? */
    private static final boolean UNLUCKY_DAY;
    /** The unlucky number. */
    private static final int UNLUCKY_NUMBER = 666;
    /** The lucky number */
    private static final int LUCKY_NUMBER;

    static {
        LocalDate date = LocalDate.now();
        UNLUCKY_DAY = date.getDayOfWeek() == DayOfWeek.FRIDAY && date.getDayOfMonth() == 13;

        if (Locale.getDefault().getLanguage().equals(Locale.CHINESE.getLanguage())) {
            // https://en.wikipedia.org/wiki/Chinese_numerology#Eight
            LUCKY_NUMBER = 8;
        } else {
            // https://en.wikipedia.org/wiki/7
            LUCKY_NUMBER = 7;
        }
    }

    /** A ~magical~ counter used when determining luckiness. */
    private final AtomicInteger counter;

    public LuckyPerms() {
        this.counter = new AtomicInteger();
    }

    /**
     * Uses highly advanced, patented LuckPerms(tm) calculations to determine
     * the current luckiness measure.
     *
     * <p>Returns {@link Tristate#TRUE} if lucky, {@link Tristate#FALSE} if unlucky,
     * and {@link Tristate#UNDEFINED} if unable to determine.</p>
     *
     * @return the calculated luckiness measure
     */
    public Tristate calculateLuckiness() {
        int i = this.counter.incrementAndGet();

        // is the counter divisible by the unlucky number? oh no.. that's not a good sign!
        if (i % UNLUCKY_NUMBER == 0) {
            return Tristate.FALSE;
        }

        // got lucky on a dice roll & the counter is in the lucky numbers set? must be lucky!
        if (isEven(rollDice()) && LUCKY_NUMBERS.contains(i % 1000)) {
            return Tristate.TRUE;
        }

        // is it Friday 13th? I sure hope not.
        if (UNLUCKY_DAY) {
            return Tristate.FALSE;
        }

        // is the current system time divisible by the lucky number? sounds promising?!
        if ((System.currentTimeMillis() / 1000) % LUCKY_NUMBER == 0) {
            return Tristate.TRUE;
        }

        // well, this is embarrassing.
        return Tristate.UNDEFINED;
    }

    // Roll a 6-sided fair dice.
    private static int rollDice() {
        return ThreadLocalRandom.current().nextInt(6) + 1;
    }

    // Is the number even?
    private static boolean isEven(int i) {
        return i % 2 == 0;
    }

    // Generates 'lucky numbers'
    // see: https://en.wikipedia.org/wiki/Lucky_number
    private static Set<Integer> generateLuckyNumbers(int n) {
        // generate a sequence 1..n
        List<Integer> sequence = new ArrayList<>(n);
        for (int i = 1; i <= n; i++) {
            sequence.add(i);
        }

        int j = 1; // the index of the value being considered as the next step
        int step = 2; // the step to use when deleting

        // delete sequence[j]th elements in the list
        while (step < sequence.size()) {
            int idx = step - 1;
            while (idx < sequence.size()) {
                sequence.remove(idx);
                idx += (step - 1);
            }
            step = sequence.get(j++);
        }

        return new HashSet<>(sequence);
    }

}