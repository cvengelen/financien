// frame to update a record in deb_cred

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


public class DebCredFrame {
    final private Logger logger = Logger.getLogger( "financien.gui.DebCredFrame" );

    Connection connection;

    final JFrame frame = new JFrame( "DebCred" );

    DebCredTableModel debCredTableModel;
    TableSorter debCredTableSorter;
    JTable debCredTable;
    
    JTextField debCredFilterTextField;


    public DebCredFrame( final Connection connection ) {
	this.connection = connection;

	final Container container = frame.getContentPane( );

	// Set grid bag layout manager
	container.setLayout( new GridBagLayout( ) );
	GridBagConstraints constraints = new GridBagConstraints( );
	constraints.anchor = GridBagConstraints.WEST;
	constraints.insets = new Insets( 0, 0, 10, 10 );


	/////////////////////////////////
	// Deb/Cred filter action listener
	/////////////////////////////////

	class DebCredFilterActionListener implements ActionListener {
	    public void actionPerformed( ActionEvent actionEvent ) {
		// Setup the Deb/Cred table
		debCredTableModel.setupDebCredTableModel( debCredFilterTextField.getText( ) );
	    }
	}

	/////////////////////////////////
	// Deb/Cred filter string
	/////////////////////////////////

	constraints.gridx = 0;
	constraints.gridy = 0;
	constraints.anchor = GridBagConstraints.EAST;
	constraints.gridwidth = 1;
	container.add( new JLabel( "Deb/Cred Filter:" ), constraints );

	debCredFilterTextField = new JTextField( 20 );
	debCredFilterTextField.addActionListener( new DebCredFilterActionListener( ) );
	constraints.gridx = GridBagConstraints.RELATIVE;
	constraints.anchor = GridBagConstraints.WEST;
	container.add( debCredFilterTextField, constraints );


	// Define the edit, cancel, save and delete buttons
	// These are enabled/disabled by the table model and the list selection listener.
	final JButton editDebCredButton = new JButton( "Edit" );
	final JButton cancelDebCredButton = new JButton( "Cancel" );
	final JButton saveDebCredButton = new JButton( "Save" );
	final JButton deleteDebCredButton = new JButton( "Delete" );

	// Create table from deb_cred table model
	debCredTableModel = new DebCredTableModel( connection,
						   cancelDebCredButton,
						   saveDebCredButton );
	debCredTableSorter = new TableSorter( debCredTableModel );
	debCredTable = new JTable( debCredTableSorter );
	debCredTableSorter.setTableHeader( debCredTable.getTableHeader( ) );

	debCredTable.setAutoResizeMode( JTable.AUTO_RESIZE_OFF );

	debCredTable.getColumnModel( ).getColumn( 0 ).setPreferredWidth(  50 );  // Deb/Cred ID
	debCredTable.getColumnModel( ).getColumn( 1 ).setPreferredWidth( 250 );  // Deb/Cred
	debCredTable.getColumnModel( ).getColumn( 2 ).setPreferredWidth( 300 );  // omschrijving
	debCredTable.getColumnModel( ).getColumn( 3 ).setPreferredWidth( 100 );  // rekening
	debCredTable.getColumnModel( ).getColumn( 4 ).setPreferredWidth( 150 );  // rubriek
	debCredTable.getColumnModel( ).getColumn( 5 ).setPreferredWidth(  50 );  // girotel text deel

	final DefaultCellEditor rubriekDefaultCellEditor =
	    new DefaultCellEditor( new RubriekComboBox( connection, 0, false ) );
	debCredTable.getColumnModel( ).getColumn( 4 ).setCellEditor( rubriekDefaultCellEditor );

	// Set vertical size just enough for 20 entries
	debCredTable.setPreferredScrollableViewportSize( new Dimension( 900, 320 ) );

	constraints.gridx = 0;
	constraints.gridy = 3;
	constraints.gridwidth = 2;
	container.add( new JScrollPane( debCredTable ), constraints );


	////////////////////////////////////////////////
	// List selection listener
	////////////////////////////////////////////////

	// Get the selection model related to the deb_cred table
	final ListSelectionModel debCredListSelectionModel = debCredTable.getSelectionModel( );

	class DebCredListSelectionListener implements ListSelectionListener {
	    int selectedRow = -1;

	    public void valueChanged( ListSelectionEvent listSelectionEvent ) {
		// Ignore extra messages.
		if ( listSelectionEvent.getValueIsAdjusting( ) ) return;

		// Check if current row has modified values
		if ( debCredTableModel.getRowModified( ) ) {
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
			    debCredTableModel.saveEditRow( selectedRow );
			} else {
			    // Cancel any edits in the selected row
			    debCredTableModel.cancelEditRow( selectedRow );
			}
		    }
		}

		// Ignore if nothing is selected
		if ( debCredListSelectionModel.isSelectionEmpty( ) ) {
		    selectedRow = -1;
		    editDebCredButton.setEnabled( false );
		    cancelDebCredButton.setEnabled( false );
		    saveDebCredButton.setEnabled( false );
		    deleteDebCredButton.setEnabled( false );
		    return;
		}

		// Remove the capability to edit the row
		debCredTableModel.unsetEditRow( );

		// Get the selected row
		int viewRow = debCredListSelectionModel.getMinSelectionIndex( );
		selectedRow = debCredTableSorter.modelIndex( viewRow );

		// Enable the edit button
		editDebCredButton.setEnabled( true );

		// Disable the cancel and save buttons (these will be enabled
		// when any data in the row is actually modified)
		cancelDebCredButton.setEnabled( false );
		saveDebCredButton.setEnabled( false );

		// Enable the delete button
		deleteDebCredButton.setEnabled( true );
	    }

	    public int getSelectedRow ( ) { return selectedRow; }
	}

	// Add debCredListSelectionListener object to the selection model of the deb_cred table
	final DebCredListSelectionListener debCredListSelectionListener = new DebCredListSelectionListener( );
	debCredListSelectionModel.addListSelectionListener( debCredListSelectionListener );


	////////////////////////////////////////////////
	// Add, Edit, Cancel, Save, Delete, Close Buttons
	////////////////////////////////////////////////

	// Class to handle button actions: uses debCredListSelectionListener
	class ButtonActionListener implements ActionListener {
	    public void actionPerformed( ActionEvent actionEvent ) {
		if ( actionEvent.getActionCommand( ).equals( "close" ) ) {
		    frame.setVisible( false );
		    System.exit( 0 );
		} else if ( actionEvent.getActionCommand( ).equals( "add" ) ) {
		    try {
			Statement statement = connection.createStatement( );
			ResultSet resultSet = statement.executeQuery( "SELECT MAX( deb_cred_id ) FROM deb_cred" );
			if ( ! resultSet.next( ) ) {
			    logger.severe( "Could not get maximum for deb_cred_id in deb_cred" );
			    return;
			}
			int debCredId = resultSet.getInt( 1 ) + 1;
			String insertString = "INSERT INTO deb_cred SET deb_cred_id = " + debCredId;

			logger.info( "insertString: " + insertString );
			if ( statement.executeUpdate( insertString ) != 1 ) {
			    logger.severe( "Could not insert in deb_cred" );
			    return;
			}
		    } catch ( SQLException ex ) {
			logger.severe( "SQLException: " + ex.getMessage( ) );
			return;
		    }

		    // Records may have been modified: setup the table model again
		    debCredTableModel.setupDebCredTableModel( debCredFilterTextField.getText( ) );
		} else {
		    int selectedRow = debCredListSelectionListener.getSelectedRow( );
		    if ( selectedRow < 0 ) {
			JOptionPane.showMessageDialog( frame,
						       "Geen Deb/Cred geselecteerd",
						       "DebCred frame error",
						       JOptionPane.ERROR_MESSAGE );
			return;
		    }

		    final int debCredId = debCredTableModel.getDebCredId( selectedRow );
		    final String debCredString = debCredTableModel.getDebCredString( selectedRow );

		    if ( actionEvent.getActionCommand( ).equals( "delete" ) ) {
			// Check if Deb/Cred is used in table rekening_mutatie
			try {
			    Statement statement = connection.createStatement( );
			    String rekeningMutatieQueryString =
				"SELECT deb_cred_id FROM rekening_mutatie WHERE deb_cred_id = " + debCredId;
			    ResultSet resultSet = statement.executeQuery( rekeningMutatieQueryString );
			    if ( resultSet.next( ) ) {
				JOptionPane.showMessageDialog( frame,
							       "Tabel rekening_mutatie gebruikt Deb/Cred " +
							       debCredString,
							       "Deb/Cred delete record error",
							       JOptionPane.ERROR_MESSAGE );
				return;
			    }
			} catch ( SQLException sqlException ) {
			    logger.severe( "SQLException: " + sqlException.getMessage( ) );
			    return;
			}

			int result =
			    JOptionPane.showConfirmDialog( frame,
							   "Delete record for rekening " +
							   debCredTableModel.getDebCredString( selectedRow ) +
							   " in deb_cred ?",
							   "Delete Deb/Cred record",
							   JOptionPane.YES_NO_OPTION,
							   JOptionPane.QUESTION_MESSAGE,
							   null );

			if ( result != JOptionPane.YES_OPTION ) return;

			String deleteString  = "DELETE FROM deb_cred";
			deleteString += " WHERE deb_cred_id = " + debCredId;

			logger.info( "deleteString: " + deleteString );

			try {
			    Statement statement = connection.createStatement( );
			    int nUpdate = statement.executeUpdate( deleteString );
			    if ( nUpdate != 1 ) {
				String errorString = ( "Could not delete record with deb_cred_id  = " +
						       debCredId + " in deb_cred" );
				JOptionPane.showMessageDialog( frame,
							       errorString,
							       "Delete Deb/Cred record",
							       JOptionPane.ERROR_MESSAGE);
				logger.severe( errorString );
				return;
			    }
			} catch ( SQLException sqlException ) {
			    logger.severe( "SQLException: " + sqlException.getMessage( ) );
			    return;
			}

			// Records may have been modified: setup the table model again
			debCredTableModel.setupDebCredTableModel( debCredFilterTextField.getText( ) );
		    } else if ( actionEvent.getActionCommand( ).equals( "edit" ) ) {
			// Allow to edit the selected row
			debCredTableModel.setEditRow( selectedRow );

			// Disable the edit button
			editDebCredButton.setEnabled( false );
		    } else if ( actionEvent.getActionCommand( ).equals( "cancel" ) ) {
			// Cancel any edits in the selected row
			debCredTableModel.cancelEditRow( selectedRow );

			// Remove the capability to edit the row
			debCredTableModel.unsetEditRow( );

			// Enable the edit button, so that the user can select edit again
			editDebCredButton.setEnabled( true );

			// Disable the cancel and save buttons
			cancelDebCredButton.setEnabled( false );
			saveDebCredButton.setEnabled( false );
		    } else if ( actionEvent.getActionCommand( ).equals( "save" ) ) {
			// Save the changes in the table model, and in the database
			debCredTableModel.saveEditRow( selectedRow );

			// Remove the capability to edit the row
			debCredTableModel.unsetEditRow( );

			// Enable the edit button, so that the user can select edit again
			editDebCredButton.setEnabled( true );

			// Disable the cancel and save buttons
			cancelDebCredButton.setEnabled( false );
			saveDebCredButton.setEnabled( false );
		    }
		}
	    }
	}
	final ButtonActionListener buttonActionListener = new ButtonActionListener( );

	JPanel buttonPanel = new JPanel( );

	final JButton addDebCredButton = new JButton( "add" );
	addDebCredButton.setActionCommand( "add" );
	addDebCredButton.addActionListener( buttonActionListener );
	buttonPanel.add( addDebCredButton );

	editDebCredButton.setActionCommand( "edit" );
	editDebCredButton.setEnabled( false );
	editDebCredButton.addActionListener( buttonActionListener );
	buttonPanel.add( editDebCredButton );

	cancelDebCredButton.setActionCommand( "cancel" );
	cancelDebCredButton.setEnabled( false );
	cancelDebCredButton.addActionListener( buttonActionListener );
	buttonPanel.add( cancelDebCredButton );

	saveDebCredButton.setActionCommand( "save" );
	saveDebCredButton.setEnabled( false );
	saveDebCredButton.addActionListener( buttonActionListener );
	buttonPanel.add( saveDebCredButton );

	deleteDebCredButton.setActionCommand( "delete" );
	deleteDebCredButton.setEnabled( false );
	deleteDebCredButton.addActionListener( buttonActionListener );
	buttonPanel.add( deleteDebCredButton );

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
