// Class to setup a TableModel for all records in rekening_mutatie

package financien.rekeningmutatie;

import financien.gui.DebCredComboBox;
import financien.gui.RekeningComboBox;
import financien.gui.RubriekComboBox;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.ResultSet;
import java.sql.Statement;

import javax.swing.*;
import javax.swing.table.*;
import java.util.*;
import java.util.logging.*;
import java.util.regex.*;


public class RekeningMutatieTableModel extends AbstractTableModel {
    private static final long serialVersionUID = 1L;
    private final Logger logger = Logger.getLogger( "financien.rekeningmutatie.RekeningMutatieTableModel" );

    private Connection connection;
    private final String[ ] headings = { "Datum", "Rekening", "Rubriek", "Deb/Cred",
				         "In", "Uit", "Nr", "Jaar", "Maand",
				         "Omschrijving", "Inleg Aandelen" };

    private class RekeningMutatieRecord {
	String  datumString;
	int     rekeningId;
	String  rekeningString;
	int     rekeningTypeId;
	int     rubriekId;
	String  rubriekString;
	int     debCredId;
	String  debCredString;
	double  mutatieIn;
	double  mutatieUit;
	int     volgNummer;
	int     jaar;
	int     maand;
	String  omschrijvingString;
	double  inlegAandelen;

	RekeningMutatieRecord( String  datumString,
                               int     rekeningId,
                               String  rekeningString,
                               int     rekeningTypeId,
                               int     rubriekId,
                               String  rubriekString,
                               int     debCredId,
                               String  debCredString,
                               double  mutatieIn,
                               double  mutatieUit,
                               int     volgNummer,
                               int     jaar,
                               int     maand,
                               String  omschrijvingString,
                               double  inlegAandelen ) {
	    this.datumString = datumString;
	    this.rekeningId = rekeningId;
	    this.rekeningString = rekeningString;
	    this.rekeningTypeId = rekeningTypeId;
	    this.rubriekId = rubriekId;
	    this.rubriekString = rubriekString;
	    this.debCredId = debCredId;
	    this.debCredString = debCredString;
	    this.mutatieIn = mutatieIn;
	    this.mutatieUit = mutatieUit;
	    this.volgNummer = volgNummer;
	    this.jaar = jaar;
	    this.maand = maand;
	    this.omschrijvingString = omschrijvingString;
	    this.inlegAandelen = inlegAandelen;
	}

	// Copy constructor
	RekeningMutatieRecord( RekeningMutatieRecord rekeningMutatieRecord ) {
	    this.datumString = rekeningMutatieRecord.datumString;
	    this.rekeningId = rekeningMutatieRecord.rekeningId;
	    this.rekeningString = rekeningMutatieRecord.rekeningString;
	    this.rekeningTypeId = rekeningMutatieRecord.rekeningTypeId;
	    this.rubriekId = rekeningMutatieRecord.rubriekId;
	    this.rubriekString = rekeningMutatieRecord.rubriekString;
	    this.debCredId = rekeningMutatieRecord.debCredId;
	    this.debCredString = rekeningMutatieRecord.debCredString;
	    this.mutatieIn = rekeningMutatieRecord.mutatieIn;
	    this.mutatieUit = rekeningMutatieRecord.mutatieUit;
	    this.volgNummer = rekeningMutatieRecord.volgNummer;
	    this.jaar = rekeningMutatieRecord.jaar;
	    this.maand = rekeningMutatieRecord.maand;
	    this.omschrijvingString = rekeningMutatieRecord.omschrijvingString;
	    this.inlegAandelen = rekeningMutatieRecord.inlegAandelen;
	}
    }

    private final ArrayList< RekeningMutatieRecord > rekeningMutatieRecordList = new ArrayList< >( 200 );

    private RekeningComboBox rekeningComboBox;
    private RubriekComboBox rubriekComboBox;
    private DebCredComboBox debCredComboBox;

    private JButton cancelMutatieButton;
    private JButton saveMutatieButton;

    private boolean		  rowModified = false;
    private int			  editRow = -1;
    private RekeningMutatieRecord rekeningMutatieRecord = null;
    private RekeningMutatieRecord originalRekeningMutatieRecord = null;

    // Pattern to find a single quote in the titel, to be replaced
    // with escaped quote (the double slashes are really necessary)
    private final Pattern quotePattern = Pattern.compile( "\\'" );

    double sumMutatieIn;
    double sumMutatieOut;


    // Constructor
    public RekeningMutatieTableModel( Connection connection,
                                      JButton    cancelMutatieButton,
                                      JButton    saveMutatieButton ) {
	this.connection = connection;
	this.cancelMutatieButton = cancelMutatieButton;
	this.saveMutatieButton = saveMutatieButton;

	// Create the combo boxes
	rekeningComboBox = new RekeningComboBox( connection, 0, 1, true );
	rubriekComboBox  = new RubriekComboBox( connection, 0, false );
	debCredComboBox  = new DebCredComboBox( connection, 0, "", true );
    }

    public void setupRekeningMutatieTableModel( int rekeningHouderId,
                                                int rekeningId,
						int rubriekId,
						int debCredId,
						String omschrijvingString ) {

	// Setup the table
	try {
	    String rekeningMutatieQueryString =
		"SELECT rekening_mutatie.datum, " +
		"rekening_mutatie.rekening_id, rekening.rekening, rekening.type_id, " +
		"rekening_mutatie.rubriek_id, rubriek.rubriek, " +
		"rekening_mutatie.deb_cred_id, deb_cred.deb_cred, deb_cred.rekening, " +
		"mutatie_in, mutatie_uit, volgnummer, jaar, maand, " +
		"rekening_mutatie.omschrijving, rekening_mutatie.inleg_aandelen " +
		"FROM rekening_mutatie " +
		"LEFT JOIN rekening ON rekening.rekening_id = rekening_mutatie.rekening_id " +
		"LEFT JOIN rubriek ON rubriek.rubriek_id = rekening_mutatie.rubriek_id " +
		"LEFT JOIN deb_cred ON deb_cred.deb_cred_id = rekening_mutatie.deb_cred_id";

            String rekeningSaldoQueryString = "SELECT SUM( mutatie_in ), SUM( mutatie_uit ) FROM rekening_mutatie " +
                    "LEFT JOIN rekening ON rekening.rekening_id = rekening_mutatie.rekening_id ";

	    if ( ( rekeningHouderId != 0 ) ||
                 ( rekeningId != 0 ) ||
		 ( rubriekId != 0 ) ||
		 ( debCredId != 0 ) ||
		 ( ( omschrijvingString != null ) && ( omschrijvingString.length( ) > 0 ) ) ) {
		String rekeningConditionString = " WHERE ";

                if ( rekeningHouderId != 0 ) {
                    rekeningConditionString += " rekening.rekening_houder_id = " + rekeningHouderId;
                    if ( ( rekeningId != 0 ) ||
                            ( rubriekId != 0 ) ||
                            ( debCredId != 0 ) ||
                            ( ( omschrijvingString != null ) && ( omschrijvingString.length( ) > 0 ) ) ) {
                        rekeningConditionString += " AND";
                    }
                }

		if ( rekeningId != 0 ) {
		    rekeningConditionString += " rekening_mutatie.rekening_id = " + rekeningId;
		    if ( ( rubriekId != 0 ) ||
			 ( debCredId != 0 ) ||
			 ( ( omschrijvingString != null ) && ( omschrijvingString.length( ) > 0 ) ) ) {
		        rekeningConditionString += " AND";
		    }
		}

		if ( rubriekId != 0 ) {
		    rekeningConditionString += " rekening_mutatie.rubriek_id = " + rubriekId;
		    if ( ( debCredId != 0 ) ||
			 ( ( omschrijvingString != null ) && ( omschrijvingString.length( ) > 0 ) ) ) {
		        rekeningConditionString += " AND";
		    }
		}

		if ( debCredId != 0 ) {
		    rekeningConditionString += " rekening_mutatie.deb_cred_id = " + debCredId;
		    if ( ( omschrijvingString != null ) && ( omschrijvingString.length( ) > 0 ) ) {
		        rekeningConditionString += " AND";
		    }
		}

		if ( ( omschrijvingString != null ) && ( omschrijvingString.length( ) > 0 ) ) {
		    rekeningConditionString += " rekening_mutatie.omschrijving LIKE '%" + omschrijvingString + "%'";
		}

		rekeningMutatieQueryString += rekeningConditionString;
		rekeningSaldoQueryString += rekeningConditionString;
	    }

	    rekeningMutatieQueryString +=
		" ORDER BY rekening_mutatie.datum DESC, rekening_mutatie.volgnummer DESC, deb_cred.deb_cred";

	    Statement statement = connection.createStatement( );
	    ResultSet resultSet = statement.executeQuery( rekeningMutatieQueryString );

	    // Clear the list
	    rekeningMutatieRecordList.clear( );

	    // Add all query results to the list
	    while ( resultSet.next( ) ) {
		rekeningMutatieRecordList.add( new RekeningMutatieRecord( resultSet.getString( 1 ),
									  resultSet.getInt( 2 ),
									  resultSet.getString( 3 ),
									  resultSet.getInt( 4 ),
									  resultSet.getInt( 5 ),
									  resultSet.getString( 6 ),
									  resultSet.getInt( 7 ),
									  resultSet.getString( 8 ) + " - " + resultSet.getString( 9 ),
									  resultSet.getDouble( 10 ),
									  resultSet.getDouble( 11 ),
									  resultSet.getInt( 12 ),
									  resultSet.getInt( 13 ),
									  resultSet.getInt( 14 ),
									  resultSet.getString( 15 ),
									  resultSet.getDouble( 16 ) ) );
	    }

	    rekeningMutatieRecordList.trimToSize( );

	    // Trigger update of table data
	    fireTableDataChanged( );

	    logger.finer( "sum query: " + rekeningSaldoQueryString );
	    Statement sumStatement = connection.createStatement( );
            ResultSet sumResultSet = sumStatement.executeQuery( rekeningSaldoQueryString );
            if ( !sumResultSet.next( ) ) {
                logger.severe( "Could not get sum in/uit from " + rekeningSaldoQueryString );
            } else {
                sumMutatieIn = sumResultSet.getDouble( 1 );
                sumMutatieOut = sumResultSet.getDouble( 2 );
            }
	} catch ( SQLException sqlException ) {
	    logger.severe( "SQLException: " + sqlException.getMessage( ) );
	}
    }

    public int getRowCount( ) { return rekeningMutatieRecordList.size( ); }

    public int getColumnCount( ) { return 11; }

    // Indicate the class for each column for setting the correct default renderer
    // see file:///home/cvengelen/java/tutorial/uiswing/components/table.html
    public Class getColumnClass( int column ) {
	switch ( column ) {
	case 0:	// datum
	case 1: // rekening
	case 2: // rubriek
	case 3:	// debCred
	case 9: // omschrijving
	    return String.class;

	case 4: // mutatieIn
	case 5: // mutatieUit
	case 10: // inlegAandelen
	    return Double.class;
	}

	// 6: volgNummer
	// 7: jaar
	// 8: maand
	return Integer.class;
    }

    public boolean isCellEditable( int row, int column ) {
	return ( row == editRow );
    }

    public Object getValueAt( int row, int column ) {
	if ( ( row < 0 ) || ( row >= rekeningMutatieRecordList.size( ) ) ) {
	    logger.severe( "Invalid row: " + row );
	    return null;
	}

	final RekeningMutatieRecord rekeningMutatieRecord = rekeningMutatieRecordList.get( row );

	if ( column ==  0 ) return rekeningMutatieRecord.datumString;
	if ( column ==  1 ) return rekeningMutatieRecord.rekeningString;
	if ( column ==  2 ) return rekeningMutatieRecord.rubriekString;
	if ( column ==  3 ) return rekeningMutatieRecord.debCredString;
	if ( column ==  4 ) return rekeningMutatieRecord.mutatieIn;
	if ( column ==  5 ) return rekeningMutatieRecord.mutatieUit;
	if ( column ==  6 ) return rekeningMutatieRecord.volgNummer;
	if ( column ==  7 ) return rekeningMutatieRecord.jaar;
	if ( column ==  8 ) return rekeningMutatieRecord.maand;
	if ( column ==  9 ) return rekeningMutatieRecord.omschrijvingString;
	if ( column == 10 ) return rekeningMutatieRecord.inlegAandelen;

	return "";
    }

    public void setValueAt( Object object, int row, int column ) {
	if ( ( row < 0 ) || ( row >= rekeningMutatieRecordList.size( ) ) ) {
	    logger.severe( "Invalid row: " + row );
	    return;
	}

	try {
	    switch ( column ) {
	    case 0:
		String datumString = ( String )object;
		if ( ( datumString == null ) && ( rekeningMutatieRecord.datumString != null ) ) {
		    rekeningMutatieRecord.datumString = null;
		    rowModified = true;
		} else if ( ( datumString != null ) &&
			    ( !datumString.equals( rekeningMutatieRecord.datumString ) ) ) {
		    rekeningMutatieRecord.datumString = datumString;
		    rowModified = true;
		}
		break;

	    case 1:
		int rekeningId = rekeningComboBox.getRekeningId( ( String )object );
		if ( rekeningId != rekeningMutatieRecord.rekeningId ) {
		    rekeningMutatieRecord.rekeningId = rekeningId;
		    rekeningMutatieRecord.rekeningString = ( String )object;
		    rowModified = true;
		}
		break;

	    case 2:
		int rubriekId = rubriekComboBox.getRubriekId( ( String )object );
		if ( rubriekId != rekeningMutatieRecord.rubriekId ) {
		    rekeningMutatieRecord.rubriekId = rubriekId;
		    rekeningMutatieRecord.rubriekString = ( String )object;
		    rowModified = true;
		}
		break;

	    case 3:
		int debCredId = debCredComboBox.getDebCredId( ( String )object );
		if ( debCredId != rekeningMutatieRecord.debCredId ) {
		    rekeningMutatieRecord.debCredId = debCredId;
		    rekeningMutatieRecord.debCredString = ( String )object;
		    rowModified = true;
		}
		break;

	    case 4:
		double mutatieIn = ( Double )object;
		if ( mutatieIn != rekeningMutatieRecord.mutatieIn ) {
		    rekeningMutatieRecord.mutatieIn = mutatieIn;
		    rowModified = true;
		}
		break;

	    case 5:
		double mutatieUit = ( Double )object;
		if ( mutatieUit != rekeningMutatieRecord.mutatieUit ) {
		    rekeningMutatieRecord.mutatieUit = mutatieUit;
		    rowModified = true;
		}
		break;

	    case 6:
		int volgNummer = ( Integer )object;
		if ( volgNummer != rekeningMutatieRecord.volgNummer ) {
		    rekeningMutatieRecord.volgNummer = volgNummer;
		    rowModified = true;
		}
		break;

	    case 7:
		int jaar = ( Integer )object;
		if ( jaar != rekeningMutatieRecord.jaar ) {
		    rekeningMutatieRecord.jaar = jaar;
		    rowModified = true;
		}
		break;

	    case 8:
		int maand = ( Integer )object;
		if ( maand != rekeningMutatieRecord.maand ) {
		    rekeningMutatieRecord.maand = maand;
		    rowModified = true;
		}
		break;

	    case 9:
		String omschrijvingString = ( String )object;
		if ( ( omschrijvingString == null ) && ( rekeningMutatieRecord.omschrijvingString != null ) ) {
		    rekeningMutatieRecord.omschrijvingString = omschrijvingString;
		    rowModified = true;
		} else if ( ( omschrijvingString != null ) &&
			    ( !omschrijvingString.equals( rekeningMutatieRecord.omschrijvingString ) ) ) {
		    rekeningMutatieRecord.omschrijvingString = omschrijvingString;
		    rowModified = true;
		}

		break;

	    case 10:
		double inlegAandelen = ( Double )object;
		if ( inlegAandelen != rekeningMutatieRecord.inlegAandelen ) {
		    rekeningMutatieRecord.inlegAandelen = inlegAandelen;
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
	rekeningMutatieRecordList.set( row, rekeningMutatieRecord );

	if ( rowModified ) {
	    // Enable cancel and save buttons
	    if ( cancelMutatieButton != null ) cancelMutatieButton.setEnabled( true );
	    if ( saveMutatieButton != null ) saveMutatieButton.setEnabled( true );
	}

	fireTableCellUpdated( row, column );
    }


    public String getColumnName( int column ) {
	return headings[ column ];
    }

    public int getNumberOfRecords( ) { return rekeningMutatieRecordList.size( ); }

    public String getDatumString( int row ) {
	if ( ( row < 0 ) || ( row >= rekeningMutatieRecordList.size( ) ) ) {
	    logger.severe( "Invalid row: " + row );
	    return null;
	}

	return rekeningMutatieRecordList.get( row ).datumString;
    }

    public int getDebCredId( int row ) {
	if ( ( row < 0 ) || ( row >= rekeningMutatieRecordList.size( ) ) ) {
	    logger.severe( "Invalid row: " + row );
	    return 0;
	}

	return rekeningMutatieRecordList.get( row ).debCredId;
    }

    public int getRekeningId( int row ) {
	if ( ( row < 0 ) || ( row >= rekeningMutatieRecordList.size( ) ) ) {
	    logger.severe( "Invalid row: " + row );
	    return 0;
	}

	return rekeningMutatieRecordList.get( row ).rekeningId;
    }

    public String getRekeningString( int row ) {
	if ( ( row < 0 ) || ( row >= rekeningMutatieRecordList.size( ) ) ) {
	    logger.severe( "Invalid row: " + row );
	    return null;
	}

	return rekeningMutatieRecordList.get( row ).rekeningString;
    }

    int getRekeningTypeId( int row ) {
	if ( ( row < 0 ) || ( row >= rekeningMutatieRecordList.size( ) ) ) {
	    logger.severe( "Invalid row: " + row );
	    return 0;
	}

	return rekeningMutatieRecordList.get( row ).rekeningTypeId;
    }

    public int getRubriekId( int row ) {
	if ( ( row < 0 ) || ( row >= rekeningMutatieRecordList.size( ) ) ) {
	    logger.severe( "Invalid row: " + row );
	    return 0;
	}

	return rekeningMutatieRecordList.get( row ).rubriekId;
    }

    public String getRubriekString( int row ) {
	if ( ( row < 0 ) || ( row >= rekeningMutatieRecordList.size( ) ) ) {
	    logger.severe( "Invalid row: " + row );
	    return null;
	}

	return rekeningMutatieRecordList.get( row ).rubriekString;
    }

    public int getVolgNummer( int row ) {
	if ( ( row < 0 ) || ( row >= rekeningMutatieRecordList.size( ) ) ) {
	    logger.severe( "Invalid row: " + row );
	    return 0;
	}

	return rekeningMutatieRecordList.get( row ).volgNummer;
    }

    public void setEditRow( int editRow ) {
	// Initialize record to be edited
	rekeningMutatieRecord = rekeningMutatieRecordList.get( editRow );

	// Copy record to use as key in table update
	originalRekeningMutatieRecord = new RekeningMutatieRecord( rekeningMutatieRecord );

	// Initialize row modified status
	rowModified = false;

	// Allow editing for the selected row
	this.editRow = editRow;
    }

    public void unsetEditRow( ) {
	this.editRow = -1;
    }

    public void cancelEditRow( int row ) {
	// Check if row being canceled equals the row currently being edited
	if ( row != editRow ) return;

	// Check if row was modified
	if ( !rowModified ) return;

	// Initialize row modified status
	rowModified = false;

	// Store original record in list
	rekeningMutatieRecordList.set( row, originalRekeningMutatieRecord );

	// Trigger update of table row data
	fireTableRowUpdated( row );
    }

    private String addToUpdateString( String updateString, String additionalUpdateString ) {
	if ( updateString != null ) {
	    return updateString + ", " + additionalUpdateString;
	}
	return additionalUpdateString;
    }

    public boolean saveEditRow( int row ) {
	String updateString = null;

	// Compare each field with the value in the original record
	// If modified, add entry to update query string

	String datumString = rekeningMutatieRecord.datumString;
	if ( ( ( datumString == null ) || ( datumString.length( ) == 0 ) ) &&
	     ( originalRekeningMutatieRecord.datumString != null ) &&
	     ( originalRekeningMutatieRecord.datumString.length( ) != 0 ) ) {
	    updateString = "datum = NULL ";
	} else if ( ( datumString != null ) &&
		    ( !datumString.equals( originalRekeningMutatieRecord.datumString ) ) ) {
	    updateString = "datum = '" + datumString + "'";
	}

	if ( rekeningMutatieRecord.rekeningId != originalRekeningMutatieRecord.rekeningId ) {
	    updateString = addToUpdateString( updateString,
					      "rekening_id = " + rekeningMutatieRecord.rekeningId );
	}

	if ( rekeningMutatieRecord.rubriekId != originalRekeningMutatieRecord.rubriekId ) {
	    updateString = addToUpdateString( updateString,
					      "rubriek_id = " + rekeningMutatieRecord.rubriekId );
	}

	if ( rekeningMutatieRecord.debCredId != originalRekeningMutatieRecord.debCredId ) {
	    updateString = addToUpdateString( updateString,
					      "deb_cred_id = " + rekeningMutatieRecord.debCredId );
	}

	if ( rekeningMutatieRecord.mutatieIn != originalRekeningMutatieRecord.mutatieIn ) {
	    updateString = addToUpdateString( updateString,
					      "mutatie_in = " + rekeningMutatieRecord.mutatieIn );
	}

	if ( rekeningMutatieRecord.mutatieUit != originalRekeningMutatieRecord.mutatieUit ) {
	    updateString = addToUpdateString( updateString,
					      "mutatie_uit = " + rekeningMutatieRecord.mutatieUit );
	}

	if ( rekeningMutatieRecord.volgNummer != originalRekeningMutatieRecord.volgNummer ) {
	    updateString = addToUpdateString( updateString,
					      "volgnummer = " + rekeningMutatieRecord.volgNummer );
	}

	if ( rekeningMutatieRecord.jaar != originalRekeningMutatieRecord.jaar ) {
	    updateString = addToUpdateString( updateString,
					      "jaar = " + rekeningMutatieRecord.jaar );
	}

	if ( rekeningMutatieRecord.maand != originalRekeningMutatieRecord.maand ) {
	    updateString = addToUpdateString( updateString,
					      "maand = " + rekeningMutatieRecord.maand );
	}

	String omschrijvingString = rekeningMutatieRecord.omschrijvingString;
	if ( ( ( omschrijvingString == null ) || ( omschrijvingString.length( ) == 0 ) ) &&
	     ( originalRekeningMutatieRecord.omschrijvingString != null ) &&
	     ( originalRekeningMutatieRecord.omschrijvingString.length( ) != 0 ) ) {
	    updateString = addToUpdateString( updateString, "omschrijving = NULL " );
	} else if ( ( omschrijvingString != null ) &&
		    ( !omschrijvingString.equals( originalRekeningMutatieRecord.omschrijvingString ) ) ) {
	    // Matcher to find single quotes in omschrijving, in order to replace these
	    // with escaped quotes (the quadruple slashes are really necessary)
	    final Matcher quoteMatcher = quotePattern.matcher( omschrijvingString );
	    updateString = addToUpdateString( updateString,
					      "omschrijving = '" + quoteMatcher.replaceAll( "\\\\'" ) + "'" );
	}

	if ( rekeningMutatieRecord.inlegAandelen != originalRekeningMutatieRecord.inlegAandelen ) {
	    updateString = addToUpdateString( updateString,
					      "inleg_aandelen = " + rekeningMutatieRecord.inlegAandelen );
	}

	// Check if update is not necessary
	if ( updateString == null ) return true;

	updateString = ( "UPDATE rekening_mutatie SET " + updateString +
			 " WHERE rekening_id = " + originalRekeningMutatieRecord.rekeningId +
			 " AND datum = '" + originalRekeningMutatieRecord.datumString + "'" +
			 " AND deb_cred_id = " + originalRekeningMutatieRecord.debCredId +
			 " AND volgnummer = " + originalRekeningMutatieRecord.volgNummer +
			 " AND rubriek_id = " + originalRekeningMutatieRecord.rubriekId );
	logger.info( "updateString: " + updateString );

	try {
	    Statement statement = connection.createStatement( );
	    int nUpdate = statement.executeUpdate( updateString );
	    if ( nUpdate != 1 ) {
	    	logger.severe( "Could not update record with rekening_id " +
			       originalRekeningMutatieRecord.rekeningId +
			       " and datum " + originalRekeningMutatieRecord.datumString +
			       " in rekening_mutatie, nUpdate = " + nUpdate );
	    	return false;
	    }
	} catch ( SQLException sqlException ) {
	    logger.severe( "SQLException: " + sqlException.getMessage( ) );
	    return false;
	}

	// Store record in list
	rekeningMutatieRecordList.set( row, rekeningMutatieRecord );

	// Initialize row modified status
	rowModified = false;

	// Trigger update of table row data
	fireTableRowUpdated( row );

	// Successful completion
	return true;
    }

    private void fireTableRowUpdated( int row ) {
	for ( int column = 0; column < getColumnCount( ); column++ ) {
	    fireTableCellUpdated( row, column );
	}
    }

    public boolean getRowModified( ) { return rowModified; }
}
