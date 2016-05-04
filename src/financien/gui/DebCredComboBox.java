package financien.gui;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.ResultSet;
import java.sql.Statement;

import java.util.*;
import java.util.logging.*;

import javax.swing.*;

/**
 * ComboBox for selection of debiteur/crediteur
 */
public class DebCredComboBox extends JComboBox<String> {
    private final Logger logger = Logger.getLogger( DebCredComboBox.class.getCanonicalName() );

    private final Map<String, Integer> debCredMap = new HashMap<>( 1200 );
    private Connection connection;

    public DebCredComboBox( final Connection connection,
			    final int        selectedDebCredId,
			    final String     debCredRekeningString,
			    final boolean    addRekening ) {
	this.connection = connection;

	setupDebCredComboBox( selectedDebCredId, debCredRekeningString, "", addRekening );
    }

    public void setupDebCredComboBox( final int     selectedDebCredId,
				      final String  debCredRekeningString,
				      final String  debCredString,
				      final boolean addRekening ) {
	// Remove all existing combo box iterms
	removeAllItems( );

	// Add first empty item to force selection of non-empty item
	addItem( "" );

	if ( !debCredMap.isEmpty( ) ) {
	    // Remove all items in the debCred hash table
	    debCredMap.clear( );
	}

	try {
	    // Fill the combo box and hash table
	    String debCredQueryString = "SELECT deb_cred_id, rekening, deb_cred FROM deb_cred";

	    if ( ( ( debCredRekeningString != null ) && ( debCredRekeningString.length( ) > 0 ) ) ||
		 ( ( debCredString != null ) && ( debCredString.length( ) > 0 ) ) ) {
		debCredQueryString += " WHERE";

		if ( ( debCredRekeningString != null ) && ( debCredRekeningString.length( ) > 0 ) ) {
		    debCredQueryString += " rekening = '" + debCredRekeningString + "'";
		    // Check if DebCred selection string is also present
		    if ( ( debCredString != null ) && ( debCredString.length( ) > 0 ) ) {
			debCredQueryString += " AND";
		    }
		}

		if ( ( debCredString != null ) && ( debCredString.length( ) > 0 ) ) {
		    debCredQueryString += " deb_cred LIKE '%" + debCredString + "%'";
		}
	    }

	    debCredQueryString += " ORDER BY deb_cred";

	    final Statement statement = connection.createStatement( );
	    final ResultSet resultSet = statement.executeQuery( debCredQueryString );

	    while ( resultSet.next( ) ) {
		String itemString = resultSet.getString( 3 );
		if ( addRekening ) {
		    itemString += " - " + resultSet.getString( 2 );
		}

		// Store the Deb/Cred id in the map indexed by the itemString
		debCredMap.put( itemString, resultSet.getInt( 1 ) );

		// Add the itemString to the combo box
		addItem( itemString );

		// Check if this is the selected Deb/Cred
		if ( resultSet.getInt( 1 ) == selectedDebCredId ) {
		    // Select this Deb/Cred
		    setSelectedItem( itemString );
		}
	    }
	} catch ( SQLException sqlException ) {
	    logger.severe( "SQLException: " + sqlException.getMessage( ) );
	}

	setMaximumRowCount( 20 );
    }

    public String getSelectedDebCredString( ) {
	return ( String )getSelectedItem( );
    }

    public int getSelectedDebCredId( ) {
	return getDebCredId( ( String )getSelectedItem( ) );
    }

    public int getDebCredId( String debCredString ) {
	if ( debCredString == null ) return 0;

	// Check if empty string is selected
	if ( debCredString.length( ) == 0 ) return 0;

	// Get the Deb/Cred id from the map
	if ( debCredMap.containsKey( debCredString ) ) {
	    return debCredMap.get( debCredString );
	}

	return 0;
    }
}
