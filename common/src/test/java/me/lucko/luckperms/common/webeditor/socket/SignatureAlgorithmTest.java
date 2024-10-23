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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.security.KeyPair;
import java.security.PublicKey;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class SignatureAlgorithmTest {

    @ParameterizedTest
    @EnumSource
    public void testKeypairGenerate(SignatureAlgorithm algorithm) {
        algorithm.generateKeyPair();
    }

    @ParameterizedTest
    @EnumSource
    public void testSignVerify(SignatureAlgorithm algorithm) {
        KeyPair keyPair = algorithm.generateKeyPair();

        String signature = algorithm.sign(keyPair.getPrivate(), "test");
        assertTrue(algorithm.verify(keyPair.getPublic(), "test", signature));

        assertFalse(algorithm.verify(keyPair.getPublic(), "test", "bleh"));
        assertFalse(algorithm.verify(keyPair.getPublic(), "test", ""));
        assertFalse(algorithm.verify(keyPair.getPublic(), "test", null));
    }

    @Test
    public void testParseAndVerifyRSA() {
        // the base64 values are generated from javascript crypto.subtle
        PublicKey publicKey = SignatureAlgorithm.V1_RSA.parsePublicKey("MIICIjANBgkqhkiG9w0BAQEFAAOCAg8AMIICCgKCAgEA+Fhv9SmQIENpseq81/BCmGJ8Pf94X4yNdsrNaZ0uNGasUlIM/+aRrSA8X586BEGc7qZeVidegW4yM3LufaDnkoAyIQij7IfHzO09H3VdbLWF+RQY/dWj/cd6O2QkrjMXsW+LKkeKAsY2KTzxoyR9vLcTAP229mQPxiFWWidMAVeU3EzHWtV74UAuycG1Ja3kRtS031lugJJKAlgXUVlAF8tI+aoTQljpndptrhRBPtnUxtCCxj8jFM5houD5010zXIAzsAjg2NPHl/R/qypfHFMWYlcCGIbKMN61gM6lRyglLC+2dxSBw7b+GHTGHwoK3UMhqyonlRAP4W+UpA/tT/LazXHXalYOz8IYcnQgb4Np7pw2TFY2HA5sR4ZfCTnE1bemlWMHbjBc5CAnb7KyZVpFsxPLvcuSLaM4t3CyXBSwDJTMNj9aLSYg6FNAwnEcskRdgkrf23+1E30CaOsIKv4Um7SlnB+6qnxmRWpcs4rWPuS7IJXemaYks+gkgZr+Wt6ITPx8NRbyO1eLwsOOyN6g6DcZwc/2MTl1ItbP0+jAvE2NIU1KU0+uuyobZh1cldDGfaboshh9Ni9D4SSWzugPN9Ohs0QueEo3qi1Z6Jv9Jx2Bx1QlIV7FNEGpU6kDknRejewm+qzl5m0fxnfNH46x2FneUisqTea9Vo9suN8CAwEAAQ==");
        assertTrue(SignatureAlgorithm.V1_RSA.verify(publicKey, "hello world", "Z9XU+AIcHF7Y96grX/NLuNN2fI3nmuXfFss1QbTg80j3Jh8jZRMyFRfWz7rc1OToEsrAQXFY426nxN7JdXTTSvw4kErIn6amvTJBqEqWB+rA1FKJqnsXbl3gIG8UqwqBfMlYh5tYhBddsKVc3jW+4kPPGBgUfxgneHcpocgrwi3aX1vqvxJ4y49M1hs0hFH1VnO1VXcffQWnZRnEuUccYH61DHZHiFyWfo2SF6wdNMJG51idUBgZY7zyMnLRzL+07N9MrDJHkc9J4O5HRDuvVefoRNcvW/tpeVDMsLynP3psmyt33euds6LkdVtExolngepKAuGE9JBtnjFEWFakQ+INhvHZ7P4jGiLKRf7kDdckLqJxsH25w6MYzsq4jHTVbrzKehUAx0nnWhL3QrSLTwvly0WHTd4yd/rTVM2JUb+z5FPzuVQP6VgQmrwXYAhA6swkE/1poBWOgsCIe7rvHn3PYvU1D66fXe4lHbyQMAmAu39GLu3RpnmeXuiUT/yygMqvb1Rr8hTeOkjxQOuus+70ybkC21nVCigRFpI4ktvILe09F8jkL6VFHYtz6fKqXKJBTT2gIKijFCJeqCgkCxNnoLeOU+hsS3pZQUwuZS7l0Eyax1eKyelOv6zg10j2ido7/55dE3U0OZGOQ6VWRq8NiPuURId5NzFGURkDda8="));
    }

    @Test
    public void testParseAndVerifyECDSA() {
        // the base64 values are generated from javascript crypto.subtle
        PublicKey publicKey = SignatureAlgorithm.V2_ECDSA.parsePublicKey("MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAEkF5EWzdsbmVOYprtfMleBZYASm7AXBQQCE29xR2hpGkjVi4Fra/KPazRShqyGvQXY24sINsxIPEd4XamDfFAaQ==");
        assertTrue(SignatureAlgorithm.V2_ECDSA.verify(publicKey, "hello world", "XAZJMxOlR5Mcq7nJxU4oS1fYyViYH1FZxWOXwOC+LRXYF8KeP58k5KLTjc35L974t3RukwAqflul0HY64bJT3w=="));
    }

}
