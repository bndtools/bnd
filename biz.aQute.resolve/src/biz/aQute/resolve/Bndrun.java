package biz.aQute.resolve;

import java.io.File;
import java.util.Arrays;
import java.util.Collection;

import org.osgi.resource.Requirement;
import org.osgi.service.resolver.ResolutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import aQute.bnd.build.Container;
import aQute.bnd.build.Run;
import aQute.bnd.build.Workspace;
import aQute.bnd.build.model.BndEditModel;
import aQute.bnd.build.model.clauses.HeaderClause;
import aQute.bnd.build.model.conversions.CollectionFormatter;
import aQute.bnd.build.model.conversions.Converter;
import aQute.bnd.build.model.conversions.HeaderClauseFormatter;
import aQute.bnd.help.Syntax;
import aQute.bnd.help.instructions.ResolutionInstructions;
import aQute.bnd.help.instructions.ResolutionInstructions.ResolveMode;
import aQute.bnd.osgi.Constants;
import aQute.bnd.osgi.Processor;
import aQute.bnd.osgi.resource.FilterParser;
import aQute.bnd.osgi.resource.FilterParser.Expression;

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
		if (resolution.updateBundles(model)) {
			if (failOnChanges) {
				error("Fail on changes set to true (--xchange,-x) and there are changes");
				error("   Existing runbundles   %s", model.getRunBundles());
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

	@Override
	public Collection<Container> getRunbundles() throws Exception {
		if (resolutionInstructions.resolve() == ResolveMode.beforelaunch) {
			return RunResolution.getRunBundles(this, true)
				.map(this::parseRunbundles)
				.orElseThrow(s -> new IllegalArgumentException(s));
		}
		return super.getRunbundles();
	}

}
