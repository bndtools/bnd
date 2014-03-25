package org.bndtools.api;


public interface IBndProject {
    String getProjectName();

    void addResource(String fullPath, BndProjectResource bndProjectResource);
}
