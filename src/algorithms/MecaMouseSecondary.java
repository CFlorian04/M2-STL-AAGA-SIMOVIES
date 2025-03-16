package algorithms;

import characteristics.IFrontSensorResult;
import characteristics.IRadarResult;
import characteristics.Parameters;
import robotsimulator.Brain;

import java.util.ArrayList;

public class MecaMouseSecondary extends Brain {
    // Constantes de précision et d'identification
    private static final double ANGLE_PRECISION = 0.01;
    private static final double FIRE_ANGLE_PRECISION = Math.PI / 6;
    private static final int ROCKY = 0x004;
    private static final int MARIO = 0x005;

    // Constantes pour les types de messages échangés entre les robots
    private static final int DETECTION = 0x101;
    private static final int DEAD = 0x102;
    private static final int ELIMINATION = 0x103;

    // Enumération des instructions possibles pour le robot
    private enum INSTRUCTIONS {
        BASE,           // 2 robots secondaires vivants
        ALONE,          // 1 robot secondaire vivant
        NO_MAIN_LEFT    // Plus de robots principaux vivants
    }

    // Dimensions du terrain de jeu
    private int PLAYGROUND_WIDTH = 3000;
    private int PLAYGROUND_HEIGHT = 2000;

    // Variables d'état du robot
    private boolean IsOnLeft;
    private INSTRUCTIONS instruction;
    private int whoAmI;
    private double myX, myY;
    private String robotName;
    private double directionToGo;

    // Compteurs pour suivre le nombre de robots vivants
    private int countMainAlive;
    private int countSecondaryAlive;

    // Variables pour suivre la position de la cible ennemie
    private double targetX, targetY;

    // Constructeur par défaut
    public MecaMouseSecondary() {
        super();
    }

    /**
     * Initialise le robot lors de son activation.
     */
    @Override
    public void activate() {
        init(); // Initialisation des variables et de l'état du robot
    }

    /**
     * Exécute une étape du comportement du robot.
     */
    @Override
    public void step() {
        showLogDetails(); // Affiche les détails de log pour le débogage
        radarDetection(); // Détection des objets avec le radar
        readBroadcast(); // Lecture des messages diffusés par les autres robots

        // Exécution de l'instruction en cours
        switch (instruction) {
            case BASE:
                instruction_base();
                break;
            case ALONE:
                instruction_alone();
                break;
            case NO_MAIN_LEFT:
                instruction_no_main_let();
                break;
        }
    }

    // Fonctions d'initialisation

    /**
     * Initialise les variables et l'état du robot.
     */
    private void init() {
        initWhoAmI(); // Initialise l'identité du robot
        initRobotName(); // Initialise le nom du robot
        initPosition(); // Initialise la position du robot

        // Initialisation des autres variables d'état
        instruction = INSTRUCTIONS.BASE;
        countMainAlive = 3;
        countSecondaryAlive = 2;
        directionToGo = Parameters.EAST;
    }

    /**
     * Détermine l'identité du robot en fonction de la direction des autres robots détectés.
     */
    private void initWhoAmI() {
        whoAmI = ROCKY;
        for (IRadarResult o : detectRadar()) {
            if (isSameDirection(o.getObjectDirection(), Parameters.NORTH)) {
                whoAmI = MARIO;
            }
        }
    }

    /**
     * Initialise la position du robot en fonction de son identité.
     */
    private void initPosition() {
        if (whoAmI == ROCKY) {
            myX = Parameters.teamASecondaryBot1InitX;
            myY = Parameters.teamASecondaryBot1InitY;
        } else {
            myX = Parameters.teamASecondaryBot2InitX;
            myY = Parameters.teamASecondaryBot2InitY;
        }
    }

    /**
     * Initialise le nom du robot en fonction de son identité.
     */
    private void initRobotName() {
        robotName = switch (whoAmI) {
            case ROCKY -> "#ROCKY";
            case MARIO -> "#MARIO";
            default -> "#UNKNOWN";
        };
    }

    // Fonctions d'instructions

    /**
     * Exécute l'instruction de base : gère le mouvement en fonction de l'identité du robot.
     */
    private void instruction_base() {
        if (whoAmI == ROCKY) {
            // Se mettre sur la bonne ligne
            if (myY >= Parameters.teamASecondaryBotFrontalDetectionRange) {
                if (isSameDirection(myGetHeading(), Parameters.NORTH)) {
                    moveForward();
                } else {
                    stepTurn(Parameters.Direction.LEFT);
                }
                return;
            }

            if (isSameDirection(myGetHeading(), directionToGo)) {
                if (detectFront().getObjectType() != IFrontSensorResult.Types.NOTHING) {
                    reverseDirectionToGo();
                } else {
                    moveForward();
                }
            } else {
                stepTurn(Parameters.Direction.RIGHT);
            }
        }

        if (whoAmI == MARIO) {
            // Se mettre sur la bonne ligne
            if (myY <= PLAYGROUND_HEIGHT - Parameters.teamASecondaryBotFrontalDetectionRange) {
                if (isSameDirection(myGetHeading(), Parameters.SOUTH)) {
                    moveForward();
                } else {
                    stepTurn(Parameters.Direction.RIGHT);
                }
                return;
            }

            if (isSameDirection(myGetHeading(), directionToGo)) {
                if (detectFront().getObjectType() != IFrontSensorResult.Types.NOTHING) {
                    reverseDirectionToGo();
                } else {
                    moveForward();
                }
            } else {
                stepTurn(Parameters.Direction.LEFT);
            }
        }
    }

    /**
     * Placeholder pour l'instruction ALONE.
     */
    private void instruction_alone() {
        // Implémentation pour l'instruction ALONE
    }

    /**
     * Placeholder pour l'instruction NO_MAIN_LEFT.
     */
    private void instruction_no_main_let() {
        // Implémentation pour l'instruction NO_MAIN_LEFT
    }

    // Fonctions de communication

    /**
     * Lit tous les messages diffusés et les traite.
     */
    private void readBroadcast() {
        ArrayList<String> messages = fetchAllMessages();
        for (String message : messages) {
            processMessage(message);
        }
    }

    /**
     * Traite un message diffusé pour mettre à jour l'état du robot.
     *
     * @param message le message à traiter
     */
    private void processMessage(String message) {
        int bot = Integer.parseInt(message.split(":")[0]);
        int type = Integer.parseInt(message.split(":")[1]);
        double arg1 = Double.parseDouble(message.split(":")[2]);
        double arg2 = Double.parseDouble(message.split(":")[3]);
        switch (type) {
            case DETECTION:
                processDetectionMessage(bot, arg1, arg2);
                break;
            default:
                return;
        }
    }

    /**
     * Envoie un message diffusé avec les informations spécifiées.
     *
     * @param type le type de message
     * @param arg1 le premier argument du message
     * @param arg2 le second argument du message
     */
    private void sendBroadcastMessage(int type, double arg1, double arg2) {
        String message = "";
        switch (type) {
            case DETECTION:
                message = whoAmI + ":" + DETECTION + ":" + arg1 + ":" + arg2;
                break;
            default:
                return;
        }
        broadcast(message);
    }

    /**
     * Traite un message de détection pour mettre à jour l'état du robot.
     *
     * @param bot  l'identifiant du robot ayant envoyé le message
     * @param botX la position X du robot ennemi
     * @param botY la position Y du robot ennemi
     */
    private void processDetectionMessage(int bot, double botX, double botY) {
        // Implémentation pour traiter le message de détection
    }

    // Fonctions de logs

    /**
     * Affiche des informations de débogage dans les logs.
     */
    private void showLogDetails() {
        sendLogMessage(robotName + " [" + (int) myX + ", " + (int) myY + "] / " + myGetHeading() + " / toGo = " + directionToGo);
    }

    // Fonction de tir

    /**
     * Calcule la distance jusqu'à la cible.
     *
     * @param x la position X de la cible
     * @param y la position Y de la cible
     * @return la distance jusqu'à la cible
     */
    private double calculateDistanceToTarget(double x, double y) {
        return Math.sqrt((y - myY) * (y - myY) + (x - myX) * (x - myX));
    }

    // Fonctions de mouvement

    /**
     * Avance le robot.
     */
    private void moveForward() {
        move();
        updatePosition(Parameters.teamASecondaryBotSpeed);
    }

    /**
     * Recule le robot.
     */
    private void moveBackward() {
        moveBack();
        updatePosition(-Parameters.teamASecondaryBotSpeed);
    }

    /**
     * Inverse la direction de déplacement du robot.
     */
    private void reverseDirectionToGo() {
        directionToGo = normalizeRadian(directionToGo + Math.PI);
    }

    /**
     * Met à jour la position du robot en fonction de la vitesse et de la direction.
     *
     * @param speed la vitesse de déplacement
     */
    private void updatePosition(double speed) {
        myX += speed * Math.cos(myGetHeading());
        myY += speed * Math.sin(myGetHeading());
    }

    // Fonctions de direction

    /**
     * Vérifie si deux directions sont similaires.
     *
     * @param dir1 la première direction
     * @param dir2 la seconde direction
     * @return true si les directions sont similaires, false sinon
     */
    private boolean isSameDirection(double dir1, double dir2) {
        return Math.abs(normalizeRadian(dir1) - normalizeRadian(dir2)) < ANGLE_PRECISION;
    }

    /**
     * Normalise un angle en radians pour qu'il soit compris entre 0 et 2π.
     *
     * @param angle l'angle à normaliser
     * @return l'angle normalisé
     */
    private double normalizeRadian(double angle) {
        double result = angle;
        while (result < 0) result += 2 * Math.PI;
        while (result >= 2 * Math.PI) result -= 2 * Math.PI;
        return result;
    }

    /**
     * Retourne la direction actuelle normalisée.
     *
     * @return la direction actuelle en radians
     */
    private double myGetHeading() {
        return normalizeRadian(getHeading());
    }

    // Fonctions de radar

    /**
     * Détecte les objets avec le radar et diffuse les informations sur les ennemis.
     */
    private void radarDetection() {
        for (IRadarResult o : detectRadar()) {
            if (o.getObjectType() == IRadarResult.Types.OpponentMainBot || o.getObjectType() == IRadarResult.Types.OpponentSecondaryBot) {
                double enemyX = myX + o.getObjectDistance() * Math.cos(o.getObjectDirection());
                double enemyY = myY + o.getObjectDistance() * Math.sin(o.getObjectDirection());
                sendBroadcastMessage(DETECTION, enemyX, enemyY);

                if(calculateDistanceToTarget(enemyX, enemyY) < calculateDistanceToTarget(targetX, targetY)) {
                    reverseDirectionToGo();
                }

                targetX = enemyX;
                targetY = enemyY;
            }
        }
    }
}
