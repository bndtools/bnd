package bndtools.bndplugins.repo;

import java.io.File;

public interface SourceRepositoryPlugin {
    File getSourceBundle(File binaryBundle, String bsn);
}
