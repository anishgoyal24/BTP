package org.workflowsim.scheduling;

import java.util.Random;

public class Crow {

    private int[] position;
    private int dim;
    private int[] memory;
    private double mFitness = Double.MAX_VALUE;
    private double fitness = Double.MAX_VALUE;



    public Crow(int dim, int numVm){
        setDim(dim);
        position = new int[dim];
        memory = new int[dim];
        init(numVm);
    }

    public void init(int numVm){
        Random random = new Random();
        for (int i = 0; i < dim; i++)
            position[i] = random.nextInt(numVm);
        for (int i = 0; i < dim; i++)
            memory[i] = random.nextInt(numVm);
    }


    public int[] getPosition() {
        return position;
    }

    public void setPosition(int[] position) {
        this.position = position;
    }

    public int getDim() {
        return dim;
    }

    public void setDim(int dim) {
        this.dim = dim;
    }

    public int[] getMemory() {
        return memory;
    }

    public void setMemory(int[] memory) {
        this.memory = memory;
    }

    public double getmFitness() {
        return mFitness;
    }

    public void setmFitness(double mFitness) {
        this.mFitness = mFitness;
    }

    public double getFitness() {
        return fitness;
    }

    public void setFitness(double fitness) {
        this.fitness = fitness;
    }
}
