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

import org.eclipse.swt.*;
import org.eclipse.swt.graphics.*;
import org.eclipse.swt.widgets.*;

/**
 * Port of AWT GridLayout to SWT.
 * @author Yannick Saillet
 */
public class GridLayout extends AWTLayout {
  private int rows, columns, hgap, vgap;

  public GridLayout() {
    this(1, 0);
  }

  public GridLayout(int rows, int columns) {
    this(rows, columns, 0, 0);
  }

  public GridLayout(int rows, int columns, int hgap, int vgap) {
    if (rows == 0 && columns == 0)
      throw new IllegalArgumentException("rows and cols cannot both be zero");

    if (rows > 0 && columns > 0)
      columns = 0;

    this.rows = rows;
    this.columns = columns;
    this.hgap = hgap;
    this.vgap = vgap;
  }

  public int getColumns() {
    return columns;
  }

  public int getHgap() {
    return hgap;
  }

  public int getRows() {
    return rows;
  }

  public int getVgap() {
    return vgap;
  }

  public void setColumns(int cols) {
    if (rows == 0 && columns == 0)
      throw new IllegalArgumentException("rows and cols cannot both be zero");

    this.columns = cols;
  }

  public void setHgap(int hgap) {
    this.hgap = hgap;
  }

  public void setRows(int rows) {
    if (rows == 0 && columns == 0)
      throw new IllegalArgumentException("rows and cols cannot both be zero");

    this.rows = rows;
  }

  public void setVgap(int vgap) {
    this.vgap = vgap;
  }

  //----------

  protected Point computeSize(
    Composite composite,
    int wHint,
    int hHint,
    boolean flushCache) {
    Control[] children = composite.getChildren();
    int nbOfVisibleChildren = 0;
    for (int i = 0; i < children.length; i++) {
      //if (children[i].isVisible())
        nbOfVisibleChildren++;
    }

    if (nbOfVisibleChildren == 0)
      return new Point(0, 0);
    int r = rows;
    int c = columns;
    if (r == 0)
      r = nbOfVisibleChildren / c + ((nbOfVisibleChildren % c) == 0 ? 0 : 1);
    else if (c == 0)
      c = nbOfVisibleChildren / r + ((nbOfVisibleChildren % r) == 0 ? 0 : 1);
    int width = 0;
    int height = 0;
    for (int i = 0; i < children.length; i++) {
      //if (!children[i].isVisible())
        //continue;
      Point size =
        getPreferredSize(children[i], SWT.DEFAULT, SWT.DEFAULT, flushCache);
      if (size.x > width)
        width = size.x;
      if (size.y > height)
        height = size.y;
    }
    return new Point(c * width + (c - 1) * hgap, r * height + (r - 1) * vgap);
  }

  protected void layout(Composite composite, boolean flushCache) {
    Rectangle clientArea = composite.getClientArea();
    Control[] children = composite.getChildren();
    int nbOfVisibleChildren = 0;
    for (int i = 0; i < children.length; i++) {
      //if (children[i].isVisible())
        nbOfVisibleChildren++;
    }
    if (nbOfVisibleChildren == 0)
      return ;

    int r = rows;
    int c = columns;
    if (r == 0)
      r = nbOfVisibleChildren / c + ((nbOfVisibleChildren % c) == 0 ? 0 : 1);
    else if (c == 0)
      c = nbOfVisibleChildren / r + ((nbOfVisibleChildren % r) == 0 ? 0 : 1);
    int width = (clientArea.width - (c - 1) * hgap) / c;
    int height = (clientArea.height - (r - 1) * vgap) / r;

    int x = clientArea.x;
    int y = clientArea.y;
    for (int i = 0; i < children.length; i++) {
      //if (!children[i].isVisible())
        //continue;
      children[i].setBounds(x, y, width, height);
      if (((i + 1) % c) == 0) // if new line
        {
        x = clientArea.x;
        y += height + vgap;
      } else
        x += width + hgap;
    }
  }
}
