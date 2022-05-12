import aQute.bnd.osgi.repository.*;
import aQute.bnd.osgi.resource.*;
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
	assert xrp.name() != null;

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


public Capability checkAbsent(Repository repo, String namespace, String filter) {

	Requirement requirement = new RequirementBuilder(namespace)
						.addDirective("filter", filter)
						.buildSyntheticRequirement();

	Map<Requirement,Collection<Capability>> caps = repo
						.findProviders(Collections.singleton(requirement));

	assert caps.get(requirement).isEmpty();
}

println "TODO: Need to write some test code for the generated index!"

// @Test local-repo-dependency
// using a local repository
repo = check("${basedir}/target/index.xml", "${basedir}/target/index.xml.gz", 1, false, false);
content = check(repo, "osgi.identity", "(osgi.identity=helloworld-for-indexer-testing)", "helloworld-for-indexer-testing", true);

return;
