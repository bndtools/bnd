package aQute.lib.unmodifiable;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

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
		List<String> list = Lists.of("e1", "e2", "e3", "e4", "e5", "e6", "e7", "e8", "e9", "e10", "e11");
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

}
