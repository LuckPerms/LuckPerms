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

package me.lucko.luckperms.sponge.service;

import me.lucko.luckperms.common.model.Group;
import me.lucko.luckperms.sponge.service.internal.GroupSubject;
import me.lucko.luckperms.sponge.service.reference.LPSubjectReference;

import org.spongepowered.api.service.permission.PermissionService;

import java.util.Comparator;

public class SubjectComparator implements Comparator<LPSubjectReference> {
    private static final Comparator<LPSubjectReference> INSTANCE = new SubjectComparator();
    private static final Comparator<LPSubjectReference> REVERSE = INSTANCE.reversed();

    public static Comparator<LPSubjectReference> normal() {
        return INSTANCE;
    }

    public static Comparator<LPSubjectReference> reverse() {
        return REVERSE;
    }

    @Override
    public int compare(LPSubjectReference o1, LPSubjectReference o2) {
        if (o1.equals(o2)) {
            return 0;
        }

        boolean o1isGroup = o1.getCollectionIdentifier().equals(PermissionService.SUBJECTS_GROUP);
        boolean o2isGroup = o2.getCollectionIdentifier().equals(PermissionService.SUBJECTS_GROUP);

        if (o1isGroup != o2isGroup) {
            return o1isGroup ? 1 : -1;
        }

        // Neither are groups
        if (!o1isGroup) {
            return 1;
        }

        Group g1 = ((GroupSubject) o1.resolveLp().join()).getParent();
        Group g2 = ((GroupSubject) o2.resolveLp().join()).getParent();

        return Integer.compare(g1.getWeight().orElse(0), g2.getWeight().orElse(0)) == 1 ? 1 : -1;
    }
}
