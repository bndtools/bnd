/*
 * Copyright 2006-2010 Paremus Limited. All rights reserved.
 * PAREMUS PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

package aQute.lib.json;

import java.io.Serializable;
import java.util.WeakHashMap;

/**
 * OSGi style VersionRange. The OSGi framework includes this concept, but does not have a
 * representation of it in its public API.
 */
public class VersionRange implements Serializable, Comparable<VersionRange> {

    private static final long serialVersionUID = 1L;

    private static WeakHashMap<VersionRange, VersionRange> internedRanges = new WeakHashMap<VersionRange, VersionRange>();
    /**
     * version range that includes every version
     */
    public static final VersionRange ANY_VERSION = new VersionRange(false, Version.ZERO_VERSION,
            Version.INFINITE_VERSION, true).intern();
    // because both ends have the same version and are open, no versions are
    // selected by this range
    public static final VersionRange NO_VERSION = new VersionRange(true, Version.ZERO_VERSION,
            Version.ZERO_VERSION, true).intern();

    private boolean openFloor;
    private Version floor;
    private Version ceiling;
    private boolean openCeiling;
    private transient VersionBound lowerBound;
    private transient VersionBound upperBound;

    public VersionRange intern() {
        VersionRange interned;
        synchronized (internedRanges) {
            interned = internedRanges.get(this);
            if (interned == null) {
                interned = this;
                internedRanges.put(this, this);
            }
        }
        return interned;
    }

    public static VersionRange parseVersionRange(String val) throws Exception {
        if (val == null || val.equals("*") || val.trim().length() == 0) {
            return ANY_VERSION;
        }

        boolean openFloor;
        boolean openCeiling;
        val = val.replaceAll("\\s", "");
        int fst = val.charAt(0);
        if (fst == '[') {
            openFloor = false;
        }
        else if (fst == '(') {
            openFloor = true;
        }
        else {
            Version atLeast = Version.parseVersion(val);
            return new VersionRange(atLeast).intern();
        }

        int lst = val.charAt(val.length() - 1);
        if (lst == ']') {
            openCeiling = false;
        }
        else if (lst == ')') {
            openCeiling = true;
        }
        else {
            throw new Exception("illegal version range syntax "
                    + ": range must end in ')' or ']'");
        }

        String inner = val.substring(1, val.length() - 1);
        String[] floorCeiling = inner.split(",");
        if (floorCeiling.length != 2) {
            throw new Exception("illegal version range syntax " + "too many commas");
        }
        Version floor = Version.parseVersion(floorCeiling[0]);
        Version ceiling = Version.parseVersion(floorCeiling[1]);
        return new VersionRange(openFloor, floor, ceiling, openCeiling).intern();
    }

    /**
     * interval constructor
     * 
     * @param openFloor
     * @param floor
     * @param ceiling
     * @param openCeiling
     */
    public VersionRange(boolean openFloor, Version floor, Version ceiling, boolean openCeiling) {
        this.openFloor = openFloor;
        this.floor = floor.intern();
        this.ceiling = ceiling.intern();
        this.openCeiling = openCeiling;
    }

    /**
     * @param atLeast
     */
    public VersionRange(Version atLeast) {
        this.openFloor = false;
        this.floor = atLeast.intern();
        this.ceiling = Version.INFINITE_VERSION;
        this.openCeiling = true;
    }

    public Version getCeiling() {
        return ceiling;
    }

    public Version getFloor() {
        return floor;
    }

    public boolean isOpenCeiling() {
        return openCeiling;
    }

    public boolean isOpenFloor() {
        return openFloor;
    }

    public boolean isPointVersion() {
        return !openFloor && !openCeiling && floor.equals(ceiling);
    }

    /**
     * test a version to see if it falls in the range
     * 
     * @param version
     * @return
     */
    public boolean contains(Version version) {
        if (version.isInfinite()) {
            return ceiling.isInfinite();
        }
        else {
            int floorComp = version.compareTo(floor);
            int ceilComp = version.compareTo(ceiling);
            return (floorComp > 0 && ceilComp < 0) || (!openFloor && floorComp == 0)
                    || (!openCeiling && ceilComp == 0);
        }
    }

    private VersionBound getLowerBound() {
        if (lowerBound == null) {
            lowerBound = new VersionBound(isOpenFloor(), floor);
        }
        return lowerBound;
    }

    private VersionBound getUpperBound() {
        if (upperBound == null) {
            upperBound = new VersionBound(isOpenCeiling(), ceiling);
        }
        return upperBound;
    }

    public VersionRange intersect(VersionRange that) {
        VersionBound lb = getLowerBound().getHighestLower(that.getLowerBound());
        VersionBound up = getUpperBound().getLowestUpper(that.getUpperBound());
        if (lb.getVersion().compareTo(up.getVersion()) > 0)
            return VersionRange.NO_VERSION;
        if (lb.getVersion().equals(up.getVersion())) {
            if (lb.isOpen() || up.isOpen())
                return lb.getVersion().asRange();
            else
                return VersionRange.NO_VERSION;
        }
        return new VersionRange(lb.isOpen(), lb.getVersion(), up.getVersion(), up.isOpen())
                .intern();
    }

    public boolean contains(VersionRange that) {
        VersionBound lowest = getLowerBound().getLowestLower(that.getLowerBound());
        if (!getLowerBound().equals(lowest))
            return false;

        VersionBound highest = getUpperBound().getHighestUpper(that.getUpperBound());
        if (!getUpperBound().equals(highest))
            return false;

        return true;
    }

    int hashCode = -1;

    @Override
    public int hashCode() {
        if (hashCode == -1) {
            hashCode = 97 * toCanonicalString().hashCode();
        }
        return hashCode;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        return toCanonicalString() == ((VersionRange) obj).toCanonicalString();
    }

    private String canonicalString;

    public String toCanonicalString() {
        if (canonicalString == null) {
            if (!openFloor && ceiling.isInfinite()) {
                canonicalString = floor.toCanonicalString();
            }
            else {
                StringBuilder builder = new StringBuilder();
                char floorChar = openFloor ? '(' : '[';
                char ceilingChar = openCeiling ? ')' : ']';
                builder.append(floorChar).append(floor.toCanonicalString()).append(',')
                        .append(ceiling.toCanonicalString()).append(ceilingChar);
                canonicalString = builder.toString();
            }
            canonicalString = canonicalString.intern();
        }
        return canonicalString;
    }

    @Override
    public String toString() {
        return toCanonicalString();
    }

    private String shortForm;

    public String toShortString() {
        if (shortForm == null) {
            if (VersionRange.ANY_VERSION.equals(this)) {
                shortForm = "*";
            }
            else if (!openFloor && ceiling.isInfinite()) {
                shortForm = floor.toShortString();
            }
            else {
                StringBuilder builder = new StringBuilder();
                char floorChar = openFloor ? '(' : '[';
                char ceilingChar = openCeiling ? ')' : ']';
                builder.append(floorChar).append(floor.toShortString()).append(',')
                        .append(ceiling.toShortString()).append(ceilingChar);
                shortForm = builder.toString();
            }
        }
        return shortForm;
    }

    String ldapString;

    public String toLDAPString() {
        if (ldapString == null) {
            ldapString = toLDAPString("version");
        }
        return ldapString;
    }

    public String toLDAPString(String attrName) {
        String customLDAPStr;
        final String min = "(" + attrName + floorOp(openFloor) + floor.toString() + ")";
        if (ceiling.isInfinite() && !openFloor) {
            customLDAPStr = min;
        }
        else {
            final String max = "(" + attrName + ceilOp(openCeiling) + ceiling.toString() + ")";
            customLDAPStr = "(&" + min + max + ")";
        }
        return customLDAPStr;
    }

    private static String floorOp(boolean open) {
        return open ? ">" : ">=";
    }

    private static String ceilOp(boolean open) {
        return open ? "<" : "<=";
    }

    public int compareTo(VersionRange that) {

        int floorCmp = floor.compareTo(that.floor);
        if (floorCmp != 0)
            return floorCmp;
        else if (openFloor && !that.openFloor) {
            return 1;
        }
        else if (!openFloor && that.openFloor) {
            return -1;
        }

        int ceilCmp = ceiling.compareTo(that.ceiling);
        if (ceilCmp != 0)
            return ceilCmp;
        else if (openCeiling && !that.openCeiling) {
            return -1;
        }
        else if (!openCeiling && that.openCeiling) {
            return 1;
        }

        return 0;
    }
}
