package biz.aQute.resolve;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.osgi.resource.Resource;
import org.osgi.resource.Wire;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import aQute.bnd.build.Run;
import aQute.bnd.build.Workspace;
import aQute.bnd.build.model.BndEditModel;
import aQute.bnd.build.model.clauses.HeaderClause;
import aQute.bnd.build.model.clauses.VersionedClause;
import aQute.bnd.build.model.conversions.CollectionFormatter;
import aQute.bnd.build.model.conversions.Converter;
import aQute.bnd.build.model.conversions.HeaderClauseFormatter;
import aQute.bnd.osgi.Constants;
import aQute.bnd.osgi.Processor;
import aQute.bnd.osgi.resource.ResourceUtils;
import aQute.bnd.properties.Document;
import aQute.lib.io.IO;

public class Bndrun extends Run {

	private static final Logger													logger						= LoggerFactory
			.getLogger(Bndrun.class);

	private static final Converter<String,Collection< ? extends HeaderClause>>	runbundlesListFormatter		= new CollectionFormatter<HeaderClause>(
			",", new HeaderClauseFormatter(), null, "", "");
	private static final Converter<String,Collection< ? extends HeaderClause>> runbundlesWrappedFormatter = new CollectionFormatter<HeaderClause>(
			",\\\n\t", new HeaderClauseFormatter(), null);

	/**
	 * Create a Bndrun that will be stand alone if it contains -standalone. In
	 * that case the given workspace is ignored. Otherwise, the workspace must
	 * be a valid workspace.
	 */
	public static Bndrun createBndrun(Workspace workspace, File file) throws Exception {
		Processor processor;
		if (workspace != null) {
			Bndrun run = new Bndrun(workspace, file);
			if (run.getProperties().get(STANDALONE) == null) {
				return run;
			}
			// -standalone specified
			processor = run;
		} else {
			processor = new Processor();
			processor.setProperties(file);
		}

		Workspace standaloneWorkspace = Workspace.createStandaloneWorkspace(processor, file.toURI());
		Bndrun run = new Bndrun(standaloneWorkspace, file);
		return run;
	}

	public Bndrun(Workspace workspace, File propertiesFile) throws Exception {
		super(workspace, propertiesFile);
		if (!isStandalone()) {
			addBasicPlugin(new WorkspaceResourcesRepository(getWorkspace()));
		}
	}

	/**
	 * Use the resolver to calculate the <code>-runbundles</code> required to
	 * execute the bndrun configuration.
	 * <p>
	 * Use the return value with {@link Run#setProperty(String, String)} with
	 * key {@link Constants#RUNBUNDLES}
	 *
	 * @param failOnChanges if the build should fail when changes to the
	 *            <code>-runbundles</code> are detected
	 * @param writeOnChanges if the bndrun file should be updated when changes
	 *            to the <code>-runbundles</code> are detected are detected
	 * @return the calculated <code>-runbundles</code>
	 * @throws Exception
	 */
	public String resolve(boolean failOnChanges, boolean writeOnChanges) throws Exception {
		return resolve(failOnChanges, writeOnChanges, runbundlesListFormatter);
	}

	public <T> T resolve(boolean failOnChanges, boolean writeOnChanges,
			Converter<T,Collection< ? extends HeaderClause>> runbundlesFormatter) throws Exception {
		try (ProjectResolver projectResolver = new ProjectResolver(this)) {
			try {
				Map<Resource,List<Wire>> resolution = projectResolver.resolve();
				if (!projectResolver.isOk()) {
					return runbundlesFormatter.convert(Collections.<VersionedClause> emptyList());
				}
				Set<Resource> resources = resolution.keySet();
				List<VersionedClause> runBundles = new ArrayList<>();
				for (Resource resource : resources) {
					VersionedClause runBundle = ResourceUtils.toVersionClause(resource, "[===,==+)");
					if (!runBundles.contains(runBundle)) {
						runBundles.add(runBundle);
					}
				}
				Collections.sort(runBundles, new Comparator<VersionedClause>() {
					@Override
					public int compare(VersionedClause a, VersionedClause b) {
						int diff = a.getName().compareTo(b.getName());
						return (diff != 0) ? diff : a.getVersionRange().compareTo(b.getVersionRange());
					}
				});

				File runFile = getPropertiesFile();
				BndEditModel bem = new BndEditModel(getWorkspace());
				Document doc = new Document(IO.collect(runFile));
				bem.loadFrom(doc);

				List<VersionedClause> bemRunBundles = bem.getRunBundles();
				if (bemRunBundles == null)
					bemRunBundles = new ArrayList<>();

				String originalRunbundlesString = runbundlesWrappedFormatter.convert(bemRunBundles);
				logger.debug("Original -runbundles was:\n\t {}", originalRunbundlesString);
				String runbundlesString = runbundlesWrappedFormatter.convert(runBundles);
				logger.debug("Resolved -runbundles is:\n\t {}", runbundlesString);

				List<VersionedClause> deltaAdd = new ArrayList<>(runBundles);
				deltaAdd.removeAll(bemRunBundles);
				List<VersionedClause> deltaRemove = new ArrayList<>(bemRunBundles);
				deltaRemove.removeAll(runBundles);
				boolean added = bemRunBundles.addAll(deltaAdd);
				boolean removed = bemRunBundles.removeAll(deltaRemove);
				if (added || removed) {
					if (failOnChanges && !bemRunBundles.isEmpty()) {
						error("The runbundles have changed. Failing the build!\nWas: %s\nIs: %s",
								originalRunbundlesString, runbundlesString);
						return runbundlesFormatter.convert(Collections.<VersionedClause> emptyList());
					}
					if (writeOnChanges) {
						bem.setRunBundles(bemRunBundles);
						String runBundlesProperty = bem.getDocumentChanges().get(Constants.RUNBUNDLES);
						logger.debug("Writing changes to {}", runFile.getAbsolutePath());
						logger.debug("{}:{}", Constants.RUNBUNDLES, runBundlesProperty);
						bem.saveChangesTo(doc);
						IO.store(doc.get(), runFile);
					}
				}
				return runbundlesFormatter.convert(bemRunBundles);
			} finally {
				getInfo(projectResolver);
			}
		}
	}
}
