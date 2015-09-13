import aQute.bnd.osgi.repository.*;
import aQute.bnd.osgi.resource.*;
import java.util.*;
import org.osgi.resource.*;
import org.osgi.service.repository.*;

public void check(String xmlFile, String gzipFile, int size, boolean localURL) {
	// Check the bundles exist!
	File xml = new File(xmlFile);
	assert xml.isFile();
	File gzip = new File(gzipFile);
	assert gzip.isFile();
	
	// Load repository
	XMLResourceParser xrp = new XMLResourceParser(xml.toURI());
	xrp.setTrace(true);
	List<Resource> resources = xrp.parse();
	assert xrp.check();
	assert resources != null;
	assert size == resources.size();
	
	ResourcesRepository repo = new ResourcesRepository(resources);
	
	Requirement requirement = new RequirementBuilder("osgi.extender")
						.addDirective("filter", "(osgi.extender=osgi.component)")
						.buildSyntheticRequirement();
	
	Map<Requirement,Collection<Capability>> caps = repo
						.findProviders(Collections.singleton(requirement));
	
	assert 1 == caps.get(requirement).size();
	
	Resource res = caps.get(requirement).iterator().next().getResource();
	
	assert "org.apache.felix.scr" ==
						ResourceUtils.getIdentityCapability(res).getAttributes().get("osgi.identity");
	
	String location = ResourceUtils.getContentCapability(res).getAttributes().get("url").toString();
	assert localURL == location.contains("file:");
}

println "TODO: Need to write some test code for the generated index!"
println "basedir ${basedir}"
println "localRepositoryPath ${localRepositoryPath}"
println "mavenVersion ${mavenVersion}"

check("${basedir}/transitive/target/index.xml", "${basedir}/transitive/target/index.xml.gz", 21, false)
check("${basedir}/non-transitive/target/index.xml", "${basedir}/non-transitive/target/index.xml.gz", 3, false)
check("${basedir}/scoped/target/index.xml", "${basedir}/scoped/target/index.xml.gz", 24, false)
check("${basedir}/require-local/target/index.xml", "${basedir}/require-local/target/index.xml.gz", 21, true)