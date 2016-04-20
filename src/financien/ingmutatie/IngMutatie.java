// Main program to copy downloaded ING mutatie records to rekening_mutatie, and deb_cred

package financien.ingmutatie;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import java.util.logging.*;

public class IngMutatie {
    public static void main( String[ ] args ) {
	final Logger logger = Logger.getLogger( "financien.ingmutatie.IngMutatie" );

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
	    IngMutatieFrame IngMutatieFrame = new IngMutatieFrame( connection );
	} catch ( SQLException sqlException ) {
	    logger.severe( "SQLException: " + sqlException.getMessage( ) );
        }
    }
}
