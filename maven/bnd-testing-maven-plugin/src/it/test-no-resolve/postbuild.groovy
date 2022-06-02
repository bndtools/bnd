import groovy.xml.XmlSlurper

File testsuite_xml = new File(basedir, "/target/test-reports/test/TEST-test-no-resolve-0.0.1.xml")
assert testsuite_xml.isFile();
def testsuite = new XmlSlurper().parse(testsuite_xml)
assert testsuite.@name == 'test.test-no-resolve'
assert testsuite.@tests == 1
assert testsuite.@errors == 0
assert testsuite.@failures == 0
