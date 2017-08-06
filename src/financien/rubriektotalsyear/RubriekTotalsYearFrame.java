package financien.rubriektotalsyear;

import financien.gui.RekeningHouderComboBox;
import financien.gui.RubriekListModel;
import table.TableSorter;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.sql.Connection;
import java.sql.SQLException;
import java.text.DecimalFormat;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Vector;
import java.util.logging.Logger;

/**
 * Frame to show totals per year for a rubriek
 * An instance of RubriekTotalsYearFrame is created by class financien.Main.
 *
 * @author Chris van Engelen
 */
public class RubriekTotalsYearFrame {
    final private Logger logger = Logger.getLogger( RubriekTotalsYearFrame.class.getCanonicalName() );

    private final JFrame frame = new JFrame( "Rubriek Totals" );
    private final Font dialogFont = new Font( "Dialog", Font.BOLD, 12 );

    private RubriekListModel rubriekListModel;
    private JList rubriekList;
    private Vector<Integer> selectedRubriekIds = new Vector<>(3);
    private JLabel rubriekOmschrijvingLabel;

    private RekeningHouderComboBox rekeningHouderComboBox;
    private int selectedRekeningHouderId = 1;

    private JSpinner firstYearSpinner;
    private int firstYear;

    private JSpinner lastYearSpinner;
    private int lastYear;

    private JLabel sumInLabel = null;
    private JLabel sumOutLabel = null;
    private JLabel sumTotalLabel = null;

    private RubriekTotalsYearTableModel rubriekTotalsTableModel;
    private TableSorter rubriekTotalsTableSorter;
    private JTable rubriekTotalsTable;
    private final DecimalFormat euroDecimalFormat = new DecimalFormat( "EUR #0.00;EUR -#" );

    public RubriekTotalsYearFrame( final Connection connection ) {
	// frame.setBackground( Color.white );

	// Get the container for the frame
	final Container container = frame.getContentPane( );
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

        // Setup a JList for the rubriek using the ListModel for the rubriek
        rubriekListModel = new RubriekListModel( connection );
        rubriekList = new JList<>(rubriekListModel);
        rubriekList.setVisibleRowCount( 3 );
        rubriekList.setLayoutOrientation( JList.VERTICAL );
        JScrollPane rubriekScrollPane = new JScrollPane(rubriekList);

        class RubriekListSelectionListener implements ListSelectionListener {
            public void valueChanged( ListSelectionEvent listSelectionEvent ) {
                selectedRubriekIds.clear();
                String selectedRubriekDescriptions = "";

                // Loop over the selected rubrieked
                for (int rubriekListModelIndex: rubriekList.getSelectedIndices()) {
                    selectedRubriekIds.add(rubriekListModel.getRubriekId( rubriekListModelIndex ));
                    if (!selectedRubriekDescriptions.isEmpty()) selectedRubriekDescriptions += "; ";
                    selectedRubriekDescriptions += rubriekListModel.getRubriekDescription( rubriekListModelIndex );
                }

                rubriekOmschrijvingLabel.setText( selectedRubriekDescriptions );

                // Setup the rubriek totals table for the selected rubriek
                setupRubriekTotalsTable( );
            }
        }
        rubriekList.addListSelectionListener(new RubriekListSelectionListener());

        constraints.insets = new Insets( 5, 5, 5, 20 );
        constraints.gridx = GridBagConstraints.RELATIVE;
        constraints.gridwidth = 2;
        constraints.fill = GridBagConstraints.BOTH;
        constraints.weightx = 1d;
        constraints.weighty = 1d;
        constraints.anchor = GridBagConstraints.WEST;
        container.add( rubriekScrollPane, constraints );
        constraints.fill = GridBagConstraints.NONE;
        constraints.weightx = 0d;
        constraints.weighty = 0d;

        rubriekOmschrijvingLabel = new JLabel( );
        constraints.gridy = 2;
        constraints.gridx = 1;
        container.add( rubriekOmschrijvingLabel, constraints );
        constraints.gridwidth = 1;

	constraints.gridx = 0;
	constraints.gridy = 3;
	constraints.insets = new Insets( 5, 20, 5, 5 );
	container.add( new JLabel( "Start jaar:" ), constraints );

	firstYear = (new GregorianCalendar( )).get( Calendar.YEAR );
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

        constraints.gridx = 0;
        constraints.gridy = 4;
        constraints.insets = new Insets( 5, 20, 5, 5 );
        container.add( new JLabel( "Laatste jaar:" ), constraints );

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
	rubriekTotalsTableModel = new RubriekTotalsYearTableModel( connection);
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
		    frame.setVisible( false );
                    frame.dispose();
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

        // Add a window listener to close the connection when the frame is disposed
        frame.addWindowListener( new WindowAdapter() {
            @Override
            public void windowClosed(WindowEvent e) {
                try {
                    // Close the connection to the MySQL database
                    connection.close( );
                } catch (SQLException sqlException) {
                    logger.severe( "SQL exception closing connection: " + sqlException.getMessage() );
                }
            }
        } );

	frame.setSize( 600, 600 );
	frame.setDefaultCloseOperation( JFrame.DISPOSE_ON_CLOSE );
        frame.getRootPane().setDefaultButton( updateButton );
	frame.setVisible( true );
        boolean focusSet = rekeningHouderComboBox.requestFocusInWindow( );
    }


    private void setupRubriekTotalsTable( ) {
	// Setup the rubriek totals table for the selected rubriek
	rubriekTotalsTableModel.setupRubriekTotalsTableModel(selectedRubriekIds, selectedRekeningHouderId, 0, firstYear, lastYear);

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
}
