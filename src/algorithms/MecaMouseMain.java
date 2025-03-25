package algorithms;

import characteristics.IFrontSensorResult;
import characteristics.IRadarResult;
import characteristics.Parameters;
import robotsimulator.Brain;

import java.util.*;

public class MecaMouseMain extends Brain {
    // Constants for precision and identification
    private static final double ANGLE_PRECISION = 0.02;
    private static final double FIRE_ANGLE_PRECISION = Math.PI / 6;
    private static final int BOT_RADIUS = (int) Parameters.teamAMainBotRadius;
    private static final int ALPHA = 0x001;
    private static final int BETA = 0x002;
    private static final int GAMMA = 0x003;

    // Constants for message types exchanged between robots
    private static final int DETECTION = 0x101;
    private static final int DEAD = 0x102;
    private static final int ELIMINATION = 0x103;
    private static final int SECONDARYINPOSITION = 0x104;
    private static final int ALLY_POSITION = 0x106;

    // Enum for possible robot instructions
    private enum INSTRUCTIONS {
        BASE,           // Base instruction: at least one secondary is still alive
        THREE_ALIVE,    // Instruction when secondaries are dead and 3 main bots are alive
        TWO_ALIVE,      // Instruction when secondaries are dead and 2 main bots are alive
        ONE_ALIVE       // Instruction when secondaries are dead and 1 main bot is alive
    }

    // Playground dimensions
    private static final int PLAYGROUND_WIDTH = 3000;
    private static final int PLAYGROUND_HEIGHT = 2000;

    // Robot state variables
    private Bot currentBot;
    private boolean isTeamA = false;
    private INSTRUCTIONS instruction = INSTRUCTIONS.BASE;
    private String robotName;
    private double directionToGo;
    private boolean isSecondaryInPosition = false;
    private boolean isInPosition = false;

    // Firing control variables
    private boolean fireOrder = false;
    private int fireRhythm = 0;

    private boolean freezed = false;

    private static final int AVOID_TIME = 200;
    private int avoidTime = 0;
    private boolean avoided = false;
    private Position avoidWreck = new Position(-1, -1);

    // Enemy target tracking variables
    private static final int MAX_ENEMY_POSITION_TIME = 500;
    private Position targetPosition = new Position(-1, -1);
    private boolean isTargetMoving = false;
    private boolean hasATarget = false;

    // Lists for tracking allies, enemies, and wrecks
    private final ArrayList<Bot> allies = new ArrayList<>();
    private final ArrayList<Enemy> enemies = new ArrayList<>();
    private final ArrayList<Position> wrecks = new ArrayList<>();

    // Default constructor
    public MecaMouseMain() {
        super();
    }

    /**
     * Initializes the robot when activated.
     */
    @Override
    public void activate() {
        init();
    }

    /**
     * Executes one step of the robot's behavior.
     */
    @Override
    public void step() {
        showLogDetails();
        radarDetection();
        broadcastOwnPosition();
        readBroadcast();
        incrementEnemyPositionTime();
        checkOptimizeTarget();


        // Handle firing orders and execute current instruction
        if (handleFireOrder()) {
            return;
        }

        if(freezed) {
            fireAtTarget(targetPosition);
            return;
        }

        // Execute current instruction
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
     * Initializes variables and robot state.
     */
    private void init() {
        // Determine which team the robot is on
        isTeamA = Parameters.teamAMainBotBrainClassName.contains("MecaMouse");

        // Determine initial movement direction
        directionToGo = isTeamA ? Parameters.EAST : Parameters.WEST;

        // Determine robot identity
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

        // Determine robot type
        IRadarResult.Types botType = IRadarResult.Types.TeamMainBot;

        // Determine robot position based on identity
        Position botPosition = new Position(0, 0);
        switch (botID) {
            case ALPHA:
                botPosition = new Position(
                        isTeamA ? (int) Parameters.teamAMainBot3InitX : (int) Parameters.teamBMainBot3InitX,
                        isTeamA ? (int) Parameters.teamAMainBot3InitY : (int) Parameters.teamBMainBot3InitY
                );
                break;
            case BETA:
                botPosition = new Position(
                        isTeamA ? (int) Parameters.teamAMainBot2InitX : (int) Parameters.teamBMainBot2InitX,
                        isTeamA ? (int) Parameters.teamAMainBot2InitY : (int) Parameters.teamBMainBot2InitY
                );
                break;
            case GAMMA:
                botPosition = new Position(
                        isTeamA ? (int) Parameters.teamAMainBot1InitX : (int) Parameters.teamBMainBot1InitX,
                        isTeamA ? (int) Parameters.teamAMainBot1InitY : (int) Parameters.teamBMainBot1InitY
                );
                break;
        }

        // Determine robot heading
        double botHeading = myGetHeading();

        // Determine robot name
        robotName = switch (botID) {
            case ALPHA -> "#ALPHA";
            case BETA -> "#BETA";
            case GAMMA -> "#GAMMA";
            default -> "#UNKNOWN";
        };

        // Create bot
        currentBot = new Bot(botID, botType, botPosition, botHeading);

        // Clear tracking lists
        enemies.clear();
        wrecks.clear();
        allies.clear();
    }

    /**
     * Executes the base instruction: manages movement and firing based on target presence.
     */
    private void instructionBase() {
        // Blind fire if no target or target out of range
        if ((!hasATarget || calculateDistanceToTarget(targetPosition) > Parameters.bulletRange)
                && fireRhythm == 0 && isSecondaryInPosition) {
            blindStraightFire();
            return;
        }

        // Check if we need to adjust position relative to target
        if (hasATarget && isInPosition) {
            int distanceToTarget = calculateDistanceToTarget(targetPosition);
            if (distanceToTarget <= Parameters.bulletRange - 100 &&
                    distanceToTarget >= Parameters.bulletRange + 100) {
                return;
            }
        }

        // Initialize robot positions
        if (!isInPosition) {
            positionRobot();
            return;
        }

//        if(avoided) {
//            int avoidY = 0;
//            double newDirection = 0.0;
//            switch (currentBot.id) {
//                case GAMMA, BETA :
//                    avoidY = avoidWreck.y + (BOT_RADIUS * 2);
//                    newDirection = Parameters.NORTH;
//                default :
//                    avoidY = avoidWreck.y - (BOT_RADIUS * 2);
//                    newDirection = Parameters.SOUTH;
//            };
//
//            if(currentBot.position.y != avoidY) {
//                if(isSameDirection(myGetHeading(), newDirection)) {
//                    moveForward();
//                } else {
//                    stepTurn(currentBot.id == GAMMA ?
//                            (isTeamA ? Parameters.Direction.RIGHT : Parameters.Direction.LEFT) :
//                            (isTeamA ? Parameters.Direction.LEFT : Parameters.Direction.RIGHT));
//                }
//            } else {
//                if(isSameDirection(myGetHeading(), directionToGo)) {
//                    if(directionToGo == Parameters.EAST) {
//                        if(currentBot.position.x < avoidWreck.x + (2 * BOT_RADIUS)) {
//                            moveForward();
//                        } else {
//                            if(isSameDirection(myGetHeading(), newDirection + Math.PI/2)) {
//                                moveForward();
//                            } else {
//                                avoided = false;
//                                avoidWreck = new Position(-1,-1);
//                            }
//                        }
//                    } else {
//                        if(currentBot.position.x > avoidWreck.x - (2 * BOT_RADIUS)) {
//                            moveForward();
//                        } else {
//                            if(isSameDirection(myGetHeading(), newDirection + Math.PI/2)) {
//                                moveForward();
//                            } else {
//                                avoided = false;
//                                avoidWreck = new Position(-1,-1);
//                            }
//                        }
//                    }
//                } else {
//                    stepTurn(currentBot.id == GAMMA ?
//                            (isTeamA ? Parameters.Direction.RIGHT : Parameters.Direction.LEFT) :
//                            (isTeamA ? Parameters.Direction.LEFT : Parameters.Direction.RIGHT));
//                }
//            }
//        }

        // Handle movement
        moveRobot();
    }

    /**
     * Positions the robot based on its ID
     */
    private void positionRobot() {
        switch (currentBot.id) {
            case BETA:
                isInPosition = true;
                break;
            case GAMMA:
                if (currentBot.position.y > (int) (PLAYGROUND_HEIGHT / 3)) {
                    if (isSameDirection(myGetHeading(), Parameters.NORTH)) {
                        moveForward();
                    } else {
                        stepTurn(isTeamA ? Parameters.Direction.LEFT : Parameters.Direction.RIGHT);
                    }
                } else {
                    isInPosition = true;
                }
                break;
            case ALPHA:
                if (currentBot.position.y < (int) (PLAYGROUND_HEIGHT - (PLAYGROUND_HEIGHT / 3))) {
                    if (isSameDirection(myGetHeading(), Parameters.SOUTH)) {
                        moveForward();
                    } else {
                        stepTurn(isTeamA ? Parameters.Direction.RIGHT : Parameters.Direction.LEFT);
                    }
                } else {
                    isInPosition = true;
                }
                break;
        }
    }

    /**
     * Handles robot movement logic
     */
    private void moveRobot() {
        if (isSameDirection(myGetHeading(), directionToGo)) {
            if (detectFront().getObjectType() != IFrontSensorResult.Types.NOTHING) {
                if (detectFront().getObjectType() == IFrontSensorResult.Types.TeamMainBot ||
                        detectFront().getObjectType() == IFrontSensorResult.Types.TeamSecondaryBot) {
                    handleTeammateFront();
                } else {
                    reverseDirectionToGo();
                }
            } else {
                moveForward();
            }
        } else {
            stepTurn(currentBot.id == GAMMA ?
                    (isTeamA ? Parameters.Direction.RIGHT : Parameters.Direction.LEFT) :
                    (isTeamA ? Parameters.Direction.LEFT : Parameters.Direction.RIGHT));
        }
    }

    /**
     * Handles the case when a teammate is detected in front
     */
    private void handleTeammateFront() {
        for (IRadarResult o : detectRadar()) {
            if (o.getObjectDistance() >= Parameters.teamAMainBotFrontalDetectionRange) {
                moveForward();
            } else if (o.getObjectDistance() <= Parameters.teamAMainBotFrontalDetectionRange / 3) {
                reverseDirectionToGo();
            }
        }
    }

    /**
     * Implementation for THREE_ALIVE instruction.
     */
    private void instructionThreeAlive() {
        // Implementation for THREE_ALIVE instruction
    }

    /**
     * Implementation for TWO_ALIVE instruction.
     */
    private void instructionTwoAlive() {
        // Implementation for TWO_ALIVE instruction
    }

    /**
     * Implementation for ONE_ALIVE instruction.
     */
    private void instructionOneAlive() {
        // Implementation for ONE_ALIVE instruction
    }

    /**
     * Reads all broadcast messages and processes them.
     */
    private void readBroadcast() {
        ArrayList<String> messages = fetchAllMessages();
        for (String message : messages) {
            processMessage(message);
        }
    }

    /**
     * Processes a broadcast message to update robot state.
     *
     * @param message the message to process
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
        }
    }

    /**
     * Processes an enemy detection message.
     *
     * @param bot     the ID of the robot that sent the message
     * @param message the detection message
     */
    private void processDetectionMessage(int bot, String message) {
        String[] parts = message.split(":");

        IRadarResult.Types botType = IRadarResult.Types.valueOf(parts[1]);
        Position botPosition = new Position(Integer.parseInt(parts[2]), Integer.parseInt(parts[3]));
        double botHeading = Double.parseDouble(parts[4]);
        int botTimeLastDetected = Integer.parseInt(parts[5]);
        boolean botIsMoving = Boolean.parseBoolean(parts[6]);

        Enemy enemy = new Enemy(botType, botPosition, botHeading, botTimeLastDetected, botIsMoving);
        addNewEnemy(enemy);
    }

    /**
     * Processes a wreck detection message.
     *
     * @param message the detection message
     */
    private void processDeadMessage(String message) {
        String[] parts = message.split(":");
        Position wreckPosition = new Position(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]));
        addWreck(wreckPosition);
    }

    /**
     * Processes an ally position message.
     *
     * @param message the position message
     */
    private void processAlliesPositionMessage(String message) {
        String[] parts = message.split(":");

        // Decode message parts
        int botId = Integer.parseInt(parts[0]);
        IRadarResult.Types botType = IRadarResult.Types.valueOf(parts[1]);
        Position botPosition = new Position(Integer.parseInt(parts[2]), Integer.parseInt(parts[3]));
        double botHeading = Double.parseDouble(parts[4]);

        // No need to process own position
        if (botId == currentBot.id) {
            return;
        }

        // Update ally position
        for (Bot ally : allies) {
            if (ally.id == botId) {
                ally.position = botPosition;
                ally.direction = botHeading;
                return;
            }
        }

        allies.add(new Bot(botId, botType, botPosition, botHeading));
    }

    /**
     * Sends a broadcast message.
     */
    private void sendBroadcastMessage(int type, String message) {
        String toSend = currentBot.id + "#" + type + "#" + message;
        broadcast(toSend);
    }

    /**
     * Broadcasts the current robot's position.
     */
    private void broadcastOwnPosition() {
        sendBroadcastMessage(ALLY_POSITION, currentBot.toString());
    }

    /**
     * Displays debug information in logs.
     */
    private void showLogDetails() {
        sendLogMessage(robotName + " [" + currentBot.position + "] / Target: " + targetPosition + " (" + isTargetMoving + ")");
    }

    /**
     * Updates target position and activates firing order.
     *
     * @param position the target position
     */
    private void firePosition(Position position) {
        targetPosition = position;
        fireOrder = true;
    }

    /**
     * Fires straight ahead without a specific target.
     */
    private void blindStraightFire() {
        int randomGap = (int) ((Math.random() * (2 * BOT_RADIUS) - BOT_RADIUS));
        if (fireRhythm == 0) {
            Position blindPosition = new Position(
                    isTeamA ?
                            currentBot.position.x + (int) Parameters.bulletRange - BOT_RADIUS :
                            currentBot.position.x - (int) Parameters.bulletRange + BOT_RADIUS,
                    currentBot.position.y + randomGap
            );

            fireAtTarget(blindPosition);
            fireRhythm++;
            return;
        }
        fireRhythm++;
        if (fireRhythm >= Parameters.bulletFiringLatency) fireRhythm = 0;
    }

    /**
     * Handles firing order if a target is present.
     *
     * @return true if a shot was fired, false otherwise
     */
    private boolean handleFireOrder() {
        if (fireOrder && fireRhythm == 0 && hasATarget) {
            fireRhythm++;
            if (fireAtTarget(targetPosition)) {
                return true;
            }
        }
        fireRhythm++;
        if (fireRhythm >= Parameters.bulletFiringLatency) fireRhythm = 0;
        return false;
    }

    /**
     * Fires at the target if it's in range and there's no ally in the line of fire.
     *
     * @param position the target position
     * @return true if fired, false otherwise
     */
    private boolean fireAtTarget(Position position) {
        int randomGapX = (int) (( (Math.random()-0.5) * (2 * BOT_RADIUS)));
        int randomGapY = (int) (( (Math.random()-0.5) * (2 * BOT_RADIUS)));

        System.out.println(randomGapX + " / " + randomGapY);

        Position newPosition = new Position(position.x + randomGapX, position.y + randomGapY);

        double angle = calculateFiringAngle(newPosition);
        boolean hasToFire = isWithinFiringRange(newPosition) &&
                !isAllyInLineOfFire(angle) &&
                !isWreckInLineOfFire(angle);
        if (hasToFire) {
            fire(angle);
        }
        return hasToFire;
    }

    /**
     * Checks if the target is within firing range.
     *
     * @param position the target position
     * @return true if the target is within firing range, false otherwise
     */
    private boolean isWithinFiringRange(Position position) {
        return calculateDistanceToTarget(position) <= Parameters.bulletRange;
    }

    /**
     * Calculates the firing angle to the target.
     *
     * @param position the target position
     * @return the firing angle in radians
     */
    private double calculateFiringAngle(Position position) {
        if (position.x == currentBot.position.x) {
            return (position.y > currentBot.position.y) ? Math.PI / 2 : 3 * Math.PI / 2;
        } else {
            double angle = Math.atan((double) (position.y - currentBot.position.y) / (position.x - currentBot.position.x));
            if (position.x < currentBot.position.x) {
                angle += Math.PI;
            } else if (position.y < currentBot.position.y) {
                angle += 2 * Math.PI;
            }
            return angle;
        }
    }

    /**
     * Calculates the distance to the target.
     *
     * @param position the target position
     * @return the distance to the target
     */
    private int calculateDistanceToTarget(Position position) {
        return (int) Math.sqrt(Math.pow(position.y - currentBot.position.y, 2) +
                Math.pow(position.x - currentBot.position.x, 2));
    }

    /**
     * Checks if an ally is in the line of fire.
     *
     * @param firingAngle the firing angle
     * @return true if an ally is in the line of fire, false otherwise
     */
    private boolean isAllyInLineOfFire(double firingAngle) {
        for (Bot ally : allies) {
            double allyAngle = calculateFiringAngle(ally.position);
            if (Math.abs(normalizeRadian(allyAngle - firingAngle)) < FIRE_ANGLE_PRECISION) {
                return true;
            }
        }
        return false;
    }

    /**
     * Checks if a wreck is in the line of fire.
     *
     * @param firingAngle the firing angle
     * @return true if a wreck is in the line of fire, false otherwise
     */
    private boolean isWreckInLineOfFire(double firingAngle) {
        for (Position wreckPosition : wrecks) {
            double wreckAngle = calculateFiringAngle(wreckPosition);
            if (Math.abs(normalizeRadian(wreckAngle - firingAngle)) < FIRE_ANGLE_PRECISION) {
                return true;
            }
        }
        return false;
    }

    /**
     * Checks and optimizes the target.
     */
    private void checkOptimizeTarget() {
        Position optimizeTargetPosition = new Position(-1, -1);

        for (Enemy enemy : enemies) {
            Position predictedPosition = predictEnemyPosition(enemy);
            if (optimizeTargetPosition.x == -1 && optimizeTargetPosition.y == -1) {
                optimizeTargetPosition = predictedPosition;
            }

            if (calculateDistanceToTarget(predictedPosition) <= calculateDistanceToTarget(optimizeTargetPosition)) {
                double fireAngle = calculateFiringAngle(predictedPosition);
                if (!isWreckInLineOfFire(fireAngle) && !isAllyInLineOfFire(fireAngle)) {
                    optimizeTargetPosition = predictedPosition;
                    isTargetMoving = enemy.isMoving;
                }
            }
        }

        if (optimizeTargetPosition.x != -1 && optimizeTargetPosition.y != -1) {
            firePosition(optimizeTargetPosition);
            hasATarget = true;
        } else {
            hasATarget = false;
            fireOrder = false;
            targetPosition = new Position(-1, -1);
        }

        if (hasATarget && calculateDistanceToTarget(targetPosition) <= Parameters.bulletRange - 150 && calculateDistanceToTarget(targetPosition) >= 150) {
            freezed = true;
        } else {
            freezed = false;
        }

    }

    /**
     * Moves forward if nothing is detected in front.
     */
    private void moveForward() {
        if (detectFront().getObjectType() == IFrontSensorResult.Types.NOTHING) {
            move();
            updatePosition((int) Parameters.teamAMainBotSpeed);
        }
    }

    /**
     * Moves backward and updates position.
     */
    private void moveBackward() {

        // Vérifier si le robot ne sort pas du terrain
        if(currentBot.position.x - (int) (Parameters.teamASecondaryBotSpeed * Math.cos(myGetHeading())) < 0 || currentBot.position.y - (int) (Parameters.teamASecondaryBotSpeed * Math.sin(myGetHeading())) < 0) {
            return;
        }
        moveBack();
        updatePosition(-(int) Parameters.teamASecondaryBotSpeed);
    }

    /**
     * Reverses the movement direction.
     */
    private void reverseDirectionToGo() {
        directionToGo = normalizeRadian(directionToGo + Math.PI);
    }

    /**
     * Updates position based on speed and direction.
     *
     * @param speed the movement speed
     */
    private void updatePosition(int speed) {
        currentBot.position.x += (int) (speed * Math.cos(myGetHeading()));
        currentBot.position.y += (int) (speed * Math.sin(myGetHeading()));
    }

    /**
     * Checks if two directions are similar.
     *
     * @param dir1 the first direction
     * @param dir2 the second direction
     * @return true if the directions are similar, false otherwise
     */
    private boolean isSameDirection(double dir1, double dir2) {
        return Math.abs(normalizeRadian(dir1) - normalizeRadian(dir2)) < ANGLE_PRECISION;
    }

    /**
     * Normalizes an angle in radians to be between 0 and 2π.
     *
     * @param angle the angle to normalize
     * @return the normalized angle
     */
    private static double normalizeRadian(double angle) {
        double result = angle;
        while (result < 0) result += 2 * Math.PI;
        while (result >= 2 * Math.PI) result -= 2 * Math.PI;
        return result;
    }

    /**
     * Returns the current normalized direction.
     *
     * @return the current direction in radians
     */
    private double myGetHeading() {
        return normalizeRadian(getHeading());
    }

    /**
     * Detects objects with radar and broadcasts information about enemies.
     */
    private void radarDetection() {
        for (IRadarResult o : detectRadar()) {
            int enemyX = currentBot.position.x + (int) (o.getObjectDistance() * Math.cos(o.getObjectDirection()));
            int enemyY = currentBot.position.y + (int) (o.getObjectDistance() * Math.sin(o.getObjectDirection()));

            if (o.getObjectType() == IRadarResult.Types.OpponentMainBot ||
                    o.getObjectType() == IRadarResult.Types.OpponentSecondaryBot) {
                hasATarget = true;
                Enemy enemy = new Enemy(o.getObjectType(), new Position(enemyX, enemyY),
                        o.getObjectDirection(), 0, false);
                addNewEnemy(enemy);
                sendBroadcastMessage(DETECTION, enemy.toString());
            }
            if (o.getObjectType() == IRadarResult.Types.Wreck) {
                if (o.getObjectDistance() == calculateDistanceToTarget(targetPosition)) {
                    hasATarget = false;
                }

                Position wreck = new Position(enemyX, enemyY);
                addWreck(wreck);
                sendBroadcastMessage(DEAD, wreck.toString());

                if(isSameDirection(calculateFiringAngle(wreck), directionToGo) && calculateDistanceToTarget(wreck) < 2 * BOT_RADIUS) {
                    avoided = true;
                    avoidWreck = wreck;
                }
            }
        }
    }

    /**
     * Updates the enemy position.
     *
     * @param enemy the enemy to update
     */
    private void addNewEnemy(Enemy enemy) {
        ArrayList<Enemy> toRemove = new ArrayList<>();
        for (Enemy existingEnemy : enemies) {
            if (existingEnemy.isInRadius(enemy)) {
                toRemove.add(existingEnemy);
            }
        }

        enemies.removeAll(toRemove);
        enemies.add(enemy);
    }

    /**
     * Increments the enemy position time and removes outdated enemies.
     */
    private void incrementEnemyPositionTime() {
        ArrayList<Enemy> toRemove = new ArrayList<>();

        for (Enemy enemy1 : enemies) {
            enemy1.incrementLastTimeDetected();

            // Check if a wreck is at the position
            for (Position wreck : wrecks) {
                if (enemy1.isInRadius(wreck)) {
                    toRemove.add(enemy1);
                    break;
                }
            }

            // Check if time exceeded
            if (enemy1.timeLastDetected >= MAX_ENEMY_POSITION_TIME) {
                toRemove.add(enemy1);
                continue;
            }

            // Check for duplicate enemies
            for (Enemy enemy2 : enemies) {
                if (enemy1 == enemy2 || toRemove.contains(enemy1)) {
                    continue;
                }

                if (enemy1.isInRadius(enemy2)) {
                    if (enemy1.timeLastDetected > enemy2.timeLastDetected) {
                        toRemove.add(enemy1);
                        if(enemy1.position == enemy2.position) {
                            enemy2.isMoving = false;
                        } else {
                            enemy2.isMoving = true;
                        }
                    } else {
                        toRemove.add(enemy2);
                        if(enemy1.position == enemy2.position) {
                            enemy1.isMoving = false;
                        } else {
                            enemy1.isMoving = true;
                        }
                    }
                }
            }
        }

        enemies.removeAll(toRemove);
        broadcastEnemyPositions();
    }

    /**
     * Adds a wreck to the list of wrecks.
     *
     * @param position the position of the wreck
     */
    private void addWreck(Position position) {
        // Add wreck if it doesn't already exist
        boolean isNewWreck = true;
        for (Position wreck : wrecks) {
            if (wreck.equals(position) || wreck.isInRadius(position)) {
                isNewWreck = false;
                break;
            }
        }

        if (isNewWreck) {
            wrecks.add(position);
        }

        // Remove enemies at the wreck position
        ArrayList<Enemy> toRemove = new ArrayList<>();
        for (Enemy existingEnemy : enemies) {
            if (position.isInRadius(existingEnemy.position)) {
                toRemove.add(existingEnemy);
            }
        }

        enemies.removeAll(toRemove);
    }

    /**
     * Broadcasts enemy positions.
     */
    private void broadcastEnemyPositions() {
        for (Enemy enemy : enemies) {
            sendBroadcastMessage(DETECTION, enemy.toString());
        }
    }

    /**
     * Predicts enemy position based on its speed and direction.
     *
     * @param enemy the enemy to predict
     * @return the predicted position
     */
    private Position predictEnemyPosition(Enemy enemy) {
        int predictedX = 0;
        int predictedY = 0;

        if(enemy.isMoving) {
            predictedX = enemy.x + (int) (enemy.getSpeed() * Math.cos(enemy.direction) * enemy.timeLastDetected);
            predictedY = enemy.y + (int) (enemy.getSpeed() * Math.sin(enemy.direction) * enemy.timeLastDetected);
        } else {
            predictedX = enemy.x;
            predictedY = enemy.y;
        }

        return new Position(predictedX, predictedY);
    }

    private void printEnemies() {
        String message = "";
        for (Enemy enemy : enemies) {
            message += " [" + enemy.position.toString() + " / " + enemy.timeLastDetected + "] ";
        }
        System.out.println(message);
    }

    /**
     * Position class for representing coordinates.
     */
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
            if (o == null || getClass() != o.getClass()) return false;
            Position position = (Position) o;
            return Math.sqrt(Math.pow((x - position.x), 2) + Math.pow((y - position.y), 2)) <= BOT_RADIUS;
        }

        @Override
        public String toString() {
            return this.x + ":" + this.y;
        }
    }

    /**
     * Bot class for representing robots.
     */
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
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Bot bot = (Bot) o;
            return this.position.equals(bot.position);
        }

        @Override
        public String toString() {
            return this.id + ":" + this.type + ":" + this.x + ":" + this.y + ":" + this.direction;
        }

        public String beautify() {
            return " [" + this.position.toString() + "]" ;
        }
    }

    /**
     * Enemy class for representing opponent robots.
     */
    static class Enemy extends Bot {
        int timeLastDetected;
        boolean isMoving;

        Enemy(IRadarResult.Types type, Position position, double direction, int timeLastDetected, boolean isMoving) {
            super(0, type, position, direction);
            this.timeLastDetected = timeLastDetected;
            this.isMoving = isMoving;
        }

        public void setTimeLastDetected(int timeLastDetected) {
            this.timeLastDetected = timeLastDetected;
        }

        public void incrementLastTimeDetected() {
            this.timeLastDetected++;
        }

        @Override
        public String toString() {
            return super.toString() + ":" + timeLastDetected + ':' + isMoving;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Enemy enemy = (Enemy) o;
            return this.position.equals(enemy.position);
        }
    }
}