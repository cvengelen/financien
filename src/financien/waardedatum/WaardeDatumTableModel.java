// Class to setup a TableModel for all records in waarde for a specific date

package financien.waardedatum;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.ResultSet;
import java.sql.Statement;

import javax.swing.table.*;
import java.util.*;
import java.util.logging.*;


class WaardeDatumTableModel extends AbstractTableModel {
    private static final long serialVersionUID = 1L;
    private final Logger logger = Logger.getLogger( "financien.waardedatum.WaardeDatumTableModel" );

    private final Connection connection;
    private final String[] headings = { "Rekening", "Type", "Saldo", "Koers", "Waarde",
            "Inleg", "Waarde-Inleg", "R. direct", "R. som", "Jaren som", "R. som/jaar", "R. 20150101/jaar" };

    // Class to store record in table model
    private class WaardeDatumRecord {
        int rekeningId;
        String rekeningString;
        int currencyId;
        Integer rekeningTypeIdInteger;
        Double saldo;
        Double koers;
        Double waarde;
        Double inleg;
        Double waardeMinusInleg;
        Double rendement;
        Double rendementTotaal;
        Double aantalJaren;
        Double rendementPerJaar;
        Double rendementComparePerJaar;

        WaardeDatumRecord( int rekeningId,
                           String rekeningString,
                           int currencyId,
                           Integer rekeningTypeIdInteger,
                           Double saldo,
                           Double koers,
                           Double waarde,
                           Double inleg,
                           Double waardeMinusInleg,
                           Double rendement,
                           Double rendementTotaal,
                           Double aantalJaren,
                           Double rendementPerJaar,
                           Double rendementComparePerJaar ) {
            this.rekeningId = rekeningId;
            this.rekeningString = rekeningString;
            this.currencyId = currencyId;
            this.rekeningTypeIdInteger = rekeningTypeIdInteger;
            this.saldo = saldo;
            this.koers = koers;
            this.waarde = waarde;
            this.inleg = inleg;
            this.waardeMinusInleg = waardeMinusInleg;
            this.rendement = rendement;
            this.rendementTotaal = rendementTotaal;
            this.aantalJaren = aantalJaren;
            this.rendementPerJaar = rendementPerJaar;
            this.rendementComparePerJaar = rendementComparePerJaar;
        }
    }

    private final ArrayList< WaardeDatumRecord > waardeDatumRecordList = new ArrayList< >( 100 );

    // Constructor
    WaardeDatumTableModel( Connection connection ) {
        this.connection = connection;
    }

    void setupWaardeDatumTableModel( String datumString ) {

        // Setup the table for the specified date
        try {
            String waardeDatumQueryString =
                    "SELECT waarde.rekening_id, rekening.rekening, rekening.currency_id, rekening.type_id, waarde.saldo, waarde.koers, waarde.waarde," +
                            " waarde.inleg, waarde.waarde_minus_inleg, waarde.rendement, " +
                            " waarde.rendement_totaal, waarde.aantal_jaren, waarde.rendement_per_jaar, waarde.rendement_compare_per_jaar" +
                            " FROM waarde" +
                            " LEFT JOIN rekening ON rekening.rekening_id = waarde.rekening_id" +
                            " WHERE waarde.datum = '" + datumString + "'" +
                            " ORDER BY rekening.type_id, rekening.rekening";

            Statement statement = connection.createStatement( );
            ResultSet resultSet = statement.executeQuery( waardeDatumQueryString );

            // Clear the list
            waardeDatumRecordList.clear( );

            // Add all query results to the list
            while ( resultSet.next( ) ) {
                waardeDatumRecordList.add( new WaardeDatumRecord( resultSet.getInt( 1 ),
                        resultSet.getString( 2 ),
                        resultSet.getInt( 3 ),
                        resultSet.getInt( 4 ),
                        resultSet.getDouble( 5 ),
                        resultSet.getDouble( 6 ),
                        resultSet.getDouble( 7 ),
                        resultSet.getDouble( 8 ),
                        resultSet.getDouble( 9 ),
                        resultSet.getDouble( 10 ),
                        resultSet.getDouble( 11 ),
                        resultSet.getDouble( 12 ),
                        resultSet.getDouble( 13 ),
                        resultSet.getDouble( 14 ) ) );
            }

            waardeDatumRecordList.trimToSize( );

            // Trigger update of table data
            fireTableDataChanged( );
        } catch ( SQLException sqlException ) {
            logger.severe( "SQLException: " + sqlException.getMessage( ) );
        }
    }

    public int getRowCount( ) {
        return waardeDatumRecordList.size( );
    }

    public int getColumnCount( ) { return 12; }

    // Indicate the class for each column for setting the correct default renderer
    // see file:///home/cvengelen/java/tutorial/uiswing/components/table.html
    public Class getColumnClass( int column ) {
        switch ( column ) {
            case 0:
                return String.class;    // rekening type
            case 1:
                return Integer.class;    // rekening type id
        }

        return Double.class;        // All other
    }

    public boolean isCellEditable( int row, int column ) {
        // Do not allow editing
        return false;
    }

    public Object getValueAt( int row, int column ) {
        if ( ( row < 0 ) || ( row >= waardeDatumRecordList.size( ) ) ) {
            logger.severe( "Invalid row: " + row );
            return null;
        }

        final WaardeDatumRecord waardeDatumRecord = waardeDatumRecordList.get( row );

        if ( column == 0 ) return waardeDatumRecord.rekeningString;
        if ( column == 1 ) return waardeDatumRecord.rekeningTypeIdInteger;
        if ( column == 2 ) return waardeDatumRecord.saldo;
        if ( column == 3 ) return waardeDatumRecord.koers;
        if ( column == 4 ) return waardeDatumRecord.waarde;
        if ( column == 5 ) return waardeDatumRecord.inleg;
        if ( column == 6 ) return waardeDatumRecord.waardeMinusInleg;
        if ( column == 7 ) return waardeDatumRecord.rendement;
        if ( column == 8 ) return waardeDatumRecord.rendementTotaal;
        if ( column == 9 ) return waardeDatumRecord.aantalJaren;
        if ( column == 10 ) return waardeDatumRecord.rendementPerJaar;
        if ( column == 11 ) return waardeDatumRecord.rendementComparePerJaar;

        return "";
    }

    public void setValueAt( Object object, int row, int column ) {
        if ( ( row < 0 ) || ( row >= waardeDatumRecordList.size( ) ) ) {
            logger.severe( "Invalid row: " + row );
        }
    }

    public String getColumnName( int column ) {
        return headings[ column ];
    }

    int getNumberOfRecords( ) {
        return waardeDatumRecordList.size( );
    }

    int getCurrencyId( int row ) {
        if ( ( row < 0 ) || ( row >= waardeDatumRecordList.size( ) ) ) {
            logger.severe( "Invalid row: " + row );
        }

        return ( waardeDatumRecordList.get( row ) ).currencyId;
    }
}
