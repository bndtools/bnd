package org.bndtools.refactor.types;

import java.util.List;

import org.bndtools.refactor.types.RefactorTestUtil.Scenario;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import aQute.lib.collections.ExtList;

class ComponentRefactorerTest {

	@ParameterizedTest
	@MethodSource("scenarios")
	void testGogoRefactoring(Scenario s) throws Exception {
		RefactorTestUtil<LiteralRefactorer> test = new RefactorTestUtil<>(new ComponentRefactorer());
		test.testRefactoring(s);
	}

	static List<Scenario> scenarios() {
		String empty = """
			import com.example.Foo;
			import com.example.Bar;
			class F {
			  Foo ref;
			  F(  String service){
			  }
			  void activate(){
			  }
			  void deactivate(){
			  }
			  void setService(  Bar service){
			  }
			  void unsetService(  Bar service){
			  }
			}
			""";
		String emptyPublic = """
			import com.example.Foo;
			import com.example.Bar;
			class F {
			  Foo ref;
			  public F(  String service){
			  }
			  void activate(){
			  }
			  void deactivate(){
			  }
			  void setService(  Bar service){
			  }
			  void unsetService(  Bar service){
			  }
			}
			""";
		String C = """
			import com.example.Foo;
			import com.example.Bar;
			import org.osgi.service.component.annotations.Component;
			@Component class F {
			  Foo ref;
			  F(  String service){
			  }
			  void activate(){
			  }
			  void deactivate(){
			  }
			  void setService(  Bar service){
			  }
			  void unsetService(  Bar service){
			  }
			}
			""";
		String Cpublic = """
			import com.example.Foo;
			import com.example.Bar;
			import org.osgi.service.component.annotations.Component;
			@Component class F {
			  Foo ref;
			  public F(  String service){
			  }
			  void activate(){
			  }
			  void deactivate(){
			  }
			  void setService(  Bar service){
			  }
			  void unsetService(  Bar service){
			  }
			}
			""";
		String CAc = """
			import com.example.Foo;
			import com.example.Bar;
			import org.osgi.service.component.annotations.Component;
			import org.osgi.service.component.annotations.Activate;
			@Component class F {
			  Foo ref;
			  @Activate public F(  String service){
			  }
			  void activate(){
			  }
			  void deactivate(){
			  }
			  void setService(  Bar service){
			  }
			  void unsetService(  Bar service){
			  }
			}
			""";
		String CAcRp = """
			import com.example.Foo;
			import com.example.Bar;
			import org.osgi.service.component.annotations.Component;
			import org.osgi.service.component.annotations.Activate;
			import org.osgi.service.component.annotations.Reference;
			@Component class F {
			  Foo ref;
			  @Activate public F(  @Reference String service){
			  }
			  void activate(){
			  }
			  void deactivate(){
			  }
			  void setService(  Bar service){
			  }
			  void unsetService(  Bar service){
			  }
			}
			""";
		String CAm = """
			import com.example.Foo;
			import com.example.Bar;
			import org.osgi.service.component.annotations.Component;
			import org.osgi.service.component.annotations.Activate;
			@Component class F {
			  Foo ref;
			  public F(  String service){
			  }
			  @Activate void activate(){
			  }
			  void deactivate(){
			  }
			  void setService(  Bar service){
			  }
			  void unsetService(  Bar service){
			  }
			}
			""";
		String CAmDm = """
			import com.example.Foo;
			import com.example.Bar;
			import org.osgi.service.component.annotations.Component;
			import org.osgi.service.component.annotations.Activate;
			import org.osgi.service.component.annotations.Deactivate;
			@Component class F {
			  Foo ref;
			  public F(  String service){
			  }
			  @Activate void activate(){
			  }
			  @Deactivate void deactivate(){
			  }
			  void setService(  Bar service){
			  }
			  void unsetService(  Bar service){
			  }
			}
			""";
		String CAmDmRm = """
			import com.example.Foo;
			import com.example.Bar;
			import org.osgi.service.component.annotations.Component;
			import org.osgi.service.component.annotations.Activate;
			import org.osgi.service.component.annotations.Deactivate;
			import org.osgi.service.component.annotations.Reference;
			@Component class F {
			  Foo ref;
			  public F(  String service){
			  }
			  @Activate void activate(){
			  }
			  @Deactivate void deactivate(){
			  }
			  @Reference void setService(  Bar service){
			  }
			  void unsetService(  Bar service){
			  }
			}
			""";
		return new ExtList<>(
		//@formatter:off

			new Scenario(CAc, Cpublic, "F\\(" 	            	    	, "comp.act-"),
			new Scenario(empty, C, "ref"             	       			, "comp+"),
			new Scenario(C, empty, "ref"             	       			, "comp-"),
			new Scenario(C, CAc, "String service"             	       	, "comp.act+"),
			new Scenario(CAc, CAm, "activate" 	            	       	, "comp.act+"),
			new Scenario(CAc, CAcRp, "String service"             	    , "comp.ref+"),
			new Scenario(CAcRp, CAc, "String service"             	    , "comp.ref-"),
			new Scenario(CAm, CAmDm, "deactivate"             	    	, "comp.deact+"),
			new Scenario(CAmDm, CAm, "deactivate"             	    	, "comp.deact-"),
			new Scenario(CAmDm, CAmDmRm, "setService"             	    , "comp.ref+"),
			new Scenario(CAmDmRm, emptyPublic, "setService"           	, "comp-")

    	//@formatter:on
		);
	}

}
