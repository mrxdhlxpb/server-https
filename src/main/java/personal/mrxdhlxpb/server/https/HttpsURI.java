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

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Represents an "https" URI.
 *
 * @author mrxdhlxpb
 */
@CompliantWith("RFC 3986: Uniform Resource Identifier (URI): Generic Syntax")
@CompliantWith("RFC 9110: HTTP Semantics")
public class HttpsURI {

    public static final String UNRESERVED_OR_SUB_DELIMS_REGEX = "[\\w-.~!$&'()*+,;=]";

    public static final String PCT_ENCODED_REGEX = "(%[\\da-fA-F]{2})";

    //pchar         = unreserved / pct-encoded / sub-delims / ":" / "@"
    public static final String PCHAR_REGEX =
            "(" + PCT_ENCODED_REGEX + "|" + UNRESERVED_OR_SUB_DELIMS_REGEX + "|" + "[:@]" + ")";

    //segment       = *pchar
    public static final String SEGMENT_REGEX = "(" + PCHAR_REGEX + "*" + ")";

    //path-abempty  = *( "/" segment )
    public static final String PATH_ABEMPTY_REGEX = "(?<PATHABEMPTY>" +
            "(" + "/" + SEGMENT_REGEX + ")*" + ")";

    //query         = *( pchar / "/" / "?" )
    public static final String QUERY_REGEX = "(?<QUERY>" +
            "(" + PCHAR_REGEX + "|" + "[/?]" + ")*" + ")";

    //query-optional = [ "?" query ]
    public static final String QUERY_OPTIONAL_REGEX = "(" + "\\?" + QUERY_REGEX + ")?";

    //port          = *DIGIT
    public static final String PORT_REGEX = "(?<PORT>\\d*)";

    //port-optional = [ ":" port ]
    public static final String PORT_OPTIONAL_REGEX = "(" + ":" + PORT_REGEX + ")?";

    public static final String IP_LITERAL_REGEX =
            "\\[" + "(" + UNRESERVED_OR_SUB_DELIMS_REGEX + "|" + ":" + ")*" + "]";

    //host          = IP-literal / IPv4address / reg-name
    public static final String URI_HOST_REGEX =
            "(?<URIHOST>" + IP_LITERAL_REGEX + "|" + "(" + PCT_ENCODED_REGEX + "|" +
                    UNRESERVED_OR_SUB_DELIMS_REGEX + ")*" + ")";

    //https-URI = "https://" host [ ":" port ] path-abempty [ "?" query ]
    public static final String HTTPS_URI_REGEX =
            "HTTPS://" + URI_HOST_REGEX + PORT_OPTIONAL_REGEX + PATH_ABEMPTY_REGEX +
                    QUERY_OPTIONAL_REGEX;

    public static final Pattern HTTPS_URI_PATTERN = Pattern.compile(HTTPS_URI_REGEX,
            Pattern.CASE_INSENSITIVE);

    public static final Pattern PCT_ENCODED_PATTERN = Pattern.compile(PCT_ENCODED_REGEX);

    private final String host;

    private final String port;

    private final String path;

    private final String query;

    /**
     * Constructs an https URI from its components.
     * @param host non-null required
     * @param path non-null required
     * @throws BadRequestException if {@code host} is empty
     */
    public HttpsURI(String host, String port, String path, String query)
            throws BadRequestException {
        this.host = Objects.requireNonNull(host);
        // Reject https URI with empty host identifier as invalid
        if (host.isEmpty())
            throw new BadRequestException("Invalid https URI: empty host");
        this.port = port;
        this.path = Objects.requireNonNull(path);
        this.query = query;
    }

    /**
     * Tries to parse a string into an instance of this class.
     * @param str the string
     * @return an {@code Optional} describing the result,
     *         or an empty {@code Optional} if the string cannot be parsed
     * @throws BadRequestException if the string can be parsed as an https URI,
     *                             but the host component is empty and we
     *                             should reject it.
     */
    public static Optional<HttpsURI> fromString(String str) throws BadRequestException {
        Matcher httpsURIMatcher = HTTPS_URI_PATTERN.matcher(str);
        if (!httpsURIMatcher.matches())
            return Optional.empty();
        String uriHost = httpsURIMatcher.group("URIHOST"); // never null, might be empty
        String port = httpsURIMatcher.group("PORT"); // nullable
        String path = httpsURIMatcher.group("PATHABEMPTY"); //  begins with "/" or is empty
        String query = httpsURIMatcher.group("QUERY"); // nullable
        return Optional.of(new HttpsURI(uriHost, port, path, query));
    }

    /**
     *
     * @return a normalized https URI
     */
    public HttpsURI normalize() {
        return normalize(this);
    }

    public InternalResourceIdentifier toInternalResourceIdentifier() {
        return new InternalResourceIdentifier(path, query);
    }

    public String recombine() {
        StringBuilder builder = new StringBuilder("https://");
        builder.append(host);
        if (port != null)
            builder.append(":").append(port);
        builder.append(path);
        if (query != null)
            builder.append("?").append(query);
        return builder.toString();
    }

    public String getHost() {
        return host;
    }

    public String getPort() {
        return port;
    }

    public String getPath() {
        return path;
    }

    public String getQuery() {
        return query;
    }

    /**
     * @return {@code port == null ? "443" : port}
     */
    public String getPortOr443() {
        return port == null ? "443" : port;
    }

    private static final Map<String, String> PERCENT_ENCODING_NORMALIZATION_MAP = new HashMap<>();

    private static int[] range(int min, int max) {
        int[] ints = new int[max - min + 1];
        for (int i = 0; i < ints.length; i++)
            ints[i] = min + i;
        return ints;
    }

    private static void put(int i) {
        PERCENT_ENCODING_NORMALIZATION_MAP.put(String.format("%%%X", i), String.valueOf((char) i));
    }

    static {
        // ALPHA (%41-%5A and %61-%7A),
        // DIGIT (%30-%39),
        // hyphen (%2D),
        // period (%2E),
        // underscore (%5F),
        // tilde (%7E)
        for (int i : range(0x41, 0x5a))
            put(i);
        for (int i : range(0x61, 0x7a))
            put(i);
        for (int i : range(0x30, 0x39))
            put(i);
        put(0x2d);
        put(0x2e);
        put(0x5f);
        put(0x7e);
    }

    /**
     * Performs percent-encoding normalization on the given string.
     * @param str the given string
     * @return normalized string
     */
    static String percentEncodingNormalization(String str) {
        return PCT_ENCODED_PATTERN
                .matcher(str)
                .replaceAll(matchResult ->
                        PERCENT_ENCODING_NORMALIZATION_MAP
                                .getOrDefault(matchResult.group(), matchResult.group()));
    }

    /**
     * Normalizes the hexadecimal digits within all percent-encoding triplets in {@code str}
     * to use uppercase letters for the digits A-F.
     * @param str the string to be normalized
     * @return normalized string
     */
    static String percentEncodingTripletCaseNormalization(String str) {
        return PCT_ENCODED_PATTERN
                .matcher(str)
                .replaceAll(matchResult -> matchResult.group().toUpperCase());
    }

    /**
     * Applies remove_dot_segments algorithm to the path, as described in
     * <em>RFC 3986: Uniform Resource Identifier (URI): Generic Syntax Section 5.2.4. Remove Dot
     * Segments</em>.
     * @param str the path
     * @return the result
     */
    static String removeDotSegments(String str) {
        StringBuilder input = new StringBuilder(str);
        StringBuilder output = new StringBuilder();
        while (!input.isEmpty()) {
            if (input.indexOf("../") == 0) {
                input.delete(0, 3);
            } else if (input.indexOf("./") == 0) {
                input.delete(0, 2);
            } else if (input.indexOf("/./") == 0) {
                input.replace(0, 3, "/");
            } else if (CharSequence.compare(input, "/.") == 0) {
                input.replace(0, 2, "/");
            } else if (input.indexOf("/../") == 0) {
                input.replace(0, 4, "/");
                int index = output.lastIndexOf("/");
                if (index != -1)
                    output.delete(index, output.length());
            } else if (CharSequence.compare(input, "/..") == 0) {
                input.replace(0, 3, "/");
                int index = output.lastIndexOf("/");
                if (index != -1)
                    output.delete(index, output.length());
            } else if ((CharSequence.compare(input, ".") &
                    CharSequence.compare(input, "..")) == 0) {
                input.delete(0, input.length());
            } else {
                int index = input.indexOf("/", 1);
                if (index == -1)
                    index = input.length();
                output.append(input.substring(0, index));
                input.delete(0, index);
            }
        }
        return output.toString();
    }


    /**
     * Performs normalization for the given https URI in a manner consistent with
     * <em>RFC 3986: Uniform Resource Identifier (URI): Generic Syntax Section 6. Normalization and
     * Comparison</em>.
     * <p> We perform <em>syntax-based normalization</em> and <em>scheme-based normalization</em> to
     * reduce the probability of false negatives.
     * @param httpsURI the given https URI to be normalized
     * @return normalized https-URI
     */
    static HttpsURI normalize(HttpsURI httpsURI) {
        //syntax-based normalization
        String host = percentEncodingNormalization(percentEncodingTripletCaseNormalization(httpsURI.
                host.toLowerCase()));
        String port = Optional
                .ofNullable(httpsURI.port)
                .map(HttpsURI::percentEncodingTripletCaseNormalization)
                .map(HttpsURI::percentEncodingNormalization)
                .orElse(null);
        String path = removeDotSegments(percentEncodingNormalization(
                percentEncodingTripletCaseNormalization(httpsURI.path)));
        String query = Optional
                .ofNullable(httpsURI.query)
                .map(HttpsURI::percentEncodingTripletCaseNormalization)
                .map(HttpsURI::percentEncodingNormalization)
                .orElse(null);
        // Scheme-Based Normalization
        // 1. empty path should be normalized to "/"
        if (path.isEmpty())
            path = "/";
        // 2. An explicit ":port", for which the port is empty or the default for the scheme,
        // is equivalent to one where the port and its ":" delimiter are elided
        // and thus should be removed by scheme-based normalization.
        if (port != null && (port.isEmpty() || port.equals("443")))
            port = null;
        try {
            return new HttpsURI(host, port, path, query);
        } catch (BadRequestException e) {
            throw new RuntimeException("unimaginable situation");
        }
    }

}
