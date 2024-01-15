package org.bndtools.refactor.types;

import static aQute.libg.re.Catalog.g;
import static aQute.libg.re.Catalog.or;
import static aQute.libg.re.Catalog.setAll;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import org.bndtools.refactor.util.BaseRefactorer;
import org.bndtools.refactor.util.Cursor;
import org.bndtools.refactor.util.DomainBase;
import org.bndtools.refactor.util.JavaModifier;
import org.bndtools.refactor.util.JavaSourceType;
import org.bndtools.refactor.util.ProposalBuilder;
import org.bndtools.refactor.util.RefactorAssistant;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.ui.text.java.IInvocationContext;
import org.eclipse.jdt.ui.text.java.IQuickFixProcessor;
import org.osgi.service.component.annotations.Component;

import aQute.libg.re.RE;

/**
 * Refactoring components
 */
@Component
public class ComponentRefactorer extends BaseRefactorer implements IQuickFixProcessor {

	public final RE				BIND_METHOD_P		= g(g("prefix", or("add", "set")), g("service", setAll));
	public final RE				UNBIND_METHOD_P		= g(g("prefix", or("remove", "unset")), g("service", setAll));
	public final RE				ACTIVATE_METHOD_P	= g(or("activate", "start", "init", "initialize", "onActivate",
		"onStart", "begin", "doActivate", "create", "setup", "ready", "load"), setAll);
	public final RE				DEACTIVATE_METHOD_P	= g(or("deactivate", "stop", "close", "finish", "dispose",
		"shutdown", "onDeactivate", "onStop", "end", "doDeactivate", "release", "teardown", "cleanup"), setAll);
	public final String			COMPONENT_A			= "org.osgi.service.component.annotations.Component";
	public final String			REFERENCE_A			= "org.osgi.service.component.annotations.Reference";
	public final String			ACTIVATE_A			= "org.osgi.service.component.annotations.Activate";
	public final String			DEACTIVATE_A		= "org.osgi.service.component.annotations.Deactivate";
	public final String			BUNDLECONTEXT_T		= "org.osgi.framework.BundleContext";
	public final String			SERVICEREFERENCE_T	= "org.osgi.framework.ServiceReference";
	public final String			MAP_T				= "java.util.Map";
	public final Set<String>	NOT_REFERENCE		= Set.of(BUNDLECONTEXT_T, SERVICEREFERENCE_T, MAP_T);
	public final static int		BASE_LEVEL			= 2000;

	class MethodState extends DomainBase<MethodDeclaration> {
		final String annotation;

		protected MethodState(Cursor<MethodDeclaration> cursor, String annotation) {
			super(cursor);
			this.annotation = annotation;
		}

		void remove() {
			cursor.forEach((ass, md) -> {
				ass.deleteAnnotation(md, annotation);
				cursor.downTo(SingleVariableDeclaration.class)
					.forEach((assx, svd) -> {
						assx.deleteAnnotation(svd, REFERENCE_A);
					});
			});
		}
	}

	interface ReferenceState {
		void remove();
	}

	class MethodReferenceState extends DomainBase<MethodDeclaration> implements ReferenceState {

		protected MethodReferenceState(Cursor<MethodDeclaration> cursor) {
			super(cursor);
		}

		@Override
		public void remove() {
			cursor.forEach((ass, md) -> ass.deleteAnnotation(md, REFERENCE_A));
		}

	}

	class FieldReferenceState extends DomainBase<VariableDeclarationFragment> implements ReferenceState {

		protected FieldReferenceState(Cursor<VariableDeclarationFragment> cursor) {
			super(cursor);
		}

		@Override
		public void remove() {
			cursor.forEach((ass, fd) -> ass.deleteAnnotation(fd.getParent(), REFERENCE_A));
		}

	}

	class ParameterReferenceState extends DomainBase<SingleVariableDeclaration> implements ReferenceState {
		final MethodState ms;

		protected ParameterReferenceState(Cursor<SingleVariableDeclaration> cursor, MethodState ms) {
			super(cursor);
			this.ms = ms;
		}

		@Override
		public void remove() {
			cursor.forEach((ass, pd) -> ass.deleteAnnotation(pd, REFERENCE_A));
		}

	}

	class ComponentState extends DomainBase<TypeDeclaration> {
		final ProposalBuilder						builder;
		final Map<MethodDeclaration, MethodState>	activate	= new LinkedHashMap<>();
		final Map<MethodDeclaration, MethodState>	deactivate	= new LinkedHashMap<>();
		final Map<ASTNode, ReferenceState>			references	= new LinkedHashMap<>();

		protected ComponentState(Cursor<TypeDeclaration> cursor, ProposalBuilder builder) {
			super(cursor);
			this.builder = builder;
			cursor.downTo(MethodDeclaration.class)
				.forEach((ass, methodDeclaration) -> {

					Cursor<MethodDeclaration> single = ass.cursor(methodDeclaration);
					MethodState ms = null;
					if (ass.hasAnnotation(methodDeclaration, ACTIVATE_A)) {
						ms = new MethodState(single, ACTIVATE_A);
						activate.put(methodDeclaration, ms);
					}
					if (ass.hasAnnotation(methodDeclaration, DEACTIVATE_A)) {
						ms = new MethodState(single, DEACTIVATE_A);
						deactivate.put(methodDeclaration, ms);
					}

					if (ms != null) {
						MethodState local = ms;
						single.downTo(SingleVariableDeclaration.class)
							.anyOfTheseAnnotations(REFERENCE_A)
							.forEach(svd -> {
								references.put(svd, new ParameterReferenceState(ass.cursor(svd), local));
							});
					}

					if (ass.hasAnnotation(methodDeclaration, REFERENCE_A)) {
						references.put(methodDeclaration, new MethodReferenceState(single));
					}
				});
			cursor.downTo(FieldDeclaration.class)
				.anyOfTheseAnnotations(REFERENCE_A)
				.downTo(VariableDeclarationFragment.class)
				.forEach((ass, fieldDeclaration) -> {
					Cursor<VariableDeclarationFragment> single = ass.cursor(fieldDeclaration);
					references.put(fieldDeclaration, new FieldReferenceState(single));
				});
		}

		void remove() {
			cursor.forEach((ass, td) -> {
				ass.deleteAnnotation(td, COMPONENT_A);
			});
			activate.values()
				.forEach(MethodState::remove);
			deactivate.values()
				.forEach(MethodState::remove);
			references.values()
				.forEach(ReferenceState::remove);
		}

		void proposeReference(ASTNode node, ASTNode withAnnotation) {
			RefactorAssistant assistant = builder.getAssistant();
			String identifier = assistant.getIdentifier(node);

			ReferenceState rs = references.get(node);
			if (rs == null) {
				builder.build("comp.ref+", "Add @Reference to " + identifier, "component", 10, () -> {
					assistant.ensureAnnotation(withAnnotation, assistant.newAnnotation(REFERENCE_A));
				});
			} else {
				builder.build("comp.ref-", "Remove @Reference from " + identifier, "component", 2, rs::remove);
			}
		}

		void proposeActivate(MethodDeclaration node) {
			RefactorAssistant assistant = builder.getAssistant();
			String identifier = assistant.getIdentifier(node);

			MethodState act = activate.get(node);
			if (act == null) {
				builder.build("comp.act+", "Add @Activate to " + identifier, "component", 10, () -> {
					if (node.isConstructor()) {
						assistant.ensureModifiers(node, JavaModifier.PUBLIC);
					}
					assistant.ensureAnnotation(node, assistant.newAnnotation(ACTIVATE_A));
					activate.values()
						.forEach(MethodState::remove);
				});
			} else {
				builder.build("comp.act-", "Remove @Activate from " + identifier, "component", 3, act::remove);
			}
		}

		void proposeDeactivate(MethodDeclaration node) {
			RefactorAssistant assistant = builder.getAssistant();
			String identifier = assistant.getIdentifier(node);
			MethodState deact = deactivate.get(node);
			if (deact == null) {
				builder.build("comp.deact+", "Add @Deactivate to " + identifier, "component", 5, () -> {
					assistant.ensureAnnotation(node, assistant.newAnnotation(DEACTIVATE_A));
					deactivate.values()
						.forEach(MethodState::remove);
				});
			} else {
				builder.build("comp.deact-", "Remove @Deactivate from " + identifier, "component", 3, deact::remove);
			}
		}

	}

	@Override
	public void addCompletions(ProposalBuilder builder, RefactorAssistant assistant, Cursor<?> root,
		IInvocationContext context) {

		root.isJavaSourceType(JavaSourceType.CLASS)
			.upTo(TypeDeclaration.class)
			.forEach((ass, typeDeclaration) -> {
				if (ass.hasAnnotation(typeDeclaration, COMPONENT_A)) {
					ComponentState cs = new ComponentState(ass.cursor(typeDeclaration), builder);

					builder.build("comp-", "Remove @Component", "component", 3, cs::remove);

					root.upTo(VariableDeclarationFragment.class)
						.isNotPrimitive()
						.and(c -> c.upTo(FieldDeclaration.class, 1))
						.forEach((x, svd) -> cs.proposeReference(svd, svd.getParent()));

					Cursor<MethodDeclaration> method = root.upTo(MethodDeclaration.class, 2);

					method.nameMatches(BIND_METHOD_P.pattern())
						.isVoidMethod()
						.noneOfTheseAnnotations(ACTIVATE_A, DEACTIVATE_A)
						.forEach((x, md) -> cs.proposeReference(md, md));

					root.upTo(SingleVariableDeclaration.class)
						.isNotPrimitive()
						.and(c -> c.upTo(MethodDeclaration.class, 1)
							.anyOfTheseAnnotations(ACTIVATE_A, DEACTIVATE_A))
						.forEach((x, svd) -> cs.proposeReference(svd, svd));

					method.nameMatches(ACTIVATE_METHOD_P.pattern())
						.forEach((x, md) -> cs.proposeActivate(md));

					method.filter(MethodDeclaration::isConstructor)
						.forEach((x, md) -> cs.proposeActivate(md));

					method.nameMatches(DEACTIVATE_METHOD_P.pattern())
						.forEach((x, md) -> cs.proposeDeactivate(md));

				} else {
					builder.build("comp+", "Add @Component", "component", 7,
						() -> ass.ensureAnnotation(typeDeclaration, ass.newAnnotation(COMPONENT_A)));
				}
			});
	}
}
