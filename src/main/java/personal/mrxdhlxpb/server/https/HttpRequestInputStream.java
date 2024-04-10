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

import personal.mrxdhlxpb.server.https.error.concrete.client.BadRequestException;

import java.io.IOException;
import java.io.InputStream;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.util.function.Supplier;

/**
 * This class provides methods particularly used for the
 * convenience of reading HTTP request messages.
 *
 * @author mrxdhlxpb
 */
public class HttpRequestInputStream extends LimitedRecordingBufferedInputStream {
    private static final byte CR = 0XD;

    private static final byte LF = 0XA;

    private static final byte SP = 0X20;

    public HttpRequestInputStream(InputStream in) {
        super(in);
    }

    public HttpRequestInputStream(InputStream in, Void placeholder) {
        super(in, placeholder);
    }

    public HttpRequestInputStream(InputStream in, int size) {
        super(in, size);
    }

    public HttpRequestInputStream(InputStream in, boolean recordingEnabled) {
        super(in, recordingEnabled);
    }

    public HttpRequestInputStream(InputStream in, boolean recordingEnabled, int size) {
        super(in, recordingEnabled, size);
    }

    /**
     * <p> Keeps reading into the given byte array from the input stream until <em>CRLF</em> is met,
     * end of stream is detected or an exception is thrown (a {@code read()} call throws an
     * exception, or this method throws an exception produced by {@code exceptionSupplier}). This
     * method blocks if input data is not yet available.
     * <p> The k-th byte read is stored into element {@code bytes[k-1]}. <em>CRLF</em> will not be
     * stored into {@code bytes}.
     * <p> When the method returns when <em>CRLF</em> is met, to describe the position of the input
     * stream, the next byte to be read from the input stream is the byte following <em>LF</em>.
     * <p> Each bare CR (a CR character not immediately followed by LF) is replaced with SP.
     *
     * @param <X>               Type of the exception to be thrown
     * @param bytes             the given byte array into which the input stream reads
     * @param exceptionSupplier the supplying function that produces an exception to be thrown
     * @return the number of elements of {@code bytes} affected
     * @throws X                    if {@code bytes} cannot contain all the data required
     * @throws IOException          if an I/O error occurs. Note:
     *                              {@link SocketTimeoutException} is thrown if the input
     *                              stream reads from a socket, and a timeout has
     *                              occurred on a socket read.
     * @throws NullPointerException if {@code bytes} is {@code null}
     */
    public <X extends Throwable> int readLine(byte[] bytes, Supplier<? extends X> exceptionSupplier)
            throws IOException, X {
        int num = 0;
        int latestCRNum = -5; // value of 'num' when the latest CR was detected
        int affected = 0;
        while (true) {
            int data = read();
            num++;
            // 'num' equals to number of bytes read,
            // or number of bytes read + 1 if data == -1
            if (data == -1) {
                // end of stream detected
                // if previous byte is CR
                // bytes[num - 2] = SP
                if (latestCRNum == num - 1) {
                    affected++;
                    bytes[latestCRNum - 1] = SP;
                }
                return affected;
            } else if (data == CR) {
                // CR detected
                // let latestCRNum = num
                latestCRNum = num;
                // do not affect 'bytes'
                // if number of bytes read > bytes.length:
                // number of bytes read == bytes.length + 1,
                // let latestCRNum = num, do not affect 'bytes'
                // number of bytes read == bytes.length + 2, throw exceptionSupplier.get()
                // number of bytes read > bytes.length + 2, impossible
                if (num == bytes.length + 2) {
                    throw exceptionSupplier.get();
                }
            } else if (data == LF) {
                // LF detected
                // if number of bytes read > bytes.length && previous byte is not CR,
                // then throw exceptionSupplier.get()
                // if previous byte is CR, return; or bytes[num - 1] = LF
                if (latestCRNum == num - 1) {
                    return affected;
                } else {
                    if (num > bytes.length)
                        throw exceptionSupplier.get();
                    affected++;
                    bytes[num - 1] = LF;
                }
            } else {
                // normal data
                // if number of bytes read > bytes.length, throw exceptionSupplier.get();
                // bytes[num - 1] = (byte) data;
                // if previous byte is CR, bytes[num - 2] = SP;
                if (num > bytes.length)
                    throw exceptionSupplier.get();
                affected++;
                bytes[num - 1] = (byte) data;
                if (latestCRNum == num - 1) {
                    affected++;
                    bytes[num - 2] = SP;
                }
            }
        }
    }


    /**
     * <p> Keeps reading into the given byte array from the input stream until <em>CRLF</em> is met,
     * end of stream is detected or an exception is thrown (a {@code read()} call throws an
     * exception, or this method throws a {@code CannotContainException}). This method blocks if
     * input data is not yet available.
     * <p> The k-th byte read is stored into element {@code bytes[k-1]}. <em>CRLF</em> will not be
     * stored into {@code bytes}.
     * <p> When the method returns when <em>CRLF</em> is met, to describe the position of the input
     * stream, the next byte to be read from the input stream is the byte following <em>LF</em>.
     * <p> Each bare CR (a CR character not immediately followed by LF) is replaced with SP.
     *
     * @param bytes the given byte array into which the input stream reads
     * @return the number of elements of {@code bytes} affected
     * @throws CannotContainException if {@code bytes} cannot contain all the data required
     * @throws IOException            if an I/O error occurs. Note:
     *                                {@link SocketTimeoutException} is thrown if the input
     *                                stream reads from a socket, and a timeout has
     *                                occurred on a socket read.
     * @throws NullPointerException   if {@code bytes} is {@code null}
     */
    public int readLine(byte[] bytes) throws IOException, CannotContainException {
        return readLine(bytes, CannotContainException::new);
    }


    /**
     * Reads the next line (separated by CRLF) from the input stream,
     * maximum length of which is limited.
     * @param maximumFieldLineLength maximum length limit
     * @return the string of the next line, decoded using US-ASCII
     * @throws IOException if an I/O error occurs
     * @throws BadRequestException if the length of the next line exceeds the limit
     */
    private String readFieldLine(int maximumFieldLineLength)
            throws IOException, BadRequestException {
        final byte[] b = new byte[maximumFieldLineLength];
        return new String(
                b,
                0,
                readLine(b, () -> new BadRequestException("length of field line exceeds limit")),
                StandardCharsets.US_ASCII
        );
    }


    /**
     * reads {@code *( field-line CRLF ) CRLF}
     * @return fields
     * @throws IOException if an i/o error occurs
     * @throws BadRequestException if length of a field line exceeds limit, or
     *                             length of the header or trailer section exceeds limit
     */
    public Fields readFields(int maximumFieldLineLength, int maximumSectionLength)
            throws IOException, BadRequestException {
        enableLimit(maximumSectionLength + 2);
        try {
            return new Fields(() -> readFieldLine(maximumFieldLineLength), String::isEmpty);
        } catch (TryToReadOutOfLimitException e) {
            throw new BadRequestException("length of header section exceeds limit");
        } finally {
            disableLimit();
        }
    }


    /**
     * reads two bytes and throws an exception unless the first byte read is <em>CR</em>, and
     * the second byte read is <em>LF</em>.
     */
    public <X extends Throwable> void requireCRLF(Supplier<? extends X> supplier)
            throws X, IOException {
        if (read() != CR || read() != LF)
            throw supplier.get();
    }


    /**
     * @see HttpRequestInputStream#readLine(byte[])
     */
    public static final class CannotContainException extends Exception {}

}

