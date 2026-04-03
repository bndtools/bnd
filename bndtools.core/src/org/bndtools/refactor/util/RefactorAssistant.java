package org.bndtools.refactor.util;

import static aQute.libg.re.Catalog.dotall;
import static aQute.libg.re.Catalog.g;
import static aQute.libg.re.Catalog.lit;
import static aQute.libg.re.Catalog.multiline;
import static aQute.libg.re.Catalog.nl;
import static aQute.libg.re.Catalog.or;
import static aQute.libg.re.Catalog.re;
import static aQute.libg.re.Catalog.setAll;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.jdt.core.IAnnotation;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.Annotation;
import org.eclipse.jdt.core.dom.AnnotationTypeDeclaration;
import org.eclipse.jdt.core.dom.ArrayCreation;
import org.eclipse.jdt.core.dom.ArrayInitializer;
import org.eclipse.jdt.core.dom.ArrayType;
import org.eclipse.jdt.core.dom.Assignment;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.BooleanLiteral;
import org.eclipse.jdt.core.dom.CharacterLiteral;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.EnumDeclaration;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.ExpressionStatement;
import org.eclipse.jdt.core.dom.FieldAccess;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.ImportDeclaration;
import org.eclipse.jdt.core.dom.IntersectionType;
import org.eclipse.jdt.core.dom.MarkerAnnotation;
import org.eclipse.jdt.core.dom.MemberValuePair;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.Modifier.ModifierKeyword;
import org.eclipse.jdt.core.dom.ModuleDeclaration;
import org.eclipse.jdt.core.dom.Name;
import org.eclipse.jdt.core.dom.NameQualifiedType;
import org.eclipse.jdt.core.dom.NodeFinder;
import org.eclipse.jdt.core.dom.NormalAnnotation;
import org.eclipse.jdt.core.dom.NullLiteral;
import org.eclipse.jdt.core.dom.NumberLiteral;
import org.eclipse.jdt.core.dom.PackageDeclaration;
import org.eclipse.jdt.core.dom.ParameterizedType;
import org.eclipse.jdt.core.dom.PrimitiveType;
import org.eclipse.jdt.core.dom.QualifiedName;
import org.eclipse.jdt.core.dom.RecordDeclaration;
import org.eclipse.jdt.core.dom.ReturnStatement;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SimpleType;
import org.eclipse.jdt.core.dom.SingleMemberAnnotation;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.StringLiteral;
import org.eclipse.jdt.core.dom.TextBlock;
import org.eclipse.jdt.core.dom.ThisExpression;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.TypeLiteral;
import org.eclipse.jdt.core.dom.UnionType;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.WildcardType;
import org.eclipse.jface.text.IDocument;
import org.eclipse.text.edits.TextEdit;
import org.osgi.dto.DTO;

import aQute.bnd.exceptions.Exceptions;
import aQute.bnd.memoize.Memoize;
import aQute.lib.converter.Converter;
import aQute.lib.strings.Strings;
import aQute.libg.re.RE;
import aQute.libg.re.RE.Match;
import aQute.libg.re.RE.MatchGroup;

/**
 * This class is quite useful if you need to analyze and modify an AST. You can
 * create it on the different types like ICompilationUnit, CompilationUnit, but
 * also on a source string. It will get an appropriate AST and provides many
 * methods that are a lot easier to use than the standard API.
 * <p>
 * Especially the {@link Cursor} API is very useful. It allows you to create a
 * selection on the tree and then navigate the tree, widening and expanding, as
 * is necessary. This often reduces a lot of low level code.
 * <p>
 * This class leverages the stream API, generics, enums, and other modern
 * features that can make the raw AST API so hard to used. For example, each
 * ASTNode has a number of children that can be grouped by their type. E.g. the
 * Type Declaration has Method Declaration as children. The generics and
 * reflection are used to make this structure significantly easier to use.
 * <p>
 * This class uses the {@link ASTEngine} class to handle access to the tree and
 * constructs an ASTWriter for the changes made to the tree.
 */
@SuppressWarnings({
	"rawtypes", "unchecked"
})
public class RefactorAssistant {

	/**
	 * Represents an entry in an Annotation
	 */
	public class Entry {
		public final String	name;
		public final Object	value;

		public Entry(String name, Object value) {
			this.name = name;
			this.value = value;
		}
	}

	/**
	 * Implementation of the Cursor interface
	 *
	 * @param <C> the given type
	 */

	class CursorImpl<C extends ASTNode> implements Cursor<C> {
		final Collection<C>	nodes;
		final int			start, length;

		CursorImpl(Collection<C> nodes, int start, int length) {
			this.nodes = nodes;
			this.start = start;
			this.length = length;
		}

		public int start() {
			return start;
		}

		public int length() {
			return length;
		}

		@Override
		public Cursor<C> and(Function<Cursor<C>, Cursor<?>> other) {
			Cursor<?> apply = other.apply(this);
			if (apply instanceof FailedCursor)
				return (Cursor<C>) apply;
			return this;
		}

		@Override
		public Cursor<C> anyOfTheseAnnotations(String... typeNames) {
			List<C> result = new ArrayList<>();
			nextNode: for (C node : nodes) {
				for (String typeName : typeNames) {
					if (getAnnotation(node, typeName).isPresent()) {
						result.add(node);
						continue nextNode;
					}
				}
			}
			if (result.isEmpty())
				return failed("%s.hasAnnotation(%s)", nodes, typeNames);
			else
				return new CursorImpl<>(result, start, length);
		}

		@Override
		public <X extends ASTNode> Cursor<X> cast(Class<X> type) {
			List<X> result = new ArrayList<>();
			for (C node : nodes) {
				if (!type.isInstance(node))
					continue;

				result.add(type.cast(node));
			}
			if (result.isEmpty())
				return failed("%s.cast(%s) : not an instance, is %s", nodes, type, nodes.getClass());
			else
				return new CursorImpl<>(result, start, length);
		}

		@Override
		public Cursor<C> checkAnnotation(BiConsumer<Cursor<C>, Boolean> body, String... annotationNames) {
			return forEach(node -> {
				boolean found = true;
				outer: for (String typeName : annotationNames) {
					if (!getAnnotation(node, typeName).isPresent()) {
						found = false;
						break outer;
					}
				}
				body.accept(cursor(node), found);
			});
		}

		@Override
		public <X extends ASTNode> Cursor<X> descend(Class<X> type) {
			LinkedHashSet<X> result = new LinkedHashSet<>();
			for (C node : nodes) {
				stream(node, type).forEach(result::add);
			}
			if (result.isEmpty())
				return failed("%s.descend(%s)", nodes, type);
			else
				return new CursorImpl<>(result, start, length);
		}

		@Override
		public <X extends ASTNode> Cursor<X> downTo(Class<X> type, int count) {

			LinkedHashSet<X> result = new LinkedHashSet<>();
			for (C node : nodes) {
				node.accept(new ASTVisitor() {
					int n = count;

					@Override
					public void postVisit(ASTNode node) {
						n++;
					}

					@Override
					public boolean preVisit2(ASTNode visited) {
						if (n-- <= 0)
							return false;

						if (type.isInstance(visited)) {
							result.add(type.cast(visited));
							return false;
						} else {
							return true;
						}
					}
				});
			}
			if (result.isEmpty())
				return failed("%s.downTo(%s,%s)", nodes, type, count);
			else
				return new CursorImpl<>(result, start, length);
		}

		@Override
		public boolean equals(Object other) {
			if (other instanceof Cursor xx) {
				List a = getNodes();
				List b = xx.getNodes();
				return Objects.equals(a, b);
			}
			return false;
		}

		@Override
		public Cursor<C> filter(Predicate<C> test) {
			return filter0(test, "%s.test(...) ", nodes);
		}

		@Override
		public <X extends ASTNode> Cursor<X> flatMap(Function<C, Cursor<X>> mapper) {
			return flatMap0(mapper, "%s.flatMap", nodes);
		}

		@Override
		public Cursor<C> forEach(BiConsumer<RefactorAssistant, C> process) {
			for (C node : nodes) {
				process.accept(getRefactorAssistant(), node);
			}
			return this;
		}

		@Override
		public Cursor<C> forEach(Consumer<C> process) {
			for (C node : nodes) {
				process.accept(node);
			}
			return this;
		}

		@Override
		public RefactorAssistant getAssistant() {
			return RefactorAssistant.this;
		}

		@Override
		public Optional<C> getNode() {
			if (nodes.size() != 1)
				return Optional.empty();
			return Optional.ofNullable(nodes.iterator()
				.next());
		}

		@Override
		public List<C> getNodes() {
			return new ArrayList(nodes);
		}

		public RefactorAssistant getRefactorAssistant() {
			return RefactorAssistant.this;
		}

		@Override
		public int hashCode() {
			List a = getNodes();
			return Objects.hashCode(a);
		}

		@Override
		public Cursor<C> hasModifier(JavaModifier... modifiers) {
			return filter0(node -> {
				return RefactorAssistant.this.hasModifiers(node, modifiers);
			}, "%s.hasModifier(%s)", nodes, modifiers);
		}

		@Override
		public Cursor<C> hasName(String name) {
			return filter0(node -> {
				String identity = getIdentifier(node);
				return name.equals(identity);
			}, "%s.hasName(%s)", nodes, name);
		}

		@Override
		public Cursor<C> ifNotPresent(Runnable process) {
			return this;
		}

		@Override
		public boolean isEmpty() {
			return nodes.isEmpty();
		}

		@Override
		public Cursor<C> isJavaSourceType(JavaSourceType... sourceType) {

			Set<JavaSourceType> or = Set.of(sourceType);
			List<C> result = new ArrayList<>();
			for (C node : nodes) {
				JavaSourceType type = getJavaSourceType(node);
				if (or.contains(type)) {
					result.add(node);
				}
			}
			if (result.isEmpty())
				return failed("%s.isJavaSourceType(%s) : unknown", nodes, sourceType);
			else
				return new CursorImpl<>(result, start, length);
		}

		@Override
		public Cursor<C> isNotInstanceOfAny(Class<?>... types) {
			return filter0(n -> {
				for (Class c : types) {
					if (c.isInstance(n))
						return false;
				}
				return true;
			}, "%s.isNotInstanceOfAny(%s) : %s", nodes, types, nodes.stream()
				.map(n -> n.getClass()
					.getSimpleName())
				.toArray());
		}

		@Override
		public Cursor<C> isNotPrimitive() {
			return filter0(node -> {
				return getType(node).map(t -> !t.isPrimitiveType())
					.orElse(false);

			}, "%s.isNotPrimitive() : %s", nodes, nodes.getClass());
		}

		@Override
		public Cursor<C> isPrimitive() {
			return filter0(node -> {
				return getType(node).map(t -> t.isPrimitiveType())
					.orElse(false);

			}, "%s.isPrimitive() : %s", nodes, nodes.getClass());
		}

		@Override
		public Cursor<C> isVoidMethod() {
			return filter0(node -> {
				if (node instanceof MethodDeclaration md) {
					Type type = md.getReturnType2();
					if (type.isPrimitiveType() && type.toString()
						.equals(VOID)) {
						return true;
					}
				}
				return false;
			}, "%s.isVoid()", nodes);
		}

		@Override
		public <X extends ASTNode> Cursor<X> map(Function<C, X> mapper) {
			return map0(mapper, "%s.map", nodes);
		}

		@Override
		public Cursor<C> nameMatches(Pattern re) {
			Predicate<String> predicate = re.asPredicate();

			return filter0(node -> {
				String identity = getIdentifier(node);
				if (identity == null)
					return false;
				return predicate.test(identity);
			}, "%s.nameMatches(%s)", nodes, re);
		}

		@Override
		public Cursor<C> nameNotMatches(Pattern re) {
			Predicate<String> predicate = re.asPredicate();

			return filter0(node -> {
				String identity = getIdentifier(node);
				if (identity == null)
					return true;
				return !predicate.test(identity);
			}, "%s.nameNotMatches(%s)", nodes, re);
		}

		@Override
		public Cursor<C> noneOfTheseAnnotations(String... typeNames) {
			return filter0(node -> {
				for (String typeName : typeNames) {
					if (getAnnotation(node, typeName).isPresent()) {
						return false;
					}
				}
				return true;
			}, "%s.lacksAnnotation(%s)", nodes, typeNames);
		}

		@Override
		public Cursor<C> parentType(Class<? extends ASTNode> type) {
			List<C> result = new ArrayList<>();
			for (C node : nodes) {
				ASTNode parent = node.getParent();
				if (type.isInstance(parent))
					result.add(node);
			}
			if (result.isEmpty())
				return failed("%s.parentType(%s)", nodes, type);
			else
				return new CursorImpl<>(result, start, length);
		}

		@Override
		public <X> List<X> processSingletons(Function<Cursor<C>, X> f) {
			List<X> result = new ArrayList<>();
			for (C node : nodes) {
				List<C> singleton = Collections.singletonList(node);
				CursorImpl<C> cursor = new CursorImpl<C>(singleton, start, length);
				X x = f.apply(cursor);
				result.add(x);
			}
			return result;
		}

		@Override
		public String toString() {
			return nodes.toString();
		}

		@Override
		public Cursor<C> typeIn(String... typeNames) {

			if (typeNames.length == 0)
				return failed("%s.typeIn() no types names specified", nodes);

			return filter0(node -> {
				return getType(node).map(type -> resolve(type).orElseGet(type::toString))
					.map(s -> Strings.in(typeNames, s))
					.orElse(false);
			}, "%s.typeIn(%s)", nodes, typeNames);
		}

		@Override
		public <X extends ASTNode> Cursor<X> upTo(Class<X> type, int n) {
			return map0(node -> {
				int count = n;
				ASTNode rover = node;
				while (rover != null && count-- >= 0) {
					if (type.isInstance(rover)) {
						return type.cast(rover);
					}
					rover = rover.getParent();
				}
				return null;
			}, "%s.upTo(%s,%d)", nodes, type, n);
		}

		Cursor failed(String format, Object... args) {
			return new FailedCursor(format, args);
		}

		Cursor<C> filter0(Predicate<C> test, String message, Object... args) {
			Set<C> result = new LinkedHashSet<>();
			for (C node : nodes) {
				if (test.test(node)) {
					result.add(node);
				}
			}
			if (result.isEmpty())
				return failed(message, args);
			else
				return new CursorImpl<>(result, start, length);
		}

		<X extends ASTNode> Cursor<X> flatMap0(Function<C, Cursor<X>> mapper, String format, Object... args) {
			Set<X> result = new LinkedHashSet<>();
			for (C node : nodes) {
				Cursor<X> apply = mapper.apply(node);
				if (apply != null) {
					apply.forEach(n -> {
						result.add(n);
					});
				}
			}
			if (result.isEmpty())
				return failed(format, args);
			else
				return new CursorImpl<>(result, start, length);
		}

		<X extends ASTNode> Cursor<X> map0(Function<C, X> mapper, String format, Object... args) {
			Set<X> result = new LinkedHashSet<>();
			for (C node : nodes) {
				X apply = mapper.apply(node);
				if (apply != null) {
					result.add(apply);
				}
			}
			if (result.isEmpty())
				return failed(format, args);
			else
				return new CursorImpl<>(result, start, length);
		}

	}

	class FailedCursor<C extends ASTNode> implements Cursor<C> {

		final Object[]	args;
		final String	message;

		FailedCursor(String message, Object... args) {
			this.message = message;
			this.args = args;
		}

		@Override
		public Cursor<C> and(Function<Cursor<C>, Cursor<?>> other) {
			return this;
		}

		@Override
		public Cursor<C> anyOfTheseAnnotations(String... typeNames) {
			return this;
		}

		@Override
		public <X extends ASTNode> Cursor<X> cast(Class<X> type) {
			return (Cursor<X>) this;
		}

		@Override
		public Cursor<C> checkAnnotation(BiConsumer<Cursor<C>, Boolean> body, String... annotationNames) {
			return this;
		}

		@Override
		public <X extends ASTNode> Cursor<X> descend(Class<X> class1) {
			return (Cursor<X>) this;
		}

		@Override
		public <X extends ASTNode> Cursor<X> downTo(Class<X> class1) {
			return (Cursor<X>) this;
		}

		@Override
		public <X extends ASTNode> Cursor<X> downTo(Class<X> class1, int count) {
			return (Cursor<X>) this;
		}

		@Override
		public Cursor<C> filter(Predicate<C> test) {
			return this;
		}

		@Override
		public <X extends ASTNode> Cursor<X> flatMap(Function<C, Cursor<X>> mapper) {
			return (Cursor<X>) this;
		}

		@Override
		public Cursor<C> forEach(BiConsumer<RefactorAssistant, C> process) {
			return this;
		}

		@Override
		public Cursor<C> forEach(Consumer<C> process) {
			return this;
		}

		@Override
		public RefactorAssistant getAssistant() {
			return RefactorAssistant.this;
		}

		@Override
		public Optional<C> getNode() {
			return Optional.empty();
		}

		@Override
		public List<C> getNodes() {
			return new ArrayList<>();
		}

		@Override
		public Cursor<C> hasModifier(JavaModifier... modifiers) {
			return this;
		}

		@Override
		public Cursor<C> hasName(String name) {
			return this;
		}

		@Override
		public Cursor<C> ifNotPresent(Runnable process) {
			process.run();
			return this;
		}

		@Override
		public boolean isEmpty() {
			return true;
		}

		@Override
		public Cursor<C> isJavaSourceType(JavaSourceType... sourceType) {
			return this;
		}

		@Override
		public Cursor<C> isNotInstanceOfAny(Class<?>... class1) {
			return this;
		}

		@Override
		public Cursor<C> isNotPrimitive() {
			return this;
		}

		@Override
		public Cursor<C> isPrimitive() {
			return this;
		}

		@Override
		public Cursor<C> isVoidMethod() {
			return this;
		}

		@Override
		public <X extends ASTNode> Cursor<X> map(Function<C, X> mapper) {
			return (Cursor<X>) this;
		}

		@Override
		public Cursor<C> nameMatches(Pattern re) {
			return this;
		}

		@Override
		public Cursor<C> nameNotMatches(Pattern re) {
			return this;
		}

		@Override
		public Cursor<C> noneOfTheseAnnotations(String... typeName) {
			return this;
		}

		@Override
		public Cursor<C> parentType(Class<? extends ASTNode> type) {
			return this;
		}

		@Override
		public <X> List<X> processSingletons(Function<Cursor<C>, X> object) {
			return new ArrayList<>();
		}

		@Override
		public String toString() {
			for (int i = 0; i < args.length; i++) {
				Object object = args[i];
				if (object.getClass()
					.isArray()) {
					args[i] = RefactorAssistant.toString(object);
				}

			}
			try {
				return String.format(message, args);
			} catch (Exception e) {
				return message + ":" + e;
			}
		}

		@Override
		public Cursor<C> typeIn(String... typeNames) {
			return this;
		}

		@Override
		public <X extends ASTNode> Cursor<X> upTo(Class<X> class1, int n) {
			return (Cursor<X>) this;
		}

	}

	class TypeGatherer extends ASTVisitor {
		final Set<ASTNode>	deleted;
		final Set<String>	typeReferences	= new HashSet<>();

		public TypeGatherer(ASTNode root, Set<ASTNode> forbidden) {
			this.deleted = forbidden;
		}

		@Override
		public boolean preVisit2(ASTNode node) {
			if (deleted.contains(node))
				return false;

			return getReferredType(node).map(x -> {
				typeReferences.add(x);
				return false;
			})
				.orElse(true);
		}

		@Override
		public boolean visit(ImportDeclaration node) {
			return false;
		}
	}

	public final static RE		PRIMITIVES_P	= or("void", "boolean", "byte", "char", "short", "int", "long", "float",
		"double");
	public final static String	VOID			= "void";
	final static RE				DQUOTES_P		= lit("\"\"\"");
	final static Set<String>	javalang		= Set.of("AbstractMethodError", "AbstractStringBuilder", "Appendable",
		"ApplicationShutdownHooks", "ArithmeticException", "ArrayIndexOutOfBoundsException", "ArrayStoreException",
		"AssertionError", "AssertionStatusDirectives", "AutoCloseable", "Boolean", "BootstrapMethodError", "Byte",
		"Character", "CharacterData", "CharacterName", "CharSequence", "Class", "ClassCastException",
		"ClassCircularityError", "ClassFormatError", "ClassLoader", "ClassNotFoundException", "ClassValue", "Cloneable",
		"CloneNotSupportedException", "Comparable", "Compiler", "CompoundEnumeration", "ConditionalSpecialCasing",
		"Deprecated", "Double", "Enum", "EnumConstantNotPresentException", "Error", "Exception",
		"ExceptionInInitializerError", "FdLibm", "Float", "FunctionalInterface", "IllegalAccessError",
		"IllegalAccessException", "IllegalArgumentException", "IllegalCallerException", "IllegalMonitorStateException",
		"IllegalStateException", "IllegalThreadStateException", "IncompatibleClassChangeError",
		"IndexOutOfBoundsException", "InheritableThreadLocal", "InstantiationError", "InstantiationException",
		"Integer", "InternalError", "InterruptedException", "Iterable", "LayerInstantiationException", "LinkageError",
		"LiveStackFrame", "LiveStackFrameInfo", "Long", "Math", "Module", "ModuleLayer", "NamedPackage",
		"NegativeArraySizeException", "NoClassDefFoundError", "NoSuchFieldError", "NoSuchFieldException",
		"NoSuchMethodError", "NoSuchMethodException", "NullPointerException", "Number", "NumberFormatException",
		"Object", "OutOfMemoryError", "Override", "Package", "Process", "ProcessBuilder", "ProcessEnvironment",
		"ProcessHandle", "ProcessHandleImpl", "ProcessImpl", "PublicMethods", "Readable", "Record",
		"ReflectiveOperationException", "Runnable", "Runtime", "RuntimeException", "RuntimePermission", "SafeVarargs",
		"SecurityException", "SecurityManager", "Short", "Shutdown", "StackFrameInfo", "StackOverflowError",
		"StackStreamFactory", "StackTraceElement", "StackWalker", "StrictMath", "String", "StringBuffer",
		"StringBuilder", "StringCoding", "StringConcatHelper", "StringIndexOutOfBoundsException", "StringLatin1",
		"StringUTF16", "SuppressWarnings", "System", "Terminator", "Thread", "ThreadDeath", "ThreadGroup",
		"ThreadLocal", "Throwable", "TypeNotPresentException", "UnknownError", "UnsatisfiedLinkError",
		"UnsupportedClassVersionError", "UnsupportedOperationException", "VerifyError", "VersionProps",
		"VirtualMachineError", "Void", "WeakPairMap");
	final static RE				TEXTBLOCK_P		= g(dotall(), multiline(), DQUOTES_P, nl, g("content", setAll),
		DQUOTES_P);

	/**
	 * Take a string from a text box and turn it into a normal Java string,
	 * unescaping and removing the whitespace.
	 *
	 * @param input the text from a Text Box
	 * @return the cleaned up string
	 */
	public static String fromTextBoxEscaped(String input) {
		Match match = TEXTBLOCK_P.matches(input)
			.orElseThrow();
		String content = match.getGroupValues()
			.get("content");
		String[] split = content.split("\\R");
		int indent = 100000;
		nextString: for (String s : split) {
			if (s.length() == 0)
				indent = 0;
			for (int i = 0; i < s.length(); i++) {
				char c = s.charAt(i);
				if (c != ' ') {
					if (i < indent)
						indent = i;
					continue nextString;
				}
			}
		}
		StringBuilder sb = new StringBuilder();
		String del = "";
		for (int i = 0; i < split.length; i++) {
			sb.append(del);
			String escaped = split[i].substring(indent);
			boolean hadBackslash = false;
			for (int j = 0; j < escaped.length(); j++) {
				char c = escaped.charAt(j);
				if (hadBackslash) {
					sb.append(switch (c) {
						case '\\' -> '\\';
						case '\'' -> '\'';
						case '\"' -> '\"';
						case 'b' -> '\b';
						case 'f' -> '\f';
						case 'n' -> '\n';
						case 'r' -> '\r';
						case 't' -> '\t';
						default -> throw new IllegalArgumentException("Unexpected escaped character: " + c);
					});
					hadBackslash = false;
				} else {
					if (c == '\\') {
						hadBackslash = true;
					} else {
						sb.append(c);
					}
				}
			}
			del = "\n";
		}

		return sb.toString();
	}

	/**
	 * Convert a normal string to a Text Box string, adding necessary
	 * whitespace.
	 *
	 * @param input the normal string
	 * @param indent the indent to aplly
	 * @return a string ready for a Text Box
	 */
	public static String toTextBoxEscaped(String input, int indent) {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < indent; i++) {
			sb.append(' ');
		}
		String indnt = sb.toString();
		sb.insert(0, "\"\"\"\n");
		for (int i = 0; i < input.length(); i++) {
			char c = input.charAt(i);
			switch (c) {
				case '\\' -> sb.append("\\\\");
				case '\b' -> sb.append("\\b");
				case '\f' -> sb.append("\\f");
				case '\n' -> sb.append("\n")
					.append(indnt);
				case '\r' -> sb.append("\\r");
				case '\t' -> sb.append("\\t");
				case '\"' -> sb.append("\"");
				case '\'' -> sb.append("\'");
				default -> sb.append(c);
			}
		}
		sb.append("\"\"\"");
		return sb.toString();
	}

	/**
	 * Return all type references from a given node. TODO types are a tad
	 * fragile
	 *
	 * @param node the node to start with
	 * @return a list of types
	 */
	public static List<Type> getAllTypeReferences(ASTNode node) {
		List<Type> types = new ArrayList<>();
		node.accept(new ASTVisitor() {

		});
		return types;
	}

	/**
	 * Helper method to turn an annotation into something useful
	 *
	 * @param an the annotation
	 * @return a map with the annotations fields
	 */
	public static Map<String, Object> getAnnotationValues(IAnnotation an) throws JavaModelException {
		if (an.exists())
			return Stream.of(an.getMemberValuePairs())
				.collect(Collectors.toMap(m -> m.getMemberName(), m -> m.getValue()));
		return Collections.emptyMap();
	}

	/**
	 * Show a textual representation of an object. Handles arrays.
	 *
	 * @param obj the object
	 * @return a string representation.
	 */

	public static String toString(Object obj) {
		if (obj == null) {
			return "null";
		}
		if (obj.getClass()
			.isArray()) {
			if (obj instanceof Object[]) {
				return Arrays.deepToString((Object[]) obj);
			} else {
				int length = Array.getLength(obj);
				Object[] objArr = new Object[length];
				for (int i = 0; i < length; i++) {
					objArr[i] = Array.get(obj, i);
				}
				return Arrays.deepToString(objArr);
			}
		}
		return obj.toString();
	}

	/**
	 * Return the closest common type in the inheritance hiearchy
	 *
	 * @param a
	 * @param b
	 * @return the nearest common type
	 */
	static Class<?> commonType(Class<?> a, Class<?> b) {
		if (a == null) {
			if (b == null)
				return Object.class;
			return b;
		}

		Set<Class<?>> superTypes = new HashSet<>();
		Class<?> rover = b;
		while (rover != null) {
			superTypes.add(rover);
			rover = rover.getSuperclass();
		}
		while (a != null) {
			if (superTypes.contains(a)) {
				return a;
			}
			a = a.getSuperclass();
		}
		return Object.class;
	}

	final Memoize<ASTEngine>				engine;
	final Map<String, ImportDeclaration>	imports	= new LinkedHashMap<>();
	final Set<ImportDeclaration>			added	= new LinkedHashSet<>();
	final @Nullable ICompilationUnit		iunit;
	final String							source;

	/**
	 * Create an assistant when there is only a CompilationUnit
	 *
	 * @param unit the unit
	 */
	public RefactorAssistant(CompilationUnit unit) throws JavaModelException {
		this(unit, null);
	}

	/**
	 * Create a new assistant with a CompilationUnit and an optional
	 * ICompilationUnit. The ICompilationUnit can be used when the original
	 * source code in the Eclipse project needs to be updated.
	 *
	 * @param unit the compilation unit
	 * @param iunit the iunit, nullable
	 */
	public RefactorAssistant(CompilationUnit unit, @Nullable
	ICompilationUnit iunit) throws JavaModelException {
		engine = Memoize.supplier(() -> new ASTEngine(unit));
		source = iunit == null ? null : iunit.getSource();
		this.iunit = iunit;
		init();
	}

	/**
	 * Eclipse JavaProject has an elaborate structure and support to work with
	 * compilation units.
	 *
	 * @param iunit the icompilation unit
	 */
	public RefactorAssistant(ICompilationUnit iunit) throws JavaModelException {
		this.engine = Memoize.supplier(() -> {
			try {
				String unit = null;
				IResource underlyingResource = iunit.getUnderlyingResource();
				if (underlyingResource != null) {
					unit = underlyingResource.getFullPath()
						.lastSegment();
				}

				IJavaProject javaProject = iunit.getJavaProject();
				return new ASTEngine(javaProject, iunit.getSource(), AST.getJLSLatest(), ASTParser.K_COMPILATION_UNIT,
					unit);
			} catch (JavaModelException e) {
				throw Exceptions.duck(e);
			}
		});
		this.iunit = iunit;
		this.source = iunit.getSource();
		init();
	}

	/**
	 * A convenient method to get the source from a document
	 *
	 * @param source the source
	 */

	public RefactorAssistant(IDocument source) throws JavaModelException {
		this(source.get());
	}

	/**
	 * Create a Refactor Assistant on a source
	 *
	 * @param source the source code
	 */
	public RefactorAssistant(String source) throws JavaModelException {
		this(source, AST.getJLSLatest(), JavaSourceType.UNKNOWN, null);
	}

	/**
	 * Specify the details of a source file based compile
	 *
	 * @param source the source file
	 * @param release the release number of java (Java 1.4=4, Java 9=9, etc.)
	 * @param jst the source type
	 * @param options the options
	 */
	public RefactorAssistant(String source, int release, JavaSourceType jst, Map<String, String> options)
		throws JavaModelException {
		int parserKind = ASTParser.K_COMPILATION_UNIT;
		int pk = parserKind;

		this.engine = Memoize.supplier(() -> {
			String unit = ".";
			if (jst == JavaSourceType.PACKAGEINFO)
				unit = "package-info.java";
			else if (jst == JavaSourceType.MODULEINFO)
				unit = "module-info.java";
			return new ASTEngine(source, release, pk, unit, options);
		});
		this.source = source;
		this.iunit = null;
		init();
	}

	public void add(ASTNode parent, Annotation newAnnotation) {
		engine().insert(parent, Annotation.class, newAnnotation);
	}

	/**
	 * For annotations, arrays have special syntax. This method will adjust the
	 * expression so it will fit in an annotation.
	 *
	 * @param expr any expression
	 * @return an expression suitable for annotation values
	 */
	public Expression annotationValue(Expression expr) {
		if (expr instanceof ArrayCreation arrayCreation) {
			ArrayInitializer arrayInit = arrayCreation.getInitializer();
			if (arrayInit != null) {
				return annotationValue(arrayInit);
			}
		} else if (expr instanceof ArrayInitializer arrayInit) {
			List<?> values = arrayInit.expressions();
			if (values.size() == 1) {
				return copy((Expression) values.get(0));
			}
			return copy(arrayInit);
		}
		return expr;
	}

	/**
	 * Apply the text delta to the compilation unit
	 *
	 * @param iunit the compilation unit
	 * @param mon the monitor or null
	 * @throws Exception
	 */
	public void apply(ICompilationUnit iunit, @Nullable
	IProgressMonitor mon) throws Exception {
		TextEdit textEdit = getTextEdit(iunit.getSource());
		iunit.applyTextEdit(textEdit, mon);
	}

	/**
	 * Apply the text delta to the document
	 *
	 * @param document the doc
	 * @param mon the monitor or null
	 * @throws Exception
	 */
	public void apply(IDocument document, @Nullable
	IProgressMonitor mon) throws Exception {
		TextEdit textEdit = engine().getTextEdit(document);
		textEdit.apply(document);
	}

	/**
	 * Apply the text delta to the current source if it was specified as a
	 * ICompilationUnit
	 *
	 * @param mon the monitor or null
	 * @throws Exception
	 */
	public void apply(@Nullable
	IProgressMonitor mon) throws Exception {
		TextEdit textEdit = getTextEdit(getSource());
		ICompilationUnit copy = iunit.getWorkingCopy(mon);
		copy.applyTextEdit(textEdit, mon);
		copy.commitWorkingCopy(true, mon);
	}

	/**
	 * Create a copy of the node
	 *
	 * @param <T> the type of the node
	 * @param node the node
	 * @return a copy
	 */
	public <T extends ASTNode> T copy(T node) {
		return (T) ASTNode.copySubtree(ast(), node);
	}

	/**
	 * Create a cursor on the node.
	 *
	 * @param <T> the type of the node
	 * @param node the node
	 * @return a cursor
	 */
	public <T extends ASTNode> Cursor<T> cursor(T node) {
		return new CursorImpl<>(Collections.singletonList(node), node.getStartPosition(), node.getLength());
	}

	/**
	 * Delete the given node. This will record the del
	 *
	 * @param node
	 */
	public void delete(ASTNode node) {
		engine().remove(node);

	}

	public void deleteAnnotation(ASTNode pd, Annotation export) {
		String fqn = export.getTypeName()
			.getFullyQualifiedName();
		deleteAnnotation(pd, fqn);
	}

	public void deleteAnnotation(ASTNode pd, String name) {
		engine().removeIf(pd, Annotation.class, isEqualFQN(Annotation.class, name));
	}

	/**
	 * Return the underlying ASTEngine
	 */
	public ASTEngine engine() {
		return this.engine.get();
	}

	/**
	 * Ensure that the the child node is added, removing any nodes that have the
	 * same identity
	 *
	 * @param <T> the type
	 * @param node the parent
	 * @param type the type of the child
	 * @param child the child
	 */
	public <T extends ASTNode> void ensure(ASTNode node, Class<T> type, T child) {
		engine().ensure(node, type, isEqualFQN(type, child), child);
	}

	/**
	 * Ensure the given node has the given new Annotation. This will first
	 * remove an annotation with the same type name.
	 *
	 * @param node the parent node
	 * @param newAnnotation the new annotation
	 */
	public void ensureAnnotation(ASTNode node, Annotation newAnnotation) {
		engine().ensure(node, Annotation.class, isEqualFQN(Annotation.class, newAnnotation.getTypeName()
			.getFullyQualifiedName()), copy(newAnnotation));
	}

	/**
	 * Ensure that there is an import declaration for the given fully qualified
	 * name.
	 *
	 * @param fqn the impor
	 */
	public void ensureImportDeclaration(String fqn) {
		ImportDeclaration id = ast().newImportDeclaration();
		id.setStatic(false);
		if (fqn.endsWith(".*")) {
			fqn = fqn.substring(0, fqn.length() - 2);
			id.setOnDemand(true);
		} else
			id.setOnDemand(false);
		id.setName(createImportedName(fqn));

		ensure(unit(), ImportDeclaration.class, id);
	}

	/**
	 * In certain cases, annotation and the modifiers (public, private,
	 * volatile, etc) are _modifiers_. This will ensure that modifiers come
	 * after the annotations.
	 * <p>
	 * See {@link JavaModifier} for the conflicting modifiers, these will be
	 * removed before adding.
	 * <p>
	 * there is no package {@link JavaModifier#PACKAGE_PRIVATE} in the AST. This
	 * is handled specially by removing all conflicting modifiers, thereby not
	 * having an access modifier.
	 *
	 * @param node the parent node
	 * @param modifiers the modifiers
	 */
	public void ensureModifiers(ASTNode node, JavaModifier... modifiers) {
		Set<JavaModifier> toAdd = new HashSet<>(Set.of(modifiers));
		Set<JavaModifier> incompatible = toAdd.stream()
			.flatMap(jm -> JavaModifier.getConflictingModifiers(jm)
				.stream())
			.collect(Collectors.toSet());

		stream(node, Modifier.class).forEach(modifier -> {
			JavaModifier presentModifier = JavaModifier.of(modifier.getKeyword());
			toAdd.remove(presentModifier);
			if (incompatible.contains(presentModifier))
				engine().remove(node, modifier);
		});

		for (JavaModifier jm : toAdd) {
			if (jm.keyword != null) {
				Modifier modifier = ast().newModifier(jm.keyword);
				engine().insert(node, Modifier.class, modifier);
			}
		}
	}

	/**
	 * Ensure there is a package declaration.
	 *
	 * @param package_
	 * @return the created package declaration or old package declaration.
	 */
	public PackageDeclaration ensurePackageDeclaration(String package_) {
		PackageDeclaration package1 = unit().getPackage();
		if (package1 == null) {
			package1 = setPackageDeclaration(package_);
		}
		return package1;
	}

	/**
	 * Create an entry for the Annotation values
	 *
	 * @param name the name of the entry
	 * @param value the value, see {@link #newLiteral(Object)}
	 * @return an entry
	 */
	public Entry entry(String name, Object value) {
		return new Entry(name, value);
	}

	/**
	 * Final fixes. For example, the import clean up
	 */
	public void fixup() {
		if (engine.isPresent() && engine().hasChanged()) {
			Set<String> referred = getReferredTypes(unit());
			imports.forEach((k, v) -> {
				if (!k.startsWith(".") && !added.contains(v)) {
					String fqn = v.getName()
						.getFullyQualifiedName();
					if (!referred.contains(fqn))
						delete(v);
				}
			});
		}
	}

	/**
	 * Return a normal Java Object based on the literal node.
	 *
	 * @param e an expression
	 * @return an object, potentially this if we can't convert it
	 */
	public Object fromLiteral(Expression e) {
		if (e instanceof NullLiteral) {
			return null;
		}
		if (e instanceof StringLiteral sl) {
			return sl.getLiteralValue();
		}
		if (e instanceof TextBlock sl) {
			return fromTextBoxEscaped(sl.getEscapedValue());
		}
		if (e instanceof NumberLiteral nl) {
			String token = nl.getToken();
			return parseNumber(token);
		}
		if (e instanceof BooleanLiteral bl) {
			return bl.booleanValue();
		}
		if (e instanceof CharacterLiteral bl) {
			return bl.charValue();
		}
		if (e instanceof TypeLiteral tl) {
			return tl.getType();
		}
		return this;
	}

	/**
	 * Get an annotation by its type name
	 *
	 * @param node the node
	 * @param name the type name of the annotation
	 */
	public Optional<Annotation> getAnnotation(ASTNode node, String name) {
		return engine().stream(node, Annotation.class)
			.filter(isEqualFQN(Annotation.class, name))
			.findAny();
	}

	/**
	 * Stream all annotations.
	 *
	 * @param node the node
	 * @return a stream of all annotations
	 */
	public Stream<Annotation> getAnnotations(ASTNode node) {
		return engine().stream(node, Annotation.class);
	}

	/**
	 * Get the value of an annotation
	 *
	 * @param ann the annotation
	 * @param methodName the annotation method name
	 */
	public Optional<Expression> getAnnotationValue(Annotation ann, String methodName) {
		Map<String, Expression> annotationValues = getAnnotationMap(ann);
		Expression expression = annotationValues.get(methodName);
		return Optional.ofNullable(expression);
	}

	/**
	 * Get the value of an annotation and convert it to the desired type
	 *
	 * @param ann the annotation
	 * @param methodName the annotation method name
	 */
	public <T> Optional<T> getAnnotationValue(Annotation ann, String methodName, java.lang.reflect.Type type) {
		return getAnnotationValue(ann, methodName).flatMap(expression -> {
			try {
				Object value = fromLiteral(expression);
				value = Converter.cnv(type, value);
				return (Optional<? extends T>) Optional.of(value);
			} catch (Exception e) {
				e.printStackTrace();
				return Optional.empty();
			}
		});
	}

	/**
	 * Get the value of an annotation method with type conversion
	 *
	 * @param <T> the type of the method
	 * @param ann the annotation
	 * @param methodName the name of the method
	 * @param type the type
	 */
	public <T> Optional<T> getAnnotationValue(Annotation ann, String methodName, Class<T> type) {
		java.lang.reflect.Type t = type;
		return getAnnotationValue(ann, methodName, t);
	}

	/**
	 * Get an annotation as a DTO. Each field in the DTO should refer to an
	 * annotation method.
	 *
	 * @param <T> the DTO type
	 * @param node the node
	 * @param annotationName the name of the annotation
	 * @param dtoClass the type of the DTO
	 */
	public <T> Optional<T> getAnnotationDTO(ASTNode node, String annotationName, Class<T> dtoClass) {
		return getAnnotation(node, annotationName).flatMap(annotation -> {
			try {
				T instance = dtoClass.getConstructor()
					.newInstance();

				for (Field m : dtoClass.getFields()) {
					m.setAccessible(true);
					String name = m.getName();
					getAnnotationValue(annotation, name, m.getGenericType()).ifPresent(value -> {
						try {
							m.set(instance, value);
						} catch (Exception e) {
							e.printStackTrace();
						}
					});
				}
				return Optional.of(instance);
			} catch (Exception e) {
				e.printStackTrace();
				return Optional.empty();
			}
		});
	}

	/**
	 * Get a Expression node of an annotation method
	 *
	 * @param node the parent node
	 * @param annotationName the type name of the annotation
	 * @param methodName the method name of the annnotation
	 */
	public Optional<Expression> getAnnotationValue(ASTNode node, String annotationName, String methodName) {
		return getAnnotation(node, annotationName).flatMap(ann -> getAnnotationValue(ann, methodName));
	}

	/**
	 * Get a the value of an annotation method in a Java type
	 *
	 * @param <T> the type of the annotation value
	 * @param node the parent node of the annotation
	 * @param typeName the type name of the annotation
	 * @param methodName the method name of the annotation
	 * @param expectedType the type this should be converted in
	 */
	public <T> Optional<T> getAnnotationValue(ASTNode node, String typeName, String methodName, Class<T> expectedType) {
		return getAnnotation(node, typeName).flatMap(ann -> getAnnotationValue(ann, methodName, expectedType));
	}

	/**
	 * Return the annotation values as a map
	 *
	 * @param an annotation
	 */
	public Map<String, Expression> getAnnotationMap(Annotation an) {
		if (an instanceof MarkerAnnotation) {
			return Collections.emptyMap();
		}
		if (an instanceof SingleMemberAnnotation san) {
			Expression value = san.getValue();
			return Collections.singletonMap("value", value);
		}
		if (an instanceof NormalAnnotation nan) {
			Map<String, Expression> map = new HashMap<>();
			return stream(nan, MemberValuePair.class).collect(Collectors.toMap(mvp -> mvp.getName()
				.getIdentifier(), mvp -> mvp.getValue()));
		}

		return Collections.emptyMap();
	}

	/**
	 * Return the annotation values as a map
	 *
	 * @param node the node
	 * @param annotationName the name of the annotation type
	 */
	public Map<String, Expression> getAnnotationMap(ASTNode node, String annotationName) {
		return getAnnotation(node, annotationName).map(this::getAnnotationMap)
			.orElse(Collections.emptyMap());
	}

	/**
	 * Get the compilation unit of this assistant
	 */
	public CompilationUnit getCompilationUnit() {
		return unit();
	}

	/**
	 * Get the cursor from the given position, this will always succeed if the
	 * start and length are withing the source.
	 *
	 * @param start the start position
	 * @param length the length to include
	 */
	public Cursor<?> getCursor(int start, int length) {
		try {
			assert start >= 0;
			assert length >= 0;

			String source = getSource();
			assert length + start <= source.length();

			ASTNode node = NodeFinder.perform(getCompilationUnit(), start, length);
			return new CursorImpl<>(Collections.singletonList(node), start, length);
		} catch (Exception e) {
			throw Exceptions.duck(e);
		}
	}

	/**
	 * Get the cursor by looking in the source buffer and finding the given
	 * regex. Group 1's region is used for start/length. If not set, group 0 is
	 * used.
	 *
	 * @param regex the regular expressions
	 * @throws JavaModelException
	 */

	public Cursor<?> getCursor(RE regex) throws JavaModelException {
		return regex.findIn(getSource())
			.map(match -> {
				MatchGroup g = match.group(1)
					.orElse(match.group(0)
						.get());

				return getCursor(g.start(), g.end() - g.start());
			})
			.orElse(new FailedCursor("getCursor(%s) not found", regex));
	}

	/**
	 * Get the cursor by looking in the source buffer and finding the given
	 * regex. Group 1's region is used for start/length. If not set, group 0 is
	 * used.
	 *
	 * @param regex the regular expressions
	 * @throws JavaModelException
	 */
	public Cursor<?> getCursor(String regex) throws JavaModelException {
		RE re = re(regex);
		return getCursor(re);
	}

	/**
	 * Get the cursor by looking in the source buffer and finding the given
	 * regex. Group 1's region is used for start/length. If not set, group 0 is
	 * used.
	 *
	 * @param regex the regular expressions
	 * @throws JavaModelException
	 */
	public Cursor<?> getCursor(Pattern regex) throws JavaModelException {
		RE re = re(regex.pattern());
		return getCursor(re);
	}

	/**
	 * TODO still a bit fuzzy Most node types have a certain unique id in a
	 * scope. This is the identity. This method find the identity of a node. See
	 * {@link #getIdentity(Class)}
	 *
	 * @param node the node for which an identity string is sought
	 * @return the identity or null
	 */
	public String getIdentifier(ASTNode node) {
		return getIdentity((Class<ASTNode>) node.getClass()).apply(node);
	}

	/**
	 * TODO still a bit fuzzy.
	 *
	 * @param <T> the type of the node
	 * @param type the type of the node
	 * @return a function that maps a node to a string identity or null
	 */

	public <T extends ASTNode> Function<T, String> getIdentity(Class<T> type) {

		if (type == Annotation.class) {
			return a -> {
				Annotation an = (Annotation) a;
				return an.getTypeName()
					.getFullyQualifiedName();
			};
		}
		if (type == SingleVariableDeclaration.class) {
			return a -> {
				SingleVariableDeclaration an = (SingleVariableDeclaration) a;
				return an.getName()
					.getIdentifier();
			};
		}
		if (type == ImportDeclaration.class) {
			return a -> {
				ImportDeclaration an = (ImportDeclaration) a;
				return an.getName()
					.getFullyQualifiedName();
			};
		}
		if (type == PackageDeclaration.class) {
			return a -> {
				PackageDeclaration an = (PackageDeclaration) a;
				return an.getName()
					.getFullyQualifiedName();
			};
		}
		if (type == MethodDeclaration.class) {
			return a -> {
				MethodDeclaration an = (MethodDeclaration) a;
				return an.getName()
					.getFullyQualifiedName();
			};
		}
		if (type == VariableDeclarationFragment.class) {
			return a -> {
				VariableDeclarationFragment an = (VariableDeclarationFragment) a;
				return an.getName()
					.getFullyQualifiedName();
			};
		}

		// TODO add additional types

		throw new IllegalArgumentException("unknown type to get the identity string");
	}

	/**
	 * Get the list of import declarations.
	 *
	 * @return a list of import declarations
	 */
	public List<ImportDeclaration> getImports() {
		List imports2 = unit().imports();
		if (imports2 == null)
			return new ArrayList<>();
		else
			return new ArrayList<>(imports2);
	}

	/**
	 * Get the JavaSourceType. There are some slightly different issues at play
	 * here. First, there are source types like moduleinfo, packageinfo, and
	 * normal class file. Second, the specific variation we're in like enum,
	 * class, interface, record, etc.
	 * <p>
	 * This will check if we're in a PACKAGEINFO or MODULEINFO file and return
	 * appropriately. Otherwise it will move upward and try to find a
	 * declaration that indicates it is a Class, Interface, Annotation, Enum, or
	 * Record.
	 *
	 * @param node the starting node
	 * @return the source type
	 */
	public JavaSourceType getJavaSourceType(ASTNode node) {
		ASTNode rover = node;

		while (rover != null) {
			if (rover instanceof CompilationUnit cu) {
				PackageDeclaration packageDecl = cu.getPackage();
				ModuleDeclaration moduleDecl = cu.getModule();

				if (packageDecl != null && cu.types()
					.isEmpty()) {
					return JavaSourceType.PACKAGEINFO;

				} else if (moduleDecl != null) {
					return JavaSourceType.MODULEINFO;
				}
				return JavaSourceType.TYPES;
			}
			if (rover instanceof ModuleDeclaration td) {
				return JavaSourceType.MODULEINFO;
			}
			if (rover instanceof TypeDeclaration td) {
				if (td.isInterface()) {
					return JavaSourceType.INTERFACE;
				}
				return JavaSourceType.CLASS;
			}

			if (rover instanceof EnumDeclaration td) {
				return JavaSourceType.ENUM;
			}

			if (rover instanceof AnnotationTypeDeclaration td) {
				return JavaSourceType.ANNOTATION;
			}

			if (rover instanceof RecordDeclaration td) {
				return JavaSourceType.RECORD;
			}

			rover = rover.getParent();
		}
		return JavaSourceType.UNKNOWN;
	}

	/**
	 * This turns a FQN into a Name and will, if possible add an import so the
	 * short name can be used.
	 *
	 * @param name the name, either an FQN or simple
	 * @return a Name
	 */
	public Name createImportedName(String name) {
		String[] parts = splitName(name);
		SimpleName simpleName = ast().newSimpleName(parts[1]);

		if (parts[0] == null || parts[0].equals("java.lang"))
			return simpleName;

		Name qualifier = ast().newName(parts[0]);
		QualifiedName qualifiedName = ast().newQualifiedName(qualifier, simpleName);
		simpleName = ast().newSimpleName(parts[1]);

		ImportDeclaration imported = imports.get(parts[1]);
		if (imported != null) {
			String fqn = imported.getName()
				.getFullyQualifiedName();

			if (name.equals(fqn))
				return simpleName;

			return qualifiedName;
		}

		ImportDeclaration newImportDeclaration = ast().newImportDeclaration();
		newImportDeclaration.setName(qualifiedName);
		engine().insertLast(unit(), ImportDeclaration.class, newImportDeclaration);
		added.add(newImportDeclaration);
		imports.put(toImportKey(qualifiedName, false, false), newImportDeclaration);
		return simpleName;
	}

	/**
	 * Get the current Package Declaration of the Compilation Unit
	 */
	public Optional<PackageDeclaration> getPackageDeclaration() {
		return Optional.ofNullable(engine().unit.getPackage());
	}

	/**
	 * Guess the types this node refers to TODO might need some work, see
	 * {@link #resolve(String)}
	 *
	 * @param node the node
	 */

	public Optional<String> getReferredType(ASTNode node) {
		if (node instanceof Annotation ann) {
			return Optional.of(ann.getTypeName()
				.toString());
		}
		if (node instanceof SimpleType x) {
			return Optional.of(x.getName()
				.toString());
		}

		if (node instanceof Name nm) {
			if (node instanceof QualifiedName qualifiedName) {
				SimpleName head = qualifiedName.getName();
				String headName = head.getIdentifier();

				if (qualifiedName.getQualifier() instanceof SimpleName simple) {
					// Collections.EMPTY_LIST
					String simpleName = simple.getIdentifier();
					if (imports.containsKey(simpleName))
						return Optional.of(simpleName);
				}

				if (headName.equals(headName.toUpperCase())) {
					return Optional.of(qualifiedName.getFullyQualifiedName());
				}

				return Optional.of(qualifiedName.toString());
			} else {
				SimpleName simpleName = (SimpleName) node;
				ImportDeclaration qualified = imports.get(simpleName.getIdentifier());
				if (qualified != null) {
					return Optional.of(simpleName.getIdentifier());
				}
			}
		}

		return Optional.empty();
	}

	/**
	 * Return a list of referred types from the node down.
	 *
	 * @param node the node to start
	 * @return a set of types
	 */
	public Set<String> getReferredTypes(ASTNode node) {
		Set<Name> set = new HashSet<>();
		TypeGatherer visitor = new TypeGatherer(node, engine().removed);
		node.accept(visitor);
		engine().added.forEach(n -> {
			n.accept(visitor);
		});
		return visitor.typeReferences.stream()
			.map(s -> resolve(s).orElse(null))
			.filter(Objects::nonNull)
			.collect(Collectors.toSet());
	}

	/**
	 * Get the current source. This can be the original string or from a
	 * {@link ICompilationUnit} if specified.
	 *
	 * @throws JavaModelException
	 */
	public String getSource() throws JavaModelException {
		if (source != null)
			return source;
		if (iunit != null)
			return iunit.getSource();
		throw new UnsupportedOperationException("no source");
	}

	/**
	 * Get the current text delta based on the original source
	 */
	public TextEdit getTextEdit() throws JavaModelException {
		return engine().getTextEdit(getSource());
	}

	/**
	 * Get the text delta on the given source from a document
	 *
	 * @param source the source code
	 */
	public TextEdit getTextEdit(IDocument source) {
		return engine().getTextEdit(source);
	}

	/**
	 * Get the text delta on the given source
	 *
	 * @param source the source code
	 */
	public TextEdit getTextEdit(String source) {
		return engine().getTextEdit(source);
	}

	/**
	 * This is a fuzzy method. It takes common nodes and designates an aspect as
	 * its type. I.e. a Field has a type, a method has a return type, a
	 * parameter has a type. TODO might use some love
	 *
	 * @param node the node
	 * @return the type
	 */
	public Optional<Type> getType(ASTNode node) {
		Type type = null;
		if (node instanceof MethodDeclaration) {
			type = ((MethodDeclaration) node).getReturnType2();
		} else if (node instanceof SingleVariableDeclaration svd) {
			type = svd.getType();
		} else if (node instanceof FieldDeclaration fd) {
			type = fd.getType();
		} else if (node instanceof VariableDeclarationFragment svd) {
			return getType(svd.getParent()).map(t -> {
				int extraDimensions = svd.getExtraDimensions();
				Type r = t;
				while (extraDimensions-- > 0) {
					r = svd.getAST()
						.newArrayType(r);
				}
				return r;
			});
		}
		return Optional.ofNullable(type);
	}

	/**
	 * Check if the node has the given annotation
	 *
	 * @param node the node
	 * @param name the annotation type name
	 * @return true if the given annotation is on the node, otherwise false
	 */
	public boolean hasAnnotation(ASTNode node, String name) {
		return getAnnotation(node, name).isPresent();
	}

	/**
	 * Check if the node has the given modifier
	 *
	 * @param node the node
	 * @param modifiers
	 * @return true if the given modifier is on the node, otherwise false
	 */
	public boolean hasModifiers(ASTNode node, JavaModifier... modifiers) {
		Set<ModifierKeyword> wanted = Set.of(modifiers)
			.stream()
			.map(m -> m.keyword)
			.collect(Collectors.toSet());

		Set<ModifierKeyword> present = stream(node, Modifier.class).map(modifier -> modifier.getKeyword())
			.collect(Collectors.toSet());

		return present.containsAll(wanted);
	}

	/**
	 * Insert a node
	 *
	 * @param <T> the type
	 * @param node the parent node
	 * @param class1 the type of the list
	 * @param es the node to insert
	 */
	public <T extends ASTNode> void insert(ASTNode node, Class<T> class1, T es) {
		engine().insert(node, class1, copy(es));
	}

	/**
	 * Insert a node after another one
	 *
	 * @param <T> the type of the nodes
	 * @param before the node to insert after
	 * @param newAfter the to be inserted node
	 */
	public <T extends ASTNode> void insertAfter(T before, T newAfter) {
		engine().insertAfter(before, copy(newAfter));
	}

	public <T extends ASTNode> Predicate<T> isEqualFQN(Class<T> type, String a) {
		String[] aName = splitName(a);
		int aN = aName[0] == null ? 0 : 1;

		Function<T, String> identity = getIdentity(type);

		return left -> {
			String b = identity.apply(left);
			String[] bName = splitName(b);

			if (!bName[1].equals(aName[1])) // simple name
				return false;

			int bN = bName[0] == null ? 0 : 2;

			return switch (aN + bN) {
				/*
				 * a,b both simple names
				 */
				case 0 -> true;
				/*
				 * a=foo.X and b=X
				 */
				case 1 -> resolve(b).map(bFqn -> bFqn.equals(a))
					.orElseGet(() -> imports.containsKey(".D_" + aName[0]));
				/*
				 * a=X and b=foo.X
				 */
				case 2 -> resolve(a).map(aFqn -> aFqn.equals(b))
					.orElseGet(() -> imports.containsKey(".D_" + bName[0]));
				/*
				 * a=foo.X and b= bar.X
				 */
				case 3 -> aName[0].equals(bName[0]);

				/*
				 * others
				 */
				default -> throw new IllegalArgumentException("impossible");
			};

		};
	}

	public <T extends ASTNode> Predicate<T> isEqualFQN(Class<T> type, T a) {
		Function<T, String> identity = getIdentity(type);
		String as = identity.apply(a);
		return isEqualFQN(type, as);
	}

	public Annotation newAnnotation(String name) {
		MarkerAnnotation simpleAnnotation = ast().newMarkerAnnotation();
		simpleAnnotation.setTypeName(createImportedName(name));
		return simpleAnnotation;
	}

	public Annotation newAnnotation(String annName, Entry... entries) {
		return newAnnotation0(annName, entries);
	}

	public Annotation newAnnotation(String annName, Object value) {
		if (value instanceof Entry e) {
			return newAnnotation0(annName, e);
		}
		if (value instanceof DTO dto) {
			return newAnnotationFromDTO(annName, dto);
		}
		if (value instanceof Map map) {
			return newAnnotationFromMap(annName, map);
		}
		SingleMemberAnnotation annotationWithValue = ast().newSingleMemberAnnotation();
		annotationWithValue.setTypeName(createImportedName(annName));
		Expression literal = newLiteral(value);
		annotationWithValue.setValue(literal);
		return annotationWithValue;
	}

	public Annotation newAnnotationFromDTO(String annName, DTO values) {
		try {
			Class<? extends DTO> dtoClass = values.getClass();

			Field[] fields = dtoClass.getFields();
			if (fields.length == 1) {
				Field single = fields[0];
				if (single.getName()
					.equals("value")) {
					Object value = single.get(values);
					if (value != null)
						return newAnnotation(annName, value);
				}
			}

			List<Entry> entries = new ArrayList<>();
			for (Field m : fields) {
				String name = m.getName();
				Object value = m.get(values);
				if (value == null)
					continue;

				Entry entry = new Entry(name, value);
				entries.add(entry);
			}
			return newAnnotation(annName, entries.toArray(Entry[]::new));
		} catch (Exception e) {
			throw Exceptions.duck(e);
		}

	}

	public Annotation newAnnotationFromMap(String annName, Map<String, Object> values) {
		try {

			if (values.size() == 1 && values.containsKey("value")) {

				Object value = values.get("value");
				if (value != null)
					return newAnnotation(annName, value);
			}

			List<Entry> entries = new ArrayList<>();
			values.forEach((k, v) -> {
				Entry entry = new Entry(k, v);
				entries.add(entry);
			});
			return newAnnotation(annName, entries.toArray(Entry[]::new));
		} catch (Exception e) {
			throw Exceptions.duck(e);
		}
	}

	public Assignment newAssignment(Expression left, Expression right) {
		Assignment assignment = ast().newAssignment();
		assignment.setLeftHandSide(copy(left));
		assignment.setRightHandSide(copy(right));
		return assignment;
	}

	public BooleanLiteral newBooleanLiteral(boolean value) {
		return ast().newBooleanLiteral(value);
	}

	public CharacterLiteral newCharacterLiteral(char value) {
		CharacterLiteral literal = ast().newCharacterLiteral();
		literal.setCharValue(value);
		return literal;
	}

	public Expression newExpression(Object value) {
		if (value instanceof Expression exp)
			return copy(exp);
		return newLiteral(value);
	}

	public ExpressionStatement newExpressionStatement(Expression expression) {
		return ast().newExpressionStatement(copy(expression));
	}

	public FieldAccess newFieldAccess(Expression thisExpression, String identifier) {
		FieldAccess newFieldAccess = ast().newFieldAccess();
		newFieldAccess.setExpression(copy(thisExpression));
		newFieldAccess.setName(newSimpleName(identifier));
		return newFieldAccess;
	}

	public FieldDeclaration newFieldDeclaration(TypeDeclaration parent, String identifier, Type type,
		JavaModifier... modifiers) {
		VariableDeclarationFragment fragment = ast().newVariableDeclarationFragment();
		fragment.setName(newSimpleName(identifier));
		FieldDeclaration newField = ast().newFieldDeclaration(fragment);
		newField.setType(copy(type));
		List fieldModifiers = newField.modifiers();
		for (JavaModifier m : modifiers) {
			Modifier newModifier = ast().newModifier(m.keyword);
			fieldModifiers.add(newModifier);
		}
		return newField;
	}

	public Expression newLiteral(Object value) {
		if (value == null) {
			return ast().newNullLiteral();
		}
		if (value instanceof Expression ex) {
			return ex;
		} else if (value instanceof Boolean b) {
			return newBooleanLiteral(b);
		} else if (value instanceof Character c) {
			return newCharacterLiteral(c);
		} else if (value instanceof Number number) {
			return newNumberLiteral(number);
		} else if (value instanceof String s) {
			return newStringLiteral(s);
		} else if (value instanceof Class c) {
			return newTypeLiteral(c);
		} else if (value instanceof Collection c) {
			ArrayInitializer arrayInitializer = ast().newArrayInitializer();
			Class<?> mergedType = null;
			for (Object elem : c) {
				arrayInitializer.expressions()
					.add(newLiteral(elem));
				mergedType = commonType(mergedType, elem.getClass());
			}
			ArrayCreation arrayCreation = ast().newArrayCreation();
			arrayCreation.setInitializer(arrayInitializer);

			if (mergedType == null)
				mergedType = Object.class;

			Type type = newType(mergedType.getName());

			arrayCreation.setType(ast().newArrayType(type));
			return arrayCreation;
		} else if (value.getClass()
			.isArray()) {
			ArrayInitializer arrayInitializer = ast().newArrayInitializer();
			int length = Array.getLength(value);
			for (int i = 0; i < length; i++) {
				Object member = Array.get(value, i);
				arrayInitializer.expressions()
					.add(newLiteral(member));
			}
			ArrayCreation arrayCreation = ast().newArrayCreation();
			arrayCreation.setInitializer(arrayInitializer);
			Type type = newType(value.getClass()
				.getComponentType()
				.getName());
			arrayCreation.setType(ast().newArrayType(type));
			return arrayCreation;
		}
		throw new IllegalArgumentException(
			"Unsupported type for ASTNode creation : " + value + " of type " + value.getClass());
	}

	public MethodDeclaration newMethodDeclaration(String methodName) {
		MethodDeclaration md = ast().newMethodDeclaration();
		md.setName(ast().newSimpleName(methodName));
		return md;
	}

	public MethodDeclaration newMethodDeclaration(TypeDeclaration typeDecl, String methodName, String returnType,
		Map<String, String> parameters) {

		MethodDeclaration method = newMethodDeclaration(methodName);
		method.setReturnType2(newType(returnType));

		parameters.forEach((identifier, type) -> {
			SingleVariableDeclaration param = newSingleVariableDeclaration(identifier, type);
			method.parameters()
				.add(param);
		});

		Block body = ast().newBlock();
		method.setBody(body);

		if (!VOID.equals(returnType)) {
			ReturnStatement returnStatement = ast().newReturnStatement();
			Expression returnValue = ast().newNullLiteral();
			returnStatement.setExpression(returnValue);
			body.statements()
				.add(returnStatement);
		}

		return method;
	}

	public NumberLiteral newNumberLiteral(Number value) {
		NumberLiteral literal = ast().newNumberLiteral();
		String token = value.toString();

		if (value instanceof Double) {
			double v = Math.abs(value.doubleValue());
			if (v >= Float.MAX_VALUE || v <= Float.MIN_VALUE) {
				token += "D";
			}
		} else if (value instanceof Long) {
			long v = value.longValue();
			if (v >= Integer.MAX_VALUE || v <= Integer.MIN_VALUE) {
				token += "L";
			}
		}
		literal.setToken(token);
		return literal;
	}

	/**
	 * Create a new unconnected Package Declaration
	 */
	public PackageDeclaration newPackageDeclaration(String package_) {
		PackageDeclaration pd = ast().newPackageDeclaration();
		pd.setName(createImportedName(package_));
		return pd;
	}

	public PrimitiveType newPrimitiveType(String name) {
		PrimitiveType.Code code = switch (name) {
			case "void" -> PrimitiveType.VOID;
			case "boolean" -> PrimitiveType.BOOLEAN;
			case "byte" -> PrimitiveType.BYTE;
			case "short" -> PrimitiveType.SHORT;
			case "char" -> PrimitiveType.CHAR;
			case "int" -> PrimitiveType.INT;
			case "long" -> PrimitiveType.LONG;
			case "float" -> PrimitiveType.FLOAT;
			case "double" -> PrimitiveType.DOUBLE;
			default -> throw new IllegalArgumentException("no such primitive " + name);
		};
		return ast().newPrimitiveType(code);
	}

	public SimpleName newSimpleName(String identifier) {
		return ast().newSimpleName(identifier);
	}

	public SingleVariableDeclaration newSingleVariableDeclaration(String name, String type) {
		SingleVariableDeclaration sd = ast().newSingleVariableDeclaration();
		sd.setName(newSimpleName(name));
		sd.setType(newType(type));
		return sd;
	}

	public StringLiteral newStringLiteral(String literal) {
		StringLiteral newStringLiteral = ast().newStringLiteral();
		newStringLiteral.setLiteralValue(literal);
		return newStringLiteral;
	}

	public TextBlock newTextBlock(String s) {
		TextBlock newTextBlock = ast().newTextBlock();
		String newer = toTextBoxEscaped(s, 8);
		newTextBlock.setEscapedValue(newer);
		return newTextBlock;
	}

	public ThisExpression newThisExpression() {
		return ast().newThisExpression();
	}

	public TypeLiteral newTypeLiteral(Class c) {
		return newTypeLiteral(c.getName());
	}

	public TypeLiteral newTypeLiteral(String c) {
		TypeLiteral lit = ast().newTypeLiteral();
		Type newType = newType(c);
		lit.setType(newType);
		return lit;
	}

	public void replace(NumberLiteral older, String token) {
		NumberLiteral newLiteral = ast().newNumberLiteral();
		newLiteral.setToken(token);
		engine().replace(older, newLiteral);

	}

	public void replace(StringLiteral older, String text) {
		StringLiteral newer = newStringLiteral(text);
		engine().replace(older, newer);

	}

	public <T extends Expression> void replace(T older, ASTNode newNode) {
		engine().replace(older, newNode);

	}

	public <T extends Expression> void replace(T older, Object value) {
		Expression newLiteral = newLiteral(value);
		engine().replace(older, newLiteral);

	}

	public void replace(TextBlock older, String token) {
		TextBlock newTextBlock = newTextBlock(token);
		engine().replace(older, newTextBlock);

	}

	public Optional<String> resolve(String t) {
		init();
		ImportDeclaration resolved = imports.get(t);
		if (resolved == null || resolved.isOnDemand() || resolved.isStatic())
			return Optional.empty();

		String fullyQualifiedName = resolved.getName()
			.getFullyQualifiedName();
		return Optional.ofNullable(fullyQualifiedName);
	}

	public Optional<String> resolve(Type type) {
		ITypeBinding resolveBinding = type.resolveBinding();
		if (resolveBinding != null) {
			return Optional.of(resolveBinding.getQualifiedName());
		}

		if (type instanceof PrimitiveType pt) {
			return Optional.of(pt.getPrimitiveTypeCode()
				.toString());
		}

		String resolve = null;
		if (type instanceof SimpleType st) {
			Name name = st.getName();
			if (name instanceof SimpleName sn) {
				resolve = sn.getIdentifier();
				ImportDeclaration resolved = imports.get(resolve);
				if (resolved == null) {
					if (javalang.contains(resolve))
						return Optional.of("java.lang." + resolve);
					return Optional.empty();
				} else {
					return Optional.ofNullable(resolved.getName()
						.getFullyQualifiedName());
				}
			} else if (name instanceof QualifiedName qn) {
				return Optional.of(qn.getFullyQualifiedName());
			}
		} else if (type instanceof ArrayType at) {
			Type et = at.getElementType();
			return resolve(et).map(s -> {
				StringBuilder sb = new StringBuilder(s);
				for (int i = 0; i < at.getDimensions(); i++) {
					sb.append("[]");
				}
				return sb.toString();
			});
		} else if (type instanceof NameQualifiedType nt) {
			SimpleName name = nt.getName();
			resolve = name.getIdentifier();
			ImportDeclaration resolved = imports.get(resolve);
			if (resolved == null) {
				return Optional.empty();
			} else
				return Optional.of(resolved.getName()
					.getFullyQualifiedName());
		} else if (type instanceof WildcardType wt) {
			return resolve(wt.getBound());
		} else if (type instanceof IntersectionType it) {
			return Optional.empty();
		} else if (type instanceof ParameterizedType pt) {
			return resolve(pt.getType());
		} else if (type instanceof UnionType pt) {
			Type first = (Type) pt.types()
				.get(0);
			return resolve(first);
		}
		return Optional.empty();
	}

	/**
	 * Set the package declaration
	 *
	 * @param package_ the package name
	 */
	public PackageDeclaration setPackageDeclaration(String package_) {
		PackageDeclaration pd = newPackageDeclaration(package_);
		engine().set(unit(), PackageDeclaration.class, pd);
		return pd;
	}

	public <T extends ASTNode> Stream<T> stream(ASTNode node, Class<T> type) {
		return engine().stream(node, type);
	}

	private AST ast() {
		return engine.get().ast;
	}

	private void init() {
		int onDemand = 1000;
		if (imports.isEmpty()) {
			for (ImportDeclaration id : getImports()) {
				String importKey = toImportKey(id.getName(), id.isStatic(), id.isOnDemand());
				imports.put(importKey, id);
			}
		}
	}

	/**
	 * <ul>
	 * <li>For a regular on-demand import, this is the name of a package.
	 * <li>For a static on-demand import, this is the qualified name of a type.
	 * <li>For a regular single-type import, this is the qualified name of a
	 * type.
	 * <li>For a static single-type import, this is the qualified name of a
	 * static member of a type.
	 * </ul>
	 */

	private String toImportKey(Name name, boolean static1, boolean onDemand2) {
		int n = static1 ? 1 : 0;
		n += onDemand2 ? 2 : 0;
		return switch (n) {
			case 0 -> ((QualifiedName) name).getName()
				.getIdentifier();
			case 1 -> ".S_" + ((QualifiedName) name).getName()
				.getIdentifier();
			case 2 -> ".D_" + name.getFullyQualifiedName();
			case 3 -> ".SD_" + name.getFullyQualifiedName();
			default -> throw new IllegalArgumentException("Unexpected value: " + n);
		};
	}

	private Annotation newAnnotation0(String annName, Entry... entries) {
		NormalAnnotation normalAnnotation = ast().newNormalAnnotation();
		normalAnnotation.setTypeName(createImportedName(annName));
		for (Entry e : entries) {
			MemberValuePair f = ast().newMemberValuePair();
			f.setName(ast().newSimpleName(e.name));
			Expression value = newExpression(e.value);
			value = annotationValue(value);
			f.setValue(value);
			normalAnnotation.values()
				.add(f);
		}
		return normalAnnotation;
	}

	private Type newType(String name) {
		if (PRIMITIVES_P.matches(name)
			.isPresent())
			return newPrimitiveType(name);

		Name xname = createImportedName(name);
		return ast().newSimpleType(xname);
	}

	private Number parseNumber(String token) {
		token = token.toLowerCase();
		boolean integer = true;
		BiFunction<String, Integer, Number> conversion;
		if (token.endsWith("l")) {
			token = token.substring(0, token.length() - 1);
			conversion = Long::parseLong;
		} else if (token.endsWith("f")) {
			token = token.substring(0, token.length() - 1);
			conversion = (s, r) -> Float.parseFloat(s);
			integer = false;
		} else if (token.endsWith("d")) {
			token = token.substring(0, token.length() - 1);
			conversion = (s, r) -> Double.parseDouble(s);
			integer = false;
		} else {
			conversion = Integer::parseInt;
		}
		int radix = 10;
		if (token.startsWith("0")) {
			if (token.startsWith("0x")) {
				token = token.substring(2);
				radix = 16;
			}
			if (token.startsWith("0b")) {
				radix = 2;
				token = token.substring(2);
			} else {
				token = token.substring(1);
				radix = 8;
			}
		}
		return conversion.apply(token, radix);
	}

	private String[] splitName(String fqn) {
		int n = fqn.lastIndexOf('.');
		if (n >= 0) {
			return new String[] {
				fqn.substring(0, n), fqn.substring(n + 1)
			};
		} else {
			return new String[] {
				null, fqn
			};
		}
	}

	private CompilationUnit unit() {
		return engine.get().unit;
	}

	public <T extends ASTNode> T getAncestor(ASTNode child, Class<T> type) {
		ASTNode rover = child;
		while (rover != null) {
			if (type.isInstance(rover))
				return type.cast(rover);
			rover = rover.getParent();
		}
		throw new IllegalArgumentException(child + " has no ancestor of type " + type.getSimpleName());
	}

	public Set<String> getFieldNames(TypeDeclaration type) {
		return stream(type, FieldDeclaration.class).flatMap(fd -> stream(fd, VariableDeclarationFragment.class))
			.map(vdf -> vdf.getName()
				.getIdentifier())
			.collect(Collectors.toSet());
	}

	public int getDistance(ASTNode selected, ASTNode ancestor, int deflt) {
		int count = 0;
		ASTNode rover = selected;
		while (rover != null) {
			if (rover == ancestor)
				return count;
			count++;
			rover = rover.getParent();
		}
		return deflt;
	}

}
