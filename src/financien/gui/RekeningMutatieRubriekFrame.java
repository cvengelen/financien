// frame to show and update records in rekening_mutatie selected on rubriek

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


public class RekeningMutatieRubriekFrame {
    final private Logger logger = Logger.getLogger( "financien.gui.RekeningMutatieRubriekFrame" );

    Connection connection;

    final JFrame frame = new JFrame( "RekeningMutatieRubriek" );

    RekeningMutatieRubriekTableModel rekeningMutatieRubriekTableModel;
    TableSorter rekeningMutatieRubriekTableSorter;
    JTable rekeningMutatieRubriekTable;

    RubriekComboBox rubriekComboBox;
    int selectedRubriekId;

    JLabel omschrijvingLabel;

    final int maximumRekeningTypeId = 9;	// Maximum value field rekening_type_id in table rekening_type
    DecimalFormat [ ] mutatieDecimalFormat = new DecimalFormat[ maximumRekeningTypeId + 1 ];

    public RekeningMutatieRubriekFrame( final Connection connection ) {
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
	container.add( new JLabel( "Rubriek:" ), constraints );

	// Setup a JComboBox with the results of the query on rubriek
	rubriekComboBox = new RubriekComboBox( connection, 0, true );
        constraints.anchor = GridBagConstraints.WEST;
	constraints.gridx = GridBagConstraints.RELATIVE;
	container.add( rubriekComboBox, constraints );

	class RubriekActionListener implements ActionListener {
	    public void actionPerformed( ActionEvent actionEvent ) {
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
		    logger.severe( "SQLException: " + sqlException.getMessage( ) );
		}
	    }
	}
	rubriekComboBox.addActionListener( new RubriekActionListener( ) );

        constraints.anchor = GridBagConstraints.EAST;
	constraints.gridx = 0;
	constraints.gridy = 2;
	container.add( new JLabel( "Omschrijving: " ), constraints );

	omschrijvingLabel = new JLabel( );
        constraints.anchor = GridBagConstraints.WEST;
	constraints.gridx = GridBagConstraints.RELATIVE;
	container.add( omschrijvingLabel, constraints );

	// Create rekening_mutatie table from rekening_mutatie table model
	rekeningMutatieRubriekTableModel = new RekeningMutatieRubriekTableModel( connection );
	rekeningMutatieRubriekTableSorter = new TableSorter( rekeningMutatieRubriekTableModel );
	rekeningMutatieRubriekTable = new JTable( rekeningMutatieRubriekTableSorter );
	rekeningMutatieRubriekTableSorter.setTableHeader( rekeningMutatieRubriekTable.getTableHeader( ) );
	// rekeningMutatieRubriekTableSorter.setSortingStatus( 0, TableSorter.DESCENDING );

	rekeningMutatieRubriekTable.setAutoResizeMode( JTable.AUTO_RESIZE_OFF );

	// Set vertical size just enough for 20 entries, horizontal 870+16 for scrollbar
	rekeningMutatieRubriekTable.setPreferredScrollableViewportSize( new Dimension( 886, 320 ) );

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
		    final double mutatie = ( ( Double )object ).doubleValue( );
		    if ( mutatie == 0 ) {
			// Return empty string
			this.setText( "" );
		    } else {
			// Get the rekening type id of this row from the table
			final int rekeningTypeId = rekeningMutatieRubriekTableModel.getRekeningTypeId( row );

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
	rekeningMutatieRubriekTable.setDefaultRenderer( Double.class, doubleRenderer );


        constraints.anchor = GridBagConstraints.CENTER;
	constraints.gridx = 0;
	constraints.gridy = 3;
	constraints.gridwidth = 2;

        // Setting weighty and fill is necessary for proper filling the frame when resized.
        constraints.weighty = 1.0;
        constraints.fill = GridBagConstraints.BOTH;

	container.add( new JScrollPane( rekeningMutatieRubriekTable ), constraints );


	////////////////////////////////////////////////
	// Insert, Edit, Delete, Close Buttons
	////////////////////////////////////////////////

	// Define the edit and delete buttons because these are used by the list selection listener
	final JButton editMutatieButton = new JButton( "Edit" );
	final JButton deleteMutatieButton = new JButton( "Delete" );

	// Get the selection model related to the rekening_mutatie table
	final ListSelectionModel mutatieListSelectionModel = rekeningMutatieRubriekTable.getSelectionModel( );

	class MutatieListSelectionListener implements ListSelectionListener {
	    int selectedRow = -1;

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
		selectedRow = rekeningMutatieRubriekTableSorter.modelIndex( viewRow );
		editMutatieButton.setEnabled( true );
		deleteMutatieButton.setEnabled( true );
	    }

	    public int getSelectedRow ( ) { return selectedRow; }
	}

	// Add mutatieListSelectionListener object to the selection model of the musici table
	final MutatieListSelectionListener mutatieListSelectionListener = new MutatieListSelectionListener( );
	mutatieListSelectionModel.addListSelectionListener( mutatieListSelectionListener );


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
						   0, 1 );
		} else {
		    int selectedRow = mutatieListSelectionListener.getSelectedRow( );
		    if ( selectedRow < 0 ) {
			JOptionPane.showMessageDialog( frame,
						       "Geen mutatie geselecteerd",
						       "Rubriek frame error",
						       JOptionPane.ERROR_MESSAGE );
			return;
		    }

		    final int rubriekId = rekeningMutatieRubriekTableModel.getRubriekId( );
		    final String datumString = rekeningMutatieRubriekTableModel.getDatumString( selectedRow );
		    final int debCredId = rekeningMutatieRubriekTableModel.getDebCredId( selectedRow );
		    final int rekeningId = rekeningMutatieRubriekTableModel.getRekeningId( selectedRow );
                    final int rekeningHouderId = rekeningMutatieRubriekTableModel.getRekeningHouderId( selectedRow );
		    final int volgNummer = rekeningMutatieRubriekTableModel.getVolgNummer( selectedRow );

		    if ( actionEvent.getActionCommand( ).equals( "edit" ) ) {
			RekeningMutatieDialog rekeningMutatieDialog = new RekeningMutatieDialog( connection,
												 frame,
												 rubriekId,
												 datumString,
												 debCredId,
												 rekeningId,
												 rekeningHouderId,
												 volgNummer );
		    } else if ( actionEvent.getActionCommand( ).equals( "delete" ) ) {
			int result =
			    JOptionPane.showConfirmDialog( frame,
							   "Delete record for rekening " +
							   rekeningMutatieRubriekTableModel.getRekeningString( selectedRow ) +
							   " at date " + datumString +
							   " in rekening_mutatie ?",
							   "Delete rekening_mutatie record",
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


	constraints.gridx = 0;
	constraints.gridy = 10;
	constraints.gridwidth = 2;
	container.add( buttonPanel, constraints );

	frame.setSize( 1000, 500 );
	frame.setDefaultCloseOperation( JFrame.DISPOSE_ON_CLOSE );
	frame.setVisible( true );
    }


    private void setupRekeningMutatieRubriekTable( int rubriekId ) {
	// Setup the rekening_mutatie table for the selected rubriek
	rekeningMutatieRubriekTableModel.setupRekeningMutatieRubriekTableModel( rubriekId );

	// Setup the tableSorter again so that the TableSorter gets the new table size (# rows)
	rekeningMutatieRubriekTableSorter.setTableModel( rekeningMutatieRubriekTableModel );

	// Need to setup preferred column width up again
	rekeningMutatieRubriekTable.getColumnModel( ).getColumn( 0 ).setPreferredWidth( 100 );  // Datum
	rekeningMutatieRubriekTable.getColumnModel( ).getColumn( 1 ).setPreferredWidth( 150 );  // Deb/Cred
	rekeningMutatieRubriekTable.getColumnModel( ).getColumn( 2 ).setPreferredWidth( 150 );  // Rekening
	rekeningMutatieRubriekTable.getColumnModel( ).getColumn( 3 ).setPreferredWidth( 100 );  // In
	rekeningMutatieRubriekTable.getColumnModel( ).getColumn( 4 ).setPreferredWidth( 100 );  // Uit
	rekeningMutatieRubriekTable.getColumnModel( ).getColumn( 5 ).setPreferredWidth( 40 );   // VolgNummer
	rekeningMutatieRubriekTable.getColumnModel( ).getColumn( 6 ).setPreferredWidth( 40 );   // Jaar
	rekeningMutatieRubriekTable.getColumnModel( ).getColumn( 7 ).setPreferredWidth( 40 );   // Maand
	rekeningMutatieRubriekTable.getColumnModel( ).getColumn( 8 ).setPreferredWidth( 250 );  // Omschrijving
    }
}
