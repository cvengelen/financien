// Class to setup a ComboBox for groep

package financien.gui;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.ResultSet;
import java.sql.Statement;

import java.util.*;
import java.util.logging.*;
import javax.swing.*;

/**
 * ComboBox for selection of groep (related to rubriek)
 */
public class GroepComboBox extends JComboBox<String> {
    private final Logger logger = Logger.getLogger( "financien.gui.GroepComboBox" );

    private final Map<String, Integer> groepMap = new HashMap<>( 20 );
    private Connection connection;

    public GroepComboBox( final Connection connection,
			  final int        selectedGroepId ) {
	this.connection = connection;

	setupGroepComboBox( selectedGroepId );
    }

    public void setupGroepComboBox( final int selectedGroepId ) {

	// Remove all existing combo box iterms
	removeAllItems( );

	// Add first empty item to force selection of non-empty item
	addItem( "" );

	if ( !groepMap.isEmpty( ) ) {
	    // Remove all items in the groep hash table
	    groepMap.clear( );
	}

	try {
	    // Fill the combo box and hash table
	    String groepQueryString = "SELECT groep_id, groep FROM groep";
	    groepQueryString += " ORDER BY groep";

	    final Statement statement = connection.createStatement( );
	    final ResultSet resultSet = statement.executeQuery( groepQueryString );

	    while ( resultSet.next( ) ) {
		String groepString = resultSet.getString( 2 );

		// Store the groep_id in the map indexed by the groepString
		groepMap.put( groepString, resultSet.getInt( 1 ) );

		// Add the groepString to the combo box
		addItem( groepString );

		// Check if this is the selected Deb/Cred
		if ( resultSet.getInt( 1 ) == selectedGroepId ) {
		    // Select this Deb/Cred
		    setSelectedItem( groepString );
		}
	    }
	} catch ( SQLException sqlException ) {
	    logger.severe( "SQLException: " + sqlException.getMessage( ) );
	}

	setMaximumRowCount( 20 );
    }

    public String getSelectedGroepString( ) {
	return ( String )getSelectedItem( );
    }

    public int getSelectedGroepId( ) {
	return getGroepId( ( String )getSelectedItem( ) );
    }

    public int getGroepId( String groepString ) {
	if ( groepString == null ) return 0;

	// Check if empty string is selected
	if ( groepString.length( ) == 0 ) return 0;

	// Get the groep_id from the map
	if ( groepMap.containsKey( groepString ) ) {
	    return groepMap.get( groepString );
	}

	return 0;
    }
}
