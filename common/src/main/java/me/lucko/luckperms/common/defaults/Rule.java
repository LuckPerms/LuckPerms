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

package me.lucko.luckperms.common.defaults;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;

import me.lucko.luckperms.api.Tristate;
import me.lucko.luckperms.common.model.User;
import me.lucko.luckperms.common.node.NodeFactory;

import java.util.List;

@Getter
@ToString
@AllArgsConstructor
public class Rule {
    private final String hasTrueExpression;
    private final String hasFalseExpression;
    private final String lacksExpression;

    private final List<String> toGive;
    private final List<String> toTake;
    private final String setPrimaryGroup;

    public boolean apply(User user) {
        if (hasTrueExpression != null) {
            try {
                boolean b = LogicParser.parse(hasTrueExpression, user, Tristate.TRUE);
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
                boolean b = LogicParser.parse(hasFalseExpression, user, Tristate.FALSE);
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
                boolean b = LogicParser.parse(lacksExpression, user, Tristate.UNDEFINED);
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
        for (String s : toTake) {
            user.unsetPermission(NodeFactory.fromSerializedNode(s, true));
        }

        for (String s : toGive) {
            user.setPermission(NodeFactory.fromSerializedNode(s, true));
        }

        if (setPrimaryGroup != null) {
            user.getPrimaryGroup().setStoredValue(setPrimaryGroup);
        }

        return true;
    }
}
