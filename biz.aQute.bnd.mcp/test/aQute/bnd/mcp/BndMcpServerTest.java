package aQute.bnd.mcp;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.channels.Channels;
import java.nio.channels.Pipe;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.modelcontextprotocol.server.McpSyncServer;

/**
 * Talks raw JSON-RPC to the server, as an MCP client would over stdio.
 */
class BndMcpServerTest {

	final ObjectMapper	mapper	= new ObjectMapper();

	@TempDir
	Path				tmp;

	Workspaces			workspaces;
	Runs				runs;
	McpSyncServer		server;
	OutputStream		toServer;
	BufferedReader		fromServer;
	int					nextId	= 1;

	@BeforeEach
	void start() throws Exception {
		Pipe request = Pipe.open();
		Pipe response = Pipe.open();
		toServer = Channels.newOutputStream(request.sink());
		fromServer = new BufferedReader(
			new InputStreamReader(Channels.newInputStream(response.source()), StandardCharsets.UTF_8));
		workspaces = new Workspaces(List.of());
		runs = new Runs();
		server = BndMcpServer.start(Channels.newInputStream(request.source()), Channels.newOutputStream(response.sink()),
			workspaces, runs);

		JsonNode init = request("initialize", Map.of("protocolVersion", "2025-06-18", "capabilities", Map.of(),
			"clientInfo", Map.of("name", "test", "version", "1")));
		assertThat(init.at("/result/serverInfo/name")
			.asText()).isEqualTo("bnd");
		send(Map.of("jsonrpc", "2.0", "method", "notifications/initialized"));
	}

	@AfterEach
	void stop() {
		server.close();
		runs.close();
		workspaces.close();
	}

	@Test
	void listsTheBndTools() throws Exception {
		JsonNode tools = request("tools/list", Map.of()).at("/result/tools");

		assertThat(tools.findValuesAsText("name")).contains("bnd_fragments_list", "bnd_workspace_create",
			"bnd_workspace_describe", "bnd_build", "bnd_resolve", "bnd_run_start", "bnd_run_stop", "bnd_run_status",
			"bnd_http_get");
		JsonNode create = find(tools, "bnd_workspace_create");
		assertThat(create.at("/inputSchema/required")
			.toString()).isEqualTo("[\"dir\"]");
		assertThat(create.at("/inputSchema/properties/fragments/type")
			.asText()).isEqualTo("array");
	}

	@Test
	void createsAWorkspaceWithStructuredResult() throws Exception {
		String index = WorkspacesTest.Fixtures.index(tmp.resolve("fixtures"));
		Path ws = tmp.resolve("ws");

		JsonNode result = request("tools/call", Map.of("name", "bnd_workspace_create", "arguments",
			Map.of("dir", ws.toString(), "fragments", List.of("app"), "indexes", List.of(index), "trustThirdParty",
				true))).get("result");

		assertThat(result.get("isError")
			.asBoolean()).isFalse();
		assertThat(result.at("/structuredContent/bndruns")
			.toString()).isEqualTo("[\"demo/launch.bndrun\"]");
	}

	@Test
	void problemsAreToolErrorsNotProtocolErrors() throws Exception {
		JsonNode notAWorkspace = request("tools/call",
			Map.of("name", "bnd_workspace_describe", "arguments", Map.of("dir", tmp.toString())));
		JsonNode missingDir = request("tools/call",
			Map.of("name", "bnd_workspace_describe", "arguments", Map.of()));

		assertThat(notAWorkspace.at("/result/isError")
			.asBoolean()).isTrue();
		assertThat(notAWorkspace.at("/result/structuredContent/diagnostics/errors/0")
			.asText()).contains("Not a bnd workspace");
		assertThat(missingDir.at("/result/isError")
			.asBoolean()).isTrue();
		assertThat(missingDir.at("/result/content/0/text")
			.asText()).contains("dir");
	}

	private JsonNode find(JsonNode tools, String name) {
		for (JsonNode tool : tools) {
			if (name.equals(tool.get("name")
				.asText())) {
				return tool;
			}
		}
		throw new AssertionError("No tool " + name);
	}

	private JsonNode request(String method, Object params) throws Exception {
		int id = nextId++;
		send(Map.of("jsonrpc", "2.0", "id", id, "method", method, "params", params));
		while (true) {
			String line = fromServer.readLine();
			assertThat(line).as("server closed the stream")
				.isNotNull();
			JsonNode message = mapper.readTree(line);
			if (message.path("id")
				.asInt(-1) == id) {
				return message;
			}
		}
	}

	private void send(Object message) throws Exception {
		toServer.write((mapper.writeValueAsString(message) + "\n").getBytes(StandardCharsets.UTF_8));
		toServer.flush();
	}
}
