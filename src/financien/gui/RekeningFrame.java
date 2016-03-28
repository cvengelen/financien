// frame to show and update records in rekening

package financien.gui;

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

import table.*;


public class RekeningFrame {
    final private Logger logger = Logger.getLogger( "financien.gui.RekeningFrame" );

    Connection connection;

    final JFrame frame = new JFrame( "Rekening" );

    JTextField rekeningTextField;
    JTextField rekeningNummerTextField;
    RekeningTypeComboBox rekeningTypeComboBox;
    int selectedRekeningTypeId = 0;
    CurrencyComboBox currencyComboBox;
    int selectedCurrencyId = 0;
    RekeningHouderComboBox rekeningHouderComboBox;
    int selectedRekeningHouderId;
    boolean onlyActiveAccounts = true;

    RekeningTableModel rekeningTableModel;
    TableSorter rekeningTableSorter;
    JTable rekeningTable;
    final DecimalFormat decimalFormat = new DecimalFormat( "#0.0000;-#" );
    final SimpleDateFormat dateFormat = new SimpleDateFormat( "yyyy-MM-dd" );

    public RekeningFrame( final Connection connection ) {
        this.connection = connection;

        class TextFilterActionListener implements ActionListener {
            public void actionPerformed( ActionEvent actionEvent ) {
                // Setup the rekening table
                setupRekeningTableModel( );
            }
        }
        TextFilterActionListener textFilterActionListener = new TextFilterActionListener( );

        final Container container = frame.getContentPane( );

        // Set grid bag layout manager
        container.setLayout( new GridBagLayout( ) );
        GridBagConstraints constraints = new GridBagConstraints( );
        constraints.anchor = GridBagConstraints.EAST;
        constraints.insets = new Insets( 5, 10, 5, 10 );
        constraints.weightx = 1;
        constraints.weighty = 0;

        constraints.gridx = 0;
        constraints.gridy = 0;
        container.add( new JLabel( "Rekening filter:" ), constraints );

        JPanel rekeningPanel = new JPanel( );

        rekeningTextField = new JTextField( 20 );
        rekeningTextField.addActionListener( textFilterActionListener );
        rekeningPanel.add( rekeningTextField );

        class RekeningSelectieActionListener implements ActionListener {
            public void actionPerformed( ActionEvent actionEvent ) {
                onlyActiveAccounts = actionEvent.getActionCommand( ).equals( "onlyActiveAccounts" );

                // Setup the rekening table
                setupRekeningTableModel( );
            }
        }
        RekeningSelectieActionListener rekeningSelectieActionListener = new RekeningSelectieActionListener( );

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

        rekeningPanel.add( rekeningSelectieButtonPanel );

        constraints.gridx = GridBagConstraints.RELATIVE;
        constraints.anchor = GridBagConstraints.WEST;
        container.add( rekeningPanel, constraints );

        constraints.gridx = 0;
        constraints.gridy = 1;
        constraints.anchor = GridBagConstraints.EAST;
        container.add( new JLabel( "Rekening nummer filter:" ), constraints );

        rekeningNummerTextField = new JTextField( 20 );
        rekeningNummerTextField.addActionListener( textFilterActionListener );
        constraints.gridx = GridBagConstraints.RELATIVE;
        constraints.anchor = GridBagConstraints.WEST;
        container.add( rekeningNummerTextField, constraints );


        constraints.gridx = 0;
        constraints.gridy = 2;
        constraints.anchor = GridBagConstraints.EAST;
        container.add( new JLabel( "Rekening type filter:" ), constraints );

        // Setup a JComboBox with the results of the query on rekeningType
        rekeningTypeComboBox = new RekeningTypeComboBox( connection, 0 );

        class RekeningTypeActionListener implements ActionListener {
            public void actionPerformed( ActionEvent actionEvent ) {
                // Get the selected Rekening Type ID
                selectedRekeningTypeId = rekeningTypeComboBox.getSelectedRekeningTypeId( );

                // Setup the rekening table
                setupRekeningTableModel( );
            }
        }
        rekeningTypeComboBox.addActionListener( new RekeningTypeActionListener( ) );

        constraints.gridx = GridBagConstraints.RELATIVE;
        constraints.anchor = GridBagConstraints.WEST;
        container.add( rekeningTypeComboBox, constraints );


        constraints.gridx = 0;
        constraints.gridy = 3;
        constraints.anchor = GridBagConstraints.EAST;
        container.add( new JLabel( "Rekening currency filter:" ), constraints );

        // Setup a JComboBox with the results of the query on currency
        currencyComboBox = new CurrencyComboBox( connection, 0 );

        class CurrencyActionListener implements ActionListener {
            public void actionPerformed( ActionEvent actionEvent ) {
                // Get the selected Rekening Type ID
                selectedCurrencyId = currencyComboBox.getSelectedCurrencyId();

                // Setup the rekening table
                setupRekeningTableModel( );
            }
        }
        currencyComboBox.addActionListener( new CurrencyActionListener( ) );

        constraints.gridx = GridBagConstraints.RELATIVE;
        constraints.anchor = GridBagConstraints.WEST;
        container.add( currencyComboBox, constraints );


        constraints.gridx = 0;
        constraints.gridy = 4;
        constraints.anchor = GridBagConstraints.EAST;
        container.add( new JLabel( "Rekeninghouder:" ), constraints );

        // Setup a JComboBox with the results of the query on currency
        rekeningHouderComboBox = new RekeningHouderComboBox( connection, 1 );

        class RekeningHouderActionListener implements ActionListener {
            public void actionPerformed( ActionEvent actionEvent ) {
                // Get the selected Rekeninghouder ID
                selectedRekeningHouderId = rekeningHouderComboBox.getSelectedRekeningHouderId( );

                // Setup the rekening table
                setupRekeningTableModel( );
            }
        }
        rekeningHouderComboBox.addActionListener( new RekeningHouderActionListener( ) );


        constraints.gridx = GridBagConstraints.RELATIVE;
        constraints.anchor = GridBagConstraints.WEST;
        container.add( rekeningHouderComboBox, constraints );


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
        rekeningTable.setPreferredScrollableViewportSize( new Dimension( 900, 320 ) );

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


        constraints.gridx = 0;
        constraints.gridy = 5;
        constraints.gridwidth = 2;
        constraints.anchor = GridBagConstraints.CENTER;
        constraints.insets = new Insets( 10, 20, 5, 20 );

        // Setting weighty and fill is necessary for proper filling the frame when resized.
        constraints.weighty = 1;
        constraints.fill = GridBagConstraints.BOTH;

        container.add( new JScrollPane( rekeningTable ), constraints );


        ////////////////////////////////////////////////
        // List selection listener
        ////////////////////////////////////////////////

        // Get the selection model related to the rekening table
        final ListSelectionModel rekeningListSelectionModel = rekeningTable.getSelectionModel( );

        class RekeningListSelectionListener implements ListSelectionListener {
            int selectedRow = -1;

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

            public int getSelectedRow ( ) { return selectedRow; }
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
                    System.exit( 0 );
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
        constraints.gridwidth = 2;
        constraints.anchor = GridBagConstraints.CENTER;
        constraints.weighty = 0;
        constraints.fill = GridBagConstraints.NONE;

        container.add( buttonPanel, constraints );

        frame.setSize( 1080, 680 );
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
