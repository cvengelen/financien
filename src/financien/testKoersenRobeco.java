package financien;

import financien.KoersenRobeco;
import java.util.logging.*;

public class testKoersenRobeco {
    public static void main( String[ ] args ) {
        final Logger logger = Logger.getLogger( "testKoersenRobeco.main" );

	KoersenRobeco koersenRobeco = new KoersenRobeco( );

	System.out.println( "Balanced mix: " + koersenRobeco.getBalancedMix( ) );
    }
}
