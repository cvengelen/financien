// frame to show and update records in rubriek

package financien.rubriek;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.ResultSet;
import java.sql.Statement;

import java.util.logging.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.event.*;

import financien.gui.DebCredComboBox;
import financien.gui.GroepComboBox;
import table.*;


class RubriekFrame {
    private final Logger logger = Logger.getLogger( RubriekFrame.class.getCanonicalName() );

    private final JFrame frame = new JFrame( "Rubriek" );

    private RubriekTableModel rubriekTableModel;
    private TableSorter rubriekTableSorter;

    private JTextField rubriekFilterTextField;


    public RubriekFrame( final Connection connection ) {

	final Container container = frame.getContentPane( );

	// Set grid bag layout manager
	container.setLayout( new GridBagLayout( ) );
	GridBagConstraints constraints = new GridBagConstraints( );
	constraints.anchor = GridBagConstraints.WEST;
	constraints.insets = new Insets( 0, 0, 10, 10 );


	/////////////////////////////////
	// Rubriek filter action listener
	/////////////////////////////////

	class RubriekFilterActionListener implements ActionListener {
	    public void actionPerformed( ActionEvent actionEvent ) {
		// Setup the Rubriek table
		rubriekTableModel.setupRubriekTableModel( rubriekFilterTextField.getText( ) );
	    }
	}

	/////////////////////////////////
	// Rubriek filter string
	/////////////////////////////////

	constraints.gridx = 0;
	constraints.gridy = 0;
	constraints.anchor = GridBagConstraints.EAST;
	constraints.gridwidth = 1;
	container.add( new JLabel( "Rubriek Filter:" ), constraints );

	rubriekFilterTextField = new JTextField( 20 );
	rubriekFilterTextField.addActionListener( new RubriekFilterActionListener( ) );
	constraints.gridx = GridBagConstraints.RELATIVE;
	constraints.anchor = GridBagConstraints.WEST;
	container.add( rubriekFilterTextField, constraints );


	// Define the edit, cancel, save and delete buttons
	// These are enabled/disabled by the table model and the list selection listener.
	final JButton editRubriekButton = new JButton( "Edit" );
	final JButton cancelRubriekButton = new JButton( "Cancel" );
	final JButton saveRubriekButton = new JButton( "Save" );
	final JButton deleteRubriekButton = new JButton( "Delete" );

	// Create table from rubriek table model
	rubriekTableModel = new RubriekTableModel( connection,
						   frame,
						   cancelRubriekButton,
						   saveRubriekButton );
	rubriekTableSorter = new TableSorter( rubriekTableModel );
	final JTable rubriekTable = new JTable( rubriekTableSorter );
	rubriekTableSorter.setTableHeader( rubriekTable.getTableHeader( ) );

	rubriekTable.setAutoResizeMode( JTable.AUTO_RESIZE_OFF );

	rubriekTable.getColumnModel( ).getColumn( 0 ).setPreferredWidth(  50 );  // Rubriek ID
	rubriekTable.getColumnModel( ).getColumn( 1 ).setPreferredWidth( 250 );  // Rubriek
	rubriekTable.getColumnModel( ).getColumn( 2 ).setPreferredWidth( 300 );  // omschrijving
	rubriekTable.getColumnModel( ).getColumn( 3 ).setPreferredWidth( 150 );  // groep
	rubriekTable.getColumnModel( ).getColumn( 4 ).setPreferredWidth( 150 );  // rubriek

	final DefaultCellEditor groepDefaultCellEditor =
	    new DefaultCellEditor( new GroepComboBox( connection, 0 ) );
	rubriekTable.getColumnModel( ).getColumn( 3 ).setCellEditor( groepDefaultCellEditor );

	final DefaultCellEditor debCredDefaultCellEditor =
	    new DefaultCellEditor( new DebCredComboBox( connection, 0, null, false ) );
	rubriekTable.getColumnModel( ).getColumn( 4 ).setCellEditor( debCredDefaultCellEditor );

	// Set vertical size just enough for 20 entries
	rubriekTable.setPreferredScrollableViewportSize( new Dimension( 900, 320 ) );

	constraints.gridx = 0;
	constraints.gridy = 3;
	constraints.gridwidth = 2;
	container.add( new JScrollPane( rubriekTable ), constraints );


	////////////////////////////////////////////////
	// List selection listener
	////////////////////////////////////////////////

	// Get the selection model related to the rubriek table
	final ListSelectionModel rubriekListSelectionModel = rubriekTable.getSelectionModel( );

	class RubriekListSelectionListener implements ListSelectionListener {
	    private int selectedRow = -1;

	    public void valueChanged( ListSelectionEvent listSelectionEvent ) {
		// Ignore extra messages.
		if ( listSelectionEvent.getValueIsAdjusting( ) ) return;

		// Check if current row has modified values
		if ( rubriekTableModel.getRowModified( ) ) {
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
			    if ( !( rubriekTableModel.saveEditRow( selectedRow ) ) ) {
				JOptionPane.showMessageDialog( frame,
							       "Error: row not saved",
							       "Save rubriek record error",
							       JOptionPane.ERROR_MESSAGE );
				return;
			    }
			} else {
			    // Cancel any edits in the selected row
			    rubriekTableModel.cancelEditRow( selectedRow );
			}
		    }
		}

		// Ignore if nothing is selected
		if ( rubriekListSelectionModel.isSelectionEmpty( ) ) {
		    selectedRow = -1;

		    editRubriekButton.setEnabled( false );
		    cancelRubriekButton.setEnabled( false );
		    saveRubriekButton.setEnabled( false );
		    deleteRubriekButton.setEnabled( false );

		    return;
		}

		// Remove the capability to edit the row
		rubriekTableModel.unsetEditRow( );

		// Get the selected row
		int viewRow = rubriekListSelectionModel.getMinSelectionIndex( );
		selectedRow = rubriekTableSorter.modelIndex( viewRow );

		// Enable the edit button
		editRubriekButton.setEnabled( true );

		// Disable the cancel and save buttons (these will be enabled
		// when any data in the row is actually modified)
		cancelRubriekButton.setEnabled( false );
		saveRubriekButton.setEnabled( false );

		// Enable the delete button
		deleteRubriekButton.setEnabled( true );
	    }

	    int getSelectedRow ( ) { return selectedRow; }
	}

	// Add rubriekListSelectionListener object to the selection model of the rubriek table
	final RubriekListSelectionListener rubriekListSelectionListener = new RubriekListSelectionListener( );
	rubriekListSelectionModel.addListSelectionListener( rubriekListSelectionListener );


	////////////////////////////////////////////////
	// Add, Edit, Cancel, Save, Delete, Close Buttons
	////////////////////////////////////////////////

	// Class to handle button actions: uses rubriekListSelectionListener
	class ButtonActionListener implements ActionListener {
	    public void actionPerformed( ActionEvent actionEvent ) {
		if ( actionEvent.getActionCommand( ).equals( "close" ) ) {
		    frame.setVisible( false );
		    System.exit( 0 );
		} else if ( actionEvent.getActionCommand( ).equals( "add" ) ) {
		    String resultString = JOptionPane.showInputDialog( frame,
                                                                       "Enter rubriek ID:",
                                                                       "Input Rubriek Id",
                                                                       JOptionPane.PLAIN_MESSAGE );
		    logger.info( "resultString:" + resultString );
		    if ( resultString == null ) return;

		    try {
			final int rubriekId = Integer.parseInt( resultString );

			try {
			    Statement statement = connection.createStatement( );
			    ResultSet resultSet = statement.executeQuery( "SELECT rubriek_id FROM rubriek " +
									  "WHERE rubriek_id = " + rubriekId );
			    if ( resultSet.next( ) ) {
				JOptionPane.showMessageDialog( frame,
							       "Rubriek ID " + rubriekId +
							       " bestaat al in tabel rubriek",
							       "Rubriek frame error",
							       JOptionPane.ERROR_MESSAGE );
				return;
			    }

			    String insertString = "INSERT INTO rubriek SET rubriek_id = " + rubriekId;
			    logger.info( "insertString: " + insertString );
			    if ( statement.executeUpdate( insertString ) != 1 ) {
				logger.severe( "Could not insert in rubriek" );
				return;
			    }
			} catch ( SQLException sqlException ) {
			    logger.severe( "SQLException: " + sqlException.getMessage( ) );
			    return;
			}
		    } catch ( NumberFormatException numberFormatException ) {
			JOptionPane.showMessageDialog( frame,
						       "Incorrect rubriek ID format " +
						       numberFormatException.getMessage( ),
						       "Rubriek frame error",
						       JOptionPane.ERROR_MESSAGE );
			return;
		    }

		    // Records may have been modified: setup the table model again
		    rubriekTableModel.setupRubriekTableModel( rubriekFilterTextField.getText( ) );
		} else {
		    int selectedRow = rubriekListSelectionListener.getSelectedRow( );
		    if ( selectedRow < 0 ) {
			JOptionPane.showMessageDialog( frame,
						       "Geen Rubriek geselecteerd",
						       "Rubriek frame error",
						       JOptionPane.ERROR_MESSAGE );
			return;
		    }

		    final int rubriekId = rubriekTableModel.getRubriekId( selectedRow );
		    final String rubriekString = rubriekTableModel.getRubriekString( selectedRow );

		    if ( actionEvent.getActionCommand( ).equals( "delete" ) ) {
			// Check if rubriek is used in table rekening_mutatie
			try {
			    Statement statement = connection.createStatement( );
			    String rekeningMutatieQueryString =
				"SELECT rubriek_id FROM rekening_mutatie WHERE rubriek_id = " + rubriekId;
			    ResultSet resultSet = statement.executeQuery( rekeningMutatieQueryString );
			    if ( resultSet.next( ) ) {
				JOptionPane.showMessageDialog( frame,
							       "Tabel rekening_mutatie gebruikt rubriek " +
							       rubriekString,
							       "Rubriek delete record error",
							       JOptionPane.ERROR_MESSAGE );
				return;
			    }
			} catch ( SQLException sqlException ) {
			    logger.severe( "SQLException: " + sqlException.getMessage( ) );
			    return;
			}

			int result =
			    JOptionPane.showConfirmDialog( frame,
							   "Delete rubriek " +
							   rubriekTableModel.getRubriekString( selectedRow ) +
							   " met ID " + rubriekId + " ?",
							   "Delete Rubriek record",
							   JOptionPane.YES_NO_OPTION,
							   JOptionPane.QUESTION_MESSAGE,
							   null );

			if ( result != JOptionPane.YES_OPTION ) return;

			String deleteString  = "DELETE FROM rubriek";
			deleteString += " WHERE rubriek_id = " + rubriekId;

			logger.info( "deleteString: " + deleteString );

			try {
			    Statement statement = connection.createStatement( );
			    int nUpdate = statement.executeUpdate( deleteString );
			    if ( nUpdate != 1 ) {
				String errorString = ( "Could not delete record with rubriek_id  = " +
						       rubriekId + " in rubriek" );
				JOptionPane.showMessageDialog( frame,
							       errorString,
							       "Delete Rubriek record",
							       JOptionPane.ERROR_MESSAGE);
				logger.severe( errorString );
				return;
			    }
			} catch ( SQLException sqlException ) {
			    logger.severe( "SQLException: " + sqlException.getMessage( ) );
			    return;
			}

			// Records may have been modified: setup the table model again
			rubriekTableModel.setupRubriekTableModel( rubriekFilterTextField.getText( ) );
		    } else if ( actionEvent.getActionCommand( ).equals( "edit" ) ) {
			// Allow to edit the selected row
			rubriekTableModel.setEditRow( selectedRow );

			// Disable the edit button
			editRubriekButton.setEnabled( false );
		    } else if ( actionEvent.getActionCommand( ).equals( "cancel" ) ) {
			// Cancel any edits in the selected row
			rubriekTableModel.cancelEditRow( selectedRow );

			// Remove the capability to edit the row
			rubriekTableModel.unsetEditRow( );

			// Enable the edit button, so that the user can select edit again
			editRubriekButton.setEnabled( true );

			// Disable the cancel and save buttons
			cancelRubriekButton.setEnabled( false );
			saveRubriekButton.setEnabled( false );
		    } else if ( actionEvent.getActionCommand( ).equals( "save" ) ) {
			// Save the changes in the table model, and in the database
			if ( !( rubriekTableModel.saveEditRow( selectedRow ) ) ) {
			    JOptionPane.showMessageDialog( frame,
							   "Error: row not saved",
							   "Save rubriek record error",
							   JOptionPane.ERROR_MESSAGE );
			    return;
			}

			// Remove the capability to edit the row
			rubriekTableModel.unsetEditRow( );

			// Enable the edit button, so that the user can select edit again
			editRubriekButton.setEnabled( true );

			// Disable the cancel and save buttons
			cancelRubriekButton.setEnabled( false );
			saveRubriekButton.setEnabled( false );
		    }
		}
	    }
	}
	final ButtonActionListener buttonActionListener = new ButtonActionListener( );

	JPanel buttonPanel = new JPanel( );

	final JButton addRubriekButton = new JButton( "Add" );
	addRubriekButton.setActionCommand( "add" );
	addRubriekButton.addActionListener( buttonActionListener );
	buttonPanel.add( addRubriekButton );

	editRubriekButton.setActionCommand( "edit" );
	editRubriekButton.setEnabled( false );
	editRubriekButton.addActionListener( buttonActionListener );
	buttonPanel.add( editRubriekButton );

	cancelRubriekButton.setActionCommand( "cancel" );
	cancelRubriekButton.setEnabled( false );
	cancelRubriekButton.addActionListener( buttonActionListener );
	buttonPanel.add( cancelRubriekButton );

	saveRubriekButton.setActionCommand( "save" );
	saveRubriekButton.setEnabled( false );
	saveRubriekButton.addActionListener( buttonActionListener );
	buttonPanel.add( saveRubriekButton );

	deleteRubriekButton.setActionCommand( "delete" );
	deleteRubriekButton.setEnabled( false );
	deleteRubriekButton.addActionListener( buttonActionListener );
	buttonPanel.add( deleteRubriekButton );

	final JButton closeButton = new JButton( "Close" );
	closeButton.addActionListener( buttonActionListener );
	closeButton.setActionCommand( "close" );
	buttonPanel.add( closeButton );


	constraints.gridx = 0;
	constraints.gridy = 10;
	constraints.gridwidth = 4;
	constraints.anchor = GridBagConstraints.CENTER;
	container.add( buttonPanel, constraints );

	frame.setSize( 970, 500 );
	frame.setDefaultCloseOperation( JFrame.DISPOSE_ON_CLOSE );
	frame.setVisible( true );
    }
}
