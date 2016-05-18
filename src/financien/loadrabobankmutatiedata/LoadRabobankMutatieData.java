package financien.loadrabobankmutatiedata;

import javax.swing.SwingUtilities;
import java.lang.reflect.InvocationTargetException;

// Main program to load downloaded Rabobank mutatie data into the financien database
public class LoadRabobankMutatieData {
    public static void main( String[ ] args ) {
        try {
            // See: http://docs.oracle.com/javase/tutorial/uiswing/concurrency/initial.html
            SwingUtilities.invokeAndWait( new LoadRabobankMutatieDataFrame( ) );
        } catch ( InterruptedException interruptedException ) {
            System.err.println( interruptedException.getLocalizedMessage( ) );
        } catch ( InvocationTargetException invocationTargetException ) {
            System.err.println( invocationTargetException.getLocalizedMessage( ) );
        }
    }
}
