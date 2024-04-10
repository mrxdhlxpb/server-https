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
package personal.mrxdhlxpb.server.https.decoder;

import personal.mrxdhlxpb.server.https.*;
import personal.mrxdhlxpb.server.https.error.concrete.client.BadRequestException;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.regex.Pattern;

/**
 * This class implements the function of decoding the chunked transfer coding.
 *
 * @author mrxdhlxpb
 */
@CompliantWith("RFC 9110: HTTP Semantics")
@CompliantWith("RFC 9112: HTTP/1.1 Section 7.1. Chunked Transfer Coding")
class ChunkedTransferCodingDecoderImpl implements Closeable {

    private static final String BWS_REGEX = "[\\x20\\x09]*";

    private static final String CHUNK_SIZE_CHUNK_EXT_DELIMITER_REGEX = ";" + BWS_REGEX;

    private static final Pattern CHUNK_SIZE_CHUNK_EXT_DELIMITER_PATTERN = Pattern
            .compile(CHUNK_SIZE_CHUNK_EXT_DELIMITER_REGEX);

    private final int memoryBufferSize;

    private final int maximumTempFileSize;

    private final int maximumChunkLineLength;

    private final int maximumChunkSize;

    private final int maximumTrailerFieldLineLength;

    private final int maximumTrailerSectionSize;

    private final String tempFilePrefix;

    private final String tempFileSuffix;

    private final File tempFileDirectory;

    private final HttpRequestInputStream httpRequestInputStream;

    private final byte[] buf;

    /**
     * count of bytes stored (in buf or in temp file)
     */
    private int count;

    private BufferedOutputStream outputStreamToTempFile;

    private FileInputStream inputStreamFromTempFile;

    private Fields trailerSection;

    private int contentLength = 0;

    ChunkedTransferCodingDecoderImpl(int memoryBufferSize,
                                     int maximumTempFileSize,
                                     int maximumChunkLineLength,
                                     int maximumChunkSize,
                                     int maximumTrailerFieldLineLength,
                                     int maximumTrailerSectionSize,
                                     String tempFilePrefix,
                                     String tempFileSuffix,
                                     File tempFileDirectory,
                                     HttpRequestInputStream httpRequestInputStream) {
        this.memoryBufferSize = memoryBufferSize;
        this.maximumTempFileSize = maximumTempFileSize;
        this.maximumChunkLineLength = maximumChunkLineLength;
        this.maximumChunkSize = maximumChunkSize;
        this.maximumTrailerFieldLineLength = maximumTrailerFieldLineLength;
        this.maximumTrailerSectionSize = maximumTrailerSectionSize;
        this.httpRequestInputStream = httpRequestInputStream;
        this.tempFilePrefix = tempFilePrefix;
        this.tempFileSuffix = tempFileSuffix;
        this.tempFileDirectory = tempFileDirectory;
        this.buf = new byte[memoryBufferSize];
    }


    /**
     * Reads from {@code httpRequestInputStream} and decodes the chunked transfer coding. The result
     * can be obtained from subsequent calls to {@code getEitherBufOrInputStreamFromTempFile()},
     * {@code getTrailerSection()} and {@code getContentLength()}.
     *
     * @throws IOException if an i/o error occurs
     * @throws BadRequestException if we cannot parse the chunked transfer coding
     */
    void decode() throws IOException, BadRequestException {
        //  chunked-body   = *chunk
        //                   last-chunk
        //                   trailer-section
        //                   CRLF

//        chunk          = chunk-size [ chunk-ext ] CRLF
//                         chunk-data CRLF
//        chunk-size     = 1*HEXDIG
//        last-chunk     = 1*("0") [ chunk-ext ] CRLF
//
//        chunk-data     = 1*OCTET ; a sequence of chunk-size octets

        while (true) {
            int chunkSize = readChunkLine();
            if (chunkSize == 0)
                break; // last-chunk
            byte[] chunkData = httpRequestInputStream.readNBytes(chunkSize);
            if (chunkData.length != chunkSize)
                throw new BadRequestException("unable to parse the chunked transfer coding");
            store(chunkData);
            httpRequestInputStream.requireCRLF(() ->
                    new BadRequestException("unable to parse the chunked transfer coding"));
            contentLength += chunkSize;
        }
        trailerSection = httpRequestInputStream
                .readFields(maximumTrailerFieldLineLength, maximumTrailerSectionSize);

        if (outputStreamToTempFile != null)
            outputStreamToTempFile.flush();
    }


    /**
     * Reads the first line of a chunk, referred to as the "chunk line".
     * Any chunk extension is ignored.
     *
     * @return chunk size
     * @throws IOException         if an I/O error occurs
     * @throws BadRequestException if we want to respond with a 400 status code
     */
    private int readChunkLine() throws IOException, BadRequestException {
        byte[] bytes = new byte[maximumChunkLineLength];
        int len;
        try {
            len = httpRequestInputStream.readLine(bytes);
        } catch (HttpRequestInputStream.CannotContainException e) {
            throw new BadRequestException("unable to parse the chunked transfer coding");
        }
        String chunkLine = new String(bytes, 0, len, StandardCharsets.US_ASCII);

        // chunk-line = chunk-size [ chunk-ext ] CRLF

        // chunk-ext      = *( BWS ";" BWS chunk-ext-name
        //                     [ BWS "=" BWS chunk-ext-val ] )

        // chunk-ext-name = token
        // chunk-ext-val  = token / quoted-string

        // Note: As defined in RFC 9110: HTTP Semantics Section 5.6.3. Whitespace,
        // the BWS rule is used where the grammar allows optional whitespace only for historical
        // reasons. A recipient MUST parse for such bad whitespace and remove it before interpreting
        // the protocol element.

        String[] chunkSizeChunkExt = CHUNK_SIZE_CHUNK_EXT_DELIMITER_PATTERN.split(chunkLine, 0);

        // ignore any chunk extension

        String chunkSize = chunkSizeChunkExt[0];

        // Note: chunk-size is a string of HEX digits

        final int chunkSizeInt;
        try {
            chunkSizeInt = Integer.parseInt(chunkSize, 16);
        } catch (NumberFormatException e) {
            throw new BadRequestException("unable to parse the chunked transfer coding");
        }
        if (chunkSizeInt > maximumChunkSize) {
            throw new BadRequestException("unable to parse the chunked transfer coding");
        }
        if (chunkSizeInt < 0) {
            throw new BadRequestException("unable to parse the chunked transfer coding");
        }
        return chunkSizeInt;
    }


    /**
     * Stores the given byte array, either in {@code buf}, if {@code buf} can hold all the data,
     * or in a temporary file.
     * @param b the given byte array
     * @throws IOException if an i/o error occurs
     * @throws BadRequestException if we cannot parse the chunked transfer coding
     */
    private void store(byte[] b) throws IOException, BadRequestException {
        final long preventOverflow = (long) count + b.length;
        if (preventOverflow > Integer.MAX_VALUE)
            throw new BadRequestException("unable to parse the chunked transfer coding");
        if (preventOverflow <= memoryBufferSize) {
            System.arraycopy(b, 0, buf, count, b.length);
            count += b.length;
        } else if (preventOverflow > memoryBufferSize && preventOverflow <= maximumTempFileSize) {
            if (inputStreamFromTempFile == null) {
                // TODO: temp file security
                File tempFile = File.createTempFile(
                        tempFilePrefix,
                        tempFileSuffix,
                        tempFileDirectory);
                tempFile.deleteOnExit();
                inputStreamFromTempFile = new FileInputStream(tempFile);
                outputStreamToTempFile = new BufferedOutputStream(new FileOutputStream(tempFile));
                outputStreamToTempFile.write(buf, 0, count);

                // TODO: buf = new byte[0] ?
            }
            outputStreamToTempFile.write(b);
            count += b.length;
        } else {
            throw new BadRequestException("unable to parse the chunked transfer coding");
        }
    }

    Either<byte[], FileInputStream> getEitherBufOrInputStreamFromTempFile() {
        return inputStreamFromTempFile == null ?
                Either.of(Arrays.copyOf(buf, count), null) :
                Either.of(null, inputStreamFromTempFile);
    }


    Fields getTrailerSection() {
        return trailerSection;
    }


    int getContentLength() {
        return contentLength;
    }

    /**
     * closes {@code outputStreamToTempFile}
     *
     * @throws IOException if an I/O error occurs
     */
    @Override
    public void close() throws IOException {
        if (outputStreamToTempFile != null)
            outputStreamToTempFile.close();
    }

    static DecodeResult decode(int memoryBufferSize,
                               int maximumTempFileSize,
                               int maximumChunkLineLength,
                               int maximumChunkSize,
                               int maximumTrailerFieldLineLength,
                               int maximumTrailerSectionSize,
                               String tempFilePrefix,
                               String tempFileSuffix,
                               File tempFileDirectory,
                               HttpRequestInputStream httpRequestInputStream)
            throws IOException, BadRequestException {
        try (ChunkedTransferCodingDecoderImpl chunkedDecoder = new ChunkedTransferCodingDecoderImpl(
                memoryBufferSize,
                maximumTempFileSize,
                maximumChunkLineLength,
                maximumChunkSize,
                maximumTrailerFieldLineLength,
                maximumTrailerSectionSize,
                tempFilePrefix,
                tempFileSuffix,
                tempFileDirectory,
                httpRequestInputStream)) {
            chunkedDecoder.decode();
            InputStream inputStream = (InputStream) chunkedDecoder
                    .getEitherBufOrInputStreamFromTempFile()
                    .map(ByteArrayInputStream::new, null)
                    .get();
            Fields trailerSection = chunkedDecoder.getTrailerSection();
            int contentLength = chunkedDecoder.getContentLength();
            return new DecodeResult(inputStream, trailerSection, contentLength);
        }
    }

    record DecodeResult(InputStream inputStream, Fields trailerSection, int contentLength) {}

}