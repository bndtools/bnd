package aQute.lib.zip;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.junit.Assert.assertEquals;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.junit.Test;

import aQute.lib.hierarchy.FolderNode;
import aQute.lib.hierarchy.Hierarchy;
import aQute.lib.hierarchy.NamedNode;
import aQute.lib.io.IO;
import aQute.libg.ints.IntCounter;

public class HierarchyTest {

	@Test
	public void directoryTest() throws Exception {
		Hierarchy index = new JarIndex(IO.getFile("testresources/fileset"));
		assertThat(index.findFolder("a/b")
			.get()
			.names()).contains("b.abc");
		assertThat(index.findFolder("")
			.get()
			.names()).contains("root");
		assertThat(index.findFolder("/a/b/c/d/e/f/")
			.get()
			.names()).contains("a.abc", "b.abc", "c.abc", "d.def", "e.def", "f.def");

	}

	@Test
	public void directoryTestWithDoNotCopy() throws Exception {
		Hierarchy index = new JarIndex(IO.getFile("testresources/fileset"), Pattern.compile(".*\\.def"));
		assertThat(index.findFolder("a/b")
			.get()
			.names()).contains("b.abc");
		assertThat(index.findFolder("")
			.get()
			.names()).contains("root");
		assertThat(index.findFolder("/a/b/c/d/e/f/")
			.get()
			.names()).contains("a.abc", "b.abc", "c.abc");
		assertThat(index.findFolder("/a/b/c/d/e/f/")
			.get()
			.names()).doesNotContain("d.def", "e.def", "f.def");
	}

	@Test
	public void directoryTestWithDoNotCopyFolder() throws Exception {
		Hierarchy index = new JarIndex(IO.getFile("testresources/fileset"), Pattern.compile("c"));
		assertThat(index.findFolder("a/b")
			.get()
			.names()).contains("b.abc");
		assertThat(index.findFolder("")
			.get()
			.names()).contains("root");
		assertThat(index.findFolder("/a/b/c/d/e/f/")).isNotPresent();
		assertThat(index.findFolder("/a/b/c/d/e/")).isNotPresent();
		assertThat(index.findFolder("/a/b/c/d/")).isNotPresent();
		assertThat(index.findFolder("/a/b/c")).isNotPresent();
		assertThat(index.findFolder("/a/b/")).isPresent();
	}

	@Test
	public void zipfileTest() throws Exception {
		Hierarchy index = new JarIndex(IO.getFile("testresources/osgi-3.0.0.jar"));
		testOSGi(index);
		testNodes(index);
	}

	@Test
	public void zipStreamTest() throws Exception {
		Hierarchy index = new JarIndex(new FileInputStream(IO.getFile("testresources/osgi-3.0.0.jar")));
		testOSGi(index);

		testNodes(index);
	}

	private void testOSGi(Hierarchy index) {
		assertThat(index.size()).isEqualTo(266);
		List<String> nodes = new ArrayList<>();
		for (NamedNode node : index) {
			nodes.add(node.name());
		}
		assertThat(nodes).hasSize(266);

		assertThat(index.findFolder("META-INF")
			.get()
			.names()).contains("MANIFEST.MF");

		assertThat(index.findFolder("org/osgi/framework")
			.get()
			.names()).contains("BundleContext.class");
	}

	@Test
	public void testEqual() throws FileNotFoundException, IOException {
		Hierarchy stream = new JarIndex(new FileInputStream(IO.getFile("testresources/osgi-3.0.0.jar")));
		Hierarchy file = new JarIndex(IO.getFile("testresources/osgi-3.0.0.jar"));

		List<String> a = stream.stream()
			.map(NamedNode::name)
			.collect(Collectors.toList());
		List<String> b = file.stream()
			.map(NamedNode::name)
			.collect(Collectors.toList());
		assertThat(a).isEqualTo(b);
	}

	@Test
	public void testIterator() throws Exception {
		Hierarchy index = new JarIndex(IO.getFile("testresources/fileset"));
		List<String> nodes = new ArrayList<>();

		for (NamedNode node : index) {
			nodes.add(node.name());

			if (!node.isFolder())
				assertThat(node.path()).endsWith(node.name());
			else if (!node.isRoot()) {
				assertThat(node.path()).endsWith("/");
			}
		}
		assertThat(nodes).contains("a.abc", "b.abc", "c.abc", "d.def", "e.def", "f.def", "root");
		assertThat(nodes).hasSize(17);
	}

	@Test
	public void testPath() throws Exception {
		Hierarchy index = new JarIndex(IO.getFile("testresources/fileset"));
		testNodes(index);
	}

	public void testNodes(Hierarchy index) {
		NamedNode root = index.find("/")
			.orElse(null);
		assertThat(root).isNotNull();

		for (NamedNode node : index) {
			assertThat(node).isEqualTo(index.find(node.path())
				.get());

			assertThat(node).isEqualTo(index.find(node.path() + "/")
				.get());
			assertThat(node).isEqualTo(index.find("/" + node.path() + "/")
				.get());
			assertThat(node).isEqualTo(index.find("/" + node.path())
				.get());
			assertThat(node.find(".")).isPresent();
			assertThat(node.find(".")
				.get()).isEqualTo(node);

			System.out.println(node);
			if (!node.isRoot()) {
				assertThat(node.find("..")).isEqualTo(node.parent());
				assertThat(node).isEqualTo(node.parent()
					.get()
					.find(node.name())
					.get());
			} else {
				try {
					node.find("..");
					fail("expected and exception");
				} catch (Exception e) {
					// ok
				}
			}

			assertEquals(node.root(), root);

			if (node.isFolder()) {
				FolderNode folder = (FolderNode) node;
				NamedNode[] children = folder.children();
				assertThat(children).isSorted();
				assertThat(folder.size()).isEqualTo(children.length);
				IntCounter n = new IntCounter();
				folder.forEach(x -> n.inc());
				folder.forEach(x -> assertThat(children).contains(x));
				assertThat(n.get()).isEqualTo(children.length);
			}
		}

	}

	@Test
	public void testPathViaSubNode() throws Exception {
		Hierarchy index = new JarIndex(IO.getFile("testresources/fileset"));

		NamedNode a = index.find("a")
			.get();
		NamedNode b = a.find("b")
			.get();
		assertThat(b).isEqualTo(index.find("a/b")
			.get());
	}

	@Test
	public void testFind() throws Exception {
		Hierarchy index = new JarIndex(IO.getFile("testresources/fileset"));

		testPos(index, "", "", "");
		testPos(index, "/", "", "");
		testPos(index, "root", "root", "root");
		testPos(index, "/root", "root", "root");
		testPos(index, "/root/", "root", "root");
		testPos(index, "/a/b/c/d/e/f/", "f", "a/b/c/d/e/f/");
		testPos(index, "a/b/c/d/e/f/", "f", "a/b/c/d/e/f/");
		testPos(index, "/a/b/c/d/e/f", "f", "a/b/c/d/e/f/");
		testPos(index, "/a/b/c/d/e/f/a.abc", "a.abc", "a/b/c/d/e/f/a.abc");

		assertThat(index.find("foobar")).isNotPresent();
	}

	private void testPos(Hierarchy index, String path, String name, String expected) {
		assertThat(index.find(path)).isPresent();
		assertThat(index.find(path)
			.get()).isNotNull();
		assertThat(index.find(path)
			.get()
			.name()).isEqualTo(name);
		assertThat(index.find(path)
			.get()
			.path()).isEqualTo(expected);

	}

}
