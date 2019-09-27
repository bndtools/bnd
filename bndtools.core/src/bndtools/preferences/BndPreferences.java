package bndtools.preferences;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.bndtools.api.NamedPlugin;
import org.bndtools.headless.build.manager.api.HeadlessBuildManager;
import org.bndtools.versioncontrol.ignores.manager.api.VersionControlIgnoresManager;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.util.IPropertyChangeListener;

import aQute.bnd.build.Workspace;
import bndtools.Plugin;
import bndtools.central.Central;
import bndtools.team.TeamUtils;

public class BndPreferences {

	private static final String		PREF_ENABLE_SUB_BUNDLES			= "enableSubBundles";
	private static final String		PREF_NOASK_PACKAGEINFO			= "noAskPackageInfo";
	private static final String		PREF_USE_ALIAS_REQUIREMENTS		= "useAliasRequirements";
	private static final String		PREF_HIDE_INITIALISE_CNF_WIZARD	= "hideInitialiseCnfWizard";
	private static final String		PREF_HIDE_INITIALISE_CNF_ADVICE	= "hideInitialiseCnfAdvice";
	private static final String		PREF_WARN_EXISTING_LAUNCH		= "warnExistingLaunch";
	private static final String		PREF_HIDE_WARNING_EXTERNAL_FILE	= "hideExternalFileWarning";
	private static final String		PREF_BUILD_LOGGING				= "buildLogging";
	private static final String		PREF_EDITOR_OPEN_SOURCE_TAB		= "editorOpenSourceTab";
	private static final String		PREF_HEADLESS_BUILD_CREATE		= "headlessBuildCreate";
	private static final String		PREF_HEADLESS_BUILD_PLUGINS		= "headlessBuildPlugins";
	private static final String		PREF_VCS_IGNORES_CREATE			= "versionControlIgnoresCreate";
	private static final String		PREF_VCS_IGNORES_PLUGINS		= "versionControlIgnoresPlugins";
	private static final String		PREF_BUILDBEFORELAUNCH			= "buildBeforeLaunch";
	private static final String		PREF_ENABLE_TEMPLATE_REPO		= "enableTemplateRepo";
	private static final String		PREF_TEMPLATE_REPO_URI_LIST		= "templateRepoUriList";

	static final String				PREF_WORKSPACE_OFFLINE			= "workspaceIsOffline";

	private final IPreferenceStore	store;

	public BndPreferences() {
		store = Plugin.getDefault()
			.getPreferenceStore();

		// Defaults...
		store.setDefault(PREF_WARN_EXISTING_LAUNCH, true);
		store.setDefault(PREF_BUILDBEFORELAUNCH, true);
		store.setDefault(PREF_HEADLESS_BUILD_CREATE, true);
		store.setDefault(PREF_HEADLESS_BUILD_PLUGINS, "");
		store.setDefault(PREF_VCS_IGNORES_CREATE, true);
		store.setDefault(PREF_VCS_IGNORES_PLUGINS, "");
		store.setDefault(PREF_ENABLE_TEMPLATE_REPO, false);
		store.setDefault(PREF_TEMPLATE_REPO_URI_LIST,
			"https://raw.githubusercontent.com/bndtools/bundle-hub/master/index.xml.gz");
		store.setDefault(PREF_WORKSPACE_OFFLINE, false);
		store.setDefault(PREF_USE_ALIAS_REQUIREMENTS, true);
	}

	private String mapToPreference(Map<String, Boolean> names) {
		StringBuilder sb = new StringBuilder();
		for (Map.Entry<String, Boolean> nameEntry : names.entrySet()) {
			if (nameEntry.getValue()
				.booleanValue()) {
				if (sb.length() > 0) {
					sb.append("|");
				}
				sb.append(nameEntry.getKey());
			}
		}
		return sb.toString();
	}

	private Map<String, Boolean> preferenceToMap(String preference,
		Collection<? extends NamedPlugin> allPluginsInformation, boolean onlyEnabled) {
		List<String> names = null;
		if (preference != null && !preference.isEmpty()) {
			names = Arrays.asList(preference.split("\\|"));
		}

		boolean atLeastOneEnabled = false;

		Map<String, Boolean> map = new TreeMap<>();
		for (NamedPlugin info : allPluginsInformation) {
			boolean enabled = (names == null) ? (info.isEnabledByDefault() && !info.isDeprecated())
				: names.contains(info.getName());
			map.put(info.getName(), enabled);
			atLeastOneEnabled = atLeastOneEnabled || enabled;
		}

		if (!atLeastOneEnabled && (map.size() > 0)) {
			for (String name : map.keySet()) {
				map.put(name, Boolean.TRUE);
			}
		}

		if (onlyEnabled) {
			Set<String> pluginsToRemove = new HashSet<>();
			for (Map.Entry<String, Boolean> entry : map.entrySet()) {
				if (!entry.getValue()
					.booleanValue()) {
					pluginsToRemove.add(entry.getKey());
				}
			}
			for (String plugin : pluginsToRemove) {
				map.remove(plugin);
			}
		}

		return map;
	}

	public void setNoAskPackageInfo(boolean noAskPackageInfo) {
		store.setValue(PREF_NOASK_PACKAGEINFO, noAskPackageInfo);
	}

	public void setUseAliasRequirements(boolean useAliases) {
		store.setValue(PREF_USE_ALIAS_REQUIREMENTS, useAliases);
	}

	public boolean getNoAskPackageInfo() {
		return store.getBoolean(PREF_NOASK_PACKAGEINFO);
	}

	public boolean getUseAliasRequirements() {
		return store.getBoolean(PREF_USE_ALIAS_REQUIREMENTS);
	}

	public void setHideInitCnfWizard(boolean hide) {
		store.setValue(PREF_HIDE_INITIALISE_CNF_WIZARD, hide);
	}

	public boolean getHideInitCnfWizard() {
		return store.getBoolean(PREF_HIDE_INITIALISE_CNF_WIZARD);
	}

	public void setWarnExistingLaunch(boolean warnExistingLaunch) {
		store.setValue(PREF_WARN_EXISTING_LAUNCH, warnExistingLaunch);
	}

	public boolean getWarnExistingLaunches() {
		return store.getBoolean(PREF_WARN_EXISTING_LAUNCH);
	}

	public void setEnableSubBundles(String enableSubs) {
		store.setValue(PREF_ENABLE_SUB_BUNDLES, enableSubs);
	}

	public String getEnableSubBundles() {
		return store.getString(PREF_ENABLE_SUB_BUNDLES);
	}

	public void setBuildLogging(int buildLogging) {
		store.setValue(PREF_BUILD_LOGGING, buildLogging);
	}

	public int getBuildLogging() {
		return store.getInt(PREF_BUILD_LOGGING);
	}

	public void setHideInitCnfAdvice(boolean hide) {
		store.setValue(PREF_HIDE_INITIALISE_CNF_ADVICE, hide);
	}

	public boolean getHideInitCnfAdvice() {
		return store.getBoolean(PREF_HIDE_INITIALISE_CNF_ADVICE);
	}

	public void setHideWarningExternalFile(boolean hide) {
		store.setValue(PREF_HIDE_WARNING_EXTERNAL_FILE, hide);
	}

	public boolean getHideWarningExternalFile() {
		return store.getBoolean(PREF_HIDE_WARNING_EXTERNAL_FILE);
	}

	public boolean getEnableTemplateRepo() {
		return store.getBoolean(PREF_ENABLE_TEMPLATE_REPO);
	}

	public void setEnableTemplateRepo(boolean enable) {
		store.setValue(PREF_ENABLE_TEMPLATE_REPO, enable);
	}

	public List<String> getTemplateRepoUriList() {
		String urisStr = store.getString(PREF_TEMPLATE_REPO_URI_LIST);
		return Arrays.asList(urisStr.split("\\s"));
	}

	public void setTemplateRepoUriList(List<String> uris) {
		StringBuilder sb = new StringBuilder();
		for (Iterator<String> i = uris.iterator(); i.hasNext();) {
			sb.append(i.next());
			if (i.hasNext())
				sb.append(' ');
		}
		store.setValue(PREF_TEMPLATE_REPO_URI_LIST, sb.toString());
	}

	public IPreferenceStore getStore() {
		return store;
	}

	public void setEditorOpenSourceTab(boolean editorOpenSourceTab) {
		store.setValue(PREF_EDITOR_OPEN_SOURCE_TAB, editorOpenSourceTab);
	}

	public boolean getEditorOpenSourceTab() {
		return store.getBoolean(PREF_EDITOR_OPEN_SOURCE_TAB);
	}

	public void setHeadlessBuildCreate(boolean headlessCreate) {
		store.setValue(PREF_HEADLESS_BUILD_CREATE, headlessCreate);
	}

	public boolean getHeadlessBuildCreate() {
		return store.getBoolean(PREF_HEADLESS_BUILD_CREATE);
	}

	public void setHeadlessBuildPlugins(Map<String, Boolean> names) {
		store.setValue(PREF_HEADLESS_BUILD_PLUGINS, mapToPreference(names));
	}

	public Map<String, Boolean> getHeadlessBuildPlugins(Collection<? extends NamedPlugin> allPluginsInformation,
		boolean onlyEnabled) {
		if (!getHeadlessBuildCreate()) {
			return Collections.emptyMap();
		}

		return preferenceToMap(store.getString(PREF_HEADLESS_BUILD_PLUGINS), allPluginsInformation, onlyEnabled);
	}

	/**
	 * Return the enabled headless build plugins.
	 * <ul>
	 * <li>When plugins is not null and not empty then plugins itself is
	 * returned</li>
	 * <li>Otherwise this method determines from the preferences which plugins
	 * are enabled</li>
	 * </ul>
	 *
	 * @param manager the headless build manager
	 * @param plugins the plugins, can be null or empty.
	 * @return the enabled plugins
	 */
	public Set<String> getHeadlessBuildPluginsEnabled(HeadlessBuildManager manager, Set<String> plugins) {
		if (plugins != null && !plugins.isEmpty()) {
			return plugins;
		}

		return getHeadlessBuildPlugins(manager.getAllPluginsInformation(), true).keySet();
	}

	public void setVersionControlIgnoresCreate(boolean versionControlIgnoresCreate) {
		store.setValue(PREF_VCS_IGNORES_CREATE, versionControlIgnoresCreate);
	}

	public boolean getVersionControlIgnoresCreate() {
		return store.getBoolean(PREF_VCS_IGNORES_CREATE);
	}

	public void setVersionControlIgnoresPlugins(Map<String, Boolean> names) {
		store.setValue(PREF_VCS_IGNORES_PLUGINS, mapToPreference(names));
	}

	public Map<String, Boolean> getVersionControlIgnoresPlugins(Collection<? extends NamedPlugin> allPluginsInformation,
		boolean onlyEnabled) {
		if (!getVersionControlIgnoresCreate()) {
			return Collections.emptyMap();
		}

		return preferenceToMap(store.getString(PREF_VCS_IGNORES_PLUGINS), allPluginsInformation, onlyEnabled);
	}

	/**
	 * Return the enabled version control ignores plugins.
	 * <ul>
	 * <li>When plugins is not null and not empty then plugins itself is
	 * returned</li>
	 * <li>Otherwise, when the files in the project are already managed by a
	 * version control system, this method tries to detect which plugins can
	 * apply ignores for the version control system</li>
	 * <li>Otherwise this method determines from the preferences which plugins
	 * are enabled</li>
	 * </ul>
	 *
	 * @param manager the version control ignores manager
	 * @param project the project (can be null to ignore it)
	 * @param plugins the plugins, can be null or empty.
	 * @return the enabled plugins
	 */
	public Set<String> getVersionControlIgnoresPluginsEnabled(VersionControlIgnoresManager manager,
		IJavaProject project, Set<String> plugins) {
		if (plugins != null && !plugins.isEmpty()) {
			return plugins;
		}

		if (project != null) {
			String repositoryProviderId = TeamUtils.getProjectRepositoryProviderId(project);
			if (repositoryProviderId != null) {
				Set<String> managingPlugins = Plugin.getDefault()
					.getVersionControlIgnoresManager()
					.getPluginsForProjectRepositoryProviderId(repositoryProviderId);
				if (managingPlugins != null && !managingPlugins.isEmpty()) {
					return managingPlugins;
				}
			}
		}

		return getVersionControlIgnoresPlugins(manager.getAllPluginsInformation(), true).keySet();
	}

	public boolean getBuildBeforeLaunch() {
		return store.getBoolean(PREF_BUILDBEFORELAUNCH);
	}

	public void setBuildBeforeLaunch(boolean b) {
		store.setValue(PREF_BUILDBEFORELAUNCH, b);
	}

	public boolean isWorkspaceOffline() {
		return store.getBoolean(PREF_WORKSPACE_OFFLINE);
	}

	public void setWorkspaceOffline(boolean b) {
		Workspace workspace = Central.getWorkspaceIfPresent();
		if (workspace != null) {
			workspace.setOffline(b);
		}
		store.setValue(PREF_WORKSPACE_OFFLINE, b);
	}

	public void addPropertyChangeListener(IPropertyChangeListener listener) {
		store.addPropertyChangeListener(listener);
	}

	public void removePropertyChangeListener(IPropertyChangeListener listener) {
		store.removePropertyChangeListener(listener);
	}
}
