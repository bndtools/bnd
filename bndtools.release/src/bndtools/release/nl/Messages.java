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
package bndtools.release.nl;

import org.eclipse.osgi.util.NLS;

public class Messages extends NLS {

    private final static String RESOURCE_BUNDLE = Messages.class.getName();

    public static String releaseDialogTitle; /* bndtools.release.Activator::message */
    public static String errorExecutingStartupParticipant; /* bndtools.release.Activator::getReleaseParticipants */
    public static String loggingError; /* bndtools.release.Activator::log */
    public static String errorDialogTitle; /* bndtools.release.Activator::error */

    public static String errorDialogTitle1; /* bndtools.release.ErrorDialog::configureShell */
    public static String project; /* bndtools.release.ErrorDialog::createDialogArea */

    public static String message; /* bndtools.release.ErrorList::createControl */
    public static String symbolicName; /* bndtools.release.ErrorList::createControl */
    public static String version1; /* bndtools.release.ErrorList::createControl */

    public static String project1; /* bndtools.release.ProjectListControl::createTable */
    public static String repository; /* bndtools.release.ProjectListControl::createTable */
    public static String bundles; /* bndtools.release.ProjectListControl::createTable */

    public static String releaseJob; /* bndtools.release.ReleaseDialogJob::ReleaseDialogJob */
    public static String cleaningProject; /* bndtools.release.ReleaseDialogJob::ReleaseDialogJob */
    public static String releasing; /* bndtools.release.ReleaseDialogJob::ReleaseDialogJob */
    public static String checkingExported; /* bndtools.release.ReleaseDialogJob::ReleaseDialogJob */

    public static String fileDoesNotExist; /* bndtools.release.ReleaseHelper::writeFully */

    public static String bundleReleaseJob; /* bndtools.release.ReleaseJob::ReleaseJob */
    public static String project2; /* bndtools.release.ReleaseJob::run */
    public static String updatedVersionInfo; /* bndtools.release.ReleaseJob::run */
    public static String released; /* bndtools.release.ReleaseJob::run */
    public static String releasedTo; /* bndtools.release.ReleaseJob::run */

    public static String calculatingBuildPath; /* bndtools.release.WorkspaceAnalyserJob::getBuildOrder */
    public static String resolvingDependenciesForProject; /* bndtools.release.WorkspaceAnalyserJob::getBuildOrder */
    public static String processingProjects; /* bndtools.release.WorkspaceAnalyserJob::run */
    public static String processingProject; /* bndtools.release.WorkspaceAnalyserJob::run */
    public static String releaseWorkspaceBundles; /* bndtools.release.WorkspaceAnalyserJob::run */
    public static String noBundlesRequireRelease; /* bndtools.release.WorkspaceAnalyserJob::run */
    public static String workspaceReleaseJob; /* bndtools.release.WorkspaceAnalyserJob::run */
    public static String workspaceReleaseJob1; /* bndtools.release.WorkspaceAnalyserJob::WorkspaceAnalyserJob */

    public static String workspaceReleaseJob2; /* bndtools.release.WorkspaceReleaseJob::WorkspaceReleaseJob */
    public static String releasingProjects; /* bndtools.release.WorkspaceReleaseJob::run */

    public static String releaseDialogTitle1; /* bndtools.release.BundleReleaseDialog::configureShell */
    public static String release; /* bndtools.release.BundleReleaseDialog::createButtonsForButtonBar */
    public static String updateVersionsAndRelease; /* bndtools.release.BundleReleaseDialog::createButtonsForButtonBar */
    public static String updateVersions; /* bndtools.release.BundleReleaseDialog::createButtonsForButtonBar */
    public static String releaseToRepo; /* bndtools.release.BundleReleaseDialog::BundleReleaseDialog */
    public static String releaseOption; /* bndtools.release.BundleReleaseDialog::BundleReleaseDialog */
    public static String comboSelectText; /* bndtools.release.BundleReleaseDialog::BundleReleaseDialog */

    public static String symbNameResources; /* bndtools.release.ui.BundleTree::createBundleTreeViewer */
    public static String showAllPackages; /* bndtools.release.ui.BundleTree::createButtons */
    public static String bundleAndPackageName; /* bndtools.release.ui.BundleTree::createInfoViewer */
    public static String newVersion; /* bndtools.release.ui.BundleTree::createInfoViewer */
    public static String version2; /* bndtools.release.ui.BundleTree::createInfoViewer */
    public static String noReleaseRepos; /* bndtools.release.WorkspaceReleaseAction::run */

    public static String versionInvalid; /* bndtools.release.ui.BundleTree::saveCellEditorValue */

    public static String versionUpdateRequired; /* bndtools.release.ProjectListControl::createLegend */
    public static String releaseRequired; /* bndtools.release.ProjectListControl::createLegend */

    public static String checkAll; /* bndtools.release.ProjectListControl::createFilter */
    public static String uncheckAll; /* bndtools.release.ProjectListControl::createFilter */

    public static String releaseOptionMustBeSpecified; /* bndtools.release.ui.WorkspaceReleaseDialog::okPressed */
    public static String macrosWillBeOverwritten1; /* bndtools.release.ui.WorkspaceReleaseDialog::okPressed */
    public static String macrosWillBeOverwritten2; /* bndtools.release.ui.WorkspaceReleaseDialog::okPressed */

    static {
        NLS.initializeMessages(RESOURCE_BUNDLE, Messages.class);
    }
}
