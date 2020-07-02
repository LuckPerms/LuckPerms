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

package me.lucko.luckperms.common.node;

import net.luckperms.api.node.NodeEqualityPredicate;

public enum NodeEquality {
    KEY_VALUE_EXPIRY_CONTEXTS {
        @Override
        public boolean equals(AbstractNode<?, ?> o1, AbstractNode<?, ?> o2) {
            return o1 == o2 ||
                    o1.key.equals(o2.key) &&
                    o1.value == o2.value &&
                    o1.expireAt == o2.expireAt &&
                    o1.getContexts().equals(o2.getContexts());
        }
    },
    KEY_EXPIRY_CONTEXTS {
        @Override
        public boolean equals(AbstractNode<?, ?> o1, AbstractNode<?, ?> o2) {
            return o1 == o2 ||
                    o1.key.equals(o2.key) &&
                    o1.expireAt == o2.expireAt &&
                    o1.getContexts().equals(o2.getContexts());
        }
    },
    KEY_VALUE_HASEXPIRY_CONTEXTS {
        @Override
        public boolean equals(AbstractNode<?, ?> o1, AbstractNode<?, ?> o2) {
            return o1 == o2 ||
                    o1.key.equals(o2.key) &&
                    o1.value == o2.value &&
                    o1.hasExpiry() == o2.hasExpiry() &&
                    o1.getContexts().equals(o2.getContexts());
        }
    },
    KEY_HASEXPIRY_CONTEXTS {
        @Override
        public boolean equals(AbstractNode<?, ?> o1, AbstractNode<?, ?> o2) {
            return o1 == o2 ||
                    o1.key.equals(o2.key) &&
                    o1.hasExpiry() == o2.hasExpiry() &&
                    o1.getContexts().equals(o2.getContexts());
        }
    },
    KEY_CONTEXTS {
        @Override
        public boolean equals(AbstractNode<?, ?> o1, AbstractNode<?, ?> o2) {
            return o1 == o2 ||
                    o1.key.equals(o2.key) &&
                    o1.getContexts().equals(o2.getContexts());
        }
    },
    KEY {
        @Override
        public boolean equals(AbstractNode<?, ?> o1, AbstractNode<?, ?> o2) {
            return o1 == o2 ||
                    o1.key.equals(o2.key);
        }
    };

    public abstract boolean equals(AbstractNode<?, ?> o1, AbstractNode<?, ?> o2);

    public boolean comparesContexts() {
        return this != KEY;
    }

    public static NodeEquality of(NodeEqualityPredicate equalityPredicate) {
        if (equalityPredicate == NodeEqualityPredicate.EXACT) {
            return NodeEquality.KEY_VALUE_EXPIRY_CONTEXTS;
        } else if (equalityPredicate == NodeEqualityPredicate.IGNORE_VALUE) {
            return NodeEquality.KEY_EXPIRY_CONTEXTS;
        } else if (equalityPredicate == NodeEqualityPredicate.IGNORE_EXPIRY_TIME) {
            return NodeEquality.KEY_VALUE_HASEXPIRY_CONTEXTS;
        } else if (equalityPredicate == NodeEqualityPredicate.IGNORE_EXPIRY_TIME_AND_VALUE) {
            return NodeEquality.KEY_HASEXPIRY_CONTEXTS;
        } else if (equalityPredicate == NodeEqualityPredicate.IGNORE_VALUE_OR_IF_TEMPORARY) {
            return NodeEquality.KEY_CONTEXTS;
        } else if (equalityPredicate == NodeEqualityPredicate.ONLY_KEY) {
            return NodeEquality.KEY;
        } else {
            return null;
        }
    }

    public static boolean comparesContexts(NodeEqualityPredicate equalityPredicate) {
        NodeEquality nodeEquality = of(equalityPredicate);
        return nodeEquality != null && nodeEquality.comparesContexts();
    }

}
