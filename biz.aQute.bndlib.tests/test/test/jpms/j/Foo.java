package test.jpms.j;

import java.lang.reflect.Type;
import java.util.Collections;

import javax.enterprise.inject.spi.Extension;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.bind.JsonbBuilder;
import javax.json.bind.serializer.DeserializationContext;
import javax.json.stream.JsonParser;

public class Foo implements Extension {

	public JsonObject toJSON() {
		return Json.createObjectBuilder()
			.add("foo", "bar")
			.build();
	}

	public String toJSONB() {
		return JsonbBuilder.create()
			.toJson(Collections.singletonMap("foo", "bar"));
	}

	@SuppressWarnings("unused")
	private DeserializationContext deserializationContext = new DeserializationContext() {
		@Override
		public <T> T deserialize(Class<T> arg0, JsonParser arg1) {
			return null;
		}

		@Override
		public <T> T deserialize(Type arg0, JsonParser arg1) {
			return null;
		}
	};

}
