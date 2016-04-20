// Main program to copy downloaded Rabobank mutatie records to rekening_mutatie, and deb_cred

package financien.rabobankmutatie;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.logging.Logger;

public class RabobankMutatie {
    public static void main( String[ ] args ) {
	final Logger logger = Logger.getLogger( RabobankMutatie.class.getCanonicalName( ) );

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
	    RabobankMutatieFrame rabobankMutatieFrame = new RabobankMutatieFrame( connection );
	} catch ( SQLException sqlException ) {
	    logger.severe( "SQLException: " + sqlException.getMessage( ) );
        }
    }
}
