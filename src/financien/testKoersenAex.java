package financien;

import financien.KoersenAex;
import java.util.logging.*;

public class testKoersenAex {
    public static void main( String[ ] args ) {
        final Logger logger = Logger.getLogger( "testKoersenAex.main" );

	KoersenAex koersenAex = new KoersenAex( );

	System.out.println( "AEX index             : " + koersenAex.getIndex( ) );
	System.out.println( "Ahold                 : " + koersenAex.getAhold( ) );
	System.out.println( "KPN                   : " + koersenAex.getKpn( ) );
	System.out.println( "TNT                   : " + koersenAex.getTnt( ) );
	System.out.println( "OHRA Aandelen fonds   : " + koersenAex.getOhraAandelenFonds( ) );
	System.out.println( "OHRA Techn Trend fonds: " + koersenAex.getOhraTechnTrendFonds( ) );
    }
}
