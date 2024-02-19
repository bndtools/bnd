package biz.aQute.openai.assistant.api;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import org.osgi.dto.DTO;

import biz.aQute.openai.api.OpenAI;
import biz.aQute.openai.api.OpenAI.BaseAPI;

public interface Run extends BaseAPI {
	enum Status {
		queued(false),
		in_progress(false),
		requires_action(false),
		cancelling(false),
		cancelled(true),
		failed(true),
		completed(true),
		expired(true);

		public final boolean isFinal;

		Status(boolean isFinal) {
			this.isFinal = isFinal;
		}
	}

	class RunDTO extends DTO {
		public Map<String, String>			metadata;
		public String						id;
		public String						object;
		public int							created_at;
		public String						thread_id;
		public String						assistant_id;
		public Status						status;
		public Object						required_action;
		public Object						last_error;
		public int							expires_at;
		public int							started_at;
		public int							cancelled_at;
		public int							failed_at;
		public int							completed_at;
		public String						model;
		public String						instructions;
		public List<Map<String, Object>>	tools;
		public List<OpenAI.FileDTO>			file_ids;
		public Map<String, Object>			usage;
	}

	interface RunBuilder {
		RunBuilder metadata(String key, String data);

		RunBuilder model(String model);

		RunBuilder instructions(String instructions);

		RunBuilder additional_instructions(String instructions);

		<T> RunBuilder function(Class<T> spec, T tool);

		Run get();
	}

	Status getStatus();

	Status process(String fromMessage, Consumer<String> message)
		throws InterruptedException;
}
