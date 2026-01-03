# Tester

Deprecated: This bundle is marked as deprecated in bnd 7.2.0 for remove in 8.0.0
Please migrate to biz.aQute.tester.junit-platform


The `biz.aQute.junit` jar placed itself \
	on the `-runpath`. This implied that the -runpath also contained the JUnit \
	libraries. This constrained the users (what JUnit to use) and it also \
	wreaked havoc in resolving. Therefore, this implementation places itself \
	on `-runbundles` instead and will import whatever version of JUnit the user \
	has installed into their system.
	
## Testing

Unit tests for `biz.aQute.tester` live in the `biz.aQute.tester.test` project \
	under the `test` directory. Specifically, `aQute.tester.test.ActivatorTest`. \
	See also the readme in that project for tips about running the tests.