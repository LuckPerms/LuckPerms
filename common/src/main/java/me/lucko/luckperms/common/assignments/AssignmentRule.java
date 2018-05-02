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

import me.lucko.luckperms.api.Node;
import me.lucko.luckperms.api.Tristate;
import me.lucko.luckperms.common.model.User;
import me.lucko.luckperms.common.node.factory.LegacyNodeFactory;
import me.lucko.luckperms.common.utils.ImmutableCollectors;

import java.util.List;

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
        if (this.hasTrueExpression != null) {
            try {
                boolean b = this.hasTrueExpression.parse(user, Tristate.TRUE);
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

        if (this.hasFalseExpression != null) {
            try {
                boolean b = this.hasFalseExpression.parse(user, Tristate.FALSE);
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

        if (this.lacksExpression != null) {
            try {
                boolean b = this.lacksExpression.parse(user, Tristate.UNDEFINED);
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
        for (Node n : this.toTake) {
            user.unsetPermission(n);
        }

        for (Node n : this.toGive) {
            user.setPermission(n);
        }

        if (this.setPrimaryGroup != null) {
            user.getPrimaryGroup().setStoredValue(this.setPrimaryGroup);
        }

        return true;
    }

    public AssignmentExpression getHasTrueExpression() {
        return this.hasTrueExpression;
    }

    public AssignmentExpression getHasFalseExpression() {
        return this.hasFalseExpression;
    }

    public AssignmentExpression getLacksExpression() {
        return this.lacksExpression;
    }

    public List<Node> getToGive() {
        return this.toGive;
    }

    public List<Node> getToTake() {
        return this.toTake;
    }

    public String getSetPrimaryGroup() {
        return this.setPrimaryGroup;
    }

    @Override
    public String toString() {
        return "AssignmentRule(" +
                "hasTrueExpression=" + this.getHasTrueExpression() + ", " +
                "hasFalseExpression=" + this.getHasFalseExpression() + ", " +
                "lacksExpression=" + this.getLacksExpression() + ", " +
                "toGive=" + this.getToGive() + ", " +
                "toTake=" + this.getToTake() + ", " +
                "setPrimaryGroup=" + this.getSetPrimaryGroup() + ")";
    }
}
