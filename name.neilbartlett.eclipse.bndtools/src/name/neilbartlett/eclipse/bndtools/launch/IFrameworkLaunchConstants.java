/*******************************************************************************
 * Copyright (c) 2010 Neil Bartlett.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Neil Bartlett - initial API and implementation
 *******************************************************************************/
package name.neilbartlett.eclipse.bndtools.launch;

import name.neilbartlett.eclipse.bndtools.Plugin;

public interface IFrameworkLaunchConstants {

	String ATTR_USE_FRAMEWORK_SPEC_LEVEL = Plugin.PLUGIN_ID + ".USE_SPEC_LEVEL";
	String ATTR_FRAMEWORK_ID = Plugin.PLUGIN_ID + ".FRAMEWORK_ID";
	String ATTR_FRAMEWORK_SPEC_LEVEL = Plugin.PLUGIN_ID + ".FRAMEWORK_SPEC_LEVEL";
	String ATTR_FRAMEWORK_INSTANCE_PATH = Plugin.PLUGIN_ID + ".FRAMEWORK_INSTANCE_PATH";

}
