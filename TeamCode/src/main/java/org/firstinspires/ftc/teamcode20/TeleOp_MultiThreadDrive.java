package org.firstinspires.ftc.teamcode20;

import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
import com.qualcomm.robotcore.hardware.DcMotor;

import org.firstinspires.ftc.teamcode20.Roadrunner.drive.mecanum.SampleMecanumDriveREV;

@TeleOp(group = "Final")
public class TeleOp_MultiThreadDrive extends BaseAuto {

    private boolean b = false, rb = false, y = false, dpad_r = false, dpad_l = false, start = false, a = false, back;
    private boolean[] Xprimed = {true},leftStickButtonPrimed = {true},rightStickButtonPrimed = {true};
    private boolean autoLevel = false;
    private boolean tapeDirectionOut = true;
    //slide
    private boolean platformGrabbed = false;

    private int placeLevel = 0;
    private double groundHeightEnc = 4.18 * slideEncoderPerInch;//1 higher placing + 1.25 base height
    private int autoplacemode = 1;
    private double grabberOutSwitch = 0.75;

    private boolean nextGrabberDelay = false;

    private SampleMecanumDriveREV drive;


    @Override
    public void runOpMode() throws InterruptedException {
        //drive=new SampleMecanumDriveREV(hardwareMap);
        //drive.setPoseEstimate(new Pose2d(0,0,0));
        showTelemetry = true;
        initDrivetrain();
        initGrabber();
        initLinSlide();
        initOdometry();
        initSideGrabber();
        LGrabElbow.setPosition(0.14);
        RGrabElbow.setPosition(0);
        xOdometryEnableServo.setPosition(xOdoDisable);
        initPlatformGrabber();
        initIMU();
        setNewGyro0();

        //grabber.setPosition(grabber_open);
        //pwmThread = new PWMThread();

        waitForStart();
        capstone.setPosition(capstoneClose);
        servoThread.setExtTarget(grabberServoGrab);
        servoThread.directSetGrabTarget(grabber_open);
        LGrabElbow.setPosition(0.3);
        L1.setPower(-0.2);
        Thread.sleep(500);
        L1.setPower(0);
        L1.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
        L1.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
        while (opModeIsActive()) {
            //drive.update();
            if(zheng(this.gamepad1.left_stick_button,leftStickButtonPrimed)){
                L1.setPower(-0.2);
                Thread.sleep(700);
                L1.setPower(0);
                L1.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
                L1.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
            }

            //switch between autoplace on first and second block
            if(zheng(this.gamepad1.right_stick_button,rightStickButtonPrimed)){
                if(autoplacemode==0){
                    grabberOutSwitch=0.75;
                    groundHeightEnc=(2+1.18+1)*slideEncoderPerInch;
                    autoplacemode=1;
                }
                else{
                    autoplacemode=0;
                    groundHeightEnc=(2)*slideEncoderPerInch;
                    grabberOutSwitch=0.63;
                }
            }

            if (zheng(this.gamepad1.x, Xprimed)) {
                if (slow == 2) slow = 0;
                else slow = 2;
            }
            //platform grabber toggle
            if (this.gamepad1.dpad_right) {
                dpad_r = true;
            }
            if (!this.gamepad1.dpad_right && dpad_r) {
                dpad_r = false;
                if (platformGrabbed) {//already held
                    platformGrabbed = false;
                    //platform_grabber.setPower(1);
                    //Thread.sleep(100);
                    //platform_grabber.setPower(0);
                    grabPlatform(false);
                } else {
                    platformGrabbed = true;
                    grabPlatform(true);
                    //platform_grabber.setPower(-0.4);
                }
            }

            if (this.gamepad1.start) {
                start = true;
            }

            if (start && !this.gamepad1.start) {
                start = false;
                if (capstone.getPosition() > (capstoneClose + capstoneOpen) / 2) {
                    capstone.setPosition(capstoneOpen);
                    nextGrabberDelay = true;
                }
                else {
                    capstone.setPosition(capstoneClose);
                }
            }

            if(this.gamepad1.y&&!y){
                y=true;
                if(placeLevel<15)
                    placeLevel++;
                autoLevel=true;
                holdSet=false;
                servoThread.setExtTarget(grabberOutSwitch);
                L1.setPower(-1);
                L2.setPower(1);
            }

            if(!this.gamepad1.y&&y){
                y=false;
            }

            if(this.gamepad1.a&&!a&&this.gamepad1.left_bumper) {
                a=true;
                placeLevel--;
                autoLevel=false;
            }

            if(this.gamepad1.a&&!a&&this.gamepad1.left_trigger>0.5) {
                a=true;
                placeLevel=0;
                autoLevel=false;
            }

            if (!this.gamepad1.a && a) {
                a=false;
            }

            //servo toggle
            if (this.gamepad1.b && !this.gamepad1.left_bumper && !this.gamepad1.y) {
                b = true;
            }
            if (!this.gamepad1.b && b) {
                b = false;
                if (servoThread.grabLastPosition > (grabber_closed + grabber_open) / 2) {//grabber.getPosition()
                    if(nextGrabberDelay){
                        nextGrabberDelay = false;
                        servoThread.delaySetGrabTarget(grabber_open);
                    }else {
                        servoThread.directSetGrabTarget(grabber_open);
                        //grabber.setPosition(grabber_open);
                    }
                } else {
                    servoThread.directSetGrabTarget(grabber_closed);
                    //grabber.setPosition(grabber_closed);
                    servoThread.servoWait(300);
                    servoThread.setExtTarget(0.72);
                }
            }
            if (this.gamepad1.b && this.gamepad1.left_bumper && !this.gamepad1.y) {
                servoThread.directSetGrabTarget(0.01);
                //grabber.setPosition(0.01);
                servoThread.setExtTarget(0.99);
            }

            //driver cancel LT&RT (by dpad up/dpad down/ RB/ LB+Left stick
            if (this.gamepad1.dpad_up || this.gamepad1.dpad_down || this.gamepad1.right_bumper || (this.gamepad1.left_bumper && !near(this.gamepad1.right_stick_y, 0, 0.05))) {
                RTState = -1; //driver interrupt auto movement
                autoPlaceState = -1;
            }

            //RT if RT not started - cancels LT
            if (this.gamepad1.right_trigger > 0.3 && RTState == -1 && !this.gamepad1.y &&!this.gamepad1.a) {
                //when can go 12in above & extender is extended & not started
                holdSet = false;
                autoPlaceState = -1;
                RTState = 0;
            }

            if(this.gamepad1.back && !back){
                servoThread.setExtTarget(0.57);
                back=true;
            }

            if(!this.gamepad1.back && back){
                back=false;
            }

            //RB toggle extender positions (not instant!)
            if (this.gamepad1.right_bumper) {
                rb = true;
            }
            if (!this.gamepad1.right_bumper && rb) {
                rb = false;
                if (servoThread.extLastPosition > (grabberOutSwitch + grabberServoGrab) / 2) {
                    servoThread.setExtTarget(grabberOutSwitch);
                } else {
                    servoThread.setExtTarget(grabberServoGrab);
                }
            }

            //DUP/DDOWN manual extender movement
            //manual extender movement is now in servoThread.


            //If not PWM: run full speed
            if (autoPlaceState != 1) {
                if (slow == 0) {//only one direction at a time
                    joystickScaledMove(-this.gamepad1.left_stick_x, -this.gamepad1.left_stick_y, (this.gamepad1.left_bumper ? 0 : -this.gamepad1.right_stick_x));
                } else if (slow == 2) {
                    slowModeMove(-0.35 * this.gamepad1.left_stick_x, -0.16 * this.gamepad1.left_stick_y, (this.gamepad1.left_bumper ? 0 : -0.3 * this.gamepad1.right_stick_x));
                } else if (slow == 1)
                    slowModeMove(-0.6 * this.gamepad1.left_stick_x, -0.3 * this.gamepad1.left_stick_y, (this.gamepad1.left_bumper ? 0 : -0.3 * this.gamepad1.right_stick_x));
            }

            //tape out/tape in
            if (this.gamepad1.dpad_left) {
                dpad_l = true;
                if (tapeDirectionOut) {
                    xOdometry.setPower(-1);
                } else {
                    xOdometry.setPower(1);
                }
            }
            else {
                if (dpad_l) {
                    dpad_l = false;
                    tapeDirectionOut = !tapeDirectionOut;
                }
                xOdometry.setPower(0);
            }


            //run LT, RT, normal control
            runSlidetoBlock(placeLevel);
            if(!autoLevel)
                runSlide();
            //autoPlace();
            handleRTState();

            if (showTelemetry) {
                telemetry.addData("slide 1", L1.getCurrentPosition());
                telemetry.addData("slide auto level", placeLevel);
                telemetry.addData("ext actual", servoThread.extLastPosition);
                telemetry.addData("RT state", RTState);
                if (holdSet) telemetry.addData("Hold pos", hold);
                telemetry.update();
            }
        }
        //pwmThread.stopThread();
        servoThread.stopThread();
    }



    //-------------------------------------Multithreading---------------------------------/
    /*
    private class PWMThread extends Thread{
        volatile boolean stop = false;
        @Override
        public void run() {
            while(!isInterrupted()&&!stop){
                if(slow==1){

                    setAllDrivePowerSlow(-1*(int)gamepad1.left_stick_y,(int)(gamepad1.left_stick_x),-0.7*(int)(gamepad1.right_stick_x));
                    //joystickScaledMove(-0.4*gamepad1.left_stick_x,-0.13*gamepad1.left_stick_y, (gamepad1.left_bumper?0:-0.25*gamepad1.right_stick_x));
                }else if(slow == 0){
                    setNewGyro0();
                }
            }
        }
        public void stopThread(){
            stop = true;
        }
    }

    private class CompositeMoveThread extends Thread{
        boolean stop = false;
        @Override
        public void run() {
            while(!stop && !isInterrupted()){

            }
        }

        public void stopThread(){
            stop = true;
        }
    }

     */

    private void runSlidetoBlock(int block){
        int goalEnc = (int) (slideEncoderPerInch * 4 * placeLevel + groundHeightEnc);
        if(autoLevel) {
            if (L1.getCurrentPosition() < goalEnc) {
                autoLevel=false;
                holdSet = false;
                holdSlide(goalEnc);
            }
        }
    }


    protected void joystickScaledMove ( double vx, double vy, double vr){
        double[] speeds = {vx - vy + vr, -vy - vx + vr, vx + vy + vr, -vx + vy + vr};
        double absMax = 0;
        for (double d : speeds)
            absMax = Math.max(Math.abs(d), absMax);
        if (Math.abs(vx) < 0.01 && Math.abs(vy) < 0.01 && Math.abs(vr) < 0.01) {
            setAllDrivePower(0);
        } else if (absMax <= 1) {
            setAllDrivePower(speeds[0], speeds[1], speeds[2], speeds[3]);
        } else {
            setAllDrivePower(speeds[0] / absMax, speeds[1] / absMax, speeds[2] / absMax, speeds[3] / absMax);
        }

    }

    protected void slowModeMove ( double vx, double vy, double vr){
        double[] speeds = {vx - vy + vr, -vy - vx + vr, vx + vy + vr, -vx + vy + vr};
        double absMax = 0;
        for (double d : speeds)
            absMax = Math.max(Math.abs(d), absMax);
        if (absMax <= 1 && Math.abs(vr) < 0.01) {
            setAllDrivePowerG(speeds[0], speeds[1], speeds[2], speeds[3]);
        } else if (Math.abs(vr) < 0.01) {
            if (showTelemetry) telemetry.addLine("SCALED power: max was " + absMax);
            setAllDrivePowerG(speeds[0] / absMax, speeds[1] / absMax, speeds[2] / absMax, speeds[3] / absMax);
        } else if (absMax <= 1) {
            setNewGyro0();
            setAllDrivePower(speeds[0], speeds[1], speeds[2], speeds[3]);
        } else {
            setNewGyro0();
            setAllDrivePower(speeds[0] / absMax, speeds[1] / absMax, speeds[2] / absMax, speeds[3] / absMax);
        }
        if (Math.abs(vx) < 0.01 && Math.abs(vy) < 0.01 && Math.abs(vr) < 0.01) {
            setNewGyro0();
            setAllDrivePower(0);
        }
    }
}