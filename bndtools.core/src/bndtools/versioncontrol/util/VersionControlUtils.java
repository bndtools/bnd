package bndtools.versioncontrol.util;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.LinkedList;
import java.util.List;
import java.util.StringTokenizer;

import org.bndtools.api.ILogger;
import org.bndtools.api.Logger;
import org.bndtools.api.ProjectPaths;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.team.core.RepositoryProvider;

import bndtools.preferences.BndPreferences;
import bndtools.types.Pair;
import bndtools.versioncontrol.VersionControlSystem;

/**
 * <p>
 * Version Control Utilities.
 * </p>
 * <p>
 * Currently used for 'ignore' functionality.
 * </p>
 * <p>
 * Note: we will internally _always_ work with ignore entries in the Git format and convert to/from the desired version
 * control system format on demand.
 * </p>
 */
public class VersionControlUtils {
    /** the logger */
    private static final ILogger logger = Logger.getLogger(VersionControlUtils.class);

    /**
     * <p>
     * Returns the version control system to use for a project.
     * </p>
     * <p>
     * If the preferences say 'never generate ignore files' then return null. Otherwise try to detect whether the
     * project is already under version control, and if so, use that version control system (if it is known). If the
     * project is not yet under version control then look at the preferences for the version control system to use.
     * </p>
     * 
     * @param javaProject
     *            The project
     * @return The version control system to use for the project (if detected or configured), null otherwise
     * @throws IllegalArgumentException
     *             When javaProject is null
     */
    private static VersionControlSystem getProjectVersionControlSystem(IJavaProject javaProject) throws IllegalArgumentException {
        if (javaProject == null) {
            throw new IllegalArgumentException("Can't get the version control system to use for a null project");
        }

        BndPreferences prefs = new BndPreferences();
        if (!prefs.getVcsCreateIgnoreFiles()) {
            /* the preferences say 'never generate ignore files': return null */
            return null;
        }

        RepositoryProvider repositoryProvider = RepositoryProvider.getProvider(javaProject.getProject());
        if (repositoryProvider != null) {
            VersionControlSystem vcs = VersionControlSystem.fromRepositoryProviderId(repositoryProvider.getID());
            if (vcs != null) {
                return vcs;
            }
        }

        /* 
         * No version control detected or an unknown version control system: 
         * look at the preference to determine which version control system
         * to use
         */

        return VersionControlSystem.reverseOrdinal(prefs.getVcsVcs());
    }

    /**
     * Read an ignore file: convert all entries to ignore entries in Git format and put them into a list (because order
     * has to be preserved).
     * 
     * @param versionControlSystem
     *            The version control system of the ignore file
     * @param ignoreFile
     *            The ignore file
     * @return A (non-null) list of ignore entries in Git format, as read from the ignore file. Empty when the file does
     *         not exists or is not a regular file.
     * @throws IllegalArgumentException
     *             When versionControlSystem and/or ignoreFile are null
     * @throws IOException
     *             When the ignore file could not be fully read (for example due to the ignore file not being an regular
     *             file or due to an IOException)
     */
    private static List<String> readIgnoreFile(VersionControlSystem versionControlSystem, IFile ignoreFile) throws IllegalArgumentException, IOException {
        if (versionControlSystem == null || ignoreFile == null) {
            throw new IllegalArgumentException("Can't read an ignore file for a null version control system and/or a null ignore file");
        }

        List<String> result = new LinkedList<String>();

        if (!ignoreFile.exists()) {
            return result;
        }

        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new InputStreamReader(ignoreFile.getContents(), "UTF-8"));

            String line;
            while ((line = reader.readLine()) != null) {
                result.add(versionControlSystem.getGitIgnoreEntryFromIgnoreEntry(line));
            }
        } catch (Exception e) {
            throw new IOException("Error reading ignore file " + ignoreFile.getFullPath().toOSString() + ": " + e.getMessage());
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    /* swallow */
                }
            }
        }

        return result;
    }

    /**
     * Add ignore entries to an ignore file in a project. If the ignore file doesn't exist then it will be created.
     * After that the new ignore entries that do not already exist in the ignore file will be added at the end.
     * 
     * @param javaProject
     *            The project in which to create the ignore file.
     * @param ignoreFolder
     *            The folder in which the ignore file must be placed. Use null to use the root of the project.
     * @param ignores
     *            A list (because order must be preserved) of ignore entries (in Git format) to write to the ignore
     *            file. Can be null, which is the same as an empty set.
     * @throws IllegalArgumentException
     *             When javaProject is null, or when the version control system is unknown
     * @throws IOException
     *             When the folder for the ignore file could not be created or if it didn't exist
     */
    public static void addToIgnoreFile(IJavaProject javaProject, IFolder ignoreFolder, List<String> ignores) throws IllegalArgumentException, IOException {
        if (javaProject == null) {
            throw new IllegalArgumentException("Can't create an ignore file for a null project");
        }

        VersionControlSystem vcs = getProjectVersionControlSystem(javaProject);
        if (vcs == null) {
            return;
        }

        List<String> newIgnores = ignores;
        if (newIgnores == null) {
            newIgnores = new LinkedList<String>();
        }

        IFile ignoreIFile;
        if (ignoreFolder == null) {
            ignoreIFile = javaProject.getProject().getFile(vcs.getIgnoreFilename());
        } else {
            ignoreIFile = ignoreFolder.getFile(vcs.getIgnoreFilename());
        }

        File ignoreFile = new File(ignoreIFile.getLocationURI());

        /* determine if the ignoreFolder is empty */
        File ignoreFileDir = ignoreFile.getParentFile();
        boolean ignoreFolderIsEmpty = !ignoreFileDir.exists() || (ignoreFileDir.list().length == 0);

        /* when an empty ignore file is not needed then exit */
        boolean needIgnoreFile = !newIgnores.isEmpty() || (ignoreFolderIsEmpty && !vcs.canStoreEmptyDirectories());
        if (!needIgnoreFile) {
            return;
        }

        /* create the ignoreFolder of the ignore file, when needed */
        if (!ignoreFileDir.exists() && !ignoreFileDir.mkdirs()) {
            throw new IOException("Could not create directory " + ignoreFileDir.getPath());
        }

        List<String> ignoresToAppend;
        if (!ignoreFile.exists()) {
            ignoresToAppend = newIgnores;
        } else {
            ignoresToAppend = new LinkedList<String>();

            /* refresh the file */
            try {
                ignoreIFile.refreshLocal(0, null);
            } catch (CoreException e) {
                throw new IOException("Could not refresh ignore file " + ignoreIFile.getFullPath().toOSString() + ": " + e.getLocalizedMessage());
            }

            /* read the current ignores */
            List<String> currentIgnores = readIgnoreFile(vcs, ignoreIFile);

            /* add new ignores to the current ignores, but only if the current ignores did not contain them */
            for (String newIgnore : newIgnores) {
                if (!currentIgnores.contains(newIgnore)) {
                    ignoresToAppend.add(newIgnore);
                }
            }

            /* exit when we have no new ignores to write */
            if (ignoresToAppend.isEmpty()) {
                return;
            }
        }

        /* write out the ignore file */
        StringBuilder sb = new StringBuilder();
        String newLine = String.format("%n");
        for (String ignoreToAppend : ignoresToAppend) {
            sb.append(vcs.getIgnoreEntryFromGitIgnoreEntry(ignoreToAppend));
            sb.append(newLine);
        }

        try {
            if (!ignoreFile.exists()) {
                ignoreIFile.create(new ByteArrayInputStream(sb.toString().getBytes("UTF-8")), IResource.FORCE, null);
            } else {
                ignoreIFile.appendContents(new ByteArrayInputStream(sb.toString().getBytes("UTF-8")), IResource.FORCE, null);
            }
        } catch (CoreException e) {
            throw new IOException("Could not write to ignore file " + ignoreIFile.getFullPath().toOSString() + " : " + e.getLocalizedMessage());
        }
    }

    /**
     * <p>
     * Add ignore entries to an ignore file in a project.
     * </p>
     * <p>
     * This is a convenience method for {@link #addToIgnoreFile(IJavaProject, IFolder, List)}:
     * </p>
     * 
     * @param javaProject
     *            The project in which to create the ignore file.
     * @param ignoreFolder
     *            The folder in which the ignore file must be placed. Use null to use the root of the project.
     * @param ignores
     *            A comma-separated list of ignore entries to write to the ignore file (in Git ignore format). Can be
     *            null, which is the same as ""
     * @throws IllegalArgumentException
     *             When javaProject is null, or when the version control system is unknown
     * @throws IOException
     *             When the folder for the ignore file could not be created or if it didn't exist
     */
    public static void addToIgnoreFile(IJavaProject javaProject, IFolder ignoreFolder, String ignores) throws IllegalArgumentException, IOException {
        List<String> ignoredEntries = null;
        if (ignores != null) {
            ignoredEntries = new LinkedList<String>();
            StringTokenizer tokenizer = new StringTokenizer(ignores, ",");
            while (tokenizer.hasMoreTokens()) {
                ignoredEntries.add(tokenizer.nextToken());
            }
        }

        addToIgnoreFile(javaProject, ignoreFolder, ignoredEntries);
    }

    /**
     * Create the (default) ignore file(s) for a project. It will create:
     * <ul>
     * <li>an empty ignore file in the src directory when that directory is empty and the version control system of the
     * project can't store empty directories</li>
     * <li>an empty ignore file in the test directory when that directory is empty and the version control system of the
     * project can't store empty directories</li>
     * <li>an ignore file in the root of the project that ignores the output locations of the src and test directories</li>
     * 
     * @param projectPaths
     *            The project paths
     * @param javaProject
     *            The project
     * @throws IllegalArgumentException
     *             When project is null
     * @throws IOException
     *             When the folder for the ignore file could not be created or if it didn't exist
     */
    public static void createDefaultProjectIgnores(ProjectPaths projectPaths, IJavaProject javaProject) throws IllegalArgumentException, IOException {
        if (javaProject == null) {
            throw new IllegalArgumentException("Can't create ignore files for a null project");
        }

        List<Pair<String,String>> sourceOutputLocations = new LinkedList<Pair<String,String>>();

        /* access the project classpath to determine the source folders and their output locations */
        IClasspathEntry[] rawClasspath = null;
        IPath defaultOutputLocation = null;
        try {
            rawClasspath = javaProject.getRawClasspath();
            defaultOutputLocation = javaProject.getOutputLocation();
        } catch (Exception e) {
            logger.logError("Could not access the project classpath for " + javaProject.getProject().getName() + ", falling back to using the defaults in the ignore file(s)", e);
        }

        if (rawClasspath != null && defaultOutputLocation != null) {
            IPath projectRootPath = javaProject.getPath();
            for (IClasspathEntry rawClasspathEntry : rawClasspath) {
                if (rawClasspathEntry.getEntryKind() == IClasspathEntry.CPE_SOURCE) {
                    IPath srcDir = rawClasspathEntry.getPath();
                    IPath outputLocation = rawClasspathEntry.getOutputLocation();

                    if (outputLocation == null) {
                        outputLocation = defaultOutputLocation;
                    }

                    assert (srcDir != null);
                    assert (outputLocation != null);

                    sourceOutputLocations.add(new Pair<String,String>(srcDir.makeRelativeTo(projectRootPath).toString(), outputLocation.makeRelativeTo(projectRootPath).toString()));
                }
            }
        } else {
            /* fallback to using defaults */
            sourceOutputLocations.add(new Pair<String,String>(projectPaths.getSrc(), projectPaths.getBin()));
            sourceOutputLocations.add(new Pair<String,String>(projectPaths.getTestSrc(), projectPaths.getTestBin()));
        }

        List<String> emptyIgnores = new LinkedList<String>();
        List<String> rootIgnores = new LinkedList<String>();

        for (Pair<String,String> sourceOutputLocation : sourceOutputLocations) {
            String srcDir = sourceOutputLocation.getFirst();
            String binDir = sourceOutputLocation.getSecond();
            assert (srcDir != null);
            assert (binDir != null);

            IFolder srcFolder = javaProject.getProject().getFolder(srcDir);

            /* create an ignore file in the source directory of the project when needed */
            VersionControlUtils.addToIgnoreFile(javaProject, srcFolder, emptyIgnores);

            /* add the corresponding output location to the ignore entries for the ignore file in the root of the project */
            rootIgnores.add(VersionControlSystem.sanitiseGitIgnoreEntry(binDir, true, true));
        }

        /* add the target directory to the ignores */
        VersionControlUtils.addToIgnoreFile(javaProject, null, "/" + projectPaths.getTargetDir() + "/");

        /* create an ignore file in the root of the project if there are entries to ignore */
        VersionControlUtils.addToIgnoreFile(javaProject, null, rootIgnores);
    }
}
