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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.osgi.service.metatype.AttributeDefinition;

import bndtools.Plugin;

/**
 * The <code>AD</code> class represents the <code>AD</code> element of the meta type descriptor.
 *
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
@SuppressWarnings({
        "rawtypes", "unchecked"
})
public class AD extends OptionalAttributes {

    /**
     * The message returned from the {@link #validate(String)} method if the value is not any of the specified
     * {@link #getOptionValues() option values} (value is "%not a valid option").
     */
    public static final String VALIDATE_NOT_A_VALID_OPTION = "%not a valid option";

    /**
     * The message returned from the {@link #validate(String)} method if the value is invalid considering its type
     * (value is "%invalid value").
     */
    public static final String VALIDATE_INVALID_VALUE = "%invalid value";

    /**
     * The message returned from the {@link #validate(String)} method if the value is greater than the specified
     * {@link #getMax() maximum value} (value is "%greater than maximum").
     */
    public static final String VALIDATE_GREATER_THAN_MAXIMUM = "%greater than maximum";

    /**
     * The message returned from the {@link #validate(String)} method if the value is less than the specified
     * {@link #getMin() minimum value} (value is "%less than minimum").
     */
    public static final String VALIDATE_LESS_THAN_MINIMUM = "%less than minimum";

    /**
     * The message returned from the {@link #validate(String)} method if the value is null or cannot be converted to an
     * attribute value and a value is {@link #isRequired() required} (value is "%missing required value").
     */
    public static final String VALIDATE_MISSING = "%missing required value";

    private String id;
    private String name;
    private String description;
    private int type;
    private int cardinality = 0;
    private String[] optionLabels;
    private String[] optionValues;
    private String[] defaultValue;
    private String min;
    private String max;
    private boolean isRequired = true;

    public String getID() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public int getType() {
        return type;
    }

    public int getCardinality() {
        return cardinality;
    }

    public String[] getOptionLabels() {
        return optionLabels;
    }

    public String[] getOptionValues() {
        return optionValues;
    }

    public String[] getDefaultValue() {
        return defaultValue;
    }

    public String getMin() {
        return min;
    }

    public String getMax() {
        return max;
    }

    public boolean isRequired() {
        return isRequired;
    }

    /**
     * Implements validation of the <code>valueString</code> and returns an indication of the validation result.
     *
     * @param valueString
     *            The string representation of the value to validate, can be <code>null</code>.
     * @return <code>null</code> if no validation is performed, <tt>""</tt> if the value is accepted as valid, or a
     *         non-empty string indicating a validation problem was found.
     * @see ADValidator#validate(AD, String)
     * @see #VALIDATE_GREATER_THAN_MAXIMUM
     * @see #VALIDATE_NOT_A_VALID_OPTION
     * @see #VALIDATE_LESS_THAN_MINIMUM
     * @see #VALIDATE_INVALID_VALUE
     * @see #VALIDATE_MISSING
     */
    public String validate(String valueString) {
        return ADValidator.validate(this, valueString);
    }

    //--------- Setters for setting up this instance --------------------------

    /**
     * @param id
     *            the id to set
     */
    public void setID(String id) {
        this.id = id;
    }

    /**
     * @param name
     *            the name to set
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * @param description
     *            the description to set
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * @param typeString
     *            the type to set
     */
    public void setType(String typeString) {
        this.type = toType(typeString);
    }

    /**
     * @param cardinality
     *            the cardinality to set
     */
    public void setCardinality(int cardinality) {
        this.cardinality = cardinality;
    }

    /**
     * @param options
     *            the options to set
     */
    public void setOptions(Map options) {
        optionLabels = new String[options.size()];
        optionValues = new String[options.size()];
        int i = 0;
        for (Iterator oi = options.entrySet().iterator(); oi.hasNext(); i++) {
            Map.Entry entry = (Map.Entry) oi.next();
            optionValues[i] = String.valueOf(entry.getKey());
            optionLabels[i] = String.valueOf(entry.getValue());
        }
    }

    /**
     * Sets the default value(s) for this AD.
     * <p>
     * NOTE: this method is depending on the value of {@link #getCardinality()}! Make sure that the cardinality is
     * properly set <b>before</b> calling this method.
     * </p>
     *
     * @param defaultValue
     *            the default value to set, as encoded string-value (using comma's as separator), can be
     *            <code>null</code>.
     */
    public void setDefaultValue(String defaultValue) {
        setDefaultValue(splitList(defaultValue), Math.abs(this.cardinality));
    }

    /**
     * @param min
     *            the min to set
     */
    public void setMin(String min) {
        this.min = min;
    }

    /**
     * @param max
     *            the max to set
     */
    public void setMax(String max) {
        this.max = max;
    }

    /**
     * @param isRequired
     *            the isRequired to set
     */
    public void setRequired(boolean isRequired) {
        this.isRequired = isRequired;
    }

    public static int toType(String typeString) {
        if ("String".equals(typeString)) {
            return AttributeDefinition.STRING;
        } else if ("Long".equals(typeString)) {
            return AttributeDefinition.LONG;
        } else if ("Double".equals(typeString)) {
            return AttributeDefinition.DOUBLE;
        } else if ("Float".equals(typeString)) {
            return AttributeDefinition.FLOAT;
        } else if ("Integer".equals(typeString)) {
            return AttributeDefinition.INTEGER;
        } else if ("Byte".equals(typeString)) {
            return AttributeDefinition.BYTE;
        } else if ("Character".equals(typeString) || "Char".equals(typeString)) {
            return AttributeDefinition.CHARACTER;
        } else if ("Boolean".equals(typeString)) {
            return AttributeDefinition.BOOLEAN;
        } else if ("Short".equals(typeString)) {
            return AttributeDefinition.SHORT;
        } else if ("Password".equals(typeString)) {
            return AttributeDefinition.PASSWORD;
        }

        // finally fall back to string for illegal values
        return AttributeDefinition.STRING;
    }

    public static String[] splitList(String listString) {
        if (listString == null) {
            return null;
        } else if (listString.length() == 0) {
            return new String[] {
                    ""
            };
        }

        List strings = new ArrayList();
        StringBuffer sb = new StringBuffer();

        int length = listString.length();
        boolean escaped = false;
        int spaceCount = 0;
        boolean start = true;
        for (int i = 0; i < length; i++) {
            char ch = listString.charAt(i);
            final boolean isWhitespace = Character.isWhitespace(ch);
            if (start) {
                if (isWhitespace) {
                    continue;
                }
                start = false;
            }
            if (ch == '\\') {
                if (!escaped) {
                    escaped = true;
                    continue;
                }
            } else if (ch == ',') {
                if (!escaped) {
                    // unescaped comma, this is a string delimiter...
                    strings.add(sb.toString());
                    sb.setLength(0);
                    start = true;
                    spaceCount = 0;
                    continue;
                }
            } else if (ch == ' ') {
                // space is only ignored at beginning and end but not if escaped
                if (!escaped) {
                    spaceCount++;
                    continue;
                }
            } else if (isWhitespace) {
                // Other whitespaces are ignored...
                continue;
            }

            if (spaceCount > 0) {
                for (int m = 0; m < spaceCount; m++) {
                    sb.append(" ");
                }
                spaceCount = 0;
            }
            sb.append(ch);
            escaped = false;
        }

        // Always add the last string, as it contains everything after the last comma...
        strings.add(sb.toString());

        return (String[]) strings.toArray(new String[strings.size()]);
    }

    protected Comparable convertToType(final String value) {
        if (value != null && value.length() > 0) {
            try {
                switch (getType()) {
                case AttributeDefinition.BOOLEAN :
                    // Boolean is only Comparable starting with Java 5
                    return new ComparableBoolean(value);
                case AttributeDefinition.CHARACTER :
                    return Character.valueOf(value.charAt(0));
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
                case AttributeDefinition.STRING :
                case AttributeDefinition.PASSWORD :
                default :
                    return value;
                }
            } catch (NumberFormatException nfe) {
                Plugin.getDefault().getLog().log(new Status(IStatus.INFO, Plugin.PLUGIN_ID, 0, "Cannot convert value '" + value + "'", nfe));
            }
        }

        return null;
    }

    /**
     * @param values
     *            the defaultValue to set
     */
    protected void setDefaultValue(String[] values, int cardinality) {
        if (values != null) {
            int count = 0;
            int max = Math.min(values.length, Math.max(1, cardinality));
            for (int i = 0; count < max && i < values.length; i++) {
                if ("".equals(ADValidator.validate(this, values[i]))) {
                    count++;
                } else {
                    values[i] = null;
                }
            }
            if (count == 0) {
                values = cardinality == 0 ? null : new String[0];
            } else if (count != values.length) {
                String[] filterValues = new String[count];
                int index = 0;
                for (int i = 0; index < count && i < values.length; i++) {
                    if (values[i] != null) {
                        filterValues[index] = values[i];
                        index++;
                    }
                }
                values = filterValues;
            }
        }
        this.defaultValue = values;
    }

    private static class ComparableBoolean implements Comparable {
        private final boolean value;

        ComparableBoolean(String boolValue) {
            value = Boolean.valueOf(boolValue).booleanValue();
        }

        @Override
        public int compareTo(Object obj) {
            ComparableBoolean cb = (ComparableBoolean) obj;
            return (cb.value == value ? 0 : (value ? 1 : -1));
        }
    }
}
