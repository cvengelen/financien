// Class to setup a TableModel for all records in waarde for a specific rekening

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


public class WaardeRekeningTableModel extends AbstractTableModel {
    final Logger logger = Logger.getLogger( "financien.gui.WaardeRekeningTableModel" );

    private Connection connection;
    private String[ ] headings = { "Datum", "Saldo", "Koers", "Waarde",
				   "Inleg", "Waarde-Inleg", "Rendement" };

    // Class to store record in table model
    class WaardeRekeningRecord {
	String  datumString;
	Double  saldo;
	Double  koers;
	Double  waarde;
	Double  inleg;
	Double  waardeMinusInleg;
	Double  rendement;
	public WaardeRekeningRecord( String datumString,
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

    ArrayList waardeRekeningRecordList = new ArrayList( 200 );

    private int rekeningId = 0;


    // Constructor
    public WaardeRekeningTableModel( Connection connection ) {
	this.connection = connection;
    }

    public void setupWaardeRekeningTableModel( int rekeningId ) {
	this.rekeningId = rekeningId;

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
									new Double( resultSet.getDouble( 2 ) ),
									new Double( resultSet.getDouble( 3 ) ),
									new Double( resultSet.getDouble( 4 ) ),
									new Double( resultSet.getDouble( 5 ) ),
									new Double( resultSet.getDouble( 6 ) ),
									new Double( resultSet.getDouble( 7 ) ) ) );
	    }

	    waardeRekeningRecordList.trimToSize( );

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

	final WaardeRekeningRecord waardeRekeningRecord =
	    ( WaardeRekeningRecord )waardeRekeningRecordList.get( row );

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

	return;
    }

    public String getColumnName( int column ) {
	return headings[ column ];
    }

    public int getNumberOfRecords( ) { return waardeRekeningRecordList.size( ); }
}
