package aQute.bnd.classfile.renamer;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.junit.Test;

import com.github.curiousoddman.rgxgen.RgxGen;

import aQute.bnd.classfile.renamer.ClassFileRenamer.SignatureParser;
import aQute.bnd.classfile.renamer.ClassFileRenamer.SignatureRenamer;
import aQute.lib.io.IO;

public class SignatureParserTest {
	final static Random						random			= new Random();
	final static RgxGen						TYPE_GENERATOR	= new RgxGen(
		"[[]{0,2}((I|B|C|L[abcd文efz$][ab\u0000\u0016cd0234文ef$]{2,8}(/[ab文_cde3434f$]{2,8}){0,3};)");
	final ClassFileRenamer.SignatureParser	sp				= new ClassFileRenamer.SignatureParser();

	/*
	 * Parse all signatures and make sure none throws an exception
	 */
	@Test
	public void testSignatures() throws IOException {

		Files.lines(IO.getFile("testresources/shade/signatures.txt")
			.toPath())
			.forEach(l -> {
				sp.parse(l);
			});
	}

	@Test
	public void testRenamer() {
		String s = "(I)Lorg/springframework/security/config/annotation/web/configurers/openid/OpenIDLoginConfigurer<TH;>.AttributeExchangeConfigurer.AttributeConfigurer;";
		Map<String, String> map = new HashMap<>();
		map.put("org/springframework/security/config/annotation/web/configurers/openid/OpenIDLoginConfigurer", "foo");
		SignatureRenamer sr = new SignatureRenamer(map::get);
		String rename = sr.rename(s);
		assertThat(sr.rename(s)).isEqualTo("(I)Lfoo<TH;>.AttributeExchangeConfigurer.AttributeConfigurer;");
	}

	@Test
	public void testRenamerNested() {
		Map<String, String> map = new HashMap<>();
		map.put("p1/p2/C1", "pxx/pyy/C1");
		SignatureRenamer sr = new SignatureRenamer(map::get);

		assertThat(sr.rename("(I)Lp1/p2/C1<TH;>.N1.N2$Foo;^Ljava/lang/String;^Lp1/p2/C1;"))
			.isEqualTo("(I)Lpxx/pyy/C1<TH;>.N1.N2$Foo;^Ljava/lang/String;^Lpxx/pyy/C1;");
		assertThat(sr.rename("(I)Lp1/p2/C1;")).isEqualTo("(I)Lpxx/pyy/C1;");

	}

	@Test
	public void testParser() {

		// generates descriptors

		for (int times = 0; times < 1000; times++) {
			for (int method : new int[] {
				-1, 0, 1, 2, 3, 10, 40
			}) {
				StringBuilder sb = new StringBuilder();
				List<String> generatedTypes = new ArrayList<>();

				if (method >= 0) {
					sb.append('(');
					while (method-- > 0) {
						String type = TYPE_GENERATOR.generate();

						picktype(generatedTypes, type);

						sb.append(type);
					}
					sb.append(')');
				}
				String type = TYPE_GENERATOR.generate();
				picktype(generatedTypes, type);
				sb.append(type);

				List<String> parsedTypes = new ArrayList<>();
				SignatureParser sp = makeParser(parsedTypes);
				sp.parse(sb.toString());

				assertThat(generatedTypes).describedAs(sb.toString())
					.isEqualTo(parsedTypes);
			}
		}
	}

	private void picktype(List<String> types, String type) {
		if (type.startsWith("L"))
			types.add(type.substring(1, type.length() - 1));
		else if (type.startsWith("[L"))
			types.add(type.substring(2, type.length() - 1));
		else if (type.startsWith("[[L"))
			types.add(type.substring(3, type.length() - 1));
		else if (type.startsWith("[[[L"))
			types.add(type.substring(4, type.length() - 1));
		else
			return;
	}

	private SignatureParser makeParser(List<String> types) {
		SignatureParser sp = new ClassFileRenamer.SignatureParser() {
			@Override
			protected void binary() {
				int begin = index();
				super.binary();
				int end = index();
				String type = source.substring(begin, end);
				types.add(type);
			}
		};
		return sp;
	}

}
