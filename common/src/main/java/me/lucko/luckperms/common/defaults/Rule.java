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

package me.lucko.luckperms.common.defaults;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;
import me.lucko.luckperms.api.Tristate;
import me.lucko.luckperms.common.core.NodeFactory;
import me.lucko.luckperms.common.users.User;
import me.lucko.luckperms.exceptions.ObjectAlreadyHasException;
import me.lucko.luckperms.exceptions.ObjectLacksException;

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
            try {
                user.unsetPermission(NodeFactory.fromSerialisedNode(s, true));
            } catch (ObjectLacksException ignored) {}
        }

        for (String s : toGive) {
            try {
                user.setPermission(NodeFactory.fromSerialisedNode(s, true));
            } catch (ObjectAlreadyHasException ignored) {}
        }

        if (setPrimaryGroup != null) {
            user.setPrimaryGroup(setPrimaryGroup);
        }

        return true;
    }
}
