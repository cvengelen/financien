package financien;

import java.net.*;
import javax.net.ssl.*;
import java.io.*;
import java.util.regex.*;
import java.util.logging.*;

public class postUrl {
    static URL url = null;

    public static void main( String[ ] args ) {
        final Logger logger = Logger.getLogger( "postUrl.main" );

	try {
	    url = new URL( args[ 0 ] );
	}
	catch ( MalformedURLException malformedURLException ) {
	    logger.severe( "Malformed URL exception: " + malformedURLException );
	    return;
	}


	final Pattern gebruikersNaamPattern = Pattern.compile( ">Gebruikersnaam<" );
	final Pattern wachtwoordPattern = Pattern.compile( ">Wachtwoord<" );
	final Pattern namePattern = Pattern.compile( "name=\"(\\w*)\"" );

	InputStreamReader urlInputStreamReader = null;
	String gebruikersNaamNameString = null;
	String wachtwoordNameString = null;

	try {
	    urlInputStreamReader = new InputStreamReader( url.openStream( ) );
	    BufferedReader bufferedReader = new BufferedReader(  urlInputStreamReader );
	    String lineString;
	    while ( ( lineString = bufferedReader.readLine( ) ) != null ) {
		Matcher gebruikersNaamMatcher = gebruikersNaamPattern.matcher( lineString );
		if ( gebruikersNaamMatcher.find( ) ) {
		    lineString = bufferedReader.readLine( );
		    lineString = bufferedReader.readLine( );
		    lineString = bufferedReader.readLine( );
		    // System.out.println( "Line: " + lineString );
		    Matcher nameMatcher = namePattern.matcher( lineString );
		    if ( nameMatcher.find( ) ) {
			int groupCount = nameMatcher.groupCount( ) ;
			// System.out.println( " Group count: " + groupCount );
			if ( groupCount > 0 ) {
			    // System.out.println( "Gebruikers naam name: " + nameMatcher.group( 1 ) );
			    gebruikersNaamNameString = nameMatcher.group( 1 );
			    break;
			}
			else {
			    logger.severe( "Gebruikers naam name field group count 0" );
			    return;
			}
		    }
		    else {
			logger.severe( "Gebruikers naam name field not found" );
			return;
		    }
		}
	    }

	    while ( ( lineString = bufferedReader.readLine( ) ) != null ) {
		Matcher wachtwoordMatcher = wachtwoordPattern.matcher( lineString );
		if ( wachtwoordMatcher.find( ) ) {
		    lineString = bufferedReader.readLine( );
		    lineString = bufferedReader.readLine( );
		    lineString = bufferedReader.readLine( );
		    // System.out.println( "Line: " + lineString );
		    Matcher nameMatcher = namePattern.matcher( lineString );
		    if ( nameMatcher.find( ) ) {
			int groupCount = nameMatcher.groupCount( ) ;
			// System.out.println( " Group count: " + groupCount );
			if ( groupCount > 0 ) {
			    // System.out.println( "Wachtwoord name: " + nameMatcher.group( 1 ) );
			    wachtwoordNameString = nameMatcher.group( 1 );
			    break;
			}
			else {
			    logger.severe( "Wachtwoord name field group count 0" );
			    return;
			}
		    }
		    else {
			logger.severe( "Wachtwoord name field not found" );
			return;
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

	// Safety check
	if ( ( gebruikersNaamNameString == null ) || ( wachtwoordNameString == null ) ) return;

	System.out.println( "Gebruikers naam name: " + gebruikersNaamNameString );
	System.out.println( "Wachtwoord name: " + wachtwoordNameString );

	StringBuffer stringBuffer = new StringBuffer( );
	stringBuffer.append( URLEncoder.encode( gebruikersNaamNameString ) + "=" );
	stringBuffer.append( URLEncoder.encode( "2540650yo3" ) );
	stringBuffer.append( "&" + URLEncoder.encode( wachtwoordNameString ) + "=" );
	stringBuffer.append( URLEncoder.encode( "pdox70" ) );
	String formDataString = stringBuffer.toString( );

	try {
	    HttpsURLConnection urlConnection = ( HttpsURLConnection )url.openConnection( );
	    urlConnection.setRequestMethod( "POST" );
	    urlConnection.setRequestProperty( "Content-type", "application/x-www-form-urlencoded" );
	    urlConnection.setDoOutput( true );
	    urlConnection.setDoInput( true );

	    OutputStream outputStream = urlConnection.getOutputStream( );
	    OutputStreamWriter outputStreamWriter = new OutputStreamWriter( outputStream, "8859_1" );
	    PrintWriter printWriter = new PrintWriter( outputStreamWriter, true );
	    printWriter.print( formDataString );
	    printWriter.flush( );

	    // Read results
	    int responseCode = urlConnection.getResponseCode( );
	    if ( responseCode == HttpURLConnection.HTTP_OK ) {
		System.out.println( "Post OK" );

		urlInputStreamReader = new InputStreamReader( urlConnection.getInputStream( ) );
		BufferedReader bufferedReader = new BufferedReader(  urlInputStreamReader );
		String lineString;
		while ( ( lineString = bufferedReader.readLine( ) ) != null ) {
		    System.out.println( lineString );
		}
	    }
	    else {
		System.out.println( "Post error: " + responseCode );
	    }
	}
	catch ( IOException ioException ) {
	    logger.severe( "IO exception: " + ioException );
	}
    }
}
