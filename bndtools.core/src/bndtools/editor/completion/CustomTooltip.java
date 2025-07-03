package bndtools.editor.completion;

import org.eclipse.jface.action.ActionContributionItem;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.jface.text.AbstractInformationControl;
import org.eclipse.jface.text.AbstractReusableInformationControlCreator;
import org.eclipse.jface.text.IInformationControl;
import org.eclipse.jface.text.IInformationControlCreator;
import org.eclipse.jface.text.IInformationControlExtension2;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.ToolBar;

import bndtools.utils.EditorUtils;

/**
 * A tooltip for hovering in source and effective view with an optional help
 * icon which can open a URL in the browser e.g. for a manual page.
 */
class CustomTooltip extends AbstractInformationControl implements IInformationControlExtension2 {


	private StyledText				label;
	private ToolBarManager		toolBarManager;
	private ActionContributionItem	helpAction;
    private String url;

	public CustomTooltip(Shell parent) {
		super(parent, true);
		create();
	}

	@Override
	protected void createContent(Composite parent) {
		parent.setLayout(new GridLayout(1, false));

		Color background = parent.getDisplay()
			.getSystemColor(SWT.COLOR_LIST_BACKGROUND);
		parent.setBackground(background);

		// StyledText (main content)
		label = new StyledText(parent, SWT.READ_ONLY | SWT.MULTI | SWT.V_SCROLL | SWT.H_SCROLL);
		label.setCaret(null);
		label.setMargins(5, 5, 5, 5);

		GridData labelData = new GridData(SWT.FILL, SWT.FILL, true, true);
		labelData.widthHint = 350;
		labelData.heightHint = 200;
		label.setLayoutData(labelData);


		helpAction = new ActionContributionItem(
			EditorUtils.createHelpButton(() -> url, () -> "Open help for more information"));

		toolBarManager = new ToolBarManager(SWT.FLAT);
		ToolBar toolBar = toolBarManager.createControl(parent);
		toolBar.setLayoutData(new GridData(SWT.END, SWT.CENTER, false, false));
		toolBarManager.update(true);

	}

    @Override
    public void setInformation(String information) {
        setInformation(information, null);
    }

    public void setInformation(String information, String url) {
        this.url = url;

		boolean showHelp = (url != null && !url.isEmpty());
		if (showHelp) {
			toolBarManager.add(helpAction);
			toolBarManager.update(true);
		} else if (!showHelp) {
			toolBarManager.remove(helpAction);
			toolBarManager.update(true);
		}

		refreshToolbar();
        label.setText(information != null ? information : "");
		label.setFocus();
    }

	private void refreshToolbar() {
		ToolBar toolBar = toolBarManager.getControl();
		if (toolBar != null && !toolBar.isDisposed()) {
			Composite parent = toolBar.getParent();
			toolBar.requestLayout();
			toolBar.redraw();
			parent.layout(true, true);
			parent.update();
		}
	}


    @Override
    public void setSizeConstraints(int width, int height) {}


    @Override
    public void setFocus() {
		if (!label.isDisposed()) {
            label.setFocus();
        }
    }

    @Override
	public boolean hasContents() {
        return label.getText() != null && !label.getText().isEmpty();
    }


    @Override
    public void setInput(Object input) {
        if (input instanceof TooltipInput) {
            TooltipInput ti = (TooltipInput) input;
            setInformation(ti.text, ti.url);
        } else if (input instanceof String) {
            setInformation((String) input, null);
        }

    }


    @Override
    public IInformationControlCreator getInformationPresenterControlCreator() {
        return new AbstractReusableInformationControlCreator() {
            @Override
            public IInformationControl doCreateInformationControl(Shell parent) {
                return new CustomTooltip(parent);
            }
        };
    }


    @Override
    public Point computeSizeConstraints(int widthInChars, int heightInChars) {
        return computeSizeHint();
    }

}
