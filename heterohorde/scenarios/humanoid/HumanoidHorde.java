package sim.app.horde.scenarios.humanoid;

import java.io.File;

import sim.app.horde.*; 
import sim.app.horde.behaviors.*;
import sim.app.horde.scenarios.humanoid.console.HordeNetworkReader;

public class HumanoidHorde extends Horde implements MacroObserver {

    private static final long serialVersionUID = -884443456143824943L;

    HumanoidAgent trainingAgent; 
    public Agent getTrainingAgent() { return trainingAgent; } 
                
    public void setBasicBehaviorLocation(String s) { BASIC_BEHAVIORS_LOCATION = s; } 
    public void setBasicTargetLocation(String s) { BASIC_TARGETS_LOCATION = s; } 
    public void setBasicFeatureLocation(String s) { BASIC_FEATURES_LOCATION = s; } 
    public void setTrainableMacroDirectory(String s) { TRAINABLE_MACRO_DIRECTORY = s;} 

        
    public HumanoidHorde(HordeNetworkReader comm) {
        super(System.currentTimeMillis());
                
        setBasicBehaviorLocation("scenarios/humanoid/humanoid.behaviors"); 
        setBasicTargetLocation("scenarios/humanoid/humanoid.targets"); 
        setBasicFeatureLocation("scenarios/humanoid/humanoid.features"); 
        setTrainableMacroDirectory("/tmp/trained/");
        //setTrainableMacroDirectory(HumanoidHorde.getPathInDirectory("trained"));
                
        trainingAgent = new HumanoidAgent(this, null, comm); 
        observer = this; 
        }

    public void transitioned(Macro macro, int from, int to) {} 

    public static String getPathInDirectory(String s) {
        File f = new File(HumanoidHorde.class.getResource("").getPath() + "/" + s +"/"); 
        //if (!f.isDirectory()) { // create directory since it doesn't exist 
        //      f.mkdirs(); 
        //      System.out.println("Created directory: " + f.getPath()); 
        //}
        return f.getPath() + File.separatorChar;                
        }

    @Override
    public void trainingAgentChanged() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

    @Override
    public void resetting() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }
    }
