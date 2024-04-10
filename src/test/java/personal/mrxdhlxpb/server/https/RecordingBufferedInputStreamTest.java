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
import personal.mrxdhlxpb.server.https.test.extension.RandomByteArray;
import personal.mrxdhlxpb.server.https.test.extension.RandomByteArrayParameterResolver;
import personal.mrxdhlxpb.server.https.test.TestConstants;
import personal.mrxdhlxpb.server.https.test.extension.RandomInt;
import personal.mrxdhlxpb.server.https.test.extension.RandomIntParameterResolver;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests most of the interfaces provided by {@link RecordingBufferedInputStream}.
 *
 * @author mrxdhlxpb
 */
@ExtendWith(RandomIntParameterResolver.class)
@ExtendWith(RandomByteArrayParameterResolver.class)
public class RecordingBufferedInputStreamTest {

    private RecordingBufferedInputStream instance;
    
    @BeforeEach
    void initialize(RepetitionInfo repetitionInfo, TestReporter testReporter) throws Exception {
        instance = new RecordingBufferedInputStream(new FileInputStream(TestConstants.TEST_FILE_1),
                (repetitionInfo.getCurrentRepetition() & 1) == 1);
        testReporter.publishEntry("instance.recordingEnabled", Boolean.toString(instance
                .isRecordingEnabled()));
    }

    @AfterEach
    void release() throws Exception {
        instance.close();
    }

    private void testRecordingCapabilityIfEnabled(byte[] actualBytesRead) {
        if (!instance.isRecordingEnabled())
            return;
        assertArrayEquals(actualBytesRead, instance.getRecordingByteArray());
        // test reset recording
        instance.resetRecording();
        assertArrayEquals(new byte[0], instance.getRecordingByteArray());
    }

    // methods in RecordingBufferedInputStream

    @RepeatedTest(2)
    void read0arg() throws Exception {
        byte data = (byte) instance.read();
        assertEquals(TestConstants.TEST_FILE_1_BYTES_FIRST_ELEMENT, data);

        testRecordingCapabilityIfEnabled(new byte[]{data});
    }

    @RepeatedTest(2)
    void read3arg() throws Exception {
        byte[] b = new byte[100];
        int result = instance.read(b, 0, 10);
        assertArrayEquals(TestConstants.TEST_FILE_1_BYTES_FIRST_TEN_ELEMENTS, Arrays.copyOf(b, 10));
        assertEquals(10, result);

        testRecordingCapabilityIfEnabled(Arrays.copyOf(b, 10));
    }

    // methods in BufferedInputStream

    @RepeatedTest(2)
    void available() throws Exception {
        assertEquals(TestConstants.TEST_FILE_1_BYTES.length, instance.available());
    }

    @Disabled
    @RepeatedTest(2)
    void close() { throw new IllegalStateException("this method does not need to be tested"); }

    /**
     * tests both mark and reset
     */
    @RepeatedTest(2)
    void markReset() throws Exception {
        instance.mark(9);
        assertEquals(9, instance.skip(9));
        byte data1 = (byte) instance.read();
        assertEquals(TestConstants.TEST_FILE_1_BYTES_FIRST_TEN_ELEMENTS[9], data1);
        instance.reset();
        byte data2 = (byte) instance.read();
        assertEquals(TestConstants.TEST_FILE_1_BYTES_FIRST_ELEMENT, data2);

        testRecordingCapabilityIfEnabled(new byte[]{data1, data2});
    }

    @RepeatedTest(2)
    void markSupported() {
        assertTrue(instance.markSupported());

        testRecordingCapabilityIfEnabled(new byte[0]);
    }

    @RepeatedTest(2)
    void skip() throws Exception {
        assertEquals(5, instance.skip(5));
        byte data = (byte) instance.read();
        assertEquals(TestConstants.TEST_FILE_1_BYTES_FIRST_TEN_ELEMENTS[5], data);

        testRecordingCapabilityIfEnabled(new byte[]{data});
    }

    @RepeatedTest(2)
    void transferTo() throws Exception {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        instance.transferTo(byteArrayOutputStream);
        assertArrayEquals(TestConstants.TEST_FILE_1_BYTES, byteArrayOutputStream.toByteArray());

        testRecordingCapabilityIfEnabled(byteArrayOutputStream.toByteArray());
    }


    // methods in FilterInputStream

    @RepeatedTest(2)
    void read1arg() throws Exception {
        byte[] b = new byte[TestConstants.TEST_FILE_1_BYTES.length];
        int result = instance.read(b);
        assertArrayEquals(TestConstants.TEST_FILE_1_BYTES, b);
        assertEquals(TestConstants.TEST_FILE_1_BYTES.length, result);

        testRecordingCapabilityIfEnabled(b);
    }

    // methods in InputStream

    @Disabled
    @RepeatedTest(2)
    void nullInputStream() { throw new IllegalStateException("We do not test this method."); }

    @RepeatedTest(16)
    void readNBytes1arg(
            @RandomInt(
                    min = 0,
                    max = Integer.MAX_VALUE,
                    fixed = {0, 1, 2, 3, 40, 500, 6000, Integer.MAX_VALUE}
            )
            int len
    ) throws Exception {
        byte[] data = instance.readNBytes(len);
        assertArrayEquals(Arrays.copyOf(TestConstants.TEST_FILE_1_BYTES, data.length), data);
        testRecordingCapabilityIfEnabled(data);
    }

    @RepeatedTest(8)
    void readNBytes3arg(
            @RandomByteArray(minLen = 16, maxLen = 24)
            byte[] b,

            @RandomInt(min = 0, max = 8)
            int off,

            @RandomInt(min = 0, max = 8)
            int len
    ) throws Exception {
        int returnValue = instance.readNBytes(b, off, len);
        assertArrayEquals(Arrays.copyOf(TestConstants.TEST_FILE_1_BYTES, returnValue),
                Arrays.copyOfRange(b, off, off + returnValue));

       testRecordingCapabilityIfEnabled(Arrays.copyOfRange(b, off, off + returnValue));
    }


    @RepeatedTest(16)
    void skipNBytes(
            @RandomInt(
                    min = 0,
                    max = TestConstants.TEST_FILE_1_BYTES_LENGTH - 1,
                    fixed = {0, TestConstants.TEST_FILE_1_BYTES_LENGTH - 1}
            )
            long n
    ) throws Exception {
        instance.skipNBytes(n);
        byte data = (byte) instance.read();
        assertEquals(TestConstants.TEST_FILE_1_BYTES[(int) n], data);
        
        testRecordingCapabilityIfEnabled(new byte[]{data});
    }

    // methods in Object are ignored

}
