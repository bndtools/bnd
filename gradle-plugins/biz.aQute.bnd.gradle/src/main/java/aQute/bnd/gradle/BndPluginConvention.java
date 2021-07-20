package aQute.bnd.gradle;

import aQute.bnd.build.Project;

/**
 * BndPluginConvention for Gradle.
 *
 * @deprecated Replaced by BndPluginExtension.
 */
@Deprecated
public class BndPluginConvention {
	private final BndPluginExtension bnd;

	public BndPluginConvention(BndPluginExtension extension) {
		this.bnd = extension;
	}

	public Project getProject() {
		return bnd.getProject();
	}

	public boolean bndis(String name) {
		return bnd.is(name);
	}

	public String bnd(String name) {
		return bnd.get(name);
	}

	public String bndMerge(String name) {
		return bnd.merge(name);
	}

	public Object bnd(String name, Object defaultValue) {
		return bnd.get(name, defaultValue);
	}

	public String bndProcess(String line) {
		return bnd.process(line);
	}

	public Object bndUnprocessed(String name, Object defaultValue) {
		return bnd.unprocessed(name, defaultValue);
	}

	public String propertyMissing(String name) {
		return bnd.propertyMissing(name);
	}
}
