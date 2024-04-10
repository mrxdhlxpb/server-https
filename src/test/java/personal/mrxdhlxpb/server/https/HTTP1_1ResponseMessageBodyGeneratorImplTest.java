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
package personal.mrxdhlxpb.server.https;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import personal.mrxdhlxpb.server.https.error.concrete.server.InternalServerErrorException;
import personal.mrxdhlxpb.server.https.test.TestConstants;

import java.io.*;

/**
 * @author mrxdhlxpb
 */
public class HTTP1_1ResponseMessageBodyGeneratorImplTest {

    private static final File TEST_FILE = TestConstants.TEST_FILE_1;
    private static final byte[] FIRST_TEN_BYTES =
            TestConstants.TEST_FILE_1_BYTES_FIRST_TEN_ELEMENTS;
    private static final byte[] ALL_BYTES = TestConstants.TEST_FILE_1_BYTES;
    private static final int LENGTH = TestConstants.TEST_FILE_1_BYTES_LENGTH;

    private HTTP1_1ResponseMessageBodyGeneratorImpl instance;
    private InputStream testContentInputStream;
    private ByteArrayOutputStream testDestination;

    @BeforeEach
    void initialize() throws Exception {
        instance = new HTTP1_1ResponseMessageBodyGeneratorImpl();
        testContentInputStream = new BufferedInputStream(new FileInputStream(TEST_FILE));
        testDestination = new ByteArrayOutputStream();
    }

    @AfterEach
    void release() throws Exception {
        testContentInputStream.close();
        testDestination.close();
    }

    @Test
    void testGetTransferEncodingChain() {
        Assertions.assertArrayEquals(new String[0], instance.getTransferEncodingChain());
    }

    @Test
    void testGenerateResponseMessageBody0() throws Exception {
        instance.generateResponseMessageBody(
                testContentInputStream,
                10,
                null,
                testDestination
        );
        Assertions.assertArrayEquals(FIRST_TEN_BYTES, testDestination.toByteArray());
    }


    @Test
    void testGenerateResponseMessageBody1() throws Exception {
        instance.generateResponseMessageBody(
                testContentInputStream,
                LENGTH,
                null,
                testDestination
        );
        Assertions.assertArrayEquals(ALL_BYTES, testDestination.toByteArray());
    }

    @Test
    void testGenerateResponseMessageBody2() throws Exception {
        instance.generateResponseMessageBody(
                testContentInputStream,
                Integer.MIN_VALUE,
                null,
                testDestination
        );
        Assertions.assertArrayEquals(ALL_BYTES, testDestination.toByteArray());
    }

    @Test
    void testGenerateResponseMessageBody3() throws Exception {
        instance.generateResponseMessageBody(
                testContentInputStream,
                0,
                null,
                testDestination
        );
        Assertions.assertArrayEquals(new byte[0], testDestination.toByteArray());
    }

    @Test
    void testGenerateResponseMessageBody4() {
        Assertions.assertThrowsExactly(InternalServerErrorException.class,
                () -> instance.generateResponseMessageBody(testContentInputStream, LENGTH + 1,
                        null, testDestination));
    }

}
