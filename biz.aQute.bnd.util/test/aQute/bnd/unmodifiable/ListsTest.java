package aQute.bnd.unmodifiable;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.ListIterator;
import java.util.NoSuchElementException;

import org.junit.jupiter.api.Test;

import aQute.lib.io.ByteBufferInputStream;
import aQute.lib.io.ByteBufferOutputStream;

public class ListsTest {

	@Test
	public void zero() {
		List<String> list = Lists.of();
		assertThat(list).hasSize(0)
			.isEmpty();
		assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() -> list.add("a"));
		assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() -> list.remove("a"));
		assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() -> list.clear());
	}

	@Test
	public void one() {
		List<String> list = Lists.of("e1");
		assertThat(list).hasSize(1)
			.containsExactly("e1");
		assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() -> list.add("a"));
		assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() -> list.remove("a"));
		assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() -> list.remove("e1"));
		assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() -> list.clear());
	}

	@Test
	public void two() {
		List<String> list = Lists.of("e1", "e2");
		assertThat(list).hasSize(2)
			.containsExactly("e1", "e2");
		assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() -> list.add("a"));
		assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() -> list.remove("a"));
		assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() -> list.remove("e1"));
		assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() -> list.clear());
	}

	@Test
	public void three() {
		List<String> list = Lists.of("e1", "e2", "e3");
		assertThat(list).hasSize(3)
			.containsExactly("e1", "e2", "e3");
		assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() -> list.add("a"));
		assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() -> list.remove("a"));
		assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() -> list.remove("e1"));
		assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() -> list.clear());
	}

	@Test
	public void four() {
		List<String> list = Lists.of("e1", "e2", "e3", "e4");
		assertThat(list).hasSize(4)
			.containsExactly("e1", "e2", "e3", "e4");
		assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() -> list.add("a"));
		assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() -> list.remove("a"));
		assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() -> list.remove("e1"));
		assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() -> list.clear());
	}

	@Test
	public void five() {
		List<String> list = Lists.of("e1", "e2", "e3", "e4", "e5");
		assertThat(list).hasSize(5)
			.containsExactly("e1", "e2", "e3", "e4", "e5");
		assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() -> list.add("a"));
		assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() -> list.remove("a"));
		assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() -> list.remove("e1"));
		assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() -> list.clear());
	}

	@Test
	public void six() {
		List<String> list = Lists.of("e1", "e2", "e3", "e4", "e5", "e6");
		assertThat(list).hasSize(6)
			.containsExactly("e1", "e2", "e3", "e4", "e5", "e6");
		assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() -> list.add("a"));
		assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() -> list.remove("a"));
		assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() -> list.remove("e1"));
		assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() -> list.clear());
	}

	@Test
	public void seven() {
		List<String> list = Lists.of("e1", "e2", "e3", "e4", "e5", "e6", "e7");
		assertThat(list).hasSize(7)
			.containsExactly("e1", "e2", "e3", "e4", "e5", "e6", "e7");
		assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() -> list.add("a"));
		assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() -> list.remove("a"));
		assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() -> list.remove("e1"));
		assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() -> list.clear());
	}

	@Test
	public void eight() {
		List<String> list = Lists.of("e1", "e2", "e3", "e4", "e5", "e6", "e7", "e8");
		assertThat(list).hasSize(8)
			.containsExactly("e1", "e2", "e3", "e4", "e5", "e6", "e7", "e8");
		assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() -> list.add("a"));
		assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() -> list.remove("a"));
		assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() -> list.remove("e1"));
		assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() -> list.clear());
	}

	@Test
	public void nine() {
		List<String> list = Lists.of("e1", "e2", "e3", "e4", "e5", "e6", "e7", "e8", "e9");
		assertThat(list).hasSize(9)
			.containsExactly("e1", "e2", "e3", "e4", "e5", "e6", "e7", "e8", "e9");
		assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() -> list.add("a"));
		assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() -> list.remove("a"));
		assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() -> list.remove("e1"));
		assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() -> list.clear());
	}

	@Test
	public void ten() {
		List<String> list = Lists.of("e1", "e2", "e3", "e4", "e5", "e6", "e7", "e8", "e9", "e10");
		assertThat(list).hasSize(10)
			.containsExactly("e1", "e2", "e3", "e4", "e5", "e6", "e7", "e8", "e9", "e10");
		assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() -> list.add("a"));
		assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() -> list.remove("a"));
		assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() -> list.remove("e1"));
		assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() -> list.clear());
	}

	@Test
	public void entries() {
		String[] entries = new String[] {
			"e1", "e2", "e3", "e4", "e5", "e6", "e7", "e8", "e9", "e10", "e11"
		};
		List<String> list = Lists.of(entries);
		entries[0] = "changed";
		assertThat(list).hasSize(11)
			.containsExactly("e1", "e2", "e3", "e4", "e5", "e6", "e7", "e8", "e9", "e10", "e11");
		assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() -> list.add("a"));
		assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() -> list.remove("a"));
		assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() -> list.remove("e1"));
		assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() -> list.clear());
	}

	@Test
	public void duplicate_element() {
		assertThatCode(() -> Lists.of("e1", "e1")).doesNotThrowAnyException();
		assertThatCode(() -> Lists.of("e1", "e2", "e2")).doesNotThrowAnyException();
		assertThatCode(() -> Lists.of("e1", "e2", "e3", "e3")).doesNotThrowAnyException();
	}

	@Test
	public void null_arguments() {
		assertThatNullPointerException().isThrownBy(() -> Lists.of("e1", null));
		assertThatNullPointerException().isThrownBy(() -> Lists.of("e1", "e2", null));
		List<String> nullElement = new ArrayList<>();
		nullElement.add("e1");
		nullElement.add(null);
		assertThatNullPointerException().isThrownBy(() -> Lists.copyOf(nullElement));
	}

	@Test
	public void copy() {
		List<String> source = new ArrayList<>();
		source.add("e1");
		source.add("e2");
		source.add("e1");
		List<String> list = Lists.copyOf(source);
		source.set(0, "changed");
		assertThat(list).hasSize(3)
			.containsExactly("e1", "e2", "e1");
		assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() -> list.add("a"));
		assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() -> list.remove("a"));
		assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() -> list.remove("e1"));
		assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() -> list.clear());
	}

	@Test
	public void copy_unmodifiable() {
		List<String> source = Lists.of("e1", "e2");
		List<String> list = Lists.copyOf(source);
		assertThat(list).isSameAs(source);
	}

	@Test
	public void list() {
		List<String> list = Lists.of("e1", "e2");
		assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() -> list.replaceAll(i -> i));
		assertThatExceptionOfType(UnsupportedOperationException.class)
			.isThrownBy(() -> list.sort(String.CASE_INSENSITIVE_ORDER));
	}

	@Test
	public void list_iterator() {
		List<String> list = Lists.of("e1", "e2", "e3", "e1", "e5");
		assertThat(list.indexOf("e1")).isEqualTo(0);
		assertThat(list.lastIndexOf("e1")).isEqualTo(3);
		ListIterator<String> listIterator = list.listIterator(0);
		assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() -> listIterator.add("a"));

		assertThat(listIterator.hasPrevious()).isFalse();
		assertThat(listIterator.hasNext()).isTrue();
		assertThat(listIterator.previousIndex()).isEqualTo(-1);
		assertThat(listIterator.nextIndex()).isEqualTo(0);
		assertThatExceptionOfType(NoSuchElementException.class).isThrownBy(() -> listIterator.previous());
		assertThat(listIterator.next()).isEqualTo("e1");
		assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() -> listIterator.set("a"));
		assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() -> listIterator.remove());

		assertThat(listIterator.hasPrevious()).isTrue();
		assertThat(listIterator.hasNext()).isTrue();
		assertThat(listIterator.previousIndex()).isEqualTo(0);
		assertThat(listIterator.nextIndex()).isEqualTo(1);
		assertThat(listIterator.next()).isEqualTo("e2");

		assertThat(listIterator.hasPrevious()).isTrue();
		assertThat(listIterator.hasNext()).isTrue();
		assertThat(listIterator.previousIndex()).isEqualTo(1);
		assertThat(listIterator.nextIndex()).isEqualTo(2);
		assertThat(listIterator.next()).isEqualTo("e3");

		assertThat(listIterator.hasPrevious()).isTrue();
		assertThat(listIterator.hasNext()).isTrue();
		assertThat(listIterator.previousIndex()).isEqualTo(2);
		assertThat(listIterator.nextIndex()).isEqualTo(3);
		assertThat(listIterator.next()).isEqualTo("e1");

		assertThat(listIterator.hasPrevious()).isTrue();
		assertThat(listIterator.hasNext()).isTrue();
		assertThat(listIterator.previousIndex()).isEqualTo(3);
		assertThat(listIterator.nextIndex()).isEqualTo(4);
		assertThat(listIterator.next()).isEqualTo("e5");

		assertThat(listIterator.hasPrevious()).isTrue();
		assertThat(listIterator.hasNext()).isFalse();
		assertThat(listIterator.previousIndex()).isEqualTo(4);
		assertThat(listIterator.nextIndex()).isEqualTo(5);
		assertThatExceptionOfType(NoSuchElementException.class).isThrownBy(() -> listIterator.next());

		assertThat(listIterator.previous()).isEqualTo("e5");
		assertThat(listIterator.hasPrevious()).isTrue();
		assertThat(listIterator.hasNext()).isTrue();
		assertThat(listIterator.previousIndex()).isEqualTo(3);
		assertThat(listIterator.nextIndex()).isEqualTo(4);

		assertThat(listIterator.previous()).isEqualTo("e1");
		assertThat(listIterator.hasPrevious()).isTrue();
		assertThat(listIterator.hasNext()).isTrue();
		assertThat(listIterator.previousIndex()).isEqualTo(2);
		assertThat(listIterator.nextIndex()).isEqualTo(3);

		assertThat(listIterator.previous()).isEqualTo("e3");
		assertThat(listIterator.hasPrevious()).isTrue();
		assertThat(listIterator.hasNext()).isTrue();
		assertThat(listIterator.previousIndex()).isEqualTo(1);
		assertThat(listIterator.nextIndex()).isEqualTo(2);

		assertThat(listIterator.previous()).isEqualTo("e2");
		assertThat(listIterator.hasPrevious()).isTrue();
		assertThat(listIterator.hasNext()).isTrue();
		assertThat(listIterator.previousIndex()).isEqualTo(0);
		assertThat(listIterator.nextIndex()).isEqualTo(1);

		assertThat(listIterator.previous()).isEqualTo("e1");
		assertThat(listIterator.hasPrevious()).isFalse();
		assertThat(listIterator.hasNext()).isTrue();
		assertThat(listIterator.previousIndex()).isEqualTo(-1);
		assertThat(listIterator.nextIndex()).isEqualTo(0);

		assertThatExceptionOfType(NoSuchElementException.class).isThrownBy(() -> listIterator.previous());
	}

	@Test
	public void list_iterator_bad_index() {
		List<String> list = Lists.of("e1", "e2", "e3", "e1", "e5");
		assertThatExceptionOfType(IndexOutOfBoundsException.class).isThrownBy(() -> list.listIterator(-1));
		assertThatExceptionOfType(IndexOutOfBoundsException.class).isThrownBy(() -> list.listIterator(list.size() + 1));
		assertThatCode(() -> list.listIterator(list.size())).doesNotThrowAnyException();
	}

	@Test
	public void sublist() {
		List<String> source = Lists.of("e1", "e2", "e3", "e1", "e5");
		List<String> list = source.subList(1, 3);
		assertThat(list).hasSize(2)
			.containsExactly("e2", "e3");
		assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() -> list.add("a"));
		assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() -> list.remove("a"));
		assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() -> list.remove("e1"));
		assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() -> list.clear());

		List<String> empty = source.subList(1, 1);
		assertThat(empty).hasSize(0)
			.isEmpty();

		assertThatExceptionOfType(IndexOutOfBoundsException.class).isThrownBy(() -> source.subList(-1, 1));
		assertThatExceptionOfType(IndexOutOfBoundsException.class)
			.isThrownBy(() -> source.subList(0, source.size() + 1));
		assertThatExceptionOfType(IndexOutOfBoundsException.class).isThrownBy(() -> source.subList(2, 1));
		assertThatCode(() -> source.subList(0, source.size())).doesNotThrowAnyException();
	}

	@Test
	public void array() {
		List<String> source = Lists.of("e1", "e2", "e3", "e1", "e5");
		Object[] array = source.toArray();
		assertThat(array).hasSize(5)
			.containsExactly("e1", "e2", "e3", "e1", "e5");
	}

	@Test
	public void array_string() {
		List<String> source = Lists.of("e1", "e2", "e3", "e1", "e5");
		String[] target = new String[0];
		String[] array = source.toArray(target);
		assertThat(array).isNotSameAs(target)
			.hasSize(5)
			.containsExactly("e1", "e2", "e3", "e1", "e5");

		target = new String[source.size() + 1];
		array = source.toArray(target);
		assertThat(array).isSameAs(target)
			.containsExactly("e1", "e2", "e3", "e1", "e5", null);
	}

	@Test
	public void contains() {
		List<String> list = Lists.of("e1", "e2", "e3", "e4", "e5");
		assertThat(list.contains("e1")).isTrue();
		assertThat(list.contains("e3")).isTrue();
		assertThat(list.contains("e5")).isTrue();
		assertThat(list.contains("e6")).isFalse();
		assertThat(list.contains(null)).isFalse();
	}

	@Test
	public void index() {
		List<String> list = Lists.of("e1", "e2", "e3", "e2", "e1");
		assertThat(list.indexOf("e1")).isEqualTo(0);
		assertThat(list.indexOf("e2")).isEqualTo(1);
		assertThat(list.indexOf("e6")).isEqualTo(-1);
		assertThat(list.indexOf(null)).isEqualTo(-1);
	}

	@Test
	public void last_index() {
		List<String> list = Lists.of("e1", "e2", "e3", "e2", "e1");
		assertThat(list.lastIndexOf("e1")).isEqualTo(4);
		assertThat(list.lastIndexOf("e2")).isEqualTo(3);
		assertThat(list.lastIndexOf("e6")).isEqualTo(-1);
		assertThat(list.lastIndexOf(null)).isEqualTo(-1);
	}

	@Test
	public void hashcode() {
		List<String> list = Lists.of("e1", "e2", "e3", "e4", "e5");
		List<String> arrayList = new ArrayList<>(Arrays.asList("e1", "e2", "e3", "e4", "e5"));

		assertThat(list.hashCode()).isEqualTo(arrayList.hashCode());
	}

	@Test
	public void equals() {
		List<String> list = Lists.of("e1", "e2", "e3", "e4", "e5");
		List<String> arrayList = new ArrayList<>(Arrays.asList("e1", "e2", "e3", "e4", "e5"));

		assertThat(list).isEqualTo(arrayList);
		assertThat(list).isNotEqualTo(Arrays.asList("e5", "e2", "e3", "e4", "e1"));
		assertThat(list).isNotEqualTo(Arrays.asList("e1", "e2", "e3", "e4"));
		assertThat(list).isNotEqualTo(Arrays.asList("e1", "e2", "e3", "e4", "e5", "e6"));
	}

	@Test
	public void serialization() throws Exception {
		List<String> list = Lists.of("e1", "e2", "e3", "e4", "e5");
		ByteBufferOutputStream bos = new ByteBufferOutputStream();
		try (ObjectOutputStream oos = new ObjectOutputStream(bos)) {
			oos.writeObject(list);
		}
		ObjectInputStream ois = new ObjectInputStream(new ByteBufferInputStream(bos.toByteBuffer()));
		@SuppressWarnings("unchecked")
		List<String> deser = (List<String>) ois.readObject();

		assertThat(deser).isEqualTo(list)
			.isNotSameAs(list)
			.containsExactlyElementsOf(list);
		assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() -> deser.add("a"));
		assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() -> deser.remove("a"));
		assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() -> deser.remove("e1"));
		assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() -> deser.clear());
	}

	@Test
	public void serialization_zero() throws Exception {
		List<String> list = Lists.of();
		ByteBufferOutputStream bos = new ByteBufferOutputStream();
		try (ObjectOutputStream oos = new ObjectOutputStream(bos)) {
			oos.writeObject(list);
		}
		ObjectInputStream ois = new ObjectInputStream(new ByteBufferInputStream(bos.toByteBuffer()));
		@SuppressWarnings("unchecked")
		List<String> deser = (List<String>) ois.readObject();

		assertThat(deser).isSameAs(list);
		assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() -> deser.add("a"));
		assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() -> deser.remove("a"));
		assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() -> deser.clear());
	}

}
