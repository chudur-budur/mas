/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package sim.app.horde.scenarios.robot.darwin.behaviors;

import com.google.gson.Gson;
import sim.app.horde.scenarios.robot.behaviors.CommandMotions;
import sim.util.Double2D;

/**
 *
 * @author drew
 */
public class Motions implements CommandMotions {
    private static final long serialVersionUID = 1;
   
    public static class Pose {
        double x,y,a;
        public Pose() {}
        public Pose(double x, double y, double a) {
            this.x = x;
            this.y = y;
            this.a = a;
            }
        }
    
    
    
    
    
    public static class PoseFacing {
        Pose gotoPose, facing;
        public PoseFacing() {}
        public PoseFacing(Pose gotoP, Pose facing) {
            this.gotoPose = gotoP;
            this.facing = facing;
            }
        }
    
    static final Gson gson = new Gson();
    public static Motions GOTO_BALL = new Motions("gotoBall", "",1);
    public static Motions APPROACH_BALL = new Motions("approachBall", "",2);
    public static Motions WALK_FORWARD = new Motions("walkForward", "",3);
    public static Motions MOVE_X = new Motions("moveX", "",4);
    public static Motions MOVE_Y = new Motions("moveY", "",5);
    public static Motions MOVE_THETA = new Motions("moveTheta", "",6);
    public static Motions STOP = new Motions("stop", "",7);
    public static Motions SIDE_KICK_RIGHT = new Motions("kickRight","",8);
    public static Motions SIDE_KICK_LEFT = new Motions("kickLeft","",9);
    public static Motions KICK_BALL = new Motions("kickBall","",10);
    
    // used as states in the robot to know when to transition
    public static Motions YELL_READY = new Motions("yellReady", "",11);
    public static Motions CLEAR_READY = new Motions("clearReady", "",12);
    public static Motions YELL_KICK = new Motions("yellKick", "",13);
    public static Motions CLEAR_KICK = new Motions("clearKick", "",14);
    
    
    public static Motions YELL_FAIL = new Motions("yellFail", "",15);
    
    public static Motions getGotoPose(double x, double y, double theta) {
        return new Motions("gotoPose", new Pose(x,y,theta),16);
        }
    
    
    public static Motions getBodyApproachTargetMotion(double x, double y, double theta) {
        return new Motions("approachTarget", new Pose(x, y, theta),17);
        }
    
    
    
    
    
            
            
    public static Motions getGotoPoseWhileFacing(Double2D loc, Double2D loc2) {
        return new Motions("gotoPoseFacing", new PoseFacing(new Pose(loc.x,loc.y,0), new Pose(loc2.x,loc2.y,0)),18);
        }
    
    public static Motions gotoPoseWhileLookingBackwards(Double2D loc, Double2D loc2) {
        return new Motions("gotoPoseWhileLookingBackwards", new PoseFacing(new Pose(loc.x,loc.y,0), new Pose(loc2.x,loc2.y,0)),19);
        }
    
    public static Motions gotoWhileFacingGoalie(Double2D loc, Double2D loc2) {
        return new Motions("gotoWhileFacingGoalie", new PoseFacing(new Pose(loc.x,loc.y,0), new Pose(loc2.x,loc2.y,0)),20);
        }
    
    public static Motions gotoPoseWhileLookingBackwardsUpdate(Double2D loc, Double2D loc2) {
        return new Motions("updateGotoPoseWhileLookingBackwards", new PoseFacing(new Pose(loc.x,loc.y,0), new Pose(loc2.x,loc2.y,0)),21);
        }
    
    
    public static Motions gotoWhileFacingGoalieUpdate(Double2D loc, Double2D loc2) {
        return new Motions("updateGotoWhileFacingGoalie", new PoseFacing(new Pose(loc.x,loc.y,0), new Pose(loc2.x,loc2.y,0)),22);
        }
    
    public static Motions getGotoPoseWhileFacingUpdate(Double2D loc, Double2D loc2) {
        return new Motions("updateGotoPoseFacing", new PoseFacing(new Pose(loc.x,loc.y,0), new Pose(loc2.x,loc2.y,0)),23);
        }
    
    public static Motions getGotoPoseUpdate(double x, double y, double theta) {
        return new Motions("updateGotoPose", new Pose(x,y,theta),24);
        }
    
    //public static Motions DECLARE = new Motions("declare", ,23);
    
    public static Motions getDeclare(int declare[]) {
        return new Motions("declare", declare, 25);
        }
    
    public static Motions UNDECLARE = new Motions("undeclare", "",26);
    
    public static Motions getDefer(Double2D loc, Double2D loc2) {
        return new Motions("gotoPoseFacing", new PoseFacing(new Pose(loc.x,loc.y,0), new Pose(loc2.x,loc2.y,0)),27);
        }
    
    DoMotion motion;
    int id;

    private Motions(String action, String args, int id) {
        motion = new DoMotion(action, args);
        this.id = id;
        }
    
    private Motions(String action, Object args, int id) {
        
        motion = new DoMotion(action, args);
        this.id = id;
        }
    
    
    
    public class DoMotion {
        String action;
        Object args;
        int ackNumber;
        public DoMotion(String action, String args) {
            this.action = action;
            this.args = args;
            }
        public DoMotion(String action, Object args) {
            this.action = action;
            this.args = args;
            }
        public void setAckNumber(int ack) {
            this.ackNumber = ack;
            //System.err.println("Action I am sending = " + action + "  ack number = " + ack);
            }
        
        }
    
    
    @Override
    public byte[] command(byte speed) {
        // System.err.println("Sending command in Motions.java: " + (gson.toJson(motion) + "\n"));
        return (gson.toJson(motion) + "\n").getBytes();
        }
    
    public DoMotion getMotion() {
        return motion;
        }
    public int getID() {
        return id;
        }
    
    }
