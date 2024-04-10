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

/**
 * Represents the HTTP's version number, consisting of a major version number and a minor version
 * number, as defined in <em>RFC 9110: HTTP Semantics Section 2.5. Protocol Version</em>.
 * @param majorVersion the major version number
 * @param minorVersion the minor version number
 *
 * @author mrxdhlxpb
 */
@CompliantWith("RFC 9110: HTTP Semantics Section 2.5. Protocol Version")
public record ProtocolVersion(byte majorVersion, byte minorVersion) {

    public static final String HTTP__1_1_STRING = "HTTP/1.1";

    public static final ProtocolVersion HTTP__1_1 = new ProtocolVersion((byte) 1, (byte) 1);

    public static final String HTTP__1_0_STRING = "HTTP/1.0";

    public static final ProtocolVersion HTTP__1_0 = new ProtocolVersion((byte) 1, (byte) 0);

    /**
     * Determines whether {@code this} is later than the argument.
     * @param protocolVersion the argument to be compared
     * @return whether {@code this} is later than the argument
     * @throws NullPointerException if the argument is {@code null}
     */
    public boolean isLaterThan(ProtocolVersion protocolVersion) {
        if (this.majorVersion > protocolVersion.majorVersion)
            return true;
        if (this.majorVersion == protocolVersion.majorVersion)
            return this.minorVersion > protocolVersion.minorVersion;
        return false;
    }
}
