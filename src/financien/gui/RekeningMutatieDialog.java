// Dialog for inserting or updating a record in rekening_mutatie

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


public class RekeningMutatieDialog {
    final Logger logger = Logger.getLogger( "financien.gui.RekeningMutatieDialog" );

    Connection connection;
    JFrame parentFrame;

    RubriekComboBox rubriekComboBox;
    int rubriekId = 0;

    final SimpleDateFormat dateFormat = new SimpleDateFormat( "yyyy-MM-dd" );
    JSpinner datumSpinner;
    String datumString;

    DebCredComboBox debCredComboBox;
    int debCredId = 0;

    RekeningComboBox rekeningComboBox;
    int rekeningId = 0;
    int rekeningHouderId = 1;

    JSpinner volgNummerSpinner;
    int volgNummer;

    JFormattedTextField mutatieInFormattedTextField;
    double mutatieIn;

    JFormattedTextField mutatieUitFormattedTextField;
    double mutatieUit;

    JSpinner jaarSpinner;
    int jaar;

    JSpinner maandSpinner;
    int maand;

    JTextField omschrijvingTextField;
    String omschrijvingString = "";

    DecimalFormat mutatieDecimalFormat;

    JDialog dialog;

    int nUpdate = 0;

    final String insertActionCommand = "insert";
    final String updateActionCommand = "update";


    // Constructor for inserting a record in rekening_mutatie
    public RekeningMutatieDialog( Connection connection,
				  JFrame     parentFrame,
				  int        rubriekId,
				  int        rekeningId,
				  int        rekeningHouderId ) {
	this.connection = connection;
	this.parentFrame = parentFrame;
	this.rubriekId = rubriekId;
	this.rekeningId = rekeningId;
	this.rekeningHouderId = rekeningHouderId;

	setupDialog( "Insert in rekening_mutatie", "Insert", insertActionCommand );
    }


    // Constructor for updating an existing record in rekening mutatie
    public RekeningMutatieDialog( Connection connection,
				  JFrame     parentFrame,
				  int        rubriekId,
				  String     datumString,
				  int	     debCredId,
				  int        rekeningId,
				  int	     rekeningHouderId,
				  int	     volgNummer ) {
	this.connection = connection;
	this.parentFrame = parentFrame;
	this.rubriekId = rubriekId;
	this.datumString = datumString;
	this.debCredId = debCredId;
	this.rekeningId = rekeningId;
	this.rekeningHouderId = rekeningHouderId;
	this.volgNummer = volgNummer;

	try {
	    Statement statement = connection.createStatement( );
	    ResultSet resultSet =
		statement.executeQuery( "SELECT mutatie_in, mutatie_uit, " +
					"jaar, maand, omschrijving, " +
					"rekening_pattern " +
					"FROM rekening_mutatie " +
					"LEFT JOIN rekening ON rekening_mutatie.rekening_id = rekening.rekening_id " +
					"LEFT JOIN rekening_type ON rekening.type_id = rekening_type.rekening_type_id " +
					"WHERE rekening_mutatie.rubriek_id = " + rubriekId +
					" AND rekening_mutatie.datum = '" + datumString + "'" +
					" AND rekening_mutatie.deb_cred_id = " + debCredId +
					" AND rekening_mutatie.rekening_id = " + rekeningId +
					" AND rekening_mutatie.volgnummer = " + volgNummer );

	    if ( ! resultSet.next( ) ) {
		String errorString = "Could not get record from rekening_mutatie" ;
		JOptionPane.showMessageDialog( parentFrame,
					       errorString,
					       "Update rekening_mutatie record",
					       JOptionPane.ERROR_MESSAGE);
		logger.severe( errorString );
		return;
	    }

	    mutatieIn = resultSet.getDouble( 1 );
	    mutatieUit = resultSet.getDouble( 2 );
	    jaar = resultSet.getInt( 3 );
	    maand = resultSet.getInt( 4 );
	    omschrijvingString = resultSet.getString( 5 );
	    logger.info( "mutatieIn: "+ mutatieIn +
			 "\nmutatieUit: " + mutatieUit +
			 "\nomschrijving: " + omschrijvingString );
	    mutatieDecimalFormat = new DecimalFormat( resultSet.getString( 6 ) );

	    if ( resultSet.next( ) ) {
		String errorString = "More than one record found in rekening_mutatie" ;
		JOptionPane.showMessageDialog( parentFrame,
					       errorString,
					       "Update rekening_mutatie record",
					       JOptionPane.ERROR_MESSAGE);
		logger.severe( errorString );
		return;
	    }
	} catch ( SQLException sqlException ) {
	    String errorString = "SQLException: " + sqlException.getMessage( );
	    JOptionPane.showMessageDialog( parentFrame,
					   errorString,
					   "RekeningMutatie dialog",
					   JOptionPane.ERROR_MESSAGE);
	    logger.severe( errorString );
	    return;
	}

	setupDialog( "Update record in rekening_mutatie", "Update", updateActionCommand );
    }

    private void setupDialog( String dialogTitle,
			      String editButtonText,
			      String editButtonActionCommand ) {
	// Create modal dialog for editing rekening_mutatie record
	dialog = new JDialog( parentFrame, dialogTitle, true );

	// Set grid bag layout manager
	Container container = dialog.getContentPane( );
	container.setLayout( new GridBagLayout( ) );

	GridBagConstraints constraints = new GridBagConstraints( );
	constraints.anchor = GridBagConstraints.WEST;
	constraints.insets = new Insets( 0, 0, 10, 10 );



	constraints.gridx = 0;
	constraints.gridy = 0;
	container.add( new JLabel( "Rubriek:" ), constraints );

	// Setup a JComboBox with the results of the query on rubriek
	rubriekComboBox = new RubriekComboBox( connection, rubriekId, true );

	// Disable the rubriek combo box if it is present, but only
	// when inserting a new mutatie record. This makes it possible
	// to modify the rubriek for an existing record.
	if ( ( rubriekId != 0 ) && ( editButtonActionCommand.equals( insertActionCommand ) ) ) {
	    rubriekComboBox.setEnabled( false );
	}

	constraints.gridx = GridBagConstraints.RELATIVE;
	constraints.gridwidth = 2;
	container.add( rubriekComboBox, constraints );

	// Datum
	Date datumDate;
	GregorianCalendar calendar = new GregorianCalendar( );
	if ( ( datumString == null ) || ( datumString.length( ) == 0 ) ){
	    datumDate = calendar.getTime( );
	} else {
	    try {
		datumDate = dateFormat.parse( datumString );
		String testDatumString = dateFormat.format( datumDate );
		logger.info( "datumDate: " + testDatumString );
	    } catch( ParseException parseException ) {
		logger.severe( "Datum parse exception: " + parseException.getMessage( ) );
		return;
	    }
	}

	calendar.add( Calendar.YEAR, -20 );
	Date earliestDate = calendar.getTime( );
	calendar.add( Calendar.YEAR, 40 );
	Date latestDate = calendar.getTime( );
	SpinnerDateModel datumSpinnerDatemodel = new SpinnerDateModel( datumDate,
								       earliestDate,
								       latestDate,
								       Calendar.DAY_OF_MONTH );
	datumSpinner = new JSpinner( datumSpinnerDatemodel );
	datumSpinner.setEditor( new JSpinner.DateEditor( datumSpinner, "dd-MM-yyyy" ) );

	// Disable the datum spinner for an update
	if ( editButtonActionCommand.equals( updateActionCommand ) ) {
	    datumSpinner.setEnabled( false );
	}

	constraints.gridx = 0;
	constraints.gridy = 1;
	constraints.gridwidth = 1;
	container.add( new JLabel( "Datum:" ), constraints );

	constraints.gridx = GridBagConstraints.RELATIVE;
	constraints.gridwidth = 2;
	container.add( datumSpinner, constraints );


	constraints.gridx = 0;
	constraints.gridy = 2;
	constraints.gridwidth = 1;
	container.add( new JLabel( "Deb/Cred:" ), constraints );

	// Setup a JComboBox with the results of the query on debCred
	debCredComboBox = new DebCredComboBox( connection, debCredId, "", true );


	constraints.gridx = GridBagConstraints.RELATIVE;
	constraints.gridwidth = 2;
	container.add( debCredComboBox, constraints );

	class FilterDebCredActionListener implements ActionListener {
	    public void actionPerformed( ActionEvent actionEvent ) {
		String filterDebCredString =
		    ( String )JOptionPane.showInputDialog( dialog,
							   "Deb/Cred:",
							   "Deb/Cred filter dialog",
							   JOptionPane.QUESTION_MESSAGE,
							   null,
							   null,
							   debCredComboBox.getSelectedDebCredString( ) );
		debCredComboBox.setupDebCredComboBox( debCredId, "", filterDebCredString, true );
	    }
	}

	final JButton filterDebCredTracksButton = new JButton( "Filter Deb/Cred" );
	filterDebCredTracksButton.setActionCommand( "filterDebCred" );
	filterDebCredTracksButton.addActionListener( new FilterDebCredActionListener( ) );
	constraints.gridx = GridBagConstraints.RELATIVE;
	constraints.gridwidth = 1;
	container.add( filterDebCredTracksButton, constraints );

	constraints.gridx = 0;
	constraints.gridy = 3;
	constraints.gridwidth = 1;
	container.add( new JLabel( "Rekening:" ), constraints );

	// Setup a JComboBox with the results of the query on rekening,
	// only using the active accounts
	rekeningComboBox = new RekeningComboBox( connection, rekeningId, rekeningHouderId, true );

	// Disable the rekening combo box if it is present.
	// So the rekening can only be selected when inserting a record without
	// a default rekening being present (i.e., called from EditRubriek).
	if ( rekeningId != 0 ) {
	    rekeningComboBox.setEnabled( false );
	}

	constraints.gridx = GridBagConstraints.RELATIVE;
	constraints.gridwidth = 2;
	container.add( rekeningComboBox, constraints );


	constraints.gridx = 0;
	constraints.gridy = 4;
	constraints.gridwidth = 1;
	container.add( new JLabel( "VolgNummer:" ), constraints );

	SpinnerNumberModel volgNummerSpinnerNumberModel = new SpinnerNumberModel( volgNummer, 0, 9999, 1 );
	volgNummerSpinner = new JSpinner( volgNummerSpinnerNumberModel );
	JFormattedTextField volgNummerSpinnerTextField =
	    ( ( JSpinner.DefaultEditor )volgNummerSpinner.getEditor( ) ).getTextField( );
	if ( volgNummerSpinnerTextField != null ) {
	    volgNummerSpinnerTextField.setColumns( 4 );
	}
	constraints.gridx = GridBagConstraints.RELATIVE;
	container.add( volgNummerSpinner, constraints );


	constraints.gridx = 0;
	constraints.gridy = 5;
	container.add( new JLabel( "Mutatie in:" ), constraints );

	mutatieInFormattedTextField = new JFormattedTextField( mutatieDecimalFormat );
	mutatieInFormattedTextField.setValue( new Double( mutatieIn ) );
	mutatieInFormattedTextField.setColumns( 10 );
	constraints.gridx = GridBagConstraints.RELATIVE;
	constraints.gridwidth = 2;
	container.add( mutatieInFormattedTextField, constraints );


	constraints.gridx = 0;
	constraints.gridy = 6;
	constraints.gridwidth = 1;
	container.add( new JLabel( "Mutatie uit:" ), constraints );

	mutatieUitFormattedTextField = new JFormattedTextField( mutatieDecimalFormat );
	mutatieUitFormattedTextField.setValue( new Double( mutatieUit ) );
	mutatieUitFormattedTextField.setColumns( 10 );
	constraints.gridx = GridBagConstraints.RELATIVE;
	constraints.gridwidth = 2;
	container.add( mutatieUitFormattedTextField, constraints );


	constraints.gridx = 0;
	constraints.gridy = 7;
	constraints.gridwidth = 1;
	container.add( new JLabel( "Jaar, maand:" ), constraints );

	SpinnerNumberModel jaarSpinnerNumberModel = new SpinnerNumberModel( jaar, 0, 2100, 1 );
	jaarSpinner = new JSpinner( jaarSpinnerNumberModel );
	JFormattedTextField jaarSpinnerTextField =
	    ( ( JSpinner.DefaultEditor )jaarSpinner.getEditor( ) ).getTextField( );
	if ( jaarSpinnerTextField != null ) {
	    jaarSpinnerTextField.setColumns( 5 );
	}
	constraints.gridx = GridBagConstraints.RELATIVE;
	container.add( jaarSpinner, constraints );

	SpinnerNumberModel maandSpinnerNumberModel = new SpinnerNumberModel( maand, 0, 12, 1 );
	maandSpinner = new JSpinner( maandSpinnerNumberModel );
	JFormattedTextField maandSpinnerTextField =
	    ( ( JSpinner.DefaultEditor )maandSpinner.getEditor( ) ).getTextField( );
	if ( maandSpinnerTextField != null ) {
	    maandSpinnerTextField.setColumns( 3 );
	}
	constraints.gridx = GridBagConstraints.RELATIVE;
	container.add( maandSpinner, constraints );


	constraints.gridx = 0;
	constraints.gridy = 8;
	container.add( new JLabel( "Omschrijving:" ), constraints );

	omschrijvingTextField = new JTextField( omschrijvingString, 60 );
	constraints.gridx = GridBagConstraints.RELATIVE;
	constraints.gridwidth = 3;
	container.add( omschrijvingTextField, constraints );


	// Insert/Update/Cancel buttons
	JPanel buttonPanel = new JPanel( );

	class EditActionListener implements ActionListener {
	    public void actionPerformed( ActionEvent actionEvent ) {
		boolean result = true;

		if ( actionEvent.getActionCommand( ).equals( insertActionCommand ) ) {
		    result = insertRekeningMutatie( );
		} else if ( actionEvent.getActionCommand( ).equals( updateActionCommand ) ) {
		    result = updateRekeningMutatie( );
		}

		// Any other actionCommand, including cancel, has no action
		if ( result ) {
		    dialog.setVisible( false );
		}
	    }
	}
	EditActionListener editActionListener = new EditActionListener( );

	JButton editButton = new JButton( editButtonText );
	editButton.setActionCommand( editButtonActionCommand );
	editButton.addActionListener( editActionListener );
	buttonPanel.add( editButton );

	JButton cancelButton = new JButton( "Cancel" );
	cancelButton.setActionCommand( "cancel" );
	cancelButton.addActionListener( editActionListener );
	buttonPanel.add( cancelButton );

	constraints.gridx = 1;
	constraints.gridy = 10;
	constraints.gridwidth = 2;
	container.add( buttonPanel, constraints );


	dialog.setSize( 900, 500 );
	dialog.setDefaultCloseOperation( JFrame.DISPOSE_ON_CLOSE );
	dialog.setVisible( true );
    }


    private boolean insertRekeningMutatie( ) {
	String insertString = "INSERT INTO rekening_mutatie SET ";

	// Insert Rubriek
	int insertRubriekId = rubriekComboBox.getSelectedRubriekId( );
	if ( insertRubriekId == 0 ) {
	    String errorString = "No Rubriek selected";
	    JOptionPane.showMessageDialog( dialog,
					   errorString,
					   "Insert record in rekening_mutatie",
					   JOptionPane.ERROR_MESSAGE);
	    logger.severe( errorString );
	    return false;
	}
 	insertString += "rubriek_id = " + insertRubriekId;

	// Insert Datum
	String insertDatumString = dateFormat.format( ( Date )datumSpinner.getValue( ) );
	if ( insertDatumString == null ) {
	    logger.severe( "Null datum string" );
	    return false;
	}
	insertString += ", datum = '" + insertDatumString + "'";

	// Insert Deb/Cred
	int insertDebCredId = debCredComboBox.getSelectedDebCredId( );
	if ( insertDebCredId != 0 ) {
	    insertString += ", deb_cred_id = " + insertDebCredId;
	}

	// Insert Rekening
	int insertRekeningId = rekeningComboBox.getSelectedRekeningId( );
	if ( insertRekeningId == 0 ) {
	    String errorString = "No rekening selected";
	    JOptionPane.showMessageDialog( dialog,
					   errorString,
					   "Insert record in rekening_mutatie",
					   JOptionPane.ERROR_MESSAGE);
	    logger.severe( errorString );
	    return false;
	}
	insertString += ", rekening_id = " + insertRekeningId;

	// Insert volgnummer
	int insertVolgNummer = ( ( Integer )( volgNummerSpinner.getValue( ) ) ).intValue( );
	if ( insertVolgNummer != 0 ) insertString += ", volgnummer = " + insertVolgNummer;

	// Insert mutatie in/uit
	try {
	    mutatieInFormattedTextField.commitEdit( );
	    mutatieIn = ( ( Double )( mutatieInFormattedTextField.getValue( ) ) ).doubleValue( );
	    if ( mutatieIn != 0 ) insertString += ", mutatie_in = " + mutatieIn;

	    mutatieUitFormattedTextField.commitEdit( );
	    mutatieUit = ( ( Double )( mutatieUitFormattedTextField.getValue( ) ) ).doubleValue( );
	    if ( mutatieUit != 0 ) insertString += ", mutatie_uit = " + mutatieUit;
	} catch( ParseException parseException ) {
	    logger.severe( "ParseException: " + parseException.getMessage( ) );
	}

	// Insert jaar
	int insertJaar = ( ( Integer )( jaarSpinner.getValue( ) ) ).intValue( );
	if ( insertJaar != 0 ) insertString += ", jaar = " + insertJaar;

	// Insert maand
	int insertMaand = ( ( Integer )( maandSpinner.getValue( ) ) ).intValue( );
	if ( insertMaand != 0 ) insertString += ", maand = " + insertMaand;

	// Insert omschrijving
	String insertOmschrijvingString = omschrijvingTextField.getText( );
	if ( insertOmschrijvingString != null ) {
	    if ( insertOmschrijvingString.length( ) > 0 ) {
		insertString += ", omschrijving = '" + insertOmschrijvingString + "'";
	    }
	}

	logger.info( "insertString: " + insertString );

	try {
	    Statement statement = connection.createStatement( );
	    if ( statement.executeUpdate( insertString ) != 1 ) {
	    	String errorString = ( "Could not insert record with\n" +
				       "rubriek_id  = " + insertRubriekId + "\n" +
				       "datum       = " + insertDatumString + "\n" +
				       "deb_cred_id = " + insertDebCredId + "\n" +
				       "rekening_id = " + insertRekeningId + "\n" +
				       "volgnummer = "  + insertVolgNummer + "\n" +
				       "in rekening_mutatie" );
		JOptionPane.showMessageDialog( dialog,
					       errorString,
					       "Insert record in rekening_mutatie",
					       JOptionPane.ERROR_MESSAGE);
	    	logger.severe( errorString );
	    	return false;
	    }
	} catch ( SQLException sqlException ) {
	    logger.severe( "SQLException: " + sqlException.getMessage( ) );
	    return false;
	}

	return true;
    }


    String updateString = null;

    private void addToUpdateString( String additionalUpdateString ) {
	if ( updateString == null ) {
	    updateString = additionalUpdateString;
	} else {
	    updateString += ", " + additionalUpdateString;
	}
    }

    private boolean updateRekeningMutatie( ) {

	// Check if key values are present and non-null
	if ( rubriekId == 0 ) {
	    logger.severe( "Rubriek key value == 0" );
	    return false;
	}

	if ( datumString == null ) {
	    logger.severe( "Datum string key value == null" );
	    return false;
	}

	if ( rekeningId == 0 ) {
	    logger.severe( "Rekening key value == 0" );
	    return false;
	}

	int result = JOptionPane.showConfirmDialog( dialog,
						    "Update existing record for rekening " +
						    rekeningComboBox.getSelectedRekeningString( ) +
						    " at date " + datumString + " in rekening_mutatie",
						    "Update rekening_mutatie record",
						    JOptionPane.YES_NO_OPTION,
						    JOptionPane.QUESTION_MESSAGE,
						    null );

	if ( result != JOptionPane.YES_OPTION ) return false;

	// Initialise string holding the update query
	updateString = null;

	// Rubriek update
	int updateRubriekId = rubriekComboBox.getSelectedRubriekId( );
	if ( updateRubriekId == 0 ) {
	    String errorString = "No Rubriek selected";
	    JOptionPane.showMessageDialog( dialog,
					   errorString,
					   "Update rekening_mutatie record",
					   JOptionPane.ERROR_MESSAGE);
	    logger.severe( errorString );
	    return false;
	}
	if ( updateRubriekId != rubriekId ) {
	    addToUpdateString( "rubriek_id = " + updateRubriekId );
	}

	// Datum update
	String updateDatumString = dateFormat.format( ( Date )datumSpinner.getValue( ) );
	if ( updateDatumString == null ) {
	    logger.severe( "Datum string update value == null" );
	    return false;
	}
	if ( !( updateDatumString.equals( datumString ) ) ) {
	    addToUpdateString( "datum = '" + updateDatumString + "'" );
	}

	// Deb/Cred update
	int updateDebCredId = debCredComboBox.getSelectedDebCredId( );
	if ( updateDebCredId != debCredId ) {
	    if ( updateDebCredId == 0 ) {
		addToUpdateString( "deb_cred_id = NULL" );
	    } else {
		addToUpdateString( "deb_cred_id = " + updateDebCredId );
	    }
	}

	// Rekening update
	int updateRekeningId = rekeningComboBox.getSelectedRekeningId( );
	if ( updateRekeningId == 0 ) {
	    String errorString = "No rekening selected";
	    JOptionPane.showMessageDialog( dialog,
					   errorString,
					   "Update rekening_mutatie record",
					   JOptionPane.ERROR_MESSAGE);
	    logger.severe( errorString );
	    return false;
	}
	if ( updateRekeningId != rekeningId ) {
	    addToUpdateString( "rekening_id = " + updateRekeningId );
	}

	// Volgnummer update
	int updateVolgNummer = ( ( Integer )( volgNummerSpinner.getValue( ) ) ).intValue( );
	if ( updateVolgNummer != volgNummer ) {
	    addToUpdateString( "volgnummer = " + updateVolgNummer );
	}

	// Mutatie in/uit update
	try {
	    mutatieInFormattedTextField.commitEdit( );
	    // Note: cast to Double does not work, since for 0 a Long object is returned.
	    double updateMutatieIn =
		( ( Number )( mutatieInFormattedTextField.getValue( ) ) ).doubleValue( );
	    if ( updateMutatieIn != mutatieIn ) {
		if ( updateMutatieIn == 0 ) {
		    addToUpdateString( "mutatie_in = NULL "  );
		} else {
		    addToUpdateString( "mutatie_in = " + updateMutatieIn );
		}
	    }

	    mutatieUitFormattedTextField.commitEdit( );
	    // Note: cast to Double does not work, since for 0 a Long object is returned.
	    double updateMutatieUit =
		( ( Number )( mutatieUitFormattedTextField.getValue( ) ) ).doubleValue( );
	    if ( updateMutatieUit != mutatieUit ) {
		if ( updateMutatieUit == 0 ) {
		    addToUpdateString( "mutatie_uit = NULL " );
		} else {
		    addToUpdateString( "mutatie_uit = " + updateMutatieUit );
		}
	    }
	} catch( ParseException parseException ) {
	    logger.severe( "ParseException: " + parseException.getMessage( ) );
	}

	// Jaar update
	int updateJaar = ( ( Integer )( jaarSpinner.getValue( ) ) ).intValue( );
	if ( updateJaar != jaar ) {
	    addToUpdateString( "jaar = " + updateJaar );
	}

	// Maand update
	int updateMaand = ( ( Integer )( maandSpinner.getValue( ) ) ).intValue( );
	if ( updateMaand != maand ) {
	    addToUpdateString( "maand = " + updateMaand );
	}

	// Omschrijving update
	String updateOmschrijvingString = omschrijvingTextField.getText( );
	// No need to update when:
	// - original omschrijving was null and current omschrijving is empty
	// - or when current omschrijving equals the original omschrijving.
	if ( ( ( omschrijvingString != null ) || ( updateOmschrijvingString.length( ) > 0 ) ) &&
	     ( !updateOmschrijvingString.equals( omschrijvingString ) ) ) {
	    if ( updateOmschrijvingString.length( ) == 0 ) {
		addToUpdateString( "omschrijving = NULL" );
	    } else {
		addToUpdateString( "omschrijving = '" + updateOmschrijvingString + "'" );
	    }
	}

	// Check if any update is necessary at all
	if ( updateString == null ) {
	    logger.info( "No update necessary" );
	    return true;
	}

	updateString  = "UPDATE rekening_mutatie SET " + updateString;
	updateString += " WHERE rubriek_id = " + rubriekId;
	updateString += " AND datum = '"       + datumString + "'";
	updateString += " AND deb_cred_id = "  + debCredId;
	updateString += " AND rekening_id = "  + rekeningId;
	updateString += " AND volgnummer = "   + volgNummer;

	logger.info( "updateString: " + updateString );

	try {
	    Statement statement = connection.createStatement( );
	    int nUpdate = statement.executeUpdate( updateString );
	    if ( nUpdate != 1 ) {
	    	String errorString = ( "Error in updating record\n" +
				       "rubriek_id  = " + rubriekId + "\n" +
				       "datum       = " + datumString + "\n" +
				       "deb_cred_id = " + debCredId + "\n" +
				       "rekening_id = " + rekeningId + "\n" +
				       "volgnummer = "  + volgNummer + "\n" +
				       "in rekening_mutatie, nUpdate = " + nUpdate );
		JOptionPane.showMessageDialog( dialog,
					       errorString,
					       "Update rekening_mutatie record",
					       JOptionPane.ERROR_MESSAGE);
	    	logger.severe( errorString );
	    	return false;
	    }
	} catch ( SQLException sqlException ) {
	    logger.severe( "SQLException: " + sqlException.getMessage( ) );
	    return false;
	}

	return true;
    }
}
