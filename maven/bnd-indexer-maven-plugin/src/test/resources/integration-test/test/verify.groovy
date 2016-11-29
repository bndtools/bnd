import aQute.bnd.osgi.repository.*;
import aQute.bnd.osgi.resource.*;
import java.net.*;
import java.util.*;
import org.osgi.resource.*;
import org.osgi.service.repository.*;

public Repository check(String xmlFile, String gzipFile, int size, boolean localURL, boolean doCheck) {
	// Check the bundles exist!
	File xml = new File(xmlFile);
	assert xml.isFile();
	
	if(gzipFile != null) {
		File gzip = new File(gzipFile);
		assert gzip.isFile();
	}
	
	// Load repository
	XMLResourceParser xrp = new XMLResourceParser(xml.toURI());
	List<Resource> resources = xrp.parse();
	assert xrp.check();
	assert resources != null;
	assert size == resources.size();
	
	ResourcesRepository repo = new ResourcesRepository(resources);
	
	if(doCheck) {
		check(repo, "osgi.extender", "(osgi.extender=osgi.component)", "org.apache.felix.scr", localURL);
	}
	
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

	if(localURL ^ location.contains("test-repo")) {
		assert location.startsWith("file:") ? new File(URI.create(location)).isFile() : 
				new File(location).isFile();	
	} else {
		URI uri = URI.create(location);
		assert null != uri.getScheme();
		assert "http" == uri.getScheme() || "https" == uri.getScheme();

		HttpURLConnection con = (HttpURLConnection) uri.toURL().openConnection();
		con.setRequestMethod("HEAD");
		con.connect();
		assert 2 == (con.getResponseCode() / 100);
	}
	return content;
}

println "TODO: Need to write some test code for the generated index!"
println "basedir ${basedir}"
println "localRepositoryPath ${localRepositoryPath}"
println "mavenVersion ${mavenVersion}"

System.setProperty('jsse.enableSNIExtension', 'false')

check("${basedir}/transitive/target/index.xml", "${basedir}/transitive/target/index.xml.gz", 19, false, true);
check("${basedir}/non-transitive/target/index.xml", "${basedir}/non-transitive/target/index.xml.gz", 3, false, true);
check("${basedir}/scoped/target/index.xml", "${basedir}/scoped/target/index.xml.gz", 22, false, true);
check("${basedir}/require-local/target/index.xml", "${basedir}/require-local/target/index.xml.gz", 19, true, true);

// The in-build needs to check that the snapshot points at the real repo

Repository repo = check("${basedir}/in-build/target/index.xml", "${basedir}/in-build/target/index.xml.gz", 3, false, true);

Capability content = check(repo, "osgi.identity", "(osgi.identity=biz.aQute.bnd)", "biz.aQute.bnd", false);

assert 828249 == content.getAttributes().get("size");


content = check(repo, "osgi.identity", "(osgi.identity=org.apache.aries.async)", "org.apache.aries.async", false);

assert 300000 < content.getAttributes().get("size");
String url = content.getAttributes().get("url").toString();
assert !(url.substring(url.lastIndexOf('/')).contains("SNAPSHOT"))

// The add-mvn needs to check that the mvn: URLs are added as well

repo = check("${basedir}/add-mvn/target/index.xml", "${basedir}/add-mvn/target/index.xml.gz", 3, false, false);

Requirement requirement = new RequirementBuilder("osgi.content")
						.addDirective("filter", "(url=mvn*)")
						.buildSyntheticRequirement();
	
Map<Requirement,Collection<Capability>> caps = repo
						.findProviders(Collections.singleton(requirement));
						
// All three resources should have a mvn: URL	
assert 3 == caps.get(requirement).size();

//Test using a local repository

repo = check("${basedir}/local-repo-dependency/target/index.xml", "${basedir}/local-repo-dependency/target/index.xml.gz", 1, false, false);
content = check(repo, "osgi.identity", "(osgi.identity=helloworld-for-indexer-testing)", "helloworld-for-indexer-testing", true);

// Test a renamed repository
check("${basedir}/rename-output/target/custom.xml", null, 19, false, true);

// Test indexing a local folder
check("${basedir}/index-folder/target/META-INF/index.xml", "${basedir}/index-folder/target/META-INF/index.xml.gz", 19, true, true);
return;