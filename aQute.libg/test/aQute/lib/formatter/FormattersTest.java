package aQute.lib.formatter;

import static aQute.lib.formatter.Formatters.format;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

public class FormattersTest {

	@Test
	public void testAllConversionsToRightType() {
		// bBhHsScCdoxXeEfgGaA%n
		assertThat(format("%b", "1")).isEqualTo("true");
		assertThat(format("%b", "  ")).isEqualTo("false");
		assertThat(format("%b", "0")).isEqualTo("false");
		assertThat(format("%b", "0.0")).isEqualTo("false");
		assertThat(format("%b", "+0.0D")).isEqualTo("false");
		assertThat(format("%b", "-0.00f")).isEqualTo("false");
		assertThat(format("%B", "  false")).isEqualTo("FALSE");
		assertThat(format("%B", "1")).isEqualTo("TRUE");
		assertThat(format("%h", "+")).isEqualTo("2b");
		assertThat(format("%H", "+")).isEqualTo("2B");
		assertThat(format("%s", "tile")).isEqualTo("tile");
		assertThat(format("%S", "tile")).isEqualTo("TILE");
		assertThat(format("%c", "i")).isEqualTo("i");
		assertThat(format("%C", "i")).isEqualTo("I");
		assertThat(format("%d", "01")).isEqualTo("1");
		assertThat(format("%e", "1")).isEqualTo("1.000000e+00");
		assertThat(format("%E", "1")).isEqualTo("1.000000E+00");
		assertThat(format("%f", "1")).isEqualTo("1.000000");
		assertThat(format("%g", "1")).isEqualTo("1.00000");
		assertThat(format("%G", "1")).isEqualTo("1.00000");
		assertThat(format("%o", "8")).isEqualTo("10");
		assertThat(format("%x", "15")).isEqualTo("f");
		assertThat(format("%X", "15")).isEqualTo("F");
		assertThat(format("%a", "1")).isEqualTo("0x1.0p0");
		assertThat(format("%A", "1")).isEqualTo("0X1.0P0");
		assertThat(format("%%")).isEqualTo("%");
		assertThat(format("%n")).isEqualTo(System.lineSeparator());
	}

	@Test
	public void testIndex() {
		assertThat(format("%s", "1")).isEqualTo("1");
		assertThat(format("%2$d %s %d", "1", "2")).isEqualTo("2 1 2");
		assertThat(format("%2$d %<d", "1", "2")).isEqualTo("2 2");
		assertThat(format("%d %<d", "1", "2")).isEqualTo("1 1");
		assertThat(format("%1$d %2$d %<s", "1", "2")).isEqualTo("1 2 2");
		assertThat(format("%1$s %<s %s", "1", "2")).isEqualTo("1 1 1");
		assertThat(format("%s %<s %s", "1", "2")).isEqualTo("1 1 2");
		assertThat(format("%% %s %% %<s %% %s", "1", "2")).isEqualTo("% 1 % 1 % 2");
		assertThat(format("%n %s %n %<s %n %s", "1", "2")).matches("\r?\n 1 \r?\n 1 \r?\n 2");
		assertThat(format("%2$d %<s %<s %s", "1", "2")).isEqualTo("2 2 2 1");
	}

	@Test
	public void testDate() {
		assertThat(format("%tY %<tm %<td %<tZ", "1970-01-01")).isEqualTo("1970 01 01 UTC");
		assertThat(format("%TY %<tm %<Td %<TZ", "1970-01-01")).isEqualTo("1970 01 01 UTC");
	}

	@Test
	public void testTime() {
		assertThat(format("%tI %<tM %<tS %<tp", "1970-01-01T13:45:00")).isEqualTo("01 45 00 pm");
		assertThat(format("%TI %<TM %<TS %<Tp", "1970-01-01T13:45:00")).isEqualTo("01 45 00 PM");
	}
}
