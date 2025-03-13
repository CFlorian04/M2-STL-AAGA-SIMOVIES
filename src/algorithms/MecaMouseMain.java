package algorithms;

import robotsimulator.Brain;
import characteristics.Parameters;
import characteristics.IFrontSensorResult;
import characteristics.IRadarResult;

import java.util.ArrayList;

public class MecaMouseMain extends Brain {
    //---PARAMETERS---//
    private static final double ANGLEPRECISION = 0.01;

    private static final int ALPHA = 0x1EADDA;
    private static final int BETA = 0x5EC0;
    private static final int GAMMA = 0x333;

    private enum TASK {

    }

    //---VARIABLES---//
    private int whoAmI;
    private double myX,myY;


    //---CONSTRUCTORS---//
    public MecaMouseMain() { super(); }

    //---ABSTRACT-METHODS-IMPLEMENTATION---//
    public void activate() {
        init_whoAmI();
        init_position();
        init_parameters();
    }

    public void step() {

    }

    private double normalizeRadian(double angle){
        double result = angle;
        while(result<0) result+=2*Math.PI;
        while(result>=2*Math.PI) result-=2*Math.PI;
        return result;
    }

    private boolean isSameDirection(double dir1, double dir2){
        return Math.abs(normalizeRadian(dir1)-normalizeRadian(dir2))<ANGLEPRECISION;
    }

    ////////////////////////////////////////////////
    //                INIT FUNCTIONS              //
    ////////////////////////////////////////////////
    private void init_whoAmI() {
        whoAmI = GAMMA;
        for (IRadarResult o: detectRadar())
            if (isSameDirection(o.getObjectDirection(),Parameters.NORTH)) whoAmI=ALPHA;
        for (IRadarResult o: detectRadar())
            if (isSameDirection(o.getObjectDirection(),Parameters.SOUTH) && whoAmI!=GAMMA) whoAmI=BETA;
    }

    private void init_position() {
        switch (whoAmI) {
            case GAMMA:
                myX=Parameters.teamAMainBot1InitX;
                myY=Parameters.teamAMainBot1InitY;
                break;
            case BETA:
                myX=Parameters.teamAMainBot2InitX;
                myY=Parameters.teamAMainBot2InitY;
                break;
            case ALPHA:
                myX=Parameters.teamAMainBot3InitX;
                myY=Parameters.teamAMainBot3InitY;
        }
    }

    private void init_parameters() {

    }

    ////////////////////////////////////////////////
}