//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package algorithms;

import characteristics.IRadarResult;
import characteristics.IRadarResult.Types;
import characteristics.Parameters.Direction;
import java.util.ArrayList;
import java.util.HashMap;
import robotsimulator.Brain;

public class FifthElementSecondary extends Brain {
    private boolean isLeftTeam = true;
    private static final double ANGLEPRECISION = 0.001;
    private static final double ANGLEPRECISIONBIS = 0.01;
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
    private static final int TURNNORTHTASK = 1;
    private static final int TURNSOUTHTASK = 2;
    private static final int TURNEASTTASK = 3;
    private static final int TURNWESTTASK = 4;
    private static final int MOVETASK = 5;
    private static final int FIRSTMOVETASK = 6;
    private static final int FLEE = 7;
    private static final int TURNLEFTTASK = 8;
    private static final int MOVEBACKTASK = 9;
    private static final int TURNRIGHTTASK = 10;
    private static final int FIRSTTURNNORTHTASK = 11;
    private static final int FIRSTTURNSOUTHTASK = 22;
    private static final int SINK = -1159983647;
    private int state;
    private double myX;
    private double myY;
    private boolean isMoving;
    private int whoAmI;
    private HashMap<Integer, ArrayList<Double>> allies = new HashMap(5);
    private ArrayList<IRadarResult> ennemies;
    private double endTaskDirection;
    private int stepNumber;
    private int stepNumberMoveBack;
    private boolean isMovingBack;
    private boolean leftTeam;

    public FifthElementSecondary() {
        ArrayList<Double> temp = new ArrayList(2);
        temp.add((double)0.0F);
        temp.add((double)0.0F);
        this.allies.put(2010586, temp);
        this.allies.put(24256, temp);
        this.allies.put(819, temp);
        this.allies.put(2014683, temp);
        this.allies.put(24269, temp);
        this.ennemies = new ArrayList();
    }

    public void activate() {
        this.whoAmI = 2014683;

        for(IRadarResult o : this.detectRadar()) {
            if (this.isSameDirection(o.getObjectDirection(), (-Math.PI / 2D))) {
                this.whoAmI = 24269;
            }
        }

        this.isLeftTeam = true;

        for(IRadarResult o : this.detectRadar()) {
            if (this.isSameDirection(o.getObjectDirection(), (double)0.0F)) {
                this.isLeftTeam = false;
            }
        }

        if (this.isLeftTeam) {
            if (this.whoAmI == 2014683) {
                this.myX = (double)500.0F;
                this.myY = (double)800.0F;
                this.state = 11;
            } else {
                this.myX = (double)500.0F;
                this.myY = (double)1200.0F;
                this.state = 22;
            }
        } else if (this.whoAmI == 2014683) {
            this.myX = (double)2500.0F;
            this.myY = (double)800.0F;
            this.state = 11;
        } else {
            this.myX = (double)2500.0F;
            this.myY = (double)1200.0F;
            this.state = 22;
        }

        if (this.myX == (double)2500.0F) {
            this.leftTeam = false;
        } else {
            this.leftTeam = true;
        }

        this.isMoving = false;
        this.stepNumber = 0;
        this.stepNumberMoveBack = 0;
        this.isMovingBack = false;
    }

    public void step() {
        ++this.stepNumber;
        if (this.getHealth() == (double)0.0F) {
            this.state = -1159983647;
        }

        if (this.isMoving) {
            this.myX += (double)3.0F * Math.cos(this.getHeading());
            this.myY += (double)3.0F * Math.sin(this.getHeading());
            this.realCoords();
            this.isMoving = false;
        }

        if (this.isMovingBack) {
            this.myX -= (double)1.0F * Math.cos(this.myGetHeading());
            this.myY -= (double)1.0F * Math.sin(this.myGetHeading());
            this.realCoords();
            this.isMovingBack = false;
        }

        if (this.whoAmI == 2014683) {
            int var10001 = (int)this.myX;
            this.sendLogMessage("#ROCKY *thinks* he is rolling at position (" + var10001 + ", " + (int)this.myY + ").#state:" + this.state);
        } else {
            int var9 = (int)this.myX;
            this.sendLogMessage("#MARIO *thinks* he is rolling at position (" + var9 + ", " + (int)this.myY + ").#state:" + this.state);
        }

        for(IRadarResult o : this.detectRadar()) {
            if (o.getObjectType() == Types.OpponentMainBot || o.getObjectType() == Types.OpponentSecondaryBot) {
                double enemyX = this.myX + o.getObjectDistance() * Math.cos(o.getObjectDirection());
                double enemyY = this.myY + o.getObjectDistance() * Math.sin(o.getObjectDirection());
                int var10 = this.whoAmI;
                this.broadcast(var10 + ":12246445:2898:" + (o.getObjectType() == Types.OpponentMainBot ? -1.431633921E9 : (double)-21846.0F) + ":" + enemyX + ":" + enemyY + ":-1073737473");
                this.ennemies.add(o);
            }
        }

        int var11 = this.whoAmI;
        this.broadcast(var11 + ":12246445:32343:" + this.myX + ":" + this.myY + ":" + this.myGetHeading() + ":-1073737473");
        this.ennemies.clear();

        for(IRadarResult o : this.detectRadar()) {
            if (o.getObjectType() == Types.OpponentMainBot && o.getObjectDistance() <= (double)400.0F || o.getObjectType() == Types.OpponentSecondaryBot && o.getObjectDistance() <= (double)350.0F) {
                this.ennemies.add(o);
                if (this.state == 5) {
                    this.state = 7;
                }
            }

            if (o.getObjectDistance() < (double)120.0F && o.getObjectType() != Types.BULLET && this.state == 5) {
                this.state = 9;
                this.stepNumberMoveBack = this.stepNumber;
            }
        }

        if (this.myX <= (double)50.0F) {
            if (this.isHeading((double)0.0F)) {
                this.state = 5;
            } else {
                this.state = 3;
            }
        } else if (this.myX >= (double)2950.0F) {
            if (this.isHeading(Math.PI)) {
                this.state = 5;
            } else {
                this.state = 4;
            }
        } else if (this.myY <= (double)50.0F) {
            if (this.isHeading((Math.PI / 2D))) {
                this.state = 5;
            } else {
                this.state = 2;
            }
        } else if (this.myY >= (double)1950.0F) {
            this.state = 1;
            if (this.isHeading((-Math.PI / 2D))) {
                this.state = 5;
            }
        } else {
            if (this.state == 6) {
                this.myMove();
                if (this.whoAmI == 24269) {
                    if (this.myY > (double)1800.0F) {
                        if (this.leftTeam) {
                            this.state = 3;
                        } else {
                            this.state = 4;
                        }

                        return;
                    }
                } else if (this.myY < (double)500.0F) {
                    if (this.leftTeam) {
                        this.state = 3;
                    } else {
                        this.state = 4;
                    }

                    return;
                }
            }

            if (this.state == 11 && !this.isHeading((-Math.PI / 2D))) {
                if (!(this.myGetHeading() < (Math.PI / 2D)) && !(this.myGetHeading() > (Math.PI * 1.5D))) {
                    this.stepTurn(Direction.RIGHT);
                } else {
                    this.stepTurn(Direction.LEFT);
                }

            } else if (this.state == 11 && this.isHeading((-Math.PI / 2D))) {
                this.state = 6;
                this.myMove();
            } else if (this.state == 22 && !this.isHeading((Math.PI / 2D))) {
                if (!(this.myGetHeading() < (Math.PI / 2D)) && !(this.myGetHeading() > (Math.PI * 1.5D))) {
                    this.stepTurn(Direction.LEFT);
                } else {
                    this.stepTurn(Direction.RIGHT);
                }

            } else if (this.state == 22 && this.isHeading((Math.PI / 2D))) {
                this.state = 6;
                this.myMove();
            } else if (this.state == 1 && !this.isHeading((-Math.PI / 2D))) {
                if (!(this.myGetHeading() < (Math.PI / 2D)) && !(this.myGetHeading() > (Math.PI * 1.5D))) {
                    this.stepTurn(Direction.RIGHT);
                } else {
                    this.stepTurn(Direction.LEFT);
                }

            } else if (this.state == 1 && this.isHeading((-Math.PI / 2D))) {
                this.state = 5;
                this.myMove();
            } else if (this.state == 2 && !this.isHeading((Math.PI / 2D))) {
                if (!(this.myGetHeading() < (Math.PI / 2D)) && !(this.myGetHeading() > (Math.PI * 1.5D))) {
                    this.stepTurn(Direction.LEFT);
                } else {
                    this.stepTurn(Direction.RIGHT);
                }

            } else if (this.state == 2 && this.isHeading((Math.PI / 2D))) {
                this.state = 5;
                this.myMove();
            } else if (this.state == 3 && !this.isHeading((double)0.0F)) {
                if (this.myGetHeading() < Math.PI && this.myGetHeading() > (double)0.0F) {
                    this.stepTurn(Direction.LEFT);
                } else {
                    this.stepTurn(Direction.RIGHT);
                }

            } else if (this.state == 3 && this.isHeading((double)0.0F)) {
                this.state = 5;
                this.myMove();
            } else if (this.state == 4 && !this.isHeading(Math.PI)) {
                if (this.myGetHeading() < Math.PI && this.myGetHeading() > (double)0.0F) {
                    this.stepTurn(Direction.RIGHT);
                } else {
                    this.stepTurn(Direction.LEFT);
                }

            } else if (this.state == 4 && this.isHeading(Math.PI)) {
                this.state = 5;
                this.myMove();
            } else if (this.state == 5) {
                if (this.detectFront().getObjectType() == characteristics.IFrontSensorResult.Types.WALL) {
                    if (this.whoAmI != 24269) {
                        this.state = 8;
                        this.endTaskDirection = this.getHeading() + (-Math.PI / 2D);
                        this.stepTurn(Direction.LEFT);
                    } else if ((!(this.myX > (double)2800.0F) || !(this.myY > (double)1800.0F)) && (!(this.myX > (double)2800.0F) || !(this.myY < (double)200.0F)) && (!(this.myX < (double)200.0F) || !(this.myY < (double)200.0F)) && (!(this.myX < (double)200.0F) || !(this.myY > (double)1800.0F))) {
                        if (!(this.myX > (double)2800.0F) && !(this.myX < (double)200.0F)) {
                            if (!(this.myY > (double)1800.0F) && !(this.myY < (double)200.0F)) {
                                this.myMove();
                            } else if (!this.isHeading((-Math.PI / 2D)) && !this.isHeading((Math.PI / 2D))) {
                                this.myMove();
                            } else {
                                this.state = 8;
                                this.endTaskDirection = this.getHeading() + (-Math.PI / 2D);
                                this.stepTurn(Direction.LEFT);
                            }
                        } else if (!this.isHeading((double)0.0F) && !this.isHeading(Math.PI)) {
                            this.myMove();
                        } else {
                            this.state = 8;
                            this.endTaskDirection = this.getHeading() + (-Math.PI / 2D);
                            this.stepTurn(Direction.LEFT);
                        }
                    } else {
                        this.state = 8;
                        this.endTaskDirection = this.getHeading() + (-Math.PI / 2D);
                        this.stepTurn(Direction.LEFT);
                    }
                } else {
                    this.myMove();
                }
            } else if (this.state == 10) {
                if (this.isHeading(this.endTaskDirection)) {
                    this.state = 5;
                    this.myMove();
                } else {
                    this.stepTurn(Direction.RIGHT);
                }

            } else if (this.state == 8) {
                if (this.isHeading(this.endTaskDirection)) {
                    this.state = 5;
                    this.myMove();
                } else {
                    this.stepTurn(Direction.LEFT);
                }

            } else if (this.state == 9) {
                if (this.stepNumber < this.stepNumberMoveBack + 25) {
                    this.myMoveBack();
                } else {
                    if (Math.random() < (double)0.5F) {
                        this.state = 8;
                        this.endTaskDirection = this.getHeading() + (-Math.PI / 2D);
                        this.stepTurn(Direction.LEFT);
                    } else {
                        this.state = 10;
                        this.endTaskDirection = this.getHeading() + (Math.PI / 2D);
                        this.stepTurn(Direction.RIGHT);
                    }

                }
            } else if (this.state != 7) {
                if (this.state != -1159983647) {
                    ;
                }
            } else if (!(this.myX > (double)2900.0F) && !(this.myX < (double)100.0F) || !(this.myY > (double)1900.0F) && !(this.myX < (double)100.0F)) {
                if (!(this.myX > (double)2900.0F) && !(this.myX < (double)100.0F)) {
                    if (!(this.myY > (double)1900.0F) && !(this.myY < (double)100.0F)) {
                        this.moveBack();
                        this.myX -= (double)3.0F * Math.cos(this.getHeading());
                        this.myY -= (double)3.0F * Math.sin(this.getHeading());
                        this.realCoords();
                        if (this.ennemies.isEmpty()) {
                            this.state = 5;
                        }

                    } else if (!this.isHeading((-Math.PI / 2D)) && !this.isHeading((Math.PI / 2D))) {
                        this.moveBack();
                        this.myX -= (double)3.0F * Math.cos(this.getHeading());
                        this.myY -= (double)3.0F * Math.sin(this.getHeading());
                        this.realCoords();
                        if (this.ennemies.isEmpty()) {
                            this.state = 5;
                        }

                    } else {
                        this.state = 10;
                        this.endTaskDirection = this.getHeading() + (Math.PI / 2D);
                        this.stepTurn(Direction.RIGHT);
                    }
                } else if (!this.isHeading((double)0.0F) && !this.isHeading(Math.PI)) {
                    this.moveBack();
                    this.myX -= (double)3.0F * Math.cos(this.getHeading());
                    this.myY -= (double)3.0F * Math.sin(this.getHeading());
                    this.realCoords();
                    if (this.ennemies.isEmpty()) {
                        this.state = 5;
                    }

                } else {
                    this.state = 10;
                    this.endTaskDirection = this.getHeading() + (Math.PI / 2D);
                    this.stepTurn(Direction.RIGHT);
                }
            } else {
                this.state = 10;
                this.endTaskDirection = this.getHeading() + (Math.PI / 2D);
                this.stepTurn(Direction.RIGHT);
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
        return Math.abs(dir1 - dir2) < 0.001;
    }

    private boolean isHeading(double dir) {
        return Math.abs(Math.sin(this.getHeading() - dir)) < 0.01;
    }

    private void realCoords() {
        this.myX = this.myX < (double)0.0F ? (double)0.0F : this.myX;
        this.myX = this.myX > (double)3000.0F ? (double)3000.0F : this.myX;
        this.myY = this.myY < (double)0.0F ? (double)0.0F : this.myY;
        this.myY = this.myY > (double)2000.0F ? (double)2000.0F : this.myY;
    }
}
