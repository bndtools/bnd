package aQute.bnd.lsp.services;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import aQute.bnd.build.Project;
import aQute.bnd.build.Run;
import aQute.bnd.build.Workspace;
import aQute.bnd.exporter.executable.ExecutableJarExporter;
import aQute.bnd.exporter.runbundles.RunbundlesExporter;
import aQute.bnd.osgi.JarResource;
import aQute.bnd.osgi.Resource;
import aQute.lib.io.IO;

/**
 * Exports a .bndrun or bnd.bnd file the way the Bndtools export commands and
 * the {@code bnd export} CLI command do: an executable JAR, or the run bundles
 * as a folder. The output goes to {@code <target>/export} of the owning
 * project, or of the .bndrun when it belongs to no project.
 */
public class BndExportService {

	public record Request(File file, String exporter, boolean build) {}

	private final BndWorkspaceManager workspaceManager;

	public BndExportService(BndWorkspaceManager workspaceManager) {
		this.workspaceManager = workspaceManager;
	}

	public Map<String, Object> export(Request request) throws Exception {
		File file = request.file();
		if (file == null || !file.isFile()) {
			throw new IllegalArgumentException("Expected an existing .bndrun or bnd.bnd file: " + file);
		}
		String exporter = request.exporter() == null || request.exporter()
			.isBlank() ? ExecutableJarExporter.EXECUTABLE_JAR : request.exporter();

		Workspace ws = workspaceManager.getWorkspaceForFile(file);
		boolean bndrun = file.getName()
			.endsWith(".bndrun");
		Project owner = ws == null ? null : workspaceManager.getProjectForFile(file);

		Project exported;
		boolean close;
		if (bndrun) {
			Run run = owner != null ? new Run(ws, owner.getBase(), file) : Run.createRun(ws, file);
			if (ws == null && !run.isStandAlone()) {
				run.close();
				throw new IllegalStateException("Not in a bnd workspace: " + file);
			}
			exported = run;
			close = true;
		} else if (file.getName()
			.equals(Project.BNDFILE)) {
			if (owner == null) {
				throw new IllegalStateException("Could not find project for: " + file);
			}
			owner.refresh();
			owner.clear();
			exported = owner;
			close = false;
		} else {
			throw new IllegalArgumentException("Expected a .bndrun or bnd.bnd file: " + file);
		}

		try {
			if (request.build()) {
				BndLaunchService.build(exported, bndrun);
			}
			File exportDir = IO.getBasedFile((owner != null ? owner : exported).getTargetDir(), "export");
			File output = null;
			if (exported.isOk()) {
				Entry<String, Resource> export = exported.export(exporter, Collections.emptyMap());
				if (export != null) {
					output = write(export, exportDir, file, exporter);
				}
			}

			List<String> errors = new ArrayList<>(exported.getErrors());
			Map<String, Object> result = new LinkedHashMap<>();
			result.put("success", errors.isEmpty() && output != null);
			result.put("exporter", exporter);
			if (output != null) {
				result.put("output", IO.absolutePath(output));
			}
			result.put("errors", errors);
			result.put("warnings", new ArrayList<>(exported.getWarnings()));
			return result;
		} finally {
			if (close) {
				exported.close();
			}
		}
	}

	private static File write(Entry<String, Resource> export, File exportDir, File source, String exporter)
		throws Exception {
		try (Resource resource = export.getValue()) {
			// the run bundles are a folder of JARs, like the Bndtools command
			if (RunbundlesExporter.RUNBUNDLES.equals(exporter) && resource instanceof JarResource jar) {
				File folder = IO.getBasedFile(exportDir, source.getName());
				IO.delete(folder);
				jar.getJar()
					.writeFolder(folder);
				return folder;
			}
			File output = IO.getBasedFile(exportDir, export.getKey());
			IO.mkdirs(output.getParentFile());
			resource.write(output);
			output.setLastModified(resource.lastModified());
			return output;
		}
	}
}
