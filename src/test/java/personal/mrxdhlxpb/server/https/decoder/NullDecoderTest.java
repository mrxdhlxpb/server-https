/*
 *  Copyright (C) 2024 mrxdhlxpb
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package personal.mrxdhlxpb.server.https.decoder;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import personal.mrxdhlxpb.server.https.HttpRequestInputStream;
import personal.mrxdhlxpb.server.https.test.TestConstants;
import personal.mrxdhlxpb.server.https.test.extension.RandomInt;
import personal.mrxdhlxpb.server.https.test.extension.RandomIntParameterResolver;

import java.io.File;
import java.io.FileInputStream;
import java.util.Arrays;
import java.util.Optional;

/**
 * @author mrxdhlxpb
 */
@ExtendWith(RandomIntParameterResolver.class)
public class NullDecoderTest {

    private static final File TEST_FILE = TestConstants.TEST_FILE_1;
    private static final int TEST_FILE_BYTES_LENGTH = TestConstants.TEST_FILE_1_BYTES_LENGTH;
    private static final byte[] TEST_FILE_BYTES = TestConstants.TEST_FILE_1_BYTES;

    @Test
    void test0() throws Exception {
        final NullDecoder instance = new NullDecoder();

        try (var in = new HttpRequestInputStream(new FileInputStream(TEST_FILE));
             var contentInputStream = instance.decode(in).getContentInputStream()) {
            Assertions.assertEquals(Optional.empty(), instance.getTrailerFields());
            Assertions.assertEquals(TEST_FILE_BYTES_LENGTH, instance.getContentLength());
            Assertions.assertArrayEquals(TEST_FILE_BYTES,
                    contentInputStream.readAllBytes());
        }
    }

    @RepeatedTest(3)
    void test1(@RandomInt(min = 1, max = 10) int i) throws Exception {
        final NullDecoder[] nullDecoders = new NullDecoder[i];
        Arrays.fill(nullDecoders, new NullDecoder());

        final ChainDecoder chainDecoder = new ChainDecoder(nullDecoders);

        try (var in = new HttpRequestInputStream(new FileInputStream(TEST_FILE));
             var decodedInputStream = chainDecoder.decode(in).getDecodedInputStream()) {
            Assertions.assertEquals(
                    TEST_FILE_BYTES_LENGTH,
                    chainDecoder.getDecodedLength()
            );
            Assertions.assertArrayEquals(
                    TEST_FILE_BYTES,
                    decodedInputStream.readAllBytes()
            );
        }
    }

}
