// frame to copy downloaded ING mutatie records to rekening_mutatie, and deb_cred

package financien.ingmutatie;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.ResultSet;
import java.sql.Statement;

import java.util.*;
import java.util.logging.*;
import java.util.regex.*;
import java.text.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.table.*;
import javax.swing.border.*;
import javax.swing.event.*;

import financien.gui.DebCredDialog;
import financien.gui.IngMededelingenParser;
import financien.rekeningmutatie.RekeningMutatieTableModel;
import financien.gui.RubriekComboBox;
import table.*;

public class IngMutatieFrame {
    final private Logger logger = Logger.getLogger( IngMutatieFrame.class.getCanonicalName() );

    private Connection connection;

    private final JFrame frame = new JFrame( "Download ING" );
    private final Font dialogFont = new Font( "Dialog", Font.BOLD, 12 );

    private String mutatieDatumString;
    private JLabel mutatieDatumLabel;

    private String mutatieTegenRekeningString;
    private JLabel mutatieTegenRekeningLabel;

    private String mutatieNaamOmschrijvingString;
    private JLabel mutatieNaamOmschrijvingLabel;

    private int    debCredId = 0;
    private JLabel debCredIdLabel;

    private JSpinner volgNummerSpinner;
    private SpinnerNumberModel volgNummerSpinnerNumberModel;

    private double mutatieBedrag;
    private JLabel mutatieBedragLabel;

    private String mutatieAfBijString;
    private JLabel mutatieAfBijLabel;

    private JSpinner jaarSpinner;
    private JSpinner maandSpinner;

    private String mutatieMededelingenString;
    private JTextField mutatieMededelingenTextField;

    private String mutatieCodeString;
    private String mutatieEigenRekeningString;
    private int eigenRekeningId = 0;

    private int rubriekId = 0;
    private RubriekComboBox rubriekComboBox;

    private RekeningMutatieTableModel rekeningMutatieTableModel;
    private TableSorter rekeningMutatieTableSorter;
    private JTable rekeningMutatieTable;
    private final DecimalFormat euroDecimalFormat = new DecimalFormat( "EUR #0.00;EUR -#" );

    private final SimpleDateFormat dateFormat = new SimpleDateFormat( "yyyy-MM-dd" );
    private final GregorianCalendar calendar = new GregorianCalendar( );

    private ResultSet mutatieResultSet;

    // Pattern to find a single quote in the titel, to be replaced
    // with escaped quote (the double slashes are really necessary)
    private final Pattern quotePattern = Pattern.compile( "\\'" );


    public IngMutatieFrame( final Connection connection ) {
        this.connection = connection;

	// frame.setBackground( Color.white );

	// Get the container for the frame
	final Container container = frame.getContentPane( );
	// container.setBackground( Color.white );

	// Set grid bag layout manager
	container.setLayout( new GridBagLayout( ) );
	GridBagConstraints constraints = new GridBagConstraints( );
	final Insets insetsLeft  = new Insets( 5, 20, 5, 5 );
	final Insets insetsRight = new Insets( 5, 5, 5, 20 );
	constraints.anchor = GridBagConstraints.WEST;
	constraints.insets = insetsLeft;

	constraints.gridx = 0;
	constraints.gridy = 0;
	container.add( new JLabel( "Datum:" ), constraints );

	mutatieDatumLabel = new JLabel( );
	constraints.gridx = GridBagConstraints.RELATIVE;
	constraints.insets = insetsRight;
	container.add( mutatieDatumLabel, constraints );

	constraints.gridx = 0;
	constraints.gridy = 1;
	constraints.insets = insetsLeft;
	container.add( new JLabel( "Deb/Cred:" ), constraints );

	mutatieTegenRekeningLabel = new JLabel( );
	constraints.gridx = GridBagConstraints.RELATIVE;
	constraints.insets = insetsRight;
	constraints.weightx = 1.0;
	constraints.gridwidth = 2;
	container.add( mutatieTegenRekeningLabel, constraints );

	mutatieNaamOmschrijvingLabel = new JLabel( );
	constraints.gridx = GridBagConstraints.RELATIVE;
	constraints.weightx = 4.0;
	constraints.gridwidth = 1;
	container.add( mutatieNaamOmschrijvingLabel, constraints );

	debCredIdLabel = new JLabel( );
	constraints.gridx = GridBagConstraints.RELATIVE;
	constraints.weightx = 1.0;
	container.add( debCredIdLabel, constraints );

	class EditDebCredButtonActionListener implements ActionListener {
	    public void actionPerformed( ActionEvent actionEvent ) {
		DebCredDialog debCredDialog = new DebCredDialog( connection, frame,
								 debCredId,
								 mutatieTegenRekeningString,
								 mutatieNaamOmschrijvingString,
								 mutatieMededelingenString,
								 mutatieCodeString );

		debCredId = debCredDialog.getDebCredId( );
		rubriekId = debCredDialog.getRubriekId( );

		// Setup the rekening_mutatie table for the selected rubriek and deb-cred id
		setupRekeningMutatieTable( );
	    }
	}

	JButton editDebCredButton = new JButton( "Edit Deb/Cred" );
	editDebCredButton.addActionListener( new EditDebCredButtonActionListener( ) );
	constraints.gridx = GridBagConstraints.RELATIVE;
	constraints.weightx = 1.0;
	container.add( editDebCredButton, constraints );


	constraints.gridx = 0;
	constraints.gridy = 2;
	constraints.weightx = 0.0;
	constraints.insets = insetsLeft;
	container.add( new JLabel( "Mutatie:" ), constraints );

	mutatieBedragLabel = new JLabel( );
	constraints.gridx = GridBagConstraints.RELATIVE;
	constraints.insets = insetsRight;
	container.add( mutatieBedragLabel, constraints );

	mutatieAfBijLabel = new JLabel( );
	constraints.gridx = GridBagConstraints.RELATIVE;
	container.add( mutatieAfBijLabel, constraints );

	constraints.gridx = 0;
	constraints.gridy = 3;
	constraints.insets = insetsLeft;
	container.add( new JLabel( "VolgNummer:" ), constraints );
	volgNummerSpinnerNumberModel = new SpinnerNumberModel( 0, 0, 10, 1 );
	volgNummerSpinner = new JSpinner( volgNummerSpinnerNumberModel );
	JFormattedTextField volgNummerSpinnerTextField =
	    ( ( JSpinner.DefaultEditor )volgNummerSpinner.getEditor( ) ).getTextField( );
	if ( volgNummerSpinnerTextField != null ) {
	    volgNummerSpinnerTextField.setColumns( 3 );
            // volgNummerSpinnerTextField.setBackground( Color.white );
            volgNummerSpinnerTextField.setFont( dialogFont );
	}
	constraints.gridx = GridBagConstraints.RELATIVE;
	constraints.insets = insetsRight;
	container.add( volgNummerSpinner, constraints );

	constraints.gridx = 0;
	constraints.gridy = 4;
	constraints.insets = insetsLeft;
	container.add( new JLabel( "Jaar, Maand:" ), constraints );

	int year = calendar.get( Calendar.YEAR );
	SpinnerNumberModel jaarSpinnerNumberModel = new SpinnerNumberModel( year, 1990, year + 1, 1 );
	jaarSpinner = new JSpinner( jaarSpinnerNumberModel );
	JFormattedTextField jaarSpinnerTextField =
	    ( ( JSpinner.DefaultEditor )jaarSpinner.getEditor( ) ).getTextField( );
	if ( jaarSpinnerTextField != null ) {
	    jaarSpinnerTextField.setColumns( 5 );
            // jaarSpinnerTextField.setBackground( Color.white );
            jaarSpinnerTextField.setFont( dialogFont );
	}
	constraints.gridx = GridBagConstraints.RELATIVE;
	constraints.insets = insetsRight;
	container.add( jaarSpinner, constraints );

	SpinnerNumberModel maandSpinnerNumberModel = new SpinnerNumberModel( getMonth( ), 0, 12, 1 );
	maandSpinner = new JSpinner( maandSpinnerNumberModel );
	JFormattedTextField maandSpinnerTextField =
	    ( ( JSpinner.DefaultEditor )maandSpinner.getEditor( ) ).getTextField( );
	if ( maandSpinnerTextField != null ) {
	    maandSpinnerTextField.setColumns( 3 );
            // maandSpinnerTextField.setBackground( Color.white );
            maandSpinnerTextField.setFont( dialogFont );
	}
	constraints.gridx = GridBagConstraints.RELATIVE;
	container.add( maandSpinner, constraints );

	constraints.gridx = 0;
	constraints.gridy = 5;
	constraints.insets = insetsLeft;
	container.add( new JLabel( "Omschrijving:" ), constraints );

	mutatieMededelingenTextField = new JTextField( "", 60 );
	// mutatieMededelingenTextField.setBackground( Color.white );
	mutatieMededelingenTextField.setFont( dialogFont );

	constraints.gridx = GridBagConstraints.RELATIVE;
	constraints.gridwidth = 5;
	constraints.weightx = 1.0;
	constraints.insets = insetsRight;
	constraints.fill = GridBagConstraints.HORIZONTAL;
	container.add( mutatieMededelingenTextField, constraints );


	constraints.gridx = 0;
	constraints.gridy = 6;
	constraints.gridwidth = 1;
	constraints.weightx = 0.0;
	constraints.insets = insetsLeft;
	constraints.fill = GridBagConstraints.NONE;
	container.add( new JLabel( "Rubriek:" ), constraints );

	// Setup a JComboBox with the results of the query on rubriek
	rubriekComboBox = new RubriekComboBox( connection, 0, true );
	constraints.gridx = GridBagConstraints.RELATIVE;
	constraints.gridwidth = 4;
	constraints.insets = insetsRight;
	container.add( rubriekComboBox, constraints );

	class RubriekActionListener implements ActionListener {
	    public void actionPerformed( ActionEvent actionEvent ) {
		// Get the selected Rubriek ID
		rubriekId = rubriekComboBox.getSelectedRubriekId( );

		// Check if rubriek has been selected
		if ( rubriekId == 0 ) {
		    return;
		}

		// Setup the rekening_mutatie table for the selected rubriek
		setupRekeningMutatieTable( );
	    }
	}
	rubriekComboBox.addActionListener( new RubriekActionListener( ) );

	// Define the edit, cancel, save and buttons because
	// the cancel/save buttons are enabled by the table model.
	final JButton copyButton = new JButton( "Copy" );
	final JButton editMutatieButton = new JButton( "Edit" );
	final JButton cancelMutatieButton = new JButton( "Cancel" );
	final JButton saveMutatieButton = new JButton( "Save" );
	final JButton deleteMutatieButton = new JButton( "Delete" );

	// Create rekening_mutatie table from rekening_mutatie table model
	rekeningMutatieTableModel = new RekeningMutatieTableModel( connection,
								   cancelMutatieButton,
								   saveMutatieButton );
	rekeningMutatieTableSorter = new TableSorter( rekeningMutatieTableModel );
	rekeningMutatieTable = new JTable( rekeningMutatieTableSorter );
	// rekeningMutatieTable.setBackground( Color.white );
	rekeningMutatieTableSorter.setTableHeader( rekeningMutatieTable.getTableHeader( ) );
	// rekeningMutatieTableSorter.setSortingStatus( 0, TableSorter.DESCENDING );

	rekeningMutatieTable.setAutoResizeMode( JTable.AUTO_RESIZE_OFF );

	// Set vertical size just enough for 12 entries
	rekeningMutatieTable.setPreferredScrollableViewportSize( new Dimension( 886, 192 ) );

	// Set renderer for Double objects
	class DoubleRenderer extends JTextField implements TableCellRenderer {
	    public Component getTableCellRendererComponent( JTable table,
							    Object object,
							    boolean isSelected,
							    boolean hasFocus,
							    int row, int column ) {
		switch ( column ) {
		case 4:		// MutatieIn
		case 5:		// MutatieUit
		case 10:
		    final double mutatie = ( Double )object;
		    if ( mutatie == 0 ) {
			// Return empty string
			this.setText( "" );
		    } else {
			// Use the formatter defined for EUR
			this.setText( euroDecimalFormat.format( mutatie ) );
		    }

		    break;

		default:	// Unexpected column: just return the string related to the object
		    logger.severe( "Unexpected column: " + column );
		    this.setText( object.toString( ) );
		}

		return this;
	    }
	}
	DoubleRenderer doubleRenderer = new DoubleRenderer( );
	doubleRenderer.setHorizontalAlignment( JTextField.RIGHT );
	doubleRenderer.setEnabled( false );
	final Border emptyBorder = BorderFactory.createEmptyBorder( );
	doubleRenderer.setBorder( emptyBorder );
	rekeningMutatieTable.setDefaultRenderer( Double.class, doubleRenderer );

	final JScrollPane scrollPane = new JScrollPane( rekeningMutatieTable );
	// scrollPane.setBackground( Color.white );
	// scrollPane.getViewport( ).setBackground( Color.white );

	constraints.gridx = 0;
	constraints.gridy = 7;
	constraints.gridwidth = 6;
	constraints.insets = new Insets( 10, 20, 5, 20 );
	// Setting weighty and fill is necessary for proper filling the frame when resized.
	constraints.weightx = 1.0;
	constraints.weighty = 1.0;
	constraints.fill = GridBagConstraints.BOTH;
	constraints.anchor = GridBagConstraints.CENTER;
	container.add( scrollPane, constraints );

	try {
	    final Statement statement = connection.createStatement( );
	    mutatieResultSet = statement.executeQuery( "SELECT datum, tegen_rekening, naam_omschrijving, " +
						       "mutatie, af_bij, mededelingen, code, eigen_rekening " +
						       "FROM ing_mutatie ORDER BY datum, naam_omschrijving" );
	} catch ( SQLException sqlException ) {
	    logger.severe( "SQLException: " + sqlException.getMessage( ) );
	    frame.setVisible( false );
	    System.exit( 0 );
	}

	getNextMutatieRecord( );


	////////////////////////////////////////////////
	// List selection listener
	////////////////////////////////////////////////

	// Get the selection model related to the rekening_mutatie table
	final ListSelectionModel mutatieListSelectionModel = rekeningMutatieTable.getSelectionModel( );

	class MutatieListSelectionListener implements ListSelectionListener {
	    private int selectedRow = -1;

	    public void valueChanged( ListSelectionEvent listSelectionEvent ) {
		// Ignore extra messages.
		if ( listSelectionEvent.getValueIsAdjusting( ) ) return;

		// Check if current row has modified values
		if ( rekeningMutatieTableModel.getRowModified( ) ) {
		    if ( selectedRow == -1 ) {
			logger.severe( "Invalid selected row" );
		    } else {
			int result =
			    JOptionPane.showConfirmDialog( frame,
							   "Data zijn gewijzigd: modificaties opslaan?",
							   "Record is gewijzigd",
							   JOptionPane.YES_NO_OPTION,
							   JOptionPane.QUESTION_MESSAGE,
							   null );

			if ( result == JOptionPane.YES_OPTION ) {
			    // Save the changes in the table model, and in the database
			    rekeningMutatieTableModel.saveEditRow( selectedRow );
			} else {
			    // Cancel any edits in the selected row
			    rekeningMutatieTableModel.cancelEditRow( selectedRow );
			}
		    }
		}

		// Ignore if nothing is selected
		if ( mutatieListSelectionModel.isSelectionEmpty( ) ) {
		    selectedRow = -1;

		    editMutatieButton.setEnabled( false );
		    cancelMutatieButton.setEnabled( false );
		    saveMutatieButton.setEnabled( false );
		    deleteMutatieButton.setEnabled( false );

		    return;
		}

		// Remove the capability to edit the row
		rekeningMutatieTableModel.unsetEditRow( );

		// Get the selected row
		int viewRow = mutatieListSelectionModel.getMinSelectionIndex( );
		selectedRow = rekeningMutatieTableSorter.modelIndex( viewRow );
		logger.info( "viewRow: " + viewRow + ", selectedRow: " + selectedRow );

		// Enable the edit button
		editMutatieButton.setEnabled( true );

		// Disable the cancel and save buttons (these will be enabled
		// when any data in the row is actually modified)
		cancelMutatieButton.setEnabled( false );
		saveMutatieButton.setEnabled( false );

		// Enable the delete button
		deleteMutatieButton.setEnabled( true );
	    }

	    private int getSelectedRow ( ) { return selectedRow; }
	}

	// Add mutatieListSelectionListener object to the selection model of the rekening mutatie table
	final MutatieListSelectionListener mutatieListSelectionListener = new MutatieListSelectionListener( );
	mutatieListSelectionModel.addListSelectionListener( mutatieListSelectionListener );


	//////////////////////////////////////////////////////////
	// Copy, Edit, Cancel, Save, Next, Delete, Close Buttons
	//////////////////////////////////////////////////////////

	class ButtonActionListener implements ActionListener {
	    public void actionPerformed( ActionEvent actionEvent ) {
		if ( actionEvent.getActionCommand( ).equals( "close" ) ) {
		    frame.setVisible( false );
		    System.exit( 0 );
		} else if ( actionEvent.getActionCommand( ).equals( "copy" ) ) {
		    // Copy the record to the rekening mutatie table
		    if ( copyDownloadIngRecord( ) ) {
			// Successful copy: disable the copy button
			copyButton.setEnabled( false );
		    }
		} else {
		    int selectedRow = mutatieListSelectionListener.getSelectedRow( );

		    if ( actionEvent.getActionCommand( ).equals( "next" ) ) {
			// Check if current row has modified values
			if ( rekeningMutatieTableModel.getRowModified( ) ) {
			    if ( selectedRow < 0 ) {
				JOptionPane.showMessageDialog( frame,
							       "Geen mutatie geselecteerd",
							       "Download ING frame error",
							       JOptionPane.ERROR_MESSAGE );
				return;
			    }

			    int result =
				JOptionPane.showConfirmDialog( frame,
							       "Data zijn gewijzigd: modificaties opslaan?",
							       "Record is gewijzigd",
							       JOptionPane.YES_NO_OPTION,
							       JOptionPane.QUESTION_MESSAGE,
							       null );

			    if ( result == JOptionPane.YES_OPTION ) {
				// Save the changes in the table model, and in the database
				rekeningMutatieTableModel.saveEditRow( selectedRow );
			    } else {
				// Cancel any edits
				rekeningMutatieTableModel.cancelEditRow( selectedRow );
			    }
			}

			// Remove the capability to edit the row
			rekeningMutatieTableModel.unsetEditRow( );

			// Disable the edit, cancel and save buttons
			editMutatieButton.setEnabled( false );
			cancelMutatieButton.setEnabled( false );
			saveMutatieButton.setEnabled( false );

			getNextMutatieRecord( );
			copyButton.setEnabled( true );

			return;
		    }

		    if ( selectedRow < 0 ) {
			JOptionPane.showMessageDialog( frame,
						       "Geen mutatie geselecteerd",
						       "DownloadING frame error",
						       JOptionPane.ERROR_MESSAGE );
			return;
		    }

		    if ( actionEvent.getActionCommand( ).equals( "delete" ) ) {
			final String datumString = rekeningMutatieTableModel.getDatumString( selectedRow );
			int result =
			    JOptionPane.showConfirmDialog( frame,
							   "Delete record for rekening " +
							   rekeningMutatieTableModel.getRekeningString( selectedRow ) +
							   " at date " + datumString +
							   " in rekening_mutatie ?",
							   "Delete rekening_mutatie record",
							   JOptionPane.YES_NO_OPTION,
							   JOptionPane.QUESTION_MESSAGE,
							   null );

			if ( result != JOptionPane.YES_OPTION ) return;

			final int rubriekId = rekeningMutatieTableModel.getRubriekId( selectedRow );
			final int debCredId = rekeningMutatieTableModel.getDebCredId( selectedRow );
			final int rekeningId = rekeningMutatieTableModel.getRekeningId( selectedRow );
			final int volgNummer = rekeningMutatieTableModel.getVolgNummer( selectedRow );

			String deleteString  = "DELETE FROM rekening_mutatie";
			deleteString += " WHERE rubriek_id = " + rubriekId;
			deleteString += " AND datum = '" + datumString + "'";
			deleteString += " AND deb_cred_id = " + debCredId;
			deleteString += " AND rekening_id = " + rekeningId;
			deleteString += " AND volgnummer = " + volgNummer;

			logger.info( "deleteString: " + deleteString );

			try {
			    Statement statement = connection.createStatement( );
			    int nUpdate = statement.executeUpdate( deleteString );
			    if ( nUpdate != 1 ) {
				String errorString = ( "Could not delete record with\n" +
						       "rubriek_id  = " + rubriekId + "\n" +
						       "datum       = " + datumString + "\n" +
						       "deb_cred_id = " + debCredId + "\n" +
						       "rekening_id = " + rekeningId + "\n" +
						       "volgnummer = "  + volgNummer + "\n" +
						       "in rekening_mutatie" );
				JOptionPane.showMessageDialog( frame,
							       errorString,
							       "Delete rekening_mutatie record",
							       JOptionPane.ERROR_MESSAGE);
				logger.severe( errorString );
				return;
			    }
			} catch ( SQLException sqlException ) {
			    logger.severe( "SQLException: " + sqlException.getMessage( ) );
			    return;
			}

			// Records may have been modified: setup the table model again
			setupRekeningMutatieTable( );
		    } else if ( actionEvent.getActionCommand( ).equals( "edit" ) ) {
			// Allow to edit the selected row
			rekeningMutatieTableModel.setEditRow( selectedRow );

			// Disable the edit button
			editMutatieButton.setEnabled( false );
		    } else if ( actionEvent.getActionCommand( ).equals( "cancel" ) ) {
			// Cancel any edits in the selected row
			rekeningMutatieTableModel.cancelEditRow( selectedRow );

			// Remove the capability to edit the row
			rekeningMutatieTableModel.unsetEditRow( );

			// Enable the edit button, so that the user can select edit again
			editMutatieButton.setEnabled( true );

			// Disable the cancel and save buttons
			cancelMutatieButton.setEnabled( false );
			saveMutatieButton.setEnabled( false );
		    } else if ( actionEvent.getActionCommand( ).equals( "save" ) ) {
			// Save the changes in the table model, and in the database
			rekeningMutatieTableModel.saveEditRow( selectedRow );

			// Remove the capability to edit the row
			rekeningMutatieTableModel.unsetEditRow( );

			// Enable the edit button, so that the user can select edit again
			editMutatieButton.setEnabled( true );

			// Disable the cancel and save buttons
			cancelMutatieButton.setEnabled( false );
			saveMutatieButton.setEnabled( false );
		    }
		}
	    }
	}
	ButtonActionListener buttonActionListener = new ButtonActionListener( );

	JPanel buttonPanel = new JPanel( );
	// buttonPanel.setBackground( Color.white );

	copyButton.setActionCommand( "copy" );
	copyButton.addActionListener( buttonActionListener );
	buttonPanel.add( copyButton );

	JButton nextButton = new JButton( "Next" );
	nextButton.setActionCommand( "next" );
	nextButton.addActionListener( buttonActionListener );
	buttonPanel.add( nextButton );

	editMutatieButton.setActionCommand( "edit" );
	editMutatieButton.setEnabled( false );
	editMutatieButton.addActionListener( buttonActionListener );
	buttonPanel.add( editMutatieButton );

	cancelMutatieButton.setActionCommand( "cancel" );
	cancelMutatieButton.setEnabled( false );
	cancelMutatieButton.addActionListener( buttonActionListener );
	buttonPanel.add( cancelMutatieButton );

	saveMutatieButton.setActionCommand( "save" );
	saveMutatieButton.setEnabled( false );
	saveMutatieButton.addActionListener( buttonActionListener );
	buttonPanel.add( saveMutatieButton );

	deleteMutatieButton.setActionCommand( "delete" );
	deleteMutatieButton.setEnabled( false );
	deleteMutatieButton.addActionListener( buttonActionListener );
	buttonPanel.add( deleteMutatieButton );

	JButton closeButton = new JButton( "Close" );
	closeButton.setActionCommand( "close" );
	closeButton.addActionListener( buttonActionListener );
	buttonPanel.add( closeButton );

	constraints.gridx = 0;
	constraints.gridy = 10;
	constraints.gridwidth = 6;
	constraints.weightx = 0.0;
	constraints.weighty = 0.0;
	constraints.fill = GridBagConstraints.NONE;
	constraints.anchor = GridBagConstraints.CENTER;
	container.add( buttonPanel, constraints );

	frame.setSize( 980, 550 );
	frame.setDefaultCloseOperation( JFrame.DISPOSE_ON_CLOSE );
	frame.setVisible( true );
    }


    private boolean copyDownloadIngRecord( )
    {
	if ( rubriekId == 0 ) {
	    String errorString = "Geen rubriek geselecteerd";
	    JOptionPane.showMessageDialog( frame,
					   errorString,
					   "Download ING",
					   JOptionPane.ERROR_MESSAGE);
	    logger.severe( errorString );
	    return false;
	}

	if ( debCredId == 0 ) {
	    String errorString = "Geen Deb/Cred gevonden";
	    JOptionPane.showMessageDialog( frame,
					   errorString,
					   "Download ING",
					   JOptionPane.ERROR_MESSAGE);
	    logger.severe( errorString );
	    return false;
	}

        // Insert de rekening mutatie
        // TODO: Gebruik StringBuilder
        String insertString = "INSERT INTO rekening_mutatie SET rekening_id = " + eigenRekeningId;
	insertString += ", rubriek_id = " + rubriekId;
	insertString += ", datum = '" + mutatieDatumString + "'";
	insertString += ", deb_cred_id = " + debCredId;

        switch ( mutatieAfBijString ) {
        case "Af":
            insertString += ", mutatie_uit = " + mutatieBedrag;
            break;
        case "Bij":
            insertString += ", mutatie_in = " + mutatieBedrag;
            break;
        default:
            logger.severe( "invalid Af/Bij string" );
            return false;
        }

	int jaar = ( Integer )( jaarSpinner.getValue( ) );
	if ( jaar != 0 ) {
	    insertString += ", jaar = " + jaar;
	}

	int maand = ( Integer )( maandSpinner.getValue( ) );
	if ( maand != 0 ) {
	    insertString += ", maand = " + maand;
	}

	String omschrijvingString = mutatieMededelingenTextField.getText( );
	if ( ( omschrijvingString != null ) && ( omschrijvingString.length( ) > 0 ) ) {
	    // Matcher to find single quotes in omschrijving, in order to replace these
	    // with escaped quotes (the quadruple slashes are really necessary)
	    final Matcher quoteMatcher = quotePattern.matcher( omschrijvingString );
	    insertString += ", omschrijving = '" + quoteMatcher.replaceAll( "\\\\'" ) + "'";
	}

	int volgNummer = ( Integer )( volgNummerSpinner.getValue( ) );
	if ( volgNummer != 0 ) insertString += ", volgNummer = " + volgNummer;

	try {
	    Statement statement = connection.createStatement( );
	    logger.info( "insertString: " + insertString );
	    if ( statement.executeUpdate( insertString ) != 1 ) {
		logger.severe( "Could not insert in rekening_mutatie" );
	    } else {
		// Update the table with rekening_mutatie data for this rubriek
		setupRekeningMutatieTable( );
	    }
	} catch ( SQLException sqlException ) {
	    JOptionPane.showMessageDialog( frame,
					   sqlException.getMessage( ),
					   "Download ING",
					   JOptionPane.ERROR_MESSAGE);
	    logger.severe( "SQLException: " + sqlException.getMessage( ) );
	    return false;
	}

	// Successful completion
	return true;
    }


    private void getNextMutatieRecord( )
    {
	try {
	    if ( ! mutatieResultSet.next( ) ) {
		logger.info( "no more records " );
		JOptionPane.showMessageDialog( frame,
					       "No more records",
					       "Download ING",
					       JOptionPane.INFORMATION_MESSAGE);
		return;
	    }

	    mutatieDatumString = mutatieResultSet.getString( 1 );
	    mutatieDatumLabel.setText( mutatieDatumString );

	    // Use the date from the mutatie record to set the jaar and maand spinners
	    try {
		Date mutatieDate = dateFormat.parse( mutatieDatumString );
		calendar.setTime( mutatieDate );
		jaarSpinner.setValue( calendar.get( Calendar.YEAR ) );
		maandSpinner.setValue( getMonth() );
	    } catch( ParseException parseException ) {
		logger.severe( "Datum parse exception: " + parseException.getMessage( ) );
	    }

	    mutatieTegenRekeningString = mutatieResultSet.getString( 2 );
	    mutatieTegenRekeningLabel.setText( mutatieTegenRekeningString );

	    mutatieNaamOmschrijvingString = mutatieResultSet.getString( 3 );
	    mutatieNaamOmschrijvingLabel.setText( mutatieNaamOmschrijvingString );

	    try {
	        mutatieBedrag = Double.parseDouble( mutatieResultSet.getString( 4 ).replace( ',', '.' ) );
	    } catch( NumberFormatException numberFormatException ) {
	        logger.severe( "Double parse number format exception: " + numberFormatException.getMessage( ) );
	        mutatieBedrag = 0;
            }
	    mutatieBedragLabel.setText( euroDecimalFormat.format( mutatieBedrag ) );

	    mutatieAfBijString = mutatieResultSet.getString( 5 );
	    mutatieAfBijLabel.setText( mutatieAfBijString );

	    mutatieMededelingenString = mutatieResultSet.getString( 6 );
            mutatieCodeString = mutatieResultSet.getString( 7 );

            // Haal de rekening ID op van de eigen rekening
            mutatieEigenRekeningString = mutatieResultSet.getString( 8 );
            try {
                final String queryEigenRekening = String.format( "SELECT rekening_id FROM rekening WHERE nummer = '%s'", mutatieEigenRekeningString );

                final Statement statement = connection.createStatement( );
                final ResultSet resultSet = statement.executeQuery( queryEigenRekening );

                // Controleer of de eigen rekening gevonden is
                if ( resultSet.next( ) ) {
                    eigenRekeningId = resultSet.getInt( 1 );
                } else {
                    throw new Exception( String.format( "Eigen rekening met nummer %s niet gevonden", mutatieEigenRekeningString ) );
                }
            } catch ( SQLException sqlException ) {
                logger.severe( "Exceptie bij ophalen eigen rekening ID: " + sqlException.getMessage() );
            }

            IngMededelingenParser ingMededelingenParser = new IngMededelingenParser( mutatieMededelingenString );
            mutatieMededelingenTextField.setText( ingMededelingenParser.getMutatieMededelingenStrippedString() );

	    // Clear jaar and maand text field
	    // jaarTextField.setText( "" );
	    // maandTextField.setText( "" );

            // Clear volgnummer
            volgNummerSpinner.setValue( 0 );

            if ( !( getDebCredId( ingMededelingenParser ) ) ) {
                JOptionPane.showMessageDialog( frame,
                        "No Deb/Cred match found for " + mutatieNaamOmschrijvingString,
                        "Download ING",
                        JOptionPane.INFORMATION_MESSAGE);

                DebCredDialog debCredDialog = new DebCredDialog( connection, frame,
                        debCredId,
                        mutatieTegenRekeningString,
                        mutatieNaamOmschrijvingString,
                        mutatieMededelingenString,
                        mutatieCodeString );

                debCredId = debCredDialog.getDebCredId( );
                rubriekId = debCredDialog.getRubriekId( );
                // Store the Deb/Cred found in the omschrijving in the Deb/Cred label
                mutatieNaamOmschrijvingString = debCredDialog.getDebCredString( );
                mutatieNaamOmschrijvingLabel.setText( debCredDialog.getDebCredString( ) );
                debCredIdLabel.setText( "(id = " + debCredId + ")" );
            }

            if ( debCredId == 0 ) {
                debCredIdLabel.setText( "(-)" );
            }

            rubriekComboBox.setSelectedRubriekId( rubriekId );

            // Setup the rekening_mutatie table for the selected rubriek
            setupRekeningMutatieTable( );
	} catch ( Exception exception ) {
	    logger.severe( "Exception: " + exception.getMessage( ) );
	}
    }

    private boolean getDebCredId( final IngMededelingenParser ingMededelingenParser ) {
        // Reset located Deb/Cred id
        debCredId = 0;

        // Controleer of de tegenrekening leeg is
        if ( mutatieTegenRekeningString.isEmpty( ) )
        {
            // Geen rekening nummer in het mutatie record: zoek de debiteur/crediteur met de naam in het naam/omschrijving veld,
            // maar alleen die waarvoor de naam in het naam_omschrijving veld gevonden moet worden (omschrijving_nr = 0)
            try {
                // Matcher to find single quotes in omschrijving, in order to replace these
                // with escaped quotes (the quadruple slashes are really necessary)
                final Matcher quoteMatcher = quotePattern.matcher( mutatieNaamOmschrijvingString );
                final String mutatieNaamOmschrijvingTest = quoteMatcher.replaceAll( "\\\\'" ).toUpperCase( );
                final String queryDebCred = "SELECT deb_cred_id, rubriek_id, deb_cred" +
                        " FROM deb_cred WHERE omschrijving_nr = 0 AND UPPER( deb_Cred ) = '" + mutatieNaamOmschrijvingTest +
                        "' ORDER BY deb_cred_id DESC";

                final Statement statement = connection.createStatement( );
                final ResultSet resultSet = statement.executeQuery( queryDebCred );

                // Controleer of debiteur/crediteur gevonden is
                if ( resultSet.next( ) ) {
                    logger.fine( "Match gevonden voor mutatie naam_omschrijving: " + mutatieNaamOmschrijvingString );
                    debCredId = resultSet.getInt( 1 );
                    rubriekId = resultSet.getInt( 2 );
                    // Store the Deb/Cred found in the omschrijving in the Deb/Cred label
                    mutatieNaamOmschrijvingString = resultSet.getString( 3 );
                    mutatieNaamOmschrijvingLabel.setText( resultSet.getString( 3 ) );
                    debCredIdLabel.setText( "(id = " + debCredId + ")" );
                    return true;
                }
            } catch ( Exception exception ) {
                logger.severe( "Exceptie voor deb/cred in naam/omschrijving zonder rekening: " + exception.getMessage() );
            }

            // Geen rekening nummer in het mutatie record: zoek de debiteur/crediteur met de naam in het mededelingen veld,
            // maar alleen die waarvoor de naam in het naam_omschrijving veld gevonden moet worden (omschrijving_nr = 0)
            try {
                final String queryDebCred = "SELECT deb_cred_id, rubriek_id, deb_cred" +
                        " FROM deb_cred WHERE omschrijving_nr = 0 AND LOCATE( UPPER( deb_Cred ), '" + mutatieMededelingenString.toUpperCase( ) +
                        "' ) ORDER BY deb_cred_id DESC";

                final Statement statement = connection.createStatement( );
                final ResultSet resultSet = statement.executeQuery( queryDebCred );

                // Controleer of debiteur/crediteur gevonden is
                if ( resultSet.next( ) ) {
                    logger.fine( "Match gevonden in mutatie mededelingen: " + mutatieMededelingenString );
                    debCredId = resultSet.getInt( 1 );
                    rubriekId = resultSet.getInt( 2 );
                    // Store the Deb/Cred found in the omschrijving in the Deb/Cred label
                    mutatieNaamOmschrijvingString = resultSet.getString( 3 );
                    mutatieNaamOmschrijvingLabel.setText( resultSet.getString( 3 ) );
                    debCredIdLabel.setText( "(id = " + debCredId + ")" );
                    return true;
                }
            } catch ( Exception exception ) {
                logger.severe( "Exceptie voor deb/cred in mededelingen zonder rekening: " + exception.getMessage() );
            }

            try {
                // Speciaal geval voor intern van/naar profijtrekening
                if ( ( mutatieNaamOmschrijvingString.startsWith( "VAN " ) || mutatieNaamOmschrijvingString.startsWith( "NAAR " ) ) &&
                        ( mutatieCodeString.equals( "GT" ) ) ) {
                    // Zoek naar debiteur/crediteur met eigen rekening
                    final String queryDebCred = "SELECT deb_cred_id, rubriek_id, deb_cred " +
                                                 " FROM deb_cred WHERE rekening = '" + mutatieEigenRekeningString + "'";
                    final Statement statement = connection.createStatement();
                    final ResultSet resultSet = statement.executeQuery( queryDebCred );

                    // Controleer of de eigen rekeing gevonden is
                    if ( resultSet.next( ) ) {
                        logger.fine( "Match gevonden met eigen rekening " + mutatieEigenRekeningString + " voor mutatie omschrijving: " + mutatieNaamOmschrijvingString );
                        debCredId = resultSet.getInt( 1 );
                        rubriekId = resultSet.getInt( 2 );
                        // Store the Deb/Cred found in the omschrijving in the Deb/Cred label
                        mutatieNaamOmschrijvingString = resultSet.getString( 3 );
                        mutatieNaamOmschrijvingLabel.setText( resultSet.getString( 3 ) );
                        debCredIdLabel.setText( "(id = " + debCredId + ")" );
                        return true;

                    }
                }
            } catch ( Exception exception ) {
                logger.severe( "Exceptie voor deb/cred eigen rekening: " + exception.getMessage() );
            }
        }

        // Find rekening number from mutatie record in deb_cred
        try {
            final Statement statement = connection.createStatement();
            String queryDebCred = "SELECT deb_cred_id, rubriek_id, deb_cred, omschrijving_nr, omschrijving " +
                                  " FROM deb_cred WHERE rekening = '" + mutatieTegenRekeningString + "'";
            // Omdat de tegen rekening pre IBAN een nummer was, moet voor een lege  tegen rekening ook naar 0 gezocht worden.
            if ( mutatieTegenRekeningString.isEmpty() ) queryDebCred += " OR rekening = '0'";
            queryDebCred += " ORDER BY deb_cred_id";
            final ResultSet resultSet = statement.executeQuery( queryDebCred );

            // Loop over all entries in deb_cred with the same rekening number
            while ( resultSet.next() ) {
                final String debCredString = resultSet.getString( 3 );

                // Check depending on where the Deb/Cred string is found
                int omschrijvingNr = resultSet.getInt( 4 );

                // Check if Deb/Cred should be in field deb_cred of table deb_cred,
                // or in field omschrijving of table deb_cred.
                if ( omschrijvingNr == 0 ) {
                    // Check deb_cred field from deb_cred with naam_omschrijving in the ing_matatie records
                    logger.fine( "comparing " + debCredString +
                                 " from deb_cred with naam_omschrijving from ing_mutatie: " + mutatieNaamOmschrijvingString );
                    if ( debCredString.equals( mutatieNaamOmschrijvingString ) ) {
                        logger.fine( "Match gevonden voor mutatie rekening " + mutatieTegenRekeningString +
                                     " voor mutatie naam_omschrijving: " + mutatieNaamOmschrijvingString );
                        debCredId = resultSet.getInt( 1 );
                        rubriekId = resultSet.getInt( 2 );
                        // Store the Deb/Cred found in the omschrijving in the Deb/Cred label
                        mutatieNaamOmschrijvingString = debCredString;
                        mutatieNaamOmschrijvingLabel.setText( debCredString );
                        debCredIdLabel.setText( "(id = " + debCredId + ")" );

                        return true;
                    }

                    //Try to find the deb_cred field in the mededelingen field
                    logger.fine( "comparing " + debCredString +
                                 " from deb_cred with mededelingen from ing_mutatie: " + mutatieMededelingenString );
                    if ( mutatieMededelingenString.contains( debCredString ) ) {
                        logger.fine( "Match gevonden voor mutatie rekening " + mutatieTegenRekeningString +
                                        " in mutatie mededelingen: " + mutatieMededelingenString );
                        debCredId = resultSet.getInt( 1 );
                        rubriekId = resultSet.getInt( 2 );
                        // Store the Deb/Cred found in the omschrijving in the Deb/Cred label
                        mutatieNaamOmschrijvingString = debCredString;
                        mutatieNaamOmschrijvingLabel.setText( debCredString );
                        debCredIdLabel.setText( "(id = " + debCredId + ")" );

                        return true;
                    }
                } else {
                    // Check deb_cred field from deb_cred with part of omschrijving field from ING mutatie record

                    // Controleer of het aantal omschrijving velden klopt
                    if ( omschrijvingNr > ingMededelingenParser.getNMatches() ) {
                        // not found
                        continue;
                    }

                    // Haal het omschrijving veld op
                    final String testString = ingMededelingenParser.getMutatieMededelingenSubString(omschrijvingNr - 1);
                    logger.fine( "comparing " + debCredString +
                            " from deb_cred with " + testString +
                            " from ING record omschrijving nr " + omschrijvingNr );
                    if ( debCredString.equals( testString ) ) {
                        logger.fine( "omschrijving match found for nr " + omschrijvingNr );
                        debCredId = resultSet.getInt( 1 );
                        rubriekId = resultSet.getInt( 2 );
                        // Store the Deb/Cred found in the omschrijving in the Deb/Cred label
                        mutatieNaamOmschrijvingString = debCredString;
                        mutatieNaamOmschrijvingLabel.setText( debCredString );
                        debCredIdLabel.setText( "(id = " + debCredId + ")" );

                        // Check if the deb cred is found at the start of
                        if ( omschrijvingNr == 1 ) {
                            logger.fine( "skip omschrijving veld 1" );
                            if ( ingMededelingenParser.getNMatches() > 1 ) {
                                StringBuilder mutatieMededelingen = new StringBuilder( ingMededelingenParser.getMutatieMededelingenSubString(1) );
                                if ( ingMededelingenParser.getNMatches() > 2 ) {
                                    mutatieMededelingen.append( "; " );
                                    mutatieMededelingen.append( ingMededelingenParser.getMutatieMededelingenSubString(2) );
                                    if ( ingMededelingenParser.getNMatches() > 3 ) {
                                        mutatieMededelingen.append( "; " );
                                        mutatieMededelingen.append( ingMededelingenParser.getMutatieMededelingenSubString(3) );
                                    }
                                }
                                mutatieMededelingenTextField.setText( mutatieMededelingen.toString( ) );
                            }
                        }

                        return true;
                    }
                }
            }
        } catch ( Exception exception ) {
            logger.severe( "Exception: " + exception.getMessage( ) );
        }

        return false;
    }

    private void setupRekeningMutatieTable( ) {
	// Setup the rekening_mutatie table for the selected rubriek
        // zonder selectie op de rekeninghouder, omdat er al selectie is op de eigen rekening.
	rekeningMutatieTableModel.setupRekeningMutatieTableModel( 0, eigenRekeningId, rubriekId, debCredId, null );

	// Setup the tableSorter again so that the TableSorter gets the new table size (# rows)
	rekeningMutatieTableSorter.setTableModel( rekeningMutatieTableModel );

	// Need to setup preferred column width up again
	rekeningMutatieTable.getColumnModel( ).getColumn(  0 ).setPreferredWidth( 100 );  // Datum
	rekeningMutatieTable.getColumnModel( ).getColumn(  1 ).setPreferredWidth( 150 );  // Rekening
	rekeningMutatieTable.getColumnModel( ).getColumn(  2 ).setPreferredWidth( 150 );  // Rubriek
	rekeningMutatieTable.getColumnModel( ).getColumn(  3 ).setPreferredWidth( 150 );  // Deb/Cred
	rekeningMutatieTable.getColumnModel( ).getColumn(  4 ).setPreferredWidth( 100 );  // In
	rekeningMutatieTable.getColumnModel( ).getColumn(  5 ).setPreferredWidth( 100 );  // Uit
	rekeningMutatieTable.getColumnModel( ).getColumn(  6 ).setPreferredWidth( 40 );   // VolgNummer
	rekeningMutatieTable.getColumnModel( ).getColumn(  7 ).setPreferredWidth( 40 );   // Jaar
	rekeningMutatieTable.getColumnModel( ).getColumn(  8 ).setPreferredWidth( 40 );   // Maand
	rekeningMutatieTable.getColumnModel( ).getColumn(  9 ).setPreferredWidth( 250 );  // Omschrijving
	rekeningMutatieTable.getColumnModel( ).getColumn( 10 ).setPreferredWidth( 100 );  // Inleg Aandelen
    }

    private int getMonth( ) {
	switch ( calendar.get( Calendar.MONTH ) ) {
	case Calendar.JANUARY:	 return 1;
	case Calendar.FEBRUARY:	 return 2;
	case Calendar.MARCH:	 return 3;
	case Calendar.APRIL:	 return 4;
	case Calendar.MAY:	 return 5;
	case Calendar.JUNE:	 return 6;
	case Calendar.JULY:	 return 7;
	case Calendar.AUGUST:	 return 8;
	case Calendar.SEPTEMBER: return 9;
	case Calendar.OCTOBER:	 return 10;
	case Calendar.NOVEMBER:  return 11;
	case Calendar.DECEMBER:  return 12;
	}

	logger.severe( "calendar return value outside expected range" );
	return 0;
    }
}
