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
public non-sealed abstract class ServerErrorException extends HttpErrorException {
    protected ServerErrorException() {
    }

    protected ServerErrorException(String message) {
        super(message);
    }

    protected ServerErrorException(String message, Throwable cause) {
        super(message, cause);
    }

    protected ServerErrorException(Throwable cause) {
        super(cause);
    }

    protected ServerErrorException(boolean closeConnection) {
        super(closeConnection);
    }

    protected ServerErrorException(boolean closeConnection, String message) {
        super(closeConnection, message);
    }

    protected ServerErrorException(boolean closeConnection, String message, Throwable cause) {
        super(closeConnection, message, cause);
    }

    protected ServerErrorException(boolean closeConnection, Throwable cause) {
        super(closeConnection, cause);
    }
}
