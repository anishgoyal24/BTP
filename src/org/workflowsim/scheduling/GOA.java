package org.workflowsim.scheduling;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

public class GOA {

    public static int taskNum;
    public static int vmNum;

    public static long time;
    public static int initFlag=0;
    public static int popSize = 20;

    public static int[][] grassHopperPositions;
    public static double[] grassHopperFitness;
    public static int[] lb;
    public static int[] ub;
    public static double cMax=1;
    public static double cMin=0.00004;

    public static double[] convergenceCurve;
    public static double[][] trajectories;
    public static double[][] fitnessHistory;
    public static int[][][] positionHistory;
    public static int[] targetPosition;
    public static double targetFitness  = Integer.MAX_VALUE;
    public static final double EPSILON = 1E-14;
    public static int maxIter = 200;

    public static double gbest_fitness=Double.MAX_VALUE;
    public static int[] gbest_schedule;
    public static List<int[]> schedules=new ArrayList<int[]>();//Before Update



    public static void init(int jobNum, int maxVmNum){
        // start timer
        time = System.nanoTime(); // Not sure where to place this

        taskNum = jobNum;
        vmNum = maxVmNum;
        
        grassHopperPositions = new int[popSize][taskNum];

        gbest_schedule=new int[taskNum];

        lb = new int[taskNum];
        ub = new int[taskNum];
        for(int i = 0; i < lb.length ; i++){
            lb[i] = 0;
            ub[i] = vmNum;
        }

        for(int i = 0; i < popSize; i++) {
            for (int j = 0; j < taskNum; j++) {
                grassHopperPositions[i][j] = new Random().nextInt(vmNum);
            }
            schedules.add(grassHopperPositions[i]);
        }

        fitnessHistory = new double[taskNum][maxIter];//initialization of zeros
        positionHistory = new int[popSize][maxIter][taskNum];//initialization of zeros
        convergenceCurve = new double[maxIter];//initialization of zeros
        trajectories = new double[taskNum][maxIter];//initialization of zeros

        initFlag = 1;
    }

    public static void updateGrasshoppers(int l) // l is the iteration number
    {
        double c = cMax-(l*((cMax-cMin)/maxIter)); // Eq. (2.8) in the paper

        ////////////////////////////////////////////////////////////
        for(int i = 0; i < grassHopperPositions.length; i++){

            double[] S_i = new double[taskNum];//initialization of zeros

            for(int j = 0; j < popSize; j++){//check
                if(i != j){
                    double distance = euclideanDistance(grassHopperPositions[i], grassHopperPositions[j]); //Calculate the distance between two grasshoppers
                    double[] r_ij_vec = new double[taskNum];
                    for(int p=0;p < r_ij_vec.length; p++)
                        r_ij_vec[p] = (grassHopperPositions[j][p] - grassHopperPositions[i][p])/(distance+EPSILON);// xj-xi/dij in Eq. (2.7)
                    double xj_xi = 2 + BigDecimal.valueOf(distance).remainder(BigDecimal.valueOf(2)).doubleValue();// |xjd - xid| in Eq. (2.7)

                    double[] s_ij = new double[taskNum];
                    for(int p=0;p < r_ij_vec.length; p++)
                        s_ij[p]=((ub[p] - lb[p])*c/2)*S_func(xj_xi)*r_ij_vec[p];// The first part inside the big bracket in Eq. (2.7)

                    for(int p=0;p < S_i.length; p++)
                        S_i[p]=S_i[p]+s_ij[p];
                }
            }

            double[] S_i_total = S_i;
            double[] X_new = new double[taskNum];
            for(int p=0;p < S_i.length; p++) {
                X_new[p] = c * S_i_total[p] + targetPosition[p];// Eq. (2.7) in the paper
                grassHopperPositions[i][p]=(int)X_new[p];
            }

        }
        ////////////////////////////////////////
        //GrassHopperPositions

        for(int i=0; i < grassHopperPositions.length;i++){
            // Relocate grasshoppers that go outside the search space

            for(int j = 0; j < grassHopperPositions[i].length ; j++){
                if(grassHopperPositions[i][j] > ub[j])grassHopperPositions[i][j]=ub[j];
                if(grassHopperPositions[i][j] < lb[j])grassHopperPositions[i][j]=lb[j];
            }

        }
    }

    public static double euclideanDistance(int[] a, int b[]){
            double distance = 0.0;
            for(int i = 0; i < a.length; i++){
                distance += Math.pow((a[i] - b[i]),2);
            }
            return Math.sqrt(distance);
        }

    public static double S_func(double r){
        double f=0.5;
        double l=1.5;
        return f*Math.exp(-r/l)-Math.exp(-r);  // Eq. (2.3) in the paper
    }

    public static void clear() {
        gbest_fitness = Double.MAX_VALUE;
        initFlag = 0;
        schedules.removeAll(schedules);
    }
}
