package algorithms;

import java.util.ArrayList;
import java.util.Random;

import robotsimulator.Brain;
import robotsimulator.RadarResult;
import characteristics.IFrontSensorResult;
import characteristics.IRadarResult;
import characteristics.IRadarResult.Types;
import characteristics.Parameters;

public class SuperheroAMain extends Brain {

    // ---PARAMETERS---//
    private static final double HEADINGPRECISION = 0.001;
    private static final double ANGLEPRECISION = 0.1;
    private static final int BOTFIRE1 = 1;
    private static final int BOTFIRE2 = 2;
    private static final int BOTFIRE3 = 3;

    // ---VARIABLES---//
    private boolean turnTask;
    private boolean taskMoveAHead;
    private boolean taskTurnABit;
    private boolean moveBackTask;
    private boolean startingStage;
    private double endTaskDirection;
    private int whoAmI;
    private int stepNumberLastFire, stepNumberMoveBack;
    private int stepNumber;
    int random = 0;
    private IRadarResult ennemy;
    private Random gen;

    // ---CONSTRUCTORS---//
    public SuperheroAMain() {
        super();
        gen = new Random();
    }

    // ---ABSTRACT-METHODS-IMPLEMENTATION---//
    public void activate() {
        whoAmI = BOTFIRE1;
        sendLogMessage("i am BOTFIRE1");
        ArrayList<IRadarResult> res = detectRadar();
        for (int i = 0; i < res.size(); i++) {
            if (isSameDirection(res.get(i).getObjectDirection(), Parameters.NORTH)) {
                if (isSameDirection(res.get(i + 1).getObjectDirection(), Parameters.SOUTH)) {
                    whoAmI = BOTFIRE2;
                    sendLogMessage("i am BOTFIRE2");
                } else {
                    whoAmI = BOTFIRE3;
                    sendLogMessage("i am BOTFIRE3");
                }
            }
        }
        stepNumber = 0;
        stepNumberLastFire = 0;
        stepNumberMoveBack = 0;
        startingStage = true;
        turnTask = false;
        taskMoveAHead = false;
        moveBackTask = false;

        move();
    }

    public void step() {
        // stepNumber pour compter on es dans quelle "step",
        // chaque "step" on faire 1 seul action
        stepNumber++;

        // debut la guerre on "fire()"; jusqu'a step 2500 ou le premiere turn()
        // on arrete fire()
        if (stepNumber > 2500) {
            startingStage = false;
        }

        // PRIORITE: chercher ennemy
        ArrayList<IRadarResult> detected = detectRadar();
        ennemy = null;
        for (IRadarResult o : detected) {
            if (o.getObjectType() == Types.OpponentMainBot || o.getObjectType() == Types.OpponentSecondaryBot) {
                if (ennemy != null) {
                    if (o.getObjectDistance() < ennemy.getObjectDistance()) {
                        ennemy = o;
                    }
                } else {
                    ennemy = o;
                }
            }
        }

        // PRIORITE: cas on voir un ennemy, on tester si il y a un Objet au
        // millieur
        if (ennemy != null) {
            boolean hasMiddle = false;
            for (IRadarResult o : detected) {
                if (Math.abs(o.getObjectDirection() - ennemy.getObjectDirection()) < Math.PI / 12.0
                        && o.getObjectDistance() < ennemy.getObjectDistance() && o.getObjectType() != Types.BULLET
                        && !o.equals(ennemy)) {
                    hasMiddle = true;
                }
            }

            // cas il n'a pas Objet au millieur, on fire() (et move pendant le
            // temp on ne peut pas fire())
            if (!hasMiddle && stepNumber > stepNumberLastFire + Parameters.bulletFiringLatency) {
                fire(ennemy.getObjectDirection());
                stepNumberLastFire = stepNumber;
                return;
            }

            // cas il n'a pas Objet au millieur, on move pendant le temp on ne
            // peut pas fire()
            if (!hasMiddle && stepNumber <= stepNumberLastFire + Parameters.bulletFiringLatency) {
                move();
                return;
            }

        }

        // STEP fire() au dÃ©but: on fire avec le direction un peu random
        //if (stepNumber > 100 && startingStage && stepNumber > stepNumberLastFire + Parameters.bulletFiringLatency) {
        //fire(getHeading() + gen.nextDouble() * Math.PI / 6 - Math.PI / 12);
        //stepNumberLastFire = stepNumber;
        //return;
        //}

        // STEP turn 90 degrÃ©, aprÃ¨s turn on move
        if (turnTask) {
            startingStage = false;
            if (isHeading(endTaskDirection)) {
                turnTask = false;
                move();
                sendLogMessage("Moving a head. Waza!");
            } else {
                stepTurn(Parameters.Direction.LEFT);
                sendLogMessage("Iceberg at 12 o'clock. Heading to my nine!");
            }
            return;
        }

        // STEP moveBack: (pour ne pas bloquer)
        // chaque fois on veut turn, il faut moveBack 25 steps, apres on turn()
        if (moveBackTask) {
            if (stepNumber < stepNumberMoveBack + 25) {
                moveBack();
                return;
            } else {
                moveBackTask = false;
                turnTask = true;
                endTaskDirection = getHeading() + Parameters.LEFTTURNFULLANGLE;
                stepTurn(Parameters.Direction.LEFT);
                sendLogMessage("Iceberg at 12 o'clock. Heading to my nine!");
                return;
            }
        }

        // tester si il y a Objet proche, et c'est pas une balle
        // alors on appelle moveBackTask : moveback (25 step) et apres on turn()
        for (IRadarResult o : detected) {
            if (o.getObjectDistance() < 120 && o.getObjectType() != Types.BULLET) {
                moveBackTask = true;
                stepNumberMoveBack = stepNumber;
                moveBack();
                return;
            }
        }

        //ici: pas ennemy, pas Objet proche,rien devant ou ami devant
        // alors on move()
        if (detectFront().getObjectType() == IFrontSensorResult.Types.NOTHING ||
                detectFront().getObjectType() == IFrontSensorResult.Types.TeamMainBot ||
                detectFront().getObjectType() == IFrontSensorResult.Types.TeamSecondaryBot) {
            move();
            sendLogMessage("Moving a head. Waza!");
            return;
        } else { //ici: pas ennemy, pas Objet proche, mais il y a quelque chose devant, alors on turn()
            turnTask = true;
            endTaskDirection = getHeading() + Parameters.LEFTTURNFULLANGLE;
            stepTurn(Parameters.Direction.LEFT);
            sendLogMessage("Iceberg at 12 o'clock. Heading to my nine!");
        }
    }

    private boolean isHeading(double dir) {
        return Math.abs(Math.sin(getHeading() - dir)) < HEADINGPRECISION;
    }

    private boolean isSameDirection(double dir1, double dir2) {
        return Math.abs(dir1 - dir2) < ANGLEPRECISION;
    }
}