package algorithms;

import characteristics.IFrontSensorResult;
import characteristics.IRadarResult;
import characteristics.Parameters;
import robotsimulator.Brain;

import java.util.*;

public class MecaMouseSecondary extends Brain {
    // Constantes de précision et d'identification
    private static final double ANGLE_PRECISION = 0.025;
    private static final double FIRE_ANGLE_PRECISION = Math.PI / 6;
    private static final int ROCKY = 0x004;
    private static final int MARIO = 0x005;

    // Constantes pour les types de messages échangés entre les robots
    private static final int DETECTION = 0x101;
    private static final int DEAD = 0x102;
    private static final int ELIMINATION = 0x103;
    private static final int SECONDARYINPOSITION = 0x104;
    private static final int POSITION = 0x105;
    private static final int ALLY_POSITION = 0x106; // Nouveau type de message pour les positions des alliés

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
    private Position myPosition;
    private String robotName;
    private double directionToGo;
    private boolean isMoving;
    private boolean isSecondaryInPosition;

    // Compteurs pour suivre le nombre de robots vivants
    private int countMainAlive;
    private int countSecondaryAlive;

    // Variables pour suivre la position de la cible ennemie
    private static final int MAX_ENEMY_POSITION_TIME = 500;
    private HashMap<Enemy, Integer> enemyPositions = new HashMap<>();
    private ArrayList<Position> wrecksPositions = new ArrayList<>();

    private Position targetPosition;

    // Liste pour stocker les positions des alliés
    private HashMap<Integer, Position> allyPositions = new HashMap<>();

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
        showLogDetails(); // Affichage des informations de débogage
        radarDetection(); // Détection des objets avec le radar
        readBroadcast(); // Lecture des messages diffusés par les autres robots
        incrementEnemyPositionTime(); // Incrémentation du temps de position des ennemis
        broadcastOwnPosition(); // Diffusion de la position actuelle du robot

        isMoving = false;

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
        targetPosition = new Position(0, 0);
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
            myPosition = new Position((int) Parameters.teamASecondaryBot1InitX, (int) Parameters.teamASecondaryBot1InitY);
        } else {
            myPosition = new Position((int) Parameters.teamASecondaryBot2InitX, (int) Parameters.teamASecondaryBot2InitY);
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
            if (myPosition.y >= Parameters.teamASecondaryBotFrontalDetectionRange) {
                if (isSameDirection(myGetHeading(), Parameters.NORTH)) {
                    moveForward();
                    if (!isSecondaryInPosition) {
                        isSecondaryInPosition = true;
                        sendBroadcastMessage(SECONDARYINPOSITION, 0, 0, 0);
                    }
                } else {
                    stepTurn(Parameters.Direction.LEFT);
                }
                return;
            }
        }

        if (whoAmI == MARIO) {
            // Se mettre sur la bonne ligne
            if (myPosition.y <= PLAYGROUND_HEIGHT - Parameters.teamASecondaryBotFrontalDetectionRange) {
                if (isSameDirection(myGetHeading(), Parameters.SOUTH)) {
                    moveForward();
                    if (!isSecondaryInPosition) {
                        isSecondaryInPosition = true;
                        sendBroadcastMessage(SECONDARYINPOSITION, 0, 0, 0);
                    }
                } else {
                    stepTurn(Parameters.Direction.RIGHT);
                }
                return;
            }
        }

        if (isSameDirection(myGetHeading(), directionToGo)) {
            if (detectFront().getObjectType() != IFrontSensorResult.Types.NOTHING) {
                System.out.println(detectFront().toString());
                reverseDirectionToGo();
            } else {
                moveForward();
            }
        } else {
            if (whoAmI == ROCKY) {
                stepTurn(Parameters.Direction.RIGHT);
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
        String[] parts = message.split(":");
        if (parts.length < 5) {
            // Message invalide, ignore
            return;
        }

        int bot = Integer.parseInt(parts[0]);
        int type = Integer.parseInt(parts[1]);
        int arg1 = Integer.parseInt(parts[2]);
        int arg2 = Integer.parseInt(parts[3]);
        int arg3 = Integer.parseInt(parts[4]);

        switch (type) {
            case DETECTION:
                updateEnemyPosition(new Enemy(arg1, arg2, arg3, Parameters.teamAMainBotSpeed), 0);
                break;
            case POSITION:
                updateEnemyPosition(new Enemy(arg1, arg2, arg3, Parameters.teamAMainBotSpeed), 0);
                break;
            case DEAD:
                Position position = new Position(arg1, arg2);
                addWreck(position);
                break;
            case ALLY_POSITION:
                allyPositions.put(bot, new Position(arg1, arg2));
                break;
            default:
                // Type de message inconnu, ignore
                break;
        }
    }

    /**
     * Envoie un message diffusé avec les informations spécifiées.
     *
     * @param type le type de message
     * @param arg1 le premier argument du message
     * @param arg2 le second argument du message
     * @param arg3 le troisième argument du message
     */
    private void sendBroadcastMessage(int type, int arg1, int arg2, int arg3) {
        String message = whoAmI + ":" + type + ":" + arg1 + ":" + arg2 + ":" + arg3;
        broadcast(message);
    }

    /**
     * Diffuse la position actuelle du robot.
     */
    private void broadcastOwnPosition() {
        sendBroadcastMessage(ALLY_POSITION, myPosition.x, myPosition.y, 0);
    }

    // Fonctions de logs

    /**
     * Affiche des informations de débogage dans les logs.
     */
    private void showLogDetails() {
        sendLogMessage(robotName + " [" + myPosition.x + ", " + myPosition.y + "] / Target [" + targetPosition.x + ", " + targetPosition.y + "]");
        sendLogMessage(enemyPositionToString());
    }

    // Fonction de tir

    /**
     * Calcule la distance jusqu'à la cible.
     *
     * @param position la position de la cible
     * @return la distance jusqu'à la cible
     */
    private int calculateDistanceToTarget(Position position) {
        return (int) Math.sqrt((position.y - myPosition.y) * (position.y - myPosition.y) + (position.x - myPosition.x) * (position.x - myPosition.x));
    }

    // Fonctions de mouvement

    /**
     * Avance le robot.
     */
    private void moveForward() {
        move();
        updatePosition((int) Parameters.teamASecondaryBotSpeed);
        isMoving = true;
    }

    /**
     * Recule le robot.
     */
    private void moveBackward() {
        moveBack();
        updatePosition(-(int) Parameters.teamASecondaryBotSpeed);
        isMoving = true;
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
    private void updatePosition(int speed) {
        myPosition.x += (int) (speed * Math.cos(myGetHeading()));
        myPosition.y += (int) (speed * Math.sin(myGetHeading()));
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
    private static double normalizeRadian(double angle) {
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

    /**
     * Calcule la distance entre deux cibles
     *
     * @param position1 la position de la première cible
     * @param position2 la position de la seconde cible
     * @return la distance entre les deux cibles
     */
    private int calculateDistanceBetweenTwoTarget(Position position1, Position position2) {
        return (int) Math.sqrt((position1.y - position2.y) * (position1.y - position2.y) + (position1.x - position2.x) * (position1.x - position2.x));
    }

    // Fonctions de radar

    /**
     * Détecte les objets avec le radar et diffuse les informations sur les ennemis.
     */
    private void radarDetection() {
        for (IRadarResult o : detectRadar()) {
            int enemyX = myPosition.x + (int) (o.getObjectDistance() * Math.cos(o.getObjectDirection()));
            int enemyY = myPosition.y + (int) (o.getObjectDistance() * Math.sin(o.getObjectDirection()));

            if (o.getObjectType() == IRadarResult.Types.OpponentMainBot || o.getObjectType() == IRadarResult.Types.OpponentSecondaryBot) {
                sendBroadcastMessage(DETECTION, enemyX, enemyY, 0);
                updateEnemyPosition(new Enemy(enemyX, enemyY, o.getObjectDirection(), Parameters.teamAMainBotSpeed), 0);

                int distanceToTarget = calculateDistanceToTarget(new Position(enemyX, enemyY));

                // S'il se rapproche de l'ennemi faire demi-tour
                if (distanceToTarget < calculateDistanceToTarget(targetPosition) && distanceToTarget <= 0.6 * Parameters.teamASecondaryBotFrontalDetectionRange) {
                    reverseDirectionToGo();
                }

                targetPosition = new Position(enemyX, enemyY);
            }

            if (o.getObjectType() == IRadarResult.Types.Wreck) {
                Position position = new Position(enemyX, enemyY);
                addWreck(position);
                sendBroadcastMessage(DEAD, position.x, position.y, 0);

            }
        }
    }

    // Fonctions sur Ennemies

    /**
     * Met à jour la position de l'ennemi.
     *
     * @param enemy l'ennemi à mettre à jour
     * @param timeDetected le temps de détection
     */
    private void updateEnemyPosition(Enemy enemy, int timeDetected) {
        ArrayList<Enemy> toRemove = new ArrayList<>();
        for (Enemy existingEnemy : enemyPositions.keySet()) {
            // Si la position est dans le rayon de la position déjà enregistrée
            if (existingEnemy.isInRadius(enemy) && enemyPositions.get(existingEnemy) > timeDetected) {
                toRemove.add(existingEnemy);
            }
        }
        for (Enemy enemyToRemove : toRemove) {
            removeEnnemyPosition(enemyToRemove);
        }
        // Si l'ennemi n'est pas déjà enregistré
        if (!enemyPositions.containsKey(enemy)) {
            enemyPositions.put(enemy, timeDetected);
        }
    }

    /**
     * Supprime la position de l'ennemi.
     *
     * @param enemy l'ennemi à supprimer
     */
    private void removeEnnemyPosition(Enemy enemy) {
        ArrayList<Enemy> toRemove = new ArrayList<>();
        for (Enemy existingEnemy : enemyPositions.keySet()) {
            if (existingEnemy.equals(enemy) || existingEnemy.isInRadius(enemy)) {
                toRemove.add(existingEnemy);
            }
        }
        for (Enemy enemyToRemove : toRemove) {
            enemyPositions.remove(enemyToRemove);
        }
    }

    /**
     * Incrémente le temps de position de l'ennemi.
     */
    private void incrementEnemyPositionTime() {
        ArrayList<Enemy> toRemoveByTime = new ArrayList<>();
        for (Enemy enemy : enemyPositions.keySet()) {
            if (enemyPositions.get(enemy) > MAX_ENEMY_POSITION_TIME) {
                toRemoveByTime.add(enemy);
            } else {
                enemyPositions.put(enemy, enemyPositions.get(enemy) + 1);
            }
        }
        for (Enemy enemy : toRemoveByTime) {
            removeEnnemyPosition(enemy);
        }

        List<Enemy> toRemoveByPosition = new ArrayList<>();
        for (Enemy enemyBot1 : enemyPositions.keySet()) {
            for (Enemy enemyBot2 : enemyPositions.keySet()) {
                if (enemyBot1.equals(enemyBot2)) {
                    continue;
                }
                if (enemyBot1.isInRadius(enemyBot2)) {
                    if (enemyPositions.get(enemyBot1) > enemyPositions.get(enemyBot2)) {
                        toRemoveByPosition.add(enemyBot2);
                    } else {
                        toRemoveByPosition.add(enemyBot1);
                    }
                }
            }
        }
        for (Enemy enemy : toRemoveByPosition) {
            removeEnnemyPosition(enemy);
        }
        for (Position wreckPosition : wrecksPositions) {
            removeEnnemyPosition(new Enemy(wreckPosition.x, wreckPosition.y, 0, 0));
        }

        broadcastEnemyPositions();
    }

    private String enemyPositionToString() {
        StringBuilder sb = new StringBuilder();
        for (Enemy enemy : enemyPositions.keySet()) {
            sb.append("[").append(enemy.x).append(", ").append(enemy.y).append("] | ");
        }
        return sb.toString();
    }

    /**
     * Ajoute une épave à la liste des épaves.
     *
     * @param position la position de l'épave
     */
    private void addWreck(Position position) {
        boolean isNewWreck = true;
        for (Position wreck : wrecksPositions) {
            if (wreck.equals(position) || wreck.isInRadius(position)) {
                isNewWreck = false;
            }
        }
        if (isNewWreck) {
            wrecksPositions.add(position);
        }
        removeEnnemyPosition(new Enemy(position.x, position.y, 0, 0));
    }

    /**
     * Affiche les positions des épaves dans les logs.
     */
    private String wrecksPositionToString() {
        StringBuilder sb = new StringBuilder();
        for (Position position : wrecksPositions) {
            sb.append("[").append(position.x).append(", ").append(position.y).append("] | ");
        }
        return sb.toString();
    }

    /**
     * Broadcast les positions des ennemis
     */
    private void broadcastEnemyPositions() {
        for (Enemy enemy : enemyPositions.keySet()) {
            sendBroadcastMessage(POSITION, enemy.x, enemy.y, enemyPositions.get(enemy));
        }
    }

    // Classe pour représenter une position
    private static class Position {
        int x, y;

        Position(int x, int y) {
            this.x = x;
            this.y = y;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Position position = (Position) o;
            return x == position.x && y == position.y;
        }

        public boolean isInRadius(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Position position = (Position) o;
            return Math.sqrt((x - position.x) * (x - position.x) + (y - position.y) * (y - position.y)) <= Parameters.teamAMainBotRadius;
        }

        @Override
        public int hashCode() {
            return Objects.hash(x, y);
        }
    }

    // Classe pour représenter un ennemi avec une position et une direction
    private static class Enemy extends Position {
        double direction;
        double speed;

        Enemy(int x, int y, double direction, double speed) {
            super(x, y);
            this.direction = direction;
            this.speed = speed;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            if (!super.equals(o)) return false;
            Enemy enemy = (Enemy) o;
            return Double.compare(enemy.direction, direction) == 0 && Double.compare(enemy.speed, speed) == 0;
        }

        @Override
        public int hashCode() {
            return Objects.hash(super.hashCode(), direction, speed);
        }

        public boolean isInRadius(Enemy other) {
            return super.isInRadius(other) && Math.abs(normalizeRadian(this.direction - other.direction)) < ANGLE_PRECISION;
        }
    }
}
