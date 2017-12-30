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

package me.lucko.luckperms.sponge.service.storage;

import lombok.Getter;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import me.lucko.luckperms.api.context.ImmutableContextSet;
import me.lucko.luckperms.common.contexts.ContextSetComparator;
import me.lucko.luckperms.common.contexts.ContextSetJsonSerializer;
import me.lucko.luckperms.common.utils.CollationKeyCache;
import me.lucko.luckperms.sponge.service.calculated.CalculatedSubjectData;
import me.lucko.luckperms.sponge.service.model.LPPermissionService;
import me.lucko.luckperms.sponge.service.model.SubjectReference;
import me.lucko.luckperms.sponge.service.model.SubjectReferenceFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Used for converting a SubjectData instance to and from JSON
 */
@Getter
public class SubjectStorageModel {
    private final LPPermissionService service;
    private final Map<ImmutableContextSet, Map<String, Boolean>> permissions;
    private final Map<ImmutableContextSet, Map<String, String>> options;
    private final Map<ImmutableContextSet, List<SubjectReference>> parents;

    public SubjectStorageModel(LPPermissionService service, Map<ImmutableContextSet, ? extends Map<String, Boolean>> permissions, Map<ImmutableContextSet, ? extends Map<String, String>> options, Map<ImmutableContextSet, ? extends List<SubjectReference>> parents) {
        this.service = service;

        ImmutableMap.Builder<ImmutableContextSet, Map<String, Boolean>> permissionsBuilder = ImmutableMap.builder();
        for (Map.Entry<ImmutableContextSet, ? extends Map<String, Boolean>> e : permissions.entrySet()) {
            permissionsBuilder.put(e.getKey(), ImmutableMap.copyOf(e.getValue()));
        }
        this.permissions = permissionsBuilder.build();

        ImmutableMap.Builder<ImmutableContextSet, Map<String, String>> optionsBuilder = ImmutableMap.builder();
        for (Map.Entry<ImmutableContextSet, ? extends Map<String, String>> e : options.entrySet()) {
            optionsBuilder.put(e.getKey(), ImmutableMap.copyOf(e.getValue()));
        }
        this.options = optionsBuilder.build();

        ImmutableMap.Builder<ImmutableContextSet, List<SubjectReference>> parentsBuilder = ImmutableMap.builder();
        for (Map.Entry<ImmutableContextSet, ? extends List<SubjectReference>> e : parents.entrySet()) {
            parentsBuilder.put(e.getKey(), ImmutableList.copyOf(e.getValue()));
        }
        this.parents = parentsBuilder.build();
    }

    public SubjectStorageModel(CalculatedSubjectData data) {
        this(data.getParentSubject().getService(), data.getAllPermissions(), data.getAllOptions(), data.getAllParents());
    }
    
    public SubjectStorageModel(LPPermissionService service, JsonObject root) {
        this.service = service;

        Preconditions.checkArgument(root.get("permissions").isJsonArray());
        Preconditions.checkArgument(root.get("options").isJsonArray());
        Preconditions.checkArgument(root.get("parents").isJsonArray());
        
        JsonArray permissions = root.get("permissions").getAsJsonArray();
        JsonArray options = root.get("options").getAsJsonArray();
        JsonArray parents = root.get("parents").getAsJsonArray();

        ImmutableMap.Builder<ImmutableContextSet, Map<String, Boolean>> permissionsBuilder = ImmutableMap.builder();
        for (JsonElement e : permissions) {
            if (!e.isJsonObject()) {
                continue;
            }
            
            JsonObject section = e.getAsJsonObject();
            if (!section.get("context").isJsonObject()) continue;
            if (!section.get("data").isJsonObject()) continue;
            
            JsonObject context = section.get("context").getAsJsonObject();
            JsonObject data = section.get("data").getAsJsonObject();
            
            ImmutableContextSet contextSet = ContextSetJsonSerializer.deserializeContextSet(context).makeImmutable();
            ImmutableMap.Builder<String, Boolean> perms = ImmutableMap.builder();
            for (Map.Entry<String, JsonElement> perm : data.entrySet()) {
                perms.put(perm.getKey(), perm.getValue().getAsBoolean());
            }
            
            permissionsBuilder.put(contextSet, perms.build());
        }
        this.permissions = permissionsBuilder.build();

        ImmutableMap.Builder<ImmutableContextSet, Map<String, String>> optionsBuilder = ImmutableMap.builder();
        for (JsonElement e : options) {
            if (!e.isJsonObject()) {
                continue;
            }

            JsonObject section = e.getAsJsonObject();
            if (!section.get("context").isJsonObject()) continue;
            if (!section.get("data").isJsonObject()) continue;

            JsonObject context = section.get("context").getAsJsonObject();
            JsonObject data = section.get("data").getAsJsonObject();

            ImmutableContextSet contextSet = ContextSetJsonSerializer.deserializeContextSet(context).makeImmutable();
            ImmutableMap.Builder<String, String> opts = ImmutableMap.builder();
            for (Map.Entry<String, JsonElement> opt : data.entrySet()) {
                opts.put(opt.getKey(), opt.getValue().getAsString());
            }

            optionsBuilder.put(contextSet, opts.build());
        }
        this.options = optionsBuilder.build();

        ImmutableMap.Builder<ImmutableContextSet, List<SubjectReference>> parentsBuilder = ImmutableMap.builder();
        for (JsonElement e : parents) {
            if (!e.isJsonObject()) {
                continue;
            }

            JsonObject section = e.getAsJsonObject();
            if (!section.get("context").isJsonObject()) continue;
            if (!section.get("data").isJsonArray()) continue;

            JsonObject context = section.get("context").getAsJsonObject();
            JsonArray data = section.get("data").getAsJsonArray();

            ImmutableContextSet contextSet = ContextSetJsonSerializer.deserializeContextSet(context).makeImmutable();
            ImmutableList.Builder<SubjectReference> pars = ImmutableList.builder();
            for (JsonElement p : data) {
                if (!p.isJsonObject()) {
                    continue;
                }
                
                JsonObject parent = p.getAsJsonObject();
                
                String collection = parent.get("collection").getAsString();
                String subject = parent.get("subject").getAsString();
                
                pars.add(SubjectReferenceFactory.obtain(service, collection, subject));
            }

            parentsBuilder.put(contextSet, pars.build());
        }
        this.parents = parentsBuilder.build();
    }

    private static <T> List<Map.Entry<ImmutableContextSet, T>> sortContextMap(Map<ImmutableContextSet, T> map) {
        List<Map.Entry<ImmutableContextSet, T>> entries = new ArrayList<>(map.entrySet());
        entries.sort((o1, o2) -> ContextSetComparator.reverse().compare(o1.getKey(), o2.getKey()));
        return entries;
    }
    
    public JsonObject toJson() {
        JsonObject root = new JsonObject();
        
        JsonArray permissions = new JsonArray();
        for (Map.Entry<ImmutableContextSet, Map<String, Boolean>> e : sortContextMap(this.permissions)) {
            if (e.getValue().isEmpty()) {
                continue;
            }

            JsonObject section = new JsonObject();
            section.add("context", ContextSetJsonSerializer.serializeContextSet(e.getKey()));
            
            JsonObject data = new JsonObject();

            // sort alphabetically.
            List<Map.Entry<String, Boolean>> perms = new ArrayList<>(e.getValue().entrySet());
            perms.sort(Map.Entry.comparingByKey(CollationKeyCache.comparator()));

            for (Map.Entry<String, Boolean> ent : perms) {
                data.addProperty(ent.getKey(), ent.getValue());
            }
            section.add("data", data);
            
            permissions.add(section);
        }
        root.add("permissions", permissions);

        JsonArray options = new JsonArray();
        for (Map.Entry<ImmutableContextSet, Map<String, String>> e : sortContextMap(this.options)) {
            if (e.getValue().isEmpty()) {
                continue;
            }

            JsonObject section = new JsonObject();
            section.add("context", ContextSetJsonSerializer.serializeContextSet(e.getKey()));

            JsonObject data = new JsonObject();

            // sort alphabetically.
            List<Map.Entry<String, String>> opts = new ArrayList<>(e.getValue().entrySet());
            opts.sort(Map.Entry.comparingByKey(CollationKeyCache.comparator()));

            for (Map.Entry<String, String> ent : opts) {
                data.addProperty(ent.getKey(), ent.getValue());
            }
            section.add("data", data);

            options.add(section);
        }
        root.add("options", options);

        JsonArray parents = new JsonArray();
        for (Map.Entry<ImmutableContextSet, List<SubjectReference>> e : sortContextMap(this.parents)) {
            if (e.getValue().isEmpty()) {
                continue;
            }

            JsonObject section = new JsonObject();
            section.add("context", ContextSetJsonSerializer.serializeContextSet(e.getKey()));

            JsonArray data = new JsonArray();
            for (SubjectReference ref : e.getValue()) {
                JsonObject parent = new JsonObject();
                parent.addProperty("collection", ref.getCollectionIdentifier());
                parent.addProperty("subject", ref.getCollectionIdentifier());
                data.add(parent);
            }
            section.add("data", data);

            options.add(section);
        }
        root.add("parents", parents);

        return root;
    }

    public void applyToData(CalculatedSubjectData subjectData) {
        subjectData.replacePermissions(permissions);
        subjectData.replaceOptions(options);
        subjectData.replaceParents(parents);
    }
}
