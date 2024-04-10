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

import java.io.InputStream;
import java.util.Objects;
import java.util.Optional;

/**
 * An abstraction of HTTP response messages which is independent of the HTTP version,
 * as defined in <em>RFC 9110: HTTP Semantics Section 6. Message Abstraction</em>.
 *
 * @author mrxdhlxpb
 */
@CompliantWith("RFC 9110: HTTP Semantics")
public class HTTPResponse {

    /**
     * The status code of the response, as defined in <em>RFC 9110: HTTP Semantics Section 15.
     * Status Codes</em>. Note: All valid status codes are within the range of 100 to 599,
     * inclusive.
     */
    private short statusCode;

    /**
     * The protocol version of this HTTP message. Non-null required.
     */
    private final ProtocolVersion protocolVersion;

    /**
     * The header section of this message. Non-null required.
     */
    private final Fields headerSection;

    /**
     * The input stream from which the response message content, as defined in <em>RFC 9110: HTTP
     * Semantics Section 6.4. Content</em>, is read. Along with {@code contentLength}, determines
     * the content.
     */
    private InputStream contentInputStream;

    /**
     * The number of octets in the content, or a value within the range of
     * {@code Integer.MIN_VALUE} to -1, inclusive, to indicate an unknown value.
     * <p> Note: This is NOT the same concept as the "Content-Length" header field,
     * as defined in <em>RFC 9110: HTTP Semantics Section 8.6. Content-Length</em>,
     * which is scoped to the <em>selected representation</em>, as defined in
     * <em>RFC 9110: Section 3.2. Representations</em>.
     */
    private int contentLength;

    /**
     * The trailer section of this message, as defined in <em>RFC 9110: HTTP Semantics Section
     * 6.5. Trailer Fields</em>, if any.
     */
    private Fields trailerSection;

    /**
     * Constructs an instance of this class.
     * @param protocolVersion the protocol version of the response message
     * @param headerSection the header section of the response message
     */
    public HTTPResponse(ProtocolVersion protocolVersion, Fields headerSection) {
        this.protocolVersion = Objects.requireNonNull(protocolVersion);
        this.headerSection = Objects.requireNonNull(headerSection);
    }

    /**
     * Constructs an instance of this class with an initially empty header section.
     * @param protocolVersion the protocol version
     */
    public HTTPResponse(ProtocolVersion protocolVersion) {
        this(Objects.requireNonNull(protocolVersion), new Fields());
    }

    public void setStatusCode(short statusCode) {
        this.statusCode = statusCode;
    }

    public short getStatusCode() {
        return statusCode;
    }

    public ProtocolVersion getProtocolVersion() {
        return protocolVersion;
    }

    public Fields getHeaderSection() {
        return headerSection;
    }

    public void setContentInputStream(InputStream contentInputStream) {
        this.contentInputStream = contentInputStream;
    }

    public InputStream getContentInputStream() {
        return contentInputStream;
    }

    public void setContentLength(int contentLength) {
        this.contentLength = contentLength;
    }

    public int getContentLength() {
        return contentLength;
    }

    public void setTrailerSection(Fields trailerSection) {
        this.trailerSection = trailerSection;
    }

    public Optional<Fields> getTrailerSection() {
        return Optional.ofNullable(trailerSection);
    }

}
