package algorithms;

import characteristics.IFrontSensorResult;
import characteristics.IRadarResult;
import characteristics.Parameters;
import robotsimulator.Brain;

import java.util.ArrayList;

public class MecaMouseMain extends Brain {
    // Constantes de précision et d'identification
    private static final double ANGLE_PRECISION = 0.01;
    private static final double FIRE_ANGLE_PRECISION = Math.PI / 6;
    private static final int ALPHA = 0x001;
    private static final int BETA = 0x002;
    private static final int GAMMA = 0x003;

    // Constantes pour les types de messages échangés entre les robots
    private static final int DETECTION = 0x101;
    private static final int DEAD = 0x102;
    private static final int ELIMINATION = 0x103;

    // Enumération des instructions possibles pour le robot
    private enum INSTRUCTIONS {
        BASE,           // Instruction de base : au moins un secondaire est encore vivant
        THREE_ALIVE,    // Instruction quand les secondaires sont morts et qu'il reste 3 principaux vivants
        TWO_ALIVE,      // Instruction quand les secondaires sont morts et qu'il reste 2 principaux vivants
        ONE_ALIVE       // Instruction quand les secondaires sont morts et qu'il reste 1 principal vivant
    }

    // Dimensions du terrain de jeu
    private static final int PLAYGROUND_WIDTH = 3000;
    private static final int PLAYGROUND_HEIGHT = 2000;

    // Variables d'état du robot
    private boolean isOnLeft;
    private INSTRUCTIONS instruction;
    private int whoAmI;
    private double myX, myY;
    private String robotName;
    private double directionToGo;
    private boolean isMoving;

    // Variables pour gérer les ordres de tir
    private boolean fireOrder;
    private int fireRhythm;

    // Compteurs pour suivre le nombre de robots vivants
    private int countMainAlive;
    private int countSecondaryAlive;

    // Variables pour suivre la position de la cible ennemie
    private double targetX, targetY;
    private boolean hasATarget;

    // Constructeur par défaut
    public MecaMouseMain() {
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

        // Gère les ordres de tir et exécute l'instruction en cours
        if (handleFireOrder()) {
            return;
        }

        // Exécution de l'instruction en cours
        switch (instruction) {
            case BASE:
                instructionBase();
                break;
            case THREE_ALIVE:
                instructionThreeAlive();
                break;
            case TWO_ALIVE:
                instructionTwoAlive();
                break;
            case ONE_ALIVE:
                instructionOneAlive();
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
        targetX = 0;
        targetY = 0;
        hasATarget = false;
        fireOrder = false;
        fireRhythm = 0;
        isMoving = true;
    }

    /**
     * Détermine l'identité du robot en fonction de la direction des autres robots détectés.
     */
    private void initWhoAmI() {
        whoAmI = GAMMA;
        for (IRadarResult result : detectRadar()) {
            if (isSameDirection(result.getObjectDirection(), Parameters.NORTH)) {
                whoAmI = ALPHA;
                break;
            }
        }
        if (whoAmI != GAMMA) {
            for (IRadarResult result : detectRadar()) {
                if (isSameDirection(result.getObjectDirection(), Parameters.SOUTH)) {
                    whoAmI = BETA;
                    break;
                }
            }
        }
    }

    /**
     * Initialise la position du robot en fonction de son identité.
     */
    private void initPosition() {
        switch (whoAmI) {
            case ALPHA:
                myX = Parameters.teamAMainBot3InitX;
                myY = Parameters.teamAMainBot3InitY;
                break;
            case BETA:
            case GAMMA:
                myX = Parameters.teamAMainBot1InitX;
                myY = Parameters.teamAMainBot1InitY;
                break;
            default:
                myX = Parameters.teamAMainBot2InitX;
                myY = Parameters.teamAMainBot2InitY;
                break;
        }
    }

    /**
     * Initialise le nom du robot en fonction de son identité.
     */
    private void initRobotName() {
        robotName = switch (whoAmI) {
            case ALPHA -> "#ALPHA";
            case BETA -> "#BETA";
            case GAMMA -> "#GAMMA";
            default -> "#UNKNOWN";
        };
    }

    // Fonctions d'instructions

    /**
     * Exécute l'instruction de base : gère le mouvement et le tir en fonction de la présence d'une cible.
     */
    private void instructionBase() {
        if ((!hasATarget || calculateDistanceToTarget(targetX, targetY) >= Parameters.bulletRange) && fireRhythm == 0) {
            blindStraightFire();
            return;
        }

        double distanceToEnemy = calculateDistanceToTarget(targetX, targetY);

        if (isSameDirection(myGetHeading(), directionToGo)) {
            if (hasATarget && distanceToEnemy > 500) {
                moveForward();
                return;
            }

            if (hasATarget && distanceToEnemy < 200) {
                moveBackward();
                return;
            }

            if (detectFront().getObjectType() != IFrontSensorResult.Types.NOTHING) {
                if (detectFront().getObjectType() == IFrontSensorResult.Types.TeamMainBot || detectFront().getObjectType() == IFrontSensorResult.Types.TeamSecondaryBot) {
                    for (IRadarResult o : detectRadar()) {
                        //if (o.getObjectDirection() == directionToGo) {
                            if (o.getObjectDistance() >= Parameters.teamAMainBotFrontalDetectionRange / 5) {
                                moveForward();
                            } else {
                                reverseDirectionToGo();
                            }
                        //}
                    }
                } else {
                    reverseDirectionToGo();
                }
            } else {
                moveForward();
            }
        } else {
            stepTurn(Parameters.Direction.LEFT);
        }
    }

    /**
     * Placeholder pour l'instruction THREE_ALIVE.
     */
    private void instructionThreeAlive() {
        // Implémentation pour l'instruction THREE_ALIVE
    }

    /**
     * Placeholder pour l'instruction TWO_ALIVE.
     */
    private void instructionTwoAlive() {
        // Implémentation pour l'instruction TWO_ALIVE
    }

    /**
     * Placeholder pour l'instruction ONE_ALIVE.
     */
    private void instructionOneAlive() {
        // Implémentation pour l'instruction ONE_ALIVE
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
     * Met à jour la cible en fonction des informations reçues.
     *
     * @param bot  l'identifiant du robot ayant envoyé le message
     * @param botX la position X du robot ennemi
     * @param botY la position Y du robot ennemi
     */
    private void processDetectionMessage(int bot, double botX, double botY) {
        if (calculateDistanceToTarget(botX, botY) <= calculateDistanceToTarget(targetX, targetY)) {
            hasATarget = true;
            firePosition(botX, botY);
        }
    }

    // Fonctions de logs

    /**
     * Affiche des informations de débogage dans les logs.
     */
    private void showLogDetails() {
        sendLogMessage(robotName + " [" + (int) myX + ", " + (int) myY + "] / " + myGetHeading() + " / toGo = " + directionToGo);
        //sendLogMessage(robotName + " Target [" + (int) targetX + ", " + (int) targetY + "] / Target = " + hasATarget + " / Firing = " + fireOrder + " / FireRhythm = " + String.valueOf(fireRhythm == 0));
    }

    // Fonctions de tir

    /**
     * Met à jour la position de la cible et active l'ordre de tir.
     *
     * @param x la position X de la cible
     * @param y la position Y de la cible
     */
    private void firePosition(double x, double y) {
        targetX = x;
        targetY = y;
        fireOrder = true;
    }

    /**
     * Tire droit devant sans cible spécifique.
     */
    private void blindStraightFire() {
        int randomGap = (int) ((Math.random() * 2 * (Parameters.teamAMainBotRadius) - Parameters.teamAMainBotRadius));
        if (fireRhythm == 0) {
            fireAtTarget(myX + Parameters.bulletRange - Parameters.teamAMainBotRadius, myY + randomGap);
            fireRhythm++;
            return;
        }
        fireRhythm++;
        if (fireRhythm >= Parameters.bulletFiringLatency) fireRhythm = 0;
    }

    /**
     * Gère l'ordre de tir si une cible est présente.
     *
     * @return true si un tir a été effectué, false sinon
     */
    private boolean handleFireOrder() {
        if (fireOrder && fireRhythm == 0 && hasATarget) {
            fireAtTarget(targetX, targetY);
            fireRhythm++;
            return true;
        }
        fireRhythm++;
        if (fireRhythm >= Parameters.bulletFiringLatency) fireRhythm = 0;
        return false;
    }

    /**
     * Tire sur la cible si elle est dans la portée de tir et qu'il n'y a pas d'allié dans la ligne de mire.
     *
     * @param x la position X de la cible
     * @param y la position Y de la cible
     */
    private void fireAtTarget(double x, double y) {
        double angle = calculateFiringAngle(x, y);
        boolean hasToFire = isWithinFiringRange(x, y) && !isAllyInLineOfFire(angle);
        if (hasToFire) {
            fire(angle);
        }
    }

    /**
     * Vérifie si la cible est dans la portée de tir.
     *
     * @param x la position X de la cible
     * @param y la position Y de la cible
     * @return true si la cible est dans la portée de tir, false sinon
     */
    private boolean isWithinFiringRange(double x, double y) {
        return calculateDistanceToTarget(x, y) <= Parameters.bulletRange;
    }

    /**
     * Calcule l'angle de tir vers la cible.
     *
     * @param x la position X de la cible
     * @param y la position Y de la cible
     * @return l'angle de tir en radians
     */
    private double calculateFiringAngle(double x, double y) {
        if (x == myX) {
            return (y > myY) ? Math.PI / 2 : 3 * Math.PI / 2;
        } else {
            double angle = Math.atan((y - myY) / (x - myX));
            if (x < myX) {
                angle += Math.PI;
            } else if (y < myY) {
                angle += 2 * Math.PI;
            }
            return angle;
        }
    }

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

    /**
     * Vérifie si un allié est dans la ligne de mire.
     *
     * @param firingAngle l'angle de tir
     * @return true si un allié est dans la ligne de mire, false sinon
     */
    private boolean isAllyInLineOfFire(double firingAngle) {
        for (IRadarResult result : detectRadar()) {
            if (result.getObjectType() == IRadarResult.Types.TeamMainBot || result.getObjectType() == IRadarResult.Types.TeamSecondaryBot) {
                double allyAngle = result.getObjectDirection();
                if (Math.abs(normalizeRadian(allyAngle - firingAngle)) < FIRE_ANGLE_PRECISION) {
                    return true;
                }
            }
        }
        return false;
    }

    // Fonctions de mouvement

    /**
     * Avance si rien n'est détecté devant.
     */
    private void moveForward() {
        if (detectFront().getObjectType() == IFrontSensorResult.Types.NOTHING) {
            move();
            updatePosition(Parameters.teamAMainBotSpeed);
        }
    }

    /**
     * Recule et met à jour la position.
     */
    private void moveBackward() {
        moveBack();
        updatePosition(-Parameters.teamAMainBotSpeed);
    }

    /**
     * Inverse la direction de déplacement.
     */
    private void reverseDirectionToGo() {
        directionToGo = normalizeRadian(directionToGo + Math.PI);
    }

    /**
     * Met à jour la position en fonction de la vitesse et de la direction.
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
            }
            if (o.getObjectType() == IRadarResult.Types.Wreck) {
                //sendLogMessage("Wreck : " + o.getObjectDistance() + " / target = " + calculateDistanceToTarget(targetX, targetY) + "/ hasATarget " + hasATarget);
                if (o.getObjectDistance() == calculateDistanceToTarget(targetX, targetY)) {
                    hasATarget = false;
                }
            }
        }
    }
}
