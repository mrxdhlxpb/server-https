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
import personal.mrxdhlxpb.server.https.error.concrete.server.NotImplementedException;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * A decoder for a transfer coding.
 *
 * @author mrxdhlxpb
 */
public sealed interface TransferCodingDecoder
        extends HTTPDecoder
        permits ChainTransferCodingDecoder, ChunkedTransferCodingDecoder,
        NonChunkedTransferCodingDecoder {

    /**
     *
     * @return trailer fields if any
     */
    Optional<Fields> getTrailerFields();

    /**
     * alias for {@code getDecodedLength()}
     */
    int getContentLength();

    @Override
    default int getDecodedLength() {
        return getContentLength();
    }

    /**
     * alias for {@code getDecodedInputStream()}
     */
    InputStream getContentInputStream();

    @Override
    default InputStream getDecodedInputStream() { return getContentInputStream(); }

    static ChainTransferCodingDecoder chain(ChunkedTransferCodingDecoder chunked,
                                            NonChunkedTransferCodingDecoder...
                                                    nonChunkedTransferCodingDecoders) {
        return new ChainTransferCodingDecoder(chunked, nonChunkedTransferCodingDecoders);
    }

    static ChainTransferCodingDecoder chain(ChunkedTransferCodingDecoder chunked,
                                            List<? extends NonChunkedTransferCodingDecoder>
                                                    nonChunkedTransferCodingDecoders) {
        return new ChainTransferCodingDecoder(chunked, nonChunkedTransferCodingDecoders);
    }

    static ChainTransferCodingDecoder of(HTTPDecoderRegistry HTTPDecoderRegistry,
                                         String[] transferEncodingFieldValueMembers)
            throws NotImplementedException {
        List<NonChunkedTransferCodingDecoder> nonChunkedTransferCodingDecoders = new ArrayList<>();
        for (int i = transferEncodingFieldValueMembers.length - 2; i >= 0; i--)
            nonChunkedTransferCodingDecoders
                    .add((NonChunkedTransferCodingDecoder) HTTPDecoderRegistry
                    .getTransferCodingDecoder(transferEncodingFieldValueMembers[i])
                    .orElseThrow(NotImplementedException::new));
        return chain((ChunkedTransferCodingDecoder) HTTPDecoderRegistry
                .getTransferCodingDecoder(transferEncodingFieldValueMembers
                        [transferEncodingFieldValueMembers.length-1])
                .orElseThrow(NotImplementedException::new),
                nonChunkedTransferCodingDecoders);
    }
}
