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

import personal.mrxdhlxpb.server.https.error.concrete.server.InternalServerErrorException;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * @author mrxdhlxpb
 */
public class HTTP1_1ResponseMessageBodyGeneratorImpl
        implements HTTP1_1ResponseMessageBodyGenerator {
    /**
     *
     * @return {@code new String[0]}
     */
    @Override
    public String[] getTransferEncodingChain() {
        return new String[0];
    }

    /**
     * This implementation does not apply any transfer encoding to the content. Therefore,
     * the message body is identical to the content.
     * <p> This method ignores {@code trailerSection}.
     *
     * @param contentInputStream the input stream from which the content is read
     * @param contentLength to determine the content
     * @param trailerSection ignored
     * @param destination the output stream into which the message body is written
     * @throws InternalServerErrorException if {@code contentLength} is positive and end of stream
     *                                      is detected before {@code contentLength} octets have
     *                                      been read from {@code contentInputStream}, or<br>
     *                                      to wrap an {@code IOException}
     */
    @Override
    public void generateResponseMessageBody(InputStream contentInputStream,
                                            int contentLength,
                                            Fields trailerSection,
                                            OutputStream destination)
            throws InternalServerErrorException {
        if (contentLength == 0)
            return;

        try {
            if (contentLength > 0) {
                byte[] contentOctets = contentInputStream.readNBytes(contentLength);
                if (contentOctets.length != contentLength)
                    throw new InternalServerErrorException(new IllegalArgumentException());
                destination.write(contentOctets);
            } else {
                contentInputStream.transferTo(destination);
            }
            destination.flush();
        } catch (IOException ioException) {
            throw new InternalServerErrorException(ioException);
        }
    }

}
