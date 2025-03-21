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
    private static final int BOT_RADIUS = (int) Parameters.teamAMainBotRadius;
    private static final int ALPHA = 0x001;
    private static final int BETA = 0x002;
    private static final int GAMMA = 0x003;


    // Constantes pour les types de messages échangés entre les robots
    private static final int DETECTION = 0x101;
    private static final int DEAD = 0x102;
    private static final int ELIMINATION = 0x103;
    private static final int SECONDARYINPOSITION = 0x104;
    private static final int UNUSED = 0x105;
    private static final int ALLY_POSITION = 0x106;

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
    private Bot currentBot;
    private boolean isTeamA = false;
    private INSTRUCTIONS instruction = INSTRUCTIONS.BASE;
    private static String robotName;
    private double directionToGo;
    private boolean isMoving = false;
    private boolean isSecondaryInPosition = false;
    private boolean isInPosition = false;

    // Variables pour gérer les ordres de tir
    private boolean fireOrder = false;
    private int fireRhythm = 0;

    // Compteurs pour suivre le nombre de robots vivants
    private int countMainAlive = 3;
    private int countSecondaryAlive = 2;

    // Variables pour suivre la position de la cible ennemie
    private static final int MAX_ENEMY_POSITION_TIME = 500;
    private static Position targetPosition = new Position(-1, -1);
    private boolean hasATarget = false;
    private int shootOnSameTarget = 8;
    private int countShootOnSameTarget = 0;

    // Variables pour suivre les positions des alliés, des ennemis et des épaves
    private ArrayList<Bot> allies = new ArrayList<>();
    private static ArrayList<Enemy> enemies = new ArrayList<>();
    private ArrayList<Position> wrecks = new ArrayList<>();

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

    /**
     * Initialise les variables et l'état du robot.
     */
    private void init() {

        // Déterminer sur quelle équipe le robot se trouve
        if (Parameters.teamAMainBotBrainClassName.contains("MecaMouse")) {
            isTeamA = true;
        } else {
            isTeamA = false;
        }

        // Déterminer le sens de déplacement initial
        if (isTeamA) {
            directionToGo = Parameters.EAST;
        } else {
            directionToGo = Parameters.WEST;
        }

        // Déterminer l'identité du robot
        int botID = GAMMA;
        for (IRadarResult result : detectRadar()) {
            if (isSameDirection(result.getObjectDirection(), Parameters.NORTH)) {
                botID = ALPHA;
                break;
            }
        }
        if (botID != GAMMA) {
            for (IRadarResult result : detectRadar()) {
                if (isSameDirection(result.getObjectDirection(), Parameters.SOUTH)) {
                    botID = BETA;
                    break;
                }
            }
        }

        // Déterminer le type de robot
        IRadarResult.Types botType = IRadarResult.Types.TeamMainBot;

        // Déterminer la position du robot en fonction de son identité
        Position botPosition = new Position(0, 0);
        switch (botID) {
            case ALPHA:
                if (isTeamA) {
                    botPosition = new Position((int) Parameters.teamAMainBot3InitX, (int) Parameters.teamAMainBot3InitY);
                } else {
                    botPosition = new Position((int) Parameters.teamBMainBot3InitX, (int) Parameters.teamBMainBot3InitY);
                }
                break;
            case BETA:
                if (isTeamA) {
                    botPosition = new Position((int) Parameters.teamAMainBot2InitX, (int) Parameters.teamAMainBot2InitY);
                } else {
                    botPosition = new Position((int) Parameters.teamBMainBot2InitX, (int) Parameters.teamBMainBot2InitY);
                }
                break;
            case GAMMA:
                if (isTeamA) {
                    botPosition = new Position((int) Parameters.teamAMainBot1InitX, (int) Parameters.teamAMainBot1InitY);
                } else {
                    botPosition = new Position((int) Parameters.teamBMainBot1InitX, (int) Parameters.teamBMainBot1InitY);
                }
                break;
            default:
                break;
        }

        // Déterminer l'orientation du robot
        double botHeading = myGetHeading();

        // Déterminer le nom
        robotName = switch (botID) {
            case ALPHA -> "#ALPHA";
            case BETA -> "#BETA";
            case GAMMA -> "#GAMMA";
            default -> "#UNKNOWN";
        };

        // Création du bot
        currentBot = new Bot(botID, botType, botPosition, botHeading);


    }

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
            switch (currentBot.id) {
                case BETA:
                    isInPosition = true;
                    break;
                case GAMMA:
                    if (currentBot.y > PLAYGROUND_HEIGHT / 3) {
                        if (isSameDirection(myGetHeading(), Parameters.NORTH)) {
                            moveForward();
                        } else {
                            if(isTeamA) {
                                stepTurn(Parameters.Direction.LEFT);
                            } else {
                                stepTurn(Parameters.Direction.RIGHT);
                            }
                        }
                    } else {
                        isInPosition = true;
                    }
                    break;
                case ALPHA:
                    if (currentBot.y < PLAYGROUND_HEIGHT - (PLAYGROUND_HEIGHT / 3)) {
                        if (isSameDirection(myGetHeading(), Parameters.SOUTH)) {
                            moveForward();
                        } else {
                            if(isTeamA) {
                                stepTurn(Parameters.Direction.RIGHT);
                            } else {
                                stepTurn(Parameters.Direction.LEFT);
                            }
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
                        if (o.getObjectDistance() >= Parameters.teamAMainBotFrontalDetectionRange) {
                            moveForward();
                        } else if(o.getObjectDistance() <= Parameters.teamAMainBotFrontalDetectionRange / 3) {
                            reverseDirectionToGo();
                        }else {
                            continue;
                        }
                    }
                } else {
                    reverseDirectionToGo();
                }
            } else {
                moveForward();
            }
        } else {
            if (currentBot.id == GAMMA) {
                if(isTeamA) {
                    stepTurn(Parameters.Direction.RIGHT);
                } else {
                    stepTurn(Parameters.Direction.LEFT);
                }
            } else {
                if(isTeamA) {
                    stepTurn(Parameters.Direction.LEFT);
                } else {
                    stepTurn(Parameters.Direction.RIGHT);
                }
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
        String[] parts = message.split("#");

        int bot = Integer.parseInt(parts[0]);
        int type = Integer.parseInt(parts[1]);
        String content = parts[2];

        switch (type) {
            case DETECTION:
                //processDetectionMessage(bot, content);
                break;
            case SECONDARYINPOSITION:
                isSecondaryInPosition = Boolean.parseBoolean(content);
                sendLogMessage(isSecondaryInPosition + "");
                break;
            case DEAD:
                processDeadMessage(content);
                break;
            case ALLY_POSITION:
                processAlliesPositionMessage(content);
                break;
            default:
                break;
        }
    }

    /**
     * Traite un message de détection d'un ennemi.
     *
     * @param bot     l'identifiant du robot ayant envoyé le message
     * @param message le message de détection
     */
    private void processDetectionMessage(int bot, String message) {
        String[] parts = message.split(":");

        int botId = Integer.parseInt(parts[0]);
        IRadarResult.Types botType = IRadarResult.Types.valueOf(parts[1]);
        Position botPosition = new Position(Integer.parseInt(parts[2]), Integer.parseInt(parts[3]));
        double botHeading = Double.parseDouble(parts[4]);
        int botTimeLastDetected = Integer.parseInt(parts[5]);

        Enemy enemy = new Enemy(botType, botPosition, botHeading, botTimeLastDetected);
        enemies.add(enemy);

        if (calculateDistanceToTarget(enemy.position) <= calculateDistanceToTarget(targetPosition)) {
            hasATarget = true;
            firePosition(enemy.position);
        }
    }

    /**
     * Traite un message de détection d'une épave.
     *
     * @param message le message de détection
     */
    private void processDeadMessage(String message) {
        String[] parts = message.split(":");
        Position wreckPosition = new Position(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]));
        addWreck(wreckPosition);
    }

    /**
     * Traite un message de position d'un allié.
     *
     * @param message le message de position
     */
    private void processAlliesPositionMessage(String message) {
        String[] parts = message.split(":");

        int botId = Integer.parseInt(parts[0]);
        IRadarResult.Types botType = IRadarResult.Types.valueOf(parts[1]);
        Position botPosition = new Position(Integer.parseInt(parts[2]), Integer.parseInt(parts[3]));
        double botHeading = Double.parseDouble(parts[4]);

        if (botId == currentBot.id) {
            return;
        }

        for (Bot allie : allies) {
            if (allie.id == botId) {
                allie.position = botPosition;
                allie.direction = botHeading;
                return;
            }
        }

        allies.add(new Bot(botId, botType, botPosition, botHeading));
    }

    /**
     * Envoie un message diffusé avec les informations spécifiées.
     *
     * @param type le type de message
     * @param arg1 le premier argument du message
     * @param arg2 le second argument du message
     */
    private void sendBroadcastMessage(int type, String message) {
        String toSend = currentBot.id + "#" + type + "#" + message;
        broadcast(toSend);
    }

    /**
     * Diffuse la position actuelle du robot.
     */
    private void broadcastOwnPosition() {
        sendBroadcastMessage(ALLY_POSITION, currentBot.toString());
    }

    /**
     * Affiche des informations de débogage dans les logs.
     */
    private void showLogDetails() {
        sendLogMessage("Fire order: " + fireOrder + " / Fire rhythm: " + fireRhythm + " / Has a target: " + hasATarget + " / " + isSecondaryInPosition);
        //sendLogMessage(currentBot.details());
    }

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
        int randomGap = (int) ((Math.random() * (2 * BOT_RADIUS) - BOT_RADIUS));
        if (fireRhythm == 0) {

            Position blindPosition;

            if (isTeamA) {
                blindPosition = new Position(currentBot.x + (int) Parameters.bulletRange - BOT_RADIUS, currentBot.y + randomGap);
            } else {
                blindPosition = new Position(currentBot.x - (int) Parameters.bulletRange + BOT_RADIUS, currentBot.y + randomGap);
            }

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
        if (position.x == currentBot.x) {
            return (position.y > currentBot.y) ? Math.PI / 2 : 3 * Math.PI / 2;
        } else {
            double angle = Math.atan((double) (position.y - currentBot.y) / (position.x - currentBot.x));
            if (position.x < currentBot.x) {
                angle += Math.PI;
            } else if (position.y < currentBot.y) {
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
        return (int) Math.sqrt((position.y - currentBot.y) * (position.y - currentBot.y) + (position.x - currentBot.x) * (position.x - currentBot.x));
    }

    /**
     * Vérifie si un allié est dans la ligne de mire.
     *
     * @param firingAngle l'angle de tir
     * @return true si un allié est dans la ligne de mire, false sinon
     */
    private boolean isAllyInLineOfFire(double firingAngle) {
        for (Bot allie : allies) {
            double allyAngle = Math.atan((double) (allie.y - currentBot.y) / (allie.x - currentBot.x));
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
        for (Position wreckPosition : wrecks) {
            double wreckAngle = Math.atan((double) (wreckPosition.y - currentBot.y) / (wreckPosition.x - currentBot.x));
            if (Math.abs(normalizeRadian(wreckAngle - firingAngle)) < FIRE_ANGLE_PRECISION) {
                return true;
            }
        }
        return false;
    }

    /**
     * Tire un projectile dans la direction spécifiée.
     *
     * @param angle l'angle de tir en radians
     */
    private void checkOptimiseTarget() {
        Position optimiseTargetPosition = new Position(-1, -1);

        for (Enemy enemy : enemies) {
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
        currentBot.x += (int) (speed * Math.cos(myGetHeading()));
        currentBot.y += (int) (speed * Math.sin(myGetHeading()));
    }

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

    /**
     * Détecte les objets avec le radar et diffuse les informations sur les ennemis.
     */
    private void radarDetection() {
        for (IRadarResult o : detectRadar()) {
            int enemyX = currentBot.x + (int) (o.getObjectDistance() * Math.cos(o.getObjectDirection()));
            int enemyY = currentBot.y + (int) (o.getObjectDistance() * Math.sin(o.getObjectDirection()));

            if (o.getObjectType() == IRadarResult.Types.OpponentMainBot || o.getObjectType() == IRadarResult.Types.OpponentSecondaryBot) {
                //sendBroadcastMessage(DETECTION, enemyX, enemyY, 0);
                hasATarget = true;
                Enemy enemy = new Enemy(o.getObjectType(), new Position(enemyX, enemyY), o.getObjectDirection(), 0);
                addNewEnemy(enemy);
            }
            if (o.getObjectType() == IRadarResult.Types.Wreck) {
                if (o.getObjectDistance() == calculateDistanceToTarget(targetPosition)) {
                    hasATarget = false;
                }

                Position position = new Position(enemyX, enemyY);
                addWreck(position);
                sendBroadcastMessage(DEAD, position.toString());

            }
        }
    }

    /**
     * Met à jour la position de l'ennemi.
     *
     * @param enemy        l'ennemi à mettre à jour
     * @param timeDetected le temps de détection
     */
    private void addNewEnemy(Enemy enemy) {
        ArrayList<Enemy> toRemove = new ArrayList<>();
        for (Enemy existingEnemy : enemies) {
            // Si la position est dans le rayon de la position déjà enregistrée
            if (existingEnemy.isInRadius(enemy) && enemy.timeLastDetected > existingEnemy.timeLastDetected) {
                toRemove.add(existingEnemy);
            }
        }
        for (Enemy enemyToRemove : toRemove) {
            removeEnnemyPosition(enemyToRemove);
        }

        enemies.add(enemy);

    }

    /**
     * Supprime la position de l'ennemi.
     *
     * @param enemy l'ennemi à supprimer
     */
    private void removeEnnemyPosition(Enemy enemy) {
        ArrayList<Enemy> toRemove = new ArrayList<>();
        for (Enemy existingEnemy : enemies) {
            if ((existingEnemy.equals(enemy) || existingEnemy.isInRadius(enemy)) && enemy.timeLastDetected < existingEnemy.timeLastDetected) {
                toRemove.add(existingEnemy);
            }
        }
        for (Enemy enemyToRemove : toRemove) {
            enemies.remove(enemyToRemove);
        }
    }

    /**
     * Incrémente le temps de position de l'ennemi.
     */
    private void incrementEnemyPositionTime() {

        ArrayList<Enemy> toRemove = new ArrayList<>();

        for (Enemy enemy1 : enemies) {

            // Si une épave est à la position
            for (Position wreck : wrecks) {
                if (enemy1.position == wreck) {
                    toRemove.add(enemy1);
                }
            }

            // Si le temps est dépassé
            if (enemy1.timeLastDetected >= MAX_ENEMY_POSITION_TIME) {
                toRemove.add(enemy1);
                continue;
            }

            for (Enemy enemy2 : enemies) {
                if (enemy1.equals(enemy2)) {
                    continue;
                }
                if (toRemove.contains(enemy1)) {
                    continue;
                }

                if (enemy1.isInRadius(enemy2)) {
                    if (enemy1.timeLastDetected > enemy2.timeLastDetected) {
                        toRemove.add(enemy1);
                    } else {
                        toRemove.add(enemy2);
                    }
                }
            }
        }

        enemies.removeAll(toRemove);
        broadcastEnemyPositions();
    }

    /**
     * Ajoute une épave à la liste des épaves.
     *
     * @param position la position de l'épave
     */
    private void addWreck(Position position) {
        boolean isNewWreck = true;
        for (Position wreck : wrecks) {
            if (wreck.equals(position) || wreck.isInRadius(position)) {
                isNewWreck = false;
            }
        }
        if (isNewWreck) {
            wrecks.add(position);
        }
    }

    /**
     * Broadcast les positions des ennemis
     */
    private void broadcastEnemyPositions() {
        for (Enemy enemy : enemies) {
            sendBroadcastMessage(DETECTION, enemy.toString());
        }
    }

    /**
     * Envoie un message de détection d'un ennemi.
     *
     * @param enemy l'ennemi à détecter
     */
    private Position predictEnemyPosition(Enemy enemy) {
        int predictedX = enemy.x + (int) (enemy.getSpeed() * Math.cos(enemy.direction) * enemy.timeLastDetected);
        int predictedY = enemy.y + (int) (enemy.getSpeed() * Math.sin(enemy.direction) * enemy.timeLastDetected);
        return new Position(predictedX, predictedY);
    }

    static class Position {
        int x, y;

        Position(int x, int y) {
            this.x = x;
            this.y = y;
        }

        Position(Position p) {
            this.x = p.x;
            this.y = p.y;
        }

        public void setPosition(Position p) {
            this.x = p.x;
            this.y = p.y;
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
            return Math.sqrt((x - position.x) * (x - position.x) + (y - position.y) * (y - position.y)) < BOT_RADIUS;
        }

        @Override
        public String toString() {
            return this.x + ":" + this.y;
        }
    }

    static class Bot extends Position {
        int id;
        IRadarResult.Types type;
        Position position;
        double direction;

        Bot(int id, IRadarResult.Types type, Position position, double direction) {
            super(position);
            this.id = id;
            this.type = type;
            this.position = position;
            this.direction = direction;
        }

        public int getSpeed() {
            if (type == IRadarResult.Types.TeamMainBot || type == IRadarResult.Types.OpponentMainBot) {
                return (int) Parameters.teamAMainBotSpeed;
            } else if (type == IRadarResult.Types.TeamSecondaryBot || type == IRadarResult.Types.OpponentSecondaryBot) {
                return (int) Parameters.teamASecondaryBotSpeed;
            } else {
                return 0;
            }
        }

        @Override
        public boolean isInRadius(Object o) {
            return super.isInRadius(o);
        }

        @Override
        public String toString() {
            return this.id + ":" + this.type + ":" + this.x + ":" + this.y + ":" + this.direction;
        }

        public String details() {
            return robotName + " [" + this.x + ", " + this.y + "] / Target [" + targetPosition.x + ", " + targetPosition.y + "] / " + enemies.size();
        }
    }

    static class Enemy extends Bot {
        int timeLastDetected;

        Enemy(IRadarResult.Types type, Position position, double direction, int timeLastDetected) {
            super(0, type, position, direction);
            this.timeLastDetected = timeLastDetected;
        }

        public void setTimeLastDetected(int timeLastDetected) {
            this.timeLastDetected = timeLastDetected;
        }

        public void incrementLastTimeDetected() {
            this.timeLastDetected++;
        }

        @Override
        public String toString() {
            return super.toString() + ":" + timeLastDetected;
        }

    }

}
