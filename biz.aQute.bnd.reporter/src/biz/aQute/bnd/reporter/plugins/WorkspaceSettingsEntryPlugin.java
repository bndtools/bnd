package biz.aQute.bnd.reporter.plugins;

import aQute.bnd.build.Project;
import aQute.bnd.build.Workspace;
import aQute.bnd.osgi.Processor;
import aQute.bnd.service.RepositoryPlugin;
import aQute.bnd.service.reporter.WorkspaceEntryPlugin;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public class WorkspaceSettingsEntryPlugin implements WorkspaceEntryPlugin {

  @SuppressWarnings("unchecked")
  @Override
  public Object extract(final Workspace workspace, final Processor reporter) throws Exception {
    Objects.requireNonNull(workspace, "workspace");
    Objects.requireNonNull(reporter, "reporter");

    final Map<String, Object> settings = new LinkedHashMap<>();

    settings.put("folderName", workspace.getBase().getName());

    final Map<String, Object> table = new LinkedHashMap<>();
    workspace.report(table);
    settings.put("plugins", adaptPlugins((Collection<Object>) table.get("Plugins")));
    settings.put("repositories", adaptRepos((Collection<RepositoryPlugin>) table.get("Repos")));
    settings.put("projectsBuildOrder",
        adaptProjects((Collection<Project>) table.get("Projects in build order")));

    return settings;
  }

  @Override
  public String getEntryName() {
    return "settings";
  }

  private Object adaptRepos(final Collection<RepositoryPlugin> input) {
    if (input == null) {
      return null;
    }
    return input.stream().map(f -> f.getName()).collect(Collectors.toList());
  }

  private Object adaptPlugins(final Collection<Object> input) {
    if (input == null) {
      return null;
    }
    return input.stream().map(f -> f.getClass().getName()).collect(Collectors.toList());
  }

  private Object adaptProjects(final Collection<Project> input) {
    if (input == null) {
      return null;
    }
    return input.stream().map(f -> f.getName()).collect(Collectors.toList());
  }
}
