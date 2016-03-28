package financien;

import financien.KoersenUsd;
import java.util.logging.*;

public class testKoersenUsd {
    public static void main( String[ ] args ) {
        final Logger logger = Logger.getLogger( "testKoersenUsd.main" );

	KoersenUsd koersenUsd = new KoersenUsd( );

	System.out.println( "USD/EUR verkoop koers: " + koersenUsd.getKoers( ) );
    }
}
