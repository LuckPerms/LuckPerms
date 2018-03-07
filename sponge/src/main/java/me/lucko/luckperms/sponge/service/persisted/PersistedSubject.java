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

package me.lucko.luckperms.sponge.service.persisted;

import me.lucko.luckperms.common.buffers.BufferedRequest;
import me.lucko.luckperms.common.model.NodeMapType;
import me.lucko.luckperms.sponge.service.LuckPermsService;
import me.lucko.luckperms.sponge.service.ProxyFactory;
import me.lucko.luckperms.sponge.service.calculated.CalculatedSubject;
import me.lucko.luckperms.sponge.service.calculated.CalculatedSubjectData;
import me.lucko.luckperms.sponge.service.calculated.MonitoredSubjectData;
import me.lucko.luckperms.sponge.service.model.LPSubject;
import me.lucko.luckperms.sponge.service.model.LPSubjectData;
import me.lucko.luckperms.sponge.service.storage.SubjectStorageModel;

import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.service.permission.Subject;

import java.io.IOException;
import java.util.Optional;

/**
 * A simple persistable Subject implementation
 */
public class PersistedSubject extends CalculatedSubject implements LPSubject {
    private final String identifier;

    private final LuckPermsService service;
    private final PersistedCollection parentCollection;

    private final PersistedSubjectData subjectData;
    private final CalculatedSubjectData transientSubjectData;

    private final BufferedRequest<Void> saveBuffer = new BufferedRequest<Void>(1000L, 500L, r -> PersistedSubject.this.service.getPlugin().getBootstrap().getScheduler().doAsync(r)) {
        @Override
        protected Void perform() {
            try {
                PersistedSubject.this.service.getStorage().saveToFile(PersistedSubject.this);
            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        }
    };

    public PersistedSubject(String identifier, LuckPermsService service, PersistedCollection parentCollection) {
        super(service.getPlugin());
        this.identifier = identifier;
        this.service = service;
        this.parentCollection = parentCollection;
        this.subjectData = new PersistedSubjectData(this, NodeMapType.ENDURING, service) {
            @Override
            protected void onUpdate(boolean success) {
                super.onUpdate(success);
                if (success) {
                    fireUpdateEvent(this);
                }
            }
        };
        this.transientSubjectData = new MonitoredSubjectData(this, NodeMapType.TRANSIENT, service) {
            @Override
            protected void onUpdate(boolean success) {
                if (success) {
                    fireUpdateEvent(this);
                }
            }
        };
    }

    private void fireUpdateEvent(LPSubjectData subjectData) {
        this.service.getPlugin().getUpdateEventHandler().fireUpdateEvent(subjectData);
    }

    public void loadData(SubjectStorageModel dataHolder) {
        this.subjectData.setSave(false);
        dataHolder.applyToData(this.subjectData);
        this.subjectData.setSave(true);
    }

    public void save() {
        this.saveBuffer.request();
    }

    @Override
    public Subject sponge() {
        return ProxyFactory.toSponge(this);
    }

    @Override
    public String getIdentifier() {
        return this.identifier;
    }

    @Override
    public LuckPermsService getService() {
        return this.service;
    }

    @Override
    public PersistedCollection getParentCollection() {
        return this.parentCollection;
    }

    @Override
    public PersistedSubjectData getSubjectData() {
        return this.subjectData;
    }

    @Override
    public CalculatedSubjectData getTransientSubjectData() {
        return this.transientSubjectData;
    }

    @Override
    public Optional<CommandSource> getCommandSource() {
        return Optional.empty();
    }
}
