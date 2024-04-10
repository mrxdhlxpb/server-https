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

import personal.mrxdhlxpb.server.https.configuration.Configuration;
import personal.mrxdhlxpb.server.https.decoder.ChainTransferCodingDecoder;
import personal.mrxdhlxpb.server.https.decoder.TransferCodingDecoder;
import personal.mrxdhlxpb.server.https.error.HttpErrorException;
import personal.mrxdhlxpb.server.https.error.concrete.client.*;
import personal.mrxdhlxpb.server.https.error.concrete.server.HTTPVersionNotSupportedException;
import personal.mrxdhlxpb.server.https.error.concrete.server.InternalServerErrorException;
import personal.mrxdhlxpb.server.https.error.concrete.server.NotImplementedException;

import java.io.IOException;
import java.io.InputStream;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author mrxdhlxpb
 */
@CompliantWith("RFC 9110: HTTP Semantics")
@CompliantWith("RFC 9112: HTTP/1.1")
public class HTTP1_1RequestParser {

    static final String ABSOLUTE_PATH_REGEX = "(?<ABSOLUTEPATH>" + "(" + "/" +
            HttpsURI.SEGMENT_REGEX + ")+" + ")";
    static final String ORIGIN_FORM_REGEX = ABSOLUTE_PATH_REGEX + HttpsURI.QUERY_OPTIONAL_REGEX;
    static final Pattern ORIGIN_FORM_PATTERN = Pattern.compile(ORIGIN_FORM_REGEX);
    static final String HOST_REGEX = HttpsURI.URI_HOST_REGEX + HttpsURI.PORT_OPTIONAL_REGEX;
    static final Pattern HOST_PATTERN = Pattern.compile(HOST_REGEX);
    static final String HTTP_VERSION_REGEX = "HTTP/(?<MAJOR>\\d)\\.(?<MINOR>\\d)";
    static final Pattern HTTP_VERSION_PATTERN = Pattern.compile(HTTP_VERSION_REGEX);

    private final Configuration configuration;

    private final HttpRequestInputStream httpRequestInputStream;

    private MutableHTTPRequest mutableHTTPRequest;

    public HTTP1_1RequestParser(Configuration configuration, InputStream inputStream) {
        this.configuration = configuration;
        this.httpRequestInputStream = new HttpRequestInputStream(inputStream);
    }

    /**
     * Reads a request message from the input stream and parses the request message into an
     * instance of {@link HTTPRequest}.
     *
     * @return the instance
     * @throws HttpErrorException if a client error or a server error occurs
     */
    public HTTPRequest parseRequest() throws HttpErrorException {
        mutableHTTPRequest = new MutableHTTPRequest();

        parseRequestLine();

        // parse header section
        try {
            mutableHTTPRequest.headerSection = httpRequestInputStream
                    .readFields(
                            configuration
                                    .getHTTP1_1Configuration()
                                    .getMaximumRequestFieldLineLength(),
                            configuration
                                    .getHTTP1_1Configuration()
                                    .getMaximumRequestHeaderSectionLength());
        } catch (HttpRequestInputStream.TryToReadOutOfLimitException e) {
            throw new BadRequestException("length of request header exceeds limit");
        } catch (SocketTimeoutException e) {
            throw new RequestTimeoutException();
        } catch (IOException e) {
            throw new InternalServerErrorException(e);
        }

        reconstructTargetURI();

        // Reject the request if it appears to have been misdirected,
        // as described in RFC 9110: HTTP Semantics Section 7.4. Rejecting Misdirected Requests.
        // Note: We do not support virtual hosts.
        if (!mutableHTTPRequest
                .targetURI
                .getHost()
                .equals(
                        configuration
                                .getNetworkConfiguration()
                                .getServerName())
                && !configuration
                .getNetworkConfiguration()
                .getServerAliases()
                .contains(
                        mutableHTTPRequest
                                .targetURI
                                .getHost())
                || !mutableHTTPRequest
                .targetURI
                .getPortOr443()
                .equals(Integer.toString(
                        configuration
                                .getNetworkConfiguration()
                                .getPort())))
            throw new MisdirectedRequestException();

        mutableHTTPRequest.targetResource = configuration
                .getInternalResourceMapper()
                .getInternalResource(
                        mutableHTTPRequest
                                .targetURI
                                .toInternalResourceIdentifier());

        // As defined in RFC 9112: HTTP/1.1 Section 6. Message Body,
        // the presence of a message body in a request is signaled by
        // a Content-Length or Transfer-Encoding header field.
        // Request message framing is independent of method semantics.

        // Therefore, even though content received in a GET / HEAD request has no generally defined
        // semantics, we continue to parse the rest of the request message, regardless of the
        // request method, in the same manner.

        // However, content received in a GET / HEAD request
        // might lead some implementations to reject the request and close the connection
        // because of its potential as a request smuggling attack.

        // TODO: prevent request smuggling attack

        parseBody();

        return mutableHTTPRequest.toImmutableHTTPRequest();
    }


    /**
     * A routine that parses the request line.
     *
     * @throws HTTPVersionNotSupportedException unless
     *                                          <ol>
     *                                              <li>
     *                                                  the request line satisfies the requirement
     *                                                  of the grammar rule as defined in
     *                                                  <em>RFC 9112: HTTP/1.1</em>,
     *                                              </li>
     *                                              <li>
     *                                                  the client's major protocol
     *                                                  version is 1, and
     *                                              </li>
     *                                              <li>
     *                                                  the client's minor protocol
     *                                                  version is 1 or 0.
     *                                              </li>
     *                                          </ol>
     * @throws BadRequestException              if
     *                                          <ol>
     *                                              <li>
     *                                                  length of the request line exceeds limit,
     *                                              </li>
     *                                              <li>
     *                                                  request method is unknown by server, or
     *                                              </li>
     *                                              <li>request target is invalid.</li>
     *                                          </ol>
     * @throws InternalServerErrorException     if an I/O error occurs
     * @throws RequestTimeoutException          if a timeout has occurred on a socket read
     * @throws MethodNotAllowedException        if the request method is known but
     *                                          not supported by the server
     */
    private void parseRequestLine() throws
            HTTPVersionNotSupportedException,
            BadRequestException,
            InternalServerErrorException,
            RequestTimeoutException,
            MethodNotAllowedException {

        //TODO:
        // RFC 9112: HTTP/1.1 Section 2.2. Message Parsing
        // In the interest of robustness, a server that is expecting to receive and parse a
        // request-line SHOULD ignore at least one empty line (CRLF) received prior to the
        // request-line.

        byte[] bytes = new byte[configuration.getHTTP1_1Configuration()
                .getMaximumRequestLineLength()];
        int len;
        try {
            len = httpRequestInputStream.readLine(bytes);
        } catch (HttpRequestInputStream.CannotContainException e) {
            throw new BadRequestException("length of the request line exceeds limit");
        } catch (SocketTimeoutException e) {
            throw new RequestTimeoutException();
        } catch (IOException e) {
            throw new InternalServerErrorException(e);
        }
        String requestLineString = new String(bytes, 0, len, StandardCharsets.US_ASCII);

        //request-line  = method SP request-target SP HTTP-version

        String[] array = requestLineString.split("\\x20", 3);
        if (array.length < 3)
            throw new HTTPVersionNotSupportedException();
        String method = array[0];
        String requestTarget = array[1];
        String httpVersion = array[2];

        this.mutableHTTPRequest.requestMethod = RequestMethod.of(method,
                HTTPVersionNotSupportedException::new);

        if (!this.mutableHTTPRequest.requestMethod.isSupported())
            throw new MethodNotAllowedException();

        this.mutableHTTPRequest.requestTarget = requestTarget;

        Matcher matcher = HTTP_VERSION_PATTERN.matcher(httpVersion);
        if (!matcher.matches())
            throw new HTTPVersionNotSupportedException();
        byte majorVersion = Byte.parseByte(matcher.group("MAJOR"));
        byte minorVersion = Byte.parseByte(matcher.group("MINOR"));
        if (minorVersion != 0 && minorVersion != 1 || majorVersion != 1)
            throw new HTTPVersionNotSupportedException();
        this.mutableHTTPRequest.protocolVersion = new ProtocolVersion(majorVersion, minorVersion);
    }


    /**
     * A routine that reconstructs the target URI in a manner consistent with
     * <em>RFC 9112: HTTP/1.1 Section 3.3. Reconstructing the Target URI</em>.
     */
    private void reconstructTargetURI() throws BadRequestException {
        // We have 4 forms of request-targets:
        // 1. origin-form
        // 2. absolute-form
        // 3. asterisk-form
        // 4. authority-form
        // We only support GET, HEAD, POST & OPTIONS requests,
        // so authority-form request-targets are forbidden.
        // If the request-target is in absolute-form, target URI = request-target
        // Otherwise,
        // we reconstruct the target URI as follows:
        // 1. We have a FIXED URI scheme: https.
        // 2. The target URI's authority component is the field value of the Host header field.
        // If there is no Host header field or if its field value is empty or invalid,
        // the target URI's authority component is empty.
        // 3. If the request-target is in asterisk-form, the target URI's combined path & query
        // component = empty.
        // Otherwise, (i.e. origin-form)
        // the target URI's combined path & query component = request-target
        // 4. "https" URI scheme requires a non-empty authority.
        // We will reject the request if the target URI's authority component is empty.

        Optional<HttpsURI> optional;
        if ((optional = HttpsURI.fromString(mutableHTTPRequest.requestTarget)).isPresent()) {
            // RFC 9112: HTTP/1.1 Section 3.2.2. absolute-form
            // When an origin server receives a request with an absolute-form of request-target,
            // the origin server MUST ignore the received Host header field (if any)
            // and instead use the host information of the request-target.
            mutableHTTPRequest.targetURI = optional.get().normalize();
        } else {
            String hostFieldValue = mutableHTTPRequest
                    .headerSection
                    .getFieldValueString("Host")
                    .orElseThrow(() -> new BadRequestException("Empty authority component"));
            Matcher hostMatcher = HOST_PATTERN.matcher(hostFieldValue);
            if (hostFieldValue.isEmpty() || !hostMatcher.matches())
                throw new BadRequestException("Empty authority component");

            String uriHost = hostMatcher.group("URIHOST"); // never null, might be empty
            String port = hostMatcher.group("PORT"); // nullable

            Matcher originFormMatcher = ORIGIN_FORM_PATTERN
                    .matcher(mutableHTTPRequest.requestTarget);

            if (mutableHTTPRequest.requestTarget.equals("*")) {
                mutableHTTPRequest.targetURI = new HttpsURI(uriHost, port, "", null).normalize();
            } else if (originFormMatcher.matches()) {
                String path = originFormMatcher.group("ABSOLUTEPATH");// never null nor empty
                String query = originFormMatcher.group("QUERY"); // nullable
                mutableHTTPRequest.targetURI = new HttpsURI(uriHost, port, path, query).normalize();
            } else {
                throw new BadRequestException("Invalid request target");
            }
        }
    }


    /**
     * A routine that parses the message body.
     * @throws BadRequestException if the server should respond with a 400 status code and then
     *                             close the connection
     * @throws NotImplementedException TODO
     * @throws InternalServerErrorException TODO
     * @throws ContentTooLargeException TODO
     */
    private void parseBody() throws
            BadRequestException,
            NotImplementedException,
            InternalServerErrorException,
            ContentTooLargeException {

        Optional<Fields.Field> contentLengthHeaderFieldOptional = mutableHTTPRequest
                .headerSection
                .getField("Content-Length");
        Optional<String[]> transferEncodingHeaderFieldValueMembersOptional = mutableHTTPRequest
                .headerSection
                .getFieldValueMembers("Transfer-Encoding");

        // RFC 9112: HTTP/1.1 Section 6.3 Message Body Length

        if (transferEncodingHeaderFieldValueMembersOptional.isPresent()) {
            String[] transferEncodingHeaderFieldValueMembers =
                    transferEncodingHeaderFieldValueMembersOptional.get();

            // 3. If a message is received with both a Transfer-Encoding and a Content-Length header
            // field,
            // the Transfer-Encoding overrides the Content-Length. Such a message might indicate an
            // attempt to perform request smuggling (Section 11.2) or response splitting
            // (Section 11.1) and ought to be handled as an error.
            if (contentLengthHeaderFieldOptional.isPresent())
                throw new BadRequestException("Both a Transfer-Encoding and a Content-Length" +
                        " header field are received.");

            String finalEncoding = transferEncodingHeaderFieldValueMembers
                    [transferEncodingHeaderFieldValueMembers.length-1];

            // 4. If a Transfer-Encoding header field is present in a request and the chunked
            // transfer coding is not the final encoding,
            // the message body length cannot be determined reliably; the server MUST respond with
            // the 400 (Bad Request) status code and then close the connection.
            if (!finalEncoding.equals("chunked"))
                throw new BadRequestException("A Transfer-Encoding header field is present " +
                        "in a request and the chunked transfer coding is not the final encoding.");

            // 4. If a Transfer-Encoding header field is present and the chunked transfer coding
            // (Section 7.1) is the final encoding,
            // the message body length is determined by reading and decoding the chunked data until
            // the transfer coding indicates the data is complete.

            try {
                ChainTransferCodingDecoder decoder = TransferCodingDecoder
                        .of(configuration.getHTTPDecoderRegistry(),
                                transferEncodingHeaderFieldValueMembers)
                        .decode(httpRequestInputStream);

                mutableHTTPRequest.requestContentInputStream = decoder.getContentInputStream();
                mutableHTTPRequest.contentLength = decoder.getContentLength();
                if (mutableHTTPRequest.contentLength > configuration
                        .getHTTP1_1Configuration()
                        .getMaximumRequestContentLength())
                    throw new ContentTooLargeException();
                mutableHTTPRequest.trailerSection = decoder.getTrailerFieldsDirectly();
            } catch (IOException e) {
                throw new InternalServerErrorException(e);
            }

            return;
        }

        // 7. If this is a request message and neither Transfer-Encoding nor Content-Length header
        // field is present,
        // then the message body length is zero (no message body is present).
        if (contentLengthHeaderFieldOptional.isEmpty()) {
            mutableHTTPRequest.contentLength = 0;
            return;
        }

        Fields.Field contentLengthHeaderField = contentLengthHeaderFieldOptional.get();

        try {
            mutableHTTPRequest.contentLength = Integer.parseInt(contentLengthHeaderField
                    .fieldValueString(), 10);
        } catch (NumberFormatException e) {
            // 5. If a message is received without Transfer-Encoding and with an invalid
            // Content-Length header field,
            // then the message framing is invalid and the recipient MUST treat it as an
            // unrecoverable error, unless the field value can be successfully parsed as a
            // comma-separated list (Section 5.6.1 of [HTTP]), all values in the list are valid, and
            // all values in the list are the same (in which case, the message is processed with
            // that single value used as the Content-Length field value). If the unrecoverable error
            // is in a request message, the server MUST respond with a 400 (Bad Request) status code
            // and then close the connection.

            String[] list = contentLengthHeaderField.fieldValueMembers();

            if (list.length == 0)
                throw new BadRequestException("Neither Transfer-Encoding nor a valid " +
                        "Content-Length is received.");

            try {
                int parsedFirstMember = Integer.parseInt(list[0], 10);
                for (int i = 1; i < list.length; i++) {
                    int parsedMember = Integer.parseInt(list[i], 10);
                    if (parsedMember != parsedFirstMember)
                        throw new BadRequestException("Neither Transfer-Encoding nor a valid " +
                                "Content-Length is received.");
                }
                mutableHTTPRequest.contentLength = parsedFirstMember;
            } catch (NumberFormatException exception) {
                throw new BadRequestException("Neither Transfer-Encoding nor a valid" +
                        " Content-Length is received.");
            }
        }

        if (mutableHTTPRequest.contentLength < 0)
            throw new BadRequestException("negative Content-Length received");

        if (mutableHTTPRequest.contentLength > configuration
                .getHTTP1_1Configuration()
                .getMaximumRequestContentLength())
            throw new ContentTooLargeException();

        // 6. If a valid Content-Length header field is present without Transfer-Encoding,
        // its decimal value defines the expected message body length in octets. If the sender
        // closes the connection or the recipient times out before the indicated number of octets
        // are received, the recipient MUST consider the message to be incomplete and close the
        // connection.

        mutableHTTPRequest.requestContentInputStream = httpRequestInputStream;
    }

    private static class MutableHTTPRequest {
        RequestMethod requestMethod;

        String requestTarget;

        ProtocolVersion protocolVersion;

        Fields headerSection;

        HttpsURI targetURI;

        InternalResource targetResource;

        InputStream requestContentInputStream;

        int contentLength;

        Fields trailerSection;

        HTTPRequest toImmutableHTTPRequest() {
            return new HTTPRequest(
                    new RequestMessageControlData(
                            requestMethod,
                            targetResource,
                            protocolVersion
                    ),
                    headerSection,
                    Optional.ofNullable(requestContentInputStream),
                    Optional.ofNullable(trailerSection),
                    contentLength
            );
        }
    }
}
