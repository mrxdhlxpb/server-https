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
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

/**
 * A chain of HTTP decoders, which is an HTTP decoder.
 *
 * @author mrxdhlxpb
 */
public final class ChainDecoder implements HTTPDecoder {

    private final List<? extends HTTPDecoder> decoders;

    private InputStream decodedInputStream;

    private int decodedLength;

    /**
     * Constructs a {@code ChainDecoder}.
     * @param decoders the decoders to be chained
     * @throws IllegalArgumentException if {@code decoders} has no element
     */
    public ChainDecoder(List<? extends HTTPDecoder> decoders) {
        if (decoders.isEmpty())
            throw new IllegalArgumentException();
        this.decoders = decoders;
    }

    /**
     * Constructs a {@code ChainDecoder}.
     * @param decoders the decoders to be chained
     * @throws IllegalArgumentException if {@code decoders} has no element
     */
    public ChainDecoder(HTTPDecoder... decoders) {
        if (decoders.length == 0)
            throw new IllegalArgumentException();
        this.decoders = Arrays.asList(decoders);
    }

    /**
     * Applies each decoder in the order of iteration. Intermediate input streams will be closed.
     * {@code encoded} will not be closed.
     * @param encoded the input stream to be decoded
     * @return {@code this}
     * @throws IOException if an i/o error occurs
     * @throws BadRequestException if {@code encoded} cannot be decoded
     */
    @Override
    public HTTPDecoder decode(InputStream encoded) throws IOException, BadRequestException {
        InputStream previous, next;
        int len;
        final Iterator<? extends HTTPDecoder> iterator = decoders.iterator();
        final HTTPDecoder firstDecoder = iterator.next();

        previous = firstDecoder.decode(encoded).getDecodedInputStream();
        len = firstDecoder.getDecodedLength();

        while (iterator.hasNext()) {
            try {
                final HTTPDecoder currentDecoder = iterator.next();
                next = currentDecoder.decode(previous).getDecodedInputStream();
                len = currentDecoder.getDecodedLength();
            } finally {
                previous.close();
            }
            previous = next;
        }

        this.decodedInputStream = previous;
        this.decodedLength = len;
        return this;
    }

    @Override
    public InputStream getDecodedInputStream() {
        return decodedInputStream;
    }

    @Override
    public int getDecodedLength() {
        return decodedLength;
    }
}
