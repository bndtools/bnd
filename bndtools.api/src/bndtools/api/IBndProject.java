package bndtools.api;

import java.net.URL;

public interface IBndProject {
    String getProjectName();

    void addResource(String path, String name, URL url);

    void addResource(String fullPath, URL url);
}
