package financien.gui;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.ResultSet;
import java.sql.Statement;

import java.util.*;
import java.util.logging.*;
import javax.swing.*;

/**
 * ComboBox for selection of rekening type
 */
public class RekeningTypeComboBox extends JComboBox<String> {
    private final Logger logger = Logger.getLogger( RekeningTypeComboBox.class.getCanonicalName() );

    private final Map<String, Integer> rekeningTypeMap = new HashMap<>( 10 );

    public RekeningTypeComboBox( Connection	connection,
				 int		selectedRekeningTypeId ) {

	// Add first empty item to force selection of non-empty item
	addItem( "" );

	if ( !rekeningTypeMap.isEmpty( ) ) {
	    // Remove all items in the rekeningType hash table
	    rekeningTypeMap.clear( );
	}

	try {
	    // Fill the combo box and hash table
	    Statement statement = connection.createStatement( );
	    ResultSet resultSet = statement.executeQuery( "SELECT rekening_type_id, rekening_type FROM rekening_type " +
							  "ORDER BY rekening_type" );

	    while ( resultSet.next( ) ) {
		String rekeningTypeString = resultSet.getString( 2 );

		// Store the rekeningType_id in the map indexed by the rekeningTypeString
		rekeningTypeMap.put( rekeningTypeString, resultSet.getInt( 1 ) );

		// Add the rekeningTypeString to the combo box
		addItem( rekeningTypeString );

		// Check if this is the selected rekeningType
		if ( resultSet.getInt( 1 ) == selectedRekeningTypeId ) {
		    // Select this rekeningType
		    setSelectedItem( rekeningTypeString );
		}
	    }

	    resultSet.close( );
	} catch ( SQLException sqlException ) {
	    logger.severe( "SQLException: " + sqlException.getMessage( ) );
	}

	setMaximumRowCount( 10 );
    }

    public String getSelectedRekeningTypeString( ) {
	return ( String )getSelectedItem( );
    }

    public int getSelectedRekeningTypeId( ) {
	return getRekeningTypeId( ( String )getSelectedItem( ) );
    }

    public int getRekeningTypeId( String rekeningTypeString ) {
	if ( rekeningTypeString == null ) return 0;

	// Check if empty string is selected
	if ( rekeningTypeString.length( ) == 0 ) return 0;

	// Get the rekeningType_id from the map
	if ( rekeningTypeMap.containsKey( rekeningTypeString ) ) {
	    return rekeningTypeMap.get( rekeningTypeString );
	}

	return 0;
    }

    public void setSelectedRekeningTypeId( int rekeningTypeId ) {
	if ( rekeningTypeId == 0 ) {
	    // Select the empty item
	    setSelectedItem( "" );
	    return;
	}

	// Find the value in rekeningTypeMap equal to rekeningTypeId
        for (Map.Entry<String, Integer> rekeningTypeMapEntry : rekeningTypeMap.entrySet( ) ) {
	    if ( rekeningTypeMapEntry.getValue( ) == rekeningTypeId ) {
		// Select this rekeningType
		setSelectedItem( rekeningTypeMapEntry.getKey( ) );
		return;
	    }
	}

	// rekeningTypeId not found
	logger.severe( "RekeningType id " + rekeningTypeId + " not found in rekeningTypeMap" );
	setSelectedItem( "" );
    }
}
