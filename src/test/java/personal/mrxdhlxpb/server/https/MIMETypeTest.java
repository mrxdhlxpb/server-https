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

import java.util.Optional;

/**
 * @author mrxdhlxpb
 */
public class MIMETypeTest {

    @Test
    void test0() throws MIMEType.MIMETypeParseException {
        MIMEType mimeType = new MIMEType("application/alto-updatestreamcontrol+json");
        Assertions.assertEquals(new MIMEType("application", "alto-updatestreamcontrol+json"),
                mimeType);
        Assertions.assertEquals("application", mimeType.getMediaType());
        Assertions.assertEquals("alto-updatestreamcontrol+json", mimeType.getSubtype());
        Assertions.assertFalse(mimeType.getParameterValue("test").isPresent());
    }

    @Test
    void test1() throws MIMEType.MIMETypeParseException {
        final MIMEType mimeType = new MIMEType("text/html;charset=utf-8");
        final MIMEType type = new MIMEType("text", "html");
        Assertions.assertTrue(mimeType.isSameAs(type));
        Assertions.assertNotEquals(mimeType, type);
        final Optional<String> charsetOptional = mimeType.getParameterValue("charset");
        Assertions.assertTrue(charsetOptional.isPresent());
        Assertions.assertEquals("utf-8", charsetOptional.get());
    }

    @Test
    void test2() throws MIMEType.MIMETypeParseException {
        final MIMEType mimeType = new MIMEType("TEXT/html; CHARSET=utf-8");
        final MIMEType type = new MIMEType("text", "html");
        Assertions.assertTrue(mimeType.isSameAs(type));
        Assertions.assertNotEquals(mimeType, type);
        final Optional<String> charsetOptional = mimeType.getParameterValue("charset");
        Assertions.assertTrue(charsetOptional.isPresent());
        Assertions.assertEquals("utf-8", charsetOptional.get());
    }

    @Test
    void test3() throws MIMEType.MIMETypeParseException {
        MIMEType mimeType = new MIMEType("text/html ;charset=utf-8");
        Assertions.assertNotEquals("html", mimeType.getSubtype());
        Assertions.assertEquals("html ", mimeType.getSubtype());
        Assertions.assertTrue(mimeType.getParameterValue("charset").isPresent());
        Assertions.assertEquals("utf-8", mimeType.getParameterValue("charset").get());
    }

    @Test
    void test4() {
        Assertions.assertThrowsExactly(MIMEType.MIMETypeParseException.class,
                () -> new MIMEType("text"));
        Assertions.assertThrowsExactly(MIMEType.MIMETypeParseException.class,
                () -> new MIMEType("text/html;charset"));
    }

    @Test
    void test5() throws Exception {
        Assertions.assertDoesNotThrow(() -> new MIMEType("text/"));
        Assertions.assertTrue(new MIMEType("text/").getSubtype().isEmpty());

        Assertions.assertDoesNotThrow(() -> new MIMEType("/html"));
        Assertions.assertTrue(new MIMEType("/html").getMediaType().isEmpty());
    }

}
