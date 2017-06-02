package bndtools.model.repo;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedSet;

import org.bndtools.api.ILogger;
import org.bndtools.api.Logger;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.StructuredViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.widgets.Display;
import org.osgi.resource.Capability;
import org.osgi.resource.Requirement;
import org.osgi.service.repository.Repository;

import aQute.bnd.build.Project;
import aQute.bnd.build.Workspace;
import aQute.bnd.build.WorkspaceRepository;
import aQute.bnd.osgi.Builder;
import aQute.bnd.service.IndexProvider;
import aQute.bnd.service.RepositoryPlugin;
import aQute.bnd.service.ResolutionPhase;
import aQute.bnd.version.Version;
import bndtools.Plugin;
import bndtools.central.Central;
import bndtools.central.WorkspaceR5Repository;

public class RepositoryTreeContentProvider implements ITreeContentProvider {

    private static final String CACHE_REPOSITORY = "cache";
    private static final ILogger logger = Logger.getLogger(RepositoryTreeContentProvider.class);

    private final EnumSet<ResolutionPhase> phases;

    private String rawFilter = null;
    private String wildcardFilter = null;
    private boolean showRepos = true;

    private Requirement requirementFilter = null;

    private final Map<RepositoryPlugin,Map<String,Object[]>> repoPluginListResults = new HashMap<>();
    private StructuredViewer structuredViewer;

    public RepositoryTreeContentProvider() {
        this.phases = EnumSet.allOf(ResolutionPhase.class);
    }

    public RepositoryTreeContentProvider(ResolutionPhase mode) {
        this.phases = EnumSet.of(mode);
    }

    public RepositoryTreeContentProvider(EnumSet<ResolutionPhase> modes) {
        this.phases = modes;
    }

    public String getFilter() {
        return rawFilter;
    }

    public void setFilter(String filter) {
        this.rawFilter = filter;
        if (filter == null || filter.length() == 0 || filter.trim().equals("*"))
            wildcardFilter = null;
        else
            wildcardFilter = "*" + filter.trim() + "*";
    }

    public void setRequirementFilter(Requirement requirement) {
        this.requirementFilter = requirement;
    }

    public void setShowRepos(boolean showRepos) {
        this.showRepos = showRepos;
    }

    public boolean isShowRepos() {
        return showRepos;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Object[] getElements(Object inputElement) {
        Collection<Object> result;
        if (inputElement instanceof Workspace) {
            result = new ArrayList<Object>();
            Workspace workspace = (Workspace) inputElement;
            addRepositoryPlugins(result, workspace);
        } else if (inputElement instanceof Collection) {
            result = new ArrayList<Object>();
            addCollection(result, (Collection<Object>) inputElement);
        } else if (inputElement instanceof Object[]) {
            result = new ArrayList<Object>();
            addCollection(result, Arrays.asList(inputElement));
        } else {
            result = Collections.emptyList();
        }

        return result.toArray();
    }

    @Override
    public void dispose() {}

    @Override
    public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
        if (viewer instanceof StructuredViewer) {
            this.structuredViewer = (StructuredViewer) viewer;
            repoPluginListResults.clear();
        }
    }

    @Override
    public Object[] getChildren(Object parentElement) {
        Object[] result = null;

        if (parentElement instanceof RepositoryPlugin) {
            RepositoryPlugin repo = (RepositoryPlugin) parentElement;
            result = getRepositoryBundles(repo);
        } else if (parentElement instanceof RepositoryBundle) {
            RepositoryBundle bundle = (RepositoryBundle) parentElement;
            result = getRepositoryBundleVersions(bundle);
        } else if (parentElement instanceof Project) {
            Project project = (Project) parentElement;
            result = getProjectBundles(project);
        }

        return result;
    }

    @Override
    public Object getParent(Object element) {
        if (element instanceof RepositoryBundle) {
            return ((RepositoryBundle) element).getRepo();
        }
        if (element instanceof RepositoryBundleVersion) {
            return ((RepositoryBundleVersion) element).getParentBundle();
        }
        return null;
    }

    @Override
    public boolean hasChildren(Object element) {
        return element instanceof RepositoryPlugin || element instanceof RepositoryBundle || element instanceof Project;
    }

    private void addRepositoryPlugins(Collection<Object> result, Workspace workspace) {
        workspace.getErrors().clear();
        List<RepositoryPlugin> repoPlugins = workspace.getPlugins(RepositoryPlugin.class);
        for (String error : workspace.getErrors()) {
            logger.logError(error, null);
        }
        for (RepositoryPlugin repoPlugin : repoPlugins) {
            if (CACHE_REPOSITORY.equals(repoPlugin.getName()))
                continue;
            if (repoPlugin instanceof IndexProvider) {
                IndexProvider indexProvider = (IndexProvider) repoPlugin;
                if (!supportsPhase(indexProvider))
                    continue;
            }
            if (showRepos)
                result.add(repoPlugin);
            else
                result.addAll(Arrays.asList(getRepositoryBundles(repoPlugin)));
        }
    }

    private void addCollection(Collection<Object> result, Collection<Object> inputs) {
        for (Object input : inputs) {
            if (input instanceof RepositoryPlugin) {
                RepositoryPlugin repo = (RepositoryPlugin) input;
                if (repo instanceof IndexProvider) {
                    if (!supportsPhase((IndexProvider) repo))
                        continue;
                }

                if (showRepos) {
                    result.add(repo);
                } else {
                    Object[] bundles = getRepositoryBundles(repo);
                    if (bundles != null && bundles.length > 0)
                        result.addAll(Arrays.asList(bundles));
                }
            }
        }
    }

    private boolean supportsPhase(IndexProvider provider) {
        Set<ResolutionPhase> supportedPhases = provider.getSupportedPhases();
        for (ResolutionPhase phase : phases) {
            if (supportedPhases.contains(phase))
                return true;
        }
        return false;
    }

    Object[] getProjectBundles(Project project) {
        ProjectBundle[] result = null;
        try {
            Collection< ? extends Builder> builders = project.getSubBuilders();
            result = new ProjectBundle[builders.size()];

            int i = 0;
            for (Builder builder : builders) {
                ProjectBundle bundle = new ProjectBundle(project, builder.getBsn());
                result[i++] = bundle;
            }
        } catch (Exception e) {
            logger.logError(MessageFormat.format("Error querying sub-bundles for project {0}.", project.getName()), e);
        }
        return result;
    }

    Object[] getRepositoryBundleVersions(RepositoryBundle bundle) {
        RepositoryBundleVersion[] result = null;

        SortedSet<Version> versions = null;
        try {
            versions = bundle.getRepo().versions(bundle.getBsn());
        } catch (Exception e) {
            logger.logError(MessageFormat.format("Error querying versions for bundle {0} in repository {1}.", bundle.getBsn(), bundle.getRepo().getName()), e);
        }
        if (versions != null) {
            result = new RepositoryBundleVersion[versions.size()];
            int i = 0;
            for (Version version : versions) {
                result[i++] = new RepositoryBundleVersion(bundle, version);
            }
        }
        return result;
    }

    Object[] getRepositoryBundles(final RepositoryPlugin repoPlugin) {
        Object[] result = null;

        if (requirementFilter != null) {
            if (repoPlugin instanceof Repository) {
                result = searchR5Repository(repoPlugin, (Repository) repoPlugin);
            } else if (repoPlugin instanceof WorkspaceRepository) {
                try {
                    WorkspaceR5Repository workspaceRepo = Central.getWorkspaceR5Repository();
                    result = searchR5Repository(repoPlugin, workspaceRepo);
                } catch (Exception e) {
                    logger.logError("Error querying workspace repository", e);
                }
            }
            return result;
        }

        /*
         * We can't directly call repoPlugin.list() since we are on the UI thread
         * so the plan is to first check to see if we have cached the list results
         * already from a previous job, if so, return those results directly
         * If not, then we need to create a background job that will call list()
         * and once it is finished, we tell the Viewer to refresh this node and the
         * next time this method gets called the 'results' will be available in the cache
         */
        Map<String,Object[]> listResults = repoPluginListResults.get(repoPlugin);

        if (listResults == null) {
            listResults = new HashMap<>();

            repoPluginListResults.put(repoPlugin, listResults);
        }

        result = listResults.get(wildcardFilter);

        if (result == null) {
            Job job = new Job("Loading " + repoPlugin.getName() + " content...") {

                @Override
                protected IStatus run(IProgressMonitor monitor) {
                    IStatus status = Status.OK_STATUS;
                    Object[] jobresult;
                    List<String> bsns = null;

                    try {
                        bsns = repoPlugin.list(wildcardFilter);
                    } catch (Exception e) {
                        String message = MessageFormat.format("Error querying repository {0}.", repoPlugin.getName());
                        logger.logError(message, e);
                        status = new Status(IStatus.ERROR, Plugin.PLUGIN_ID, message, e);
                    }
                    if (bsns != null) {
                        Collections.sort(bsns);
                        jobresult = new RepositoryBundle[bsns.size()];
                        int i = 0;
                        for (String bsn : bsns) {
                            jobresult[i++] = new RepositoryBundle(repoPlugin, bsn);
                        }

                        Map<String,Object[]> listResults = repoPluginListResults.get(repoPlugin);
                        listResults.put(wildcardFilter, jobresult);

                        Display.getDefault().asyncExec(new Runnable() {
                            @Override
                            public void run() {
                                structuredViewer.refresh(repoPlugin, true);
                            }
                        });
                    }

                    return status;
                }
            };
            job.schedule();

            // wait 100 ms and see if the job will complete fast (likely already cached)
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {}

            IStatus status = job.getResult();

            if (status != null && status.isOK()) {
                Map<String,Object[]> fastResults = repoPluginListResults.get(repoPlugin);
                result = fastResults.get(wildcardFilter);
            } else {
                Object[] loading = new Object[] {
                        new LoadingContentElement()
                };

                listResults.put(wildcardFilter, loading);
                result = loading;
            }
        }

        return result;
    }

    private Object[] searchR5Repository(RepositoryPlugin repoPlugin, Repository osgiRepo) {
        Object[] result;
        Set<RepositoryResourceElement> resultSet = new LinkedHashSet<RepositoryResourceElement>();
        Map<Requirement,Collection<Capability>> providers = osgiRepo.findProviders(Collections.singleton(requirementFilter));

        for (Entry<Requirement,Collection<Capability>> providersEntry : providers.entrySet()) {
            for (Capability providerCap : providersEntry.getValue())
                resultSet.add(new RepositoryResourceElement(repoPlugin, providerCap.getResource()));
        }

        result = resultSet.toArray();
        return result;
    }
}
