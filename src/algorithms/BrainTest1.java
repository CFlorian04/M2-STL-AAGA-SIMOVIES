/* ******************************************************
 * Simovies - Eurobot 2015 Robomovies Simulator.
 * Copyright (C) 2014 <Binh-Minh.Bui-Xuan@ens-lyon.org>.
 * GPL version>=3 <http://www.gnu.org/licenses/>.
 * $Id: algorithms/BrainCanevas.java 2014-10-19 buixuan.
 * ******************************************************/
package algorithms;

import characteristics.Parameters;
import robotsimulator.Brain;
import characteristics.IFrontSensorResult;

public class BrainTest1 extends Brain {

    private int taskStep;
    private static final IFrontSensorResult.Types WALL = IFrontSensorResult.Types.WALL;

    public BrainTest1() {
        super();
    }

    public void activate() {
        taskStep = 0;
    }

    public void step() {

        double epsilon = 0.05;
        double botHeading = getHeading() % (2 * Math.PI);
        double parameterOrientation = 0;

        switch (taskStep) {
            case 0, 4:
                parameterOrientation = Parameters.NORTH;
                break;
            case 1:
                parameterOrientation = Parameters.EAST;
                break;
            case 2:
                parameterOrientation = Parameters.SOUTH;
                break;
            case 3:
                parameterOrientation = Parameters.WEST;
                break;
            case 5:
                return;
        }

        if (parameterOrientation < 0) {
            parameterOrientation += 2 * Math.PI;
        }
        if (botHeading < 0) {
            botHeading += 2 * Math.PI;
        }

        double headMinus = parameterOrientation - epsilon;
        double headPlus = parameterOrientation + epsilon;

        sendLogMessage("Task " + taskStep);

        if (botHeading >= headMinus && botHeading <= headPlus) {
            if (detectFront().getObjectType() == WALL) {
                taskStep++;
            } else {
                move();
            }
        } else {
            stepTurn(Parameters.Direction.RIGHT);
        }
    }
}
