package bndtools.wizards.project;

import java.util.HashSet;
import java.util.Set;

import org.bndtools.api.ProjectLayout;
import org.bndtools.api.ProjectPaths;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;

public class ProjectLayoutGroup {
    private final String groupTitle;
    private ProjectLayout chosenProjectLayout = ProjectLayout.BND;
    private final Set<Button> layoutChoices = new HashSet<Button>();

    public ProjectLayoutGroup(String title) {
        this.groupTitle = title;
    }

    public Control createControl(Composite parent) {
        Group group = new Group(parent, SWT.NONE);
        group.setText(groupTitle);
        group.setLayout(new GridLayout(Math.max(4, ProjectLayout.values().length), true));

        SelectionListener radioListener = new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                for (Button button : layoutChoices) {
                    if (button.getSelection()) {
                        assert (button.getData() instanceof ProjectLayout);
                        chosenProjectLayout = (ProjectLayout) button.getData();
                        return;
                    }
                }
            }
        };

        for (ProjectLayout projectLayout : ProjectLayout.values()) {
            ProjectPaths projectPaths = ProjectPaths.get(projectLayout);
            final Button radioButton = new Button(group, SWT.RADIO);
            radioButton.setText(projectPaths.getTitle());
            radioButton.setData(projectLayout);
            radioButton.setSelection(this.chosenProjectLayout == projectLayout);
            radioButton.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
            radioButton.setToolTipText(projectPaths.getToolTip());
            radioButton.addSelectionListener(radioListener);

            layoutChoices.add(radioButton);
        }

        return group;
    }

    public ProjectLayout getProjectLayout() {
        return chosenProjectLayout;
    }
}