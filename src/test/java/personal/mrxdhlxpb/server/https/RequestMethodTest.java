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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.opentest4j.AssertionFailedError;

/**
 * @author mrxdhlxpb
 */
public class RequestMethodTest {

    @Test
    void testIsSupported() {
        final boolean supported0 = RequestMethod.GET.isSupported();
        final boolean supported1 = RequestMethod.HEAD.isSupported();
        final boolean supported2 = RequestMethod.POST.isSupported();
        final boolean supported3 = RequestMethod.PUT.isSupported();
        final boolean supported4 = RequestMethod.DELETE.isSupported();
        final boolean supported5 = RequestMethod.CONNECT.isSupported();
        final boolean supported6 = RequestMethod.OPTIONS.isSupported();
        final boolean supported7 = RequestMethod.TRACE.isSupported();

        Assertions.assertTrue(supported0 && supported1 && supported2 && supported6);
        Assertions.assertFalse(supported3 || supported4 || supported5 || supported7);
    }

    @Test
    void testOf() {
        Assertions.assertEquals(
                RequestMethod.GET,
                RequestMethod.of("GET", AssertionFailedError::new)
        );
        Assertions.assertEquals(
                RequestMethod.HEAD,
                RequestMethod.of("HEAD", AssertionFailedError::new)
        );
        Assertions.assertEquals(
                RequestMethod.POST,
                RequestMethod.of("POST", AssertionFailedError::new)
        );
        Assertions.assertEquals(
                RequestMethod.PUT,
                RequestMethod.of("PUT", AssertionFailedError::new)
        );
        Assertions.assertEquals(
                RequestMethod.DELETE,
                RequestMethod.of("DELETE", AssertionFailedError::new)
        );
        Assertions.assertEquals(
                RequestMethod.CONNECT,
                RequestMethod.of("CONNECT", AssertionFailedError::new)
        );
        Assertions.assertEquals(
                RequestMethod.OPTIONS,
                RequestMethod.of("OPTIONS", AssertionFailedError::new)
        );
        Assertions.assertEquals(
                RequestMethod.CONNECT,
                RequestMethod.of("CONNECT", AssertionFailedError::new)
        );

        Assertions.assertThrowsExactly(
                AssertionFailedError.class,
                () -> RequestMethod.of("get", AssertionFailedError::new)
        );
        Assertions.assertThrowsExactly(
                AssertionFailedError.class,
                () -> RequestMethod.of("GET ", AssertionFailedError::new)
        );
        Assertions.assertThrowsExactly(
                AssertionFailedError.class,
                () -> RequestMethod.of(" GET ", AssertionFailedError::new)
        );
        Assertions.assertThrowsExactly(
                AssertionFailedError.class,
                () -> RequestMethod.of("Get", AssertionFailedError::new)
        );

    }
}
