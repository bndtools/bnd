import java.util.regex.Pattern

File buildLogFile = new File("${basedir}/build.log")
assert buildLogFile.exists()
String buildLog = buildLogFile.text

assert buildLog =~ Pattern.quote("An error occurred while calculating the baseline")
