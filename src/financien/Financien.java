package financien;

import javax.swing.*;
import java.awt.event.*;
import java.awt.*;
import java.lang.reflect.InvocationTargetException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.logging.Logger;

/**
 * Financien main program
 * @author Chris van Engelen
 */
public class Financien extends JFrame implements ActionListener {
    private final static Logger logger = Logger.getLogger( financien.Main.class.getCanonicalName() );

    private JDesktopPane desktopPane;
    private Connection connection;
    private String password;

    private int openFrameCount = 0;
    private static final int xOffset = 30, yOffset = 30;

    private Financien() {
        super("Financien");

        final int inset = 100;
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        setBounds(inset, inset, screenSize.width  - (3 * inset), screenSize.height - (2 * inset));

        // Set up the GUI.
        desktopPane = new JDesktopPane();
        setContentPane(desktopPane);
        setJMenuBar(createMenuBar());

        // Make dragging a little faster but perhaps uglier.
        desktopPane.setDragMode(JDesktopPane.OUTLINE_DRAG_MODE);

        try {
            // Load the MySQL JDBC driver
            Class.forName( "com.mysql.jdbc.Driver" );
        } catch ( ClassNotFoundException classNotFoundException ) {
            logger.severe( "ClassNotFoundException: " + classNotFoundException.getMessage() );
            System.exit( 1 );
        }

        try {
            // Get the password for the financien account, which gives access to schema financien.
            final financien.gui.PasswordPanel passwordPanel = new financien.gui.PasswordPanel();
            password = passwordPanel.getPassword();
            if (password == null) {
                logger.info("No password");
                System.err.println("Geen password gegeven");
                System.exit( 1 );
            }

            // Get the connection to the financien schema in the MySQL database
            connection = DriverManager.getConnection( "jdbc:mysql://localhost/financien?user=financien&password=" + password );
        } catch ( SQLException sqlException ) {
            logger.severe( "SQLException: " + sqlException.getMessage( ) );
            System.exit( 1 );
        } catch ( Exception exception ) {
            logger.severe( "Exception: " + exception.getMessage( ) );
            System.exit( 1 );
        }

        // Add a window listener to close the connection when the frame is disposed
        addWindowListener( new WindowAdapter() {
            @Override
            public void windowClosed(WindowEvent e) {
                try {
                    // Close the connection to the MySQL database
                    connection.close( );
                } catch (SQLException sqlException) {
                    logger.severe( "SQL exception closing connection: " + sqlException.getMessage() );
                }
            }
        } );
    }

    private JMenuBar createMenuBar() {
        JMenuBar menuBar = new JMenuBar();

        // Set up the ING menu
        JMenu menu = new JMenu("ING");
        menuBar.add(menu);

        // Load ING mutaties
        JMenuItem menuItem = new JMenuItem("Load ING mutaties", KeyEvent.VK_L);
        menuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_L, InputEvent.ALT_MASK));
        menuItem.setActionCommand("loadIngMutaties");
        menuItem.addActionListener(this);
        menu.add(menuItem);

        // Process ING mutaties
        menuItem = new JMenuItem("Process ING mutaties", KeyEvent.VK_P);
        menuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_P, InputEvent.ALT_MASK));
        menuItem.setActionCommand("processIngMutaties");
        menuItem.addActionListener(this);
        menu.add(menuItem);

        // Set up the Rabobank menu
        menu = new JMenu("Rabobank");
        menuBar.add(menu);

        // Load Rabobank mutaties
        menuItem = new JMenuItem("Load Rabobank mutaties");
        menuItem.setActionCommand("loadRabobankMutaties");
        menuItem.addActionListener(this);
        menu.add(menuItem);

        // Process Rabobank mutaties
        menuItem = new JMenuItem("Process Rabobank mutaties");
        menuItem.setActionCommand("processRabobankMutaties");
        menuItem.addActionListener(this);
        menu.add(menuItem);

        // Set up the Rekeningen menu
        menu = new JMenu("Edit");
        menuBar.add(menu);

        // Edit rekening mutaties
        menuItem = new JMenuItem("Edit rekening mutaties", KeyEvent.VK_R);
        menuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_R, InputEvent.ALT_MASK));
        menuItem.setActionCommand("editRekeningMutaties");
        menuItem.addActionListener(this);
        menu.add(menuItem);

        // Edit rekening
        menuItem = new JMenuItem("Edit rekening");
        menuItem.setActionCommand("editRekening");
        menuItem.addActionListener(this);
        menu.add(menuItem);

        // Edit deb/cred
        menuItem = new JMenuItem("Edit debiteur/crediteur", KeyEvent.VK_D);
        menuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_D, InputEvent.ALT_MASK));
        menuItem.setActionCommand("editDebCred");
        menuItem.addActionListener(this);
        menu.add(menuItem);

        // Edit rubriek
        menuItem = new JMenuItem("Edit rubriek");
        menuItem.setActionCommand("editRubriek");
        menuItem.addActionListener(this);
        menu.add(menuItem);

        // Edit koersen
        menuItem = new JMenuItem("Edit koersen", KeyEvent.VK_K);
        menuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_K, InputEvent.ALT_MASK));
        menuItem.setActionCommand("editKoersen");
        menuItem.addActionListener(this);
        menu.add(menuItem);

        // Set up the Show menu
        menu = new JMenu("Show");
        menu.setMnemonic(KeyEvent.VK_S);
        menuBar.add(menu);

        // Show waarde for a given date
        menuItem = new JMenuItem("Show waarde op datum", KeyEvent.VK_W);
        menuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_W, InputEvent.ALT_MASK));
        menuItem.setActionCommand("showWaardeDatum");
        menuItem.addActionListener(this);
        menu.add(menuItem);

        // Show waarde for a given rekening
        menuItem = new JMenuItem("Show waarde rekening");
        menuItem.setActionCommand("showWaardeRekening");
        menuItem.addActionListener(this);
        menu.add(menuItem);

        // Show rubriek totals per month
        menuItem = new JMenuItem("Show rubriek total per maand");
        menuItem.setActionCommand("showRubriekTotalsMonth");
        menuItem.addActionListener(this);
        menu.add(menuItem);

        // Show rubriek totals per year
        menuItem = new JMenuItem("Show rubriek total per jaar");
        menuItem.setActionCommand("showRubriekTotalsYear");
        menuItem.addActionListener(this);
        menu.add(menuItem);

        return menuBar;
    }

    // React to menu selections.
    public void actionPerformed(ActionEvent actionEvent) {
        JInternalFrame internalFrame = null;
        if ("loadIngMutaties".equals(actionEvent.getActionCommand())) {
            internalFrame = new financien.ingmutaties.LoadIngMutaties( password, xOffset * openFrameCount, yOffset * openFrameCount );
        } else if ("processIngMutaties".equals(actionEvent.getActionCommand())) {
            internalFrame = new financien.ingmutaties.ProcessIngMutaties( connection, this, xOffset * openFrameCount, yOffset * openFrameCount );
        } else if ("loadRabobankMutaties".equals(actionEvent.getActionCommand())) {
            internalFrame = new financien.rabobankmutaties.LoadRabobankMutaties(password, xOffset*openFrameCount, yOffset*openFrameCount);
        } else if ("processRabobankMutaties".equals(actionEvent.getActionCommand())) {
            internalFrame = new financien.rabobankmutaties.ProcessRabobankMutaties( connection, this, xOffset * openFrameCount, yOffset * openFrameCount );
        } else if ("editRekeningMutaties".equals(actionEvent.getActionCommand())) {
            internalFrame = new financien.rekeningmutaties.EditRekeningMutaties( connection, this, xOffset * openFrameCount, yOffset * openFrameCount );
        } else if ("editRekeningMutatiesRubriek".equals(actionEvent.getActionCommand())) {
            internalFrame = new financien.rekeningmutatiesrubriek.EditRekeningMutatiesRubriek( connection, this, xOffset * openFrameCount, yOffset * openFrameCount );
        } else if ("editRekening".equals(actionEvent.getActionCommand())) {
            internalFrame = new financien.rekening.EditRekening( connection, this, xOffset * openFrameCount, yOffset * openFrameCount );
        } else if ("editDebCred".equals(actionEvent.getActionCommand())) {
            internalFrame = new financien.debcred.EditDebCred( connection, this, xOffset * openFrameCount, yOffset * openFrameCount );
        } else if ("editRubriek".equals(actionEvent.getActionCommand())) {
            internalFrame = new financien.rubriek.EditRubriek( connection, this, xOffset * openFrameCount, yOffset * openFrameCount );
        } else if ("editKoersen".equals(actionEvent.getActionCommand())) {
            internalFrame = new financien.koersen.EditKoersen( connection, this, xOffset * openFrameCount, yOffset * openFrameCount );
        } else if ("showWaardeDatum".equals(actionEvent.getActionCommand())) {
            internalFrame = new financien.waardedatum.ShowWaardeDatum( connection, this, xOffset * openFrameCount, yOffset * openFrameCount );
        } else if ("showWaardeRekening".equals(actionEvent.getActionCommand())) {
            internalFrame = new financien.waarderekening.ShowWaardeRekening( connection, this, xOffset * openFrameCount, yOffset * openFrameCount );
        } else if ("showRubriekTotalsMonth".equals(actionEvent.getActionCommand())) {
            internalFrame = new financien.rubriektotalsmonth.ShowRubriekTotalsMonth( connection, this, xOffset * openFrameCount, yOffset * openFrameCount );
        } else if ("showRubriekTotalsYear".equals(actionEvent.getActionCommand())) {
            internalFrame = new financien.rubriektotalsyear.ShowRubriekTotalsYear( connection, this, xOffset * openFrameCount, yOffset * openFrameCount );
        } else if ("showRubriekTotalsYear".equals(actionEvent.getActionCommand())) {
            internalFrame = new financien.rubriektotalsyear.ShowRubriekTotalsYear( connection, this, xOffset * openFrameCount, yOffset * openFrameCount );
        }

        if (internalFrame == null) {
            logger.severe( "Invalid action command: " + actionEvent.getActionCommand() );
            return;
        }

        internalFrame.setVisible( true );
        desktopPane.add( internalFrame );
        try {
            internalFrame.setSelected( true );
            openFrameCount++;
        } catch ( java.beans.PropertyVetoException propertyVetoException ) {
            JOptionPane.showMessageDialog( this, propertyVetoException.getMessage( ),
                    "The internal frame could not be dusplayed",
                    JOptionPane.ERROR_MESSAGE);
            logger.severe( propertyVetoException.getMessage() );
        }
    }

    public static void main(String[] args) {
        // Schedule a job for the event-dispatching thread, creating and showing this application's GUI.
        // See: https://docs.oracle.com/javase/tutorial/uiswing/concurrency/initial.html
        // and: https://docs.oracle.com/javase/tutorial/uiswing/concurrency/dispatch.html
        try {
            javax.swing.SwingUtilities.invokeAndWait( () -> {
                // Use the default window decorations.
                JFrame.setDefaultLookAndFeelDecorated( true );

                // Create and set up the window.
                Financien financien = new Financien();
                financien.setDefaultCloseOperation( WindowConstants.EXIT_ON_CLOSE );

                // Display the window.
                financien.setVisible( true );
            } );
        }
        catch (InvocationTargetException | InterruptedException exc) {
            System.err.print("Exception: " + exc.getMessage());
            System.exit(1);
        }
    }
}
