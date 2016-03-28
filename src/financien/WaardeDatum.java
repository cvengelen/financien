// Main program to inspect table waarde for a selected date

package financien;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import java.util.logging.*;

import financien.gui.WaardeDatumFrame;

public class WaardeDatum {
    public static void main( String[ ] args ) {
	final Logger logger = Logger.getLogger( "financien.WaardeDatum" );

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
	    WaardeDatumFrame WaardeDatumFrame = new WaardeDatumFrame( connection );
	} catch ( SQLException sqlException ) {
	    logger.severe( "SQLException: " + sqlException.getMessage( ) );
        }
    }
}
