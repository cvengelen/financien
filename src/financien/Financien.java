package financien;

import javax.swing.*;
import java.awt.event.*;
import java.awt.*;
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

        // Set up the Load menu
        JMenu menu = new JMenu("Load");
        menu.setMnemonic(KeyEvent.VK_L);
        menuBar.add(menu);

        // Load ING mutaties
        JMenuItem menuItem = new JMenuItem("ING mutaties");
        menuItem.setActionCommand("loadIngMutaties");
        menuItem.addActionListener(this);
        menu.add(menuItem);

        // Load Rabobank mutaties
        menuItem = new JMenuItem("Rabobank mutaties");
        menuItem.setActionCommand("loadRabobankMutatie");
        menuItem.addActionListener(this);
        menu.add(menuItem);

        // Set up the Process menu
        menu = new JMenu("Process");
        menu.setMnemonic(KeyEvent.VK_P);
        menuBar.add(menu);

        // Process ING mutaties
        menuItem = new JMenuItem("ING mutaties", KeyEvent.VK_I);
        menuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_I, InputEvent.ALT_MASK));
        menuItem.setActionCommand("processIngMutaties");
        menuItem.addActionListener(this);
        menu.add(menuItem);

        // Process Rabobank mutaties
        menuItem = new JMenuItem("Rabobank mutaties", KeyEvent.VK_R);
        menuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_R, InputEvent.ALT_MASK));
        menuItem.setActionCommand("processRabobankMutaties");
        menuItem.addActionListener(this);
        menu.add(menuItem);

        // Set up the Edit menu
        menu = new JMenu("Edit");
        menu.setMnemonic(KeyEvent.VK_E);
        menuBar.add(menu);

        // Edit rekening mutaties
        menuItem = new JMenuItem("Rekening mutaties", KeyEvent.VK_M);
        menuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_M, InputEvent.ALT_MASK));
        menuItem.setActionCommand("editRekeningMutaties");
        menuItem.addActionListener(this);
        menu.add(menuItem);

        // Edit rekening mutaties rubriek
        menuItem = new JMenuItem("Rekening mutaties rubriek");
        menuItem.setActionCommand("editRekeningMutatiesRubriek");
        menuItem.addActionListener(this);
        menu.add(menuItem);

        // Edit rekening
        menuItem = new JMenuItem("Rekening");
        menuItem.setActionCommand("editRekening");
        menuItem.addActionListener(this);
        menu.add(menuItem);

        // Edit deb/cred
        menuItem = new JMenuItem("Deb/Cred", KeyEvent.VK_D);
        menuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_D, InputEvent.ALT_MASK));
        menuItem.setActionCommand("editDebCred");
        menuItem.addActionListener(this);
        menu.add(menuItem);

        // Edit rubriek
        menuItem = new JMenuItem("Rubriek");
        menuItem.setActionCommand("editRubriek");
        menuItem.addActionListener(this);
        menu.add(menuItem);

        // Edit koersen
        menuItem = new JMenuItem("Koersen", KeyEvent.VK_K);
        menuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_K, InputEvent.ALT_MASK));
        menuItem.setActionCommand("editKoersen");
        menuItem.addActionListener(this);
        menu.add(menuItem);

        // Set up the Show menu
        menu = new JMenu("Show");
        menu.setMnemonic(KeyEvent.VK_S);
        menuBar.add(menu);

        // Show rubriek totals per month
        menuItem = new JMenuItem("Rubriek total per month");
        menuItem.setActionCommand("showRubriekTotalsMonth");
        menuItem.addActionListener(this);
        menu.add(menuItem);

        // Show rubriek totals per year
        menuItem = new JMenuItem("Rubriek total per year");
        menuItem.setActionCommand("showRubriekTotalsYear");
        menuItem.addActionListener(this);
        menu.add(menuItem);

        return menuBar;
    }

    // React to menu selections.
    public void actionPerformed(ActionEvent actionEvent) {
        openFrameCount++;
        if ("loadIngMutaties".equals(actionEvent.getActionCommand())) {
            JInternalFrame loadIngMutaties = new financien.ingmutaties.LoadIngMutaties( password, xOffset * openFrameCount, yOffset * openFrameCount );
            loadIngMutaties.setVisible( true );
            desktopPane.add( loadIngMutaties );
            try {
                loadIngMutaties.setSelected( true );
            } catch ( java.beans.PropertyVetoException propertyVetoException ) {
                logger.severe( propertyVetoException.getMessage() );
            }
        } else if ("loadRabobankMutaties".equals(actionEvent.getActionCommand())) {
            JInternalFrame loadRabobankMutaties = new financien.rabobankmutaties.LoadRabobankMutaties(password, xOffset*openFrameCount, yOffset*openFrameCount);
            loadRabobankMutaties.setVisible( true );
            desktopPane.add(loadRabobankMutaties);
            try {
                loadRabobankMutaties.setSelected(true);
            } catch (java.beans.PropertyVetoException propertyVetoException) {
                logger.severe( propertyVetoException.getMessage() );
            }
        } else if ("processIngMutaties".equals(actionEvent.getActionCommand())) {
            JInternalFrame processIngMutaties = new financien.ingmutaties.ProcessIngMutaties( connection, this, xOffset * openFrameCount, yOffset * openFrameCount );
            processIngMutaties.setVisible( true );
            desktopPane.add( processIngMutaties );
            try {
                processIngMutaties.setSelected( true );
            } catch ( java.beans.PropertyVetoException propertyVetoException ) {
                logger.severe( propertyVetoException.getMessage() );
            }
        } else if ("processRabobankMutaties".equals(actionEvent.getActionCommand())) {
            JInternalFrame processRabobankMutaties = new financien.rabobankmutaties.ProcessRabobankMutaties( connection, this, xOffset * openFrameCount, yOffset * openFrameCount );
            processRabobankMutaties.setVisible( true );
            desktopPane.add( processRabobankMutaties );
            try {
                processRabobankMutaties.setSelected( true );
            } catch ( java.beans.PropertyVetoException propertyVetoException ) {
                logger.severe( propertyVetoException.getMessage() );
            }
        } else if ("editRekeningMutaties".equals(actionEvent.getActionCommand())) {
            JInternalFrame editRekeningMutaties = new financien.rekeningmutaties.EditRekeningMutaties( connection, this, xOffset * openFrameCount, yOffset * openFrameCount );
            editRekeningMutaties.setVisible( true );
            desktopPane.add( editRekeningMutaties );
            try {
                editRekeningMutaties.setSelected( true );
            } catch ( java.beans.PropertyVetoException propertyVetoException ) {
                logger.severe( propertyVetoException.getMessage() );
            }
        } else if ("editRekeningMutatiesRubriek".equals(actionEvent.getActionCommand())) {
            JInternalFrame editRekeningMutatiesRubriek = new financien.rekeningmutatiesrubriek.EditRekeningMutatiesRubriek( connection, this, xOffset * openFrameCount, yOffset * openFrameCount );
            editRekeningMutatiesRubriek.setVisible( true );
            desktopPane.add( editRekeningMutatiesRubriek );
            try {
                editRekeningMutatiesRubriek.setSelected( true );
            } catch ( java.beans.PropertyVetoException propertyVetoException ) {
                logger.severe( propertyVetoException.getMessage() );
            }
        } else if ("editRekening".equals(actionEvent.getActionCommand())) {
            JInternalFrame editRekening = new financien.rekening.EditRekening( connection, this, xOffset * openFrameCount, yOffset * openFrameCount );
            editRekening.setVisible( true );
            desktopPane.add( editRekening );
            try {
                editRekening.setSelected( true );
            } catch ( java.beans.PropertyVetoException propertyVetoException ) {
                logger.severe( propertyVetoException.getMessage() );
            }
        } else if ("editDebCred".equals(actionEvent.getActionCommand())) {
            JInternalFrame editDebCred = new financien.debcred.EditDebCred( connection, this, xOffset * openFrameCount, yOffset * openFrameCount );
            editDebCred.setVisible( true );
            desktopPane.add( editDebCred );
            try {
                editDebCred.setSelected( true );
            } catch ( java.beans.PropertyVetoException propertyVetoException ) {
                logger.severe( propertyVetoException.getMessage() );
            }
        } else if ("editRubriek".equals(actionEvent.getActionCommand())) {
            JInternalFrame editRubriek = new financien.rubriek.EditRubriek( connection, this, xOffset * openFrameCount, yOffset * openFrameCount );
            editRubriek.setVisible( true );
            desktopPane.add( editRubriek );
            try {
                editRubriek.setSelected( true );
            } catch ( java.beans.PropertyVetoException propertyVetoException ) {
                logger.severe( propertyVetoException.getMessage() );
            }
        } else if ("editKoersen".equals(actionEvent.getActionCommand())) {
            JInternalFrame editKoersen = new financien.koersen.EditKoersen( connection, this, xOffset * openFrameCount, yOffset * openFrameCount );
            editKoersen.setVisible( true );
            desktopPane.add( editKoersen );
            try {
                editKoersen.setSelected( true );
            } catch ( java.beans.PropertyVetoException propertyVetoException ) {
                logger.severe( propertyVetoException.getMessage() );
            }
        } else if ("showRubriekTotalsMonth".equals(actionEvent.getActionCommand())) {
            JInternalFrame showRubriekTotalsMonth = new financien.rubriektotalsmonth.ShowRubriekTotalsMonth( connection, this, xOffset * openFrameCount, yOffset * openFrameCount );
            showRubriekTotalsMonth.setVisible( true );
            desktopPane.add( showRubriekTotalsMonth );
            try {
                showRubriekTotalsMonth.setSelected( true );
            } catch ( java.beans.PropertyVetoException propertyVetoException ) {
                logger.severe( propertyVetoException.getMessage() );
            }
        } else if ("showRubriekTotalsYear".equals(actionEvent.getActionCommand())) {
            JInternalFrame showRubriekTotalsYear = new financien.rubriektotalsyear.ShowRubriekTotalsYear( connection, this, xOffset * openFrameCount, yOffset * openFrameCount );
            showRubriekTotalsYear.setVisible( true );
            desktopPane.add( showRubriekTotalsYear );
            try {
                showRubriekTotalsYear.setSelected( true );
            } catch ( java.beans.PropertyVetoException propertyVetoException ) {
                logger.severe( propertyVetoException.getMessage() );
            }
        }
    }

    /**
     * Create the GUI and show it.
     */
    private static void createAndShowGUI() {
        // Use the default window decorations.
        JFrame.setDefaultLookAndFeelDecorated(true);

        // Create and set up the window.
        Financien financien = new Financien();
        financien.setDefaultCloseOperation( WindowConstants.EXIT_ON_CLOSE);

        // Display the window.
        financien.setVisible(true);
    }

    public static void main(String[] args) {
        // Schedule a job for the event-dispatching thread, creating and showing this application's GUI.
        javax.swing.SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                createAndShowGUI();
            }
        });
    }
}
