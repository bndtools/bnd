package test.help;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.Test;

import aQute.bnd.header.Attrs;
import aQute.bnd.header.Parameters;
import aQute.bnd.help.Syntax;
import aQute.bnd.help.instructions.BuilderInstructions;
import aQute.bnd.osgi.Constants;
import aQute.bnd.osgi.Jar;
import aQute.bnd.osgi.Processor;

public class SyntaxTest {

	@Test
	public void testBasicAdding() {
		Syntax syntax = Syntax.HELP.get("-compression");
		assertNotNull(syntax);
		assertEquals("DEFLATE,STORE", syntax.getValues());
	}

	@Test
	public void testTypedPropertyInstruction() throws IOException {
		try (Processor p = new Processor()) {

			BuilderInstructions instructions = p.getInstructions(BuilderInstructions.class);

			assertFalse(instructions.compression()
				.isPresent());

			p.setProperty(Constants.COMPRESSION, "STORE");

			assertEquals(Jar.Compression.STORE, instructions.compression()
				.get());

			p.setProperty(Constants.COMPRESSION, "DEFLATE");
			assertEquals(Jar.Compression.DEFLATE, instructions.compression()
				.get());
		}
	}

	interface TypedParameter {
		String string();

		Optional<String> optionalString();

		List<Integer> numbers();

		Optional<List<Integer>> optionalNumbers();

		int integer();

		Optional<Integer> optionalInteger();

		boolean bool();
	}

	interface TypedParameters {

		Map<String, TypedParameter> typedParameters();

		Optional<Map<String, TypedParameter>> optionalTypedParameters();

		TypedParameters properties();

		Optional<TypedParameter> optionalProperties();

		String string();

		Optional<String> optionalString();

		Attrs attrs();

		Optional<Attrs> optionalAttrs();

		Parameters parameters();

		Optional<Parameters> optionalParameters();

		List<Integer> numbers();

		Optional<List<Integer>> optionalNumbers();

		boolean bool();
	}

	@Test
	public void testTypedParametersInstruction() throws IOException {

		try (Processor p = new Processor()) {
			TypedParameters instructions = p.getInstructions(TypedParameters.class);

			assertNotNull(instructions.properties()
				.numbers());
			assertNotNull(instructions.numbers());

			p.setProperty("-typedParameters",
				"1;string=s1;numbers='1,2,3';integer=42;bool=true, 2;string=s2;numbers='3,2,1';integer=42;bool=true");

			Map<String, TypedParameter> typedParameters = instructions.typedParameters();
			assertEquals(2, typedParameters.size());
			TypedParameter t1 = typedParameters.get("1");
			assertEquals("s1", t1.string());
			assertFalse(t1.optionalString()
				.isPresent());
			assertEquals(42, t1.integer());
			assertFalse(t1.optionalInteger()
				.isPresent());

			assertEquals(Arrays.asList(1, 2, 3), t1.numbers());
			assertTrue(t1.bool());

			assertFalse(instructions.optionalTypedParameters()
				.isPresent());

			p.setProperty("-optionalTypedParameters",
				"1;string=s1;numbers='1,2,3';integer=42;bool=true, 2;string=s2;numbers='3,2,1';integer=43;bool=false");
			assertTrue(instructions.optionalTypedParameters()
				.isPresent());

			Map<String, TypedParameter> optionalTypedParameters = instructions.optionalTypedParameters()
				.get();
			assertEquals(2, optionalTypedParameters.size());

			TypedParameter t2 = optionalTypedParameters.get("2");
			assertEquals("s2", t2.string());
			assertFalse(t2.optionalString()
				.isPresent());
			assertEquals(43, t2.integer());
			assertFalse(t2.optionalInteger()
				.isPresent());

			assertEquals(Arrays.asList(3, 2, 1), t2.numbers());
			assertFalse(t2.bool());

			TypedParameters tp1 = instructions.properties();
			assertNotNull(tp1);
			Optional<TypedParameter> tp2 = instructions.optionalProperties();
			assertFalse(tp2.isPresent());

			p.setProperty("-properties", "string=s1,numbers='1,2,3',integer=42,bool=true");

			TypedParameters tp3 = instructions.properties();
			assertEquals(Arrays.asList(1, 2, 3), tp3.numbers());

			assertTrue(instructions.attrs()
				.isEmpty());

			p.setProperty("-attrs", "string=s1,numbers='1,2,3',integer=42,bool=true");

			Attrs tp4 = instructions.attrs();
			assertNotNull(tp4);
			assertEquals("s1", tp4.get("string"));

			p.setProperty("-parameters", "1;string=s1;numbers='1,2,3';integer=42;bool=true");

			Parameters tp5 = instructions.parameters();
			assertNotNull(tp5);
			assertEquals("s1", tp5.get("1")
				.get("string"));

			p.setProperty("-numbers", "1,2,3");
			p.setProperty("-optionalNumbers", "1,2,3");
			assertEquals(Arrays.asList(1, 2, 3), instructions.numbers());
			assertEquals(Arrays.asList(1, 2, 3), instructions.optionalNumbers()
				.get());

		}
	}

}
