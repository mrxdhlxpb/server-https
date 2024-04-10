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

import java.io.IOException;
import java.io.PrintStream;
import java.util.*;
import java.util.regex.Pattern;

/**
 * @author mrxdhlxpb
 */
//TODO: cookie
@CompliantWith("RFC 9110: HTTP Semantics Section 5. Fields")
public class Fields {
    private static final byte[] CRLF = {0XD, 0XA};

    private static final Pattern FIELD_LINE_NAME_VALUE_DELIMITER_PATTERN = Pattern
            .compile(":\\x20*");

    // OWS            = *( SP / HTAB )
    private static final String OWS_REGEX = "[\\x20\\x09]*";

    private static final Pattern OWS_COMMA_OWS_PATTERN = Pattern
            .compile(OWS_REGEX + "," + OWS_REGEX);

    // ArrayList permits null,
    // but we do not
    private final List<Field> internalList = new ArrayList<>();

    // HashMap permits null values and the null key,
    // but we do not
    private final Map<String, Field> internalMap = new HashMap<>();

    public Fields() {}

    /**
     * @throws BadRequestException if syntax of a field line is invalid
     */
    public Fields(String[] fieldLines) throws BadRequestException {
        for (String fieldLine : fieldLines) {
            appendFieldLine(fieldLine);
        }
    }

    /**
     * @throws BadRequestException if syntax of a field line is invalid
     */
    public Fields(Iterable<String> fieldLines) throws BadRequestException {
        for (String fieldLine : fieldLines) {
            appendFieldLine(fieldLine);
        }
    }

    /**
     * @throws BadRequestException if length of a field line exceeds limit, or
     *                             syntax of a field line is invalid
     * @throws IOException if {@code fieldLineSupplier} throws the {@code IOException}
     */
    public Fields(FieldLineSupplier fieldLineSupplier, StopSupplyCondition stopSupplyCondition)
            throws BadRequestException, IOException {
        fieldLineSupplier.supplyTo(this::appendFieldLine).until(stopSupplyCondition);
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof Fields fields && fields.internalList.equals(internalList);
    }

    @Override
    public int hashCode() {
        return internalList.hashCode();
    }

    @Override
    public String toString() {
        return internalList.toString();
    }

    public void print(PrintStream printStream) {
        for (Field field : toList()) {
            printStream.print(field);
            printStream.writeBytes(CRLF);
        }
    }

    // Search Operations

    /**
     * @param fieldName non-null required
     * @throws NullPointerException if {@code fieldName} is {@code null}
     */
    public int indexOf(String fieldName) {
        return getField(fieldName).map(this::indexOf).orElse(-1);
    }

    /**
     * @param field non-null required
     * @throws NullPointerException if {@code field} is {@code null}
     */
    public int indexOf(Field field) { return internalList.indexOf(Objects.requireNonNull(field)); }

    // Query Operations

    public int size() { return internalList.size(); }

    public boolean isEmpty() {return internalList.isEmpty();}

    /**
     * @param fieldName non-null required
     * @throws NullPointerException if {@code fieldName} is {@code null}
     */
    public boolean contains(String fieldName) {
        return internalMap.containsKey(Objects.requireNonNull(fieldName).toLowerCase(Locale.ROOT));
    }

    /**
     * @throws IndexOutOfBoundsException if the index is out of range
     */
    public Field getField(int index) {
        return internalList.get(index);
    }

    /**
     * @param fieldName non-null required
     * @throws NullPointerException if {@code fieldName} is {@code null}
     */
    public Optional<Field> getField(String fieldName) {
        return Optional.ofNullable(internalMap.get(Objects.requireNonNull(fieldName).toLowerCase(
                Locale.ROOT)));
    }

    /**
     * @throws IndexOutOfBoundsException if the index is out of range
     */
    public StringBuilder getFieldValue(int index) {
        return getField(index).fieldValue();
    }

    /**
     * @param fieldName non-null required
     * @throws NullPointerException if {@code fieldName} is {@code null}
     */
    public Optional<StringBuilder> getFieldValue(String fieldName) {
        return getField(fieldName).map(Field::fieldValue);
    }

    /**
     * @throws IndexOutOfBoundsException if the index is out of range
     */
    public String getFieldValueString(int index) { return getField(index).fieldValueString(); }

    /**
     * @param fieldName non-null required
     * @throws NullPointerException if {@code fieldName} is {@code null}
     */
    public Optional<String> getFieldValueString(String fieldName) {
        return getField(fieldName).map(Field::fieldValueString);
    }

    /**
     * @throws IndexOutOfBoundsException if the index is out of range
     */
    public String[] getFieldValueMembers(int index) {
        return getField(index).fieldValueMembers();
    }

    /**
     * @param fieldName non-null required
     * @throws NullPointerException if {@code fieldName} is {@code null}
     */
    public Optional<String[]> getFieldValueMembers(String fieldName) {
        return getField(fieldName).map(Field::fieldValueMembers);
    }

    public List<Field> toList() { return List.copyOf(internalList); }

    // Modification Operations

    /**
     * @throws BadRequestException if syntax of the field line is invalid
     */
    public void appendFieldLine(String fieldLine) throws BadRequestException {
        String[] fieldNameFieldValue = FIELD_LINE_NAME_VALUE_DELIMITER_PATTERN.split(fieldLine, 2);

        if (fieldNameFieldValue.length < 2)
            throw new BadRequestException("invalid field line syntax");

        String fieldName = fieldNameFieldValue[0];
        String fieldValue = fieldNameFieldValue[1];

        append(fieldName, fieldValue);
    }

    /**
     * @throws NullPointerException if {@code name} or {@code value} is {@code null}
     */
    public void append(String name, String value) {
        Objects.requireNonNull(value);
        Optional<StringBuilder> fieldValueOptional = getFieldValue(name);
        if (fieldValueOptional.isPresent()) {
            fieldValueOptional.get().append(", ").append(value);
        } else {
            final String nameLowerCase = name.toLowerCase(Locale.ROOT);
            Field field = new Field(nameLowerCase, new StringBuilder(value));
            internalList.add(field);
            internalMap.put(nameLowerCase, field);
        }
    }

    /**
     * @throws NullPointerException if {@code name} or {@code value} is {@code null}
     */
    public void set(String name, String value) {
        Objects.requireNonNull(value);
        Optional<StringBuilder> fieldValueOptional = getFieldValue(name);
        if (fieldValueOptional.isPresent()) {
            StringBuilder fieldValue = fieldValueOptional.get();
            fieldValue.replace(0, fieldValue.length(), value);
        } else {
            final String nameLowerCase = name.toLowerCase(Locale.ROOT);
            Field field = new Field(nameLowerCase, new StringBuilder(value));
            internalList.add(field);
            internalMap.put(nameLowerCase, field);
        }
    }


    /**
     * @param fieldName not null
     * @param fieldValue not null
     */
    public record Field(String fieldName, StringBuilder fieldValue) {

        @Override
        public boolean equals(Object obj) {
            return obj instanceof Field field
                    && (CharSequence.compare(field.fieldName, this.fieldName) |
                    CharSequence.compare(field.fieldValue, this.fieldValue)) == 0;
        }

        @Override
        public int hashCode() {
            return toString().hashCode();
        }

        @Override
        public String toString() {
            return fieldName + ": " + fieldValue;
        }

        public String[] fieldValueMembers() {
            return Arrays.stream(OWS_COMMA_OWS_PATTERN.split(fieldValue, 0))
                    .filter((element) -> !element.isEmpty())
                    .toArray(String[]::new);
        }

        public String fieldValueString() { return fieldValue.toString(); }
    }

    @FunctionalInterface
    public interface FieldLineSupplier {

        /**
         * supplies next field line
         * @return next field line, or a string indicating termination
         * @throws BadRequestException if length of a field line exceeds limit
         * @throws IOException if an I/O error occurs
         */
        String supply() throws BadRequestException, IOException;

        default SupplyToConsumer supplyTo(FieldLineConsumer consumer) {
            return new SupplyToConsumer(this, consumer);
        }

    }

    @FunctionalInterface
    public interface FieldLineConsumer {
        /**
         * @throws BadRequestException if the syntax of the field line is invalid
         */
        void consume(String fieldLine) throws BadRequestException;

        default SupplyToConsumer consumeFrom(FieldLineSupplier supplier) {
            return new SupplyToConsumer(supplier, this);
        }

    }

    public record SupplyToConsumer(FieldLineSupplier supplier, FieldLineConsumer consumer) {
        /**
         *
         * @throws BadRequestException if length of a field line exceeds limit, or
         *                             {@code consumer} throws the {@code BadRequestException}
         * @throws IOException if an I/O error occurs
         */
        public void until(StopSupplyCondition stopSupplyCondition)
                throws BadRequestException, IOException {
            String fieldLine;
            while (!stopSupplyCondition.stopOrNot(fieldLine = supplier.supply()))
                consumer.consume(fieldLine);
        }
    }

    @FunctionalInterface
    public interface StopSupplyCondition { boolean stopOrNot(String str); }

}
