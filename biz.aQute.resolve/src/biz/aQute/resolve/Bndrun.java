package biz.aQute.resolve;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.osgi.resource.Requirement;
import org.osgi.service.resolver.ResolutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import aQute.bnd.build.Container;
import aQute.bnd.build.Run;
import aQute.bnd.build.Workspace;
import aQute.bnd.build.WorkspaceLayout;
import aQute.bnd.build.model.BndEditModel;
import aQute.bnd.build.model.clauses.HeaderClause;
import aQute.bnd.build.model.clauses.VersionedClause;
import aQute.bnd.build.model.conversions.CollectionFormatter;
import aQute.bnd.build.model.conversions.Converter;
import aQute.bnd.build.model.conversions.HeaderClauseFormatter;
import aQute.bnd.exceptions.Exceptions;
import aQute.bnd.header.Parameters;
import aQute.bnd.help.Syntax;
import aQute.bnd.help.instructions.ResolutionInstructions;
import aQute.bnd.help.instructions.ResolutionInstructions.ResolveMode;
import aQute.bnd.osgi.Constants;
import aQute.bnd.osgi.Processor;
import aQute.bnd.osgi.resource.FilterParser;
import aQute.bnd.osgi.resource.FilterParser.Expression;
import aQute.lib.io.IO;
import aQute.lib.utf8properties.UTF8Properties;
import aQute.libg.cryptography.SHA1;

/**
 * This is a resolving version of the Run class. The name of this class is known
 * in the super class so do not rename it or change {@link Run}
 */
public class Bndrun extends Run {
	Logger																		logger					= LoggerFactory
		.getLogger(Bndrun.class);
	final BndEditModel															model;
	final ResolutionInstructions												resolutionInstructions;
	private static final Converter<String, Collection<? extends HeaderClause>>	runbundlesListFormatter	= new CollectionFormatter<>(
		",", new HeaderClauseFormatter(), null, "", "");

	/**
	 * Create a Bndrun that will be stand alone if it contains -standalone. In
	 * that case the given workspace is ignored. Otherwise, the workspace must
	 * be a valid workspace.
	 */
	public static Bndrun createBndrun(Workspace workspace, File file) throws Exception {
		Processor processor;
		if (workspace != null) {
			Bndrun run = new Bndrun(workspace, file);
			if (run.getProperties()
				.get(STANDALONE) == null) {
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

	public Bndrun(BndEditModel model) throws Exception {
		super(model.getWorkspace(), model.getProject()
			.getPropertiesFile());
		this.model = model;
		this.resolutionInstructions = Syntax.getInstructions(model.getProject(), ResolutionInstructions.class);

	}

	public Bndrun(Workspace workspace, File propertiesFile) throws Exception {
		super(workspace, propertiesFile);
		this.model = new BndEditModel(this);
		this.resolutionInstructions = Syntax.getInstructions(model.getProject(), ResolutionInstructions.class);
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
		Converter<T, Collection<? extends HeaderClause>> runbundlesFormatter) throws Exception {

		RunResolution resolution = RunResolution.resolve(this, this, null);

		if (!resolution.isOK()) {
			throw resolution.exception;
		}
		update(resolution, failOnChanges, writeOnChanges);
		return runbundlesFormatter.convert(model.getRunBundles());
	}

	public RunResolution resolve(ResolutionCallback... callbacks) throws Exception {
		RunResolution resolution = RunResolution.resolve(this, this, Arrays.asList(callbacks))
			.reportException();
		if (!resolution.isOK()) {
			if (resolution.exception instanceof ResolutionException) {
				ResolutionException re = (ResolutionException) resolution.exception;
				FilterParser filterParser = new FilterParser();
				for (Requirement r : re.getUnresolvedRequirements()) {
					Expression parse = filterParser.parse(r);
					error(" -> %s : %s", r.getNamespace(), parse);
				}
				error("  log: %s", resolution.log);
				return resolution;
			}
			throw resolution.exception;
		}
		return resolution;
	}

	public boolean update(RunResolution resolution, boolean failOnChanges, boolean writeOnChanges) throws Exception {
		List<VersionedClause> runBundlesBeforeUpdate = model.getRunBundles();
		if (resolution.updateBundles(model)) {
			if (failOnChanges) {
				error("Fail on changes set to true (--xchange,-x) and there are changes");
				error("   Existing runbundles   %s", runBundlesBeforeUpdate);
				error("   Calculated runbundles %s", resolution.getRunBundles());
			} else {
				if (writeOnChanges) {
					try {
						model.saveChanges();
					} catch (Exception e) {
						error("Could not save runbundles in their properties file %s. %s", model.getProject(),
							e.getMessage());
					}
				}
			}
			return true;
		}
		return false;
	}

	public BndEditModel getModel() {
		return model;
	}

	/**
	 * We override to resolve before we get the runbundles.
	 */
	@Override
	public Collection<Container> getRunbundles() throws Exception {
		Parameters gestalt = getWorkspace().getGestalt();

		ResolveMode resolve = resolutionInstructions.resolve();
		if (resolve == null)
			resolve = ResolveMode.manual;

		switch (resolve) {
			case auto :
			case manual :
			default :
				break;

			case cache :
				return cache();

			case batch :
				if (!gestalt.containsKey(Constants.GESTALT_BATCH) && !super.getRunbundles().isEmpty())
					break;

				// fall through

			case beforelaunch :
				return RunResolution.getRunBundles(this, true)
					.map(this::parseRunbundles)
					.orElseThrow(IllegalArgumentException::new);
		}
		return super.getRunbundles();
	}

	enum CacheReason {
		NO_CACHE_FILE,
		NOT_A_BND_LAYOUT,
		CACHE_STALE_PROJECT,
		CACHE_STALE_WORKSPACE,
		USE_CACHE,
		INVALID_CACHE;

	}

	CacheReason testReason;

	private Collection<Container> cache() throws Exception {
		Workspace ws = getWorkspace();
		File ours = getPropertiesFile();
		File cache = getCacheFile(ours);

		CacheReason reason = getCacheReason(cache);

		testReason = reason;

		trace("force = %s", reason);

		try (Processor p = new Processor()) {

			if (cache.isFile())
				p.setProperties(cache);

			if (reason == CacheReason.USE_CACHE) {
				trace("attempting to use cache");
				String runbundles = p.getProperty(Constants.RUNBUNDLES);
				Collection<Container> containers = parseRunbundles(runbundles);
				if (isAllOk(containers)) {
					trace("from cache %s", containers);
					return containers;
				} else {
					testReason = CacheReason.INVALID_CACHE;
					trace("the cached bundles were not ok, will resolve");
				}
			}
			trace("resolving");

			IO.delete(cache);
			if (cache.isFile()) {
				throw new IllegalStateException("cannot delete cache file " + cache);
			}

			RunResolution resolved = RunResolution.resolve(this, Collections.emptyList());
			if (!resolved.isOK()) {
				throw new IllegalStateException(resolved.report(false));
			}

			String spec = resolved.getRunBundlesAsString();

			List<Container> containers = parseRunbundles(spec);
			if (isAllOk(containers)) {
				UTF8Properties props = new UTF8Properties(p.getProperties());
				props.setProperty(Constants.RUNBUNDLES, spec);
				cache.getParentFile()
					.mkdirs();
				props.store(cache);
			}
			return containers;
		}
	}

	CacheReason getCacheReason(File cached) {
		long cacheLastModified = cached.lastModified();

		CacheReason reason;
		if (getWorkspace().getLayout() != WorkspaceLayout.BND)
			reason = CacheReason.NOT_A_BND_LAYOUT;
		else if (!cached.isFile())
			reason = CacheReason.NO_CACHE_FILE;
		else if (cacheLastModified < getWorkspace().lastModified())
			reason = CacheReason.CACHE_STALE_WORKSPACE;
		else if (cacheLastModified < lastModified())
			reason = CacheReason.CACHE_STALE_PROJECT;
		else
			reason = CacheReason.USE_CACHE;
		return reason;
	}


	/**
	 * Return the file used to cache the resolved solution for the given file
	 *
	 * @param file the file to find the cached file for
	 * @return the cached file
	 */
	public File getCacheFile(File file) {
		try {
			String path = file.getAbsolutePath();
			String digest = SHA1.digest(path.getBytes(StandardCharsets.UTF_8))
				.asHex();
			return getWorkspace().getCache("resolved-cache/".concat(digest)
				.concat(".resolved"));
		} catch (Exception e) {
			// not gonna happen
			throw Exceptions.duck(e);
		}
	}

	private boolean isAllOk(Collection<Container> containers) {
		for (Container c : containers) {
			if (c.getError() != null) {
				return false;
			}
		}
		return true;
	}

}
