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

import java.util.function.Supplier;

/**
 * Represents an HTTP request method, as defined in <em>RFC9110 HTTP
 * Semantics Section 9. Methods</em>.
 *
 * @author mrxdhlxpb
 */
@CompliantWith("RFC 9110: HTTP Semantics Section 9. Methods")
public enum RequestMethod {

    GET(true),

    HEAD(true),

    POST(true),

    PUT(false),

    DELETE(false),

    CONNECT(false),

    OPTIONS(true),

    TRACE(false);

    private final boolean supported;

    RequestMethod(boolean supported) {
        this.supported = supported;
    }

    public boolean isSupported() { return supported; }

    public static <X extends Throwable> RequestMethod of(String str,
                                                         Supplier<? extends X> supplier)
            throws X {
        try {
            return valueOf(str);
        } catch (IllegalArgumentException illegalArgumentException) {
            throw supplier.get();
        }
    }
}
