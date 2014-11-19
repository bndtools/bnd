/*
 * Copyright 2006-2010 Paremus Limited. All rights reserved.
 * PAREMUS PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

package aQute.lib.json;

public class VersionBound {
    private boolean open;
    private Version version;

    public VersionBound(boolean open, Version version) {
        this.open = open;
        this.version = version;
    }

    public boolean isOpen() {
        return open;
    }

    public Version getVersion() {
        return version;
    }

    public VersionBound getHighestUpper(VersionBound other) {
        if (other == null)
            return this;
        int vcomp = version.compareTo(other.version);
        if (vcomp > 0)
            return this;
        else if (vcomp < 0)
            return other;
        else if (this.open)
            return other;
        else
            return this;
    }

    public VersionBound getLowestUpper(VersionBound other) {
        if (other == null)
            return this;
        int vcomp = version.compareTo(other.version);
        if (vcomp > 0)
            return other;
        else if (vcomp < 0)
            return this;
        else if (this.open)
            return this;
        else
            return other;
    }

    public VersionBound getHighestLower(VersionBound other) {
        if (other == null)
            return this;
        int vcomp = version.compareTo(other.version);
        if (vcomp > 0)
            return this;
        else if (vcomp < 0)
            return other;
        else if (this.open)
            return this;
        else
            return other;
    }

    public VersionBound getLowestLower(VersionBound other) {
        if (other == null)
            return other;
        int vcomp = version.compareTo(other.version);
        if (vcomp > 0)
            return other;
        else if (vcomp < 0)
            return this;
        else if (this.open)
            return other;
        else
            return this;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (open ? 1231 : 1237);
        result = prime * result + ((version == null) ? 0 : version.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        VersionBound other = (VersionBound) obj;
        if (open != other.open)
            return false;
        if (version == null) {
            if (other.version != null)
                return false;
        }
        else if (!version.equals(other.version))
            return false;
        return true;
    }

    @Override
    public String toString() {
        return version + "(" + open + ")";
    }

}