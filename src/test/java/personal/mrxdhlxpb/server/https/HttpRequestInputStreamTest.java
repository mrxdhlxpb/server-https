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

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.opentest4j.AssertionFailedError;
import personal.mrxdhlxpb.server.https.error.concrete.client.BadRequestException;
import personal.mrxdhlxpb.server.https.test.TestConstants;
import personal.mrxdhlxpb.server.https.test.extension.RandomByteArrayParameterResolver;
import personal.mrxdhlxpb.server.https.test.extension.RandomIntParameterResolver;

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author mrxdhlxpb
 */
@ExtendWith({RandomIntParameterResolver.class, RandomByteArrayParameterResolver.class})
public class HttpRequestInputStreamTest {

    private static final byte CR = 0xd;

    private static final byte LF = 0xa;

    private static final byte SP = 0x20;

    private HttpRequestInputStream createHttpRequestInputStreamWithDataSource(
            byte[] dataSource,
            boolean isRecordingEnabled) {
        return new HttpRequestInputStream(new ByteArrayInputStream(dataSource), isRecordingEnabled);
    }

    private HttpRequestInputStream createHttpRequestInputStreamWithDataSource(byte[] dataSource) {
        return createHttpRequestInputStreamWithDataSource(dataSource, false);
    }

    private void assertReadLine(byte[] dataSource,
                                byte[] givenByteArray,
                                int expectedReturnValue,
                                byte[] expectedByteArray) throws Exception {
        try (var httpRequestInputStream = createHttpRequestInputStreamWithDataSource(dataSource)) {
            int actualReturnValue = httpRequestInputStream.readLine(givenByteArray);
            assertEquals(expectedReturnValue, actualReturnValue);
            assertArrayEquals(expectedByteArray, givenByteArray);
        }
    }

    private <T extends Exception> void assertReadLine(byte[] dataSource,
                                                      byte[] givenByteArray,
                                                      Class<T> expectedExceptionType)
            throws Exception {
        try (var httpRequestInputStream = createHttpRequestInputStreamWithDataSource(dataSource)) {
            assertThrowsExactly(expectedExceptionType,
                    () -> httpRequestInputStream.readLine(givenByteArray));
        }
    }

    private byte[] emptyByteArrayWithLength(int length) { return new byte[length]; }

    @RepeatedTest(8)
    void readLine() throws Exception {
        // return when CRLF is met
        assertReadLine(
                new byte[]{0x79, 0x78, 0x76, CR, LF},
                emptyByteArrayWithLength(3),
                3,
                new byte[]{0x79, 0x78, 0x76}
        );
        assertReadLine(
                new byte[]{0x79, 0x78, 0x76, CR, LF},
                emptyByteArrayWithLength(30), // longer array
                3,
                Arrays.copyOf(new byte[]{0x79, 0x78, 0x76}, 30)
        );

        // return when end of stream is detected
        assertReadLine(
                new byte[]{0x75, 0x74, 0x73},
                emptyByteArrayWithLength(3),
                3,
                new byte[]{0x75, 0x74, 0x73}
        );
        assertReadLine(
                new byte[]{0x75, 0x74, 0x73},
                emptyByteArrayWithLength(30), // longer array
                3,
                Arrays.copyOf(new byte[]{0x75, 0x74, 0x73}, 30)
        );
        assertReadLine(
                new byte[]{0x75, 0x74, CR}, // trailing CR
                emptyByteArrayWithLength(3),
                3,
                new byte[]{0x75, 0x74, SP}
        );
        assertReadLine(
                new byte[]{0x75, 0x74, LF}, // trailing LF
                emptyByteArrayWithLength(3),
                3,
                new byte[]{0x75, 0x74, LF}
        );
        assertReadLine(
                new byte[]{0x75, LF, CR}, // trailing LF, CR
                emptyByteArrayWithLength(3),
                3,
                new byte[]{0x75, LF, SP}
        );

        // return when CannotContainException is thrown
        assertReadLine(
                new byte[]{0x72, 0x71, 0x70},
                emptyByteArrayWithLength(2),
                HttpRequestInputStream.CannotContainException.class
        );
        assertReadLine(
                new byte[]{0x72, 0x71, 0x70, CR, LF}, // trailing CR, LF
                emptyByteArrayWithLength(2),
                HttpRequestInputStream.CannotContainException.class
        );

        // test bare CR
        assertReadLine(
                new byte[]{0x69, CR, 0x67},
                emptyByteArrayWithLength(3),
                3,
                new byte[]{0x69, SP, 0x67}
        );

    }

    @Nested
    class ReadFieldsTest {

        private HttpRequestInputStream ins;

        @BeforeEach
        void initialize() throws Exception {
            ins = new HttpRequestInputStream(new FileInputStream(TestConstants.TEST_FILE_2), false);
        }

        @AfterEach
        void release() throws Exception {
            ins.close();
        }

        @RepeatedTest(1)
        void readFieldsTest0() throws Exception {
            Fields fields = ins.readFields(
                    TestConstants.TEST_FILE_2_LONGEST_FILED_LINE_LENGTH,
                    TestConstants.TEST_FILE_2_SECTION_SIZE);

            assertEquals(TestConstants.TEST_FILE_2_FIELDS, fields);
        }

        // BadRequestException expected thrown if length of one of field lines/entire section
        // exceeds limit

        @RepeatedTest(1)
        void readFieldsTest1() {
            assertThrowsExactly(BadRequestException.class, () -> ins.readFields(
                    TestConstants.TEST_FILE_2_LONGEST_FILED_LINE_LENGTH - 1,
                    TestConstants.TEST_FILE_2_SECTION_SIZE));

        }

        @RepeatedTest(1)
        void readFieldsTest2() {
            assertThrowsExactly(BadRequestException.class, () -> ins.readFields(
                    TestConstants.TEST_FILE_2_LONGEST_FILED_LINE_LENGTH,
                    TestConstants.TEST_FILE_2_SECTION_SIZE - 1));
        }
    }

    @Test
    void testRequireCRLF() throws Exception {
        try (final HttpRequestInputStream httpRequestInputStream =
                     createHttpRequestInputStreamWithDataSource(new byte[]{CR, LF})) {
            assertDoesNotThrow(() -> httpRequestInputStream.requireCRLF(AssertionFailedError::new));
        }

        try (final HttpRequestInputStream httpRequestInputStream =
                     createHttpRequestInputStreamWithDataSource(new byte[]{CR, LF, CR, LF})) {
            assertDoesNotThrow(() -> httpRequestInputStream.requireCRLF(AssertionFailedError::new));
        }

        try (final HttpRequestInputStream httpRequestInputStream =
                     createHttpRequestInputStreamWithDataSource(new byte[]{CR, LF, SP, SP})) {
            assertDoesNotThrow(() -> httpRequestInputStream.requireCRLF(AssertionFailedError::new));
        }


        try (final HttpRequestInputStream httpRequestInputStream =
                     createHttpRequestInputStreamWithDataSource(new byte[]{CR})) {
            assertThrowsExactly(AssertionFailedError.class,
                    () -> httpRequestInputStream.requireCRLF(AssertionFailedError::new));
        }

        try (final HttpRequestInputStream httpRequestInputStream =
                     createHttpRequestInputStreamWithDataSource(new byte[]{LF})) {
            assertThrowsExactly(AssertionFailedError.class,
                    () -> httpRequestInputStream.requireCRLF(AssertionFailedError::new));
        }

        try (final HttpRequestInputStream httpRequestInputStream =
                     createHttpRequestInputStreamWithDataSource(new byte[]{LF, CR})) {
            assertThrowsExactly(AssertionFailedError.class,
                    () -> httpRequestInputStream.requireCRLF(AssertionFailedError::new));
        }

        try (final HttpRequestInputStream httpRequestInputStream =
                     createHttpRequestInputStreamWithDataSource(new byte[]{CR, CR})) {
            assertThrowsExactly(AssertionFailedError.class,
                    () -> httpRequestInputStream.requireCRLF(AssertionFailedError::new));
        }

        try (final HttpRequestInputStream httpRequestInputStream =
                     createHttpRequestInputStreamWithDataSource(new byte[]{LF, LF})) {
            assertThrowsExactly(AssertionFailedError.class,
                    () -> httpRequestInputStream.requireCRLF(AssertionFailedError::new));
        }

        try (final HttpRequestInputStream httpRequestInputStream =
                     createHttpRequestInputStreamWithDataSource(new byte[]{CR, SP})) {
            assertThrowsExactly(AssertionFailedError.class,
                    () -> httpRequestInputStream.requireCRLF(AssertionFailedError::new));
        }
    }

}
