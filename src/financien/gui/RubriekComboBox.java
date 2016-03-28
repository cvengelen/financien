// Class to setup a ComboBox for rubriek

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

public class RubriekComboBox extends JComboBox {
    final private Logger logger = Logger.getLogger( "financien.gui.RubriekComboBox" );

    private Connection connection;

    private Map rubriekMap = new HashMap( );
    private String newRubriekString = null;


    public RubriekComboBox( Connection	connection,
			    int		selectedRubriekId,
			    boolean	addRubriekId ) {
	this.connection = connection;

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
		rubriekMap.put( rubriekString, resultSet.getObject( 1 ) );

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
	    return ( ( Integer )rubriekMap.get( rubriekString ) ).intValue( );
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
	final Iterator iterator = rubriekMap.entrySet( ).iterator( );
	while ( iterator.hasNext( ) ) {
	    Map.Entry mapEntry = ( Map.Entry )iterator.next( );
	    if ( ( ( Integer )( mapEntry.getValue( ) ) ).intValue( ) == rubriekId ) {
		// Select this rubriek
		setSelectedItem( mapEntry.getKey( ) );
		return;
	    }
	}

	// rubriekId not found
	logger.severe( "Rubriek id " + rubriekId + " not found in rubriekMap" );
	setSelectedItem( "" );
	return;
    }
}
