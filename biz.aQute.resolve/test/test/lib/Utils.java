package test.lib;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.osgi.resource.Capability;
import org.osgi.resource.Resource;
import org.osgi.service.repository.Repository;

import aQute.bnd.http.HttpClient;
import aQute.bnd.osgi.Processor;
import aQute.bnd.repository.osgi.OSGiRepository;

public class Utils {

	public static Repository createRepo(File index, String name) throws Exception {
		OSGiRepository repo = new OSGiRepository();
		HttpClient httpClient = new HttpClient();
		Map<String, String> map = new HashMap<>();
		map.put("locations", index.getAbsoluteFile()
			.toURI()
			.toString());
		map.put("name", name);
		map.put("cache", new File("generated/tmp/test/cache/" + name).getAbsolutePath());
		repo.setProperties(map);
		Processor p = new Processor();
		p.addBasicPlugin(httpClient);
		repo.setRegistry(p);
		return repo;
	}

	public static URI findContentURI(Resource resource) {
		List<Capability> contentCaps = resource.getCapabilities("osgi.content");
		if (contentCaps == null || contentCaps.isEmpty())
			throw new IllegalArgumentException("Resource has no content capability");
		if (contentCaps.size() > 1)
			throw new IllegalArgumentException("Resource has more than one content capability");

		Object uriObj = contentCaps.get(0)
			.getAttributes()
			.get("url");
		if (uriObj == null)
			throw new IllegalArgumentException("Resource content capability has no 'url' attribute.");
		if (uriObj instanceof URI)
			return (URI) uriObj;
		else
			try {
				return new URI(uriObj.toString());
			} catch (URISyntaxException e) {
				throw new IllegalArgumentException("Resource content capability has invalid 'url' attribute.", e);
			}
	}
}
