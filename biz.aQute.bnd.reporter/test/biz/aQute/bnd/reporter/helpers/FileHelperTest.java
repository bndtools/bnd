package biz.aQute.bnd.reporter.helpers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.jupiter.api.Test;

import aQute.bnd.test.jupiter.InjectTemporaryDirectory;

public class FileHelperTest {

	@Test
	public void testExtension() {
		assertEquals("", FileHelper.getExtension(null));
		assertEquals("", FileHelper.getExtension(new File("")));
		assertEquals("", FileHelper.getExtension(new File(".git")));
		assertEquals("git", FileHelper.getExtension(new File("name.git")));
		assertEquals("", FileHelper.getExtension(new File("name.")));
		assertEquals("name", FileHelper.getExtension(new File("..name")));
		assertEquals("", FileHelper.getExtension(new File(".name.")));
		assertEquals("", FileHelper.getExtension(new File(".name.")));
		assertEquals("two", FileHelper.getExtension(new File("/path/io.dir/one.name.two")));
		assertEquals("two", FileHelper.getExtension(new File("@/path/io.dir/one.name.two")));
		assertEquals("two", FileHelper.getExtension(new File("file:///path/io.dir/one.name.two")));
		assertEquals("", FileHelper.getExtension(new File("http:/www.google.com/admin")));
	}

	@Test
	public void testName() {
		assertEquals("", FileHelper.getName(null));
		assertEquals("", FileHelper.getName(new File("")));
		assertEquals(".git", FileHelper.getName(new File(".git")));
		assertEquals("name", FileHelper.getName(new File("name.git")));
		assertEquals("name", FileHelper.getName(new File("name.")));
		assertEquals(".", FileHelper.getName(new File("..name")));
		assertEquals(".name", FileHelper.getName(new File(".name.")));
		assertEquals(".name", FileHelper.getName(new File(".name.")));
		assertEquals("one.name", FileHelper.getName(new File("/path/io.dir/one.name.two")));
		assertEquals("one.name", FileHelper.getName(new File("@/path/io.dir/one.name.two")));
		assertEquals("@one.name", FileHelper.getName(new File("@one.name.two")));
	}

	@Test
	public void testSearch(@InjectTemporaryDirectory
	Path tmp) throws IOException {
		final File oneWithExtDir = Files.createTempDirectory(tmp, "bndtest-oneWithExtDir")
			.toFile();
		final File oneWithoutExtDir = Files.createTempDirectory(tmp, "bndtest-oneWithoutExtDir")
			.toFile();
		final File twoWithExtDir = Files.createTempDirectory(tmp, "bndtest-twoWithExtDir")
			.toFile();
		final File twoWithoutExtDir = Files.createTempDirectory(tmp, "bndtest-twoWithoutExtDir")
			.toFile();
		final File multiDir = Files.createTempDirectory(tmp, "bndtest-multi")
			.toFile();

		final File baseOneWithExt = Files.createFile(Paths.get(oneWithExtDir.getAbsolutePath() + "/base.md"))
			.toFile();
		final File baseOneWithoutExtDir = Files.createFile(Paths.get(oneWithoutExtDir.getAbsolutePath() + "/base"))
			.toFile();
		final File baseTwoWithExtDir = Files.createFile(Paths.get(twoWithExtDir.getAbsolutePath() + "/base.md"))
			.toFile();
		final File tempTwoWithExtDir = Files.createFile(Paths.get(twoWithExtDir.getAbsolutePath() + "/basE.xslt"))
			.toFile();
		final File baseTwoWithoutExtDir = Files.createFile(Paths.get(twoWithoutExtDir.getAbsolutePath() + "/base"))
			.toFile();
		final File tempTwoWithoutExtDir = Files.createFile(Paths.get(twoWithoutExtDir.getAbsolutePath() + "/base.xslt"))
			.toFile();
		final File baseMultiDir = Files.createFile(Paths.get(multiDir.getAbsolutePath() + "/base.md"))
			.toFile();
		final File other1MultiDir = Files.createFile(Paths.get(multiDir.getAbsolutePath() + "/other.xslt"))
			.toFile();
		final File other2MultiDir = Files.createFile(Paths.get(multiDir.getAbsolutePath() + "/other.MD"))
			.toFile();
		final File other3MultiDir = Files.createFile(Paths.get(multiDir.getAbsolutePath() + "/base"))
			.toFile();
		final File other4MultiDir = Files.createDirectories(Paths.get(multiDir.getAbsolutePath() + "/base.other"))
			.toFile();
		final File tempMultiDir = Files.createFile(Paths.get(multiDir.getAbsolutePath() + "/base.xslt"))
			.toFile();
		final File temp2MultiDir = Files.createFile(Paths.get(multiDir.getAbsolutePath() + "/base.twig"))
			.toFile();

		assertEquals(null, FileHelper.searchSiblingWithDifferentExtension(new File("notnot"), new String[] {
			"xslt"
		}));

		assertEquals(null, FileHelper.searchSiblingWithDifferentExtension(null, new String[] {
			"xslt"
		}));

		assertEquals(null, FileHelper.searchSiblingWithDifferentExtension(baseOneWithExt, new String[] {}));

		assertEquals(null, FileHelper.searchSiblingWithDifferentExtension(baseOneWithExt, null));

		assertEquals(null, FileHelper.searchSiblingWithDifferentExtension(baseOneWithExt, new String[] {
			"xslt"
		}));

		assertEquals(null, FileHelper.searchSiblingWithDifferentExtension(baseOneWithoutExtDir, new String[] {
			"xslt"
		}));

		assertEquals(tempTwoWithExtDir.getAbsolutePath(),
			FileHelper.searchSiblingWithDifferentExtension(baseTwoWithExtDir, new String[] {
				"xslt"
			})
				.getAbsolutePath());

		assertEquals(null, FileHelper.searchSiblingWithDifferentExtension(baseTwoWithExtDir, new String[] {
			""
		}));

		assertEquals(tempTwoWithoutExtDir.getAbsolutePath(),
			FileHelper.searchSiblingWithDifferentExtension(baseTwoWithoutExtDir, new String[] {
				"xslt"
			})
				.getAbsolutePath());

		assertEquals(null, FileHelper.searchSiblingWithDifferentExtension(baseTwoWithoutExtDir, new String[] {
			""
		}));

		assertEquals(tempMultiDir.getAbsolutePath(),
			FileHelper.searchSiblingWithDifferentExtension(baseMultiDir, new String[] {
				"xslt"
			})
				.getAbsolutePath());

		assertEquals(null, FileHelper.searchSiblingWithDifferentExtension(baseMultiDir, new String[] {}));

		assertEquals(null, FileHelper.searchSiblingWithDifferentExtension(baseMultiDir, new String[] {
			"other"
		}));

		assertEquals(temp2MultiDir.getAbsolutePath(),
			FileHelper.searchSiblingWithDifferentExtension(baseMultiDir, new String[] {
				"twig"
			})
				.getAbsolutePath());

		final String e = FileHelper.searchSiblingWithDifferentExtension(baseMultiDir, new String[] {
			"twig", "xslt"
		})
			.getAbsolutePath();

		assertTrue(e.equals(temp2MultiDir.getAbsolutePath()) || e.equals(tempMultiDir.getAbsolutePath()));
	}
}
