package aQute.lib.date;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

import org.junit.Test;

public class DatesTest {
	@Test
	public void testRFC_7231() {
		Instant now = Instant.now();
		now = now.minusNanos(now.getNano());
		String dateHeader = Dates.RFC_7231_DATE_TIME.format(now);
		ZonedDateTime zdt = Dates.parse(dateHeader);
		assertThat(zdt.toInstant()).isEqualTo(now);
	}

	@Test
	public void testMillis() {
		final ZoneId otherZone = ZoneId.of("Australia/Eucla");
		final long milli = System.currentTimeMillis();

		String formattedCurrent = Dates.formatMillis(DateTimeFormatter.ISO_LOCAL_DATE_TIME, milli);
		ZonedDateTime current = Dates.parse(formattedCurrent);
		assertThat(current.getZone()).isEqualTo(Dates.UTC_ZONE_ID);
		ZonedDateTime other = current.withZoneSameInstant(otherZone);
		assertThat(other.getZone()).isEqualTo(otherZone);
		String formattedOther = DateTimeFormatter.ISO_DATE_TIME.format(other);
		long currentMillis = Dates.parseMillis(DateTimeFormatter.ISO_LOCAL_DATE_TIME, formattedCurrent);
		assertThat(currentMillis).isEqualTo(milli);
		long otherMillis = Dates.parseMillis(DateTimeFormatter.ISO_DATE_TIME, formattedOther);
		assertThat(otherMillis).isEqualTo(milli);
		LocalDateTime local = other.toLocalDateTime();
		long localMillis = Dates.parseMillis(DateTimeFormatter.ISO_LOCAL_DATE_TIME.withZone(otherZone),
			local.toString());
		assertThat(localMillis).isEqualTo(milli);
	}


	@Test
	public void testBackAndForth() {
		check(Instant.ofEpochMilli(0)
			.toString());
		check(Dates.parse("1970-01-01")
			.toString());
	}


	@Test
	public void testISO_LOCAL_DATE_TIME() {
		check("1970-01-01T00:00:00Z");
		check("1970-01-01T00:00Z");
		check("1970-01-01T00:00:00");
		check("1970-01-01T00:00");
	}


	@Test
	public void testISO_LOCAL_DATE() {
		check("1970-01-01");

	}

	@Test
	public void testISO_OFFSET_DATE() {
		check("1970-01-01+00:00:00");
		check("1970-01-01+00:00:00");
	}

	@Test
	public void testISO_OFFSET_DATE_TIME() {
		check("1970-01-01T00:00:00+00:00");
		check("1970-01-01T00:00:00+00:00:00");
		check("1970-01-01T00:00+00:00");
		check("1970-01-01T00:00+00:00:00");
	}

	@Test
	public void testISO_ORDINAL_DATE() {
		check("1970-001");
	}

	@Test
	public void testISO_ZONED_DATE_TIME() {
		check("1970-01-01T00:00:00+00:00[UTC]");
		check("1970-01-01T00:00:00+00:00:00[UTC]");
		check("1970-01-01T00:00+00:00:00[UTC]");
	}

	@Test
	public void testISO_WEEK_DATE() {
		check("1970-W01-4");
	}

	@Test
	public void RFC_1123_DATE_TIME() {
		check("Thu, 1 Jan 1970 00:00:00 GMT");
	}

	@Test
	public void RFC_7231_DATE_TIME() {
		check("Thu, 01 Jan 1970 00:00:00 GMT");
	}

	@Test
	public void yyMMdd() {
		check("19700101");
	}

	@Test
	public void yyMMddHHmm() {
		check("197001010000");
	}

	@Test
	public void yyyyMMddHHmmss() {
		check("19700101000000.000+00");
		check("19700101000000+00");
		check("19700101000000");
		check("1970/01/01 00:00:00");
	}

	@Test
	public void yyMMddHHmmss_SSSZ() {
		check("19700101000000.000Z");
		check("19700101000000.000+00");
		check("19700101000000.000+0000");
		check("19700101000000.000+0000");
	}

	@Test
	public void yyyyMMdd_HHmm() {
		check("19700101 0000");
	}

	@Test
	public void dd_MM_yyyy() {
		check("01-01-1970");
	}

	@Test
	public void MM_dd_yyyy() {
		check("01-01-1970");
	}

	@Test
	public void yyyy_MM_dd() {
		check("1970/01/01");

	}

	@Test
	public void dd_MMM_yyyy() {
		check("01 Jan 1970");
	}

	@Test
	public void yyyy_MM_dd_HH_mm() {
		check("1970-01-01 00:00");
	}

	@Test
	public void yyyy_MM_dd_HH_mm2() {
		check("1970/01/01 00:00");
	}

	@Test
	public void MM_dd_yyyy_HH_mm() {
		check("01/01/1970 00:00");
	}

	@Test
	public void dd_MM_yyyy_HH_mm() {
		check("01 Jan 1970 00:00");
	}

	@Test
	public void dd_MMM_yyyy_HH_mm() {
		check("01 January 1970 00:00");
	}

	@Test
	public void yyyyMMdd_HHmmss() {
		check("19700101 000000");
		check("19700101 000000+00");
	}

	@Test
	public void dd_MM_yyyy_HH_mm_ss() {
		check("01-01-1970 00:00:00");
	}

	@Test
	public void yyyy_MM_dd_HH_mm_ss() {
		check("1970-01-01 00:00:00");
	}

	private void check(String string) {
		ZonedDateTime parse = Dates.parse(string);
		assertThat(parse).isNotNull();

		assertThat(parse
			.toInstant()
			.toEpochMilli()).isEqualTo(0L);
	}
}
