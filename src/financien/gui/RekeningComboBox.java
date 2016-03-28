// Class to setup a ComboBox for rekening

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

public class RekeningComboBox extends JComboBox {
    final private Logger logger = Logger.getLogger( "financien.gui.RekeningComboBox" );

    private Map rekeningMap = new HashMap( );

    private Connection	connection = null;


    public RekeningComboBox( final Connection	connection,
                             final int		selectedRekeningId,
                             final int          rekeningHouderId,
                             final boolean	activeOnly ) {
        this.connection = connection;

        setupRekeningComboBox( selectedRekeningId, rekeningHouderId, activeOnly );
    }

    public void setupRekeningComboBox( final int	selectedRekeningId,
				       final int	rekeningHouderId,
                                       final boolean	activeOnly ) {
        // Remove all existing items in the boek combo box
        removeAllItems( );

        // Add first empty item to force selection of non-empty item
        addItem( "" );

        if ( !rekeningMap.isEmpty( ) ) {
            // Remove all items in the rekening hash table
            rekeningMap.clear( );
        }

        try {
            // Fill the combo box and hash table
            String rekeningQueryString = "SELECT rekening_id, rekening FROM rekening WHERE rekening_houder_id = " + rekeningHouderId;

            // Check if only active accounts are required
            if ( activeOnly ) {
                rekeningQueryString += " AND aktief != 0";
            }

            // Add order to query
            rekeningQueryString += " ORDER BY rekening";

            final Statement statement = connection.createStatement( );
            final ResultSet resultSet = statement.executeQuery( rekeningQueryString );

            while ( resultSet.next( ) ) {
                String rekeningString = resultSet.getString( 2 );

                // Store the rekening_id in the map indexed by the rekeningString
                rekeningMap.put( rekeningString, resultSet.getObject( 1 ) );

                // Add the rekeningString to the combo box
                addItem( rekeningString );

                // Check if this is the selected rekening
                if ( resultSet.getInt( 1 ) == selectedRekeningId ) {
                    // Select this rekening
                    setSelectedItem( rekeningString );
                }
            }
        } catch ( SQLException sqlException ) {
            logger.severe( "SQLException: " + sqlException.getMessage( ) );
        }

        setMaximumRowCount( 20 );
    }


    public String getSelectedRekeningString( ) {
        return ( String )getSelectedItem( );
    }


    public int getSelectedRekeningId( ) {
        return getRekeningId( ( String )getSelectedItem( ) );
    }


    public int getRekeningId( String rekeningString ) {
        if ( rekeningString == null ) return 0;

        // Check if empty string is selected
        if ( rekeningString.length( ) == 0 ) return 0;

        // Get the rekening_id from the map
        if ( rekeningMap.containsKey( rekeningString ) ) {
            return ( ( Integer )rekeningMap.get( rekeningString ) ).intValue( );
        }

        return 0;
    }
}
