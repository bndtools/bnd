/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.felix.metatype;

import java.math.BigDecimal;
import java.math.BigInteger;

import org.osgi.service.metatype.AttributeDefinition;

/**
 * Provides various validation routines used by the {@link AD#validate(String)} method.
 *
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
@SuppressWarnings({
    "rawtypes", "unchecked", "deprecation"
})
final class ADValidator {
    /**
     * Validates a given input string according to the type specified by the given attribute definition.
     * <p>
     * The validation is done in the following way:
     * </p>
     * <ul>
     * <li>If the input is undefined (ie. <code>null</code>), and the attribute is mandatory, the validation fails due
     * to a missing value. If the attribute is optional, the input is accepted;</li>
     * <li>If the input represents a <em>boolean</em> value, it is tested whether it is defined (in case of non-zero
     * cardinality) and represents either <tt>"true"</tt> or <tt>"false"</tt>. The minimum and maximum parameters are
     * <b>not</b> used in this validation;</li>
     * <li>If the input represents a <em>character</em> value, it is tested whether it is defined (in case of non-zero
     * cardinality). The character value must be defined within the character range specified by the minimum and maximum
     * parameters (if defined);</li>
     * <li>If the input represents a <em>numeric</em> value, it is tested whether it is defined (in case of non-zero
     * cardinality). The numeric value must be defined within the numeric range specified by the minimum and maximum
     * parameters (if defined);</li>
     * <li>If the input represents a <em>string</em> or <em>password</em>, it is tested whether it is defined (in case
     * of non-zero cardinality). The length of the string value must be in the range specified by the minimum and
     * maximum parameters (if defined).</li>
     * </ul>
     * <p>
     * For all types of attributes, if it defines option values, the input should be present as one of the defined
     * option values.
     * </p>
     *
     * @param ad the attribute definition to use in the validation;
     * @param rawInput the raw input value to validate.
     * @return <code>null</code> if no validation is available, <tt>""</tt> if validation was successful, or any other
     *         non-empty string in case validation fails.
     */
    public static String validate(AD ad, String rawInput) {
        // Handle the case in which the given input is not defined...
        if (rawInput == null) {
            if (ad.isRequired()) {
                return AD.VALIDATE_MISSING;
            }

            return ""; // accept null value...
        }

        // Raw input is defined, validate it further
        String[] input;
        if (ad.getCardinality() == 0) {
            input = new String[] {
                rawInput.trim()
            };
        } else {
            input = AD.splitList(rawInput);
        }

        int type = ad.getType();
        switch (type) {
            case AttributeDefinition.BOOLEAN :
                return validateBooleanValue(ad, input);

            case AttributeDefinition.CHARACTER :
                return validateCharacterValue(ad, input);

            case AttributeDefinition.BIGDECIMAL :
            case AttributeDefinition.BIGINTEGER :
            case AttributeDefinition.BYTE :
            case AttributeDefinition.DOUBLE :
            case AttributeDefinition.FLOAT :
            case AttributeDefinition.INTEGER :
            case AttributeDefinition.LONG :
            case AttributeDefinition.SHORT :
                return validateNumericValue(ad, input);

            case AttributeDefinition.PASSWORD :
            case AttributeDefinition.STRING :
                return validateString(ad, input);

            default :
                return null; // no validation present...
        }
    }

    /**
     * Searches for a given search value in a given array of options.
     *
     * @param searchValue the value to search for;
     * @param optionValues the values to search in.
     * @return <code>null</code> if the given search value is not found in the given options, the searched value if
     *         found, or <tt>""</tt> if no search value or options were given.
     */
    private static String findOptionValue(String searchValue, String[] optionValues) {
        if ((searchValue == null) || (optionValues == null) || (optionValues.length == 0)) {
            // indicates that we've not searched...
            return "";
        }

        for (int i = 0; i < optionValues.length; i++) {
            if (optionValues[i].equals(searchValue)) {
                return optionValues[i];
            }
        }

        return null;
    }

    /**
     * Parses a given string value into a numeric type.
     *
     * @param type the type to parse;
     * @param value the value to parse.
     * @return a {@link Number} representation of the given value, or <code>null</code> if the input was
     *         <code>null</code>, empty, or not a numeric type.
     * @throws NumberFormatException in case the given value cannot be parsed as numeric value.
     */
    private static Comparable parseNumber(int type, String value) throws NumberFormatException {
        if ((value != null) && (value.length() > 0)) {
            switch (type) {
                case AttributeDefinition.BIGDECIMAL :
                    return new BigDecimal(value);
                case AttributeDefinition.BIGINTEGER :
                    return new BigInteger(value);
                case AttributeDefinition.BYTE :
                    return Byte.valueOf(value);
                case AttributeDefinition.SHORT :
                    return Short.valueOf(value);
                case AttributeDefinition.INTEGER :
                    return Integer.valueOf(value);
                case AttributeDefinition.LONG :
                    return Long.valueOf(value);
                case AttributeDefinition.FLOAT :
                    return Float.valueOf(value);
                case AttributeDefinition.DOUBLE :
                    return Double.valueOf(value);
                default :
                    return null;
            }
        }
        return null;
    }

    /**
     * Parses a given string value as character, allowing <code>null</code> -values and empty values to be given as
     * input.
     *
     * @param value the value to parse as character, can be <code>null</code> or an empty value.
     * @return the character value if, and only if, the given input was non- <code>null</code> and a non-empty string.
     */
    private static Character parseOptionalChar(String value) {
        if ((value != null) && (value.length() > 0)) {
            return Character.valueOf(value.charAt(0));
        }
        return null;
    }

    /**
     * Parses a given string value as numeric value, allowing <code>null</code>-values and invalid numeric values to be
     * given as input.
     *
     * @param type the type of number, should only be a numeric type;
     * @param value the value to parse as integer, can be <code>null</code> or a non-numeric value.
     * @return the integer value if, and only if, the given input was non- <code>null</code> and a valid integer
     *         representation.
     */
    private static Comparable parseOptionalNumber(int type, String value) {
        if (value != null) {
            try {
                return parseNumber(type, value);
            } catch (NumberFormatException e) {
                // Ignore; invalid value...
            }
        }
        return null;
    }

    /**
     * Validates a given input string as boolean value.
     *
     * @param ad the attribute definition to use in the validation;
     * @param input the array with input values to validate.
     * @return <code>null</code> if no validation is available, <tt>""</tt> if validation was successful, or any other
     *         non-empty string in case validation fails.
     */
    private static String validateBooleanValue(AD ad, String[] input) {
        for (int i = 0; i < input.length; i++) {
            String value = input[i];
            int length = (value == null) ? 0 : value.length();

            if ((length == 0) && ad.isRequired()) {
                return AD.VALIDATE_MISSING;
            } else if (length > 0 && !"true".equalsIgnoreCase(value) && !"false".equalsIgnoreCase(value)) {
                return AD.VALIDATE_INVALID_VALUE;
            }
        }

        String[] optionValues = ad.getOptionValues();
        if ((optionValues != null) && (optionValues.length > 0)) {
            return null; // no validation possible for this type...
        }

        return ""; // accept given value...
    }

    /**
     * Validates a given input string as character value.
     *
     * @param ad the attribute definition to use in the validation;
     * @param input the array with input values to validate.
     * @return <code>null</code> if no validation is available, <tt>""</tt> if validation was successful, or any other
     *         non-empty string in case validation fails.
     */
    private static String validateCharacterValue(AD ad, String[] input) {
        Character min = parseOptionalChar(ad.getMin());
        Character max = parseOptionalChar(ad.getMax());
        String[] optionValues = ad.getOptionValues();

        for (int i = 0; i < input.length; i++) {
            Character ch = null;

            int length = (input[i] == null) ? 0 : input[i].length();
            if (length > 1) {
                return AD.VALIDATE_GREATER_THAN_MAXIMUM;
            } else if ((length == 0) && ad.isRequired()) {
                return AD.VALIDATE_MISSING;
            } else if (length == 1) {
                ch = Character.valueOf(input[i].charAt(0));
                // Check whether the minimum value is adhered for all values...
                if ((min != null) && (ch.compareTo(min) < 0)) {
                    return AD.VALIDATE_LESS_THAN_MINIMUM;
                }
                // Check whether the maximum value is adhered for all values...
                if ((max != null) && (ch.compareTo(max) > 0)) {
                    return AD.VALIDATE_GREATER_THAN_MAXIMUM;
                }
            }

            if (findOptionValue(input[i], optionValues) == null) {
                return AD.VALIDATE_NOT_A_VALID_OPTION;
            }
        }

        return ""; // accept given value...
    }

    /**
     * Validates a given input string as numeric value.
     *
     * @param ad the attribute definition to use in the validation;
     * @param input the array with input values to validate.
     * @return <code>null</code> if no validation is available, <tt>""</tt> if validation was successful, or any other
     *         non-empty string in case validation fails.
     */
    private static String validateNumericValue(AD ad, String[] input) {
        Comparable min = parseOptionalNumber(ad.getType(), ad.getMin());
        Comparable max = parseOptionalNumber(ad.getType(), ad.getMax());
        String[] optionValues = ad.getOptionValues();

        for (int i = 0; i < input.length; i++) {
            Comparable value = null;
            try {
                value = parseNumber(ad.getType(), input[i]);
            } catch (NumberFormatException e) {
                return AD.VALIDATE_INVALID_VALUE;
            }

            if ((value == null) && ad.isRequired()) {
                // Possible if the cardinality != 0 and input was something like
                // "0,,1"...
                return AD.VALIDATE_MISSING;
            }
            // Check whether the minimum value is adhered for all values...
            if ((min != null) && (value != null) && (value.compareTo(min) < 0)) {
                return AD.VALIDATE_LESS_THAN_MINIMUM;
            }
            // Check whether the maximum value is adhered for all values...
            if ((max != null) && (value != null) && (value.compareTo(max) > 0)) {
                return AD.VALIDATE_GREATER_THAN_MAXIMUM;
            }

            if (findOptionValue(input[i], optionValues) == null) {
                return AD.VALIDATE_NOT_A_VALID_OPTION;
            }
        }

        return ""; // accept given value...
    }

    /**
     * Validates a given input string as string (or password).
     *
     * @param ad the attribute definition to use in the validation;
     * @param input the array with input values to validate.
     * @return <code>null</code> if no validation is available, <tt>""</tt> if validation was successful, or any other
     *         non-empty string in case validation fails.
     */
    private static String validateString(AD ad, String[] input) {
        // The length() method of a string yields an Integer, so the maximum string length is 2^31-1...
        Integer min = (Integer) parseOptionalNumber(AttributeDefinition.INTEGER, ad.getMin());
        Integer max = (Integer) parseOptionalNumber(AttributeDefinition.INTEGER, ad.getMax());
        String[] optionValues = ad.getOptionValues();

        for (int i = 0; i < input.length; i++) {
            String value = input[i];
            int length = (value == null) ? 0 : value.length();

            if (ad.isRequired() && ((value == null) || (length == 0))) {
                // Possible if the cardinality != 0 and input was something like
                // "0,,1"...
                return AD.VALIDATE_MISSING;
            }
            // Check whether the minimum length is adhered for all values...
            if ((min != null) && (length < min.intValue())) {
                return AD.VALIDATE_LESS_THAN_MINIMUM;
            }
            // Check whether the maximum length is adhered for all values...
            if ((max != null) && (length > max.intValue())) {
                return AD.VALIDATE_GREATER_THAN_MAXIMUM;
            }

            if (findOptionValue(value, optionValues) == null) {
                return AD.VALIDATE_NOT_A_VALID_OPTION;
            }
        }

        return ""; // accept given value...
    }
}
