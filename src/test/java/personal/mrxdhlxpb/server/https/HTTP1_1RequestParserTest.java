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
import personal.mrxdhlxpb.server.https.configuration.Configuration;
import personal.mrxdhlxpb.server.https.configuration.HTTP1_1Configuration;
import personal.mrxdhlxpb.server.https.configuration.NetworkConfiguration;
import personal.mrxdhlxpb.server.https.decoder.ChunkedTransferCodingDecoder;
import personal.mrxdhlxpb.server.https.decoder.ContentCodingDecoder;
import personal.mrxdhlxpb.server.https.decoder.HTTPDecoderRegistry;
import personal.mrxdhlxpb.server.https.decoder.TransferCodingDecoder;
import personal.mrxdhlxpb.server.https.error.HttpErrorException;
import personal.mrxdhlxpb.server.https.error.concrete.client.BadRequestException;
import personal.mrxdhlxpb.server.https.error.concrete.client.ContentTooLargeException;
import personal.mrxdhlxpb.server.https.test.TestConstants;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.Set;

/**
 * @author mrxdhlxpb
 */
public class HTTP1_1RequestParserTest {
    private static final Configuration CONFIG = new Configuration() {
        @Override
        public NetworkConfiguration getNetworkConfiguration() {
            return new NetworkConfiguration() {
                @Override
                public int getPort() {
                    return 443;
                }

                @Override
                public int getServerSocketBacklog() {
                    return 0;
                }

                @Override
                public InetAddress getServerSocketBindAddress() {
                    return null;
                }

                @Override
                public File getKeyStoreFile() {
                    return null;
                }

                @Override
                public char[] getKeyStorePassword() {
                    return new char[0];
                }

                @Override
                public int getSocketSoTimeout() {
                    return 0;
                }

                @Override
                public String getServerName() {
                    return "localhost";
                }

                @Override
                public Set<String> getServerAliases() {
                    return Set.of();
                }
            };
        }

        @Override
        public InternalResourceMapper getInternalResourceMapper() {
            return new InternalResourceMapper() {
                @Override
                public InternalResource getInternalResource(InternalResourceIdentifier
                                                                    internalResourceIdentifier) {
                    return new InternalResource() {
                        @Override
                        public InternalResourceIdentifier getInternalResourceIdentifier() {
                            return internalResourceIdentifier;
                        }

                        @Override
                        public HTTPRequestHandler getHTTPRequestHandler() {
                            return null;
                        }

                        @Override
                        public HTTP1_1ResponseMessageBodyGenerator
                        getHTTP1_1ResponseMessageBodyGenerator() {
                            return null;
                        }
                    };
                }
            };
        }

        @Override
        public HTTPDecoderRegistry getHTTPDecoderRegistry() {
            return new HTTPDecoderRegistry() {
                @Override
                public Optional<TransferCodingDecoder> getTransferCodingDecoder(String name) {
                    return Optional.empty();
                }

                @Override
                public Optional<ContentCodingDecoder> getContentCodingDecoder(String name) {
                    return Optional.empty();
                }
            };
        }

        @Override
        public HttpErrorHandlerRegistry getHttpErrorHandlerRegistry() {
            return new HttpErrorHandlerRegistry() {
                @Override
                public HttpErrorHandler getHttpErrorHandler(Class<? extends HttpErrorException>
                                                                    httpErrorExceptionClass) {
                    return null;
                }
            };
        }

        @Override
        public HTTP1_1Configuration getHTTP1_1Configuration() {
            return new HTTP1_1Configuration() {
                @Override
                public int getMaximumRequestLineLength() {
                    return 100;
                }

                @Override
                public int getMaximumRequestFieldLineLength() {
                    return 100;
                }

                @Override
                public int getMaximumRequestHeaderSectionLength() {
                    return 500;
                }

                @Override
                public int getMaximumRequestContentLength() {
                    return 26;
                }

                @Override
                public HTTP1_1ResponseMessageBodyGenerator
                getHttpErrorHTTP1_1ResponseMessageBodyGenerator() {
                    return null;
                }
            };
        }
    };
    
    @Test
    void testParseRequest0() throws Exception {
        final String requestMessage = """
                HEAD /test HTTP/1.1\r
                Host: localhost\r
                Connection: keep-alive\r
                \r
                """;
        final ByteArrayInputStream requestMessageInputStream = new ByteArrayInputStream(
                requestMessage.getBytes(StandardCharsets.US_ASCII));
        HTTP1_1RequestParser http1_1RequestParser = new HTTP1_1RequestParser(
                CONFIG, 
                requestMessageInputStream);

        final RequestMethod expectedRequestMethod = RequestMethod.HEAD;
        final Fields expectedHeaderSection = new Fields();
        expectedHeaderSection.set("host", "localhost");
        expectedHeaderSection.set("connection", "keep-alive");
        final Optional<InputStream> expectedRequestContentInputStream = Optional.empty();
        final Optional<Fields> expectedTrailerSection = Optional.empty();

        try (HTTPRequest httpRequest = http1_1RequestParser.parseRequest()) {
            Assertions.assertEquals(
                    expectedRequestMethod,
                    httpRequest
                            .requestMessageControlData()
                            .requestMethod());
            Assertions.assertEquals(
                    expectedHeaderSection,
                    httpRequest
                            .headerSection()
            );
            Assertions.assertEquals(
                    expectedRequestContentInputStream,
                    httpRequest.requestContentInputStream()
            );
            Assertions.assertEquals(
                    expectedTrailerSection,
                    httpRequest.trailerSection()
            );
        }
    }

    @Test
    void testParseRequest1() throws Exception {
        final String requestMessage = """
                POST /login HTTP/1.1\r
                Host: localhost\r
                Connection: keep-alive\r
                Content-Length: 26\r
                Content-Type: application/x-www-form-urlencoded\r
                \r
                username=abc&&password=123\r
                \r
                """;
        final ByteArrayInputStream requestMessageInputStream = new ByteArrayInputStream(
                requestMessage.getBytes(StandardCharsets.US_ASCII));
        HTTP1_1RequestParser http1_1RequestParser = new HTTP1_1RequestParser(
                CONFIG,
                requestMessageInputStream);


        final RequestMethod expectedRequestMethod = RequestMethod.POST;
        final InternalResourceIdentifier expectedInternalResourceIdentifier =
                new InternalResourceIdentifier("/login", null);
        final ProtocolVersion expectedProtocolVersion = ProtocolVersion.HTTP__1_1;

        final Fields expectedHeaderSection = new Fields();
        expectedHeaderSection.set("host", "localhost");
        expectedHeaderSection.set("connection", "keep-alive");
        expectedHeaderSection.set("content-length", "26");
        expectedHeaderSection.set("content-type", "application/x-www-form-urlencoded");

        final byte[] expectedRequestContentByteArray = "username=abc&&password=123"
                .getBytes(StandardCharsets.US_ASCII);

        final Optional<Fields> expectedTrailerSection = Optional.empty();

        try (HTTPRequest httpRequest = http1_1RequestParser.parseRequest()) {
            // request line
            // request method
            Assertions.assertEquals(
                    expectedRequestMethod,
                    httpRequest
                            .requestMessageControlData()
                            .requestMethod());
            // request target
            Assertions.assertEquals(
                    expectedInternalResourceIdentifier,
                    httpRequest
                            .requestMessageControlData()
                            .targetResource()
                            .getInternalResourceIdentifier()
            );
            // protocol version
            Assertions.assertEquals(
                    expectedProtocolVersion,
                    httpRequest
                            .requestMessageControlData()
                            .protocolVersion()
            );

            // header section
            Assertions.assertEquals(
                    expectedHeaderSection,
                    httpRequest
                            .headerSection()
            );

            // content
            try (InputStream requestContentInputStream = httpRequest
                    .requestContentInputStream()
                    .orElseThrow(AssertionFailedError::new)) {
                Assertions.assertArrayEquals(
                        expectedRequestContentByteArray,
                        requestContentInputStream.readNBytes(httpRequest.contentLength())
                );
            }

            // trailer section
            Assertions.assertEquals(
                    expectedTrailerSection,
                    httpRequest.trailerSection()
            );
        }
    }


    /**
     * invokes {@code reconstructTargetURI()} using reflection.
     * @param requestTarget the specified request target
     * @param hostHeaderFieldValueString the specified "Host" header field value, or
     *                                   {@code null} if not present
     * @return the target URI
     * @throws Exception if any exception occurs
     */
    private static HttpsURI invokeReconstructTargetURI(String requestTarget,
                                                       String hostHeaderFieldValueString)
        throws Exception {

        final HTTP1_1RequestParser http1_1RequestParser = new HTTP1_1RequestParser(null, null);
        final Method reconstructTargetURIMethod = http1_1RequestParser
                .getClass()
                .getDeclaredMethod("reconstructTargetURI");
        reconstructTargetURIMethod.setAccessible(true);

        final Field mutableHTTPRequestField = http1_1RequestParser
                .getClass()
                .getDeclaredField("mutableHTTPRequest");
        mutableHTTPRequestField.setAccessible(true);

        final Class<?> mutableHTTPRequestClass = http1_1RequestParser
                .getClass()
                .getDeclaredClasses()[0];
        final Constructor<?> mutableHTTPRequestClassDeclaredConstructor = mutableHTTPRequestClass
                .getDeclaredConstructor();
        mutableHTTPRequestClassDeclaredConstructor.setAccessible(true);
        final Object mutableHTTPRequestObject = mutableHTTPRequestClassDeclaredConstructor
                .newInstance();
        final Field mutableHTTPRequestRequestTargetField = mutableHTTPRequestClass
                .getDeclaredField("requestTarget");
        mutableHTTPRequestRequestTargetField.setAccessible(true);

        final Field mutableHTTPRequestHeaderSectionField = mutableHTTPRequestClass
                .getDeclaredField("headerSection");
        mutableHTTPRequestHeaderSectionField.setAccessible(true);

        final Field mutableHTTPRequestTargetURIField = mutableHTTPRequestClass
                .getDeclaredField("targetURI");
        mutableHTTPRequestTargetURIField.setAccessible(true);

        mutableHTTPRequestRequestTargetField.set(mutableHTTPRequestObject, requestTarget);

        final Fields headerSection = new Fields();
        if (hostHeaderFieldValueString != null)
            headerSection.set("host", hostHeaderFieldValueString);

        mutableHTTPRequestHeaderSectionField.set(mutableHTTPRequestObject, headerSection);

        mutableHTTPRequestField.set(http1_1RequestParser, mutableHTTPRequestObject);

        reconstructTargetURIMethod.invoke(http1_1RequestParser);

        return (HttpsURI) mutableHTTPRequestTargetURIField.get(mutableHTTPRequestObject);
    }

    @Test
    void testReconstructTargetURI() throws Exception {

        // origin-form

        Assertions.assertEquals(
                "https://localhost/test",
                invokeReconstructTargetURI("/test", "localhost").recombine()
        );

        Assertions.assertEquals(
                "https://www.example.org/test/another_test?name=value&another_name=another_value",
                invokeReconstructTargetURI(
                        "/test/another_test?name=value&another_name=another_value",
                        "www.example.org"
                )
                        .recombine()
        );

        Assertions.assertEquals(
                "https://www.example.org/test/another_test?name=value&another_name=another_value",
                invokeReconstructTargetURI(
                        "/test/another_test?name=value&another_name=another_value",
                        "www.example.org:443"
                )
                        .recombine()
        );

        Assertions.assertEquals(
                "https://www.example.org:9999/test/another_test" +
                        "?name=value&another_name=another_value",
                invokeReconstructTargetURI(
                        "/test/another_test?name=value&another_name=another_value",
                        "www.example.org:9999"
                )
                        .recombine()
        );

        // absolute-form

        Assertions.assertEquals(
                "https://www.example.org/test",
                invokeReconstructTargetURI(
                        "https://www.example.org/test",
                        null
                )
                        .recombine()
        );

        Assertions.assertEquals(
                "https://www.example.org/test",
                invokeReconstructTargetURI(
                        "https://www.example.org:443/test",
                        "www.example.org:9999"
                )
                        .recombine()
        );

        // asterisk-form

        Assertions.assertEquals(
                "https://localhost/",
                invokeReconstructTargetURI("*", "localhost")
                        .recombine()
        );

    }


    private record InvokeParseBodyResult(int contentLength,
                                         Optional<InputStream> contentInputStream,
                                         Optional<Fields> trailerSection) {}

    private static InvokeParseBodyResult invokeParseBody(
            String contentLengthHeaderFieldValueString,
            String transferEncodingHeaderFieldValueString,
            HTTPDecoderRegistry httpDecoderRegistry,
            InputStream inputStream,
            int maximumRequestContentLength)
            throws Exception {

        final Configuration config = new Configuration() {
            @Override
            public NetworkConfiguration getNetworkConfiguration() {
                return null;
            }

            @Override
            public InternalResourceMapper getInternalResourceMapper() {
                return null;
            }

            @Override
            public HTTPDecoderRegistry getHTTPDecoderRegistry() {
                return httpDecoderRegistry;
            }

            @Override
            public HttpErrorHandlerRegistry getHttpErrorHandlerRegistry() {
                return null;
            }

            @Override
            public HTTP1_1Configuration getHTTP1_1Configuration() {
                return new HTTP1_1Configuration() {
                    @Override
                    public int getMaximumRequestLineLength() {
                        return 0;
                    }

                    @Override
                    public int getMaximumRequestFieldLineLength() {
                        return 0;
                    }

                    @Override
                    public int getMaximumRequestHeaderSectionLength() {
                        return 0;
                    }

                    @Override
                    public int getMaximumRequestContentLength() {
                        return maximumRequestContentLength;
                    }

                    @Override
                    public HTTP1_1ResponseMessageBodyGenerator
                    getHttpErrorHTTP1_1ResponseMessageBodyGenerator() {
                        return null;
                    }
                };
            }
        };

        final HTTP1_1RequestParser http1_1RequestParser = new HTTP1_1RequestParser(config,
                inputStream);

        // prepare mutableHTTPRequest

        final Field mutableHTTPRequestField = http1_1RequestParser
                .getClass()
                .getDeclaredField("mutableHTTPRequest");
        mutableHTTPRequestField.setAccessible(true);

        final Class<?> mutableHTTPRequestClass = http1_1RequestParser
                .getClass()
                .getDeclaredClasses()[0];

        final Constructor<?> mutableHTTPRequestClassConstructor = mutableHTTPRequestClass
                .getDeclaredConstructor();
        mutableHTTPRequestClassConstructor.setAccessible(true);

        final Object mutableHTTPRequestObject = mutableHTTPRequestClassConstructor.newInstance();

        final Field mutableHTTPRequestHeaderSectionField = mutableHTTPRequestClass
                .getDeclaredField("headerSection");
        mutableHTTPRequestHeaderSectionField.setAccessible(true);

        final Fields headerSection = new Fields();
        if (contentLengthHeaderFieldValueString != null)
            headerSection.set("content-length", contentLengthHeaderFieldValueString);
        if (transferEncodingHeaderFieldValueString != null)
            headerSection.set("transfer-encoding", transferEncodingHeaderFieldValueString);

        mutableHTTPRequestHeaderSectionField.set(mutableHTTPRequestObject, headerSection);

        mutableHTTPRequestField.set(http1_1RequestParser, mutableHTTPRequestObject);

        // invoke method

        final Method parseBodyMethod = http1_1RequestParser
                .getClass()
                .getDeclaredMethod("parseBody");
        parseBodyMethod.setAccessible(true);

        try {
            parseBodyMethod.invoke(http1_1RequestParser);
        } catch (InvocationTargetException invocationTargetException) {
            throw (Exception) invocationTargetException.getCause();
        }

        // obtain result

        final Field mutableHTTPRequestContentLengthField = mutableHTTPRequestClass
                .getDeclaredField("contentLength");
        mutableHTTPRequestContentLengthField.setAccessible(true);

        final Field mutableHTTPRequestRequestContentInputStreamField = mutableHTTPRequestClass
                .getDeclaredField("requestContentInputStream");
        mutableHTTPRequestRequestContentInputStreamField.setAccessible(true);

        final Field mutableHTTPRequestTrailerSectionField = mutableHTTPRequestClass
                .getDeclaredField("trailerSection");
        mutableHTTPRequestTrailerSectionField.setAccessible(true);

        return new InvokeParseBodyResult(
                (int) mutableHTTPRequestContentLengthField.get(mutableHTTPRequestObject),
                Optional.ofNullable((InputStream) mutableHTTPRequestRequestContentInputStreamField
                        .get(mutableHTTPRequestObject)),
                Optional.ofNullable((Fields) mutableHTTPRequestTrailerSectionField
                        .get(mutableHTTPRequestObject))
        );
    }


    @Test
    void testParseBody0() throws Exception {

        // 3. If a message is received with both a Transfer-Encoding and a Content-Length header
        // field,
        // the Transfer-Encoding overrides the Content-Length. Such a message might indicate an
        // attempt to perform request smuggling (Section 11.2) or response splitting
        // (Section 11.1) and ought to be handled as an error.

        Assertions.assertThrowsExactly(BadRequestException.class, () ->
                invokeParseBody("", "", null, InputStream.nullInputStream(), 0));
    }


    @Test
    void testParseBody1() throws Exception {

        // 4. If a Transfer-Encoding header field is present in a request and the chunked
        // transfer coding is not the final encoding,
        // the message body length cannot be determined reliably; the server MUST respond with
        // the 400 (Bad Request) status code and then close the connection.

        Assertions.assertThrowsExactly(BadRequestException.class, () -> invokeParseBody(null,
                "compress, deflate, gzip", null, InputStream.nullInputStream(), 0));
    }


    @Test
    void testParseBody2() throws Exception {

        // 4. If a Transfer-Encoding header field is present and the chunked transfer coding
        // (Section 7.1) is the final encoding,
        // the message body length is determined by reading and decoding the chunked data until
        // the transfer coding indicates the data is complete.

        final InputStream inputStream = new ByteArrayInputStream(TestConstants.CHUNKED_TEST_FILE_1_BYTES);
        final HTTPDecoderRegistry httpDecoderRegistry = new HTTPDecoderRegistry() {
            @Override
            public Optional<TransferCodingDecoder> getTransferCodingDecoder(String name) {
                if (!"chunked".equals(name))
                    return Optional.empty();

                return Optional.of(new ChunkedTransferCodingDecoder(
                        TestConstants.TEST_FILE_1_BYTES_LENGTH,
                        0,
                        100,
                        500,
                        100,
                        500,
                        HTTP1_1RequestParserTest.class.getCanonicalName(),
                        null,
                        null
                ));
            }

            @Override
            public Optional<ContentCodingDecoder> getContentCodingDecoder(String name) {
                return Optional.empty();
            }
        };
        final InvokeParseBodyResult result = invokeParseBody(
                null,
                "chunked",
                httpDecoderRegistry,
                inputStream,
                TestConstants.TEST_FILE_1_BYTES_LENGTH
        );
        Assertions.assertEquals(TestConstants.TEST_FILE_1_BYTES_LENGTH, result.contentLength());
        Assertions.assertTrue(result.contentInputStream().isPresent());
        Assertions.assertArrayEquals(
                TestConstants.TEST_FILE_1_BYTES,
                result.contentInputStream().get().readAllBytes()
        );
        Assertions.assertTrue(result.trailerSection().isPresent());
        Assertions.assertEquals(
                TestConstants.CHUNKED_TEST_FILE_1_TRAILER_FIELDS,
                result.trailerSection().get()
        );
    }


    @Test
    void testParseBody3() throws Exception {
        // 7. If this is a request message and neither Transfer-Encoding nor Content-Length header
        // field is present,
        // then the message body length is zero (no message body is present).

        Assertions.assertEquals(
                new InvokeParseBodyResult(0, Optional.empty(), Optional.empty()),
                invokeParseBody(null, null, null, InputStream.nullInputStream(), 0)
        );
    }


    @Test
    void testParseBody4() throws Exception {
        // 5. If a message is received without Transfer-Encoding and with an invalid
        // Content-Length header field,
        // then the message framing is invalid and the recipient MUST treat it as an
        // unrecoverable error, unless the field value can be successfully parsed as a
        // comma-separated list (Section 5.6.1 of [HTTP]), all values in the list are valid, and
        // all values in the list are the same (in which case, the message is processed with
        // that single value used as the Content-Length field value). If the unrecoverable error
        // is in a request message, the server MUST respond with a 400 (Bad Request) status code
        // and then close the connection.

        Assertions.assertThrowsExactly(BadRequestException.class, () ->
                invokeParseBody("", null, null, InputStream.nullInputStream(), 1024));
        Assertions.assertThrowsExactly(BadRequestException.class, () ->
                invokeParseBody(" ", null, null, InputStream.nullInputStream(), 1024));
        Assertions.assertThrowsExactly(BadRequestException.class, () ->
                invokeParseBody("abc", null, null, InputStream.nullInputStream(), 1024));
        Assertions.assertThrowsExactly(BadRequestException.class, () ->
                invokeParseBody("a, b, c", null, null, InputStream.nullInputStream(), 1024));
        Assertions.assertThrowsExactly(BadRequestException.class, () ->
                invokeParseBody("a, b, ", null, null, InputStream.nullInputStream(), 1024));
        Assertions.assertThrowsExactly(BadRequestException.class, () ->
                invokeParseBody("1, 2, 3", null, null, InputStream.nullInputStream(), 1024));

        Assertions.assertDoesNotThrow(() ->
                invokeParseBody("1, 1,", null, null, InputStream.nullInputStream(), 1024));
        Assertions.assertDoesNotThrow(() ->
                invokeParseBody("1, 1,  ", null, null, InputStream.nullInputStream(), 1024));
        Assertions.assertDoesNotThrow(() ->
                invokeParseBody("100, 100, 100", null, null, InputStream.nullInputStream(), 100));
    }


    @Test
    void testParseBody5() throws Exception {
        // 6. If a valid Content-Length header field is present without Transfer-Encoding,
        // its decimal value defines the expected message body length in octets. If the sender
        // closes the connection or the recipient times out before the indicated number of octets
        // are received, the recipient MUST consider the message to be incomplete and close the
        // connection.

        Assertions.assertDoesNotThrow(() ->
                invokeParseBody("1024", null, null, InputStream.nullInputStream(), 1024));
        Assertions.assertThrowsExactly(ContentTooLargeException.class, () ->
                invokeParseBody("1024", null, null, InputStream.nullInputStream(), 1023));
    }


}
