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

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Optional;

/**
 * A {@code TransferCodingDecoder} that applies a {@code ChunkedTransferCodingDecoder} first,
 * and then applies a chain of {@code NonChunkedTransferCodingDecoder}.
 *
 * @author mrxdhlxpb
 */
public final class ChainTransferCodingDecoder implements TransferCodingDecoder {

    private final ChunkedTransferCodingDecoder chunked;

    private final ChainDecoder nonChunkedChain;

    public ChainTransferCodingDecoder(ChunkedTransferCodingDecoder chunked,
                                      NonChunkedTransferCodingDecoder...
                                              nonChunkedTransferCodingDecoders) {
        this.chunked = chunked;
        this.nonChunkedChain = nonChunkedTransferCodingDecoders.length == 0 ?
                null :
                HTTPDecoder.chain(nonChunkedTransferCodingDecoders);
    }

    public ChainTransferCodingDecoder(ChunkedTransferCodingDecoder chunked,
                                      List<? extends NonChunkedTransferCodingDecoder>
                                              nonChunkedTransferCodingDecoders) {
        this.chunked = chunked;
        this.nonChunkedChain = nonChunkedTransferCodingDecoders.isEmpty() ?
                null :
                HTTPDecoder.chain(nonChunkedTransferCodingDecoders);
    }

    /**
     * This method throws {@code IllegalStateException}.
     *
     * @deprecated the decode method of {@code ChainTransferCodingDecoder} only
     *             accept {@code HttpRequestInputStream}
     */
    @Override
    @Deprecated
    public ChainTransferCodingDecoder decode(InputStream encoded) {
        throw new IllegalStateException();
    }

    public ChainTransferCodingDecoder decode(HttpRequestInputStream encoded)
            throws IOException, BadRequestException {
        if (nonChunkedChain == null) {
            chunked.decode(encoded);
            return this;
        }

        try (var chunkedDecoded = chunked.decode(encoded).getDecodedInputStream()) {
            nonChunkedChain.decode(chunkedDecoded);
        }
        return this;
    }

    public Fields getTrailerFieldsDirectly() { return chunked.getTrailerFieldsDirectly(); }

    @Override
    public Optional<Fields> getTrailerFields() {
        return chunked.getTrailerFields();
    }

    @Override
    public int getContentLength() {
        return chunked.getContentLength();
    }

    @Override
    public InputStream getContentInputStream() {
        return nonChunkedChain == null ?
                chunked.getContentInputStream() :
                nonChunkedChain.getDecodedInputStream();
    }
}
