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

// With provider (no bundle version change required)
assert build_log =~ Pattern.quote('[INFO] Baselining check succeeded checking biz.aQute.bnd-test:valid-with-provider-no-bundle-version-change:jar:1.0.1 against biz.aQute.bnd-test:valid-no-previous:jar:1.0.1')

// With provider
assert build_log =~ Pattern.quote('[ERROR] * bnd.test                                           PACKAGE    MINOR      1.0.0      1.0.0      1.1.0      -')

assert build_log =~ Pattern.quote('[WARNING] The baselining check failed when checking biz.aQute.bnd-test:invalid-with-provider:jar:1.0.2 against biz.aQute.bnd-test:valid-no-previous:jar:1.0.1 but the bnd-baseline-maven-plugin is configured not to fail the build.')

// With provider (require bundle version change)
assert build_log =~ Pattern.quote('[ERROR] * valid-no-previous                                  BUNDLE     MAJOR      1.0.1      1.0.1      1.1.0')

assert build_log =~ Pattern.quote('[WARNING] The baselining check failed when checking biz.aQute.bnd-test:invalid-with-provider-require-bundle-version-change:jar:1.0.1 against biz.aQute.bnd-test:valid-no-previous:jar:1.0.1 but the bnd-baseline-maven-plugin is configured not to fail the build.')


// With consumer
assert build_log =~ Pattern.quote('[ERROR] * bnd.test                                           PACKAGE    MAJOR      1.0.0      1.0.0      2.0.0      1.1.0')

assert build_log =~ Pattern.quote('[WARNING] The baselining check failed when checking biz.aQute.bnd-test:invalid-with-consumer:jar:1.0.2 against biz.aQute.bnd-test:valid-no-previous:jar:1.0.1 but the bnd-baseline-maven-plugin is configured not to fail the build.')


