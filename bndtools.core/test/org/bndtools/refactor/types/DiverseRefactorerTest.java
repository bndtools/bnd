package org.bndtools.refactor.types;

import java.util.List;

import org.bndtools.refactor.types.RefactorTestUtil.Scenario;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import aQute.lib.collections.ExtList;

class DiverseRefactorerTest {

	public static String	none_before	= """
		class Foo {
		  Foo(  int x){
		  }
		}
		""";
	public static String	one_after		= """
		class Foo {
		  final int x;
		  Foo(  int x){
		    this.x=x;
		  }
		}
		""";

	public static String	one_before	= """
		class Foo {
		  final double d;
		  Foo(  int x){
		  }
		}
		""";
	public static String	two_after	= """
		class Foo {
		  final double d;
		  final int x;
		  Foo(  int x){
		    this.x=x;
		  }
		}
		""";

	public static String	duplicate_name_before	= """
		class Foo {
		  final int x;
		  Foo(  int x){
		  }
		}
		""";
	public static String	duplicate_name_after	= """
		class Foo {
		  final int x;
		  final int x1;
		  Foo(  int x){
		    this.x1=x;
		  }
		}
		""";

	@ParameterizedTest
	@MethodSource("scenarios")
	void testDiverseRefactoring(Scenario s) throws Exception {
		RefactorTestUtil<DiverseRefactorer> test = new RefactorTestUtil<>(new DiverseRefactorer());
		test.testRefactoring(s);
	}

	static List<Scenario> scenarios() {
		return new ExtList<>(
		//@formatter:off

			new Scenario(duplicate_name_before, duplicate_name_after, "int ()x\\)", "div.constr.final"),
			new Scenario(none_before, one_after, "int ()x"             	       , "div.constr.final"),
			new Scenario(one_before, two_after, "int ()x"             	       , "div.constr.final")

    	//@formatter:on
		);
	}

}
