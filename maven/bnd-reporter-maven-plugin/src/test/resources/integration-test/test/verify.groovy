import java.nio.file.Files;

println "basedir ${basedir}"

// Check the report files exist!
File noReportFile = new File(basedir, 'report-single-project-no-report/metadata.json')
File singleReportFile = new File(basedir, 'report-single-project/metadata.json')
File multiReportFile1 = new File(basedir, 'report-multi-project/metadata.xml')
File multiReportFile2 = new File(basedir, 'report-multi-project/projectA/metadata.xml')
File multiReportFile3 = new File(basedir, 'report-multi-project/projectA/readmeOther.md')
File multiReportFile4 = new File(basedir, 'report-multi-project/projectB/metadata.xml')
File multiScopedReportFile1 = new File(basedir, 'report-multi-project-scoped/metadata.xml')
File multiScopedReportFile2 = new File(basedir, 'report-multi-project-scoped/projectC/metadata.xml')
File multiScopedReportFile3 = new File(basedir, 'report-multi-project-scoped/projectC/readmeOther.md')
File multiScopedReportFile4 = new File(basedir, 'report-multi-project-scoped/projectD/metadata.xml')

assert !noReportFile.isFile()
assert singleReportFile.isFile()
assert multiReportFile1.isFile()
assert multiReportFile2.isFile()
assert multiReportFile3.isFile()
assert multiReportFile4.isFile()
assert multiScopedReportFile1.isFile()
assert !multiScopedReportFile2.isFile()
assert multiScopedReportFile3.isFile()
assert !multiScopedReportFile4.isFile()

assert new String(Files.readAllBytes(multiReportFile3.toPath())).equals("myValue")
assert new String(Files.readAllBytes(multiScopedReportFile3.toPath())).equals("projectC")

// Check the readme files
File noDefinedReportFileReadme = new File(basedir, 'report-single-project-no-report/readme.md')
File singleReportFileReadme = new File(basedir, 'report-single-project/readme.md')
File multiReportFile1Readme = new File(basedir, 'report-multi-project/readme.md')
File multiReportFile2Readme = new File(basedir, 'report-multi-project/projectA/readme.md')
File multiReportFile3Readme = new File(basedir, 'report-multi-project/projectA/readme.md')
File multiReportFile4Readme = new File(basedir, 'report-multi-project/projectB/readme.md')

assert !noDefinedReportFileReadme.isFile()
assert singleReportFileReadme.isFile()
assert multiReportFile1Readme.isFile()
assert multiReportFile2Readme.isFile()
assert multiReportFile3Readme.isFile()
assert multiReportFile4Readme.isFile()
