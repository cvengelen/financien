package financien.rekening;

import financien.gui.CurrencyComboBox;
import financien.gui.RekeningTypeComboBox;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.ResultSet;
import java.sql.Statement;

import javax.swing.*;
import javax.swing.table.*;
import java.text.*;
import java.util.*;
import java.util.logging.*;

/**
 * TableModel for records in deb_cred
 */
class RekeningTableModel extends AbstractTableModel {
    private final Logger logger = Logger.getLogger( RekeningTableModel.class.getCanonicalName() );

    private Connection connection;
    private final String[ ] headings = { "Id", "Rekening", "Nummer", "Type", "Fonds", "Currency",
                                         "Aktief", "Start Datum", "Start saldo",
                                         "Laatste update", "Saldo", "Waarde", "Koers" };

    private class RekeningRecord {
        int	rekeningId;
        String	rekeningString;
        String	rekeningNummerString;
        int	rekeningTypeId;
        String	rekeningTypeString;
        String	fondsString;
        int	currencyId;
        String	currencyString;
        int	aktief;
        String	startDatumString;
        double	startSaldo;
        String	datumString;
        double	saldo;
        double	waarde;
        double	koers;

        RekeningRecord( int	rekeningId,
                        String	rekeningString,
                        String	rekeningNummerString,
                        int	rekeningTypeId,
                        String	rekeningTypeString,
                        String	fondsString,
                        int	currencyId,
                        String	currencyString,
                        int	aktief,
                        String	startDatumString,
                        double	startSaldo,
                        String	datumString,
                        double	saldo,
                        double	waarde,
                        double	koers ) {
            this.rekeningId = rekeningId;
            this.rekeningString = rekeningString;
            this.rekeningNummerString = rekeningNummerString;
            this.rekeningTypeId = rekeningTypeId;
            this.rekeningTypeString = rekeningTypeString;
            this.fondsString = fondsString;
            this.currencyId = currencyId;
            this.currencyString = currencyString;
            this.aktief = aktief;
            this.startDatumString = startDatumString;
            this.startSaldo = startSaldo;
            this.datumString = datumString;
            this.saldo = saldo;
            this.waarde = waarde;
            this.koers = koers;
        }

        // Copy constructor
        RekeningRecord( RekeningRecord rekeningRecord ) {
            this.rekeningId = rekeningRecord.rekeningId;
            this.rekeningString = rekeningRecord.rekeningString;
            this.rekeningNummerString = rekeningRecord.rekeningNummerString;
            this.rekeningTypeId = rekeningRecord.rekeningTypeId;
            this.rekeningTypeString = rekeningRecord.rekeningTypeString;
            this.fondsString = rekeningRecord.fondsString;
            this.currencyId = rekeningRecord.currencyId;
            this.currencyString = rekeningRecord.currencyString;
            this.aktief = rekeningRecord.aktief;
            this.startDatumString = rekeningRecord.startDatumString;
            this.startSaldo = rekeningRecord.startSaldo;
            this.datumString = rekeningRecord.datumString;
            this.saldo = rekeningRecord.saldo;
            this.waarde = rekeningRecord.waarde;
            this.koers = rekeningRecord.koers;
        }
    }

    private final ArrayList< RekeningRecord > rekeningRecordList = new ArrayList< >( 40 );

    // Create rekeningType combo box to get rekening type ID from rekeningType string
    private RekeningTypeComboBox rekeningTypeComboBox;

    // Create currency combo box to get currency ID from Currency string
    private CurrencyComboBox currencyComboBox;

    private final SimpleDateFormat dateFormat = new SimpleDateFormat( "yyyy-MM-dd" );

    private JButton cancelRekeningButton;
    private JButton saveRekeningButton;

    private boolean	   rowModified = false;
    private int		   editRow = -1;
    private RekeningRecord rekeningRecord;
    private RekeningRecord originalRekeningRecord;


    // Constructor
    RekeningTableModel( final Connection connection,
                        JButton          cancelRekeningButton,
                        JButton          saveRekeningButton ) {
        this.connection = connection;
        this.cancelRekeningButton = cancelRekeningButton;
        this.saveRekeningButton = saveRekeningButton;

        // Create the combo boxes
        rekeningTypeComboBox = new RekeningTypeComboBox( connection, 0 );
        currencyComboBox = new CurrencyComboBox( connection, 0 );

        dateFormat.setLenient( false );

        setupRekeningTableModel( null, null, 0, 0, 1, true );
    }

    void setupRekeningTableModel( String rekeningString,
                                  String rekeningNummerString,
                                  int rekeningTypeId,
                                  int currencyId,
                                  int selectedRekeningHouderId,
                                  boolean onlyActiveAccounts ) {
        // Setup the table
        try {
            String rekeningQueryString =
                "SELECT rekening.rekening_id, rekening.rekening, rekening.nummer, " +
                "rekening.type_id, rekening_type.rekening_type, " +
                "rekening.fonds, " +
                "rekening.currency_id, currency.currency, " +
                "aktief, start_datum, start_saldo, datum, saldo, waarde, koers " +
                "FROM rekening " +
                "LEFT JOIN rekening_type ON rekening.type_id = rekening_type.rekening_type_id " +
                "LEFT JOIN currency ON rekening.currency_id = currency.currency_id ";

            if ( ( ( rekeningString != null ) && ( rekeningString.length( ) > 0 ) ) ||
                 ( ( rekeningNummerString != null ) && ( rekeningNummerString.length( ) > 0 ) ) ||
                 ( rekeningTypeId != 0 ) || (currencyId != 0 ) || onlyActiveAccounts ) {
                rekeningQueryString += "WHERE ";

                if ( ( rekeningString != null ) && ( rekeningString.length( ) > 0 ) ) {
                    rekeningQueryString +=
                        "rekening.rekening LIKE '%" + rekeningString + "%' ";

                    if ( ( ( rekeningNummerString != null ) && ( rekeningNummerString.length( ) > 0 ) ) ||
                         ( rekeningTypeId != 0 ) || (currencyId != 0 ) || onlyActiveAccounts ) {
                        rekeningQueryString += "AND ";
                    }
                }

                if ( ( rekeningNummerString != null ) && ( rekeningNummerString.length( ) > 0 ) ) {
                    rekeningQueryString +=
                        "rekening.nummer LIKE '%" + rekeningNummerString + "%' ";

                    if ( ( rekeningTypeId != 0 ) || (currencyId != 0 ) || onlyActiveAccounts ) {
                        rekeningQueryString += "AND ";
                    }
                }

                if ( rekeningTypeId > 0 ) {
                    rekeningQueryString += "rekening.type_id = " + rekeningTypeId + " ";

                    if ( ( currencyId != 0 ) || onlyActiveAccounts ) {
                        rekeningQueryString += "AND ";
                    }
                }

                if ( currencyId > 0 ) {
                    rekeningQueryString += "rekening.currency_id = " + currencyId + " ";

                    if ( onlyActiveAccounts ) {
                        rekeningQueryString += "AND ";
                    }
                }

                if ( selectedRekeningHouderId > 0 ) {
                    rekeningQueryString += "rekening.rekening_houder_id = " + selectedRekeningHouderId + " ";

                    if ( onlyActiveAccounts ) {
                        rekeningQueryString += "AND ";
                    }
                }

                if ( onlyActiveAccounts ) {
                    rekeningQueryString += "rekening.aktief != 0 ";
                }
            }

            rekeningQueryString += "ORDER BY rekening.rekening";


            Statement statement = connection.createStatement( );
            ResultSet resultSet = statement.executeQuery( rekeningQueryString );

            // Clear the list
            rekeningRecordList.clear( );

            // Add all query results to the list
            while ( resultSet.next( ) ) {
                rekeningRecordList.add( new RekeningRecord( resultSet.getInt( 1 ),
                                                            resultSet.getString( 2 ),
                                                            resultSet.getString( 3 ),
                                                            resultSet.getInt( 4 ),
                                                            resultSet.getString( 5 ),
                                                            resultSet.getString( 6 ),
                                                            resultSet.getInt( 7 ),
                                                            resultSet.getString( 8 ),
                                                            resultSet.getInt( 9 ),
                                                            resultSet.getString( 10 ),
                                                            resultSet.getDouble( 11 ),
                                                            resultSet.getString( 12 ),
                                                            resultSet.getDouble( 13 ),
                                                            resultSet.getDouble( 14 ),
                                                            resultSet.getDouble( 15 ) ) );
            }

            rekeningRecordList.trimToSize( );
            logger.info("Table shows " + rekeningRecordList.size() + " rekening records");

            // Trigger update of table data
            fireTableDataChanged( );
        } catch ( SQLException sqlException ) {
            logger.severe( "SQLException: " + sqlException.getMessage( ) );
        }
    }

    @Override
    public int getRowCount( ) { return rekeningRecordList.size( ); }

    @Override
    public int getColumnCount( ) { return 13; }

    // Indicate the class for each column for setting the correct default renderer
    // see file:///home/cvengelen/java/tutorial/uiswing/components/table.html
    @Override
    public Class getColumnClass( int column ) {
        switch ( column ) {
        case 0:
        case 6:
            return Integer.class;
        case 8:
        case 10:
        case 11:
        case 12:
            return Double.class;
        }

        return String.class;
    }

    @Override
    public boolean isCellEditable( int row, int column ) {
        // Only allow editing for the selected edit row
        if ( row != editRow ) return false;

        switch ( column ) {
        case 1:
        case 2:
        case 3:
        case 4:
        case 5:
        case 6:
        case 7:
        case 8:
            return true;
        }

        // ID, Saldo is not editable
        return false;
    }

    @Override
    public Object getValueAt( int row, int column ) {
        if ( ( row < 0 ) || ( row >= rekeningRecordList.size( ) ) ) {
            logger.severe( "Invalid row: " + row );
            return null;
        }

        final RekeningRecord rekeningRecord = rekeningRecordList.get( row );

        if ( column == 0 ) return rekeningRecord.rekeningId;
        if ( column == 1 ) return rekeningRecord.rekeningString;
        if ( column == 2 ) return rekeningRecord.rekeningNummerString;
        if ( column == 3 ) return rekeningRecord.rekeningTypeString;
        if ( column == 4 ) return rekeningRecord.fondsString;
        if ( column == 5 ) return rekeningRecord.currencyString;
        if ( column == 6 ) return rekeningRecord.aktief;
        if ( column == 7 ) return rekeningRecord.startDatumString;
        if ( column == 8 ) return rekeningRecord.startSaldo;
        if ( column == 9 ) return rekeningRecord.datumString;
        if ( column == 10 ) return rekeningRecord.saldo;
        if ( column == 11 ) return rekeningRecord.waarde;
        if ( column == 12 ) return rekeningRecord.koers;

        return "";
    }

    @Override
    public void setValueAt( Object object, int row, int column ) {
        if ( ( row < 0 ) || ( row >= rekeningRecordList.size( ) ) ) {
            logger.severe( "Invalid row: " + row );
            return;
        }

        try {
            switch ( column ) {
            case 1:
                String rekeningString = ( String )object;
                if ( ( rekeningString == null ) && ( rekeningRecord.rekeningString != null ) ) {
                    rekeningRecord.rekeningString = null;
                    rowModified = true;
                } else if ( ( rekeningString != null ) &&
                            ( !rekeningString.equals( rekeningRecord.rekeningString ) ) ) {
                    rekeningRecord.rekeningString = rekeningString;
                    rowModified = true;
                }
                break;

            case 2:
                String rekeningNummerString = ( String )object;
                if ( ( rekeningNummerString == null ) && ( rekeningRecord.rekeningNummerString != null ) ) {
                    rekeningRecord.rekeningNummerString = null;
                    rowModified = true;
                } else if ( ( rekeningNummerString != null ) &&
                            ( !rekeningNummerString.equals( rekeningRecord.rekeningNummerString ) ) ) {
                    rekeningRecord.rekeningNummerString = rekeningNummerString;
                    rowModified = true;
                }
                break;

            case 3:
                int rekeningTypeId = rekeningTypeComboBox.getRekeningTypeId( ( String )object );
                if ( rekeningTypeId != rekeningRecord.rekeningTypeId ) {
                    rekeningRecord.rekeningTypeId = rekeningTypeId;
                    rekeningRecord.rekeningTypeString = ( String )object;
                    rowModified = true;
                }
                break;

            case 4:
                String fondsString = ( String )object;
                if ( ( fondsString == null ) && ( rekeningRecord.fondsString != null ) ) {
                    rekeningRecord.fondsString = null;
                    rowModified = true;
                } else if ( ( fondsString != null ) &&
                            ( !fondsString.equals( rekeningRecord.fondsString ) ) ) {
                    rekeningRecord.fondsString = fondsString;
                    rowModified = true;
                }
                break;

            case 5:
                int currencyId = currencyComboBox.getCurrencyId( ( String )object );
                if ( currencyId != rekeningRecord.currencyId ) {
                    rekeningRecord.currencyId = currencyId;
                    rekeningRecord.currencyString = ( String )object;
                    rowModified = true;
                }
                break;

            case 6:
                int aktief = ( ( Integer )object );
                if ( aktief != rekeningRecord.aktief ) {
                    rekeningRecord.aktief = aktief;
                    rowModified = true;
                }
                break;

            case 7:
                String startDatumString = ( String )object;
                if ( ( startDatumString == null ) && ( rekeningRecord.startDatumString != null ) ) {
                    rekeningRecord.startDatumString = null;
                    rowModified = true;
                } else if ( ( startDatumString != null ) &&
                            ( !startDatumString.equals( rekeningRecord.startDatumString ) ) ) {
                    rekeningRecord.startDatumString = startDatumString;
                    rowModified = true;
                }
                break;

            case 8:
                double startSaldo = ( ( Double )object );
                if ( startSaldo != rekeningRecord.startSaldo ) {
                    rekeningRecord.startSaldo = startSaldo;
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
        rekeningRecordList.set( row, rekeningRecord );

        if ( rowModified ) {
            // Enable cancel and save buttons
            cancelRekeningButton.setEnabled( true );
            saveRekeningButton.setEnabled( true );
        }

        fireTableCellUpdated( row, column );
    }


    @Override
    public String getColumnName( int column ) {
        return headings[ column ];
    }


    int getNumberOfRecords( ) { return rekeningRecordList.size( ); }


    int getRekeningId( int row ) {
        if ( ( row < 0 ) || ( row >= rekeningRecordList.size( ) ) ) {
            logger.severe( "Invalid row: " + row );
            return 0;
        }

        return ( rekeningRecordList.get( row ) ).rekeningId;
    }


    String getRekeningString( int row ) {
        if ( ( row < 0 ) || ( row >= rekeningRecordList.size( ) ) ) {
            logger.severe( "Invalid row: " + row );
            return null;
        }

        return ( rekeningRecordList.get( row ) ).rekeningString;
    }


    void setEditRow( int editRow ) {
        // Initialize record to be edited
        rekeningRecord = rekeningRecordList.get( editRow );

        // Copy record to use as key in table update
        originalRekeningRecord = new RekeningRecord( rekeningRecord );

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
        rekeningRecordList.set( row, originalRekeningRecord );

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

        String rekeningString = rekeningRecord.rekeningString;
        if ( ( ( rekeningString == null ) || ( rekeningString.length( ) == 0 ) ) &&
             ( originalRekeningRecord.rekeningString != null ) &&
             ( originalRekeningRecord.rekeningString.length( ) != 0 ) ) {
            updateString = "rekening = NULL ";
        } else if ( ( rekeningString != null ) &&
                    ( !rekeningString.equals( originalRekeningRecord.rekeningString ) ) ) {
            updateString = "rekening = '" + rekeningString + "'";
        }

        String rekeningNummerString = rekeningRecord.rekeningNummerString;
        if ( ( ( rekeningNummerString == null ) || ( rekeningNummerString.length( ) == 0 ) ) &&
             ( originalRekeningRecord.rekeningNummerString != null ) &&
             ( originalRekeningRecord.rekeningNummerString.length( ) != 0 ) ) {
            updateString = addToUpdateString( updateString, "nummer = NULL " );
        } else if ( ( rekeningNummerString != null ) &&
                    ( !rekeningNummerString.equals( originalRekeningRecord.rekeningNummerString ) ) ) {
            updateString = addToUpdateString( updateString,
                                              "nummer = '" + rekeningNummerString + "'" );
        }

        int rekeningTypeId = rekeningRecord.rekeningTypeId;
        if ( rekeningTypeId != originalRekeningRecord.rekeningTypeId ) {
            updateString = addToUpdateString( updateString, "type_id = " + rekeningTypeId );
        }

        String fondsString = rekeningRecord.fondsString;
        if ( ( ( fondsString == null ) || ( fondsString.length( ) == 0 ) ) &&
             ( originalRekeningRecord.fondsString != null ) &&
             ( originalRekeningRecord.fondsString.length( ) != 0 ) ) {
            updateString = addToUpdateString( updateString, "fonds = NULL " );
        } else if ( ( fondsString != null ) &&
                    ( !fondsString.equals( originalRekeningRecord.fondsString ) ) ) {
            updateString = addToUpdateString( updateString, "fonds = '" + fondsString + "'" );
        }

        int currencyId = rekeningRecord.currencyId;
        if ( currencyId != originalRekeningRecord.currencyId ) {
            updateString = addToUpdateString( updateString, "currency_id = " + currencyId );
        }

        int aktief = rekeningRecord.aktief;
        if ( aktief != originalRekeningRecord.aktief ) {
            updateString = addToUpdateString( updateString, "aktief = " + aktief );
        }

        String startDatumString = rekeningRecord.startDatumString;
        if ( ( ( startDatumString == null ) || ( startDatumString.length( ) == 0 ) ) &&
             ( originalRekeningRecord.startDatumString != null ) &&
             ( originalRekeningRecord.startDatumString.length( ) != 0 ) ) {
            updateString = addToUpdateString( updateString, "start_datum = NULL " );
        } else if ( ( startDatumString != null ) &&
                    ( !startDatumString.equals( originalRekeningRecord.startDatumString ) ) ) {
            try {
                Date startDatumDate = dateFormat.parse( startDatumString );
                String testStartDatumString = dateFormat.format( startDatumDate );
                logger.info( "start datum date: " + testStartDatumString );
            } catch( ParseException parseException ) {
                logger.severe( "Start datum parse exception: " + parseException.getMessage( ) );
                return;
            }
            updateString = addToUpdateString( updateString,
                                              "start_datum = '" + startDatumString + "'" );
        }

        double startSaldo = rekeningRecord.startSaldo;
        if ( startSaldo != originalRekeningRecord.startSaldo ) {
            updateString = addToUpdateString( updateString, "start_saldo = " + startSaldo );
        }

        // Check if update is not necessary
        if ( updateString == null ) return;

        // Complete the update string, using the saved rekening ID as key
        updateString = ( "UPDATE rekening SET " + updateString +
                         " WHERE rekening_id = " + originalRekeningRecord.rekeningId );

        logger.info( "updateString: " + updateString );

        try {
            Statement statement = connection.createStatement( );
            int nUpdate = statement.executeUpdate( updateString );
            if ( nUpdate != 1 ) {
                logger.severe( "Could not update record with rekening_id " +
                               originalRekeningRecord.rekeningId +
                               " in rekening, nUpdate = " + nUpdate );
                return;
            }
        } catch ( SQLException sqlException ) {
            logger.severe( "SQLException: " + sqlException.getMessage( ) );
            return;
        }

        // Store record in list
        // logger.info( "storing record at row " + row );
        rekeningRecordList.set( row, rekeningRecord );

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
