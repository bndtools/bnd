import java.io.*;
import java.util.regex.Pattern

File build_log_file = new File("${basedir}/build.log")
assert build_log_file.exists();
def build_log = build_log_file.text

// With provider (no bundle version change required)
File valid_with_provider_no_bundle_version_change = new File("${basedir}/target/baseline/valid-with-provider-no-bundle-version-change-1.0.1.txt")
assert valid_with_provider_no_bundle_version_change.isFile()
assert build_log =~ Pattern.quote("[INFO] Baseline check succeeded. See the report in ${valid_with_provider_no_bundle_version_change}")
