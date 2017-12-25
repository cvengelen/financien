package financien.rekeningmutatiesrubriek;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.ResultSet;
import java.sql.Statement;

import java.util.logging.*;
import java.text.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.table.*;
import javax.swing.border.*;
import javax.swing.event.*;

import financien.gui.RekeningMutatieDialog;
import financien.gui.RubriekComboBox;
import table.*;

/**
 * Frame to show, insert and update records for a specific rubriek in the rekening_mutatie table in schema financien.
 * @author Chris van Engelen
 */
public class EditRekeningMutatiesRubriek extends JInternalFrame {
    private final Logger logger = Logger.getLogger( EditRekeningMutatiesRubriek.class.getCanonicalName() );

    private RekeningMutatiesRubriekTableModel rekeningMutatiesRubriekTableModel;
    private TableSorter rekeningMutatiesRubriekTableSorter;
    private JTable rekeningMutatiesRubriekTable;

    private RubriekComboBox rubriekComboBox;
    private int selectedRubriekId;

    private JLabel omschrijvingLabel;

    private final int maximumRekeningTypeId = 9;	// Maximum value field rekening_type_id in table rekening_type
    private final DecimalFormat [ ] mutatieDecimalFormat = new DecimalFormat[ maximumRekeningTypeId + 1 ];

    public EditRekeningMutatiesRubriek( final Connection connection, final JFrame parentFrame, int x, int y ) {
        super("Edit rekening mutatie rubriek", true, true, true, true);

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
            JOptionPane.showMessageDialog( parentFrame,
                    sqlException.getMessage( ),
                    "Edit rekening mutatie rubriek SQL exception",
                    JOptionPane.ERROR_MESSAGE);
	    logger.severe( "SQLException in rekeningTypeStatement: " + sqlException.getMessage( ) );
	}

	final Container container = getContentPane( );

	// Set grid bag layout manager
	container.setLayout( new GridBagLayout( ) );
	GridBagConstraints constraints = new GridBagConstraints( );

        constraints.insets = new Insets( 20, 20, 5, 5 );
	constraints.gridx = 0;
	constraints.gridy = 0;
        constraints.anchor = GridBagConstraints.EAST;
	container.add( new JLabel( "Rubriek:" ), constraints );

	// Setup a JComboBox with the results of the query on rubriek
	rubriekComboBox = new RubriekComboBox( connection, 0, true );
        rubriekComboBox.addActionListener( ( ActionEvent actionEvent ) -> {
            // Get the selected Rubriek ID
            selectedRubriekId = rubriekComboBox.getSelectedRubriekId( );

            // Check if rubriek has been selected
            if ( selectedRubriekId == 0 ) {
                return;
            }

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

                omschrijvingLabel.setText( resultSet.getString( 1 ) );

                // Setup the rekening_mutatie table for the selected rubriek
                setupRekeningMutatieRubriekTable( selectedRubriekId );
            } catch ( SQLException sqlException ) {
                JOptionPane.showMessageDialog( parentFrame,
                        sqlException.getMessage( ),
                        "Edit rekening mutatie rubriek SQL exception",
                        JOptionPane.ERROR_MESSAGE);
                logger.severe( "SQLException: " + sqlException.getMessage( ) );
            }
        } );

        constraints.insets = new Insets( 20, 5, 5, 20 );
        constraints.anchor = GridBagConstraints.WEST;
	constraints.gridx = GridBagConstraints.RELATIVE;
	container.add( rubriekComboBox, constraints );


        constraints.insets = new Insets( 5, 20, 5, 5 );
        constraints.anchor = GridBagConstraints.EAST;
	constraints.gridx = 0;
	constraints.gridy = 1;
	container.add( new JLabel( "Omschrijving:" ), constraints );

	omschrijvingLabel = new JLabel( );

        constraints.insets = new Insets( 5, 5, 5, 20 );
        constraints.anchor = GridBagConstraints.WEST;
	constraints.gridx = GridBagConstraints.RELATIVE;
	container.add( omschrijvingLabel, constraints );

	// Create rekening_mutaties table from rekening_mutaties table model
	rekeningMutatiesRubriekTableModel = new RekeningMutatiesRubriekTableModel( connection, parentFrame );
	rekeningMutatiesRubriekTableSorter = new TableSorter( rekeningMutatiesRubriekTableModel );
	rekeningMutatiesRubriekTable = new JTable( rekeningMutatiesRubriekTableSorter );
	rekeningMutatiesRubriekTableSorter.setTableHeader( rekeningMutatiesRubriekTable.getTableHeader( ) );
	// rekeningMutatiesRubriekTableSorter.setSortingStatus( 0, TableSorter.DESCENDING );

	rekeningMutatiesRubriekTable.setAutoResizeMode( JTable.AUTO_RESIZE_OFF );

	// Set vertical size just enough for 20 entries, horizontal 870+16 for scrollbar
	rekeningMutatiesRubriekTable.setPreferredScrollableViewportSize( new Dimension( 1220, 320 ) );

	// Set renderer for Double objects
	class DoubleRenderer extends JTextField implements TableCellRenderer {
	    public Component getTableCellRendererComponent( JTable table,
							    Object object,
							    boolean isSelected,
							    boolean hasFocus,
							    int row, int column ) {
		switch ( column ) {
		case 3:		// MutatieIn
		case 4:		// MutatieUit
		    final double mutatie = ( Double )object;
		    if ( mutatie == 0 ) {
			// Return empty string
			this.setText( "" );
		    } else {
			// Get the rekening type id of this row from the table
			final int rekeningTypeId = rekeningMutatiesRubriekTableModel.getRekeningTypeId( row );

			// Use the formatter defined for this rekening_type in table rekening_type
			this.setText( mutatieDecimalFormat[ rekeningTypeId ].format( mutatie ) );
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
	rekeningMutatiesRubriekTable.setDefaultRenderer( Double.class, doubleRenderer );

        constraints.insets = new Insets( 5, 20, 5, 20 );
        constraints.anchor = GridBagConstraints.CENTER;
	constraints.gridx = 0;
	constraints.gridy = 2;
	constraints.gridwidth = 2;

        // Setting weightx, weighty and fill is necessary for proper filling the frame when resized.
        constraints.weightx = 1d;
        constraints.weighty = 1d;
        constraints.fill = GridBagConstraints.BOTH;

	container.add( new JScrollPane( rekeningMutatiesRubriekTable ), constraints );

        constraints.weightx = 0d;
        constraints.weighty = 0d;
        constraints.fill = GridBagConstraints.NONE;


	////////////////////////////////////////////////
	// Insert, Edit, Delete, Close Buttons
	////////////////////////////////////////////////

	// Define the edit and delete buttons because these are used by the list selection listener
	final JButton editMutatieButton = new JButton( "Edit" );
	final JButton deleteMutatieButton = new JButton( "Delete" );

	// Get the selection model related to the rekening_mutatie table
	final ListSelectionModel mutatieListSelectionModel = rekeningMutatiesRubriekTable.getSelectionModel( );

	class MutatieListSelectionListener implements ListSelectionListener {
	    private int selectedRow = -1;

	    public void valueChanged( ListSelectionEvent listSelectionEvent ) {
		// Ignore extra messages.
		if ( listSelectionEvent.getValueIsAdjusting( ) ) return;

		// Ignore if nothing is selected
		if ( mutatieListSelectionModel.isSelectionEmpty( ) ) {
		    selectedRow = -1;
		    editMutatieButton.setEnabled( false );
		    deleteMutatieButton.setEnabled( false );
		    return;
		}

		int viewRow = mutatieListSelectionModel.getMinSelectionIndex( );
		selectedRow = rekeningMutatiesRubriekTableSorter.modelIndex( viewRow );
		editMutatieButton.setEnabled( true );
		deleteMutatieButton.setEnabled( true );
	    }

            private int getSelectedRow ( ) { return selectedRow; }
	}

	// Add mutatieListSelectionListener object to the selection model of the musici table
	final MutatieListSelectionListener mutatieListSelectionListener = new MutatieListSelectionListener( );
	mutatieListSelectionModel.addListSelectionListener( mutatieListSelectionListener );


	// Class to handle button actions: uses mutatieListSelectionListener
	class ButtonActionListener implements ActionListener {
	    public void actionPerformed( ActionEvent actionEvent ) {
		if ( actionEvent.getActionCommand( ).equals( "close" ) ) {
		    setVisible( false );
                    dispose();
		    return;
		} else if ( actionEvent.getActionCommand( ).equals( "insert" ) ) {
		    new RekeningMutatieDialog( connection,
                                               parentFrame,
                                               selectedRubriekId,
                                               0, 1 );
		} else {
		    int selectedRow = mutatieListSelectionListener.getSelectedRow( );
		    if ( selectedRow < 0 ) {
			JOptionPane.showMessageDialog( parentFrame,
						       "Geen mutatie geselecteerd",
						       "Rubriek frame error",
						       JOptionPane.ERROR_MESSAGE );
			return;
		    }

		    final int rubriekId = rekeningMutatiesRubriekTableModel.getRubriekId( );
		    final String datumString = rekeningMutatiesRubriekTableModel.getDatumString( selectedRow );
		    final int debCredId = rekeningMutatiesRubriekTableModel.getDebCredId( selectedRow );
		    final int rekeningId = rekeningMutatiesRubriekTableModel.getRekeningId( selectedRow );
                    final int rekeningHouderId = rekeningMutatiesRubriekTableModel.getRekeningHouderId( selectedRow );
		    final int volgNummer = rekeningMutatiesRubriekTableModel.getVolgNummer( selectedRow );

		    if ( actionEvent.getActionCommand( ).equals( "edit" ) ) {
			new RekeningMutatieDialog( connection,
                                                   parentFrame,
                                                   rubriekId,
                                                   datumString,
                                                   debCredId,
                                                   rekeningId,
                                                   rekeningHouderId,
                                                   volgNummer );
		    } else if ( actionEvent.getActionCommand( ).equals( "delete" ) ) {
			int result =
			    JOptionPane.showConfirmDialog( parentFrame,
							   "Delete record for rekening " +
							   rekeningMutatiesRubriekTableModel.getRekeningString( selectedRow ) +
							   " at date " + datumString +
							   " in rekening_mutatie ?",
							   "Delete rekening mutatie record",
							   JOptionPane.YES_NO_OPTION,
							   JOptionPane.QUESTION_MESSAGE,
							   null );

			if ( result != JOptionPane.YES_OPTION ) return;

			String deleteString  = "DELETE FROM rekening_mutatie";
			deleteString += " WHERE rubriek_id = " + rubriekId;
			deleteString += " AND datum = '" + datumString + "'";
			deleteString += " AND deb_cred_id = " + debCredId;
			deleteString += " AND rekening_id = " + rekeningId;
			deleteString += " AND volgnummer = " + volgNummer;

			logger.info( "deleteString: " + deleteString );

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
				JOptionPane.showMessageDialog( parentFrame,
							       errorString,
							       "Delete rekening mutatie record",
							       JOptionPane.ERROR_MESSAGE);
				logger.severe( errorString );
				return;
			    }
			} catch ( SQLException sqlException ) {
                            JOptionPane.showMessageDialog( parentFrame,
                                    sqlException.getMessage( ),
                                    "Delete rekening mutatie record SQL exception",
                                    JOptionPane.ERROR_MESSAGE);
			    logger.severe( "SQLException: " + sqlException.getMessage( ) );
			    return;
			}
		    }
		}

		// Records may have been modified: setup the table model again
		setupRekeningMutatieRubriekTable( selectedRubriekId );
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

	deleteMutatieButton.setActionCommand( "delete" );
	deleteMutatieButton.setEnabled( false );
	deleteMutatieButton.addActionListener( buttonActionListener );
	buttonPanel.add( deleteMutatieButton );

	final JButton closeButton = new JButton( "Close" );
	closeButton.addActionListener( buttonActionListener );
	closeButton.setActionCommand( "close" );
	buttonPanel.add( closeButton );

        constraints.insets = new Insets( 5, 20, 20, 20 );
	constraints.gridx = 0;
	constraints.gridy = 3;
	container.add( buttonPanel, constraints );

	setSize( 1280, 500 );
        setLocation(x, y);
	setDefaultCloseOperation( JFrame.DISPOSE_ON_CLOSE );
	setVisible( true );
    }


    private void setupRekeningMutatieRubriekTable( int rubriekId ) {
	// Setup the rekening_mutatie table for the selected rubriek
	rekeningMutatiesRubriekTableModel.setupRekeningMutatieRubriekTableModel( rubriekId );

	// Setup the tableSorter again so that the TableSorter gets the new table size (# rows)
	rekeningMutatiesRubriekTableSorter.setTableModel( rekeningMutatiesRubriekTableModel );

	// Need to setup preferred column width up again
	rekeningMutatiesRubriekTable.getColumnModel( ).getColumn( 0 ).setPreferredWidth( 100 );  // Datum
	rekeningMutatiesRubriekTable.getColumnModel( ).getColumn( 1 ).setPreferredWidth( 150 );  // Deb/Cred
	rekeningMutatiesRubriekTable.getColumnModel( ).getColumn( 2 ).setPreferredWidth( 150 );  // Rekening
	rekeningMutatiesRubriekTable.getColumnModel( ).getColumn( 3 ).setPreferredWidth( 100 );  // In
	rekeningMutatiesRubriekTable.getColumnModel( ).getColumn( 4 ).setPreferredWidth( 100 );  // Uit
	rekeningMutatiesRubriekTable.getColumnModel( ).getColumn( 5 ).setPreferredWidth( 40 );   // VolgNummer
	rekeningMutatiesRubriekTable.getColumnModel( ).getColumn( 6 ).setPreferredWidth( 40 );   // Jaar
	rekeningMutatiesRubriekTable.getColumnModel( ).getColumn( 7 ).setPreferredWidth( 40 );   // Maand
	rekeningMutatiesRubriekTable.getColumnModel( ).getColumn( 8 ).setPreferredWidth( 500 );  // Omschrijving
    }
}
