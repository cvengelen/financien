package financien.gui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Arrays;

/**
 * Show a dialogue for input of the password for the MySQL financien account,
 * which gives access to schema financien.
 *
 * See: https://docs.oracle.com/javase/tutorial/uiswing/components/dialog.html
 * and: https://docs.oracle.com/javase/7/docs/api/javax/swing/JOptionPane.html
 * and: http://stackoverflow.com/questions/8881213/joptionpane-to-get-password
 *
 * Created by cvengelen on 25-04-16.
 */
public class PasswordPanel extends JPanel {
    private final JPasswordField passwordField = new JPasswordField(12);
    private boolean focusRequested;
    private String password;

    public PasswordPanel() {
        super(new FlowLayout());

        add(new JLabel("Password: "));
        add(passwordField);

        // Use the password input panel in an OK/Cancel option pane
        JOptionPane passwordOptionPane = new JOptionPane(this, JOptionPane.PLAIN_MESSAGE, JOptionPane.OK_CANCEL_OPTION);

        // Create a password dialog for the password option pane
        JDialog passwordDialog = passwordOptionPane.createDialog("Financien database");

        // Use a WindowAdapter object to create a listener for when the password dialogue has gained focus
        passwordDialog.addWindowFocusListener(new WindowAdapter() {
            @Override
            public void windowGainedFocus(WindowEvent e) {
                // Let the password input panel know that the dialogue has gained focus
                gainedFocus();
            }
        });

        // Show the password dialogue and wait for user input
        passwordDialog.setVisible( true );

        // Check for the OK option
        if (passwordOptionPane.getValue() != null && passwordOptionPane.getValue().equals(JOptionPane.OK_OPTION)) {
            // Convert the char[] input to a string
            password = new String(passwordField.getPassword());
        }

        // Zero out the password field, for security.
        Arrays.fill(passwordField.getPassword(), '0');

        // Dispose of all resources used by the password dialogue
        passwordDialog.dispose();
    }

    /**
     * Hook method called when the password dialog gained focus
     */
    private void gainedFocus() {
        if (!focusRequested) {
            focusRequested = true;

            // Set the focus on the password field
            passwordField.requestFocusInWindow();
        }
    }

    /**
     * Return the password when the password dialogue is closed with the OK option, otherwise return null.
     */
    public String getPassword() { return password; }
}
