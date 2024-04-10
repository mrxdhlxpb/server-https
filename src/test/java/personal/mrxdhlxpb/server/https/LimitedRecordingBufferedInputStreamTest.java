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
import personal.mrxdhlxpb.server.https.test.TestConstants;

import java.io.FileInputStream;

/**
 * @author mrxdhlxpb
 */
public class LimitedRecordingBufferedInputStreamTest {

    private LimitedRecordingBufferedInputStream instance;

    @BeforeEach
    void initialize() throws Exception {
        instance = new LimitedRecordingBufferedInputStream(new FileInputStream(TestConstants
                .TEST_FILE_1));
    }

    @AfterEach
    void release() throws Exception {
        instance.close();
    }

    @Test
    void test0() {
        Assertions.assertDoesNotThrow(() -> instance.readNBytes(10));
        instance.enableLimit(10);
        Assertions.assertDoesNotThrow(() -> instance.readNBytes(10));
        Assertions.assertThrowsExactly(LimitedRecordingBufferedInputStream
                        .TryToReadOutOfLimitException.class,
                () -> instance.readNBytes(1));
        instance.disableLimit();
        Assertions.assertDoesNotThrow(() -> instance.readNBytes(10));
    }

    @Test
    void test1() {
        Assertions.assertDoesNotThrow(() -> instance.enableLimit(0));
    }

    @Test
    void test2() {
        Assertions.assertThrowsExactly(
                IllegalArgumentException.class,
                () -> instance.enableLimit(Integer.MIN_VALUE)
        );
    }

    @Test
    void test3() {
        instance.enableLimit(0);
        Assertions.assertThrowsExactly(
                IllegalStateException.class,
                () -> instance.enableLimit(0)
        );
    }

    @Test
    void test4() {
        instance.enableLimit(0);
        Assertions.assertThrowsExactly(
                IllegalArgumentException.class,
                () -> instance.enableLimit(Integer.MIN_VALUE)
        );
    }

    @Test
    void test5() {
        Assertions.assertThrowsExactly(
                IllegalStateException.class,
                () -> instance.disableLimit()
        );
    }

    @Test
    void test6() {
        instance.enableLimit(0);
        Assertions.assertDoesNotThrow(() -> instance.disableLimit());
    }

}
