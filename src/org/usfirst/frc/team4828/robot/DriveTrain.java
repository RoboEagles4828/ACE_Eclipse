package org.usfirst.frc.team4828.robot;

import com.ctre.CANTalon;
import com.kauailabs.navx.frc.AHRS;
import edu.wpi.first.wpilibj.SPI;
import edu.wpi.first.wpilibj.Timer;
import org.usfirst.frc.team4828.vision.Pixy;


public class DriveTrain {
    private static final double TWIST_THRESHOLD = 0.15;
    private static final double[] X_SPEED_RANGE = {.3, .5};
    // TODO: Calibrate all of these to find real min speeds and reasonable max speeds

    private static final double[] Y_SPEED_RANGE = {0.1, .25};
    private static final double[] TURN_SPEED_RANGE = {0.2, .5};
    private static final double[] LIFT_ANGLE = {330, 270, 210};
    private static final double TURN_DEADZONE = 5.0;
    private static final double MAX_HORIZONTAL_OFFSET = 36.0;
    private static final double MAX_ULTRA_DISTANCE = 40.0;
    private static final double VISION_DEADZONE = 1;
    private static final double PIXY_OFFSET = 0; // distance from center of the gear to the pixy
    private static final double PLACING_DIST = 8.0;
    // TODO: Determine distance from the wall to stop when placing gear

    private static final double DIST_TO_ENC = 77.066;
    private CANTalon frontLeft;
    private CANTalon frontRight;
    private CANTalon backLeft;
    private CANTalon backRight;
    private AHRS navx;
    public int gearRoutineProgress;
    // 0: turning to angle, 1: centering, 2: approaching, 3: backing off

    /**
     * Create drive train object containing mecanum motor functionality.
     *
     * @param frontLeftPort  port of the front left motor
     * @param backLeftPort   port of the back left motor
     * @param frontRightPort port of the front right motor
     * @param backRightPort  port of the back right motor
     */
    DriveTrain(int frontLeftPort, int backLeftPort, int frontRightPort, int backRightPort) {
        frontLeft = new CANTalon(frontLeftPort);
        frontRight = new CANTalon(frontRightPort);
        backLeft = new CANTalon(backLeftPort);
        backRight = new CANTalon(backRightPort);
        frontLeft.setFeedbackDevice(CANTalon.FeedbackDevice.CtreMagEncoder_Relative);
        frontRight.setFeedbackDevice(CANTalon.FeedbackDevice.CtreMagEncoder_Relative);
        backLeft.setFeedbackDevice(CANTalon.FeedbackDevice.CtreMagEncoder_Relative);
        backRight.setFeedbackDevice(CANTalon.FeedbackDevice.CtreMagEncoder_Relative);
        backLeft.reverseSensor(true);
        frontLeft.reverseSensor(true);
        navx = new AHRS(SPI.Port.kMXP);
        gearRoutineProgress = 0;
    }

    /**
     * Test drive train object.
     */
    DriveTrain(boolean gyro) {
        if (gyro) {
            navx = new AHRS(SPI.Port.kMXP);
        }
        System.out.println("Created dummy drivetrain");
    }

    /**
     * Ensure that wheel speeds are all valid numbers.
     *
     * @param wheelSpeeds wheel speeds
     */
    static void normalize(double[] wheelSpeeds) {
        double maxMagnitude = Math.abs(wheelSpeeds[0]);
        for (int i = 1; i < 4; i++) {
            double temp = Math.abs(wheelSpeeds[i]);
            if (maxMagnitude < temp) {
                maxMagnitude = temp;
            }
        }
        if (maxMagnitude > 1.0) {
            for (int i = 0; i < 4; i++) {
                wheelSpeeds[i] = wheelSpeeds[i] / maxMagnitude;
            }
        }
    }

    /**
     * Rotate a vector in Cartesian space.
     *
     * @param xcomponent X component of the vector
     * @param ycomponent Y component of the vector
     * @return the resultant vector as a double[2]
     */
    private double[] rotateVector(double xcomponent, double ycomponent) {
        double angle = navx.getAngle();
        double cosA = Math.cos(angle * (3.14159 / 180.0));
        double sinA = Math.sin(angle * (3.14159 / 180.0));
        double[] out = new double[2];
        out[0] = xcomponent * cosA - ycomponent * sinA;
        out[1] = xcomponent * sinA + ycomponent * cosA;
        return out;
    }

    /**
     * Adjust motor speeds according to joystick input.
     *
     * @param xcomponent x component of the joystick
     * @param ycomponent y component of the joystick
     * @param rotation   rotation of the joystick
     */
    void mecanumDriveAbsolute(double xcomponent, double ycomponent, double rotation) {
        if (Math.abs(rotation) <= TWIST_THRESHOLD) {
            rotation = 0.0;
        }

        // Negate y for the joystick.
        ycomponent = -ycomponent;
        double[] wheelSpeeds = new double[4];
        wheelSpeeds[0] = xcomponent + ycomponent + rotation;
        wheelSpeeds[1] = -xcomponent + ycomponent - rotation;
        wheelSpeeds[2] = -xcomponent + ycomponent + rotation;
        wheelSpeeds[3] = xcomponent + ycomponent - rotation;

        normalize(wheelSpeeds);
        frontLeft.set(wheelSpeeds[0]);
        frontRight.set(wheelSpeeds[1]);
        backLeft.set(wheelSpeeds[2]);
        backRight.set(wheelSpeeds[3]);
    }

    /**
     * Adjust motor speeds according to heading and joystick input.
     * Uses input from the gyroscope to determine field orientation.
     *
     * @param xcomponent x component of the joystick
     * @param ycomponent y component of the joystick
     * @param rotation   rotation of the joystick
     */
    void mecanumDrive(double xcomponent, double ycomponent, double rotation) {
        // Ignore tiny inadvertent joystick rotations
        if (Math.abs(rotation) <= TWIST_THRESHOLD) {
            rotation = 0.0;
        }

        // Negate y for the joystick.
        ycomponent = -ycomponent;
        // Compensate for gyro angle.
        double[] rotated = rotateVector(xcomponent, ycomponent);
        xcomponent = rotated[0];
        ycomponent = rotated[1];

        double[] wheelSpeeds = new double[4];
        wheelSpeeds[0] = xcomponent + ycomponent + rotation;
        wheelSpeeds[1] = -xcomponent + ycomponent - rotation;
        wheelSpeeds[2] = -xcomponent + ycomponent + rotation;
        wheelSpeeds[3] = xcomponent + ycomponent - rotation;

        normalize(wheelSpeeds);
        frontLeft.set(wheelSpeeds[0]);
        frontRight.set(wheelSpeeds[1]);
        backLeft.set(wheelSpeeds[2]);
        backRight.set(wheelSpeeds[3]);
    }

    /**
     * Move motors a certain distance.
     *
     * @param dist  distance
     * @param speed 0-1 no negatives
     */
    public void moveDistance(double dist, double speed) {
        zeroEncoders();
        while (frontLeft.getEncPosition() < Math.abs(dist)) {
            mecanumDrive(0, speed * Math.signum(dist), 0);
        }
        brake();
    }

    /**
     * Gear placement routine.
     *
     * @param pos 0 = Left, 1 = Middle, 2 = Right
     */
    void placeGearAuton(int pos, Pixy pixy, Ultrasonic ultrasonic, GearGobbler gobbler) {
        //TURN TO FACE THE LIFT
        double targetAngle = LIFT_ANGLE[pos];
        while (Math.abs(closestAngle(getTrueAngle(navx.getAngle()), targetAngle)) > TURN_DEADZONE) {
            mecanumDriveAbsolute(0, 0, scaledRotation(targetAngle));
        }
        //ONLY PROCEED IF VISION IS WORKING
        if (pixy.blocksDetected()) {
            //CENTER THE GEAR GOBBLER LATERALLY TO THE TARGET
            while (Math.abs(pixy.horizontalOffset() - PIXY_OFFSET) >= VISION_DEADZONE) {
                mecanumDriveAbsolute(0, scaledYAxis(pixy.horizontalOffset(), PIXY_OFFSET),
                        scaledRotation(targetAngle));
            }
            while (ultrasonic.getDist() >= PLACING_DIST) {
                //APPROACH THE TARGET, CORRECTING ALL AXES SIMULTANEOUSLY
                mecanumDriveAbsolute(scaledXAxis(ultrasonic.getDist(), PLACING_DIST),
                        scaledYAxis(pixy.horizontalOffset(), PIXY_OFFSET),
                        scaledRotation(targetAngle));
            }
            brake();
            gobbler.open();
            Timer.delay(.5);
            while (ultrasonic.getDist() <= 20) {
                // move back 20 inches at max speed to get away from the lift
                mecanumDriveAbsolute(-X_SPEED_RANGE[1], 0, scaledRotation(targetAngle));
            }
            gobbler.close();
            System.out.println("GEAR PLACEMENT ROUTINE FINISHED");
        } else {
            System.out.println("VISION DOES NOT SEE LIFT");
        }

    }

    /**
     * Gear placement routine.
     *
     * @param pos 0 = Left, 1 = Middle, 2 = Right
     */
    void placeGear(int pos, Pixy pixy, Ultrasonic ultrasonic, GearGobbler gobbler) {
        if(pos > 2 || pos < 0){
            System.out.println("ERROR: pos > 2, read the javadoc for the valid pos values");
            return;
        }
        double offset;
        double targetAngle = LIFT_ANGLE[pos];
        System.out.println("Gear place routine on pos: " + pos);
        switch(gearRoutineProgress){
            case 0:
                if (Math.abs(closestAngle(getTrueAngle(navx.getAngle()), targetAngle))
                        > TURN_DEADZONE) {
                    mecanumDriveAbsolute(0, 0, scaledRotation(targetAngle));
                    return;
                }
                break;
            case 1:
                offset = pixy.horizontalOffset();
                if (pixy.blocksDetected()) {
                    if (Math.abs(offset - PIXY_OFFSET) >= VISION_DEADZONE) {
                        mecanumDriveAbsolute(0, scaledYAxis(offset, PIXY_OFFSET),
                                scaledRotation(targetAngle));
                        System.out.println("Offset: " + offset);
                        return;
                    }
                    break;
                } else {
                    System.out.println("No blocks detected");
                    return;
                }
            case 2:
                offset = pixy.horizontalOffset();
                if (ultrasonic.getDist() >= PLACING_DIST) {
                    double temp = scaledYAxis(offset, PIXY_OFFSET);
                    if (!pixy.blocksDetected()) {
                        temp = -.017; //TODO: Maybe change to fix drift
                    }
                    System.out.println("2 blocks?  " + pixy.blocksDetected());
                    mecanumDriveAbsolute(X_SPEED_RANGE[1], temp, scaledRotation(targetAngle));
                    return;
                }
                break;
            case 3:
                if (ultrasonic.getDist() <= 20) {
                    mecanumDriveAbsolute(X_SPEED_RANGE[1], 0, scaledRotation(targetAngle));
                    return;
                }
                break;
            case 4:
//                gobbler.open();
//                Timer.delay(0.5);
//                gobbler.push();
//                Timer.delay(0.5);
//                gobbler.retract();
//                Timer.delay(0.5);
//                gobbler.close();
                break;
            default:
                System.out.println("Finished gear placement routine");
        }
        brake();
        gearRoutineProgress++;
    }

    /**
     * Teleop version finds nearest angle before starting routine.
     */
    void placeGear(Pixy pixy, Ultrasonic ultrasonic, GearGobbler gobbler) {
        double angle = getTrueAngle(navx.getAngle());
        int closest = 0;
        double distance = Double.MAX_VALUE;
        for (int i = 0; i < 3; i++) {
            if (Math.abs(closestAngle(angle, LIFT_ANGLE[i])) < distance) {
                closest = i;
                distance = Math.abs(closestAngle(angle, LIFT_ANGLE[i]));
            }
        }
        placeGear(closest, pixy, ultrasonic, gobbler);
    }

    /**
     * Maps value a from range [inMin, inMax] to range [outMin, outMax]
     *
     * @return mapped double
     */
    private static double map(double alpha, double inMin,
                              double inMax, double outMin, double outMax) {
        return Math.min(Math.max((alpha - inMin) / (inMax - inMin)
                * (outMax - outMin) + outMin, outMin), outMax);
    }

    /**
     * Adjust an angle to fall within 1 rotation.
     *
     * @return [0, 360]
     */
    private static double getTrueAngle(double alpha) {
        double temp = alpha % 360;
        if (temp < 0) {
            temp += 360;
        }
        return temp;
    }

    /**
     * Computes the shortest distance between two angles.
     *
     * @param currentAngle [0, 360]
     * @param targetAngle  [0, 360]
     * @return signed distance from current to target angle [-180,180]
     */
    public static double closestAngle(double currentAngle, double targetAngle) {
        return (getTrueAngle(targetAngle - currentAngle) + 180) % 360 - 180;
    }

    /**
     * Compute rotation with speed scaled to the distance from the desired angle.
     *
     * @param target [0, 360]
     * @return [-1, 1]
     */
    public double scaledRotation(double target) {
        double temp = closestAngle(getTrueAngle(navx.getAngle()), target);
        if (Math.abs(temp) < TURN_DEADZONE) {
            return 0;
        }
        return map(Math.abs(temp), TURN_DEADZONE, 180, TURN_SPEED_RANGE[0], TURN_SPEED_RANGE[1])
                * Math.signum(temp);
    }

    /**
     * Compute speed along x axis scaled to the distance from the lift.
     *
     * @return [-1, 1]
     */
    public double scaledXAxis(double current, double target) {
        double temp = current - target;
        return map(Math.abs(temp), 0, MAX_ULTRA_DISTANCE, X_SPEED_RANGE[0], X_SPEED_RANGE[1])
                * Math.signum(temp);
    }

    /**
     * Compute speed along x axis scaled to the distance between the center
     * of the pixy's frame and the retro-reflective tape.
     *
     * @return [-1, 1]
     */
    public double scaledYAxis(double current, double target) {
        double temp = current - target;
        if (Math.abs(temp) < VISION_DEADZONE) {
            return 0;
        }
        return map(Math.abs(temp), 0, MAX_HORIZONTAL_OFFSET, Y_SPEED_RANGE[0], Y_SPEED_RANGE[1])
                * Math.signum(temp);
    }

    /**
     * Turn all wheels at set speeds.
     *
     * @param fl speed for front left wheel
     * @param fr speed for front right wheel
     * @param bl speed for back left wheel
     * @param br speed for back right wheel
     */
    public void testMotors(int fl, int fr, int bl, int br) {
        frontLeft.set(fl);
        frontRight.set(fr);
        backLeft.set(bl);
        backRight.set(br);
    }

    /**
     * Stop all motors.
     */
    void brake() {
        frontLeft.set(0);
        frontRight.set(0);
        backRight.set(0);
        backLeft.set(0);
    }

    /**
     * @return the current gyro heading
     */
    public String toString() {
        return Double.toString(navx.getAngle());
    }

    /**
     * Prints current average encoder values.
     */
    void debugEncoders() {
        System.out.println("bl " + backLeft.getPosition() + " br "
                + backRight.getPosition() + " fl "
                + frontLeft.getPosition() + " fr " + frontRight.getPosition());
    }

    void debugGyro() {
        System.out.println("Angle: " + getTrueAngle(navx.getAngle()));
    }

    double getGyroDebug(){
        return backRight.getPosition();
    }

    void debugNavx() {
        System.out.println("Displacement. X: " + navx.getDisplacementX() + "  Y: "
                + navx.getDisplacementY() + "  Z: "
                + navx.getDisplacementZ());
        System.out.println("Acceleration. X: " + navx.getRawAccelX() + "  Y: "
                + navx.getRawAccelY() + "  Z: "
                + navx.getRawAccelZ());
    }

    /**
     * Zero the gyro.
     */
    void reset() {
        navx.reset();
    }

    void zeroEncoders() {
        frontLeft.setPosition(0);
        frontRight.setPosition(0);
        backLeft.setPosition(0);
        backRight.setPosition(0);
    }

    /**
     * Takes distance and returns encoder change
     *
     * @param dist distance
     * @return encoder change
     */
    public double distToEncChange(double dist) {
        return dist * DIST_TO_ENC;
    }

    public double encChangeToDist(double encchange) {
        return encchange / DIST_TO_ENC;
    }
}
