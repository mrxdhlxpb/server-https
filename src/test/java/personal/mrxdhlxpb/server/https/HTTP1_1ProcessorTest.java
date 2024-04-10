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
import personal.mrxdhlxpb.server.https.error.concrete.client.GoneException;
import personal.mrxdhlxpb.server.https.error.concrete.client.NotFoundException;
import personal.mrxdhlxpb.server.https.error.concrete.server.InternalServerErrorException;

import java.io.*;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.Set;

/**
 * @author mrxdhlxpb
 */
public class HTTP1_1ProcessorTest {

    @Test
    void testProcess() throws Exception {
        final String requestMessage = """
                GET /test HTTP/1.1\r
                Host: localhost\r
                Connection: keep-alive\r
                \r
                """;
        final String expectedResponseMessage = """
                HTTP/1.1 404\r
                host: localhost\r
                connection: close\r
                content-length: 9\r
                \r
                not found""";
        final boolean expectedReturnValue = false;

        final ByteArrayInputStream requestInputStream = new ByteArrayInputStream(requestMessage.
                getBytes(StandardCharsets.US_ASCII));
        final ByteArrayOutputStream responseOutputStream = new ByteArrayOutputStream();

        final HTTP1_1Processor processor = new HTTP1_1Processor(
                new Configuration() {
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
                            public InternalResource getInternalResource(
                                    InternalResourceIdentifier internalResourceIdentifier
                            )
                                    throws NotFoundException, GoneException {
                                throw new NotFoundException();
                            }
                        };
                    }

                    @Override
                    public HTTPDecoderRegistry getHTTPDecoderRegistry() {
                        return null;
                    }

                    @Override
                    public HttpErrorHandlerRegistry getHttpErrorHandlerRegistry() {
                        return new HttpErrorHandlerRegistry() {
                            @Override
                            public HttpErrorHandler getHttpErrorHandler(
                                    Class<? extends HttpErrorException> httpErrorExceptionClass
                            ) throws InternalServerErrorException {
                                if (httpErrorExceptionClass == NotFoundException.class)
                                    return (error, response) -> {
                                        response.setStatusCode((short) 404);
                                        response.getHeaderSection().set("host", "localhost");
                                        response.getHeaderSection().set("connection", "close");
                                        response.getHeaderSection().set("content-length", "9");
                                        response.setContentInputStream(new ByteArrayInputStream(
                                                "not found".getBytes()));
                                        response.setContentLength(9);
                                    };
                                throw new InternalServerErrorException("no appropriate handler " +
                                        "available");
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
                                return 1000;
                            }

                            @Override
                            public int getMaximumRequestContentLength() {
                                return 100;
                            }

                            @Override
                            public HTTP1_1ResponseMessageBodyGenerator
                            getHttpErrorHTTP1_1ResponseMessageBodyGenerator() {
                                return new HTTP1_1ResponseMessageBodyGeneratorImpl();
                            }
                        };
                    }
                },
                new FakeSocket(requestInputStream, responseOutputStream)
        );

        Assertions.assertEquals(
                expectedReturnValue,
                processor.process()
        );

        Assertions.assertEquals(
                expectedResponseMessage,
                responseOutputStream.toString()
        );

    }

    @Test
    void testIsPersistent() throws Exception {
        final HTTP1_1Processor instance = new HTTP1_1Processor(null, new FakeSocket(null, null));

        Method isPersistentMethod = instance.getClass().getDeclaredMethod("isPersistent",
                HTTPRequest.class);
        isPersistentMethod.setAccessible(true);

        final boolean isPersistent0 = (boolean) isPersistentMethod.invoke(instance,
                createHTTPRequest(ProtocolVersion.HTTP__1_1, "keep-alive"));

        final boolean isPersistent1 = (boolean) isPersistentMethod.invoke(instance,
                createHTTPRequest(ProtocolVersion.HTTP__1_1, "close"));

        final boolean isPersistent2 = (boolean) isPersistentMethod.invoke(instance,
                createHTTPRequest(ProtocolVersion.HTTP__1_1, null));

        final boolean isPersistent3 = (boolean) isPersistentMethod.invoke(instance,
                createHTTPRequest(ProtocolVersion.HTTP__1_0, "keep-alive"));

        final boolean isPersistent4 = (boolean) isPersistentMethod.invoke(instance,
                createHTTPRequest(ProtocolVersion.HTTP__1_0, "close"));

        final boolean isPersistent5 = (boolean) isPersistentMethod.invoke(instance,
                createHTTPRequest(ProtocolVersion.HTTP__1_0, null));

        final boolean isPersistent6 = (boolean) isPersistentMethod.invoke(instance,
                createHTTPRequest(new ProtocolVersion((byte) 10, (byte) 10), "keep-alive"));

        final boolean isPersistent7 = (boolean) isPersistentMethod.invoke(instance,
                createHTTPRequest(new ProtocolVersion((byte) 10, (byte) 10), "close"));

        final boolean isPersistent8 = (boolean) isPersistentMethod.invoke(instance,
                createHTTPRequest(new ProtocolVersion((byte) 10, (byte) 10), null));

        final boolean isPersistent9 = (boolean) isPersistentMethod.invoke(instance,
                createHTTPRequest(new ProtocolVersion((byte) 10, (byte) 10), "Close"));

        Assertions.assertTrue(isPersistent0 && isPersistent2 && isPersistent3 && isPersistent6 &&
                isPersistent8 && isPersistent9);
        Assertions.assertFalse(isPersistent1 || isPersistent4 || isPersistent5 || isPersistent7);
    }

    /**
     * creates an {@code HTTPRequest} object with specified protocol version and
     * "Connection" header field.
     * @param protocolVersion the specified protocol version
     * @param connectionHeaderFieldValueString the specified "Connection" header field, or
     *                                         {@code null} if not present
     * @return the object
     */
    private HTTPRequest createHTTPRequest(ProtocolVersion protocolVersion,
                                          String connectionHeaderFieldValueString) {
        final Fields headerSection = new Fields();
        if (connectionHeaderFieldValueString != null)
            headerSection.set("Connection", connectionHeaderFieldValueString);

        return new HTTPRequest(
                new RequestMessageControlData(null, null, protocolVersion),
                headerSection,
                Optional.empty(),
                Optional.empty(),
                0
        );
    }

    private static final class FakeSocket extends Socket {

        private final InputStream inputStream;
        private final OutputStream outputStream;

        public FakeSocket(InputStream inputStream, OutputStream outputStream) {
            super();
            this.inputStream = inputStream;
            this.outputStream = outputStream;
        }

        @Override
        public InputStream getInputStream() {
            return inputStream;
        }

        @Override
        public OutputStream getOutputStream() {
            return outputStream;
        }
    }
}
