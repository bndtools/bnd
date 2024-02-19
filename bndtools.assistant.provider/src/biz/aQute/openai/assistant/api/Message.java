package biz.aQute.openai.assistant.api;

import java.util.List;
import java.util.Map;

import biz.aQute.openai.api.OpenAI.BaseAPI;
import biz.aQute.openai.api.OpenAI.BaseDTO;

public interface Message extends BaseAPI {

	class MessageDTO extends BaseDTO {
		public String				assistant_id;
		public String				thread_id;
		public String				run_id;
		public String				role;
		public Map<String, Object>	content[];
		public List<String>			file_ids;
	}

	interface MessageBuilder {
		MessageBuilder metadata(String key, String data);

		MessageBuilder role(String role);

		MessageBuilder content(String content);

		MessageBuilder put(String key, String value);

		Message get();
	}

	Assistant getAssistant();

	MessageThread getMessageThread();

	MessageDTO getDTO();
}
