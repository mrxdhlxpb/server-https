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
package personal.mrxdhlxpb.server.https.error;

import personal.mrxdhlxpb.server.https.HTTPRequest;

/**
 * Represents either a client error as defined in <em>RFC 9110: HTTP Semantics Section 15.5. Client
 * Error 4xx</em> or a server error as defined in <em>RFC 9110: HTTP Semantics Section 15.6. Server
 * Error 5xx</em>.
 * <p> We define <em>request parsing process</em> as the process by which the server tries to parse
 * the request message into an {@link HTTPRequest} object, whose result is either an
 * {@code HTTPRequest} object or an error.
 * <p> We define <em>response generation process</em> as the process by which the server tries to
 * generate the response message based on either an {@code HTTPRequest} object or an error.
 * <p> A <em>response generation process</em> may take either the result of a <em>request parsing
 * process</em> or an error occurs in another <em>response generation process</em> taking an
 * {@code HTTPRequest} object as the "argument" as the "argument". Any error's occurring in a
 * <em>response generation process</em> taking over an error terminates the program.
 *
 * @author mrxdhlxpb
 */
public sealed abstract class HttpErrorException
        extends Exception
        permits ClientErrorException, ServerErrorException {

    protected final boolean closeConnection;

    protected HttpErrorException() {
        this(true);
    }

    protected HttpErrorException(String message) {
        this(true, message);
    }

    protected HttpErrorException(String message, Throwable cause) {
        this(true, message, cause);
    }

    protected HttpErrorException(Throwable cause) {
        this(true, cause);
    }

    protected HttpErrorException(boolean closeConnection) {
        this.closeConnection = closeConnection;
    }

    protected HttpErrorException(boolean closeConnection, String message) {
        super(message);
        this.closeConnection = closeConnection;
    }

    protected HttpErrorException(boolean closeConnection, String message, Throwable cause) {
        super(message, cause);
        this.closeConnection = closeConnection;
    }

    protected HttpErrorException(boolean closeConnection, Throwable cause) {
        super(cause);
        this.closeConnection = closeConnection;
    }

    public boolean isCloseConnection() {
        return closeConnection;
    }
}