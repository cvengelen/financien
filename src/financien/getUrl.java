package financien;

import java.net.*;
import java.io.*;
import java.util.regex.*;
import java.util.logging.*;

public class getUrl {
    static URL url = null;

    public static void main( String[ ] args ) {
        final Logger logger = Logger.getLogger( "getUrl.main" );

	try {
	    url = new URL( args[ 0 ] );
	}
	catch ( MalformedURLException malformedURLException ) {
	    logger.severe( "Malformed URL exception: " + malformedURLException );
	    return;
	}

	getPostbankBeschermBeleggenKoers( "aex clicker" );
	getPostbankBeschermBeleggenKoers( "china clicker" );
    }

    private static boolean getPostbankBeschermBeleggenKoers( String fondsString ) {
        final Logger logger = Logger.getLogger( "getUrl.getPostbankBeschermBeleggenKoers" );
	final Pattern fondsPattern = Pattern.compile( fondsString );
	final Pattern koersPattern = Pattern.compile( ">(\\d*,\\d*)<" );

	InputStreamReader urlInputStreamReader = null;

	try {
	    urlInputStreamReader = new InputStreamReader( url.openStream( ) );
	    BufferedReader bufferedReader = new BufferedReader(  urlInputStreamReader );
	    String lineString;
	    while ( ( lineString = bufferedReader.readLine( ) ) != null ) {
		Matcher fondsMatcher = fondsPattern.matcher( lineString );
		if ( fondsMatcher.find( ) ) {
		    System.out.println( "Line: " + lineString );
		    lineString = bufferedReader.readLine( );
		    System.out.println( "Line: " + lineString );
		    Matcher koersMatcher = koersPattern.matcher( lineString );
		    if ( koersMatcher.find( ) ) {
			int groupCount = koersMatcher.groupCount( ) ;
			System.out.println( " Group count: " + groupCount );
			if ( groupCount > 0 ) {
			    System.out.println( "Koers " + fondsString + ": " +
						koersMatcher.group( 1 ) );
			    float koers = Float.parseFloat( koersMatcher.group( 1 ).replace( ',', '.' ) );
			    System.out.println( "Koers " + fondsString + ": " + koers );
			    return true;
			}
		    }
		}
	    }
	}
	catch ( IOException ioException ) {
	    logger.severe( "IO exception: " + ioException );
	}
	finally {
	    try {
		urlInputStreamReader.close( );
	    }
	    catch ( IOException ioException ) {
		logger.severe( "IO exception: " + ioException );
	    }
	}

	return false;
    }
}
