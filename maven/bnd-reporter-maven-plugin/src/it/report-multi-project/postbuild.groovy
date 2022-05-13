import java.nio.file.Files;

// Check the report files exist!
File multiReportFile1 = new File(basedir, 'metadata.xml')
File multiReportFile2 = new File(basedir, 'projectA/metadata.xml')
File multiReportFile3 = new File(basedir, 'projectA/readmeOther.md')
File multiReportFile4 = new File(basedir, 'projectB/metadata.xml')

assert multiReportFile1.isFile()
assert multiReportFile2.isFile()
assert multiReportFile3.isFile()
assert multiReportFile4.isFile()

assert new String(Files.readAllBytes(multiReportFile3.toPath())).equals("myValue")

// Check the readme files
File multiReportFile1Readme = new File(basedir, 'readme.md')
File multiReportFile2Readme = new File(basedir, 'projectA/readme.md')
File multiReportFile4Readme = new File(basedir, 'projectB/readme.md')

assert multiReportFile1Readme.isFile()
assert multiReportFile2Readme.isFile()
assert multiReportFile4Readme.isFile()
