package financien.gui;

import javax.swing.*;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Vector;
import java.util.logging.Logger;

/**
 * ListModel for selection of rubriek
 */
public class RubriekListModel extends AbstractListModel<String> {
    private final Logger logger = Logger.getLogger( RubriekListModel.class.getCanonicalName() );

    private class RubriekRecord {
        int m_rubriekId;
        String m_rubriek;
        String m_rubriekDescription;

        RubriekRecord( int rubriekId, String rubriek, String rubriekDescription ) {
            m_rubriekId = rubriekId;
            m_rubriek = rubriek;
            m_rubriekDescription = rubriekDescription;
        }

        RubriekRecord( RubriekRecord rubriekRecord ) {
            m_rubriekId = rubriekRecord.m_rubriekId;
            m_rubriek = rubriekRecord.m_rubriek;
            m_rubriekDescription = rubriekRecord.m_rubriekDescription;
        }
    }

    private final Vector< RubriekRecord > rubriekVector = new Vector<>( 300 );

    public RubriekListModel( Connection connection ) {

        if ( !rubriekVector.isEmpty() ) {
            // Remove all items in the rubriek vector
            rubriekVector.clear();
        }

        try {
            // Fill the vector with rubriek records
            Statement statement = connection.createStatement();
            ResultSet resultSet = statement.executeQuery( "SELECT rubriek_id, rubriek, omschrijving FROM rubriek " +
                    "ORDER BY rubriek" );

            int index = 0;
            while ( resultSet.next() ) {
                rubriekVector.add( index++, new RubriekRecord( resultSet.getInt( 1 ), resultSet.getString( 2 ), resultSet.getString( 3 ) ) );
            }
        } catch ( SQLException sqlException ) {
            logger.severe( "SQLException: " + sqlException.getMessage() );
        }
    }

    public int getSize() {
        return rubriekVector.size();
    }

    public String getElementAt( int index ) {
        return rubriekVector.get( index ).m_rubriek;
    }

    public int getRubriekId(int index) {
        return rubriekVector.get( index ).m_rubriekId;
    }

    public String getRubriek(int index) {
        return rubriekVector.get( index ).m_rubriek;
    }

    public String getRubriekDescription( int index ) {
        return rubriekVector.get( index ).m_rubriekDescription;
    }
}
