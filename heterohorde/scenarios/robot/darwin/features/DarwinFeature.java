/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package sim.app.horde.scenarios.robot.darwin.features;

/**
 *
 * @author drew
 */
public class DarwinFeature {
    private static final long serialVersionUID = 1;

    public double ballX, ballY;
    public int ballDetect, doneApproach, yelledReady, yelledKick, yelledFail;
    public double particleX[];
    public double particleY[];
    public double particleA[];
    public int playerID, role;// same value
    // robot 1: {otherRobotsX[0], otherRobotsY[0], otherRobotA[0]}
    public double poseX[];
    public double poseY[];
    public double poseA[];
    public int allYelledReady[], allYelledKick[]; // all of the yelled ready from all robots
    public int ackNumber;
    public double closestToBallLoc[];
    public double midpoint[];
    public int isClosestToBall, isClosestToGoalDefend, isClosestToGoalOffend;
    public double penaltyBounds[]; // x and y of the penalty bounds of the goalie
    public int status; // [0-4]
    public int declared[]; //  {kiddie,support,safety} entry is the id of the person that has declared else 0
    public int goalieCloseEnough; //  0/1
    public int goalSign; // the sign of defending goal post
    public int connected;// if the robot becomes disconnected from the other robots/ref-box this is set to false.
    public int inPlay;
    public int timedOut;
    public DarwinFeature() {
        }
    
    }
