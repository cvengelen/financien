package financien.waarderekening;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.ResultSet;
import java.sql.Statement;

import javax.swing.table.*;
import java.util.*;
import java.util.logging.*;

/**
 * TableModel for records in waarde for a specific rekening
 */
class WaardeRekeningTableModel extends AbstractTableModel {
    private final Logger logger = Logger.getLogger( WaardeRekeningTableModel.class.getCanonicalName() );

    private Connection connection;
    private final String[ ] headings = { "Datum", "Saldo", "Koers", "Waarde",
				         "Inleg", "Waarde-Inleg", "Rendement" };

    // Class to store record in table model
    private class WaardeRekeningRecord {
	String  datumString;
	Double  saldo;
	Double  koers;
	Double  waarde;
	Double  inleg;
	Double  waardeMinusInleg;
	Double  rendement;
	WaardeRekeningRecord( String datumString,
                              Double saldo,
                              Double koers,
                              Double waarde,
                              Double inleg,
                              Double waardeMinusInleg,
                              Double rendement ) {
	    this.datumString = datumString;
	    this.saldo = saldo;
	    this.koers = koers;
	    this.waarde = waarde;
	    this.inleg = inleg;
	    this.waardeMinusInleg = waardeMinusInleg;
	    this.rendement = rendement;
	}
    }

    private final ArrayList<WaardeRekeningRecord> waardeRekeningRecordList = new ArrayList<>( 50 );

    // Constructor
    WaardeRekeningTableModel( Connection connection ) {
	this.connection = connection;
    }

    void setupWaardeRekeningTableModel( int rekeningId ) {

	// Setup the table for the specified rekeningId
	try {
	    String waardeRekeningQueryString =
		"SELECT datum, saldo, koers, waarde, inleg, waarde_minus_inleg, rendement " +
		"FROM waarde " +
		"WHERE rekening_id = " + rekeningId + " AND abs( saldo > 0.01 ) " +
		"ORDER BY datum DESC";

	    Statement statement = connection.createStatement( );
	    ResultSet resultSet = statement.executeQuery( waardeRekeningQueryString );

	    // Clear the list
	    waardeRekeningRecordList.clear( );

	    // Add all query results to the list
	    while ( resultSet.next( ) ) {
		waardeRekeningRecordList.add( new WaardeRekeningRecord( resultSet.getString( 1 ),
									resultSet.getDouble( 2 ),
									resultSet.getDouble( 3 ),
									resultSet.getDouble( 4 ),
									resultSet.getDouble( 5 ),
									resultSet.getDouble( 6 ),
									resultSet.getDouble( 7 ) ) );
	    }

	    waardeRekeningRecordList.trimToSize( );
            logger.info("Table shows " + waardeRekeningRecordList.size() + " waarde-rekening records");

	    // Trigger update of table data
	    fireTableDataChanged( );
	} catch ( SQLException sqlException ) {
	    logger.severe( "SQLException: " + sqlException.getMessage( ) );
	}
    }

    public int getRowCount( ) { return waardeRekeningRecordList.size( ); }

    public int getColumnCount( ) { return 7; }

    // Indicate the class for each column for setting the correct default renderer
    // see file:///home/cvengelen/java/tutorial/uiswing/components/table.html
    public Class getColumnClass( int column ) {
	switch ( column ) {
	case 0:
	    return String.class;	// datum
	}

	return Double.class;		// All other
    }

    public boolean isCellEditable( int row, int column ) {
	// Do not allow editing
	return false;
    }

    public Object getValueAt( int row, int column ) {
	if ( ( row < 0 ) || ( row >= waardeRekeningRecordList.size( ) ) ) {
	    logger.severe( "Invalid row: " + row );
	    return null;
	}

	final WaardeRekeningRecord waardeRekeningRecord = waardeRekeningRecordList.get( row );

	if ( column == 0 ) return waardeRekeningRecord.datumString;
	if ( column == 1 ) return waardeRekeningRecord.saldo;
	if ( column == 2 ) return waardeRekeningRecord.koers;
	if ( column == 3 ) return waardeRekeningRecord.waarde;
	if ( column == 4 ) return waardeRekeningRecord.inleg;
	if ( column == 5 ) return waardeRekeningRecord.waardeMinusInleg;
	if ( column == 6 ) return waardeRekeningRecord.rendement;

	return "";
    }

    public void setValueAt( Object object, int row, int column ) {
	if ( ( row < 0 ) || ( row >= waardeRekeningRecordList.size( ) ) ) {
	    logger.severe( "Invalid row: " + row );
	}
    }

    public String getColumnName( int column ) {
	return headings[ column ];
    }
}
