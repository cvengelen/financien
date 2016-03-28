// Class to setup a TableModel for all records in rubriek

package financien.gui;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.ResultSet;
import java.sql.Statement;

import javax.swing.*;
import javax.swing.table.*;
import java.util.ArrayList;
import java.util.logging.*;
import java.util.regex.*;


public class RubriekTableModel extends AbstractTableModel {
    final Logger logger = Logger.getLogger( RubriekTableModel.class.getCanonicalName( ) );

    private Connection connection;
    private String[ ] headings = { "Id", "Rubriek", "Omschrijving", "Groep", "Deb/Cred" };

    class RubriekRecord {
	int	rubriekId;
	String  rubriekString;
	String  omschrijvingString;
	int	groepId;
	String  groepString;
	int	debCredId;
	String  debCredString;
	public RubriekRecord( int    rubriekId,
			      String rubriekString,
			      String omschrijvingString,
			      int    groepId,
			      String groepString,
			      int    debCredId,
			      String debCredString ) {
	    this.rubriekId = rubriekId;
	    this.rubriekString = rubriekString;
	    this.omschrijvingString = omschrijvingString;
	    this.groepId = groepId;
	    this.groepString = groepString;
	    this.debCredId = debCredId;
	    this.debCredString = debCredString;
	}

	// Copy constructor
	public RubriekRecord( RubriekRecord rubriekRecord ) {
	    this.rubriekId = rubriekRecord.rubriekId;
	    this.rubriekString = rubriekRecord.rubriekString;
	    this.omschrijvingString = rubriekRecord.omschrijvingString;
	    this.groepId = rubriekRecord.groepId;
	    this.groepString = rubriekRecord.groepString;
	    this.debCredId = rubriekRecord.debCredId;
	    this.debCredString = rubriekRecord.debCredString;
	}
    }

    ArrayList< RubriekRecord > rubriekRecordList = new ArrayList< >( 200 );

    JFrame frame = null;

    // Create groep combo box to get groe ID from groep string
    private GroepComboBox groepComboBox;

    // Create Deb/Cred combo box to get Deb/Cred ID from Deb/Cred string
    private DebCredComboBox debCredComboBox;

    // Pattern to find a single quote in the titel, to be replaced
    // with escaped quote (the double slashes are really necessary)
    final Pattern quotePattern = Pattern.compile( "\\'" );

    JButton cancelRubriekButton;
    JButton saveRubriekButton;

    boolean	   rowModified = false;
    int		   editRow = -1;
    RubriekRecord rubriekRecord = null;
    RubriekRecord originalRubriekRecord = null;


    // Constructor
    public RubriekTableModel( final Connection	connection,
			      final JFrame	frame,
			      final JButton	cancelRubriekButton,
			      final JButton	saveRubriekButton ) {
	this.connection = connection;
	this.frame = frame;
	this.cancelRubriekButton = cancelRubriekButton;
	this.saveRubriekButton = saveRubriekButton;

	// Create the combo boxes
	groepComboBox = new GroepComboBox( connection, 0 );
	debCredComboBox = new DebCredComboBox( connection, 0, null, false );

	setupRubriekTableModel( null );
    }

    public void setupRubriekTableModel( String rubriekFilterString ) {
	// Setup the table for the specified groep
	try {
	    String rubriekQueryString =
		"SELECT rubriek.rubriek_id, rubriek.rubriek, rubriek.omschrijving, " +
		"rubriek.groep_id, groep.groep, " +
		"rubriek.debcred_id, deb_cred.deb_cred " +
		"FROM rubriek " +
		"LEFT JOIN groep ON rubriek.groep_id = groep.groep_id " +
		"LEFT JOIN deb_cred ON rubriek.debcred_id = deb_cred.deb_cred_id ";

	    if ( ( rubriekFilterString != null ) && ( rubriekFilterString.length( ) > 0 ) ) {
		// Matcher to find single quotes in rubriekFilterString, in order to replace these
		// with escaped quotes (the quadruple slashes are really necessary)
		Matcher quoteMatcher = quotePattern.matcher( rubriekFilterString );
		rubriekQueryString += "WHERE rubriek.rubriek LIKE \"%" + quoteMatcher.replaceAll( "\\\\'" ) + "%\" ";
	    }

	    rubriekQueryString += "ORDER BY rubriek.rubriek_id";


	    Statement statement = connection.createStatement( );
	    ResultSet resultSet = statement.executeQuery( rubriekQueryString );

	    // Clear the list
	    rubriekRecordList.clear( );

	    // Add all query results to the list
	    while ( resultSet.next( ) ) {
		rubriekRecordList.add( new RubriekRecord( resultSet.getInt( 1 ),
							  resultSet.getString( 2 ),
							  resultSet.getString( 3 ),
							  resultSet.getInt( 4 ),
							  resultSet.getString( 5 ),
							  resultSet.getInt( 6 ),
							  resultSet.getString( 7 ) ) );
	    }

	    rubriekRecordList.trimToSize( );

	    // Trigger update of table data
	    fireTableDataChanged( );
	} catch ( SQLException sqlException ) {
	    logger.severe( "SQLException: " + sqlException.getMessage( ) );
	}
    }

    public int getRowCount( ) { return rubriekRecordList.size( ); }

    public int getColumnCount( ) { return 5; }

    // Indicate the class for each column for setting the correct default renderer
    // see file:///home/cvengelen/java/tutorial/uiswing/components/table.html
    public Class getColumnClass( int column ) {
	switch ( column ) {
	case 0: return Integer.class;
	}

	return String.class;
    }

    public boolean isCellEditable( int row, int column ) {
	// Only allow editing for the selected edit row
        return ( row == editRow );
    }

    public Object getValueAt( int row, int column ) {
	if ( ( row < 0 ) || ( row >= rubriekRecordList.size( ) ) ) {
	    logger.severe( "Invalid row: " + row );
	    return null;
	}

	final RubriekRecord rubriekRecord = rubriekRecordList.get( row );

	if ( column == 0 ) return rubriekRecord.rubriekId;
	if ( column == 1 ) return rubriekRecord.rubriekString;
	if ( column == 2 ) return rubriekRecord.omschrijvingString;
	if ( column == 3 ) return rubriekRecord.groepString;
	if ( column == 4 ) return rubriekRecord.debCredString;

	return "";
    }

    public void setValueAt( Object object, int row, int column ) {
	if ( ( row < 0 ) || ( row >= rubriekRecordList.size( ) ) ) {
	    logger.severe( "Invalid row: " + row );
	    return;
	}

	try {
	    switch ( column ) {
	    case 0:
		int intValue = ( Integer )object;
		if ( intValue != rubriekRecord.rubriekId ) {
		    rubriekRecord.rubriekId = intValue;
		    rowModified = true;
		}
		break;

	    case 1:
		String rubriekString = ( String )object;
		if ( ( rubriekString == null ) && ( rubriekRecord.rubriekString != null ) ) {
		    rubriekRecord.rubriekString = null;
		    rowModified = true;
		} else if ( ( rubriekString != null ) &&
			    ( !rubriekString.equals( rubriekRecord.rubriekString ) ) ) {
		    rubriekRecord.rubriekString = rubriekString;
		    rowModified = true;
		}
		break;

	    case 2:
		String omschrijvingString = ( String )object;
		if ( ( omschrijvingString == null ) && ( rubriekRecord.omschrijvingString != null ) ) {
		    rubriekRecord.omschrijvingString = null;
		    rowModified = true;
		} else if ( ( omschrijvingString != null ) &&
			    ( !omschrijvingString.equals( rubriekRecord.omschrijvingString ) ) ) {
		    rubriekRecord.omschrijvingString = omschrijvingString;
		    rowModified = true;
		}
		break;

	    case 3:
		int groepId = groepComboBox.getGroepId( ( String )object );
		if ( groepId != rubriekRecord.groepId ) {
		    rubriekRecord.groepId = groepId;
		    rubriekRecord.groepString = ( String )object;
		    rowModified = true;
		}
		break;

	    case 4:
		int debCredId = debCredComboBox.getDebCredId( ( String )object );
		if ( debCredId != rubriekRecord.debCredId ) {
		    rubriekRecord.debCredId = debCredId;
		    rubriekRecord.debCredString = ( String )object;
		    rowModified = true;
		}
		break;

	    default:
		logger.severe( "Invalid column: " + column );
		return;
	    }
	} catch ( Exception exception ) {
	    logger.severe( "could not get value from " +
			   object + " for column " + column + " in row " + row );
	    return;
	}

	// Store record in list
	rubriekRecordList.set( row, rubriekRecord );


	if ( rowModified ) {
	    // Enable cancel and save buttons
	    cancelRubriekButton.setEnabled( true );
	    saveRubriekButton.setEnabled( true );
	}

	fireTableCellUpdated( row, column );
    }


    public String getColumnName( int column ) {
	return headings[ column ];
    }


    public int getNumberOfRecords( ) { return rubriekRecordList.size( ); }


    public int getRubriekId( int row ) {
	if ( ( row < 0 ) || ( row >= rubriekRecordList.size( ) ) ) {
	    logger.severe( "Invalid row: " + row );
	    return 0;
	}

	return ( rubriekRecordList.get( row ) ).rubriekId;
    }


    public String getRubriekString( int row ) {
	if ( ( row < 0 ) || ( row >= rubriekRecordList.size( ) ) ) {
	    logger.severe( "Invalid row: " + row );
	    return null;
	}

	return ( rubriekRecordList.get( row ) ).rubriekString;
    }


    public void setEditRow( int editRow ) {
	// Initialize record to be edited
	rubriekRecord = ( RubriekRecord )rubriekRecordList.get( editRow );

	// Copy record to use as key in table update
	originalRubriekRecord = new RubriekRecord( rubriekRecord );

	// Initialize row modified status
	rowModified = false;

	// Allow editing for the selected row
	this.editRow = editRow;
    }

    public void unsetEditRow( ) {
	this.editRow = -1;
    }

    public void cancelEditRow( int row ) {
	// Check if row being canceled equals the row currently being edited
	if ( row != editRow ) return;

	// Check if row was modified
	if ( !rowModified ) return;

	// Initialize row modified status
	rowModified = false;

	// Store original record in list
	rubriekRecordList.set( row, originalRubriekRecord );

	// Trigger update of table row data
	fireTableRowUpdated( row );
    }

    private String addToUpdateString( String updateString, String additionalUpdateString ) {
	if ( updateString != null ) {
	    return updateString + ", " + additionalUpdateString;
	}
	return additionalUpdateString;
    }

    public boolean saveEditRow( int row ) {
	String updateString = null;

	// Compare each field with the value in the original record
	// If modified, add entry to update query string

	int rubriekId = rubriekRecord.rubriekId;
	if ( rubriekId != originalRubriekRecord.rubriekId ) {
	    // Check if new value already exists
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
		    return false;
		}
	    } catch ( SQLException sqlException ) {
		logger.severe( "SQLException: " + sqlException.getMessage( ) );
		return false;
	    }

	    // Confirmation dialog
	    int result = JOptionPane.showConfirmDialog( frame,
							"Replace rubriek ID " +
							originalRubriekRecord.rubriekId +
							" met ID " + rubriekId + " ?",
							"Replace Rubriek ID",
							JOptionPane.YES_NO_OPTION,
							JOptionPane.QUESTION_MESSAGE,
								null );
	    if ( result == JOptionPane.YES_OPTION ) {
		// Replace rubriek ID
		updateString = "rubriek_id = " + rubriekId;
	    }
	}

	String rubriekString = rubriekRecord.rubriekString;
	if ( ( ( rubriekString == null ) || ( rubriekString.length( ) == 0 ) ) &&
	     ( originalRubriekRecord.rubriekString != null ) &&
	     ( originalRubriekRecord.rubriekString.length( ) != 0 ) ) {
	    updateString = addToUpdateString( updateString, "rubriek = NULL " );
	} else if ( ( rubriekString != null ) &&
		    ( !rubriekString.equals( originalRubriekRecord.rubriekString ) ) ) {
	    // Matcher to find single quotes in rubriek, in order to replace these
	    // with escaped quotes (the quadruple slashes are really necessary)
	    final Matcher quoteMatcher = quotePattern.matcher( rubriekString );
	    updateString = addToUpdateString( updateString,
					      "rubriek = '" + quoteMatcher.replaceAll( "\\\\'" ) + "'" );
	}

	String omschrijvingString = rubriekRecord.omschrijvingString;
	if ( ( ( omschrijvingString == null ) || ( omschrijvingString.length( ) == 0 ) ) &&
	     ( originalRubriekRecord.omschrijvingString != null ) &&
	     ( originalRubriekRecord.omschrijvingString.length( ) != 0 ) ) {
	    updateString = addToUpdateString( updateString, "omschrijving = NULL " );
	} else if ( ( omschrijvingString != null ) &&
		    ( !omschrijvingString.equals( originalRubriekRecord.omschrijvingString ) ) ) {
	    // Matcher to find single quotes in omschrijving, in order to replace these
	    // with escaped quotes (the quadruple slashes are really necessary)
	    final Matcher quoteMatcher = quotePattern.matcher( omschrijvingString );
	    updateString = addToUpdateString( updateString,
					      "omschrijving = '" + quoteMatcher.replaceAll( "\\\\'" ) + "'" );
	}

	int groepId = rubriekRecord.groepId;
	if ( groepId != originalRubriekRecord.groepId ) {
	    updateString = addToUpdateString( updateString, "groep_id = " + groepId );
	}

	int debCredId = rubriekRecord.debCredId;
	if ( debCredId != originalRubriekRecord.debCredId ) {
	    updateString = addToUpdateString( updateString, "debCred_id = " + debCredId );
	}

	// Check if update is not necessary
	if ( updateString == null ) return true;

	// Complete the update string, using the saved rubriek ID as key
	updateString = ( "UPDATE rubriek SET " + updateString +
			 " WHERE rubriek_id = " + originalRubriekRecord.rubriekId );

	logger.info( "updateString: " + updateString );

	try {
	    Statement statement = connection.createStatement( );
	    int nUpdate = statement.executeUpdate( updateString );
	    if ( nUpdate != 1 ) {
	    	logger.severe( "Could not update record with rubriek_id " +
			       originalRubriekRecord.rubriekId +
			       " in rubriek, nUpdate = " + nUpdate );
	    	return false;
	    }

	    // Check if the rubriek ID has changed
	    if ( rubriekId != originalRubriekRecord.rubriekId ) {
		// Update all records in rekening_mutatie with the previous rubriekId
		updateString = ( "UPDATE rekening_mutatie SET rubriek_id = " + rubriekId +
				 " WHERE rubriek_id = " + originalRubriekRecord.rubriekId );
		statement = connection.createStatement( );
		int nUpdateRekeningMutatie = statement.executeUpdate( updateString );
		logger.info( "rekenening_mutatie records updated: " + nUpdateRekeningMutatie );

		// Update all records in deb_cred with the original rubriekId
		updateString = ( "UPDATE deb_cred SET rubriek_id = " + rubriekId +
				 " WHERE rubriek_id = " + originalRubriekRecord.rubriekId );
		statement = connection.createStatement( );
		int nUpdateDebCred = statement.executeUpdate( updateString );
		logger.info( "deb_cred records updated: " + nUpdateDebCred );

		JOptionPane.showMessageDialog( frame,
					       "Rubriek ID " + originalRubriekRecord.rubriekId +
					       " is veranderd in ID " + rubriekId +
					       "\nAantal records gemodificieerd in rekening_mutatie: " +
					       nUpdateRekeningMutatie +
					       "\nAantal records gemodificieerd in deb_cred: " +
					       nUpdateDebCred,
					       "Replace Rubriek ID",
					       JOptionPane.INFORMATION_MESSAGE );
	    }
	} catch ( SQLException sqlException ) {
	    logger.severe( "SQLException: " + sqlException.getMessage( ) );
	    return false;
	}

	// Store record in list
	// logger.info( "storing record at row " + row );
	rubriekRecordList.set( row, rubriekRecord );

	// Initialize row modified status
	rowModified = false;

	// Trigger update of table row data
	fireTableRowUpdated( row );

	// Successful completion
	return true;
    }

    private void fireTableRowUpdated( int row ) {
	for ( int column = 0; column < getColumnCount( ); column++ ) {
	    fireTableCellUpdated( row, column );
	}
    }

    public boolean getRowModified( ) { return rowModified; }
}
