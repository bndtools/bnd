package bndtools.launch;

import org.eclipse.jface.dialogs.PopupDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.internal.Workbench;

import aQute.bnd.build.ProjectLauncher.NotificationListener;
import aQute.bnd.build.ProjectLauncher.NotificationType;

public class LaunchNotifier implements NotificationListener {

    private final Display display = Workbench.getInstance().getDisplay();
    private Text textArea;
    private PopupDialog dialog;

    void begin() {
        display.syncExec(new Runnable() {

            @Override
            public void run() {
                dialog = new PopupDialog(new Shell(display), PopupDialog.INFOPOPUPRESIZE_SHELLSTYLE, false, true, true, true, false, "Errors in running OSGi Framework", "") {

                    @Override
                    protected Control createDialogArea(Composite parent) {
                        textArea = new Text(parent, SWT.LEAD | SWT.READ_ONLY | SWT.WRAP);
                        return textArea;
                    }

                    @Override
                    protected Point getDefaultSize() {
                        Point p = getShell().getSize();
                        p.x = Math.max(400, p.x / 2);
                        p.y = Math.max(200, p.y / 2);
                        return p;
                    }

                    @Override
                    protected Point getInitialLocation(Point initialSize) {
                        Rectangle r = getShell().getBounds();
                        return new Point(r.x + r.width - initialSize.x, r.y + r.height - initialSize.y);
                    }

                    @Override
                    public boolean close() {
                        if (textArea != null) {
                            textArea.setText("");
                        }
                        return super.close();
                    }
                };
            }
        });
    }

    @Override
    public void notify(NotificationType type, final String notification) {

        if (type == NotificationType.ERROR) {
            display.syncExec(new Runnable() {
                @Override
                public void run() {
                    dialog.open();
                    textArea.append(notification + "\n\n");
                    dialog.getShell().redraw();
                }
            });
        }
    }

    public void close() {
        display.asyncExec(new Runnable() {
            @Override
            public void run() {
                if (dialog != null && dialog.getShell() != null) {
                    dialog.getShell().dispose();
                }
            }
        });
    }
}
