package financien.koersen;

import java.net.*;
import java.io.*;
import java.util.regex.*;
import java.util.logging.*;

/**
 * Retrieve AEX index and koersen from beursgorilla web site.
 *
 * @author Chris van Engelen
 */
class Aex {
    private final Logger logger = Logger.getLogger( Aex.class.getCanonicalName() );

    float getKoersRobecoEurG( ) {
        return getKoers( "http://www.beursgorilla.nl/beleggingsfonds.asp?uitgebreid=ja&v=&administrator=Robeco+Institutional+Asset+Management+B.&sector=alle&region=alle&assat_allocation=Aandelen&ISIN=alle&country_of_origin=alle&valuta=alle&diamond=alle&listed=alle",
                         "Robeco EUR G" );
    }

    float getKoersRobecoGrowthMixEurG( ) {
        return getKoers( "http://www.beursgorilla.nl/beleggingsfonds.asp?uitgebreid=ja&v=&administrator=Robeco+Fund+Management+BV&sector=alle&region=alle&assat_allocation=Gemengd&ISIN=alle&country_of_origin=alle&valuta=alle&diamond=alle&listed=alle",
                         "Robeco Growth Mix - EUR G");
    }

    float getKoersNnDynamicMixFundIv( ) {
        return getKoers( "http://www.beursgorilla.nl/beleggingsfonds.asp?uitgebreid=ja&v=aanbieder&administrator=NN+Investment+Partners+B.V.&sector=alle&region=alle&assat_allocation=Gemengd&ISIN=alle&country_of_origin=alle&valuta=alle&diamond=alle&listed=alle",
                         "NN Dynamic Mix Fund IV");
    }

    float getKoersBlackRockMixFonds3( ) {
        return getKoers( "http://www.beursgorilla.nl/beleggingsfonds.asp?uitgebreid=ja&v=aanbieder&administrator=BlackRock+Asset+Management+Ireland+Ltd&sector=alle&region=alle&assat_allocation=Gemengd&ISIN=alle&country_of_origin=alle&valuta=alle&diamond=alle&listed=alle",
                         "BUF BlackRock Mix Fds 3 R€");
    }

    private float getKoers( final String urlString, final String fundName  ) {

        URL url = null;
        try {
            url = new URL( urlString );
        }
        catch ( MalformedURLException malformedURLException ) {
            logger.severe( "Malformed URL exception: " + malformedURLException.getLocalizedMessage() );
            return 0;
        }

        final Pattern fundNamePattern = Pattern.compile( ">" + fundName + "<" );
        final Pattern koersPattern = Pattern.compile( ">(\\d+,\\d+)<" );

        try ( InputStreamReader urlInputStreamReader = new InputStreamReader( url.openStream( ) );
              BufferedReader bufferedReader = new BufferedReader(  urlInputStreamReader ) ) {
            String lineString;
            while ( ( lineString = bufferedReader.readLine( ) ) != null ) {
                // Find line with fund name
                Matcher fundNameMatcher = fundNamePattern .matcher( lineString );
                if ( fundNameMatcher.find( ) ) {
                    // Read the next line, which contains the stock rate
                    lineString = bufferedReader.readLine( );
                    Matcher koersMatcher = koersPattern.matcher( lineString );
                    if ( koersMatcher.find( ) ) {
                        int groupCount = koersMatcher.groupCount( ) ;
                        if ( groupCount > 0 ) {
                            return Float.parseFloat( koersMatcher.group( 1 ).replace( ',', '.' ) );
                        }
                        else {
                            logger.severe( "Invalid group count" );
                            return 0;
                        }
                    }
                    else {
                        logger.severe( "Koerswaarde voor " + fundName + " niet gevonden in regel: '" + lineString + "'" );
                        return 0;
                    }
                }
            }
        }
        catch ( IOException ioException ) {
            logger.severe( "IO exception: " + ioException.getLocalizedMessage() );
        }

        logger.severe( "Koers voor " + fundName + " niet gevonden op URL: " + urlString );
        return 0;
    }

    int getIndex( ) {

        URL url = null;
        try {
            url = new URL( "http://www.beursgorilla.nl/aex.asp" );
        }
        catch ( MalformedURLException malformedURLException ) {
            logger.severe( "Malformed URL exception: " + malformedURLException );
            return 0;
        }

	final Pattern aexIndexPattern = Pattern.compile( ">AEX</a> \\(live\\)<" );
	final Pattern indexWaardePattern = Pattern.compile( ">(\\d+,\\d+)<" );

	try ( InputStreamReader urlInputStreamReader = new InputStreamReader( url.openStream( ) );
              BufferedReader bufferedReader = new BufferedReader(  urlInputStreamReader ) ) {
	    String lineString;
	    while ( ( lineString = bufferedReader.readLine( ) ) != null ) {
		Matcher aexIndexMatcher = aexIndexPattern.matcher( lineString );
		if ( aexIndexMatcher.find( ) ) {
                    // Read the next line, which contains the up/down arrow
                    bufferedReader.readLine( );
                    // Read the next line, which contains the stock rate
                    lineString = bufferedReader.readLine( );
		    Matcher koersMatcher = indexWaardePattern.matcher( lineString );
		    if ( koersMatcher.find( ) ) {
			int groupCount = koersMatcher.groupCount( ) ;
			if ( groupCount > 0 ) {
			    return ( new Float( koersMatcher.group( 1 ).replace( ',', '.' ) ) ).intValue( );
			}
			else {
			    logger.severe( "Invalid group count for AEX index" );
			    return 0;
			}
		    }
		    else {
                        logger.severe( "AEX index niet gevonden in regel: '" + lineString + "'" );
			return 0;
		    }
		}
	    }
	}
	catch ( IOException ioException ) {
	    logger.severe( "IO exception: " + ioException );
	}

	logger.severe( "AEX index niet gevonden op URL: " + url.toString() );
	return 0;
    }
}
