package org.bndtools.refactor.ai;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bndtools.refactor.ai.api.Chat;
import org.bndtools.refactor.ai.api.OpenAI.Configuration;
import org.bndtools.refactor.ai.api.Reply;
import org.osgi.dto.DTO;


class ChatImpl implements Chat {
	final static String		CHAT_URL	= "https://api.openai.com/v1/chat/completions";

	final OpenAIProvider	openAIProvider;

	ChatRequest				request		= new ChatRequest();

	final Configuration		configuration;

	public static class Message extends DTO {
		public String	role;
		public String	content;
	}

	public static class ChatRequest extends DTO {
		public String				model				= "gpt-3.5-turbo-1106";
		public List<Message>		messages			= new ArrayList<>();
		public double				temperature			= 0.1;
		public double				top_p				= 1;
		public int					n					= 1;
		public boolean				stream				= false;
		public String				stop;
		public Integer				max_tokens			= null;
		public double				presence_penalty	= 0D;
		public double				frequency_penalty	= 0D;
		public Map<String, Double>	logit_bias			= new HashMap<>();
		public String				user				= "bndtools";
	}

	public static class ChatUsage extends DTO {
		public int	prompt_tokens;
		public int	completion_tokens;
		public int	total_tokens;
	}

	public static class ChatChoice extends DTO {
		public Message	message;
		public String	finish_reason;
		public int		index;
	}

	public static class ChatResponse extends DTO {
		public String			id;
		public String			object;
		public long				created;
		public String			model;
		public ChatUsage		usage;
		public List<ChatChoice>	choices;

	}

	ChatImpl(OpenAIProvider openAIProvider, Configuration configuration) {
		this.configuration = configuration;
		this.openAIProvider = openAIProvider;
	}

	@Override
	public Reply ask(String question) {
		Message m = new Message();
		m.role = "user";
		m.content = question;
		if (configuration.model != null)
			request.model = configuration.model;
		request.messages.add(m);
		ChatResponse chatResponse = openAIProvider.get(CHAT_URL, request, ChatResponse.class);

		Reply reply = new Reply();
		reply.reply = chatResponse.choices.get(0).message.content;
		return reply;
	}

	@Override
	public void system(String command) {
		Message m = new Message();
		m.role = "system";
		m.content = command;
		request.messages.add(m);
	}

	@Override
	public void assistant(String command) {
		Message m = new Message();
		m.role = "assistant";
		m.content = command;
		request.messages.add(m);
	}
	@Override
	public void model(String model) {
		request.model = model;
	}

	@Override
	public void clear() {
		request.messages.clear();
	}

	@Override
	public void clear(String role) {
		request.messages.removeIf(m -> m.role.equals(role));
	}

	@Override
	public void setProlog(String string) {
		request.messages.removeIf( m -> m.role.equals("system"));
		system(string);
	}
}
