package financien.koersen;

/**
 * Get AEX index and koersen
 *
 * @author Chris van Engelen
 */
public class GetAex {
    public static void main( String[ ] args ) {
	final Aex aex = new Aex( );
        System.out.println( "AEX index               : " + aex.getIndex( ) );
        System.out.println( "Robeco EUR G            : " + aex.getKoersRobecoEurG( ) );
        System.out.println( "Robeco Growth Mix EUR G : " + aex.getKoersRobecoGrowthMixEurG( ) );
        System.out.println( "NN Dynamic MixFund IV   : " + aex.getKoersNnDynamicMixFundIv( ) );
        System.out.println( "BlackRock MixFonds 3    : " + aex.getKoersBlackRockMixFonds3( ) );
    }
}
