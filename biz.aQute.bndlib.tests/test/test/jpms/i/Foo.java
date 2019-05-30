package test.jpms.i;

import java.util.Collections;

import javax.enterprise.inject.spi.Extension;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.bind.JsonbBuilder;

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
}
