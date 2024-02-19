package biz.aQute.ai.assistant.provider;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.commonmark.Extension;
import org.commonmark.ext.autolink.AutolinkExtension;
import org.commonmark.ext.gfm.tables.TablesExtension;
import org.commonmark.node.Node;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;
import org.commonmark.renderer.html.UrlSanitizer;

import aQute.bnd.build.Workspace;
import aQute.bnd.header.Attrs;
import aQute.bnd.header.Parameters;
import aQute.lib.io.IO;
import aQute.lib.settings.Settings;
import biz.aQute.openai.assistant.api.Assistant;
import biz.aQute.openai.assistant.api.Message;
import biz.aQute.openai.assistant.api.Message.MessageDTO;
import biz.aQute.openai.assistant.api.MessageThread;
import biz.aQute.openai.assistant.api.Run;
import biz.aQute.openai.assistant.api.Run.RunBuilder;
import biz.aQute.openai.assistant.api.Run.Status;
import biz.aQute.openai.assistant.api.Tool;
import biz.aQute.openai.assistant.api.Tool.ToolInstance;
import biz.aQute.openai.provider.OpenAIProvider;
import biz.aQute.openai.provider.OpenAIProvider.Client;

public class BndAssistant implements AutoCloseable {
	final List<Extension>		extensions				= Arrays.asList(TablesExtension.create(),
		AutolinkExtension.create());
	final Parser				parser					= Parser.builder()
		.extensions(extensions)
		.build();
	final HtmlRenderer			renderer				= HtmlRenderer.builder()
		.extensions(extensions)
		.urlSanitizer(new UrlSanitizer() {

			@Override
			public String sanitizeImageUrl(String arg0) {
				return arg0;
			}

			@Override
			public String sanitizeLinkUrl(String arg0) {
				return arg0;
			}
		})
		.build();

	public static final String	DEFAULT_INSTRUCTIONS	= """
		You are an assistant helping a Bndtools/Eclipse user developing
		java software for OSGi. Functions are provided to interact with
		a bnd workspace.
		${def;ai-description}
		A workspace is a Unix directory where the **top level** are the
		many projects. Each project has the following layout:
		- ${def;src;src} – contain the Java source files like some_package/Source.java
		- ${def;test;test} – contain the Java test files
		- ${def;bin;bin} – contain the Java class files
		- ${def;testbin;bin_test} – contain the Java test class files
		- ${def;target-dir;generated} – contain the generated JAR files
		- bnd.bnd – the bnd file with setup information
		- .project – the Eclipse project file
		- .classpath – the Eclipse class path file

		* When writing or updating a Java source, always rewrite the complete file and never use // ...
		* Do not be lazy
		* Always write files in a project, at the proper place, never in the workspace root
		* When adding a dependency to a bnd.bnd file, do not set the version
		""";
	final Settings				settings				= new Settings();
	final List<Tool>			tools;
	final Workspace				workspace;
	final OpenAIProvider		openAI;
	final Assistant				assistant;
	final MessageThread			messageThread;
	final AssistantView			aiView;
	final String				instructions;
	final Parameters			ai;
	final AutoCloseable			messagePoller;
	String						additional_instructions;

	public interface AssistantView {
		void status(String statusMessage);

		void received(String text);

		void bind(BndAssistant a);

		void refresh(File file);
	}

	public BndAssistant(Workspace workspace, AssistantView aiView, Client client, List<Tool> tools) {
		this.tools = tools;
		this.workspace = workspace;
		this.aiView = aiView;
		this.openAI = new OpenAIProvider(settings.get("openai.apikey"), client);

		ai = this.workspace.getMergedParameters("-ai");

		Attrs deflt = ai.getOrDefault("default", new Attrs());
		String domain = deflt.getOrDefault("domain", "bnd");
		StringBuilder instructions = new StringBuilder(deflt.getOrDefault("instructions", DEFAULT_INSTRUCTIONS));

		this.instructions = workspace.getReplacer()
			.process(instructions.toString());
		this.assistant = this.openAI.findAssistant("(domain=" + domain + ")")
			.findAny()
			.orElseGet(() -> this.openAI.createAssistant("gpt-3.5-turbo-1106")
				.metadata("domain", domain)
				.get());
		messageThread = this.assistant.createMessageThread()
			.get();
		messagePoller = messageThread.onReceiveMessage(message -> {
			String s = toHTML(message);
			aiView.received(s);
		});
	}

	@SuppressWarnings("unchecked")
	private String toHTML(Message message) {
		MessageDTO dto = message.getDTO();
		StringBuilder sb = new StringBuilder();
		for (Map<String, Object> content : dto.content) {
			String type = (String) content.get("type");
			switch (type) {
				case "text" -> {
					Map<String, Object> text = (Map<String, Object>) content.get("text");
					sb.append(text.get("value"))
						.append("\n");
				}
				default -> {
					sb.append("<div class='unknown'>\n")
						.append(content)
						.append("\n</div>\n");
				}
			}
		}
		sb.append("\n");
		Node document = parser.parse(sb.toString());
		String html = renderer.render(document);
		sb = new StringBuilder();
		sb.append("<div class='message ")
			.append(dto.role)
			.append("'>\n");
		sb.append(html);
		sb.append("\n</div>");
		return sb.toString();
	}

	@SuppressWarnings({
		"rawtypes", "unchecked"
	})
	public String send(String input) {
		List<ToolInstance> instances = new ArrayList<>();
		try {
			aiView.status("sending message");
			Message message = messageThread.createMessage()
				.content(input)
				.role("user")
				.get();
			aiView.status("creating run");
			RunBuilder runBuilder = messageThread.createRun()
				.instructions(this.instructions)
				.additional_instructions(this.additional_instructions);

			tools.forEach(tool -> {
				ToolInstance newInstance = tool.newInstance();
				runBuilder.function(newInstance.type(), newInstance.tool());
			});

			Run run = runBuilder.get();

			try {
				Status status = run.process(message.getId(), aiView::status);
				return status.toString();

			} catch (InterruptedException e) {
				return "interrupted";
			} finally {
				IO.close(run);
			}
		} finally {
			instances.stream()
				.map(ToolInstance::close)
				.forEach(IO::close);
		}
	}

	@Override
	public void close() throws Exception {
		IO.close(messagePoller);
	}

}
