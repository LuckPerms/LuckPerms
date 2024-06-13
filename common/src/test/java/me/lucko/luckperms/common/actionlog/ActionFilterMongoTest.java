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

package me.lucko.luckperms.common.actionlog;

import me.lucko.luckperms.common.actionlog.filter.ActionFilterMongoBuilder;
import me.lucko.luckperms.common.actionlog.filter.ActionFilters;
import me.lucko.luckperms.common.filter.FilterList;
import net.luckperms.api.actionlog.Action;
import org.bson.BsonDocument;
import org.bson.UuidRepresentation;
import org.bson.codecs.configuration.CodecRegistries;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.conversions.Bson;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.UUID;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ActionFilterMongoTest {

    private static Stream<Arguments> testQueries() {
        return Stream.of(
                Arguments.of(
                        ActionFilters.source(UUID.fromString("725d585e-4ff1-4f18-acca-6ac538364080")),
                        // {"$and": [{"source.uniqueId": {"$binary": {"base64": "cl1YXk/xTxisymrFODZAgA==", "subType": "04"}}}]}
                        "{\"$and\": [{\"source.uniqueId\": {\"$binary\": {\"base64\": \"cl1YXk/xTxisymrFODZAgA==\", \"subType\": \"04\"}}}]}"
                ),
                Arguments.of(
                        ActionFilters.user(UUID.fromString("725d585e-4ff1-4f18-acca-6ac538364080")),
                        // {"$and": [{"target.type": "USER"}, {"target.uniqueId": {"$binary": {"base64": "cl1YXk/xTxisymrFODZAgA==", "subType": "04"}}}]}
                        "{\"$and\": [{\"target.type\": \"USER\"}, {\"target.uniqueId\": {\"$binary\": {\"base64\": \"cl1YXk/xTxisymrFODZAgA==\", \"subType\": \"04\"}}}]}"
                ),
                Arguments.of(
                        ActionFilters.group("test"),
                        // {"$and": [{"target.type": "GROUP"}, {"target.name": "test"}]}
                        "{\"$and\": [{\"target.type\": \"GROUP\"}, {\"target.name\": \"test\"}]}"
                ),
                Arguments.of(
                        ActionFilters.track("test"),
                        // {"$and": [{"target.type": "TRACK"}, {"target.name": "test"}]}
                        "{\"$and\": [{\"target.type\": \"TRACK\"}, {\"target.name\": \"test\"}]}"
                ),
                Arguments.of(
                        ActionFilters.search("test"),
                        // {"$or": [{"source.name": {"$regularExpression": {"pattern": ".*test.*", "options": "i"}}}, {"target.name": {"$regularExpression": {"pattern": ".*test.*", "options": "i"}}}, {"description": {"$regularExpression": {"pattern": ".*test.*", "options": "i"}}}]}
                        "{\"$or\": [{\"source.name\": {\"$regularExpression\": {\"pattern\": \".*test.*\", \"options\": \"i\"}}}, {\"target.name\": {\"$regularExpression\": {\"pattern\": \".*test.*\", \"options\": \"i\"}}}, {\"description\": {\"$regularExpression\": {\"pattern\": \".*test.*\", \"options\": \"i\"}}}]}"
                )
        );
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource
    public void testQueries(FilterList<Action> filters, String expectedQuery) {
        Bson bson = ActionFilterMongoBuilder.INSTANCE.make(filters);

        CodecRegistry codec = CodecRegistries.withUuidRepresentation(Bson.DEFAULT_CODEC_REGISTRY, UuidRepresentation.STANDARD);
        String json = bson.toBsonDocument(BsonDocument.class, codec).toJson();

        assertEquals(expectedQuery, json);
    }

}
