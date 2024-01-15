package org.bndtools.refactor.types;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.bndtools.refactor.util.Cursor;
import org.bndtools.refactor.util.ProposalBuilder;
import org.bndtools.refactor.util.ProposalBuilder.Proposal;
import org.bndtools.refactor.util.RefactorAssistant;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import aQute.lib.collections.ExtList;

class GogoRefactorerTest {

	/**
	 * naming is DPF_D is Descriptor, Parameter, Flag on command, Descriptor on
	 * secondary. Lower case means NO.
	 */
	final static String	gd_dpf_d	= """
		public class Foo {
		  public String command(  String s,  boolean flag,  int some){
		    return "";
		  }
		  public String secondary(){
		    return "";
		  }
		}
		""";
	final static String	Gd_dpf_d	= """
		import org.apache.felix.service.command.annotations.GogoCommand;
		@GogoCommand(scope="foo",function={}) public class Foo {
		  public String command(  String s,  boolean flag,  int some){
		    return "";
		  }
		  public String secondary(){
		    return "";
		  }
		}
		""";
	final static String	GD_dpf_d	= """
		import org.apache.felix.service.command.Descriptor;
		import org.apache.felix.service.command.annotations.GogoCommand;
		@GogoCommand(scope="foo",function={}) @Descriptor("foo - Foo") public class Foo {
		  public String command(  String s,  boolean flag,  int some){
		    return "";
		  }
		  public String secondary(){
		    return "";
		  }
		}
				""";
	final static String	Gd_Dpf_d	= """
		import org.apache.felix.service.command.annotations.GogoCommand;
		import org.apache.felix.service.command.Descriptor;
		@GogoCommand(scope="foo",function="command") public class Foo {
		  @Descriptor("command") public String command(  String s,  boolean flag,  int some){
		    return "";
		  }
		  public String secondary(){
		    return "";
		  }
		}
				""";
	final static String	Gd_DpF_d	= """
		import org.apache.felix.service.command.annotations.GogoCommand;
		import org.apache.felix.service.command.Descriptor;
		import org.apache.felix.service.command.Parameter;
		@GogoCommand(scope="foo",function="command") public class Foo {
		  @Descriptor("command") public String command(  String s,  @Descriptor("flag") @Parameter(absentValue="false",presentValue="true",names={"-f","--flag"}) boolean flag,  int some){
		    return "";
		  }
		  public String secondary(){
		    return "";
		  }
		}
		""";

	final static String	Gd_DPF_d	= """
		import org.apache.felix.service.command.annotations.GogoCommand;
		import org.apache.felix.service.command.Descriptor;
		import org.apache.felix.service.command.Parameter;
		@GogoCommand(scope="foo",function="command") public class Foo {
		  @Descriptor("command") public String command(  String s,  @Descriptor("flag") @Parameter(absentValue="false",presentValue="true",names={"-f","--flag"}) boolean flag,  @Descriptor("some") @Parameter(absentValue="",names={"-s","--some"}) int some){
		    return "";
		  }
		  public String secondary(){
		    return "";
		  }
		}
		""";

	final static String	Gd_Dpf_D	= """
		import org.apache.felix.service.command.annotations.GogoCommand;
		import org.apache.felix.service.command.Descriptor;
		@GogoCommand(scope="foo",function={"command","secondary"}) public class Foo {
		  @Descriptor("command") public String command(  String s,  boolean flag,  int some){
		    return "";
		  }
		  @Descriptor("secondary") public String secondary(){
		    return "";
		  }
		}
		""";

	record Scenario(String source, String target, String selector, String proposal) {}

	@ParameterizedTest
	@MethodSource("scenarios")
	void testGogoRefactoring(Scenario s) throws Exception {
		GogoRefactorer gf = new GogoRefactorer();
		RefactorAssistant assistant = new RefactorAssistant(s.source);
		assertThat(s.source).isEqualTo(assistant.getCompilationUnit()
			.toString());

		ProposalBuilder proposalBuilder = new ProposalBuilder(assistant, false);
		Cursor<?> cursor = assistant.getCursor(s.selector);

		assertThat(cursor.getNode()).isPresent();

		gf.addCompletions(proposalBuilder, assistant, cursor, null);
		Proposal p = proposalBuilder.getProposal(s.proposal)
			.orElseThrow();

		p.complete()
			.accept();
		assistant.fixup();
		IDocument d = new Document(s.source);
		assistant.apply(d, null);
		RefactorAssistant rr = new RefactorAssistant(d.get());
		assertThat(rr.getCompilationUnit()
			.toString()).isEqualTo(s.target);
	}

	static List<Scenario> scenarios() {
		return new ExtList<>(
		//@formatter:off

			new Scenario(Gd_DpF_d, Gd_DPF_d, "int ()some"             	       , "gogo.parm+"),
			new Scenario(Gd_DPF_d, Gd_DpF_d, "int ()some"             	       , "gogo.parm-"),
			new Scenario(Gd_Dpf_d, Gd_DpF_d, "boolean ()flag"             	   , "gogo.parm+"),
			new Scenario(Gd_DpF_d, Gd_Dpf_d, "boolean ()flag"             	   , "gogo.parm-"),
			new Scenario(gd_dpf_d, GD_dpf_d, "String co(mma)nd\\("             , "gogo.scope+"),
			new Scenario(Gd_dpf_d, gd_dpf_d, "String co(mma)nd\\("             , "gogo.scope-"),
			new Scenario(Gd_Dpf_D, gd_dpf_d, "String co(mma)nd\\("             , "gogo.scope-"),
			new Scenario(Gd_dpf_d, Gd_Dpf_d, "String co(mma)nd\\("             , "gogo.cmd.desc+"),
			new Scenario(Gd_dpf_d, Gd_Dpf_d, "String com()mand\\("             , "gogo.cmd.desc+"),
			new Scenario(Gd_dpf_d, Gd_Dpf_d, "String command\\("               , "gogo.cmd.desc+"),
			new Scenario(Gd_Dpf_d, Gd_dpf_d, "String com()mand\\("             , "gogo.cmd.desc-"),
			new Scenario(Gd_Dpf_D, Gd_Dpf_d, "String sec()ondary\\("           , "gogo.cmd.desc-"),
			new Scenario(Gd_Dpf_d, Gd_Dpf_D, "String sec()ondary\\("           , "gogo.cmd.desc+")

    	//@formatter:on
		);
	}
}
