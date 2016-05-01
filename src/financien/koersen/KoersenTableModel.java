package financien.koersen;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.ResultSet;
import java.sql.Statement;

import javax.swing.*;
import javax.swing.table.*;
import java.util.*;
import java.util.logging.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * TableModel for records in koersen
 */
class KoersenTableModel extends AbstractTableModel {
    private final Logger logger = Logger.getLogger( KoersenTableModel.class.getCanonicalName( ) );
    private Connection connection;
    private final String[ ] headings = { "Datum", "AEX", "Opmerkingen" };
    private final ArrayList< String > fondsenList = new ArrayList<>( 20 );

    private class KoersenRecord {
	String    datumString;
	int       aexIndex;
	String    opmerkingen;
	ArrayList< Double > koersenList;

	KoersenRecord( String  datumString,
			      int     aexIndex,
			      String  opmerkingen ) {
	    this.datumString = datumString;
	    this.aexIndex = aexIndex;
	    this.opmerkingen = opmerkingen;

	    koersenList = new ArrayList<>( 20 );
	}

	// Copy constructor
	KoersenRecord( KoersenRecord koersenRecord ) {
	    this.datumString = koersenRecord.datumString;
	    this.aexIndex = koersenRecord.aexIndex;
	    this.opmerkingen = koersenRecord.opmerkingen;

	    koersenList = new ArrayList<>( 20 );
            koersenList.addAll(koersenRecord.koersenList);
	}
    }
    private final ArrayList< KoersenRecord > koersenRecordList = new ArrayList<>( 300 );

    private JButton cancelKoersenButton;
    private JButton saveKoersenButton;

    private boolean	  rowModified = false;
    private int		  editRow = -1;
    private KoersenRecord koersenRecord = null;
    private KoersenRecord originalKoersenRecord = null;

    // Pattern to find a single quote in the titel, to be replaced
    // with escaped quote (the double slashes are really necessary)
    private static final Pattern quotePattern = Pattern.compile( "\\'" );

    // Constructor
    KoersenTableModel( Connection connection,
                       JButton    cancelKoersenButton,
                       JButton    saveKoersenButton ) {
	this.connection = connection;
	this.cancelKoersenButton = cancelKoersenButton;
	this.saveKoersenButton = saveKoersenButton;

	setupKoersenTableModel( );
    }

    void setupKoersenTableModel( ) {
	// Setup the table
	try {
	    Statement statement = connection.createStatement( );
	    String rekeningQueryString =
		"SELECT fonds FROM rekening WHERE " +
		"( type_id = 4 OR type_id = 5 OR type_id = 7 ) AND ( aktief != 0 ) AND " +
		"( NOT ISNULL( fonds ) ) ORDER BY fonds";
	    ResultSet resultSet = statement.executeQuery( rekeningQueryString );

	    // Clear the list
	    fondsenList.clear( );

	    // Add all query results to the list
	    while ( resultSet.next( ) ) {
		String fondsString = resultSet.getString( 1 );
		if ( fondsString.length( ) > 0 ) {
		    fondsenList.add( fondsString );
		}
	    }

	    String koersenQueryString = "SELECT datum, aex_index, opmerkingen";
            for (String fonds: fondsenList) {
		koersenQueryString += ", " + fonds;
	    }
	    koersenQueryString += " FROM koersen ORDER BY datum DESC";

	    statement = connection.createStatement( );
	    resultSet = statement.executeQuery( koersenQueryString );

	    // Clear the list
	    koersenRecordList.clear( );

	    // Add all query results to the list
	    while ( resultSet.next( ) ) {
		KoersenRecord koersenRecord =
		    new KoersenRecord( resultSet.getString( 1 ),
				       resultSet.getInt( 2 ),
				       resultSet.getString( 3 ) );

		int resultSetIndex = 4;
                Iterator< String > fondsenListIterator = fondsenList.iterator( );
		while ( fondsenListIterator.hasNext(  ) ) {
		    koersenRecord.koersenList.add( resultSet.getDouble( resultSetIndex++ ) );
		    fondsenListIterator.next(  );
		}
		koersenRecord.koersenList.trimToSize( );
		koersenRecordList.add( koersenRecord );
	    }
	    koersenRecordList.trimToSize( );
            logger.info("Table shows " + koersenRecordList.size() + " koersen records");

	    // Trigger update of table data
	    fireTableDataChanged( );

	    // Cleanup the result set
	    resultSet.close( );
	} catch ( SQLException sqlException ) {
	    logger.severe( "SQLException: " + sqlException.getMessage( ) );
	}
    }

    public int getRowCount( ) { return koersenRecordList.size( ); }

    public int getColumnCount( ) { return 3 + fondsenList.size( ); }

    // Indicate the class for each column for setting the correct default renderer
    // see file:///home/cvengelen/java/tutorial/uiswing/components/table.html
    public Class getColumnClass( int column ) {
	switch ( column ) {
	case 0:	// datum
	    return String.class;

	case 1: // AEX index
	    return Integer.class;

        case 2: // opmerkingen
            return String.class;
        }

        return Double.class;
    }

    public boolean isCellEditable( int row, int column ) {
	// Only allow editing for the selected edit row
	return ( row == editRow );
    }

    public Object getValueAt( int row, int column ) {
	if ( ( row < 0 ) || ( row >= koersenRecordList.size( ) ) ) {
	    logger.severe( "Invalid row: " + row );
	    return null;
	}

	final KoersenRecord koersenRecord = koersenRecordList.get( row );

	if ( column ==  0 ) return koersenRecord.datumString;
	if ( column ==  1 ) return koersenRecord.aexIndex;
	if ( column ==  2 ) return koersenRecord.opmerkingen;

	if ( column < fondsenList.size( ) + 3 ) {
	    return koersenRecord.koersenList.get( column - 3 );
	}

	return "";
    }

    public void setValueAt( Object object, int row, int column ) {
	if ( ( row < 0 ) || ( row >= koersenRecordList.size( ) ) ) {
	    logger.severe( "Invalid row: " + row );
	    return;
	}

	try {
	    switch ( column ) {
	    case 0:
		String datumString = ( String )object;
		if ( ( datumString == null ) || ( datumString.length( ) == 0 ) ) return;
		if ( !datumString.equals( koersenRecord.datumString ) ) {
		    koersenRecord.datumString = datumString;
		    rowModified = true;
		}
		break;

	    case 1:
		int aexIndex =  ( Integer )object;
		if ( aexIndex != koersenRecord.aexIndex ) {
		    koersenRecord.aexIndex = aexIndex;
		    rowModified = true;
		}
		break;

	    case 2:
		String opmerkingen =( String )object;
		if ( !( opmerkingen.equals(koersenRecord.opmerkingen) ) ) {
		    koersenRecord.opmerkingen = opmerkingen;
		    rowModified = true;
		}
		break;

	    default:
		double koersFromTable = ( Double )object;
		double koersFromRecord = koersenRecord.koersenList.get( column - 3 );
		if ( koersFromTable != koersFromRecord ) {
		    koersenRecord.koersenList.set( column - 3, ( Double )object );
		    rowModified = true;
		}
		break;
	    }
	} catch ( Exception exception ) {
	    logger.severe( "could not get value from " +
			   object + " for column " + column + " in row " + row );
	    return;
	}

	// Store record in list
	koersenRecordList.set( row, koersenRecord );

	if ( rowModified ) {
	    // Enable cancel and save buttons
	    cancelKoersenButton.setEnabled( true );
	    saveKoersenButton.setEnabled( true );
	}

	fireTableCellUpdated( row, column );
    }

    public String getColumnName( int column ) {
	if ( column < 3 ) return headings[ column ];

	if ( column < fondsenList.size( ) + 3 ) {
	    return fondsenList.get( column - 3 );
	}

	return "";
    }

    String getDatumString( int row ) {
	if ( ( row < 0 ) || ( row >= koersenRecordList.size( ) ) ) {
	    logger.severe( "Invalid row: " + row );
	    return null;
	}

	return koersenRecordList.get( row ).datumString;
    }

    void setEditRow( int editRow ) {
	// Initialize record to be edited
	koersenRecord = koersenRecordList.get( editRow );

	// Copy record to use as key in table update
	originalKoersenRecord = new KoersenRecord( koersenRecord );

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
	koersenRecordList.set( row, originalKoersenRecord );

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

	String datumString = koersenRecord.datumString;
	if ( ( datumString == null ) || ( datumString.length( ) == 0 ) ) {
	    logger.severe( "Invalid empty datum" );
	    return;
	}
	if ( !datumString.equals( originalKoersenRecord.datumString ) ) {
	    updateString = "datum = '" + datumString + "'";
	}

	int aexIndex = koersenRecord.aexIndex;
	if ( aexIndex != originalKoersenRecord.aexIndex ) {
	    updateString = addToUpdateString( updateString,
					      "aex_index = " + aexIndex );
	}

	String opmerkingen = koersenRecord.opmerkingen;
	if ( !( opmerkingen.equals( originalKoersenRecord.opmerkingen ) ) ) {
            final Matcher quoteMatcher = quotePattern.matcher( opmerkingen );
	    updateString = addToUpdateString( updateString,
					      "opmerkingen = '" + quoteMatcher.replaceAll( "\\\\'" ) + "'" );
	}

	for ( int koersenListIndex = 0;
	      koersenListIndex < koersenRecord.koersenList.size( );
	      koersenListIndex++ ) {
	    double koers         = koersenRecord.koersenList.get( koersenListIndex );
	    double originalKoers = originalKoersenRecord.koersenList.get( koersenListIndex );
	    if ( koers != originalKoers ) {
		String fondsString = fondsenList.get( koersenListIndex );
		updateString = addToUpdateString( updateString,
						  fondsString + " = " + koers );
	    }
	}

	// Check if update is not necessary
	if ( updateString == null ) return;

	updateString =
	    "UPDATE koersen SET " + updateString +
	    " WHERE datum = '" + originalKoersenRecord.datumString + "'";
	logger.info( "updateString: " + updateString );

	try {
	    Statement statement = connection.createStatement( );
	    int nUpdate = statement.executeUpdate( updateString );
	    if ( nUpdate != 1 ) {
	    	logger.severe( "Could not update record with datum " + originalKoersenRecord.datumString +
			       " in koersen, nUpdate = " + nUpdate );
	    	return;
	    }
	} catch ( SQLException sqlException ) {
	    logger.severe( "SQLException: " + sqlException.getMessage( ) );
	    return;
	}

	// Store record in list
	// logger.info( "storing record at row " + row );
	koersenRecordList.set( row, koersenRecord );

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
