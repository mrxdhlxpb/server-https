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
import org.junit.jupiter.api.Test;
import personal.mrxdhlxpb.server.https.Fields;
import personal.mrxdhlxpb.server.https.HttpRequestInputStream;
import personal.mrxdhlxpb.server.https.test.TestConstants;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;

/**
 * @author mrxdhlxpb
 */
public class ChunkedTransferCodingDecoderTest {

    private static final byte[] TEST_FILE_BYTES = TestConstants.TEST_FILE_1_BYTES;
    private static final int TEST_FILE_BYTES_LENGTH = TestConstants.TEST_FILE_1_BYTES_LENGTH;
    private static final File CHUNKED_TEST_FILE = TestConstants.CHUNKED_TEST_FILE_1;
    private static final Fields TRAILER_FIELDS = TestConstants.CHUNKED_TEST_FILE_1_TRAILER_FIELDS;

    @Test
    void test() throws Exception {
        final ChunkedTransferCodingDecoder instance = new ChunkedTransferCodingDecoder(
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
            Assertions.assertEquals(
                    TRAILER_FIELDS,
                    instance.getTrailerFieldsDirectly()
            );
        }
    }

}
