/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package sim.app.horde.scenarios.robot.comm;

/**
 *   
 * @author drew
 */
public class DefaultParser implements Parse{

    protected byte[] in;
    
    @Override
    public void setInput(byte[] input) {
        in = input;
        }

    @Override
    public String getInput() {
        return in.toString();
        }
    
    public byte[] getByteInput() {
        return in;
        }

    
    }
