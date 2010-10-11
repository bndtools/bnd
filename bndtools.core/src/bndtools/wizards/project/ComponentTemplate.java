package bndtools.wizards.project;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import bndtools.api.IProjectTemplate;
import bndtools.editor.model.BndEditModel;
import bndtools.model.clauses.VersionedClause;

public class ComponentTemplate implements IProjectTemplate {

    public void modifyInitialBndModel(BndEditModel model) {
        List<VersionedClause> buildPath = new ArrayList<VersionedClause>();
        List<VersionedClause> tmp;

        tmp = model.getBuildPath();
        if (tmp != null) buildPath.addAll(tmp);
        buildPath.add(new VersionedClause("osgi.core", new HashMap<String, String>()));
        buildPath.add(new VersionedClause("osgi.cmpn", new HashMap<String, String>()));
        buildPath.add(new VersionedClause("biz.aQute.bnd.annotation", new HashMap<String, String>()));
        buildPath.add(new VersionedClause("junit.osgi", new HashMap<String, String>()));

        model.setBuildPath(buildPath);

        List<VersionedClause> runPath = new ArrayList<VersionedClause>();
        tmp = model.getRunBundles();
        if (tmp != null) runPath.addAll(tmp);
        runPath.add(new VersionedClause("osgi.cmpn", new HashMap<String, String>()));
        runPath.add(new VersionedClause("org.apache.felix.scr", new HashMap<String, String>()));
        model.setRunBundles(runPath);
    }

}
