package financien;

import financien.gui.PasswordPanel;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.logging.Logger;

/**
 * Main program for schema financien.
 * The first and only argument must specify the fully-qualified name of the class which should be started,
 * e.g.: java -cp ...  financien.Main financien.rekeningmutaties.EditRekeningMutaties
 *
 * Created by cvengelen on 25-04-16.
 */
public class Main {
    private final static Logger logger = Logger.getLogger( financien.Main.class.getCanonicalName() );

    public static void main( String[ ] args ) {
        if (args.length == 0 || args[0].length() == 0) {
            logger.severe("No class");
            System.err.println("Geef de naam van de class die gestart moet worden, bijvoorbeeld: financien.rekeningmutaties.EditRekeningMutaties");
            System.exit(1);
        }
        final String financienClassName = args[0];
        logger.info( "Starting " + financienClassName );

        try {
            // Load the MySQL JDBC driver
            Class.forName( "com.mysql.jdbc.Driver" );
        } catch ( ClassNotFoundException classNotFoundException ) {
            logger.severe( "ClassNotFoundException: " + classNotFoundException.getMessage() );
            System.exit( 1 );
        }

        try {
            // Get the password for the financien account, which gives access to schema financien.
            final PasswordPanel passwordPanel = new PasswordPanel();
            final String password = passwordPanel.getPassword();
            if (password == null) {
                logger.info("No password");
                System.err.println("Geen password gegeven");
                System.exit( 1 );
            }

            // Find the constructor of the class with name financienClassName which has a Connection as parameter
            // See: https://docs.oracle.com/javase/tutorial/reflect/class/classNew.html
            // and: http://tutorials.jenkov.com/java-reflection/constructors.html
            final Constructor constructor = Class.forName(financienClassName).getConstructor( Connection.class );

            // Open a connection to the financien schema of the MySQL database, and create the frame with the MySQL connection as parameter.
            // No need to save a reference of the instance: when the frame is finished, the application is finished.
            constructor.newInstance( DriverManager.getConnection( "jdbc:mysql://localhost/financien?user=financien&password=" + password ) );
        } catch ( ClassNotFoundException classNotFoundException ) {
            logger.severe( "ClassNotFoundException: " + classNotFoundException.getMessage( ) );
            System.err.println("Class " + financienClassName + " bestaat niet.\nControleer de naam van de class, bijvoorbeeld: financien.rekeningmutaties.EditRekeningMutaties");
            System.exit( 1 );
        } catch ( NoSuchMethodException noSuchMethodException ) {
            logger.severe( "NoSuchMethodException: " + noSuchMethodException.getMessage( ) );
            System.err.println("Class " + financienClassName + " bestaat niet.\nControleer de naam van de class, bijvoorbeeld: financien.rekeningmutaties.EditRekeningMutaties");
            System.exit( 1 );
        } catch ( InstantiationException instantiationException ) {
            logger.severe( "InstantiationException: " + instantiationException.getMessage( ) );
            System.exit( 1 );
        } catch ( IllegalAccessException illegalAccessException ) {
            logger.severe( "IllegalAccessException: " + illegalAccessException.getMessage( ) );
            System.exit( 1 );
        } catch ( InvocationTargetException invocationTargetException) {
            logger.severe( "InvocationTargetException: " + invocationTargetException.getMessage( ) );
            System.exit( 1 );
        } catch ( SQLException sqlException ) {
            logger.severe( "SQLException: " + sqlException.getMessage( ) );
            System.exit( 1 );
        } catch ( Exception exception ) {
            logger.severe( "Exception: " + exception.getMessage( ) );
            System.exit( 1 );
        }
    }
}
