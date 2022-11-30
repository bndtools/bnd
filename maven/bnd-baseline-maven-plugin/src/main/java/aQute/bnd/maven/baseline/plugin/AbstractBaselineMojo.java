package aQute.bnd.maven.baseline.plugin;

import static java.util.stream.Collectors.toList;

import java.io.File;
import java.io.IOException;
import java.util.Comparator;
import java.util.Formatter;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import aQute.bnd.differ.Baseline;
import aQute.bnd.differ.Baseline.BundleInfo;
import aQute.bnd.differ.Baseline.Info;
import aQute.bnd.differ.DiffPluginImpl;
import aQute.bnd.header.Parameters;
import aQute.bnd.osgi.Instructions;
import aQute.bnd.osgi.Jar;
import aQute.bnd.osgi.Processor;
import aQute.bnd.service.diff.Diff;
import aQute.lib.io.IO;
import aQute.lib.strings.Strings;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Parameter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractBaselineMojo extends AbstractMojo {

	protected static final Logger	logger	= LoggerFactory.getLogger(BaselineMojo.class);

	@Parameter(property = "bnd.baseline.full.report", defaultValue = "false")
	protected boolean				fullReport;

	@Parameter(required = false, defaultValue = "*", property = "bnd.baseline.diffpackages")
	protected List<String>			diffpackages;

	@Parameter(required = false, property = "bnd.baseline.diffignores")
	protected List<String>	diffignores;

	@Parameter(property = "bnd.baseline.report.file", defaultValue = "${project.build.directory}/baseline/${project.build.finalName}.txt")
	protected File reportFile;

	@Parameter(property = "bnd.baseline.continue.on.error", defaultValue = "false")
	protected boolean				continueOnError;

	protected void baselineAction(File bundle, File baseline) throws Exception, IOException {
		try (Jar newer = new Jar(bundle); Jar older = new Jar(baseline)) {
			baselineAction(newer, older);
		}
	}

	protected void baselineAction(Jar newer, Jar older) throws Exception, IOException {
		IO.mkdirs(reportFile.getParentFile());
		boolean failure = false;
		try (Processor processor = new Processor()) {
			logger.info("Baseline bundle {} against baseline {}", newer.getName(), older.getName());
			DiffPluginImpl differ = new DiffPluginImpl();
			differ.setIgnore(new Parameters(Strings.join(diffignores), processor));
			Baseline baseliner = new Baseline(processor, differ);
			List<Info> infos = baseliner
				.baseline(newer, older, new Instructions(new Parameters(Strings.join(diffpackages), processor)))
				.stream()
				.sorted(Comparator.comparing(info -> info.packageName))
				.collect(toList());
			BundleInfo bundleInfo = baseliner.getBundleInfo();
			try (Formatter f = new Formatter(reportFile, "UTF-8", Locale.US)) {
				String format = "%s %-50s %-10s %-10s %-10s %-10s %-10s %s\n";
				f.format("===============================================================\n");
				f.format(format, " ", "Name", "Type", "Delta", "New", "Old", "Suggest", "");
				Diff diff = baseliner.getDiff();
				f.format(format, bundleInfo.mismatch ? "*" : " ", bundleInfo.bsn, diff.getType(), diff.getDelta(),
					newer.getVersion(), older.getVersion(),
					bundleInfo.mismatch && Objects.nonNull(bundleInfo.suggestedVersion) ? bundleInfo.suggestedVersion
						: "-",
					"");
				if (fullReport || bundleInfo.mismatch) {
					f.format("%#2S\n", diff);
				}
				if (bundleInfo.mismatch) {
					failure = true;
				}

				if (!infos.isEmpty()) {
					f.format("===============================================================\n");
					f.format(format, " ", "Name", "Type", "Delta", "New", "Old", "Suggest", "If Prov.");
					for (Info info : infos) {
						diff = info.packageDiff;
						f.format(format, info.mismatch ? "*" : " ", diff.getName(), diff.getType(), diff.getDelta(),
							info.newerVersion,
							Objects.nonNull(info.olderVersion)
								&& info.olderVersion.equals(aQute.bnd.version.Version.LOWEST) ? "-" : info.olderVersion,
							Objects.nonNull(info.suggestedVersion)
								&& info.suggestedVersion.compareTo(info.newerVersion) <= 0 ? "ok"
									: info.suggestedVersion,
							Objects.nonNull(info.suggestedIfProviders) ? info.suggestedIfProviders : "-");
						if (fullReport || info.mismatch) {
							f.format("%#2S\n", diff);
						}
						if (info.mismatch) {
							failure = true;
						}
					}
				}
			}
		}

		if (failure) {
			String msg = String.format("Baseline problems detected. See the report in %s.\n%s", reportFile,
				IO.collect(reportFile));
			if (continueOnError) {
				logger.warn(msg);
			} else {
				throw new MojoFailureException(msg);
			}
		} else {
			logger.info("Baseline check succeeded. See the report in {}.", reportFile);
		}
	}
}
