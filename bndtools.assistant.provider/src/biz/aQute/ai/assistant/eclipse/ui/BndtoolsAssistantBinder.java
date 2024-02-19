package biz.aQute.ai.assistant.eclipse.ui;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;

import aQute.bnd.build.Workspace;
import aQute.bnd.http.HttpClient;
import aQute.lib.io.IO;
import biz.aQute.ai.assistant.provider.BndAssistant;
import biz.aQute.ai.assistant.provider.BndClient;
import biz.aQute.ai.assistant.provider.BndAssistant.AssistantView;
import biz.aQute.openai.assistant.api.Tool;

@Component(immediate = true)
@SuppressWarnings({
	"unchecked", "rawtypes"
})
public class BndtoolsAssistantBinder {
	final Workspace							workspace;
	final Object							lock	= new Object();
	final Map<AssistantView, BndAssistant>	views	= new ConcurrentHashMap<>();
	final BndClient							client;
	final List<Tool>						tools	= new CopyOnWriteArrayList<>();

	@Activate
	public BndtoolsAssistantBinder(@Reference
	Workspace workspace) {
		this.workspace = workspace;
		HttpClient plugin = workspace.getPlugin(HttpClient.class);
		this.client = new BndClient(plugin);

	}

	@Reference(cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC)
	void addView(AssistantView view) {
		BndAssistant a = new BndAssistant(workspace, view, client, tools);
		views.put(view, a);
		view.bind(a);
	}

	void removeView(AssistantView view) throws Exception {
		view.bind(null);
		BndAssistant remove = views.remove(view);
		IO.close(remove);
	}

	@Reference
	void addTool(Tool tool) {
		tools.add(tool);
	}

	void removeTool(Tool tool) {
		tools.remove(tool);
	}
}
