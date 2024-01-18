package org.bndtools.refactor.types;

import static aQute.libg.re.Catalog.re;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.bndtools.refactor.util.BaseRefactorer;
import org.bndtools.refactor.util.Cursor;
import org.bndtools.refactor.util.DomainBase;
import org.bndtools.refactor.util.JavaModifier;
import org.bndtools.refactor.util.JavaSourceType;
import org.bndtools.refactor.util.ProposalBuilder;
import org.bndtools.refactor.util.RefactorAssistant;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.ui.text.java.IInvocationContext;
import org.eclipse.jdt.ui.text.java.IQuickFixProcessor;
import org.osgi.dto.DTO;
import org.osgi.service.component.annotations.Component;

import aQute.libg.re.RE;

/**
 * Refactoring for Gogo
 */
@Component
public class GogoRefactorer extends BaseRefactorer implements IQuickFixProcessor {
	public final static RE		SINGLE_CHAR_P	= re("-.");
	public final static String	GOGOCOMMAND_A	= "org.apache.felix.service.command.annotations.GogoCommand";
	public final static String	DESCRIPTOR_A	= "org.apache.felix.service.command.Descriptor";
	public final static String	PARAMETER_A		= "org.apache.felix.service.command.Parameter";

	public static class ParameterDTO extends DTO {
		public String	absentValue;
		public String	presentValue;
		public String[]	names;
	}

	public static class GogoCommandDTO extends DTO {
		public String	scope;
		public String[]	function;
	}

	public static class DescriptorDTO extends DTO {
		public String value;
	}

	class Base<T extends ASTNode> extends DomainBase<T> {
		final DescriptorDTO	descriptor;

		Base(Cursor<T> cursor) {
			super(cursor);
			RefactorAssistant assistant = cursor.getAssistant();
			this.descriptor = assistant.getAnnotationDTO(node, DESCRIPTOR_A, DescriptorDTO.class)
				.orElse(null);
		}
	}

	class Par extends Base<SingleVariableDeclaration> {
		String			name;
		ParameterDTO	parameter;

		Par(Cursor<SingleVariableDeclaration> cursor) {
			super(cursor);
			RefactorAssistant assistant = cursor.getAssistant();
			this.name = assistant.getIdentifier(node);
			this.parameter = assistant.getAnnotationDTO(node, PARAMETER_A, ParameterDTO.class)
				.orElse(null);
		}

		char character() {
			if (parameter == null || parameter.names == null || parameter.names.length == 0)
				return 0;

			for (String s : parameter.names) {
				if (SINGLE_CHAR_P.matches(s)
					.isPresent()) {
					return s.charAt(1);
				}
			}
			return 0;
		}

		void delete() {
			cursor.forEach((ass, svd) -> {
				ass.deleteAnnotation(svd, DESCRIPTOR_A);
				ass.deleteAnnotation(svd, PARAMETER_A);
			});
		}

	}

	class Command extends Base<MethodDeclaration> {
		final Map<String, Par>	pars	= new HashMap<>();
		final String			name;

		Command(Cursor<MethodDeclaration> cursor) {
			super(cursor);
			name = cursor.getAssistant()
				.getIdentifier(node);
			cursor.downTo(SingleVariableDeclaration.class)
				.processSingletons(Par::new)
				.forEach(par -> {
					pars.put(par.name, par);
				});
		}

		void delete() {
			cursor.forEach((ass, md) -> {
				ass.deleteAnnotation(md, DESCRIPTOR_A);
			});
		}

		public void ensure() {
			cursor.forEach((ass, md) -> {
				DescriptorDTO descriptor = new DescriptorDTO();
				descriptor.value = md.getName()
					.getIdentifier();
				ass.ensureModifiers(md, JavaModifier.PUBLIC);
				ass.ensureAnnotation(md, ass.newAnnotation(DESCRIPTOR_A, descriptor));
			});
		}
	}

	class GogoState extends Base<TypeDeclaration> {

		final TypeDeclaration					node;
		final GogoCommandDTO					gogo;
		final Map<MethodDeclaration, Command>	commands;
		final DescriptorDTO						descriptor;

		GogoState(Cursor<TypeDeclaration> cursor) {
			super(cursor);
			RefactorAssistant assistant = cursor.getAssistant();
			node = cursor.getNode()
				.get();

			this.descriptor = assistant.getAnnotationDTO(node, DESCRIPTOR_A, DescriptorDTO.class)
				.orElse(null);
			this.gogo = assistant.getAnnotationDTO(node, GOGOCOMMAND_A, GogoCommandDTO.class)
				.orElse(null);

			commands = cursor.downTo(MethodDeclaration.class)
				.hasModifier(JavaModifier.PUBLIC)
				.anyOfTheseAnnotations(DESCRIPTOR_A)
				.processSingletons(Command::new)
				.stream()
				.collect(Collectors.toMap(c -> c.node, c -> c));
		}

		boolean isPresent() {
			return gogo != null;
		}

		void deleteAll() {
			cursor.forEach((ass, td) -> {
				ass.deleteAnnotation(td, DESCRIPTOR_A);
				ass.deleteAnnotation(td, GOGOCOMMAND_A);
			})
				.downTo(MethodDeclaration.class)
				.forEach((ass, md) -> {
					ass.deleteAnnotation(md, DESCRIPTOR_A);
				})
				.downTo(SingleVariableDeclaration.class)
				.forEach((ass, svd) -> {
					ass.deleteAnnotation(svd, DESCRIPTOR_A);
					ass.deleteAnnotation(svd, PARAMETER_A);
				});
		}

		public void addGogo() {
			cursor.forEach((ass, td) -> {
				String name = scrunch(td.getName()
					.toString());
				DescriptorDTO descriptor = new DescriptorDTO();
				descriptor.value = name + " - " + td.getName();
				GogoCommandDTO gogo = new GogoCommandDTO();
				gogo.scope = name;
				gogo.function = getFunction();
				ass.ensureAnnotation(td, ass.newAnnotation(DESCRIPTOR_A, descriptor));
				ass.ensureAnnotation(td, ass.newAnnotation(GOGOCOMMAND_A, gogo));
			});
		}

		private String scrunch(String string) {
			String s = string.toLowerCase();
			if (s.length() < 8)
				return s;

			StringBuilder result = new StringBuilder(s);
			for (int i = result.length() - 1; i >= 0 && result.length() > 8; i--) {
				char c = result.charAt(i);
				if (!Character.isAlphabetic(c) || "aeiou".indexOf(c) >= 0) {
					result.delete(i, 1);
				}
			}
			if (result.length() > 8) {
				result.delete(8, result.length());
			}
			return result.toString();
		}

		String[] calculateNames(String name) {
			if (name.length() < 1)
				return new String[0];
			name = name.toLowerCase();
			char c = name.charAt(0);
			Set<Character> used = getUsedCharacters();
			if (used.contains(c)) {
				c = Character.toUpperCase(c);
				while (used.contains(c)) {
					c = (char) (c + 1);
				}
			}
			return new String[] {
				"-" + c, "--" + name
			};
		}

		Set<Character> getUsedCharacters() {
			return commands.values()
				.stream()
				.flatMap(c -> c.pars.values()
					.stream())
				.map(Par::character)
				.collect(Collectors.toSet());
		}

		void delete(Cursor<MethodDeclaration> cursor) {
			cursor.forEach((ass, md) -> {
				Command c = commands.remove(md);
				if (c != null) {
					c.delete();
					update();
				}
			});
		}

		void update() {
			GogoCommandDTO gogo = new GogoCommandDTO();
			gogo.scope = this.gogo.scope;
			gogo.function = getFunction();
			cursor.forEach((ass, td) -> {
				ass.ensureAnnotation(td, ass.newAnnotation(GOGOCOMMAND_A, gogo));
			});
		}

		public String[] getFunction() {
			return commands.values()
				.stream()
				.map(c -> c.name)
				.sorted()
				.toArray(String[]::new);
		}

		public void add(Command command) {
			command.ensure();
			commands.put(command.node, command);
			update();
		}
	}

	@Override
	public void addCompletions(ProposalBuilder builder, RefactorAssistant assistant, Cursor<?> root,
		IInvocationContext context) {


		root = root.isJavaSourceType(JavaSourceType.CLASS);

		GogoState state = root.upTo(TypeDeclaration.class)
			.anyOfTheseAnnotations(ComponentRefactorer.COMPONENT_A)
			.processSingletons(GogoState::new)
			.stream()
			.findAny()
			.orElse(null);

		if (state != null) {
			root.upTo(TypeDeclaration.class, 1)
				.forEach((ass, n) -> {
					doScope(builder, state);
				});
			if (state.isPresent()) {
				root.upTo(SingleVariableDeclaration.class, 4)
					.parentType(MethodDeclaration.class)
					.checkAnnotation(
						(formalParameter, present) -> doParameter(builder, formalParameter, present, state),
						PARAMETER_A);

				root.upTo(MethodDeclaration.class, 4)
					.checkAnnotation(
						(methodDeclaration, present) -> doCommand(builder, methodDeclaration, present, state),
						DESCRIPTOR_A);
			}
		}
	}

	void doParameter(ProposalBuilder builder, Cursor<SingleVariableDeclaration> cursor, boolean present,
		GogoState state) {
		RefactorAssistant ass = cursor.getAssistant();
		SingleVariableDeclaration formalParameter = cursor.getNode()
			.get();
		MethodDeclaration methodDeclaration = (MethodDeclaration) formalParameter.getParent();

		String type = isBoolean(formalParameter.getType()) ? "flag" : "parameter";
		if (present) {
			builder.build("gogo.parm-", "Remove the " + type + " " + formalParameter.getName(), "gogo", -10, () -> {
				Cursor<MethodDeclaration> upTo = cursor.upTo(MethodDeclaration.class, 2);
				ass.deleteAnnotation(formalParameter, DESCRIPTOR_A);
				ass.deleteAnnotation(formalParameter, PARAMETER_A);
			});
		} else {
			builder.build("gogo.parm+", "Add @Parameter " + type, "gogo", -10, () -> {
				DescriptorDTO descriptor = new DescriptorDTO();
				String name = formalParameter.getName()
					.toString();
				descriptor.value = name;

				ParameterDTO parameter = new ParameterDTO();
				if (type.equals("flag")) {
					parameter.absentValue = "false";
					parameter.presentValue = "true";
				} else {
					parameter.absentValue = "";
					parameter.presentValue = null;
				}

				parameter.names = state.calculateNames(name);
				ass.ensureAnnotation(formalParameter, ass.newAnnotation(PARAMETER_A, parameter));
				ass.ensureAnnotation(formalParameter, ass.newAnnotation(DESCRIPTOR_A, descriptor));

				ass.ensureModifiers(methodDeclaration, JavaModifier.PUBLIC);
				if (!ass.getAnnotation(methodDeclaration, DESCRIPTOR_A)
					.isPresent()) {
					ass.ensureAnnotation(methodDeclaration,
						ass.newAnnotation(DESCRIPTOR_A, ass.getIdentifier(methodDeclaration)));
				}
			});
		}
	}

	private boolean isBoolean(Type type) {
		return type.toString()
			.equals("boolean")
			|| type.toString()
				.equals("java.lang.Boolean");
	}

	void doScope(ProposalBuilder builder, GogoState state) {
		if (state.isPresent()) {
			builder.build("gogo.scope-", "Remove Gogo", "gogo", -10, () -> {
				state.deleteAll();
			});
		} else {
			builder.build("gogo.scope+", "Add Gogo", "gogo", -10, () -> {
				state.addGogo();
			});
		}

	}

	void doCommand(ProposalBuilder builder, Cursor<MethodDeclaration> methodDeclaration, boolean present,
		GogoState state) {
		if (present)
			builder.build("gogo.cmd.desc-", "Remove Gogo command", "gogo", 0, () -> state.delete(methodDeclaration));
		else
			builder.build("gogo.cmd.desc+", "Add Gogo command", "gogo", 0,
				() -> state.add(new Command(methodDeclaration)));
	}

}
