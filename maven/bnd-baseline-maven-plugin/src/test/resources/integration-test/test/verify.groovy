import java.io.*;
import java.util.regex.Pattern

println "basedir ${basedir}"

File build_log_file = new File("${basedir}/build.log")
assert build_log_file.exists();
def build_log = build_log_file.text

// No previous
assert build_log =~ Pattern.quote('[WARNING] No previous version of biz.aQute.bnd-test:valid-no-previous:jar:1.0.1 could be found to baseline against')

// With previous-same
assert build_log =~ Pattern.quote('[INFO] Baselining check succeeded checking biz.aQute.bnd-test:valid-with-previous-same:jar:1.0.2 against biz.aQute.bnd-test:valid-no-previous:jar:1.0.1')

// With previous-provider
assert build_log =~ Pattern.quote('[INFO] Baselining check succeeded checking biz.aQute.bnd-test:valid-with-provider:jar:1.0.2 against biz.aQute.bnd-test:valid-no-previous:jar:1.0.1')


// With provider
assert build_log =~ Pattern.quote('[ERROR] Baseline mismatch for package bnd.test, MINOR change. Current is 1.0.0, repo is 1.0.0, suggest 1.1.0 or -')

assert build_log =~ Pattern.quote('[WARNING] The baselining check failed when checking biz.aQute.bnd-test:invalid-with-provider:jar:1.0.2 against biz.aQute.bnd-test:valid-no-previous:jar:1.0.1 but the bnd-baseline-maven-plugin is configured not to fail the build.')


// With consumer
assert build_log =~ Pattern.quote('[ERROR] Baseline mismatch for package bnd.test, MAJOR change. Current is 1.0.0, repo is 1.0.0, suggest 2.0.0 or 1.0.0')

assert build_log =~ Pattern.quote('[WARNING] The baselining check failed when checking biz.aQute.bnd-test:invalid-with-consumer:jar:1.0.2 against biz.aQute.bnd-test:valid-no-previous:jar:1.0.1 but the bnd-baseline-maven-plugin is configured not to fail the build.')


