package aQute.bnd.maven.lib.resolve;

public enum Scope {
	compile,
	provided,
	runtime,
	system,
	test,

	/**
	 * Custom scope for isolating bnd distro artifacts
	 */
	distro;
}
