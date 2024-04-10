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
import org.junit.jupiter.api.extension.ExtendWith;
import personal.mrxdhlxpb.server.https.test.extension.RandomByteArray;
import personal.mrxdhlxpb.server.https.test.extension.RandomByteArrayParameterResolver;

import java.io.ByteArrayInputStream;

/**
 * @author mrxdhlxpb
 */
@ExtendWith(RandomByteArrayParameterResolver.class)
public class ChainDecoderTest {

    @Test
    void test(@RandomByteArray(len = 1024) byte[] byteArray) throws Exception {
        final ChainDecoder instance = new ChainDecoder(
                new NullDecoder(),
                new NullDecoder(),
                new NullDecoder(),
                new NullDecoder(),
                new NullDecoder(),
                new NullDecoder());

        final ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(byteArray);

        try (var decodedInputStream = instance
                .decode(byteArrayInputStream)
                .getDecodedInputStream()) {
            Assertions.assertEquals(1024, instance.getDecodedLength());

            Assertions.assertArrayEquals(
                    byteArray,
                    decodedInputStream.readAllBytes()
            );
        }
    }

}
