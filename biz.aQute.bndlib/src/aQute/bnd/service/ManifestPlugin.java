package aQute.bnd.service;

import java.util.jar.Manifest;

import aQute.bnd.osgi.Analyzer;

/**
 * Some plugins started to use the verifier state because they needed to have
 * access to the calculated manifest and then added extra resources. (Looking at
 * you JPMSModuleInfoPlugin!). Unfortunately this is wrong since any added
 * resources will not end up in the name section of the JAR. It should be
 * obvious that during verification it is clearly wrong to add additional files.
 * <p>
 * Plugins are called in {@link #ordering()} order. Plugins may add/modify the
 * given manifest and they may add additional resources that do not affect the
 * manifest except for the name section. These plugins must not add classes or
 * other resources that could influence the manifest information.
 * <p>
 * The plugins are called from calcManifest _just_ before the name section is
 * calculated.
 */
public interface ManifestPlugin extends OrderedPlugin {

	/**
	 * Called just after the manifest has been calculated and just before name
	 * section is calculated. The analyzer will not make any more changes to the
	 * main section of the manifest. This is also after any non-wanted headers
	 * are removed.
	 *
	 * @param analyzer the current analyzer, it is calling from
	 *            {@link Analyzer#calcManifest()}
	 * @param manifest the current intermediate manifest, lacking a name section
	 * @throws Exception
	 */
	default void mainSet(Analyzer analyzer, Manifest manifest) throws Exception {}

	/**
	 * Called when the manifest is completely calculated. The analyzer will not
	 * make any more changes to the manifest. This is a read only operation.
	 *
	 * @param analyzer the current analyzer, it is calling from
	 *            {@link Analyzer#calcManifest()}
	 * @param manifest the final manifest
	 */
	default void nameSet(Analyzer analyzer, Manifest manifest) throws Exception {}
}
