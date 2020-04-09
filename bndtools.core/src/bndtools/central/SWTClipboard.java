package bndtools.central;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.widgets.Display;

import aQute.bnd.service.clipboard.Clipboard;

public class SWTClipboard implements Clipboard {
	private static final Transfer[] TEXT_TRANSFER = new Transfer[] {
		TextTransfer.getInstance()
	};

	@Override
	public <T> boolean copy(T content) {
		Display d = Display.getCurrent() == null ? Display.getDefault() : Display.getCurrent();
		AtomicBoolean ok = new AtomicBoolean();
		d.syncExec(() -> {
			final org.eclipse.swt.dnd.Clipboard cb = new org.eclipse.swt.dnd.Clipboard(d);
			if (content instanceof String) {
				cb.setContents(new Object[] {
					content
				}, TEXT_TRANSFER);
				ok.set(true);
			}
		});
		return ok.get();
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> Optional<T> paste(Class<T> type) {
		Display d = Display.getCurrent() == null ? Display.getDefault() : Display.getCurrent();
		AtomicReference<Optional<T>> ok = new AtomicReference<>(Optional.empty());
		d.syncExec(() -> {
			final org.eclipse.swt.dnd.Clipboard cb = new org.eclipse.swt.dnd.Clipboard(null);
			if (type == String.class) {
				String data = (String) cb.getContents(TEXT_TRANSFER[0]);
				ok.set((Optional<T>) Optional.ofNullable(data));
			}
		});
		return ok.get();
	}
}
