// Class to setup a TableModel for all records in rekening_mutatie for a specific rekening

package financien.gui;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.ResultSet;
import java.sql.Statement;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.table.*;
import java.text.*;
import java.util.*;
import java.util.logging.*;


public class RekeningMutatieRekeningTableModel extends AbstractTableModel {
    final Logger logger = Logger.getLogger( "financien.gui.RekeningMutatieRekeningTableModel" );

    private Connection connection;
    private String[ ] headings = { "Datum", "Deb/Cred",
				   "Rubriek", "In", "Uit", "Nr",
				   "Jaar", "Maand", "Omschrijving",
				   "Inleg Aandelen" };

    // Jaar, maand and volgNummer are stored as Integer for correct default table renderer.
    class RekeningMutatieRekeningRecord {
	String  datumString;
	int     debCredId;
	String  debCredString;
	int     rubriekId;
	String  rubriekString;
	Double  mutatieInDouble;
	Double  mutatieUitDouble;
	Integer volgNummerInteger;
	Integer jaarInteger;
	Integer maandInteger;
	String  omschrijvingString;
	Double  inlegAandelenDouble;
	public RekeningMutatieRekeningRecord( String  datumString,
					      int     debCredId,
					      String  debCredString,
					      int     rubriekId,
					      String  rubriekString,
					      Double  mutatieInDouble,
					      Double  mutatieUitDouble,
					      Integer volgNummerInteger,
					      Integer jaarInteger,
					      Integer maandInteger,
					      String  omschrijvingString,
					      Double  inlegAandelenDouble ) {
	    this.datumString = datumString;
	    this.debCredId = debCredId;
	    this.debCredString = debCredString;
	    this.rubriekId = rubriekId;
	    this.rubriekString = rubriekString;
	    this.mutatieInDouble = mutatieInDouble;
	    this.mutatieUitDouble = mutatieUitDouble;
	    this.volgNummerInteger = volgNummerInteger;
	    this.jaarInteger = jaarInteger;
	    this.maandInteger = maandInteger;
	    this.omschrijvingString = omschrijvingString;
	    this.inlegAandelenDouble = inlegAandelenDouble;
	}
    }

    ArrayList rekeningMutatieRekeningRecordList = new ArrayList( 200 );

    private int rekeningId = 0;
    private int rekeningTypeId = 0;


    // Constructor
    public RekeningMutatieRekeningTableModel( Connection connection ) {
	this.connection = connection;
    }

    public void setupRekeningMutatieRekeningTableModel( int rekeningId,
							int rekeningTypeId ) {
	this.rekeningId = rekeningId;
	this.rekeningTypeId = rekeningTypeId;

	// Setup the table for the specified rekening
	try {
	    String rekeningMutatieQueryString =
		"SELECT rekening_mutatie.datum, " +
		"rekening_mutatie.deb_cred_id, deb_cred.deb_cred, " +
		"rekening_mutatie.rubriek_id, rubriek.rubriek, " +
		"mutatie_in, mutatie_uit, volgnummer, jaar, maand, " +
		"rekening_mutatie.omschrijving, rekening_mutatie.inleg_aandelen " +
		"FROM rekening_mutatie " +
		"LEFT JOIN deb_cred ON deb_cred.deb_cred_id = rekening_mutatie.deb_cred_id " +
		"LEFT JOIN rubriek ON rubriek.rubriek_id = rekening_mutatie.rubriek_id " +
		"WHERE rekening_mutatie.rekening_id = " + rekeningId + " " +
		"ORDER BY rekening_mutatie.datum DESC, rekening_mutatie.volgnummer";

	    Statement statement = connection.createStatement( );
	    ResultSet resultSet = statement.executeQuery( rekeningMutatieQueryString );

	    // Clear the list
	    rekeningMutatieRekeningRecordList.clear( );

	    // Add all query results to the list
	    while ( resultSet.next( ) ) {
		rekeningMutatieRekeningRecordList.add( new RekeningMutatieRekeningRecord( resultSet.getString( 1 ),
											  resultSet.getInt( 2 ),
											  resultSet.getString( 3 ),
											  resultSet.getInt( 4 ),
											  resultSet.getString( 5 ),
											  new Double( resultSet.getDouble( 6 ) ),
											  new Double( resultSet.getDouble( 7 ) ),
											  new Integer( resultSet.getInt( 8 ) ),
											  new Integer( resultSet.getInt( 9 ) ),
											  new Integer( resultSet.getInt( 10 ) ),
											  resultSet.getString( 11 ),
											  new Double( resultSet.getDouble( 12 ) ) ) );
	    }

	    rekeningMutatieRekeningRecordList.trimToSize( );

	    // Trigger update of table data
	    fireTableDataChanged( );
	} catch ( SQLException sqlException ) {
	    logger.severe( "SQLException: " + sqlException.getMessage( ) );
	}
    }

    public int getRowCount( ) { return rekeningMutatieRekeningRecordList.size( ); }

    public int getColumnCount( ) {
	switch ( rekeningTypeId ) {
	case 4:		// AEX aandelen
	case 5:		// Lucent
	case 7:		// niet AEX aandelen
	    return 10;
	}

	// All else
	return 9;
    }

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
	case 9: // inlegAandelen
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
	if ( ( row < 0 ) || ( row >= rekeningMutatieRekeningRecordList.size( ) ) ) {
	    logger.severe( "Invalid row: " + row );
	    return null;
	}

	final RekeningMutatieRekeningRecord rekeningMutatieRekeningRecord =
	    ( RekeningMutatieRekeningRecord )rekeningMutatieRekeningRecordList.get( row );

	if ( column == 0 ) return rekeningMutatieRekeningRecord.datumString;
	if ( column == 1 ) return rekeningMutatieRekeningRecord.debCredString;
	if ( column == 2 ) return rekeningMutatieRekeningRecord.rubriekString;
	if ( column == 3 ) return rekeningMutatieRekeningRecord.mutatieInDouble;
	if ( column == 4 ) return rekeningMutatieRekeningRecord.mutatieUitDouble;
	if ( column == 5 ) return rekeningMutatieRekeningRecord.volgNummerInteger;
	if ( column == 6 ) return rekeningMutatieRekeningRecord.jaarInteger;
	if ( column == 7 ) return rekeningMutatieRekeningRecord.maandInteger;
	if ( column == 8 ) return rekeningMutatieRekeningRecord.omschrijvingString;
	if ( column == 9 ) return rekeningMutatieRekeningRecord.inlegAandelenDouble;

	return "";
    }

    public void setValueAt( Object object, int row, int column ) {
	if ( ( row < 0 ) || ( row >= rekeningMutatieRekeningRecordList.size( ) ) ) {
	    logger.severe( "Invalid row: " + row );
	    return;
	}

	final RekeningMutatieRekeningRecord rekeningMutatieRekeningRecord =
	    ( RekeningMutatieRekeningRecord )rekeningMutatieRekeningRecordList.get( row );

	int currentVolgNummer = rekeningMutatieRekeningRecord.volgNummerInteger.intValue( );
	String updateString  = "UPDATE deb_cred SET ";

	try {
	    switch ( column ) {
	    case 5:
		rekeningMutatieRekeningRecord.volgNummerInteger.parseInt( ( String )object );
		updateString += "volgnummer = " + rekeningMutatieRekeningRecord.volgNummerInteger;
		break;
	    case 6:
		rekeningMutatieRekeningRecord.jaarInteger.parseInt( ( String )object );
		updateString += "jaar = " + rekeningMutatieRekeningRecord.jaarInteger;
		break;
	    case 7:
		rekeningMutatieRekeningRecord.maandInteger.parseInt( ( String )object );
		updateString += "maand = " + rekeningMutatieRekeningRecord.maandInteger;
		break;
	    case 8:
		rekeningMutatieRekeningRecord.omschrijvingString = ( String )object;
		if ( ( rekeningMutatieRekeningRecord.omschrijvingString == null ) ||
		     ( rekeningMutatieRekeningRecord.omschrijvingString.length( ) == 0 ) ) {
		    updateString += "omschrijving = NULL ";
		} else {
		    updateString += "omschrijving = " + rekeningMutatieRekeningRecord.omschrijvingString;
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

	rekeningMutatieRekeningRecordList.set( row, rekeningMutatieRekeningRecord );

	updateString += ( " WHERE rekening_id = " + rekeningId +
			  " AND datum = '" + rekeningMutatieRekeningRecord.datumString + "'" +
			  " AND deb_cred_id = " + rekeningMutatieRekeningRecord.debCredId + 
			  " AND volgnummer = " + currentVolgNummer +
			  " AND rubriek_id = " + rekeningMutatieRekeningRecord.rubriekId );

	logger.info( "updateString: " + updateString );

	try {
	    Statement statement = connection.createStatement( );
	    int nUpdate = statement.executeUpdate( updateString );
	    if ( nUpdate != 1 ) {
	    	logger.severe( "Could not update record with rekening_id " + rekeningId +
			       " and datum " + rekeningMutatieRekeningRecord.datumString +
			       " in rekening_mutatie, nUpdate = " + nUpdate );
	    	return;
	    }
	} catch ( SQLException sqlException ) {
	    logger.severe( "SQLException: " + sqlException.getMessage( ) );
	    return;
	}

	fireTableCellUpdated( row, column );
    }


    public String getColumnName( int column ) {
	return headings[ column ];
    }

    public int getNumberOfRecords( ) { return rekeningMutatieRekeningRecordList.size( ); }


    public int getRekeningId( ) { return rekeningId; }


    public String getDatumString( int row ) {
	if ( ( row < 0 ) || ( row >= rekeningMutatieRekeningRecordList.size( ) ) ) {
	    logger.severe( "Invalid row: " + row );
	    return null;
	}

	return ( ( RekeningMutatieRekeningRecord )rekeningMutatieRekeningRecordList.get( row ) ).datumString;
    }


    public int getDebCredId( int row ) {
	if ( ( row < 0 ) || ( row >= rekeningMutatieRekeningRecordList.size( ) ) ) {
	    logger.severe( "Invalid row: " + row );
	    return 0;
	}

	return ( ( RekeningMutatieRekeningRecord )rekeningMutatieRekeningRecordList.get( row ) ).debCredId;
    }


    public int getRubriekId( int row ) {
	if ( ( row < 0 ) || ( row >= rekeningMutatieRekeningRecordList.size( ) ) ) {
	    logger.severe( "Invalid row: " + row );
	    return 0;
	}

	return ( ( RekeningMutatieRekeningRecord )rekeningMutatieRekeningRecordList.get( row ) ).rubriekId;
    }


    public String getRubriekString( int row ) {
	if ( ( row < 0 ) || ( row >= rekeningMutatieRekeningRecordList.size( ) ) ) {
	    logger.severe( "Invalid row: " + row );
	    return null;
	}

	return ( ( RekeningMutatieRekeningRecord )rekeningMutatieRekeningRecordList.get( row ) ).rubriekString;
    }


    public int getVolgNummer( int row ) {
	if ( ( row < 0 ) || ( row >= rekeningMutatieRekeningRecordList.size( ) ) ) {
	    logger.severe( "Invalid row: " + row );
	    return 0;
	}

	return ( ( RekeningMutatieRekeningRecord )rekeningMutatieRekeningRecordList.get( row ) ).volgNummerInteger.intValue( );
    }
}
