package biz.aQute.bnd.reporter.generator;

import java.io.File;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import aQute.bnd.build.Project;
import aQute.bnd.build.ProjectBuilder;
import aQute.bnd.osgi.Builder;
import aQute.bnd.osgi.Jar;
import aQute.bnd.osgi.Processor;
import aQute.bnd.service.reporter.ProjectEntryPlugin;
import biz.aQute.bnd.reporter.plugins.ProjectSettingsEntryPlugin;

/**
 * Tool to create reports of projects.
 * <p>
 * This tool used the {@link ProjectEntryPlugin} plugins to generate the content
 * of the report.
 */
public final class ProjectReportGenerator extends ReportGenerator {

	private final Project _project;

	public ProjectReportGenerator(final Project project) {
		super(Objects.requireNonNull(project, "project"));
		_project = project;
		addDefaultPlugins();
	}

	private void addDefaultPlugins() {
		addBasicPlugin(new ProjectSettingsEntryPlugin());
	}

	@Override
	protected void extractMetadataFromPlugins(final Map<String, Object> metadata, final List<String> includes,
			final List<String> locales) {
		final List<ProjectEntryPlugin> plugins = getPlugins(ProjectEntryPlugin.class);
		for (final ProjectEntryPlugin rp : plugins) {
			if (includes.contains(rp.getEntryName())) {
				try {
					final Processor r = new Processor();
					r.use(this);
					r.setBase(getBase());
					metadata.put(rp.getEntryName(), rp.extract(_project, r));
					getInfo(r, rp.getEntryName() + " project entry: ");
				} catch (final Exception e) {
					exception(e, "failed to report project entry: %s", rp.getEntryName());
				}
			}
		}
	}

	@Override
	protected Set<String> getAllAvailableEntries() {
		return getPlugins(ProjectEntryPlugin.class).stream().map(p -> p.getEntryName()).collect(Collectors.toSet());
	}

	@Override
	protected List<ReportGenerator> getSubGenerator() {
		final List<ReportGenerator> result = new LinkedList<>();

		try {
			final ProjectBuilder pb = _project.getBuilder(null);
			addClose(pb);

			final File[] jarFiles = _project.getBuildFiles(false);
			final List<Builder> builders = pb.getSubBuilders();
			if (jarFiles != null) {
				for (final File jarFile : jarFiles) {
					try {
						final Jar jar = new Jar(jarFile);
						final Optional<Builder> opt = builders.stream().filter(b -> {
							try {
								return b.getBsn().equals(jar.getBsn());
							} catch (final Exception e) {
								exception(e, "unable to get the bundle symbolic name of jar: %s", jar.getName());
								return false;
							}
						}).findAny();
						if (opt.isPresent()) {
							final BundleReportGenerator sub = new BundleReportGenerator(jar, opt.get());
							sub.addClose(jar);
							sub.addClose(opt.get());
							result.add(sub);
						} else {
							jar.close();
						}
					} catch (final IOException e1) {
						exception(e1, "failed to open the jar file at %s", jarFile.getAbsolutePath());
					}
				}
			}
		} catch (final Exception e2) {
			exception(e2, "failed to locate built jar of project %s", _project);
		}
		return result;
	}

	@Override
	protected String getReporterTypeName() {
		return "project";
	}

	@Override
	public String toString() {
		return _project + " project reporter";
	}
}
