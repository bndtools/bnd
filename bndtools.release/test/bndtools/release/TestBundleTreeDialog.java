/*******************************************************************************
 * Copyright (c) 2010 Per Kr. Soreide.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Per Kr. Soreide - initial API and implementation
 *******************************************************************************/
package bndtools.release;

import java.io.File;
import java.io.IOException;
import java.util.LinkedHashSet;
import java.util.Set;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

import aQute.bnd.differ.Baseline;
import aQute.bnd.differ.Baseline.Info;
import aQute.bnd.differ.DiffPluginImpl;
import aQute.bnd.service.diff.Diff;
import aQute.bnd.service.diff.Tree;
import aQute.bnd.osgi.Jar;
import aQute.bnd.osgi.Processor;
import aQute.bnd.version.Version;
import bndtools.release.ui.BundleTree;

public class TestBundleTreeDialog {


	public static void main (String [] args) {

		Display display = new Display ();
		Shell shell = new Shell(display);

		Dialog err = new TestDialog(shell);
		//shell.open ();
		err.open();

		while (!shell.isDisposed ()) {
			if (!display.readAndDispatch ()) display.sleep ();
		}

		display.dispose ();
	}

	private static class TestDialog extends Dialog {

		DiffPluginImpl differ = new DiffPluginImpl();

		public TestDialog(Shell parentShell) {
			super(parentShell);
		}

		@Override
		protected void okPressed() {
			close();
			System.exit(0);
		}
		@Override
		protected void cancelPressed() {
			okPressed();
		}
		@Override
		protected Control createDialogArea(Composite parent) {

			Composite root = new Composite(parent, SWT.NONE);
			GridLayout gridLayout = new GridLayout();
			gridLayout.numColumns = 1;
			root.setLayout(gridLayout);

			GridData gridData = new GridData(GridData.FILL, GridData.FILL, true, true);
			gridData.minimumHeight = 800;
			gridData.minimumWidth = 600;
			root.setLayoutData(gridData);

			BundleTree release = new BundleTree(root, SWT.NONE);

			release.setInput(getBaseline());

			root.layout(true);

			return root;
		}

		private Baseline getBaseline() {

			try {
				Diff diff = getDiff();

				Diff api = diff.get("<api>");
				Diff packageDiff = api.get("aQute.bnd.annotation");

				Set<Info> infos = new LinkedHashSet<Info>();
				Info info = new Info();
				info.packageName = "biz.aQute.bnd.annotation";
				info.olderVersion = new Version(1,0);
				info.newerVersion = new Version(1,1);
				info.packageDiff = packageDiff;
				info.suggestedVersion = new Version(1,2);
				infos.add(info);

				Baseline baseline = new Baseline(new Processor(), differ);
				baseline.baseline(new Jar(new File("jar/biz.aQute.bnd.annotation-1.48.0.jar")), new Jar(new File("jar/biz.aQute.bnd.annotation-1.47.0.jar")), null);
				return baseline;
			} catch (Exception e) {
				e.printStackTrace();
			}
			return null;
		}

		private Diff getDiff() {
			try {
				Tree newer = differ.tree(new Jar(new File("jar/biz.aQute.bnd.annotation-1.48.0.jar")));
				Tree older = differ.tree(new Jar(new File("jar/biz.aQute.bnd.annotation-1.47.0.jar")));
				return newer.diff(older);
			} catch (IOException e) {
				e.printStackTrace();
			} catch (Exception e) {
				e.printStackTrace();
			}
			return null;
		}
	}
}
