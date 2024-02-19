package biz.aQute.openai.api;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.osgi.dto.DTO;

import biz.aQute.openai.assistant.api.Assistant;
import biz.aQute.openai.assistant.api.Assistant.AssistantBuilder;

public interface OpenAI {
	class BaseDTO extends DTO {
		public String				id;
		public Map<String, String>	metadata;
		public long					created_at;
		public String				object;
	}

	interface BaseAPI extends AutoCloseable {
		String getId();
		boolean delete();

	}

	class ModelDTO extends DTO {
		public String	id;
		public long		created;
		public String	object;
		public String	owned_by;
	}

	class FileDTO extends DTO {
		public String	id;
		public int		bytes;
		public int		created_at;
		public String	filename;
		public String	object;
		public String	purpose;
		@Deprecated
		public String	status;
		@Deprecated
		public String	status_details;
	}

	Stream<Assistant> findAssistant(String filter);

	AssistantBuilder createAssistant(String model);

	List<Assistant> getAssistants();

	List<ModelDTO> getModels();
}
