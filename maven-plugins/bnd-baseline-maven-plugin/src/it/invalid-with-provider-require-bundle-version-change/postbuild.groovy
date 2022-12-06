import java.util.regex.Pattern

File build_log_file = new File("${basedir}/build.log")
assert build_log_file.exists();
def build_log = build_log_file.text

// With provider (require bundle version change)
File invalid_with_provider_require_bundle_version_change = new File("${basedir}/target/baseline/invalid-with-provider-require-bundle-version-change-1.0.1.txt")
assert invalid_with_provider_require_bundle_version_change.isFile()
assert build_log =~ Pattern.quote("[WARNING] Baseline problems detected. See the report in ${invalid_with_provider_require_bundle_version_change}")
assert build_log =~ Pattern.quote('* valid-no-previous                                  BUNDLE     MAJOR      1.0.1      1.0.1      1.1.0')

