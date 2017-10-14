package biz.aQute.bnd.reporter.generator;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

import aQute.bnd.osgi.Jar;
import aQute.bnd.osgi.Processor;
import aQute.bnd.osgi.Resource;
import aQute.bnd.service.reporter.ReportGeneratorPlugin;
import aQute.bnd.service.reporter.XmlReportPart;
import aQute.lib.io.IOConstants;
import aQute.lib.tag.Tag;
import aQute.libg.xslt.Transform;

/**
 * Tools to create reports of jars.
 * <p>
 * This tools used the {@link ReportGeneratorPlugin} plugins to generate the
 * content of the report.
 *
 */
public class ReportGenerator extends Processor {

	public ReportGenerator(final Processor processor) {
		super(processor);
		use(this);
	}

	/**
	 * Generates a report based on the specified configuration.
	 *
	 * @param config
	 *            the configuration for the report, must not be {@code null}
	 * @return true if the report was successfully generated
	 */
	public boolean generate(final ReportConfig config) {
		Objects.requireNonNull(config, "config");

		if (config.getInputJar() != null) {
			return generate(config.getInputJar(), config);
		} else {
			return generate(config.getInputPath(), config);
		}
	}

	private boolean generate(final String jarPath, final ReportConfig config) {
		boolean closed = false;
		final Jar inputJar = getJar(jarPath);
		if (inputJar != null) {
			try {
				if (generate(inputJar, config) && config.getOutputPath() != null) {
					try {
						final File tmp = File.createTempFile("tmp", ".jar");
						tmp.deleteOnExit();

						inputJar.write(tmp);
						inputJar.close();

						closed = true;

						Files.copy(tmp.toPath(), Paths.get(config.getInputPath()), StandardCopyOption.REPLACE_EXISTING);

						return true;
					} catch (final Exception e) {
						error("report generator: failed to save the modified jar at " + config.getInputPath(), e);
						return false;
					}
				} else {
					return false;
				}
			} finally {
				if (!closed) {
					inputJar.close();
				}
			}
		} else {
			return false;
		}
	}

	private boolean generate(final Jar inputJar, final ReportConfig config) {
		final List<XmlReportPart> jarReport = createReport(inputJar, config);
		if (jarReport != null) {
			return generate(jarReport, config, inputJar);
		} else {
			return false;
		}
	}

	private boolean generate(final List<XmlReportPart> source, final ReportConfig config, final Jar jar) {
		try (ReportResource r = new ReportResource(source); InputStream input = r.openInputStream()) {
			return generate(input, config, jar);
		} catch (final Exception e) {
			throw new RuntimeException("report generator: failed to read the generated report", e);
		}
	}

	private boolean generate(final InputStream input, final ReportConfig config, final Jar jar) {
		if (config.getOutputPath() == null) {
			return applyTemplates(input, config.getOutputStream(), config.getTemplatePaths());
		} else {
			final ByteArrayOutputStream output = new ByteArrayOutputStream();
			if (applyTemplates(input, output, config.getTemplatePaths())) {
				jar.putResource(config.getOutputPath(), new ByteResource(output), true);
				return true;
			} else {
				return false;
			}
		}
	}

	private Jar getJar(final String jarPath) {
		final File jarFile = getFile(jarPath);
		if (jarFile.isFile()) {
			try {
				return new Jar(jarFile);
			} catch (final Exception e) {
				error("report generator: failed to read the input file at: " + jarPath, e);
			}
		} else {
			error("report generator: failed to read the input file at: " + jarPath + ". It must be a Jar zip");
		}
		return null;
	}

	private List<XmlReportPart> createReport(final Jar jar, final ReportConfig config) {
		final List<XmlReportPart> parts = new LinkedList<>();
		try {
			for (final ReportGeneratorPlugin rp : getParent().getPlugins(ReportGeneratorPlugin.class)) {
				parts.add(rp.report(jar, config.getLocale(), this));
			}

			for (final String includePath : config.getIncludePaths()) {
				final ReportResult r = new ReportResult();

				if (includePath.startsWith("@")) {
					final String pathInJar = includePath.substring(1);
					final Resource resource = jar.getResource(pathInJar);
					if (resource != null) {
						try (InputStream is = resource.openInputStream()) {
							r.addAll(convertFile(is, pathInJar, config.getIncludeType(includePath),
									config.getIncludeParent(includePath)));
						} catch (final Exception e) {
							throw new Exception("unable to include the file " + includePath, e);
						}
					} 
				} else {
					try (InputStream is = new FileInputStream(getFile(includePath))) {
						r.addAll(convertFile(is, includePath, config.getIncludeType(includePath),
								config.getIncludeParent(includePath)));
					} catch (final FileNotFoundException expected) {
						// included files are optional
					}
				}

				parts.add(r);
			}
			return parts;
		} catch (final Exception e) {
			error("report generator: failed to generate the report of " + jar.getName(), e);
		}

		return null;
	}

	private List<Tag> convertFile(final InputStream inputStream, final String path, final String type,
			final String parent) throws Exception {
		List<Tag> result = null;
		final String effectivePath = path.toLowerCase();
		String effectiveType = null;
		String effectiveParent = null;

		if (type != null) {
			effectiveType = type;
		} else {
			if (effectivePath.endsWith(".xml")) {
				effectiveType = "xml";
			} else if (effectivePath.endsWith(".json")) {
				effectiveType = "json";
			} else if (effectivePath.endsWith(".properties")) {
				effectiveType = "properties";
			} else if (effectivePath.endsWith(".mf")) {
				effectiveType = "mf";
			} else {
				effectiveType = "unknow";
			}
		}

		if (parent != null) {
			effectiveParent = parent;
		} else {
			if (!effectiveType.equals("xml")) {
				effectiveParent = effectivePath;

				final int indexSlash = effectivePath.lastIndexOf("/");
				if (indexSlash != -1 && indexSlash < effectiveParent.length() - 1) {
					effectiveParent = effectiveParent.substring(indexSlash + 1);
				}

				final int indexDot = effectiveParent.lastIndexOf(".");
				if (indexDot != -1) {
					effectiveParent = effectiveParent.substring(0, indexDot);
				}
			}
		}

		if (effectiveType.equals("xml")) {
			if (effectiveParent != null) {
				result = ReportConverter.fromXml(inputStream, effectiveParent);
			} else {
				result = ReportConverter.fromXml(inputStream);
			}
		} else if (effectiveType.equals("json")) {
			result = ReportConverter.fromJson(inputStream, effectiveParent);
		} else if (effectiveType.equals("properties")) {
			result = ReportConverter.fromProperties(inputStream, effectiveParent);
		} else if (effectiveType.equals("mf")) {
			result = ReportConverter.fromManifest(inputStream, effectiveParent);
		} else {
			throw new UnsupportedOperationException("unsupported file type " + effectiveType);
		}

		return result;
	}

	private boolean applyTemplates(final InputStream input, final OutputStream output, final List<String> templates) {
		int i = 0;
		try {
			if (templates.size() == 0) {
				copy(input, output);
			} else {
				InputStream in = input;
				URL xslt;
				ByteArrayOutputStream out;

				for (i = 0; i < templates.size() - 1; i++) {
					out = new ByteArrayOutputStream();
					xslt = getFile(templates.get(i)).toURI().toURL();

					Transform.transform(xslt, in, out);
					in = new ByteArrayInputStream(out.toByteArray());
				}
				xslt = getFile(templates.get(i)).toURI().toURL();
				Transform.transform(xslt, in, output);
			}
			return true;
		} catch (final MalformedURLException e) {
			error("report generator: failed to locate the template at " + templates.get(i), e);
		} catch (final IOException e) {
			error("report generator: failed to write the report", e);
		} catch (final Exception e) {
			error("report generator: failed to transform the report with the template at " + templates.get(i), e);
		}
		return false;
	}

	private static void copy(final InputStream in, final OutputStream out) throws IOException {
		final byte[] buffer = new byte[IOConstants.PAGE_SIZE];
		while (true) {
			final int bytesRead = in.read(buffer);
			if (bytesRead == -1) {
				break;
			}
			out.write(buffer, 0, bytesRead);
		}
		out.flush();
	}
}
