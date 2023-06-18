package hsafx;

import javax.swing.JDialog;
import javax.swing.JOptionPane;

/**
 * A re-implementation of the FatalError class from the hsa console. Replaced
 * constructor with a static method. Added code to make sure popup appears on
 * top.
 *
 * @author Sam Scott
 * 
 * @version 6.0, May 24, 2018
 */
public class ErrorReporting {

    static void fatalError(String message) {
        JOptionPane optionPane = new JOptionPane(message, JOptionPane.ERROR_MESSAGE + JOptionPane.OK_OPTION);
        JDialog dialog = optionPane.createDialog("Fatal Error");
        dialog.setAlwaysOnTop(true);
        dialog.setVisible(true);
        System.exit(0);
    }
}
