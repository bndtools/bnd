package aQute.libg.parameters;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

class AttributesTest {

	@SuppressWarnings("unchecked")
	@Test
	void test() {
		Attributes a = new Attributes("foo = 1 , bar:Long = 42, l:List<Long> = '1,2,3,4'");
		assertThat(a.toString()).isEqualTo("foo=1,bar:Long=42,l:List<Long>=\"1,2,3,4\"");
		assertThat(a.get("foo")).isEqualTo("1");
		assertThat(a.getTyped("foo")).isEqualTo("1");
		assertThat(a.getTyped("bar")).isEqualTo(42L);
		assertThat(a.get("l")).isEqualTo("1,2,3,4");
		assertThat((List<Long>) a.getTyped("l")).containsExactly(1L, 2L, 3L, 4L);
	}

}
