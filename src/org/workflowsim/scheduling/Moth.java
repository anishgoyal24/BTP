package org.workflowsim.scheduling;

import java.util.Random;

public class Moth {

    private int[] position;
    private double fitness = Double.MAX_VALUE;

    public Moth(int taskNum, int vmNum) {
        Random random = new Random();
        position = new int[taskNum];
        for (int i = 0; i < taskNum; i++)
            position[i] = random.nextInt(vmNum);
    }

    public Moth(int[] position, double fitness) {
        this.position = position;
        this.fitness = fitness;
    }

    public int[] getPosition() {
        return position;
    }

    public void setPosition(int[] position) {
        this.position = position;
    }

    public double getFitness() {
        return fitness;
    }

    public void setFitness(double fitness) {
        this.fitness = fitness;
    }
}
