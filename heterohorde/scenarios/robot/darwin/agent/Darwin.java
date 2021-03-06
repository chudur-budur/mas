/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package sim.app.horde.scenarios.robot.darwin.agent;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JOptionPane;
import sim.app.horde.scenarios.robot.Robot;
import sim.app.horde.scenarios.robot.behaviors.CommandMotions;
import sim.app.horde.scenarios.robot.comm.Communication;
import sim.app.horde.scenarios.robot.comm.Parse;
import sim.app.horde.scenarios.robot.darwin.comm.DarwinComm;
import sim.app.horde.scenarios.robot.darwin.comm.DarwinParser;

/**
 *
 * @author drew
 */
public class Darwin implements Robot{
    static final long serialVersionUID = 1;

    
    
    Communication comm;
    Parse myParser;
    
    final DarwinParser parser = new DarwinParser();
    final byte readDelayMS = 30;
    Thread th;
    final byte speed;
    public Darwin() { speed = 1; }
    public Darwin(String host, int port, byte speed) {
        this.speed = speed;
        try {       
            
            comm = new DarwinComm(host, port, parser, readDelayMS);
            
            myParser = parser;
            th = new Thread(comm);
            th.start();
            } catch (IOException ex) {
            System.out.println("Error creating FlockbotComm.");
            }
        }
    
    
    @Override
    public void sendCommand(CommandMotions cms) {
        try {
            
            /*
              try {
              Thread.currentThread().sleep(500); // wait so that the robot can catch up... TODO: fix this behavior
              } catch (InterruptedException ex) {
              Logger.getLogger(Darwin.class.getName()).log(Level.SEVERE, null, ex);
              }
            */
            
            comm.write(cms.command(speed));
            
            } catch (IOException ex) {
            System.out.println("Error sending command " + cms.toString() + " to " + comm.getIP() + ":" + comm.getPort());
            new JOptionPane("Error sending Command!" + cms.toString() + " to " + comm.getIP() + ":" + comm.getPort()).setVisible(true);
                
            //System.exit(9); // want it to crash if we loose connection.
            //throw new RuntimeException("Lost Connection to robot at ip : " + comm.getIP() + " trying to send " + cms.toString());
                
            }
        
        }
    
    public void sendCommand(CommandMotions cms, byte customSpeed) {
        try {
            comm.write(cms.command(customSpeed));
            } catch (IOException ex) {
            throw new RuntimeException("Lost Connection to robot at ip : " + comm.getIP() + " trying to send " + cms.toString());
            //System.out.println("Error sending command " + cms.toString() + " to " + comm.getIP() + ":" + comm.getPort());
            }
        }

    @Override
    public Parse getParser() {
        return myParser;
        }
    
    }
