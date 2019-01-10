package bndtools.bndplugins.repo.git;

import java.awt.GraphicsEnvironment;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPasswordField;
import javax.swing.JTextField;

import org.eclipse.jgit.errors.UnsupportedCredentialItem;
import org.eclipse.jgit.transport.CredentialItem;
import org.eclipse.jgit.transport.CredentialsProvider;
import org.eclipse.jgit.transport.URIish;

import bndtools.bndplugins.repo.git.GitOBRRepo.Mapping;

public class GitCredentialsProvider extends CredentialsProvider {

    private final GitOBRRepo repo;

    private static final String CRED_ITEM = "credentialItem";

    public GitCredentialsProvider(GitOBRRepo repo) {
        this.repo = repo;
    }

    @Override
    public boolean get(URIish uri, CredentialItem... items) throws UnsupportedCredentialItem {
        Mapping mapping = repo.findMapping(uri.toString());
        if (mapping != null) {
            for (CredentialItem item : items) {
                if (item instanceof CredentialItem.Username) {
                    ((CredentialItem.Username) item).setValue(mapping.user);
                    continue;
                }
                if (item instanceof CredentialItem.Password) {
                    ((CredentialItem.Password) item).setValue(mapping.pass);
                    continue;
                }
                // Usually Passphrase
                if (item instanceof CredentialItem.StringType && item.isValueSecure()) {
                    ((CredentialItem.StringType) item).setValue(new String(mapping.pass));
                    continue;
                }
            }
            return true;
        }
        if (isInteractive()) {

            JComponent[] inputs = getSwingUI(items);
            int result = JOptionPane.showConfirmDialog(null, inputs, "Enter credentials for " + repo.getName(), JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
            if (result != JOptionPane.OK_OPTION) {
                return false;
            }
            updateCredentialItems(inputs);
            return true;
        }
        return false;
    }

    @Override
    public boolean isInteractive() {
        if (repo.containsMappings()) {
            return false;
        }
        return !GraphicsEnvironment.isHeadless();
    }

    @Override
    public boolean supports(CredentialItem... items) {
        return true;
    }

    private static JComponent[] getSwingUI(CredentialItem... items) {
        List<JComponent> components = new ArrayList<JComponent>();

        for (CredentialItem item : items) {

            if (item instanceof CredentialItem.Username) {
                components.add(new JLabel(item.getPromptText()));
                JTextField field = new JTextField();
                field.putClientProperty(CRED_ITEM, item);
                components.add(field);
                continue;
            }
            if (item instanceof CredentialItem.Password) {
                components.add(new JLabel(item.getPromptText()));
                JTextField field = new JPasswordField();
                field.putClientProperty(CRED_ITEM, item);
                components.add(field);
                continue;
            }
            if (item instanceof CredentialItem.StringType) {
                components.add(new JLabel(item.getPromptText()));
                JTextField field;
                if (item.isValueSecure()) {
                    field = new JPasswordField();
                } else {
                    field = new JTextField();
                }
                field.putClientProperty(CRED_ITEM, item);
                components.add(field);
                continue;
            }
            if (item instanceof CredentialItem.InformationalMessage) {
                components.add(new JLabel(item.getPromptText()));
                continue;
            }
            if (item instanceof CredentialItem.YesNoType) {
                JCheckBox field = new JCheckBox(item.getPromptText(), ((CredentialItem.YesNoType) item).getValue());
                field.putClientProperty(CRED_ITEM, item);
                components.add(field);
                continue;
            }
        }
        return components.toArray(new JComponent[0]);
    }

    private static void updateCredentialItems(JComponent[] components) {
        for (JComponent component : components) {
            CredentialItem item = (CredentialItem) component.getClientProperty(CRED_ITEM);
            if (item == null) {
                continue;
            }
            if (item instanceof CredentialItem.Username) {
                JTextField field = (JTextField) component;
                ((CredentialItem.Username) item).setValue(field.getText());
                continue;
            }
            if (item instanceof CredentialItem.Password) {
                JPasswordField field = (JPasswordField) component;
                ((CredentialItem.Password) item).setValue(field.getPassword());
                continue;
            }
            if (item instanceof CredentialItem.StringType) {
                if (item.isValueSecure()) {
                    JPasswordField field = (JPasswordField) component;
                    ((CredentialItem.StringType) item).setValue(new String(field.getPassword()));
                    continue;
                }
                JTextField field = (JTextField) component;
                ((CredentialItem.Username) item).setValue(field.getText());
                continue;
            }
            if (item instanceof CredentialItem.YesNoType) {
                JCheckBox field = (JCheckBox) component;
                ((CredentialItem.YesNoType) item).setValue(field.isSelected());
                continue;
            }
        }
    }
}
