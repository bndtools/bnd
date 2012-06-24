package test;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.osgi.resource.Capability;
import org.osgi.resource.Resource;
import org.osgi.service.repository.Repository;

import aQute.lib.deployer.repository.FixedIndexedRepo;

public class Utils {

    public static Repository createRepo(File index) {
        FixedIndexedRepo repo = new FixedIndexedRepo();

        Map<String,String> props = new HashMap<String,String>();
        props.put(FixedIndexedRepo.PROP_LOCATIONS, index.toURI().toString());
        repo.setProperties(props);

        return repo;
    }

    public static URI findContentURI(Resource resource) {
        List<Capability> contentCaps = resource.getCapabilities("osgi.content");
        if (contentCaps == null || contentCaps.isEmpty())
            throw new IllegalArgumentException("Resource has no content capability");
        if (contentCaps.size() > 1)
            throw new IllegalArgumentException("Resource has more than one content capability");

        Object uriObj = contentCaps.get(0).getAttributes().get("url");
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
