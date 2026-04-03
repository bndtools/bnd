package org.bndtools.refactor.util;

import static org.eclipse.jdt.core.dom.Modifier.ModifierKeyword.ABSTRACT_KEYWORD;
import static org.eclipse.jdt.core.dom.Modifier.ModifierKeyword.FINAL_KEYWORD;
import static org.eclipse.jdt.core.dom.Modifier.ModifierKeyword.NATIVE_KEYWORD;
import static org.eclipse.jdt.core.dom.Modifier.ModifierKeyword.PRIVATE_KEYWORD;
import static org.eclipse.jdt.core.dom.Modifier.ModifierKeyword.PROTECTED_KEYWORD;
import static org.eclipse.jdt.core.dom.Modifier.ModifierKeyword.PUBLIC_KEYWORD;
import static org.eclipse.jdt.core.dom.Modifier.ModifierKeyword.STATIC_KEYWORD;
import static org.eclipse.jdt.core.dom.Modifier.ModifierKeyword.STRICTFP_KEYWORD;
import static org.eclipse.jdt.core.dom.Modifier.ModifierKeyword.SYNCHRONIZED_KEYWORD;
import static org.eclipse.jdt.core.dom.Modifier.ModifierKeyword.TRANSIENT_KEYWORD;
import static org.eclipse.jdt.core.dom.Modifier.ModifierKeyword.VOLATILE_KEYWORD;

import java.util.EnumSet;
import java.util.Set;

import org.eclipse.jdt.core.dom.Modifier.ModifierKeyword;

/**
 * Model for the modifiers. In the JDT API, the modifiers have no semantics.
 * These modifiers know which other modifiers should be removed when added. Also
 * a bit more useful and less coupled.
 */
public enum JavaModifier {

	PUBLIC(PUBLIC_KEYWORD),
	PROTECTED(PROTECTED_KEYWORD),
	PRIVATE(PRIVATE_KEYWORD),
	STATIC(STATIC_KEYWORD),
	FINAL(FINAL_KEYWORD),
	SYNCHRONIZED(SYNCHRONIZED_KEYWORD),
	VOLATILE(VOLATILE_KEYWORD),
	TRANSIENT(TRANSIENT_KEYWORD),
	NATIVE(NATIVE_KEYWORD),
	ABSTRACT(ABSTRACT_KEYWORD),
	STRICTFP(STRICTFP_KEYWORD),
	PACKAGE_PRIVATE(null);

	public static EnumSet<JavaModifier>	NONE	= EnumSet.noneOf(JavaModifier.class);
	public final ModifierKeyword		keyword;

	JavaModifier(ModifierKeyword publicKeyword) {
		this.keyword = publicKeyword;
	}

	/**
	 * Return a set of JavaModifiers that should be removed if the given
	 * modifier is added. For example, adding PUBLIC should remove PROTECTED, or
	 * PRIVATE
	 *
	 * @param modifier the modifier
	 * @return a set of other Modifiers that are incompatible.
	 */
	public static Set<JavaModifier> getConflictingModifiers(JavaModifier modifier) {
		return switch (modifier) {
			case PUBLIC, PROTECTED, PRIVATE -> EnumSet.of(PROTECTED, PRIVATE);
			case STATIC -> EnumSet.of(ABSTRACT);
			case ABSTRACT -> EnumSet.of(STATIC, FINAL, NATIVE, PRIVATE, SYNCHRONIZED);
			case FINAL -> EnumSet.of(ABSTRACT, VOLATILE, TRANSIENT);
			case SYNCHRONIZED -> EnumSet.of(NATIVE, ABSTRACT);
			case VOLATILE -> EnumSet.of(FINAL, TRANSIENT);
			case TRANSIENT -> EnumSet.of(FINAL, VOLATILE);
			case NATIVE -> EnumSet.of(ABSTRACT, SYNCHRONIZED, VOLATILE);
			case PACKAGE_PRIVATE -> EnumSet.of(PUBLIC, PROTECTED, PRIVATE);
			default -> NONE;
		};
	}


	/**
	 * Get the corresponding JavaModifier from JDT's ModifierKeyword
	 *
	 * @param keyword the JDT modifier keyword
	 * @return a JavaModifier
	 */
	public static JavaModifier of(ModifierKeyword keyword) {

		String name = keyword.toString()
			.toUpperCase();
		return JavaModifier.valueOf(name);
	}
}
