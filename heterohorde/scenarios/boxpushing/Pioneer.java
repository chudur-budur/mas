package sim.app.horde.scenarios.boxpushing;

import sim.app.horde.*;

public class Pioneer 
    {
    public final static int robotPort = 6008;

    public final static int MOVE_SPEED = 7;
    public final static int TURN_SPEED = 7;

    public final static int TRAINING_TURN_SPEED = 7;
    public final static int TRAINING_MOVE_SPEED = 7;

    public final static int FORWARD = 0;
    public final static int BACKWARD = 1;
    public final static int LEFT = 2;
    public final static int RIGHT = 3;

    public gmu.robot.pioneer.PioneerRobot robot = new gmu.robot.pioneer.PioneerRobot();

    String ipAddress = "";

    public Pioneer(Horde horde, String ipAddress)
        {
        this.ipAddress = ipAddress;

        robot.setVerbose(false);
        robot.connect(ipAddress, robotPort);
        robot.enable(true);
        robot.sonar(false);

        }

    public void stop()
        {
        System.err.println("[" + getClass().getName() + "] Disconnecting from Robot " + ipAddress);
        stopRobot();
        for (int i = 0; i < 10; i++)
            if (robot.close())
                break;
        robot.disconnect();
        robot = null;
        System.err.println("[" + getClass().getName() + "] Stopped robot " + ipAddress);

        }

    public void stopRobot()
        {
        robot.vel2((byte) 0, (byte) 0);
        }

    public void move(int direction, byte speed)
        {
        if (direction == FORWARD)
            robot.vel2(speed, speed);
        else
            robot.vel2((byte) -speed, (byte) -speed);
        }

    public void turn(int direction, byte speed)
        {
        /* 
         * byte speed = (byte) TURN_SPEED;
         if (isTraining())
         speed = (byte) TRAINING_TURN_SPEED;
        */
        if (direction == LEFT)
            robot.vel2((byte) -speed, speed);
        else
            robot.vel2(speed, (byte) -speed);
        }

    }