create a new bnd feature for support of parsing of eclipse features

sample eclipse feature is /Users/peterkir/idefix/peterkir-bnd-sonatype/ws/feature.sample/feature.xml

implement inside bundle biz.aQute.repository
use the api package aQute/p2/api
and the provider package aQute/p2/provider/
add testcases for the new implementations validating the parsed index file

use the ecf p2 repo as testdata from repo url https://download.eclipse.org/rt/ecf/3.16.5/site.p2/3.16.5.v20250914-0333/ 
create a expected data for the index file

for parsing of the eclipse feature use the capability / requirement model similar to the bundles parsed and stored
create a new capability inside the namespace osgi.identity with the type eclipse.feature
capabilities should contain the eclipse feature properties
requirements should be used for the included and required features and bundles 

generate code, compile and run the testcases and validate they are working