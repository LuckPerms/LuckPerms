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

package me.lucko.luckperms.sponge.service.storage;

import lombok.Getter;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import me.lucko.luckperms.api.context.ContextSet;
import me.lucko.luckperms.api.context.ImmutableContextSet;
import me.lucko.luckperms.api.context.MutableContextSet;
import me.lucko.luckperms.sponge.service.calculated.CalculatedSubjectData;
import me.lucko.luckperms.sponge.service.references.SubjectReference;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Used for converting a SubjectData instance to and from JSON
 */
@Getter
public class SubjectStorageModel {
    private final Map<ImmutableContextSet, Map<String, Boolean>> permissions;
    private final Map<ImmutableContextSet, Map<String, String>> options;
    private final Map<ImmutableContextSet, List<SubjectReference>> parents;

    public SubjectStorageModel(Map<ImmutableContextSet, Map<String, Boolean>> permissions, Map<ImmutableContextSet, Map<String, String>> options, Map<ImmutableContextSet, List<SubjectReference>> parents) {
        ImmutableMap.Builder<ImmutableContextSet, Map<String, Boolean>> permissionsBuilder = ImmutableMap.builder();
        for (Map.Entry<ImmutableContextSet, Map<String, Boolean>> e : permissions.entrySet()) {
            permissionsBuilder.put(e.getKey(), ImmutableMap.copyOf(e.getValue()));
        }
        this.permissions = permissionsBuilder.build();

        ImmutableMap.Builder<ImmutableContextSet, Map<String, String>> optionsBuilder = ImmutableMap.builder();
        for (Map.Entry<ImmutableContextSet, Map<String, String>> e : options.entrySet()) {
            optionsBuilder.put(e.getKey(), ImmutableMap.copyOf(e.getValue()));
        }
        this.options = optionsBuilder.build();

        ImmutableMap.Builder<ImmutableContextSet, List<SubjectReference>> parentsBuilder = ImmutableMap.builder();
        for (Map.Entry<ImmutableContextSet, List<SubjectReference>> e : parents.entrySet()) {
            parentsBuilder.put(e.getKey(), ImmutableList.copyOf(e.getValue()));
        }
        this.parents = parentsBuilder.build();
    }

    public SubjectStorageModel(CalculatedSubjectData data) {
        this(data.getPermissions(), data.getOptions(), data.getParentsAsList());
    }
    
    public SubjectStorageModel(JsonObject root) {
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
            
            ImmutableContextSet contextSet = contextsFromJson(context);
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

            ImmutableContextSet contextSet = contextsFromJson(context);
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

            ImmutableContextSet contextSet = contextsFromJson(context);
            ImmutableList.Builder<SubjectReference> pars = ImmutableList.builder();
            for (JsonElement p : data) {
                if (!p.isJsonObject()) {
                    continue;
                }
                
                JsonObject parent = p.getAsJsonObject();
                
                String collection = parent.get("collection").getAsString();
                String subject = parent.get("subject").getAsString();
                
                pars.add(SubjectReference.of(collection, subject));
            }

            parentsBuilder.put(contextSet, pars.build());
        }
        this.parents = parentsBuilder.build();
    }
    
    public JsonObject toJson() {
        JsonObject root = new JsonObject();
        
        JsonArray permissions = new JsonArray();
        for (Map.Entry<ImmutableContextSet, Map<String, Boolean>> e : this.permissions.entrySet()) {
            if (e.getValue().isEmpty()) {
                continue;
            }

            JsonObject section = new JsonObject();
            section.add("context", contextsToJson(e.getKey()));
            
            JsonObject data = new JsonObject();
            for (Map.Entry<String, Boolean> ent : e.getValue().entrySet()) {
                data.addProperty(ent.getKey(), ent.getValue());
            }
            section.add("data", data);
            
            permissions.add(section);
        }
        root.add("permissions", permissions);

        JsonArray options = new JsonArray();
        for (Map.Entry<ImmutableContextSet, Map<String, String>> e : this.options.entrySet()) {
            if (e.getValue().isEmpty()) {
                continue;
            }

            JsonObject section = new JsonObject();
            section.add("context", contextsToJson(e.getKey()));

            JsonObject data = new JsonObject();
            for (Map.Entry<String, String> ent : e.getValue().entrySet()) {
                data.addProperty(ent.getKey(), ent.getValue());
            }
            section.add("data", data);

            options.add(section);
        }
        root.add("options", options);

        JsonArray parents = new JsonArray();
        for (Map.Entry<ImmutableContextSet, List<SubjectReference>> e : this.parents.entrySet()) {
            if (e.getValue().isEmpty()) {
                continue;
            }

            JsonObject section = new JsonObject();
            section.add("context", contextsToJson(e.getKey()));

            JsonArray data = new JsonArray();
            for (SubjectReference ref : e.getValue()) {
                JsonObject parent = new JsonObject();
                parent.addProperty("collection", ref.getCollection());
                parent.addProperty("subject", ref.getCollection());
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
    
    private static ImmutableContextSet contextsFromJson(JsonObject contexts) {
        MutableContextSet ret = MutableContextSet.create();
        for (Map.Entry<String, JsonElement> e : contexts.entrySet()) {
            String key = e.getKey();
            
            if (e.getValue().isJsonArray()) {
                JsonArray values = e.getValue().getAsJsonArray();
                for (JsonElement value : values) {
                    ret.add(key, value.getAsString());
                }
            } else {
                ret.add(key, e.getValue().getAsString());
            }
        }
        return ret.makeImmutable();
    }

    private static JsonObject contextsToJson(ContextSet contexts) {
        JsonObject ret = new JsonObject();
        for (Map.Entry<String, Collection<String>> e : contexts.toMultimap().asMap().entrySet()) {
            String key = e.getKey();
            List<String> values = new ArrayList<>(e.getValue());
            
            if (values.size() == 1) {
                ret.addProperty(key, values.get(0));
            } else if (values.size() > 1) {
                JsonArray arr = new JsonArray();
                for (String s : values) {
                    arr.add(new JsonPrimitive(s));
                }
                ret.add(key, arr);
            }
        }
        return ret;
    }
}
