/*
 -----------------------------------------------------------------------------
  (c) Copyright IBM Corp. 2003  All rights reserved.

 The sample program(s) is/are owned by International Business Machines
 Corporation or one of its subsidiaries ("IBM") and is/are copyrighted and
 licensed, not sold.

 You may copy, modify, and distribute this/these sample program(s) in any form
 without payment to IBM, for any purpose including developing, using, marketing
 or distributing programs that include or are derivative works of the sample
 program(s).

 The sample program(s) is/are provided to you on an "AS IS" basis, without
 warranty of any kind.  IBM HEREBY EXPRESSLY DISCLAIMS ALL WARRANTIES, EITHER
 EXPRESS OR IMPLIED, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF
 MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE.  Some jurisdictions do
 not allow for the exclusion or limitation of implied warranties, so the above
 limitations or exclusions may not apply to you.  IBM shall not be liable for
 any damages you suffer as a result of using, modifying or distributing the
 sample program(s) or its/their derivatives.

 Each copy of any portion of this/these sample program(s) or any derivative
 work, must include the above copyright notice and disclaimer of warranty.

 -----------------------------------------------------------------------------
*/

package swing2swt.layout;

import java.awt.Dimension;

import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Layout;

/**
 * Superclass for all the AWT layouts ported to SWT.
 * @author Yannick Saillet
 */
public abstract class AWTLayout extends Layout {

   /** 
   * Key under which an eventual preferred size (set with setPreferredSize)
   * is stored as a user data in the SWT control.
   */
  public final static String KEY_PREFERRED_SIZE = "preferredSize";

  /**
   * Gets the preferred size of a component.
   * If a preferred size has been set with setPreferredSize, returns it, 
   * otherwise returns the component computed preferred size.
   */
  protected Point getPreferredSize(
    Control control,
    int wHint,
    int hHint,
    boolean changed) {
    // check if a preferred size was set on the control with 
    // SWTComponent.setPreferredSize(Dimension)
    Dimension d = (Dimension)control.getData(KEY_PREFERRED_SIZE);
    if (d != null)
      return new Point(d.width, d.height);
    return control.computeSize(wHint, hHint, changed);
  }
}
