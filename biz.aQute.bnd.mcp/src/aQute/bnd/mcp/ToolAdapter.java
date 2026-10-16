package aQute.bnd.mcp;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.RecordComponent;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import com.fasterxml.jackson.databind.ObjectMapper;

import aQute.bnd.exceptions.Exceptions;
import io.modelcontextprotocol.json.McpJsonMapper;
import io.modelcontextprotocol.json.jackson2.JacksonMcpJsonMapper;
import io.modelcontextprotocol.json.schema.JsonSchemaValidator;
import io.modelcontextprotocol.server.McpServerFeatures.SyncToolSpecification;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import io.modelcontextprotocol.spec.McpSchema.Tool;

/**
 * Turns plain Java functions over records into MCP tools. The input schema is
 * derived from the input record, arguments are bound to it, and the returned
 * record becomes structured content. This is the only class that knows the MCP
 * tool model; the domain modules never see it.
 */
final class ToolAdapter {

	/** Inputs are bound and checked here, so the SDK does not need networknt. */
	static final JsonSchemaValidator	ACCEPT_ALL	= (schema, content) -> JsonSchemaValidator.ValidationResponse
		.asValid(null);

	private final ObjectMapper			mapper		= new ObjectMapper();
	private final McpJsonMapper			jsonMapper	= new JacksonMcpJsonMapper(mapper);
	private final List<SyncToolSpecification>	tools		= new ArrayList<>();

	<I extends Record> ToolAdapter tool(String name, String description, Class<I> input, Function<I, ?> handler) {
		Tool tool = Tool.builder()
			.name(name)
			.description(description)
			.inputSchema(schema(input))
			.build();
		tools.add(new SyncToolSpecification(tool, (exchange, request) -> call(input, handler, request.arguments())));
		return this;
	}

	List<SyncToolSpecification> tools() {
		return List.copyOf(tools);
	}

	McpJsonMapper jsonMapper() {
		return jsonMapper;
	}

	<I extends Record> CallToolResult call(Class<I> input, Function<I, ?> handler, Map<String, Object> arguments) {
		try {
			I in = mapper.convertValue(arguments == null ? Map.of() : arguments, input);
			List<String> missing = missingRequired(in);
			if (!missing.isEmpty()) {
				return failure("Missing required parameter(s): " + String.join(", ", missing));
			}
			Object out = handler.apply(in);
			boolean failed = out instanceof Diagnostics.Carrier carrier && !carrier.diagnostics()
				.ok();
			return CallToolResult.builder()
				.addTextContent(mapper.writeValueAsString(out))
				.structuredContent(mapper.convertValue(out, Map.class))
				.isError(failed)
				.build();
		} catch (IllegalArgumentException e) {
			return failure("Invalid arguments: " + e.getMessage());
		} catch (Exception e) {
			Throwable cause = Exceptions.unrollCause(e);
			return failure(cause.getClass()
				.getSimpleName() + ": " + cause.getMessage());
		}
	}

	private CallToolResult failure(String message) {
		return CallToolResult.builder()
			.addTextContent(message)
			.isError(true)
			.build();
	}

	private static List<String> missingRequired(Record in) throws ReflectiveOperationException {
		List<String> missing = new ArrayList<>();
		for (RecordComponent component : in.getClass()
			.getRecordComponents()) {
			Param param = component.getAnnotation(Param.class);
			if (param != null && param.required() && isEmpty(component.getAccessor()
				.invoke(in))) {
				missing.add(component.getName());
			}
		}
		return missing;
	}

	private static boolean isEmpty(Object value) {
		return value == null || value instanceof String s && s.isBlank()
			|| value instanceof List<?> l && l.isEmpty();
	}

	static Map<String, Object> schema(Class<? extends Record> input) {
		Map<String, Object> properties = new LinkedHashMap<>();
		List<String> required = new ArrayList<>();
		for (RecordComponent component : input.getRecordComponents()) {
			Map<String, Object> property = new LinkedHashMap<>(type(component.getGenericType()));
			Param param = component.getAnnotation(Param.class);
			if (param != null) {
				property.put("description", param.value());
				if (param.required()) {
					required.add(component.getName());
				}
			}
			properties.put(component.getName(), property);
		}
		Map<String, Object> schema = new LinkedHashMap<>();
		schema.put("type", "object");
		schema.put("properties", properties);
		schema.put("required", required);
		schema.put("additionalProperties", false);
		return schema;
	}

	private static Map<String, Object> type(Type type) {
		if (type == String.class) {
			return Map.of("type", "string");
		}
		if (type == boolean.class || type == Boolean.class) {
			return Map.of("type", "boolean");
		}
		if (type == int.class || type == Integer.class || type == long.class || type == Long.class) {
			return Map.of("type", "integer");
		}
		if (type instanceof ParameterizedType p && p.getRawType() == List.class) {
			return Map.of("type", "array", "items", type(p.getActualTypeArguments()[0]));
		}
		if (type instanceof ParameterizedType p && p.getRawType() == Map.class) {
			return Map.of("type", "object", "additionalProperties", type(p.getActualTypeArguments()[1]));
		}
		throw new IllegalArgumentException("Unsupported tool parameter type " + type);
	}
}
