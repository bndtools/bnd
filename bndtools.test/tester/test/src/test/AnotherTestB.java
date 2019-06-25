package test;

import static org.assertj.core.api.Assertions.*;

import org.junit.Test;

public class AnotherTestB {

	@Test
	public void junit4Test() {
		assertThat("something").isEqualTo("something else");
	}
	
	@org.junit.jupiter.api.Test
	public void junit5Test() {
		assertThat("something or other").isEqualTo("somethingher");
	}
}
