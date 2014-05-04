package org.bndtools.versioncontrol.ignores.plugin.git;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.LinkedList;
import java.util.List;

import org.bndtools.api.NamedPlugin;
import org.bndtools.versioncontrol.ignores.manager.api.VersionControlIgnoresPlugin;

import aQute.bnd.annotation.component.Component;

@Component
public class GitVersionControlIgnoresPlugin implements VersionControlIgnoresPlugin {
    private static final String GITIGNORE_FILE_NAME = ".gitignore";

    /**
     * Fully read an ignore file, including comments.
     * 
     * @param ignoreFile
     *            The ignore file
     * @return null when the ignore file is null, when the ignore file doesn't exist or when the ignore file is empty. A
     *         non-empty list of lines as read from the ignore file otherwise.
     * @throws IOException
     *             When the ignore file could not be fully read (for example due to the ignore file not being an regular
     *             file or due to an IOException)
     */
    private List<String> readIgnoreFile(File ignoreFile) throws IOException {
        if (ignoreFile == null || !ignoreFile.exists()) {
            return null;
        }

        List<String> result = new LinkedList<String>();

        int lineNr = 0;
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new InputStreamReader(new FileInputStream(ignoreFile), "UTF-8"));

            String line;
            while ((line = reader.readLine()) != null) {
                result.add(line);
                lineNr++;
            }
        } catch (Exception e) {
            throw new IOException(String.format("Error reading ignore file %s on line %d", ignoreFile.getAbsolutePath(), lineNr), e);
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    /* swallow */
                }
            }
        }

        if (result.isEmpty()) {
            return null;
        }

        return result;
    }

    /*
     * VersionControlIgnoresPlugin
     */

    @Override
    public NamedPlugin getInformation() {
        return new GitVersionControlIgnoresPluginInformation();
    }

    @Override
    public boolean canStoreEmptyDirectories() {
        return false;
    }

    @Override
    public boolean matchesRepositoryProviderId(String repositoryProviderId) {
        return "org.eclipse.egit.core.GitProvider".equals(repositoryProviderId);
    }

    @Override
    public void addIgnores(File dstDir, List<String> ignores) throws Exception {
        if (dstDir == null) {
            return;
        }

        List<String> newIgnores = ignores;
        if (newIgnores == null) {
            newIgnores = new LinkedList<String>();
        }

        /* create the directory of the ignore file, when needed */
        if (!dstDir.exists() && !dstDir.mkdirs()) {
            throw new IOException("Could not create directory " + dstDir.getPath());
        }

        File ignoreFile = new File(dstDir, GITIGNORE_FILE_NAME);

        List<String> ignoresToAppend;
        if (!ignoreFile.exists()) {
            ignoresToAppend = newIgnores;
        } else {
            /* read the current ignores */
            List<String> currentIgnores = readIgnoreFile(ignoreFile);

            /*
             * add new ignores to the current ignores, but only if the current
             * ignores did not contain them
             */
            if (currentIgnores == null) {
                ignoresToAppend = newIgnores;
            } else {
                for (String newIgnore : newIgnores) {
                    if (!currentIgnores.contains(newIgnore)) {
                        currentIgnores.add(newIgnore);
                    }
                }
                ignoresToAppend = currentIgnores;
            }

            /* exit when we have no new ignores to write */
            if (ignoresToAppend.isEmpty()) {
                return;
            }
        }

        /* write out the ignore file */
        BufferedWriter writer = null;
        try {
            writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(ignoreFile), "UTF-8"));
            for (String ignoreToAppend : ignoresToAppend) {
                writer.write(ignoreToAppend);
                writer.newLine();
            }
            writer.flush();
        } catch (Exception e) {
            throw new IOException(String.format("Error appending %s to ignore file %s", ignoresToAppend, ignoreFile.getAbsolutePath()), e);
        } finally {
            if (writer != null) {
                try {
                    writer.close();
                } catch (IOException e) {
                    /* swallow */
                }
            }
        }
    }
}