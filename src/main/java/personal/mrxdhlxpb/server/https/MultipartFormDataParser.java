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
import personal.mrxdhlxpb.server.https.error.concrete.client.ContentTooLargeException;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.Consumer;

/**
 * An object that parses multipart/form-data data streams.
 * <p> Note: The length of data is not limited, which should be concerned by the client programmer.
 *
 * @author mrxdhlxpb
 */
@CompliantWith("RFC 1867: Form-based File Upload in HTML")
public class MultipartFormDataParser {

    public enum Mode { LAZY, DILIGENT }

    private final Mode mode;

    private static final byte CR = 0XD;

    private static final byte LF = 0XA;

    private static final byte DASH = 0X2D;

    private static final byte[] DOUBLE_DASH = {DASH, DASH};

    private final HttpRequestInputStream httpRequestInputStream;

    private final int maximumPartFieldLineLength;

    private final int maximumPartHeaderSectionLength;

    private final int partBodyParserMemoryBufferSize;

    private final int maximumPartBodyParserTemporaryFileSize;

    private final String partBodyParserTemporaryFilePrefix;

    private final String partBodyParserTemporaryFileSuffix;

    private final File partBodyParserTemporaryFileDirectory;

    private final byte[] dashDashBoundary, dashDashBoundaryDashDash;

    private boolean hasNextPart = true;

    // Fields below are reset in each nextPart() call.

    private Fields currentPartHeaderSection;

    private InputStream currentPartBodyInputStream;

    private MIMEType currentPartContentType; // only initialized in DILIGENT mode

    // Fields above are reset in each nextPart() call.

    public MultipartFormDataParser(HttpRequestInputStream httpRequestInputStream,
                                   Mode mode,
                                   int maximumPartFieldLineLength,
                                   int maximumPartHeaderSectionLength,
                                   int partBodyParserMemoryBufferSize,
                                   int maximumPartBodyParserTemporaryFileSize,
                                   String partBodyParserTemporaryFilePrefix,
                                   String partBodyParserTemporaryFileSuffix,
                                   File partBodyParserTemporaryFileDirectory,
                                   byte[] boundary) {
        this.httpRequestInputStream = httpRequestInputStream;
        this.mode = mode;
        this.maximumPartFieldLineLength = maximumPartFieldLineLength;
        this.maximumPartHeaderSectionLength = maximumPartHeaderSectionLength;
        this.partBodyParserMemoryBufferSize = partBodyParserMemoryBufferSize;
        this.maximumPartBodyParserTemporaryFileSize = maximumPartBodyParserTemporaryFileSize;
        this.partBodyParserTemporaryFilePrefix = partBodyParserTemporaryFilePrefix;
        this.partBodyParserTemporaryFileSuffix = partBodyParserTemporaryFileSuffix;
        this.partBodyParserTemporaryFileDirectory = partBodyParserTemporaryFileDirectory;

        this.dashDashBoundary = new byte[boundary.length + 2];
        this.dashDashBoundaryDashDash = new byte[boundary.length + 4];
        System.arraycopy(DOUBLE_DASH, 0, dashDashBoundary, 0, 2);
        System.arraycopy(boundary, 0, dashDashBoundary, 2, boundary.length);
        System.arraycopy(DOUBLE_DASH, 0, dashDashBoundaryDashDash, 0, 2);
        System.arraycopy(boundary, 0, dashDashBoundaryDashDash, 2, boundary.length);
        System.arraycopy(DOUBLE_DASH, 0, dashDashBoundaryDashDash, boundary.length + 2, 2);
    }

    private void reset() {
        this.currentPartHeaderSection = null;
        this.currentPartBodyInputStream = null;
        this.currentPartContentType = null;
    }

    /**
     * reads a line and checks if the line equals to {@code dashDashBoundary}
     *
     * @throws IOException if an io error occurs
     * @throws BadRequestException if the line does not equal to {@code dashDashBoundary}
     */
    public void readFirstLine() throws IOException, BadRequestException {
        byte[] line = new byte[dashDashBoundary.length];
        try {
            httpRequestInputStream.readLine(line);
        } catch (HttpRequestInputStream.CannotContainException e) {
            throw new BadRequestException("unexpected boundary");
        }
        if (!Arrays.equals(dashDashBoundary, line))
            throw new BadRequestException("unexpected boundary");
    }

    /**
     *
     * @return the next part, or {@code null} if there are no more parts
     */
    public Part nextPart() throws IOException, ContentTooLargeException, BadRequestException {
        if (!hasNextPart)
            return null;
        reset();
        parseHeaderSection();
        return parseBody();
    }

    public void forEachRemaining(Consumer<Part> action)
            throws IOException, ContentTooLargeException, BadRequestException {
        Objects.requireNonNull(action);
        while (hasNextPart)
            action.accept(nextPart());
    }

    private void parseHeaderSection() throws IOException, BadRequestException {
        currentPartHeaderSection = httpRequestInputStream
                .readFields(maximumPartFieldLineLength, maximumPartHeaderSectionLength);

        // initialize currentPartContentType in DILIGENT mode
        if (mode == Mode.DILIGENT) {
            try {
                Optional<String> contentTypeOptional = currentPartHeaderSection
                        .getFieldValueString("Content-Type");
                currentPartContentType = contentTypeOptional.isEmpty() ?
                        MIMEType.TEXT__PLAIN :
                        new MIMEType(contentTypeOptional.get());
            } catch (MIMEType.MIMETypeParseException e) {
                throw new BadRequestException("invalid value of field 'Content-Type'");
            }
        }
    }

    private Part parseBody() throws IOException, ContentTooLargeException, BadRequestException {

        if (mode == Mode.DILIGENT && currentPartContentType.isSameAs(MIMEType.MULTIPART__MIXED)) {

            MultipartFormDataParser multipartFormDataParser = new MultipartFormDataParser(
                    httpRequestInputStream,
                    Mode.DILIGENT,
                    maximumPartFieldLineLength,
                    maximumPartHeaderSectionLength,
                    partBodyParserMemoryBufferSize,
                    maximumPartBodyParserTemporaryFileSize,
                    partBodyParserTemporaryFilePrefix,
                    partBodyParserTemporaryFileSuffix,
                    partBodyParserTemporaryFileDirectory,
                    currentPartContentType
                            .getParameterValue("boundary")
                            .orElseThrow(() -> new BadRequestException("boundary not found"))
                            .getBytes(StandardCharsets.US_ASCII));

            multipartFormDataParser.readFirstLine();
            final List<DiligentModePart> diligentModePartList = new ArrayList<>();
            multipartFormDataParser.forEachRemaining((part) -> {
                if (part instanceof DiligentModePart diligentModePart) {
                    diligentModePartList.add(diligentModePart);
                } else {
                    throw new IllegalStateException("Unexpected situation");
                }
            });

            // reads the next line and checks if the next part exists
            this.hasNextPart = this.hasNextPart();

            return DiligentModePart.buildFrom(
                    currentPartHeaderSection,
                    currentPartContentType,
                    Either.of(null, diligentModePartList));
        }

        try (var parser = new PartBodyParser()) {
            currentPartBodyInputStream = parser.parse();
        }

        return switch (mode) {
            case LAZY -> new LazyModePart(currentPartHeaderSection, currentPartBodyInputStream);
            case DILIGENT -> DiligentModePart.buildFrom(
                    currentPartHeaderSection,
                    currentPartContentType,
                    Either.of(currentPartBodyInputStream, null));
        };
    }

    private boolean hasNextPart() throws IOException, BadRequestException {
        byte[] line = new byte[dashDashBoundaryDashDash.length];
        try {
            httpRequestInputStream.readLine(line);
        } catch (HttpRequestInputStream.CannotContainException e) {
            throw new BadRequestException("unexpected syntax");
        }
        if (Arrays.equals(line, 0, dashDashBoundary.length-1,
                dashDashBoundary, 0, dashDashBoundary.length-1)) {
            return !Arrays.equals(line, dashDashBoundaryDashDash);
        }
        throw new BadRequestException("unexpected syntax");
    }

    /**
     * An object that parses the bodies of parts.
     */
    private final class PartBodyParser implements Closeable {

        private final byte[] buffer = new byte[partBodyParserMemoryBufferSize];

        private int count = 0; // count of data in buffer

        private int temporaryFileSize = 0; // number of bytes written into temporary file

        private boolean bufferFilled = false;

        private File tempFile;

        private OutputStream outputStreamToTempFile;

        @Override
        public void close() throws IOException {
            if (outputStreamToTempFile != null)
                outputStreamToTempFile.close();
        }

        private InputStream result() throws IOException {
            if (outputStreamToTempFile != null)
                outputStreamToTempFile.flush();
            this.close();
            return bufferFilled ? new FileInputStream(tempFile) : new ByteArrayInputStream(buffer);
        }

        private void store(int data) throws IOException, ContentTooLargeException {
            if (count < buffer.length) {
                buffer[count++] = (byte) data;
                return;
            }
            // buffer is filled with data
            if (!bufferFilled) {
                // first time
                // create temporary file
                // TODO: temp file security
                tempFile = File.createTempFile(
                        partBodyParserTemporaryFilePrefix,
                        partBodyParserTemporaryFileSuffix,
                        partBodyParserTemporaryFileDirectory);
                tempFile.deleteOnExit();
                outputStreamToTempFile = new BufferedOutputStream(new FileOutputStream(tempFile));
                // write buffer to temporary file
                outputStreamToTempFile.write(buffer);
                temporaryFileSize += buffer.length;

                bufferFilled = true;
            }
            if (temporaryFileSize >= maximumPartBodyParserTemporaryFileSize)
                throw new ContentTooLargeException();
            outputStreamToTempFile.write(data);
            temporaryFileSize++;
        }

        public InputStream parse() throws IOException, ContentTooLargeException {
            while (true) {
                int data = httpRequestInputStream.read();
                if (data == -1)
                    return result();
                if (data != CR) {
                    store(data);
                    continue;
                }
                if (httpRequestInputStream.read() != LF) {
                    store(CR);
                    store(data);
                    continue;
                }
                httpRequestInputStream.mark(dashDashBoundaryDashDash.length+2);
                byte[] line = new byte[dashDashBoundaryDashDash.length];
                try {
                    httpRequestInputStream.readLine(line);
                } catch (HttpRequestInputStream.CannotContainException e) {
                    httpRequestInputStream.reset();
                    continue;
                }
                if (Arrays.equals(line, 0, dashDashBoundary.length-1,
                        dashDashBoundary, 0, dashDashBoundary.length-1)) {
                    hasNextPart = !Arrays.equals(line, dashDashBoundaryDashDash);
                    return result();
                }
                httpRequestInputStream.reset();
            }
        }

    }

    public sealed interface Part
            extends Closeable
            permits LazyModePart, DiligentModePart {}

    public record LazyModePart(Fields headerSection, InputStream bodyInputStream)
            implements Part {

        @Override
        public void close() throws IOException {
            if (bodyInputStream != null)
                bodyInputStream.close();
        }
    }

    /**
     *
     * @param name never null
     * @param filename never null
     * @param contentType never null
     * @param contentTransferEncoding never null
     * @param eitherBodyInputStreamOrDiligentModePartList never null
     */
    @CompliantWith("RFC 1867: Form-based File Upload in HTML Section 7. Registration " +
            "of multipart/form-data")
    public record DiligentModePart(String name,
                                   Optional<String> filename,
                                   MIMEType contentType,
                                   Optional<String> contentTransferEncoding,
                                   Either<InputStream, List<DiligentModePart>>
                                               eitherBodyInputStreamOrDiligentModePartList)
            implements Part {

        public DiligentModePart {
            Objects.requireNonNull(name);
            Objects.requireNonNull(filename);
            Objects.requireNonNull(contentType);
            Objects.requireNonNull(contentTransferEncoding);
            Objects.requireNonNull(eitherBodyInputStreamOrDiligentModePartList);
        }

        public static DiligentModePart buildFrom(Fields headerSection,
                                                 MIMEType contentType,
                                                 Either<InputStream, List<DiligentModePart>>
                                                     eitherBodyInputStreamOrDiligentModePartList)
                throws BadRequestException {
            String contentDisposition = headerSection
                    .getFieldValueString("Content-Disposition")
                    .orElseThrow(() -> new BadRequestException("field 'Content-Disposition' " +
                            "required but missing"));
            String[] contentDispositionArguments = contentDisposition.split(";", 3);
            if (!contentDispositionArguments[0].equals("form-data"))
                throw new BadRequestException("invalid value of field 'Content-Disposition'");
            String name = null;
            Optional<String> filename = Optional.empty();
            for (Map.Entry<String, String> entry : Arrays.stream(contentDispositionArguments)
                    .map(String::trim)
                    .filter((str) -> str.contains("="))
                    .map((str) -> Map.entry(str.split("=", 2)[0], str.split("=", 2)[1]))
                    .toList()) {
                switch (entry.getKey()) {
                    // TODO: Field names originally in non-ASCII character sets may be
                    //  encoded using the method outlined in RFC 1522.
                    case "name" -> name = entry.getValue();
                    case "filename" -> filename = Optional.of(entry.getValue());
                    default -> throw new BadRequestException("unknown attribute name");
                }
            }
            if (name == null)
                throw new BadRequestException("'name' attribute missing");

            final Optional<String> contentTransferEncoding = headerSection
                    .getFieldValueString("Content-Transfer-Encoding");

            return new DiligentModePart(
                    name,
                    filename,
                    contentType,
                    contentTransferEncoding,
                    eitherBodyInputStreamOrDiligentModePartList);
        }

        @Override
        public void close() throws IOException {
            if (eitherBodyInputStreamOrDiligentModePartList.hasLeftValue())
                eitherBodyInputStreamOrDiligentModePartList.getLeftValue().close();
            else
                for (DiligentModePart diligentModePart :
                        eitherBodyInputStreamOrDiligentModePartList.getRightValue())
                    diligentModePart.close();
        }
    }

}
