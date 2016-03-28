// Combobox voor rekening houder

package financien.gui;

import javax.swing.*;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

public class RekeningHouderComboBox extends JComboBox< String > {
    final private static long serialVersionUID = 1L;
    // final private Logger logger = Logger.getLogger( "financien.gui.RekeningHouderComboBox" );
    final private Logger logger = Logger.getLogger( RekeningHouderComboBox.class.getCanonicalName( ) );

    private Map< String, Integer > rekeningHouderMap = new HashMap< >( );

    public RekeningHouderComboBox( Connection connection,
                                   int selectedRekeningHouderId ) {
        // Add first empty item to force selection of non-empty item
        // addItem( "" );

        if ( !rekeningHouderMap.isEmpty( ) ) {
            // Remove all items in the rekeninghouder hash table
            rekeningHouderMap.clear( );
        }

        try {
            // Fill the combo box and hash table
            Statement statement = connection.createStatement( );
            ResultSet resultSet = statement.executeQuery( "SELECT rekening_houder_id, rekening_houder FROM rekening_houder " +
                                                          "ORDER BY rekening_houder" );

            while ( resultSet.next( ) ) {
                String rekeningHouderString = resultSet.getString( 2 );

                // Store the rekening_houder_id in the map indexed by the rekeningHouderString
                rekeningHouderMap.put( rekeningHouderString, resultSet.getInt( 1 ) );

                // Add the rekeningHouderString to the combo box
                addItem( rekeningHouderString );

                // Check if this is the selected rekeninghouder
                if ( resultSet.getInt( 1 ) == selectedRekeningHouderId ) {
                    // Select this rekeninghouder
                    setSelectedItem( rekeningHouderString );
                }
            }

            resultSet.close( );
        } catch ( SQLException sqlException ) {
            logger.severe( "SQLException: " + sqlException.getMessage( ) );
        }

        setMaximumRowCount( 10 );
    }


    public String getSelectedRekeningHouderString( ) {
        return ( String )getSelectedItem( );
    }


    public int getSelectedRekeningHouderId( ) {
        return getRekeningHouderId( ( String )getSelectedItem( ) );
    }


    public int getRekeningHouderId( String rekeningHouderString ) {
        if ( rekeningHouderString == null ) return 0;

        // Check if empty string is selected
        if ( rekeningHouderString.length( ) == 0 ) return 0;

        // Get the rekening_houder_id from the map
        if ( rekeningHouderMap.containsKey( rekeningHouderString ) ) {
            return rekeningHouderMap.get( rekeningHouderString );
        }

        return 0;
    }


    public void setSelectedRekeningHouderId( int rekeningHouderId ) {
        if ( rekeningHouderId == 0 ) {
            // Select the empty item
            setSelectedItem( "" );
            return;
        }

        // Check if the map contains the rekeninghouder ID value
        if ( rekeningHouderMap.containsValue( rekeningHouderId ) ) {
            // Find the value in rekeningHouderMap equal to rekeningHouderId
            for ( Map.Entry< String, Integer > mapEntry : rekeningHouderMap.entrySet( ) ) {
                if ( mapEntry.getValue( ) == rekeningHouderId ) {
                    // Select this rekeninghouder
                    setSelectedItem( mapEntry.getKey() );
                    return;
                }
            }
        }

        // rekeningHouderId not found
        logger.severe( "Rekeninghouder id " + rekeningHouderId + " not found in rekeningHouderMap" );
    }
}
