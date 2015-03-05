/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package sim.app.horde.scenarios.boxpushing;

import sim.app.horde.*;
import sim.app.horde.scenarios.robot.flockbots.comm.RCParser;
import sim.engine.SimState;

public class BoxpushingHitabWithUI extends SimHordeWithUI
    {
    RCParser rcp;
    boolean setup;
    public BoxpushingHitabWithUI()
        {
        this(new BoxpushingHitab(System.currentTimeMillis()));
        
        }

    public BoxpushingHitabWithUI(SimState state)
        {
        super(state);
        }
    
    public void setup() {
        if(!setup) {
            
            
            
            
            }
        }

    @Override
    public void quit() {
        super.quit(); //To change body of generated methods, choose Tools | Templates.
        }
    
    
    

    @Override
    public void start() {
        super.start();
        
        }
        
        
        
        

    @Override
    public void setupPortrayals()
        {
        setupAgentsAndPlacesPortrayals();
        }



    public static void main(String[] args)
        {
        new BoxpushingHitabWithUI().createController();
        }
    }

