package financien.koersen;

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
import javax.swing.event.*;

import table.TableSorter;

/**
 * Frame to show, insert and update records in the koersen table in schema financien.
 * @author Chris van Engelen
 */
public class EditKoersen extends JInternalFrame {
    final private Logger logger = Logger.getLogger( EditKoersen.class.getCanonicalName( ) );

    private KoersenTableModel koersenTableModel;
    private TableSorter koersenTableSorter;
    private static final DecimalFormat koersDecimalFormat = new DecimalFormat( "#0.0000;-#" );
    private static final SimpleDateFormat dateFormat = new SimpleDateFormat( "yyyy-MM-dd" );

    public EditKoersen( final Connection connection, final JFrame parentFrame, int x, int y ) {
        super("Edit koersen", true, true, true, true);

	final Container container = getContentPane( );

	// Set grid bag layout manager
	container.setLayout( new GridBagLayout( ) );
	GridBagConstraints constraints = new GridBagConstraints( );
	constraints.anchor = GridBagConstraints.CENTER;

	// Define the edit, cancel, save and delete buttons
	// These are enabled/disabled by the table model and the list selection listener.
	final JButton editKoersenButton = new JButton( "Edit" );
	final JButton cancelKoersenButton = new JButton( "Cancel" );
	final JButton saveKoersenButton = new JButton( "Save" );
	final JButton deleteKoersenButton = new JButton( "Delete" );

	// Create koersen table from koersen table model
	koersenTableModel = new KoersenTableModel( connection,
						   cancelKoersenButton,
						   saveKoersenButton );
	koersenTableSorter = new TableSorter( koersenTableModel );
	final JTable koersenTable = new JTable( koersenTableSorter );
	koersenTableSorter.setTableHeader( koersenTable.getTableHeader( ) );
	// koersenTableSorter.setSortingStatus( 0, TableSorter.DESCENDING );

	// Setup table column width
	final int fullTableWidth = 800;
	final int datumColumnWidth = 85;
	koersenTable.getColumnModel( ).getColumn( 0 ).setPreferredWidth( datumColumnWidth );
	final int aexColumnWidth = 35;
	koersenTable.getColumnModel( ).getColumn( 1 ).setPreferredWidth( aexColumnWidth );
        final int opmerkingenColumnWidth = 200;
        koersenTable.getColumnModel( ).getColumn( 2 ).setPreferredWidth( opmerkingenColumnWidth );

	final int tableWidth = fullTableWidth - ( datumColumnWidth + aexColumnWidth + opmerkingenColumnWidth );
	final int columnCount = koersenTableModel.getColumnCount( ) - 3;
	final int columnWidth = tableWidth / columnCount;
	for ( int column = 3; column < 3 + columnCount; column++ ) {
	    koersenTable.getColumnModel( ).getColumn( column ).setPreferredWidth( columnWidth );
	}

	koersenTable.setSelectionMode( ListSelectionModel.SINGLE_SELECTION );
	koersenTable.setAutoResizeMode( JTable.AUTO_RESIZE_OFF );

	// Set vertical size just enough for 20 entries
	koersenTable.setPreferredScrollableViewportSize( new Dimension( fullTableWidth, 320 ) );

	// Set renderer for Double objects
	class DoubleRenderer extends JTextField implements TableCellRenderer {
	    private static final long serialVersionUID = 1L;

	    public Component getTableCellRendererComponent( JTable table,
							    Object object,
							    boolean isSelected,
							    boolean hasFocus,
							    int row, int column ) {
		final double koers = ( Double )object;
		if ( koers == 0 ) {
		    // Return empty string
		    this.setText( "" );
		} else {
		    // Use the formatter
		    this.setText( koersDecimalFormat.format( koers ) );
		}
		return this;
	    }
	}
	DoubleRenderer doubleRenderer = new DoubleRenderer( );
	doubleRenderer.setHorizontalAlignment( JTextField.RIGHT );
	final Border emptyBorder = BorderFactory.createEmptyBorder( );
	doubleRenderer.setBorder( emptyBorder );
	koersenTable.setDefaultRenderer( Double.class, doubleRenderer );


	constraints.gridx = 0;
	constraints.gridy = 0;
	constraints.gridwidth = 1;
	constraints.insets = new Insets( 20, 20, 5, 20 );
	constraints.anchor = GridBagConstraints.CENTER;

        // Setting weightx, weighty and fill is necessary for proper filling the frame when resized.
        constraints.weightx = 1d;
        constraints.weighty = 1d;
        constraints.fill = GridBagConstraints.BOTH;

        container.add( new JScrollPane( koersenTable ), constraints );


	////////////////////////////////////////////////
	// List selection listener
	////////////////////////////////////////////////

	// Get the selection model related to the koersen table
	final ListSelectionModel mutatieListSelectionModel = koersenTable.getSelectionModel( );

	class MutatieListSelectionListener implements ListSelectionListener {
	    private int selectedRow = -1;

	    public void valueChanged( ListSelectionEvent listSelectionEvent ) {
		// Ignore extra messages.
		if ( listSelectionEvent.getValueIsAdjusting( ) ) return;

		// Check if current row has modified values
		if ( koersenTableModel.getRowModified( ) ) {
		    if ( selectedRow == -1 ) {
			logger.severe( "Invalid selected row" );
		    } else {
			int result =
			    JOptionPane.showConfirmDialog( parentFrame,
							   "Data zijn gewijzigd: modificaties opslaan?",
							   "Record is gewijzigd",
							   JOptionPane.YES_NO_OPTION,
							   JOptionPane.QUESTION_MESSAGE,
							   null );

			if ( result == JOptionPane.YES_OPTION ) {
			    // Save the changes in the table model, and in the database
			    koersenTableModel.saveEditRow( selectedRow );
			} else {
			    // Cancel any edits in the selected row
			    koersenTableModel.cancelEditRow( selectedRow );
			}
		    }
		}

		// Ignore if nothing is selected
		if ( mutatieListSelectionModel.isSelectionEmpty( ) ) {
		    selectedRow = -1;
		    editKoersenButton.setEnabled( false );
		    cancelKoersenButton.setEnabled( false );
		    saveKoersenButton.setEnabled( false );
		    deleteKoersenButton.setEnabled( false );
		    return;
		}

		// Remove the capability to edit the row
		koersenTableModel.unsetEditRow( );

		int viewRow = mutatieListSelectionModel.getMinSelectionIndex( );
		selectedRow = koersenTableSorter.modelIndex( viewRow );


		// Enable the edit button
		editKoersenButton.setEnabled( true );

		// Disable the cancel and save buttons (these will be enabled
		// when any data in the row is actually modified)
		cancelKoersenButton.setEnabled( false );
		saveKoersenButton.setEnabled( false );

		// Enable the delete button
		deleteKoersenButton.setEnabled( true );
	    }

	    private int getSelectedRow ( ) { return selectedRow; }
	}

	// Add mutatieListSelectionListener object to the selection model of the musici table
	final MutatieListSelectionListener mutatieListSelectionListener = new MutatieListSelectionListener( );
	mutatieListSelectionModel.addListSelectionListener( mutatieListSelectionListener );


	////////////////////////////////////////////////
	// Insert, Edit, Cancel, Save, Delete, Close Buttons
	////////////////////////////////////////////////

	// Class to handle button actions: uses mutatieListSelectionListener
	class ButtonActionListener implements ActionListener {
	    public void actionPerformed( ActionEvent actionEvent ) {
		if ( actionEvent.getActionCommand( ).equals( "close" ) ) {
		    setVisible( false );
                    dispose();
		    return;
		} else if ( actionEvent.getActionCommand( ).equals( "insert" ) ) {
		    final Aex aex = new Aex( );

		    final GregorianCalendar calendar = new GregorianCalendar( );
		    final Date todayDate = calendar.getTime( );
		    final String insertDatumString = dateFormat.format( todayDate );
		    String insertString = "INSERT INTO koersen SET datum = '" + insertDatumString +
                                          "', aex_index = " + aex.getIndex( ) +
                                          ", ing_dynamic_mix_fund_iv = " + aex.getKoersNnDynamicMixFundIv() +
                                          ", rabo_rendemix = " + aex.getKoersBlackRockMixFonds3() +
                                          ", robeco_robeco = " + aex.getKoersRobecoEurG() +
                                          ", robeco_growth_mix = " + aex.getKoersRobecoGrowthMixEurG();
		    logger.info( "insertString: " + insertString );
		    try {
			Statement statement = connection.createStatement( );
			if ( statement.executeUpdate( insertString ) != 1 ) {
			    String errorString = ( "Could not insert record with\n" +
						   "datum       = " + insertDatumString +
						   " in koersen" );
			    JOptionPane.showMessageDialog( parentFrame,
							   errorString,
							   "Insert koersen record",
							   JOptionPane.ERROR_MESSAGE);
			    logger.severe( errorString );
			    return;
			}
		    } catch ( SQLException sqlException ) {
                        JOptionPane.showMessageDialog( parentFrame,
                                sqlException.getMessage( ),
                                "Insert koersen record exception",
                                JOptionPane.ERROR_MESSAGE);
			logger.severe( "SQLException: " + sqlException.getMessage( ) );
			return;
		    }

		    // Records may have been modified: setup the table model again
		    koersenTableModel.setupKoersenTableModel( );
		} else {
		    int selectedRow = mutatieListSelectionListener.getSelectedRow( );
		    if ( selectedRow < 0 ) {
			JOptionPane.showMessageDialog( parentFrame,
						       "Geen mutatie geselecteerd",
						       "Edit koersen error",
						       JOptionPane.ERROR_MESSAGE );
			return;
		    }

		    if ( actionEvent.getActionCommand( ).equals( "delete" ) ) {
			String datumString = koersenTableModel.getDatumString( selectedRow );

			// Check if datum is used in table totaal
			try {
			    Statement statement = connection.createStatement( );
			    String totaalQueryString =
				"SELECT datum_koersen FROM totaal WHERE datum_koersen = '" +
				datumString + "'";
			    ResultSet resultSet = statement.executeQuery( totaalQueryString );
			    if ( resultSet.next( ) ) {
				JOptionPane.showMessageDialog( parentFrame,
							       "Tabel totaal gebruikt datum " +
							        datumString,
							       "Edit koersen error",
							       JOptionPane.ERROR_MESSAGE );
				return;
			    }
			} catch ( SQLException sqlException ) {
                            JOptionPane.showMessageDialog( parentFrame,
                                    sqlException.getMessage( ),
                                    "Edit koersen SQL exception",
                                    JOptionPane.ERROR_MESSAGE);
			    logger.severe( "SQLException: " + sqlException.getMessage( ) );
			    return;
			}

			int result =
			    JOptionPane.showConfirmDialog( parentFrame,
							   "Delete record for datum " +
							   datumString + " in koersen ?",
							   "Delete koersen record",
							   JOptionPane.YES_NO_OPTION,
							   JOptionPane.QUESTION_MESSAGE,
							   null );

			if ( result != JOptionPane.YES_OPTION ) return;

			String deleteString  = "DELETE FROM koersen";
			deleteString += " WHERE datum = '" + datumString + "'";
			logger.info( "deleteString: " + deleteString );

			try {
			    Statement statement = connection.createStatement( );
			    int nUpdate = statement.executeUpdate( deleteString );
			    if ( nUpdate != 1 ) {
				String errorString = ( "Could not delete record with\n" +
						       "datum       = " + datumString +
						       " in koersen" );
				JOptionPane.showMessageDialog( parentFrame,
							       errorString,
							       "Delete koersen record",
							       JOptionPane.ERROR_MESSAGE);
				logger.severe( errorString );
				return;
			    }
			} catch ( SQLException sqlException ) {
                            JOptionPane.showMessageDialog( parentFrame,
                                    sqlException.getMessage( ),
                                    "Delete koersen record SQL exception",
                                    JOptionPane.ERROR_MESSAGE);
			    logger.severe( "SQLException: " + sqlException.getMessage( ) );
			    return;
			}

			// Records may have been modified: setup the table model again
			koersenTableModel.setupKoersenTableModel( );
		    } else if ( actionEvent.getActionCommand( ).equals( "edit" ) ) {
			// Allow to edit the selected row
			koersenTableModel.setEditRow( selectedRow );

			// Disable the edit button
			editKoersenButton.setEnabled( false );
		    } else if ( actionEvent.getActionCommand( ).equals( "cancel" ) ) {
			// Cancel any edits in the selected row
			koersenTableModel.cancelEditRow( selectedRow );

			// Remove the capability to edit the row
			koersenTableModel.unsetEditRow( );

			// Enable the edit button, so that the user can select edit again
			editKoersenButton.setEnabled( true );

			// Disable the cancel and save buttons
			cancelKoersenButton.setEnabled( false );
			saveKoersenButton.setEnabled( false );
		    } else if ( actionEvent.getActionCommand( ).equals( "save" ) ) {
			// Save the changes in the table model, and in the database
			koersenTableModel.saveEditRow( selectedRow );

			// Remove the capability to edit the row
			koersenTableModel.unsetEditRow( );

			// Enable the edit button, so that the user can select edit again
			editKoersenButton.setEnabled( true );

			// Disable the cancel and save buttons
			cancelKoersenButton.setEnabled( false );
			saveKoersenButton.setEnabled( false );
		    }
		}
	    }
	}
	final ButtonActionListener buttonActionListener = new ButtonActionListener( );

	JPanel buttonPanel = new JPanel( );

	final JButton insertKoersenButton = new JButton( "Insert" );
	insertKoersenButton.setActionCommand( "insert" );
	insertKoersenButton.addActionListener( buttonActionListener );
	buttonPanel.add( insertKoersenButton );

	editKoersenButton.setActionCommand( "edit" );
	editKoersenButton.setEnabled( false );
	editKoersenButton.addActionListener( buttonActionListener );
	buttonPanel.add( editKoersenButton );

	cancelKoersenButton.setActionCommand( "cancel" );
	cancelKoersenButton.setEnabled( false );
	cancelKoersenButton.addActionListener( buttonActionListener );
	buttonPanel.add( cancelKoersenButton );

	saveKoersenButton.setActionCommand( "save" );
	saveKoersenButton.setEnabled( false );
	saveKoersenButton.addActionListener( buttonActionListener );
	buttonPanel.add( saveKoersenButton );

	deleteKoersenButton.setActionCommand( "delete" );
	deleteKoersenButton.setEnabled( false );
	deleteKoersenButton.addActionListener( buttonActionListener );
	buttonPanel.add( deleteKoersenButton );

	final JButton closeButton = new JButton( "Close" );
	closeButton.addActionListener( buttonActionListener );
	closeButton.setActionCommand( "close" );
	buttonPanel.add( closeButton );

	constraints.gridx = 0;
	constraints.gridy = 1;
        constraints.insets = new Insets( 5, 20, 20, 20 );
        constraints.anchor = GridBagConstraints.CENTER;
        constraints.weightx = 0d;
        constraints.weighty = 0d;
        constraints.fill = GridBagConstraints.NONE;
	container.add( buttonPanel, constraints );

	setSize( fullTableWidth + 60, 500 );
        setLocation( x, y );
	setDefaultCloseOperation( JFrame.DISPOSE_ON_CLOSE );
	setVisible( true );
    }
}
