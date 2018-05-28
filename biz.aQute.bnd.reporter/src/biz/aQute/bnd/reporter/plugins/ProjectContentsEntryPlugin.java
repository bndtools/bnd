package biz.aQute.bnd.reporter.plugins;

import java.io.File;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import aQute.bnd.build.Project;
import aQute.bnd.build.ProjectBuilder;
import aQute.bnd.osgi.Builder;
import aQute.bnd.osgi.Jar;
import aQute.bnd.osgi.Processor;
import aQute.bnd.service.Plugin;
import aQute.bnd.service.reporter.ProjectEntryPlugin;
import aQute.service.reporter.Reporter;
import biz.aQute.bnd.reporter.generator.BundleReportGenerator;

public class ProjectContentsEntryPlugin implements ProjectEntryPlugin, Plugin {

	private final Set<String> excludes = new HashSet<>();

	@Override
	public Object extract(final Project project, final Processor reporter) throws Exception {
		Objects.requireNonNull(project, "project");
		Objects.requireNonNull(reporter, "reporter");

		final List<Map<String, Object>> bundlesReports = new LinkedList<>();

		try (final ProjectBuilder pb = project.getBuilder(null)) {
			final File[] jarFiles = project.getBuildFiles(false);
			final List<Builder> builders = pb.getSubBuilders();
			if (jarFiles != null) {
				for (final File jarFile : jarFiles) {
					try (final Jar jar = new Jar(jarFile)) {
						final Optional<Builder> opt = builders.stream().filter(b -> {
							try {
								return b.getBsn().equals(jar.getBsn());
							} catch (final Exception exception) {
								throw new RuntimeException(exception);
							}
						}).findAny();
						if (opt.isPresent()) {
							final Builder builder = opt.get();
							if (!excludes.contains(jar.getBsn())) {
								try (BundleReportGenerator gen = new BundleReportGenerator(jar, builder)) {
									bundlesReports.add(gen.getMetadata());
									gen.addClose(builder);
									reporter.getInfo(gen, gen.toString() + ": ");
								}
							} else {
								builder.close();
							}
						}
					}
				}
			}
		}
		return bundlesReports;
	}

	@Override
	public String getEntryName() {
		return "contents";
	}

	@Override
	public void setProperties(final Map<String, String> map) throws Exception {
		if (map.containsKey("excludes")) {
			for (final String exclude : map.get("excludes").split(",")) {
				excludes.add(exclude.trim());
			}
		}
	}

	@Override
	public void setReporter(final Reporter processor) {
		// Not used
	}
}
