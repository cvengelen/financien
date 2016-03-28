package financien;

import financien.KoersenPostbank;
import java.util.logging.*;

public class testKoersenPostbank {
    public static void main( String[ ] args ) {
        final Logger logger = Logger.getLogger( "testKoersenPostbank.main" );

	KoersenPostbank koersenPostbank = new KoersenPostbank( );

	System.out.println( "AEX   clicker: " + koersenPostbank.getAexClicker( ) );
	System.out.println( "China clicker: " + koersenPostbank.getChinaClicker( ) );
    }
}
