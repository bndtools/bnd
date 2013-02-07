package bndtools.versioncontrol;

import java.util.Map;
import java.util.TreeMap;

import bndtools.Logger;
import bndtools.api.ILogger;

/**
 * Supported version control systems
 */
public enum VersionControlSystem {
    /*
     * START of code to modify to add version control systems
     */

    GIT("Git");

    /**
     * @return True when the version control system can store empty directories, false otherwise
     * @throws IllegalArgumentException
     *             When the version control system is unknown
     */
    public boolean canStoreEmptyDirectories() throws IllegalArgumentException {
        switch (this) {
        case GIT :
            return false;

        default :
            throw new IllegalArgumentException("Unknown version control system " + this.getName());
        }
    }

    /**
     * @return The (simple) filename to use for an ignore file
     * @throws IllegalArgumentException
     *             When the version control system is unknown
     */
    public String getIgnoreFilename() throws IllegalArgumentException {
        switch (this) {
        case GIT :
            return ".gitignore";

        default :
            throw new IllegalArgumentException("Unknown version control system " + this.getName());
        }
    }

    /**
     * Convert a Git ignore entry to the format required by the version control system.
     * 
     * @param gitIgnoreEntry
     *            The ignore entry in Git format
     * @return The ignore entry in the format required by the target version control system
     * @throws IllegalArgumentException
     *             When the version control system is unknown
     */
    public String getIgnoreEntryFromGitIgnoreEntry(String gitIgnoreEntry) throws IllegalArgumentException {
        switch (this) {
        case GIT :
            return gitIgnoreEntry;

        default :
            throw new IllegalArgumentException("Unknown version control system " + this.getName());
        }
    }

    /**
     * Convert an ignore entry from the format required by the version control system to the Git format.
     * 
     * @param ignoreEntry
     *            The ignore entry in the format required by the version control system
     * @return The ignore entry in Git format
     * @throws IllegalArgumentException
     *             When the version control system is unknown
     */
    public String getGitIgnoreEntryFromIgnoreEntry(String ignoreEntry) throws IllegalArgumentException {
        switch (this) {
        case GIT :
            return ignoreEntry;

        default :
            throw new IllegalArgumentException("Unknown version control system " + this.getName());
        }
    }

    /**
     * Determine the version control system from a repository provider ID
     * 
     * @param repositoryProviderId
     *            the repository provider ID
     * @return the version control system, or null in case the repository provider ID is unknown
     */
    public static VersionControlSystem fromRepositoryProviderId(String repositoryProviderId) {
        if ("org.eclipse.egit.core.GitProvider".equals(repositoryProviderId)) {
            return GIT;
        }

        logger.logError("Unknown repository provider ID encountered: " + repositoryProviderId, null);
        return null;
    }

    /*
     * END of code to modify to add version control systems
     */

    /** the logger */
    private static final ILogger logger = Logger.getLogger();

    /**
     * Constructor.
     * 
     * @param name
     *            The friendly name of the version control system
     */
    private VersionControlSystem(String name) {
        this.name = name;
    }

    /** The friendly name of the version control system */
    private String name;

    /**
     * @return The friendly name of the version control system
     */
    public String getName() {
        return name;
    }

    /** a map for the reverseOrdinal function */
    private static final Map<Integer,VersionControlSystem> reverseOrdinalMap;

    /** initialise the reverse ordinal map */
    static {
        reverseOrdinalMap = new TreeMap<Integer,VersionControlSystem>();
        VersionControlSystem[] vcsEntries = VersionControlSystem.values();
        for (VersionControlSystem vcsEntry : vcsEntries) {
            reverseOrdinalMap.put(Integer.valueOf(vcsEntry.ordinal()), vcsEntry);
        }
    }

    /**
     * Performs a reverse ordinal lookup.
     * 
     * @param index
     *            the ordinal of the enum
     * @return the corresponding enum, or null when not found
     */
    public static VersionControlSystem reverseOrdinal(int index) {
        return reverseOrdinalMap.get(Integer.valueOf(index));
    }

    /**
     * Sanitise a Git ignore entry (a path) so that it is a properly formatted ignore entry in Git format.
     * 
     * @param ignoreEntry
     *            The ignore entry (the path)
     * @param directory
     *            True when the path denotes a directory
     * @param rooted
     *            True when the ignore entry must be rooted in the directory of the ignore file
     * @return The (sanitised) ignore in Git format
     */
    public static String sanitiseGitIgnoreEntry(String ignoreEntry, boolean directory, boolean rooted) {
        /* replace all consecutive slashes with a single slash */
        String newPath = ignoreEntry.replaceAll("/+", "/");

        /* remove all leading slashes (with optional leading whitespace) */
        newPath = newPath.replaceAll("^\\s*/+", "");

        /* remove all trailing slashes (with optional trailing whitespace) */
        newPath = newPath.replaceAll("/+\\s*$", "");

        if (rooted) {
            newPath = "/" + newPath;
        }
        if (directory) {
            newPath = newPath + "/";
        }

        return newPath;
    }
}
