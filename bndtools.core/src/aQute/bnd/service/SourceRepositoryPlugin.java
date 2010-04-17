package aQute.bnd.service;

import java.io.File;

public interface SourceRepositoryPlugin {
    File getSourceBundle(File binaryBundle, String bsn);
}
