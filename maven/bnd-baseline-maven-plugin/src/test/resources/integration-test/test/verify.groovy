import java.io.*;
import java.util.regex.Pattern

println "basedir ${basedir}"

File build_log_file = new File("${basedir}/build.log")
assert build_log_file.exists();
def build_log = build_log_file.text

// No previous
assert build_log =~ Pattern.quote('[WARNING] No previous version of biz.aQute.bnd-test:valid-no-previous:jar:1.0.1 could be found to baseline against')

// With previous-same
File valid_with_previous_same = new File("${basedir}/valid-with-previous-same/target/baseline/valid-with-previous-same-1.0.2.txt")
assert valid_with_previous_same.isFile()
assert build_log =~ Pattern.quote("[INFO] Baseline check succeeded. See the report in ${valid_with_previous_same}")

// With previous-provider
File valid_with_provider = new File("${basedir}/valid-with-provider/target/baseline/valid-with-provider-1.0.2.txt")
assert valid_with_provider.isFile()
assert build_log =~ Pattern.quote("[INFO] Baseline check succeeded. See the report in ${valid_with_provider}")

// With provider (no bundle version change required)
File valid_with_provider_no_bundle_version_change = new File("${basedir}/valid-with-provider-no-bundle-version-change/target/baseline/valid-with-provider-no-bundle-version-change-1.0.1.txt")
assert valid_with_provider_no_bundle_version_change.isFile()
assert build_log =~ Pattern.quote("[INFO] Baseline check succeeded. See the report in ${valid_with_provider_no_bundle_version_change}")

// With provider
File invalid_with_provider = new File("${basedir}/invalid-with-provider/target/baseline/invalid-with-provider-1.0.2.txt")
assert invalid_with_provider.isFile()
assert build_log =~ Pattern.quote("[WARNING] Baseline problems detected. See the report in ${invalid_with_provider}")
assert build_log =~ Pattern.quote('* bnd.test                                           PACKAGE    MINOR      1.0.0      1.0.0      1.1.0      -')

// With provider (require bundle version change)
File invalid_with_provider_require_bundle_version_change = new File("${basedir}/invalid-with-provider-require-bundle-version-change/target/baseline/invalid-with-provider-require-bundle-version-change-1.0.1.txt")
assert invalid_with_provider_require_bundle_version_change.isFile()
assert build_log =~ Pattern.quote("[WARNING] Baseline problems detected. See the report in ${invalid_with_provider_require_bundle_version_change}")
assert build_log =~ Pattern.quote('* valid-no-previous                                  BUNDLE     MAJOR      1.0.1      1.0.1      1.1.0')

// With consumer
File invalid_with_consumer = new File("${basedir}/invalid-with-consumer/target/baseline/invalid-with-consumer-1.0.2.text")
assert invalid_with_consumer.isFile()
assert build_log =~ Pattern.quote("[WARNING] Baseline problems detected. See the report in ${invalid_with_consumer}")
assert build_log =~ Pattern.quote('* bnd.test                                           PACKAGE    MAJOR      1.0.0      1.0.0      2.0.0      1.1.0')


