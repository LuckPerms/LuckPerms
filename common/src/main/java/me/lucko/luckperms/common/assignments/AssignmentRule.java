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

package me.lucko.luckperms.common.assignments;

import lombok.Getter;
import lombok.ToString;

import me.lucko.luckperms.api.Node;
import me.lucko.luckperms.api.Tristate;
import me.lucko.luckperms.common.model.User;
import me.lucko.luckperms.common.node.LegacyNodeFactory;
import me.lucko.luckperms.common.utils.ImmutableCollectors;

import java.util.List;

@Getter
@ToString
public class AssignmentRule {
    private final AssignmentExpression hasTrueExpression;
    private final AssignmentExpression hasFalseExpression;
    private final AssignmentExpression lacksExpression;

    private final List<Node> toGive;
    private final List<Node> toTake;
    private final String setPrimaryGroup;

    public AssignmentRule(String hasTrueExpression, String hasFalseExpression, String lacksExpression, List<String> toGive, List<String> toTake, String setPrimaryGroup) {
        this.hasTrueExpression = AssignmentExpression.compile(hasTrueExpression);
        this.hasFalseExpression = AssignmentExpression.compile(hasFalseExpression);
        this.lacksExpression = AssignmentExpression.compile(lacksExpression);
        this.toGive = toGive.stream().map(s -> LegacyNodeFactory.fromLegacyString(s, true)).collect(ImmutableCollectors.toList());
        this.toTake = toTake.stream().map(s -> LegacyNodeFactory.fromLegacyString(s, true)).collect(ImmutableCollectors.toList());
        this.setPrimaryGroup = setPrimaryGroup;
    }

    public boolean apply(User user) {
        if (hasTrueExpression != null) {
            try {
                boolean b = hasTrueExpression.parse(user, Tristate.TRUE);
                if (!b) {
                    // The holder does not meet this requirement
                    return false;
                }
            } catch (IllegalArgumentException e) {
                // Couldn't parse
                e.printStackTrace();
                return false;
            }
        }

        if (hasFalseExpression != null) {
            try {
                boolean b = hasFalseExpression.parse(user, Tristate.FALSE);
                if (!b) {
                    // The holder does not meet this requirement
                    return false;
                }
            } catch (IllegalArgumentException e) {
                // Couldn't parse
                e.printStackTrace();
                return false;
            }
        }

        if (lacksExpression != null) {
            try {
                boolean b = lacksExpression.parse(user, Tristate.UNDEFINED);
                if (!b) {
                    // The holder does not meet this requirement
                    return false;
                }
            } catch (IllegalArgumentException e) {
                // Couldn't parse
                e.printStackTrace();
                return false;
            }
        }

        // The holder meets all of the requirements of this rule.
        for (Node n : toTake) {
            user.unsetPermission(n);
        }

        for (Node n : toGive) {
            user.setPermission(n);
        }

        if (setPrimaryGroup != null) {
            user.getPrimaryGroup().setStoredValue(setPrimaryGroup);
        }

        return true;
    }
}
