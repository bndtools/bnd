package aQute.bnd.mcp;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.Objects;

import io.modelcontextprotocol.server.McpServer;
import io.modelcontextprotocol.server.McpSyncServer;
import io.modelcontextprotocol.server.transport.StdioServerTransportProvider;
import io.modelcontextprotocol.spec.McpSchema.ServerCapabilities;

/**
 * The bnd MCP server: the set of tools an agent sees, mapped onto the domain
 * modules.
 */
public final class BndMcpServer {

	public record FragmentQuery(
		@Param("Extra template index URLs in addition to the bndtools default index") List<String> indexes,
		@Param("Include archived fragments, e.g. older Eclipse releases") boolean includeArchived) {

		public FragmentQuery {
			indexes = indexes == null ? List.of() : List.copyOf(indexes);
		}
	}

	public record Location(@Param(value = "Directory of the bnd workspace", required = true) String dir) {}

	private BndMcpServer() {}

	static ToolAdapter tools(Workspaces workspaces, Runs runs) {
		return new ToolAdapter()
			.tool("bnd_fragments_list",
				"List bnd workspace template fragments (e.g. gradle, osgi, demo-webapp, Eclipse release repositories) from the template indexes.",
				FragmentQuery.class, q -> workspaces.fragments(q.indexes(), q.includeArchived()))
			.tool("bnd_workspace_create",
				"Create a bnd workspace, or extend an existing one, by installing template fragments and their required fragments. Existing files are kept.",
				Workspaces.Spec.class, workspaces::create)
			.tool("bnd_workspace_describe",
				"Describe a bnd workspace: projects, bndrun files, repositories, errors and warnings.", Location.class,
				l -> workspaces.describe(l.dir()))
			.tool("bnd_build", "Build a project in a bnd workspace.", Runs.BuildSpec.class, runs::build)
			.tool("bnd_resolve", "Resolve a workspace .bndrun file.", Runs.ResolveSpec.class, runs::resolve)
			.tool("bnd_run_start", "Start a resolved .bndrun and wait for its HTTP port to accept connections.",
				Runs.StartSpec.class, runs::start)
			.tool("bnd_run_stop", "Stop a running .bndrun.", Runs.StopSpec.class, runs::stop)
			.tool("bnd_run_status", "Get run state and recent launcher output.", Runs.StatusSpec.class, runs::status)
			.tool("bnd_http_get", "GET a loopback HTTP URL and check its status and response body.",
				LocalHttp.GetSpec.class, new LocalHttp()::get);
	}

	/**
	 * Serve MCP over the given streams until the client disconnects.
	 */
	public static McpSyncServer start(InputStream in, OutputStream out, Workspaces workspaces, Runs runs) {
		ToolAdapter tools = tools(workspaces, runs);
		String version = Objects.requireNonNullElse(BndMcpServer.class.getPackage()
			.getImplementationVersion(), "dev");
		return McpServer.sync(new StdioServerTransportProvider(tools.jsonMapper(), in, out))
			.serverInfo("bnd", version)
			.jsonMapper(tools.jsonMapper())
			.jsonSchemaValidator(ToolAdapter.ACCEPT_ALL)
			.validateToolInputs(false)
			.capabilities(ServerCapabilities.builder()
				.tools(false)
				.build())
			.tools(tools.tools())
			.build();
	}
}
