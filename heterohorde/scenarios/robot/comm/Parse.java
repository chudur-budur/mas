/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package sim.app.horde.scenarios.robot.comm;

/**
 * Takes input and will then parse it.  Implementers will want to write a method
 * that will parse and clean up the data...
 * @author drew
 */
public interface Parse {
    
    public void setInput(byte[] input);
    public String getInput();
    public byte[] getByteInput();
    }
