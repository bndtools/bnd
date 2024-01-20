package org.bndtools.refactor.ai;

import org.bndtools.refactor.ai.api.OpenAI;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import aQute.bnd.build.Workspace;
import aQute.bnd.http.HttpClient;
import aQute.lib.settings.Settings;
import aQute.lib.strings.Strings;

@Component
public class OpenAIComponent {
	final static Settings				settings	= new Settings();
	final OpenAIProvider				provider;
	final Workspace						workspace;
	final ServiceRegistration<OpenAI>	registerService;

	@Activate
	public OpenAIComponent(@Reference
	Workspace workspace, ComponentContext context) {
		this.workspace = workspace;
		HttpClient client = workspace.getPlugin(HttpClient.class);
		if (client != null) {
			String apiKey = settings.getOrDefault("openai.apikey", System.getProperty("OPENAI_APIKEY"));
			String models = settings.getOrDefault("openai.models", null);
			if (apiKey != null) {
				provider = new OpenAIProvider(client, apiKey, models == null ? null : Strings.split(models));
				registerService = context.getBundleContext()
					.registerService(OpenAI.class, provider, null);
				return;
			}
		}
		provider = null;
		registerService = null;
	}
}
