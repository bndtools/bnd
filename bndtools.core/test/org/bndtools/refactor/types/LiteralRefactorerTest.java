package org.bndtools.refactor.types;

import java.util.List;

import org.bndtools.refactor.types.RefactorTestUtil.Scenario;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import aQute.lib.collections.ExtList;

class LiteralRefactorerTest {

	@ParameterizedTest
	@MethodSource("integers")
	void testIntegerRefactoring(Scenario s) throws Exception {
		RefactorTestUtil<LiteralRefactorer> test = new RefactorTestUtil<>(new LiteralRefactorer());
		test.testRefactoring(s);
	}

	static List<Scenario> integers() {
		String dec = """
			class Foo {
			  long v=12345678900000L;
			}
			""";
		String hex = """
			class Foo {
			  long v=0x0b3a_73ce_2b20L;
			}
			""";
		String bin = """
			class Foo {
			  long v=0b1011_0011_1010_0111_0011_1100_1110_0010_1011_0010_0000L;
			}
			""";
		String dec_ = """
			class Foo {
			  long v=12_345_678_900_000L;
			}
			""";
		String dec_w = """
			class Foo {
			  long v=12_345_678_90000_0L;
			}
			""";
		String dbl = """
			class Foo {
			  double v=1e10;
			}
			""";
		return new ExtList<>(
		//@formatter:off

			new Scenario(dbl, dbl, "1e()10"             	       , null),
			new Scenario(dec, hex, "789()0"             	       , "lit.nmbr.dec.hex"),
			new Scenario(dec, bin, "789()0"             	       , "lit.nmbr.dec.bin"),
			new Scenario(bin, dec, "0_()"  	            	       , "lit.nmbr.bin.dec"),
			new Scenario(hex, dec, "73ce()_"             	       , "lit.nmbr.hex.dec"),
			new Scenario(dec, dec_, "789()"             	       , "lit.nmbr._+"),
			new Scenario(dec_, dec, "678()"             	       , "lit.nmbr._-"),
			new Scenario(dec_w, dec_, "9000()"             	       , "lit.nmbr._+")

    	//@formatter:on
		);
	}

	@ParameterizedTest
	@MethodSource("versions")
	void testVersionScenarios(Scenario s) throws Exception {
		RefactorTestUtil<LiteralRefactorer> test = new RefactorTestUtil<>(new LiteralRefactorer());
		test.testRefactoring(s);
	}

	static List<Scenario> versions() {
		String base = """
			class Foo {
			  String v="1.2.3";
			}
			""";
		String base_major = """
			class Foo {
			  String v="2.0.0";
			}
			""";
		String base_minor = """
			class Foo {
			  String v="1.3.0";
			}
			""";
		String base_qualifier = """
			class Foo {
			  String v="1.2.3.foobar";
			}
			""";

		return new ExtList<>(
		//@formatter:off
			new Scenario(base_qualifier, base, "1.()2"             	   , "lit.vers.qual-"),
			new Scenario(base, base_major, "1.()2"             	       , "lit.vers.majr"),
			new Scenario(base, base_minor, "1.()2"             	       , "lit.vers.minr")
    	//@formatter:on
		);
	}

	@ParameterizedTest
	@MethodSource("finalScenarios")
	void testFinalModifier(Scenario s) throws Exception {
		RefactorTestUtil<LiteralRefactorer> test = new RefactorTestUtil<>(new LiteralRefactorer());
		test.testRefactoring(s);
	}

	static List<Scenario> finalScenarios() {
		String base = """
			class Foo {
			  private String v="1.2.3";
			}
			""";
		String base_final = """
			class Foo {
			  final String v="1.2.3";
			}
			""";

		return new ExtList<>(
		//@formatter:off
			new Scenario(base, base_final, "riva"             	   , "lit.final")
    	//@formatter:on
		);
	}

	@ParameterizedTest
	@MethodSource("stringliterals")
	void testStringLiterals(Scenario s) throws Exception {
		RefactorTestUtil<LiteralRefactorer> test = new RefactorTestUtil<>(new LiteralRefactorer());
		test.testRefactoring(s);
	}

	static List<Scenario> stringliterals() {
		String abc = """
			class Foo {
			  String v="abc";
			}
			""";
		String ABC = """
			class Foo {
			  String v="ABC";
			}
			""";
		String abc_block = """
			class Foo {
			  String v=\"\"\"
			        abc\"\"\";
			}
			""";
		String ABC_block = """
			class Foo {
			  String v=\"\"\"
			        ABC\"\"\";
			}
			""";
		String verylongstring = """
			class Foo {
			  String v=\"\"\"
			    The quick brown fox jumped over the lazy dog and had no way to go. Fortunately there was an old lady passing by that had a compassionate heart.
			 \"\"\";
			}
			""";
		String verylongstring_wrapped = """
			class Foo {
			  String v=\"\"\"
			           The quick brown fox jumped over the lazy dog and had no
			           way to go. Fortunately there was an old lady passing by
			           that had a compassionate heart.
			        \"\"\";
			}
			""";

		return new ExtList<>(
	//@formatter:off
			new Scenario(verylongstring, verylongstring_wrapped, "quick"             	   , "lit.blck.wrap.60"),
			new Scenario(abc, ABC, "a(bc)"             	   , "lit.strn.uppr"),
			new Scenario(abc, ABC, "abc"             	   , "lit.strn.uppr"),
			new Scenario(ABC, abc, "ABC"             	   , "lit.strn.lowr"),
			new Scenario(abc_block, ABC_block, "abc"       , "lit.blck.uppr"),
			new Scenario(ABC_block, abc_block, "ABC"       , "lit.blck.lowr"),
			new Scenario(abc_block, abc, "abc"             , "lit.blck.strn"),
			new Scenario(abc, abc_block, "abc"             , "lit.strn.blck")
    	//@formatter:on
		);
	}

}
