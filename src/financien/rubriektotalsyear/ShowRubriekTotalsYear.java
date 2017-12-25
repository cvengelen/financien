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
import java.sql.Connection;
import java.text.DecimalFormat;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Vector;
import java.util.logging.Logger;

/**
 * Frame to show totals per year for a rubriek
 * @author Chris van Engelen
 */
public class ShowRubriekTotalsYear extends JInternalFrame {
    final private Logger logger = Logger.getLogger( ShowRubriekTotalsYear.class.getCanonicalName() );

    private final Font dialogFont = new Font( "Dialog", Font.BOLD, 12 );

    private RubriekListModel rubriekListModel;
    private JList rubriekList;
    private Vector<Integer> selectedRubriekIds = new Vector<>(10);
    private Vector<String> selectedRubrieken = new Vector<>(10);

    private RekeningHouderComboBox rekeningHouderComboBox;
    private int selectedRekeningHouderId = 1;

    private JSpinner firstYearSpinner;
    private int firstYear;

    private JSpinner lastYearSpinner;
    private int lastYear;

    private RubriekTotalsYearTableModel rubriekTotalsTableModel;
    private TableSorter rubriekTotalsTableSorter;
    private JTable rubriekTotalsTable;
    private final DecimalFormat decimalFormat = new DecimalFormat( "#0.00;-#" );

    public ShowRubriekTotalsYear( final Connection connection, final JFrame parentFrame, int x, int y ) {
        super("Show rubriek totals per year", true, true, true, true);

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

        // Setup a JList for the rubriek using the ListModel for the rubriek
        rubriekListModel = new RubriekListModel( connection );
        rubriekList = new JList<>(rubriekListModel);
        rubriekList.setVisibleRowCount( 3 );
        rubriekList.setLayoutOrientation( JList.VERTICAL );
        JScrollPane rubriekScrollPane = new JScrollPane(rubriekList);

        class RubriekListSelectionListener implements ListSelectionListener {
            public void valueChanged( ListSelectionEvent listSelectionEvent ) {
                selectedRubriekIds.clear();
                selectedRubrieken.clear();

                // Loop over the selected rubrieken
                for (int rubriekListModelIndex: rubriekList.getSelectedIndices()) {
                    selectedRubriekIds.add(rubriekListModel.getRubriekId( rubriekListModelIndex ));
                    selectedRubrieken.add(rubriekListModel.getRubriek ( rubriekListModelIndex ));
                }

                // Setup the rubriek totals table for the selected rubriek
                setupRubriekTotalsTable( );
            }
        }
        rubriekList.addListSelectionListener(new RubriekListSelectionListener());

        constraints.insets = new Insets( 5, 5, 5, 250 );
        constraints.gridx = GridBagConstraints.RELATIVE;
        constraints.gridwidth = 2;
        constraints.fill = GridBagConstraints.BOTH;
        constraints.weightx = 0d;
        constraints.weighty = 1.5d;
        constraints.anchor = GridBagConstraints.WEST;
        container.add( rubriekScrollPane, constraints );
        constraints.fill = GridBagConstraints.NONE;
        constraints.weightx = 0d;
        constraints.weighty = 0d;

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

        // Create rubriek totals table from rubriek totals table model
        rubriekTotalsTableModel = new RubriekTotalsYearTableModel( connection, parentFrame );
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

                // Check the column
                if (column == 0) {
                    logger.severe( "Unexpected column: " + column );
                    this.setText( object.toString( ) );
                }
                else {
                    final double total = ( Double )object;
                    if ( total == 0 ) {
                        // Return empty string
                        this.setText( "" );
                    } else {
                        // Use the formatter defined for EUR
                        this.setText( decimalFormat.format( total ) );
                    }
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

        setSize( 700, 800 );
        setLocation( x, y );
        setDefaultCloseOperation( JFrame.DISPOSE_ON_CLOSE );
        getRootPane().setDefaultButton( updateButton );
        setVisible( true );
        rekeningHouderComboBox.requestFocusInWindow( );
    }

    private void setupRubriekTotalsTable( ) {
        // Setup the rubriek totals table for the selected rubriek
        rubriekTotalsTableModel.setupRubriekTotalsTableModel(selectedRubriekIds, selectedRubrieken,
                                                             selectedRekeningHouderId, 0, firstYear, lastYear);

        // Setup the tableSorter again so that the TableSorter gets the new table size (# rows)
        rubriekTotalsTableSorter.setTableModel( rubriekTotalsTableModel );

        // Set the preferred column width for the Year
        rubriekTotalsTable.getColumnModel( ).getColumn(  0 ).setPreferredWidth( 70 );

        // Loop over columns 1 .. end
        for (int column = 1; column < rubriekTotalsTableModel.getColumnCount(); column++) {
            // Set the preferred column width for the rubriek total
            rubriekTotalsTable.getColumnModel().getColumn( column ).setPreferredWidth( 80 );
        }
    }
}
