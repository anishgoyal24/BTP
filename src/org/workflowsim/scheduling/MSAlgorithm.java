package org.workflowsim.scheduling;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Random;

public class MSAlgorithm {


    public static int taskNum;
    public static int vmNum;
    public static ArrayList<Moth> schedules = new ArrayList<>();
    public static int[] gBestMoth;
    public static double gBestFitness = Double.MAX_VALUE;
    public static int initFlag = 0;
    public static int popSize = 30;
    public static int iterations = 500;
    public static double beta = 1.5;
    public static double sMax = 1.0;
    public static double accFactor = 0.618;
    public static int t = 0;
    public static double lambda;

    public static void initPopsRandomly(int taskNum1,int vmNum1){
        taskNum = taskNum1;
        vmNum = vmNum1;

        for(int i = 0; i < popSize; i++){
            Moth schedule = new Moth(taskNum, vmNum);
            schedules.add(schedule);
        }
        gBestMoth = new int[taskNum];
        initFlag = 1;
        lambda = new Random().nextDouble();
    }

    public static void algo(){
        Random random = new Random();
        Collections.sort(schedules, Comparator.comparing(Moth::getFitness));
        gBestMoth = schedules.get(0).getPosition();
        gBestFitness = schedules.get(0).getFitness();

        // First Subpopulation
        double alpha = sMax/Math.pow(t, 2);
        double sigma = doSigma();
        double step = getStep(sigma);
        for (int i = 0; i < popSize/2; i++){
            int[] newSchedule = new int[taskNum];
            for (int j = 0; j < taskNum; j++){
                int vm = schedules.get(i).getPosition()[j] + (int)(alpha * step);
                newSchedule[j] = vm >= 0 && vm < vmNum ? vm : random.nextInt(vmNum);
            }
            schedules.set(i, new Moth(newSchedule, Double.MAX_VALUE));
        }

        // Second Subpopulation
        for (int i = popSize/2; i < popSize; i++){
            double r = random.nextDouble();
            int[] newSchedule = new int[taskNum];
            if (r > 0.4){
                for (int j = 0; j < taskNum; j++){
                    int vm = (int)(((gBestMoth[j] - schedules.get(i).getPosition()[j]) * (accFactor) + schedules.get(i).getPosition()[j]) * lambda);
                    newSchedule[j] = vm >= 0 && vm < vmNum ? vm : random.nextInt(vmNum);
                }
            }
            else{
                for (int j = 0; j < taskNum; j++){
                    int vm = (int)(((gBestMoth[j] - schedules.get(i).getPosition()[j]) * (1/accFactor) + schedules.get(i).getPosition()[j]) * lambda);
                    newSchedule[j] = vm >= 0 && vm < vmNum ? vm : random.nextInt(vmNum);
                }
            }
            schedules.set(i, new Moth(newSchedule, Double.MAX_VALUE));
        }
    }

    public static void clear(){
        initFlag = 0;
        schedules.clear();
        gBestFitness = Double.MAX_VALUE;
        t = 0;
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
