// Check the report files exist!
File singleReportFile = new File(basedir, 'metadata.json')

assert singleReportFile.isFile()

// Check the readme files
File singleReportFileReadme = new File(basedir, 'readme.md')

assert singleReportFileReadme.isFile()
