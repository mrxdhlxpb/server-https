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

import java.util.*;
import java.util.regex.Pattern;

/**
 * Represents a Multipurpose Internet Mail Extensions (MIME) type, as defined in <em>RFC 2045:
 * Multipurpose Internet Mail Extensions (MIME) Part One: Format of Internet Message Bodies</em> &
 * <em>RFC 2046: Multipurpose Internet Mail Extensions (MIME) Part Two: Media Types</em>.
 *
 * @author mrxdhlxpb
 */
public class MIMEType {

    private static final String MEDIA_TYPE_REMAINING_DELIMITER_REGEX = "/";
    private static final Pattern MEDIA_TYPE_REMAINING_DELIMITER_PATTERN = Pattern.compile(
            MEDIA_TYPE_REMAINING_DELIMITER_REGEX);
    private static final String SUBTYPE_PARAMETERS_DELIMITER_REGEX = ";\\x20?";
    private static final Pattern SUBTYPE_PARAMETERS_DELIMITER_PATTERN = Pattern.compile(
            SUBTYPE_PARAMETERS_DELIMITER_REGEX);
    private static final String PARAMETER_NAME_PARAMETER_VALUE_DELIMITER_REGEX = "=";
    private static final Pattern PARAMETER_NAME_PARAMETER_VALUE_DELIMITER_PATTERN = Pattern.compile(
            PARAMETER_NAME_PARAMETER_VALUE_DELIMITER_REGEX);

    public static final MIMEType TEXT__PLAIN = new MIMEType("text", "plain");
    public static final MIMEType MULTIPART__MIXED = new MIMEType("multipart", "mixed");

    /**
     * the top-level media type used to declare the general type of data
     */
    private final String mediaType;

    /**
     * the subtype which specifies the specific format for the type of data
     */
    private final String subtype;

    /**
     * the set of parameters
     */
    private final Map<String, String> parameters = new HashMap<>();

    /**
     * Constructs a MIME type with specified media type and subtype identifiers, and an empty
     * set of parameters.
     * @param mediaType the specified media type identifier
     * @param subtype the specified subtype identifier
     * @throws NullPointerException if any argument is {@code null}
     */
    public MIMEType(String mediaType, String subtype) {
        this.mediaType = Objects.requireNonNull(mediaType);
        this.subtype = Objects.requireNonNull(subtype);
    }

    /**
     * Parses the give string into a MIME type.
     * @param str the give string to be parsed
     * @throws MIMETypeParseException if the string cannot be parsed
     */
    public MIMEType(String str) throws MIMETypeParseException {
        final String[] mediaTypeRemaining = MEDIA_TYPE_REMAINING_DELIMITER_PATTERN.split(str, 2);
        if (mediaTypeRemaining.length < 2)
            throw new MIMETypeParseException();
        mediaType = mediaTypeRemaining[0].toLowerCase(Locale.ROOT);
        final String remaining = mediaTypeRemaining[1];
        final String[] subtypeParameters = SUBTYPE_PARAMETERS_DELIMITER_PATTERN
                .split(remaining, -1);
        subtype = subtypeParameters[0].toLowerCase(Locale.ROOT);
        if (subtypeParameters.length < 2)
            return;
        for (int i = 1; i < subtypeParameters.length; i++) {
            final String[] parameterNameParameterValue =
                    PARAMETER_NAME_PARAMETER_VALUE_DELIMITER_PATTERN.split(subtypeParameters[i], 2);
            if (parameterNameParameterValue.length < 2)
                throw new MIMETypeParseException();
            parameters.put(parameterNameParameterValue[0].toLowerCase(Locale.ROOT),
                    parameterNameParameterValue[1]);
        }
    }

    /**
     * Returns {@code true} if the media type and subtype of this object are the same
     * as the specified {@code mimeType}, otherwise returns {@code false}.
     * @param mimeType the specified {@code mimeType}
     * @return as described above
     */
    public boolean isSameAs(MIMEType mimeType) {
        return mediaType.equals(mimeType.mediaType)
                && subtype.equals(mimeType.subtype);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof MIMEType mimeType)
            return isSameAs(mimeType) && parameters.equals(mimeType.parameters);
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(mediaType, subtype, parameters);
    }

    public String getMediaType() {
        return this.mediaType;
    }

    public String getSubtype() {
        return this.subtype;
    }

    public Optional<String> getParameterValue(String parameterName) {
        return Optional.ofNullable(parameters.get(parameterName.toLowerCase(Locale.ROOT)));
    }

    public static final class MIMETypeParseException extends Exception {}
}
