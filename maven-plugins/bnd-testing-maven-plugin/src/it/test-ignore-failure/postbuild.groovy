import groovy.xml.XmlSlurper

File testsuite_xml = new File(basedir, "target/test-reports/test/TEST-test-ignore-failure-0.0.1.xml")
assert testsuite_xml.isFile();
testsuite = new XmlSlurper().parse(testsuite_xml)
assert testsuite.@name == "test.test-ignore-failure"
assert testsuite.@tests == 2
assert testsuite.@errors == 1
assert testsuite.@failures == 0
