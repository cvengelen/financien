package financien.rubriektotalsmonth;

import javax.swing.table.AbstractTableModel;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.logging.Logger;

/**
 * TableModel for totals of a rubriek per month
 */
class RubriekTotalsMonthTableModel extends AbstractTableModel {
    private static final long serialVersionUID = 1L;
    private final Logger m_logger = Logger.getLogger( "financien.gui.RubriekTotalsMonthTableModel" );

    private Connection m_connection;
    private final String[ ] m_headings = { "Month", "In", "Uit", "Totaal" };

    double m_sumIn, m_sumOut, m_sumTotal;

    private class RubriekTotalRecord {
	String  m_month;
	double  m_in;
	double  m_out;
	double  m_total;

	RubriekTotalRecord( String  month,
                            double  in,
                            double  out,
                            double  total ) {
	    m_month = month;
            m_in = in;
            m_out = out;
            m_total = total;
	}

	// Copy constructor
        RubriekTotalRecord( RubriekTotalRecord rubriekTotalRecord ) {
            m_month = rubriekTotalRecord.m_month;
            m_in = rubriekTotalRecord.m_in;
            m_out = rubriekTotalRecord.m_out;
            m_total = rubriekTotalRecord.m_total;
	}
    }

    private final ArrayList< RubriekTotalRecord > rubriekTotalRecordList = new ArrayList< >( 200 );

    // Constructor
    RubriekTotalsMonthTableModel( Connection connection ) {
	m_connection = connection;
    }

    void setupRubriekTotalsTableModel( int rubriekId,
                                       int rekeningHouderId,
                                       int rekeningId,
                                       int firstYear,
                                       int firstMonth,
                                       int lastYear,
                                       int lastMonth ) {

        // Clear the list
        rubriekTotalRecordList.clear( );
        m_sumIn = 0;
        m_sumOut = 0;

        String rubriekTotalsBaseQuery = "SELECT sum(rekening_mutatie.mutatie_in), sum(rekening_mutatie.mutatie_uit) FROM financien.rekening_mutatie " +
                "INNER JOIN rekening on rekening.rekening_id = rekening_mutatie.rekening_id " +
                "WHERE rekening_mutatie.rubriek_id = " + rubriekId + " AND rekening.rekening_houder_id = " + rekeningHouderId;

        // Check for an additional selection on rekening ID
        if (rekeningId != 0) rubriekTotalsBaseQuery += " AND rekening_id = " + rekeningId;

        // Setup the table
        try {
            // Loop over the years
            for (int year = firstYear; year <= lastYear; year++ ) {
                // Loop over the months
                int startMonth = (year == firstYear ? firstMonth : 1);
                int endMonth = (year == lastYear ? lastMonth : 12);
                for (int month = startMonth; month <= endMonth; month++) {
                    int beforeYear = year;
                    int beforeMonth = month + 1;
                    // Check for december
                    if (month == 12) {
                        beforeYear++;
                        beforeMonth = 1;
                    }
                    String rubriekTotalsQuery = rubriekTotalsBaseQuery + " AND rekening_mutatie.datum >= date('" + year + "-" + month + "-1') " +
                            "AND rekening_mutatie.datum < date('" + beforeYear + "-" + beforeMonth + "-1')";
                    Statement statement = m_connection.createStatement( );
                    ResultSet resultSet = statement.executeQuery( rubriekTotalsQuery );

                    // Add the query result to the list
                    while ( resultSet.next( ) ) {
                        String date = Integer.toString( year ) + "-" + Integer.toString(month);
                        double in = resultSet.getDouble(1);
                        double out = resultSet.getDouble(2);
                        double total = in - out;

                        rubriekTotalRecordList.add( new RubriekTotalRecord(date, in, out, total));
                        m_sumIn += in;
                        m_sumOut += out;
                    }
                }
	    }

            m_sumTotal = m_sumIn - m_sumOut;

            rubriekTotalRecordList.trimToSize( );
            m_logger.info("Table shows " + rubriekTotalRecordList.size() + " rubriek totals per month records");

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
        // First column: date, is a string
	if ( column == 0 ) return String.class;

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

	if ( column ==  0 ) return rubriekTotalRecord.m_month;
	if ( column ==  1 ) return rubriekTotalRecord.m_in;
	if ( column ==  2 ) return rubriekTotalRecord.m_out;
	if ( column ==  3 ) return rubriekTotalRecord.m_total;

	return "";
    }

    public String getColumnName( int column ) {
	return m_headings[ column ];
    }

    public int getNumberOfRecords( ) { return rubriekTotalRecordList.size( ); }

    public String getMonth( int row ) {
	if ( ( row < 0 ) || ( row >= rubriekTotalRecordList.size( ) ) ) {
	    m_logger.severe( "Invalid row: " + row );
	    return null;
	}

	return rubriekTotalRecordList.get( row ).m_month;
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
