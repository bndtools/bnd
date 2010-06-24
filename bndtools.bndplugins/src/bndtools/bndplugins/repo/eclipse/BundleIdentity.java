package bndtools.bndplugins.repo.eclipse;

class BundleIdentity {
    private final String bsn;
    private final String version;

    BundleIdentity(String bsn, String version) {
        this.bsn = bsn;
        this.version = version;
    }

    public String getBsn() {
        return bsn;
    }

    public String getVersion() {
        return version;
    }
}
