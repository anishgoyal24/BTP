package org.workflowsim.scheduling;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class IWOA {

    public static int taskNum;
    public static int vmNum;

    public static int initFlag=0;
    public static int popSize = 20;

    public static int[][] whalePositions;
    public static double[] whaleFitness;

    public static double cMax=1;
    public static double cMin=0.00004;

    public static int[] targetPosition;
    public static double targetFitness  = Integer.MAX_VALUE;

    public static int maxIter = 200;

    public static double gbest_fitness=Double.MAX_VALUE;
    public static int[] gbest_schedule;
    public static List<int[]> schedules=new ArrayList<int[]>();

    public static void init(int jobNum, int maxVmNum){

        taskNum = jobNum;
        vmNum = maxVmNum;

        whalePositions = new int[popSize][taskNum];
        whaleFitness = new double[popSize];
        targetPosition = new int[taskNum];

        gbest_schedule=new int[taskNum];

        for(int i = 0; i < popSize; i++) {
            for (int j = 0; j < taskNum; j++) {
                whalePositions[i][j] = new Random().nextInt(vmNum);
            }
            schedules.add(whalePositions[i]);
        }

        initFlag = 1;
    }

    public static void updateWhales(){

    }

}
