import java.util.regex.Pattern

File build_log_file = new File("${basedir}/build.log")
assert build_log_file.exists();
def build_log = build_log_file.text

// With consumer
File invalid_with_consumer = new File("${basedir}/target/baseline/invalid-with-consumer-1.0.2.text")
assert invalid_with_consumer.isFile()
assert build_log =~ Pattern.quote("[WARNING] Baseline problems detected. See the report in ${invalid_with_consumer}")
assert build_log =~ Pattern.quote('* bnd.test                                           PACKAGE    MAJOR      1.0.0      1.0.0      2.0.0      1.1.0')


