import java.io.*;

println "basedir ${basedir}"

assert new File("${basedir}/build.log").exists();

List<String> fileContents = new ArrayList<String>();

BufferedReader reader = new File("${basedir}/build.log").newReader();

String s = null;

while((s = reader.readLine()) != null) {
    fileContents.add(s);
}

// Simple test
int idx = fileContents.indexOf("Tests run  : 1");
assert idx != -1;
assert fileContents.get(idx + 1).equals("Passed     : 1");

// Resolving test
idx = fileContents.indexOf("Tests run  : 2");
assert idx != -1;
assert fileContents.get(idx + 1).equals("Passed     : 2");
