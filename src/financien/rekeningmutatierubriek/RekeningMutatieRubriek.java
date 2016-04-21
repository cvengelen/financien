// Main program to show rekening_mutatie records for a specific rubriek

package financien.rekeningmutatierubriek;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import java.util.logging.*;

public class RekeningMutatieRubriek {
    public static void main( String[ ] args ) {
	final Logger logger = Logger.getLogger( RekeningMutatieRubriek.class.getCanonicalName() );

        try {
            // The newInstance() call is a work around for some broken Java implementations
            // Class.forName("com.mysql.jdbc.Driver").newInstance();
            Class.forName( "com.mysql.jdbc.Driver" );
        } catch ( Exception exception ) {
            logger.severe( "Driver registration failed: " + exception );
	    return;
        }

	Connection connection;
	try {
            logger.info( "Opening db connection" );
            connection = DriverManager.getConnection( "jdbc:mysql://localhost/financien?user=cvengelen&password=cve123" );
	    RekeningMutatieRubriekFrame RekeningMutatieRubriekFrame = new RekeningMutatieRubriekFrame( connection );
	} catch ( SQLException sqlException ) {
	    logger.severe( "SQLException: " + sqlException.getMessage( ) );
        }
    }
}
