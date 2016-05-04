package financien.gui;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.ResultSet;
import java.sql.Statement;

import java.util.*;
import java.util.logging.*;
import javax.swing.*;

/**
 * ComboBox for selection of rubriek
 */
public class RubriekComboBox extends JComboBox<String> {
    private final Logger logger = Logger.getLogger( RubriekComboBox.class.getCanonicalName() );

    private final Map<String, Integer> rubriekMap = new HashMap<>( 300 );

    public RubriekComboBox( Connection	connection,
			    int		selectedRubriekId,
			    boolean	addRubriekId ) {

	// Add first empty item to force selection of non-empty item
	addItem( "" );

	if ( !rubriekMap.isEmpty( ) ) {
	    // Remove all items in the rubriek hash table
	    rubriekMap.clear( );
	}

	try {
	    // Fill the combo box and hash table
	    Statement statement = connection.createStatement( );
	    ResultSet resultSet = statement.executeQuery( "SELECT rubriek_id, rubriek FROM rubriek " +
							  "ORDER BY rubriek" );

	    while ( resultSet.next( ) ) {
		String rubriekString = resultSet.getString( 2 );
		if ( addRubriekId ) {
		    rubriekString += " (" + resultSet.getInt( 1 ) + ")";
		}

		// Store the rubriek_id in the map indexed by the rubriekString
		rubriekMap.put( rubriekString, resultSet.getInt( 1 ) );

		// Add the rubriekString to the combo box
		addItem( rubriekString );

		// Check if this is the selected rubriek
		if ( resultSet.getInt( 1 ) == selectedRubriekId ) {
		    // Select this rubriek
		    setSelectedItem( rubriekString );
		}
	    }
	} catch ( SQLException sqlException ) {
	    logger.severe( "SQLException: " + sqlException.getMessage( ) );
	}

	setMaximumRowCount( 20 );
    }

    public String getSelectedRubriekString( ) {
	return ( String )getSelectedItem( );
    }

    public int getSelectedRubriekId( ) {
	return getRubriekId( ( String )getSelectedItem( ) );
    }

    public int getRubriekId( String rubriekString ) {
	if ( rubriekString == null ) return 0;

	// Check if empty string is selected
	if ( rubriekString.length( ) == 0 ) return 0;

	// Get the rubriek_id from the map
	if ( rubriekMap.containsKey( rubriekString ) ) {
	    return rubriekMap.get( rubriekString );
	}

	return 0;
    }

    public void setSelectedRubriekId( int rubriekId ) {
	if ( rubriekId == 0 ) {
	    // Select the empty item
	    setSelectedItem( "" );
	    return;
	}

	// Find the value in rubriekMap equal to rubriekId
        for (Map.Entry<String, Integer> rubriekMapEntry : rubriekMap.entrySet( ) ) {
            if ( rubriekMapEntry.getValue( ) == rubriekId ) {
                // Select this rekeningType
                setSelectedItem( rubriekMapEntry.getKey( ) );
                return;
            }
        }

	// rubriekId not found
	logger.severe( "Rubriek id " + rubriekId + " not found in rubriekMap" );
	setSelectedItem( "" );
    }
}
