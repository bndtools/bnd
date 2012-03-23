package bndtools.preferences;

import org.eclipse.jface.preference.IPreferenceStore;

import bndtools.Plugin;

public class BndPreferences {

    private static final String PREF_ENABLE_SUB_BUNDLES = "enableSubBundles";
    private static final String PREF_NOASK_PACKAGEINFO = "noAskPackageInfo";
    private static final String PREF_HIDE_INITIALISE_CNF_WIZARD = "hideInitialiseCnfWizard";
    private static final String PREF_HIDE_INITIALISE_CNF_ADVICE = "hideInitialiseCnfAdvice";
    private static final String PREF_WARN_EXISTING_LAUNCH = "warnExistingLaunch";
    private static final String PREF_HIDE_WARNING_EXTERNAL_FILE = "hideExternalFileWarning";
    private static final String PREF_BUILD_LOGGING = "buildLogging";

    private IPreferenceStore store;

    public BndPreferences() {
        store = Plugin.getDefault().getPreferenceStore();

        // Defaults...
        store.setDefault(PREF_WARN_EXISTING_LAUNCH, true);
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

}
