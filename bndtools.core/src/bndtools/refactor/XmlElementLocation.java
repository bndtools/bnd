package bndtools.refactor;

public class XmlElementLocation {

    private final long openStart;
    private final long openEnd;
    private final long closeStart;
    private final long closeEnd;

    XmlElementLocation(long openStart, long openEnd, long closeStart, long closeEnd) {
        this.openStart = openStart;
        this.openEnd = openEnd;
        this.closeStart = closeStart;
        this.closeEnd = closeEnd;
    }

    public long getOpenStart() {
        return openStart;
    }

    public long getOpenEnd() {
        return openEnd;
    }

    public long getCloseStart() {
        return closeStart;
    }

    public long getCloseEnd() {
        return closeEnd;
    }

}
