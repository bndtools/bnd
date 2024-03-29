import aQute.lib.io.IO

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths;

Path source = Paths.get("${projectsDirectory}/local-repo/org/objenesis");
Path target = Paths.get("${localRepositoryPath}/org/objenesis");

IO.delete(target)

source.toFile().eachFileRecurse({ f ->
    Path s = Paths.get(f.toURI());
    Path t = target.resolve(source.relativize(s));
    t.toFile().getParentFile().mkdirs();
    Files.copy(s, t);
});

// We run this check to be sure that the metadata needed by the
// bad-remote-metadata test is correctly installed in the local repo

String expected = """\
#NOTE: This is an Aether internal implementation file, its format can be changed without prior notice.
#Thu Jul 27 10:28:27 BST 2017
objenesis-2.2.pom>central=
objenesis-2.2.jar>central=
objenesis-2.2.pom>=
objenesis-2.2.jar>=
"""

Path remote = target.resolve("objenesis/2.2/_remote.repositories");

assert remote.toFile().text.normalize() == expected.normalize()

return;
