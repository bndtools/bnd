import java.util.regex.Pattern

File build_log_file = new File("${basedir}/build.log")
assert build_log_file.exists();
def build_log = build_log_file.text

// No previous
assert build_log =~ Pattern.quote('[WARNING] No previous version of biz.aQute.bnd-test:valid-no-previous:jar:1.0.1 could be found to baseline against')
