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
package personal.mrxdhlxpb.server.https.configuration;

import personal.mrxdhlxpb.server.https.InternalResourceMapper;
import personal.mrxdhlxpb.server.https.HttpErrorHandlerRegistry;
import personal.mrxdhlxpb.server.https.decoder.HTTPDecoderRegistry;

/**
 * @author mrxdhlxpb
 */
public interface Configuration {

    NetworkConfiguration getNetworkConfiguration();

    InternalResourceMapper getInternalResourceMapper();

    HTTPDecoderRegistry getHTTPDecoderRegistry();

    HttpErrorHandlerRegistry getHttpErrorHandlerRegistry();

    HTTP1_1Configuration getHTTP1_1Configuration();

}
