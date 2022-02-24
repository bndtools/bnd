package test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;

import aQute.bnd.header.Parameters;
import aQute.bnd.osgi.Instruction;
import aQute.bnd.osgi.Instructions;
import aQute.bnd.osgi.Processor;
import aQute.libg.glob.AntGlob;
import aQute.libg.glob.Glob;

public class InstructionTest {

	@Test
	public void testSelect() {
		assertEquals(Arrays.asList("a", "c"), new Instructions("b").reject(Arrays.asList("a", "b", "c")));
		assertEquals(Arrays.asList("a", "c"), new Instructions("a,c").select(Arrays.asList("a", "b", "c"), false));
		assertEquals(Arrays.asList("a", "c"), new Instructions("!b,*").select(Arrays.asList("a", "b", "c"), false));
	}

	@Test
	public void buildpath_decoration() throws Exception {
		try (Processor p = new Processor()) {
			p.setProperty("maven.target.version", "3.3.9");
			p.setProperty("-buildpath+.maven",
				"org.apache.maven:*;version=${maven.target.version};maven-scope=provided");
			p.setProperty("-buildpath",
				"osgi.annotation;version=latest;maven-scope=provided,org.osgi.dto;version='1.0',org.osgi.resource;version='1.0',org.osgi.framework;version='1.8',org.osgi.service.repository;version=latest,org.osgi.util.function;version=latest,org.osgi.util.promise;version=latest,aQute.libg;version=project;packages=\"!aQute.lib.exceptions.*,*\",biz.aQute.bnd.util;version=latest,biz.aQute.bndlib;version=latest,biz.aQute.resolve;version=latest,biz.aQute.repository;version=latest,org.eclipse.m2e.maven.runtime,org.apache.maven:maven-artifact,org.apache.maven:maven-core,org.apache.maven:maven-model,org.apache.maven:maven-plugin-api,org.apache.maven:maven-repository-metadata,org.apache.maven:maven-settings,org.codehaus.plexus:plexus-utils,org.eclipse.aether.api;version=1.0.2.v20150114,org.slf4j.api;version=latest");
			Parameters bundles = p.getMergedParameters("-buildpath");
			assertThat(bundles.get("org.apache.maven:maven-artifact")).isEmpty();
			assertThat(bundles.get("org.apache.maven:maven-core")).isEmpty();
			assertThat(bundles.get("org.apache.maven:maven-model")).isEmpty();
			assertThat(bundles.get("org.apache.maven:maven-plugin-api")).isEmpty();
			assertThat(bundles.get("org.apache.maven:maven-repository-metadata")).isEmpty();
			assertThat(bundles.get("org.apache.maven:maven-settings")).isEmpty();

			Instructions decorator = new Instructions(p.mergeProperties("-buildpath" + "+"));
			decorator.decorate(bundles, true);
			System.out.println(bundles);
			assertThat(bundles.get("org.apache.maven:maven-artifact")).containsOnly(entry("version", "3.3.9"),
				entry("maven-scope", "provided"));
			assertThat(bundles.get("org.apache.maven:maven-core")).containsOnly(entry("version", "3.3.9"),
				entry("maven-scope", "provided"));
			assertThat(bundles.get("org.apache.maven:maven-model")).containsOnly(entry("version", "3.3.9"),
				entry("maven-scope", "provided"));
			assertThat(bundles.get("org.apache.maven:maven-plugin-api")).containsOnly(entry("version", "3.3.9"),
				entry("maven-scope", "provided"));
			assertThat(bundles.get("org.apache.maven:maven-repository-metadata"))
				.containsOnly(entry("version", "3.3.9"), entry("maven-scope", "provided"));
			assertThat(bundles.get("org.apache.maven:maven-settings")).containsOnly(entry("version", "3.3.9"),
				entry("maven-scope", "provided"));

		}
	}

	@Test
	public void testDecorate() {
		Instructions instrs = new Instructions("a;x=1,b*;y=2, literal;n=1, foo.com.example.bar;startlevel=10");
		Parameters params = new Parameters("foo.com.example.bar;version=1, a;v=0, bbb;v=1");
		instrs.decorate(params, true);
		System.out.println(params);
		assertThat(params.keySet()).containsExactly("foo.com.example.bar", "a", "bbb", "literal");

		assertThat(params.get("a"))
			.containsOnly(entry("v", "0"), entry("x", "1"));

		assertThat(params.get("bbb"))
			.containsOnly(entry("v", "1"), entry("y", "2"));

		assertThat(params.get("foo.com.example.bar"))
			.containsOnly(entry("version", "1"), entry("startlevel", "10"));

		assertThat(params.get("literal"))
			.containsOnly(entry("n", "1"));
	}

	@Test
	public void unused_literals_at_start() {
		Instructions instrs = new Instructions("=!literal.*;n=1, {a};x=1,b*;y=2, foo.com.example.bar;startlevel=10");
		Parameters params = new Parameters("foo.com.example.bar;version=1, a;v=0, bbb;v=1");
		instrs.decorate(params, true);
		System.out.println(params);
		assertThat(params.keySet()).containsExactly("!literal.*", "foo.com.example.bar", "a", "bbb");

		assertThat(params.get("a"))
			.containsOnly(entry("v", "0"), entry("x", "1"));

		assertThat(params.get("bbb"))
			.containsOnly(entry("v", "1"), entry("y", "2"));

		assertThat(params.get("foo.com.example.bar"))
			.containsOnly(entry("version", "1"), entry("startlevel", "10"));

		assertThat(params.get("!literal.*"))
			.containsOnly(entry("n", "1"));
	}

	@Test
	public void unused_literals_only() {
		Instructions instrs = new Instructions("=!literal, literal2;n=1");
		Parameters params = new Parameters("foo.com.example.bar;version=1, a;v=0, bbb;v=1");
		instrs.decorate(params, true);
		System.out.println(params);
		assertThat(params.keySet()).containsExactly("!literal", "literal2", "foo.com.example.bar", "a", "bbb");

		assertThat(params.get("a")).containsOnly(entry("v", "0"));

		assertThat(params.get("bbb")).containsOnly(entry("v", "1"));

		assertThat(params.get("bbb")).containsOnly(entry("v", "1"));

		assertThat(params.get("foo.com.example.bar")).containsOnly(entry("version", "1"));

		assertThat(params.get("!literal")).isEmpty();
		assertThat(params.get("literal2")).containsOnly(entry("n", "1"));
	}

	@Test
	public void unused_literals_split() {
		Instructions instrs = new Instructions("=!literal.*,!unused.negated, *,{unused.nonliteral}, literal2;n=1");
		Parameters params = new Parameters("foo.com.example.*;version=1, a;v=0, bbb;v=1");
		instrs.decorate(params, true);
		System.out.println(params);
		assertThat(params.keySet()).containsExactly("!literal.*", "foo.com.example.*", "a", "bbb", "literal2");

		assertThat(params.get("a")).containsOnly(entry("v", "0"));

		assertThat(params.get("bbb")).containsOnly(entry("v", "1"));

		assertThat(params.get("bbb")).containsOnly(entry("v", "1"));

		assertThat(params.get("foo.com.example.*")).containsOnly(entry("version", "1"));

		assertThat(params.get("!literal.*")).isEmpty();
		assertThat(params.get("literal2")).containsOnly(entry("n", "1"));
	}

	@Test
	public void duplicates() {
		Instructions instrs = new Instructions("a;x=1,b*;y=2, literal;n=1, foo.com.example.bar;startlevel=10");
		Parameters params = new Parameters("foo.com.example.bar;version=1, a;v=0, bbb;v=1, a;k=1, a;z=9", null, true);
		instrs.decorate(params, true);
		System.out.println(params);
		assertThat(params.keySet()).containsExactly("foo.com.example.bar", "a", "bbb", "a~", "a~~", "literal");

		assertThat(params.get("a")).containsOnly(entry("v", "0"), entry("x", "1"));
		assertThat(params.get("a~")).containsOnly(entry("k", "1"), entry("x", "1"));
		assertThat(params.get("a~~")).containsOnly(entry("z", "9"), entry("x", "1"));

		assertThat(params.get("bbb")).containsOnly(entry("v", "1"), entry("y", "2"));

		assertThat(params.get("foo.com.example.bar")).containsOnly(entry("version", "1"), entry("startlevel", "10"));

		assertThat(params.get("literal")).containsOnly(entry("n", "1"));
	}

	@Test
	public void removal() {
		Instructions instrs = new Instructions("a;x=1;k=!;q=!,b*;y=2, literal;n=1, foo.com.example.bar;startlevel=10");
		Parameters params = new Parameters("foo.com.example.bar;version=1, a;v=0, bbb;v=1, a;k=1, a;z=9", null, true);
		instrs.decorate(params, true);
		System.out.println(params);
		assertThat(params.keySet()).containsExactly("foo.com.example.bar", "a", "bbb", "a~", "a~~", "literal");

		assertThat(params.get("a")).containsOnly(entry("v", "0"), entry("x", "1"));
		assertThat(params.get("a~")).containsOnly(entry("x", "1"));
		assertThat(params.get("a~~")).containsOnly(entry("z", "9"), entry("x", "1"));

		assertThat(params.get("bbb")).containsOnly(entry("v", "1"), entry("y", "2"));

		assertThat(params.get("foo.com.example.bar")).containsOnly(entry("version", "1"), entry("startlevel", "10"));

		assertThat(params.get("literal")).containsOnly(entry("n", "1"));
	}

	@Test
	public void no_overwrite() {
		Instructions instrs = new Instructions("a;x=1;~k=4,b*;y=2, literal;n=1, foo.com.example.bar;startlevel=10");
		Parameters params = new Parameters("foo.com.example.bar;version=1, a;x=0, bbb;v=1, a;k=1, a;z=9", null, true);
		instrs.decorate(params, true);
		System.out.println(params);
		assertThat(params.keySet()).containsExactly("foo.com.example.bar", "a", "bbb", "a~", "a~~", "literal");

		assertThat(params.get("a")).containsOnly(entry("x", "1"), entry("k", "4"));
		assertThat(params.get("a~")).containsOnly(entry("k", "1"), entry("x", "1"));
		assertThat(params.get("a~~")).containsOnly(entry("z", "9"), entry("x", "1"), entry("k", "4"));

		assertThat(params.get("bbb")).containsOnly(entry("v", "1"), entry("y", "2"));

		assertThat(params.get("foo.com.example.bar")).containsOnly(entry("version", "1"), entry("startlevel", "10"));

		assertThat(params.get("literal")).containsOnly(entry("n", "1"));
	}

	@Test
	public void testDecoratePriority() {
		Instructions instrs = new Instructions("def;x=1, *;x=0");
		Parameters params = new Parameters("abc, def, ghi");
		instrs.decorate(params);
		System.out.println(params);
		assertThat(params.keySet()).containsExactly("abc", "def", "ghi");

		assertThat(params.get("abc")).containsOnly(entry("x", "0"));

		assertThat(params.get("def")).containsOnly(entry("x", "1"));

		assertThat(params.get("ghi")).containsOnly(entry("x", "0"));
	}

	@Test
	public void testNegate() {
		Instructions instrs = new Instructions("!def, *;x=0");
		Parameters params = new Parameters("abc, def, ghi");
		instrs.decorate(params);
		System.out.println(params);
		assertThat(params.keySet()).containsExactly("abc", "ghi");

		assertThat(params.get("abc")).containsOnly(entry("x", "0"));

		assertThat(params.get("ghi")).containsOnly(entry("x", "0"));
	}

	@Test
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

	@Test
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

	@Test
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

	@Test
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

	@Test
	public void testGlobPattern() {
		Pattern p = Glob.toPattern("com.foo{,.*}");
		Instruction i = new Instruction(p);
		assertThat(i.matches("com.foo")).isTrue();
		assertThat(i.matches("com.foo.bar")).isTrue();
		assertThat(i.matches("com.foo.bar.baz")).isTrue();
		assertThat(i.matches("com.bar")).isFalse();
	}

	@Test
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
