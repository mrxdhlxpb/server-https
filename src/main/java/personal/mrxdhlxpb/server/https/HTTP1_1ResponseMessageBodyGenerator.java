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

import java.io.InputStream;
import java.io.OutputStream;

/**
 * @author mrxdhlxpb
 */
public interface HTTP1_1ResponseMessageBodyGenerator {

    String[] getTransferEncodingChain();

    /**
     * Generates the message body for an HTTP/1.1 response message.
     * <p> The message body, as defined in <em>RFC 9112: HTTP/1.1 Section 6. Message Body</em>,
     * is either identical to the content, if no transfer coding is applied, or obtained
     * by applying the transfer codings to the content. What transfer codings are applied is
     * determined by implementation.
     * <p> If the implementation does not apply any transfer encoding, this method writes the
     * content, which is determined in a manner as described below, to {@code destination}.
     * Otherwise, the method applies the encoding transformation to the determined content,
     * and then writes the result to {@code destination}.
     * <p> If {@code contentLength} is positive, the content is determined by reading
     * {@code contentLength} octets from {@code contentInputStream}. If end of stream is
     * detected before {@code contentLength} octets have been read, an exception will be
     * thrown. This method blocks until {@code contentLength} octets have been read, end of
     * stream is detected or an exception is thrown.
     * <p> If {@code contentLength} is zero, the response message will not contain a message
     * body, i.e. no octet will be written into {@code destination}.
     * <p> If {@code contentLength} is negative, the content is determined by reading from
     * {@code contentInputStream} until end of stream is detected. This method blocks until
     * end of stream is detected or an exception is thrown.
     * <p> {@code trailerSection} will be ignored unless a trailer section is allowed to be
     * sent.
     *
     * @param contentInputStream the input stream from which the content is read
     * @param contentLength to determine the content
     * @param trailerSection the trailer section, if any
     * @param destination the output stream into which the message body is written
     * @throws InternalServerErrorException if {@code contentLength} is positive and end of stream
     *                                      is detected before {@code contentLength} octets have
     *                                      been read from {@code contentInputStream}, or<br>
     *                                      to wrap an {@code IOException}
     */
    void generateResponseMessageBody(InputStream contentInputStream,
                                     int contentLength,
                                     Fields trailerSection,
                                     OutputStream destination)
        throws InternalServerErrorException;

}
