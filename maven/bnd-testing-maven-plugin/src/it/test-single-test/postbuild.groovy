File testsuite_xml = new File(basedir, "target/test-reports/test/TEST-test.xml")
assert testsuite_xml.isFile();
testsuite = new XmlSlurper().parse(testsuite_xml)
assert testsuite.@name == 'test.run'
assert testsuite.@tests == 1
assert testsuite.@errors == 0
assert testsuite.@failures == 0
