package financien.rekening;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.ResultSet;
import java.sql.Statement;

import java.util.*;
import java.util.logging.*;
import java.text.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.table.*;
import javax.swing.border.*;
import javax.swing.event.*;

import financien.gui.CurrencyComboBox;
import financien.gui.RekeningHouderComboBox;
import financien.gui.RekeningTypeComboBox;
import table.*;

/**
 * Frame to show, insert and update records in the rekening table in schema financien.
 * An instance of RekeningFrame is created by class financien.Main.
 *
 * @author Chris van Engelen
 */
public class RekeningFrame {
    final private Logger logger = Logger.getLogger( RekeningFrame.class.getCanonicalName() );

    private Connection connection;

    private final JFrame frame = new JFrame( "Rekening" );

    private JTextField rekeningTextField;
    private JTextField rekeningNummerTextField;
    private RekeningTypeComboBox rekeningTypeComboBox;
    private int selectedRekeningTypeId = 0;
    private CurrencyComboBox currencyComboBox;
    private int selectedCurrencyId = 0;
    private RekeningHouderComboBox rekeningHouderComboBox;
    private int selectedRekeningHouderId;
    private boolean onlyActiveAccounts = true;

    private RekeningTableModel rekeningTableModel;
    private TableSorter rekeningTableSorter;
    private JTable rekeningTable;
    private static final DecimalFormat decimalFormat = new DecimalFormat( "#0.0000;-#" );
    private static final SimpleDateFormat dateFormat = new SimpleDateFormat( "yyyy-MM-dd" );

    public RekeningFrame( final Connection connection ) {
        this.connection = connection;

        final Container container = frame.getContentPane( );

        final ActionListener textFilterActionListener = ( ActionEvent actionEvent ) -> setupRekeningTableModel( );

        // Set grid bag layout manager
        container.setLayout( new GridBagLayout( ) );
        GridBagConstraints constraints = new GridBagConstraints( );
        constraints.gridwidth = 1;

        constraints.insets = new Insets( 20, 20, 5, 5 );
        constraints.gridx = 0;
        constraints.gridy = 0;
        constraints.anchor = GridBagConstraints.EAST;
        container.add( new JLabel( "Rekening filter:" ), constraints );

        rekeningTextField = new JTextField( 20 );
        rekeningTextField.addActionListener( textFilterActionListener );

        constraints.insets = new Insets( 20, 5, 5, 5 );
        constraints.gridx = GridBagConstraints.RELATIVE;
        constraints.anchor = GridBagConstraints.WEST;
        constraints.weightx = 1d;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        container.add( rekeningTextField, constraints );
        constraints.weightx = 0d;
        constraints.fill = GridBagConstraints.NONE;

        ActionListener rekeningSelectieActionListener = ( ActionEvent actionEvent ) -> {
            onlyActiveAccounts = actionEvent.getActionCommand( ).equals( "onlyActiveAccounts" );

            // Setup the rekening table
            setupRekeningTableModel( );
        };

        JRadioButton onlyActiveAccountsButton = new JRadioButton( "Aktieve rekeningen",
                                                                  onlyActiveAccounts );
        onlyActiveAccountsButton.setActionCommand( "onlyActiveAccounts" );
        onlyActiveAccountsButton.addActionListener( rekeningSelectieActionListener );

        JRadioButton allAccountsButton = new JRadioButton( "Alle rekeningen",
                                                           !onlyActiveAccounts );
        allAccountsButton.setActionCommand( "allAccounts" );
        allAccountsButton.addActionListener( rekeningSelectieActionListener );

        ButtonGroup rekeningSelectieButtonGroup = new ButtonGroup( );
        rekeningSelectieButtonGroup.add( onlyActiveAccountsButton );
        rekeningSelectieButtonGroup.add( allAccountsButton );

        JPanel rekeningSelectieButtonPanel = new JPanel( new GridLayout( 2, 1 ) );
        rekeningSelectieButtonPanel.add( onlyActiveAccountsButton );
        rekeningSelectieButtonPanel.add( allAccountsButton );

        constraints.insets = new Insets( 20, 5, 5, 500 );
        constraints.gridx = GridBagConstraints.RELATIVE;
        constraints.anchor = GridBagConstraints.WEST;
        container.add( rekeningSelectieButtonPanel , constraints );


        constraints.insets = new Insets( 5, 20, 5, 5 );
        constraints.gridx = 0;
        constraints.gridy = 1;
        constraints.anchor = GridBagConstraints.EAST;
        container.add( new JLabel( "Rekening nummer filter:" ), constraints );

        rekeningNummerTextField = new JTextField( 20 );
        rekeningNummerTextField.addActionListener( textFilterActionListener );

        constraints.insets = new Insets( 5, 5, 5, 500 );
        constraints.gridx = GridBagConstraints.RELATIVE;
        constraints.gridwidth = 2;
        constraints.anchor = GridBagConstraints.WEST;
        constraints.weightx = 1d;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        container.add( rekeningNummerTextField, constraints );
        constraints.gridwidth = 1;
        constraints.weightx = 0d;
        constraints.fill = GridBagConstraints.NONE;


        constraints.insets = new Insets( 5, 20, 5, 5 );
        constraints.gridx = 0;
        constraints.gridy = 2;
        constraints.anchor = GridBagConstraints.EAST;
        container.add( new JLabel( "Rekening type filter:" ), constraints );

        // Setup a JComboBox with the results of the query on rekeningType
        rekeningTypeComboBox = new RekeningTypeComboBox( connection, 0 );
        rekeningTypeComboBox.addActionListener( ( ActionEvent actionEvent ) -> {
            // Get the selected Rekening Type ID
            selectedRekeningTypeId = rekeningTypeComboBox.getSelectedRekeningTypeId( );

            // Setup the rekening table
            setupRekeningTableModel( );
        } );

        constraints.insets = new Insets( 5, 5, 5, 20 );
        constraints.gridx = GridBagConstraints.RELATIVE;
        constraints.gridwidth = 2;
        constraints.anchor = GridBagConstraints.WEST;
        container.add( rekeningTypeComboBox, constraints );
        constraints.gridwidth = 1;


        constraints.insets = new Insets( 5, 20, 5, 5 );
        constraints.gridx = 0;
        constraints.gridy = 3;
        constraints.anchor = GridBagConstraints.EAST;
        container.add( new JLabel( "Rekening currency filter:" ), constraints );

        // Setup a JComboBox with the results of the query on currency
        currencyComboBox = new CurrencyComboBox( connection, 0 );
        currencyComboBox.addActionListener( ( ActionEvent actionEvent ) -> {
            // Get the selected Rekening Type ID
            selectedCurrencyId = currencyComboBox.getSelectedCurrencyId();

            // Setup the rekening table
            setupRekeningTableModel( );
        } );

        constraints.insets = new Insets( 5, 5, 5, 20 );
        constraints.gridx = GridBagConstraints.RELATIVE;
        constraints.gridwidth = 2;
        constraints.anchor = GridBagConstraints.WEST;
        container.add( currencyComboBox, constraints );
        constraints.gridwidth = 1;


        constraints.insets = new Insets( 5, 20, 5, 5 );
        constraints.gridx = 0;
        constraints.gridy = 4;
        constraints.anchor = GridBagConstraints.EAST;
        container.add( new JLabel( "Rekeninghouder:" ), constraints );

        // Setup a JComboBox with the results of the query on currency
        rekeningHouderComboBox = new RekeningHouderComboBox( connection, 1 );
        rekeningHouderComboBox.addActionListener( ( ActionEvent actionEvent ) -> {
            // Get the selected Rekeninghouder ID
            selectedRekeningHouderId = rekeningHouderComboBox.getSelectedRekeningHouderId( );

            // Setup the rekening table
            setupRekeningTableModel( );
        } );


        constraints.insets = new Insets( 5, 5, 5, 20 );
        constraints.gridx = GridBagConstraints.RELATIVE;
        constraints.gridwidth = 2;
        constraints.anchor = GridBagConstraints.WEST;
        container.add( rekeningHouderComboBox, constraints );
        constraints.gridwidth = 1;


        // Define the edit, cancel, save and delete buttons
        // These are enabled/disabled by the table model and the list selection listener.
        final JButton editRekeningButton = new JButton( "Edit" );
        final JButton cancelRekeningButton = new JButton( "Cancel" );
        final JButton saveRekeningButton = new JButton( "Save" );
        final JButton deleteRekeningButton = new JButton( "Delete" );

        // Create rekening table from rekening table model
        rekeningTableModel = new RekeningTableModel( connection,
                                                     cancelRekeningButton,
                                                     saveRekeningButton );
        rekeningTableSorter = new TableSorter( rekeningTableModel );
        rekeningTable = new JTable( rekeningTableSorter );
        rekeningTableSorter.setTableHeader( rekeningTable.getTableHeader( ) );

        // Setup the table columns and editors
        setupRekeningTable( );

        rekeningTable.setSelectionMode( ListSelectionModel.SINGLE_SELECTION );
        rekeningTable.setAutoResizeMode( JTable.AUTO_RESIZE_OFF );

        // Set vertical size just enough for 20 entries
        rekeningTable.setPreferredScrollableViewportSize( new Dimension( 1230, 320 ) );

        // Set renderer for Double objects
        class DoubleRenderer extends JTextField implements TableCellRenderer {
            public Component getTableCellRendererComponent( JTable table,
                                                            Object object,
                                                            boolean isSelected,
                                                            boolean hasFocus,
                                                            int row, int column ) {
                this.setText( decimalFormat.format( ( ( Double )object ).doubleValue( ) ) );
                return this;
            }
        }
        DoubleRenderer doubleRenderer = new DoubleRenderer( );
        doubleRenderer.setHorizontalAlignment( JTextField.RIGHT );
        doubleRenderer.setEnabled( false );
        final Border emptyBorder = BorderFactory.createEmptyBorder( );
        doubleRenderer.setBorder( emptyBorder );
        rekeningTable.setDefaultRenderer( Double.class, doubleRenderer );


        constraints.insets = new Insets( 5, 20, 5, 20 );
        constraints.gridx = 0;
        constraints.gridy = 5;
        constraints.gridwidth = 3;
        constraints.anchor = GridBagConstraints.CENTER;

        // Setting weightx, weighty and fill is necessary for proper filling the frame when resized.
        constraints.weightx = 1d;
        constraints.weighty = 1d;
        constraints.fill = GridBagConstraints.BOTH;

        container.add( new JScrollPane( rekeningTable ), constraints );


        ////////////////////////////////////////////////
        // List selection listener
        ////////////////////////////////////////////////

        // Get the selection model related to the rekening table
        final ListSelectionModel rekeningListSelectionModel = rekeningTable.getSelectionModel( );

        class RekeningListSelectionListener implements ListSelectionListener {
            private int selectedRow = -1;

            public void valueChanged( ListSelectionEvent listSelectionEvent ) {
                // Ignore extra messages.
                if ( listSelectionEvent.getValueIsAdjusting( ) ) return;

                // Check if current row has modified values
                if ( rekeningTableModel.getRowModified( ) ) {
                    if ( selectedRow == -1 ) {
                        logger.severe( "Invalid selected row" );
                    } else {
                        int result =
                            JOptionPane.showConfirmDialog( frame,
                                                           "Data zijn gewijzigd: modificaties opslaan?",
                                                           "Record is gewijzigd",
                                                           JOptionPane.YES_NO_OPTION,
                                                           JOptionPane.QUESTION_MESSAGE,
                                                           null );

                        if ( result == JOptionPane.YES_OPTION ) {
                            // Save the changes in the table model, and in the database
                            rekeningTableModel.saveEditRow( selectedRow );
                        } else {
                            // Cancel any edits in the selected row
                            rekeningTableModel.cancelEditRow( selectedRow );
                        }
                    }
                }

                // Ignore if nothing is selected
                if ( rekeningListSelectionModel.isSelectionEmpty( ) ) {
                    selectedRow = -1;

                    editRekeningButton.setEnabled( false );
                    cancelRekeningButton.setEnabled( false );
                    saveRekeningButton.setEnabled( false );
                    deleteRekeningButton.setEnabled( false );

                    return;
                }


                // Remove the capability to edit the row
                rekeningTableModel.unsetEditRow( );

                // Get the selected row
                int viewRow = rekeningListSelectionModel.getMinSelectionIndex( );
                selectedRow = rekeningTableSorter.modelIndex( viewRow );

                // Enable the edit button
                editRekeningButton.setEnabled( true );

                // Disable the cancel and save buttons (these will be enabled
                // when any data in the row is actually modified)
                cancelRekeningButton.setEnabled( false );
                saveRekeningButton.setEnabled( false );

                // Enable the delete button
                deleteRekeningButton.setEnabled( true );
            }

            private int getSelectedRow ( ) { return selectedRow; }
        }

        // Add rekeningListSelectionListener object to the selection model of the rekening table
        final RekeningListSelectionListener rekeningListSelectionListener = new RekeningListSelectionListener( );
        rekeningListSelectionModel.addListSelectionListener( rekeningListSelectionListener );


        ////////////////////////////////////////////////
        // Insert, Edit, Cancel, Save, Delete, Close Buttons
        ////////////////////////////////////////////////

        // Class to handle button actions: uses rekeningListSelectionListener
        class ButtonActionListener implements ActionListener {
            public void actionPerformed( ActionEvent actionEvent ) {
                if ( actionEvent.getActionCommand( ).equals( "close" ) ) {
                    frame.setVisible( false );
                    frame.dispose();
                    return;
                } else if ( actionEvent.getActionCommand( ).equals( "insert" ) ) {
                    try {
                        Statement statement = connection.createStatement( );
                        ResultSet resultSet = statement.executeQuery( "SELECT MAX( rekening_id ) FROM rekening" );
                        if ( ! resultSet.next( ) ) {
                            logger.severe( "Could not get maximum for rekening_id in rekening" );
                            return;
                        }
                        final int rekeningId = resultSet.getInt( 1 ) + 1;

                        final GregorianCalendar calendar = new GregorianCalendar( );
                        final Date todayDate = calendar.getTime( );
                        final String startDatumString = dateFormat.format( todayDate );

                        String insertString =
                            "INSERT INTO rekening SET rekening_id = " + rekeningId +
                            ", aktief = 1, start_datum = '" + startDatumString + "'" +
                            ", start_saldo = 0";
                        logger.info( "insertString: " + insertString );
                        if ( statement.executeUpdate( insertString ) != 1 ) {
                            logger.severe( "Could not insert in rekening" );
                            return;
                        }
                    } catch ( SQLException ex ) {
                        logger.severe( "SQLException: " + ex.getMessage( ) );
                        return;
                    }

                    // Records may have been modified: setup the table model again
                    setupRekeningTableModel( );
                } else {
                    int selectedRow = rekeningListSelectionListener.getSelectedRow( );
                    if ( selectedRow < 0 ) {
                        JOptionPane.showMessageDialog( frame,
                                                       "Geen rekening geselecteerd",
                                                       "Rekening frame error",
                                                       JOptionPane.ERROR_MESSAGE );
                        return;
                    }
                    int rekeningId = rekeningTableModel.getRekeningId( selectedRow );
                    String rekeningString = rekeningTableModel.getRekeningString( selectedRow );

                    if ( actionEvent.getActionCommand( ).equals( "delete" ) ) {
                        // Check if rekening is used in table rekening_mutatie
                        try {
                            Statement statement = connection.createStatement( );
                            String rekeningMutatieQueryString =
                                "SELECT rekening_id FROM rekening_mutatie WHERE rekening_id = " +
                                rekeningId;
                            ResultSet resultSet = statement.executeQuery( rekeningMutatieQueryString );
                            if ( resultSet.next( ) ) {
                                JOptionPane.showMessageDialog( frame,
                                                               "Tabel rekening_mutatie gebruikt rekening " +
                                                               rekeningString,
                                                               "Rekening frame error",
                                                               JOptionPane.ERROR_MESSAGE );
                                return;
                            }
                        } catch ( SQLException sqlException ) {
                            logger.severe( "SQLException: " + sqlException.getMessage( ) );
                            return;
                        }

                        int result =
                            JOptionPane.showConfirmDialog( frame,
                                                           "Delete rekening " + rekeningString + " ?",
                                                           "Delete rekening record",
                                                           JOptionPane.YES_NO_OPTION,
                                                           JOptionPane.QUESTION_MESSAGE,
                                                           null );

                        if ( result != JOptionPane.YES_OPTION ) return;

                        String deleteString =
                            "DELETE FROM rekening WHERE rekening_id = " + rekeningId;
                        logger.info( "deleteString: " + deleteString );

                        try {
                            Statement statement = connection.createStatement( );
                            int nUpdate = statement.executeUpdate( deleteString );
                            if ( nUpdate != 1 ) {
                                String errorString = "Could not delete rekening_id " + rekeningId;
                                JOptionPane.showMessageDialog( frame,
                                                               errorString,
                                                               "Delete rekening record",
                                                               JOptionPane.ERROR_MESSAGE);
                                logger.severe( errorString );
                                return;
                            }
                        } catch ( SQLException sqlException ) {
                            logger.severe( "SQLException: " + sqlException.getMessage( ) );
                            return;
                        }

                        // Records may have been modified: setup the table model again
                        setupRekeningTableModel( );
                    } else if ( actionEvent.getActionCommand( ).equals( "edit" ) ) {
                        // Allow to edit the selected row
                        rekeningTableModel.setEditRow( selectedRow );

                        // Disable the edit button
                        editRekeningButton.setEnabled( false );
                    } else if ( actionEvent.getActionCommand( ).equals( "cancel" ) ) {
                        // Cancel any edits in the selected row
                        rekeningTableModel.cancelEditRow( selectedRow );

                        // Remove the capability to edit the row
                        rekeningTableModel.unsetEditRow( );

                        // Enable the edit button, so that the user can select edit again
                        editRekeningButton.setEnabled( true );

                        // Disable the cancel and save buttons
                        cancelRekeningButton.setEnabled( false );
                        saveRekeningButton.setEnabled( false );
                    } else if ( actionEvent.getActionCommand( ).equals( "save" ) ) {
                        // Save the changes in the table model, and in the database
                        rekeningTableModel.saveEditRow( selectedRow );

                        // Remove the capability to edit the row
                        rekeningTableModel.unsetEditRow( );

                        // Enable the edit button, so that the user can select edit again
                        editRekeningButton.setEnabled( true );

                        // Disable the cancel and save buttons
                        cancelRekeningButton.setEnabled( false );
                        saveRekeningButton.setEnabled( false );
                    }
                }
            }
        }
        final ButtonActionListener buttonActionListener = new ButtonActionListener( );

        JPanel buttonPanel = new JPanel( );

        final JButton insertRekeningButton = new JButton( "Insert" );
        insertRekeningButton.setActionCommand( "insert" );
        insertRekeningButton.addActionListener( buttonActionListener );
        buttonPanel.add( insertRekeningButton );

        editRekeningButton.setActionCommand( "edit" );
        editRekeningButton.setEnabled( false );
        editRekeningButton.addActionListener( buttonActionListener );
        buttonPanel.add( editRekeningButton );

        cancelRekeningButton.setActionCommand( "cancel" );
        cancelRekeningButton.setEnabled( false );
        cancelRekeningButton.addActionListener( buttonActionListener );
        buttonPanel.add( cancelRekeningButton );

        saveRekeningButton.setActionCommand( "save" );
        saveRekeningButton.setEnabled( false );
        saveRekeningButton.addActionListener( buttonActionListener );
        buttonPanel.add( saveRekeningButton );

        deleteRekeningButton.setActionCommand( "delete" );
        deleteRekeningButton.setEnabled( false );
        deleteRekeningButton.addActionListener( buttonActionListener );
        buttonPanel.add( deleteRekeningButton );

        final JButton closeButton = new JButton( "Close" );
        closeButton.addActionListener( buttonActionListener );
        closeButton.setActionCommand( "close" );
        buttonPanel.add( closeButton );


        constraints.gridx = 0;
        constraints.gridy = 6;
        constraints.weightx = 0d;
        constraints.weighty = 0d;
        constraints.fill = GridBagConstraints.NONE;

        container.add( buttonPanel, constraints );

        // Add a window listener to close the connection when the frame is disposed
        frame.addWindowListener( new WindowAdapter() {
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

        frame.setSize( 1290, 680 );
        frame.setDefaultCloseOperation( JFrame.DISPOSE_ON_CLOSE );
        frame.setVisible( true );
    }

    private void setupRekeningTable( ) {
        // Setup preferred column width up
        rekeningTable.getColumnModel( ).getColumn(  0 ).setPreferredWidth(  30 );  // id
        rekeningTable.getColumnModel( ).getColumn(  1 ).setPreferredWidth( 160 );  // rekening
        rekeningTable.getColumnModel( ).getColumn(  2 ).setPreferredWidth( 140 );  // nummer
        rekeningTable.getColumnModel( ).getColumn(  3 ).setPreferredWidth( 130 );  // type
        rekeningTable.getColumnModel( ).getColumn(  4 ).setPreferredWidth( 150 );  // fonds
        rekeningTable.getColumnModel( ).getColumn(  5 ).setPreferredWidth(  30 );  // currency
        rekeningTable.getColumnModel( ).getColumn(  6 ).setPreferredWidth(  20 );  // aktief
        rekeningTable.getColumnModel( ).getColumn(  7 ).setPreferredWidth(  85 );  // start datum
        rekeningTable.getColumnModel( ).getColumn(  8 ).setPreferredWidth( 100 );  // start saldo
        rekeningTable.getColumnModel( ).getColumn(  9 ).setPreferredWidth(  85 );  // laatste update
        rekeningTable.getColumnModel( ).getColumn( 10 ).setPreferredWidth( 100 );  // saldo
        rekeningTable.getColumnModel( ).getColumn( 11 ).setPreferredWidth( 100 );  // waarde
        rekeningTable.getColumnModel( ).getColumn( 12 ).setPreferredWidth( 100 );  // koers

        final DefaultCellEditor rekeningTypeDefaultCellEditor =
            new DefaultCellEditor( new RekeningTypeComboBox( connection, 0 ) );
        rekeningTable.getColumnModel( ).getColumn( 3 ).setCellEditor( rekeningTypeDefaultCellEditor );

        final DefaultCellEditor currencyDefaultCellEditor =
            new DefaultCellEditor( new CurrencyComboBox( connection, 0 ) );
        rekeningTable.getColumnModel( ).getColumn( 5 ).setCellEditor( currencyDefaultCellEditor );
    }

    private void setupRekeningTableModel( ) {
        // Setup the rekening table
        rekeningTableModel.setupRekeningTableModel( rekeningTextField.getText( ),
                                                    rekeningNummerTextField.getText( ),
                                                    selectedRekeningTypeId,
                                                    selectedCurrencyId,
                                                    selectedRekeningHouderId,
                                                    onlyActiveAccounts );
        // Setup the tableSorter
        rekeningTableSorter.setTableModel( rekeningTableModel );

        // Setup the table columns and editors
        setupRekeningTable( );
    }
}
