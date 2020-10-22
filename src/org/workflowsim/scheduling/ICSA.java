package org.workflowsim.scheduling;

import java.util.ArrayList;
import java.util.Random;

public class ICSA {

    public static int taskNum;
    public static int vmNum;
    public static ArrayList<Crow> schedules = new ArrayList<>();
    public static int[] bestSchedule;
    public static double bestFitness = Double.MAX_VALUE;
    public static int initFlag = 0;
    public static int popSize = 20;
    public static int iterations = 50;
    public static int flightLength = 1;
    public static double worstFitness = Double.MIN_VALUE;
    public static double beta = 1.5;

    public static void initPopsRandomly(int taskNum1,int vmNum1){
        taskNum = taskNum1;
        vmNum = vmNum1;

        for(int i = 0; i < popSize; i++){
            Crow schedule = new Crow(taskNum, vmNum);
            schedules.add(schedule);
        }
        bestSchedule = new int[taskNum];
        initFlag = 1;
    }


    public static void algo(){
        Random random = new Random();
        for (int i = 0; i < popSize; i++){
            int j = random.nextInt(popSize);    // Select crow to be followed randomly
            Crow followed = schedules.get(j);
            Crow following = schedules.get(i);
            double dap = (0.9 * followed.getFitness() / worstFitness) + 0.1;
            double r = random.nextDouble();

            // Finding new position
            if (r > dap){ // Pursuit Mode
                double[] diffVector = new double[taskNum];
                for (int k = 0; k < taskNum; k++)
                    diffVector[k] = (followed.getMemory()[k] - following.getPosition()[k]) * flightLength * r;
                int[] newPosition = new int[taskNum];
                for (int k = 0; k < taskNum; k++) {
                    newPosition[k] = following.getPosition()[k] + (int) diffVector[k];
                    if (newPosition[k] >= vmNum || newPosition[k] < 0) newPosition[k] = random.nextInt(vmNum);
                }
                //added by ab
                following.setPosition(newPosition.clone());
            }
            else{ // Evasion Mode using Levy Flight
                double sigma = doSigma();
                double step = getStep(sigma);
                double[] diffVector = new double[taskNum];
                int[] newPosition = new int[taskNum];
                for (int k = 0; k < taskNum; k++){
                    diffVector[k] = 0.01 - step*(following.getPosition()[k] - bestSchedule[k]);
                    newPosition[k] = following.getPosition()[k] + (int)diffVector[k];
                    if (newPosition[k] >= vmNum || newPosition[k] < 0) newPosition[k] = random.nextInt(vmNum);
                }
                following.setPosition(newPosition.clone());
            }

//            // Updating Memory
//            double newFitness = following.fitness();
//            if (newFitness < following.getmFitness()){
//                following.setmFitness(newFitness);
//                following.setMemory(following.getPosition().clone());
//            }
//
//            // Updating best schedule
//            if (newFitness < bestFitness){
//                bestFitness = newFitness;
//                bestSchedule = following.getPosition().clone();
//            }
//
//            // Updating worst fitness
//            worstFitness = Math.max(worstFitness, newFitness);
        }
    }


    public static void clear(){
        initFlag = 0;
        schedules.clear();
        bestFitness = Double.MAX_VALUE;
        worstFitness = Double.MIN_VALUE;
    }

    private static double getStep(double sigma){
        double u = new Random().nextGaussian()*sigma;
        double v = new Random().nextGaussian();
        return u/Math.pow(Math.abs(v), (1/beta));
    }


    private static double doSigma(){
        double term1 = logGamma(beta+1)*Math.sin((Math.PI*beta)/2);
        double term2 = logGamma((beta+1)/2)*beta*Math.pow(2,(beta-1)/2);
        return Math.pow((term1/term2),(1/beta));
    }

    private static Double logGamma(double x){
        double tmp = (x - 0.5) * Math.log(x + 4.5) - (x + 4.5);
        double ser = 1.0 + 76.18009173    / (x + 0)   - 86.50532033    / (x + 1)
                + 24.01409822    / (x + 2)   -  1.231739516   / (x + 3)
                +  0.00120858003 / (x + 4)   -  0.00000536382 / (x + 5);
        return Math.exp(tmp + Math.log(ser * Math.sqrt(2 * Math.PI)));
    }


}
