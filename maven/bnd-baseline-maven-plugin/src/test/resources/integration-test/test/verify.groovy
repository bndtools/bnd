import java.io.*;

println "basedir ${basedir}"

assert new File("${basedir}/build.log").exists();

String fileContents = new File("${basedir}/build.log").getText('UTF-8');

// No previous
assert fileContents.contains("[WARNING] No previous version of biz.aQute.bnd-test:valid-no-previous:jar:0.0.1 could be found to baseline against");

// With previous-same
assert fileContents.contains("[INFO] Baselining check succeeded checking biz.aQute.bnd-test:valid-with-previous-same:jar:0.0.2 against biz.aQute.bnd-test:valid-no-previous:jar:0.0.1")

// With provider
assert fileContents.contains("[ERROR] Baseline mismatch for package bnd.test, MINOR change. Current is 1.0.0, repo is 1.0.0, suggest 1.1.0 or -\n" +
"\n" +
"[ERROR] The bundle version change (0.0.1 to 0.0.2) is too low, the new version must be at least 0.1.0\n" +
"[WARNING] The baselining check failed when checking biz.aQute.bnd-test:invalid-with-provider:jar:0.0.2 against biz.aQute.bnd-test:valid-no-previous:jar:0.0.1 but the bnd-baseline-maven-plugin is configured not to fail the build.")


// With consumer
assert fileContents.contains("[ERROR] Baseline mismatch for package bnd.test, MAJOR change. Current is 1.0.0, repo is 1.0.0, suggest 2.0.0 or 1.0.0\n" +
"\n" +
"[ERROR] The bundle version change (0.0.1 to 0.0.2) is too low, the new version must be at least 1.0.0\n" +
"[WARNING] The baselining check failed when checking biz.aQute.bnd-test:invalid-with-consumer:jar:0.0.2 against biz.aQute.bnd-test:valid-no-previous:jar:0.0.1 but the bnd-baseline-maven-plugin is configured not to fail the build.")

