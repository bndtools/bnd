package aQute.lib.annotations.setter;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class AnnotationSetterTest {

	@interface Foo {
		int number();

	}

	@Test
	public void testSimple() {
		AnnotationSetter<Foo> as = new AnnotationSetter<>(Foo.class);
		as.set(as.a()
			.number(), 10);
		assertEquals("@aQute.lib.annotations.setter.AnnotationSetterTest.Foo(number=10)", as.a()
			.toString());
	}

	enum AnEnum {
		A,
		B,
		C
	}

	@interface Types {
		byte abyte() default 5;

		short ashort() default 5;

		int anint() default 5;

		long along() default 5;

		float afloat() default 5;

		double adouble() default 5;

		boolean aboolean() default false;

		char achar() default '5';

		String astring() default "5";

		Class<?> aclass() default Object.class;

		Class<?>[] aclassarray() default {
			Object.class
		};

		AnEnum anenum() default AnEnum.A;

		Foo nested();

		byte[] abytearray() default {
			5
		};

		short[] ashortarray() default {
			5
		};

		int[] anintarray() default {
			1, 2
		};

		long[] alongarray() default {
			5
		};

		float[] afloatarray() default {
			5
		};

		double[] adoublearray() default {
			5
		};

		boolean[] abooleanarray() default {
			false
		};

		char[] achararray() default {
			'5'
		};

		String[] astringarray() default {
			"5"
		};

		Foo[] nestedarray() default {};

		AnEnum[] anenumarray() default {
			AnEnum.A
		};

	}

	@Test
	public void testTypes() {
		AnnotationSetter<Types> as = new AnnotationSetter<>(Types.class);

		as.set(as.a()
			.aclass(), "a.b.C.class");
		as.set(as.a()
			.aclassarray(), new String[] {
				"a.b.D.class"
		});
		as.set(as.a()
			.aboolean(), 1);

		as.set(as.a()
			.abyte(), 1);
		as.set(as.a()
			.achar(), 1);
		as.set(as.a()
			.ashort(), 1);
		as.set(as.a()
			.anint(), 1);
		as.set(as.a()
			.along(), 1);
		as.set(as.a()
			.afloat(), 1);
		as.set(as.a()
			.adouble(), 1);
		as.set(as.a()
			.astring(), 1);
		as.set(as.a()
			.anenum(), AnEnum.B);
		as.set(as.a()
			.abooleanarray(), new boolean[] {
				false
		});
		as.set(as.a()
			.abytearray(), new byte[] {
				1, 2
		});
		as.set(as.a()
			.achararray(), new char[] {
				'1', '2'
		});
		as.set(as.a()
			.ashortarray(), new short[] {
				1, 2
		});
		as.set(as.a()
			.anintarray(), new int[] {
				1, 2
		});
		as.set(as.a()
			.alongarray(), new long[] {
				1, 2
		});
		as.set(as.a()
			.afloatarray(), new float[] {
				1, 2
		});
		as.set(as.a()
			.adoublearray(), new double[] {
				1, 2
		});
		as.set(as.a()
			.astringarray(), new String[] {
				"1", "2"
		});
		as.set(as.a()
			.anenumarray(), new AnEnum[] {
				AnEnum.B, AnEnum.C
		});

		assertEquals(
			"@aQute.lib.annotations.setter.AnnotationSetterTest.Types(adouble=1.0,astringarray={\"1\",\"2\"},astring=\"1\",ashortarray={1,2},anint=1,aboolean=true,abyte=1,afloat=1.0,anenum=aQute.lib.annotations.setter.AnnotationSetterTest.AnEnum.B,alongarray={1,2},ashort=1,achar=,along=1,achararray={1,2},anenumarray={aQute.lib.annotations.setter.AnnotationSetterTest.AnEnum.B,aQute.lib.annotations.setter.AnnotationSetterTest.AnEnum.C},aclassarray=a.b.D.class,afloatarray={1.0,2.0},adoublearray={1.0,2.0},aclass=a.b.C.class,abytearray={1,2})",
			as.a()
				.toString());
	}

	@Test
	public void testDefaults() {
		AnnotationSetter<Types> as = new AnnotationSetter<>(Types.class);
		as.set(as.a()
			.anint(), 5);
		assertEquals("@aQute.lib.annotations.setter.AnnotationSetterTest.Types", as.a()
			.toString());
		as.set(as.a()
			.anint(), 6);
		assertEquals("@aQute.lib.annotations.setter.AnnotationSetterTest.Types(anint=6)", as.a()
			.toString());
	}

	@Test
	public void testDefaultArrays() {
		AnnotationSetter<Types> as = new AnnotationSetter<>(Types.class);
		as.set(as.a()
			.anintarray(), new int[] {
				1, 2
		});
		assertEquals("@aQute.lib.annotations.setter.AnnotationSetterTest.Types", as.a()
			.toString());
		as.set(as.a()
			.anintarray(), new int[] {
				3, 4
		});
		assertEquals("@aQute.lib.annotations.setter.AnnotationSetterTest.Types(anintarray={3,4})", as.a()
			.toString());
	}

	@Test
	public void testEscaping() {
		AnnotationSetter<Types> as = new AnnotationSetter<>(Types.class);
		as.set(as.a()
			.astring(), "abc\t\n\r\f\u0200def~");
		assertEquals(
			"@aQute.lib.annotations.setter.AnnotationSetterTest.Types(astring=\"abc\\t\\n\\r\\f\\u0200def\\u007e\")",
			as.a()
				.toString());
	}

}
