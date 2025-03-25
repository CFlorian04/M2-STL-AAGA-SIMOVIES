package algorithms;

import robotsimulator.Brain;
import characteristics.Parameters;
import characteristics.IRadarResult;
import java.util.ArrayList;

public class NeuroBotSecondary extends Brain {
    // --- CONSTANTES ---
    private static final double ANGLEPRECISION = 0.01;

    private static final int ALPHA   = 0x1EADDA;
    private static final int BETA    = 0x5EC0;
    private static final int GAMMA   = 0x333;
    private static final int ROCKY   = 0x1EADDB;
    private static final int MARIO   = 0x5ED0;
    private static final int TEAM    = 0xBAD1AD;
    private static final int UNDEFINED = 0xBADC0DE0;

    private static final int FIRE    = 0xB52;
    private static final int FALLBACK = 0xFA11BAC;
    private static final int ROGER   = 0x0C0C0C0C;
    private static final int OVER    = 0xC00010FF;
    private static final int COMM    = 0xC00120F3;

    // Tâches communes
    private static final int TURN_SOUTH_TASK = 1;
    private static final int MOVE_SOUTH_TASK = 2;
    private static final int TURN_NORTH_TASK = 3;
    private static final int MOVE_NORTH_TASK = 4;
    private static final int TURN_EAST_TASK  = 5;
    private static final int MOVE_EAST_TASK  = 6;
    private static final int TURN_WEST_TASK  = 7;
    private static final int MOVE_WEST_TASK  = 8;

    // Tâches d'initialisation Secondary
    private static final int ROCKY_INIT_TURN_TASK = 11;
    private static final int ROCKY_INIT_MOVE_TASK = 12;
    private static final int MARIO_INIT_TURN_TASK  = 13;
    private static final int MARIO_INIT_MOVE_TASK  = 14;

    // Tâches de déplacement
    private static final int MOVE_TASK      = 21;
    private static final int MOVE_BACK_TASK = 22;

    private static final int END_OF_FE_TASK_1 = 31;
    private static final int NOT_FIFTH_ELEMENT = 41;

    // Tâches de contournement – mêmes numéros dans les deux versions
    // Pour la partie "est" (nord-sud)
    private static final int TURN_NORTH_TO_BYBASS_EAST_TASK_1 = 61;
    private static final int TURN_NORTH_TO_BYBASS_EAST_TASK_2 = 62;
    private static final int TURN_NORTH_TO_BYBASS_EAST_TASK_3 = 63;
    private static final int TURN_NORTH_TO_BYBASS_EAST_TASK_4 = 64;
    private static final int TURN_NORTH_TO_BYBASS_EAST_TASK_5 = 65;
    private static final int TURN_NORTH_TO_BYBASS_EAST_TASK_6 = 66;
    private static final int TURN_NORTH_TO_BYBASS_EAST_TASK_7 = 67;
    private static final int TURN_SOUTH_TO_BYBASS_EAST_TASK_1 = 71;
    private static final int TURN_SOUTH_TO_BYBASS_EAST_TASK_2 = 72;
    private static final int TURN_SOUTH_TO_BYBASS_EAST_TASK_3 = 73;
    private static final int TURN_SOUTH_TO_BYBASS_EAST_TASK_4 = 74;
    private static final int TURN_SOUTH_TO_BYBASS_EAST_TASK_5 = 75;
    private static final int TURN_SOUTH_TO_BYBASS_EAST_TASK_6 = 76;
    private static final int TURN_SOUTH_TO_BYBASS_EAST_TASK_7 = 77;

    // Pour la partie "ouest" (nord-sud)
    private static final int TURN_NORTH_TO_BYBASS_WEST_TASK_1 = 81;
    private static final int TURN_NORTH_TO_BYBASS_WEST_TASK_2 = 82;
    private static final int TURN_NORTH_TO_BYBASS_WEST_TASK_3 = 83;
    private static final int TURN_NORTH_TO_BYBASS_WEST_TASK_4 = 84;
    private static final int TURN_NORTH_TO_BYBASS_WEST_TASK_5 = 85;
    private static final int TURN_NORTH_TO_BYBASS_WEST_TASK_6 = 86;
    private static final int TURN_NORTH_TO_BYBASS_WEST_TASK_7 = 87;
    private static final int TURN_SOUTH_TO_BYBASS_WEST_TASK_1 = 91;
    private static final int TURN_SOUTH_TO_BYBASS_WEST_TASK_2 = 92;
    private static final int TURN_SOUTH_TO_BYBASS_WEST_TASK_3 = 93;
    private static final int TURN_SOUTH_TO_BYBASS_WEST_TASK_4 = 94;
    private static final int TURN_SOUTH_TO_BYBASS_WEST_TASK_5 = 95;
    private static final int TURN_SOUTH_TO_BYBASS_WEST_TASK_6 = 96;
    private static final int TURN_SOUTH_TO_BYBASS_WEST_TASK_7 = 97;

    private static final int SINK = 0xBADC0DE1;

    private static final int DISTANCE_M_S = 400;

    // --- VARIABLES ---
    private int state;
    private double oldAngle;
    private double myX, myY;
    private boolean isMoving;
    private boolean isMovingBack;
    private int whoAmI;
    private ArrayList<ArrayList<Integer>> memberList = new ArrayList<ArrayList<Integer>>();
    private double targetX, targetY;
    private boolean freeze;
    private boolean Is_Fifth_Element = true;
    private boolean notFindR = false;
    private boolean notFindM = false;
    private int oldX, oldY;

    // Drapeau pour distinguer le comportement Secondary (Team A vs Team B)
    private boolean isTeamA;  // true = Team A Secondary, false = Team B Secondary

    // --- CONSTRUCTEUR ---
    public NeuroBotSecondary() {
        super();
    }

    // --- Cycle de vie ---
    public void activate() {
        logIn();
    }

    public void step() {
        // On appelle la fonction step selon l'équipe détectée
        if (isTeamA) {
            stepTeamA();
        } else {
            stepTeamB();
        }
    }

    // --- Détection du type de Secondary par nom de classe ---
    private void determineTeamByBrainClassName() {
        String paramA = Parameters.teamASecondaryBotBrainClassName;
        String paramB = Parameters.teamBSecondaryBotBrainClassName;

        if (paramA.equals("algorithms.NeuroBotSecondary")) {
            // Si paramA vaut "algorithms.Main", alors c'est Team A
            isTeamA = true;
            System.out.println("Je suis secondary A ");
            sendLogMessage("paramA = " + paramA + " donc Team A");
        } else if (paramB.equals("algorithms.NeuroBotSecondary")) {
            System.out.println("Je suis secondary B ");
            // Sinon, si paramB vaut "algorithms.Main", c'est Team B
            isTeamA = false;
            sendLogMessage("paramB = " + paramB + " donc Team B");
        } else {
            isTeamA = true;
            sendLogMessage("Aucune correspondance trouvée, affectation par défaut: Team A");
        }
    }

    // --- Initialisation ---
    private void logIn() {
        determineTeamByBrainClassName();
        if (isTeamA) {
            logIn_teamA();
        } else {
            logIn_teamB();
        }
    }

    private void logIn_teamA() {
        // Pour Team A Secondary
        whoAmI = ROCKY;
        for (IRadarResult o : detectRadar())
            if (isSameDirection(o.getObjectDirection(), Parameters.NORTH))
                whoAmI = MARIO;
        if (whoAmI == ROCKY) {
            myX = Parameters.teamASecondaryBot1InitX;
            myY = Parameters.teamASecondaryBot1InitY;
            state = ROCKY_INIT_TURN_TASK;
        } else {
            myX = Parameters.teamASecondaryBot2InitX;
            myY = Parameters.teamASecondaryBot2InitY;
            state = MARIO_INIT_TURN_TASK;
        }
        isMoving = false;
        isMovingBack = false;
        oldAngle = getHeading();
        memberList.clear();
        for (int i = 0; i < 5; i++) {
            ArrayList<Integer> tmp = new ArrayList<Integer>();
            for (int j = 0; j < 4; j++) {
                tmp.add(0);
            }
            memberList.add(tmp);
        }
    }

    private void logIn_teamB() {
        // Pour Team B Secondary
        whoAmI = ROCKY;
        for (IRadarResult o : detectRadar())
            if (isSameDirection(o.getObjectDirection(), Parameters.NORTH))
                whoAmI = MARIO;
        if (whoAmI == ROCKY) {
            myX = Parameters.teamBSecondaryBot1InitX;
            myY = Parameters.teamBSecondaryBot1InitY;
            state = ROCKY_INIT_TURN_TASK;
        } else {
            myX = Parameters.teamBSecondaryBot2InitX;
            myY = Parameters.teamBSecondaryBot2InitY;
            state = MARIO_INIT_TURN_TASK;
        }
        isMoving = false;
        isMovingBack = false;
        oldAngle = getHeading();
        memberList.clear();
        for (int i = 0; i < 5; i++) {
            ArrayList<Integer> tmp = new ArrayList<Integer>();
            for (int j = 0; j < 4; j++) {
                tmp.add(0);
            }
            memberList.add(tmp);
        }
    }

    // --- Step pour Team A Secondary ---
    public void stepTeamA() {
        // ODOMETRY CODE
        if (isMoving) {
            myX += Parameters.teamASecondaryBotSpeed * Math.cos(getHeading());
            myY += Parameters.teamASecondaryBotSpeed * Math.sin(getHeading());
            isMoving = false;
        }
        if (isMovingBack) {
            myX -= Parameters.teamASecondaryBotSpeed * Math.cos(getHeading());
            myY -= Parameters.teamASecondaryBotSpeed * Math.sin(getHeading());
            isMovingBack = false;
        }
        broadcast(whoAmI + "/" + TEAM + "/" + COMM + "/" + state + "/" + myX + "/" + myY + "/" + OVER);

        // DEBUG MESSAGE
        if (whoAmI == ROCKY)
            sendLogMessage("#ROCKY : (" + (int) myX + ", " + (int) myY + ")");
        else
            sendLogMessage("#MARIO : (" + (int) myX + ", " + (int) myY + ")");

        // COMMUNICATION
        broadcast(whoAmI + "/" + TEAM + "/" + COMM + "/" + state + "/" + myX + "/" + myY + "/" + OVER);
        ArrayList<String> messages = fetchAllMessages();
        for (String m : messages)
            if (Integer.parseInt(m.split("/")[1]) == TEAM)
                process(m);

        // RADAR DETECTION
        freeze = false;
        for (IRadarResult o : detectRadar()) {
            if (o.getObjectType() == IRadarResult.Types.OpponentMainBot
                    || o.getObjectType() == IRadarResult.Types.OpponentSecondaryBot) {
                double enemyX = myX + o.getObjectDistance() * Math.cos(o.getObjectDirection());
                double enemyY = myY + o.getObjectDistance() * Math.sin(o.getObjectDirection());
                broadcast(whoAmI + "/" + TEAM + "/" + FIRE + "/" + enemyX + "/" + enemyY + "/" + OVER);

                if (state == MOVE_TASK && enemyX>=myX) {
                    state = MOVE_BACK_TASK;
                }
                if (state == MOVE_BACK_TASK && enemyX<=myX) {
                    state = MOVE_TASK;
                }

                if (Is_Fifth_Element && whoAmI == ROCKY && ((int) enemyY <= 795 || (int) enemyY >= 805)) {
                    broadcast(whoAmI + "/" + TEAM + "/" + NOT_FIFTH_ELEMENT + "/" + OVER);
                }
                if (Is_Fifth_Element && whoAmI == MARIO && ((int) enemyY <= 1195 || (int) enemyY >= 1205)) {
                    broadcast(whoAmI + "/" + TEAM + "/" + NOT_FIFTH_ELEMENT + "/" + OVER);
                }
            }
            if (o.getObjectDistance() <= 100) {
                freeze = true;
            }

            if (o.getObjectType() == IRadarResult.Types.TeamMainBot || o.getObjectType() == IRadarResult.Types.TeamSecondaryBot) {
                double teamX = myX + o.getObjectDistance() * Math.cos(o.getObjectDirection());
                double teamY = myY + o.getObjectDistance() * Math.sin(o.getObjectDirection());
                int dist = distance(myX, myY, teamX, teamY);
                if (dist <= 150 && state == MOVE_TASK && teamX>=myX) {
                    state = MOVE_BACK_TASK;
                }
                if (dist <= 150 && state == MOVE_BACK_TASK && teamX<=myX) {
                    state = MOVE_TASK;
                }
            }

            if (!Is_Fifth_Element) {
                if (o.getObjectType() == IRadarResult.Types.Wreck) {
                    int wreckX = (int) (myX + o.getObjectDistance() * Math.cos(o.getObjectDirection()));
                    int wreckY = (int) (myY + o.getObjectDistance() * Math.sin(o.getObjectDirection()));
                    int dist = distance(myX, myY, wreckX, wreckY);
                    if (state == MOVE_TASK) {
                        if (dist < 150 && wreckY >= myY - 100 && wreckY <= myY + 100 && wreckX>=myX) {
                            if (wreckY >= myY) {
                                state = TURN_NORTH_TO_BYBASS_EAST_TASK_1;
                            } else {
                                state = TURN_SOUTH_TO_BYBASS_EAST_TASK_1;
                            }
                        }
                    } else if (state == MOVE_BACK_TASK) {
                        if (dist < 150 && wreckY >= myY - 100 && wreckY <= myY + 100 && wreckX<=myX) {
                            if (wreckY >= myY) {
                                state = TURN_NORTH_TO_BYBASS_WEST_TASK_1;
                            } else {
                                state = TURN_SOUTH_TO_BYBASS_WEST_TASK_1;
                            }
                        }
                    }
                }
            }

        }
        if (freeze)
            return;
        goPlaces();
        bypassWreck_teamA();
        if (state == TURN_EAST_TASK && !(isSameDirection(getHeading(), Parameters.EAST))) {
            if (whoAmI==ROCKY)
                stepTurn(Parameters.Direction.RIGHT);
            if (whoAmI==MARIO)
                stepTurn(Parameters.Direction.LEFT);
            return;
        }
        if (state == TURN_EAST_TASK && isSameDirection(getHeading(), Parameters.EAST)) {
            state = MOVE_TASK;
            myMove();
            return;
        }

        int maxX = 0, minX = 3000;
        for (int i = 0; i < 3; i++) {
            if (memberList.get(i).get(2) > maxX) {
                maxX = memberList.get(i).get(2);
            }
            if (memberList.get(i).get(2) < minX) {
                minX = memberList.get(i).get(2);
            }
        }
        if (state == MOVE_TASK && myX < maxX + 500) {
            myMove();
            return;
        }
        if (state == MOVE_TASK && myX >= maxX + 500) {
            state = MOVE_BACK_TASK;
            myMoveBack();
            return;
        }
        if (state == MOVE_BACK_TASK && myX > minX - 150) {
            myMoveBack();
            return;
        }
        if (state == MOVE_BACK_TASK && myX <= minX - 150) {
            state = MOVE_TASK;
            myMove();
            return;
        }
        if (state == MOVE_BACK_TASK && myX <= 55) {
            state = MOVE_TASK;
            myMove();
            return;
        }
        if (state == MOVE_TASK && myX >= 2945) {
            state = MOVE_BACK_TASK;
            myMoveBack();
            return;
        }
        if (true) {
            return;
        }
    }
    // --- Step pour Team B Secondary ---
    public void stepTeamB() {
        // ODOMETRY CODE
        if (isMoving) {
            myX += Parameters.teamASecondaryBotSpeed * Math.cos(getHeading());
            myY += Parameters.teamASecondaryBotSpeed * Math.sin(getHeading());
            isMoving = false;
        }
        if (isMovingBack) {
            myX -= Parameters.teamASecondaryBotSpeed * Math.cos(getHeading());
            myY -= Parameters.teamASecondaryBotSpeed * Math.sin(getHeading());
            isMovingBack = false;
        }
        broadcast(whoAmI + "/" + TEAM + "/" + COMM + "/" + state + "/" + myX + "/" + myY + "/" + OVER);

        // DEBUG MESSAGE
        if (whoAmI == ROCKY)
            sendLogMessage("#ROCKY : (" + (int) myX + ", " + (int) myY + ")");
        else
            sendLogMessage("#MARIO : (" + (int) myX + ", " + (int) myY + ")");

        // COMMUNICATION
        broadcast(whoAmI + "/" + TEAM + "/" + COMM + "/" + state + "/" + myX + "/" + myY + "/" + OVER);
        ArrayList<String> messages = fetchAllMessages();
        for (String m : messages)
            if (Integer.parseInt(m.split("/")[1]) == TEAM)
                process(m);

        // RADAR DETECTION
        freeze = false;
        for (IRadarResult o : detectRadar()) {
            if (o.getObjectType() == IRadarResult.Types.OpponentMainBot
                    || o.getObjectType() == IRadarResult.Types.OpponentSecondaryBot) {
                double enemyX = myX + o.getObjectDistance() * Math.cos(o.getObjectDirection());
                double enemyY = myY + o.getObjectDistance() * Math.sin(o.getObjectDirection());
                broadcast(whoAmI + "/" + TEAM + "/" + FIRE + "/" + enemyX + "/" + enemyY + "/" + OVER);

                if (state == MOVE_TASK && enemyX<=myX) {
                    state = MOVE_BACK_TASK;
                }
                if (state == MOVE_BACK_TASK && enemyX>=myX) {
                    state = MOVE_TASK;
                }

                if (Is_Fifth_Element && whoAmI == ROCKY && ((int) enemyY <= 795 || (int) enemyY >= 805)) {
                    broadcast(whoAmI + "/" + TEAM + "/" + NOT_FIFTH_ELEMENT + "/" + OVER);
                }
                if (Is_Fifth_Element && whoAmI == MARIO && ((int) enemyY <= 1195 || (int) enemyY >= 1205)) {
                    broadcast(whoAmI + "/" + TEAM + "/" + NOT_FIFTH_ELEMENT + "/" + OVER);
                }
            }
            if (o.getObjectDistance() <= 100) { // TODO : if <=300 move back
                freeze = true;
            }

            if (o.getObjectType() == IRadarResult.Types.TeamMainBot || o.getObjectType() == IRadarResult.Types.TeamSecondaryBot) {
                double teamX = myX + o.getObjectDistance() * Math.cos(o.getObjectDirection());
                double teamY = myY + o.getObjectDistance() * Math.sin(o.getObjectDirection());
                int dist = distance(myX, myY, teamX, teamY);
                if (dist <= 150 && state == MOVE_TASK && teamX<=myX) {
                    state = MOVE_BACK_TASK;
                }
                if (dist <= 150 && state == MOVE_BACK_TASK && teamX>=myX) {
                    state = MOVE_TASK;
                }
            }

            if (!Is_Fifth_Element) {
                if (o.getObjectType() == IRadarResult.Types.Wreck) {
                    int wreckX = (int) (myX + o.getObjectDistance() * Math.cos(o.getObjectDirection()));
                    int wreckY = (int) (myY + o.getObjectDistance() * Math.sin(o.getObjectDirection()));
                    int dist = distance(myX, myY, wreckX, wreckY);
                    if (state == MOVE_TASK) {
                        if (dist <= 150 && wreckY >= myY - 100 && wreckY <= myY + 100 && wreckX<=myX) {
                            if (wreckY >= myY) {
                                state = TURN_NORTH_TO_BYBASS_WEST_TASK_1;
                            } else {
                                state = TURN_SOUTH_TO_BYBASS_WEST_TASK_1;
                            }
                        }
                    } else if (state == MOVE_BACK_TASK) {
                        if (dist <= 150 && wreckY >= myY - 100 && wreckY <= myY + 100 && wreckX>myX) {
                            if (wreckY >= myY) {
                                state = TURN_NORTH_TO_BYBASS_EAST_TASK_1;
                            } else {
                                state = TURN_SOUTH_TO_BYBASS_EAST_TASK_1;
                            }
                        }
                    }
                }
            }

        }
        if (freeze)
            return;
        goPlaces();
        bypassWreck_teamB();
        if (state == TURN_WEST_TASK && !(isSameDirection(getHeading(), Parameters.WEST))) {
            if (whoAmI==ROCKY)
                stepTurn(Parameters.Direction.LEFT);
            if (whoAmI==MARIO)
                stepTurn(Parameters.Direction.RIGHT);
            return;
        }
        if (state == TURN_WEST_TASK && isSameDirection(getHeading(), Parameters.WEST)) {
            state = MOVE_TASK;
            myMove();
            return;
        }

        int maxX = 0, minX = 3000;
        for (int i = 0; i < 3; i++) {
            if (memberList.get(i).get(2) > maxX) {
                maxX = memberList.get(i).get(2);
            }
            if (memberList.get(i).get(2) < minX) {
                minX = memberList.get(i).get(2);
            }
        }
        if (state == MOVE_TASK && myX > minX - 500) {
            myMove();
            return;
        }
        if (state == MOVE_TASK && myX <= minX - 500) {
            state = MOVE_BACK_TASK;
            myMoveBack();
            return;
        }
        if (state == MOVE_BACK_TASK && myX < maxX + 150) {
            myMoveBack();
            return;
        }
        if (state == MOVE_BACK_TASK && myX >= maxX + 150) {
            state = MOVE_TASK;
            myMove();
            return;
        }
        if (state == MOVE_BACK_TASK && myX >= 2945) {
            state = MOVE_TASK;
            myMove();
            return;
        }
        if (state == MOVE_TASK && myX <= 55) {
            state = MOVE_BACK_TASK;
            myMoveBack();
            return;
        }
        if (true) {
            return;
        }
    }
    // --- Méthodes utilitaires ---
    public double getCurrentX() {
        return myX;
    }

    private void myMove() {
        isMoving = true;
        move();
    }

    private void myMoveBack() {
        isMovingBack = true;
        moveBack();
    }

    private double myGetHeading() {
        return normalizeRadian(getHeading());
    }

    private double normalizeRadian(double angle) {
        double result = angle;
        while (result < 0)
            result += 2 * Math.PI;
        while (result >= 2 * Math.PI)
            result -= 2 * Math.PI;
        return result;
    }

    private boolean isSameDirection(double dir1, double dir2) {
        return Math.abs(normalizeRadian(dir1) - normalizeRadian(dir2)) < ANGLEPRECISION;
    }

    private void process(String message) {
        if (Integer.parseInt(message.split("/")[2]) == COMM) {
            int i = -1;
            int sender = Integer.parseInt(message.split("/")[0]);
            if (sender == GAMMA)
                i = 0;
            else if (sender == BETA)
                i = 1;
            else if (sender == ALPHA)
                i = 2;
            else if (sender == ROCKY)
                i = 3;
            else if (sender == MARIO)
                i = 4;
            memberList.get(i).set(0, (int) Double.parseDouble(message.split("/")[0]));
            memberList.get(i).set(1, (int) Double.parseDouble(message.split("/")[3]));
            memberList.get(i).set(2, (int) Double.parseDouble(message.split("/")[4]));
            memberList.get(i).set(3, (int) Double.parseDouble(message.split("/")[5]));
        } else if (Integer.parseInt(message.split("/")[2]) == NOT_FIFTH_ELEMENT) {
            Is_Fifth_Element = false;
        }
    }
    private int distance(double x, double y, double x1, double y1) {
        return (int) Math.sqrt(Math.abs((x - x1) * (x - x1) + (y - y1) * (y - y1)));
    }
    private void goPlaces() {
        if (isTeamA) {
            goPlacesteamA();
        } else {
            goPlacesteamB();
        }
    }
    private void goPlacesteamB() {
        int max = memberList.get(0).get(3);
        int min = memberList.get(0).get(3);
        for (int i = 1; i < 3; i++) {
            if (memberList.get(i).get(3) > max) {
                max = memberList.get(i).get(3);
            }
            if (memberList.get(i).get(3) < min) {
                min = memberList.get(i).get(3);
            }
        }
        max += DISTANCE_M_S;
        min -= DISTANCE_M_S;

        if (state == ROCKY_INIT_TURN_TASK && !(isSameDirection(getHeading(), Parameters.NORTH))) {
            stepTurn(Parameters.Direction.RIGHT);
            return;
        }
        if (state == ROCKY_INIT_TURN_TASK && isSameDirection(getHeading(), Parameters.NORTH)) {
            state = ROCKY_INIT_MOVE_TASK;
            myMove();
            return;
        }
        if (state == ROCKY_INIT_MOVE_TASK && myY > min) {
            myMove();
            return;
        }
        if (state == ROCKY_INIT_MOVE_TASK && myY <= min) {
            state = TURN_WEST_TASK;
            stepTurn(Parameters.Direction.LEFT);
            return;
        }

        if (state == MARIO_INIT_TURN_TASK && !(isSameDirection(getHeading(), Parameters.SOUTH))) {
            stepTurn(Parameters.Direction.LEFT);
            return;
        }
        if (state == MARIO_INIT_TURN_TASK && isSameDirection(getHeading(), Parameters.SOUTH)) {
            state = MARIO_INIT_MOVE_TASK;
            myMove();
            return;
        }
        if (state == MARIO_INIT_MOVE_TASK && myY < max) {
            myMove();
            return;
        }
        if (state == MARIO_INIT_MOVE_TASK && myY >= max) {
            state = TURN_WEST_TASK;
            stepTurn(Parameters.Direction.RIGHT);
            return;
        }

    }
    private void goPlacesteamA() {
        int max = memberList.get(0).get(3);
        int min = memberList.get(0).get(3);
        for (int i = 1; i < 3; i++) {
            if (memberList.get(i).get(3) > max) {
                max = memberList.get(i).get(3);
            }
            if (memberList.get(i).get(3) < min) {
                min = memberList.get(i).get(3);
            }
        }
        max += DISTANCE_M_S;
        min -= DISTANCE_M_S;

        if (state == ROCKY_INIT_TURN_TASK && !(isSameDirection(getHeading(), Parameters.NORTH))) {
            stepTurn(Parameters.Direction.LEFT);
            return;
        }
        if (state == ROCKY_INIT_TURN_TASK && isSameDirection(getHeading(), Parameters.NORTH)) {
            state = ROCKY_INIT_MOVE_TASK;
            myMove();
            return;
        }
        if (state == ROCKY_INIT_MOVE_TASK && myY > min) {
            myMove();
            return;
        }
        if (state == ROCKY_INIT_MOVE_TASK && myY <= min) {
            state = TURN_EAST_TASK;
            stepTurn(Parameters.Direction.RIGHT);
            return;
        }

        if (state == MARIO_INIT_TURN_TASK && !(isSameDirection(getHeading(), Parameters.SOUTH))) {
            stepTurn(Parameters.Direction.RIGHT);
            return;
        }
        if (state == MARIO_INIT_TURN_TASK && isSameDirection(getHeading(), Parameters.SOUTH)) {
            state = MARIO_INIT_MOVE_TASK;
            myMove();
            return;
        }
        if (state == MARIO_INIT_MOVE_TASK && myY < max) {
            myMove();
            return;
        }
        if (state == MARIO_INIT_MOVE_TASK && myY >= max) {
            state = TURN_EAST_TASK;
            stepTurn(Parameters.Direction.LEFT);
            return;
        }
    }

    // --- Bypass methods pour Secondary Team A ---
    private void bypassWreck_teamA() {
        bypassWreck_N_E_teamA();
        bypassWreck_S_E_teamA();
        bypassWreck_N_W_teamA();
        bypassWreck_S_W_teamA();
    }
    private void bypassWreck_N_E_teamA() {
        if (state == TURN_NORTH_TO_BYBASS_EAST_TASK_1 && !isSameDirection(getHeading(), Parameters.NORTH)) {
            stepTurn(Parameters.Direction.LEFT);
            return;
        }
        if (state == TURN_NORTH_TO_BYBASS_EAST_TASK_1 && isSameDirection(getHeading(), Parameters.NORTH)) {
            state = TURN_NORTH_TO_BYBASS_EAST_TASK_2;
            oldY = (int) myY;
            myMove();
            return;
        }
        if (state == TURN_NORTH_TO_BYBASS_EAST_TASK_2 && oldY - myY <= 100) {
            myMove();
            return;
        }
        if (state == TURN_NORTH_TO_BYBASS_EAST_TASK_2 && oldY - myY > 100) {
            stepTurn(Parameters.Direction.RIGHT);
            state = TURN_NORTH_TO_BYBASS_EAST_TASK_3;
            return;
        }
        if (state == TURN_NORTH_TO_BYBASS_EAST_TASK_3 && !isSameDirection(getHeading(), Parameters.EAST)) {
            stepTurn(Parameters.Direction.RIGHT);
            return;
        }
        if (state == TURN_NORTH_TO_BYBASS_EAST_TASK_3 && isSameDirection(getHeading(), Parameters.EAST)) {
            state = TURN_NORTH_TO_BYBASS_EAST_TASK_4;
            oldX = (int) myX;
            myMove();
            return;
        }
        if (state == TURN_NORTH_TO_BYBASS_EAST_TASK_4 && myX - oldX <= 350) {
            myMove();
            return;
        }
        if (state == TURN_NORTH_TO_BYBASS_EAST_TASK_4 && myX - oldX > 350) {
            stepTurn(Parameters.Direction.RIGHT);
            state = TURN_NORTH_TO_BYBASS_EAST_TASK_5;
            return;
        }
        if (state == TURN_NORTH_TO_BYBASS_EAST_TASK_5 && !isSameDirection(getHeading(), Parameters.SOUTH)) {
            stepTurn(Parameters.Direction.RIGHT);
            return;
        }
        if (state == TURN_NORTH_TO_BYBASS_EAST_TASK_5 && isSameDirection(getHeading(), Parameters.SOUTH)) {
            state = TURN_NORTH_TO_BYBASS_EAST_TASK_6;
            oldY = (int) myY;
            myMove();
            return;
        }
        if (state == TURN_NORTH_TO_BYBASS_EAST_TASK_6 && myY - oldY <= 100) {
            myMove();
            return;
        }
        if (state == TURN_NORTH_TO_BYBASS_EAST_TASK_6 && myY - oldY > 100) {
            stepTurn(Parameters.Direction.LEFT);
            state = TURN_NORTH_TO_BYBASS_EAST_TASK_7;
            return;
        }
        if (state == TURN_NORTH_TO_BYBASS_EAST_TASK_7 && !isSameDirection(getHeading(), Parameters.EAST)) {
            stepTurn(Parameters.Direction.LEFT);
            return;
        }
        if (state == TURN_NORTH_TO_BYBASS_EAST_TASK_7 && isSameDirection(getHeading(), Parameters.EAST)) {
            state = MOVE_TASK;
            return;
        }
    }
    private void bypassWreck_S_E_teamA() {
        if (state == TURN_SOUTH_TO_BYBASS_EAST_TASK_1 && !isSameDirection(getHeading(), Parameters.SOUTH)) {
            stepTurn(Parameters.Direction.RIGHT);
            return;
        }
        if (state == TURN_SOUTH_TO_BYBASS_EAST_TASK_1 && isSameDirection(getHeading(), Parameters.SOUTH)) {
            state = TURN_SOUTH_TO_BYBASS_EAST_TASK_2;
            oldY = (int) myY;
            myMove();
            return;
        }
        if (state == TURN_SOUTH_TO_BYBASS_EAST_TASK_2 && myY - oldY <= 100) {
            myMove();
            return;
        }
        if (state == TURN_SOUTH_TO_BYBASS_EAST_TASK_2 && myY - oldY > 100) {
            stepTurn(Parameters.Direction.LEFT);
            state = TURN_SOUTH_TO_BYBASS_EAST_TASK_3;
            return;
        }
        if (state == TURN_SOUTH_TO_BYBASS_EAST_TASK_3 && !isSameDirection(getHeading(), Parameters.EAST)) {
            stepTurn(Parameters.Direction.LEFT);
            return;
        }
        if (state == TURN_SOUTH_TO_BYBASS_EAST_TASK_3 && isSameDirection(getHeading(), Parameters.EAST)) {
            state = TURN_SOUTH_TO_BYBASS_EAST_TASK_4;
            oldX = (int) myX;
            myMove();
            return;
        }
        if (state == TURN_SOUTH_TO_BYBASS_EAST_TASK_4 && myX - oldX <= 350) {
            myMove();
            return;
        }
        if (state == TURN_SOUTH_TO_BYBASS_EAST_TASK_4 && myX - oldX > 350) {
            stepTurn(Parameters.Direction.LEFT);
            state = TURN_SOUTH_TO_BYBASS_EAST_TASK_5;
            return;
        }
        if (state == TURN_SOUTH_TO_BYBASS_EAST_TASK_5 && !isSameDirection(getHeading(), Parameters.NORTH)) {
            stepTurn(Parameters.Direction.LEFT);
            return;
        }
        if (state == TURN_SOUTH_TO_BYBASS_EAST_TASK_5 && isSameDirection(getHeading(), Parameters.NORTH)) {
            state = TURN_SOUTH_TO_BYBASS_EAST_TASK_6;
            oldY = (int) myY;
            myMove();
            return;
        }
        if (state == TURN_SOUTH_TO_BYBASS_EAST_TASK_6 && myY - oldY <= 100) {
            myMove();
            return;
        }
        if (state == TURN_SOUTH_TO_BYBASS_EAST_TASK_6 && myY - oldY > 100) {
            stepTurn(Parameters.Direction.RIGHT);
            state = TURN_SOUTH_TO_BYBASS_EAST_TASK_7;
            return;
        }
        if (state == TURN_SOUTH_TO_BYBASS_EAST_TASK_7 && !isSameDirection(getHeading(), Parameters.EAST)) {
            stepTurn(Parameters.Direction.RIGHT);
            return;
        }
        if (state == TURN_SOUTH_TO_BYBASS_EAST_TASK_7 && isSameDirection(getHeading(), Parameters.EAST)) {
            state = MOVE_TASK;
            return;
        }
    }

    private void bypassWreck_N_W_teamA() {
        if (state == TURN_NORTH_TO_BYBASS_WEST_TASK_1 && !isSameDirection(getHeading(), Parameters.NORTH)) {
            stepTurn(Parameters.Direction.LEFT);
            return;
        }
        if (state == TURN_NORTH_TO_BYBASS_WEST_TASK_1 && isSameDirection(getHeading(), Parameters.NORTH)) {
            state = TURN_NORTH_TO_BYBASS_WEST_TASK_2;
            oldY = (int) myY;
            myMove();
            return;
        }
        if (state == TURN_NORTH_TO_BYBASS_WEST_TASK_2 && oldY - myY <= 100) {
            myMove();
            return;
        }
        if (state == TURN_NORTH_TO_BYBASS_WEST_TASK_2 && oldY - myY > 100) {
            stepTurn(Parameters.Direction.LEFT);
            state = TURN_NORTH_TO_BYBASS_WEST_TASK_3;
            return;
        }
        if (state == TURN_NORTH_TO_BYBASS_WEST_TASK_3 && !isSameDirection(getHeading(), Parameters.WEST)) {
            stepTurn(Parameters.Direction.LEFT);
            return;
        }
        if (state == TURN_NORTH_TO_BYBASS_WEST_TASK_3 && isSameDirection(getHeading(), Parameters.WEST)) {
            state = TURN_NORTH_TO_BYBASS_WEST_TASK_4;
            oldX = (int) myX;
            myMove();
            return;
        }
        if (state == TURN_NORTH_TO_BYBASS_WEST_TASK_4 && oldX - myX <= 350) {
            myMove();
            return;
        }
        if (state == TURN_NORTH_TO_BYBASS_WEST_TASK_4 && oldX - myX > 350) {
            stepTurn(Parameters.Direction.LEFT);
            state = TURN_NORTH_TO_BYBASS_WEST_TASK_5;
            return;
        }
        if (state == TURN_NORTH_TO_BYBASS_WEST_TASK_5 && !isSameDirection(getHeading(), Parameters.SOUTH)) {
            stepTurn(Parameters.Direction.LEFT);
            return;
        }
        if (state == TURN_NORTH_TO_BYBASS_WEST_TASK_5 && isSameDirection(getHeading(), Parameters.SOUTH)) {
            state = TURN_NORTH_TO_BYBASS_WEST_TASK_6;
            oldY = (int) myY;
            myMove();
            return;
        }
        if (state == TURN_NORTH_TO_BYBASS_WEST_TASK_6 && myY - oldY <= 100) {
            myMove();
            return;
        }
        if (state == TURN_NORTH_TO_BYBASS_WEST_TASK_6 && myY - oldY > 100) {
            stepTurn(Parameters.Direction.LEFT);
            state = TURN_NORTH_TO_BYBASS_WEST_TASK_7;
            return;
        }
        if (state == TURN_NORTH_TO_BYBASS_WEST_TASK_7 && !isSameDirection(getHeading(), Parameters.EAST)) {
            stepTurn(Parameters.Direction.LEFT);
            return;
        }
        if (state == TURN_NORTH_TO_BYBASS_WEST_TASK_7 && isSameDirection(getHeading(), Parameters.EAST)) {
            state = MOVE_BACK_TASK;
            return;
        }
    }

    private void bypassWreck_S_W_teamA() {
        if (state == TURN_SOUTH_TO_BYBASS_WEST_TASK_1 && !isSameDirection(getHeading(), Parameters.SOUTH)) {
            stepTurn(Parameters.Direction.RIGHT);
            return;
        }
        if (state == TURN_SOUTH_TO_BYBASS_WEST_TASK_1 && isSameDirection(getHeading(), Parameters.SOUTH)) {
            state = TURN_SOUTH_TO_BYBASS_WEST_TASK_2;
            oldY = (int) myY;
            myMove();
            return;
        }
        if (state == TURN_SOUTH_TO_BYBASS_WEST_TASK_2 && myY - oldY <= 100) {
            myMove();
            return;
        }
        if (state == TURN_SOUTH_TO_BYBASS_WEST_TASK_2 && myY - oldY > 100) {
            stepTurn(Parameters.Direction.RIGHT);
            state = TURN_SOUTH_TO_BYBASS_WEST_TASK_3;
            return;
        }
        if (state == TURN_SOUTH_TO_BYBASS_WEST_TASK_3 && !isSameDirection(getHeading(), Parameters.WEST)) {
            stepTurn(Parameters.Direction.RIGHT);
            return;
        }
        if (state == TURN_SOUTH_TO_BYBASS_WEST_TASK_3 && isSameDirection(getHeading(), Parameters.WEST)) {
            state = TURN_SOUTH_TO_BYBASS_WEST_TASK_4;
            oldX = (int) myX;
            myMove();
            return;
        }
        if (state == TURN_SOUTH_TO_BYBASS_WEST_TASK_4 && oldX - myX <= 350) {
            myMove();
            return;
        }
        if (state == TURN_SOUTH_TO_BYBASS_WEST_TASK_4 && oldX - myX > 350) {
            stepTurn(Parameters.Direction.RIGHT);
            state = TURN_SOUTH_TO_BYBASS_WEST_TASK_5;
            return;
        }
        if (state == TURN_SOUTH_TO_BYBASS_WEST_TASK_5 && !isSameDirection(getHeading(), Parameters.NORTH)) {
            stepTurn(Parameters.Direction.RIGHT);
            return;
        }
        if (state == TURN_SOUTH_TO_BYBASS_WEST_TASK_5 && isSameDirection(getHeading(), Parameters.NORTH)) {
            state = TURN_SOUTH_TO_BYBASS_WEST_TASK_6;
            oldY = (int) myY;
            myMove();
            return;
        }
        if (state == TURN_SOUTH_TO_BYBASS_WEST_TASK_6 && myY - oldY <= 100) {
            myMove();
            return;
        }
        if (state == TURN_SOUTH_TO_BYBASS_WEST_TASK_6 && myY - oldY > 100) {
            stepTurn(Parameters.Direction.LEFT);
            state = TURN_SOUTH_TO_BYBASS_WEST_TASK_7;
            return;
        }
        if (state == TURN_SOUTH_TO_BYBASS_WEST_TASK_7 && !isSameDirection(getHeading(), Parameters.WEST)) {
            stepTurn(Parameters.Direction.LEFT);
            return;
        }
        if (state == TURN_SOUTH_TO_BYBASS_WEST_TASK_7 && isSameDirection(getHeading(), Parameters.WEST)) {
            state = MOVE_TASK;
            return;
        }
    }
    // --- Bypass methods pour Secondary Team B ---
    private void bypassWreck_teamB() {
        bypassWreck_N_E_teamB();
        bypassWreck_S_E_teamB();
        bypassWreck_N_W_teamB();
        bypassWreck_S_W_teamB();
    }
    private void bypassWreck_N_E_teamB() {
        if (state == TURN_NORTH_TO_BYBASS_EAST_TASK_1 && !isSameDirection(getHeading(), Parameters.NORTH)) {
            stepTurn(Parameters.Direction.RIGHT);
            return;
        }
        if (state == TURN_NORTH_TO_BYBASS_EAST_TASK_1 && isSameDirection(getHeading(), Parameters.NORTH)) {
            state = TURN_NORTH_TO_BYBASS_EAST_TASK_2;
            oldY = (int) myY;
            myMove();
            return;
        }
        if (state == TURN_NORTH_TO_BYBASS_EAST_TASK_2 && oldY - myY <= 100) {
            myMove();
            return;
        }
        if (state == TURN_NORTH_TO_BYBASS_EAST_TASK_2 && oldY - myY > 100) {
            stepTurn(Parameters.Direction.RIGHT);
            state = TURN_NORTH_TO_BYBASS_EAST_TASK_3;
            return;
        }
        if (state == TURN_NORTH_TO_BYBASS_EAST_TASK_3 && !isSameDirection(getHeading(), Parameters.EAST)) {
            stepTurn(Parameters.Direction.RIGHT);
            return;
        }
        if (state == TURN_NORTH_TO_BYBASS_EAST_TASK_3 && isSameDirection(getHeading(), Parameters.EAST)) {
            state = TURN_NORTH_TO_BYBASS_EAST_TASK_4;
            oldX = (int) myX;
            myMove();
            return;
        }
        if (state == TURN_NORTH_TO_BYBASS_EAST_TASK_4 && myX - oldX <= 350) {
            myMove();
            return;
        }
        if (state == TURN_NORTH_TO_BYBASS_EAST_TASK_4 && myX - oldX > 350) {
            stepTurn(Parameters.Direction.RIGHT);
            state = TURN_NORTH_TO_BYBASS_EAST_TASK_5;
            return;
        }
        if (state == TURN_NORTH_TO_BYBASS_EAST_TASK_5 && !isSameDirection(getHeading(), Parameters.SOUTH)) {
            stepTurn(Parameters.Direction.RIGHT);
            return;
        }
        if (state == TURN_NORTH_TO_BYBASS_EAST_TASK_5 && isSameDirection(getHeading(), Parameters.SOUTH)) {
            state = TURN_NORTH_TO_BYBASS_EAST_TASK_6;
            oldY = (int) myY;
            myMove();
            return;
        }
        if (state == TURN_NORTH_TO_BYBASS_EAST_TASK_6 && myY - oldY <= 100) {
            myMove();
            return;
        }
        if (state == TURN_NORTH_TO_BYBASS_EAST_TASK_6 && myY - oldY > 100) {
            stepTurn(Parameters.Direction.RIGHT);
            state = TURN_NORTH_TO_BYBASS_EAST_TASK_7;
            return;
        }
        if (state == TURN_NORTH_TO_BYBASS_EAST_TASK_7 && !isSameDirection(getHeading(), Parameters.WEST)) {
            stepTurn(Parameters.Direction.RIGHT);
            return;
        }
        if (state == TURN_NORTH_TO_BYBASS_EAST_TASK_7 && isSameDirection(getHeading(), Parameters.WEST)) {
            state = MOVE_BACK_TASK;
            return;
        }
    }

    private void bypassWreck_S_E_teamB() {
        if (state == TURN_SOUTH_TO_BYBASS_EAST_TASK_1 && !isSameDirection(getHeading(), Parameters.SOUTH)) {
            stepTurn(Parameters.Direction.LEFT);
            return;
        }
        if (state == TURN_SOUTH_TO_BYBASS_EAST_TASK_1 && isSameDirection(getHeading(), Parameters.SOUTH)) {
            state = TURN_SOUTH_TO_BYBASS_EAST_TASK_2;
            oldY = (int) myY;
            myMove();
            return;
        }
        if (state == TURN_SOUTH_TO_BYBASS_EAST_TASK_2 && myY - oldY <= 100) {
            myMove();
            return;
        }
        if (state == TURN_SOUTH_TO_BYBASS_EAST_TASK_2 && myY - oldY > 100) {
            stepTurn(Parameters.Direction.LEFT);
            state = TURN_SOUTH_TO_BYBASS_EAST_TASK_3;
            return;
        }
        if (state == TURN_SOUTH_TO_BYBASS_EAST_TASK_3 && !isSameDirection(getHeading(), Parameters.EAST)) {
            stepTurn(Parameters.Direction.LEFT);
            return;
        }
        if (state == TURN_SOUTH_TO_BYBASS_EAST_TASK_3 && isSameDirection(getHeading(), Parameters.EAST)) {
            state = TURN_SOUTH_TO_BYBASS_EAST_TASK_4;
            oldX = (int) myX;
            myMove();
            return;
        }
        if (state == TURN_SOUTH_TO_BYBASS_EAST_TASK_4 && myX - oldX <= 350) {
            myMove();
            return;
        }
        if (state == TURN_SOUTH_TO_BYBASS_EAST_TASK_4 && myX - oldX > 350) {
            stepTurn(Parameters.Direction.LEFT);
            state = TURN_SOUTH_TO_BYBASS_EAST_TASK_5;
            return;
        }
        if (state == TURN_SOUTH_TO_BYBASS_EAST_TASK_5 && !isSameDirection(getHeading(), Parameters.NORTH)) {
            stepTurn(Parameters.Direction.LEFT);
            return;
        }
        if (state == TURN_SOUTH_TO_BYBASS_EAST_TASK_5 && isSameDirection(getHeading(), Parameters.NORTH)) {
            state = TURN_SOUTH_TO_BYBASS_EAST_TASK_6;
            oldY = (int) myY;
            myMove();
            return;
        }
        if (state == TURN_SOUTH_TO_BYBASS_EAST_TASK_6 && myY - oldY <= 100) {
            myMove();
            return;
        }
        if (state == TURN_SOUTH_TO_BYBASS_EAST_TASK_6 && myY - oldY > 100) {
            stepTurn(Parameters.Direction.RIGHT);
            state = TURN_SOUTH_TO_BYBASS_EAST_TASK_7;
            return;
        }
        if (state == TURN_SOUTH_TO_BYBASS_EAST_TASK_7 && !isSameDirection(getHeading(), Parameters.EAST)) {
            stepTurn(Parameters.Direction.RIGHT);
            return;
        }
        if (state == TURN_SOUTH_TO_BYBASS_EAST_TASK_7 && isSameDirection(getHeading(), Parameters.EAST)) {
            state = MOVE_BACK_TASK;
            return;
        }
    }

    private void bypassWreck_N_W_teamB() {
        if (state == TURN_NORTH_TO_BYBASS_WEST_TASK_1 && !isSameDirection(getHeading(), Parameters.NORTH)) {
            stepTurn(Parameters.Direction.RIGHT);
            return;
        }
        if (state == TURN_NORTH_TO_BYBASS_WEST_TASK_1 && isSameDirection(getHeading(), Parameters.NORTH)) {
            state = TURN_NORTH_TO_BYBASS_WEST_TASK_2;
            oldY = (int) myY;
            myMove();
            return;
        }
        if (state == TURN_NORTH_TO_BYBASS_WEST_TASK_2 && oldY - myY <= 100) {
            myMove();
            return;
        }
        if (state == TURN_NORTH_TO_BYBASS_WEST_TASK_2 && oldY - myY > 100) {
            stepTurn(Parameters.Direction.LEFT);
            state = TURN_NORTH_TO_BYBASS_WEST_TASK_3;
            return;
        }
        if (state == TURN_NORTH_TO_BYBASS_WEST_TASK_3 && !isSameDirection(getHeading(), Parameters.WEST)) {
            stepTurn(Parameters.Direction.LEFT);
            return;
        }
        if (state == TURN_NORTH_TO_BYBASS_WEST_TASK_3 && isSameDirection(getHeading(), Parameters.WEST)) {
            state = TURN_NORTH_TO_BYBASS_WEST_TASK_4;
            oldX = (int) myX;
            myMove();
            return;
        }
        if (state == TURN_NORTH_TO_BYBASS_WEST_TASK_4 && oldX - myX <= 350) {
            myMove();
            return;
        }
        if (state == TURN_NORTH_TO_BYBASS_WEST_TASK_4 && oldX - myX > 350) {
            stepTurn(Parameters.Direction.LEFT);
            state = TURN_NORTH_TO_BYBASS_WEST_TASK_5;
            return;
        }
        if (state == TURN_NORTH_TO_BYBASS_WEST_TASK_5 && !isSameDirection(getHeading(), Parameters.SOUTH)) {
            stepTurn(Parameters.Direction.LEFT);
            return;
        }
        if (state == TURN_NORTH_TO_BYBASS_WEST_TASK_5 && isSameDirection(getHeading(), Parameters.SOUTH)) {
            state = TURN_NORTH_TO_BYBASS_WEST_TASK_6;
            oldY = (int) myY;
            myMove();
            return;
        }
        if (state == TURN_NORTH_TO_BYBASS_WEST_TASK_6 && myY - oldY <= 100) {
            myMove();
            return;
        }
        if (state == TURN_NORTH_TO_BYBASS_WEST_TASK_6 && myY - oldY > 100) {
            stepTurn(Parameters.Direction.RIGHT);
            state = TURN_NORTH_TO_BYBASS_WEST_TASK_7;
            return;
        }
        if (state == TURN_NORTH_TO_BYBASS_WEST_TASK_7 && !isSameDirection(getHeading(), Parameters.WEST)) {
            stepTurn(Parameters.Direction.RIGHT);
            return;
        }
        if (state == TURN_NORTH_TO_BYBASS_WEST_TASK_7 && isSameDirection(getHeading(), Parameters.WEST)) {
            state = MOVE_TASK;
            return;
        }
    }

    private void bypassWreck_S_W_teamB() {
        if (state == TURN_SOUTH_TO_BYBASS_WEST_TASK_1 && !isSameDirection(getHeading(), Parameters.SOUTH)) {
            stepTurn(Parameters.Direction.LEFT);
            return;
        }
        if (state == TURN_SOUTH_TO_BYBASS_WEST_TASK_1 && isSameDirection(getHeading(), Parameters.SOUTH)) {
            state = TURN_SOUTH_TO_BYBASS_WEST_TASK_2;
            oldY = (int) myY;
            myMove();
            return;
        }
        if (state == TURN_SOUTH_TO_BYBASS_WEST_TASK_2 && myY - oldY <= 100) {
            myMove();
            return;
        }
        if (state == TURN_SOUTH_TO_BYBASS_WEST_TASK_2 && myY - oldY > 100) {
            stepTurn(Parameters.Direction.RIGHT);
            state = TURN_SOUTH_TO_BYBASS_WEST_TASK_3;
            return;
        }
        if (state == TURN_SOUTH_TO_BYBASS_WEST_TASK_3 && !isSameDirection(getHeading(), Parameters.WEST)) {
            stepTurn(Parameters.Direction.RIGHT);
            return;
        }
        if (state == TURN_SOUTH_TO_BYBASS_WEST_TASK_3 && isSameDirection(getHeading(), Parameters.WEST)) {
            state = TURN_SOUTH_TO_BYBASS_WEST_TASK_4;
            oldX = (int) myX;
            myMove();
            return;
        }
        if (state == TURN_SOUTH_TO_BYBASS_WEST_TASK_4 && oldX - myX <= 350) {
            myMove();
            return;
        }
        if (state == TURN_SOUTH_TO_BYBASS_WEST_TASK_4 && oldX - myX > 350) {
            stepTurn(Parameters.Direction.RIGHT);
            state = TURN_SOUTH_TO_BYBASS_WEST_TASK_5;
            return;
        }
        if (state == TURN_SOUTH_TO_BYBASS_WEST_TASK_5 && !isSameDirection(getHeading(), Parameters.NORTH)) {
            stepTurn(Parameters.Direction.RIGHT);
            return;
        }
        if (state == TURN_SOUTH_TO_BYBASS_WEST_TASK_5 && isSameDirection(getHeading(), Parameters.NORTH)) {
            state = TURN_SOUTH_TO_BYBASS_WEST_TASK_6;
            oldY = (int) myY;
            myMove();
            return;
        }
        if (state == TURN_SOUTH_TO_BYBASS_WEST_TASK_6 && myY - oldY <= 100) {
            myMove();
            return;
        }
        if (state == TURN_SOUTH_TO_BYBASS_WEST_TASK_6 && myY - oldY > 100) {
            stepTurn(Parameters.Direction.LEFT);
            state = TURN_SOUTH_TO_BYBASS_WEST_TASK_7;
            return;
        }
        if (state == TURN_SOUTH_TO_BYBASS_WEST_TASK_7 && !isSameDirection(getHeading(), Parameters.WEST)) {
            stepTurn(Parameters.Direction.LEFT);
            return;
        }
        if (state == TURN_SOUTH_TO_BYBASS_WEST_TASK_7 && isSameDirection(getHeading(), Parameters.WEST)) {
            state = MOVE_BACK_TASK;
            return;
        }
    }
}