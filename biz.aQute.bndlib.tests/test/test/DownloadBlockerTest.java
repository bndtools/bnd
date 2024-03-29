package test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.io.File;

import org.junit.jupiter.api.Test;

import aQute.bnd.build.DownloadBlocker;

public class DownloadBlockerTest {

	@Test
	public void testSimple() throws Exception {
		{
			DownloadBlocker db = new DownloadBlocker(null);
			assertEquals(DownloadBlocker.Stage.INIT, db.getStage());
			db.failure(new File(""), "ouch");
			assertEquals(DownloadBlocker.Stage.FAILURE, db.getStage());
		}
		{
			DownloadBlocker db = new DownloadBlocker(null);
			db.success(new File(""));
			assertEquals(DownloadBlocker.Stage.SUCCESS, db.getStage());
		}
		{
			final DownloadBlocker dbb = new DownloadBlocker(null);

			Thread t = new Thread() {
				@Override
				public void run() {
					try {
						Thread.sleep(1000);
						dbb.failure(new File(""), "Ouch");
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			};
			t.start();
			assertEquals(DownloadBlocker.Stage.INIT, dbb.getStage());
			assertEquals("Ouch", dbb.getReason());
			assertEquals(DownloadBlocker.Stage.FAILURE, dbb.getStage());
		}
		{
			final DownloadBlocker dbb = new DownloadBlocker(null);

			Thread t = new Thread() {
				@Override
				public void run() {
					try {
						Thread.sleep(1000);
						dbb.success(new File(""));
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			};
			t.start();
			assertEquals(DownloadBlocker.Stage.INIT, dbb.getStage());
			assertNull(dbb.getReason());
			assertEquals(DownloadBlocker.Stage.SUCCESS, dbb.getStage());
		}
	}
}
