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
import personal.mrxdhlxpb.server.https.configuration.Configuration;
import personal.mrxdhlxpb.server.https.configuration.HTTP1_1Configuration;
import personal.mrxdhlxpb.server.https.configuration.NetworkConfiguration;
import personal.mrxdhlxpb.server.https.decoder.HTTPDecoderRegistry;
import personal.mrxdhlxpb.server.https.error.HttpErrorException;
import personal.mrxdhlxpb.server.https.error.concrete.client.BadRequestException;
import personal.mrxdhlxpb.server.https.error.concrete.server.InternalServerErrorException;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

/**
 * @author mrxdhlxpb
 */
public class HTTP1_1ResponseGeneratorTest {

    @Test
    void test0() throws Exception {
        final HTTPRequest httpRequest = new HTTPRequest(
                new RequestMessageControlData(
                        null,
                        new InternalResource() {
                            @Override
                            public InternalResourceIdentifier getInternalResourceIdentifier() {
                                return null;
                            }

                            @Override
                            public HTTPRequestHandler getHTTPRequestHandler() {
                                return (request, response) -> {
                                    response.getHeaderSection().set("connection", "keep-alive");
                                    response.getHeaderSection().set("content-length", "5");

                                    final ByteArrayInputStream responseContentInputStream =
                                            new ByteArrayInputStream("hello"
                                                    .getBytes(StandardCharsets.US_ASCII));

                                    response.setContentInputStream(responseContentInputStream);
                                    response.setContentLength(5);

                                    response.setStatusCode((short) 200);
                                };
                            }

                            @Override
                            public HTTP1_1ResponseMessageBodyGenerator
                            getHTTP1_1ResponseMessageBodyGenerator() {
                                return new HTTP1_1ResponseMessageBodyGeneratorImpl();
                            }
                        },
                        null
                ),
                null,
                Optional.empty(),
                Optional.empty(),
                0
        );
        final Either<HTTPRequest, HttpErrorException> eitherHTTPRequestOrHttpErrorException =
                Either.of(httpRequest, null);
        final String expectedResponseMessage = """
                HTTP/1.1 200\r
                connection: keep-alive\r
                content-length: 5\r
                \r
                hello""";

        final Configuration configuration = new Configuration() {
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
                return null;
            }

            @Override
            public HttpErrorHandlerRegistry getHttpErrorHandlerRegistry() {
                return null;
            }

            @Override
            public HTTP1_1Configuration getHTTP1_1Configuration() {
                return null;
            }
        };
        final ByteArrayOutputStream responseOutputStream = new ByteArrayOutputStream();
        final HTTP1_1ResponseGenerator http1_1ResponseGenerator = new HTTP1_1ResponseGenerator(
                configuration,
                responseOutputStream);

        http1_1ResponseGenerator.generateResponse(eitherHTTPRequestOrHttpErrorException);

        Assertions.assertEquals(
                expectedResponseMessage,
                responseOutputStream.toString()
        );
    }

    @Test
    void test1() {
        final HttpErrorException httpErrorException = new BadRequestException("test 400");
        final String expectedResponseMessage = """
                HTTP/1.1 400\r
                connection: close\r
                content-length: 8\r
                \r
                test 400""";

        final Configuration configuration = new Configuration() {
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
                return null;
            }

            @Override
            public HttpErrorHandlerRegistry getHttpErrorHandlerRegistry() {
                return new HttpErrorHandlerRegistry() {
                    @Override
                    public HttpErrorHandler getHttpErrorHandler(Class<? extends HttpErrorException>
                                                                        httpErrorExceptionClass)
                            throws InternalServerErrorException {
                        if (httpErrorExceptionClass == BadRequestException.class)
                            return (error, response) -> {
                                final int contentLength = error.getMessage().length();
                                final String contentLengthString = String.valueOf(contentLength);

                                response.setStatusCode((short) 400);
                                response.getHeaderSection().set("connection", "close");
                                response.getHeaderSection().set(
                                        "content-length",
                                        contentLengthString
                                );
                                response.setContentInputStream(new ByteArrayInputStream(error
                                        .getMessage().getBytes()));
                                response.setContentLength(contentLength);
                            };

                        throw new InternalServerErrorException("No appropriate handler present.");
                    }
                };
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
                        return 0;
                    }

                    @Override
                    public HTTP1_1ResponseMessageBodyGenerator
                    getHttpErrorHTTP1_1ResponseMessageBodyGenerator() {
                        return new HTTP1_1ResponseMessageBodyGeneratorImpl();
                    }
                };
            }
        };

        final ByteArrayOutputStream responseContentOutputStream = new ByteArrayOutputStream();

        final HTTP1_1ResponseGenerator http1_1ResponseGenerator = new HTTP1_1ResponseGenerator(
                configuration,
                responseContentOutputStream
        );

        http1_1ResponseGenerator.generateResponse(httpErrorException);

        Assertions.assertEquals(
                expectedResponseMessage,
                responseContentOutputStream.toString()
        );
    }

}
