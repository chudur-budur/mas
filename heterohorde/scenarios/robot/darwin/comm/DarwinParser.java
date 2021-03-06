/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package sim.app.horde.scenarios.robot.darwin.comm;

import com.google.gson.Gson;
import sim.app.horde.scenarios.robot.comm.DefaultParser;
import sim.app.horde.scenarios.robot.darwin.features.DarwinFeature;

/**
 *
 * @author drew
 */
public class DarwinParser extends DefaultParser {

    private final Object lock = new Object[0];
    Gson json = new Gson();
    DarwinFeature feature = new DarwinFeature();
    
    // how long since we last saw the ball until we declare the ball lost in miliseconds
    long untilLost = 3000;// 3 seconds
    long untilLastKick = 1000;
    // time that the ball was last seen.
    long lastTimeSeen, lastTimeKicked;
    volatile boolean hasReceivedPacket = false;
    double distanceLastSeen;
    @Override
    public void setInput(byte[] input) {
        super.setInput(input); 
        
        synchronized(lock) {
            
            String readings = new String(input);
            
            
            
            // System.err.println(readings);
            //System.err.println("In Set input");
            //System.err.println("Input " + readings);
            DarwinFeature prevFeature = feature;
            try {
                feature = json.fromJson(readings, DarwinFeature.class);
                } catch(Exception jse) {
                System.err.println("Got a malformed feature vector so:\n " + jse.getMessage());
                feature = prevFeature; // reset to prev so that don't error on things
                return;
                }
            //System.err.println("The feature" + feature + ((feature != null) ? feature.ballDetect : ""));
            
            if (getBallLost() != 1.0)
                distanceLastSeen = Math.sqrt(feature.ballX * feature.ballX + feature.ballY * feature.ballY);
            else // we lost it
                distanceLastSeen = 9001;
            
            }
        
        hasReceivedPacket = true;
        //System.err.println("Received new valid packet ack# = " + feature.ackNumber);
        /*
          String s = "For id " + getPlayerID() + " my status is " + feature.status + " and declared vector: ";
        
          for (int i = 0; i < feature.declared.length; i++) {
          s += feature.declared[i] + " ";
          }
          System.out.println(s);*/
        //System.err.println("PlayerID = " + feature.playerID + "  x y theta " + feature.poseX[feature.playerID - 1] + ", " + feature.poseY[feature.playerID - 1] + ", " + feature.poseA[feature.playerID - 1]);
        /*
          String s = "";
          for (int i = 0; i < feature.poseA.length; i++) {
          s += " I am PlayerID" + feature.playerID +  " xytheta for id = " + i + " " +  feature.poseX[i] + ", " + feature.poseY[i] + ", " + feature.poseA[i] + "  ";
          }
          System.err.println(s);
        
          //System.err.println("PoseX " + getPoseX());
          //System.err.println("Current Distance to ball: "+ getDistanceToBall() + " ball x y" + getBallX() + " " + getBallY());
          //System.err.println("Current Distance To ball: " + getDistanceToBall() + "  Ball lost: " + getBallLost());
          System.err.println("ID =  "  + getPlayerID() + " Yelled Fail! " + getYelledFail() +  " Done Front Aproach: " + doneApproach() + " Kicked = " + getKicked() + " Ready = " + getReady() + "  Distance to ball: " + getDistanceToBall()+ "  Ball lost: " + getBallLost());
        */
        
        //getBallLost();
        }
    
    
    public int getInPlay() {
        synchronized(lock) {
            return feature.inPlay;
            }
        }
    
    
    public int getConnected() {
        synchronized(lock) {
            return feature.connected;
            }
        }

    /*
      has the other robot signaled to me that we have failed.
    */
    public int getYelledFail() {
        synchronized(lock) {
            return feature.yelledFail;
            }
        }
    
    public int getPlayerID() {
        synchronized(lock) {
            return feature.playerID;
            }
        }
    
    public int getRole() {
        synchronized(lock) {
            return feature.role;
            }
        }
    
    // make methods to access the features.
    public double detectBall() {
        synchronized(lock) {
            //System.err.println("Detect Ball: " + feature.ballDetect);
            return feature.ballDetect;
            }
        }
    
    public double getDistanceLastSeen() {
        synchronized(lock) {
            //System.err.println("Distance Last Seen: " + distanceLastSeen);
            return distanceLastSeen;
            }
        
        }

    public double getDistanceToBall() {
        synchronized(lock) {
            double dist = Math.sqrt(feature.ballX * (double)feature.ballX + feature.ballY * (double)feature.ballY);
            //System.err.println("distance: " + dist);
            return dist;
            }
        }

    public double getPoseX() {
        synchronized(lock) {
            //System.err.println("Pose X: " + feature.poseX);
            return feature.poseX[feature.playerID - 1];
            }
        }

    public double getPoseY() {
        synchronized(lock) {
            //System.err.println("Pose Y: " + feature.poseY);
            return feature.poseY[feature.playerID - 1];
            }
        }

    public double getPoseAngle() {
        synchronized(lock) {
            // System.err.println("Pose Angle: " + feature.poseA);
            return feature.poseA[feature.playerID - 1];
            }
        }

    public double getBallX() {
        synchronized(lock) {
            //System.err.println("Ball X: " + feature.ballX);
            return feature.ballX;
            }
        }
    public double getBallY() {
        synchronized(lock) {
            //System.err.println("Ball Y: " + feature.ballY);
            return feature.ballY;
            }
        }
    
    public double getClosestToBallLocX() {
        synchronized(lock) {
            return feature.closestToBallLoc[0];
            }
        }
    
    public double getClosestToBallLocY() {
        synchronized(lock) {
            return feature.closestToBallLoc[1];
            }
        }
    
    
    public double getPenaltyBoundsX() {
        synchronized(lock) {
            return feature.penaltyBounds[0];
            }
        }
    
    public double getPenaltyBoundsY() {
        synchronized(lock) {
            return feature.penaltyBounds[1];
            }
        }
    
    public double getMidpointX() {
        synchronized(lock) {
            return feature.midpoint[0];
            }
        }
    
    public double getMidpointY() {
        synchronized(lock) {
            return feature.midpoint[1];
            }
        }
    
    public int getSomeoneYelledReady() {
        synchronized(lock) {
            // What I really want is if the support yelled ready
            //System.err.println(" id " + getSupportDeclared() + " yelled ready = " + ((getSupportDeclared() != 0) ? feature.allYelledReady[getSupportDeclared() - 1] : 0));
            return (getSupportDeclared() != 0) ? feature.allYelledReady[getSupportDeclared() - 1] : 0;
            
            /*
              for (int i =0 ; i < feature.allYelledReady.length; i++) {
              if (feature.allYelledReady[i] == 1) {
              return 1;
              }
              }
              return 0;
            */
            }
        }
    
    public int getSomeoneYelledKick() {
        synchronized(lock) {
            for (int i =0 ; i < feature.allYelledKick.length; i++) {
                if (feature.allYelledKick[i] == 1) {
                    return 1;
                    }
                }
            return 0;
            }
        }
    
    public int getDefendingGoalSign() {
        synchronized(lock) {
            return feature.goalSign;
            }
        }
    public int getAttackingGoalSign() {
        synchronized(lock) {
            return feature.goalSign * -1;
            }
        }
    
    
    public int getIsClosestToBall() {
        synchronized(lock) {
            return feature.isClosestToBall;
            }
        }

    public int getIsClosestToGoalDefend() {
        synchronized(lock) {
            return feature.isClosestToGoalDefend;
            }
        }
    public int getIsClosestToGoalOffend() {
        synchronized(lock) {
            return feature.isClosestToGoalOffend;
            }
        }
    
    public double doneApproach() {
        synchronized(lock) {
            //System.err.println("done Front Approach: " + feature.doneFrontApproach);
            return feature.doneApproach;
            }
        }
    
    public double[] getParticleX() {
        synchronized(lock) {
            //System.err.println("done Front Approach: " + feature.doneFrontApproach);
            return feature.particleX;
            }
        }
    public double[] getParticleY() {
        synchronized(lock) {
            //System.err.println("done Front Approach: " + feature.doneFrontApproach);
            return feature.particleY;
            }
        }
    public double[] getParticleA() {
        synchronized(lock) {
            //System.err.println("done Front Approach: " + feature.doneFrontApproach);
            return feature.particleA;
            }
        }
    
    public int getNumParticles() {
        synchronized(lock) {
            //System.err.println("done Front Approach: " + feature.doneFrontApproach);
            return feature.particleX.length;
            }
        }
    
    
    public double getKicked() {
        synchronized(lock) {
            // kicked
            if (feature.yelledKick <= 0.0) {
                if (lastTimeKicked == 0)
                    lastTimeKicked = System.currentTimeMillis();
                if (Math.abs(System.currentTimeMillis() - lastTimeKicked) >= untilLastKick) {
                    
                    //    System.err.println("Ball Lost");
                    return 0.0; // 
                    }
                else {
                    //    System.err.println("Kick Not seen but within time period where it is not lost");
                    return 1.0;
                    } // still we believe we will see it we just missed it
                }
            else {
                //  System.err.println("Ball Found");
                lastTimeKicked = System.currentTimeMillis();
                return 1.0; // we saw the ball.
                }
            
            }
        }
    public double getReady() {
        synchronized(lock) {
            //System.err.println("Ready: " + feature.ready);
            return feature.yelledReady;
            }
        }
    
    public double getOtherRobotX(int i) {
        synchronized(lock) {
            return feature.poseX[i];
            }
        }
    public double getOtherRobotY(int i) {
        synchronized(lock) {
            return feature.poseY[i];
            }
        }
    public double getOtherRobotA(int i) {
        synchronized(lock) {
            return feature.poseA[i];
            }
        }
    
    public int getStatus() {
        synchronized(lock) {
            return feature.status;
            }
        }
    
    public int getKiddieDeclared() {
        synchronized(lock) {
            return feature.declared[0];
            }
        }
    public int getSupportDeclared() {
        synchronized(lock) {
            return feature.declared[1];
            }
        }
    public int getSafetyDeclared() {
        synchronized(lock) {
            return feature.declared[2];
            }
        }
    
    public int getGoalieCloseEnough() {
        synchronized(lock) {
            return feature.goalieCloseEnough;
            }
        }
    
    
    
    public double getBallLost() {
        
        synchronized(lock) {
            // lost
            if (feature.ballDetect <= 0.0) {
                if (lastTimeSeen == 0)
                    lastTimeSeen = System.currentTimeMillis();
                if (Math.abs(System.currentTimeMillis() - lastTimeSeen) >= untilLost) {
                    
                    //    System.err.println("Ball Lost");
                    return 1.0; // 
                    }
                else {
                    //    System.err.println("Ball Not seen but within time period where it is not lost");
                    return 0.0;
                    } // still we believe we will see it we just missed it
                }
            else {
                //  System.err.println("Ball Found");
                lastTimeSeen = System.currentTimeMillis();
                return 0.0; // we saw the ball.
                }
            }
        }
    
    
    public int getAckNumber() {
        synchronized(lock) {
            return feature.ackNumber;
            }
        
        }

    public double getTimedOut() {
        synchronized(lock) {
            return feature.timedOut;
            }
        }

    public boolean getHasReceivedPacket() {
        return hasReceivedPacket;
        }
    
    
    
    }
