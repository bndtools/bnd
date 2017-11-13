import java.nio.file.*;

Path source = Paths.get("${pluginBasedir}/src/test/resources/integration-test/local-repo/org/objenesis");
Path target = Paths.get("${pluginBuildDirectory}/integration-test/repo/org/objenesis");

target.deleteDir();


source.toFile().eachFileRecurse({ f ->
    Path s = Paths.get(f.toURI());
    Path t = target.resolve(source.relativize(s));
    t.toFile().getParentFile().mkdirs();
    Files.copy(s, t);
});

// We run this check to be sure that the metadata needed by the
// bad-remote-metadata test is correctly installed in the local repo

String expected = "#NOTE: This is an Aether internal implementation file, its format can be changed without prior notice.\n" +
    "#Thu Jul 27 10:28:27 BST 2017\n" +
    "objenesis-2.2.pom>central=\n" +
    "objenesis-2.2.jar>central=\n" +
    "objenesis-2.2.pom>=\n" +
    "objenesis-2.2.jar>=\n";

Path remote = Paths.get(target.toString(), "objenesis/2.2/_remote.repositories");

assert expected == remote.toFile().getText();

return;