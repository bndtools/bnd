package bndtools.ace.launch.ui;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

import bndtools.ace.launch.LaunchConstants;
import bndtools.launch.ui.AbstractLaunchTabPiece;


public class AceConfigLaunchTabPiece extends AbstractLaunchTabPiece{
    private String aceAddress;
    private Text aceAddressText;
    
    private String aceFeature;
    private Text aceFeatureText;
    
    private String aceDistribution;
    private Text aceDistributionText;
    
    private String aceTarget;
    private Text aceTargetText;
    
    public Control createControl(Composite parent) {
        Group aceGroup = new Group(parent, SWT.NONE);
        aceGroup.setText("ACE:");
        
        Label aceAddressLabel = new Label(aceGroup, SWT.NONE);
        aceAddressLabel.setText("ACE address:");
        aceAddressText = new Text(aceGroup, SWT.BORDER);
        
        aceAddressText.addModifyListener(new ModifyListener() {
            public void modifyText(ModifyEvent e) {
                setDirty(true);
                String oldAddress = aceAddress;
                aceAddress = aceAddressText.getText();
                firePropertyChange("aceAddress", oldAddress, aceAddress);
            }
        }); 
        
        Label aceFeatureLabel = new Label(aceGroup, SWT.NONE);
        aceFeatureLabel.setText("Feature name:");
        aceFeatureText = new Text(aceGroup, SWT.BORDER);
        
        aceFeatureText.addModifyListener(new ModifyListener() {
            public void modifyText(ModifyEvent e) {
                setDirty(true);
                String oldFeature = aceFeature;
                aceFeature = aceFeatureText.getText();
                firePropertyChange("aceFeature", oldFeature, aceFeature);
            }
        }); 
        
        Label aceDistributionLabel = new Label(aceGroup, SWT.NONE);
        aceDistributionLabel.setText("Distribution name:");
        aceDistributionText = new Text(aceGroup, SWT.BORDER);
        
        aceDistributionText.addModifyListener(new ModifyListener() {
            public void modifyText(ModifyEvent e) {
                setDirty(true);
                String oldDistribution = aceDistribution;
                aceDistribution = aceDistributionText.getText();
                firePropertyChange("aceDistribution", oldDistribution, aceDistribution);
            }
        }); 
        
        Label aceTargetLabel = new Label(aceGroup, SWT.NONE);
        aceTargetLabel.setText("Target name:");
        aceTargetText = new Text(aceGroup, SWT.BORDER);
        
        aceTargetText.addModifyListener(new ModifyListener() {
            public void modifyText(ModifyEvent e) {
                setDirty(true);
                String oldAceTarget = aceTarget;
                aceTarget = aceTargetText.getText();
                firePropertyChange("aceTarget", oldAceTarget, aceTarget);
            }
        }); 
        
        GridLayout layout = new GridLayout(2, false);
        aceGroup.setLayout(layout);
        aceAddressLabel.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false));
        aceAddressText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
        
        aceFeatureLabel.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false));
        aceFeatureText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
        
        aceDistributionLabel.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false));
        aceDistributionText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
        
        aceTargetLabel.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false));
        aceTargetText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
        return aceGroup;
    }

    public void setDefaults(ILaunchConfigurationWorkingCopy configuration) {
        configuration.setAttribute(LaunchConstants.ATTR_ACE_ADDRESS, LaunchConstants.DEFAULT_ACE_ADDRESS);
        configuration.setAttribute(LaunchConstants.ATTR_ACE_FEATURE, LaunchConstants.DEFAULT_ACE_FEATURE);
        configuration.setAttribute(LaunchConstants.ATTR_ACE_DISTRIBUTION, LaunchConstants.DEFAULT_ACE_DISTRIBUTION);
        configuration.setAttribute(LaunchConstants.ATTR_ACE_TARGET, LaunchConstants.DEFAULT_ACE_TARGET);
    }

    public void initializeFrom(ILaunchConfiguration configuration) throws CoreException {
        aceAddress = configuration.getAttribute(LaunchConstants.ATTR_ACE_ADDRESS, LaunchConstants.DEFAULT_ACE_ADDRESS);
        aceAddressText.setText(aceAddress);
        
        aceFeature = configuration.getAttribute(LaunchConstants.ATTR_ACE_FEATURE, LaunchConstants.DEFAULT_ACE_FEATURE);
        aceFeatureText.setText(aceFeature);
        
        aceDistribution = configuration.getAttribute(LaunchConstants.ATTR_ACE_DISTRIBUTION, LaunchConstants.DEFAULT_ACE_DISTRIBUTION);
        aceDistributionText.setText(aceDistribution);
        
        aceTarget = configuration.getAttribute(LaunchConstants.ATTR_ACE_TARGET, LaunchConstants.DEFAULT_ACE_TARGET);
        aceTargetText.setText(aceTarget);
    }

    public void performApply(ILaunchConfigurationWorkingCopy configuration) {
        configuration.setAttribute(LaunchConstants.ATTR_ACE_ADDRESS, aceAddressText.getText());
        configuration.setAttribute(LaunchConstants.ATTR_ACE_FEATURE, aceFeatureText.getText());
        configuration.setAttribute(LaunchConstants.ATTR_ACE_DISTRIBUTION, aceDistributionText.getText());
        configuration.setAttribute(LaunchConstants.ATTR_ACE_TARGET, aceTargetText.getText());
    }

}
