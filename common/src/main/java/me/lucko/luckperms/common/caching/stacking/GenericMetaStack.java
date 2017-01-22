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

package me.lucko.luckperms.common.caching.stacking;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import me.lucko.luckperms.common.utils.ImmutableCollectors;

import java.util.ArrayList;
import java.util.List;

@Getter
@RequiredArgsConstructor
public class GenericMetaStack implements MetaStack {

    private final List<MetaStackElement> elements;
    private final String startSpacer;
    private final String middleSpacer;
    private final String endSpacer;

    @Override
    public String toFormattedString() {
        List<MetaStackElement> ret = new ArrayList<>(elements);
        ret.removeIf(m -> !m.getEntry().isPresent());

        if (ret.isEmpty()) {
            return null;
        }

        StringBuilder sb = new StringBuilder();
        sb.append(startSpacer);
        for (int i = 0; i < ret.size(); i++) {
            if (i != 0) {
                sb.append(middleSpacer);
            }

            MetaStackElement e = ret.get(i);
            sb.append(e.getEntry().get().getValue());
        }
        sb.append(endSpacer);

        return sb.toString();
    }

    @Override
    public MetaStack copy() {
        return new GenericMetaStack(
                elements.stream().map(MetaStackElement::copy).collect(ImmutableCollectors.toImmutableList()),
                startSpacer,
                middleSpacer,
                endSpacer
        );
    }
}
