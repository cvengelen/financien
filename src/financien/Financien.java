/*
 * Copyright (c) 1995, 2008, Oracle and/or its affiliates. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 *   - Redistributions of source code must retain the above copyright
 *     notice, this list of conditions and the following disclaimer.
 *
 *   - Redistributions in binary form must reproduce the above copyright
 *     notice, this list of conditions and the following disclaimer in the
 *     documentation and/or other materials provided with the distribution.
 *
 *   - Neither the name of Oracle or the names of its
 *     contributors may be used to endorse or promote products derived
 *     from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS
 * IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO,
 * THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED.  IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package financien;

import financien.gui.PasswordPanel;
import financien.ingmutaties.LoadIngMutaties;
import financien.ingmutaties.ProcessIngMutaties;
import financien.rabobankmutaties.LoadRabobankMutaties;
import financien.rabobankmutaties.ProcessRabobankMutaties;

import javax.swing.*;

import java.awt.event.*;
import java.awt.*;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.logging.Logger;

/*
 * Financien main program
 *   MyInternalFrame.java
 */
public class Financien extends JFrame implements ActionListener {
    private final static Logger logger = Logger.getLogger( financien.Main.class.getCanonicalName() );

    private JDesktopPane desktopPane;
    private Connection connection;
    private String password;

    private int openFrameCount = 0;
    private static final int xOffset = 30, yOffset = 30;

    private Financien() {
        super("Financien");

        //Make the big window be indented 50 pixels from each edge
        //of the screen.
        int inset = 50;
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        setBounds(inset, inset,
                  screenSize.width  - inset*2,
                  screenSize.height - inset*2);

        //Set up the GUI.
        desktopPane = new JDesktopPane();
        setContentPane(desktopPane);
        setJMenuBar(createMenuBar());

        //Make dragging a little faster but perhaps uglier.
        desktopPane.setDragMode(JDesktopPane.OUTLINE_DRAG_MODE);


        try {
            // Load the MySQL JDBC driver
            Class.forName( "com.mysql.jdbc.Driver" );
        } catch ( ClassNotFoundException classNotFoundException ) {
            logger.severe( "ClassNotFoundException: " + classNotFoundException.getMessage() );
            System.exit( 1 );
        }

        try {
            // Get the password for the financien account, which gives access to schema financien.
            final PasswordPanel passwordPanel = new PasswordPanel();
            password = passwordPanel.getPassword();
            if (password == null) {
                logger.info("No password");
                System.err.println("Geen password gegeven");
                System.exit( 1 );
            }

            connection = DriverManager.getConnection( "jdbc:mysql://localhost/financien?user=financien&password=" + password );
        } catch ( SQLException sqlException ) {
            logger.severe( "SQLException: " + sqlException.getMessage( ) );
            System.exit( 1 );
        } catch ( Exception exception ) {
            logger.severe( "Exception: " + exception.getMessage( ) );
            System.exit( 1 );
        }

        // Add a window listener to close the connection when the frame is disposed
        addWindowListener( new WindowAdapter() {
            @Override
            public void windowClosed(WindowEvent e) {
                try {
                    // Close the connection to the MySQL database
                    connection.close( );
                } catch (SQLException sqlException) {
                    logger.severe( "SQL exception closing connection: " + sqlException.getMessage() );
                }
            }
        } );
    }

    private JMenuBar createMenuBar() {
        JMenuBar menuBar = new JMenuBar();

        //Set up the ING Mutaties menu
        JMenu menu = new JMenu("ING Mutaties");
        menu.setMnemonic(KeyEvent.VK_I);
        menuBar.add(menu);

        // Load ING mutaties.
        JMenuItem menuItem = new JMenuItem("Load");
        menuItem.setMnemonic(KeyEvent.VK_L);
        menuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_L, InputEvent.ALT_MASK));
        menuItem.setActionCommand("loadIngMutaties");
        menuItem.addActionListener(this);
        menu.add(menuItem);

        // Process ING mutaties
        menuItem = new JMenuItem("Process");
        menuItem.setMnemonic(KeyEvent.VK_P);
        menuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_P, InputEvent.ALT_MASK));
        menuItem.setActionCommand("processIngMutaties");
        menuItem.addActionListener(this);
        menu.add(menuItem);

        //Set up the Rabobank mutaties menu
        menu = new JMenu("Rabobank Mutaties");
        menu.setMnemonic(KeyEvent.VK_R);
        menuBar.add(menu);

        // Load Rabobank mutaties
        menuItem = new JMenuItem("Load");
        menuItem.setMnemonic(KeyEvent.VK_L);
        menuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_L, InputEvent.ALT_MASK));
        menuItem.setActionCommand("LoadRabobankMutatie");
        menuItem.addActionListener(this);
        menu.add(menuItem);

        // Process Rabobank mutaties
        menuItem = new JMenuItem("Process");
        menuItem.setMnemonic(KeyEvent.VK_P);
        menuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_P, InputEvent.ALT_MASK));
        menuItem.setActionCommand("processRabobankMutaties");
        menuItem.addActionListener(this);
        menu.add(menuItem);

        return menuBar;
    }

    //React to menu selections.
    public void actionPerformed(ActionEvent actionEvent) {
        openFrameCount++;
        if ("loadIngMutaties".equals(actionEvent.getActionCommand())) {
            JInternalFrame loadIngMutaties = new LoadIngMutaties( password, xOffset * openFrameCount, yOffset * openFrameCount );
            loadIngMutaties.setVisible( true );
            desktopPane.add( loadIngMutaties );
            try {
                loadIngMutaties.setSelected( true );
            } catch ( java.beans.PropertyVetoException propertyVetoException ) {
                logger.severe( propertyVetoException.getMessage() );
            }
        } else if ("processIngMutaties".equals(actionEvent.getActionCommand())) {
            JInternalFrame processIngMutaties = new ProcessIngMutaties( connection, this, xOffset * openFrameCount, yOffset * openFrameCount );
            processIngMutaties.setVisible( true );
            desktopPane.add( processIngMutaties );
            try {
                processIngMutaties.setSelected( true );
            } catch ( java.beans.PropertyVetoException propertyVetoException ) {
                logger.severe( propertyVetoException.getMessage() );
            }
        } else if ("loadRabobankMutaties".equals(actionEvent.getActionCommand())) {
            JInternalFrame loadRabobankMutaties = new LoadRabobankMutaties(password, xOffset*openFrameCount, yOffset*openFrameCount);
            loadRabobankMutaties.setVisible( true );
            desktopPane.add(loadRabobankMutaties);
            try {
                loadRabobankMutaties.setSelected(true);
            } catch (java.beans.PropertyVetoException propertyVetoException) {
                logger.severe( propertyVetoException.getMessage() );
            }
        } else if ("processRabobankMutaties".equals(actionEvent.getActionCommand())) {
            JInternalFrame processRabobankMutaties = new ProcessRabobankMutaties( connection, this, xOffset * openFrameCount, yOffset * openFrameCount );
            processRabobankMutaties.setVisible( true );
            desktopPane.add( processRabobankMutaties );
            try {
                processRabobankMutaties.setSelected( true );
            } catch ( java.beans.PropertyVetoException propertyVetoException ) {
                logger.severe( propertyVetoException.getMessage() );
            }
        }
    }

    /**
     * Create the GUI and show it.  For thread safety,
     * this method should be invoked from the
     * event-dispatching thread.
     */
    private static void createAndShowGUI() {
        //Make sure we have nice window decorations.
        JFrame.setDefaultLookAndFeelDecorated(true);

        //Create and set up the window.
        Financien financien = new Financien();
        financien.setDefaultCloseOperation( WindowConstants.EXIT_ON_CLOSE);

        //Display the window.
        financien.setVisible(true);
    }

    public static void main(String[] args) {
        //Schedule a job for the event-dispatching thread:
        //creating and showing this application's GUI.
        javax.swing.SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                createAndShowGUI();
            }
        });
    }
}
