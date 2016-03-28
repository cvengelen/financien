// frame to inspect waarde for all rekeningen for a specific date

package financien.gui;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.ResultSet;
import java.sql.Statement;

import java.util.*;
import java.util.logging.*;
import java.text.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.table.*;
import javax.swing.event.*;

import table.*;


public class WaardeFrame {
    final private Logger logger = Logger.getLogger( "financien.gui.WaardeFrame" );

    Connection connection;

    final JFrame frame = new JFrame( "Waarde" );

    WaardeDatumTableModel waardeDatumTableModel;
    
    WaardeDatumComboBox waardeDatumComboBox;
    String selectedWaardeDatumString = null;

    final SimpleDateFormat dateFormat = new SimpleDateFormat( "yyyy-MM-dd" );


    public WaardeFrame( final Connection connection ) {
	this.connection = connection;

	final Container container = frame.getContentPane( );

	// Set grid bag layout manager
	container.setLayout( new GridBagLayout( ) );
	GridBagConstraints constraints = new GridBagConstraints( );
	constraints.anchor = GridBagConstraints.WEST;
	constraints.insets = new Insets( 0, 0, 10, 10 );

	constraints.gridx = 0;
	constraints.gridy = 0;
	container.add( new JLabel( "Datum:" ), constraints );

	// Setup a JComboBox with the results of the query on datum in table waarde
	waardeDatumComboBox = new WaardeDatumComboBox( connection, null );
	constraints.gridx = GridBagConstraints.RELATIVE;
	constraints.gridwidth = 3;
	container.add( waardeDatumComboBox, constraints );

	class WaardeDatumActionListener implements ActionListener {
	    public void actionPerformed( ActionEvent actionEvent ) {
		// Get the selected waarde datum
		selectedWaardeDatumString = waardeDatumComboBox.getSelectedWaardeDatumString( );

		// Check if rekening has been selected
		if ( ( selectedWaardeDatumString == null ) ||
		     ( selectedWaardeDatumString.length( ) == 0 ) ) {
		    return;
		}

		// Setup the waarde table for the specified date
		waardeDatumTableModel.setupWaardeDatumTableModel( selectedWaardeDatumString );
	    }
	}
	waardeDatumComboBox.addActionListener( new WaardeDatumActionListener( ) );



	// Create waarde-datum table from waarde-datum table model
	waardeDatumTableModel = new WaardeDatumTableModel( connection );
	final TableSorter waardeDatumTableSorter = new TableSorter( waardeDatumTableModel );
	final JTable waardeDatumTable = new JTable( waardeDatumTableSorter );
	waardeDatumTableSorter.setTableHeader( waardeDatumTable.getTableHeader( ) );
	// waardeDatumTableSorter.setSortingStatus( 1, TableSorter.ASCENDING );

	// Setup a table with rekening_mutatie records for the selected rubriek
	waardeDatumTable.setAutoResizeMode( JTable.AUTO_RESIZE_OFF );
	waardeDatumTable.getColumnModel( ).getColumn( 0 ).setPreferredWidth( 110 );  // Rekening
	waardeDatumTable.getColumnModel( ).getColumn( 1 ).setPreferredWidth(  30 );  // Rekening Type Id
	waardeDatumTable.getColumnModel( ).getColumn( 2 ).setPreferredWidth( 110 );  // Saldo
	waardeDatumTable.getColumnModel( ).getColumn( 3 ).setPreferredWidth( 110 );  // Koers
	waardeDatumTable.getColumnModel( ).getColumn( 4 ).setPreferredWidth( 110 );  // Waarde
	waardeDatumTable.getColumnModel( ).getColumn( 5 ).setPreferredWidth( 110 );  // Inleg
	waardeDatumTable.getColumnModel( ).getColumn( 6 ).setPreferredWidth( 110 );  // Waarde-Inleg
	waardeDatumTable.getColumnModel( ).getColumn( 7 ).setPreferredWidth( 110 );  // Rendement

	// Set vertical size just enough for 20 entries
	waardeDatumTable.setPreferredScrollableViewportSize( new Dimension( 816, 320 ) );
	constraints.gridx = 0;
	constraints.gridy = 7;
	constraints.gridwidth = 5;
	container.add( new JScrollPane( waardeDatumTable ), constraints );



	// Class to handle button actions
	class ButtonActionListener implements ActionListener {
	    public void actionPerformed( ActionEvent actionEvent ) {
		if ( actionEvent.getActionCommand( ).equals( "close" ) ) {
		    frame.setVisible( false );
		    System.exit( 0 );
		} else if ( actionEvent.getActionCommand( ).equals( "update" ) ) {
		    // Get the selected waarde datum
		    selectedWaardeDatumString = waardeDatumComboBox.getSelectedWaardeDatumString( );

		    // Check if rekening has been selected
		    if ( ( selectedWaardeDatumString == null ) ||
			 ( selectedWaardeDatumString.length( ) == 0 ) ) {
			JOptionPane.showMessageDialog( frame,
						       "Geen datum geselecteerd",
						       "Waarde frame error",
						       JOptionPane.ERROR_MESSAGE );			
			return;
		    }

		    updateWaarde( selectedWaardeDatumString );
		}
	    }
	}
	final ButtonActionListener buttonActionListener = new ButtonActionListener( );

	JPanel buttonPanel = new JPanel( );

	final JButton updateButton = new JButton( "Update" );
	updateButton.addActionListener( buttonActionListener );
	updateButton.setActionCommand( "update" );
	buttonPanel.add( updateButton );

	final JButton closeButton = new JButton( "Close" );
	closeButton.addActionListener( buttonActionListener );
	closeButton.setActionCommand( "close" );
	buttonPanel.add( closeButton );



	constraints.gridx = 1;
	constraints.gridy = 10;
	constraints.gridwidth = 3;
	container.add( buttonPanel, constraints );

	frame.setSize( 950, 650 );
	frame.setDefaultCloseOperation( JFrame.DISPOSE_ON_CLOSE );
	frame.setVisible( true );
    }

    private boolean updateWaarde( String waardeDatumString ) {
	final String euroDatumString = "2002-01-01";
	final String euroKoersenDatumString = "1999-01-01";
	final double euroConv = 2.20371;
	final SimpleDateFormat dateFormat = new SimpleDateFormat( "yyyy-MM-dd" );

	Date waardeDatumDate;
	Date euroDatumDate;
	Date euroKoersenDatumDate;
	try {
	    waardeDatumDate      = dateFormat.parse( waardeDatumString );
	    euroDatumDate        = dateFormat.parse( euroDatumString );
	    euroKoersenDatumDate = dateFormat.parse( euroKoersenDatumString );
	} catch( ParseException parseException ) {
	    logger.severe( "Euro datum parse exception: " + parseException.getMessage( ) );
	    return false;
	}

	// Find nearest date in koersen table
	GregorianCalendar calendar = new GregorianCalendar( );
	calendar.setTime( waardeDatumDate );
	calendar.add( Calendar.MONTH, -1 );
	Date minimumKoersenDatumDate = calendar.getTime( );
	String minimumKoersenDatumString = dateFormat.format( minimumKoersenDatumDate );

	String koersenDatumString = null;
	try {
	    Statement koersenDatumStatement = connection.createStatement( );
	    ResultSet koersenDatumResultSet =
		koersenDatumStatement.executeQuery( "SELECT datum FROM koersen" +
						    " WHERE datum <= '" + waardeDatumString + "'" +
						    " AND datum >= '" + minimumKoersenDatumString + "'" +
						    " ORDER BY datum DESC" );

	    if ( !koersenDatumResultSet.next( ) ) {
		logger.severe( "Could not get record for datum " + waardeDatumString +
			       " in koersen" );
		return false;
	    }

	    koersenDatumString = koersenDatumResultSet.getString( 1 );
	    logger.info( "Datum koersen: " + koersenDatumString );
	} catch ( SQLException sqlException ) {
	    logger.severe( "SQLException in koersenDatumStatement: " + sqlException.getMessage( ) );
	}

	Date koersenDatumDate = null;
	try {
	    koersenDatumDate = dateFormat.parse( koersenDatumString );
	} catch( ParseException parseException ) {
	    logger.severe( "Koersen datum parse exception: " + parseException.getMessage( ) );
	    return false;
	}


	// Loop over all entries in rekening
	try {
	    Statement rekeningStatement = connection.createStatement( );
	    ResultSet rekeningResultSet =
		rekeningStatement.executeQuery( "SELECT rekening_id, rekening, type_id, " +
						"fonds, start_saldo, start_datum, currency_id, " +
						"rekening_type.rekening_pattern " +
						"FROM rekening " +
						"LEFT JOIN rekening_type "+
						"ON rekening.type_id = rekening_type.rekening_type_id " );

	    while ( rekeningResultSet.next( ) ) {
		final int    rekeningId       = rekeningResultSet.getInt( 1 );
		final String rekeningString   = rekeningResultSet.getString( 2 );
		final int    rekeningTypeId   = rekeningResultSet.getInt( 3 );
		final String fonds            = rekeningResultSet.getString( 4 );
		final double startSaldo       = rekeningResultSet.getDouble( 5 );
		final String startDatumString = rekeningResultSet.getString( 6 );
		final int    currencyId       = rekeningResultSet.getInt( 7 );

		final DecimalFormat saldoDecimalFormat   = new DecimalFormat( rekeningResultSet.getString( 8 ) );
		final DecimalFormat euroDecimalFormat    = new DecimalFormat( "EUR #0.00;EUR -#" );
		final DecimalFormat nlgDecimalFormat     = new DecimalFormat( "NLG #0.00;NLG -#" );
		final DecimalFormat usdDecimalFormat     = new DecimalFormat( "USD #0.00;USD -#" );
		final DecimalFormat percentDecimalFormat = new DecimalFormat( "% #0.00;% -#" );

		////////////////////////////////////
		// Determine the account saldo
		////////////////////////////////////

		double saldo = 0;

		try {
		    Statement sumStatement = connection.createStatement( );
		    ResultSet sumResultSet =
			sumStatement.executeQuery( "SELECT SUM( mutatie_in ), SUM( mutatie_uit )" +
						   " FROM rekening_mutatie " +
						   " WHERE rekening_id = " + rekeningId +
						   " AND datum >= '" + startDatumString + "'" +
						   " AND datum <= '" + waardeDatumString + "'" );

		    if ( !sumResultSet.next( ) ) {
			logger.severe( "Could not get sum in/uit for rekening " +
				       rekeningString + " in rekening_mutatie" );
			continue;
		    }

		    saldo = sumResultSet.getDouble( 1 ) - sumResultSet.getDouble( 2 );
		} catch ( SQLException sqlException ) {
		    logger.severe( "SQLException in sumStatement: " + sqlException.getMessage( ) );
		}


		///////////////////////////////////////////////////////////////
		// Determine the start saldo, and convert to EUR if necessary
		///////////////////////////////////////////////////////////////

		if ( startSaldo != 0 ) {
		    // Get a Date object from the start datum string
		    Date startDatumDate;
		    try {
			startDatumDate = dateFormat.parse( startDatumString );
		    } catch( ParseException parseException ) {
			logger.severe( "Start datum parse exception: " + parseException.getMessage( ) );
			continue;
		    }
		    if ( ( startDatumDate.before( euroDatumDate ) ) &&
			 ( ( rekeningTypeId == 1 ) || ( rekeningTypeId == 2 ) ||
			   ( rekeningTypeId == 3 ) || ( rekeningTypeId == 6 ) ) ) {
			logger.info( "converting start saldo from NLG to EUR" );
			saldo += startSaldo / euroConv;
		    } else {
			saldo += startSaldo;
		    }
		}

		// Continue with next rekening if absolute value of rekening is 0
		if ( Math.abs( saldo ) < 0.1 ) continue;

		double waarde = saldo;
		double koers  = 0.0;
		String koersString = "";
		double inlegAandelen = 0;
		double waardeMinusInleg = 0;
		double rendement = 0;

		// Check the type of account
		if ( ( rekeningTypeId == 4 ) || ( rekeningTypeId == 5 ) ||
		     ( rekeningTypeId == 7 ) ) {

		    ///////////////////////////////////////
		    // Stock account
		    ///////////////////////////////////////

		    ///////////////////////////////////////////////////////////////
		    // Correct the saldo for a stock split, if necessary
		    ///////////////////////////////////////////////////////////////

		    // If a stock was split, all mutatie_in and mutatie_uit have been
		    // corrected for the split. However, the koers still has the original
		    // value. So if the koersen date is before a stock-split, the amount
		    // of stock has to be recalculated as before the split.
		    double splitsFactor = 1.0;

		    try {
			boolean   splits = false;
			Statement splitsStatement = connection.createStatement( );
			ResultSet splitsResultSet =
			    splitsStatement.executeQuery( "SELECT splits_factor, splits_datum " +
							  " FROM splits" +
							  " WHERE rekening_id = " + rekeningId +
							  " AND splits_datum > '" + koersenDatumString + "'" );

			// The stock may have been split multiple times.
			// Loop over all entries in the result set
			while ( splitsResultSet.next( ) ) {
			    splits = true;
			    splitsFactor *= splitsResultSet.getDouble( 1 );
			    logger.info( "Splits factor increased to " + splitsFactor +
					 " for rekening " + rekeningString +
					 " at splits date " + splitsResultSet.getString( 2 ) );
			}

			// Check if stock split after waarde date
			if ( splits ) {
			    // Correct saldo for split in stock account
			    saldo /= splitsFactor;
			    logger.info( "Saldo converted to " + saldo +
					 " for rekening " + rekeningString +
					 " at waarde date " + waardeDatumString );
			}
		    } catch ( SQLException sqlException ) {
			logger.severe( "SQLException in splitsStatement: " + sqlException.getMessage( ) );
		    }


		    ///////////////////////////////////////////////////////////////
		    // Get the stock koers from the koersen table
		    ///////////////////////////////////////////////////////////////

		    // Before 1-1-1999 the Lucent koers is directly in NLG
		    // and the USD conversion field is empty.
		    // Therefore the default for the USD conversion field is 1.0
		    double usDollarConv = 1.0;

		    try {
			Statement koersenStatement = connection.createStatement( );
			ResultSet koersenResultSet =
			    koersenStatement.executeQuery( "SELECT us_dollar, " + fonds +
							   " FROM koersen" +
							   " WHERE datum = '" + koersenDatumString + "'" );

			if ( !koersenResultSet.next( ) ) {
			    logger.severe( "Could not get record for datum " + koersenDatumString +
					   " in koersen" );
			    continue;
			}

			// The USD conversion field is only present after 1-1-1999
			if ( koersenDatumDate.after( euroKoersenDatumDate ) ) {
			    usDollarConv = koersenResultSet.getDouble( 1 );
			}

			koers = koersenResultSet.getDouble( 2 );
			waarde = saldo * koers;
		    } catch ( SQLException sqlException ) {
			logger.severe( "SQLException in koersenStatement: " + sqlException.getMessage( ) );
		    }


		    //////////////////////////////////////////////////////////////////
		    // Handle NLG to EUR, USD to NLG to EUR, or USD to EUR conversion
		    //////////////////////////////////////////////////////////////////

		    // Check the currency
		    switch ( currencyId ) {
		    case 2:
			// For dates before 1-1-1999, the koers is given in NLG,
			// after 1-1-1999 in EUR.
			if ( koersenDatumDate.before( euroKoersenDatumDate ) ) {
			    // Koers is in NLG, therefore convert to EUR
			    waarde = waarde / euroConv;
			    koersString = nlgDecimalFormat.format( koers );
			} else {
			    koersString = euroDecimalFormat.format( koers );
			}

			break;

		    case 3:
			// For dates before 1-1-1999, the koers is given in NLG,
			// after 1-1-1999 in USD.
			if ( koersenDatumDate.before( euroKoersenDatumDate ) ) {
			    // Koers is in NLG
			    koersString = nlgDecimalFormat.format( koers );
			} else {
			    // Koers is in USD
			    koersString = usdDecimalFormat.format( koers );

			    // After 1-1-1999 the koersen table contains the USD conversion field
			    waarde = waarde * usDollarConv;
			}

			// For dates before 1-1-1999, the koers of US stocks is given in NLG,
			// and for dates between 1-1-1999 and 1-1-2002, the USD conversion field
			// gives the conversion to NLG. So for all dates before 1-1-2002
			// the waarde must be converted from NLG to EUR.
			// (After 1-1-2002 the USD conversion field is the conversion to EUR)
			if ( koersenDatumDate.before( euroDatumDate ) ) {
			    // US stock waarde is in NLG: convert to EUR
			    waarde = waarde / euroConv;
			}

			break;
		    }

		    try {
			Statement inlegStatement = connection.createStatement( );
			ResultSet inlegResultSet =
			    inlegStatement.executeQuery( "SELECT SUM( inleg_aandelen )" +
							 " FROM rekening_mutatie " +
							 " LEFT JOIN rubriek ON rekening_mutatie.rubriek_id = rubriek.rubriek_id "+
							 " WHERE rekening_id = " + rekeningId +
							 " AND rubriek.groep_id = 1000 " +
							 " AND datum >= '" + startDatumString + "'" +
							 " AND datum <= '" + waardeDatumString + "'" );

			if ( !inlegResultSet.next( ) ) {
			    logger.severe( "Could not get sum inleg_aandelen for rekening " + rekeningString +
					   " in rekening_mutatie" );
			    continue;
			}

			inlegAandelen = inlegResultSet.getDouble( 1 );
			waardeMinusInleg = waarde - inlegAandelen;
			if ( Math.abs( inlegAandelen ) > 0.1 ) {
			    rendement = waardeMinusInleg / inlegAandelen;
			}
		    } catch ( SQLException sqlException ) {
			logger.severe( "SQLException in inlegStatement: " + sqlException.getMessage( ) );
		    }

		}
		
		try {
		    Statement waardeStatement = connection.createStatement( );
		    ResultSet waardeResultSet =
			waardeStatement.executeQuery( "SELECT saldo, koers, waarde, inleg, " +
						      "waarde_minus_inleg, rendement " + 
						      " FROM waarde " +
						      " WHERE rekening_id = " + rekeningId +
						      " AND datum = '" + waardeDatumString + "'" );

		    if ( !waardeResultSet.next( ) ) {
			logger.severe( "Could not get record for rekening " + rekeningString +
				       " at date " + waardeDatumString +
				       " in waarde" );
			continue;
		    }

		    final String infoString =
			"\nField  \t\tTable  \tCalculation for rekening " + rekeningString + "\n" +
			"saldo  \t\t" + waardeResultSet.getDouble( 1 ) + "  \t" + saldoDecimalFormat.format( saldo ) + "\n" +
			"koers  \t\t" + waardeResultSet.getDouble( 2 ) + "  \t" + koersString + "\n" +
			"waarde  \t\t" + waardeResultSet.getDouble( 3 ) + "  \t" + euroDecimalFormat.format( waarde ) + "\n" +
			"inleg  \t\t" + waardeResultSet.getDouble( 4 ) + "  \t" + euroDecimalFormat.format( inlegAandelen ) + "\n" +
			"waarde-inleg  \t" + waardeResultSet.getDouble( 5 ) + "  \t" + euroDecimalFormat.format( waardeMinusInleg ) + "\n" +
			"rendement  \t" + waardeResultSet.getDouble( 6 ) + "  \t" + percentDecimalFormat.format( rendement );
		    logger.info( infoString );
		    JOptionPane.showMessageDialog( frame,
						   infoString,
						   "Waarde frame info",
						   JOptionPane.INFORMATION_MESSAGE );			

		} catch ( SQLException sqlException ) {
		    logger.severe( "SQLException in sumStatement: " + sqlException.getMessage( ) );
		}



	    }

	} catch ( SQLException sqlException ) {
	    logger.severe( "SQLException in rekeningStatement: " + sqlException.getMessage( ) );
	}

	return true;
    }

}
