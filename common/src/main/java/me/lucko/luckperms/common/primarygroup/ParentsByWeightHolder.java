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

package me.lucko.luckperms.common.primarygroup;

import me.lucko.luckperms.api.Node;
import me.lucko.luckperms.common.core.model.Group;
import me.lucko.luckperms.common.core.model.User;

import java.util.Comparator;
import java.util.Optional;

public class ParentsByWeightHolder extends StoredHolder {

    private String cachedValue = null;
    private boolean useCached = false;

    public ParentsByWeightHolder(User user) {
        super(user);
        user.getStateListeners().add(() -> useCached = false);
    }

    @Override
    public String getValue() {
        if (useCached) {
            return cachedValue;
        }

        cachedValue = user.getPermissions(true).stream()
                .filter(Node::isGroupNode)
                .filter(Node::getValue)
                .map(n -> Optional.ofNullable(user.getPlugin().getGroupManager().getIfLoaded(n.getGroupName())))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .sorted(Comparator.comparingInt(o -> o.getWeight().orElse(0)))
                .findFirst()
                .map(Group::getName)
                .orElse(null);

        useCached = true;
        return cachedValue;
    }
}
