package org.usfirst.frc.team4828.robot;

import edu.wpi.first.wpilibj.Spark;

public class Hopper {

    private static final double AGITATOR_SPEED = 0.35;
    private static final double INTAKE_SPEED = 0.95;

    private Spark agitator1;
    private Spark agitator2;

    private Spark intakeMotor;

    /**
     * Create hopper encapsulating the intake and agitator functionality.
     *
     * @param agitatorPort port of the agitator motor
     * @param intakePort   port of the intake motor
     */
    public Hopper(int agitatorPort1, int agitatorPort2, int intakePort) {
        agitator1 = new Spark(agitatorPort1);
        agitator2 = new Spark(agitatorPort2);

        intakeMotor = new Spark(intakePort);
    }

    /**
     * Start stirring the hopper.
     */
    public void stir() {
        agitator1.set(AGITATOR_SPEED);
        agitator2.set(AGITATOR_SPEED);

    }

    /**
     * Stop stirring the hopper.
     */
    public void stopStir() {
        agitator1.set(0);
        agitator2.set(0);
    }

    /**
     * Start the intake.
     */
    public void intake() {
        intakeMotor.set(INTAKE_SPEED);
    }
    /**
     * Set Intake to a speed
     */
    public void intake(double speed) {
        intakeMotor.set(speed);
    }

    /**
     * Stop the intake.
     */
    public void stopIntake() {
        intakeMotor.set(0);
    }

}
