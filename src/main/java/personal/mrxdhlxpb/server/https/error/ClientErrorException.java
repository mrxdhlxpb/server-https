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

/**
 * @author mrxdhlxpb
 */
public non-sealed abstract class ClientErrorException extends HttpErrorException {

    protected ClientErrorException() {
    }

    protected ClientErrorException(String message) {
        super(message);
    }

    protected ClientErrorException(String message, Throwable cause) {
        super(message, cause);
    }

    protected ClientErrorException(Throwable cause) {
        super(cause);
    }

    protected ClientErrorException(boolean closeConnection) {
        super(closeConnection);
    }

    protected ClientErrorException(boolean closeConnection, String message) {
        super(closeConnection, message);
    }

    protected ClientErrorException(boolean closeConnection, String message, Throwable cause) {
        super(closeConnection, message, cause);
    }

    protected ClientErrorException(boolean closeConnection, Throwable cause) {
        super(closeConnection, cause);
    }
}
