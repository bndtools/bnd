package org.bndtools.refactor.types;

import static aQute.bnd.osgi.Verifier.isVersion;
import static aQute.libg.re.Catalog.Alpha;
import static aQute.libg.re.Catalog.bindigit;
import static aQute.libg.re.Catalog.caseInsenstive;
import static aQute.libg.re.Catalog.digit;
import static aQute.libg.re.Catalog.g;
import static aQute.libg.re.Catalog.hexdigit;
import static aQute.libg.re.Catalog.lit;
import static aQute.libg.re.Catalog.opt;
import static aQute.libg.re.Catalog.or;
import static aQute.libg.re.Catalog.some;

import java.util.Map;

import org.bndtools.refactor.util.BaseRefactorer;
import org.bndtools.refactor.util.Cursor;
import org.bndtools.refactor.util.JavaModifier;
import org.bndtools.refactor.util.ProposalBuilder;
import org.bndtools.refactor.util.RefactorAssistant;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.NumberLiteral;
import org.eclipse.jdt.core.dom.StringLiteral;
import org.eclipse.jdt.core.dom.TextBlock;
import org.eclipse.jdt.ui.text.java.IInvocationContext;
import org.eclipse.jdt.ui.text.java.IQuickFixProcessor;
import org.osgi.framework.Version;
import org.osgi.service.component.annotations.Component;

import aQute.lib.justif.Justif;
import aQute.libg.re.Catalog;
import aQute.libg.re.RE;

/**
 * A number of refactorings on literals.
 */
@Component
public class LiteralRefactorer extends BaseRefactorer implements IQuickFixProcessor {

	final static RE	BASE_P			= g("base", or("0x", "0b"));
	final static RE	NUMBER_SUFFIX_P	= g("suffix", Catalog.cc("lL"));
	final static RE	HEX_NUMBER		= g(lit("0x"), g("hex", some(hexdigit)));
	final static RE	BIN_NUMBER		= g(lit("0b"), g("bin", some(bindigit)));
	final static RE	DEC_NUMBER		= g("dec", some(digit));
	final static RE	NUMBER_P		= g(caseInsenstive(), or(HEX_NUMBER, BIN_NUMBER, DEC_NUMBER), opt(NUMBER_SUFFIX_P));
	final static RE	HAS_LETTERS		= caseInsenstive(some(Alpha));

	/**
	 * Add the completions available on the current selection for literals and
	 * txt blocks.
	 */
	@Override
	public void addCompletions(ProposalBuilder builder, RefactorAssistant assistant, Cursor<?> root,
		IInvocationContext context) {

		root.cast(StringLiteral.class)
			.forEach(stringLiteral -> stringliteral(stringLiteral, builder, root))
			.filter(stringLiteral -> isVersion(stringLiteral.getLiteralValue()))
			.forEach(stringLiteral -> versions(stringLiteral, builder));

		root.cast(TextBlock.class)
			.forEach(textBlock -> textblock(textBlock, builder));

		root.cast(Modifier.class)
			.filter(Modifier::isPrivate)
			.upTo(FieldDeclaration.class, 3)
			.forEach(field -> builder.build("lit.final", "Make field final", "final", 0,
				() -> {
					assistant.ensureModifiers(field, JavaModifier.FINAL, JavaModifier.PACKAGE_PRIVATE);
				}));

		root.cast(NumberLiteral.class)
			.forEach(numberLiteral -> numberLiteral(numberLiteral, builder));
	}

	/**
	 * A StringLiteral holding a version
	 *
	 * @param stringLiteral
	 * @param builder
	 */
	private void versions(StringLiteral stringLiteral, ProposalBuilder builder) {
		RefactorAssistant ass = builder.getAssistant();
		Version v = new Version(stringLiteral.getLiteralValue());
		String major = new Version(v.getMajor() + 1, 0, 0).toString();
		String minor = new Version(v.getMajor(), v.getMinor() + 1, 0).toString();

		builder.build("lit.vers.majr", "major bump: " + major, "version-major", 0,
			() -> ass.replace(stringLiteral, major.toString()));
		builder.build("lit.vers.minr", "minor bump: " + minor, "version-minor", 0,
			() -> ass.replace(stringLiteral, minor.toString()));

		if (v.getQualifier() != null && !v.getQualifier()
			.isBlank()) {
			String withoutQualifier = new Version(v.getMajor(), v.getMinor(), v.getMicro()).toString();
			builder.build("lit.vers.qual-", withoutQualifier, "version-plain", 0,
				() -> ass.replace(stringLiteral, withoutQualifier.toString()));
		}
	}

	/**
	 * A TextBlock
	 *
	 * @param textBlock
	 * @param builder
	 */
	private void textblock(TextBlock textBlock, ProposalBuilder builder) {
		RefactorAssistant assistant = builder.getAssistant();
		String content = textBlock.getLiteralValue();

		builder.build("lit.blck.strn", "Convert to string", null, 0, () -> {
			assistant.replace(textBlock, assistant.newStringLiteral(content));
		});

		HAS_LETTERS.findIn(content)
			.ifPresent(match -> {

				builder.build("lit.blck.uppr", "-> UPPER case", "aA", 0,
					() -> assistant.replace(textBlock, content.toUpperCase()));
				builder.build("lit.blck.lowr", "-> lower case", "Aa", 0,
					() -> assistant.replace(textBlock, content.toLowerCase()));
				if (content.length() > 40) {
					Justif j = new Justif(60, 0, 10, 20, 30, 40, 50);
					StringBuilder sb = new StringBuilder(content);
					j.wrap(sb);
					builder.build("lit.blck.wrap.60", "Wrap @ 60", null, 0,
						() -> assistant.replace(textBlock, sb.toString()));
				}
			});
	}

	/**
	 * A StringLiteral
	 *
	 * @param stringLiteral
	 * @param builder
	 * @param root
	 */
	private void stringliteral(StringLiteral stringLiteral, ProposalBuilder builder, Cursor<?> root) {
		RefactorAssistant assistant = builder.getAssistant();

		String content = stringLiteral.getLiteralValue();
		builder.build("lit.strn.blck", "Convert to text block", null, 0, () -> {
			assistant.replace(stringLiteral, assistant.newTextBlock(content));
		});

		HAS_LETTERS.findIn(content)
			.ifPresent(match -> {

				builder.build("lit.strn.uppr", "-> UPPER case", "aA", 0,
					() -> assistant.replace(stringLiteral, content.toUpperCase()));
				builder.build("lit.strn.lowr", "-> lower case", "Aa", 0,
					() -> assistant.replace(stringLiteral, content.toLowerCase()));
			});
	}

	/**
	 * A Number Literal
	 *
	 * @param numberLiteral
	 * @param builder
	 */
	private void numberLiteral(NumberLiteral numberLiteral, ProposalBuilder builder) {
		RefactorAssistant assistant = builder.getAssistant();

		String content = numberLiteral.getToken();
		String withoutUnderscores = content.replaceAll("_", "");

		if (content.length() > withoutUnderscores.length()) {
			builder.build("lit.nmbr._-", "remove separators: " + withoutUnderscores, "minus", 0, () -> assistant.replace(numberLiteral, withoutUnderscores));
		}

		NUMBER_P.matches(withoutUnderscores)
			.ifPresent(match -> {

				Map<String, String> values = match.getGroupValues();
				String bin = values.get("bin");
				String hex = values.get("hex");
				String dec = values.get("dec");

				String suffix = values.getOrDefault("suffix", "");

				if (hex != null) {
					replace(builder, Long.parseLong(hex, 16), "lit.nmbr.hex.dec", 0, numberLiteral);

					underscores(builder, numberLiteral, content, "0x", hex, 4, suffix);
				} else if (bin != null) {
					replace(builder, Long.parseLong(bin, 2), "lit.nmbr.bin.dec", 0, numberLiteral);
					underscores(builder, numberLiteral, content, "0b", bin, 4, suffix);
				} else if (dec != null) {
					long l = Long.parseLong(dec, 10);
					if (l > Integer.MAX_VALUE || l < Integer.MIN_VALUE) {
						suffix = "L";
					}
					String hexf = format("0x", Long.toString(l, 16), suffix, 4, true);
					builder.build("lit.nmbr.dec.hex", "to hex: " + hexf, null, 0,
						() -> assistant.replace(numberLiteral, hexf));
					String binaryf = format("0b", Long.toString(l, 2), suffix, 4, true);
					builder.build("lit.nmbr.dec.bin", "to binary: " + binaryf, null, 0,
						() -> assistant.replace(numberLiteral, binaryf));
					underscores(builder, numberLiteral, content, "", dec, 3, suffix);
				}
			});
	}

	private void underscores(ProposalBuilder builder, NumberLiteral numberLiteral, String original, String prefix,
		String digits, int width,
		String suffix) {
		if (digits.length() > width) {
			String replace = format(prefix, digits, suffix, width, false);
			if (!replace.equals(original)) {
				builder.build("lit.nmbr._+", "insert _'s: " + replace, null, 0,
					() -> builder.getAssistant()
						.replace(numberLiteral, replace));
			}
		}
	}

	private void replace(ProposalBuilder builder, long value, String key, int level, NumberLiteral numberLiteral) {
		String suffix = "";
		if (value >= Integer.MAX_VALUE || value <= Integer.MIN_VALUE) {
			suffix = "L";
		}
		String v = Long.toString(value) + suffix;
		builder.build(key, "to decimal: " + v, null, level, () -> builder.getAssistant()
			.replace(numberLiteral, v));

	}

	static String format(String prefix, String digitsOnly, String suffix, int separation, boolean fixedWidth) {
		StringBuilder sb = new StringBuilder(digitsOnly);
		if (fixedWidth) {
			while ((sb.length() % separation) != 0)
				sb.insert(0, "0");
		}
		for (int i = sb.length() - separation; i > 0; i -= separation) {
			sb.insert(i, '_');
		}
		sb.insert(0, prefix);
		sb.append(suffix);
		return sb.toString();
	}

}
