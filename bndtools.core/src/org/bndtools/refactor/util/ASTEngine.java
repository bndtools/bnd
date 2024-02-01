package org.bndtools.refactor.util;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodHandles.Lookup;
import java.lang.invoke.MethodType;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Stream;

import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.ChildListPropertyDescriptor;
import org.eclipse.jdt.core.dom.ChildPropertyDescriptor;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.StructuralPropertyDescriptor;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.dom.rewrite.ListRewrite;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.eclipse.text.edits.TextEdit;

import aQute.lib.collections.MultiMap;

/**
 * Provides a facade to the AST nodes that uses the java type system to find the
 * nodes instead of the rather cumbersome descriptors/keys.
 */
@SuppressWarnings({
	"rawtypes", "unchecked"
})
public class ASTEngine {

	final static MultiMap<Class, StructuralPropertyDescriptor>	descriptors				= new MultiMap<>();
	final static Lookup											lookup					= MethodHandles.lookup();
	final static MethodType										descriptorsMethodType	= MethodType
		.methodType(List.class, int.class);

	final CompilationUnit										unit;
	final AST													ast;
	final ASTRewrite											rewriter;
	final String												source;
	final Set<ASTNode>											removed					= new HashSet<>();
	final Set<ASTNode>											added					= new HashSet<>();

	/**
	 * If a CompilationUnit is available, we can provide some extra features and
	 * then we do not have to compile the source
	 *
	 * @param unit the compilation unit
	 */
	public ASTEngine(CompilationUnit unit) {
		this.unit = unit;
		this.ast = this.unit.getAST();
		this.rewriter = ASTRewrite.create(ast);
		this.source = null;
	}

	/**
	 * When no CompilationUnit is available, mostly for testing. The source is
	 * assumed to be a compilation unit on the latest supported source code
	 * release and a normal java source file.
	 *
	 * @param source the source code
	 */
	public ASTEngine(String source) {
		this(source, AST.getJLSLatest(), ASTParser.K_COMPILATION_UNIT, ".", null);
	}

	/**
	 * Provide the details of a compilation
	 *
	 * @param source the source code
	 * @param jls the jls version
	 * @param kind the type of code we're seeing
	 * @param unit the name of the unit (for package-info.java and
	 *            module-info.java)
	 * @param optionsx the compiler and java options
	 */
	public ASTEngine(String source, int jls, int kind, String unit, @Nullable
	Map<String, String> optionsx) {
		this.source = source;
		ASTParser parser = ASTParser.newParser(jls);
		parser.setResolveBindings(true);
		parser.setSource(source.toCharArray());
		parser.setKind(kind);
		if (optionsx == null) {
			optionsx = JavaCore.getOptions();
		}
		String sourceLevel = Integer.toString(jls);
		if (jls < 9)
			sourceLevel = "1." + sourceLevel;
		optionsx.put(JavaCore.COMPILER_SOURCE, sourceLevel);
		parser.setCompilerOptions(optionsx);
		parser.setUnitName(unit);

		this.unit = (CompilationUnit) parser.createAST(null);
		this.ast = this.unit.getAST();
		this.rewriter = ASTRewrite.create(ast);
	}

	/**
	 * Calculate the rewrite from the given document assumed to be the original
	 *
	 * @param document the document as reference
	 * @return a TextEdit
	 */
	public TextEdit getTextEdit(IDocument document) {
		return rewriter.rewriteAST(document, null);
	}

	/**
	 * Calculate the rewrite from the original source
	 *
	 * @return a TextEdit
	 */
	public TextEdit getTextEdit() throws Exception {
		assert source != null;
		return getTextEdit(source);
	}

	/**
	 * Calculate the rewrite from the given source as reference
	 *
	 * @return a TextEdit
	 */
	public TextEdit getTextEdit(String source) {
		return getTextEdit(new Document(source));
	}

	/**
	 * Replace the existing property with the new value. The previous value is
	 * removed.
	 *
	 * @param <T> the property type
	 * @param node the parent
	 * @param type the property
	 * @param value the new value
	 */
	public <T extends ASTNode> void set(ASTNode node, Class<T> type, T value) {
		StructuralPropertyDescriptor key = getDescriptor(node, type);
		Object previous = node.getStructuralProperty(key);
		if (previous instanceof ASTNode old)
			removed.add(old);
		added.add(value);
		rewriter.set(node, key, value, null);

	}

	/**
	 * Get a property from the node for the given type
	 *
	 * @param <T> the type
	 * @param node the parent node
	 * @param type the type
	 */
	public <T extends ASTNode> Optional<T> get(ASTNode node, Class<T> type) {
		StructuralPropertyDescriptor key = getDescriptor(node, type);
		T value = type.cast(node.getStructuralProperty(key));
		return Optional.ofNullable(value);
	}

	/**
	 * Get a list property from the node for the given type
	 *
	 * @param <T> the type
	 * @param node the parent node
	 * @param type the type
	 */
	public <T extends ASTNode> Optional<List<T>> getList(ASTNode node, Class<T> type) {
		ChildListPropertyDescriptor key = getListDescriptor(node, type);
		List<T> value = (List<T>) node.getStructuralProperty(key);
		return Optional.ofNullable(value);
	}

	/**
	 * Insert a new property into a list property
	 *
	 * @param <T> the property type
	 * @param node the parent
	 * @param type the property
	 * @param value the new value
	 */
	public <T extends ASTNode> void insert(ASTNode node, Class<T> type, T value) {
		ChildListPropertyDescriptor key = getListDescriptor(node, type);
		added.add(value);
		rewriter.getListRewrite(node, key)
			.insertAt(value, 0, null);
	}

	/**
	 * Insert a new property into a list property at the end
	 *
	 * @param <T> the property type
	 * @param node the parent
	 * @param type the property
	 * @param value the new value
	 */
	public <T extends ASTNode> void insertLast(ASTNode node, Class<T> type, T value) {
		ChildListPropertyDescriptor key = getListDescriptor(node, type);
		added.add(value);
		rewriter.getListRewrite(node, key)
			.insertLast(value, null);
	}

	/**
	 * Insert a new property into a list property after another property
	 *
	 * @param <T> the property type
	 * @param before the node before the new node
	 * @param value the new node
	 */
	public <T extends ASTNode> void insertAfter(T before, T value) {
		assert before != null;
		assert value != null;

		ASTNode parent = before.getParent();
		assert parent != null;

		ChildListPropertyDescriptor key = getListDescriptor(parent, before.getClass());
		added.add(value);
		rewriter.getListRewrite(parent, key)
			.insertAfter(value, before, null);
	}

	/**
	 * Remove a list property
	 *
	 * @param <T> the type of the list property
	 * @param node the parent node
	 * @param type the type
	 * @param test the predicate that tests each node for removal
	 */
	public <T extends ASTNode> void removeIf(ASTNode node, Class<T> type, Predicate<T> test) {
		ChildListPropertyDescriptor key = getListDescriptor(node, type);
		ListRewrite listRewrite = rewriter.getListRewrite(node, key);
		stream(node, type).filter(test)
			.forEach(child -> {
				removed.add(child);
				listRewrite.remove(child, null);
			});
	}

	/**
	 * Remove a node from a list node
	 *
	 * @param <T> the type
	 * @param node the parent node
	 * @param value the node to be removed
	 */
	public <T extends ASTNode> void remove(ASTNode node, T value) {
		ChildListPropertyDescriptor key = getListDescriptor(node, value.getClass());
		ListRewrite listRewrite = rewriter.getListRewrite(node, key);
		removed.add(value);
		listRewrite.remove(value, null);
	}

	/**
	 * Remove a node
	 *
	 * @param value the node to be removed
	 */
	public void remove(ASTNode value) {
		removed.add(value);
		rewriter.remove(value, null);
	}

	/**
	 * Ensure that the the value node is inserted while any predicate matching
	 * nodes are removed
	 *
	 * @param <T> the type
	 * @param node the parent node
	 * @param type the type
	 * @param test the predicate to test what to remove
	 * @param value the new value
	 */
	public <T extends ASTNode> void ensure(ASTNode node, Class<T> type, Predicate<T> test, T value) {
		removeIf(node, type, test);
		insert(node, type, value);
	}

	/**
	 * Return a stream of nodes for a list property
	 *
	 * @param <T> the type
	 * @param node the parent node
	 * @param type the type
	 * @return a stream of values
	 */
	public <T extends ASTNode> Stream<T> stream(ASTNode node, Class<T> type) {
		return findListDescriptor(node, type).map(k -> toStream(node, type, k))
			.orElse(Stream.empty());
	}

	public AST ast() {
		return ast;
	}

	public CompilationUnit unit() {
		return unit;
	}

	/**
	 * Replace an older value with a newer value
	 *
	 * @param <T> the type
	 * @param older the older value
	 * @param newer the newer value
	 */

	public <T extends ASTNode> void replace(T older, T newer) {
		removed.add(older);
		added.add(newer);
		rewriter.replace(older, newer, null);
	}

	public boolean hasChanged() {
		return added.size() + removed.size() > 0;
	}

	<T extends ASTNode> Stream<T> toStream(ASTNode node, Class<T> type, ChildListPropertyDescriptor k) {
		List<T> list = (List) node.getStructuralProperty(k);
		if (list == null)
			return Stream.empty();

		return list.stream()
			.filter(x -> type.isInstance(x));
	}

	Optional<ChildListPropertyDescriptor> findListDescriptor(ASTNode owner, Class<? extends ASTNode> elementType) {
		return findDescriptor(owner, elementType).filter(d -> d instanceof ChildListPropertyDescriptor)
			.map(d -> (ChildListPropertyDescriptor) d);
	}

	Optional<ChildPropertyDescriptor> findPropertyDescriptor(ASTNode owner, Class<? extends ASTNode> childType) {
		return findDescriptor(owner, childType).filter(d -> d instanceof ChildPropertyDescriptor)
			.map(d -> (ChildPropertyDescriptor) d);
	}

	Optional<StructuralPropertyDescriptor> findDescriptor(ASTNode owner, Class<? extends ASTNode> childType) {
		Class rover = owner.getClass();
		while (rover != null) {
			List<StructuralPropertyDescriptor> list = descriptors.computeIfAbsent(rover, this::getDescriptors);
			for (StructuralPropertyDescriptor d : list) {
				if (d instanceof ChildListPropertyDescriptor clpd) {
					if (clpd.getElementType()
						.isAssignableFrom(childType)) {
						return Optional.of(clpd);
					}
				}
				if (d instanceof ChildPropertyDescriptor clpd) {
					if (clpd.getChildType()
						.isAssignableFrom(childType)) {
						return Optional.of(clpd);
					}
				}
			}
			rover = rover.getSuperclass();
		}
		return Optional.empty();
	}

	List<StructuralPropertyDescriptor> getDescriptors(Class<? extends ASTNode> c) {
		try {
			MethodHandle getter = lookup.findStatic(c, "propertyDescriptors", descriptorsMethodType);
			return (List) getter.invoke(-1);
		} catch (Exception e) {
			System.out.println(c + " has no properties " + e.getMessage());
		} catch (Throwable e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return Collections.emptyList();
	}

	StructuralPropertyDescriptor getDescriptor(ASTNode node, Class<? extends ASTNode> type) {
		return findDescriptor(node, type)
			.orElseThrow(() -> new IllegalArgumentException("no list key for " + node + ":" + type));
	}

	<T extends ASTNode> ChildListPropertyDescriptor getListDescriptor(ASTNode node, Class<T> key) {
		StructuralPropertyDescriptor ld = getDescriptor(node, key);
		if (ld instanceof ChildListPropertyDescriptor clpd)
			return clpd;

		throw new IllegalArgumentException("expected a list descriptor, got " + ld);
	}

	<T extends ASTNode> ChildPropertyDescriptor getPropertyDescriptor(ASTNode node, Class<T> key) {
		StructuralPropertyDescriptor ld = getDescriptor(node, key);
		if (ld instanceof ChildPropertyDescriptor cpd)
			return cpd;

		throw new IllegalArgumentException("expected a property descriptor, got " + ld);
	}

}
