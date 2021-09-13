package org.bndtools.remoteinstall.store;

import java.util.ArrayList;
import java.util.List;

import org.bndtools.remoteinstall.dto.RemoteRuntimeConfiguration;
import org.eclipse.jface.preference.IPreferenceStore;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import bndtools.Plugin;

@Component(service = RemoteRuntimeConfigurationStore.class)
public final class RemoteRuntimeConfigurationStore {

    private static final String CONFIG_KEY = "runtimeconfigs";

    private IPreferenceStore preferenceStore;

    @Activate
    void activate() {
        preferenceStore = Plugin.getDefault().getPreferenceStore();
    }

    public void addConfiguration(final RemoteRuntimeConfiguration configuration) { 
        final List<RemoteRuntimeConfiguration> configurations = getConfigurations();
        configurations.add(configuration);
        setConfigurations(configurations);
    }

    public RemoteRuntimeConfiguration getConfiguration(final int index) {
        if (index < 0) {
            return null;
        }
        final List<RemoteRuntimeConfiguration> configurations = getConfigurations();
        if (configurations.size() > index) {
            return configurations.get(index);
        }
        return null;
    }

    public List<RemoteRuntimeConfiguration> getConfigurations() {
        final String storedConfiguration = preferenceStore.getString(CONFIG_KEY);
        if (!storedConfiguration.isEmpty()) {
            return new Gson().fromJson(storedConfiguration, new TypeToken<List<RemoteRuntimeConfiguration>>() {
            }.getType());
        }
        return new ArrayList<>();
    }

    public void removeConfiguration(final int index) {
        final List<RemoteRuntimeConfiguration> configurations = getConfigurations();
        configurations.remove(index);
        setConfigurations(configurations);
    }

    public void setConfigurations(final List<RemoteRuntimeConfiguration> configurations) {
        preferenceStore.putValue(CONFIG_KEY, new Gson().toJson(configurations));
    }

    public void updateConfiguration(final int index, final RemoteRuntimeConfiguration configuration) {
        final List<RemoteRuntimeConfiguration> configurations = getConfigurations();
        configurations.set(index, configuration);
        setConfigurations(configurations);
    }
}
