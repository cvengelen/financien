// frame to load downloaded Rabobank mutatie data into the financien database

package financien.gui;

import javax.swing.*;
import javax.swing.filechooser.FileFilter;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class LoadRabobankMutatieDataFrame {
    final private Logger logger = Logger.getLogger( LoadRabobankMutatieDataFrame.class.getCanonicalName( ) );
    final JFrame frame = new JFrame( "Load Rabobank transactions file" );
    File rabobankMutatieDataFile;
    final JLabel rabobankMutatieDataFileLabel = new JLabel( );
    final JButton okButton = new JButton( "OK" );

    public LoadRabobankMutatieDataFrame() {

	class TransactionsFilenameFilter implements FilenameFilter {
	    public boolean accept( File directory, String filenameString ) {
		Matcher fileNameMatcher = Pattern.compile( "transactions.*\\.txt" ).matcher( filenameString );
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
		Matcher fileNameMatcher = Pattern.compile( "transactions.*\\.txt" ).matcher( fileNameString );
		return fileNameMatcher.find( );
	    }

	    public String getDescription() {
		return "Rabobank transactions files";
	    }
	}

        final Container container = frame.getContentPane( );

        // Set grid bag layout manager
        container.setLayout( new GridBagLayout( ) );
        GridBagConstraints constraints = new GridBagConstraints( );
        constraints.anchor = GridBagConstraints.EAST;
        constraints.insets = new Insets( 5, 5, 5, 5 );

        final JFileChooser loadRabobankMutatieDataFileChooser = new JFileChooser( rabobankMutatieDataDirectory );

        loadRabobankMutatieDataFileChooser.setFileFilter( new TransactionsFileFilter( ) );
        loadRabobankMutatieDataFileChooser.setFileSelectionMode( JFileChooser.FILES_ONLY );
        loadRabobankMutatieDataFileChooser.setSelectedFile( rabobankMutatieDataFile );
        loadRabobankMutatieDataFileChooser.ensureFileIsVisible( rabobankMutatieDataFile );
        loadRabobankMutatieDataFileChooser.setDialogType( JFileChooser.OPEN_DIALOG );
        loadRabobankMutatieDataFileChooser.setVisible( false );
        loadRabobankMutatieDataFileChooser.addActionListener( new ActionListener( ) {
            @Override
            public void actionPerformed( ActionEvent actionEvent ) {
                logger.info( "event: " + actionEvent.getActionCommand( ) );
                if ( actionEvent.getActionCommand().equals( JFileChooser.APPROVE_SELECTION ) ) {
                    rabobankMutatieDataFile = loadRabobankMutatieDataFileChooser.getSelectedFile( );
                    rabobankMutatieDataFileLabel.setText( rabobankMutatieDataFile.getName( ) );
                }
                loadRabobankMutatieDataFileChooser.setVisible( false );
                okButton.setEnabled( true );
                frame.getRootPane( ).setDefaultButton( okButton );
            }
        });

        constraints.gridx = 0;
        constraints.gridy = 0;
        constraints.anchor = GridBagConstraints.CENTER;
        constraints.weightx = 1.0;
        constraints.weighty = 1.0;
        constraints.fill = GridBagConstraints.BOTH;
        JPanel panel = new JPanel( );
        panel.add( loadRabobankMutatieDataFileChooser );
        container.add( panel, constraints );

        final JPanel filePanel = new JPanel( );
        final JLabel filePrefix = new JLabel( "Rabobank transactions file:" );
        final Font dialogFont = new Font( "Dialog", Font.BOLD, 12 );
        filePrefix.setFont( dialogFont );
        filePanel.add( filePrefix, constraints );
        rabobankMutatieDataFileLabel.setFont( dialogFont );
        filePanel.add( rabobankMutatieDataFileLabel, constraints );

        constraints.gridx = 0;
        constraints.gridy = 1;
        constraints.weightx = 0.0;
        constraints.weighty = 0.0;
        constraints.fill = GridBagConstraints.NONE;
        container.add( filePanel, constraints );

        final JButton cancelButton = new JButton( "Cancel" );
        cancelButton.addActionListener( new ActionListener( ) {
            @Override
            public void actionPerformed( ActionEvent actionEvent ) {
                logger.info( "event: " + actionEvent.getActionCommand( ) );
                if ( actionEvent.getActionCommand().equals( "Cancel" ) ) {
                    System.exit( 1 );
                }
            }
        });

        final JButton selectFileButton = new JButton( "Zoek file" );
        selectFileButton.addActionListener( new ActionListener( ) {
            @Override
            public void actionPerformed( ActionEvent actionEvent ) {
                logger.info( "event: " + actionEvent.getActionCommand( ) );
                if ( actionEvent.getActionCommand().equals( "Zoek file" ) ) {
                    loadRabobankMutatieDataFileChooser.setVisible( true );
                    okButton.setEnabled( false );
                }
            }
        });

        okButton.addActionListener( new ActionListener( ) {
            @Override
            public void actionPerformed( ActionEvent actionEvent ) {
                logger.info( "event: " + actionEvent.getActionCommand( ) );
                if ( actionEvent.getActionCommand().equals( "OK" ) ) {
                    int exitStatus = 0;
                    String loadRabobankMutatieDataCmd = "/Users/cvengelen/bin/load-rabo-mutatie-data -f " +
                            rabobankMutatieDataFile.getAbsolutePath( );
                    try {
                        logger.info( "Executing command: " + loadRabobankMutatieDataCmd );
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
                    System.exit( exitStatus );
                }
            }
        });

        final JPanel buttonPanel = new JPanel( );
        buttonPanel.add( okButton );
        buttonPanel.add( selectFileButton );
        buttonPanel.add( cancelButton );

        constraints.gridx = 0;
        constraints.gridy = 2;
        container.add( buttonPanel, constraints );

        frame.setSize( 600, 530 );
        frame.setDefaultCloseOperation( JFrame.DISPOSE_ON_CLOSE );
        frame.getRootPane( ).setDefaultButton( okButton );
        logger.info( "set frame visible" );
        frame.setVisible(true);
    }
}
