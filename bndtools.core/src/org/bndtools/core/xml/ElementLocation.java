package org.bndtools.core.xml;

public class ElementLocation {

    private final TagLocation open;
    private final TagLocation close;

    ElementLocation(TagLocation open, TagLocation close) {
        this.open = open;
        this.close = close;
    }

    public TagLocation getOpen() {
        return open;
    }

    public TagLocation getClose() {
        return close;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((close == null) ? 0 : close.hashCode());
        result = prime * result + ((open == null) ? 0 : open.hashCode());
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
        ElementLocation other = (ElementLocation) obj;
        if (close == null) {
            if (other.close != null)
                return false;
        } else if (!close.equals(other.close))
            return false;
        if (open == null) {
            if (other.open != null)
                return false;
        } else if (!open.equals(other.open))
            return false;
        return true;
    }

    @Override
    public String toString() {
        return "ElementLocation [open=" + open + ", close=" + close + "]";
    }


}
