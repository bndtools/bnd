package test;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.regex.Pattern;

import aQute.bnd.header.Parameters;
import aQute.bnd.osgi.Instruction;
import aQute.bnd.osgi.Instructions;
import aQute.libg.glob.AntGlob;
import aQute.libg.glob.Glob;
import junit.framework.TestCase;

public class InstructionTest extends TestCase {

	public void testSelect() {
		assertEquals(Arrays.asList("a", "c"), new Instructions("b").reject(Arrays.asList("a", "b", "c")));
		assertEquals(Arrays.asList("a", "c"), new Instructions("a,c").select(Arrays.asList("a", "b", "c"), false));
		assertEquals(Arrays.asList("a", "c"), new Instructions("!b,*").select(Arrays.asList("a", "b", "c"), false));
	}

	public void testDecorate() {
		Instructions instrs = new Instructions("a;x=1,b*;y=2, literal;n=1, foo.com.example.bar;startlevel=10");
		Parameters params = new Parameters("foo.com.example.bar;version=1, a;v=0, bbb;v=1");
		instrs.decorate(params, true);
		System.out.println(params);
		assertThat(params.get("a")).isNotNull()
			.containsEntry("v", "0")
			.containsEntry("x", "1");

		assertThat(params.get("bbb")).isNotNull()
			.containsEntry("v", "1")
			.containsEntry("y", "2");

		assertThat(params.get("bbb")).isNotNull()
			.containsEntry("v", "1")
			.containsEntry("y", "2");

		assertThat(params.get("foo.com.example.bar")).isNotNull()
			.containsEntry("version", "1")
			.containsEntry("startlevel", "10");

		assertThat(params.get("literal")).isNotNull()
			.containsEntry("n", "1");
	}

	public void testDecoratePriority() {
		Instructions instrs = new Instructions("def;x=1, *;x=0");
		Parameters params = new Parameters("abc, def, ghi");
		instrs.decorate(params);
		System.out.println(params);
		assertThat(params.get("abc")).isNotNull()
			.containsEntry("x", "0");

		assertThat(params.get("def")).isNotNull()
			.containsEntry("x", "1");

		assertThat(params.get("ghi")).isNotNull()
			.containsEntry("x", "0");
	}

	public void testNegate() {
		Instructions instrs = new Instructions("!def, *;x=0");
		Parameters params = new Parameters("abc, def, ghi");
		instrs.decorate(params);
		System.out.println(params);
		assertThat(params.get("abc")).isNotNull()
			.containsEntry("x", "0");

		assertThat(params.get("def")).isNull();

		assertThat(params.get("ghi")).isNotNull()
			.containsEntry("x", "0");
	}

	public void testWildcard() {
		assertTrue(new Instruction("a|b").matches("a"));
		assertTrue(new Instruction("a|b").matches("b"));
		assertTrue(new Instruction("com.foo.*").matches("com.foo"));
		assertTrue(new Instruction("com.foo.*").matches("com.foo.bar"));
		assertTrue(new Instruction("com.foo.*").matches("com.foo.bar.baz"));

		assertTrue(new Instruction("!com.foo.*").matches("com.foo"));
		assertTrue(new Instruction("!com.foo.*").matches("com.foo.bar"));
		assertTrue(new Instruction("!com.foo.*").matches("com.foo.bar.baz"));

		assertTrue(new Instruction("com.foo.*~").matches("com.foo"));
		assertTrue(new Instruction("com.foo.*~").matches("com.foo.bar"));
		assertTrue(new Instruction("com.foo.*~").matches("com.foo.bar.baz"));

		assertTrue(new Instruction("!com.foo.*~").matches("com.foo"));
		assertTrue(new Instruction("!com.foo.*~").matches("com.foo.bar"));
		assertTrue(new Instruction("!com.foo.*~").matches("com.foo.bar.baz"));

		assertTrue(new Instruction("com.foo.*~").isDuplicate());
		assertTrue(new Instruction("!com.foo.*~").isDuplicate());
		assertTrue(new Instruction("!com.foo.*~").isNegated());
	}

	public void testAdvancedPatterns() {
		assertThat(new Instruction("ab*").getPattern()).isEqualTo("ab.*");
		assertThat(new Instruction("ab?").getPattern()).isEqualTo("ab.");
		assertThat(new Instruction("(ab)?").getPattern()).isEqualTo("(ab)?");
		assertTrue(new Instruction("(a){3}").matches("aaa"));
		assertFalse(new Instruction("a{3}").matches("aa"));
		assertTrue(new Instruction("[a]+").matches("aaa"));
		assertFalse(new Instruction("[a]+").matches("x"));

		assertThat(new Instruction("[ab]?").getPattern()).isEqualTo("[ab]?");
		assertThat(new Instruction("[ab]*").getPattern()).isEqualTo("[ab]*");
		assertThat(new Instruction("[ab]+").getPattern()).isEqualTo("[ab]+");
		assertThat(new Instruction("(ab)+").getPattern()).isEqualTo("(ab)+");
	}

	public void testLiteral() {
		assertTrue(new Instruction("literal").isLiteral());
		assertTrue(new Instruction("literal").matches("literal"));
		assertTrue(new Instruction("!literal").matches("literal"));
		assertTrue(new Instruction("=literal").matches("literal"));
		assertTrue(new Instruction("literal~").matches("literal"));
		assertTrue(new Instruction("!literal~").matches("literal"));
		assertTrue(new Instruction("=literal~").matches("literal"));
		assertFalse(new Instruction("=literal").matches(""));
		assertFalse(new Instruction("!literal").matches(""));
		assertFalse(new Instruction("literal").matches(""));
		assertTrue(new Instruction("literal").isLiteral());
		assertTrue(new Instruction("=literal").isLiteral());
		assertTrue(new Instruction("!literal").isNegated());
		assertTrue(new Instruction("!=literal").isNegated());
		assertTrue(new Instruction("=*********").isLiteral());
	}

	public void testPattern() {
		Pattern p = Pattern.compile("com\\.foo(?:\\..*)?");
		Instruction i = new Instruction(p);
		assertThat(i.matches("com.foo")).isTrue();
		assertThat(i.matches("com.foo.bar")).isTrue();
		assertThat(i.matches("com.foo.bar.baz")).isTrue();
		assertThat(i.matches("com.bar")).isFalse();
		assertThat(i.isNegated()).isFalse();

		i = new Instruction(p, true);
		assertThat(i.matches("com.foo")).isTrue();
		assertThat(i.matches("com.foo.bar")).isTrue();
		assertThat(i.matches("com.foo.bar.baz")).isTrue();
		assertThat(i.matches("com.bar")).isFalse();
		assertThat(i.isNegated()).isTrue();
	}

	public void testGlobPattern() {
		Pattern p = Glob.toPattern("com.foo{,.*}");
		Instruction i = new Instruction(p);
		assertThat(i.matches("com.foo")).isTrue();
		assertThat(i.matches("com.foo.bar")).isTrue();
		assertThat(i.matches("com.foo.bar.baz")).isTrue();
		assertThat(i.matches("com.bar")).isFalse();
	}

	public void testAntGlobPattern() {
		Pattern p = AntGlob.toPattern("com/foo/");
		Instruction i = new Instruction(p);
		assertThat(i.matches("com/foo")).isTrue();
		assertThat(i.matches("com/foo/")).isTrue();
		assertThat(i.matches("com/foo/bar")).isTrue();
		assertThat(i.matches("com/foo/bar/baz")).isTrue();
		assertThat(i.matches("com/bar")).isFalse();
	}
}
