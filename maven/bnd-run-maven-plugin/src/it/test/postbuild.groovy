import java.io.*;
import java.util.regex.Pattern

println "basedir ${basedir}"

File build_log_file = new File("${basedir}/build.log")
assert build_log_file.exists();
def build_log = build_log_file.text

// No previous
assert build_log =~ Pattern.quote('Run Barry, RUN!!!')
