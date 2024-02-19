package biz.aQute.openai;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodHandles.Lookup;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Set;

import org.junit.Test;

import aQute.lib.settings.Settings;
import biz.aQute.jsonschema.api.Description;
import biz.aQute.openai.assistant.api.Assistant;
import biz.aQute.openai.assistant.api.Message;
import biz.aQute.openai.assistant.api.MessageThread;
import biz.aQute.openai.assistant.api.Run;
import biz.aQute.openai.provider.OpenAIProvider;

public class OpenAIProviderTest {
	final static Lookup		lookup		= MethodHandles.lookup();
	final static Settings	settings	= new Settings();

	interface TimeTool {
		@Description("""
				Get the current time
				""")
		String time(String timezone);

		@Description("""
				Get the current zones that are active
				""")
		Set<String> currentZones();
	}

	class TimeToolImpl implements TimeTool {

		@Override
		public String time(String timezone) {
			return LocalDateTime.now(ZoneId.of(timezone)).toString();
		}

		@Override
		public Set<String> currentZones() {
			return Set.of("Europe/Paris", "Asia/Karachi","America/Chicago");
		}

	}

	@Test
	public void testAssistantCreate() throws InterruptedException {
		OpenAIProvider provider = new OpenAIProvider(settings.get("openai.apikey"), new TestClient());

		Assistant assistant = provider.createAssistant("gpt-3.5-turbo").name("test").description("test assistant")
				.instructions("""
						You are a brilliant assistant.
						- ALWAYS output in markdown!
						""")
				.get();

		try {
			assertThat(assistant).isNotNull();
			MessageThread thread = assistant.createMessageThread().get();
			Message message = thread.createMessage().role("user").content("""
					Get met the current time in the current zones
					""").get();
			assertThat(message).isNotNull();

			Run run = thread.createRun().function(TimeTool.class, new TimeToolImpl()).get();
			assertThat(run).isNotNull();

			run.process(null, System.err::println);

			List<Message> messages = thread.listMessages();
			System.out.println(messages.get(0));
		} catch( Exception e) {
			e.printStackTrace();
		} finally {
			assertThat(assistant.delete()).isTrue();
		}
	}

	@Test
	public void testGetAssistants() {
		OpenAIProvider provider = new OpenAIProvider(settings.get("openai.apikey"), new TestClient());
		List<Assistant> assistants = provider.getAssistants();
		assertThat(assistants).isNotNull();
		assistants.forEach(Assistant::delete);
	}
}
