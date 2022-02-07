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

import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

/**
 * Utilities for public/private key crypto used by the web editor socket connection.
 */
public final class CryptographyUtils {
    private CryptographyUtils() {}

    /**
     * Parse a public key from the given string.
     *
     * @param base64String a base64 string encoding the public key
     * @return the parsed public key
     * @throws IllegalArgumentException if the input was invalid
     */
    public static PublicKey parsePublicKey(String base64String) throws IllegalArgumentException {
        try {
            byte[] bytes = Base64.getDecoder().decode(base64String);
            X509EncodedKeySpec spec = new X509EncodedKeySpec(bytes);
            KeyFactory rsa = KeyFactory.getInstance("RSA");
            return rsa.generatePublic(spec);
        } catch (Exception e) {
            throw new IllegalArgumentException("Exception parsing public key", e);
        }
    }

    /**
     * Generate a public/private key pair.
     *
     * @return the generated key pair
     */
    public static KeyPair generateKeyPair() {
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(4096);
            return generator.generateKeyPair();
        } catch (Exception e) {
            throw new RuntimeException("Exception generating keypair", e);
        }
    }

    /**
     * Signs {@code msg} using the given {@link PrivateKey}.
     *
     * @param privateKey the private key to sign with
     * @param msg the message
     * @return a base64 string containing the signature
     */
    public static String sign(PrivateKey privateKey, String msg) {
        try {
            Signature sign = Signature.getInstance("SHA256withRSA");
            sign.initSign(privateKey);
            sign.update(msg.getBytes(StandardCharsets.UTF_8));

            return Base64.getEncoder().encodeToString(sign.sign());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Verify that the given base64 encoded signature matches
     * the given message and {@link PublicKey}.
     *
     * @param publicKey the public key that the message was supposedly signed with
     * @param msg the message
     * @param signatureBase64 the provided signature
     * @return true if the signature is ok
     */
    public static boolean verify(PublicKey publicKey, String msg, String signatureBase64) {
        try {
            Signature sign = Signature.getInstance("SHA256withRSA");
            sign.initVerify(publicKey);
            sign.update(msg.getBytes(StandardCharsets.UTF_8));

            byte[] signatureBytes = Base64.getDecoder().decode(signatureBase64);
            return sign.verify(signatureBytes);
        } catch (Exception e) {
            return false;
        }
    }

}
