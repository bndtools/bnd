// Check the report files exist!
File noReportFile = new File(basedir, 'metadata.json')

assert !noReportFile.isFile()

// Check the readme files
File noDefinedReportFileReadme = new File(basedir, 'readme.md')

assert !noDefinedReportFileReadme.isFile()
