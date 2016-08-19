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

package me.lucko.luckperms.utils;

import lombok.experimental.UtilityClass;
import me.lucko.luckperms.constants.Patterns;

@UtilityClass
public class ArgumentChecker {

    public static boolean checkUsername(String s) {
        return (s.length() > 16 || Patterns.NON_USERNAME.matcher(s).find());
    }

    public static boolean checkName(String s) {
        return (s.length() > 36 || Patterns.NON_ALPHA_NUMERIC.matcher(s).find());
    }

    public static boolean checkServer(String s) {
        return s.toLowerCase().startsWith("r=") || (s.startsWith("(") && s.endsWith(")") && s.contains("|")) || Patterns.NON_ALPHA_NUMERIC.matcher(s).find();
    }

    public static boolean checkNode(String s) {
        return (s.contains("/") || s.contains("$"));
    }

    public static boolean checkTime(long l) {
        return DateUtil.shouldExpire(l);
    }

}
