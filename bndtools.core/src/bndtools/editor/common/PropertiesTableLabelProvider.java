/*******************************************************************************
 * Copyright (c) 2010 Neil Bartlett.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Neil Bartlett - initial API and implementation
 ******************************************************************************/
package bndtools.editor.common;

import java.util.Map;

import org.eclipse.jface.viewers.ColumnViewer;
import org.eclipse.jface.viewers.StyledCellLabelProvider;
import org.eclipse.jface.viewers.ViewerCell;

public class PropertiesTableLabelProvider extends StyledCellLabelProvider {

    @Override
    public void update(ViewerCell cell) {
        ColumnViewer viewer = getViewer();
        @SuppressWarnings("unchecked")
        Map<String,String> map = (Map<String,String>) viewer.getInput();

        String key = (String) cell.getElement();

        if (cell.getColumnIndex() == 0) {
            cell.setText(key);
        } else if (cell.getColumnIndex() == 1) {
            cell.setText(map.get(key));
        }
    }
}