package aQute.libg.qtokens;

import static org.junit.Assert.*;

import org.junit.Test;

public class QuotedTokenizerTest {
	@Test
	public void testQuotedTokenize() {
		String test = "this,is','a,test";
		QuotedTokenizer tokenizer = new QuotedTokenizer(test, ",");
		assertEquals("this", tokenizer.nextToken());
		assertEquals("is,a", tokenizer.nextToken());
		assertEquals("test", tokenizer.nextToken());
	}
}