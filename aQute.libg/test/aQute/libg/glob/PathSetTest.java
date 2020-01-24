package aQute.libg.glob;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

public class PathSetTest {

	@Test
	public void matchesNoInclude() {
		PathSet pathSet = new PathSet();
		assertThat(pathSet.matches()).rejects("foo.txt", "xxx/bar.txt", "foo.bar", "xxx/foo.txt/yyy");
	}

	@Test
	public void matchesOneIncludeConstructor() {
		PathSet pathSet = new PathSet("**/*.txt");
		assertThat(pathSet.matches()).accepts("foo.txt", "xxx/bar.txt")
			.rejects("foo.bar", "xxx/foo.txt/yyy");
	}

	@Test
	public void matchesOneIncludeMethod() {
		PathSet pathSet = new PathSet().include("**/*.txt");
		assertThat(pathSet.matches()).accepts("foo.txt", "xxx/bar.txt")
			.rejects("foo.bar", "xxx/foo.txt/yyy");
	}

	@Test
	public void matchesOneIncludeDefault() {
		PathSet pathSet = new PathSet();
		assertThat(pathSet.matches("**/*.txt")).accepts("foo.txt", "xxx/bar.txt")
			.rejects("foo.bar", "xxx/foo.txt/yyy");
	}

	@Test
	public void matchesMultipleIncludeConstructor() {
		PathSet pathSet = new PathSet("**/*.txt", "xxx/*");
		assertThat(pathSet.matches()).accepts("foo.txt", "xxx/bar.bar")
			.rejects("foo.bar", "xxx/foo.txt/yyy");
	}

	@Test
	public void matchesMultipleIncludeMethod() {
		PathSet pathSet = new PathSet().include("**/*.txt", "xxx/*");
		assertThat(pathSet.matches()).accepts("foo.txt", "xxx/bar.bar")
			.rejects("foo.bar", "xxx/foo.txt/yyy");
	}

	@Test
	public void matchesMultipleIncludeDefault() {
		PathSet pathSet = new PathSet();
		assertThat(pathSet.matches("**/*.txt", "xxx/*")).accepts("foo.txt", "xxx/bar.bar")
			.rejects("foo.bar", "xxx/foo.txt/yyy");
	}

	@Test
	public void matchesOneIncludeConstructorExclude() {
		PathSet pathSet = new PathSet("**/*.txt").exclude("xxx/*");
		assertThat(pathSet.matches()).accepts("foo.txt")
			.rejects("foo.bar", "xxx/foo.txt/yyy", "xxx/bar.txt");
	}

	@Test
	public void matchesOneIncludeMethodExclude() {
		PathSet pathSet = new PathSet().include("**/*.txt")
			.exclude("xxx/*");
		assertThat(pathSet.matches()).accepts("foo.txt")
			.rejects("foo.bar", "xxx/foo.txt/yyy", "xxx/bar.txt");
	}

	@Test
	public void matchesOneIncludeDefaultExclude() {
		PathSet pathSet = new PathSet().exclude("xxx/*");
		assertThat(pathSet.matches("**/*.txt")).accepts("foo.txt")
			.rejects("foo.bar", "xxx/foo.txt/yyy", "xxx/bar.txt");
	}

	@Test
	public void findNoInclude() {
		PathSet pathSet = new PathSet();
		assertThat(pathSet.find()).rejects("foo.txt", "xxx/bar.txt", "foo.bar", "xxx/foo.txt/yyy");
	}

	@Test
	public void findOneIncludeConstructor() {
		PathSet pathSet = new PathSet("txt");
		assertThat(pathSet.find()).accepts("foo.txt", "xxx/bar.txt", "xxx/foo.txt/yyy")
			.rejects("foo.bar");
	}

	@Test
	public void findOneIncludeMethod() {
		PathSet pathSet = new PathSet().include("txt");
		assertThat(pathSet.find()).accepts("foo.txt", "xxx/bar.txt", "xxx/foo.txt/yyy")
			.rejects("foo.bar");
	}

	@Test
	public void findOneIncludeDefault() {
		PathSet pathSet = new PathSet();
		assertThat(pathSet.find("txt")).accepts("foo.txt", "xxx/bar.txt", "xxx/foo.txt/yyy")
			.rejects("foo.bar");
	}

	@Test
	public void findMultipleIncludeConstructor() {
		PathSet pathSet = new PathSet("txt", "xxx/");
		assertThat(pathSet.find()).accepts("foo.txt", "xxx/bar.bar", "xxx/foo.txt/yyy")
			.rejects("foo.bar");
	}

	@Test
	public void findMultipleIncludeMethod() {
		PathSet pathSet = new PathSet().include("txt", "xxx/");
		assertThat(pathSet.find()).accepts("foo.txt", "xxx/bar.bar", "xxx/foo.txt/yyy")
			.rejects("foo.bar");
	}

	@Test
	public void findMultipleIncludeDefault() {
		PathSet pathSet = new PathSet();
		assertThat(pathSet.find("txt", "xxx/")).accepts("foo.txt", "xxx/bar.bar", "xxx/foo.txt/yyy")
			.rejects("foo.bar");
	}

	@Test
	public void findOneIncludeConstructorExclude() {
		PathSet pathSet = new PathSet("txt").exclude("xxx/");
		assertThat(pathSet.find()).accepts("foo.txt")
			.rejects("foo.bar", "xxx/foo.txt/yyy", "xxx/bar.txt");
	}

	@Test
	public void findOneIncludeMethodExclude() {
		PathSet pathSet = new PathSet().include("txt")
			.exclude("xxx/");
		assertThat(pathSet.find()).accepts("foo.txt")
			.rejects("foo.bar", "xxx/foo.txt/yyy", "xxx/bar.txt");
	}

	@Test
	public void findOneIncludeDefaultExclude() {
		PathSet pathSet = new PathSet().exclude("xxx/");
		assertThat(pathSet.find("txt")).accepts("foo.txt")
			.rejects("foo.bar", "xxx/foo.txt/yyy", "xxx/bar.txt");
	}
}
