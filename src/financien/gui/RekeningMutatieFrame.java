// frame to show and update records in rekening_mutatie

package financien.gui;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.ResultSet;
import java.sql.Statement;

import java.util.*;
import java.util.logging.*;
import java.text.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.table.*;
import javax.swing.border.*;
import javax.swing.event.*;

import table.*;


public class RekeningMutatieFrame {
    final private Logger logger = Logger.getLogger( RekeningMutatieFrame.class.getCanonicalName( ) );

    Connection connection;

    final JFrame frame = new JFrame( "RekeningMutatie" );

    RekeningMutatieTableModel rekeningMutatieTableModel;
    TableSorter rekeningMutatieTableSorter;
    JTable rekeningMutatieTable;

    RekeningHouderComboBox rekeningHouderComboBox;
    int selectedRekeningHouderId = 1;

    RekeningComboBox rekeningComboBox;
    int selectedRekeningId = 0;
    JLabel rekeningNummerLabel;
    JLabel saldoLabel;
    JLabel datumLabel;
    final JButton updateSaldoButton = new JButton( "Update" );

    final SimpleDateFormat dateFormat = new SimpleDateFormat( "yyyy-MM-dd" );
    DecimalFormat rekeningDecimalFormat;

    int rekeningTypeId = 0;
    double startSaldo;
    String startDatumString;
    final String euroDatumString = "2002-01-01";

    RubriekComboBox rubriekComboBox;
    int selectedRubriekId = 0;
    JLabel rubriekOmschrijvingLabel;

    DebCredComboBox debCredComboBox;
    int selectedDebCredId = 0;
    JLabel debCredOmschrijvingLabel;
    String debCredFilterString = null;

    JTextField omschrijvingFilterTextField = null;

    JLabel sumMutatieSaldoLabel;
    JLabel sumMutatieInLabel;
    JLabel sumMutatieOutLabel;

    // Maximum value field rekening_type_id in table rekening_type
    final int maximumRekeningTypeId = 9;
    DecimalFormat [ ] mutatieDecimalFormat = new DecimalFormat[ maximumRekeningTypeId + 1 ];


    public RekeningMutatieFrame( final Connection connection ) {
        logger.fine( "Starting RekeningMutatieFrame" );
        this.connection = connection;

        // Get the values for rekening_pattern, used for rendering mutatieIn and mutatieUit,
        // for all records in table rekening_type and store in array indexed by rekening_type_id.
        try {
            Statement rekeningTypeStatement = connection.createStatement( );
            ResultSet rekeningTypeResultSet =
                rekeningTypeStatement.executeQuery( "SELECT rekening_type_id, rekening_pattern " +
                                                    "FROM rekening_type" );
            while ( rekeningTypeResultSet.next( ) ) {
                final int rekeningTypeId = rekeningTypeResultSet.getInt( 1 );
                mutatieDecimalFormat[ rekeningTypeId ] = new DecimalFormat( rekeningTypeResultSet.getString( 2 ) );
            }
        } catch ( SQLException sqlException ) {
            logger.severe( "SQLException in rekeningTypeStatement: " + sqlException.getMessage( ) );
        }

        final Container container = frame.getContentPane( );

        // Set grid bag layout manager
        container.setLayout( new GridBagLayout( ) );
        GridBagConstraints constraints = new GridBagConstraints( );
        constraints.anchor = GridBagConstraints.EAST;
        constraints.insets = new Insets( 5, 10, 5, 10 );
        constraints.weightx = 1.0;
        constraints.weighty = 0.0;

        constraints.gridx = 0;
        constraints.gridy = 0;
        container.add( new JLabel( "Rekening:" ), constraints );

        JPanel rekeningPanel = new JPanel( );

        // Setup a JComboBox with the results of the query on rekening houder
        rekeningHouderComboBox = new RekeningHouderComboBox( connection, 1 );
        rekeningPanel.add( rekeningHouderComboBox );

        class RekeningSelectieActionListener implements ActionListener {
            public void actionPerformed( ActionEvent actionEvent ) {
                rekeningComboBox.setupRekeningComboBox( selectedRekeningId,
                                                        selectedRekeningHouderId,
                                                        actionEvent.getActionCommand( ).equals( "onlyActiveAccounts" ) );
            }
        }
        RekeningSelectieActionListener rekeningSelectieActionListener = new RekeningSelectieActionListener( );

        final JRadioButton onlyActiveAccountsButton = new JRadioButton( "Aktieve rekeningen", true );
        onlyActiveAccountsButton.setActionCommand( "onlyActiveAccounts" );
        onlyActiveAccountsButton.addActionListener( rekeningSelectieActionListener );

        JRadioButton allAccountsButton = new JRadioButton( "Alle rekeningen" );
        allAccountsButton.setActionCommand( "allAccounts" );
        allAccountsButton.addActionListener( rekeningSelectieActionListener );

        ButtonGroup rekeningSelectieButtonGroup = new ButtonGroup( );
        rekeningSelectieButtonGroup.add( onlyActiveAccountsButton );
        rekeningSelectieButtonGroup.add( allAccountsButton );

        JPanel rekeningSelectieButtonPanel = new JPanel( new GridLayout( 2, 1 ) );
        rekeningSelectieButtonPanel.add( onlyActiveAccountsButton );
        rekeningSelectieButtonPanel.add( allAccountsButton );

        rekeningPanel.add( rekeningSelectieButtonPanel );

        class RekeningHouderActionListener implements ActionListener {
            public void actionPerformed( ActionEvent actionEvent ) {
                // Controleer of de rekeninghouder gewijzigd is
                if ( rekeningHouderComboBox.getSelectedRekeningHouderId( ) != selectedRekeningHouderId ) {
                    // Get the selected Rekeninghouder ID
                    selectedRekeningHouderId = rekeningHouderComboBox.getSelectedRekeningHouderId();

                    // Reset de geselecteerde rekening
                    selectedRekeningId = 0;

                    // Setup the rekening combobox
                    rekeningComboBox.setupRekeningComboBox( selectedRekeningId,
                                                            selectedRekeningHouderId,
                                                            onlyActiveAccountsButton.isSelected() );

                    // Setup the rekening_mutatie table for the selected debCred
                    setupRekeningMutatieTable();
                }
            }
        }
        rekeningHouderComboBox.addActionListener( new RekeningHouderActionListener() );

        // Setup a JComboBox with the results of the query on rekening
        rekeningComboBox = new RekeningComboBox( connection, selectedRekeningId,
                selectedRekeningHouderId,
                true );
        rekeningPanel.add( rekeningComboBox );

        class RekeningActionListener implements ActionListener {
            public void actionPerformed( ActionEvent actionEvent ) {
                // Get the selected Rekening ID
                selectedRekeningId = rekeningComboBox.getSelectedRekeningId( );

                // Check if rekening has been selected
                if ( selectedRekeningId == 0 ) {
                    rekeningNummerLabel.setText( "" );
                    saldoLabel.setText( "" );
                    datumLabel.setText( "" );
                    updateSaldoButton.setEnabled( false );
                } else {
                    try {
                        Statement statement = connection.createStatement( );
                        ResultSet resultSet =
                            statement.executeQuery( "SELECT nummer, type_id, saldo, datum, " +
                                                    "start_saldo, start_datum, rekening_pattern " +
                                                    "FROM rekening " +
                                                    "LEFT JOIN rekening_type ON rekening.type_id = rekening_type.rekening_type_id " +
                                                    "WHERE rekening_id = " +
                                                    selectedRekeningId );
                        if ( ! resultSet.next( ) ) {
                            logger.severe( "Could not get record for rekening_id " +
                                           selectedRekeningId + " in rekening" );
                            return;
                        }
                        rekeningNummerLabel.setText( resultSet.getString( 1 ) );

                        rekeningTypeId = resultSet.getInt( 2 );

                        // Save the output pattern for this rekening
                        rekeningDecimalFormat = new DecimalFormat( resultSet.getString( 7 ) );

                        // Output the saldo using the output pattern
                        saldoLabel.setText( rekeningDecimalFormat.format( resultSet.getDouble( 3 ) ) );

                        datumLabel.setText( resultSet.getString( 4 ) );

                        startSaldo = resultSet.getDouble( 5 );
                        startDatumString = resultSet.getString( 6 );

                        // Enable the button to update the saldo
                        updateSaldoButton.setEnabled( true );
                    } catch ( SQLException sqlException ) {
                        logger.severe( "SQLException: " + sqlException.getMessage( ) );
                    }
                }

                // Setup the rekening_mutatie table
                setupRekeningMutatieTable( );
            }
        }
        rekeningComboBox.addActionListener( new RekeningActionListener( ) );

        rekeningNummerLabel = new JLabel( );
        rekeningPanel.add( rekeningNummerLabel );

        constraints.gridx = GridBagConstraints.RELATIVE;
        constraints.anchor = GridBagConstraints.WEST;
        container.add( rekeningPanel, constraints );

        constraints.gridx = 0;
        constraints.gridy = 1;
        constraints.anchor = GridBagConstraints.EAST;
        container.add( new JLabel( "Saldo: " ), constraints );

        JPanel saldoPanel = new JPanel( );

        saldoLabel = new JLabel( );
        constraints.gridx = GridBagConstraints.RELATIVE;
        constraints.anchor = GridBagConstraints.WEST;
        saldoPanel.add( saldoLabel );

        datumLabel = new JLabel( );
        saldoPanel.add( datumLabel );

        class UpdateRekeningActionListener implements ActionListener {
            public void actionPerformed( ActionEvent actionEvent ) {
                updateSaldo( );
            }
        }
        updateSaldoButton.addActionListener( new UpdateRekeningActionListener( ) );
        updateSaldoButton.setEnabled( false );
        saldoPanel.add( updateSaldoButton );

        constraints.gridx = GridBagConstraints.RELATIVE;
        container.add( saldoPanel, constraints );


        constraints.gridx = 0;
        constraints.gridy = 2;
        constraints.anchor = GridBagConstraints.EAST;
        container.add( new JLabel( "Rubriek:" ), constraints );

        JPanel rubriekPanel = new JPanel( );

        // Setup a JComboBox with the results of the query on rubriek
        rubriekComboBox = new RubriekComboBox( connection, 0, true );
        rubriekPanel.add( rubriekComboBox );

        class RubriekActionListener implements ActionListener {
            public void actionPerformed( ActionEvent actionEvent ) {
                // Get the selected Rubriek ID
                selectedRubriekId = rubriekComboBox.getSelectedRubriekId( );

                // Check if rubriek has been selected
                if ( selectedRubriekId == 0 ) {
                    rubriekOmschrijvingLabel.setText( "" );
                } else {
                    try {
                        Statement statement = connection.createStatement( );
                        ResultSet resultSet = statement.executeQuery( "SELECT omschrijving " +
                                                                      "FROM rubriek WHERE rubriek_id = " +
                                                                      selectedRubriekId );
                        if ( ! resultSet.next( ) ) {
                            logger.severe( "Could not get record for rubriek_id " +
                                           selectedRubriekId + " in rubriek" );
                            return;
                        }
                        rubriekOmschrijvingLabel.setText( resultSet.getString( 1 ) );
                    } catch ( SQLException sqlException ) {
                        logger.severe( "SQLException: " + sqlException.getMessage( ) );
                    }
                }

                // Setup the rekening_mutatie table for the selected rubriek
                setupRekeningMutatieTable( );
            }
        }
        rubriekComboBox.addActionListener( new RubriekActionListener( ) );

        rubriekOmschrijvingLabel = new JLabel( );
        rubriekPanel.add( rubriekOmschrijvingLabel );

        constraints.gridx = GridBagConstraints.RELATIVE;
        constraints.anchor = GridBagConstraints.WEST;
        container.add( rubriekPanel, constraints );

        constraints.gridx = 0;
        constraints.gridy = 3;
        constraints.anchor = GridBagConstraints.EAST;
        container.add( new JLabel( "DebCred:" ), constraints );

        JPanel debCredPanel = new JPanel( );

        // Setup a JComboBox with the results of the query on debCred
        debCredComboBox = new DebCredComboBox( connection, selectedDebCredId, "", true );
        debCredPanel.add( debCredComboBox );

        class DebCredActionListener implements ActionListener {
            public void actionPerformed( ActionEvent actionEvent ) {
                // Get the selected DebCred ID
                selectedDebCredId = debCredComboBox.getSelectedDebCredId( );

                // Check if debCred has been selected
                if ( selectedDebCredId == 0 ) {
                    debCredOmschrijvingLabel.setText( "" );
                } else {
                    try {
                        Statement statement = connection.createStatement( );
                        ResultSet resultSet = statement.executeQuery( "SELECT omschrijving " +
                                                                      "FROM deb_cred WHERE deb_cred_id = " +
                                                                      selectedDebCredId );
                        if ( ! resultSet.next( ) ) {
                            logger.severe( "Could not get record for deb_cred_id " +
                                           selectedDebCredId + " in deb_cred" );
                            return;
                        }
                        debCredOmschrijvingLabel.setText( resultSet.getString( 1 ) );
                    } catch ( SQLException sqlException ) {
                        logger.severe( "SQLException: " + sqlException.getMessage( ) );
                    }
                }

                // Setup the rekening_mutatie table for the selected debCred
                setupRekeningMutatieTable( );
            }
        }
        debCredComboBox.addActionListener( new DebCredActionListener( ) );

        debCredOmschrijvingLabel = new JLabel( );
        debCredPanel.add( debCredOmschrijvingLabel );

        // Class to handle Deb/Cred filter button
       class DebCredFilterButtonActionListener implements ActionListener {
           public void actionPerformed( ActionEvent actionEvent ) {
                if ( actionEvent.getActionCommand( ).equals( "filter" ) ) {
                    String newDebCredFilterString =
                        ( String )JOptionPane.showInputDialog( frame,
                                                               "Deb/Cred filter:",
                                                               "Deb/Cred filter dialog",
                                                               JOptionPane.QUESTION_MESSAGE,
                                                               null,
                                                               null,
                                                               debCredFilterString );

                    // Check if dialog was completed successfully (i.e., not canceled)
                    if ( newDebCredFilterString != null ) {
                        // Store the new Deb/Cred filter
                        debCredFilterString = newDebCredFilterString;

                        // Setup the debCred combo box with the Deb/Cred filter
                        // Reset the selected Deb/Cred ID in order to avoid immediate selection
                        debCredComboBox.setupDebCredComboBox( 0, "", debCredFilterString, true );
                    }
                }
            }
        }
        final DebCredFilterButtonActionListener debCredFilterButtonActionListener = new DebCredFilterButtonActionListener( );

        final JButton debCredFilterButton = new JButton( "Filter Deb/Cred" );
        debCredFilterButton.setActionCommand( "filter" );
        debCredFilterButton.addActionListener( debCredFilterButtonActionListener );
        debCredPanel.add( debCredFilterButton );

        constraints.gridx = GridBagConstraints.RELATIVE;
        constraints.anchor = GridBagConstraints.WEST;
        container.add( debCredPanel, constraints );


        ////////////////////////
        // Omschrijving filter
        ///////////////////////

        constraints.gridx = 0;
        constraints.gridy = 4;
        constraints.weightx = 1;
        constraints.anchor = GridBagConstraints.EAST;
        container.add( new JLabel( "Omschrijving:" ), constraints );
        omschrijvingFilterTextField = new JTextField( 50 );
        JPanel omschrijvingPanel = new JPanel( );
        omschrijvingPanel.add( omschrijvingFilterTextField );

        constraints.gridx = GridBagConstraints.RELATIVE;
        constraints.anchor = GridBagConstraints.WEST;
        // constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.weightx = 1;
        container.add( omschrijvingPanel, constraints );

        class OmschrijvingFilterActionListener implements ActionListener {
            public void actionPerformed( ActionEvent actionEvent ) {
                // Setup the rekening_mutatie table
                setupRekeningMutatieTable( );
            }
        }
        omschrijvingFilterTextField.addActionListener( new OmschrijvingFilterActionListener( ) );


        constraints.gridx = 0;
        constraints.gridy = 5;
        constraints.weightx = 1;
        constraints.anchor = GridBagConstraints.EAST;
        container.add( new JLabel( "Saldo in/uit:" ), constraints );

        sumMutatieSaldoLabel = new JLabel( "saldo" );
        sumMutatieInLabel = new JLabel( "in" );
        sumMutatieOutLabel = new JLabel( "uit" );
        JPanel sumMutatiePanel = new JPanel( );
        sumMutatiePanel.add( sumMutatieInLabel );
        sumMutatiePanel.add( sumMutatieOutLabel );
        sumMutatiePanel.add( sumMutatieSaldoLabel );

        constraints.gridx = GridBagConstraints.RELATIVE;
        constraints.anchor = GridBagConstraints.WEST;
        // constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.weightx = 1;
        container.add( sumMutatiePanel, constraints );


        // Define the edit, cancel, save and delete buttons because
        // these are enabled/disabled by the list selection listener.
        final JButton editMutatieButton = new JButton( "Edit" );
        final JButton cancelMutatieButton = new JButton( "Cancel" );
        final JButton saveMutatieButton = new JButton( "Save" );
        final JButton deleteMutatieButton = new JButton( "Delete" );

        // Create rekening_mutatie table from rekening_mutatie table model
        rekeningMutatieTableModel = new RekeningMutatieTableModel( connection,
                                                                   cancelMutatieButton,
                                                                   saveMutatieButton );
        rekeningMutatieTableSorter = new TableSorter( rekeningMutatieTableModel );
        rekeningMutatieTable = new JTable( rekeningMutatieTableSorter );
        rekeningMutatieTableSorter.setTableHeader( rekeningMutatieTable.getTableHeader( ) );
        // rekeningMutatieTableSorter.setSortingStatus( 0, TableSorter.DESCENDING );

        // Initialize the table and the columns width
        setupRekeningMutatieTable( );

        rekeningMutatieTable.setSelectionMode( ListSelectionModel.SINGLE_SELECTION );
        rekeningMutatieTable.setAutoResizeMode( JTable.AUTO_RESIZE_OFF );

        // Set vertical size just enough for 20 entries
        rekeningMutatieTable.setPreferredScrollableViewportSize( new Dimension( 900, 320 ) );

        // Set renderer for Double objects
        class DoubleRenderer extends JTextField implements TableCellRenderer {
            public Component getTableCellRendererComponent( JTable table,
                                                            Object object,
                                                            boolean isSelected,
                                                            boolean hasFocus,
                                                            int row, int column ) {
                switch ( column ) {
                case 4:		// MutatieIn
                case 5:		// MutatieUit
                    final double mutatie = ( ( Double )object ).doubleValue( );
                    if ( mutatie == 0 ) {
                        // Return empty string
                        this.setText( "" );
                    } else {
                        // Get the rekening type id of this row from the table
                        final int rekeningTypeId = rekeningMutatieTableModel.getRekeningTypeId( row );

                        // Use the formatter defined for this rekening_type in table rekening_type
                        this.setText( mutatieDecimalFormat[ rekeningTypeId ].format( mutatie ) );
                    }
                    break;

                case 10:	// Inleg aandelen
                    final double inlegAandelen = ( ( Double )object ).doubleValue( );
                    if ( inlegAandelen == 0 ) {
                        // Return empty string
                        this.setText( "" );
                    } else {
                        // Use the formatter user for normal accounts
                        this.setText( mutatieDecimalFormat[ 1 ].format( inlegAandelen ) );
                    }
                    break;

                default:	// Unexpected column: just return the string related to the object
                    logger.severe( "Unexpected column: " + column );
                    this.setText( object.toString( ) );
                }

                return this;
            }
        }
        DoubleRenderer doubleRenderer = new DoubleRenderer( );
        doubleRenderer.setHorizontalAlignment( JTextField.RIGHT );
        doubleRenderer.setEnabled( false );
        final Border emptyBorder = BorderFactory.createEmptyBorder( );
        doubleRenderer.setBorder( emptyBorder );
        rekeningMutatieTable.setDefaultRenderer( Double.class, doubleRenderer );


        constraints.gridx = 0;
        constraints.gridy = 6;
        constraints.gridwidth = 2;
        constraints.anchor = GridBagConstraints.CENTER;

        // Setting weighty and fill is necessary for proper filling the frame when resized.
        constraints.weighty = 1.0;
        constraints.fill = GridBagConstraints.BOTH;

        container.add( new JScrollPane( rekeningMutatieTable ), constraints );



        ////////////////////////////////////////////////
        // List selection listener
        ////////////////////////////////////////////////

        // Get the selection model related to the rekening_mutatie table
        final ListSelectionModel mutatieListSelectionModel = rekeningMutatieTable.getSelectionModel( );

        class MutatieListSelectionListener implements ListSelectionListener {
            int selectedRow = -1;

            public void valueChanged( ListSelectionEvent listSelectionEvent ) {
                // Ignore extra messages.
                if ( listSelectionEvent.getValueIsAdjusting( ) ) return;

                // Check if current row has modified values
                if ( rekeningMutatieTableModel.getRowModified( ) ) {
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
                            if ( !( rekeningMutatieTableModel.saveEditRow( selectedRow ) ) ) {
                                JOptionPane.showMessageDialog( frame,
                                                               "Error: row not saved",
                                                               "Save rekening-mutatie record error",
                                                               JOptionPane.ERROR_MESSAGE );
                            }
                        } else {
                            // Cancel any edits in the selected row
                            rekeningMutatieTableModel.cancelEditRow( selectedRow );
                        }
                    }
                }

                // Ignore if nothing is selected
                if ( mutatieListSelectionModel.isSelectionEmpty( ) ) {
                    selectedRow = -1;
                    deleteMutatieButton.setEnabled( false );
                    editMutatieButton.setEnabled( false );
                    cancelMutatieButton.setEnabled( false );
                    saveMutatieButton.setEnabled( false );
                    return;
                }

                // Remove the capability to edit the row
                rekeningMutatieTableModel.unsetEditRow( );

                // Get the selected row
                int viewRow = mutatieListSelectionModel.getMinSelectionIndex( );
                selectedRow = rekeningMutatieTableSorter.modelIndex( viewRow );
                // logger.info( "viewRow: " + viewRow + ", selectedRow: " + selectedRow );

                // Enable the edit button
                editMutatieButton.setEnabled( true );

                // Disable the cancel and save buttons (these will be enabled
                // when any data in the row is actually modified)
                cancelMutatieButton.setEnabled( false );
                saveMutatieButton.setEnabled( false );

                // Enable the delete button
                deleteMutatieButton.setEnabled( true );
            }

            public int getSelectedRow ( ) { return selectedRow; }
        }

        // Add mutatieListSelectionListener object to the selection model of the rekening mutatie table
        final MutatieListSelectionListener mutatieListSelectionListener = new MutatieListSelectionListener( );
        mutatieListSelectionModel.addListSelectionListener( mutatieListSelectionListener );


        rekeningMutatieTable.addMouseListener( new MouseListener ( ) {
            public void mouseClicked( MouseEvent mouseEvent ) {
                logger.info( "Mouse clicked: " + mouseEvent.getClickCount( ) );
                if ( mouseEvent.getClickCount( ) == 2 ) {
                    // Allow to edit the selected row
                    rekeningMutatieTableModel.setEditRow( mutatieListSelectionListener.getSelectedRow( ) );
                    // Disable the edit button
                    editMutatieButton.setEnabled( false );
                }
            }
            public void mouseEntered( MouseEvent mouseEvent ) {
            }
            public void mouseExited( MouseEvent mouseEvent ) {
            }
            public void mousePressed( MouseEvent mouseEvent ) {
            }
            public void mouseReleased( MouseEvent mouseEvent ) {
            }
        });


        ////////////////////////////////////////////////
        // Edit, cancel, save, insert, delete, close Buttons
        ////////////////////////////////////////////////

        // Class to handle button actions: uses mutatieListSelectionListener
        class ButtonActionListener implements ActionListener {
            public void actionPerformed( ActionEvent actionEvent ) {
                if ( actionEvent.getActionCommand( ).equals( "close" ) ) {
                    frame.setVisible( false );
                    System.exit( 0 );
                } else if ( actionEvent.getActionCommand( ).equals( "insert" ) ) {
                    RekeningMutatieDialog rekeningMutatieDialog =
                        new RekeningMutatieDialog( connection,
                                                   frame,
                                                   selectedRubriekId,
                                                   0,
                                                   selectedRekeningHouderId );

                    // Records may have been modified: setup the table model again
                    setupRekeningMutatieTable( );
                } else {
                    int selectedRow = mutatieListSelectionListener.getSelectedRow( );
                    if ( selectedRow < 0 ) {
                        JOptionPane.showMessageDialog( frame,
                                                       "Geen mutatie geselecteerd",
                                                       "Rekening-mutatie frame error",
                                                       JOptionPane.ERROR_MESSAGE );
                        return;
                    }

                    if ( actionEvent.getActionCommand( ).equals( "delete" ) ) {
                        final String datumString = rekeningMutatieTableModel.getDatumString( selectedRow );
                        int result =
                            JOptionPane.showConfirmDialog( frame,
                                                           "Delete record for rekening " +
                                                           rekeningMutatieTableModel.getRekeningString( selectedRow ) +
                                                           " at date " + datumString +
                                                           " in rekening_mutatie ?",
                                                           "Delete rekening_mutatie record",
                                                           JOptionPane.YES_NO_OPTION,
                                                           JOptionPane.QUESTION_MESSAGE,
                                                           null );

                        if ( result != JOptionPane.YES_OPTION ) return;

                        final int rubriekId = rekeningMutatieTableModel.getRubriekId( selectedRow );
                        final int debCredId = rekeningMutatieTableModel.getDebCredId( selectedRow );
                        final int rekeningId = rekeningMutatieTableModel.getRekeningId( selectedRow );
                        final int volgNummer = rekeningMutatieTableModel.getVolgNummer( selectedRow );

                        String deleteString  = "DELETE FROM rekening_mutatie";
                        deleteString += " WHERE rubriek_id = " + rubriekId;
                        deleteString += " AND datum = '" + datumString + "'";
                        deleteString += " AND deb_cred_id = " + debCredId;
                        deleteString += " AND rekening_id = " + rekeningId;
                        deleteString += " AND volgnummer = " + volgNummer;

                        logger.fine( "deleteString: " + deleteString );

                        try {
                            Statement statement = connection.createStatement( );
                            int nUpdate = statement.executeUpdate( deleteString );
                            if ( nUpdate != 1 ) {
                                String errorString = ( "Could not delete record with\n" +
                                                       "rubriek_id  = " + rubriekId + "\n" +
                                                       "datum       = " + datumString + "\n" +
                                                       "deb_cred_id = " + debCredId + "\n" +
                                                       "rekening_id = " + rekeningId + "\n" +
                                                       "volgnummer = "  + volgNummer + "\n" +
                                                       "in rekening_mutatie" );
                                JOptionPane.showMessageDialog( frame,
                                                               errorString,
                                                               "Delete rekening_mutatie record",
                                                               JOptionPane.ERROR_MESSAGE);
                                logger.severe( errorString );
                                return;
                            }
                        } catch ( SQLException sqlException ) {
                            logger.severe( "SQLException: " + sqlException.getMessage( ) );
                            return;
                        }

                        // Records may have been modified: setup the table model again
                        setupRekeningMutatieTable( );
                    } else if ( actionEvent.getActionCommand( ).equals( "edit" ) ) {
                        // Allow to edit the selected row
                        rekeningMutatieTableModel.setEditRow( selectedRow );

                        // Disable the edit button
                        editMutatieButton.setEnabled( false );
                    } else if ( actionEvent.getActionCommand( ).equals( "cancel" ) ) {
                        // Cancel any edits in the selected row
                        rekeningMutatieTableModel.cancelEditRow( selectedRow );

                        // Remove the capability to edit the row
                        rekeningMutatieTableModel.unsetEditRow( );

                        // Enable the edit button, so that the user can select edit again
                        editMutatieButton.setEnabled( true );

                        // Disable the cancel and save buttons
                        cancelMutatieButton.setEnabled( false );
                        saveMutatieButton.setEnabled( false );
                    } else if ( actionEvent.getActionCommand( ).equals( "save" ) ) {
                        // Save the changes in the table model, and in the database
                        if ( !( rekeningMutatieTableModel.saveEditRow( selectedRow ) ) ) {
                            JOptionPane.showMessageDialog( frame,
                                                           "Error: row not saved",
                                                           "Save rekening-mutatie record error",
                                                           JOptionPane.ERROR_MESSAGE );
                            return;
                        }

                        // Remove the capability to edit the row
                        rekeningMutatieTableModel.unsetEditRow( );

                        // Enable the edit button, so that the user can select edit again
                        editMutatieButton.setEnabled( true );

                        // Disable the cancel and save buttons
                        cancelMutatieButton.setEnabled( false );
                        saveMutatieButton.setEnabled( false );
                    }
                }
            }
        }
        final ButtonActionListener buttonActionListener = new ButtonActionListener( );

        JPanel buttonPanel = new JPanel( );

        final JButton insertMutatieButton = new JButton( "Insert" );
        insertMutatieButton.setActionCommand( "insert" );
        insertMutatieButton.addActionListener( buttonActionListener );
        buttonPanel.add( insertMutatieButton );

        editMutatieButton.setActionCommand( "edit" );
        editMutatieButton.setEnabled( false );
        editMutatieButton.addActionListener( buttonActionListener );
        buttonPanel.add( editMutatieButton );

        cancelMutatieButton.setActionCommand( "cancel" );
        cancelMutatieButton.setEnabled( false );
        cancelMutatieButton.addActionListener( buttonActionListener );
        buttonPanel.add( cancelMutatieButton );

        saveMutatieButton.setActionCommand( "save" );
        saveMutatieButton.setEnabled( false );
        saveMutatieButton.addActionListener( buttonActionListener );
        buttonPanel.add( saveMutatieButton );

        deleteMutatieButton.setActionCommand( "delete" );
        deleteMutatieButton.setEnabled( false );
        deleteMutatieButton.addActionListener( buttonActionListener );
        buttonPanel.add( deleteMutatieButton );

        final JButton closeButton = new JButton( "Close" );
        closeButton.addActionListener( buttonActionListener );
        closeButton.setActionCommand( "close" );
        buttonPanel.add( closeButton );


        constraints.gridx = 0;
        constraints.gridy = 7;

        constraints.weighty = 0.0;
        constraints.fill = GridBagConstraints.NONE;

        constraints.gridwidth = 6;
        constraints.anchor = GridBagConstraints.CENTER;
        container.add( buttonPanel, constraints );

        frame.setSize( 980, 750 );
        frame.setDefaultCloseOperation( JFrame.DISPOSE_ON_CLOSE );
        frame.setVisible( true );
    }


    private void setupRekeningMutatieTable( ) {
        // Setup the rekening_mutatie table
        rekeningMutatieTableModel.setupRekeningMutatieTableModel( selectedRekeningHouderId,
                                                                  selectedRekeningId,
                                                                  selectedRubriekId,
                                                                  selectedDebCredId,
                                                                  omschrijvingFilterTextField.getText( ) );

        // Setup the tableSorter again so that the TableSorter gets the new table size (# rows)
        rekeningMutatieTableSorter.setTableModel( rekeningMutatieTableModel );

        // Need to setup preferred column width up again
        rekeningMutatieTable.getColumnModel( ).getColumn(  0 ).setPreferredWidth( 100 );  // Datum
        rekeningMutatieTable.getColumnModel( ).getColumn(  1 ).setPreferredWidth( 150 );  // Rekening
        rekeningMutatieTable.getColumnModel( ).getColumn(  2 ).setPreferredWidth( 150 );  // Rubriek
        rekeningMutatieTable.getColumnModel( ).getColumn(  3 ).setPreferredWidth( 150 );  // Deb/Cred
        rekeningMutatieTable.getColumnModel( ).getColumn(  4 ).setPreferredWidth( 100 );  // In
        rekeningMutatieTable.getColumnModel( ).getColumn(  5 ).setPreferredWidth( 100 );  // Uit
        rekeningMutatieTable.getColumnModel( ).getColumn(  6 ).setPreferredWidth( 40 );   // VolgNummer
        rekeningMutatieTable.getColumnModel( ).getColumn(  7 ).setPreferredWidth( 40 );   // Jaar
        rekeningMutatieTable.getColumnModel( ).getColumn(  8 ).setPreferredWidth( 40 );   // Maand
        rekeningMutatieTable.getColumnModel( ).getColumn(  9 ).setPreferredWidth( 600 );  // Omschrijving
        rekeningMutatieTable.getColumnModel( ).getColumn( 10 ).setPreferredWidth( 100 );  // Inleg Aandelen

        final DefaultCellEditor rekeningDefaultCellEditor =
            new DefaultCellEditor( new RekeningComboBox( connection, 0, selectedRekeningHouderId, true ) );
        rekeningMutatieTable.getColumnModel( ).getColumn( 1 ).setCellEditor( rekeningDefaultCellEditor );

        final DefaultCellEditor rubriekDefaultCellEditor =
            new DefaultCellEditor( new RubriekComboBox( connection, 0, false ) );
        rekeningMutatieTable.getColumnModel( ).getColumn( 2 ).setCellEditor( rubriekDefaultCellEditor );

        final DefaultCellEditor debCredDefaultCellEditor =
            new DefaultCellEditor( new DebCredComboBox( connection, 0, "", true ) );
        rekeningMutatieTable.getColumnModel( ).getColumn( 3 ).setCellEditor( debCredDefaultCellEditor );

        DecimalFormat sumMutatieDecimalFormat = new DecimalFormat( "EUR #0.00;EUR -#" );
        sumMutatieInLabel.setText( sumMutatieDecimalFormat.format( rekeningMutatieTableModel.sumMutatieIn ) );
        sumMutatieOutLabel.setText( sumMutatieDecimalFormat.format( rekeningMutatieTableModel.sumMutatieOut ) );
        double saldo = rekeningMutatieTableModel.sumMutatieIn - rekeningMutatieTableModel.sumMutatieOut;
        sumMutatieSaldoLabel.setText( sumMutatieDecimalFormat.format( saldo ) );
    }


    private void updateSaldo( ) {
        // Check if rekening has been selected
        if ( selectedRekeningId == 0 ) {
            return;
        }

        double saldo;
        try {
            Statement statement = connection.createStatement( );
            ResultSet resultSet = statement.executeQuery( "SELECT SUM( mutatie_in ), SUM( mutatie_uit )" +
                                                          " FROM rekening_mutatie " +
                                                          " WHERE rekening_id = " + selectedRekeningId +
                                                          " AND datum >= '" + startDatumString + "'" );

            if ( ! resultSet.next( ) ) {
                logger.severe( "Could not get record for rekening_id " +
                               selectedRekeningId + " in rekening" );
                return;
            }
            double sumIn = resultSet.getDouble( 1 );
            double sumUit = resultSet.getDouble( 2 );
            saldo = sumIn - sumUit;
        } catch ( SQLException sqlException ) {
            logger.severe( "SQLException: " + sqlException.getMessage( ) );
            return;
        }

        if ( startSaldo != 0 ) {
            // Get a Date object from the start datum string
            Date startDatumDate;
            Date euroDatumDate;
            try {
                startDatumDate = dateFormat.parse( startDatumString );
                euroDatumDate  = dateFormat.parse( euroDatumString );
                String testStartDatumString = dateFormat.format( startDatumDate );
                logger.fine( "startDatumDate: " + testStartDatumString );
            } catch( ParseException parseException ) {
                logger.severe( "Datum parse exception: " + parseException.getMessage( ) );
                return;
            }
            if ( ( startDatumDate.before( euroDatumDate ) ) &&
                    ( ( rekeningTypeId == 1 ) || ( rekeningTypeId == 2 ) ||
                      ( rekeningTypeId == 3 ) || ( rekeningTypeId == 6 ) ) ) {
                logger.fine( "converting start saldo from NLG to EUR" );
                final double euroConv = 2.20371;
                saldo += startSaldo / euroConv;
            } else {
                saldo += startSaldo;
            }
        }

        String saldoString = rekeningDecimalFormat.format( saldo );
        int result = JOptionPane.showConfirmDialog( frame,
                                                    "Insert Saldo " + saldoString + " in " +
                                                    rekeningComboBox.getSelectedRekeningString( ) + " ?",
                                                    "Saldo",
                                                    JOptionPane.YES_NO_OPTION,
                                                    JOptionPane.QUESTION_MESSAGE,
                                                    null );

        if ( result != JOptionPane.YES_OPTION ) return;

        // Get the current date
        GregorianCalendar calendar = new GregorianCalendar( );
        Date datumDate = calendar.getTime( );
        String datumString = dateFormat.format( datumDate );

        // Truncate on 5 decimals, to avoid data truncation error in SQL update
        // Zie: http://docs.oracle.com/javase/6/docs/api/java/math/BigDecimal.html
        // Scale is the number of digits to the right of the decimal point
        logger.fine( "saldo: " + saldo );
        BigDecimal bigDecimal = new BigDecimal( saldo );
        String saldoConverted = bigDecimal.setScale( 5, RoundingMode.HALF_UP ).toString( );
        logger.fine( "saldo converted: " + saldoConverted );

        int nUpdate = 0;
        try
        {
            String updateString = ( "UPDATE rekening SET saldo = " + saldoConverted +
                                    ", datum = '" + datumString + "'" +
                                    " WHERE rekening_id = " + selectedRekeningId );
            logger.info( "updateString: " + updateString );
            Statement statement = connection.createStatement( );
            nUpdate = statement.executeUpdate( updateString );
        } catch ( SQLException sqlException ) {
            logger.severe( "SQLException: " + sqlException.getMessage( ) );
        }
        if ( nUpdate != 1 ) {
            logger.severe( "Could not update in rekening" );
            return;
        }
        logger.info( "saldo updated" );

        // Update the saldo label
        saldoLabel.setText( saldoString );

        // Update the datum label
        datumLabel.setText( datumString );
    }
}
