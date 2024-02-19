package biz.aQute.openai.assistant.api;

import java.lang.invoke.MethodHandle;
import java.util.List;
import java.util.Map;

import org.osgi.dto.DTO;

import biz.aQute.jsonschema.api.SchemaDTO;
import biz.aQute.openai.api.OpenAI.BaseAPI;
import biz.aQute.openai.api.OpenAI.FileDTO;
import biz.aQute.openai.assistant.api.MessageThread.MessageThreadBuilder;

public interface Assistant extends BaseAPI {

	class AssistantDTO extends DTO {
		public String				id;
		public String				object;
		public int					created_at;
		public String				model;
		public String				name;
		public String				description;
		public String				instructions;
		public List<Object>			tools;
		public List<FileDTO>		file_ids;
		public Map<String, String>	metadata;
	}

	public class FunctionDTO extends DTO {
		public String		description;
		public String		name;
		public SchemaDTO	parameters;

	}

	public class FunctionToolDTO extends DTO {
		public String		type;
		public FunctionDTO	function	= new FunctionDTO();
	}

	interface AssistantBuilder {
		AssistantBuilder metadata(String key, String data);

		AssistantBuilder name(String name);

		AssistantBuilder description(String description);

		AssistantBuilder instructions(String instructions);

		Assistant get();
	}

	void bind(String functionName, MethodHandle handle);

	List<String> getFunctions();

	MessageThreadBuilder createMessageThread();

	MessageThread getMessageThread(String id);

	List<MessageThread> listMessageThreads();

	boolean matches(String filter);
}
