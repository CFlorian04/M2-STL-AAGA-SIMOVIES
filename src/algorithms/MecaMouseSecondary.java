package algorithms;

import characteristics.IFrontSensorResult;
import characteristics.IRadarResult;
import characteristics.Parameters;
import robotsimulator.Brain;

import java.util.*;

public class MecaMouseSecondary extends Brain {
    // Constantes de précision et d'identification
    private static final double ANGLE_PRECISION = 0.05;
    private static final double FIRE_ANGLE_PRECISION = Math.PI / 6;
    private static final int BOT_RADIUS = (int) Parameters.teamAMainBotRadius;
    private static final int ROCKY = 0x004;
    private static final int MARIO = 0x005;

    // Constantes pour les types de messages échangés entre les robots
    private static final int DETECTION = 0x101;
    private static final int DEAD = 0x102;
    private static final int ELIMINATION = 0x103;
    private static final int SECONDARYINPOSITION = 0x104;
    private static final int UNUSED = 0x105;
    private static final int ALLY_POSITION = 0x106;

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
    private Bot currentBot;
    private boolean isTeamA = false;
    private INSTRUCTIONS instruction = INSTRUCTIONS.BASE;
    private String robotName;
    private double directionToGo;
    private boolean isMoving = false;
    private boolean isSecondaryInPosition = false;
    private boolean isInPosition = false;

    // Compteurs pour suivre le nombre de robots vivants
    private int countMainAlive = 3;
    private int countSecondaryAlive = 2;

    private static final int ESCAPE_TIME = 200;
    private int escapeTime = 0;
    private boolean isEscaping = false;

    // Variables pour suivre la position de la cible ennemie
    private static final int MAX_ENEMY_POSITION_TIME = 500;

    // Variables pour suivre les positions des alliés, des ennemis et des épaves
    private ArrayList<Bot> allies = new ArrayList<>();
    private ArrayList<Enemy> enemies = new ArrayList<>();
    private ArrayList<Position> wrecks = new ArrayList<>();


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
        broadcastOwnPosition(); // Diffusion de la position actuelle du robot

        readBroadcast(); // Lecture des messages diffusés par les autres robots

        incrementEnemyPositionTime(); // Incrémentation du temps de position des ennemis

        isMoving = false;

        if (isEscaping) {
            escapeTime++;
            moveBackward();
            if (escapeTime >= ESCAPE_TIME) {
                isEscaping = false;
                escapeTime = 0;
            }
            return;
        }

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
        int botID = ROCKY;
        for (IRadarResult o : detectRadar()) {
            if (isSameDirection(o.getObjectDirection(), Parameters.NORTH)) {
                botID = MARIO;
            }
        }

        // Déterminer le type de robot
        IRadarResult.Types botType = IRadarResult.Types.TeamSecondaryBot;

        // Déterminer la position du robot en fonction de son identité
        Position botPosition = new Position(0, 0);
        switch (botID) {
            case ROCKY:
                if (isTeamA) {
                    botPosition = new Position((int) Parameters.teamASecondaryBot1InitX, (int) Parameters.teamASecondaryBot1InitY);
                } else {
                    botPosition = new Position((int) Parameters.teamBSecondaryBot1InitX, (int) Parameters.teamBSecondaryBot1InitY);
                }
                break;
            case MARIO:
                if (isTeamA) {
                    botPosition = new Position((int) Parameters.teamASecondaryBot2InitX, (int) Parameters.teamASecondaryBot2InitY);
                } else {
                    botPosition = new Position((int) Parameters.teamBSecondaryBot2InitX, (int) Parameters.teamBSecondaryBot2InitY);
                }
                break;
            default:
                break;
        }

        // Déterminer l'orientation du robot
        double botHeading = myGetHeading();

        // Déterminer le nom
        robotName = switch (botID) {
            case ROCKY -> "#ROCKY";
            case MARIO -> "#MARIO";
            default -> "#UNKNOWN";
        };

        // Création du bot
        currentBot = new Bot(botID, botType, botPosition, botHeading);

        enemies.clear();
        wrecks.clear();
        allies.clear();
    }

    /**
     * Exécute l'instruction de base : gère le mouvement en fonction de l'identité du robot.
     */
    private void instruction_base() {
        if (currentBot.id == ROCKY) {
            // Se mettre sur la bonne ligne
            if (currentBot.position.y >= Parameters.teamASecondaryBotFrontalDetectionRange) {
                if (isSameDirection(myGetHeading(), Parameters.NORTH)) {
                    moveForward();
                } else {
                    if (isTeamA) {
                        stepTurn(Parameters.Direction.LEFT);
                    } else {
                        stepTurn(Parameters.Direction.RIGHT);
                    }
                }
                return;
            }
            if (!isSecondaryInPosition) {
                isSecondaryInPosition = true;
                sendBroadcastMessage(SECONDARYINPOSITION, "true");
            }
        }

        if (currentBot.id == MARIO) {
            // Se mettre sur la bonne ligne
            if (currentBot.position.y <= PLAYGROUND_HEIGHT - Parameters.teamASecondaryBotFrontalDetectionRange) {
                if (isSameDirection(myGetHeading(), Parameters.SOUTH)) {
                    moveForward();
                } else {
                    if (isTeamA) {
                        stepTurn(Parameters.Direction.RIGHT);
                    } else {
                        stepTurn(Parameters.Direction.LEFT);
                    }
                }
                return;
            }

            if (!isSecondaryInPosition) {
                isSecondaryInPosition = true;
                sendBroadcastMessage(SECONDARYINPOSITION, "true");
            }
        }

        if (isSameDirection(myGetHeading(), directionToGo)) {
            if (detectFront().getObjectType() != IFrontSensorResult.Types.NOTHING) {
                reverseDirectionToGo();
            } else {
                moveForward();
            }
        } else {
            if (currentBot.id == ROCKY) {
                if (isTeamA) {
                    stepTurn(Parameters.Direction.RIGHT);
                } else {
                    stepTurn(Parameters.Direction.LEFT);
                }
            } else {
                if (isTeamA) {
                    stepTurn(Parameters.Direction.LEFT);
                } else {
                    stepTurn(Parameters.Direction.RIGHT);
                }
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
                processDetectionMessage(bot, content);
                break;
            case DEAD:
                processDeadMessage(content);
                break;
            case ALLY_POSITION:
                processAlliesPositionMessage(content);
                break;
            default:
                // Type de message inconnu, ignore
                break;
        }
    }

    /**
     * Traite un message de détection d'un ennemi.
     *
     * @param bot     l'identifiant du robot ayant détecté l'ennemi
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
        addNewEnemy(enemy);

    }

    /**
     * Traite un message de mort d'un ennemi.
     *
     * @param message le message de mort
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

        Bot newAllie = new Bot(botId, botType, botPosition, botHeading);

        for (Bot allie : allies) {
            if (allie.id == botId) {
                allie.position = botPosition;
                allie.direction = botHeading;
                return;
            }
        }

        allies.add(newAllie);
    }

    /**
     * Envoie un message en broadcast.
     *
     * @param message le message à envoyer
     */
    private void sendBroadcastMessage(int type, String message) {
        String toSend = currentBot.id + "#" + type + "#" + message;
        broadcast(toSend);
        if (type == SECONDARYINPOSITION) {
            sendLogMessage(toSend);
        }
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
        sendLogMessage(robotName + " [" + currentBot.position.toString() + "]");
    }

    /**
     * Calcule la distance jusqu'à la cible.
     *
     * @param position la position de la cible
     * @return la distance jusqu'à la cible
     */
    private int calculateDistanceToTarget(Position position) {
        return (int) Math.sqrt((position.y - currentBot.position.y) * (position.y - currentBot.position.y) + (position.x - currentBot.position.x) * (position.x - currentBot.position.x));
    }

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

        // Vérifier si le robot ne sort pas du terrain
        if(currentBot.position.x - (int) (Parameters.teamASecondaryBotSpeed * Math.cos(myGetHeading())) < 0 || currentBot.position.y - (int) (Parameters.teamASecondaryBotSpeed * Math.sin(myGetHeading())) < 0) {
            return;
        }

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
        currentBot.position.x += (int) (speed * Math.cos(myGetHeading()));
        currentBot.position.y += (int) (speed * Math.sin(myGetHeading()));
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
            int enemyX = currentBot.position.x + (int) (o.getObjectDistance() * Math.cos(o.getObjectDirection()));
            int enemyY = currentBot.position.y + (int) (o.getObjectDistance() * Math.sin(o.getObjectDirection()));

            if (o.getObjectType() == IRadarResult.Types.OpponentMainBot || o.getObjectType() == IRadarResult.Types.OpponentSecondaryBot) {

                Enemy enemy = new Enemy(o.getObjectType(), new Position(enemyX, enemyY), o.getObjectDirection(), 0);
                addNewEnemy(enemy);
                sendBroadcastMessage(DETECTION, enemy.toString());

                if (o.getObjectDistance() <= Parameters.teamASecondaryBotFrontalDetectionRange - 200) {
                    isEscaping = true;
                }
            }
            if (o.getObjectType() == IRadarResult.Types.Wreck) {

                Position position = new Position(enemyX, enemyY);
                addWreck(position);
                sendBroadcastMessage(DEAD, position.toString());

            }
        }
    }

    /**
     * Ajoute une nouvelle position d'ennemi.
     *
     * @param enemy la nouvelle position de l'ennemi
     */
    private void addNewEnemy(Enemy enemy) {
        ArrayList<Enemy> toRemove = new ArrayList<>();
        for (Enemy existingEnemy : enemies) {
            if (existingEnemy.isInRadius(enemy)) {
                toRemove.add(existingEnemy);
            }
        }
        for (Enemy enemyToRemove : toRemove) {
            enemies.remove(enemyToRemove);
        }

        //System.out.println("Removing " + toRemove.size() + " enemies / Remaining : " + enemies.size());
        //System.out.println(robotName + "# " + enemy.toString());

        enemies.add(enemy);
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

        // Ajouter l'épave si elle n'existe pas déjà
        for (Position wreck : wrecks) {
            if (wreck.equals(position) || wreck.isInRadius(position)) {
                isNewWreck = false;
            }
        }
        if (isNewWreck) {
            wrecks.add(position);
        }

        // Retirer les ennemis à la position de l'épave
        ArrayList<Enemy> toRemove = new ArrayList<>();
        for (Enemy existingEnemy : enemies) {
            if (position.isInRadius(existingEnemy.position)) {
                toRemove.add(existingEnemy);
            }
        }

        for (Enemy enemyToRemove : toRemove) {
            enemies.remove(enemyToRemove);
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

    static class Position extends MecaMouseMain.Position {
        public Position(int x, int y) {
            super(x, y);
        }
    }

    static class Bot extends MecaMouseMain.Bot {
        public Bot(int id, IRadarResult.Types type, Position position, double heading) {
            super(id, type, position, heading);
        }
    }

    static class Enemy extends MecaMouseMain.Enemy {
        public Enemy(IRadarResult.Types type, Position position, double direction, int timeLastDetected) {
            super(type, position, direction, timeLastDetected);
        }
    }
}
