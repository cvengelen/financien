// Class to setup a TableModel for all records in rekening_mutatie for a specific rubriek

package financien.rekeningmutatiesrubriek;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.ResultSet;
import java.sql.Statement;

import javax.swing.*;
import javax.swing.table.*;
import java.util.*;
import java.util.logging.*;

/**
 * Setup a TableModel for all records in rekening_mutatie for a specific rubriek
 */
class RekeningMutatiesRubriekTableModel extends AbstractTableModel {
    private final Logger logger = Logger.getLogger( RekeningMutatiesRubriekTableModel.class.getCanonicalName() );

    private Connection connection;
    private JFrame parentFrame;

    private final String[ ] headings = { "Datum", "Deb/Cred",
				         "Rekening", "In", "Uit", "Nr",
				         "Jaar", "Maand", "Omschrijving" };

    // Jaar, maand and volgNummer are stored as Integer for correct default table renderer.
    private class RekeningMutatieRubriekRecord {

	String  datumString;
	int     debCredId;
	String  debCredString;
	int     rekeningId;
	String  rekeningString;
        int     rekeningHouderId;
	int     rekeningTypeId;
	Double  mutatieInDouble;
	Double  mutatieUitDouble;
	Integer volgNummerInteger;
	Integer jaarInteger;
	Integer maandInteger;
	String  omschrijvingString;
	RekeningMutatieRubriekRecord( String  datumString,
                                      int     debCredId,
                                      String  debCredString,
                                      int     rekeningId,
                                      String  rekeningString,
                                      int     rekeningHouderId,
                                      int     rekeningTypeId,
                                      Double  mutatieInDouble,
                                      Double  mutatieUitDouble,
                                      Integer volgNummerInteger,
                                      Integer jaarInteger,
                                      Integer maandInteger,
                                      String  omschrijvingString ) {
	    this.datumString = datumString;
	    this.debCredId = debCredId;
	    this.debCredString = debCredString;
	    this.rekeningId = rekeningId;
	    this.rekeningString = rekeningString;
            this.rekeningHouderId = rekeningHouderId;
	    this.rekeningTypeId = rekeningTypeId;
	    this.mutatieInDouble = mutatieInDouble;
	    this.mutatieUitDouble = mutatieUitDouble;
	    this.volgNummerInteger = volgNummerInteger;
	    this.jaarInteger = jaarInteger;
	    this.maandInteger = maandInteger;
	    this.omschrijvingString = omschrijvingString;
	}
    }

    private final ArrayList< RekeningMutatieRubriekRecord > rekeningMutatieRubriekRecordList = new ArrayList< >( 100 );

    private int rubriekId = 0;


    // Constructor
    RekeningMutatiesRubriekTableModel( Connection connection, JFrame parentFrame) {
        this.connection = connection;
        this.parentFrame = parentFrame;
    }

    void setupRekeningMutatieRubriekTableModel( int rubriekId ) {
	this.rubriekId = rubriekId;

	// Setup the table for the specified rubriek
	try {
	    String rekeningMutatieQueryString =
		"SELECT rekening_mutatie.datum, " +
		"rekening_mutatie.deb_cred_id, deb_cred.deb_cred, " +
		"rekening_mutatie.rekening_id, rekening.rekening, rekening_houder_id, rekening.type_id, " +
		"mutatie_in, mutatie_uit, volgnummer, jaar, maand, " +
		"rekening_mutatie.omschrijving " +
		"FROM rekening_mutatie " +
		"LEFT JOIN deb_cred ON deb_cred.deb_cred_id = rekening_mutatie.deb_cred_id " +
		"LEFT JOIN rekening ON rekening.rekening_id = rekening_mutatie.rekening_id " +
		"WHERE rekening_mutatie.rubriek_id = " + rubriekId + " " +
		"ORDER BY rekening_mutatie.datum DESC, rekening_mutatie.rekening_id, rekening_mutatie.volgnummer";

	    Statement statement = connection.createStatement( );
	    ResultSet resultSet = statement.executeQuery( rekeningMutatieQueryString );

	    // Clear the list
	    rekeningMutatieRubriekRecordList.clear( );

	    // Add all query results to the list
	    while ( resultSet.next( ) ) {
		rekeningMutatieRubriekRecordList.add( new RekeningMutatieRubriekRecord( resultSet.getString( 1 ),
											resultSet.getInt( 2 ),
											resultSet.getString( 3 ),
											resultSet.getInt( 4 ),
											resultSet.getString( 5 ),
											resultSet.getInt( 6 ),
                                                                                        resultSet.getInt( 7),
											resultSet.getDouble( 8 ),
											resultSet.getDouble( 9 ),
											resultSet.getInt( 10 ),
											resultSet.getInt( 11 ),
											resultSet.getInt( 12 ),
											resultSet.getString( 13 ) ) );
	    }

	    rekeningMutatieRubriekRecordList.trimToSize( );
            logger.info("Table shows " + rekeningMutatieRubriekRecordList.size() + " rekening_mutatie records");

	    // Trigger update of table data
	    fireTableDataChanged( );
	} catch ( SQLException sqlException ) {
            JOptionPane.showMessageDialog( parentFrame,
                    sqlException.getMessage( ),
                    "Rekening mutatie rubriek SQL exception",
                    JOptionPane.ERROR_MESSAGE);
	    logger.severe( "SQLException: " + sqlException.getMessage( ) );
	}
    }

    public int getRowCount( ) { return rekeningMutatieRubriekRecordList.size( ); }

    public int getColumnCount( ) { return 9; }

    // Indicate the class for each column for setting the correct default renderer
    // see file:///home/cvengelen/java/tutorial/uiswing/components/table.html
    public Class getColumnClass( int column ) {
	switch ( column ) {
	case 0:	// datum
	case 1:	// debCred
	case 2: // rekening
	case 8: // omschrijving
	    return String.class;

	case 3: // mutatieIn
	case 4: // mutatieUit
	    return Double.class;
	}

	// 5: volgNummer
	// 6: jaar
	// 7: maand
	return Integer.class;
    }

    public boolean isCellEditable( int row, int column ) {
	switch ( column ) {
	case 0:
	case 1:
	case 2:
	case 3:
	case 4:
	    // Do not allow editing
	    return false;
	}

	return true;
    }

    public Object getValueAt( int row, int column ) {
	if ( ( row < 0 ) || ( row >= rekeningMutatieRubriekRecordList.size( ) ) ) {
	    logger.severe( "Invalid row: " + row );
	    return null;
	}

	final RekeningMutatieRubriekRecord rekeningMutatieRubriekRecord = rekeningMutatieRubriekRecordList.get( row );

	if ( column == 0 ) return rekeningMutatieRubriekRecord.datumString;
	if ( column == 1 ) return rekeningMutatieRubriekRecord.debCredString;
	if ( column == 2 ) return rekeningMutatieRubriekRecord.rekeningString;
	if ( column == 3 ) return rekeningMutatieRubriekRecord.mutatieInDouble;
	if ( column == 4 ) return rekeningMutatieRubriekRecord.mutatieUitDouble;
	if ( column == 5 ) return rekeningMutatieRubriekRecord.volgNummerInteger;
	if ( column == 6 ) return rekeningMutatieRubriekRecord.jaarInteger;
	if ( column == 7 ) return rekeningMutatieRubriekRecord.maandInteger;
	if ( column == 8 ) return rekeningMutatieRubriekRecord.omschrijvingString;

	return "";
    }

    public void setValueAt( Object object, int row, int column ) {
	if ( ( row < 0 ) || ( row >= rekeningMutatieRubriekRecordList.size( ) ) ) {
	    logger.severe( "Invalid row: " + row );
	    return;
	}

	final RekeningMutatieRubriekRecord rekeningMutatieRubriekRecord = rekeningMutatieRubriekRecordList.get( row );

	int currentVolgNummer = rekeningMutatieRubriekRecord.volgNummerInteger;
	String updateString  = "UPDATE deb_cred SET ";

	try {
	    switch ( column ) {
	    case 5:
		rekeningMutatieRubriekRecord.volgNummerInteger = Integer.parseInt( ( String )object );
		updateString += "volgnummer = " + rekeningMutatieRubriekRecord.volgNummerInteger;
		break;
	    case 6:
		rekeningMutatieRubriekRecord.jaarInteger = Integer.parseInt( ( String )object );
		updateString += "jaar = " + rekeningMutatieRubriekRecord.jaarInteger;
		break;
	    case 7:
		rekeningMutatieRubriekRecord.maandInteger = Integer.parseInt( ( String )object );
		updateString += "maand = " + rekeningMutatieRubriekRecord.maandInteger;
		break;
	    case 8:
		rekeningMutatieRubriekRecord.omschrijvingString = ( String )object;
		if ( ( rekeningMutatieRubriekRecord.omschrijvingString == null ) ||
		     ( rekeningMutatieRubriekRecord.omschrijvingString.length( ) == 0 ) ) {
		    updateString += "omschrijving = NULL ";
		} else {
		    updateString += "omschrijving = " + rekeningMutatieRubriekRecord.omschrijvingString;
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

	rekeningMutatieRubriekRecordList.set( row, rekeningMutatieRubriekRecord );

	updateString += ( " WHERE rubriek_id = " + rubriekId +
			  " AND datum = '" + rekeningMutatieRubriekRecord.datumString + "'" +
			  " AND deb_cred_id = " + rekeningMutatieRubriekRecord.debCredId +
			  " AND volgnummer = " + currentVolgNummer +
			  " AND rekening_id = " + rekeningMutatieRubriekRecord.rekeningId );

	logger.info( "updateString: " + updateString );

	try {
	    Statement statement = connection.createStatement( );
	    int nUpdate = statement.executeUpdate( updateString );
	    if ( nUpdate != 1 ) {
	    	logger.severe( "Could not update record with rubriek_id " + rubriekId +
			       " and datum " + rekeningMutatieRubriekRecord.datumString +
			       " in rekening_mutatie, nUpdate = " + nUpdate );
	    	return;
	    }
	} catch ( SQLException sqlException ) {
            JOptionPane.showMessageDialog( parentFrame,
                    sqlException.getMessage( ),
                    "Rekening mutatie rubriek SQL exception",
                    JOptionPane.ERROR_MESSAGE);
	    logger.severe( "SQLException: " + sqlException.getMessage( ) );
	    return;
	}

	fireTableCellUpdated( row, column );
    }

    public String getColumnName( int column ) {
	return headings[ column ];
    }

    int getRubriekId( ) { return rubriekId; }

    String getDatumString( int row ) {
	if ( ( row < 0 ) || ( row >= rekeningMutatieRubriekRecordList.size( ) ) ) {
	    logger.severe( "Invalid row: " + row );
	    return null;
	}

	return ( rekeningMutatieRubriekRecordList.get( row ) ).datumString;
    }

    int getDebCredId( int row ) {
	if ( ( row < 0 ) || ( row >= rekeningMutatieRubriekRecordList.size( ) ) ) {
	    logger.severe( "Invalid row: " + row );
	    return 0;
	}

	return ( rekeningMutatieRubriekRecordList.get( row ) ).debCredId;
    }

    int getRekeningId( int row ) {
	if ( ( row < 0 ) || ( row >= rekeningMutatieRubriekRecordList.size( ) ) ) {
	    logger.severe( "Invalid row: " + row );
	    return 0;
	}

	return ( rekeningMutatieRubriekRecordList.get( row ) ).rekeningId;
    }

    String getRekeningString( int row ) {
	if ( ( row < 0 ) || ( row >= rekeningMutatieRubriekRecordList.size( ) ) ) {
	    logger.severe( "Invalid row: " + row );
	    return null;
	}

	return ( rekeningMutatieRubriekRecordList.get( row ) ).rekeningString;
    }

    int getRekeningHouderId( int row ) {
        if ( ( row < 0 ) || ( row >= rekeningMutatieRubriekRecordList.size( ) ) ) {
            logger.severe( "Invalid row: " + row );
            return 0;
        }

        return ( rekeningMutatieRubriekRecordList.get( row ) ).rekeningHouderId;
    }

    int getRekeningTypeId( int row ) {
	if ( ( row < 0 ) || ( row >= rekeningMutatieRubriekRecordList.size( ) ) ) {
	    logger.severe( "Invalid row: " + row );
	    return 0;
	}

	return ( rekeningMutatieRubriekRecordList.get( row ) ).rekeningTypeId;
    }

    int getVolgNummer( int row ) {
	if ( ( row < 0 ) || ( row >= rekeningMutatieRubriekRecordList.size( ) ) ) {
	    logger.severe( "Invalid row: " + row );
	    return 0;
	}

	return ( rekeningMutatieRubriekRecordList.get( row ) ).volgNummerInteger;
    }
}
