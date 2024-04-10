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

import org.junit.jupiter.api.Test;
import personal.mrxdhlxpb.server.https.error.concrete.client.BadRequestException;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author mrxdhlxpb
 */
public class HttpsURITest {

    @Test
    void testRejectEmptyHost() {
        assertThrowsExactly(BadRequestException.class, () -> new HttpsURI("", "", "", ""));
        assertThrowsExactly(BadRequestException.class, () -> HttpsURI.fromString("https://:443/?"));
    }

    @Test
    void testFromString() throws Exception {
        // invalid https URI syntax
        assertTrue(HttpsURI.fromString("http://www.example.org/index.html").isEmpty());

        // valid https URI
        HttpsURI instance = HttpsURI.fromString("https://www.example.org:443/index.html?query")
                .orElseThrow();
        assertEquals("www.example.org", instance.getHost());
        assertEquals("443", instance.getPort());
        assertEquals("/index.html", instance.getPath());
        assertEquals("query", instance.getQuery());
    }

    private void assertNormalizationSucceed(String normal, String variant) throws Exception {
        assertEquals(normal, HttpsURI.fromString(variant).orElseThrow().normalize().recombine());
    }

    private void assertNormalizationFail(String normal, String variant) throws Exception {
        assertNotEquals(normal, HttpsURI.fromString(variant).orElseThrow().normalize().recombine());
    }

    @Test
    void testNormalize() throws Exception {
        assertNormalizationSucceed("https://www.example.org/?", "hTtps://www.example.org/?");
        assertNormalizationSucceed("https://www.example.org/?", "hTtps://www.Example.org/?");
        assertNormalizationSucceed("https://www.example.org/?", "hTtps://www.example.org?");
        assertNormalizationSucceed("https://www.example.org/?", "hTtps://www%2Eexample.org?");
        assertNormalizationSucceed("https://www.example.org/?", "hTtps://www%2eexample.org?");
        assertNormalizationSucceed("https://www.example.org/?", "hTtps://www%2eexample.org:?");
        assertNormalizationSucceed("https://www.example.org/?", "hTtps://www%2eexample.org:443?");
        assertNormalizationSucceed("https://www.example.org/?",
                "hTtps://www%2eexample.org:443/a/..?");
        assertNormalizationSucceed("https://www.example.org/?",
                "hTtps://www%2eexample.org:443/a/../.?");

        assertNormalizationFail("https://www.example.org/?", "https://www.example.org/");
        assertNormalizationFail("https://www.example.org/?", "https://www.example.org");
        assertNormalizationFail("https://www.example.org/?", "hTtps://www%2eexample.org:333?");
    }

    @Test
    void testRecombine() throws Exception {
        assertEquals("https://www.example.org:/", new HttpsURI("www.example.org",
                "", "/", null).recombine());
    }

}
