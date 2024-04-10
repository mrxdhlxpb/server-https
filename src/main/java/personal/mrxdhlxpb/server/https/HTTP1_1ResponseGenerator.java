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

import personal.mrxdhlxpb.server.https.configuration.Configuration;
import personal.mrxdhlxpb.server.https.error.HttpErrorException;
import personal.mrxdhlxpb.server.https.error.concrete.server.InternalServerErrorException;

import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

/**
 * @author mrxdhlxpb
 */
public class HTTP1_1ResponseGenerator {
    private static final byte[] CRLF = {0XD, 0XA};

    private static final byte SP = 0X20;

    private final Configuration configuration;

    private final OutputStream responseOutputStream;

    private final PrintStream responsePrintStream;

    public HTTP1_1ResponseGenerator(Configuration configuration,
                                    OutputStream responseOutputStream) {
        this.configuration = configuration;
        this.responseOutputStream = responseOutputStream;
        responsePrintStream = new PrintStream(
                responseOutputStream,
                false,
                StandardCharsets.US_ASCII
        );
    }

    public void generateResponse(Either<HTTPRequest, HttpErrorException>
                                         eitherHTTPRequestOrHttpErrorException)
        throws HttpErrorException {

//        HTTP-message   = status-line CRLF
//                         *( field-line CRLF )
//                         CRLF
//                         [ message-body ]

        final HTTPResponse httpResponse = new HTTPResponse(ProtocolVersion.HTTP__1_1);
        final HTTP1_1ResponseMessageBodyGenerator messageBodyGenerator;

        if (eitherHTTPRequestOrHttpErrorException.hasLeftValue())
            eitherHTTPRequestOrHttpErrorException
                    .getLeftValue()
                    .requestMessageControlData()
                    .targetResource()
                    .getHTTPRequestHandler()
                    .handle(
                            eitherHTTPRequestOrHttpErrorException
                                    .getLeftValue(),
                            httpResponse
                    );
        else
            configuration
                    .getHttpErrorHandlerRegistry()
                    .getHttpErrorHandler(
                            eitherHTTPRequestOrHttpErrorException
                                    .getRightValue()
                                    .getClass()
                    )
                    .handle(
                            eitherHTTPRequestOrHttpErrorException
                                    .getRightValue(),
                            httpResponse
                    );

        messageBodyGenerator = eitherHTTPRequestOrHttpErrorException.hasLeftValue() ?
                eitherHTTPRequestOrHttpErrorException
                        .getLeftValue()
                        .requestMessageControlData()
                        .targetResource()
                        .getHTTP1_1ResponseMessageBodyGenerator() :
                configuration
                        .getHTTP1_1Configuration()
                        .getHttpErrorHTTP1_1ResponseMessageBodyGenerator();

        // status-line
        generateStatusLine(httpResponse.getStatusCode());

        // CRLF
        responsePrintStream.writeBytes(CRLF);

        // *( field-line CRLF )
        httpResponse.getHeaderSection().print(responsePrintStream);

        // CRLF
        responsePrintStream.writeBytes(CRLF);

        if (responsePrintStream.checkError())
            throw new InternalServerErrorException("The print stream has encountered" +
                    " an IOException.");

        // [ message-body ]
        messageBodyGenerator
                .generateResponseMessageBody(
                        httpResponse.getContentInputStream(),
                        httpResponse.getContentLength(),
                        httpResponse.getTrailerSection().orElse(null),
                        responseOutputStream
                );
    }

    private void generateStatusLine(short statusCode) {
        responsePrintStream.print(ProtocolVersion.HTTP__1_1_STRING);
        responsePrintStream.write(SP);
        responsePrintStream.print(statusCode);
    }

    public void generateResponse(HttpErrorException httpErrorException) {
        try {
            generateResponse(Either.of(null, httpErrorException));
        } catch (HttpErrorException e) { throw new RuntimeException(e); }
    }
}
