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

package me.lucko.luckperms.common.calculators;

import me.lucko.luckperms.api.Contexts;
import me.lucko.luckperms.common.model.User;

import java.util.List;

/**
 * Creates a calculator instance given a set of contexts
 */
public interface CalculatorFactory {

    /**
     * Builds a PermissionCalculator for the user in the given context
     *
     * @param contexts the contexts to build the calculator in
     * @param user     the user to build for
     * @return a permission calculator instance
     */
    PermissionCalculator build(Contexts contexts, User user);

    /**
     * Gets the processors which are currently being added to built calculators
     *
     * @return a list of processors
     */
    List<String> getActiveProcessors();

    /**
     * Invalidates all calculators build by this factory
     */
    void invalidateAll();

}
