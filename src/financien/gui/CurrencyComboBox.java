// Class to setup a ComboBox for currency

package financien.gui;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.ResultSet;
import java.sql.Statement;

import java.util.*;
import java.util.logging.*;
import javax.swing.*;

public class CurrencyComboBox extends JComboBox< String > {
    final private static long serialVersionUID = 1L;
    final private Logger logger = Logger.getLogger( "financien.gui.CurrencyComboBox" );

    private Map< String, Integer > currencyMap = new HashMap< >( );

    public CurrencyComboBox( Connection	connection,
                             int	selectedCurrencyId ) {
        // Add first empty item to force selection of non-empty item
        addItem( "" );

        if ( !currencyMap.isEmpty( ) ) {
            // Remove all items in the currency hash table
            currencyMap.clear( );
        }

        try {
            // Fill the combo box and hash table
            Statement statement = connection.createStatement( );
            ResultSet resultSet = statement.executeQuery( "SELECT currency_id, currency FROM currency " +
                                                          "ORDER BY currency" );

            while ( resultSet.next( ) ) {
                String currencyString = resultSet.getString( 2 );

                // Store the currency_id in the map indexed by the currencyString
                currencyMap.put( currencyString, resultSet.getInt( 1 ) );

                // Add the currencyString to the combo box
                addItem( currencyString );

                // Check if this is the selected currency
                if ( resultSet.getInt( 1 ) == selectedCurrencyId ) {
                    // Select this currency
                    setSelectedItem( currencyString );
                }
            }

            resultSet.close( );
        } catch ( SQLException sqlException ) {
            logger.severe( "SQLException: " + sqlException.getMessage( ) );
        }

        setMaximumRowCount( 10 );
    }


    public String getSelectedCurrencyString( ) {
        return ( String )getSelectedItem( );
    }


    public int getSelectedCurrencyId( ) {
        return getCurrencyId( ( String )getSelectedItem( ) );
    }


    public int getCurrencyId( String currencyString ) {
        if ( currencyString == null ) return 0;

        // Check if empty string is selected
        if ( currencyString.length( ) == 0 ) return 0;

        // Get the currency_id from the map
        if ( currencyMap.containsKey( currencyString ) ) {
            return currencyMap.get( currencyString );
        }

        return 0;
    }


    public void setSelectedCurrencyId( int currencyId ) {
        if ( currencyId == 0 ) {
            // Select the empty item
            setSelectedItem( "" );
            return;
       }

        // Check if the map contains the rekeninghouder ID value
        if ( currencyMap.containsValue( currencyId ) ) {
            // Find the value in currencyMap equal to currencyId
            for (  Map.Entry< String, Integer > mapEntry : currencyMap.entrySet( ) ) {
                if ( mapEntry.getValue( ) == currencyId ) {
                    // Select this currency
                    setSelectedItem( mapEntry.getKey() );
                    return;
                }
            }
        }

        // currencyId not found
        logger.severe( "Currency id " + currencyId + " not found in currencyMap" );
        setSelectedItem( "" );
    }
}
