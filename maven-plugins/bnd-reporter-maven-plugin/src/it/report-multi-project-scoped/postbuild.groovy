import java.nio.file.Files;

// Check the report files exist!
File multiScopedReportFile1 = new File(basedir, 'metadata.xml')
File multiScopedReportFile2 = new File(basedir, 'projectC/metadata.xml')
File multiScopedReportFile3 = new File(basedir, 'projectC/readmeOther.md')
File multiScopedReportFile4 = new File(basedir, 'projectD/metadata.xml')

assert multiScopedReportFile1.isFile()
assert !multiScopedReportFile2.isFile()
assert multiScopedReportFile3.isFile()
assert !multiScopedReportFile4.isFile()

assert new String(Files.readAllBytes(multiScopedReportFile3.toPath())).equals("projectC")
