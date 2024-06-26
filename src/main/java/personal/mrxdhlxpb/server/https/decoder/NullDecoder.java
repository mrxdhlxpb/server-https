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
import personal.mrxdhlxpb.server.https.error.concrete.client.BadRequestException;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;

/**
 * @author mrxdhlxpb
 */
public final class NullDecoder implements ContentCodingDecoder, NonChunkedTransferCodingDecoder {

    private ByteArrayInputStream contentInputStream;

    @Override
    public NullDecoder decode(InputStream encoded)
            throws IOException, BadRequestException {
        contentInputStream = new ByteArrayInputStream(encoded.readAllBytes());
        return this;
    }

    @Override
    public Optional<Fields> getTrailerFields() {
        return Optional.empty();
    }

    @Override
    public int getContentLength() {
        return contentInputStream.available();
    }

    @Override
    public InputStream getContentInputStream() {
        return contentInputStream;
    }
}
