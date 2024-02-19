package biz.aQute.openai.assistant.api;

public interface Tool {
	record ToolInstance(@SuppressWarnings("rawtypes")
	Class type, Object tool, AutoCloseable close) {
	}
	ToolInstance newInstance();
}
