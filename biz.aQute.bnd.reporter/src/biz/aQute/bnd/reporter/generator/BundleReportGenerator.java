package biz.aQute.bnd.reporter.generator;

import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import aQute.bnd.osgi.Jar;
import aQute.bnd.osgi.Processor;
import aQute.bnd.osgi.Resource;
import aQute.bnd.service.reporter.BundleEntryPlugin;
import biz.aQute.bnd.reporter.plugins.ManifestEntryPlugin;

/**
 * Tool to create reports of bundles.
 * <p>
 * This tool used the {@link BundleEntryPlugin} plugins to generate the content
 * of the report.
 */
public final class BundleReportGenerator extends ReportGenerator {

	private final Jar _jar;

	public BundleReportGenerator(final Jar jar, final Processor processor) {
		super(Objects.requireNonNull(processor, "processor"));
		_jar = Objects.requireNonNull(jar, "jar");
		addDefaultPlugins();
	}

	private void addDefaultPlugins() {
		addBasicPlugin(new ManifestEntryPlugin());
	}

	@Override
	protected void extractMetadataFromPlugins(final Map<String, Object> metadata, final List<String> includes,
			final List<String> locales) {
		final List<BundleEntryPlugin> plugins = getPlugins(BundleEntryPlugin.class);
		for (final BundleEntryPlugin rp : plugins) {
			if (includes.contains(rp.getEntryName())) {
				try {
					final Processor r = new Processor();
					r.use(this);
					r.setBase(getBase());
					metadata.put(rp.getEntryName(), rp.extract(_jar, "", r));
					getInfo(r, rp.getEntryName() + " bundle entry: ");
				} catch (final Exception e) {
					exception(e, "failed to report bundle entry: %s", rp.getEntryName());
				}
			}
		}

		final Map<String, Object> localizations = new LinkedHashMap<>();
		for (final String locale : locales) {
			final Map<String, Object> localizedResult = new LinkedHashMap<>();
			for (final BundleEntryPlugin rp : plugins) {
				if (includes.contains(rp.getEntryName())) {
					try {
						final Processor r = new Processor();
						r.use(this);
						r.setBase(getBase());
						localizedResult.put(rp.getEntryName(), rp.extract(_jar, locale, this));
						getInfo(r, rp.getEntryName() + " bundle entry: ");
					} catch (final Exception e) {
						exception(e, "failed to report bundle entry: %s", rp.getEntryName());
					}
				}
			}
			if (!localizedResult.isEmpty()) {
				localizations.put(locale, localizedResult);
			}
		}
		if (!localizations.isEmpty()) {
			metadata.put("localization", localizations);
		}
	}

	@Override
	protected Set<String> getAllAvailableEntries() {
		return getPlugins(BundleEntryPlugin.class).stream().map(p -> p.getEntryName()).collect(Collectors.toSet());
	}

	@Override
	protected List<ReportGenerator> getSubGenerator() {
		return new LinkedList<>();
	}

	@Override
	protected InputStream resolveImportedFile(final String path) throws Exception {
		if (path.startsWith("@")) {
			final String pathInJar = path.substring(1);
			final Resource resource = _jar.getResource(pathInJar);
			if (resource != null) {
				return resource.openInputStream();
			} else {
				return null;
			}
		} else {
			return super.resolveImportedFile(path);
		}
	}

	@Override
	protected String getReporterTypeName() {
		return "bundle";
	}

	@Override
	public String toString() {
		return _jar.getName() + " bundle reporter";
	}
}
