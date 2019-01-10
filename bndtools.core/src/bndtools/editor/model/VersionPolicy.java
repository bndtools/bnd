/*******************************************************************************
 * Copyright (c) 2010 Neil Bartlett.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Neil Bartlett - initial API and implementation
 *******************************************************************************/
package bndtools.editor.model;

public class VersionPolicy {
    private final LowerVersionMatchType lowerMatch;
    private final UpperVersionMatchType upperMatch;
    private final boolean upperInclusive;

    public VersionPolicy(LowerVersionMatchType lowerMatch, UpperVersionMatchType upperMatch, boolean upperInclusive) {
        assert lowerMatch != null;
        this.lowerMatch = lowerMatch;
        this.upperMatch = upperMatch;
        this.upperInclusive = upperInclusive;
    }

    static VersionPolicy parse(String string) throws IllegalArgumentException {
        String lowerSegment;
        String upperSegment;
        boolean upperInclusive;

        if (string.charAt(0) == '[') {
            int commaIndex = string.indexOf(',');
            if (commaIndex < 0)
                throw new IllegalArgumentException("Failed to parse version policy.");
            lowerSegment = string.substring(1, commaIndex);

            char lastChar = string.charAt(string.length() - 1);
            if (lastChar == ')')
                upperInclusive = false;
            else if (lastChar == ']')
                upperInclusive = true;
            else
                throw new IllegalArgumentException("Failed to parse version policy.");

            upperSegment = string.substring(commaIndex + 1, string.length() - 1);
        } else {
            lowerSegment = string;
            upperSegment = null;
            upperInclusive = true;
        }

        LowerVersionMatchType lower = LowerVersionMatchType.parse(lowerSegment);
        UpperVersionMatchType upper = upperSegment != null ? UpperVersionMatchType.parse(upperSegment) : null;

        return new VersionPolicy(lower, upper, upperInclusive);
    }

    public LowerVersionMatchType getLowerMatch() {
        return lowerMatch;
    }

    public UpperVersionMatchType getUpperMatch() {
        return upperMatch;
    }

    public boolean isUpperInclusive() {
        return upperInclusive;
    }

    @Override
    public String toString() {
        StringBuilder buffer = new StringBuilder();

        if (upperMatch != null) {
            buffer.append('[');
            buffer.append(lowerMatch.getRepresentation());
            buffer.append(',');
            buffer.append(upperMatch.getRepresentation());
            buffer.append(upperInclusive ? ']' : ')');
        } else {
            buffer.append(lowerMatch.getRepresentation());
        }

        return buffer.toString();
    }
}
