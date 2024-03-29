package financien.waarderekening;

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

import financien.gui.RekeningComboBox;
import financien.gui.RekeningHouderComboBox;
import table.*;

/**
 * Frame to show waarde for a specific rekening.
 * @author Chris van Engelen
 */
public class ShowWaardeRekening extends JInternalFrame {
    private final Logger logger = Logger.getLogger( ShowWaardeRekening.class.getCanonicalName( ) );

    private WaardeRekeningTableModel waardeRekeningTableModel;
    private TableSorter waardeRekeningTableSorter;
    private JTable waardeRekeningTable;

    private RekeningHouderComboBox rekeningHouderComboBox;
    private int selectedRekeningHouderId = 1;

    private RekeningComboBox rekeningComboBox;
    private int selectedRekeningId = 0;

    private static final SimpleDateFormat dateFormat = new SimpleDateFormat( "yyyy-MM-dd" );
    private static final String euroDatumString = "2002-01-01";
    private static final String euroKoersenDatumString = "1999-01-01";
    private Date euroDatumDate;
    private Date euroKoersenDatumDate;

    private int rekeningCurrencyId = 0;
    private DecimalFormat rekeningDecimalFormat;

    private static final DecimalFormat euroDecimalFormat    = new DecimalFormat( "EUR #0.00;EUR -#" );
    private static final DecimalFormat nlgDecimalFormat     = new DecimalFormat( "NLG #0.00;NLG -#" );
    private static final DecimalFormat usdDecimalFormat     = new DecimalFormat( "USD #0.00;USD -#" );
    private static final DecimalFormat percentDecimalFormat = new DecimalFormat( "% #0.00;% -#" );

    public ShowWaardeRekening( final Connection connection, final JFrame parentFrame, int x, int y ) {
        super("Show waarde rekening", true, true, true, true);

	// Get date from datum string objects
	try {
	    euroDatumDate        = dateFormat.parse( euroDatumString );
	    euroKoersenDatumDate = dateFormat.parse( euroKoersenDatumString );
	} catch( ParseException parseException ) {
	    logger.severe( "Euro datum parse exception: " + parseException.getMessage( ) );
	    return;
	}

	final Container container = getContentPane( );

	// Set grid bag layout manager
	container.setLayout( new GridBagLayout( ) );
	GridBagConstraints constraints = new GridBagConstraints( );

	constraints.insets = new Insets( 20, 20, 5, 5 );
        constraints.gridx = 0;
	constraints.gridy = 0;
	constraints.anchor = GridBagConstraints.EAST;
	container.add( new JLabel( "Rekening:" ), constraints );

        JPanel rekeningPanel = new JPanel( );

        // Setup a JComboBox with the results of the query on rekening houder
        rekeningHouderComboBox = new RekeningHouderComboBox( connection, 1 );
        rekeningPanel.add( rekeningHouderComboBox );

        ActionListener rekeningSelectieActionListener =
                ( ActionEvent actionEvent ) -> rekeningComboBox.setupRekeningComboBox( selectedRekeningId,
                                                                                       1,
                                                                                       actionEvent.getActionCommand( ).equals( "onlyActiveAccounts" ) );

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
        rekeningHouderComboBox.addActionListener( ( ActionEvent actionEvent ) -> {
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
        } );

        // Setup a JComboBox with the rekening
	rekeningComboBox = new RekeningComboBox( connection, selectedRekeningId, 1, true );
        rekeningPanel.add( rekeningComboBox );

        constraints.insets = new Insets( 20, 5, 5, 20 );
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
                    JOptionPane.showMessageDialog( parentFrame,
                            sqlException.getMessage( ),
                            "Show waarde rekening SQL exception in rekening",
                            JOptionPane.ERROR_MESSAGE);
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
	waardeRekeningTableModel = new WaardeRekeningTableModel( connection, parentFrame );
	waardeRekeningTableSorter = new TableSorter( waardeRekeningTableModel );
	waardeRekeningTable = new JTable( waardeRekeningTableSorter );
	waardeRekeningTableSorter.setTableHeader( waardeRekeningTable.getTableHeader( ) );
	// waardeRekeningTableSorter.setSortingStatus( 1, TableSorter.ASCENDING );

	waardeRekeningTable.setAutoResizeMode( JTable.AUTO_RESIZE_OFF );

	// Set vertical size just enough for 20 entries, horizotal: 820 plus 16 for the scrollbar
	waardeRekeningTable.setPreferredScrollableViewportSize( new Dimension( 820, 320 ) );

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
        constraints.insets = new Insets( 5, 20, 5, 20 );
	constraints.gridx = 0;
	constraints.gridy = 1;
	constraints.gridwidth = 2;
        constraints.anchor = GridBagConstraints.CENTER;

        // Setting weightx, weighty and fill is necessary for proper filling the frame when resized.
        constraints.weightx = 1d;
        constraints.weighty = 1d;
        constraints.fill = GridBagConstraints.BOTH;

	container.add( new JScrollPane( waardeRekeningTable ), constraints );

        constraints.weightx = 0d;
        constraints.weighty = 0d;
        constraints.fill = GridBagConstraints.NONE;


	JPanel buttonPanel = new JPanel( );

	final JButton closeButton = new JButton( "Close" );
	closeButton.addActionListener( ( ActionEvent actionEvent ) -> {
            if ( actionEvent.getActionCommand( ).equals( "close" ) ) {
                setVisible( false );
                dispose();
            }
        } );
	closeButton.setActionCommand( "close" );
	buttonPanel.add( closeButton );


	constraints.gridx = 0;
	constraints.gridy = 2;
	constraints.insets = new Insets( 5, 20, 20, 20 );
	container.add( buttonPanel, constraints );

	setSize( 880, 550 );
        setLocation( x, y );
	setDefaultCloseOperation( JFrame.DISPOSE_ON_CLOSE );
	setVisible( true );
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
