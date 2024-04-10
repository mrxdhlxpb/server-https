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

import personal.mrxdhlxpb.server.https.Fields;
import personal.mrxdhlxpb.server.https.HttpRequestInputStream;
import personal.mrxdhlxpb.server.https.error.concrete.client.BadRequestException;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;

/**
 * Decodes the chunked transfer coding.
 *
 * @author mrxdhlxpb
 */
public final class ChunkedTransferCodingDecoder implements TransferCodingDecoder {

    private final int memoryBufferSize;

    private final int maximumTempFileSize;

    private final int maximumChunkLineLength;

    private final int maximumChunkSize;

    private final int maximumTrailerFieldLineLength;

    private final int maximumTrailerSectionSize;

    private final String tempFilePrefix;

    private final String tempFileSuffix;

    private final File tempFileDirectory;

    private ChunkedTransferCodingDecoderImpl.DecodeResult currentResult;

    public ChunkedTransferCodingDecoder(int memoryBufferSize,
                                        int maximumTempFileSize,
                                        int maximumChunkLineLength,
                                        int maximumChunkSize,
                                        int maximumTrailerFieldLineLength,
                                        int maximumTrailerSectionSize,
                                        String tempFilePrefix,
                                        String tempFileSuffix,
                                        File tempFileDirectory) {
        this.memoryBufferSize = memoryBufferSize;
        this.maximumTempFileSize = maximumTempFileSize;
        this.maximumChunkLineLength = maximumChunkLineLength;
        this.maximumChunkSize = maximumChunkSize;
        this.maximumTrailerFieldLineLength = maximumTrailerFieldLineLength;
        this.maximumTrailerSectionSize = maximumTrailerSectionSize;
        this.tempFilePrefix = tempFilePrefix;
        this.tempFileSuffix = tempFileSuffix;
        this.tempFileDirectory = tempFileDirectory;
    }

    /**
     * This method throws {@code IllegalStateException}.
     *
     * @deprecated the decode method of {@code ChunkedTransferCodingDecoder} only
     *             accept {@code HttpRequestInputStream}
     */
    @Override
    @Deprecated
    public ChunkedTransferCodingDecoder decode(InputStream encoded) {
        throw new IllegalStateException();
    }

    public ChunkedTransferCodingDecoder decode(HttpRequestInputStream encoded)
            throws IOException, BadRequestException {
        currentResult = ChunkedTransferCodingDecoderImpl.decode(
                memoryBufferSize,
                maximumTempFileSize,
                maximumChunkLineLength,
                maximumChunkSize,
                maximumTrailerFieldLineLength,
                maximumTrailerSectionSize,
                tempFilePrefix,
                tempFileSuffix,
                tempFileDirectory,
                encoded
        );
        return this;
    }

    @Override
    public InputStream getContentInputStream() {
        return currentResult.inputStream();
    }

    @Override
    public int getContentLength() {
        return currentResult.contentLength();
    }

    public Fields getTrailerFieldsDirectly() { return currentResult.trailerSection(); }

    @Override
    public Optional<Fields> getTrailerFields() {
        return Optional.of(getTrailerFieldsDirectly());
    }
}
