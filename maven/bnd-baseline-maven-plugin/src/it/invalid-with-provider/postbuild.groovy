import java.io.*;
import java.util.regex.Pattern

File build_log_file = new File("${basedir}/build.log")
assert build_log_file.exists();
def build_log = build_log_file.text

// With provider
File invalid_with_provider = new File("${basedir}/target/baseline/invalid-with-provider-1.0.2.txt")
assert invalid_with_provider.isFile()
assert build_log =~ Pattern.quote("[WARNING] Baseline problems detected. See the report in ${invalid_with_provider}")
assert build_log =~ Pattern.quote('* bnd.test                                           PACKAGE    MINOR      1.0.0      1.0.0      1.1.0      -')
