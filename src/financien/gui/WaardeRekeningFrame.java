// frame to inspect waarde for all dates for a specific rekening

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
import javax.swing.border.*;

import table.*;


public class WaardeRekeningFrame {
    final private Logger logger = Logger.getLogger( WaardeRekeningFrame.class.getCanonicalName( ) );

    Connection connection;

    final JFrame frame = new JFrame( "Waarde geselecteerde rekening" );

    WaardeRekeningTableModel waardeRekeningTableModel;
    TableSorter waardeRekeningTableSorter;
    JTable waardeRekeningTable;

    RekeningHouderComboBox rekeningHouderComboBox;
    int selectedRekeningHouderId = 1;

    RekeningComboBox rekeningComboBox;
    int selectedRekeningId = 0;

    final SimpleDateFormat dateFormat = new SimpleDateFormat( "yyyy-MM-dd" );
    final String euroDatumString = "2002-01-01";
    final String euroKoersenDatumString = "1999-01-01";
    Date euroDatumDate;
    Date euroKoersenDatumDate;

    int rekeningCurrencyId = 0;
    DecimalFormat rekeningDecimalFormat;

    final DecimalFormat euroDecimalFormat    = new DecimalFormat( "EUR #0.00;EUR -#" );
    final DecimalFormat nlgDecimalFormat     = new DecimalFormat( "NLG #0.00;NLG -#" );
    final DecimalFormat usdDecimalFormat     = new DecimalFormat( "USD #0.00;USD -#" );
    final DecimalFormat percentDecimalFormat = new DecimalFormat( "% #0.00;% -#" );


    public WaardeRekeningFrame( final Connection connection ) {
	this.connection = connection;

	// Get date from datum string objects
	try {
	    euroDatumDate        = dateFormat.parse( euroDatumString );
	    euroKoersenDatumDate = dateFormat.parse( euroKoersenDatumString );
	} catch( ParseException parseException ) {
	    logger.severe( "Euro datum parse exception: " + parseException.getMessage( ) );
	    return;
	}

	final Container container = frame.getContentPane( );

	// Set grid bag layout manager
	container.setLayout( new GridBagLayout( ) );
	GridBagConstraints constraints = new GridBagConstraints( );
	constraints.anchor = GridBagConstraints.EAST;
	constraints.insets = new Insets( 5, 10, 5, 10 );
        constraints.weightx = 1.0;
        constraints.weighty = 0.0;

	constraints.gridx = 0;
	constraints.gridy = 0;
	constraints.anchor = GridBagConstraints.EAST;
	container.add( new JLabel( "Rekening:" ), constraints );

        JPanel rekeningPanel = new JPanel( );

        // Setup a JComboBox with the results of the query on rekening houder
        rekeningHouderComboBox = new RekeningHouderComboBox( connection, 1 );
        rekeningPanel.add( rekeningHouderComboBox );


        class RekeningSelectieActionListener implements ActionListener {
            public void actionPerformed( ActionEvent actionEvent ) {
                rekeningComboBox.setupRekeningComboBox( selectedRekeningId,
                        1,
                        actionEvent.getActionCommand( ).equals( "onlyActiveAccounts" ) );
            }
        }
        RekeningSelectieActionListener rekeningSelectieActionListener = new RekeningSelectieActionListener( );


        final JRadioButton onlyActiveAccountsButton = new JRadioButton( "Alleen aktieve rekeningen", true );
        onlyActiveAccountsButton.setActionCommand( "onlyActiveAccounts" );
        onlyActiveAccountsButton.addActionListener( rekeningSelectieActionListener );

        JRadioButton allAccountsButton = new JRadioButton( "Alle rekeningen" );
        allAccountsButton.setActionCommand( "allAccounts" );
        allAccountsButton.addActionListener( rekeningSelectieActionListener );

        ButtonGroup rekeningSelectieButtonGroup = new ButtonGroup( );
        rekeningSelectieButtonGroup.add( onlyActiveAccountsButton );
        rekeningSelectieButtonGroup.add( allAccountsButton );

        JPanel rekeningSelectieButtonPanel = new JPanel( new GridLayout( 2, 1 ) );
        rekeningSelectieButtonPanel.add( onlyActiveAccountsButton );
        rekeningSelectieButtonPanel.add( allAccountsButton );

        constraints.gridx = GridBagConstraints.RELATIVE;
        rekeningPanel.add( rekeningSelectieButtonPanel );


        // Rekeninghouder actie listener, na declaratie van onlyActiveAccountsButton
        class RekeningHouderActionListener implements ActionListener {
            public void actionPerformed( ActionEvent actionEvent ) {
                // Controleer of de rekeninghouder gewijzigd is
                if ( rekeningHouderComboBox.getSelectedRekeningHouderId( ) != selectedRekeningHouderId ) {
                    // Get the selected Rekeninghouder ID
                    selectedRekeningHouderId = rekeningHouderComboBox.getSelectedRekeningHouderId();

                    // Reset de geselecteerde rekening
                    selectedRekeningId = 0;

                    // Setup the rekening combobox
                    rekeningComboBox.setupRekeningComboBox( selectedRekeningId,
                            selectedRekeningHouderId,
                            onlyActiveAccountsButton.isSelected() );

                    // Setup the waarde table for the selected rekening
                    setupWaardeRekeningTable( selectedRekeningId );
                }
            }
        }
        rekeningHouderComboBox.addActionListener( new RekeningHouderActionListener() );


        // Setup a JComboBox with the rekening
	rekeningComboBox = new RekeningComboBox( connection, selectedRekeningId, 1, true );
        rekeningPanel.add( rekeningComboBox );

	constraints.anchor = GridBagConstraints.WEST;
	constraints.gridx = GridBagConstraints.RELATIVE;
	container.add( rekeningPanel, constraints );

	class RekeningActionListener implements ActionListener {
	    public void actionPerformed( ActionEvent actionEvent ) {
		// Get the selected rekening id
		selectedRekeningId = rekeningComboBox.getSelectedRekeningId( );

		// Check if rekening has been selected
		if ( selectedRekeningId == 0 ) {
		    return;
		}

		// Get the related record for the selected rekening_id from table rekening
		try {
		    Statement rekeningStatement = connection.createStatement( );
		    ResultSet rekeningResultSet =
			rekeningStatement.executeQuery( "SELECT currency_id, rekening_type.rekening_pattern " +
							"FROM rekening " +
							"LEFT JOIN rekening_type ON rekening.type_id = rekening_type.rekening_type_id " +
							"WHERE rekening_id = " + selectedRekeningId );

		    if ( !rekeningResultSet.next( ) ) {
			logger.severe( "Could not get record for rekeningId " + selectedRekeningId +
				       " in rekening" );
			return;
		    }

		    // Save the currency id and output pattern for this rekening
		    rekeningCurrencyId = rekeningResultSet.getInt( 1 );
		    rekeningDecimalFormat = new DecimalFormat( rekeningResultSet.getString( 2 ) );
		} catch ( SQLException sqlException ) {
		    logger.severe( "SQLException in rekeningStatement: " + sqlException.getMessage( ) );
		}

		// Setup the waarde table for the selected rekening
		setupWaardeRekeningTable( selectedRekeningId );

		// Setup the tableSorter again so that the TableSorter gets the new table size (# rows)
		waardeRekeningTableSorter.setTableModel( waardeRekeningTableModel );

		// Need to set column width again
		waardeRekeningTable.getColumnModel( ).getColumn( 0 ).setPreferredWidth( 100 );  // Datum
		waardeRekeningTable.getColumnModel( ).getColumn( 1 ).setPreferredWidth( 120 );  // Saldo
		waardeRekeningTable.getColumnModel( ).getColumn( 2 ).setPreferredWidth( 120 );  // Koers
		waardeRekeningTable.getColumnModel( ).getColumn( 3 ).setPreferredWidth( 120 );  // Waarde
		waardeRekeningTable.getColumnModel( ).getColumn( 4 ).setPreferredWidth( 120 );  // Inleg
		waardeRekeningTable.getColumnModel( ).getColumn( 5 ).setPreferredWidth( 120 );  // Waarde-Inleg
		waardeRekeningTable.getColumnModel( ).getColumn( 6 ).setPreferredWidth( 120 );  // Rendement
	    }
	}
	rekeningComboBox.addActionListener( new RekeningActionListener( ) );


	// Create waarde-datum table from waarde-datum table model
	waardeRekeningTableModel = new WaardeRekeningTableModel( connection );
	waardeRekeningTableSorter = new TableSorter( waardeRekeningTableModel );
	waardeRekeningTable = new JTable( waardeRekeningTableSorter );
	waardeRekeningTableSorter.setTableHeader( waardeRekeningTable.getTableHeader( ) );
	// waardeRekeningTableSorter.setSortingStatus( 1, TableSorter.ASCENDING );

	waardeRekeningTable.setAutoResizeMode( JTable.AUTO_RESIZE_OFF );

	// Set vertical size just enough for 20 entries, horizotal: 820 plus 16 for the scrollbar
	waardeRekeningTable.setPreferredScrollableViewportSize( new Dimension( 836, 320 ) );

	// Set renderer for Double objects
	class DoubleRenderer extends JTextField implements TableCellRenderer {
	    public Component getTableCellRendererComponent( JTable table,
							    Object object,
							    boolean isSelected,
							    boolean hasFocus,
							    int row, int column ) {
		switch ( column ) {
		case 1:		// Saldo
		    // Use the formatter defined for this rekening_type in table rekening_type
		    this.setText( rekeningDecimalFormat.format( ( ( Double )object ).doubleValue( ) ) );

		    break;

		case 2:		// Koers
		    // Get the datum of this row from the table
		    final String waardeDatumString = ( String )waardeRekeningTable.getValueAt( row, 0 );

		    Date waardeDatumDate;
		    try {
			waardeDatumDate = dateFormat.parse( waardeDatumString );
		    } catch( ParseException parseException ) {
			logger.severe( "Waarde datum parse exception: " + parseException.getMessage( ) );
			this.setText( object.toString( ) );
			return this;
		    }

		    // For dates before 1-1-1999, the koers is given in NLG, after 1-1-1999 in EUR or USD
		    if ( waardeDatumDate.before( euroKoersenDatumDate ) ) {
			// Koers is in NLG
			this.setText( nlgDecimalFormat.format( ( ( Double )object ).doubleValue( ) ) );
		    } else {
			// Koers is in EUR or USD: get the currencyId from the table model
			if ( rekeningCurrencyId == 3 ) {
			    // Koers is in USD
			    this.setText( usdDecimalFormat.format( ( ( Double )object ).doubleValue( ) ) );
			} else {
			    // In all other cases the koers is in EUR
			    this.setText( euroDecimalFormat.format( ( ( Double )object ).doubleValue( ) ) );
			}
		    }

		    break;

		case 3:		// Waarde
		case 4:		// Inleg
		case 5:		// Waarde - inleg
		    this.setText( euroDecimalFormat.format( ( ( Double )object ).doubleValue( ) ) );
		    break;
		case 6:		// Rendement
		    this.setText( percentDecimalFormat.format( ( ( Double )object ).doubleValue( ) ) );
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
	waardeRekeningTable.setDefaultRenderer( Double.class, doubleRenderer );

	// Add table to the container
	constraints.gridx = 0;
	constraints.gridy = 1;
	constraints.gridwidth = 2;

        // Setting weighty and fill is necessary for proper filling the frame when resized.
        constraints.weighty = 1.0;
        constraints.fill = GridBagConstraints.BOTH;

	container.add( new JScrollPane( waardeRekeningTable ), constraints );


	// Class to handle button actions
	class ButtonActionListener implements ActionListener {
	    public void actionPerformed( ActionEvent actionEvent ) {
		if ( actionEvent.getActionCommand( ).equals( "close" ) ) {
		    frame.setVisible( false );
		    System.exit( 0 );
		}
	    }
	}
	final ButtonActionListener buttonActionListener = new ButtonActionListener( );

	JPanel buttonPanel = new JPanel( );

	final JButton closeButton = new JButton( "Close" );
	closeButton.addActionListener( buttonActionListener );
	closeButton.setActionCommand( "close" );
	buttonPanel.add( closeButton );


	constraints.gridx = 0;
	constraints.gridy = 2;
	constraints.anchor = GridBagConstraints.CENTER;
	constraints.insets = new Insets( 5, 10, 5, 10 );
        constraints.weighty = 0.0;
        constraints.fill = GridBagConstraints.NONE;
	container.add( buttonPanel, constraints );

	frame.setSize( 920, 550 );
	frame.setDefaultCloseOperation( JFrame.DISPOSE_ON_CLOSE );
	frame.setVisible( true );
    }

    private void setupWaardeRekeningTable( int rekeningId ) {
	// Setup the waarde table for the selected rekening
	waardeRekeningTableModel.setupWaardeRekeningTableModel( rekeningId );

	// Setup the tableSorter again so that the TableSorter gets the new table size (# rows)
	waardeRekeningTableSorter.setTableModel( waardeRekeningTableModel );

	// Need to set column width again
	waardeRekeningTable.getColumnModel( ).getColumn( 0 ).setPreferredWidth( 100 );  // Datum
	waardeRekeningTable.getColumnModel( ).getColumn( 1 ).setPreferredWidth( 120 );  // Saldo
	waardeRekeningTable.getColumnModel( ).getColumn( 2 ).setPreferredWidth( 120 );  // Koers
	waardeRekeningTable.getColumnModel( ).getColumn( 3 ).setPreferredWidth( 120 );  // Waarde
	waardeRekeningTable.getColumnModel( ).getColumn( 4 ).setPreferredWidth( 120 );  // Inleg
	waardeRekeningTable.getColumnModel( ).getColumn( 5 ).setPreferredWidth( 120 );  // Waarde-Inleg
	waardeRekeningTable.getColumnModel( ).getColumn( 6 ).setPreferredWidth( 120 );  // Rendement
    }
}
