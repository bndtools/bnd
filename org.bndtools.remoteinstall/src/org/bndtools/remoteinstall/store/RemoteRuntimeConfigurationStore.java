package org.bndtools.remoteinstall.store;

import java.util.ArrayList;
import java.util.List;

import org.bndtools.remoteinstall.dto.RemoteRuntimeConfiguration;
import org.eclipse.jface.preference.IPreferenceStore;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;

import aQute.bnd.exceptions.Exceptions;
import aQute.lib.converter.TypeReference;
import aQute.lib.json.JSONCodec;
import bndtools.Plugin;

@Component(service = RemoteRuntimeConfigurationStore.class)
public final class RemoteRuntimeConfigurationStore {

	private static final String		CONFIG_KEY	= "runtimeconfigs";
	private final static JSONCodec	codec		= new JSONCodec();

	private IPreferenceStore		preferenceStore;

	@Activate
	void activate() {
		preferenceStore = Plugin.getDefault()
			.getPreferenceStore();
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
			try {
				return codec.dec()
					.from(storedConfiguration)
					.get(new TypeReference<List<RemoteRuntimeConfiguration>>() {});
			} catch (Exception e) {
				throw Exceptions.duck(e);
			}
		}
		return new ArrayList<>();
	}

	public void removeConfiguration(final int index) {
		final List<RemoteRuntimeConfiguration> configurations = getConfigurations();
		configurations.remove(index);
		setConfigurations(configurations);
	}

	public void setConfigurations(final List<RemoteRuntimeConfiguration> configurations) {
		try {
			preferenceStore.putValue(CONFIG_KEY, codec.enc()
				.put(configurations)
				.toString());
		} catch (Exception e) {
			throw Exceptions.duck(e);
		}
	}

	public void updateConfiguration(final int index, final RemoteRuntimeConfiguration configuration) {
		final List<RemoteRuntimeConfiguration> configurations = getConfigurations();
		configurations.set(index, configuration);
		setConfigurations(configurations);
	}
}
