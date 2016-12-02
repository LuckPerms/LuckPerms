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

package me.lucko.luckperms.sponge.service.persisted;

import lombok.Getter;
import lombok.Setter;

import me.lucko.luckperms.api.context.ContextSet;
import me.lucko.luckperms.sponge.service.LuckPermsService;
import me.lucko.luckperms.sponge.service.calculated.CalculatedSubjectData;
import me.lucko.luckperms.sponge.service.references.SubjectReference;

/**
 * Extension of MemorySubjectData which persists data when modified
 */
public class PersistedSubjectData extends CalculatedSubjectData {
    private final PersistedSubject subject;

    @Getter
    @Setter
    private boolean save = true;

    public PersistedSubjectData(LuckPermsService service, String calculatorDisplayName, PersistedSubject subject) {
        super(subject, service, calculatorDisplayName);
        this.subject = subject;
    }

    private void save() {
        if (!save) {
            return;
        }

        if (subject != null) {
            subject.save();
        }
    }

    @Override
    public boolean setPermission(ContextSet contexts, String permission, me.lucko.luckperms.api.Tristate value) {
        boolean r = super.setPermission(contexts, permission, value);
        save();
        return r;
    }

    @Override
    public boolean clearPermissions() {
        boolean r = super.clearPermissions();
        save();
        return r;
    }

    @Override
    public boolean clearPermissions(ContextSet contexts) {
        boolean r = super.clearPermissions(contexts);
        save();
        return r;
    }

    @Override
    public boolean addParent(ContextSet contexts, SubjectReference parent) {
        boolean r = super.addParent(contexts, parent);
        save();
        return r;
    }

    @Override
    public boolean removeParent(ContextSet contexts, SubjectReference parent) {
        boolean r = super.removeParent(contexts, parent);
        save();
        return r;
    }

    @Override
    public boolean clearParents() {
        boolean r = super.clearParents();
        save();
        return r;
    }

    @Override
    public boolean clearParents(ContextSet contexts) {
        boolean r = super.clearParents(contexts);
        save();
        return r;
    }

    @Override
    public boolean setOption(ContextSet contexts, String key, String value) {
        boolean r = super.setOption(contexts, key, value);
        save();
        return r;
    }

    @Override
    public boolean unsetOption(ContextSet contexts, String key) {
        boolean r = super.unsetOption(contexts, key);
        save();
        return r;
    }

    @Override
    public boolean clearOptions(ContextSet contexts) {
        boolean r = super.clearOptions(contexts);
        save();
        return r;
    }

    @Override
    public boolean clearOptions() {
        boolean r = super.clearOptions();
        save();
        return r;
    }
}
