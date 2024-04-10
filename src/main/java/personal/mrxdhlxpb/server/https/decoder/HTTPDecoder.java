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

import personal.mrxdhlxpb.server.https.error.concrete.client.BadRequestException;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

/**
 * An HTTP decoder that performs a decoding transformation on the data read from the given input
 * stream.
 * <p> HTTP codings include "HTTP Content Codings" and "HTTP Transfer Codings", names of both of
 * which are registered at <a href="https://www.iana.org/assignments/http-parameters">link</a>.
 * This {@code sealed} interface permits {@code ContentCodingDecoder} to represent a decoder
 * for a content coding, {@code TransferCodingDecoder} to represent a decoder for a transfer coding
 * and {@code ChainDecoder} to combine HTTP decoders into a chain.
 * <p> As defined in <em>RFC 9110: HTTP Semantics Section 16.6.1. Content Coding Registry</em> &
 * <em>RFC 9112: HTTP/1.1 Section 7.3. Transfer Coding Registry</em>, names of content codings
 * and transfer codings never overlap with each other unless the encoding (decoding) transformation
 * is identical. A decoder that apply a decoding transformation registered as both a transfer
 * coding and a content coding may implement both the {@code TransferCodingDecoder} and the
 * {@code ContentCodingDecoder} interfaces.
 * <p> The mapping between coding names and decoders is managed by {@link HTTPDecoderRegistry}.
 *
 * @author mrxdhlxpb
 */
public sealed interface HTTPDecoder
        permits ContentCodingDecoder, TransferCodingDecoder, ChainDecoder {

    /**
     * Decodes the data read from {@code encoded}. The result can be obtained from subsequent calls
     * to {@code getDecodedInputStream()} and {@code getDecodedLength()}.
     * <p> This method does not close {@code encoded}.
     *
     * @param encoded the input stream to be decoded
     * @return {@code this}
     * @throws IOException         if an i/o error occurs
     * @throws BadRequestException if {@code encoded} cannot be decoded
     * @implNote wrap {@code encoded} with {@code BufferedInputStream} if necessary
     */
    HTTPDecoder decode(InputStream encoded) throws IOException, BadRequestException;

    /**
     * Returns an {@code InputStream} from which the decoded data can be read. Calling this method
     * prior to calling the decode method has undefined behaviour.
     * <p> The decoded data is determined by reading a certain number of bytes from the returned
     * input stream. The number of bytes is the return value of {@code getDecodedLength()}. The
     * situation where end of stream has been detected before the specified number of bytes can be
     * read is unexpected and should be taken as an error.
     * <p> The returned input stream is usually either an instance of {@code ByteArrayInputStream}
     * if we want to keep the data in memory or an instance of {@code FileInputStream} that reads
     * from a temporary file.
     * <p> The returned input stream is usually not wrapped with {@code BufferedInputStream}.
     *
     * @return as described above
     */
    InputStream getDecodedInputStream();

    /**
     * Returns the length of the decoded data, in bytes. Calling this method prior to calling the
     * decode method has undefined behaviour.
     *
     * @return as described above
     */
    int getDecodedLength();

    /**
     * Combines multiple HTTP decoders into a {@code ChainDecoder}.
     *
     * @param decoders the decoders to be chained
     * @return the {@code ChainDecoder}
     */
    static ChainDecoder chain(HTTPDecoder... decoders) { return new ChainDecoder(decoders); }

    /**
     * Combines multiple HTTP decoders into a {@code ChainDecoder}.
     *
     * @param decoders the decoders to be chained
     * @return the {@code ChainDecoder}
     */
    static ChainDecoder chain(List<? extends HTTPDecoder> decoders) {
        return new ChainDecoder(decoders);
    }

}
