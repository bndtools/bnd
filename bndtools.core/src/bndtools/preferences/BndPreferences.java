package bndtools.preferences;

import org.eclipse.jface.preference.IPreferenceStore;

import bndtools.Plugin;
import bndtools.versioncontrol.VersionControlSystem;

public class BndPreferences {

    private static final String PREF_ENABLE_SUB_BUNDLES = "enableSubBundles";
    private static final String PREF_NOASK_PACKAGEINFO = "noAskPackageInfo";
    private static final String PREF_HIDE_INITIALISE_CNF_WIZARD = "hideInitialiseCnfWizard";
    private static final String PREF_HIDE_INITIALISE_CNF_ADVICE = "hideInitialiseCnfAdvice";
    private static final String PREF_WARN_EXISTING_LAUNCH = "warnExistingLaunch";
    private static final String PREF_HIDE_WARNING_EXTERNAL_FILE = "hideExternalFileWarning";
    private static final String PREF_BUILD_LOGGING = "buildLogging";
    private static final String PREF_EDITOR_OPEN_SOURCE_TAB = "editorOpenSourceTab";
    private static final String PREF_VCS_CREATE_IGNORE_FILES = "vcsCreateIgnoreFiles";
    private static final String PREF_VCS_VCS = "vcsVcs";

    private final IPreferenceStore store;

    public BndPreferences() {
        store = Plugin.getDefault().getPreferenceStore();

        // Defaults...
        store.setDefault(PREF_WARN_EXISTING_LAUNCH, true);
        store.setDefault(PREF_VCS_CREATE_IGNORE_FILES, true);
        store.setDefault(PREF_VCS_VCS, VersionControlSystem.GIT.ordinal());
    }

    public void setNoAskPackageInfo(boolean noAskPackageInfo) {
        store.setValue(PREF_NOASK_PACKAGEINFO, noAskPackageInfo);
    }

    public boolean getNoAskPackageInfo() {
        return store.getBoolean(PREF_NOASK_PACKAGEINFO);
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

    public IPreferenceStore getStore() {
        return store;
    }

    public void setEditorOpenSourceTab(boolean editorOpenSourceTab) {
        store.setValue(PREF_EDITOR_OPEN_SOURCE_TAB, editorOpenSourceTab);
    }

    public boolean getEditorOpenSourceTab() {
        return store.getBoolean(PREF_EDITOR_OPEN_SOURCE_TAB);
    }

    public void setVcsCreateIgnoreFiles(boolean vcsCreateIgnoreFiles) {
        store.setValue(PREF_VCS_CREATE_IGNORE_FILES, vcsCreateIgnoreFiles);
    }

    public boolean getVcsCreateIgnoreFiles() {
        return store.getBoolean(PREF_VCS_CREATE_IGNORE_FILES);
    }

    public void setVcsVcs(int vcs) {
        store.setValue(PREF_VCS_VCS, vcs);
    }

    public int getVcsVcs() {
        return store.getInt(PREF_VCS_VCS);
    }
}
