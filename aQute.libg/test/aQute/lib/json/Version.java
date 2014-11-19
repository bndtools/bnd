/*
 * Copyright 2006-2010 Paremus Limited. All rights reserved.
 * PAREMUS PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

package aQute.lib.json;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.util.WeakHashMap;

/**
 * OSGi style version. The OSGi framework's own Version class has been avoided on purpose here, so
 * that this can be used stand alone with the accompanying VersionRange class
 */
public class Version implements Comparable<Version>, Serializable {

    private static final String STRICT_PARSING_PROPERTY = "com.paremus.strict.version.parsing";
    private static final boolean STRICT_PARSING;

    static {
        String strict = System.getProperty(STRICT_PARSING_PROPERTY, "true");
        STRICT_PARSING = "TRUE".equalsIgnoreCase(strict);
    }

    /**
     * 
     */
    private static final long serialVersionUID = 1L;
    private static WeakHashMap<Version, Version> internedVersions = new WeakHashMap<Version, Version>();
    public static final String INFINITY = "infinity";
    public static final Version INFINITE_VERSION = new InfiniteVersion().intern();
    public static final Version ZERO_VERSION = new Version(0, 0, 0, "").intern();
    /**
     * default according to the OSGI R4 spec
     */
    public static final Version DEFAULT_VERSION = ZERO_VERSION;
    public static final String EMPTY_QUALIFIER = "";

    private int major;
    private int minor;
    private int micro;
    private String qualifier;
    private int hashCode = -1;
    private String friendlyString;
    private String canonicalString;

    /**
     * 
     * @param major
     * @param minor
     * @param micro
     * @param qualifier
     *            null is converted into an empty qualifier
     */
    public Version(int major, int minor, int micro, String qualifier) {
        this.major = major;
        this.minor = minor;
        this.micro = micro;
        if (qualifier == null || qualifier.trim().length() == 0) {
            qualifier = EMPTY_QUALIFIER;
        }
        this.qualifier = qualifier;
    }

    public Version intern() {
        Version interned;
        synchronized (internedVersions) {
            interned = internedVersions.get(this);
            if (interned == null) {
                interned = this;
                internedVersions.put(this, this);
            }
        }
        return interned;
    }

    public boolean isInfinite() {
        return false;
    }

    public int getMajor() {
        return major;
    }

    public int getMinor() {
        return minor;
    }

    public int getMicro() {
        return micro;
    }

    public String getQualifier() {
        return qualifier;
    }

    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
    }

    @Override
    public int hashCode() {
        if (hashCode == -1) {
            hashCode = toCanonicalString().hashCode() * 53;
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

        return toCanonicalString() == ((Version) obj).toCanonicalString();
    }

    public int compareTo(Version o) {
        if (o.isInfinite()) {
            return -1;
        }
        if (major != o.major)
            return major - o.major;
        if (minor != o.minor)
            return minor - o.minor;
        if (micro != o.micro)
            return micro - o.micro;
        return qualifier.compareTo(o.qualifier);
    }

    @Override
    public String toString() {
        return toCanonicalString();
    }

    public String toShortString() {
        if (friendlyString == null) {
            StringBuilder builder = new StringBuilder();

            int precision = (!EMPTY_QUALIFIER.equals(qualifier)) ? 3 : (micro != 0 ? 2
                    : (minor != 0 ? 1 : 0));

            builder.append(major);
            if (precision >= 1)
                builder.append(".").append(minor);
            if (precision >= 2)
                builder.append(".").append(micro);
            if (precision == 3)
                builder.append(".").append(qualifier);

            friendlyString = builder.toString();
        }
        return friendlyString;
    }

    public VersionRange asRange() {
        return new VersionRange(false, this, this, false).intern();
    }

    public static Version parseVersion(String versionStr) throws Exception {
        if (versionStr == null || versionStr.trim().length() == 0) {
            return DEFAULT_VERSION;
        }
        else if (INFINITY.equals(versionStr)) {
            return INFINITE_VERSION;
        }
        try {
            String[] parts = versionStr.split("\\.");
            int major = (parts.length > 0) ? Integer.parseInt(parts[0]) : 0;
            int minor = (parts.length > 1) ? Integer.parseInt(parts[1]) : 0;
            int micro = (parts.length > 2) ? Integer.parseInt(parts[2]) : 0;

            String qual = (parts.length > 3) ? parts[3] : EMPTY_QUALIFIER;
            return new Version(major, minor, micro, qual).intern();
        } catch (Exception e) {
            // special handling for the case where only a qualifier is provided.
            // we map myqualifier to 0.0.0.myqualifier
            if (!STRICT_PARSING && versionStr.indexOf('.') == -1) {
                return new Version(0, 0, 0, versionStr);
            }
            else
                throw new Exception("invalid version string: " + versionStr, e);
        }
    }

    public static String canonicalize(String verStr) throws Exception {
        return parseVersion(verStr).toCanonicalString();
    }

    public static String toFriendlyString(String verStr) throws Exception {
        return parseVersion(verStr).toShortString();
    }

    public String toCanonicalString() {
        if (canonicalString == null) {
            canonicalString = buildCanonicalString(major, minor, micro, qualifier).intern();
        }
        return canonicalString;
    }

    private static String buildCanonicalString(int major, int minor, int micro, String qualifier) {
        StringBuilder builder = new StringBuilder();
        builder.append(major).append(".").append(minor).append(".").append(micro);
        if (qualifier != null && !"".equals(qualifier)) {
            builder.append(".").append(qualifier);
        }
        return builder.toString();
    }

    final static class InfiniteVersion extends Version {

        /**
         * 
         */
        private static final long serialVersionUID = 1L;

        private InfiniteVersion() {
            // fields unused
            super(0, 0, 0, "");
        }

        @Override
        public boolean isInfinite() {
            return true;
        }

        @Override
        public int getMajor() {
            throw new UnsupportedOperationException("Infinite versions do not have major parts");
        }

        @Override
        public int getMinor() {
            throw new UnsupportedOperationException("Infinite versions do not have minor parts");
        }

        @Override
        public int getMicro() {
            throw new UnsupportedOperationException("Infinite versions do not have micro parts");
        }

        @Override
        public String getQualifier() {
            throw new UnsupportedOperationException("Infinite versions do not have qualifier parts");
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            return obj.getClass() == InfiniteVersion.class;
        }

        @Override
        public int hashCode() {
            // prime
            return 102797;
        }

        @Override
        public int compareTo(Version o) {
            if (o.isInfinite()) {
                return 0;
            }
            else {
                return 1;
            }

        }

        @Override
        public String toString() {
            return INFINITY;
        }
    }

}
