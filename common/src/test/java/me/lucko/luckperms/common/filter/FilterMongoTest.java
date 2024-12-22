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

package me.lucko.luckperms.common.filter;

import me.lucko.luckperms.common.filter.mongo.FilterMongoBuilder;
import org.bson.BsonDocument;
import org.bson.UuidRepresentation;
import org.bson.codecs.configuration.CodecRegistries;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.conversions.Bson;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Locale;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class FilterMongoTest {

    private static Stream<Arguments> testQueries() {
        return Stream.of(
                Arguments.of(
                        FilterList.empty(),
                        // {}
                        "{}"
                ),
                Arguments.of(
                        FilterList.and(
                                TestField.FOO.isEqualTo("hello", ConstraintFactory.STRINGS)
                        ),
                        // {"$and": [{"foo": "hello"}]}
                        "{\"$and\": [{\"foo\": \"hello\"}]}"
                ),
                Arguments.of(
                        FilterList.and(
                                TestField.FOO.isEqualTo("hello", ConstraintFactory.STRINGS),
                                TestField.BAR.isEqualTo("world", ConstraintFactory.STRINGS)
                        ),
                        // {"$and": [{"foo": "hello"}, {"bar": "world"}]}
                        "{\"$and\": [{\"foo\": \"hello\"}, {\"bar\": \"world\"}]}"
                ),
                Arguments.of(
                        FilterList.or(
                                TestField.FOO.isEqualTo("hello", ConstraintFactory.STRINGS)
                        ),
                        // {"$or": [{"foo": "hello"}]}
                        "{\"$or\": [{\"foo\": \"hello\"}]}"
                ),
                Arguments.of(
                        FilterList.or(
                                TestField.FOO.isEqualTo("hello", ConstraintFactory.STRINGS),
                                TestField.BAR.isEqualTo("world", ConstraintFactory.STRINGS)
                        ),
                        // {"$or": [{"foo": "hello"}, {"bar": "world"}]}
                        "{\"$or\": [{\"foo\": \"hello\"}, {\"bar\": \"world\"}]}"
                ),
                Arguments.of(
                        FilterList.or(
                                TestField.FOO.isEqualTo("hello", ConstraintFactory.STRINGS),
                                TestField.BAR.isNotEqualTo("world", ConstraintFactory.STRINGS),
                                TestField.BAZ.isSimilarTo("abc%xyz", ConstraintFactory.STRINGS),
                                TestField.BAZ.isNotSimilarTo("a_c", ConstraintFactory.STRINGS)
                        ),
                        // {"$or": [
                        //   {"foo": "hello"},
                        //   {"bar": {"$ne": "world"}},
                        //   {"baz": {"$regularExpression": {"pattern": "abc.*xyz", "options": "i"}}},
                        //   {"baz": {"$not": {"$regularExpression": {"pattern": "a.c", "options": "i"}}}}
                        // ]}
                        "{\"$or\": [{\"foo\": \"hello\"}, {\"bar\": {\"$ne\": \"world\"}}, {\"baz\": {\"$regularExpression\": {\"pattern\": \"abc.*xyz\", \"options\": \"i\"}}}, {\"baz\": {\"$not\": {\"$regularExpression\": {\"pattern\": \"a.c\", \"options\": \"i\"}}}}]}"
                )
        );
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource
    public void testQueries(FilterList<Object> filters, String expectedQuery) {
        Bson bson = new TestFilterMongoBuilder().make(filters);

        CodecRegistry codec = CodecRegistries.withUuidRepresentation(Bson.DEFAULT_CODEC_REGISTRY, UuidRepresentation.STANDARD);
        String json = bson.toBsonDocument(BsonDocument.class, codec).toJson();

        assertEquals(expectedQuery, json);
    }

    private enum TestField implements FilterField<Object, String> {
        FOO, BAR, BAZ;

        @Override
        public String getValue(Object object) {
            return "null";
        }

        @Override
        public String toString() {
            return name().toLowerCase(Locale.ROOT);
        }
    }

    private static final class TestFilterMongoBuilder extends FilterMongoBuilder<Object> {

        @Override
        public String mapFieldName(FilterField<Object, ?> field) {
            return field.toString();
        }
    }

}
