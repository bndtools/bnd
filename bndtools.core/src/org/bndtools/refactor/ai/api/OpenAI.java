package org.bndtools.refactor.ai.api;

import java.util.List;

import org.osgi.dto.DTO;

public interface OpenAI {
	class Configuration extends DTO {
		public String model;
	}
	List<String> models();

	Embedder getEmbedder(Configuration configuration);

	Chat createChat(Configuration configuration);

	default Chat createChat() {
		return createChat(new Configuration());
	}

	/**
	 * Get a list of preferred models.
	 *
	 * @return the list of preferred models
	 */
	List<String> getModels();

}
