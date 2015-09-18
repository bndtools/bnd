import aQute.bnd.osgi.repository.*;
import aQute.bnd.osgi.resource.*;
import java.util.*;
import org.osgi.resource.*;
import org.osgi.service.repository.*;

public Repository check(String xmlFile, String gzipFile, int size, boolean localURL) {
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
	
	check(repo, "osgi.extender", "(osgi.extender=osgi.component)", "org.apache.felix.scr", localURL);
	
	return repo;
}

public Capability check(Repository repo, String namespace, String filter, String identity, boolean localURL) {
	
	Requirement requirement = new RequirementBuilder(namespace)
						.addDirective("filter", filter)
						.buildSyntheticRequirement();
	
	Map<Requirement,Collection<Capability>> caps = repo
						.findProviders(Collections.singleton(requirement));
	
	assert 1 == caps.get(requirement).size();
	
	Resource res = caps.get(requirement).iterator().next().getResource();
	
	assert identity == ResourceUtils.getIdentityCapability(res).getAttributes().get("osgi.identity");
	
	Capability content = ResourceUtils.getContentCapability(res);
	
	String location = content.getAttributes().get("url").toString();

	if(localURL) {
		assert location.startsWith("file:") ? new File(URI.create(location)).isFile() : 
				new File(location).isFile();	
	} else {
		assert null != URI.create(location).getScheme();
		assert "file" != URI.create(location).getScheme();
	}
	return content;
}

println "TODO: Need to write some test code for the generated index!"
println "basedir ${basedir}"
println "localRepositoryPath ${localRepositoryPath}"
println "mavenVersion ${mavenVersion}"

check("${basedir}/transitive/target/index.xml", "${basedir}/transitive/target/index.xml.gz", 21, false);
check("${basedir}/non-transitive/target/index.xml", "${basedir}/non-transitive/target/index.xml.gz", 3, false);
check("${basedir}/scoped/target/index.xml", "${basedir}/scoped/target/index.xml.gz", 24, false);
check("${basedir}/require-local/target/index.xml", "${basedir}/require-local/target/index.xml.gz", 21, true);

Repository repo = check("${basedir}/in-build/target/index.xml", "${basedir}/in-build/target/index.xml.gz", 4, false);

Capability content = check(repo, "osgi.identity", "(osgi.identity=biz.aQute.bnd)", "biz.aQute.bnd", false);

assert 828249 == content.getAttributes().get("size");


content = check(repo, "osgi.identity", "(osgi.identity=org.apache.aries.async)", "org.apache.aries.async", false);

assert 300000 < content.getAttributes().get("size");
String url = content.getAttributes().get("url").toString();
assert !(url.substring(url.lastIndexOf('/')).contains("SNAPSHOT"))

