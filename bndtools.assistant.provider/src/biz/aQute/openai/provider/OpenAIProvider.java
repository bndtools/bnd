package biz.aQute.openai.provider;

import java.lang.invoke.MethodHandle;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.Proxy;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.Consumer;
import java.util.stream.Stream;

import org.osgi.dto.DTO;
import org.osgi.util.promise.Promise;
import org.osgi.util.promise.PromiseFactory;

import aQute.bnd.exceptions.Exceptions;
import aQute.lib.converter.Converter;
import aQute.lib.converter.TypeReference;
import aQute.lib.filter.Filter;
import aQute.lib.io.IO;
import aQute.lib.json.JSONCodec;
import biz.aQute.jsonschema.api.Description;
import biz.aQute.jsonschema.api.SchemaDTO;
import biz.aQute.openai.api.OpenAI;
import biz.aQute.openai.assistant.api.Assistant;
import biz.aQute.openai.assistant.api.Assistant.AssistantBuilder;
import biz.aQute.openai.assistant.api.Assistant.FunctionToolDTO;
import biz.aQute.openai.assistant.api.Message;
import biz.aQute.openai.assistant.api.Message.MessageBuilder;
import biz.aQute.openai.assistant.api.Message.MessageDTO;
import biz.aQute.openai.assistant.api.MessageThread;
import biz.aQute.openai.assistant.api.MessageThread.MessageThreadBuilder;
import biz.aQute.openai.assistant.api.Run;
import biz.aQute.openai.assistant.api.Run.RunBuilder;
import biz.aQute.openai.provider.OpenAIProvider.RunStep.RunStepDTO;

@SuppressWarnings("unused")
public class OpenAIProvider implements OpenAI, AutoCloseable {
	final static String				BASE		= "https://api.openai.com/v1/";
	final static JSONCodec			codec		= new JSONCodec().promiscuous();
	final static PromiseFactory		promises	= new PromiseFactory(null);

	final ScheduledExecutorService	scheduler	= Executors.newScheduledThreadPool(10);

	public interface Client {
		<M, R> R webrequest(String url, String method, Map<String, String> headers, M msg, Class<R> replyType);
	}

	public static class DeletionStatus extends DTO {
		public boolean	deleted;
		public String	id;
		public String	object;
	}

	public static class ListDTO extends DTO {
		public List<Map<String, Object>>	data;
		public String						object;
	}

	class BaseBuilderImpl<T extends BaseBuilderImpl<T>> {
		final Map<String, String> metadata = new LinkedHashMap<>();

		public T metadata(String key, String value) {
			return getThis();
		}

		@SuppressWarnings("unchecked")
		final T getThis() {
			return (T) this;
		}

	}

	abstract class BaseImpl implements BaseAPI {

		final Map<String, Object> dto;

		public BaseImpl(Map<String, Object> dto) {
			this.dto = dto;
		}

		@Override
		public String getId() {
			return (String) dto.get("id");
		}

		abstract void setDTO(Map<String, Object> dto);

		public boolean matches(String filter) {
			try {
				Filter f = new Filter(filter);
				Map<String, Object> attributes = new LinkedHashMap<>();
				getAttributes(attributes);
				return f.matchMap(attributes);
			} catch (Exception e) {
				throw Exceptions.duck(e);
			}
		}

		public void getAttributes(Map<String, Object> attributes) {
			try {
				attributes.putAll(dto);
			} catch (Exception e) {
				throw Exceptions.duck(e);
			}
		}
	}

	class AssistantImpl extends BaseImpl implements Assistant {
		final Map<String, MethodHandle>					function		= new LinkedHashMap<>();
		final REST<MessageThreadImpl, MessageThread>	messageThreads	= new REST<>(scheduler, client, headers,
			BASE + "threads", MessageThreadImpl.class, MessageThread.class, dto -> new MessageThreadImpl(this, dto));
		AssistantDTO									dto;

		AssistantImpl(Map<String, Object> dto) {
			super(dto);
			this.setDTO(dto);
		}

		@Override
		void setDTO(Map<String, Object> map) {
			dto = cnv(AssistantDTO.class, map);
		}

		@Override
		public void bind(String functionName, MethodHandle handle) {
			function.put(functionName, handle);
		}

		@Override
		public MessageThreadBuilder createMessageThread() {
			return builder(MessageThreadBuilder.class, new BaseBuilder() {
				public MessageThread get() {
					return messageThreads.create(map);
				}
			});
		}

		@Override
		public boolean delete() {
			return assistants.delete(this);
		}

		@Override
		public List<String> getFunctions() {
			return function.keySet()
				.stream()
				.toList();
		}

		@Override
		public MessageThreadImpl getMessageThread(String id) {
			return messageThreads.get(id);
		}

		@Override
		public List<MessageThread> listMessageThreads() {
			return messageThreads.list();
		}

		@Override
		public void close() {
			messageThreads.close();
		}

	}

	class MessageImpl extends BaseImpl implements Message {
		final MessageThreadImpl	thread;
		MessageDTO				dto;

		MessageImpl(MessageThreadImpl thread, Map<String, Object> dto) {
			super(dto);
			this.thread = thread;
			setDTO(dto);
		}

		@Override
		void setDTO(Map<String, Object> map) {
			this.dto = cnv(MessageDTO.class, map);
		}

		@Override
		public boolean delete() {
			return thread.messages.delete(this);
		}

		@Override
		public Assistant getAssistant() {
			return thread.assistant;
		}

		@Override
		public MessageThread getMessageThread() {
			return thread;
		}

		@Override
		public MessageDTO getDTO() {
			return dto;
		}

		@Override
		public String toString() {
			return "MessageImpl [dto=" + Arrays.toString(dto.content) + ", id=" + getId() + "]";
		}

		@Override
		public void close() throws Exception {

		}
	}

	class MessageThreadImpl extends BaseImpl implements MessageThread {
		final AssistantImpl					assistant;
		final REST<MessageImpl, Message>	messages;
		final REST<RunImpl, Run>			runs;

		MessageThreadDTO					dto;
		AutoCloseable						msgPoller;

		@SuppressWarnings("unchecked")
		MessageThreadImpl(AssistantImpl assistant, Map<String, Object> dto) {
			super(dto);
			this.assistant = assistant;
			String id = (String) dto.get("id");
			this.messages = new REST<>(scheduler, client, headers, BASE + "threads/" + id + "/messages",
				MessageImpl.class, Message.class, dtox -> new MessageImpl(this, dtox));
			messages.setValidator(mi -> {
				// bug in openAI
				// first sends empty message and then the same
				// id is used for a content message
				MessageDTO d = mi.getDTO();
				if (d.content.length == 0)
					return false;

				Map<String, Object> context = d.content[0];
				Map<String, Object> text = (Map<String, Object>) context.get("text");
				if (text != null) {
					String object = (String) text.get("value");
					if (object != null) {
						if (object == null || object.isEmpty()) {
							System.out.println("skipping id " + d.id);
							return false;
						}
					}
				}
				return true;
			});
			this.runs = new REST<>(scheduler, client, headers, BASE + "threads/" + id + "/runs", RunImpl.class,
				Run.class, dtox -> new RunImpl(this, dtox));
			setDTO(dto);
		}

		@Override
		void setDTO(Map<String, Object> map) {
			this.dto = cnv(MessageThreadDTO.class, map);
		}

		@Override
		public MessageBuilder createMessage() {
			return builder(MessageBuilder.class, new BaseBuilder() {
				public Message get() {
					return messages.create(map);
				}
			});
		}

		@Override
		@SuppressWarnings("unused")
		public RunBuilder createRun() {
			return builder(RunBuilder.class, new BaseBuilder() {
				final Map<String, ToolImpl> tools = new LinkedHashMap<>();

				public <T> void function(Class<T> spec, T implementation) {
					for (Method m : spec.getMethods()) {
						Description td = m.getAnnotation(Description.class);
						FunctionToolDTO tool = new FunctionToolDTO();
						tool.function.name = m.getName();
						if (td != null) {
							tool.function.description = td.value();
						}
						tool.type = "function";
						tool.function.parameters = SchemaDTO
							.createSchemaFromMethod("tools/" + tools.size() + "/function/parameters", m);
						tools.put(m.getName(), new ToolImpl(tool, m, implementation));
					}
				}

				public Run get() {
					fixupTools();
					map.put("assistant_id", assistant.getId());
					RunImpl runImpl = runs.create(map);
					runImpl.setTools(tools);
					return runImpl;
				}

				private void fixupTools() {
					if (!tools.isEmpty()) {
						map.put("tools", tools.values()
							.stream()
							.map(t -> t.dto)
							.toList());
					}
				}
			});
		}

		@Override
		public boolean delete() {
			close();
			return assistant.messageThreads.delete(this);
		}

		@Override
		public List<Message> listMessages() {
			return messages.list();
		}

		@Override
		public MessageThreadBuilder modify() {
			return builder(MessageThreadBuilder.class, new BaseBuilder() {
				public MessageThread get() {
					return assistant.messageThreads.modify(getId(), map);
				}
			});
		}

		public Message getMessage(String message_id) {
			return messages.get(message_id);
		}

		public List<Message> getMessages(Integer limit, String firstId, String lastId) {
			return messages.list(limit, firstId, lastId);
		}

		@Override
		public AutoCloseable onReceiveMessage(Consumer<Message> receiver) {
			return messages.onReceive(receiver);
		}

		@Override
		public AutoCloseable onReceiveRun(Consumer<Run> receiver) {
			return runs.onReceive(receiver);
		}

		@Override
		public void close() {
			IO.close(messages);
			IO.close(runs);

		}
	}

	interface RunStep extends BaseAPI {
		record RunStepDTO(String id, String object, int created_at, String assistant_id, String thread_id,
			String run_id, String type, Run.Status status, Map<String, Object> step_details,
			Map<String, Object> last_error, int expired_at, int canceled_at, int failed_at,
			Map<String, String> metadata, Map<String, Object> usage) {}

		RunStepDTO getRunStepDTO();
	}

	class RunStepImpl extends BaseImpl implements RunStep {

		final RunImpl	runImpl;
		RunStepDTO		dto;

		public RunStepImpl(RunImpl runImpl, Map<String, Object> dto) {
			super(dto);
			this.runImpl = runImpl;
			setDTO(dto);
		}

		@Override
		public boolean delete() {
			return false;
		}

		@Override
		void setDTO(Map<String, Object> dto) {
			this.dto = cnv(RunStepDTO.class, dto);
		}

		@Override
		public RunStepDTO getRunStepDTO() {
			return dto;
		}

		@Override
		public void close() throws Exception {}

	}

	class RunImpl extends BaseImpl implements Run {
		final REST<RunStepImpl, RunStep>	runSteps;
		final Set<String>					processed	= new HashSet<>();

		public static class ToolCallDTO extends DTO {
			public String		id;
			public String		type;
			public FunctionDTO	function;
		}

		public static class FunctionDTO extends DTO {
			public String	name;
			public String	arguments;
		}

		public static class ToolOutputDTO extends DTO {
			public String	tool_call_id;
			public String	output;
		}

		final MessageThreadImpl	messageThread;
		RunDTO					dto;
		Map<String, ToolImpl>	tools;

		public RunImpl(MessageThreadImpl messageThreadImpl, Map<String, Object> dto) {
			super(dto);

			runSteps = new REST<>(scheduler, client, headers,
				BASE + "threads/" + messageThreadImpl.getId() + "/runs/" + getId() + "/steps", RunStepImpl.class,
				RunStep.class, dtox -> new RunStepImpl(this, dtox));
			this.messageThread = messageThreadImpl;
			setDTO(dto);
		}

		void setTools(Map<String, ToolImpl> tools) {
			this.tools = tools;
		}

		@Override
		void setDTO(Map<String, Object> map) {
			this.dto = cnv(RunDTO.class, map);
		}

		@Override
		public boolean delete() {
			return messageThread.runs.delete(this);
		}

		@Override
		public Status getStatus() {
			return dto.status;
		}

		List<RunStep> listRunSteps() {
			return runSteps.list();
		}

		public record ToolOutput(String tool_call_id, String output) {}

		public record ToolOutputs(List<ToolOutput> tool_outputs) {}

		@SuppressWarnings("unchecked")
		@Override
		public Status process(String fromId, Consumer<String> log)
			throws InterruptedException {
			AutoCloseable steps = runSteps.poll(step -> {
				RunStepDTO dto = step.getRunStepDTO();
				Map<String, Object> details = dto.step_details;
				String type = (String) details.get("type");
				switch (type) {
					case "message_creation" -> {
						log.accept("new message " + details.get("message_creation"));
					}
					default -> {
						log.accept(details.toString());
					}
				}

			}, 1000);

			try {
				do {
					refresh();

					Status status = getStatus();

					switch (status) {
						case requires_action -> {
							require_action(log);
						}
						case failed -> {
							log.accept("run failed " + dto);
						}
						default -> {
							if (status.isFinal) {
								log.accept("run is done " + status);
							} else {
								log.accept("run continues " + status);
							}
						}
					}

					Thread.sleep(500);
				} while (!getStatus().isFinal);

				return getStatus();
			} catch (Exception e) {
				log.accept("failed with exception " + e);
				return Status.failed;
			} finally {
				IO.close(steps);
			}
		}

		@SuppressWarnings("unchecked")
		public void require_action(Consumer<String> log) throws InterruptedException {
			Map<String, Object> map = (Map<String, Object>) dto.required_action;
			assert "submit_tool_outputs".equals(map.get("type"));
			Map<String, Object> submit_tool_outputs = (Map<String, Object>) map
				.get("submit_tool_outputs");
			List<ToolCallDTO> tool_calls = cnv(new TypeReference<List<ToolCallDTO>>() {},
				submit_tool_outputs.get("tool_calls"));

			Map<String, Promise<ToolOutput>> all = new LinkedHashMap<>();
			for (ToolCallDTO tool_call : tool_calls) {
				log.accept("requiring action: " + tool_call);
				Promise<ToolOutput> pt = promises.submit(() -> {
					try {
						log.accept("begin tool call " + tool_call.function.name + " " + tool_call.id);
						ToolImpl tool = tools.get(tool_call.function.name);
						String execute = execute(tool, tool_call.function.arguments);
						return new ToolOutput(tool_call.id, execute);
					} catch (Exception e) {
						return new ToolOutput(tool_call.id, Exceptions.toString(e));
					} finally {
						log.accept("end tool call " + tool_call.function.name + " " + tool_call.id);
					}
				});
				all.put(tool_call.id, pt);
			}

			List<ToolOutput> list = all.entrySet()
				.stream()
				.map( (Map.Entry<String,Promise<ToolOutput>> entry) -> {
					ToolOutput to;
					String call_id = entry.getKey();
					Promise<ToolOutput> output = entry.getValue().timeout(15_000);
					try {
						to= output.getValue();
					} catch (InvocationTargetException | InterruptedException e) {
						log.accept(Exceptions.toString(e));
						to = new ToolOutput(call_id, Exceptions.toString(e));
					}
					return to;
				})
				.toList();

			client.webrequest(BASE + "threads/" + messageThread.getId() + "/runs/" + getId() + "/submit_tool_outputs",
				"POST", headers, new ToolOutputs(list), null);

			log.accept("done requiring action");
		}

		private void refresh() {
			messageThread.runs.get(getId());
		}

		@Override
		public void close() throws Exception {
			IO.close(runSteps);
		}
	}

	static {
		codec.setIgnorenull(true);
	}

	abstract class BaseBuilder implements InvocationHandler {
		final Map<String, Object>			map			= new LinkedHashMap<>();
		final Class<? extends BaseBuilder>	thisClass	= getClass();

		@Override
		public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {

			try {
				Method m = thisClass.getMethod(method.getName(), method.getParameterTypes());
				Object result = m.invoke(this, args);
				if (result == null)
					return proxy;
				else
					return result;
			} catch (InvocationTargetException e) {
				throw e.getTargetException();
			} catch (NoSuchMethodException e) {
				// ignore
			}

			if (method.getParameterCount() == 1) {
				map.put(method.getName(), args[0]);
				return proxy;
			} else
				throw new IllegalArgumentException(
					"A builder interface must have a get() method, 2 arg methods for map parameters, and 1 arg methods for parameters");
		}

		public void metadata(String key, String data) {
			@SuppressWarnings("unchecked")
			Map<String, String> metadata = (Map<String, String>) map.computeIfAbsent("metadata",
				k -> new LinkedHashMap<>());
			metadata.put(key, data);
		}
	}

	@SuppressWarnings("unchecked")
	static <B, T> B builder(Class<B> builderInterface, BaseBuilder builder) {
		return (B) Proxy.newProxyInstance(builderInterface.getClassLoader(), new Class[] {
			builderInterface
		}, builder);
	}

	record ToolImpl(FunctionToolDTO dto, Method spec, Object implementaton) {

	}

	final String							apiKey;
	final Client							client;
	final Map<String, String>				headers;
	final REST<AssistantImpl, Assistant>	assistants;

	public OpenAIProvider(String apiKey, Client client) {
		this.client = client;
		this.apiKey = apiKey;
		this.headers = new LinkedHashMap<>();
		this.headers.put("Content-Type", "application/json");
		this.headers.put("Authorization", "Bearer " + apiKey);
		this.headers.put("OpenAI-Beta", "assistants=v1");
		this.assistants = new REST<>(scheduler, client, headers, BASE + "assistants", AssistantImpl.class,
			Assistant.class, par -> new AssistantImpl(par));
	}

	@Override
	@SuppressWarnings("unused")
	public AssistantBuilder createAssistant(String model) {
		return builder(AssistantBuilder.class, new BaseBuilder() {
			final Map<String, MethodHandle> functions = new LinkedHashMap<>();

			public void function(String name, MethodHandle handle) {
				functions.put(name, handle);
			}

			public Assistant get() {
				map.putIfAbsent("model", model);
				return assistants.create(map);
			}
		});
	}

	@Override
	public Stream<Assistant> findAssistant(String filter) {
		return getAssistants().stream()
			.filter(ass -> ass.matches(filter));
	}

	@Override
	public List<Assistant> getAssistants() {
		return assistants.list();
	}

	@Override
	public List<ModelDTO> getModels() {
		ListDTO list = client.webrequest(BASE + "models", "GET", headers, null, ListDTO.class);
		return list.data.stream()
			.map(map -> cnv(ModelDTO.class, map))
			.toList();
	}

	String execute(ToolImpl tool, String arguments) {
		try {
			if (tool == null) {
				return "no such tool";
			}
			StringBuilder sb = new StringBuilder();
			Map<String, Object> parameters = convertJSON(arguments);
			Method spec = tool.spec;
			sb.append("call ")
				.append(spec.getName())
				.append("(");
			Object[] args = new Object[spec.getParameterCount()];
			String del = "";
			for (int i = 0; i < spec.getParameterCount(); i++) {
				Parameter p = spec.getParameters()[i];
				args[i] = cnv(p.getParameterizedType(), parameters.get(p.getName()));
				sb.append(del)
					.append(args[i]);
				del = ",";
			}
			sb.append(") = ");
			Object result = spec.invoke(tool.implementaton, args);
			sb.append(result);
			System.out.println(sb);
			return codec.enc()
				.put(result)
				.toString();
		} catch (Exception e) {
			System.out.println("arg = " + arguments + " exception = " + e);
			return "Invalid arguments " + e.toString();
		}
	}

	private Map<String, Object> convertJSON(String json) {
		try {
			return codec.dec()
				.from(json)
				.get(new TypeReference<Map<String, Object>>() {});
		} catch (Exception e) {
			throw Exceptions.duck(e);
		}
	}

	private <T> T cnv(Class<T> type, Object dto) {
		try {
			return Converter.cnv(type, dto);
		} catch (Exception e) {
			throw Exceptions.duck(e);
		}
	}

	private <T> T cnv(TypeReference<T> type, Object dto) {
		try {
			return Converter.cnv(type, dto);
		} catch (Exception e) {
			throw Exceptions.duck(e);
		}
	}

	private Object cnv(Type type, Object dto) {
		try {
			return Converter.cnv(type, dto);
		} catch (Exception e) {
			throw Exceptions.duck(e);
		}
	}

	@Override
	public void close() throws Exception {
		assistants.close();
	}
}
