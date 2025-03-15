package algorithms;

import characteristics.IRadarResult;
import characteristics.Parameters;
import robotsimulator.Brain;

public class MecaMouseMain extends Brain {
    //---PARAMETERS---//
    private static final double ANGLEPRECISION = 0.01;
    private static final double FIREANGLEPRECISION = Math.PI/(double)6;
    private static final int ALPHA = 0x1EADDA;
    private static final int BETA = 0x5EC0;
    private static final int GAMMA = 0x333;

    private enum INSTRUCTIONS {
        BASE, THREE_ALIVE, TWO_ALIVE, ONE_ALIVE
    }

    //---VARIABLES---//
    private boolean IsOnLeft;
    private INSTRUCTIONS instruction;
    private int whoAmI;
    private double myX, myY;


    //---OTHERS ROBOTS STATES---//
    private int countSecondaryAlive;
    private int countMainAlive;

    //---ENEMIES ROBOTS STATES---//


    //---CONSTRUCTORS---//
    public MecaMouseMain() { super(); }

    @Override
    public void activate() {
        init();
    }

    @Override
    public void step() {

        switch (instruction) {
            case INSTRUCTIONS.BASE:
                instruction_base();
                break;
            case INSTRUCTIONS.THREE_ALIVE :
                instruction_three_alive();
                break;
            case INSTRUCTIONS.TWO_ALIVE :
                instruction_two_alive();
                break;
            case INSTRUCTIONS.ONE_ALIVE :
                instruction_one_alive();
                break;
        }

    }

    //--- INITIALISATION FUNCTIONS --//

    private void init() {
        init_whoAmI();
        init_position();
    }

    private void init_whoAmI() {
        //ODOMETRY CODE
        whoAmI = GAMMA;
        for (IRadarResult o: detectRadar())
            if (isSameDirection(o.getObjectDirection(), Parameters.NORTH)) whoAmI=ALPHA;
        for (IRadarResult o: detectRadar())
            if (isSameDirection(o.getObjectDirection(),Parameters.SOUTH) && whoAmI!=GAMMA) whoAmI=BETA;

    }

    private void init_position() {
        if (whoAmI == GAMMA){
            myX=Parameters.teamAMainBot1InitX;
            myY=Parameters.teamAMainBot1InitY;
        } else {
            myX=Parameters.teamAMainBot2InitX;
            myY=Parameters.teamAMainBot2InitY;
        }
        if (whoAmI == ALPHA){
            myX=Parameters.teamAMainBot3InitX;
            myY=Parameters.teamAMainBot3InitY;
        }
    }

    //--- INSTRUCTIONS FUNCTIONS --//

    private void instruction_base() {

    }

    private void instruction_three_alive() {

    }

    private void instruction_two_alive() {

    }

    private void instruction_one_alive() {

    }

    //--- COMMUNICATION FUNCTIONS --//


    //--- FIRE FUNCTIONS --//
    private void firePosition(double x, double y) {

    }

    //--- DIRECTION FUNCTIONS --//

    private boolean isSameDirection(double dir1, double dir2){
        return Math.abs(normalizeRadian(dir1)-normalizeRadian(dir2))<ANGLEPRECISION;
    }

    private double normalizeRadian(double angle){
        double result = angle;
        while(result<0) result+=2*Math.PI;
        while(result>=2*Math.PI) result-=2*Math.PI;
        return result;
    }



}
