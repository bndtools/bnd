package test.fragment;

import static org.assertj.core.api.Assertions.*;

import org.assertj.core.util.Arrays;
import org.junit.jupiter.api.Test;

public class FragTest {

	@Test
	public void myTest() {
		assertThat("a").isNotEqualTo("b");
	}
	
	@Test
	public void anotherTestr() {
		assertThat("a").isNotEqualTo("a");
	}
	
	@Test
	public void yetAnotherTest() {
		assertThat(Arrays.array("A", "B", "C")).isEqualTo("a");
	}	
}
