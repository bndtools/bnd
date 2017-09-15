package bndtools.editor.project;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.bndtools.utils.collections.CollectionUtils;
import org.osgi.service.repository.Repository;

import aQute.bnd.build.Run;
import aQute.bnd.build.Workspace;
import aQute.bnd.build.model.BndEditModel;
import aQute.bnd.build.model.clauses.HeaderClause;
import aQute.bnd.osgi.Processor;
import aQute.bnd.service.RepositoryPlugin;
import bndtools.central.Central;

class RepositoriesEditModel {
    private final List<Repository> pluginOrder;
    private final List<HeaderClause> standalone;
    private final List<HeaderClause> ignoreStandalone;
    private final List<Repository> actualOrder = new ArrayList<>();
    private final Set<Repository> included = new HashSet<>();
    private final List<String> runrepos;
    private final Run run;
    private final BndEditModel model;

    RepositoriesEditModel(BndEditModel model) {
        this.model = model;
        this.pluginOrder = model.getWorkspace().getPlugins(Repository.class);
        this.standalone = model.getStandaloneLinks();
        this.runrepos = model.getRunRepos();
        this.ignoreStandalone = model.getIgnoreStandalone();
        this.run = (Run) (model.getProject() instanceof Run ? model.getProject() : null);

        if (runrepos == null) {
            actualOrder.addAll(pluginOrder);
            included.addAll(pluginOrder);
        } else {
            List<Repository> remains = new ArrayList<>(pluginOrder);

            for (String name : runrepos) {
                Repository r = find(name);
                if (r != null) {
                    actualOrder.add(r);
                    included.add(r);
                    remains.remove(r);
                }
            }
            actualOrder.addAll(remains);
        }
    }

    private Repository find(String sought) {
        for (Repository r : pluginOrder) {
            String name = toName(r);
            if (sought.equals(name))
                return r;
        }
        return null;
    }

    boolean up(int[] indexes) {
        boolean result = CollectionUtils.moveUp(actualOrder, indexes);
        commitToModel(model);
        return result;
    }

    boolean down(int[] indexes) {
        boolean result = CollectionUtils.moveDown(actualOrder, indexes);
        commitToModel(model);
        return result;
    }

    boolean isIncluded(Repository repository) {
        return included.contains(repository);
    }

    void toggleInclusion(Repository repository) {
        if (included.contains(repository))
            included.remove(repository);
        else
            included.add(repository);
        model.setRunRepos(getRunRepos());
    }

    public void setIncluded(boolean checked, Repository repository) {
        if (isIncluded(repository) == checked)
            return;

        toggleInclusion(repository);
    }

    boolean canRemove(Repository repository) {
        HeaderClause clause = toHeaderClause(repository);
        return clause != null;
    }

    boolean canAdd(HeaderClause clause) {
        return isStandalone() && !standalone.contains(clause);
    }

    boolean remove(Repository repository) throws Exception {
        HeaderClause clause = toHeaderClause(repository);
        if (clause != null && standalone != null) {
            standalone.remove(clause);
            model.setStandaloneLinks(standalone);
            updateStandaloneWorkspace(model);
            commitToModel(model);
            return true;
        }
        return false;
    }

    List<String> getRunRepos() {
        if (included.size() == actualOrder.size() && pluginOrder.equals(actualOrder))
            return null;

        List<String> out = new ArrayList<String>();
        for (Repository r : actualOrder) {
            if (isIncluded(r))
                out.add(toName(r));
        }
        return out;
    }

    private String toName(Repository repo) {
        if (repo instanceof aQute.bnd.deployer.repository.wrapper.Plugin) {
            aQute.bnd.deployer.repository.wrapper.Plugin wrapper = (aQute.bnd.deployer.repository.wrapper.Plugin) repo;
            wrapper.init();
            return wrapper.toString();
        }
        if (repo instanceof RepositoryPlugin) {
            return ((RepositoryPlugin) repo).getName();
        }
        return repo.toString();
    }

    private HeaderClause toHeaderClause(Repository repository) {
        String name = toName(repository);
        for (HeaderClause clause : standalone) {
            if (name.equals(clause.getName())) {
                return clause;
            }
        }
        return null;
    }

    List<HeaderClause> getStandalone() {
        return this.standalone;
    }

    boolean isStandalone() {
        return standalone != null;
    }

    boolean isDirty() {
        return getRunRepos().equals(runrepos);
    }

    public List<Repository> getOrdered() {
        return Collections.unmodifiableList(actualOrder);
    }

    void commitToModel(BndEditModel model) {
        model.setRunRepos(getRunRepos());
        model.setStandaloneLinks(standalone);
    }

    void add(HeaderClause clause) throws Exception {
        if (canAdd(clause)) {
            standalone.remove(clause);
            standalone.add(clause);
            model.setStandaloneLinks(standalone);
            commitToModel(model);
            updateStandaloneWorkspace(model);
        }
    }

    RepositoriesEditModel toggleStandalone(BndEditModel model) throws Exception {
        if (run == null)
            return this;

        if (isStandalone()) {
            //
            // Set to normal workspace mode
            //
            model.setStandaloneLinks(null);
            model.setIgnoreStandalone(standalone);
            model.setWorkspace(Central.getWorkspace());
        } else {
            //
            // Create standalone
            //
            List<HeaderClause> emptyList = Collections.emptyList();
            model.setStandaloneLinks(ignoreStandalone == null ? emptyList : ignoreStandalone);
            model.setIgnoreStandalone(null);
            updateStandaloneWorkspace(model);
        }

        RepositoriesEditModel r = new RepositoriesEditModel(model);
        return r;
    }

    void updateStandaloneWorkspace(BndEditModel model) throws Exception {

        assert isStandalone();

        Processor properties = model.getProperties();
        model.setWorkspace(Workspace.createStandaloneWorkspace(properties, model.getProject().getPropertiesFile().toURI()));
    }

    public RepositoriesEditModel setStandalone(boolean standalone, BndEditModel model) throws Exception {

        if (isStandalone() == standalone)
            return this;

        return toggleStandalone(model);

    }

    public void setOrder(List<Repository> order) {
        this.actualOrder.clear();
        this.actualOrder.addAll(order);
        commitToModel(model);
    }

    public boolean isStandaloneRepository(Repository r) {
        if (!isStandalone())
            return false;

        String rname = toName(r);

        for (HeaderClause clause : standalone) {
            String name = clause.getAttribs().get("name");
            if (name == null)
                name = clause.getName();

            if (name.equals(rname))
                return true;
        }
        return false;
    }

}
