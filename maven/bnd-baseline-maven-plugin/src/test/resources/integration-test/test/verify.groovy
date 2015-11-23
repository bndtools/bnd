import java.io.*;

println "basedir ${basedir}"

assert new File("${basedir}/build.log").exists();

List<String> fileContents = new ArrayList<String>();

BufferedReader reader = new File("${basedir}/build.log").newReader();

String s = null;

while((s = reader.readLine()) != null) {
    fileContents.add(s);
}

// No previous
assert fileContents.contains("[WARNING] No previous version of biz.aQute.bnd-test:valid-no-previous:jar:0.0.1 could be found to baseline against");

// With previous-same
assert fileContents.contains("[INFO] Baselining check succeeded checking biz.aQute.bnd-test:valid-with-previous-same:jar:0.0.2 against biz.aQute.bnd-test:valid-no-previous:jar:0.0.1");

// With previous-provider
assert fileContents.contains("[INFO] Baselining check succeeded checking biz.aQute.bnd-test:valid-with-provider:jar:0.0.2 against biz.aQute.bnd-test:valid-no-previous:jar:0.0.1");


// With provider
int idx = fileContents.indexOf("[ERROR] Baseline mismatch for package bnd.test, MINOR change. Current is 1.0.0, repo is 1.0.0, suggest 1.1.0 or -");

assert fileContents.get(idx + 2) == "[WARNING] The baselining check failed when checking biz.aQute.bnd-test:invalid-with-provider:jar:0.0.2 against biz.aQute.bnd-test:valid-no-previous:jar:0.0.1 but the bnd-baseline-maven-plugin is configured not to fail the build.";


// With consumer
idx = fileContents.indexOf("[ERROR] Baseline mismatch for package bnd.test, MAJOR change. Current is 1.0.0, repo is 1.0.0, suggest 2.0.0 or 1.0.0");

assert fileContents.get(idx + 2) == "[WARNING] The baselining check failed when checking biz.aQute.bnd-test:invalid-with-consumer:jar:0.0.2 against biz.aQute.bnd-test:valid-no-previous:jar:0.0.1 but the bnd-baseline-maven-plugin is configured not to fail the build.";


