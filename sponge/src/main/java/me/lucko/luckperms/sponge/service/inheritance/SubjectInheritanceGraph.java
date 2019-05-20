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

import me.lucko.luckperms.api.query.QueryOptions;
import me.lucko.luckperms.common.graph.Graph;
import me.lucko.luckperms.sponge.service.model.calculated.CalculatedSubject;

import java.util.stream.Collectors;

/**
 * A {@link Graph} which represents an "inheritance tree" for subjects.
 */
public class SubjectInheritanceGraph implements Graph<CalculatedSubject> {

    /**
     * The contexts to resolve inheritance in.
     */
    private final QueryOptions queryOptions;

    public SubjectInheritanceGraph(QueryOptions queryOptions) {
        this.queryOptions = queryOptions;
    }

    @Override
    public Iterable<? extends CalculatedSubject> successors(CalculatedSubject subject) {
        return subject.getCombinedParents(this.queryOptions).stream()
                .map(ref -> ref.resolveLp().join())
                .filter(p -> p instanceof CalculatedSubject)
                .map(p -> ((CalculatedSubject) p))
                .collect(Collectors.toList());
    }

}
