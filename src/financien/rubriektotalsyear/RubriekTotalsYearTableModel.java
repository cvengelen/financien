package financien.rubriektotalsyear;

import javax.swing.*;
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

    private final Connection m_connection;
    private final JFrame m_parentFrame;
    private final Vector<String> m_headings = new Vector<>();

    // Class to hold total in-out for a rubriek in a year, and indicatie of mutatie present
    static class RubriekTotal {
        Double  m_total;
        boolean m_mutatie;

        RubriekTotal( Double total, boolean mutatie ) {
            m_total = total;
            m_mutatie = mutatie;
        }
    }

    // Class to hold the list of rubriek totals for a year
    private static class RubriekTotalsRecord {
	    int                  m_year;
        Vector<RubriekTotal> m_rubriekTotals;

	    RubriekTotalsRecord( int year, Vector<RubriekTotal> rubriekTotals ) {
	        m_year = year;
            m_rubriekTotals = rubriekTotals;
	    }
    }

    // The contents of the table: for each year the list of rubriek totals
    private final ArrayList< RubriekTotalsRecord > m_rubriekTotalsRecordList = new ArrayList< >( 200 );

    // Constructor
    RubriekTotalsYearTableModel( Connection connection, JFrame parentFrame ) {
	    m_connection = connection;
        m_parentFrame = parentFrame;
    }

    void setupRubriekTotalsTableModel( Vector<Integer> selectedRubriekIds,
                                       Vector<String> selectedRubrieken,
                                       int rekeningHouderId,
                                       int rekeningId,
                                       int firstYear,
                                       int lastYear ) {

        // Clear the list of rubriek totals
        m_rubriekTotalsRecordList.clear( );

        // Initialize the headings
        m_headings.clear();
        m_headings.add( "Jaar" );
        m_headings.addAll( selectedRubrieken );
        m_headings.add("Totaal" );

        // Set up the base part of the query:
        // - sum over all mutatie_in, and over all mutatie_uit
        // - select the rekening houder
        // - select only betaalrekeningen, spaarrekeningen, credit card, beleggingsrekeningen
        String rubriekTotalsBaseQuery = "SELECT sum(rekening_mutatie.mutatie_in), sum(rekening_mutatie.mutatie_uit) FROM financien.rekening_mutatie " +
                "INNER JOIN rekening on rekening.rekening_id = rekening_mutatie.rekening_id " +
                "WHERE rekening.rekening_houder_id = " + rekeningHouderId + " AND rekening.type_id IN (1, 2, 3, 6, 8, 9)";

        // Check for an additional selection on rekening ID
        if (rekeningId != 0) rubriekTotalsBaseQuery += " AND rekening_id = " + rekeningId;

        // Set up the table
        try {
            // Loop over the years
            for (int year = firstYear; year <= lastYear; year++ ) {
                String rubriekTotalsYearQuery = rubriekTotalsBaseQuery + " AND rekening_mutatie.datum >= date('" + year + "-1-1') " +
                        "AND rekening_mutatie.datum < date('" + (year + 1) + "-1-1')";

                Vector<RubriekTotal> rubriekTotals = new Vector<>();
                double  sumTotal = 0d;
                boolean sumMutatie = false;

                // Loop over the rubrieken
                for ( int selectedRubriekId : selectedRubriekIds ) {
                    String rubriekTotalsQuery = rubriekTotalsYearQuery + " AND rekening_mutatie.rubriek_id = " + selectedRubriekId;

                    // Execute the query
                    Statement statement = m_connection.createStatement( );
                    ResultSet resultSet = statement.executeQuery( rubriekTotalsQuery );

                    // Add the query result to the list
                    while ( resultSet.next( ) ) {
                        double in = resultSet.getDouble( 1 );
                        double out = resultSet.getDouble( 2 );
                        double total = in - out;
                        boolean mutatie = in != 0.0d || out != 0.0d;
                        RubriekTotal rubriekTotal = new RubriekTotal( total, mutatie );
                        rubriekTotals.add( rubriekTotal );
                        sumTotal += total;
                        // if any of the rubrieken has a mutation, then the resulting sum has a mutation
                        sumMutatie = sumMutatie || mutatie;
                    }
                }

                // Add the sum over the totals
                rubriekTotals.add( new RubriekTotal( sumTotal, sumMutatie ) );

                // Add this year to the list of rubriek total records
                m_rubriekTotalsRecordList.add( new RubriekTotalsRecord( year, rubriekTotals ) );
	        }

            m_rubriekTotalsRecordList.trimToSize( );
            m_logger.fine("Table shows " + m_rubriekTotalsRecordList.size() + " rubriek totals per year records");

	        // Trigger update of table data
	        fireTableDataChanged( );
	    } catch ( SQLException sqlException ) {
            JOptionPane.showMessageDialog( m_parentFrame,
                    sqlException.getMessage( ),
                    "Rubriek totals per year SQL exception",
                    JOptionPane.ERROR_MESSAGE);
	        m_logger.severe( "SQLException: " + sqlException.getMessage( ) );
	    }
    }

    public int getRowCount( ) { return m_rubriekTotalsRecordList.size( ); }

    public int getColumnCount( ) { return m_headings.size(); }

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
	    if ( ( row < 0 ) || ( row >= m_rubriekTotalsRecordList.size( ) ) ) {
	        m_logger.severe( "Invalid row: " + row );
	        return null;
	    }

	    final RubriekTotalsRecord rubriekTotalsRecord = m_rubriekTotalsRecordList.get( row );

        if ( column ==  0 ) return rubriekTotalsRecord.m_year;

        return rubriekTotalsRecord.m_rubriekTotals.get( column - 1 );
    }

    public String getColumnName( int column ) {
	return m_headings.get(column);
    }

    public int getNumberOfRecords( ) { return m_rubriekTotalsRecordList.size( ); }

    public int getYear( int row ) {
	    if ( ( row < 0 ) || ( row >= m_rubriekTotalsRecordList.size( ) ) ) {
	        m_logger.severe( "Invalid row: " + row );
	        return 0;
	    }

	    return m_rubriekTotalsRecordList.get( row ).m_year;
    }

    private void fireTableRowUpdated( int row ) {
	    for ( int column = 0; column < getColumnCount( ); column++ ) {
	        fireTableCellUpdated( row, column );
	    }
    }
}
