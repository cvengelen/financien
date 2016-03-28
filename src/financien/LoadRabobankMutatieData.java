// Main program to load downloaded Rabo mutatie data into the financien database

package financien;

import java.util.logging.*;
import financien.gui.LoadRabobankMutatieDataFrame;

public class LoadRabobankMutatieData {
    public static void main( String[ ] args ) {
        final Logger logger = Logger.getLogger( LoadRabobankMutatieData.class.getCanonicalName( ) );
        LoadRabobankMutatieDataFrame LoadRaboMutatieDataFrame = new LoadRabobankMutatieDataFrame( );
    }
}
