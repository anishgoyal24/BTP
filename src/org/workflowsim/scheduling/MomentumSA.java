package org.workflowsim.scheduling;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class MomentumSA {

    public static int taskNum;
    public static int vmNum;

    public static int initFlag=0;
    public static int popSize = 30;

    public static int[][] bodyPositions;
    public static double[] bodyFitness;
    public static double[] bodyMass;
    public static double[] bodyVelocity;
    public static int[] best;
    public static int[] worst;
    public static int bestIndex;
    public static int worstIndex;
    public static double externalMass;
    public static double[] externalVelocity;
    public static double vMax = 20.0; // FInd Best value of this
    public static double r1;
    public static double r2;
    public static int sign;

    public static int maxIter = 200;
    public static int current_iteration = 0;

    public static double gbest_fitness=Double.MAX_VALUE;
    public static int[] gbest_schedule;
    public static List<int[]> schedules=new ArrayList<int[]>();

    public static void init(int jobNum, int maxVmNum){
        //System.out.println("gwo-init-"+current_iteration);

        taskNum = jobNum;
        vmNum = maxVmNum;

        bodyPositions = new int[popSize][taskNum];
        bodyFitness = new double[popSize];
        bodyMass = new double[popSize];
        bodyVelocity = new double[taskNum];
        best = new int[taskNum];
        worst = new int[taskNum];
        externalVelocity = new double[taskNum];

        vMax = 2.0;

        gbest_schedule=new int[taskNum];

        for(int i = 0; i < popSize; i++) {
            for (int j = 0; j < taskNum; j++) {
                bodyPositions[i][j] = new Random().nextInt(vmNum);
            }
            schedules.add(bodyPositions[i]);
        }

        initFlag = 1;
    }

    public static void updateBodies(){
        // Find best and worst body

        bestIndex = 0;
        worstIndex = 0;

        for(int i=0;i<popSize;i++){
            if(bodyFitness[i] < bodyFitness[bestIndex]){
                bestIndex = i;
            }
            if(bodyFitness[i] > bodyFitness[worstIndex]){
                worstIndex = i;
            }
        }

        best = bodyPositions[bestIndex];
        worst = bodyPositions[worstIndex];

        // Calculate Mass of all bodies

        for(int i=0;i<popSize;i++){

            bodyMass[i] = (bodyFitness[i]-bodyFitness[worstIndex])/(bodyFitness[bestIndex]-bodyFitness[worstIndex]);
        }

        // Update mass of external body

        externalMass = 1 - (current_iteration-1)/(maxIter-1);

        // For every body, calculate external body velocity, velocity after collision, update position

        for(int i=0;i<popSize;i++){

            // Calculate external body velocity
            r1 = new Random().nextDouble();
            for(int j=0;j<taskNum;j++){
                sign = (best[j]-bodyPositions[i][j])>=0 ? 1 : -1;
                externalVelocity[j] = r1*(externalMass)*vMax*sign;
            }

            // velocity after collision
            for(int j=0;j<taskNum;j++){
                bodyVelocity[j] = (2*externalMass*externalVelocity[j])/(externalMass + bodyMass[i]);
            }

            // update position
            r2 = new Random().nextDouble();
            for(int j=0;j<taskNum;j++){
                bodyPositions[i][j] = bodyPositions[i][j] + (int)(r2*bodyVelocity[j]);
                if(bodyPositions[i][j]<0) bodyPositions[i][j] = 0;
                if(bodyPositions[i][j]>vmNum-1) bodyPositions[i][j] = vmNum-1;

            }

            schedules.set(i,bodyPositions[i]);
        }
    }

    public static void clear() {
        gbest_fitness = Double.MAX_VALUE;
        initFlag = 0;
        schedules.removeAll(schedules);
        current_iteration = 0;
        vMax = 2.0;
    }

}
