package financien.rubriektotalsyear;

import javax.swing.table.AbstractTableModel;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Vector;
import java.util.logging.Logger;

/**
 * TableModel for totals of a rubriek per year
 */
class RubriekTotalsYearTableModel extends AbstractTableModel {
    private static final long serialVersionUID = 1L;
    private final Logger m_logger = Logger.getLogger( "financien.gui.RubriekTotalsYearTableModel" );

    private Connection m_connection;
    private final String[ ] m_headings = { "Year", "In", "Uit", "Totaal" };

    double m_sumIn, m_sumOut, m_sumTotal;

    private class RubriekTotalRecord {
	int     m_year;
	double  m_in;
	double  m_out;
	double  m_total;

	RubriekTotalRecord( int     year,
                            double  in,
                            double  out,
                            double  total ) {
	    m_year = year;
            m_in = in;
            m_out = out;
            m_total = total;
	}

	// Copy constructor
        RubriekTotalRecord( RubriekTotalRecord rubriekTotalRecord ) {
            m_year = rubriekTotalRecord.m_year;
            m_in = rubriekTotalRecord.m_in;
            m_out = rubriekTotalRecord.m_out;
            m_total = rubriekTotalRecord.m_total;
	}
    }

    private final ArrayList< RubriekTotalRecord > rubriekTotalRecordList = new ArrayList< >( 200 );

    // Constructor
    RubriekTotalsYearTableModel( Connection connection ) {
	m_connection = connection;
    }

    void setupRubriekTotalsTableModel( Vector<Integer> selectedRubriekIds,
                                       int rekeningHouderId,
                                       int rekeningId,
                                       int firstYear,
                                       int lastYear ) {

        // Clear the list
        rubriekTotalRecordList.clear( );
        m_sumIn = 0;
        m_sumOut = 0;

        String rubriekTotalsBaseQuery = "SELECT sum(rekening_mutatie.mutatie_in), sum(rekening_mutatie.mutatie_uit) FROM financien.rekening_mutatie " +
                "INNER JOIN rekening on rekening.rekening_id = rekening_mutatie.rekening_id " +
                "WHERE rekening.rekening_houder_id = " + rekeningHouderId;

        if (!selectedRubriekIds.isEmpty()) {
            String rubriekIdSelection = "";
            for ( int selectedRubriekId : selectedRubriekIds ) {
                if ( !rubriekIdSelection.isEmpty() ) rubriekIdSelection += " OR ";
                rubriekIdSelection += "rekening_mutatie.rubriek_id = " + selectedRubriekId;
            }
            rubriekTotalsBaseQuery += " AND (" + rubriekIdSelection + ")";
        }

        // Check for an additional selection on rekening ID
        if (rekeningId != 0) rubriekTotalsBaseQuery += " AND rekening_id = " + rekeningId;

        // Setup the table
        try {
            // Loop over the years
            for (int year = firstYear; year <= lastYear; year++ ) {
                String rubriekTotalsQuery = rubriekTotalsBaseQuery + " AND rekening_mutatie.datum >= date('" + year + "-1-1') " +
                        "AND rekening_mutatie.datum < date('" + (year + 1) + "-1-1')";
                Statement statement = m_connection.createStatement( );
                ResultSet resultSet = statement.executeQuery( rubriekTotalsQuery );

                // Add the query result to the list
                while ( resultSet.next( ) ) {
                    double in = resultSet.getDouble(1);
                    double out = resultSet.getDouble(2);
                    double total = in - out;

                    rubriekTotalRecordList.add( new RubriekTotalRecord(year, in, out, total));
                    m_sumIn += in;
                    m_sumOut += out;
                }
	    }

            m_sumTotal = m_sumIn - m_sumOut;

            rubriekTotalRecordList.trimToSize( );
            m_logger.info("Table shows " + rubriekTotalRecordList.size() + " rubriek totals per year records");

	    // Trigger update of table data
	    fireTableDataChanged( );
	} catch ( SQLException sqlException ) {
	    m_logger.severe( "SQLException: " + sqlException.getMessage( ) );
	}
    }

    public int getRowCount( ) { return rubriekTotalRecordList.size( ); }

    public int getColumnCount( ) { return 4; }

    // Indicate the class for each column for setting the correct default renderer
    // see file:///home/cvengelen/java/tutorial/uiswing/components/table.html
    public Class getColumnClass( int column ) {
        // First column: year
	if ( column == 0 ) return Integer.class;

        // Rest is double
	return Double.class;
    }

    public boolean isCellEditable( int row, int column ) {
	return false;
    }

    public Object getValueAt( int row, int column ) {
	if ( ( row < 0 ) || ( row >= rubriekTotalRecordList.size( ) ) ) {
	    m_logger.severe( "Invalid row: " + row );
	    return null;
	}

	final RubriekTotalRecord rubriekTotalRecord = rubriekTotalRecordList.get( row );

	if ( column ==  0 ) return rubriekTotalRecord.m_year;
	if ( column ==  1 ) return rubriekTotalRecord.m_in;
	if ( column ==  2 ) return rubriekTotalRecord.m_out;
	if ( column ==  3 ) return rubriekTotalRecord.m_total;

	return "";
    }

    public String getColumnName( int column ) {
	return m_headings[ column ];
    }

    public int getNumberOfRecords( ) { return rubriekTotalRecordList.size( ); }

    public int getYear( int row ) {
	if ( ( row < 0 ) || ( row >= rubriekTotalRecordList.size( ) ) ) {
	    m_logger.severe( "Invalid row: " + row );
	    return 0;
	}

	return rubriekTotalRecordList.get( row ).m_year;
    }

    double getSumIn() { return m_sumIn; }
    double getSumOut() { return m_sumOut; }
    double getSumTotal() { return m_sumTotal; }

    private void fireTableRowUpdated( int row ) {
	for ( int column = 0; column < getColumnCount( ); column++ ) {
	    fireTableCellUpdated( row, column );
	}
    }
}
