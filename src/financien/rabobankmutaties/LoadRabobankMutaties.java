package financien.rabobankmutaties;

import financien.gui.PasswordPanel;

import javax.swing.*;
import javax.swing.event.AncestorEvent;
import javax.swing.event.AncestorListener;
import javax.swing.filechooser.FileFilter;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Frame to load downloaded Rabobank mutaties into the financien database.
 * @author Chris van Engelen
 */
public class LoadRabobankMutaties extends JInternalFrame {
    private final Logger logger = Logger.getLogger( LoadRabobankMutaties.class.getCanonicalName( ) );
    private File rabobankMutatieDataFile;
    private final JLabel rabobankMutatieDataFileLabel = new JLabel( );
    private final JButton okButton = new JButton( "OK" );
    private final JButton selectFileButton = new JButton( "Select other file" );

    public LoadRabobankMutaties(String password, int x, int y) {
        super("Load Rabobank mutaties", false, true, true, true);

	class TransactionsFilenameFilter implements FilenameFilter {
	    public boolean accept( File directory, String filenameString ) {
		Matcher fileNameMatcher = Pattern.compile( "CSV_A.*\\.csv" ).matcher( filenameString );
		return fileNameMatcher.find( );
	    }
	}

	// Find the last modified file in the directory with downloaded Rabobank mutatie data files
	final String rabobankMutatieDataDirectory = "/Users/cvengelen/Downloads";
	File raboMutatieDataDirectoryFile = new File( rabobankMutatieDataDirectory );
	String[ ] fileNamesArray = raboMutatieDataDirectoryFile.list( new TransactionsFilenameFilter( ) );
	SortedMap< Long, File > fileNamesMap = new TreeMap<>( );
	for ( String fileName: fileNamesArray ) {
	    File raboMutatieDataFile = new File( rabobankMutatieDataDirectory + "/" + fileName );
	    fileNamesMap.put( raboMutatieDataFile.lastModified( ), raboMutatieDataFile );
	}
        rabobankMutatieDataFile = fileNamesMap.get( fileNamesMap.lastKey( ) );
        logger.info( "Last modified file: " + rabobankMutatieDataFile.getName( ) );
        rabobankMutatieDataFileLabel.setText( rabobankMutatieDataFile.getName( ) );

	class TransactionsFileFilter extends FileFilter {
	    public boolean accept( File file ) {
		//Accept directories, and comma separated value files only
		if ( file.isDirectory( ) ) {
		    return true;
 		}

		String fileNameString = file.getName( );
		Matcher fileNameMatcher = Pattern.compile( "CSV_A.*\\.csv" ).matcher( fileNameString );
		return fileNameMatcher.find( );
	    }

	    public String getDescription() {
		return "Rabobank transactions files";
	    }
	}

        final JFileChooser loadRabobankMutatieDataFileChooser = new JFileChooser( rabobankMutatieDataDirectory );

        loadRabobankMutatieDataFileChooser.setFileFilter( new TransactionsFileFilter( ) );
        loadRabobankMutatieDataFileChooser.setFileSelectionMode( JFileChooser.FILES_ONLY );
        loadRabobankMutatieDataFileChooser.setDialogType( JFileChooser.OPEN_DIALOG );


        // When the JFileChooser dialog is opened with showOpenDialog, a bug in the underlying Swing/Aqua code
        // sets the selected file back to null. See AquaFileChooserUI.SelectionListener.valueChanged,
        // which calls JFileChooser.setSelectedFile with a null value:   var4.setSelectedFile(var2);
        // Therefore, the AncestorListener is used to set the selected file after the JFileChooser window has been opened.
        loadRabobankMutatieDataFileChooser.addAncestorListener( new AncestorListener() {
            @Override
            public void ancestorAdded( AncestorEvent event ) {
                loadRabobankMutatieDataFileChooser.setSelectedFile( rabobankMutatieDataFile );
            }

            @Override
            public void ancestorRemoved( AncestorEvent event ) { }

            @Override
            public void ancestorMoved( AncestorEvent event ) { }
        } );

        // Use the grid bag layout manager
        final Container container = getContentPane( );
        container.setLayout( new GridBagLayout( ) );
        final GridBagConstraints constraints = new GridBagConstraints( );

        final JPanel filePanel = new JPanel( );
        final JLabel filePrefix = new JLabel( "Rabobank transactions file:" );
        final Font dialogFont = new Font( "Dialog", Font.BOLD, 12 );
        filePrefix.setFont( dialogFont );
        filePanel.add( filePrefix, constraints );
        rabobankMutatieDataFileLabel.setFont( dialogFont );
        filePanel.add( rabobankMutatieDataFileLabel, constraints );

        constraints.insets = new Insets( 15, 15, 5, 15 );
        constraints.gridx = 0;
        constraints.gridy = 0;
        constraints.anchor = GridBagConstraints.CENTER;
        constraints.weightx = 1d;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        container.add( filePanel, constraints );

        final JButton cancelButton = new JButton( "Cancel" );
        cancelButton.addActionListener( ( ActionEvent actionEvent ) -> {
            logger.fine( "event: " + actionEvent.getActionCommand( ) );
            if ( actionEvent.getActionCommand().equals( "Cancel" ) ) {
                dispose( );
            }
        } );

        selectFileButton.addActionListener( ( ActionEvent actionEvent ) -> {
            logger.fine( "event: " + actionEvent.getActionCommand( ) );
            if ( actionEvent.getActionCommand().equals( "Select other file" ) ) {
                okButton.setEnabled( false );
                selectFileButton.setEnabled( false );

                // Set the selected file to null, to make sure that there is a change in selected file
                // when the selected file is set in the AncestorListener.
                loadRabobankMutatieDataFileChooser.setSelectedFile( null );

                if ( loadRabobankMutatieDataFileChooser.showOpenDialog( this ) == JFileChooser.APPROVE_OPTION ) {
                    rabobankMutatieDataFile = loadRabobankMutatieDataFileChooser.getSelectedFile( );
                    rabobankMutatieDataFileLabel.setText( rabobankMutatieDataFile.getName( ) );
                    logger.info( "Selected file: " + rabobankMutatieDataFile.getName( ) );
                }
                okButton.setEnabled( true );
                selectFileButton.setEnabled( true );
                getRootPane( ).setDefaultButton( okButton );
                okButton.requestFocusInWindow();
            }
        } );

        okButton.addActionListener( ( ActionEvent actionEvent ) -> {
            logger.fine( "event: " + actionEvent.getActionCommand( ) );
            if ( actionEvent.getActionCommand().equals( "OK" ) ) {
                int exitStatus = 0;
                String loadRabobankMutatieDataCmd = "/Users/cvengelen/bin/load-rabobank-mutatie-data -f " +
                        rabobankMutatieDataFile.getAbsolutePath( ) + " -p " + password;
                try {
                    logger.fine( "Executing command: " + loadRabobankMutatieDataCmd );
                    Process process = Runtime.getRuntime( ).exec( loadRabobankMutatieDataCmd );

                    // The thread must wait for the process to finish
                    exitStatus = process.waitFor( );
                    logger.info( "Process exit status: " + exitStatus );
                    if ( exitStatus != 0 ) {
                        System.err.println( "Error in executing " + loadRabobankMutatieDataCmd );
                    }
                } catch ( InterruptedException | IOException exception ) {
                    logger.severe( exception.getMessage( ) );
                    exitStatus = 1;
                }
                dispose( );
            }
        } );

        final JPanel buttonPanel = new JPanel( );
        buttonPanel.add( okButton );
        buttonPanel.add( selectFileButton );
        buttonPanel.add( cancelButton );

        constraints.insets = new Insets( 5, 15, 15, 15 );
        constraints.gridx = 0;
        constraints.gridy = 1;
        container.add( buttonPanel, constraints );

        setSize( 600, 150 );
        setLocation(x, y);
        setDefaultCloseOperation( JFrame.DISPOSE_ON_CLOSE );
        getRootPane( ).setDefaultButton( okButton );
        setVisible(true);
    }
}
