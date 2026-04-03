package org.bndtools.refactor.ai;

import java.net.URI;
import java.util.List;

import org.bndtools.refactor.ai.api.Chat;
import org.bndtools.refactor.ai.api.Embedder;
import org.bndtools.refactor.ai.api.OpenAI;
import org.osgi.dto.DTO;

import aQute.bnd.exceptions.Exceptions;
import aQute.bnd.http.HttpClient;
import aQute.bnd.service.url.TaggedData;
import aQute.lib.json.JSONCodec;
import aQute.lib.strings.Strings;

public class OpenAIProvider implements OpenAI {
	final static String		DEFAULT_MODELS	= """
		gpt-3.5-turbo-1106;
		   context=16385;
		   output=4096,
		gpt-3.5-turbo;
		   context=4096;
		   output=4096,
		gpt-3.5-turbo-16k;
		   context=16385;
		   output=4096,
		gpt-4-1106-preview;
		  context=128000;
		  output=4096,
		gpt-4
		  context=8192;
		  output=4096,
		gpt-4-32k;
		  context=32000;
		  output=4096""";

	final static JSONCodec codec = new JSONCodec();
	static {
		codec.setIgnorenull(true);
	}
	final static String	openaiApiUrl	= "https://api.openai.com/v1/chat/completions";
	final static String	modelsUrl		= "https://api.openai.com/v1/models";

	final String		apiKey;
	final HttpClient	client;
	final List<String>	models;

	public OpenAIProvider(HttpClient client, String apiKey, List<String> models) {
		this.models = models == null ? Strings.split(DEFAULT_MODELS) : models;
		this.apiKey = apiKey;
		this.client = client;
	}

	@Override
	public Chat createChat(Configuration configuration) {
		return new ChatImpl(this, configuration);
	}

	<M extends DTO, R extends DTO> R get(String url, M msg, Class<R> replyType) {
		try {

			TaggedData reply;
			if (msg != null) {
				String payload = codec.enc()
					.writeDefaults()
					.put(msg)
					.toString();
				System.out.println(payload);
				reply = client.build()
					.headers("Content-Type", "application/json")
					.headers("Authorization", "Bearer " + apiKey)
					.upload(payload)
					.post()
					.asTag()
					.go(new URI(url));
			} else {
				reply = client.build()
					.headers("Content-Type", "application/json")
					.headers("Authorization", "Bearer " + apiKey)
					.get()
					.asTag()
					.go(new URI(url));
			}

			if (reply.getResponseCode() >= 300) {
				throw new RuntimeException("Error response: " + reply);
			} else {
				return codec.dec()
					.from(reply.getInputStream())
					.get(replyType);
			}
		} catch (Exception e) {
			throw Exceptions.duck(e);
		}
	}

	public static class Model extends DTO {
		public String		id;
		public String		object;
		public String		owned_by;
		public List<Object>	permission;
	}

	public static class Models extends DTO {
		public List<Model>	data;
		public String		object;
	}

	@Override
	public List<String> models() {
		return get(modelsUrl, null, Models.class).data.stream()
			.map(m -> m.id)
			.toList();
	}

	public static class Usage extends DTO {
		public int	prompt_tokens;
		public int	total_tokens;
	}

	public static class EmbeddingRequestDTO extends DTO {
		public String	input;
		public String	model;
	}

	public static class EmbeddingPayloadDTO extends DTO {
		public float[]	embedding;
		public int		index;
		public String	object;
	}

	public static class EmbeddingResponseDTO extends DTO {
		public List<EmbeddingPayloadDTO>	data;
		public String						model;
		public String						object;
		public Usage						usage;
	}

	@Override
	public Embedder getEmbedder(Configuration configuration) {
		return new Embedder() {

			@Override
			public float[] getEmbedding(String text) {
				EmbeddingRequestDTO request = new EmbeddingRequestDTO();
				request.model = configuration.model == null ? "gpt-3.5-turbo" : configuration.model;
				request.input = text;
				EmbeddingResponseDTO response = get("https://api.openai.com/v1/embeddings", request,
					EmbeddingResponseDTO.class);
				return response.data.get(0).embedding;
			}
		};
	}

	@Override
	public List<String> getModels() {
		return models;
	}

}
