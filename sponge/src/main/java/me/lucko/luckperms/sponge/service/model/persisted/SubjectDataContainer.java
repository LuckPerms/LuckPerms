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

package me.lucko.luckperms.sponge.service.model.persisted;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import me.lucko.luckperms.common.context.comparator.ContextSetComparator;
import me.lucko.luckperms.common.context.serializer.ContextSetJsonSerializer;
import me.lucko.luckperms.sponge.service.model.LPPermissionService;
import me.lucko.luckperms.sponge.service.model.LPSubjectData;
import me.lucko.luckperms.sponge.service.model.LPSubjectReference;
import me.lucko.luckperms.sponge.service.model.calculated.CalculatedSubjectData;
import net.luckperms.api.context.ImmutableContextSet;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Immutable container for a subjects persisted data
 */
public class SubjectDataContainer {

    /**
     * Creates a {@link SubjectDataContainer} container for the given {@link LPSubjectData}.
     *
     * @param subjectData the subject data to copy from
     * @return a new container
     */
    public static SubjectDataContainer copyOf(LPSubjectData subjectData) {
        return new SubjectDataContainer(subjectData.getAllPermissions(), subjectData.getAllOptions(), subjectData.getAllParents());
    }

    /**
     * Deserializes a {@link SubjectDataContainer} container from a json object
     *
     * @param service the permission service
     * @param root the root json object
     * @return a container representing the json data
     */
    public static SubjectDataContainer deserialize(LPPermissionService service, JsonObject root) {
        return new SubjectDataContainer(service, root);
    }

    private final Map<ImmutableContextSet, Map<String, Boolean>> permissions;
    private final Map<ImmutableContextSet, Map<String, String>> options;
    private final Map<ImmutableContextSet, List<LPSubjectReference>> parents;

    private SubjectDataContainer(Map<ImmutableContextSet, ? extends Map<String, Boolean>> permissions, Map<ImmutableContextSet, ? extends Map<String, String>> options, Map<ImmutableContextSet, ? extends List<LPSubjectReference>> parents) {
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

        ImmutableMap.Builder<ImmutableContextSet, List<LPSubjectReference>> parentsBuilder = ImmutableMap.builder();
        for (Map.Entry<ImmutableContextSet, ? extends List<LPSubjectReference>> e : parents.entrySet()) {
            parentsBuilder.put(e.getKey(), ImmutableList.copyOf(e.getValue()));
        }
        this.parents = parentsBuilder.build();
    }

    private static JsonArray getArray(JsonObject root, String key) {
        JsonElement element = root.get(key);
        if (element == null) {
            return new JsonArray();
        }
        Preconditions.checkArgument(element instanceof JsonArray);
        return (JsonArray) element;
    }
    
    private SubjectDataContainer(LPPermissionService service, JsonObject root) {
        JsonArray permissions = getArray(root, "permissions");
        JsonArray options = getArray(root, "options");
        JsonArray parents = getArray(root, "parents");

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

            ImmutableContextSet contextSet = ContextSetJsonSerializer.deserialize(context).immutableCopy();
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

            ImmutableContextSet contextSet = ContextSetJsonSerializer.deserialize(context).immutableCopy();
            ImmutableMap.Builder<String, String> opts = ImmutableMap.builder();
            for (Map.Entry<String, JsonElement> opt : data.entrySet()) {
                opts.put(opt.getKey(), opt.getValue().getAsString());
            }

            optionsBuilder.put(contextSet, opts.build());
        }
        this.options = optionsBuilder.build();

        ImmutableMap.Builder<ImmutableContextSet, List<LPSubjectReference>> parentsBuilder = ImmutableMap.builder();
        for (JsonElement e : parents) {
            if (!e.isJsonObject()) {
                continue;
            }

            JsonObject section = e.getAsJsonObject();
            if (!section.get("context").isJsonObject()) continue;
            if (!section.get("data").isJsonArray()) continue;

            JsonObject context = section.get("context").getAsJsonObject();
            JsonArray data = section.get("data").getAsJsonArray();

            ImmutableContextSet contextSet = ContextSetJsonSerializer.deserialize(context).immutableCopy();
            ImmutableList.Builder<LPSubjectReference> pars = ImmutableList.builder();
            for (JsonElement p : data) {
                if (!p.isJsonObject()) {
                    continue;
                }

                JsonObject parent = p.getAsJsonObject();

                String collection = parent.get("collection").getAsString();
                String subject = parent.get("subject").getAsString();

                pars.add(service.getReferenceFactory().obtain(collection, subject));
            }

            parentsBuilder.put(contextSet, pars.build());
        }
        this.parents = parentsBuilder.build();
    }

    /**
     * Converts this container to json
     *
     * @return a json serialisation of the data in this container
     */
    public JsonObject serialize() {
        JsonObject root = new JsonObject();
        
        JsonArray permissions = new JsonArray();
        for (Map.Entry<ImmutableContextSet, Map<String, Boolean>> e : sortContextMap(this.permissions)) {
            if (e.getValue().isEmpty()) {
                continue;
            }

            JsonObject section = new JsonObject();
            section.add("context", ContextSetJsonSerializer.serialize(e.getKey()));
            
            JsonObject data = new JsonObject();

            // sort alphabetically.
            List<Map.Entry<String, Boolean>> perms = new ArrayList<>(e.getValue().entrySet());
            perms.sort(Map.Entry.comparingByKey());

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
            section.add("context", ContextSetJsonSerializer.serialize(e.getKey()));

            JsonObject data = new JsonObject();

            // sort alphabetically.
            List<Map.Entry<String, String>> opts = new ArrayList<>(e.getValue().entrySet());
            opts.sort(Map.Entry.comparingByKey());

            for (Map.Entry<String, String> ent : opts) {
                data.addProperty(ent.getKey(), ent.getValue());
            }
            section.add("data", data);

            options.add(section);
        }
        root.add("options", options);

        JsonArray parents = new JsonArray();
        for (Map.Entry<ImmutableContextSet, List<LPSubjectReference>> e : sortContextMap(this.parents)) {
            if (e.getValue().isEmpty()) {
                continue;
            }

            JsonObject section = new JsonObject();
            section.add("context", ContextSetJsonSerializer.serialize(e.getKey()));

            JsonArray data = new JsonArray();
            for (LPSubjectReference ref : e.getValue()) {
                JsonObject parent = new JsonObject();
                parent.addProperty("collection", ref.collectionIdentifier());
                parent.addProperty("subject", ref.subjectIdentifier());
                data.add(parent);
            }
            section.add("data", data);

            options.add(section);
        }
        root.add("parents", parents);

        return root;
    }

    /**
     * Applies the data encapsulated by this container to the given
     * {@link CalculatedSubjectData}.
     *
     * @param subjectData the data to apply to
     */
    public void applyToData(CalculatedSubjectData subjectData) {
        subjectData.replacePermissions(this.permissions);
        subjectData.replaceOptions(this.options);
        subjectData.replaceParents(this.parents);
    }

    private static <T> List<Map.Entry<ImmutableContextSet, T>> sortContextMap(Map<ImmutableContextSet, T> map) {
        List<Map.Entry<ImmutableContextSet, T>> entries = new ArrayList<>(map.entrySet());
        entries.sort((o1, o2) -> ContextSetComparator.reverse().compare(o1.getKey(), o2.getKey()));
        return entries;
    }

}
