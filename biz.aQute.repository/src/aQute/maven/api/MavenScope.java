package aQute.maven.api;

public enum MavenScope {
	/**
	 * compile - this is the default scope, used if none is specified. Compile
	 * dependencies are available in all classpaths. Furthermore, those
	 * dependencies are propagated to dependent projects.
	 */
	compile(true),
	/**
	 * provided - this is much like compile, but indicates you expect the JDK or
	 * a container to provide it at runtime. It is only available on the
	 * compilation and test classpath, and is not transitive.
	 */
	provided(false),
	/**
	 * runtime - this scope indicates that the dependency is not required for
	 * compilation, but is for execution. It is in the runtime and test
	 * classpaths, but not the compile classpath.
	 */
	runtime(true),
	/**
	 * test - this scope indicates that the dependency is not required for
	 * normal use of the application, and is only available for the test
	 * compilation and execution phases.
	 */
	test(false),
	/**
	 * this scope is similar to provided except that you have to provide the JAR
	 * which contains it explicitly. The artifact is always available and is not
	 * looked up in a repository.
	 */
	system(false), //

	/**
	 *
	 */
	import_(false),;

	private boolean transitive;

	MavenScope(boolean transitive) {
		this.transitive = transitive;
	}

	public boolean isTransitive() {
		return transitive;
	}

	public static MavenScope getScope(String scope) {
		switch (scope.toLowerCase()) {
			case "import" :
				return MavenScope.import_;

			case "provided" :
				return MavenScope.provided;

			case "runtime" :
				return MavenScope.runtime;

			case "system" :
				return MavenScope.system;

			case "test" :
				return MavenScope.test;

			case "compile" :
			default :
				return MavenScope.compile;
		}
	}

}
