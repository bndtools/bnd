package biz.aQute.bnd.reporter.generator;

import aQute.bnd.header.Attrs;
import aQute.bnd.header.OSGiHeader;
import aQute.bnd.header.Parameters;
import aQute.bnd.osgi.Processor;
import aQute.bnd.service.reporter.ReportImportDeserializerPlugin;
import aQute.bnd.service.reporter.ReportSerializerPlugin;
import aQute.bnd.service.reporter.ReportTransformerPlugin;
import aQute.lib.io.IO;
import aQute.libg.glob.Glob;
import aQute.libg.slf4j.GradleLogging;
import biz.aQute.bnd.reporter.helpers.ArrayHelper;
import biz.aQute.bnd.reporter.helpers.FileHelper;
import biz.aQute.bnd.reporter.plugins.JsonImportDeserializerPlugin;
import biz.aQute.bnd.reporter.plugins.JsonReportSerializerPlugin;
import biz.aQute.bnd.reporter.plugins.ManifestImportDeserializerPlugin;
import biz.aQute.bnd.reporter.plugins.PropertiesImportDeserializerPlugin;
import biz.aQute.bnd.reporter.plugins.XmlImportDeserializerPlugin;
import biz.aQute.bnd.reporter.plugins.XmlReportSerializerPlugin;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Base class of reporter.
 */
abstract class ReportGenerator extends Processor {

  private final static Logger logger = LoggerFactory.getLogger(ReportGenerator.class);

  public static final String IMPORTS_PROPERTY = "imports";
  public static final String LOCALES_PROPERTY = "locales";
  public static final String INCLUDES_PROPERTY = "includes";
  public static final String EXCLUDES_PROPERTY = "excludes";

  public static final String TEMPLATE_DIRECTIVE = "template:";

  private Map<String, Object> _metadataCache = null;
  private final Map<String, byte[]> _serializationCache = new HashMap<>();
  private final Map<Parameters, ReportGenerator> _configCache = new LinkedHashMap<>();

  protected ReportGenerator(final Processor parent) {
    super(parent);
    use(parent);
    setBase(parent.getBase());
    addDefaultPlugins();
  }

  private void addDefaultPlugins() {
    addBasicPlugin(new JsonReportSerializerPlugin());
    addBasicPlugin(new XmlReportSerializerPlugin());
    addBasicPlugin(new JsonImportDeserializerPlugin());
    addBasicPlugin(new XmlImportDeserializerPlugin());
    addBasicPlugin(new ManifestImportDeserializerPlugin());
    addBasicPlugin(new PropertiesImportDeserializerPlugin());
  }

  /**
   * Get the extracted metadata.
   *
   * @return a {@code Map} of all the metadata entries, never {@code null}
   */
  final public Map<String, Object> getMetadata() {
    if (_metadataCache != null) {
      return _metadataCache;
    } else {
      _metadataCache = new LinkedHashMap<>();
      extractMetadata(_metadataCache);
      return _metadataCache;
    }
  }

  private final void extractMetadata(final Map<String, Object> metadata) {
    final MetadataExtractionOption option = getMetadataExtractionOption();

    extractMetadataFromPlugins(metadata, option.includes, option.locales);
    extractMetadataFromArbritraryProperties(metadata, option.arbitraryProperties);
    extractMetadataFromImportedFiles(metadata, option.importPaths, option.importParents,
        option.importExtensions);
  }

  abstract protected String getReporterTypeName();

  abstract protected void extractMetadataFromPlugins(Map<String, Object> metadata,
      List<String> includes, List<String> locales);

  private void extractMetadataFromArbritraryProperties(final Map<String, Object> metadata,
      final Map<String, String> arbitraryProperties) {
    metadata.putAll(arbitraryProperties);
  }

  private void extractMetadataFromImportedFiles(final Map<String, Object> metadata,
      final List<String> importPaths, final Map<String, String> importParents,
      final Map<String, String> importExtensions) {
    for (final String importPath : importPaths) {
      try (InputStream is = resolveImportedFile(importPath)) {
        if (is != null) {
          final File importedFile = new File(importPath);
          final String parentName =
              importParents.get(importPath) != null ? importParents.get(importPath)
                  : FileHelper.getName(importedFile);
          final String extension =
              importExtensions.get(importPath) != null ? importExtensions.get(importPath)
                  : FileHelper.getExtension(importedFile);
          try {
            metadata.put(parentName, convertFile(is, extension));
          } catch (final Exception e) {
            exception(e, "failed to deserialize the imported file at %s", importPath);
          }
        }
      } catch (final Exception e) {
        exception(e, "failed to import the file at %s", importPath);
      }
    }
  }

  @SuppressWarnings("unused")
  protected InputStream resolveImportedFile(final String path) throws Exception {
    try {
      return new FileInputStream(getFile(path));
    } catch (final FileNotFoundException ignored) {
      // imported files are optional
      return null;
    }
  }

  private Object convertFile(final InputStream inputStream, final String extension)
      throws Exception {
    for (final ReportImportDeserializerPlugin deserializer : getPlugins(
        ReportImportDeserializerPlugin.class)) {
      if (ArrayHelper.containsIgnoreCase(deserializer.getHandledExtensions(), extension)) {
        return deserializer.deserialyze(inputStream);
      }
    }
    throw new UnsupportedOperationException(
        "unable to deserialize the imported file with the extension " + extension
            + ", available extension are: " + getAllDeserializerExtensions());
  }

  private final MetadataExtractionOption getMetadataExtractionOption() {
    final MetadataExtractionOption option = new MetadataExtractionOption();
    final String param = getProperty("-report.model." + getReporterTypeName());
    if (param != null) {
      final Attrs parameter = OSGiHeader.parseProperties(param, this);
      fillImports(parameter, option);
      fillLocales(parameter, option);
      fillArbitraryAttrs(parameter, option);
      fillIncludes(parameter, option);
    } else {
      option.includes.addAll(getAllAvailableEntries());
    }

    return option;
  }

  private void fillImports(final Attrs parameter, final MetadataExtractionOption option) {
    if (parameter.containsKey(IMPORTS_PROPERTY)) {
      for (final String importPath : parameter.get(IMPORTS_PROPERTY).split(",")) {
        final String[] parts = importPath.split(":");
        for (int i = 0; i < parts.length; i++) {
          parts[i] = parts[i].trim();
        }
        if (parts.length >= 1 && !parts[0].isEmpty()) {
          option.importPaths.add(parts[0]);
        }
        if (parts.length >= 2 && !parts[1].isEmpty()) {
          option.importParents.put(parts[0], parts[1]);
        }
        if (parts.length >= 3 && !parts[2].isEmpty()) {
          option.importExtensions.put(parts[0], parts[2]);
        }
      }
    }
  }

  private void fillLocales(final Attrs parameter, final MetadataExtractionOption option) {
    if (parameter.containsKey(LOCALES_PROPERTY)) {
      for (final String locale : parameter.get(LOCALES_PROPERTY).split(",")) {
        option.locales.add(locale.trim());
      }
    }
  }

  private void fillArbitraryAttrs(final Attrs parameter, final MetadataExtractionOption option) {
    for (final Entry<String, String> property : parameter.entrySet()) {
      if (property.getKey().startsWith("@")) {
        option.arbitraryProperties.put(property.getKey().substring(1), property.getValue());
      }
    }
  }

  private void fillIncludes(final Attrs parameter, final MetadataExtractionOption option) {
    final List<String> includes = new LinkedList<>();
    final List<String> excludes = new LinkedList<>();

    if (parameter.containsKey(INCLUDES_PROPERTY)) {
      for (final String include : parameter.get(INCLUDES_PROPERTY).split(",")) {
        includes.add(include);
      }
    }
    if (parameter.containsKey(EXCLUDES_PROPERTY)) {
      for (final String exclude : parameter.get(EXCLUDES_PROPERTY).split(",")) {
        excludes.add(exclude);
      }
    }

    if (includes.isEmpty()) {
      includes.addAll(getAllAvailableEntries());
    }

    includes.removeAll(excludes);
    option.includes.addAll(includes);
  }

  abstract protected Set<String> getAllAvailableEntries();

  /**
   * Get the set of absolute report pathnames which can be generated from this report generator.
   *
   * @return a {@code Set} of absolute pathnames, never {@code null}
   */
  final public Set<String> getAvailableReports() {
    final Set<String> reportPaths = new HashSet<>();
    for (final Entry<Parameters, ReportGenerator> entry : getReportConfigs().entrySet()) {
      for (final String path : entry.getKey().keySet()) {
        reportPaths.add(
            FileHelper.getCanonicalPath(entry.getValue().getFile(removeDuplicateMarker(path))));
      }
    }
    return reportPaths;
  }

  /**
   * Generate the reports whose absolute pathnames match the specified Glob expression.
   *
   * @param glob a glob expression which must match the desired reports, must not be {@code null}
   * @return a set of the generated report files, never {@code null}.
   */
  final public Set<File> generateReports(final String glob) {
    Objects.requireNonNull(glob, "glob");

    final Set<File> generated = new HashSet<>();

    for (final Entry<Parameters, ReportGenerator> configs : getReportConfigs().entrySet()) {
      final Parameters parameters = configs.getKey();
      final ReportGenerator generator = configs.getValue();
      for (final Entry<String, Attrs> entry : parameters.entrySet()) {
        final Attrs attributes = entry.getValue();
        final File outputFile = generator.getFile(removeDuplicateMarker(entry.getKey()));
        final String outputPath = FileHelper.getCanonicalPath(outputFile);
        final Pattern pattern = Glob.toPattern(glob);
        if (pattern.matcher(outputPath).matches()) {
          logger.info(GradleLogging.LIFECYCLE, "Generate report {}", outputPath);
          if (generator.checkOutputFile(outputFile)) {
            File template = FileHelper.searchSiblingWithDifferentExtension(outputFile,
                generator.getAllTransformerExtensions());
            if (template == null) {
              final String templatePath = attributes.get(TEMPLATE_DIRECTIVE);
              if (templatePath != null && !templatePath.isEmpty()) {
                template = generator.getFile(templatePath);
                if (generator.checkTemplateFile(template)) {
                  generator.transform(template, outputFile, generated,
                      getTemplateParameters(attributes));
                }
              } else {
                try (InputStream is = generator.getMetadata(FileHelper.getExtension(outputFile))) {
                  IO.copy(is, outputFile);
                  generated.add(outputFile);
                } catch (final Exception e) {
                  generator.exception(e, "failed to write the report to %s", outputPath);
                }
              }
            } else {
              generator.transform(template, outputFile, generated,
                  getTemplateParameters(attributes));
            }
          }
        }
        if (!generator.equals(this)) {
          getInfo(generator, generator.toString() + ": ");
        }
      }
    }
    return generated;
  }

  private void transform(final File templateFile, final File outputFile, final Set<File> generated,
      final Map<String, String> parameters) {
    final String extension = FileHelper.getExtension(templateFile);
    final String[] serializerExtensions = getAllSerializerExtensions();

    for (final ReportTransformerPlugin transformer : getPlugins(ReportTransformerPlugin.class)) {
      if (ArrayHelper.containsIgnoreCase(transformer.getHandledTemplateExtensions(), extension)) {
        final String inputExt =
            ArrayHelper.oneInBoth(transformer.getHandledModelExtensions(), serializerExtensions);
        if (inputExt != null) {
          try (InputStream isT = new FileInputStream(templateFile)) {
            try (InputStream is = getMetadata(inputExt);
                OutputStream os = new FileOutputStream(outputFile)) {
              try {
                transformer.transform(is, isT, os, parameters);
                generated.add(outputFile);
              } catch (final Exception e) {
                exception(e, "failed to transform the report at %s",
                    FileHelper.getCanonicalPath(outputFile));
              }
            } catch (final Exception e) {
              exception(e, "failed to write the report to %s",
                  FileHelper.getCanonicalPath(outputFile));
            }
          } catch (final Exception e1) {
            exception(e1, "failed to read to the template file at %s",
                FileHelper.getCanonicalPath(templateFile));
          }
          return;
        }
      }
    }
    error(
        "unable to find a template processor for the report %s, available template extensions are: %s",
        outputFile, getAllTransformerExtensions());
  }

  final private InputStream getMetadata(final String extension) {
    if (_serializationCache.containsKey(extension)) {
      return new ByteArrayInputStream(_serializationCache.get(extension));
    } else {
      if (_metadataCache != null) {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();

        for (final ReportSerializerPlugin serializer : getPlugins(ReportSerializerPlugin.class)) {
          if (ArrayHelper.containsIgnoreCase(serializer.getHandledExtensions(), extension)) {
            try {
              serializer.serialize(_metadataCache, bos);
              _serializationCache.put(extension, bos.toByteArray());
            } catch (final Exception e) {
              _serializationCache.put(extension, new byte[0]);
              exception(e, "failed to serialize the report to %s", extension);
            }
            return getMetadata(extension);
          }
        }
        error("unable to serialize the metadata to %s, available extension are: %s", extension,
            getAllSerializerExtensions());
        return new ByteArrayInputStream(new byte[0]);
      } else {
        getMetadata();
        return getMetadata(extension);
      }
    }
  }

  private Parameters getReportConfig() {
    final Parameters parameters = new Parameters();

    final String param = mergeProperties("-report." + getReporterTypeName());
    if (param != null && !param.trim().equalsIgnoreCase("false")) {
      for (final Entry<String, Attrs> entry : OSGiHeader.parseHeader(param, this).entrySet()) {
        parameters.add(entry.getKey(), entry.getValue());
      }
    }
    return parameters;
  }

  private Map<Parameters, ReportGenerator> getReportConfigs() {
    if (_configCache.isEmpty()) {
      _configCache.put(getReportConfig(), this);
      for (final ReportGenerator subGenerator : getSubGenerator()) {
        addClose(subGenerator);
        _configCache.putAll(subGenerator.getReportConfigs());
        getInfo(subGenerator, subGenerator.toString() + ": ");
      }
    }
    return _configCache;
  }

  abstract protected List<ReportGenerator> getSubGenerator();

  private static Map<String, String> getTemplateParameters(final Attrs value) {
    final Map<String, String> params = new HashMap<>();
    for (final Entry<String, String> a : value.entrySet()) {
      if (!a.getKey().endsWith(":")) {
        params.put(a.getKey(), a.getValue());
      }
    }
    return params;
  }

  private String[] getAllSerializerExtensions() {
    return getPlugins(ReportSerializerPlugin.class).stream()
        .flatMap(r -> Arrays.stream(r.getHandledExtensions())).collect(Collectors.toList())
        .toArray(new String[0]);
  }

  private String[] getAllDeserializerExtensions() {
    return getPlugins(ReportSerializerPlugin.class).stream()
        .flatMap(r -> Arrays.stream(r.getHandledExtensions())).collect(Collectors.toList())
        .toArray(new String[0]);
  }

  private String[] getAllTransformerExtensions() {
    return getPlugins(ReportTransformerPlugin.class).stream()
        .flatMap(r -> Arrays.stream(r.getHandledTemplateExtensions())).collect(Collectors.toList())
        .toArray(new String[0]);
  }

  private boolean checkOutputFile(final File outputFile) {
    if (outputFile.isDirectory()) {
      error("the output must be a file: %s", outputFile.getPath());
      return false;
    }

    final File parent = outputFile.getParentFile();
    if (!parent.exists()) {
      error("the parent directory of the output file does not exist: %s", outputFile.getPath());
      return false;
    }
    return true;
  }

  private boolean checkTemplateFile(final File template) {
    if (!template.isFile()) {
      error("the template file does not exist: %s", template.getPath());
      return false;
    }
    return true;
  }

  @Override
  public boolean refresh() {
    _metadataCache = null;
    _serializationCache.clear();
    _configCache.values().forEach(r -> {
      try {
        if (!r.equals(this)) {
          r.close();
        }
      } catch (final IOException exception) {
        logger.error("failed to close the report generator", exception);
      }
    });
    _configCache.clear();

    return super.refresh();
  }

  @Override
  abstract public String toString();

  static class MetadataExtractionOption {

    public List<String> locales = new LinkedList<>();
    public List<String> includes = new LinkedList<>();
    public List<String> importPaths = new LinkedList<>();
    public Map<String, String> importExtensions = new HashMap<>();
    public Map<String, String> importParents = new HashMap<>();
    public Map<String, String> arbitraryProperties = new HashMap<>();
  }
}
