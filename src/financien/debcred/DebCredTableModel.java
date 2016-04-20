// Class to setup a TableModel for all records in deb_cred

package financien.debcred;

import financien.gui.RubriekComboBox;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.ResultSet;
import java.sql.Statement;

import javax.swing.*;
import javax.swing.table.*;
import java.util.*;
import java.util.logging.*;
import java.util.regex.*;


class DebCredTableModel extends AbstractTableModel {
    private static final long serialVersionUID = 7782709968547613151L;
    final Logger logger = Logger.getLogger( DebCredTableModel.class.getCanonicalName() );

    private Connection connection;
    private String[ ] headings = { "Id", "Deb/Cred", "Omschrijving", "Rekening",
				   "Rubriek", "GT Omschr. Nr." };

    private class DebCredRecord {
	int	debCredId;
	String  debCredString;
	String  omschrijvingString;
	String  rekeningString;
	int	rubriekId;
	String  rubriekString;
	int	gtOmschrijvingNr;

	DebCredRecord( int    debCredId,
                       String debCredString,
                       String omschrijvingString,
                       String rekeningString,
                       int    rubriekId,
                       String rubriekString,
                       int    gtOmschrijvingNr ) {
	    this.debCredId = debCredId;
	    this.debCredString = debCredString;
	    this.omschrijvingString = omschrijvingString;
	    this.rekeningString = rekeningString;
	    this.rubriekId = rubriekId;
	    this.rubriekString = rubriekString;
	    this.gtOmschrijvingNr = gtOmschrijvingNr;
	}

	// Copy constructor
	DebCredRecord( DebCredRecord debCredRecord ) {
	    this.debCredId = debCredRecord.debCredId;
	    this.debCredString = debCredRecord.debCredString;
	    this.omschrijvingString = debCredRecord.omschrijvingString;
	    this.rekeningString = debCredRecord.rekeningString;
	    this.rubriekId = debCredRecord.rubriekId;
	    this.rubriekString = debCredRecord.rubriekString;
	    this.gtOmschrijvingNr = debCredRecord.gtOmschrijvingNr;
	}
    }

    private ArrayList< DebCredRecord > debCredRecordList = new ArrayList< >( 200 );

    // Create rubriek combo box to get rubriek ID from rubriek string
    private RubriekComboBox rubriekComboBox;

    private JButton cancelDebCredButton;
    private JButton saveDebCredButton;

    private boolean	  rowModified = false;
    private int		  editRow = -1;
    private DebCredRecord debCredRecord = null;
    private DebCredRecord originalDebCredRecord = null;

    // Pattern to find a single quote in the titel, to be replaced
    // with escaped quote (the double slashes are really necessary)
    private final Pattern quotePattern = Pattern.compile( "\\'" );


    // Constructor
    DebCredTableModel( Connection connection,
                       JButton    cancelDebCredButton,
                       JButton    saveDebCredButton ) {
	this.connection = connection;
	this.cancelDebCredButton = cancelDebCredButton;
	this.saveDebCredButton = saveDebCredButton;

	rubriekComboBox = new RubriekComboBox( connection, 0, false );
	setupDebCredTableModel( null );
    }

    void setupDebCredTableModel( String debCredFilterString ) {
	// Setup the table for the specified rekening
	try {
	    String debCredQueryString =
		"SELECT deb_cred.deb_cred_id, " +
		"deb_cred.deb_cred, deb_cred.omschrijving, deb_cred.rekening, " +
		"deb_cred.rubriek_id, rubriek.rubriek, deb_cred.omschrijving_nr " +
		"FROM deb_cred " +
		"LEFT JOIN rubriek ON rubriek.rubriek_id = deb_cred.rubriek_id ";

	    if ( ( debCredFilterString != null ) && ( debCredFilterString.length( ) > 0 ) ) {
		// Matcher to find single quotes in debCredFilterString, in order to replace these
		// with escaped quotes (the quadruple slashes are really necessary)
		Matcher quoteMatcher = quotePattern.matcher( debCredFilterString );
		debCredQueryString += "WHERE deb_cred.deb_cred LIKE \"%" + quoteMatcher.replaceAll( "\\\\'" ) + "%\" ";
	    }

	    debCredQueryString += "ORDER BY deb_cred.deb_cred, deb_cred.rekening";


	    Statement statement = connection.createStatement( );
	    ResultSet resultSet = statement.executeQuery( debCredQueryString );

	    // Clear the list
	    debCredRecordList.clear( );

	    // Add all query results to the list
	    while ( resultSet.next( ) ) {
		debCredRecordList.add( new DebCredRecord( resultSet.getInt( 1 ),
							  resultSet.getString( 2 ),
							  resultSet.getString( 3 ),
							  resultSet.getString( 4 ),
							  resultSet.getInt( 5 ),
							  resultSet.getString( 6 ),
							  resultSet.getInt( 7 ) ) );
	    }

	    debCredRecordList.trimToSize( );

	    // Trigger update of table data
	    fireTableDataChanged( );
	} catch ( SQLException sqlException ) {
	    logger.severe( "SQLException: " + sqlException.getMessage( ) );
	}
    }

    public int getRowCount( ) { return debCredRecordList.size( ); }

    public int getColumnCount( ) { return 6; }

    // Indicate the class for each column for setting the correct default renderer
    // see file:///home/cvengelen/java/tutorial/uiswing/components/table.html
    public Class getColumnClass( int column ) {
	switch ( column ) {
	case 0: // Id
	case 5:	// Nummer
	    return Integer.class;
	}

	return String.class;
    }

    public boolean isCellEditable( int row, int column ) {
	// Only allow editing for the selected edit row
	if ( row != editRow ) return false;

	switch ( column ) {
	case 0: // Id
	    // Do not allow editing
	    return false;
	}

	return true;
    }

    public Object getValueAt( int row, int column ) {
	if ( ( row < 0 ) || ( row >= debCredRecordList.size( ) ) ) {
	    logger.severe( "Invalid row: " + row );
	    return null;
	}

	final DebCredRecord debCredRecord = debCredRecordList.get( row );

	if ( column == 0 ) return debCredRecord.debCredId;
	if ( column == 1 ) return debCredRecord.debCredString;
	if ( column == 2 ) return debCredRecord.omschrijvingString;
	if ( column == 3 ) return debCredRecord.rekeningString;
	if ( column == 4 ) return debCredRecord.rubriekString;
	if ( column == 5 ) return debCredRecord.gtOmschrijvingNr;

	return "";
    }

    public void setValueAt( Object object, int row, int column ) {
	if ( ( row < 0 ) || ( row >= debCredRecordList.size( ) ) ) {
	    logger.severe( "Invalid row: " + row );
	    return;
	}

	try {
	    switch ( column ) {
	    case 1:
		String debCredString = ( String )object;
		if ( ( debCredString == null ) && ( debCredRecord.debCredString != null ) ) {
		    debCredRecord.debCredString = null;
		    rowModified = true;
		} else if ( ( debCredString != null ) &&
			    ( !debCredString.equals( debCredRecord.debCredString ) ) ) {
		    debCredRecord.debCredString = debCredString;
		    rowModified = true;
		}
		break;

	    case 2:
		String omschrijvingString = ( String )object;
		if ( ( omschrijvingString == null ) && ( debCredRecord.omschrijvingString != null ) ) {
		    debCredRecord.omschrijvingString = omschrijvingString;
		    rowModified = true;
		} else if ( ( omschrijvingString != null ) &&
			    ( !omschrijvingString.equals( debCredRecord.omschrijvingString ) ) ) {
		    debCredRecord.omschrijvingString = omschrijvingString;
		    rowModified = true;
		}
		break;

	    case 3:
		String rekeningString = ( String )object;
		if ( ( rekeningString == null ) && ( debCredRecord.rekeningString != null ) ) {
		    debCredRecord.rekeningString = rekeningString;
		    rowModified = true;
		} else if ( ( rekeningString != null ) &&
			    ( !rekeningString.equals( debCredRecord.rekeningString ) ) ) {
		    debCredRecord.rekeningString = rekeningString;
		    rowModified = true;
		}
		debCredRecord.rekeningString = rekeningString;
		break;

	    case 4:
		int rubriekId = rubriekComboBox.getRubriekId( ( String )object );
		if ( rubriekId != debCredRecord.rubriekId ) {
		    debCredRecord.rubriekId = rubriekId;
		    debCredRecord.rubriekString = ( String )object;
		    rowModified = true;
		}
		break;

	    case 5:
		int gtOmschrijvingNr = ( ( Integer )object ).intValue( );
		if ( gtOmschrijvingNr != debCredRecord.gtOmschrijvingNr ) {
		    debCredRecord.gtOmschrijvingNr = gtOmschrijvingNr;
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
	debCredRecordList.set( row, debCredRecord );

	if ( rowModified ) {
	    // Enable cancel and save buttons
	    cancelDebCredButton.setEnabled( true );
	    saveDebCredButton.setEnabled( true );
	}

	fireTableCellUpdated( row, column );
    }


    public String getColumnName( int column ) {
	return headings[ column ];
    }


    int getNumberOfRecords( ) { return debCredRecordList.size( ); }


    int getDebCredId( int row ) {
	if ( ( row < 0 ) || ( row >= debCredRecordList.size( ) ) ) {
	    logger.severe( "Invalid row: " + row );
	    return 0;
	}

	return ( debCredRecordList.get( row ) ).debCredId;
    }


    String getDebCredString( int row ) {
	if ( ( row < 0 ) || ( row >= debCredRecordList.size( ) ) ) {
	    logger.severe( "Invalid row: " + row );
	    return null;
	}

	return ( debCredRecordList.get( row ) ).debCredString;
    }


    void setEditRow( int editRow ) {
	// Initialize record to be edited
	debCredRecord = debCredRecordList.get( editRow );

	// Copy record to use as key in table update
	originalDebCredRecord = new DebCredRecord( debCredRecord );

	// Initialize row modified status
	rowModified = false;

	// Allow editing for the selected row
	this.editRow = editRow;
    }

    void unsetEditRow( ) {
	this.editRow = -1;
    }

    void cancelEditRow( int row ) {
	// Check if row being canceled equals the row currently being edited
	if ( row != editRow ) return;

	// Check if row was modified
	if ( !rowModified ) return;

	// Initialize row modified status
	rowModified = false;

	// Store original record in list
	debCredRecordList.set( row, originalDebCredRecord );

	// Trigger update of table row data
	fireTableRowUpdated( row );
    }

    private String addToUpdateString( String updateString, String additionalUpdateString ) {
	if ( updateString != null ) {
	    return updateString + ", " + additionalUpdateString;
	}
	return additionalUpdateString;
    }

    void saveEditRow( int row ) {
	String updateString = null;

	// Compare each field with the value in the original record
	// If modified, add entry to update query string

	String debCredString = debCredRecord.debCredString;
	if ( ( ( debCredString == null ) || ( debCredString.length( ) == 0 ) ) &&
	     ( originalDebCredRecord.debCredString != null ) &&
	     ( originalDebCredRecord.debCredString.length( ) != 0 ) ) {
	    updateString = "deb_cred = NULL ";
	} else if ( ( debCredString != null ) &&
		    ( !debCredString.equals( originalDebCredRecord.debCredString ) ) ) {
	    // Matcher to find single quotes in debCred, in order to replace these
	    // with escaped quotes (the quadruple slashes are really necessary)
	    final Matcher quoteMatcher = quotePattern.matcher( debCredString );
	    originalDebCredRecord.debCredString = debCredString;
	    updateString = "deb_cred = '" + quoteMatcher.replaceAll( "\\\\'" ) + "'";
	}

	String omschrijvingString = debCredRecord.omschrijvingString;
	if ( ( ( omschrijvingString == null ) || ( omschrijvingString.length( ) == 0 ) ) &&
	     ( originalDebCredRecord.omschrijvingString != null ) &&
	     ( originalDebCredRecord.omschrijvingString.length( ) != 0 ) ) {
	    updateString = addToUpdateString( updateString, "omschrijving = NULL " );
	} else if ( ( omschrijvingString != null ) &&
		    ( !omschrijvingString.equals( originalDebCredRecord.omschrijvingString ) ) ) {
	    // Matcher to find single quotes in omschrijving, in order to replace these
	    // with escaped quotes (the quadruple slashes are really necessary)
	    final Matcher quoteMatcher = quotePattern.matcher( omschrijvingString );
	    updateString = addToUpdateString( updateString,
					      "omschrijving = '" + quoteMatcher.replaceAll( "\\\\'" ) + "'" );
	}

	String rekeningString = debCredRecord.rekeningString;
	if ( ( ( rekeningString == null ) || ( rekeningString.length( ) == 0 ) ) &&
	     ( originalDebCredRecord.rekeningString != null ) &&
	     ( originalDebCredRecord.rekeningString.length( ) != 0 ) ) {
	    updateString = addToUpdateString( updateString, "rekening = NULL " );
	} else if ( ( rekeningString != null ) &&
		    ( !rekeningString.equals( originalDebCredRecord.rekeningString ) ) ) {
	    updateString = addToUpdateString( updateString, "rekening = '" + rekeningString + "'" );
	}

	if ( debCredRecord.rubriekId != originalDebCredRecord.rubriekId ) {
	    updateString = addToUpdateString( updateString, "rubriek_id = " + debCredRecord.rubriekId );
	}

	if ( debCredRecord.gtOmschrijvingNr != originalDebCredRecord.gtOmschrijvingNr ) {
	    updateString = addToUpdateString( updateString,
					      "omschrijving_nr = " + debCredRecord.gtOmschrijvingNr );
	}

	// Check if update is not necessary
	if ( updateString == null ) return;

	updateString = ( "UPDATE deb_cred SET " + updateString +
			 " WHERE deb_cred_id = " + originalDebCredRecord.debCredId );

	logger.info( "updateString: " + updateString );

	try {
	    Statement statement = connection.createStatement( );
	    int nUpdate = statement.executeUpdate( updateString );
	    if ( nUpdate != 1 ) {
	    	logger.severe( "Could not update record with deb_cred_id " +
			       originalDebCredRecord.debCredId +
			       " in deb_cred, nUpdate = " + nUpdate );
	    	return;
	    }
	} catch ( SQLException sqlException ) {
	    logger.severe( "SQLException: " + sqlException.getMessage( ) );
	    return;
	}

	// Store record in list
	// logger.info( "storing record at row " + row );
	debCredRecordList.set( row, debCredRecord );

	// Initialize row modified status
	rowModified = false;

	// Trigger update of table row data
	fireTableRowUpdated( row );
    }

    private void fireTableRowUpdated( int row ) {
	for ( int column = 0; column < getColumnCount( ); column++ ) {
	    fireTableCellUpdated( row, column );
	}
    }

    boolean getRowModified( ) { return rowModified; }
}
