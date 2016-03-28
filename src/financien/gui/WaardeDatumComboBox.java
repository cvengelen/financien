// Class to setup a ComboBox for all dates in table waarde

package financien.gui;

import javax.swing.*;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.logging.Logger;

public class WaardeDatumComboBox extends JComboBox< String > {
    final private Logger logger = Logger.getLogger( "financien.gui.WaardeDatumComboBox" );

    Connection connection;

    public WaardeDatumComboBox( final Connection connection,
        final String     selectedWaardeDatumString ) {
        this.connection = connection;

        setupWaardeDatumComboBox( selectedWaardeDatumString );
    }

    public void setupWaardeDatumComboBox( final String selectedWaardeDatumString ) {
	// Remove all existing combo box items
	removeAllItems( );

	// Add first empty item to force selection of non-empty item
	addItem( "" );

	try {
	    // Fill the combo box and hash table
	    final String waardeDatumQueryString = "SELECT DISTINCT datum FROM waarde ORDER BY datum DESC";
	    final Statement statement = connection.createStatement( );
	    final ResultSet resultSet = statement.executeQuery( waardeDatumQueryString );

	    while ( resultSet.next( ) ) {
		String waardeDatumString = resultSet.getString( 1 );

		// Add the waardeDatumString to the combo box
		addItem( waardeDatumString );

		// Check if this is the selected datum
		if ( ( selectedWaardeDatumString != null ) &&
		     ( waardeDatumString.equals( selectedWaardeDatumString ) ) ) {
		    // Select this datum
		    setSelectedItem( waardeDatumString );
		}
	    }
	} catch ( SQLException sqlException ) {
	    logger.severe( "SQLException: " + sqlException.getMessage( ) );
	}

	setMaximumRowCount( 20 );
    }


    public String getSelectedWaardeDatumString( ) {
	return ( String )getSelectedItem( );
    }
}
