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

import java.util.ArrayList;
import java.util.List;

import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

import bndtools.release.api.IReleaseParticipant.Scope;
import bndtools.release.api.ReleaseContext.Error;

public class TestErrorDialog {

	
	public static void main (String [] args) {
		
		
		Display display = new Display ();
		Shell shell = new Shell(display);

		List<Error> errors = addTestErrors(); 
		
		ErrorDialog err = new TestDialog(shell, "ProjectName", errors);
		//shell.open ();
		err.open();
		
		while (!shell.isDisposed ()) {
			if (!display.readAndDispatch ()) display.sleep ();
		}
		
		display.dispose ();
	}
	
	private static List<Error> addTestErrors() {
		
		List<Error> errors = new ArrayList<Error>();

		TestError error = new TestError();
		
		error.setMessage("Simple message!");
		
		error.setScope(Scope.PRE_RELEASE);
		errors.add(error);
		
		error = new TestError();
		error.setHeaders("Col1", "Col2", "Col3");
		String[][] table = new String[5][3];
		table[0][0] = "Col1Row1";
		table[0][1] = "Col2Row1";
		table[0][2] = "Col3Row1";

		table[1][0] = "Col1Row2";
		table[1][1] = "Col2Row2";
		table[1][2] = "Col3Row2";

		table[2][0] = "Col1Row3xxxxxxx";
		table[2][1] = "Col2Row3";
		table[2][2] = "Col3Row3";

		table[3][0] = "Col1Row4";
		table[3][1] = "Col2Row4";
		table[3][2] = "Col3Row4";

		table[4][0] = "Col1Row5";
		table[4][1] = "Col2Row5";
		table[4][2] = "Col3Row5xxxxxxxxxxxxx";

		error.setList(table);
		
		error.setMessage("Message with table, symbolicName and version");
		
		error.setSymbolicName("net.comactivity.gore");
		error.setVersion("666.0.0");
		error.setScope(Scope.PRE_JAR_RELEASE);
		errors.add(error);

		error = new TestError();
		error.setHeaders("Col1", "Col2", "Col3", "Col4", "Col5");
		table = new String[5][5];
		table[0][0] = "Col1Row1";
		table[0][1] = "Col2Row1";
		table[0][2] = "Col3Row1";
		table[0][3] = "Col4Row1";
		table[0][4] = "Col5ow1";

		table[1][0] = "Col1Row2";
		table[1][1] = "Col2Row2";
		table[1][2] = "Col3Row2";
		table[1][3] = "Col4Row2";
		table[1][4] = "Col5Row2";

		table[2][0] = "Col1Row3";
		table[2][1] = "Col2Row3";
		table[2][2] = "Col3Row3";
		table[2][3] = "Col4Row3";
		table[2][4] = "Col5Row3XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX";

		table[3][0] = "Col1Row4";
		table[3][1] = "Col2Row4";
		table[3][2] = "Col3Row4";
		table[3][3] = "Col4Row4";
		table[3][4] = "Col5Row4";

		table[4][0] = "Col1Row5";
		table[4][1] = "Col2Row5";
		table[4][2] = "Col3Row5YYYYYYYYYYY";
		table[4][3] = "Col4Row5";
		table[4][4] = "Col5Row5";

		error.setList(table);
		
		error.setMessage("Another message with table, symbolicName and version");
		
		error.setSymbolicName("net.comactivity.al.gore");
		error.setVersion("77.77.77");
		error.setScope(Scope.POST_JAR_RELEASE);
		errors.add(error);
	
		
		error = new TestError();
		error.setMessage("Another simple message...");
		
		error.setScope(Scope.POST_RELEASE);
		errors.add(error);

		
		return errors;
	}
	
	private static class TestDialog extends ErrorDialog {

		public TestDialog(Shell parentShell, String name, List<Error> errors) {
			super(parentShell, name, errors);
		}

		@Override
		protected void okPressed() {
			close();
			System.exit(0);
		}

		
	}
	
	private static class TestError extends Error {
		
		public void setHeaders(String... headers) {
			super.headers = headers;
		}

		public void setList(String[][] list) {
			super.list = list;
		}
		public void setMessage(String message) {
			super.message = message;
		}
		public void setScope(Scope scope) {
			super.scope = scope;
		}

		public void setSymbolicName(String symbName) {
			super.symbName = symbName;
		}

		public void setVersion(String version) {
			super.version = version;
		}
	}
}
