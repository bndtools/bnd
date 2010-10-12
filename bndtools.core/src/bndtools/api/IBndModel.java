package bndtools.api;

import java.util.List;
import java.util.Map;

import bndtools.model.clauses.VersionedClause;

public interface IBndModel {

    public List<VersionedClause> getBuildPath();

    public void setBuildPath(List<? extends VersionedClause> paths);

    public List<VersionedClause> getRunBundles();

    public void setRunBundles(List<? extends VersionedClause> paths);

    public String getRunFramework();

    public void setRunFramework(String clause);

    public List<String> getSubBndFiles();

    public void setSubBndFiles(List<String> subBndFiles);

    public Map<String, String> getRunProperties();

    public void setRunProperties(Map<String, String> props);

    public String getRunVMArgs();

    public void setRunVMArgs(String args);

}