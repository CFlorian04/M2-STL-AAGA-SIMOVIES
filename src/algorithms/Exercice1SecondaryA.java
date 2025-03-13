/* ******************************************************
 * Simovies - Eurobot 2015 Robomovies Simulator.
 * Copyright (C) 2014 <Binh-Minh.Bui-Xuan@ens-lyon.org>.
 * GPL version>=3 <http://www.gnu.org/licenses/>.
 * $Id: algorithms/Stage1.java 2014-10-18 buixuan.
 * ******************************************************/
package algorithms;

import robotsimulator.Brain;
import characteristics.Parameters;
import characteristics.IFrontSensorResult;
import characteristics.IRadarResult;

import java.util.ArrayList;

public class Exercice1SecondaryA extends Brain {
    //---PARAMETERS---//
    private static final double ANGLEPRECISION = 0.05;

    private static final int ROCKY = 0x1EADDA;
    private static final int MARIO = 0x5EC0;
    private static final int TEAM = 0xBADDAD;
    private static final int UNDEFINED = 0xBADC0DE0;

    private static final int FIRE = 0xB52;
    private static final int FALLBACK = 0xFA11BAC;
    private static final int ROGER = 0x0C0C0C0C;
    private static final int OVER = 0xC00010FF;
    private static final int DETECT = 0xD373C7;

    private static final int TURNLEFTTASK = 1;
    private static final int MOVETASK = 2;
    private static final int TURNRIGHTTASK = 3;
    private static final int SINK = 0xBADC0DE1;

    //---VARIABLES---//
    private int state;
    private double oldAngle;
    private double myX,myY;
    private boolean isMoving;
    private boolean freeze;
    private boolean escape;
    private int whoAmI;

    private double targetX;
    private double targetY;

    //---CONSTRUCTORS---//
    public Exercice1SecondaryA() { super(); }

    //---ABSTRACT-METHODS-IMPLEMENTATION---//
    public void activate() {
        //ODOMETRY CODE
        whoAmI = ROCKY;
        for (IRadarResult o: detectRadar())
            if (isSameDirection(o.getObjectDirection(),Parameters.NORTH)) whoAmI=UNDEFINED;
        if (whoAmI == ROCKY){
            myX=Parameters.teamASecondaryBot1InitX;
            myY=Parameters.teamASecondaryBot1InitY;
        } else {
            myX=Parameters.teamASecondaryBot2InitX;
            myY=Parameters.teamASecondaryBot2InitY;
        }

        //INIT
        state=TURNLEFTTASK;
        isMoving=false;
        oldAngle=getHeading();
        escape = false;
    }
    public void step() {
        //ODOMETRY CODE
        if (isMoving){
            myX+=Parameters.teamASecondaryBotSpeed*Math.cos(getHeading());
            myY+=Parameters.teamASecondaryBotSpeed*Math.sin(getHeading());
            isMoving=false;
        }
        //DEBUG MESSAGE
        if (whoAmI == ROCKY) sendLogMessage("#ROCKY *thinks* he is rolling at position ("+(int)myX+", "+(int)myY+").");
        else sendLogMessage("#MARIO *thinks* he is rolling at position ("+(int)myX+", "+(int)myY+").");

        //COMMUNICATION
        ArrayList<String> messages=fetchAllMessages();
        for (String m: messages) if (Integer.parseInt(m.split(":")[1])==whoAmI || Integer.parseInt(m.split(":")[1])==TEAM) process(m);


        if(escape) {
            double angle = calculateAngle(myX, myY, targetX, targetY);
            if( !isSameDirection(angle, getHeading()%Math.PI) ) {
                stepTurn(Parameters.Direction.LEFT);
            } else {
                myMove();
            }
            return;
        }

        //RADAR DETECTION
        freeze=false;
        for (IRadarResult o: detectRadar()){
            if (o.getObjectType()==IRadarResult.Types.OpponentMainBot || o.getObjectType()==IRadarResult.Types.OpponentSecondaryBot) {
                double enemyX=myX+o.getObjectDistance()*Math.cos(o.getObjectDirection());
                double enemyY=myY+o.getObjectDistance()*Math.sin(o.getObjectDirection());
                //broadcast(whoAmI+":"+TEAM+":"+FIRE+":"+enemyX+":"+enemyY+":"+OVER);
                broadcast(whoAmI+":"+TEAM+":"+DETECT+":"+enemyX+":"+enemyY+":"+OVER);
                escape = true;
                targetX = enemyX;
                targetY = enemyY;
            }
            if (o.getObjectDistance()<=100) {
                freeze=true;
            }
        }
        if (freeze) return;



        //AUTOMATON
        if (state==TURNLEFTTASK && !(isSameDirection(getHeading(),Parameters.NORTH))) {
            stepTurn(Parameters.Direction.LEFT);
            //sendLogMessage("Initial TeamA Secondary Bot1 position. Heading North!");
            return;
        }
        if (state==TURNLEFTTASK && isSameDirection(getHeading(),Parameters.NORTH)) {
            state=MOVETASK;
            myMove();
            //sendLogMessage("Moving a head. Waza!");
            return;
        }
        if (state==MOVETASK && detectFront().getObjectType()==IFrontSensorResult.Types.NOTHING) {
            myMove(); //And what to do when blind blocked?
            //sendLogMessage("Moving a head. Waza!");
            return;
        }
        if (state==MOVETASK && detectFront().getObjectType()!=IFrontSensorResult.Types.NOTHING) {
            state=TURNRIGHTTASK;
            oldAngle=getHeading();
            stepTurn(Parameters.Direction.RIGHT);
            //sendLogMessage("Iceberg at 12 o'clock. Heading 3!");
            return;
        }
        if (state==TURNRIGHTTASK && !(isSameDirection(getHeading(),oldAngle+Parameters.RIGHTTURNFULLANGLE))) {
            stepTurn(Parameters.Direction.RIGHT);
            //sendLogMessage("Iceberg at 12 o'clock. Heading 3!");
            return;
        }
        if (state==TURNRIGHTTASK && isSameDirection(getHeading(),oldAngle+Parameters.RIGHTTURNFULLANGLE)) {
            state=MOVETASK;
            myMove();
            //sendLogMessage("Moving a head. Waza!");
            return;
        }

        if (state==SINK) {
            myMove();
            return;
        }
        if (true) {
            return;
        }
    }
    private void myMove(){
        isMoving=true;
        move();
    }
    private boolean isSameDirection(double dir1, double dir2){
        return Math.abs(dir1-dir2)<ANGLEPRECISION;
    }

    private void process(String message){
        if(Integer.parseInt(message.split(":")[2])==DETECT) {
            escape = true;
            targetX=Double.parseDouble(message.split(":")[3]);
            targetY=Double.parseDouble(message.split(":")[4]);
        }
    }

    private double calculateAngle(double x1, double y1, double x2, double y2) {
        double deltaX = x2 - x1;
        double deltaY = y2 - y1;
        return Math.atan2(deltaY, deltaX) - Math.PI;
    }

    private double calculateDistance(double x1, double y1, double x2, double y2) {
        double deltaX = x2 - x1;
        double deltaY = y2 - y1;
        return Math.sqrt(deltaX * deltaX + deltaY * deltaY);
    }
}