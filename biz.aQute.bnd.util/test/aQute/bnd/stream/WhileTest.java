package aQute.bnd.stream;

import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Stream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class WhileTest {

	Set<String> testSet;

	@BeforeEach
	@SuppressWarnings("unchecked")
	public void setUp() throws Exception {
		testSet = new HashSet<>();
		testSet.add("key1");
		testSet.add("key2");
		testSet.add("key3");
		testSet.add("key4");
		testSet.add("key5");
	}

	@AfterEach
	public void tearDown() throws Exception {}

	@Test
	public void takeWhile() {
		Supplier<Stream<String>> supplier = () -> TakeWhile.takeWhile(testSet.stream()
			.sorted(), k -> !k.equals("key3"));
		assertThat(supplier.get()
			.count()).isEqualTo(2);
		assertThat(supplier.get()).containsExactly("key1", "key2");
		assertThat(supplier.get()
			.collect(toList())).containsExactly("key1", "key2");
	}

	@Test
	public void takeWhileAll() {
		Supplier<Stream<String>> supplier = () -> TakeWhile.takeWhile(testSet.stream(), k -> !k.equals("all"));
		assertThat(supplier.get()
			.count()).isEqualTo(testSet.size());
		assertThat(supplier.get()).containsExactlyInAnyOrder("key1", "key2", "key3", "key4", "key5");
		assertThat(supplier.get()
			.collect(toList())).containsExactlyInAnyOrder("key1", "key2", "key3", "key4", "key5");
	}

	@Test
	public void takeWhileNone() {
		Supplier<Stream<String>> supplier = () -> TakeWhile.takeWhile(testSet.stream(), k -> k.equals("none"));
		assertThat(supplier.get()
			.count()).isEqualTo(0);
		assertThat(supplier.get()).isEmpty();
		assertThat(supplier.get()
			.collect(toList())).isEmpty();
	}

	@Test
	public void dropWhile() {
		Supplier<Stream<String>> supplier = () -> DropWhile.dropWhile(testSet.stream()
			.sorted(), k -> !k.equals("key3"));
		assertThat(supplier.get()
			.count()).isEqualTo(3);
		assertThat(supplier.get()).containsExactly("key3", "key4", "key5");
		assertThat(supplier.get()
			.collect(toList())).containsExactly("key3", "key4", "key5");
	}

	@Test
	public void dropWhileNone() {
		Supplier<Stream<String>> supplier = () -> DropWhile.dropWhile(testSet.stream()
			.sorted(), k -> k.equals("none"));
		assertThat(supplier.get()
			.count()).isEqualTo(testSet.size());
		assertThat(supplier.get()).containsExactlyInAnyOrder("key1", "key2", "key3", "key4", "key5");
		assertThat(supplier.get()
			.collect(toList())).containsExactlyInAnyOrder("key1", "key2", "key3", "key4", "key5");
	}

	@Test
	public void dropWhileAll() {
		Supplier<Stream<String>> supplier = () -> DropWhile.dropWhile(testSet.stream(), k -> !k.equals("all"));
		assertThat(supplier.get()
			.count()).isEqualTo(0);
		assertThat(supplier.get()).isEmpty();
		assertThat(supplier.get()
			.collect(toList())).isEmpty();
	}

}
