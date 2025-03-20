package algorithms;

import characteristics.IFrontSensorResult;
import characteristics.IRadarResult;
import characteristics.Parameters;
import robotsimulator.Brain;

import java.util.*;

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
    private static final int SECONDARYINPOSITION = 0x104;
    private static final int POSITION = 0x105;
    private static final int ALLY_POSITION = 0x106; // Nouveau type de message pour les positions des alliés

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
    private Position myPosition;
    private String robotName;
    private double directionToGo;
    private boolean isMoving;
    private boolean isSecondaryInPosition;
    private boolean isInPosition;

    // Variables pour gérer les ordres de tir
    private boolean fireOrder;
    private int fireRhythm;

    // Compteurs pour suivre le nombre de robots vivants
    private int countMainAlive;
    private int countSecondaryAlive;

    // Variables pour suivre la position de la cible ennemie
    private static final int MAX_ENEMY_POSITION_TIME = 500;
    private HashMap<Enemy, Integer> enemyPositions = new HashMap<>();
    private ArrayList<Position> wrecksPositions = new ArrayList<>();
    private Position targetPosition;
    private boolean hasATarget;
    private int shootOnSameTarget;
    private int countShootOnSameTarget;

    // Liste pour stocker les positions des alliés
    private HashMap<Integer, Position> allyPositions = new HashMap<>();

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
        incrementEnemyPositionTime(); // Incrémentation du temps de position des ennemis
        broadcastOwnPosition(); // Diffusion de la position actuelle du robot

        isMoving = false;

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
        targetPosition = new Position(0, 0);
        hasATarget = false;
        fireOrder = false;
        fireRhythm = 0;
        isMoving = true;
        isInPosition = false;

        countShootOnSameTarget = 0;
        shootOnSameTarget = 8;
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
                myPosition = new Position((int) Parameters.teamAMainBot3InitX, (int) Parameters.teamAMainBot3InitY);
                break;
            case BETA:
                myPosition = new Position((int) Parameters.teamAMainBot2InitX, (int) Parameters.teamAMainBot2InitY);
                break;
            case GAMMA:
                myPosition = new Position((int) Parameters.teamAMainBot1InitX, (int) Parameters.teamAMainBot1InitY);
                break;
            default:
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
        if ((!hasATarget || calculateDistanceToTarget(targetPosition) >= Parameters.bulletRange) && fireRhythm == 0 && isSecondaryInPosition) {
            blindStraightFire();
            return;
        }

        // Initialiser les positions des robots
        if (!isInPosition) {
            switch (whoAmI) {
                case BETA:
                    isInPosition = true;
                    break;
                case GAMMA:
                    if (myPosition.y > PLAYGROUND_HEIGHT / 3) {
                        if (isSameDirection(myGetHeading(), Parameters.NORTH)) {
                            moveForward();
                        } else {
                            stepTurn(Parameters.Direction.LEFT);
                        }
                    } else {
                        isInPosition = true;
                    }
                    break;
                case ALPHA:
                    if (myPosition.y < PLAYGROUND_HEIGHT - (PLAYGROUND_HEIGHT / 3)) {
                        if (isSameDirection(myGetHeading(), Parameters.SOUTH)) {
                            moveForward();
                        } else {
                            stepTurn(Parameters.Direction.RIGHT);
                        }
                    } else {
                        isInPosition = true;
                    }
                    break;
            }
            return;
        }

        if (isSameDirection(myGetHeading(), directionToGo)) {
            if (detectFront().getObjectType() != IFrontSensorResult.Types.NOTHING) {
                if (detectFront().getObjectType() == IFrontSensorResult.Types.TeamMainBot || detectFront().getObjectType() == IFrontSensorResult.Types.TeamSecondaryBot) {
                    for (IRadarResult o : detectRadar()) {
                        if (o.getObjectDistance() >= Parameters.teamAMainBotFrontalDetectionRange / 5) {
                            moveForward();
                        } else {
                            reverseDirectionToGo();
                        }
                    }
                } else {
                    reverseDirectionToGo();
                }
            } else {
                moveForward();
            }
        } else {
            if (whoAmI == GAMMA) {
                stepTurn(Parameters.Direction.RIGHT);
            } else {
                stepTurn(Parameters.Direction.LEFT);
            }
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
                processDetectionMessage(bot, arg1, arg2);
                break;
            case SECONDARYINPOSITION:
                isSecondaryInPosition = true;
                break;
            case POSITION:
                updateEnemyPosition(new Enemy(arg1, arg2, arg3, Parameters.teamAMainBotSpeed), 0);
                break;
            case DEAD:
                addWreck(new Position(arg1, arg2));
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
     */
    private void sendBroadcastMessage(int type, int arg1, int arg2, int arg3) {
        String message = whoAmI + ":" + type + ":" + arg1 + ":" + arg2 + ":" + arg3;
        broadcast(message);
    }

    /**
     * Met à jour la cible en fonction des informations reçues.
     *
     * @param bot  l'identifiant du robot ayant envoyé le message
     * @param botX la position X du robot ennemi
     * @param botY la position Y du robot ennemi
     */
    private void processDetectionMessage(int bot, int botX, int botY) {
        Position enemyPosition = new Position(botX, botY);
        if (calculateDistanceToTarget(enemyPosition) <= calculateDistanceToTarget(targetPosition)) {
            hasATarget = true;
            firePosition(enemyPosition);
        }
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
        sendLogMessage(robotName + " [" + myPosition.x + ", " + myPosition.y + "] / Target [" + targetPosition.x + ", " + targetPosition.y + "] / " + enemyPositions.size());
        //sendLogMessage(enemyPositionToString());
    }

    // Fonctions de tir

    /**
     * Met à jour la position de la cible et active l'ordre de tir.
     *
     * @param position la position de la cible
     */
    private void firePosition(Position position) {
        targetPosition = position;
        fireOrder = true;
    }

    /**
     * Tire droit devant sans cible spécifique.
     */
    private void blindStraightFire() {
        int randomGap = (int) ((Math.random() * 2 * (Parameters.teamAMainBotRadius) - Parameters.teamAMainBotRadius));
        if (fireRhythm == 0) {
            Position blindPosition = new Position(myPosition.x + (int) Parameters.bulletRange - (int) Parameters.teamAMainBotRadius, myPosition.y + randomGap);
            fireAtTarget(blindPosition);
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
        checkOptimiseTarget();

        if (fireOrder && fireRhythm == 0 && hasATarget) {
            fireRhythm++;
            if (fireAtTarget(targetPosition)) {
                countShootOnSameTarget++;
                return true;
            }
        }
        fireRhythm++;
        if (fireRhythm >= Parameters.bulletFiringLatency) fireRhythm = 0;
        return false;
    }

    /**
     * Tire sur la cible si elle est dans la portée de tir et qu'il n'y a pas d'allié dans la ligne de mire.
     *
     * @param position la position de la cible
     */
    private boolean fireAtTarget(Position position) {
        double angle = calculateFiringAngle(position);
        boolean hasToFire = isWithinFiringRange(position) && !isAllyInLineOfFire(angle) && !isWreckInLineOfFire(angle);
        if (hasToFire) {
            fire(angle);
        }
        return hasToFire;
    }

    /**
     * Vérifie si la cible est dans la portée de tir.
     *
     * @param position la position de la cible
     * @return true si la cible est dans la portée de tir, false sinon
     */
    private boolean isWithinFiringRange(Position position) {
        return calculateDistanceToTarget(position) <= Parameters.bulletRange;
    }

    /**
     * Calcule l'angle de tir vers la cible.
     *
     * @param position la position de la cible
     * @return l'angle de tir en radians
     */
    private double calculateFiringAngle(Position position) {
        if (position.x == myPosition.x) {
            return (position.y > myPosition.y) ? Math.PI / 2 : 3 * Math.PI / 2;
        } else {
            double angle = Math.atan((double) (position.y - myPosition.y) / (position.x - myPosition.x));
            if (position.x < myPosition.x) {
                angle += Math.PI;
            } else if (position.y < myPosition.y) {
                angle += 2 * Math.PI;
            }
            return angle;
        }
    }

    /**
     * Calcule la distance jusqu'à la cible.
     *
     * @param position la position de la cible
     * @return la distance jusqu'à la cible
     */
    private int calculateDistanceToTarget(Position position) {
        return (int) Math.sqrt((position.y - myPosition.y) * (position.y - myPosition.y) + (position.x - myPosition.x) * (position.x - myPosition.x));
    }

    /**
     * Vérifie si un allié est dans la ligne de mire.
     *
     * @param firingAngle l'angle de tir
     * @return true si un allié est dans la ligne de mire, false sinon
     */
    private boolean isAllyInLineOfFire(double firingAngle) {
        for (Position allyPosition : allyPositions.values()) {
            double allyAngle = Math.atan((double) (allyPosition.y - myPosition.y) / (allyPosition.x - myPosition.x));
            if (Math.abs(normalizeRadian(allyAngle - firingAngle)) < FIRE_ANGLE_PRECISION) {
                return true;
            }
        }
        return false;
    }

    /**
     * Vérifie si une épave est dans la ligne de mire.
     *
     * @param firingAngle l'angle de tir
     * @return true si une épave est dans la ligne de mire, false sinon
     */
    private boolean isWreckInLineOfFire(double firingAngle) {
        for (Position wreckPosition : wrecksPositions) {
            double wreckAngle = Math.atan((double) (wreckPosition.y - myPosition.y) / (wreckPosition.x - myPosition.x));
            if (Math.abs(normalizeRadian(wreckAngle - firingAngle)) < FIRE_ANGLE_PRECISION) {
                return true;
            }
        }
        return false;
    }

    // Regarde quel est le meilleur bot à viser
    private void checkOptimiseTarget() {
        Position optimiseTargetPosition = new Position(-1, -1);

        for (Enemy enemy : enemyPositions.keySet()) {
            Position predictedPosition = predictEnemyPosition(enemy);
            if (optimiseTargetPosition.x == -1 && optimiseTargetPosition.y == -1) {
                optimiseTargetPosition = predictedPosition;
                continue;
            }

            if (calculateDistanceToTarget(predictedPosition) < calculateDistanceToTarget(optimiseTargetPosition)) {
                if (!isWreckInLineOfFire(calculateFiringAngle(predictedPosition))) {
                    optimiseTargetPosition = predictedPosition;
                }
            }
        }

        if (optimiseTargetPosition.x != -1 && optimiseTargetPosition.y != -1) {
            firePosition(optimiseTargetPosition);
            hasATarget = true;
        } else {
            hasATarget = false;
            targetPosition = new Position(-1, -1);
        }
    }

    // Fonctions de mouvement

    /**
     * Avance si rien n'est détecté devant.
     */
    private void moveForward() {
        if (detectFront().getObjectType() == IFrontSensorResult.Types.NOTHING) {
            move();
            updatePosition((int) Parameters.teamAMainBotSpeed);
            isMoving = true;
        }
    }

    /**
     * Recule et met à jour la position.
     */
    private void moveBackward() {
        moveBack();
        updatePosition(-(int) Parameters.teamAMainBotSpeed);
        isMoving = true;
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
                hasATarget = true;
                updateEnemyPosition(new Enemy(enemyX, enemyY, o.getObjectDirection(), Parameters.teamAMainBotSpeed), 0);
            }
            if (o.getObjectType() == IRadarResult.Types.Wreck) {
                if (o.getObjectDistance() == calculateDistanceToTarget(targetPosition)) {
                    hasATarget = false;
                }

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
     * @param enemy        l'ennemi à mettre à jour
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

    /**
     * Affiche les positions des ennemis dans les logs.
     */
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

    // Prédit la prochaine position de l'ennemi
    private Position predictEnemyPosition(Enemy enemy) {
        int predictedX = enemy.x + (int) (enemy.speed * Math.cos(enemy.direction));
        int predictedY = enemy.y + (int) (enemy.speed * Math.sin(enemy.direction));
        return new Position(predictedX, predictedY);
    }
}
