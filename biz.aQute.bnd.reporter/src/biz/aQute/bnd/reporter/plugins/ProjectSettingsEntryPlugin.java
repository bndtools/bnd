package biz.aQute.bnd.reporter.plugins;

import aQute.bnd.build.Container;
import aQute.bnd.build.Project;
import aQute.bnd.osgi.Processor;
import aQute.bnd.service.action.Action;
import aQute.bnd.service.reporter.ProjectEntryPlugin;
import com.google.common.collect.ImmutableMap;
import java.io.File;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public class ProjectSettingsEntryPlugin implements ProjectEntryPlugin {

  @SuppressWarnings("unchecked")
  @Override
  public Object extract(final Project project, final Processor reporter) throws Exception {
    Objects.requireNonNull(project, "project");
    Objects.requireNonNull(reporter, "reporter");

    final Map<String, Object> settings = new LinkedHashMap<>();

    settings.put("folderName", project.getBase().getName());

    final Map<String, Object> table = new LinkedHashMap<>();
    project.report(table);
    settings.put("target", adapt((File) table.get("Target")));
    settings.put("source", adapt((File) table.get("Source")));
    settings.put("output", adapt((File) table.get("Output")));
    settings.put("buildFiles", adaptFiles((List<File>) table.get("BuildFiles")));
    settings.put("classpath", adapt((Collection<Container>) table.get("Classpath")));
    settings.put("actions", adapt((Map<String, Action>) table.get("Actions")));
    settings.put("allSourcePath", adaptFiles((Collection<File>) table.get("AllSourcePath")));
    settings.put("bootClassPath", adapt((Collection<Container>) table.get("BootClassPath")));
    settings.put("buildPath", adapt((Collection<Container>) table.get("BuildPath")));
    settings.put("deliverables", adapt((Collection<Container>) table.get("Deliverables")));
    settings.put("dependsOn", adaptProjects((Collection<Project>) table.get("DependsOn")));
    settings.put("sourcePath", adaptFiles((Collection<File>) table.get("SourcePath")));
    settings.put("runPath", adapt((Collection<Container>) table.get("RunPath")));
    settings.put("testPath", adapt((Collection<Container>) table.get("TestPath")));
    settings.put("runProgramArgs", table.get("RunProgramArgs"));
    settings.put("runVM", table.get("RunVM"));
    settings.put("runfw", adapt((Collection<Container>) table.get("Runfw")));
    settings.put("runbundles", adapt((Collection<Container>) table.get("Runbundles")));

    return settings;
  }

  @Override
  public String getEntryName() {
    return "settings";
  }

  private Object adapt(final File input) {
    if (input == null) {
      return null;
    }
    return input.getAbsolutePath();
  }

  private Object adaptFiles(final Collection<File> input) {
    if (input == null) {
      return null;
    }
    return input.stream().map(f -> f.getAbsolutePath()).collect(Collectors.toList());
  }

  private Object adapt(final Collection<Container> input) {
    if (input == null) {
      return null;
    }
    return input.stream().map(f -> ImmutableMap.of("bundleSymbolicName", f.getBundleSymbolicName(),
        "version", f.getVersion())).collect(Collectors.toList());
  }

  private Object adapt(final Map<String, Action> input) {
    if (input == null) {
      return null;
    }
    return input.keySet();
  }

  private Object adaptProjects(final Collection<Project> input) {
    if (input == null) {
      return null;
    }
    return input.stream().map(f -> f.getName()).collect(Collectors.toList());
  }
}
