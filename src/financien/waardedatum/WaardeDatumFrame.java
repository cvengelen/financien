// frame to inspect waarde for all rekeningen for a specific date

package financien.waardedatum;

import java.math.BigDecimal;
import java.math.RoundingMode;
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

import financien.gui.WaardeDatumComboBox;
import table.*;

class WaardeDatumFrame {
    final private Logger logger = Logger.getLogger( WaardeDatumFrame.class.getCanonicalName() );

    private final Connection connection;

    private final JFrame frame = new JFrame( "Waarde op geselecteerde datum" );

    private WaardeDatumTableModel waardeDatumTableModel;
    private TableSorter waardeDatumTableSorter;
    private JTable waardeDatumTable;

    private WaardeDatumComboBox waardeDatumComboBox;
    private String selectedWaardeDatumString = null;

    private final SimpleDateFormat dateFormat = new SimpleDateFormat( "yyyy-MM-dd" );
    private final String euroDatumString = "2002-01-01";
    private final String euroKoersenDatumString = "1999-01-01";
    private Date euroDatumDate;
    private Date euroKoersenDatumDate;

    private final int maximumRekeningTypeId = 9;    // Maximum value field rekening_type_id in table rekening_type

    private final String[] rekeningTypeString = new String[ maximumRekeningTypeId + 1 ];
    private final String[] rekeningTypeTotaalFieldString = new String[ maximumRekeningTypeId + 1 ];
    private final JTextField[] rekeningTypeTotaalTextField = new JTextField[ maximumRekeningTypeId + 1 ];
    private JTextField totaalTextField = new JTextField( );
    private String selectQueryTotaalString;

    private JTextField totaalKoersenDatumTextField = new JTextField( );
    private String totaalKoersenDatumString = null;
    private Date totaalKoersenDatumDate;

    private final DecimalFormat[] saldoDecimalFormat = new DecimalFormat[ maximumRekeningTypeId + 1 ];

    private final DecimalFormat euroDecimalFormat = new DecimalFormat( "EUR #0.00;EUR -#" );
    private final DecimalFormat nlgDecimalFormat = new DecimalFormat( "NLG #0.00;NLG -#" );
    private final DecimalFormat usdDecimalFormat = new DecimalFormat( "USD #0.00;USD -#" );
    private final DecimalFormat percentDecimalFormat = new DecimalFormat( "% #0.00;% -#" );

    private final String insertActionCommandString = "insert";
    private final String updateActionCommandString = "update";

    private final long milliSecondsPerDay = 24 * 60 * 60 * 1000;

    WaardeDatumFrame( final Connection connection ) {
        this.connection = connection;

        // Do not allow incorrect dates (e.g., day>31)
        dateFormat.setLenient( false );

        // Get date from datum string objects
        try {
            euroDatumDate = dateFormat.parse( euroDatumString );
            euroKoersenDatumDate = dateFormat.parse( euroKoersenDatumString );
        } catch ( ParseException parseException ) {
            logger.severe( "Euro datum parse exception: " + parseException.getMessage( ) );
            return;
        }

        final Container container = frame.getContentPane( );

        // Set grid bag layout manager
        container.setLayout( new GridBagLayout( ) );
        GridBagConstraints constraints = new GridBagConstraints( );
        constraints.anchor = GridBagConstraints.EAST;
        constraints.insets = new Insets( 10, 50, 5, 5 );
        constraints.gridwidth = 1;
        constraints.gridheight = 1;
        constraints.weightx = 1.0;
        constraints.weighty = 0.0;
        constraints.gridx = 0;
        constraints.gridy = 0;
        container.add( new JLabel( "Datum:" ), constraints );

        // Setup a JComboBox with the results of the query on datum in table waarde
        waardeDatumComboBox = new WaardeDatumComboBox( connection, null );
        constraints.anchor = GridBagConstraints.WEST;
        constraints.gridx = GridBagConstraints.RELATIVE;
        constraints.weightx = 1.0;
        constraints.insets = new Insets( 10, 5, 5, 450 );
        container.add( waardeDatumComboBox, constraints );

        waardeDatumComboBox.addActionListener( ( ActionEvent actionEvent ) -> {
            // Get the selected waarde datum
            selectedWaardeDatumString = waardeDatumComboBox.getSelectedWaardeDatumString( );

            // Check if rekening has been selected
            if ( ( selectedWaardeDatumString == null ) ||
                    ( selectedWaardeDatumString.length( ) == 0 ) ) {
                return;
            }

            // Setup the totaal fields and waarde table for the specified date
            setupWaardeDatumTable( selectedWaardeDatumString );
        } );


        // Get the values for rekening_type, rekening_pattern, and rekening_totaal_field for all
        // records in table rekening_type and store in the arrays indexed by rekening_type_id.
        try {
            Statement rekeningTypeStatement = connection.createStatement( );
            ResultSet rekeningTypeResultSet =
                    rekeningTypeStatement.executeQuery( "SELECT rekening_type_id, rekening_type, " +
                            "rekening_pattern, rekening_totaal_field " +
                            " FROM rekening_type" );
            while ( rekeningTypeResultSet.next( ) ) {
                final int rekeningTypeId = rekeningTypeResultSet.getInt( 1 );
                rekeningTypeString[ rekeningTypeId ] = rekeningTypeResultSet.getString( 2 );
                saldoDecimalFormat[ rekeningTypeId ] = new DecimalFormat( rekeningTypeResultSet.getString( 3 ) );
                rekeningTypeTotaalFieldString[ rekeningTypeId ] = rekeningTypeResultSet.getString( 4 );
            }
        } catch ( SQLException sqlException ) {
            logger.severe( "SQLException in rekeningTypeStatement: " + sqlException.getMessage( ) );
        }

        // Setup the JTextField fields for the totaal value of each rekening_type,
        // and setup the string used in the SELECT query on the totaal table
        selectQueryTotaalString = "";
        final Insets insetsLabel = new Insets( 2, 50, 2, 5 );
        final Insets insetsText = new Insets( 2, 5, 2, 450 );
        int rekeningTypeId;
        for ( rekeningTypeId = 1; rekeningTypeId <= maximumRekeningTypeId; rekeningTypeId++ ) {
            constraints.gridx = 0;
            constraints.gridy = rekeningTypeId;
            constraints.anchor = GridBagConstraints.EAST;
            constraints.insets = insetsLabel;
            constraints.gridwidth = 1;
            constraints.weightx = 1.0;
            constraints.fill = GridBagConstraints.NONE;
            container.add( new JLabel( rekeningTypeString[ rekeningTypeId ] + ":" ), constraints );

            rekeningTypeTotaalTextField[ rekeningTypeId ] = new JTextField( 10 );
            rekeningTypeTotaalTextField[ rekeningTypeId ].setHorizontalAlignment( JTextField.RIGHT );
            rekeningTypeTotaalTextField[ rekeningTypeId ].setEnabled( false );
            constraints.gridx = GridBagConstraints.RELATIVE;
            constraints.anchor = GridBagConstraints.WEST;
            constraints.insets = insetsText;
            constraints.gridwidth = 1;
            constraints.weightx = 1.0;
            constraints.fill = GridBagConstraints.HORIZONTAL;
            container.add( rekeningTypeTotaalTextField[ rekeningTypeId ], constraints );

            selectQueryTotaalString += rekeningTypeTotaalFieldString[ rekeningTypeId ] + ", ";
        }
        logger.info( "selectQueryTotaalString: " + selectQueryTotaalString );

        // Setup the JTextField field for the overall totaal
        constraints.gridx = 0;
        constraints.gridy = rekeningTypeId;
        constraints.gridwidth = 1;
        constraints.weightx = 1.0;
        constraints.fill = GridBagConstraints.NONE;
        constraints.anchor = GridBagConstraints.EAST;
        constraints.insets = insetsLabel;
        container.add( new JLabel( "Totaal:" ), constraints );

        totaalTextField = new JTextField( 10 );
        totaalTextField.setHorizontalAlignment( JTextField.RIGHT );
        totaalTextField.setEnabled( false );
        constraints.gridx = GridBagConstraints.RELATIVE;
        constraints.anchor = GridBagConstraints.WEST;
        constraints.insets = insetsText;
        constraints.gridwidth = 1;
        constraints.weightx = 1.0;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        container.add( totaalTextField, constraints );

        // Setup the JTextField field for the koersen datum
        constraints.gridx = 0;
        constraints.gridy += 1;
        constraints.gridwidth = 1;
        constraints.weightx = 1.0;
        constraints.fill = GridBagConstraints.NONE;
        constraints.anchor = GridBagConstraints.EAST;
        constraints.insets = insetsLabel;
        container.add( new JLabel( "Datum koersen:" ), constraints );

        totaalKoersenDatumTextField = new JTextField( 10 );
        totaalKoersenDatumTextField.setHorizontalAlignment( JTextField.RIGHT );
        totaalKoersenDatumTextField.setEnabled( false );
        constraints.gridx = GridBagConstraints.RELATIVE;
        constraints.anchor = GridBagConstraints.WEST;
        constraints.insets = insetsText;
        constraints.gridwidth = 1;
        constraints.weightx = 1.0;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        container.add( totaalKoersenDatumTextField, constraints );


        // Create waarde-datum table from waarde-datum table model
        waardeDatumTableModel = new WaardeDatumTableModel( connection );
        waardeDatumTableSorter = new TableSorter( waardeDatumTableModel );
        waardeDatumTable = new JTable( waardeDatumTableSorter );
        waardeDatumTableSorter.setTableHeader( waardeDatumTable.getTableHeader( ) );
        // waardeDatumTableSorter.setSortingStatus( 1, TableSorter.ASCENDING );

        waardeDatumTable.setAutoResizeMode( JTable.AUTO_RESIZE_OFF );
        waardeDatumTable.getColumnModel( ).getColumn( 0 ).setPreferredWidth( 200 );  // Rekening
        waardeDatumTable.getColumnModel( ).getColumn( 1 ).setPreferredWidth( 50 );  // Rekening Type Id
        waardeDatumTable.getColumnModel( ).getColumn( 2 ).setPreferredWidth( 110 );  // Saldo
        waardeDatumTable.getColumnModel( ).getColumn( 3 ).setPreferredWidth( 110 );  // Koers
        waardeDatumTable.getColumnModel( ).getColumn( 4 ).setPreferredWidth( 110 );  // Waarde
        waardeDatumTable.getColumnModel( ).getColumn( 5 ).setPreferredWidth( 110 );  // Inleg
        waardeDatumTable.getColumnModel( ).getColumn( 6 ).setPreferredWidth( 110 );  // Waarde-Inleg
        waardeDatumTable.getColumnModel( ).getColumn( 7 ).setPreferredWidth( 60 );  // Rendement
        waardeDatumTable.getColumnModel( ).getColumn( 8 ).setPreferredWidth( 60 );  // RendementTotaal
        waardeDatumTable.getColumnModel( ).getColumn( 9 ).setPreferredWidth( 60 );  // AantalJaren
        waardeDatumTable.getColumnModel( ).getColumn( 10 ).setPreferredWidth( 60 );  // RendementPerJaar

        // Set vertical size just enough for 20 entries
        waardeDatumTable.setPreferredScrollableViewportSize( new Dimension( 1040, 320 ) );

        // Set renderer for Double objects
        class DoubleRenderer extends JTextField implements TableCellRenderer {
            public Component getTableCellRendererComponent( JTable table,
                                                            Object object,
                                                            boolean isSelected,
                                                            boolean hasFocus,
                                                            int row, int column ) {
                switch ( column ) {
                    case 2:        // Saldo
                        // Get the rekening type id of this row from the table
                        final int rekeningTypeId = ( Integer )waardeDatumTable.getValueAt( row, 1 );
                        // Use the formatter defined for this rekening_type in table rekening_type
                        this.setText( saldoDecimalFormat[ rekeningTypeId ].format( ( ( Double ) object ).doubleValue( ) ) );

                        break;

                    case 3:        // Koers
                        // For dates before 1-1-1999, the koers is given in NLG, after 1-1-1999 in EUR or USD
                        if ( totaalKoersenDatumDate.before( euroKoersenDatumDate ) ) {
                            // Koers is in NLG
                            this.setText( nlgDecimalFormat.format( ( ( Double ) object ).doubleValue( ) ) );
                        } else {
                            // Koers is in EUR or USD: get the currencyId from the table model
                            if ( waardeDatumTableModel.getCurrencyId( row ) == 3 ) {
                                // Koers is in USD
                                this.setText( usdDecimalFormat.format( ( ( Double ) object ).doubleValue( ) ) );
                            } else {
                                // In all other cases the koers is in EUR
                                this.setText( euroDecimalFormat.format( ( ( Double ) object ).doubleValue( ) ) );
                            }
                        }

                        break;

                    case  4:        // Waarde
                    case  5:        // Inleg
                    case  6:        // Waarde - inleg
                        this.setText( euroDecimalFormat.format( ( ( Double ) object ).doubleValue( ) ) );
                        break;
                    case  7:        // Rendement
                    case  8:        // RendementTotaal
                    case 10:        // RendementPerJaar
                    case 11:        // RendementComparePerJaar
                        this.setText( percentDecimalFormat.format( ( ( Double ) object ).doubleValue( ) ) );
                        break;
                    case 9:         // aantal jaren
                        this.setText( String.format( "%.2f", ( Double )object ) );
                        break;
                    default:    // Unexpected column: just return the string related to the object
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
        waardeDatumTable.setDefaultRenderer( Double.class, doubleRenderer );

        // Add table to the container
        constraints.gridx = 0;
        constraints.gridy += 1;
        constraints.gridwidth = 2;
        constraints.weightx = 1.0;
        constraints.ipadx = 40;

        // Setting weighty and fill is necessary for proper filling the frame when resized.
        constraints.weighty = 1.0;
        constraints.fill = GridBagConstraints.BOTH;
        constraints.anchor = GridBagConstraints.CENTER;
        constraints.insets = new Insets( 5, 20, 5, 20 );
        container.add( new JScrollPane( waardeDatumTable ), constraints );


        // Class to handle button actions
        class ButtonActionListener implements ActionListener {
            public void actionPerformed( ActionEvent actionEvent ) {
                final String actionCommandString = actionEvent.getActionCommand( );
                if ( actionEvent.getActionCommand( ).equals( "close" ) ) {
                    frame.setVisible( false );
                    System.exit( 0 );
                } else if ( actionCommandString.equals( insertActionCommandString ) ) {
                    final GregorianCalendar calendar = new GregorianCalendar( );
                    final Date todayDate = calendar.getTime( );
                    String insertDatumString = dateFormat.format( todayDate );
                    insertDatumString = ( String ) JOptionPane.showInputDialog( frame,
                            "Insert datum:",
                            "Insert waarde datum",
                            JOptionPane.PLAIN_MESSAGE,
                            null,
                            null,
                            insertDatumString );
                    logger.info( "insertDatumString: " + insertDatumString );
                    try {
                        // The dateFormat is set to be not lenient, so parsing the date
                        // also checks for invalid day or month.
                        final Date insertDate = dateFormat.parse( insertDatumString );

                        // Check for insert date in the future
                        if ( insertDate.after( todayDate ) ) {
                            JOptionPane.showMessageDialog( frame,
                                    "Insert datum: " + insertDatumString +
                                            "\nin de toekomst niet toegestaan",
                                    "Insert waarde datum error",
                                    JOptionPane.ERROR_MESSAGE
                            );
                            return;
                        }

                        // Insert new records in waarde and totaal for the specified date
                        if ( !updateWaarde( actionCommandString, insertDatumString ) ) {
                            JOptionPane.showMessageDialog( frame,
                                    "Geen insert nodig voor datum " + insertDatumString,
                                    "Insert waarde",
                                    JOptionPane.INFORMATION_MESSAGE );
                        }

                        // Alway setup the waarde Datum combo box, the totaal fields,
                        // and the waarde table for the specified date again because
                        // even if the dat already existed and no update was necessary,
                        // this date may not be the selected date.
                        waardeDatumComboBox.setupWaardeDatumComboBox( insertDatumString );
                        setupWaardeDatumTable( insertDatumString );
                    } catch ( ParseException parseException ) {
                        // The exception message already shows the invalid date string
                        JOptionPane.showMessageDialog( frame,
                                "Incorrecte insert datum syntax (yyyy-mm-dd)\n" +
                                        parseException.getMessage( ),
                                "Insert waarde datum error",
                                JOptionPane.ERROR_MESSAGE
                        );
                        return;
                    }
                } else if ( actionCommandString.equals( updateActionCommandString ) ) {
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

                    // Update existing records in waarde and totaal for the selected date
                    if ( updateWaarde( actionCommandString, selectedWaardeDatumString ) ) {
                        // Setup the totaal fields and waarde table for the specified date
                        setupWaardeDatumTable( selectedWaardeDatumString );
                    } else {
                        JOptionPane.showMessageDialog( frame,
                                "Geen update nodig voor datum " + selectedWaardeDatumString,
                                "Update waarde",
                                JOptionPane.INFORMATION_MESSAGE );
                    }

                }
            }
        }
        final ButtonActionListener buttonActionListener = new ButtonActionListener( );

        JPanel buttonPanel = new JPanel( );

        final JButton insertButton = new JButton( "Insert" );
        insertButton.addActionListener( buttonActionListener );
        insertButton.setActionCommand( "insert" );
        buttonPanel.add( insertButton );

        final JButton updateButton = new JButton( "Update" );
        updateButton.addActionListener( buttonActionListener );
        updateButton.setActionCommand( "update" );
        buttonPanel.add( updateButton );

        final JButton closeButton = new JButton( "Close" );
        closeButton.addActionListener( buttonActionListener );
        closeButton.setActionCommand( "close" );
        buttonPanel.add( closeButton );


        constraints.gridx = 0;
        constraints.gridy += 1;
        constraints.weightx = 1.0;
        constraints.weighty = 0.0;
        constraints.fill = GridBagConstraints.NONE;
        constraints.anchor = GridBagConstraints.CENTER;
        constraints.insets = new Insets( 5, 10, 10, 10 );
        container.add( buttonPanel, constraints );

        frame.setSize( 1260, 850 );
        frame.setDefaultCloseOperation( JFrame.DISPOSE_ON_CLOSE );
        frame.setVisible( true );
    }


    private void setupWaardeDatumTable( String waardeDatumString ) {
        // Get the related record for the selected date from table totaal
        try {
            Statement totaalStatement = connection.createStatement( );
            ResultSet totaalResultSet =
                    totaalStatement.executeQuery( "SELECT " + selectQueryTotaalString + " totaal, datum_koersen " +
                            "FROM totaal WHERE datum = '" + waardeDatumString + "'" );

            if ( !totaalResultSet.next( ) ) {
                logger.severe( "Could not get record for date " + waardeDatumString +
                        " in totaal" );
                return;
            }

            // Store the values from the totaal record in the totaal labels of the accounts
            int rekeningTypeId;
            for ( rekeningTypeId = 1; rekeningTypeId <= maximumRekeningTypeId; rekeningTypeId++ ) {
                final String rekeningTypeTotaalString =
                        euroDecimalFormat.format( totaalResultSet.getDouble( rekeningTypeId ) );
                rekeningTypeTotaalTextField[ rekeningTypeId ].setText( rekeningTypeTotaalString );
            }

            // Store the value from the totaal record for the overall totaal
            totaalTextField.setText( euroDecimalFormat.format( totaalResultSet.getDouble( rekeningTypeId ) ) );

            // Get the date from which the koersen are taken,
            // used in DoubleRenderer for rendering Koers
            totaalKoersenDatumString = totaalResultSet.getString( rekeningTypeId + 1 );

            // Set the totaal koersen datum text field
            totaalKoersenDatumTextField.setText( totaalKoersenDatumString );
        } catch ( SQLException sqlException ) {
            logger.severe( "SQLException in totaalStatement: " + sqlException.getMessage( ) );
        }

        // Convert the totaal koersen datum string to a Date object
        try {
            totaalKoersenDatumDate = dateFormat.parse( totaalKoersenDatumString );
        } catch ( ParseException parseException ) {
            logger.severe( "Totaal koersen datum parse exception: " + parseException.getMessage( ) );
            return;
        }

        // Setup the waarde datum table model for the specified date
        waardeDatumTableModel.setupWaardeDatumTableModel( waardeDatumString );

        // Setup the tableSorter again so that the TableSorter gets the new table size (# rows)
        waardeDatumTableSorter.setTableModel( waardeDatumTableModel );

        // Need to set column width again
        waardeDatumTable.getColumnModel( ).getColumn(  0 ).setPreferredWidth( 200 );  // Rekening
        waardeDatumTable.getColumnModel( ).getColumn(  1 ).setPreferredWidth(  40 );  // Rekening Type Id
        waardeDatumTable.getColumnModel( ).getColumn(  2 ).setPreferredWidth( 110 );  // Saldo
        waardeDatumTable.getColumnModel( ).getColumn(  3 ).setPreferredWidth( 110 );  // Koers
        waardeDatumTable.getColumnModel( ).getColumn(  4 ).setPreferredWidth( 110 );  // Waarde
        waardeDatumTable.getColumnModel( ).getColumn(  5 ).setPreferredWidth( 110 );  // Inleg
        waardeDatumTable.getColumnModel( ).getColumn(  6 ).setPreferredWidth( 110 );  // Waarde-Inleg
        waardeDatumTable.getColumnModel( ).getColumn(  7 ).setPreferredWidth(  80 );  // Rendement
        waardeDatumTable.getColumnModel( ).getColumn(  8 ).setPreferredWidth(  80 );  // Rendement totaal
        waardeDatumTable.getColumnModel( ).getColumn(  9 ).setPreferredWidth(  60 );  // Jaren
        waardeDatumTable.getColumnModel( ).getColumn( 10 ).setPreferredWidth(  80 );  // Rendement per jaar
        waardeDatumTable.getColumnModel( ).getColumn( 11 ).setPreferredWidth( 110 );  // Rendement compare per jaar
    }


    private String updateString = null;

    private void addToUpdateString( String additionalUpdateString ) {
        if ( updateString == null ) {
            updateString = additionalUpdateString;
        } else {
            updateString += ", " + additionalUpdateString;
        }
    }


    private boolean updateWaarde( String actionCommandString,
                                  String waardeDatumString ) {
        final double euroConv = 2.20371;
        boolean update = false;

        // Get the Date for the waarde datum
        Date waardeDatumDate;
        try {
            waardeDatumDate = dateFormat.parse( waardeDatumString );
        } catch ( ParseException parseException ) {
            logger.severe( "Waarde datum parse exception: " + parseException.getMessage( ) );
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
                final String errorString =
                        "Geen koers info gevonden tussen " + minimumKoersenDatumString +
                                " en " + waardeDatumString;
                logger.severe( errorString );
                JOptionPane.showMessageDialog( frame, errorString,
                        actionCommandString + " waarde error",
                        JOptionPane.ERROR_MESSAGE );
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
        } catch ( ParseException parseException ) {
            logger.severe( "Koersen datum parse exception: " + parseException.getMessage( ) );
            return false;
        }

        // TODO: selectie van rekeninghouder
        // TODO: waarde opslaan per rekeninghouder
        try {
            Statement rekeningStatement = connection.createStatement( );
            ResultSet rekeningResultSet =
                    rekeningStatement.executeQuery( "SELECT rekening_id, rekening, type_id, " +
                            "fonds, start_saldo, start_datum, currency_id, " +
                            "rekening_type.rekening_pattern " +
                            "FROM rekening " +
                            "LEFT JOIN rekening_type ON rekening.type_id = rekening_type.rekening_type_id " +
                            "WHERE rekening_houder_id = 1" );

            // Setup Totaal for each rekening type
            double[] rekeningTypeTotaal = new double[ maximumRekeningTypeId + 1 ];
            for ( int rekeningTypeId = 0; rekeningTypeId <= maximumRekeningTypeId; rekeningTypeId++ ) {
                rekeningTypeTotaal[ rekeningTypeId ] = 0;
            }

            //////////////////////////////////////
            // Loop over all entries in rekening
            //////////////////////////////////////

            while ( rekeningResultSet.next( ) ) {
                final int rekeningId = rekeningResultSet.getInt( 1 );
                final String rekeningString = rekeningResultSet.getString( 2 );
                final int rekeningTypeId = rekeningResultSet.getInt( 3 );
                final String fonds = rekeningResultSet.getString( 4 );
                final double startSaldo = rekeningResultSet.getDouble( 5 );
                final String startDatumString = rekeningResultSet.getString( 6 );
                final int currencyId = rekeningResultSet.getInt( 7 );

                final DecimalFormat saldoDecimalFormat = new DecimalFormat( rekeningResultSet.getString( 8 ) );

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
                    } catch ( ParseException parseException ) {
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
                // Maar niet voor Credit cards, want daarvoor kan het saldo 0 zijn
                if ( ( rekeningTypeId != 6 ) && ( Math.abs( saldo ) < 0.1 ) ) continue;

                double waarde = saldo;
                double koers = 0.0;
                double splitsFactor = 1.0;
                String koersString = "";
                double inlegAandelenTotaal = 0;
                double waardeMinusInleg = 0;
                double rendement = 0;
                double rendementPerDag = 0;
                double rendementPerJaar = 0;
                double rendementTotaal = 0;
                double rendementComparePerJaar = 0;
                double aantalJaren = 0;

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

                    try {
                        boolean splits = false;
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

                    // Rendement berekening
                    try {
                        Statement inlegStatement = connection.createStatement( );
                        ResultSet inlegResultSet =
                                inlegStatement.executeQuery( "SELECT inleg_aandelen, datum" +
                                        " FROM rekening_mutatie " +
                                        " LEFT JOIN rubriek ON rekening_mutatie.rubriek_id = rubriek.rubriek_id " +
                                        " WHERE rekening_id = " + rekeningId +
                                        " AND rubriek.groep_id = 1000 " +
                                        " AND datum >= '" + startDatumString + "'" +
                                        " AND datum <= '" + waardeDatumString + "'" +
                                        " AND NOT inleg_aandelen IS NULL" +
                                        " ORDER BY datum" );

                        logger.fine( "rendement aandelen voor " + rekeningString );

                        // 20160101: nieuwe berekening
                        // De waarde is de som over de inleg, plus voor elke inleg de inleg maal het rendement per dag, maal het aantal dagen tot de waarde datum.
                        // In formule:
                        //   waarde = som(inleg(i)) + som( rendementPerDag * inleg(i) * dagenTotWaardeDatumDate(i) )
                        // Het rendement per dag kan uit de som gehaald worden:
                        //   waarde = som(inleg(i)) + rendementPerDag * som( inleg(i) * dagenTotWaardeDatumDate(i) )
                        // En dus kan het rendement per dag bepaald worden met:
                        //   rendementPerDag = ( waarde = som(inleg(i) ) / som( inleg(i) * dagenTotWaardeDatumDate(i) )

                        final GregorianCalendar gregorianCalendar = new GregorianCalendar( );
                        gregorianCalendar.setTime( waardeDatumDate );
                        final long waardeDatumTime = gregorianCalendar.getTimeInMillis();
                        double somInlegDagenTotWaardeDatum = 0;
                        long dagTotaal = 0;

                        // 20160101: test rendement vanaf compare datum 2015-0101
                        final Statement waardeCompareStatement = connection.createStatement( );
                        final ResultSet waardeCompareResultSet =
                                waardeCompareStatement.executeQuery( "SELECT waarde FROM waarde " +
                                        " WHERE rekening_id = " + rekeningId + " AND datum = '2015-01-01'" );
                        double compareWaardeTotaal = 0L;
                        if ( waardeCompareResultSet.next( ) ) compareWaardeTotaal = waardeCompareResultSet.getDouble( 1 );
                        gregorianCalendar.set( 2015, Calendar.JANUARY, 1 );
                        Date compareDate = gregorianCalendar.getTime();
                        long compareTime = gregorianCalendar.getTimeInMillis( );
                        long diffCompareTimeDays = ( waardeDatumTime - compareTime ) / milliSecondsPerDay;

                        // Som over de inleg maal het aantal dagen dat de inleg rendement heeft gegeven
                        double somCompareDagenTotWaardeDatum = compareWaardeTotaal * diffCompareTimeDays;
                        logger.fine( "compare datum: " + compareDate.toString( ) + ", compare delta dagen: " + diffCompareTimeDays + ", compare waarde: " + compareWaardeTotaal );

                        // Loop over alle inleggingen in het fonds
                        while ( inlegResultSet.next( ) ) {
                            final double inlegAandelen = inlegResultSet.getDouble( 1 );
                            final Date inlegAandelenDate = inlegResultSet.getDate( 2 );

                            // Som over de inleg
                            inlegAandelenTotaal += inlegAandelen;

                            // 20160101: nieuwe berekening
                            // Bepaal aantal dagen van inleg datum tot de waarde datum
                            gregorianCalendar.setTime( inlegAandelenDate );
                            long inlegAandelenTime = gregorianCalendar.getTimeInMillis( );
                            long diffTimeDays = ( waardeDatumTime - inlegAandelenTime ) / milliSecondsPerDay;

                            // Som over de inleg maal het aantal dagen dat de inleg rendement heeft gegeven
                            somInlegDagenTotWaardeDatum += inlegAandelen * diffTimeDays;
                            logger.fine( "datum: " + inlegAandelenDate.toString( ) + ", delta dagen: " + diffTimeDays + ", inleg: " + inlegAandelen + ", som( inleg(i) * dagenTotWaardeDatumDate(i)): " + somInlegDagenTotWaardeDatum );

                            // Enkel ter controle: het aantal dagen vanaf de eerste inleg tot de waarde datum
                            if ( dagTotaal == 0 ) dagTotaal = diffTimeDays;

                            // Controleer of de inleg na de compare datum ligt
                            if ( inlegAandelenDate.after( compareDate ) ) {
                                compareWaardeTotaal += inlegAandelen;
                                somCompareDagenTotWaardeDatum += inlegAandelen * diffTimeDays;
                                logger.fine( "datum: " + inlegAandelenDate.toString( ) + ", compare waarde totaal: " + compareWaardeTotaal );
                            }
                        }

                        // 20160101: Nieuwe berekening
                        rendementPerDag = ( waarde - inlegAandelenTotaal ) / somInlegDagenTotWaardeDatum;
                        rendementPerJaar = rendementPerDag * 365D;
                        rendementTotaal = rendementPerDag * dagTotaal;
                        aantalJaren = 1D * dagTotaal / 365D;
                        logger.fine( String.format( "Waarde nu: %s, aantal jaren: %.2f, rendementPerJaar: %.4f, rendementTotaal: %.4f", waarde, aantalJaren, rendementPerJaar, rendementTotaal ) );

                        // 20160101: rendement vanaf compare datum (20150101)
                        double rendementComparePerDag = ( waarde - compareWaardeTotaal ) / somCompareDagenTotWaardeDatum;
                        rendementComparePerJaar = rendementComparePerDag * 365D;
                        double rendementCompareTotaal = rendementComparePerDag * diffCompareTimeDays;
                        double aantalCompareJaren = 1D * diffCompareTimeDays / 365D;
                        logger.fine( String.format( "Waarde nu: %s, aantal compare jaren: %.2f, rendementComparePerJaar: %.4f, rendementCompareTotaal: %.4f", waarde, aantalCompareJaren, rendementComparePerJaar, rendementCompareTotaal ) );

                        // Ter controle: bepaal rendement simpel uit verschil huidige waarde en de som van de inleg
                        waardeMinusInleg = waarde - inlegAandelenTotaal;
                        if ( Math.abs( inlegAandelenTotaal ) > 0.1 ) {
                            // Store rendement as a fraction.
                            // The % in the format pattern makes this a percentage when formatted
                            rendement = waardeMinusInleg / inlegAandelenTotaal;
                        }
                    } catch ( SQLException sqlException ) {
                        logger.severe( "SQLException in inlegStatement: " + sqlException.getMessage( ) );
                    }
                }

                // Update totaal for the rekening type
                rekeningTypeTotaal[ rekeningTypeId ] += waarde;

                // Get existing record in waarde, if present
                try {
                    Statement waardeStatement = connection.createStatement( );
                    ResultSet waardeResultSet =
                            waardeStatement.executeQuery( "SELECT saldo, koers, waarde, inleg, waarde_minus_inleg," +
                                    " rendement, rendement_totaal, aantal_jaren, rendement_per_jaar, rendement_compare_per_jaar" +
                                    " FROM waarde " +
                                    " WHERE rekening_id = " + rekeningId +
                                    " AND datum = '" + waardeDatumString + "'" );

                    if ( waardeResultSet.next( ) ) {
                        // Record for this rekening at this date exist, so update this record.

                        // Clear the string used for assembling the update query
                        updateString = null;

                        String infoString =
                                "\nUpdate tabel waarde voor datum " + waardeDatumString +
                                        " en rekening " + rekeningString;

                        final double saldoWaardeTable = waardeResultSet.getDouble( 1 );
                        if ( Math.abs( saldo - saldoWaardeTable ) > 0.01 ) {
                            addToUpdateString( "saldo = " + saldo );
                            infoString +=
                                    "\nsaldo  \t\t" + saldoDecimalFormat.format( saldoWaardeTable ) +
                                            "  \t" + saldoDecimalFormat.format( saldo ) +
                                            "  \t" + String.valueOf( saldo - saldoWaardeTable );
                        }

                        final double waardeWaardeTable = waardeResultSet.getDouble( 3 );
                        if ( Math.abs( waarde - waardeWaardeTable ) > 0.01 ) {
                            addToUpdateString( "waarde = " + waarde );
                            infoString +=
                                    "\nwaarde  \t" + euroDecimalFormat.format( waardeWaardeTable ) +
                                            "  \t" + euroDecimalFormat.format( waarde ) +
                                            "  \t" + String.valueOf( waarde - waardeWaardeTable );
                        }

                        // Check the type of account
                        if ( ( rekeningTypeId == 4 ) || ( rekeningTypeId == 5 ) ||
                                ( rekeningTypeId == 7 ) ) {
                            // Stock account

                            final double koersWaardeTable = waardeResultSet.getDouble( 2 );
                            if ( Math.abs( koers - koersWaardeTable ) > 0.01 ) {
                                addToUpdateString( "koers = " + koers );
                                infoString +=
                                        "\nkoers  \t\t" + koersWaardeTable + "  \t" + koersString +
                                                "  \t" + String.valueOf( koers - koersWaardeTable );
                            }

                            final double inlegWaardeTable = waardeResultSet.getDouble( 4 );
                            if ( Math.abs( inlegAandelenTotaal - inlegWaardeTable ) > 0.02 ) {
                                addToUpdateString( "inleg = " + inlegAandelenTotaal );
                                infoString +=
                                        "\ninleg  \t\t" + inlegWaardeTable +
                                                "  \t" + euroDecimalFormat.format( inlegAandelenTotaal ) +
                                                "  \t" + String.valueOf( inlegAandelenTotaal - inlegWaardeTable );
                            }

                            final double waardeMinusInlegWaardeTable = waardeResultSet.getDouble( 5 );
                            if ( Math.abs( waardeMinusInleg - waardeMinusInlegWaardeTable ) > 0.02 ) {
                                addToUpdateString( "waarde_minus_inleg = " + waardeMinusInleg );
                                infoString +=
                                        "\nwaarde-inleg  \t" + waardeMinusInlegWaardeTable +
                                                "  \t" + euroDecimalFormat.format( waardeMinusInleg ) +
                                                "  \t" + String.valueOf( waardeMinusInleg - waardeMinusInlegWaardeTable );
                            }

                            final double rendementWaardeTable = waardeResultSet.getDouble( 6 );
                            if ( Math.abs( rendement - rendementWaardeTable ) > 0.0001 ) {
                                // Truncate on 5 decimals, to avoid data truncation error in SQL update
                                // Zie: http://docs.oracle.com/javase/6/docs/api/java/math/BigDecimal.html
                                // Scale is the number of digits to the right of the decimal point
                                BigDecimal bigDecimal = new BigDecimal( rendement );
                                String rendementConverted = bigDecimal.setScale( 4, RoundingMode.HALF_UP ).toString( );

                                addToUpdateString( "rendement = " + rendementConverted );
                                infoString +=
                                        "\nrendement  \t" + percentDecimalFormat.format( rendementWaardeTable ) +
                                                "  \t" + percentDecimalFormat.format( rendement ) +
                                                "  \t" + String.valueOf( rendement - rendementWaardeTable );
                            }

                            final double rendementTotaalWaardeTable = waardeResultSet.getDouble( 7 );
                            if ( Math.abs( rendementTotaal - rendementTotaalWaardeTable ) > 0.0001 ) {
                                // Truncate on 5 decimals, to avoid data truncation error in SQL update
                                // Zie: http://docs.oracle.com/javase/6/docs/api/java/math/BigDecimal.html
                                // Scale is the number of digits to the right of the decimal point
                                BigDecimal bigDecimal = new BigDecimal( rendementTotaal );
                                String rendementTotaalConverted = bigDecimal.setScale( 4, RoundingMode.HALF_UP ).toString( );

                                addToUpdateString( "rendement_totaal = " + rendementTotaalConverted );
                                infoString +=
                                        "\nrendement totaal  \t" + percentDecimalFormat.format( rendementTotaalWaardeTable ) +
                                                "  \t" + percentDecimalFormat.format( rendementTotaal ) +
                                                "  \t" + String.valueOf( rendementTotaal - rendementTotaalWaardeTable );
                            }

                            final double aantalJarenWaardeTable = waardeResultSet.getDouble( 8 );
                            if ( Math.abs( aantalJaren - aantalJarenWaardeTable ) > 0.01 ) {
                                // Truncate on 2 decimals, to avoid data truncation error in SQL update
                                // Zie: http://docs.oracle.com/javase/6/docs/api/java/math/BigDecimal.html
                                // Scale is the number of digits to the right of the decimal point
                                BigDecimal bigDecimal = new BigDecimal( aantalJaren );
                                String aantalJarenConverted = bigDecimal.setScale( 2, RoundingMode.HALF_UP ).toString( );

                                addToUpdateString( "aantal_jaren = " + aantalJarenConverted );
                                infoString +=
                                        "\naantal jaren  \t" + String.format( "%.2f", aantalJarenWaardeTable ) +
                                                "  \t" + String.format( "%.2f", aantalJaren ) +
                                                "  \t" + String.valueOf( aantalJaren - aantalJarenWaardeTable );
                            }

                            final double rendementPerJaarWaardeTable = waardeResultSet.getDouble( 9 );
                            if ( Math.abs( rendementPerJaar - rendementPerJaarWaardeTable ) > 0.0001 ) {
                                // Truncate on 5 decimals, to avoid data truncation error in SQL update
                                // Zie: http://docs.oracle.com/javase/6/docs/api/java/math/BigDecimal.html
                                // Scale is the number of digits to the right of the decimal point
                                BigDecimal bigDecimal = new BigDecimal( rendementPerJaar );
                                String rendementPerJaarConverted = bigDecimal.setScale( 4, RoundingMode.HALF_UP ).toString( );

                                addToUpdateString( "rendement_per_jaar = " + rendementPerJaarConverted );
                                infoString +=
                                        "\nrendement per jaar  \t" + percentDecimalFormat.format( rendementPerJaarWaardeTable ) +
                                                "  \t" + percentDecimalFormat.format( rendementPerJaar ) +
                                                "  \t" + String.valueOf( rendementPerJaar - rendementPerJaarWaardeTable );
                            }

                            final double rendementComparePerJaarWaardeTable = waardeResultSet.getDouble( 10 );
                            if ( Math.abs( rendementComparePerJaar - rendementComparePerJaarWaardeTable ) > 0.0001 ) {
                                // Truncate on 5 decimals, to avoid data truncation error in SQL update
                                // Zie: http://docs.oracle.com/javase/6/docs/api/java/math/BigDecimal.html
                                // Scale is the number of digits to the right of the decimal point
                                BigDecimal bigDecimal = new BigDecimal( rendementComparePerJaar );
                                String rendementComparePerJaarConverted = bigDecimal.setScale( 4, RoundingMode.HALF_UP ).toString( );

                                addToUpdateString( "rendement_compare_per_jaar = " + rendementComparePerJaarConverted );
                                infoString +=
                                        "\nrendement compare per jaar  \t" + percentDecimalFormat.format( rendementComparePerJaarWaardeTable ) +
                                                "  \t" + percentDecimalFormat.format( rendementComparePerJaar ) +
                                                "  \t" + String.valueOf( rendementComparePerJaar - rendementComparePerJaarWaardeTable );
                            }
                        }

                        // Check if update is necessary
                        if ( updateString == null ) {
                            logger.info( "Geen update in tabel waarde nodig voor rekening " + rekeningString +
                                    " en datum " + waardeDatumString );

                            // Go to the next rekening, if any
                            continue;
                        }

                        updateString = "UPDATE waarde SET " + updateString;
                        updateString +=
                                " WHERE datum = '" + waardeDatumString +
                                        "' AND rekening_id = " + rekeningId;

                        logger.info( infoString + "\n" + "updateString: " + updateString + "\n" );

                        JOptionPane.showMessageDialog( frame, infoString,
                                "Update waarde",
                                JOptionPane.INFORMATION_MESSAGE );

                        // Update the record in waarde
                        try {
                            final Statement updateStatement = connection.createStatement( );
                            if ( updateStatement.executeUpdate( updateString ) != 1 ) {
                                logger.severe( "Could not update in waarde" );
                            } else {
                                update = true;
                            }
                        } catch ( SQLException sqlException ) {
                            logger.severe( "SQLException in updateStatement: " + sqlException.getMessage( ) );
                        }

                    } else if ( actionCommandString.equals( updateActionCommandString ) ) {
                        logger.severe( "Could not get record in table waarde for rekening " + rekeningString +
                                " and date " + waardeDatumString );
                        continue;
                    } else {
                        // Datum not found, and action command is not update
                        // Insert new record in table waarde

                        String insertString =
                                "INSERT INTO waarde SET datum = '" + waardeDatumString +
                                        "', rekening_id = " + rekeningId +
                                        ", saldo = " + saldo +
                                        ", waarde = " + waarde;

                        String infoString =
                                "\nInsert record in tabel waarde voor datum " + waardeDatumString +
                                        " en rekening " + rekeningString +
                                        "\nsaldo:  \t" + saldoDecimalFormat.format( saldo ) +
                                        "\nwaarde:  \t" + euroDecimalFormat.format( waarde );

                        // Check the type of account
                        if ( ( rekeningTypeId == 4 ) || ( rekeningTypeId == 5 ) ||
                                ( rekeningTypeId == 7 ) ) {
                            // Stock account
                            insertString +=
                                    ", koers = " + koers +
                                            ", splits_factor = " + splitsFactor +
                                            ", inleg  = " + inlegAandelenTotaal +
                                            ", waarde_minus_inleg = " + waardeMinusInleg +
                                            ", rendement = " + rendement;
                            infoString +=
                                    "\nkoers:  \t" + koersString +
                                            "\ninleg:  \t" + euroDecimalFormat.format( inlegAandelenTotaal ) +
                                            "\nwaarde-inleg:  \t" + euroDecimalFormat.format( waardeMinusInleg ) +
                                            "\nrendement:  \t" + percentDecimalFormat.format( rendement );
                        }

                        logger.info( infoString + "\n" + "insertString: " + insertString + "\n" );

                        JOptionPane.showMessageDialog( frame, infoString,
                                "Insert waarde",
                                JOptionPane.INFORMATION_MESSAGE );

                        // Insert the record in waarde
                        try {
                            final Statement insertStatement = connection.createStatement( );
                            if ( insertStatement.executeUpdate( insertString ) != 1 ) {
                                logger.severe( "Could not insert in waarde" );
                            } else {
                                update = true;
                            }
                        } catch ( SQLException sqlException ) {
                            logger.severe( "SQLException in insertStatement: " + sqlException.getMessage( ) );
                        }
                    }
                } catch ( SQLException sqlException ) {
                    logger.severe( "SQLException in waardeStatement: " + sqlException.getMessage( ) );
                }
            }

            // Get the totaal over all rekening types
            double totaal = 0;
            for ( int rekeningTypeId = 1; rekeningTypeId <= maximumRekeningTypeId; rekeningTypeId++ ) {
                totaal += rekeningTypeTotaal[ rekeningTypeId ];
            }

            // Get existing record in totaal, if present
            try {
                Statement totaalStatement = connection.createStatement( );
                ResultSet totaalResultSet =
                        totaalStatement.executeQuery( "SELECT " + selectQueryTotaalString + " totaal, datum_koersen " +
                                "FROM totaal WHERE datum = '" + waardeDatumString + "'" );

                if ( totaalResultSet.next( ) ) {
                    // Record in totaal for this date exist, so update this record.

                    // Clear the string used for assembling the update query
                    updateString = null;

                    String infoString = "\nUpdate tabel totaal voor datum " + waardeDatumString;

                    // Loop over the rekening types
                    int rekeningTypeId;
                    for ( rekeningTypeId = 1; rekeningTypeId <= maximumRekeningTypeId; rekeningTypeId++ ) {
                        // Compare the calculated totaal for this rekening type with the value from table totaal
                        final double rekeningTypeTotaalTable = totaalResultSet.getDouble( rekeningTypeId );
                        final double rekeningTypeTotaalUpdate = Math.round( 100.0 * rekeningTypeTotaal[ rekeningTypeId ] ) / 100.0;
                        // logger.info( rekeningTypeString[ rekeningTypeId ] + ": table=" + rekeningTypeTotaalTable + ", Update=" + rekeningTypeTotaalUpdate );
                        if ( Math.abs( rekeningTypeTotaalUpdate - rekeningTypeTotaalTable ) > 0.01 ) {
                            // Difference found: update record in table
                            addToUpdateString( rekeningTypeTotaalFieldString[ rekeningTypeId ] +
                                    " = " + rekeningTypeTotaalUpdate );
                            infoString +=
                                    "\n" + rekeningTypeString[ rekeningTypeId ] + "  \t\t" + rekeningTypeTotaalTable +
                                            "  \t" + rekeningTypeTotaalUpdate +
                                            "  \t" + String.valueOf( rekeningTypeTotaalUpdate - rekeningTypeTotaalTable );
                        }
                    }

                    // Compare the value from the totaal record for the overall totaal
                    final double totaalTable = totaalResultSet.getDouble( rekeningTypeId );
                    final double totaalUpdate = Math.round( 100.0 * totaal ) / 100.0;

                    if ( Math.abs( totaalUpdate - totaalTable ) > 0.01 ) {
                        // Difference found: update record in table
                        addToUpdateString( "totaal = " + totaalUpdate );
                        infoString +=
                                "\ntotaal  \t\t" + totaalTable + "  \t" + totaalUpdate +
                                        "  \t" + String.valueOf( totaalUpdate - totaalTable );
                    }

                    // Check if update is necessary
                    if ( updateString == null ) {
                        logger.info( "Geen update nodig in tabel totaal voor datum " + waardeDatumString );

                        // Ready
                        return update;
                    }

                    updateString = "UPDATE totaal SET " + updateString;
                    updateString += " WHERE datum = '" + waardeDatumString + "'";

                    logger.info( infoString + "\n" + "updateString: " + updateString + "\n" );

                    JOptionPane.showMessageDialog( frame, infoString,
                            "Update totaal",
                            JOptionPane.INFORMATION_MESSAGE );

                    // Update the record in totaal
                    try {
                        final Statement updateStatement = connection.createStatement( );
                        if ( updateStatement.executeUpdate( updateString ) != 1 ) {
                            logger.severe( "Could not update in totaal" );
                        } else {
                            update = true;
                        }
                    } catch ( SQLException sqlException ) {
                        logger.severe( "SQLException in updateStatement: " + sqlException.getMessage( ) );
                    }

                } else if ( actionCommandString.equals( updateActionCommandString ) ) {
                    logger.severe( "Could not get record in table totaal for date " + waardeDatumString );

                    // Ready
                    return update;
                } else {
                    // Datum not found, and action command is not update
                    // Insert new record in table totaal

                    String insertString =
                            "INSERT INTO totaal SET datum = '" + waardeDatumString +
                                    "', datum_koersen = '" + koersenDatumString + "'";

                    String infoString =
                            "\nInsert record in tabel totaal voor datum " + waardeDatumString;

                    // Loop over the rekening types
                    int rekeningTypeId;
                    for ( rekeningTypeId = 1; rekeningTypeId <= maximumRekeningTypeId; rekeningTypeId++ ) {
                        // Insert the totaal for this rekening type
                        insertString +=
                                ", " + rekeningTypeTotaalFieldString[ rekeningTypeId ] +
                                        " = " + rekeningTypeTotaal[ rekeningTypeId ];

                        infoString +=
                                "\n" + rekeningTypeString[ rekeningTypeId ] + "  \t" +
                                        euroDecimalFormat.format( rekeningTypeTotaal[ rekeningTypeId ] );
                    }

                    // Insert the value for the overall totaal
                    insertString += ", totaal = " + totaal;

                    infoString += "\ntotaal  \t\t" + euroDecimalFormat.format( totaal );

                    logger.info( infoString + "\n" + "insertString: " + insertString + "\n" );

                    JOptionPane.showMessageDialog( frame, infoString,
                            "Insert totaal",
                            JOptionPane.INFORMATION_MESSAGE );

                    // Insert the record in totaal
                    try {
                        final Statement insertStatement = connection.createStatement( );
                        if ( insertStatement.executeUpdate( insertString ) != 1 ) {
                            logger.severe( "Could not insert in waarde" );
                        } else {
                            update = true;
                        }
                    } catch ( SQLException sqlException ) {
                        logger.severe( "SQLException in insertStatement: " + sqlException.getMessage( ) );
                    }
                }
            } catch ( SQLException sqlException ) {
                logger.severe( "SQLException in totaalStatement: " + sqlException.getMessage( ) );
            }
        } catch ( SQLException sqlException ) {
            logger.severe( "SQLException in rekeningStatement: " + sqlException.getMessage( ) );
        }

        return update;
    }
}
