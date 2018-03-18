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

package me.lucko.luckperms.sponge.service.inheritance;

import me.lucko.luckperms.api.context.ImmutableContextSet;
import me.lucko.luckperms.sponge.service.calculated.CalculatedSubject;

import java.util.stream.Collectors;

public final class SubjectInheritanceGraphs {
    private static final SubjectInheritanceGraph NON_CONTEXTUAL = new NonContextual();


    public static SubjectInheritanceGraph getGraph() {
        return NON_CONTEXTUAL;
    }

    public static SubjectInheritanceGraph getGraph(ImmutableContextSet contextSet) {
        return new Contextual(contextSet);
    }

    private static final class NonContextual implements SubjectInheritanceGraph {
        @Override
        public Iterable<? extends CalculatedSubject> successors(CalculatedSubject subject) {
            return subject.getCombinedParents().stream()
                    .map(ref -> ref.resolveLp().join())
                    .filter(p -> p instanceof CalculatedSubject)
                    .map(p -> ((CalculatedSubject) p))
                    .collect(Collectors.toList());
        }
    }

    private static final class Contextual implements SubjectInheritanceGraph {

        /**
         * The contexts to resolve inheritance in.
         */
        private final ImmutableContextSet contextSet;

        private Contextual(ImmutableContextSet contextSet) {
            this.contextSet = contextSet;
        }

        @Override
        public Iterable<? extends CalculatedSubject> successors(CalculatedSubject subject) {
            return subject.getCombinedParents(this.contextSet).stream()
                    .map(ref -> ref.resolveLp().join())
                    .filter(p -> p instanceof CalculatedSubject)
                    .map(p -> ((CalculatedSubject) p))
                    .collect(Collectors.toList());
        }
    }

    private SubjectInheritanceGraphs() {}
}
