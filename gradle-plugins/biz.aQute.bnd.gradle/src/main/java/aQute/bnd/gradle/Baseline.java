package aQute.bnd.gradle;

import static aQute.bnd.gradle.BndUtils.builtBy;
import static aQute.bnd.gradle.BndUtils.unwrap;
import static java.util.stream.Collectors.toList;
import static org.gradle.api.tasks.PathSensitivity.RELATIVE;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Comparator;
import java.util.Deque;
import java.util.Formatter;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.stream.StreamSupport;

import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.ProjectLayout;
import org.gradle.api.file.RegularFile;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.Provider;
import org.gradle.api.provider.ProviderFactory;
import org.gradle.api.reporting.ReportingExtension;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.TaskAction;

import aQute.bnd.differ.Baseline.BundleInfo;
import aQute.bnd.differ.Baseline.Info;
import aQute.bnd.differ.DiffPluginImpl;
import aQute.bnd.header.Parameters;
import aQute.bnd.osgi.Instructions;
import aQute.bnd.osgi.Jar;
import aQute.bnd.osgi.Processor;
import aQute.bnd.service.diff.Diff;
import aQute.bnd.version.Version;
import aQute.lib.io.IO;
import aQute.lib.strings.Strings;

/**
 * Baseline task type for Gradle.
 * <p>
 * This task type can be used to baseline a bundle.
 * <p>
 * Here is an example of using the Baseline task type:
 *
 * <pre>
 * import aQute.bnd.gradle.Baseline
 * configurations {
 *   baseline
 * }
 * dependencies {
 *     baseline("group": group, "name": jar.archiveBaseName) {
 *       version {
 *         strictly "(0,${jar.archiveVersion.get()}["
 *       }
 *       transitive false
 *     }
 * }
 * tasks.register("baseline", Baseline) {
 *   bundle = jar
 *   baseline = configurations.baseline
 * }
 * </pre>
 * <p>
 * Properties:
 * <ul>
 * <li>ignoreFailures - If true the build will not fail due to baseline
 * problems; instead an error message will be logged. Otherwise, the build will
 * fail. The default is false.</li>
 * <li>baselineReportDirName - This is the name of the baseline reports
 * directory. Can be a name or a path relative to
 * ReportingExtension.getBaseDir(). The default name is "baseline".</li>
 * <li>bundle - This is the bundle to be baselined. It can either be a File or a
 * task that produces a bundle. This property must be set.</li>
 * <li>baseline - This is the baseline bundle. It can either be a File or a
 * Configuration. If a Configuration is specified, it must contain a single
 * file; otherwise an exception will fail the build. This property must be
 * set.</li>
 * <li>diffignore - These are the bundle headers or resource paths to ignore
 * when baselining. The default is nothing ignored.</li>
 * <li>diffpackages - These are the names of the exported packages to baseline.
 * The default is all exported packages.</li>
 * </ul>
 */
public class Baseline extends DefaultTask {
	private boolean								ignoreFailures	= false;
	private final ListProperty<String>			diffpackages;
	private final ListProperty<String>			diffignore;
	private final Property<String>				baselineReportDirName;
	private final DirectoryProperty				baselineReportDirectory;
	private final RegularFileProperty			reportFile;
	private final ConfigurableFileCollection	bundleCollection;
	private final ConfigurableFileCollection	baselineCollection;

	private final ProjectLayout					layout;
	private final ProviderFactory				providers;

	/**
	 * Whether baseline failures should be ignored.
	 *
	 * @return <code>true</code> if baseline failures will not fail the task.
	 *         Otherwise, a baseline failure will fail the task. The default is
	 *         <code>false</code>.
	 */
	@Input
	public boolean isIgnoreFailures() {
		return ignoreFailures;
	}

	/**
	 * Whether baseline failures should be ignored.
	 * <p>
	 * Alias for {@link #isIgnoreFailures()}.
	 *
	 * @return <code>true</code> if baseline failures will not fail the task.
	 *         Otherwise, a baseline failure will fail the task. The default is
	 *         <code>false</code>.
	 */
	@Internal
	public boolean getIgnoreFailures() {
		return isIgnoreFailures();
	}

	/**
	 * Set whether baseline failures should be ignored.
	 *
	 * @param ignoreFailures If <code>true</code>, then baseline failures will
	 *            not fail the task. Otherwise, a baseline failure will fail the
	 *            task. The default is <code>false</code>.
	 */
	public void setIgnoreFailures(boolean ignoreFailures) {
		this.ignoreFailures = ignoreFailures;
	}

	/**
	 * The names of the exported packages to baseline.
	 * <p>
	 * The default is all exported packages.
	 *
	 * @return The list property of the exported packages to baseline.
	 */
	@Input
	public ListProperty<String> getDiffpackages() {
		return diffpackages;
	}

	/**
	 * The bundle headers or resource paths to ignore when baselining.
	 * <p>
	 * The default is nothing ignored.
	 *
	 * @return The list property of the bundle headers or resource paths to
	 *         ignore when baselining.
	 */
	@Input
	public ListProperty<String> getDiffignore() {
		return diffignore;
	}

	/**
	 * The name of the baseline reports directory.
	 * <p>
	 * Can be a name or a path relative to
	 * <code>ReportingExtension.getBaseDirectory()</code>. The default name is
	 * "baseline".
	 *
	 * @return The property of the name of the baseline reports directory.
	 */
	@Internal("Represented by reportFile")
	public Property<String> getBaselineReportDirName() {
		return baselineReportDirName;
	}

	/**
	 * The baseline reports directory.
	 * <p>
	 * The default is
	 * <code>ReportingExtension.getBaseDirectory().dir(baselineReportDirName)</code>.
	 *
	 * @return The property of the baseline reports directory.
	 */
	@Internal("Represented by reportFile")
	public DirectoryProperty getBaselineReportDirectory() {
		return baselineReportDirectory;
	}

	/**
	 * The baseline reports file.
	 * <p>
	 * The default is
	 * <code>baselineReportDirectory.dir("${task.name}/${bundlename}.txt")</code>.
	 *
	 * @return The property of the baseline reports file.
	 */
	@OutputFile
	public RegularFileProperty getReportFile() {
		return reportFile;
	}

	/**
	 * Create a Baseline task.
	 */
	public Baseline() {
		super();
		this.layout = getProject().getLayout();
		this.providers = getProject().getProviders();
		ObjectFactory objects = getProject().getObjects();
		baselineReportDirName = objects.property(String.class)
			.convention("baseline");
		baselineReportDirectory = objects.directoryProperty()
			.convention(getProject().getExtensions()
				.getByType(ReportingExtension.class)
				.getBaseDirectory()
				.dir(baselineReportDirName));
		String taskName = getName();
		reportFile = objects.fileProperty()
			.convention(baselineReportDirectory.file(providers.provider(() -> {
				File bundlefile = unwrap(getBundle());
				String[] parts = Strings.extension(bundlefile.getName());
				return String.format("%s/%s.txt", taskName, (parts != null) ? parts[0] : bundlefile.getName());
			})));
		diffignore = objects.listProperty(String.class)
			.empty();
		diffpackages = objects.listProperty(String.class)
			.empty();
		bundleCollection = objects.fileCollection();
		baselineCollection = objects.fileCollection();
		getInputs().files(bundleCollection)
			.withPathSensitivity(RELATIVE)
			.withPropertyName("bundleCollection");
		getInputs().files(baselineCollection)
			.withPropertyName("baselineCollection");
	}

	/**
	 * Return the bundle file to be baselined.
	 * <p>
	 * An exception will be thrown if the set bundle does not result in exactly
	 * one file.
	 *
	 * @return The provider of the bundle file to be baselined.
	 */
	@InputFile
	public Provider<RegularFile> getBundle() {
		return layout.file(providers.provider(bundleCollection::getSingleFile));
	}

	/**
	 * Set the bundle to be baselined.
	 *
	 * @param file The argument will be handled using
	 *            ConfigurableFileCollection.from().
	 */
	public void setBundle(Object file) {
		bundleCollection.setFrom(file);
		builtBy(bundleCollection, file);
	}

	/**
	 * Get the baseline bundle file.
	 * <p>
	 * An exception will be thrown if the baseline argument does not result in
	 * exactly one file.
	 *
	 * @return The provider of the baseline bundle file.
	 */
	@InputFile
	public Provider<RegularFile> getBaseline() {
		return layout.file(providers.provider(baselineCollection::getSingleFile));
	}

	/**
	 * Set the baseline bundle from a File.
	 *
	 * @param file The argument will be handled using
	 *            ConfigurableFileCollection.from().
	 */
	public void setBaseline(Object file) {
		baselineCollection.setFrom(file);
		builtBy(baselineCollection, file);
	}

	/**
	 * Add diffignore values.
	 *
	 * @param diffignore Bundle headers or resource paths to ignore when
	 *            baselining.
	 */
	public void diffignore(String... diffignore) {
		getDiffignore().addAll(diffignore);
	}

	/**
	 * Add diffpackages values.
	 *
	 * @param diffpackages Names of exported packages to baseline.
	 */
	public void diffpackages(String... diffpackages) {
		getDiffpackages().addAll(diffpackages);
	}

	@Internal("Used by baseline configuration")
	public org.gradle.api.tasks.bundling.Jar getBundleTask() {
		Deque<Object> queue = new ArrayDeque<>(bundleCollection.getBuiltBy());
		while (!queue.isEmpty()) {
			Object o = queue.removeFirst();
			if (o instanceof org.gradle.api.tasks.bundling.Jar) {
				org.gradle.api.tasks.bundling.Jar t = (org.gradle.api.tasks.bundling.Jar) o;
				if (Objects.nonNull(t.getExtensions()
					.findByName(BundleTaskExtension.NAME))) {
					return t;
				}
			} else if (o instanceof Provider) {
				Provider<?> p = (Provider<?>) o;
				queue.addFirst(p.get());
			} else if (o instanceof Map) {
				Map<?, ?> m = (Map<?, ?>) o;
				queue.addFirst(m.values());
			} else if (o instanceof Collection) {
				Collection<?> c = (Collection<?>) o;
				queue.addFirst(c.toArray());
			} else if ((o instanceof Iterable) && !(o instanceof Path)) {
				Iterable<?> i = (Iterable<?>) o;
				queue.addFirst(StreamSupport.stream(i.spliterator(), false)
					.toArray());
			} else if (o instanceof Object[]) {
				Object[] a = (Object[]) o;
				for (int i = a.length - 1; i >= 0; i--) {
					queue.addFirst(a[i]);
				}
			}
		}
		return null;
	}

	/**
	 * Baseline the bundle.
	 *
	 * @throws Exception An exception from baselining.
	 */
	@TaskAction
	public void baselineAction() throws Exception {
		File bundle = unwrap(getBundle());
		File baseline = unwrap(getBaseline());
		File report = unwrap(getReportFile());
		List<String> diffignoreList = unwrap(getDiffignore());
		List<String> diffpackagesList = unwrap(getDiffpackages());
		IO.mkdirs(report.getParentFile());
		boolean failure = false;
		try (Processor processor = new Processor(); Jar newer = new Jar(bundle); Jar older = new Jar(baseline)) {
			getLogger().debug("Baseline bundle {} against baseline {}", bundle, baseline);

			DiffPluginImpl differ = new DiffPluginImpl();
			differ.setIgnore(new Parameters(Strings.join(diffignoreList), processor));
			aQute.bnd.differ.Baseline baseliner = new aQute.bnd.differ.Baseline(processor, differ);
			List<Info> infos = baseliner
				.baseline(newer, older, new Instructions(new Parameters(Strings.join(diffpackagesList), processor)))
				.stream()
				.sorted(Comparator.comparing(info -> info.packageName))
				.collect(toList());
			BundleInfo bundleInfo = baseliner.getBundleInfo();
			try (Formatter f = new Formatter(report, "UTF-8", Locale.US)) {
				f.format("===============================================================%n");
				f.format("%s %s %s-%s", bundleInfo.mismatch ? "*" : " ", bundleInfo.bsn, newer.getVersion(),
					older.getVersion());

				if (bundleInfo.mismatch) {
					failure = true;
					if (Objects.nonNull(bundleInfo.suggestedVersion)) {
						f.format(" suggests %s", bundleInfo.suggestedVersion);
					}
					f.format("%n%#2S", baseliner.getDiff());
				}

				f.format("%n===============================================================%n");

				String format = "%s %-50s %-10s %-10s %-10s %-10s %-10s %s%n";
				f.format(format, " ", "Name", "Type", "Delta", "New", "Old", "Suggest", "If Prov.");

				for (Info info : infos) {
					Diff packageDiff = info.packageDiff;
					f.format(format, info.mismatch ? "*" : " ", packageDiff.getName(), packageDiff.getType(),
						packageDiff.getDelta(), info.newerVersion,
						Objects.nonNull(info.olderVersion) && info.olderVersion.equals(Version.LOWEST) ? "-"
							: info.olderVersion,
						Objects.nonNull(info.suggestedVersion)
							&& info.suggestedVersion.compareTo(info.newerVersion) <= 0 ? "ok" : info.suggestedVersion,
						Objects.nonNull(info.suggestedIfProviders) ? info.suggestedIfProviders : "-");
					if (info.mismatch) {
						failure = true;
						f.format("%#2S%n", packageDiff);
					}
				}
			}
		}

		if (failure) {
			String msg = String.format("Baseline problems detected. See the report in %s.\n%s", report,
				IO.collect(report));
			if (isIgnoreFailures()) {
				getLogger().error(msg);
			} else {
				throw new GradleException(msg);
			}
		}
	}
}
