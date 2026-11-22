package financien.millenniummutaties;

import financien.gui.DebCredDialog;
import financien.gui.RekeningMutatieTableModel;
import financien.gui.RubriekComboBox;
import table.TableSorter;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Frame to copy downloaded Millennium mutatie records to rekening_mutatie and deb_cred tables in schema financien.
 * @author Chris van Engelen
 */
public class ProcessMillenniumMutaties extends JInternalFrame {
	final private Logger logger = Logger.getLogger( ProcessMillenniumMutaties.class.getCanonicalName( ) );

	private final Connection connection;
	private final JFrame parentFrame;

	private final Font dialogFont = new Font( "Dialog", Font.BOLD, 12 );

	private String mutatieDatumString;
	private final JLabel mutatieDatumLabel;

	private String debCredString;
	private final JLabel debCredLabel;
	private int debCredId = 0;
	private final JLabel debCredIdLabel;

	private final JSpinner volgNummerSpinner;

	private double mutatieBedrag;
	private final JLabel mutatieBedragLabel;

	private final JSpinner jaarSpinner;
	private final JSpinner maandSpinner;

	private String mutatieOmschrijvingString;
	private final JTextField mutatieOmschrijvingTextField;

	private int eigenRekeningId = 0;

	private int rubriekId = 0;
	private final RubriekComboBox rubriekComboBox;

	private final RekeningMutatieTableModel rekeningMutatieTableModel;
	private final TableSorter rekeningMutatieTableSorter;
	private final JTable rekeningMutatieTable;
	private static final DecimalFormat euroDecimalFormat = new DecimalFormat( "EUR #0.00;EUR -#" );

	private static final SimpleDateFormat dateFormat = new SimpleDateFormat( "yyyy-MM-dd" );
	private final GregorianCalendar calendar = new GregorianCalendar( );

	private ResultSet mutatieResultSet;

	private final JButton closeButton = new JButton( "Close" );

	// Pattern to find a single quote in the titel, to be replaced
	// with escaped quote (the double slashes are really necessary)
	private static final Pattern quotePattern = Pattern.compile( "\\'" );

	public ProcessMillenniumMutaties( final Connection connection, final JFrame parentFrame, int x, int y ) {
		super( "Process Millennium mutaties", true, true, true, true );
		this.connection = connection;
		this.parentFrame = parentFrame;

		// Get the container for the frame
		final Container container = getContentPane( );

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

		debCredLabel = new JLabel( );
		JPanel debCredPanel = new JPanel( );
		debCredPanel.add( debCredLabel );

		debCredIdLabel = new JLabel( );
		debCredPanel.add( debCredIdLabel );

		// Set up a JComboBox with the results of the query on rubriek
		rubriekComboBox = new RubriekComboBox( connection, 0, true );

		JButton editDebCredButton = new JButton( "Edit Deb/Cred" );
		editDebCredButton.addActionListener( ( ActionEvent actionEvent ) -> {
			DebCredDialog debCredDialog = new DebCredDialog( connection, parentFrame,
					debCredId,
					"",
					mutatieOmschrijvingString,
					"" );

			debCredId = debCredDialog.getDebCredId( );
			rubriekId = debCredDialog.getRubriekId( );
			debCredString = debCredDialog.getDebCredString( );

			// Set the deb/cred name and id labels
			setDebCredLabels( );

			// Set up the rubriek combobox and rekening_mutatie table for the selected rubriek and deb-cred id
			rubriekComboBox.setSelectedRubriekId( rubriekId );
			setupRekeningMutatieTable( );
		} );
		debCredPanel.add( editDebCredButton );

		constraints.gridx = GridBagConstraints.RELATIVE;
		constraints.insets = insetsRight;
		constraints.gridwidth = 2;
		container.add( debCredPanel, constraints );
		constraints.gridwidth = 1;

		constraints.gridx = 0;
		constraints.gridy = 2;
		constraints.insets = insetsLeft;
		container.add( new JLabel( "Mutatie:" ), constraints );

		mutatieBedragLabel = new JLabel( );
		constraints.gridx = GridBagConstraints.RELATIVE;
		constraints.insets = insetsRight;
		container.add( mutatieBedragLabel, constraints );

		constraints.gridx = 0;
		constraints.gridy = 3;
		constraints.insets = insetsLeft;
		container.add( new JLabel( "VolgNummer:" ), constraints );

		SpinnerNumberModel volgNummerSpinnerNumberModel = new SpinnerNumberModel( 0, 0, 10, 1 );
		volgNummerSpinner = new JSpinner( volgNummerSpinnerNumberModel );
		JFormattedTextField volgNummerSpinnerTextField =
				( ( JSpinner.DefaultEditor )volgNummerSpinner.getEditor( ) ).getTextField( );
		if ( volgNummerSpinnerTextField != null ) {
			volgNummerSpinnerTextField.setColumns( 3 );
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
			jaarSpinnerTextField.setFont( dialogFont );
		}
		constraints.gridx = GridBagConstraints.RELATIVE;
		constraints.insets = insetsRight;
		container.add( jaarSpinner,  constraints );

		SpinnerNumberModel maandSpinnerNumberModel = new SpinnerNumberModel( getMonth( ), 0, 12, 1 );
		maandSpinner = new JSpinner( maandSpinnerNumberModel );
		JFormattedTextField maandSpinnerTextField =
				( ( JSpinner.DefaultEditor )maandSpinner.getEditor( ) ).getTextField( );
		if ( maandSpinnerTextField != null ) {
			maandSpinnerTextField.setColumns( 3 );
			maandSpinnerTextField.setFont( dialogFont );
		}
		constraints.gridx = GridBagConstraints.RELATIVE;
		container.add( maandSpinner, constraints );

		constraints.gridx = 0;
		constraints.gridy = 6;
		constraints.insets = insetsLeft;
		constraints.gridwidth = 1;
		container.add( new JLabel( "Omschrijving:" ), constraints );

		mutatieOmschrijvingTextField = new JTextField( "", 60 );
		mutatieOmschrijvingTextField.setFont( dialogFont );

		constraints.gridwidth = 5;
		constraints.gridx = GridBagConstraints.RELATIVE;
		constraints.insets = insetsRight;
		constraints.fill = GridBagConstraints.HORIZONTAL;
		container.add( mutatieOmschrijvingTextField, constraints );

		constraints.gridx = 0;
		constraints.gridy = 7;
		constraints.gridwidth = 1;
		constraints.insets = insetsLeft;
		constraints.fill = GridBagConstraints.NONE;
		container.add( new JLabel( "Rubriek:" ), constraints );

		rubriekComboBox.addActionListener( ( ActionEvent actionEvent ) -> {
			// Get the selected Rubriek ID
			rubriekId = rubriekComboBox.getSelectedRubriekId( );

			// Check if rubriek has been selected
			if ( rubriekId == 0 ) {
				return;
			}

			// Set up the rekening_mutatie table for the selected rubriek
			setupRekeningMutatieTable( );
		} );

		constraints.gridx = GridBagConstraints.RELATIVE;
		constraints.insets = insetsRight;
		constraints.gridwidth = 4;
		container.add( rubriekComboBox, constraints );

		// Define the edit, cancel, save and buttons because
		// the cancel/save buttons are enabled by the table model.
		final JButton copyButton = new JButton( "Copy" );
		final JButton nextButton = new JButton( "Next" );
		final JButton editMutatieButton = new JButton( "Edit" );
		final JButton cancelMutatieButton = new JButton( "Cancel" );
		final JButton saveMutatieButton = new JButton( "Save" );
		final JButton deleteMutatieButton = new JButton( "Delete" );

		// Create rekening_mutatie table from rekening_mutatie table model
		rekeningMutatieTableModel = new RekeningMutatieTableModel( connection, parentFrame,
				cancelMutatieButton, saveMutatieButton );
		rekeningMutatieTableSorter = new TableSorter( rekeningMutatieTableModel );
		rekeningMutatieTable = new JTable( rekeningMutatieTableSorter );
		rekeningMutatieTableSorter.setTableHeader( rekeningMutatieTable.getTableHeader( ) );
		// rekeningMutatieTableSorter.setSortingStatus( 0, TableSorter.DESCENDING );

		rekeningMutatieTable.setAutoResizeMode( JTable.AUTO_RESIZE_OFF );

		// Set vertical size just enough for 12 entries
		rekeningMutatieTable.setPreferredScrollableViewportSize( new Dimension( 1220, 192 ) );

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
						break;
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
		constraints.gridy = 8;
		constraints.gridwidth = 6;
		constraints.insets = new Insets( 10, 20, 5, 20 );
		// Setting weightx, weighty and fill is necessary for proper filling the frame when resized.
		constraints.weightx = 1d;
		constraints.weighty = 1d;
		constraints.fill = GridBagConstraints.BOTH;
		constraints.anchor = GridBagConstraints.CENTER;
		container.add( scrollPane, constraints );

		try {
			final Statement statement = connection.createStatement( );
			mutatieResultSet = statement.executeQuery( "SELECT datum_b, omschrijving, mutatie " +
					"FROM millennium_mutatie ORDER BY datum_b" );
		} catch ( SQLException sqlException ) {
			logger.severe( "SQLException: " + sqlException.getMessage( ) );
			JOptionPane.showMessageDialog( parentFrame,
					"SQLException: " + sqlException.getMessage( ),
					"Process Millennium mutaties exception",
					JOptionPane.ERROR_MESSAGE );
			setVisible( false );
			dispose( );
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
						int result = JOptionPane.showConfirmDialog( parentFrame,
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
					setVisible( false );
					dispose( );
					return;
				} else if ( actionEvent.getActionCommand( ).equals( "copy" ) ) {
					// Copy the record to the rekening mutatie table
					if ( copyDownloadMillenniumRecord( ) ) {
						// Successful copy: disable the copy button
						copyButton.setEnabled( false );
						getRootPane( ).setDefaultButton( nextButton );
					}
				} else {
					int selectedRow = mutatieListSelectionListener.getSelectedRow( );

					if ( actionEvent.getActionCommand( ).equals( "next" ) ) {
						// Check if current row has modified values
						if ( rekeningMutatieTableModel.getRowModified( ) ) {
							if ( selectedRow < 0 ) {
								JOptionPane.showMessageDialog( parentFrame,
										"Geen mutatie geselecteerd",
										"Process Millennium mutaties error",
										JOptionPane.ERROR_MESSAGE );
								return;
							}

							int result = JOptionPane.showConfirmDialog( parentFrame,
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

						if ( getNextMutatieRecord( ) ) {
							copyButton.setEnabled( true );
							getRootPane( ).setDefaultButton( copyButton );
							mutatieOmschrijvingTextField.requestFocusInWindow( );
						} else {
							getRootPane( ).setDefaultButton( closeButton );
						}

						return;
					}

					if ( selectedRow < 0 ) {
						JOptionPane.showMessageDialog( parentFrame,
								"Geen mutatie geselecteerd",
								"Process Millennium mutaties error",
								JOptionPane.ERROR_MESSAGE );
						return;
					}

					if ( actionEvent.getActionCommand( ).equals( "delete" ) ) {
						final String datumString = rekeningMutatieTableModel.getDatumString( selectedRow );
						int result = JOptionPane.showConfirmDialog( parentFrame,
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
								JOptionPane.showMessageDialog( parentFrame,
										errorString,
										"Delete rekening_mutatie record",
										JOptionPane.ERROR_MESSAGE );
								logger.severe( errorString );
								return;
							}
						} catch ( SQLException sqlException ) {
							logger.severe( "SQLException: " + sqlException.getMessage( ) );
							JOptionPane.showMessageDialog( parentFrame,
									"SQLException: " + sqlException.getMessage( ),
									"Process Millennium mutaties exception",
									JOptionPane.ERROR_MESSAGE );
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

		copyButton.setActionCommand( "copy" );
		copyButton.addActionListener( buttonActionListener );
		buttonPanel.add( copyButton );

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

		closeButton.setActionCommand( "close" );
		closeButton.addActionListener( buttonActionListener );
		buttonPanel.add( closeButton );

		constraints.gridx = 0;
		constraints.gridy = 9;
		constraints.weightx = 0d;
		constraints.weighty = 0d;
		constraints.fill = GridBagConstraints.NONE;
		constraints.insets = new Insets( 5, 20, 20, 20 );
		container.add( buttonPanel, constraints );

		setSize( 1280, 550 );
		setLocation( x, y );
		setDefaultCloseOperation( JFrame.DISPOSE_ON_CLOSE );
		getRootPane( ).setDefaultButton( copyButton );
		setVisible( true );
		mutatieOmschrijvingTextField.requestFocusInWindow( );
	}

	private boolean copyDownloadMillenniumRecord( )
	{
		if ( rubriekId == 0 ) {
			String errorString = "Geen rubriek geselecteerd";
			JOptionPane.showMessageDialog( parentFrame,
						   errorString,
						   "Process Millennium mutaties",
						   JOptionPane.ERROR_MESSAGE );
			logger.severe( errorString );
			return false;
		}

		if ( debCredId == 0 ) {
			String errorString = "Geen Deb/Cred gevonden";
			JOptionPane.showMessageDialog( parentFrame,
						   errorString,
						   "Process Millennium mutaties",
						   JOptionPane.ERROR_MESSAGE );
			logger.severe( errorString );
			return false;
		}

		// Insert de rekening mutatie
		// TODO: Gebruik StringBuilder
		String insertString = "INSERT INTO rekening_mutatie SET rekening_id = " + eigenRekeningId;
		insertString += ", rubriek_id = " + rubriekId;
		insertString += ", datum = '" + mutatieDatumString + "'";
		insertString += ", deb_cred_id = " + debCredId;

		if ( mutatieBedrag < 0.0 ) {
			insertString += ", mutatie_uit = " + Math.abs( mutatieBedrag );
		} else {
			insertString += ", mutatie_in = " + mutatieBedrag;
		}

		int jaar = ( Integer )( jaarSpinner.getValue( ) );
		if ( jaar != 0 ) {
			insertString += ", jaar = " + jaar;
		}

		int maand = ( Integer )( maandSpinner.getValue( ) );
		if ( maand != 0 ) {
			insertString += ", maand = " + maand;
		}

		String omschrijvingString = mutatieOmschrijvingTextField.getText( );
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
			JOptionPane.showMessageDialog( parentFrame,
						   "SQL exception: " + sqlException.getMessage( ),
						   "Process Millennium mutaties exception",
						   JOptionPane.ERROR_MESSAGE );
			logger.severe( "SQLException: " + sqlException.getMessage( ) );
			return false;
		}

		// Successful completion
		return true;
	}

	private boolean getNextMutatieRecord( )
	{
		try {
			if ( ! mutatieResultSet.next( ) ) {
				logger.info( "no more records " );
				JOptionPane.showMessageDialog( parentFrame,
							   "No more records",
							   "Process Millennium mutaties",
							   JOptionPane.INFORMATION_MESSAGE );
				return false;
			}

			mutatieDatumString = mutatieResultSet.getString( 1 );
			mutatieDatumLabel.setText( mutatieDatumString );

			// Use the date from the mutatie record to set the jaar and maand spinners
			try {
				Date mutatieDate = dateFormat.parse( mutatieDatumString );
				calendar.setTime( mutatieDate );
				jaarSpinner.setValue( calendar.get( Calendar.YEAR ) );
				//maandSpinner.setValue( getMonth( ) );
			} catch ( ParseException parseException ) {
				logger.severe( "Datum parse exception: " + parseException.getMessage( ) );
			}

			mutatieOmschrijvingString = mutatieResultSet.getString( 2 );
			mutatieOmschrijvingTextField.setText( mutatieOmschrijvingString );

			try {
				mutatieBedrag = Double.parseDouble( mutatieResultSet.getString( 3 ) );
			} catch( NumberFormatException numberFormatException ) {
				logger.severe( "Double parse number format exception: " + numberFormatException.getMessage( ) );
				mutatieBedrag = 0;
			}
			mutatieBedragLabel.setText( euroDecimalFormat.format( mutatieBedrag ) );

			// Haal de rekening ID op
			try {
				final Statement statement = connection.createStatement( );
				final ResultSet resultSet = statement.executeQuery( "SELECT rekening_id FROM rekening WHERE nummer = 'PT50003300004565768524305'" );

				// Controleer of de eigen rekening gevonden is
				if ( resultSet.next( ) ) {
					eigenRekeningId = resultSet.getInt( 1 );
				} else {
					throw new Exception( "Eigen rekening met nummer PT50003300004565768524305 niet gevonden" );
				}
			} catch ( SQLException sqlException ) {
				JOptionPane.showMessageDialog( parentFrame,
						"SQLException bij ophalen eigen rekening ID: " + sqlException.getMessage( ),
						"Process Millennium mutaties exception",
						JOptionPane.ERROR_MESSAGE );
				logger.severe( "Exceptie bij ophalen eigen rekening ID: " + sqlException.getMessage( ) );
			}

			if ( !( getDebCredId( ) ) ) {
				// Deb/cred not found: reset fields
				debCredString = mutatieOmschrijvingString;
				debCredId = 0;
				rubriekId = 0;
				rubriekComboBox.setSelectedRubriekId( rubriekId );
				setDebCredLabels( );
				setupRekeningMutatieTable( );

				JOptionPane.showMessageDialog( parentFrame,
						"No Deb/Cred match found for " + mutatieOmschrijvingString,
						"Process Millennium mutaties",
						JOptionPane.INFORMATION_MESSAGE );

				DebCredDialog debCredDialog = new DebCredDialog( connection, parentFrame,
						debCredId,
						"",
						mutatieOmschrijvingString,
						"" );

				debCredId = debCredDialog.getDebCredId( );
				rubriekId = debCredDialog.getRubriekId( );
				debCredString = debCredDialog.getDebCredString( );
			}

			// Set the deb/cred name and id labels
			setDebCredLabels( );

			// Set up the rubriek combobox and rekening_mutatie table for the selected rubriek and deb-cred id
			rubriekComboBox.setSelectedRubriekId( rubriekId );
			setupRekeningMutatieTable( );
		} catch ( Exception exception ) {
			logger.severe( "Exception: " + exception.getMessage( ) );
		}

		return true;
	}

	private boolean getDebCredId( ) {
		// Reset located Deb/Cred id
		debCredId = 0;

		// Zoek voor een deb/cred gelijk aan de omschrijving (bv "Hr CPM van Engelen")
		try {
			// Matcher to find single quotes in omschrijving, in order to replace these
			// with escaped quotes (the quadruple slashes are really necessary)
			final Matcher quoteMatcher = quotePattern.matcher( mutatieOmschrijvingString );
			final String mutatieOmschrijvingTest = quoteMatcher.replaceAll( "\\\\'" ).toUpperCase( );
			final String queryDebCred = "SELECT deb_cred_id, rubriek_id, deb_cred" +
					" FROM deb_cred WHERE UPPER( deb_cred ) = '" + mutatieOmschrijvingTest +
					"' ORDER BY deb_cred_id DESC";

			final Statement statement = connection.createStatement( );
			final ResultSet resultSet = statement.executeQuery( queryDebCred );

			// Controleer of debiteur/crediteur gevonden is
			if ( resultSet.next( ) ) {
				logger.info( "Match gevonden voor deb/cred = mutatie omschrijving: " + mutatieOmschrijvingString );
				debCredId = resultSet.getInt( 1 );
				rubriekId = resultSet.getInt( 2 );
				debCredString = resultSet.getString( 3 );
				return true;
			}
		} catch ( Exception exception ) {
			logger.severe( "Exceptie voor deb/cred in omschrijving: " + exception.getMessage( ) );
		}

		// Zoek de debiteur/crediteur met de zoekterm in het omschrijvingveld,
		try {
			final String queryDebCred = "SELECT deb_cred_id, rubriek_id, deb_cred" +
					" FROM deb_cred WHERE LOCATE( UPPER( zoekterm ), '" + mutatieOmschrijvingString.toUpperCase( ) +
					"' ) ORDER BY deb_cred_id DESC";

			final Statement statement = connection.createStatement( );
			final ResultSet resultSet = statement.executeQuery( queryDebCred );

			// Controleer of debiteur/crediteur gevonden is
			if ( resultSet.next( ) ) {
				logger.info( "Match gevonden voor zoekterm in: "  + mutatieOmschrijvingString );
				debCredId = resultSet.getInt( 1 );
				rubriekId = resultSet.getInt( 2 );
				debCredString = resultSet.getString( 3 );
				return true;
			}
		} catch ( Exception exception ) {
			logger.severe( "Exceptie voor deb/cred bij zoeken in deb_cred: " + exception.getMessage( ) );
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

	private void setDebCredLabels( ) {
		// Store the Deb/Cred found in the omschrijving in the Deb/Cred label
		if ( debCredString != null && !debCredString.isEmpty( ) ) debCredLabel.setText( debCredString + "  " );
		if ( debCredId == 0 ) {
			debCredIdLabel.setText( "(-)" );
		} else {
			debCredIdLabel.setText( "(id = " + debCredId + ")  " );
		}
	}
}
