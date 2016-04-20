// frame to add or update a record in deb_cred

package financien.gui;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.ResultSet;
import java.sql.Statement;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import java.util.logging.*;
import java.util.regex.*;


public class DebCredDialog {
    final private Logger logger = Logger.getLogger( "financien.gui.DebCredDialog" );

    private Connection connection;
    private JFrame parentFrame;
    private JDialog dialog;

    private int debCredId = 0;
    private String debCredRekeningString;
    private String debCredString;
    private int debCredOmschrijvingNr;
    private String debCredOmschrijvingString;
    private int debCredRubriekId;

    private String mutatieTegenRekeningString;
    private String mutatieNaamOmschrijvingString;

    private String mutatieMededelingenString;
    private IngMededelingenParser ingMededelingenParser;

    final Font dialogFont = new Font( "Dialog", Font.BOLD, 12 );

    JTextField mutatieMededelingenTextField;
    JTextField selectSearchMededelingenTextField;
    JRadioButton selectNaamOmschrijvingRadioButton;
    JRadioButton selectSearchMededelingenRadioButton;
    JRadioButton selectMededelingen1RadioButton;
    JRadioButton selectMededelingen2RadioButton;
    JRadioButton selectMededelingen3RadioButton;
    JRadioButton selectMededelingen4RadioButton;
    DebCredComboBox debCredComboBox;
    JTextField debCredOmschrijvingTextField;
    RubriekComboBox rubriekComboBox;

    JButton insertDebCredButton;
    JButton updateDebCredButton;

    // Pattern to find a single quote in the titel, to be replaced
    // with escaped quote (the double slashes are really necessary)
    final Pattern quotePattern = Pattern.compile( "\\'" );


    public DebCredDialog( final Connection connection,
			  final JFrame	   parentFrame,
			  final int        selectedDebCredId,
			  final String     mutatieTegenRekeningString,
			  final String	   mutatieNaamOmschrijvingString,
			  final String     mutatieMededelingenString ) {
	this.connection = connection;
	this.parentFrame = parentFrame;
	this.debCredId = selectedDebCredId;
	this.mutatieTegenRekeningString = mutatieTegenRekeningString;
	this.mutatieNaamOmschrijvingString = mutatieNaamOmschrijvingString;
	this.mutatieMededelingenString = mutatieMededelingenString;

	initialise( );
    }

    public DebCredDialog( final Connection connection,
                          final JFrame     parentFrame,
                          final int        selectedDebCredId,
                          final String     mutatieTegenRekeningString,
                          final String     mutatieNaamOmschrijvingString,
                          final String     mutatieMededelingenString,
                          final String     mutatieCodeString ) {
        this.connection = connection;
        this.parentFrame = parentFrame;
        this.debCredId = selectedDebCredId;
        this.mutatieTegenRekeningString = mutatieTegenRekeningString;
        this.mutatieNaamOmschrijvingString = mutatieNaamOmschrijvingString;
        this.mutatieMededelingenString = mutatieMededelingenString;

        initialise( );
    }

    private void initialise( ) {
	dialog = new JDialog( parentFrame, "Deb/Cred", true );
	final Container container = dialog.getContentPane( );

	// Set grid bag layout manager
	container.setLayout( new GridBagLayout( ) );
	GridBagConstraints constraints = new GridBagConstraints( );
	constraints.anchor = GridBagConstraints.WEST;
	constraints.insets = new Insets( 0, 0, 10, 10 );

	constraints.gridx = 0;
	constraints.gridy = 0;
	constraints.anchor = GridBagConstraints.WEST;
	constraints.insets = new Insets( 0, 0, 10, 10 );
	container.add( new JLabel( "Mutatie tegenrekening: " ), constraints );

	constraints.gridx = GridBagConstraints.RELATIVE;
	container.add( new JLabel( mutatieTegenRekeningString ), constraints );

	constraints.gridx = 0;
	constraints.gridy = 1;
	container.add( new JLabel( "Mutatie naam/omschrijving: " ), constraints );

	constraints.gridx = GridBagConstraints.RELATIVE;
	container.add( new JLabel( mutatieNaamOmschrijvingString ), constraints );

        constraints.gridx = 0;
        constraints.gridy = 2;
        container.add( new JLabel( "Mutatie mededelingen: " ), constraints );

        mutatieMededelingenTextField = new JTextField( mutatieMededelingenString, 80 );
        mutatieMededelingenTextField.setEditable( false );
        constraints.gridx = GridBagConstraints.RELATIVE;
        constraints.gridwidth = 2;
        container.add( mutatieMededelingenTextField, constraints );
        constraints.gridwidth = 1;

        ingMededelingenParser = new IngMededelingenParser( mutatieMededelingenString );
        mutatieMededelingenTextField.setText( ingMededelingenParser.getMutatieMededelingenStrippedString() );

	for ( int omschrijvingIndex = 0, gridy = 3; omschrijvingIndex < ingMededelingenParser.getNMatches(); omschrijvingIndex++, gridy++ ) {
	    // Put label on frame for this omschrijving substring
	    constraints.gridx = 0;
	    constraints.gridy = gridy;
	    constraints.gridwidth = 1;
	    constraints.anchor = GridBagConstraints.WEST;
	    constraints.insets = new Insets( 0, 0, 10, 10 );
	    container.add( new JLabel( "Mutatie mededelingen " + String.valueOf( omschrijvingIndex + 1 ) + ": " ), constraints );
	    constraints.gridx = GridBagConstraints.RELATIVE;
	    constraints.gridwidth = 2;
	    logger.fine( "mutatieMededelingenSubString" + omschrijvingIndex + ":" + ingMededelingenParser.getMutatieMededelingenSubString(omschrijvingIndex) );
	    container.add( new JLabel( ingMededelingenParser.getMutatieMededelingenSubString(omschrijvingIndex) ), constraints );
	}

	constraints.gridx = 0;
	constraints.gridy = 8;
        constraints.gridwidth = 1;
	constraints.gridheight = 2;
	container.add( new JLabel( "DebCred:" ), constraints );

	// Setup a JComboBox with the results of the query on debCred
	debCredComboBox = new DebCredComboBox( connection, debCredId, mutatieTegenRekeningString, true );
	constraints.gridx = GridBagConstraints.RELATIVE;
	container.add( debCredComboBox, constraints );

	class DebCredActionListener implements ActionListener {
	    public void actionPerformed( ActionEvent actionEvent ) {
		// Get the selected DebCred ID
		debCredId = debCredComboBox.getSelectedDebCredId( );

		// Set the dialog fields to the values related to this Deb/Cred
		setDebCredFields( debCredId );
	    }
	}
	debCredComboBox.addActionListener( new DebCredActionListener( ) );

	class SelectDebCredActionListener implements ActionListener {
	    public void actionPerformed( ActionEvent actionEvent ) {
		if ( actionEvent.getActionCommand( ).equals( "selectRekening" ) ) {
		    debCredComboBox.setupDebCredComboBox( debCredId, mutatieTegenRekeningString, "", true );
		}

		if ( actionEvent.getActionCommand( ).equals( "selectDebCred" ) ) {
		    String selectDebCredString =
			( String )JOptionPane.showInputDialog( dialog,
							       "Deb/Cred:",
							       "Deb/Cred filter dialog",
							       JOptionPane.QUESTION_MESSAGE,
							       null,
							       null,
							       mutatieNaamOmschrijvingString );
		    debCredComboBox.setupDebCredComboBox( debCredId, "", selectDebCredString, true );
		}
	    }
	}
	final SelectDebCredActionListener selectDebCredActionListener = new SelectDebCredActionListener( );

	JButton selectRekeningButton = new JButton( "Select rekening" );
	selectRekeningButton.setActionCommand( "selectRekening" );
	selectRekeningButton.addActionListener( selectDebCredActionListener );
	constraints.gridx = GridBagConstraints.RELATIVE;
	constraints.gridheight = 1;
	container.add( selectRekeningButton, constraints );

	JButton selectDebCredTracksButton = new JButton( "Select Deb/Cred" );
	selectDebCredTracksButton.setActionCommand( "selectDebCred" );
	selectDebCredTracksButton.addActionListener( selectDebCredActionListener );
	constraints.gridy = 9;
	container.add( selectDebCredTracksButton, constraints );


	constraints.gridx = 0;
	constraints.gridy = 10;
	constraints.gridwidth = 1;
	container.add( new JLabel( "Mutatie veld selectie:" ), constraints );

	JPanel selectButtonPanel = new JPanel( new GridBagLayout( ) );

	selectNaamOmschrijvingRadioButton = new JRadioButton( "Mutatie naam/omschrijving" );
	selectNaamOmschrijvingRadioButton.setMnemonic(KeyEvent.VK_N);
	selectNaamOmschrijvingRadioButton.setActionCommand( "selectNaamOmschrijving" );
	selectNaamOmschrijvingRadioButton.setSelected(true);

	GridBagConstraints selectButtonConstraints = new GridBagConstraints( );
	selectButtonConstraints.insets = new Insets( 0, 0, 10, 10 );
	selectButtonConstraints.gridx = 0;
	selectButtonConstraints.gridy = 0;
	selectButtonConstraints.anchor = GridBagConstraints.WEST;
	selectButtonPanel.add( selectNaamOmschrijvingRadioButton, selectButtonConstraints );

        selectSearchMededelingenRadioButton = new JRadioButton( "Zoeken in mutatie mededelingen naar: " );
        selectSearchMededelingenRadioButton.setMnemonic(KeyEvent.VK_M);
        selectSearchMededelingenRadioButton.setActionCommand( "selectSearchMededelingen" );
        selectSearchMededelingenRadioButton.setEnabled( false );
        selectButtonConstraints.gridy = 1;
        selectButtonPanel.add( selectSearchMededelingenRadioButton, selectButtonConstraints );

        selectSearchMededelingenTextField = new JTextField( 20 );
        selectButtonConstraints.gridx = GridBagConstraints.RELATIVE;
        selectButtonPanel.add( selectSearchMededelingenTextField, selectButtonConstraints );
        selectButtonConstraints.gridx = 0;

        class SelectSearchMededelingenActionListener implements ActionListener {
            public void actionPerformed( ActionEvent actionEvent ) {
                selectSearchMededelingenRadioButton.setEnabled( selectSearchMededelingenTextField.getText( ).length( ) > 0 );
            }
        }
        final SelectSearchMededelingenActionListener selectSearchMededelingenActionListener = new SelectSearchMededelingenActionListener( );
        selectSearchMededelingenTextField.addActionListener(selectSearchMededelingenActionListener );

        class SelectSearchMededelingenFocusListener implements FocusListener {
            public void focusGained( FocusEvent focusEvent ) {
                selectSearchMededelingenRadioButton.setEnabled( selectSearchMededelingenTextField.getText( ).length( ) > 0 );
            }
            public void focusLost( FocusEvent focusEvent ) {
                selectSearchMededelingenRadioButton.setEnabled( selectSearchMededelingenTextField.getText( ).length( ) > 0 );
            }
        }
        final SelectSearchMededelingenFocusListener selectSearchMededelingenFocusListener = new SelectSearchMededelingenFocusListener( );
        selectSearchMededelingenTextField.addFocusListener(selectSearchMededelingenFocusListener );

	selectMededelingen1RadioButton = new JRadioButton( "Mutatie mededelingen 1" );
	selectMededelingen1RadioButton.setMnemonic(KeyEvent.VK_1);
	selectMededelingen1RadioButton.setActionCommand( "selectMededelingen1" );
	selectMededelingen1RadioButton.setEnabled( ingMededelingenParser.getMutatieMededelingenSubString(0) != null );
	selectButtonConstraints.gridy = 2;
	selectButtonPanel.add( selectMededelingen1RadioButton, selectButtonConstraints );

	selectMededelingen2RadioButton = new JRadioButton( "Mutatie mededelingen 2" );
	selectMededelingen2RadioButton.setMnemonic(KeyEvent.VK_2);
	selectMededelingen2RadioButton.setActionCommand( "selectMededelingen2" );
	selectMededelingen2RadioButton.setEnabled( ingMededelingenParser.getMutatieMededelingenSubString(1) != null );
	selectButtonConstraints.gridy = 3;
	selectButtonPanel.add( selectMededelingen2RadioButton, selectButtonConstraints );

	selectMededelingen3RadioButton = new JRadioButton( "Mutatie mededelingen 3" );
	selectMededelingen3RadioButton.setMnemonic(KeyEvent.VK_3);
	selectMededelingen3RadioButton.setActionCommand( "selectMededelingen3" );
	selectMededelingen3RadioButton.setEnabled( ingMededelingenParser.getMutatieMededelingenSubString(2) != null );
	selectButtonConstraints.gridy = 4;
	selectButtonPanel.add( selectMededelingen3RadioButton, selectButtonConstraints );

	selectMededelingen4RadioButton = new JRadioButton( "Mutatie mededelingen 4" );
	selectMededelingen4RadioButton.setMnemonic(KeyEvent.VK_4);
	selectMededelingen4RadioButton.setActionCommand( "selectMededelingen4" );
	selectMededelingen4RadioButton.setEnabled( ingMededelingenParser.getMutatieMededelingenSubString(3) != null );
	selectButtonConstraints.gridy = 5;
	selectButtonPanel.add( selectMededelingen4RadioButton, selectButtonConstraints );

	ButtonGroup selectButtonGroup = new ButtonGroup( );
	selectButtonGroup.add( selectNaamOmschrijvingRadioButton );
        selectButtonGroup.add( selectSearchMededelingenRadioButton );
	selectButtonGroup.add( selectMededelingen1RadioButton );
	selectButtonGroup.add( selectMededelingen2RadioButton );
	selectButtonGroup.add( selectMededelingen3RadioButton );
	selectButtonGroup.add( selectMededelingen4RadioButton );

	constraints.gridx = GridBagConstraints.RELATIVE;
	constraints.gridwidth = 2;
	container.add( selectButtonPanel, constraints );

	constraints.gridx = 0;
	constraints.gridy = 11;
        constraints.gridwidth = 1;
	constraints.anchor = GridBagConstraints.WEST;
	constraints.insets = new Insets( 0, 0, 10, 10 );
	container.add( new JLabel( "Deb/Cred omschrijving: " ), constraints );

	debCredOmschrijvingTextField = new JTextField( "", 30 );
	debCredOmschrijvingTextField.setFont( dialogFont );
	constraints.gridx = GridBagConstraints.RELATIVE;
        constraints.gridwidth = 2;
	container.add( debCredOmschrijvingTextField, constraints );

	constraints.gridx = 0;
	constraints.gridy = 12;
        constraints.gridwidth = 1;
	container.add( new JLabel( "Default rubriek:" ), constraints );

	rubriekComboBox = new RubriekComboBox( connection, 0, true );
	constraints.gridx = GridBagConstraints.RELATIVE;
        constraints.gridwidth = 2;
	container.add( rubriekComboBox, constraints );

	JPanel editButtonPanel = new JPanel( );

	class EditDebCredActionListener implements ActionListener {
	    public void actionPerformed( ActionEvent actionEvent ) {
		boolean result = true;

		if ( actionEvent.getActionCommand( ).equals( "insert" ) ) {
		    result = insertDebCred( );
		} else if ( actionEvent.getActionCommand( ).equals( "update" ) ) {
		    result = updateDebCred( );
		}

		// Any other actionCommand, including cancel, has no action
		if ( result ) {
		    dialog.setVisible( false );
		}
	    }
	}
	final EditDebCredActionListener editDebCredActionListener = new EditDebCredActionListener( );

	insertDebCredButton = new JButton( "Insert" );
	insertDebCredButton.setActionCommand( "insert" );
	insertDebCredButton.addActionListener( editDebCredActionListener );
	editButtonPanel.add( insertDebCredButton );

	updateDebCredButton = new JButton( "Update" );
	updateDebCredButton.setActionCommand( "update" );
	updateDebCredButton.setEnabled( false );
	updateDebCredButton.addActionListener( editDebCredActionListener );
	editButtonPanel.add( updateDebCredButton );

	JButton closeButton = new JButton( "Close" );
	closeButton.addActionListener( editDebCredActionListener );
	editButtonPanel.add( closeButton );

	constraints.gridx = 0;
	constraints.gridy = 13;
        constraints.gridwidth = 6;
        constraints.weightx = 0.0;
        constraints.weighty = 0.0;
        constraints.fill = GridBagConstraints.NONE;
        constraints.anchor = GridBagConstraints.CENTER;
	container.add( editButtonPanel, constraints );

	// Set the dialog fields to the values related to this Deb/Cred
	setDebCredFields( debCredId );

	dialog.setSize( 1250, 650 );
	dialog.setDefaultCloseOperation( JFrame.DISPOSE_ON_CLOSE );
	dialog.setVisible( true );
    }


    private void setDebCredFields( int debCredId ) {
	// Check if debCred has been selected
	if ( debCredId == 0 ) {
	    debCredOmschrijvingNr = 0;
	    selectNaamOmschrijvingRadioButton.setSelected( true );

	    debCredRubriekId = 0;
	    rubriekComboBox.setSelectedRubriekId( debCredRubriekId );

	    debCredString = null;

	    debCredOmschrijvingString = "";
	    debCredOmschrijvingTextField.setText( debCredOmschrijvingString );

	    updateDebCredButton.setEnabled( false );

	    return;
	}

	try {
	    Statement statement = connection.createStatement( );
	    ResultSet resultSet = statement.executeQuery( "SELECT rekening, omschrijving_nr, " +
							  "rubriek_id, deb_cred, omschrijving " +
							  "FROM deb_cred WHERE deb_cred_id = " +
							  debCredId );
	    if ( ! resultSet.next( ) ) {
		logger.severe( "Could not get record for deb_cred_id " +
			       debCredId + " in deb_cred" );
		return;
	    }

	    debCredRekeningString = resultSet.getString( 1 );

	    debCredOmschrijvingNr = resultSet.getInt( 2 );
	    switch ( debCredOmschrijvingNr ) {
	    case 0:
		selectNaamOmschrijvingRadioButton.setSelected( true );
		break;
	    case 1:
		selectMededelingen1RadioButton.setSelected( true );
		break;
	    case 2:
		selectMededelingen2RadioButton.setSelected( true );
		break;
	    case 3:
		selectMededelingen3RadioButton.setSelected( true );
		break;
	    case 4:
		selectMededelingen4RadioButton.setSelected( true );
		break;
	    }

	    debCredRubriekId = resultSet.getInt( 3 );
	    rubriekComboBox.setSelectedRubriekId( debCredRubriekId );

	    debCredString = resultSet.getString( 4 );

	    debCredOmschrijvingString = resultSet.getString( 5 );
	    debCredOmschrijvingTextField.setText( debCredOmschrijvingString );

	    updateDebCredButton.setEnabled( true );
	} catch ( SQLException sqlException ) {
	    logger.severe( "SQLException: " + sqlException.getMessage( ) );
	    updateDebCredButton.setEnabled( false );
	}
    }


    private boolean insertDebCred( ) {
	String insertString = "INSERT INTO deb_cred SET";

	// De rekening van een pin automaat is leeg.
	if ( !( mutatieTegenRekeningString.isEmpty( ) ) ) insertString += " rekening = '" + mutatieTegenRekeningString + "',";

	int insertOmschrijvingNr = 0;
	String insertDebCredString = "";
	if ( selectNaamOmschrijvingRadioButton.isSelected( ) ) {
	    insertOmschrijvingNr = 0;
	    insertDebCredString = mutatieNaamOmschrijvingString;
	} else if ( selectSearchMededelingenRadioButton.isSelected( ) ) {
	    insertOmschrijvingNr = 0;
	    insertDebCredString = selectSearchMededelingenTextField.getText( );
        } else if ( selectMededelingen1RadioButton.isSelected( ) ) {
            insertOmschrijvingNr = 1;
            insertDebCredString = ingMededelingenParser.getMutatieMededelingenSubString(0);
	} else if ( selectMededelingen2RadioButton.isSelected( ) ) {
	    insertOmschrijvingNr = 2;
	    insertDebCredString = ingMededelingenParser.getMutatieMededelingenSubString(1);
	} else if ( selectMededelingen3RadioButton.isSelected( ) ) {
	    insertOmschrijvingNr = 3;
	    insertDebCredString = ingMededelingenParser.getMutatieMededelingenSubString(2);
	} else if ( selectMededelingen4RadioButton.isSelected( ) ) {
	    insertOmschrijvingNr = 4;
	    insertDebCredString = ingMededelingenParser.getMutatieMededelingenSubString(3);
	}

	insertString += " omschrijving_nr = " + insertOmschrijvingNr;

	// Matcher to find single quotes in debcred, in order to replace these
	// with escaped quotes (the quadruple slashes are really necessary)
	final Matcher insertDebCredQuoteMatcher = quotePattern.matcher( insertDebCredString );
	insertString += ", deb_cred = '" + insertDebCredQuoteMatcher.replaceAll( "\\\\'" ) + "'";

	String debCredOmschrijvingString = debCredOmschrijvingTextField.getText( );
	if ( debCredOmschrijvingString != null ) {
	    if ( debCredOmschrijvingString.length( ) > 0 ) {
		// Matcher to find single quotes in omschrijvingString, in order to replace these
		// with escaped quotes (the quadruple slashes are really necessary)
		final Matcher debCredOmschrijvingQuoteMatcher = quotePattern.matcher( debCredOmschrijvingString );
		insertString += ", omschrijving = '" + debCredOmschrijvingQuoteMatcher.replaceAll( "\\\\'" ) + "'";
	    }
	}

	int rubriekId = rubriekComboBox.getSelectedRubriekId( );
	if ( rubriekId != 0 ) insertString += ", rubriek_id = " + rubriekId;

	try {
	    Statement statement = connection.createStatement( );
	    ResultSet resultSet = statement.executeQuery( "SELECT MAX( deb_cred_id ) FROM deb_cred" );
	    if ( ! resultSet.next( ) ) {
		logger.severe( "Could not get maximum for deb_cred_id in deb_cred" );
		return false;
	    }
	    debCredId = resultSet.getInt( 1 ) + 1;
	    insertString += ", deb_cred_id = " + debCredId;

	    logger.info( "insertString: " + insertString );
	    if ( statement.executeUpdate( insertString ) != 1 ) {
	    	logger.severe( "Could not insert in deb_cred" );
	    	return false;
	    }
	} catch ( SQLException ex ) {
	    logger.severe( "SQLException: " + ex.getMessage( ) );
	    return false;
	}

        // Sla de gewijzigde deb/cred op voor het deb/cred frame
        debCredString = insertDebCredString;

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

    private boolean updateDebCred( ) {
	if ( debCredId == 0 ) {
	    logger.severe( "No Deb/Cred selected");
	    return false;
	}

	int updateOmschrijvingNr = 0;
	String updateDebCredString = "";
	if ( selectNaamOmschrijvingRadioButton.isSelected( ) ) {
	    updateOmschrijvingNr = 0;
	    updateDebCredString = mutatieNaamOmschrijvingString;
	} else if ( selectSearchMededelingenRadioButton.isSelected( ) ) {
	    updateOmschrijvingNr = 0;
	    updateDebCredString = selectSearchMededelingenTextField.getText( );
        } else if ( selectMededelingen1RadioButton.isSelected( ) ) {
            updateOmschrijvingNr = 1;
            updateDebCredString = ingMededelingenParser.getMutatieMededelingenSubString(0);
	} else if ( selectMededelingen2RadioButton.isSelected( ) ) {
	    updateOmschrijvingNr = 2;
	    updateDebCredString = ingMededelingenParser.getMutatieMededelingenSubString(1);
	} else if ( selectMededelingen3RadioButton.isSelected( ) ) {
	    updateOmschrijvingNr = 3;
	    updateDebCredString = ingMededelingenParser.getMutatieMededelingenSubString(2);
	} else if ( selectMededelingen4RadioButton.isSelected( ) ) {
	    updateOmschrijvingNr = 4;
	    updateDebCredString = ingMededelingenParser.getMutatieMededelingenSubString(3);
	}

	int result = JOptionPane.showConfirmDialog( dialog,
						    "Rekening " + debCredRekeningString +" in Deb/Cred bijwerken met\nrekening " +
						    mutatieTegenRekeningString + " met naam " + updateDebCredString + " ?",
						    "Deb/Cred",
						    JOptionPane.YES_NO_OPTION,
						    JOptionPane.QUESTION_MESSAGE,
						    null );

	if ( result != JOptionPane.YES_OPTION ) return false;

	// Initialise string holding the update query
	updateString = null;

	if ( mutatieTegenRekeningString == null ) {
	    logger.severe( "Tegenrekening in mutatie data is null" );
	    return false;
	}
	addToUpdateString( "rekening = '" + mutatieTegenRekeningString + "'" );

	if ( updateOmschrijvingNr != debCredOmschrijvingNr ) {
	    addToUpdateString( "omschrijving_nr = '" + updateOmschrijvingNr + "'" );
	}

	int rubriekId = rubriekComboBox.getSelectedRubriekId( );
	if ( rubriekId != debCredRubriekId ) {
	    addToUpdateString( "rubriek_id = '" + rubriekId + "'" );
	}

	// Do not update when current Deb/Cred is null and update Deb/Cred is also null,
	// or when current Deb/Cred equals update Deb/Cred
	if ( ( ( debCredString != null ) || ( updateDebCredString.length( ) > 0 ) ) &&
	     ( !updateDebCredString.equals( debCredString ) ) ) {
	    if ( updateDebCredString.length( ) == 0 ) {
		addToUpdateString( "deb_cred = NULL" );
	    } else {
		// Matcher to find single quotes in debcred, in order to replace these
		// with escaped quotes (the quadruple slashes are really necessary)
		final Matcher quoteMatcher = quotePattern.matcher( updateDebCredString );
		addToUpdateString( "deb_cred = '" + quoteMatcher.replaceAll( "\\\\'" ) + "'" );
	    }
	}

	String updateDebCredOmschrijvingString = debCredOmschrijvingTextField.getText( );

	// Do not update when current omschrijving is null and update omschrijving is also null,
	// or when current omschrijving equals update omschrijving
	if ( ( ( debCredOmschrijvingString != null ) || ( updateDebCredOmschrijvingString.length( ) > 0 ) ) &&
	     ( !updateDebCredOmschrijvingString.equals( debCredOmschrijvingString ) ) ) {
	    if ( updateDebCredOmschrijvingString.length( ) == 0 ) {
		addToUpdateString( "omschrijving = NULL" );
	    } else {
		// Matcher to find single quotes in omschrijving, in order to replace these
		// with escaped quotes (the quadruple slashes are really necessary)
		final Matcher quoteMatcher = quotePattern.matcher( updateDebCredOmschrijvingString );
		addToUpdateString( "omschrijving = '" + quoteMatcher.replaceAll( "\\\\'" ) + "'" );
	    }
	}

	// Check if any update is necessary at all
	if ( updateString == null ) {
	    logger.info( "No update necessary" );
	    return true;
	}

	updateString  = "UPDATE deb_cred SET " + updateString;
	updateString += " WHERE deb_cred_id = " + debCredId;
	logger.info( "updateString: " + updateString );

	try {
	    Statement statement = connection.createStatement( );
	    int nUpdate = statement.executeUpdate( updateString );
	    if ( nUpdate != 1 ) {
	    	String errorString = ( "Could not update record with deb_cred_id " + debCredId +
				       " in deb_cred" );
		JOptionPane.showMessageDialog( dialog,
					       errorString,
					       "Deb/Cred",
					       JOptionPane.ERROR_MESSAGE);
	    	logger.severe( errorString );
	    	return false;
	    }
	} catch ( SQLException ex ) {
	    logger.severe( "SQLException: " + ex.getMessage( ) );
	    return false;
	}

        // Sla de gewijzigde deb/cred op voor het deb/cred frame
        debCredString = updateDebCredString;

	return true;
    }

    public int getDebCredId( ) {
	return debCredId;
    }

    public int getRubriekId( ) {
	return rubriekComboBox.getSelectedRubriekId( );
    }

    public String getDebCredString( ) {
        return debCredString;
    }
}
