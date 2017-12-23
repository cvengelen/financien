package financien.ingmutaties;

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
 * Frame to load downloaded ING mutaties into the financien database.
 * @author Chris van Engelen
 */
public class LoadIngMutaties extends JInternalFrame {
    private final Logger logger = Logger.getLogger( LoadIngMutaties.class.getCanonicalName( ) );
    private File ingMutatieDataFile;
    private final JLabel ingMutatieDataFileLabel = new JLabel( );
    private final JButton okButton = new JButton( "OK" );
    private final JButton selectFileButton = new JButton( "Select other file" );

    public LoadIngMutaties(String password, int x, int y) {
        super("Load ING mutaties", false, true, true, true);

        class CsvFilenameFilter implements FilenameFilter {
	    public boolean accept( File directory, String filenameString ) {
		Matcher fileNameMatcher = Pattern.compile( "NL.*(2540650|1514423).*\\.csv" ).matcher( filenameString );
		return fileNameMatcher.find( );
	    }
	}

	// Find the last modified file in the directory with downloaded ING mutatie data files
	final String ingMutatieDataDirectoryString = "/Users/cvengelen/Downloads";
	File ingMutatieDataDirectoryFile = new File( ingMutatieDataDirectoryString );
	String[ ] filenameStringArray = ingMutatieDataDirectoryFile.list( new CsvFilenameFilter( ) );
	SortedMap< Long, File > filenameMap = new TreeMap<>( );
	for ( String filenameString:  filenameStringArray ) {
	    File ingMutatieDataFile = new File( ingMutatieDataDirectoryString + "/" + filenameString );
	    filenameMap.put( ingMutatieDataFile.lastModified( ), ingMutatieDataFile );
	}
	ingMutatieDataFile = filenameMap.get( filenameMap.lastKey( ) );
        logger.info( "Last modified file: " + ingMutatieDataFile.getName( ) );
        ingMutatieDataFileLabel.setText( ingMutatieDataFile.getName( ) );

	class CsvFileFilter extends FileFilter {
	    public boolean accept( File file ) {
		//Accept directories, and comma separated value files only
                if ( file.isDirectory( ) ) {
                    return true;
                }

		String fileNameString = file.getName( );
		Matcher fileNameMatcher = Pattern.compile( "NL.*(2540650|1514423).*\\.csv" ).matcher( fileNameString );
		return fileNameMatcher.find( );
	    }

	    public String getDescription() {
		return "ING export files";
	    }
	}

        final JFileChooser loadIngMutatieDataFileChooser = new JFileChooser( ingMutatieDataDirectoryString );

	loadIngMutatieDataFileChooser.setFileFilter( new CsvFileFilter( ) );
        loadIngMutatieDataFileChooser.setFileSelectionMode( JFileChooser.FILES_ONLY );
        loadIngMutatieDataFileChooser.setDialogType( JFileChooser.OPEN_DIALOG );

        // When the JFileChooser dialog is opened with showOpenDialog, a bug in the underlying Swing/Aqua code
        // sets the selected file back to null. See AquaFileChooserUI.SelectionListener.valueChanged,
        // which calls JFileChooser.setSelectedFile with a null value:   var4.setSelectedFile(var2);
        // Therefore, the AncestorListener is used to set the selected file after the JFileChooser window has been opened.
        loadIngMutatieDataFileChooser.addAncestorListener( new AncestorListener() {
            @Override
            public void ancestorAdded( AncestorEvent event ) {
                loadIngMutatieDataFileChooser.setSelectedFile( ingMutatieDataFile );
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
        final JLabel filePrefix = new JLabel( "ING export file:" );
        final Font dialogFont = new Font( "Dialog", Font.BOLD, 12 );
        filePrefix.setFont( dialogFont );
        filePanel.add( filePrefix, constraints );
        ingMutatieDataFileLabel.setFont( dialogFont );
        filePanel.add( ingMutatieDataFileLabel, constraints );

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
                loadIngMutatieDataFileChooser.setSelectedFile( null );

                if ( loadIngMutatieDataFileChooser.showOpenDialog( this ) == JFileChooser.APPROVE_OPTION ) {
                    ingMutatieDataFile = loadIngMutatieDataFileChooser.getSelectedFile( );
                    ingMutatieDataFileLabel.setText( ingMutatieDataFile.getName( ) );
                    logger.info( "Selected file: " + ingMutatieDataFile.getName( ) );
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
                String loadIngMutatieDataCmd = "/Users/cvengelen/bin/load-ing-mutatie-data -f " +
                        ingMutatieDataFile.getAbsolutePath( ) + " -p " + password;
                try {
                    logger.fine( "Executing command: " + loadIngMutatieDataCmd );
                    Process process = Runtime.getRuntime( ).exec( loadIngMutatieDataCmd );

                    // The thread must wait for the process to finish
                    exitStatus = process.waitFor( );
                    logger.info( "Process exit status: " + exitStatus );
                    if ( exitStatus != 0 ) {
                        System.err.println( "Error in executing " + loadIngMutatieDataCmd );
                        JOptionPane.showMessageDialog( this,
                                "Error in executing " + loadIngMutatieDataCmd,
                                "Load ING mutaties error",
                                JOptionPane.ERROR_MESSAGE );
                    }
                } catch ( InterruptedException | IOException exception ) {
                    logger.severe( exception.getMessage( ) );
                    JOptionPane.showMessageDialog( this,
                            "Exception when executing " + loadIngMutatieDataCmd + ": " + exception.getMessage(),
                            "Load ING mutaties exception",
                            JOptionPane.ERROR_MESSAGE );
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
