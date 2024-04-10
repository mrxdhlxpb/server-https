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
import org.junit.jupiter.api.extension.ExtendWith;
import personal.mrxdhlxpb.server.https.HttpRequestInputStream;
import personal.mrxdhlxpb.server.https.test.TestConstants;
import personal.mrxdhlxpb.server.https.test.extension.RandomInt;
import personal.mrxdhlxpb.server.https.test.extension.RandomIntParameterResolver;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Arrays;

/**
 * @author mrxdhlxpb
 */
@ExtendWith(RandomIntParameterResolver.class)
public class ChainTransferCodingDecoderTest {

    private static final byte[] TEST_FILE_BYTES = TestConstants.TEST_FILE_1_BYTES;
    private static final int TEST_FILE_BYTES_LENGTH = TestConstants.TEST_FILE_1_BYTES_LENGTH;
    private static final File CHUNKED_TEST_FILE = TestConstants.CHUNKED_TEST_FILE_1;

    @RepeatedTest(5)
    void test(@RandomInt(min = 0, max = 10, fixed = {0, 1, 2, 10}) int i) throws Exception {
        final NullDecoder[] nullDecoders = new NullDecoder[i];
        Arrays.fill(nullDecoders, new NullDecoder());

        final ChunkedTransferCodingDecoder chunkedTransferCodingDecoder =
                new ChunkedTransferCodingDecoder(
                        1024,
                        1048576,
                        10,
                        100,
                        100,
                        1000,
                        getClass().getCanonicalName(),
                        null,
                        null
                );

        final ChainTransferCodingDecoder instance =
                new ChainTransferCodingDecoder(chunkedTransferCodingDecoder, nullDecoders);

        try (final HttpRequestInputStream httpRequestInputStream = new HttpRequestInputStream(new
                FileInputStream(CHUNKED_TEST_FILE));
             final InputStream contentInputStream = instance
                     .decode(httpRequestInputStream)
                     .getContentInputStream()) {
            Assertions.assertEquals(
                    TEST_FILE_BYTES_LENGTH,
                    instance.getContentLength()
            );
            Assertions.assertArrayEquals(
                    TEST_FILE_BYTES,
                    contentInputStream.readAllBytes()
            );
        }
    }

}
