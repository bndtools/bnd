package aQute.bnd.osgi;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class JarCopyUtil {
    private static final Logger logger = LoggerFactory.getLogger(JarCopyUtil.class);

    /**
     * Copy package resources from source JAR to destination JAR with preprocessing.
     * This method handles:
     * - Standard package content copying
     * - Source file copying (when -sources is enabled)
     * - bnd.info preprocessing
     *
     * @param dest destination JAR
     * @param srce source JAR
     * @param path package path
     * @param overwrite whether to overwrite existing resources
     * @param processor the processor context (for hasSources check and preprocessing)
     */
	static void copyPackageWithPreprocessing(Jar dest, Jar srce, String path,
                                                     boolean overwrite, Processor processor) {
        logger.debug("copy d={} s={} p={}", dest, srce, path);
        dest.copy(srce, path, overwrite);

        if (Processor.isTrue(processor.getProperty(Constants.SOURCES))) {
            dest.copy(srce, Processor.appendPath("OSGI-OPT/src", path), overwrite);
        }

        // bnd.info sources must be preprocessed
        String bndInfoPath = Processor.appendPath(path, "bnd.info");
        Resource r = dest.getResource(bndInfoPath);
        if (r != null && !(r instanceof PreprocessResource)) {
            logger.debug("preprocessing bnd.info");
            PreprocessResource pp = new PreprocessResource(processor, r);
            dest.putResource(bndInfoPath, pp);
        }

        if (Processor.isTrue(processor.getProperty(Constants.SOURCES))) {
            String srcPath = Processor.appendPath("OSGI-OPT/src", path);
            Map<String, Resource> srcContents = srce.getDirectory(srcPath);
            if (srcContents != null) {
                dest.addDirectory(srcContents, overwrite);
            }
        }

    }
}