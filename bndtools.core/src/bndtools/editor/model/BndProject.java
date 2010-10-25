package bndtools.editor.model;

import java.net.URL;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import bndtools.api.IBndProject;

public class BndProject implements IBndProject {

    private Map<String, URL> resources;
    
    public void addResource(String path, String name, URL url) {
       if (path.endsWith("/")) {
           addResource(path + name, url);
       } else {
           addResource(path + '/' + name, url);
       }
    }

    public void addResource(String fullPath, URL url) {
        if (resources == null) {
            resources  = new HashMap<String, URL>();
        }
        resources.put(fullPath, url);
    }
    
    public Map<String, URL> getResources() {
        if (resources == null) {
            return Collections.emptyMap();
        }
        return resources;
    }
}
