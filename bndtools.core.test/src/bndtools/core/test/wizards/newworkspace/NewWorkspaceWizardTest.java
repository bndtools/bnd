package bndtools.core.test.wizards.newworkspace;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.regex.Pattern;
import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.wizard.IWizard;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.ImageLoader;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swtbot.swt.finder.SWTBot;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTable;
import org.junit.jupiter.api.Test;
import org.osgi.framework.FrameworkUtil;

import bndtools.central.Central;

class NewWorkspaceWizardTest {
	@Test
	void plainTextGlobMatchesSubstring() {
		Pattern pattern = Pattern.compile(globToRegex("eclipse"),
			Pattern.CASE_INSENSITIVE);

		assertThat(pattern.matcher("Eclipse Workspace").matches()).isTrue();
		assertThat(pattern.matcher("workspace").matches()).isFalse();
	}

	@Test
	void wildcardGlobMatchesExpectedText() {
		Pattern pattern = Pattern.compile(globToRegex("*eclipse?"),
			Pattern.CASE_INSENSITIVE);

		assertThat(pattern.matcher("bndtools eclipseX").matches()).isTrue();
		assertThat(pattern.matcher("bndtools eclipse").matches()).isFalse();
	}

	@Test
	void regexCharactersRemainLiteral() {
		Pattern pattern = Pattern.compile(globToRegex("a[b].c"),
			Pattern.CASE_INSENSITIVE);

		assertThat(pattern.matcher("template a[b].c").matches()).isTrue();
		assertThat(pattern.matcher("template abc").matches()).isFalse();
	}

	@Test
	void wizardTableShowsVisibleIndexNumbers() throws Exception {
		awaitWorkspace();
		Display display = Display.getDefault();
		File screenshot = new File(screenshotsDirectory(), "new-workspace-wizard.png");
		assertThat(screenshot.getParentFile().mkdirs() || screenshot.getParentFile().isDirectory()).isTrue();
		if (screenshot.exists()) {
			assertThat(screenshot.delete()).isTrue();
		}
		AtomicReference<WizardDialog> dialogReference = new AtomicReference<>();
		AtomicReference<Shell> shellReference = new AtomicReference<>();
		AtomicReference<org.eclipse.swt.widgets.Table> tableReference = new AtomicReference<>();
		display.syncExec(() -> {
			Shell shell = new Shell(display);
			IWizard wizard = newWizard();
			WizardDialog dialog = new WizardDialog(shell, wizard);
			dialog.create();
			Shell dialogShell = dialog.getShell();
			dialogShell.setLocation(100, 100);
			dialogShell.open();
			dialogReference.set(dialog);
			shellReference.set(dialogShell);
			SWTBotTable table = new SWTBot(dialogShell).table();
			tableReference.set(table.widget);
			assertThat(table.rowCount()).isGreaterThan(0);
			assertThat(table.widget.getItemCount()).isGreaterThan(0);
			assertThat(table.widget.getColumn(1).getWidth()).isGreaterThan(0);
			assertThat(table.widget.getItem(0).getData()).isNotNull();
			assertThat(table.widget.getItem(0).getText(1)).matches("\\d+");
			assertThat(table.widget.getItem(0).getText(2)).isNotBlank();
			assertThat(table.cell(0, 1)).matches("\\d+");
			assertThat(table.cell(0, 2)).isNotBlank();
		});
		try {
			display.syncExec(() -> {
				tableReference.get().redraw();
				tableReference.get().update();
			});
			new SWTBot().sleep(2_000);
			captureShell(shellReference.get(), screenshot);
			assertThat(screenshot).isNotEmpty();
		} finally {
			display.syncExec(() -> {
				dialogReference.get().close();
				shellReference.get().dispose();
			});
		}
	}

	private static void captureShell(Shell shell, File screenshot) {
		Display display = shell.getDisplay();
		display.syncExec(() -> {
			Image image = new Image(display, shell.getClientArea().width, shell.getClientArea().height);
			GC gc = new GC(shell);
			try {
				gc.copyArea(image, 0, 0);
				ImageLoader loader = new ImageLoader();
				loader.data = new ImageData[] {
					image.getImageData()
				};
				loader.save(screenshot.getPath(), SWT.IMAGE_PNG);
			} finally {
				gc.dispose();
				image.dispose();
			}
		});
	}

	private static String globToRegex(String glob) {
		try {
			Class<?> wizardClass = wizardClass();
			return (String) wizardClass.getMethod("globToRegex", String.class)
				.invoke(null, glob);
		} catch (ReflectiveOperationException e) {
			throw new AssertionError(e);
		}
	}

	private static void awaitWorkspace() throws InterruptedException {
		long deadline = System.nanoTime() + 30_000_000_000L;
		while (Central.getWorkspaceIfPresent() == null && System.nanoTime() < deadline) {
			Thread.sleep(100);
		}
		assertThat(Central.getWorkspaceIfPresent()).isNotNull();
	}

	private static IWizard newWizard() {
		try {
			return wizardClass().asSubclass(IWizard.class)
				.getConstructor()
				.newInstance();
		} catch (ReflectiveOperationException e) {
			throw new AssertionError(e);
		}
	}

	private static File screenshotsDirectory() throws URISyntaxException {
		String location = FrameworkUtil.getBundle(NewWorkspaceWizardTest.class)
			.getLocation()
			.replaceFirst("^(?:initial@)?reference:", "");
		if (location.matches("file:/[^/].*")) {
			location = "file://".concat(location.substring("file:".length()));
		}
		File jarFile = new File(new URI(location));
		return new File(jarFile.getParentFile().getParentFile(), "screenshots");
	}

	private static Class<?> wizardClass() throws ClassNotFoundException {
		return Platform.getBundle("bndtools.core")
			.loadClass(String.join("", "bndtools.wizards.new", "workspace.NewWorkspaceWizard"));
	}
}
