package aQute.bnd.build;

import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.osgi.resource.Capability;

import aQute.bnd.build.Workspace.ResourceRepositoryStrategy;
import aQute.bnd.header.Attrs;
import aQute.bnd.header.Parameters;
import aQute.bnd.osgi.Constants;
import aQute.bnd.osgi.Jar;
import aQute.bnd.osgi.Processor;
import aQute.bnd.result.Result;
import aQute.bnd.version.Version;
import aQute.bnd.workspace.extension.WorkspaceExtensionNamespace;
import aQute.lib.io.IO;

public class WorkspaceExtensionHandler {

	private Set<Capability>			availableWorkspaceExtensions	= new HashSet<>();
	private Map<String, File>	enabledWorkspaceExtensions		= new HashMap<>();
	private Workspace				ws;
	private Processor				processor;

	public WorkspaceExtensionHandler(Workspace ws) {
		this.ws = ws;
		processor = ws;
	}

	public WorkspaceExtensionHandler(Project project) {
		ws = project.getWorkspace();
		processor = project;
		setKnownExtensions(ws);
	}

	void setKnownExtensions(Workspace ws) {
		enabledWorkspaceExtensions.putAll(ws.getEnabledWorkspaceExtensions());
	}

	void clean() {
		availableWorkspaceExtensions.clear();
		enabledWorkspaceExtensions.clear();
	}

	public void doWorkspaceExtensions() {
		if (!(processor instanceof Workspace)) {
			setKnownExtensions(ws);
		}
		// if we have done something we need to tell the processor, that the
		// properties have changed
		if (doWorkspaceExtensionsRound())
			processor.propertiesChanged();
	}

	private boolean doWorkspaceExtensionsRound() {
		String extensions = processor.mergeProperties(Constants.WORKSPACE_EXTENSIONS);
		boolean doneSomething = false;
		if (extensions != null) {
			extensions = processor.getReplacer()
				.process(extensions);
			Parameters extensionsParameters = new Parameters(extensions, processor);
			for (Entry<String, Attrs> entry : extensionsParameters.entrySet()) {
				String extensionName = Processor.removeDuplicateMarker(entry.getKey());
				String versionString = entry.getValue()
					.get(WorkspaceExtensionNamespace.CAPABILITY_VERSION_ATTRIBUTE);

				String extensionIdentifyer = extensionName + "-" + versionString;

				if (enabledWorkspaceExtensions.containsKey(extensionIdentifyer)) {
					continue;
				}

				//the first time we want to apply a new Extension, we will need to look for the available ones
				if (!doneSomething) {
					try {
						updateAvailableWorkspaceExtension();
					} catch (Exception e) {
						processor.getLogger()
						.error("Could not update avaialbe WorkspaceExtensions", e);
						return false;
					}
					doneSomething = true;
				}

				try {

					processor.getLogger()
					.info("Adding workspace extension {} in version {}", extensionName, versionString);

					Result<Capability> capabilityResult = WorkspaceExtensionNamespace
						.findWorkspaceExtension(extensionName, versionString, availableWorkspaceExtensions);

					if (capabilityResult.isErr()) {
						processor.error(capabilityResult.error()
							.get());
						continue;
					}
					Capability cap = capabilityResult.unwrap();

					org.osgi.resource.Resource resource = cap.getResource();
					String include = (String) cap.getAttributes()
						.get(WorkspaceExtensionNamespace.CAPABILITY_INCLUDE_ATTRIBUTE);

					if (include == null) {
						processor.error(
							"Can't load workspace extension %s with version %s, because the where directive is missing.",
							extensionName, versionString);
						continue;
					}

					Result<File> result = ws.getBundle(resource, ResourceRepositoryStrategy.REPOS);
					if (result.isErr()) {
						processor.error("Can't load workspace extension %s with version %s. Cause: %s", extensionName,
							versionString, result.error()
								.get());
						continue;
					}

					try (Jar bundle = new Jar(result.unwrap())) {
						File cacheFolder = ws
							.getCache("workspaceextensions/" + extensionName + "/" + bundle.getVersion());
						Version jarVersion = new Version(bundle.getVersion());
						if (jarVersion.isSnapshot() && cacheFolder.exists()) {
							cacheFolder.delete();
						}
						bundle.writeFolder(cacheFolder);

						File includeFile = IO.getFile(cacheFolder, include);
						if (includeFile == null || !includeFile.exists()) {
							processor.error(
								"Workspace Extension %s with Version %s seems corrupt. The stated file to include %s can't be found.",
								extensionName, versionString, include);
							continue;
						}
						processor.doIncludeFile(includeFile, false, processor.getProperties(), extensionName);
						enabledWorkspaceExtensions.put(extensionIdentifyer, cacheFolder);
					}
				} catch (Exception e) {
					processor.getLogger()
						.error("Could not apply workspace extension " + extensionName + " in version " + versionString,
							e);
				}
			}
		}
		return doneSomething;
	}

	private void updateAvailableWorkspaceExtension() throws Exception {
		ws.findProviders(WorkspaceExtensionNamespace.CAPABILITY_NAME_ATTRIBUTE, null, ResourceRepositoryStrategy.REPOS)
			.filter(c -> !availableWorkspaceExtensions.contains(c))
			.forEach(availableWorkspaceExtensions::add);
	}

	public Set<Capability> getAvailableWorkspaceExtensions() {
		return availableWorkspaceExtensions;
	}

	public Map<String, File> getEnabledWorkspaceExtensions() {
		return enabledWorkspaceExtensions;
	}
}
