package biz.aQute.openai.assistant.api;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import biz.aQute.openai.api.OpenAI.BaseAPI;
import biz.aQute.openai.api.OpenAI.BaseDTO;
import biz.aQute.openai.assistant.api.Message.MessageBuilder;
import biz.aQute.openai.assistant.api.Run.RunBuilder;

public interface MessageThread extends BaseAPI {
	class MessageThreadDTO extends BaseDTO {
		public String				id;
		public String				object;
		public long					created_at;
		public Map<String, String>	metadata;
	}

	interface MessageThreadBuilder {
		MessageThreadBuilder metadata(String key, String data);
		MessageThread get();
	}

	MessageThreadBuilder modify();

	MessageBuilder createMessage();

	RunBuilder createRun();

	List<Message> listMessages();

	AutoCloseable onReceiveMessage(Consumer<Message> receiver);

	AutoCloseable onReceiveRun(Consumer<Run> receiver);
}
