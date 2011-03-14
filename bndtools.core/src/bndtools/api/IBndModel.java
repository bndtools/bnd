package bndtools.api;

import java.util.List;
import java.util.Map;

import bndtools.model.clauses.VersionedClause;

public interface IBndModel {

    List<String> getAllPropertyNames();

    Object genericGet(String propertyName);

    void genericSet(String propertyName, Object value);

    List<VersionedClause> getBuildPath();

    void setBuildPath(List<? extends VersionedClause> paths);

    List<VersionedClause> getRunBundles();

    void setRunBundles(List<? extends VersionedClause> paths);

    String getRunFramework();

    void setRunFramework(String clause);

    List<String> getSubBndFiles();

    void setSubBndFiles(List<String> subBndFiles);

    Map<String, String> getRunProperties();

    void setRunProperties(Map<String, String> props);

    String getRunVMArgs();

    void setRunVMArgs(String args);

    void setTestSuites(List<String> suites);

    List<String> getTestSuites();

}