package org.bndtools.refactor.util;

import java.util.List;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.regex.Pattern;

import org.eclipse.jdt.core.dom.ASTNode;

/**
 * A cursor holds a multiselection of nodes in the AST tree. I guess it is a
 * monad but the idea is derived from the the CSS selections as used in D3 and
 * JQuery. The Cursor holds the selections and the only way to access it is
 * through the forEach methods. The selection can be modified and filtered with
 * different methods. It uses the builder pattern but the object is immutable.
 * If the selection goes to empty, the result will be a failed cursor that
 * operates normally but will never call the body of a forEach. All the methods
 * defined here are no-ops and return the failed cursor. The toString of the
 * failed cursor shows details when things went south.
 * <p>
 * The primary idea is to be able to declare selections in the tree without
 * checking if the selection is valid. This makes the code more concise.
 *
 * @param <T> the ASTNode type
 */
public interface Cursor<T extends ASTNode> {

	/**
	 * Return the underlying assistant
	 */
	RefactorAssistant getAssistant();

	/**
	 * If there is a single node selected, return it, otherwise empty.
	 */
	Optional<T> getNode();

	/**
	 * Get the current set of nodes
	 */
	List<T> getNodes();

	/**
	 * For each node in the selection, move up in the tree until a node of the
	 * given class is found. Duplicates are removed.
	 *
	 * @param <X> the type of the ancestor node
	 * @param class1 the type of the ancestor node
	 * @return a cursors on the deduplicated ancestors of all current nodes.
	 */
	default <X extends ASTNode> Cursor<X> upTo(Class<X> class1) {
		return upTo(class1, Integer.MAX_VALUE);
	}

	/**
	 * For each node in the selection, move up in the tree until a node of the
	 * given class is found. Duplicates are removed. Repeat max n times
	 *
	 * @param <X> the type of the ancestor node
	 * @param class1 the type of the ancestor node
	 * @param n max nodes to look up
	 * @return a cursors on the deduplicated ancestors of all current nodes up
	 *         to n ancestors
	 */
	<X extends ASTNode> Cursor<X> upTo(Class<X> class1, int n);

	/**
	 * Filter the selection that NONE of its annotations are in typeNames (AND)
	 *
	 * @param typeNames the type names
	 */
	Cursor<T> noneOfTheseAnnotations(String... typeName);

	/**
	 * For each node in the selection, call process
	 *
	 * @param process the lambdo that processes the node
	 */
	Cursor<T> forEach(Consumer<T> process);

	/**
	 * For each node in the selection, call process
	 *
	 * @param process the lambdo that processes the node with this' assistant
	 */
	Cursor<T> forEach(BiConsumer<RefactorAssistant, T> process);

	/**
	 * Run the process if there is no selection
	 *
	 * @param process the process to run
	 */
	Cursor<T> ifNotPresent(Runnable process);

	/**
	 * Filter the nodes for any node that has any of the annotations in
	 * typeNames (OR)
	 *
	 * @param typeNames the type names
	 */
	Cursor<T> anyOfTheseAnnotations(String... typeNames);

	/**
	 * Cast the nodes to the given type and filter any nodes that cannot be
	 * cast.
	 *
	 * @param <X> the desired type
	 * @param type desired type
	 * @return a new Cursor on the desired type.
	 */
	<X extends ASTNode> Cursor<X> cast(Class<X> type);

	/**
	 * Filter any nodes that cannot pass the give test
	 *
	 * @param test the test predicate
	 * @return a new cursor with a filtered selection
	 */
	Cursor<T> filter(Predicate<T> test);

	/**
	 * Map the nodeset to a new type
	 *
	 * @param <X> the new type
	 * @param mapper the mapper function
	 * @return a new Cursor<X>
	 */
	<X extends ASTNode> Cursor<X> map(Function<T, X> mapper);

	/**
	 * expand the selection from the nodes to cursors
	 *
	 * @param <X> the result type
	 * @param mapper the mapper function
	 * @return a new Cursor
	 */
	<X extends ASTNode> Cursor<X> flatMap(Function<T, Cursor<X>> mapper);

	/**
	 * Return a selection where none of the selected nodes is an instance of any
	 * of the given types
	 *
	 * @param types
	 */
	Cursor<T> isNotInstanceOfAny(Class<?>... types);

	/**
	 * Check each node in the selection for annotation and react on it. Quite
	 * often in refactoring you need to react on the presence and absence of an
	 * annotation. Than this method is your friend.
	 *
	 * @param body the consumer to call
	 * @param annotationNames the list of fully qualified annotation names
	 */
	Cursor<T> checkAnnotation(BiConsumer<Cursor<T>, Boolean> body, String... annotationNames);

	/**
	 * Check of the type is not a primitive. The type depends on the member
	 * kind: method return type, field type, parameter type, etc. See
	 * {@link RefactorAssistant#getType(ASTNode)}
	 */
	Cursor<T> isNotPrimitive();

	/**
	 * Check of the type is a primitive. The type depends on the member kind:
	 * method return type, field type, parameter type, etc. See
	 * {@link RefactorAssistant#getType(ASTNode)}
	 */
	Cursor<T> isPrimitive();

	/**
	 * Check if the name not matches against the nodes. What the name is depends
	 * on the actual kind of node. See
	 * {@link RefactorAssistant#getIdentifier(ASTNode)}
	 *
	 * @param re the regular expression
	 */
	Cursor<T> nameNotMatches(Pattern re);

	/**
	 * Check if the name not matches against the nodes. What the name is depends
	 * on the actual kind of node. See
	 * {@link RefactorAssistant#getIdentifier(ASTNode)}
	 *
	 * @param re the regular expression
	 */
	default Cursor<T> nameNotMatches(String re) {
		return nameNotMatches(Pattern.compile(re));
	}

	/**
	 * Check if the name matches against the nodes. What the name is depends on
	 * the actual kind of node. See
	 * {@link RefactorAssistant#getIdentifier(ASTNode)}
	 *
	 * @param re the regular expression
	 */
	Cursor<T> nameMatches(Pattern re);

	/**
	 * Check if the name matches against the nodes. What the name is depends on
	 * the actual kind of node. See
	 * {@link RefactorAssistant#getIdentifier(ASTNode)}
	 *
	 * @param re the regular expression
	 */
	default Cursor<T> nameMatches(String re) {
		return nameMatches(Pattern.compile(re));
	}

	/**
	 * Reduce the selection to the nodes with the given name.
	 *
	 * @param name the name to match
	 */
	Cursor<T> hasName(String name);

	/**
	 * Filter the selection on source type
	 *
	 * @param sourceType the source type
	 */
	Cursor<T> isJavaSourceType(JavaSourceType... sourceType);

	/**
	 * Sometimes it is necessary to check a thing elsewhere in the tree but you
	 * do not want to give up the current selection. The and branches off
	 * another branch of cursors. If the result has failed (no selection) then
	 * this branch will fail. Otherwise we continue with the current selection.
	 *
	 * @param other a function to calculate the check branch
	 * @return this or a failed cursor
	 */
	Cursor<T> and(Function<Cursor<T>, Cursor<?>> other);

	/**
	 * Prune the selection for any node that has any of the given modifiers. If
	 * no modifiers are given this has no effect. See
	 * {@link RefactorAssistant#hasModifiers(ASTNode, JavaModifier...)} for the
	 * modifiers.
	 *
	 * @param modifiers the modifiers
	 * @return a new selection
	 */
	Cursor<T> hasModifier(JavaModifier... modifiers);

	/**
	 * Maps each node in the selection to a singleton cursor, then calls the
	 * mapping versions and returns a list of the results. The list may contain
	 * duplicates.
	 *
	 * @param <X> the result type
	 * @param mapper the mapper function
	 * @return a list of the results
	 */
	<X> List<X> processSingletons(Function<Cursor<T>, X> mapper);

	/**
	 * Traverse the selection and descend until the given type is met in the
	 * children, collect those as the new selection. Go at max count levels
	 * deep.
	 * <p>
	 * Note that this will descend to any depth. For example, if you're looking
	 * for the methods of a type, it will include nested types. See also
	 * {@link #descend(Class)}
	 *
	 * @param <X> the searched type
	 * @param class1 the type
	 * @return a new selection on all children to count levels that are an X
	 */
	<X extends ASTNode> Cursor<X> downTo(Class<X> class1, int count);

	/**
	 * Traverse the selection and descend until the given type is met in the
	 * children, collect those as the new selection to any depth.
	 * <p>
	 * Note that this will descend to any depth. For example, if you're looking
	 * for the methods of a type, it will include nested types. See also
	 * {@link #descend(Class)}
	 *
	 * @param <X> the searched type
	 * @param class1 the type
	 * @return a new selection on all children to count levels that are an X
	 */
	default <X extends ASTNode> Cursor<X> downTo(Class<X> class1) {
		return downTo(class1, Integer.MAX_VALUE);
	}

	/**
	 * Traverse the selection and descend one level. If the given type is met in
	 * the children, collect those as the new selection.
	 *
	 * @param <X> the type we're looking for
	 * @param class1 the class object of the type we're looking for
	 * @return a new selection or failed
	 */
	<X extends ASTNode> Cursor<X> descend(Class<X> class1);

	/**
	 * Verify that the parent is of the desired type. If not, will fail
	 * otherwise it is the same object.
	 *
	 * @param type the type of the parent
	 * @return this or a failed cursor
	 */
	Cursor<T> parentType(Class<? extends ASTNode> type);

	/**
	 * Check if the current selection is a void method
	 */
	Cursor<T> isVoidMethod();

	/**
	 * Check if the current type is in the set of given names. These are fully
	 * qualified names. The check is defined in
	 * {@link RefactorAssistant#getType(ASTNode)} and is resolved via
	 * {@link RefactorAssistant#resolve(org.eclipse.jdt.core.dom.Type)}
	 * <p>
	 * If no names are given, this method always fails
	 *
	 * @param typeNames the set of names of types this is matched agains
	 */

	Cursor<T> typeIn(String... typeNames);

	/**
	 * If there is no selection
	 */
	boolean isEmpty();


}
