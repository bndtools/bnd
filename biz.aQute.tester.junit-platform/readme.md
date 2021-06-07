# JUnit Platform Tester

This is a `Tester-Plugin` implementation that uses the JUnit Platform library \
	to discover and launch tests. It is mostly compatible with `biz.aQute.tester` \
	and (like `biz.aQute.tester`) adds itself to `-runbundles`, rather than \
	adding itself to `-runpath` (like `biz.aQute.junit`). Because it uses the JUnit \
	Platform library it is able to run JUnit Jupiter tests and JUnit 3/4 tests \
	simply by ensuring that the right `TestEngine` implementations are installed as \
	bundles.
	
## Testing

Unit tests for `biz.aQute.tester.junit-platform` live in the `biz.aQute.tester.test` project \
	under the `test` directory. See also the readme in that project for tips about running the \
	tests.