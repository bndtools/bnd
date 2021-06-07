# Tester tests

This project contains the unit tests for `biz.aQute.tester` and `biz.aQute.tester.junit-platform`.

Summary: *If you want to run all the tests in Eclipse, don't right-click the project. Instead, right-click
the `test` folder.*
	
Reason: The project contains a series of dummy test classes that are used as inputs for the real test 
classes. Some of these tests fail by design, as they are meant to test that the testers handle failures properly.

All the dummy tests are in the `src` directory and all the real tests are in the `test` directory.
This way, the Gradle build will skip the dummy tests. However, Eclipse does not - if you run tests
for the entire project, then it will also run the JUnit 5 dummy tests in `src` (it will skip the JUnit 3/4
dummy tests because the Vintage engine is not configured for the project). This will not do any harm, but 
it might be confusing. You can safely ignore the results for the dummy tests, but if you want to de-clutter 
the test output you can simply run all the tests in `test` only, by right-clicking on the `test` folder and 
selecting *Run As...|JUnit Test*.