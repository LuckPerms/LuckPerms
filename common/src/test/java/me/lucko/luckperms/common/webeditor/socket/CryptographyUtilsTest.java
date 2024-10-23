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

package me.lucko.luckperms.common.webeditor.socket;

import org.junit.jupiter.api.Test;

import java.security.KeyPair;
import java.security.PublicKey;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class CryptographyUtilsTest {

    @Test
    public void testKeypairGenerate() {
        CryptographyUtils.generateKeyPair();
    }

    @Test
    public void testSignVerify() {
        KeyPair keyPair = CryptographyUtils.generateKeyPair();

        String signature = CryptographyUtils.sign(keyPair.getPrivate(), "test");
        assertTrue(CryptographyUtils.verify(keyPair.getPublic(), "test", signature));

        assertFalse(CryptographyUtils.verify(keyPair.getPublic(), "test", "bleh"));
        assertFalse(CryptographyUtils.verify(keyPair.getPublic(), "test", ""));
        assertFalse(CryptographyUtils.verify(keyPair.getPublic(), "test", null));
    }

    @Test
    public void testParseAndVerify() {
        // the base64 values are generated from javascript crypto.subtle
        PublicKey publicKey = CryptographyUtils.parsePublicKey("MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAEkF5EWzdsbmVOYprtfMleBZYASm7AXBQQCE29xR2hpGkjVi4Fra/KPazRShqyGvQXY24sINsxIPEd4XamDfFAaQ==");
        assertTrue(CryptographyUtils.verify(publicKey, "hello world", "XAZJMxOlR5Mcq7nJxU4oS1fYyViYH1FZxWOXwOC+LRXYF8KeP58k5KLTjc35L974t3RukwAqflul0HY64bJT3w=="));
    }

}
