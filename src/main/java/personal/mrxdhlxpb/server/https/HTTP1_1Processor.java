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
import personal.mrxdhlxpb.server.https.error.HttpErrorException;
import personal.mrxdhlxpb.server.https.error.concrete.server.InternalServerErrorException;

import java.io.IOException;
import java.net.Socket;
import java.util.Optional;

/**
 * @author mrxdhlxpb
 */
@CompliantWith("RFC 9112: HTTP/1.1")
public class HTTP1_1Processor {

    private final HTTP1_1RequestParser http1_1RequestParser;

    private final HTTP1_1ResponseGenerator http1_1ResponseGenerator;

    public HTTP1_1Processor(Configuration configuration, Socket socket) throws IOException {
        this.http1_1RequestParser = new HTTP1_1RequestParser(
                configuration,
                socket.getInputStream());
        this.http1_1ResponseGenerator = new HTTP1_1ResponseGenerator(
                configuration,
                socket.getOutputStream());
    }

    /**
     * @return whether the connection should persist
     */
    public boolean process() {
        try (HTTPRequest httpRequest = http1_1RequestParser.parseRequest()) {
            http1_1ResponseGenerator.generateResponse(Either.of(httpRequest, null));
            return isPersistent(httpRequest);
        } catch (HttpErrorException httpErrorException) {
            http1_1ResponseGenerator.generateResponse(httpErrorException);
            return !httpErrorException.isCloseConnection();
        } catch (IOException ioException) {
            http1_1ResponseGenerator
                    .generateResponse(new InternalServerErrorException(ioException));
            return false;
        }
    }

    /**
     * Determines whether a connection is persistent in a manner consistent with
     * <em>RFC 9112: HTTP/1.1 Section 9.3. Persistence</em>.
     * @param httpRequest the request
     * @return whether a connection is persistent
     */
    private boolean isPersistent(HTTPRequest httpRequest) {
        final ProtocolVersion protocolVersion = httpRequest
                .requestMessageControlData()
                .protocolVersion();
        final Optional<String> connectionHeaderFieldValueStringOptional = httpRequest
                .headerSection()
                .getFieldValueString("Connection");

        // If the "close" connection option is present (Section 9.6), the connection will not
        // persist after the current response; else,

        if (connectionHeaderFieldValueStringOptional.isPresent()
                && connectionHeaderFieldValueStringOptional.get().equals("close"))
            return false;

        // If the received protocol is HTTP/1.1 (or later), the connection will persist after the
        // current response; else,

        if (!ProtocolVersion.HTTP__1_1.isLaterThan(protocolVersion))
            return true;

        // If the received protocol is HTTP/1.0, the "keep-alive" connection option is present,
        // either the recipient is not a proxy or the message is a response, and the recipient
        // wishes to honor the HTTP/1.0 "keep-alive" mechanism, the connection will persist after
        // the current response; otherwise,

        // The connection will close after the current response.

        return protocolVersion.equals(ProtocolVersion.HTTP__1_0)
                && connectionHeaderFieldValueStringOptional.isPresent()
                && connectionHeaderFieldValueStringOptional.get().equals("keep-alive");
    }

}
