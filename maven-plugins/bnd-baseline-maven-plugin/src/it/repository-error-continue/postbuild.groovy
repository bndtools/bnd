import java.util.regex.Pattern

File buildLogFile = new File("${basedir}/build.log")
assert buildLogFile.exists()
String buildLog = buildLogFile.text

assert buildLog =~ Pattern.quote("An error occurred while calculating the baseline")
assert buildLog =~ Pattern.quote("No previous version of biz.aQute.bnd-test:repository-error-continue:jar:1.0.1 could be found to baseline against")
