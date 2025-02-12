package org.workflowsim.scheduling;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class GWO {
    public static int taskNum;
    public static int vmNum;

    public static int initFlag=0;
    public static int popSize = 30;

    public static int[][] wolfPositions;
    public static double[] wolfFitness;
    public static double[][] X1;
    public static double[][] X2;
    public static double[][] X3;
    public static double[] a;
    public static double[] A1;
    public static double[] C1;
    public static double[] A2;
    public static double[] C2;
    public static double[] A3;
    public static double[] C3;
    public static int alphaIndex = 0;
    public static int betaIndex = 0;
    public static int deltaIndex = 0;
    public static double r1;
    public static double r2;
    public static double thetha1;
    public static double thetha2;
    public static double w1;
    public static double w2;
    public static double w3;

    public static int[] alpha_wolf;
    public static int[] beta_wolf;
    public static int[] delta_wolf;

    public static int maxIter = 500;
    public static int current_iteration = 0;

    public static double gbest_fitness=Double.MAX_VALUE;
    public static int[] gbest_schedule;
    public static List<int[]> schedules=new ArrayList<int[]>();

    public static void init(int jobNum, int maxVmNum){
        //System.out.println("gwo-init-"+current_iteration);

        taskNum = jobNum;
        vmNum = maxVmNum;

        wolfPositions = new int[popSize][taskNum];
        wolfFitness = new double[popSize];
        alpha_wolf = new int[taskNum];
        beta_wolf = new int[taskNum];
        delta_wolf = new int[taskNum];
        X1 = new double[popSize][taskNum];
        X2 = new double[popSize][taskNum];
        X3 = new double[popSize][taskNum];
        a = new double[taskNum];
        A1 = new double[taskNum];
        C1 = new double[taskNum];
        A2 = new double[taskNum];
        C2 = new double[taskNum];
        A3 = new double[taskNum];
        C3 = new double[taskNum];

        gbest_schedule=new int[taskNum];

        for(int i = 0; i < popSize; i++) {
            for (int j = 0; j < taskNum; j++) {
                wolfPositions[i][j] = new Random().nextInt(vmNum);
            }
            schedules.add(wolfPositions[i]);
        }

        initFlag = 1;
    }

    public static void updateWolves(){
        // Update alpha, beta and delta wolf
        //System.out.println("gwo-upd-"+current_iteration);

        alphaIndex=0;

        for(int i=0;i<popSize;i++){
            if(wolfFitness[i]<wolfFitness[alphaIndex]){
                alphaIndex = i;
            }
        }

        betaIndex=0;
        Double temp = Double.MAX_VALUE;

        for(int i=0;i<popSize;i++){
            if(i!=alphaIndex && wolfFitness[i]<temp){
                temp = wolfFitness[i];
                betaIndex = i;
            }
        }

        deltaIndex=0;
        temp = Double.MAX_VALUE;

        for(int i=0;i<popSize;i++){
            if(i!=alphaIndex && i!=betaIndex && wolfFitness[i]<temp){
                temp = wolfFitness[i];
                deltaIndex = i;
            }
        }

        //Update top 3 as alpha, beta and delta

        alpha_wolf = wolfPositions[alphaIndex];
        beta_wolf = wolfPositions[betaIndex];
        delta_wolf = wolfPositions[deltaIndex];

        //Update a according to the iteration number

        for(int j=0;j<taskNum;j++)
        {
            //a[j]=2.0-((double)current_iteration*(2.0/(double)maxIter));  //GWO
            a[j] = 2.0*Math.exp((-1*current_iteration)/(maxIter)); //IGWO
            //a[j] = 2.0*(1-((Math.pow(current_iteration,2))/(Math.pow(maxIter,2)))); //mGWO
        }

        // Update wi
        thetha1 = 0.5*Math.atan(current_iteration);
        thetha2 = (2/Math.PI)*(Math.acos(1/3))*(Math.atan(current_iteration));
        w1 = Math.cos(thetha2);
        w2 = 0.5*Math.sin(thetha2)*Math.cos(thetha1);
        w3 = 1-w1-w2;

        //Update position of all wolves

        for(int i=0;i<popSize;i++)
        {

                //r1=Math.random();
                //r2=Math.random();
                r1 = new Random().nextDouble();
                r2 = new Random().nextDouble();

                for(int ii=0;ii<taskNum;ii++)
                {A1[ii]=2.0*a[ii]*r1-a[ii];}
                for(int ii=0;ii<taskNum;ii++)
                {C1[ii]=2.0*r2;}

                r1 = new Random().nextDouble();
                r2 = new Random().nextDouble();

                for(int ii=0;ii<taskNum;ii++)
                {A2[ii]=2.0*a[ii]*r1-a[ii];}
                for(int ii=0;ii<taskNum;ii++)
                {C2[ii]=2.0*r2;}

                r1 = new Random().nextDouble();
                r2 = new Random().nextDouble();

                for(int ii=0;ii<taskNum;ii++)
                {A3[ii]=2.0*a[ii]*r1-a[ii];}
                for(int ii=0;ii<taskNum;ii++)
                {C3[ii]=2.0*r2;}

            for(int j=0;j<taskNum;j++)
            {
                X1[i][j]=alpha_wolf[j]-A1[j]*(Math.abs(C1[j]*alpha_wolf[j]-wolfPositions[i][j]));
                X1[i][j]=simplebounds(X1[i][j]);

                X2[i][j]=beta_wolf[j]-A2[j]*(Math.abs(C2[j]*beta_wolf[j]-wolfPositions[i][j]));
                X2[i][j]=simplebounds(X2[i][j]);

                X3[i][j]=delta_wolf[j]-A3[j]*(Math.abs(C3[j]*delta_wolf[j]-wolfPositions[i][j]));
                X3[i][j]=simplebounds(X3[i][j]);

                wolfPositions[i][j]=(int)( w1*X1[i][j] + w2*X2[i][j] + w3*X3[i][j]);
                //wolfPositions[i][j]=(int)((X1[i][j] + X2[i][j] + X3[i][j])/3.0);

                if(wolfPositions[i][j]<0) wolfPositions[i][j] = 0;
                if(wolfPositions[i][j]>vmNum-1) wolfPositions[i][j] = vmNum-1;

                //if(wolfPositions[i][j]<0 || wolfPositions[i][j]>vmNum-1) wolfPositions[i][j] = new Random().nextInt(vmNum);
            }

            schedules.set(i,wolfPositions[i]);
        }

    }

    public static double simplebounds(double val)
    {  if(val<0) return 0;
       if(val>vmNum-1) return vmNum-1;

       return val;
    }

    public static void clear() {
        gbest_fitness = Double.MAX_VALUE;
        initFlag = 0;
        current_iteration = 0;
        schedules.removeAll(schedules);
        //schedules.clear();
    }

}
