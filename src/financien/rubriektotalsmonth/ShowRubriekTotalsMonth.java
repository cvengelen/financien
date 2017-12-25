package financien.rubriektotalsmonth;

import financien.gui.*;
import table.TableSorter;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.event.ChangeEvent;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.DecimalFormat;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.logging.Logger;

/**
 * Frame to show totals per month for a rubriek
 * @author Chris van Engelen
 */
public class ShowRubriekTotalsMonth extends JInternalFrame {
    final private Logger logger = Logger.getLogger( ShowRubriekTotalsMonth.class.getCanonicalName() );

    private final Font dialogFont = new Font( "Dialog", Font.BOLD, 12 );

    private RubriekComboBox rubriekComboBox;
    private int selectedRubriekId = 0;
    private JLabel rubriekOmschrijvingLabel;

    private RekeningHouderComboBox rekeningHouderComboBox;
    private int selectedRekeningHouderId = 1;

    private JSpinner firstYearSpinner;
    private int firstYear;
    private JSpinner firstMonthSpinner;
    private int firstMonth;

    private JSpinner lastYearSpinner;
    private int lastYear;
    private JSpinner lastMonthSpinner;
    private int lastMonth;

    private JLabel sumInLabel = null;
    private JLabel sumOutLabel = null;
    private JLabel sumTotalLabel = null;

    private RubriekTotalsMonthTableModel rubriekTotalsTableModel;
    private TableSorter rubriekTotalsTableSorter;
    private JTable rubriekTotalsTable;
    private final DecimalFormat euroDecimalFormat = new DecimalFormat( "EUR #0.00;EUR -#" );

    private final GregorianCalendar calendar = new GregorianCalendar( );


    public ShowRubriekTotalsMonth( final Connection connection, final JFrame parentFrame, int x, int y ) {
        super("Show rubriek totals per month", true, true, true, true);

	// Get the container for the frame
	final Container container = getContentPane( );
	// container.setBackground( Color.white );

	// Set grid bag layout manager
	container.setLayout( new GridBagLayout( ) );
	GridBagConstraints constraints = new GridBagConstraints( );

	constraints.anchor = GridBagConstraints.WEST;
	constraints.insets = new Insets( 20, 20, 5, 5 );
	constraints.gridx = 0;
	constraints.gridy = 0;

        container.add( new JLabel( "Rekening houder:" ), constraints );

        JPanel rekeningPanel = new JPanel( );

        // Setup a JComboBox with the results of the query on rekening houder
        rekeningHouderComboBox = new RekeningHouderComboBox( connection, 1 );
        rekeningPanel.add( rekeningHouderComboBox );
        rekeningHouderComboBox.addActionListener( ( ActionEvent actionEvent ) -> {
            // Setup the rubriek totals table for the selected rekening houder
            selectedRekeningHouderId = rekeningHouderComboBox.getSelectedRekeningHouderId();
            setupRubriekTotalsTable( );
        });

        constraints.insets = new Insets( 20, 5, 5, 20 );
        constraints.gridx = GridBagConstraints.RELATIVE;
        constraints.gridwidth = 2;
        constraints.anchor = GridBagConstraints.WEST;
        container.add( rekeningPanel, constraints );
        constraints.gridwidth = 1;

        constraints.insets = new Insets( 5, 20, 5, 5 );
        constraints.gridx = 0;
        constraints.gridy = 1;
        constraints.anchor = GridBagConstraints.EAST;
        container.add( new JLabel( "Rubriek:" ), constraints );

        // Setup a JComboBox with the results of the query on rubriek
        rubriekComboBox = new RubriekComboBox( connection, 0, true );

        class RubriekActionListener implements ActionListener {
            public void actionPerformed( ActionEvent actionEvent ) {
                // Get the selected Rubriek ID
                selectedRubriekId = rubriekComboBox.getSelectedRubriekId( );

                // Check if rubriek has been selected
                if ( selectedRubriekId == 0 ) {
                    rubriekOmschrijvingLabel.setText( "" );
                } else {
                    try {
                        Statement statement = connection.createStatement( );
                        ResultSet resultSet = statement.executeQuery( "SELECT omschrijving " +
                                "FROM rubriek WHERE rubriek_id = " +
                                selectedRubriekId );
                        if ( ! resultSet.next( ) ) {
                            logger.severe( "Could not get record for rubriek_id " +
                                    selectedRubriekId + " in rubriek" );
                            return;
                        }
                        rubriekOmschrijvingLabel.setText( resultSet.getString( 1 ) );
                    } catch ( SQLException sqlException ) {
                        JOptionPane.showMessageDialog( parentFrame,
                                sqlException.getMessage( ),
                                "Show rubriek totals per month SQL exception",
                                JOptionPane.ERROR_MESSAGE);
                        logger.severe( "SQLException: " + sqlException.getMessage( ) );
                    }
                }

                // Setup the rubriek totals table for the selected rubriek
                setupRubriekTotalsTable( );
            }
        }
        rubriekComboBox.addActionListener( new RubriekActionListener( ) );

        constraints.insets = new Insets( 5, 5, 5, 20 );
        constraints.gridx = GridBagConstraints.RELATIVE;
        constraints.gridwidth = 2;
        constraints.anchor = GridBagConstraints.WEST;
        container.add( rubriekComboBox, constraints );

        rubriekOmschrijvingLabel = new JLabel( );
        constraints.gridy = 2;
        constraints.gridx = 1;
        container.add( rubriekOmschrijvingLabel, constraints );
        constraints.gridwidth = 1;

	constraints.gridx = 0;
	constraints.gridy = 3;
	constraints.insets = new Insets( 5, 20, 5, 5 );
	container.add( new JLabel( "Start jaar, maand:" ), constraints );

	firstYear = calendar.get( Calendar.YEAR );
	SpinnerNumberModel firstYearSpinnerNumberModel = new SpinnerNumberModel( firstYear, 1990, firstYear + 1, 1 );
	firstYearSpinner = new JSpinner( firstYearSpinnerNumberModel );
	JFormattedTextField firstYearSpinnerTextField = ( ( JSpinner.DefaultEditor )firstYearSpinner.getEditor( ) ).getTextField( );
	if ( firstYearSpinnerTextField != null ) {
            firstYearSpinnerTextField.setColumns( 5 );
            // jaarSpinnerTextField.setBackground( Color.white );
            firstYearSpinnerTextField.setFont( dialogFont );
	}
        firstYearSpinner.addChangeListener( ( ChangeEvent changeEvent ) -> {
            // Setup the rubriek totals table for the selected first year
            firstYear = (Integer)(firstYearSpinner.getValue());
            setupRubriekTotalsTable( );
        });

	constraints.gridx = GridBagConstraints.RELATIVE;
	constraints.insets = new Insets( 5, 5, 5, 5 );
	container.add( firstYearSpinner, constraints );

        firstMonth = getMonth( );
	SpinnerNumberModel firstMonthSpinnerNumberModel = new SpinnerNumberModel( firstMonth, 1, 12, 1 );
	firstMonthSpinner = new JSpinner( firstMonthSpinnerNumberModel );
	JFormattedTextField firstMonthSpinnerTextField = ( ( JSpinner.DefaultEditor )firstMonthSpinner.getEditor( ) ).getTextField( );
	if ( firstMonthSpinnerTextField != null ) {
            firstMonthSpinnerTextField.setColumns( 3 );
            // maandSpinnerTextField.setBackground( Color.white );
            firstMonthSpinnerTextField.setFont( dialogFont );
	}
        firstMonthSpinner.addChangeListener( ( ChangeEvent changeEvent ) -> {
            // Setup the rubriek totals table for the selected first month
            firstMonth = (Integer)(firstMonthSpinner.getValue());
            setupRubriekTotalsTable( );
        });

	constraints.gridx = GridBagConstraints.RELATIVE;
	container.add( firstMonthSpinner, constraints );

        constraints.gridx = 0;
        constraints.gridy = 4;
        constraints.insets = new Insets( 5, 20, 5, 5 );
        container.add( new JLabel( "Laatste jaar, maand:" ), constraints );

        lastYear = firstYear;
        SpinnerNumberModel lastYearSpinnerNumberModel = new SpinnerNumberModel( lastYear, 1990, lastYear + 1, 1 );
        lastYearSpinner = new JSpinner( lastYearSpinnerNumberModel );
        JFormattedTextField lastYearSpinnerTextField = ( ( JSpinner.DefaultEditor )lastYearSpinner.getEditor( ) ).getTextField( );
        if ( lastYearSpinnerTextField != null ) {
            lastYearSpinnerTextField.setColumns( 5 );
            // jaarSpinnerTextField.setBackground( Color.white );
            lastYearSpinnerTextField.setFont( dialogFont );
        }
        lastYearSpinner.addChangeListener( ( ChangeEvent changeEvent ) -> {
            // Setup the rubriek totals table for the selected last year
            lastYear = (Integer)(lastYearSpinner.getValue());
            setupRubriekTotalsTable( );
        });

        constraints.gridx = GridBagConstraints.RELATIVE;
        constraints.insets = new Insets( 5, 5, 5, 5 );
        container.add( lastYearSpinner, constraints );

        lastMonth = firstMonth;
        SpinnerNumberModel lastMonthSpinnerNumberModel = new SpinnerNumberModel( lastMonth, 1, 12, 1 );
        lastMonthSpinner = new JSpinner( lastMonthSpinnerNumberModel );
        JFormattedTextField lastMonthSpinnerTextField = ( ( JSpinner.DefaultEditor )lastMonthSpinner.getEditor( ) ).getTextField( );
        if ( lastMonthSpinnerTextField != null ) {
            lastMonthSpinnerTextField.setColumns( 3 );
            // lastMonthSpinnerTextField.setBackground( Color.white );
            lastMonthSpinnerTextField.setFont( dialogFont );
        }
        lastMonthSpinner.addChangeListener( ( ChangeEvent changeEvent ) -> {
            // Setup the rubriek totals table for the selected last month
            lastMonth = (Integer)(lastMonthSpinner.getValue());
            setupRubriekTotalsTable( );
        });

        constraints.gridx = GridBagConstraints.RELATIVE;
        container.add( lastMonthSpinner, constraints );

        constraints.insets = new Insets( 5, 20, 5, 5 );
        constraints.gridx = 0;
        constraints.gridy = 5;
        constraints.anchor = GridBagConstraints.EAST;
        container.add( new JLabel( "Totalen:" ), constraints );

        JPanel sumPanel = new JPanel( );

        sumPanel.add(new JLabel("in: "));
        sumInLabel = new JLabel("");
        sumPanel.add(sumInLabel);

        sumPanel.add(new JLabel(" uit: "));
        sumOutLabel = new JLabel("");
        sumPanel.add(sumOutLabel);

        sumPanel.add(new JLabel(" totaal: "));
        sumTotalLabel = new JLabel("");
        sumPanel.add(sumTotalLabel);

        constraints.insets = new Insets( 5, 5, 5, 20 );
        constraints.gridx = GridBagConstraints.RELATIVE;
        constraints.anchor = GridBagConstraints.WEST;
        constraints.gridwidth = 2;
        container.add( sumPanel, constraints );
        constraints.gridwidth = 1;

	// Create rubriek totals table from rubriek totals table model
	rubriekTotalsTableModel = new RubriekTotalsMonthTableModel( connection, parentFrame );
	rubriekTotalsTableSorter = new TableSorter( rubriekTotalsTableModel );
	rubriekTotalsTable = new JTable( rubriekTotalsTableSorter );
	// rubriekTotalsTable.setBackground( Color.white );
	rubriekTotalsTableSorter.setTableHeader( rubriekTotalsTable.getTableHeader( ) );
	// rubriekTotalsTableSorter.setSortingStatus( 0, TableSorter.DESCENDING );

	rubriekTotalsTable.setAutoResizeMode( JTable.AUTO_RESIZE_OFF );

	// Set vertical size just enough for 12 entries
	rubriekTotalsTable.setPreferredScrollableViewportSize( new Dimension( 1220, 192 ) );

	// Set renderer for Double objects
	class DoubleRenderer extends JTextField implements TableCellRenderer {
	    public Component getTableCellRendererComponent( JTable table,
							    Object object,
							    boolean isSelected,
							    boolean hasFocus,
							    int row, int column ) {
		switch ( column ) {
		case 1:		// in
		case 2:		// out
		case 3:         // total
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
	rubriekTotalsTable.setDefaultRenderer( Double.class, doubleRenderer );

	final JScrollPane scrollPane = new JScrollPane( rubriekTotalsTable );
	// scrollPane.setBackground( Color.white );
	// scrollPane.getViewport( ).setBackground( Color.white );

	constraints.gridx = 0;
	constraints.gridy = 6;
	constraints.gridwidth = 3;
	constraints.insets = new Insets( 10, 20, 5, 20 );
	// Setting weightx, weighty and fill is necessary for proper filling the frame when resized.
	constraints.weightx = 1d;
	constraints.weighty = 1d;
	constraints.fill = GridBagConstraints.BOTH;
	constraints.anchor = GridBagConstraints.CENTER;
	container.add( scrollPane, constraints );


        // Define the buttons
        final JButton updateButton = new JButton( "Update" );
        final JButton closeButton = new JButton( "Close" );

	class ButtonActionListener implements ActionListener {
	    public void actionPerformed( ActionEvent actionEvent ) {
		if ( actionEvent.getActionCommand( ).equals( "close" ) ) {
		    setVisible( false );
                    dispose();
		} else if ( actionEvent.getActionCommand( ).equals( "update" ) ) {
                    setupRubriekTotalsTable();
		}
	    }
	}
	ButtonActionListener buttonActionListener = new ButtonActionListener( );

	JPanel buttonPanel = new JPanel( );
	// buttonPanel.setBackground( Color.white );

        updateButton.setActionCommand( "update" );
        updateButton.addActionListener( buttonActionListener );
	buttonPanel.add( updateButton );

        closeButton.setActionCommand( "close" );
        closeButton.addActionListener( buttonActionListener );
	buttonPanel.add( closeButton );

	constraints.gridx = 0;
	constraints.gridy = 8;
	constraints.weightx = 0d;
	constraints.weighty = 0d;
	constraints.fill = GridBagConstraints.NONE;
        constraints.insets = new Insets( 5, 20, 20, 20 );
	container.add( buttonPanel, constraints );

	setSize( 600, 600 );
        setLocation( x, y );
	setDefaultCloseOperation( JFrame.DISPOSE_ON_CLOSE );
        getRootPane().setDefaultButton( updateButton );
	setVisible( true );
        rekeningHouderComboBox.requestFocusInWindow( );
    }

    private void setupRubriekTotalsTable( ) {
	// Setup the rubriek totals table for the selected rubriek
	rubriekTotalsTableModel.setupRubriekTotalsTableModel(selectedRubriekId, selectedRekeningHouderId, 0,
                firstYear, firstMonth, lastYear, lastMonth);

	// Setup the tableSorter again so that the TableSorter gets the new table size (# rows)
	rubriekTotalsTableSorter.setTableModel( rubriekTotalsTableModel );

	// Need to setup preferred column width up again
	rubriekTotalsTable.getColumnModel( ).getColumn(  0 ).setPreferredWidth( 70 );  // Month
	rubriekTotalsTable.getColumnModel( ).getColumn(  1 ).setPreferredWidth( 120 );  // in
	rubriekTotalsTable.getColumnModel( ).getColumn(  2 ).setPreferredWidth( 120 );  // out
	rubriekTotalsTable.getColumnModel( ).getColumn(  3 ).setPreferredWidth( 120 );  // total

        sumInLabel.setText( euroDecimalFormat.format(rubriekTotalsTableModel.getSumIn()) );
        sumOutLabel.setText( euroDecimalFormat.format(rubriekTotalsTableModel.getSumOut()) );
        sumTotalLabel.setText( euroDecimalFormat.format(rubriekTotalsTableModel.getSumTotal()) );
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
