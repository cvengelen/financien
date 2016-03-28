// Class to setup a ComboBox for rekening_type

package financien.gui;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.ResultSet;
import java.sql.Statement;

import java.util.*;
import java.util.logging.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

public class RekeningTypeComboBox extends JComboBox {
    final private Logger logger = Logger.getLogger( "financien.gui.RekeningTypeComboBox" );

    private Connection connection;

    private Map rekeningTypeMap = new HashMap( );
    private String newRekeningTypeString = null;


    public RekeningTypeComboBox( Connection	connection,
				 int		selectedRekeningTypeId ) {
	this.connection = connection;

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
		rekeningTypeMap.put( rekeningTypeString, resultSet.getObject( 1 ) );

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
	    return ( ( Integer )rekeningTypeMap.get( rekeningTypeString ) ).intValue( );
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
	final Iterator iterator = rekeningTypeMap.entrySet( ).iterator( );
	while ( iterator.hasNext( ) ) {
	    Map.Entry mapEntry = ( Map.Entry )iterator.next( );
	    if ( ( ( Integer )( mapEntry.getValue( ) ) ).intValue( ) == rekeningTypeId ) {
		// Select this rekeningType
		setSelectedItem( mapEntry.getKey( ) );
		return;
	    }
	}

	// rekeningTypeId not found
	logger.severe( "RekeningType id " + rekeningTypeId + " not found in rekeningTypeMap" );
	setSelectedItem( "" );
	return;
    }
}
