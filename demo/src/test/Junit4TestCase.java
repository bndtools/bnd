package test;

import java.util.Arrays;
import java.util.Collection;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

@RunWith(Parameterized.class)
public class Junit4TestCase {

	@Parameters
	public static Collection<Object[]> data() {
		return Arrays.asList(new Object[][] {
			{
				10, 1
			}, {
				10, 1
			}
		});
	}

	public Junit4TestCase(int i, int j) {
		System.out.println("out: " + i + " " + j);
	}

	@Test
	public void foo() {
		System.out.println("foo");
	}
}
