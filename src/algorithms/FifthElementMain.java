//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package algorithms;

import characteristics.IRadarResult;
import characteristics.IRadarResult.Types;
import characteristics.Parameters.Direction;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Random;
import robotsimulator.Brain;

public class FifthElementMain extends Brain {
    private boolean isLeftTeam = true;
    private static final double ANGLEPRECISION = 0.001;
    private static final double ANGLEPRECISIONBIS = 0.01;
    private static final double DISTANCEPRECISION = (double)10.0F;
    private static final double SAMELINEPRECISION = (double)125.0F;
    private static final double MAIN = -1.431633921E9;
    private static final double SECONDARY = (double)-21846.0F;
    private static final int ROCKY = 2014683;
    private static final int MARIO = 24269;
    private static final int ALPHA = 2010586;
    private static final int BETA = 24256;
    private static final int GAMMA = 819;
    private static final int TEAM = 12246445;
    private static final int UNDEFINED = -1159983648;
    private static final int FIRE = 2898;
    private static final int POSITION = 32343;
    private static final int OVER = -1073737473;
    private static final int MOVETASK = 1;
    private static final int STANDINGFIRINGTASK = 2;
    private static final int BACKWARDFIRINGTASK = 3;
    private static final int MOVEBACKTASK = 5;
    private static final int TURNLEFTTASK = 6;
    private static final int TURNRIGHTTASK = 7;
    private static final int STARTINGTASK = 8;
    private static final int HUNTINGTASK = 9;
    private static final int TURNNORTHTASK = 10;
    private static final int TURNSOUTHTASK = 11;
    private static final int TURNEASTTASK = 12;
    private static final int TURNWESTTASK = 13;
    private static final int SINK = -1159983647;
    private int state;
    private int fireStep;
    private double myX;
    private double myY;
    private boolean isMoving;
    private int whoAmI;
    private double targetX;
    private double targetY;
    private boolean fireOrder;
    private Random rand;
    private HashMap<Integer, ArrayList<Double>> allies = new HashMap();
    private ArrayList<ArrayList<Double>> targets;
    private int stepNumberLastFire;
    private int stepNumber;
    private int stepNumberMoveBack;
    private double endTaskDirection;
    private String huntingMode;
    private boolean findNewShotAngle;
    private boolean isMovingBack;
    private int counter;

    public FifthElementMain() {
        ArrayList<Double> temp = new ArrayList(3);
        temp.add((double)0.0F);
        temp.add((double)0.0F);
        temp.add((double)0.0F);
        this.allies.put(2010586, temp);
        this.allies.put(24256, temp);
        this.allies.put(819, temp);
        this.allies.put(2014683, temp);
        this.allies.put(24269, temp);
        this.targets = new ArrayList(5);
        this.rand = new Random();
    }

    public void activate() {
        this.whoAmI = 819;

        for(IRadarResult o : this.detectRadar()) {
            if (this.isSameDirection(o.getObjectDirection(), (-Math.PI / 2D))) {
                this.whoAmI = 2010586;
            }
        }

        for(IRadarResult o : this.detectRadar()) {
            if (this.isSameDirection(o.getObjectDirection(), (Math.PI / 2D)) && this.whoAmI != 819) {
                this.whoAmI = 24256;
            }
        }

        this.isLeftTeam = true;

        for(IRadarResult o : this.detectRadar()) {
            if (o.getObjectDirection() > 1.7278759594743864 && o.getObjectDirection() < 4.5553093477052) {
                this.isLeftTeam = false;
            }
        }

        if (!this.isLeftTeam) {
            this.broadcast("663121:663121:663121:663121:663121:663121:663121");
        }

        if (this.isLeftTeam) {
            if (this.whoAmI == 819) {
                this.myX = (double)200.0F;
                this.myY = (double)800.0F;
            } else {
                this.myX = (double)200.0F;
                this.myY = (double)1000.0F;
            }

            if (this.whoAmI == 2010586) {
                this.myX = (double)200.0F;
                this.myY = (double)1200.0F;
            }
        } else {
            if (this.whoAmI == 819) {
                this.myX = (double)2800.0F;
                this.myY = (double)800.0F;
            } else {
                this.myX = (double)2800.0F;
                this.myY = (double)1000.0F;
            }

            if (this.whoAmI == 2010586) {
                this.myX = (double)2800.0F;
                this.myY = (double)1200.0F;
            }
        }

        this.state = 8;
        this.isMoving = false;
        this.fireOrder = false;
        this.targetX = (double)0.0F;
        this.targetY = (double)0.0F;
        this.stepNumberLastFire = 0;
        this.stepNumber = 0;
        this.stepNumberMoveBack = 0;
        this.findNewShotAngle = false;
        this.isMovingBack = false;
        this.counter = 0;
    }

    public void step() {
        ArrayList<String> messages = this.fetchAllMessages();
        if (this.stepNumber == 0 && this.whoAmI == 24256) {
            for(String m : messages) {
                if (Integer.parseInt(m.split(":")[0]) == 663121) {
                    this.isLeftTeam = false;
                    this.myX = (double)2800.0F;
                    this.myY = (double)1000.0F;
                }
            }
        }

        ++this.stepNumber;
        if (this.stepNumber > 3000 && this.state == 8) {
            this.state = 1;
        }

        if (this.counter > 460) {
            this.counter = 0;
        }

        if (this.getHealth() == (double)0.0F) {
            this.state = -1159983647;
        }

        if (this.isMoving) {
            this.myX += (double)1.0F * Math.cos(this.myGetHeading());
            this.myY += (double)1.0F * Math.sin(this.myGetHeading());
            this.realCoords();
            this.isMoving = false;
        }

        if (this.isMovingBack) {
            this.myX -= (double)1.0F * Math.cos(this.myGetHeading());
            this.myY -= (double)1.0F * Math.sin(this.myGetHeading());
            this.realCoords();
            this.isMovingBack = false;
        }

        boolean debug = true;
        if (debug && this.whoAmI == 2010586 && this.state != -1159983647) {
            int var10001 = (int)this.myX;
            this.sendLogMessage("#ALPHA *thinks* (x,y)= (" + var10001 + ", " + (int)this.myY + ") theta= " + (int)(this.myGetHeading() * (double)180.0F / Math.PI) + "°. #State= " + this.state);
        }

        if (debug && this.whoAmI == 24256 && this.state != -1159983647) {
            int var24 = (int)this.myX;
            this.sendLogMessage("#BETA *thinks* (x,y)= (" + var24 + ", " + (int)this.myY + ") theta= " + (int)(this.myGetHeading() * (double)180.0F / Math.PI) + "°. #State= " + this.state);
        }

        if (debug && this.whoAmI == 819 && this.state != -1159983647) {
            int var25 = (int)this.myX;
            this.sendLogMessage("#GAMMA *thinks* (x,y)= (" + var25 + ", " + (int)this.myY + ") theta= " + (int)(this.myGetHeading() * (double)180.0F / Math.PI) + "°. #State= " + this.state);
        }

        if (debug && this.fireOrder) {
            ++this.counter;
            this.sendLogMessage("Firing enemy!!");
        }

        this.targets.clear();

        for(String m : messages) {
            if (Integer.parseInt(m.split(":")[1]) == this.whoAmI || Integer.parseInt(m.split(":")[1]) == 12246445) {
                this.process(m);
            }
        }

        int var26 = this.whoAmI;
        this.broadcast(var26 + ":12246445:32343:" + this.myX + ":" + this.myY + ":" + this.getHeading() + ":-1073737473");

        for(IRadarResult o : this.detectRadar()) {
            if (o.getObjectType() == Types.OpponentMainBot || o.getObjectType() == Types.OpponentSecondaryBot) {
                double enemyX = this.myX + o.getObjectDistance() * Math.cos(o.getObjectDirection());
                double enemyY = this.myY + o.getObjectDistance() * Math.sin(o.getObjectDirection());
                var26 = this.whoAmI;
                this.broadcast(var26 + ":12246445:2898:" + (o.getObjectType() == Types.OpponentMainBot ? -1.431633921E9 : (double)-21846.0F) + ":" + enemyX + ":" + enemyY + ":-1073737473");
            }

            if (o.getObjectDistance() < (double)120.0F && o.getObjectType() != Types.BULLET && this.state == 1) {
                this.state = 5;
                this.stepNumberMoveBack = this.stepNumber;
                return;
            }
        }

        if (this.fireOrder && !this.findNewShotAngle) {
            this.setTarget();
        }

        if (this.myX <= (double)50.0F) {
            if (this.isHeading((double)0.0F)) {
                this.state = 1;
            } else {
                this.state = 12;
            }
        } else if (this.myX >= (double)2950.0F) {
            if (this.isHeading(Math.PI)) {
                this.state = 1;
            } else {
                this.state = 13;
            }
        } else if (this.myY <= (double)50.0F) {
            if (this.isHeading((Math.PI / 2D))) {
                this.state = 1;
            } else {
                this.state = 11;
            }
        } else if (this.myY >= (double)1950.0F) {
            this.state = 10;
            if (this.isHeading((-Math.PI / 2D))) {
                this.state = 1;
            }
        } else {
            if (this.state == 8) {
                if (this.stepNumber > 100 && this.canFireLatency()) {
                    if (this.fireOrder && this.canIShot(this.targetX, this.targetY)) {
                        this.firePosition(this.targetX, this.targetY);
                        this.stepNumberLastFire = this.stepNumber;
                        return;
                    }

                    this.fire(this.myGetHeading());
                    this.stepNumberLastFire = this.stepNumber;
                    return;
                }

                this.myMove();
            }

            if (this.fireOrder && this.canFireLatency() && this.canIShot(this.targetX, this.targetY)) {
                this.firePosition(this.targetX, this.targetY);
                this.stepNumberLastFire = this.stepNumber;
            } else {
                if (!this.fireOrder && this.stepNumber > 6000 && this.targets.size() > 0 && this.state != 9) {
                    double tx = (Double)((ArrayList)this.targets.get(0)).get(1);
                    double ty = (Double)((ArrayList)this.targets.get(0)).get(2);
                    double distX = Math.abs(tx - this.myX);
                    double distY = Math.abs(ty - this.myY);
                    if (distX > distY || distY < (double)200.0F) {
                        this.state = 9;
                        this.huntingMode = "x";
                        return;
                    }

                    if (distX > (double)200.0F) {
                        this.state = 9;
                        this.huntingMode = "y";
                        return;
                    }
                }

                if (this.state == 9 && !this.fireOrder) {
                    if (this.targets.isEmpty()) {
                        this.state = 1;
                        this.huntingMode = "";
                    } else if (this.huntingMode.equals("x")) {
                        double tx = (Double)((ArrayList)this.targets.get(0)).get(1);
                        double distX = Math.abs(tx - this.myX);
                        if (distX < (double)200.0F) {
                            this.huntingMode = "";
                            this.state = 1;
                        } else if (tx < this.myX) {
                            if (this.isHeading(Math.PI)) {
                                this.myMove();
                            } else {
                                if (this.myGetHeading() < Math.PI && this.myGetHeading() > (double)0.0F) {
                                    this.stepTurn(Direction.RIGHT);
                                } else {
                                    this.stepTurn(Direction.LEFT);
                                }

                            }
                        } else if (this.isHeading((double)0.0F)) {
                            this.myMove();
                        } else {
                            if (this.myGetHeading() < Math.PI && this.myGetHeading() > (double)0.0F) {
                                this.stepTurn(Direction.RIGHT);
                            } else {
                                this.stepTurn(Direction.LEFT);
                            }

                        }
                    } else {
                        double ty = (Double)((ArrayList)this.targets.get(0)).get(2);
                        double distY = Math.abs(ty - this.myY);
                        if (distY < (double)200.0F) {
                            this.huntingMode = "";
                            this.state = 1;
                        } else if (ty < this.myY) {
                            if (this.isHeading((-Math.PI / 2D))) {
                                this.myMove();
                            } else {
                                if (!(this.myGetHeading() < (Math.PI / 2D)) && !(this.myGetHeading() > (Math.PI * 1.5D))) {
                                    this.stepTurn(Direction.RIGHT);
                                } else {
                                    this.stepTurn(Direction.LEFT);
                                }

                            }
                        } else if (this.isHeading((Math.PI / 2D))) {
                            this.myMove();
                        } else {
                            if (!(this.myGetHeading() < (Math.PI / 2D)) && !(this.myGetHeading() > (Math.PI * 1.5D))) {
                                this.stepTurn(Direction.RIGHT);
                            } else {
                                this.stepTurn(Direction.LEFT);
                            }

                        }
                    }
                } else if (this.state == 9 && this.fireOrder) {
                    this.state = 1;
                    this.myMove();
                    this.huntingMode = "";
                } else if (this.state == 1 && this.detectFront().getObjectType() != characteristics.IFrontSensorResult.Types.WALL) {
                    if (this.canFireLatency()) {
                        for(int i = 0; i < 10; ++i) {
                            double angle = this.rand.nextDouble() * Math.PI / (double)6.0F - 0.2617993877991494;
                            double x = this.myX + (double)1000.0F * Math.cos(this.myGetHeading() + angle);
                            double y = this.myY + (double)1000.0F * Math.sin(this.myGetHeading() + angle);
                            if (this.canIShot(x, y)) {
                                this.firePosition(x, y);
                                this.stepNumberLastFire = this.stepNumber;
                                return;
                            }
                        }
                    }

                    this.myMove();
                } else if (this.state == 1 && this.detectFront().getObjectType() == characteristics.IFrontSensorResult.Types.WALL) {
                    if ((!(this.myX > (double)2915.0F) || !(this.myY > (double)1915.0F)) && (!(this.myX > (double)2915.0F) || !(this.myY < (double)85.0F)) && (!(this.myX < (double)85.0F) || !(this.myY < (double)85.0F)) && (!(this.myX < (double)85.0F) || !(this.myY > (double)1915.0F))) {
                        if (!(this.myX > (double)2915.0F) && !(this.myX < (double)85.0F)) {
                            if (!(this.myY > (double)1915.0F) && !(this.myY < (double)85.0F)) {
                                this.myMove();
                            } else if (!this.isHeading((-Math.PI / 2D)) && !this.isHeading((Math.PI / 2D))) {
                                this.myMove();
                            } else {
                                this.state = 6;
                                this.endTaskDirection = this.getHeading() + (-Math.PI / 2D);
                                this.stepTurn(Direction.LEFT);
                            }
                        } else if (!this.isHeading((double)0.0F) && !this.isHeading(Math.PI)) {
                            this.myMove();
                        } else {
                            this.state = 6;
                            this.endTaskDirection = this.getHeading() + (-Math.PI / 2D);
                            this.stepTurn(Direction.LEFT);
                        }
                    } else {
                        this.state = 6;
                        this.endTaskDirection = this.getHeading() + (-Math.PI / 2D);
                        this.stepTurn(Direction.LEFT);
                    }
                } else if (this.state == 2) {
                    this.state = 1;
                    if (++this.fireStep % 2 == 0 && this.counter < 415) {
                        this.moveBack();
                        this.myX -= (double)1.0F * Math.cos(this.getHeading());
                        this.myY -= (double)1.0F * Math.sin(this.getHeading());
                        this.realCoords();
                    } else {
                        this.myMove();
                    }

                } else if (this.state == 3) {
                    this.state = 1;
                    if (++this.fireStep % 1 == 0) {
                        this.moveBack();
                        this.myX -= (double)1.0F * Math.cos(this.getHeading());
                        this.myY -= (double)1.0F * Math.sin(this.getHeading());
                        this.realCoords();
                    } else {
                        this.myMove();
                    }

                } else if (this.state == 5) {
                    if (this.stepNumber < this.stepNumberMoveBack + 25) {
                        this.myMoveBack();
                    } else {
                        if (Math.random() < (double)0.5F) {
                            this.state = 6;
                            this.endTaskDirection = this.getHeading() + (-Math.PI / 2D);
                            this.stepTurn(Direction.LEFT);
                        } else {
                            this.state = 7;
                            this.endTaskDirection = this.getHeading() + (Math.PI / 2D);
                            this.stepTurn(Direction.RIGHT);
                        }

                    }
                } else if (this.state == 7) {
                    if (this.isHeading(this.endTaskDirection)) {
                        this.state = 1;
                        this.myMove();
                    } else {
                        this.stepTurn(Direction.RIGHT);
                    }

                } else if (this.state == 6) {
                    if (this.isHeading(this.endTaskDirection)) {
                        this.state = 1;
                        this.myMove();
                    } else {
                        this.stepTurn(Direction.LEFT);
                    }

                } else if (this.state == 10 && !this.isHeading((-Math.PI / 2D))) {
                    if (!(this.myGetHeading() < (Math.PI / 2D)) && !(this.myGetHeading() > (Math.PI * 1.5D))) {
                        this.stepTurn(Direction.RIGHT);
                    } else {
                        this.stepTurn(Direction.LEFT);
                    }

                } else if (this.state == 10 && this.isHeading((-Math.PI / 2D))) {
                    this.state = 1;
                    this.myMove();
                } else if (this.state == 11 && !this.isHeading((Math.PI / 2D))) {
                    if (!(this.myGetHeading() < (Math.PI / 2D)) && !(this.myGetHeading() > (Math.PI * 1.5D))) {
                        this.stepTurn(Direction.LEFT);
                    } else {
                        this.stepTurn(Direction.RIGHT);
                    }

                } else if (this.state == 11 && this.isHeading((Math.PI / 2D))) {
                    this.state = 1;
                    this.myMove();
                } else if (this.state == 12 && !this.isHeading((double)0.0F)) {
                    if (this.myGetHeading() < Math.PI && this.myGetHeading() > (double)0.0F) {
                        this.stepTurn(Direction.LEFT);
                    } else {
                        this.stepTurn(Direction.RIGHT);
                    }

                } else if (this.state == 12 && this.isHeading((double)0.0F)) {
                    this.state = 1;
                    this.myMove();
                } else if (this.state == 13 && !this.isHeading(Math.PI)) {
                    if (this.myGetHeading() < Math.PI && this.myGetHeading() > (double)0.0F) {
                        this.stepTurn(Direction.RIGHT);
                    } else {
                        this.stepTurn(Direction.LEFT);
                    }

                } else if (this.state == 13 && this.isHeading(Math.PI)) {
                    this.state = 1;
                    this.myMove();
                } else if (this.state != -1159983647) {
                    ;
                }
            }
        }
    }

    private void myMove() {
        this.isMoving = true;
        this.move();
    }

    private void myMoveBack() {
        this.isMovingBack = true;
        this.moveBack();
    }

    private double myGetHeading() {
        return this.normalizeRadian(this.getHeading());
    }

    private double normalizeRadian(double angle) {
        double result;
        for(result = angle; result < (double)0.0F; result += (Math.PI * 2D)) {
        }

        while(result >= (Math.PI * 2D)) {
            result -= (Math.PI * 2D);
        }

        return result;
    }

    private boolean isSameDirection(double dir1, double dir2) {
        return Math.abs(this.normalizeRadian(dir1) - this.normalizeRadian(dir2)) < 0.001;
    }

    private void process(String message) {
        if (Integer.parseInt(message.split(":")[2]) == 2898) {
            double x = Double.parseDouble(message.split(":")[4]);
            double y = Double.parseDouble(message.split(":")[5]);
            boolean already = false;

            for(ArrayList<Double> list : this.targets) {
                if (Math.abs(x - (Double)list.get(1)) <= (double)10.0F && Math.abs(y - (Double)list.get(2)) <= (double)10.0F) {
                    already = true;
                }
            }

            if (!already) {
                ArrayList<Double> target = new ArrayList(3);
                target.add(Double.parseDouble(message.split(":")[3]));
                target.add(x);
                target.add(y);
                this.targets.add(target);
            }

            this.fireOrder = true;
        }

        if (Integer.parseInt(message.split(":")[2]) == 32343) {
            ArrayList<Double> temp = new ArrayList(2);
            temp.add(Double.parseDouble(message.split(":")[3]));
            temp.add(Double.parseDouble(message.split(":")[4]));
            temp.add(Double.parseDouble(message.split(":")[5]));
            this.allies.replace(Integer.parseInt(message.split(":")[0]), temp);
        }

    }

    private void setTarget() {
        ArrayList<ArrayList<Double>> realTargets = new ArrayList(5);

        for(ArrayList<Double> target : this.targets) {
            if (this.distance(this.myX, this.myY, (Double)target.get(1), (Double)target.get(2)) <= (double)1000.0F) {
                realTargets.add(target);
            }
        }

        realTargets.sort(new Comparator<ArrayList<Double>>() {
            public int compare(ArrayList<Double> l1, ArrayList<Double> l2) {
                if (l1.get(0) == l2.get(0)) {
                    return Double.compare(FifthElementMain.this.distance(FifthElementMain.this.myX, FifthElementMain.this.myY, (Double)l2.get(1), (Double)l2.get(2)), FifthElementMain.this.distance(FifthElementMain.this.myX, FifthElementMain.this.myY, (Double)l1.get(1), (Double)l1.get(2)));
                } else if ((Double)l1.get(0) == -1.431633921E9) {
                    return FifthElementMain.this.distance(FifthElementMain.this.myX, FifthElementMain.this.myY, (Double)l1.get(1), (Double)l1.get(2)) <= (double)300.0F ? 1 : -1;
                } else {
                    return FifthElementMain.this.distance(FifthElementMain.this.myX, FifthElementMain.this.myY, (Double)l2.get(1), (Double)l2.get(2)) > (double)300.0F ? 1 : -1;
                }
            }
        });

        for(ArrayList<Double> target : realTargets) {
            if (this.canIShot((Double)target.get(1), (Double)target.get(2))) {
                this.targetX = (Double)target.get(1);
                this.targetY = (Double)target.get(2);
                if (this.distance(this.myX, this.myY, this.targetX, this.targetY) > (double)600.0F) {
                    this.state = 2;
                } else {
                    this.state = 3;
                }

                return;
            }
        }

        this.fireOrder = false;
    }

    private void firePosition(double x, double y) {
        if (this.myX <= x) {
            this.fire(Math.atan((y - this.myY) / (x - this.myX)));
        } else {
            this.fire(Math.PI + Math.atan((y - this.myY) / (x - this.myX)));
        }

    }

    private boolean canIShot(double x, double y) {
        double a = (y - this.myY) / (x - this.myX);
        double b = this.myY - a * this.myX;

        for(ArrayList<Double> ally : this.allies.values()) {
            if (!(this.distance(this.myX, this.myY, (Double)ally.get(0), (Double)ally.get(1)) <= (double)10.0F)) {
                double angleToAlly = this.getDirectionToTarget((Double)ally.get(0), (Double)ally.get(1));
                double angleToTarget = this.getDirectionToTarget(x, y);
                if (Math.abs(angleToAlly - angleToTarget) < 0.2617993877991494 && this.distance(this.myX, this.myY, (Double)ally.get(0), (Double)ally.get(1)) < this.distance(this.myX, this.myY, x, y)) {
                    return false;
                }

                if (this.getHeading() == (double)0.0F && Math.abs((Double)ally.get(1) - this.myY) < (double)15.0F && (Double)ally.get(0) > this.myX) {
                    return false;
                }

                if (this.getHeading() == Math.PI && Math.abs((Double)ally.get(1) - this.myY) < (double)15.0F && (Double)ally.get(0) < this.myX) {
                    return false;
                }

                if (this.getHeading() == (Math.PI / 2D) && Math.abs((Double)ally.get(0) - this.myX) < (double)15.0F && (Double)ally.get(0) > this.myX) {
                    return false;
                }

                if (this.getHeading() == (-Math.PI / 2D) && Math.abs((Double)ally.get(0) - this.myX) < (double)15.0F && (Double)ally.get(0) < this.myX) {
                    return false;
                }

                double allyA = Math.tan((Double)ally.get(2));
                double allyB = (Double)ally.get(1) - allyA * (Double)ally.get(0);
                double allyX = (b - allyB) / (allyA - a);
                double allyY = a * allyX + b;
                if (this.distance((Double)ally.get(0), (Double)ally.get(1), allyX, allyY) <= (double)125.0F && (x >= allyX && allyX >= this.myX || x <= allyX && allyX <= this.myX) && (y >= allyY && allyY >= this.myY || y <= allyY && allyY <= this.myY)) {
                    return false;
                }
            }
        }

        if (this.detectFront().getObjectType() != characteristics.IFrontSensorResult.Types.TeamMainBot && this.detectFront().getObjectType() != characteristics.IFrontSensorResult.Types.TeamSecondaryBot) {
            return true;
        } else {
            return false;
        }
    }

    private double distance(double x1, double y1, double x2, double y2) {
        return Math.sqrt((x2 - x1) * (x2 - x1) + (y2 - y1) * (y2 - y1));
    }

    private void realCoords() {
        this.myX = this.myX < (double)0.0F ? (double)0.0F : this.myX;
        this.myX = this.myX > (double)3000.0F ? (double)3000.0F : this.myX;
        this.myY = this.myY < (double)0.0F ? (double)0.0F : this.myY;
        this.myY = this.myY > (double)2000.0F ? (double)2000.0F : this.myY;
    }

    private boolean canFireLatency() {
        return this.stepNumber > this.stepNumberLastFire + 20;
    }

    private boolean isHeading(double dir) {
        return Math.abs(Math.sin(this.getHeading() - dir)) < 0.01;
    }

    private double getDirectionToTarget(double x, double y) {
        double dir = Math.atan2(y - this.myY, x - this.myX);
        return this.normalizeRadian(dir);
    }
}
