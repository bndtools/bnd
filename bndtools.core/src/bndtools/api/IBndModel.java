package bndtools.api;

import java.beans.PropertyChangeListener;
import java.util.List;
import java.util.Map;

import bndtools.model.clauses.ExportedPackage;
import bndtools.model.clauses.HeaderClause;
import bndtools.model.clauses.ServiceComponent;
import bndtools.model.clauses.VersionedClause;

public interface IBndModel {

    List<String> getAllPropertyNames();

    Object genericGet(String propertyName);

    void genericSet(String propertyName, Object value);

    String getBundleActivator();

    void setBundleActivator(String bundleActivator);

    List<VersionedClause> getBuildPath();

    void setBuildPath(List<? extends VersionedClause> paths);

    List<VersionedClause> getRunBundles();

    void setRunBundles(List<? extends VersionedClause> paths);

    List<VersionedClause> getBackupRunBundles();

    void setBackupRunBundles(List<? extends VersionedClause> paths);

    String getRunFramework();

    void setRunFramework(String framework);

    List<String> getSubBndFiles();

    void setSubBndFiles(List<String> subBndFiles);

    Map<String, String> getRunProperties();

    void setRunProperties(Map<String, String> props);

    String getRunVMArgs();

    void setRunVMArgs(String args);

    void setTestSuites(List<String> suites);

    List<String> getTestSuites();
    
    List<String> getDSAnnotationPatterns();
    
    void setDSAnnotationPatterns(List<? extends String> patterns);

    /**
     * @deprecated
     */
    @Deprecated
    void setServiceComponents(List<? extends ServiceComponent> components);

    /**
     * @deprecated
     */
    @Deprecated
    List<ServiceComponent> getServiceComponents();

    void setPrivatePackages(List<? extends String> packages);

    List<String> getPrivatePackages();

    void setSystemPackages(List<? extends ExportedPackage> packages);

    List<ExportedPackage> getSystemPackages();

    EE getEE();

    void setEE(EE ee);

    List<Requirement> getRunRequire();

    void setRunRequire(List<Requirement> requires);

    List<String> getRunRepos();

    void setRunRepos(List<String> repos);

    ResolveMode getResolveMode();

    void setResolveMode(ResolveMode mode);
    
    List<HeaderClause> getPlugins();
    
    void setPlugins(List<HeaderClause> plugins);
    
    List<String> getPluginPath();
    
    void setPluginPath(List<String> pluginPath);
    
    

    void addPropertyChangeListener(String property, PropertyChangeListener listener);

    void removePropertyChangeListener(String property, PropertyChangeListener listener);
    
}