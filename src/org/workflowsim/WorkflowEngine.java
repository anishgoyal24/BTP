/**
 * Copyright 2012-2013 University Of Southern California
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.workflowsim;

import java.util.ArrayList;
import java.util.Formatter;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.cloudbus.cloudsim.Cloudlet;
import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.Vm;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.core.CloudSimTags;
import org.cloudbus.cloudsim.core.SimEntity;
import org.cloudbus.cloudsim.core.SimEvent;
import org.fog.entities.Controller;
import org.fog.entities.FogBroker;
import org.fog.entities.OffloadingEngine;
import org.fog.utils.FogEvents;
import org.workflowsim.reclustering.ReclusteringEngine;
import org.workflowsim.scheduling.*;
import org.workflowsim.utils.Parameters;

/**
 * WorkflowEngine represents a engine acting on behalf of a user. It hides VM
 * management, as vm creation, submission of cloudlets to this VMs and
 * destruction of VMs.
 *
 * @author Weiwei Chen
 * @since WorkflowSim Toolkit 1.0
 * @date Apr 9, 2013
 */
public class WorkflowEngine extends SimEntity {

    /**
     * The job list.
     */
    protected List<? extends Cloudlet> jobsList;
    public static double fitness[];
    public static double fitness2[];
    public static int index1=0;
    public static int indexForUpdate=0;
    public static int updateFlag=0;
    public static int updateFlag2=0;
    public static int startlastSchedule=0;
    public static List<Job> jobList =new ArrayList<Job>();
    
    public static int already=0;
    public static int  initIndexForGA=0;
    public static int gaFlag=0;
    public static int gaFlag2=0;
    public static int  tempChildrenIndex=0;
    public static double fitnessForGA[];
    public static double fitnessForGATempChildren[]=new double[4];
    public static int findBestSchedule=0;
    public int iterateNum=0;
	public static ArrayList<double[]> indicators =new ArrayList<double[]>();
	public static ArrayList<Double> updatebest =new ArrayList<Double>();
	public static List<Long> offloadingTimes = new ArrayList<>();

	// For fitness normalized
	public static double maxTime = 0.0;
	public static double maxEnergy = 0.0;
	public static double maxCost = 0.0;


	//ICSA
	public static double fitnessICSA[] = new double[ICSA.popSize];
	public static int initPopIndex = 0;
	public static int indexToUpdate = 0;
	public static ArrayList<Double> gBestFitness = new ArrayList<>();

	//GOA
	public static double fitnessGOA[] = new double[GOA.popSize];
	public static int initGOAIndex = 0;
	public static int updateGOAIndex = 0;

	//GWO
	public static double fitnessGWO[] = new double[GWO.popSize];
	public static int initGWOIndex = 0;
	public static int updateGWOIndex = 0;

	//MS
	public static double fitnessMS[] = new double[MSAlgorithm.popSize];
	public static int initMSIndex = 0;
	public static int updateMSIndex = 0;

	//MomentumSA
	public static double fitnessMomentumSA[] = new double[MomentumSA.popSize];
	public static int initMomentumSAIndex = 0;
	public static int updateMomentumSAIndex = 0;
	
    /**
     * The job submitted list.
     */
    protected List<? extends Cloudlet> jobsSubmittedList;
    /**
     * The job received list.
     */
    protected List<? extends Cloudlet> jobsReceivedList;
    /**
     * The job submitted.
     */
    protected int jobsSubmitted;
    protected List<? extends Vm> vmList;
    /**
     * The associated scheduler id*
     */
    private List<Integer> schedulerId;
    private List<FogBroker> scheduler;
    private int controllerId;
    private static OffloadingEngine offloadingEngine;
    
    /**
     * the end time of algorithm
     */
    public long endTime;
    /**
     * the excution time of algorithm
     */
    public long algorithmTime;
    /**
     * the deadline for workflow
     */
    public double DeadLine;

    /**
     * Created a new WorkflowEngine object.
     *
     * @param name name to be associated with this entity (as required by
     * Sim_entity class from simjava package)
     * @throws Exception the exception
     * @pre name != null
     * @post $none
     */
    public WorkflowEngine(String name) throws Exception {
        this(name, 1);
    }

    public WorkflowEngine(String name, int schedulers) throws Exception {
        super(name);

        setJobsList(new ArrayList<>());
        setJobsSubmittedList(new ArrayList<>());
        setJobsReceivedList(new ArrayList<>());

        jobsSubmitted = 0;

        setSchedulers(new ArrayList<>());
        setSchedulerIds(new ArrayList<>());

        for (int i = 0; i < schedulers; i++) {
            //FogBroker wfs = new FogBroker(name + "_Scheduler_" + i);
            
            offloadingEngine = new OffloadingEngine("OffloadingEngine", null);
            offloadingEngine.setWorkflowEngine(this);
            
            FogBroker wfs = new FogBroker("FogScheduler");
            getSchedulers().add(wfs);
            getSchedulerIds().add(wfs.getId());
            wfs.setWorkflowEngineId(this.getId());
        }
    }

    /**
     * This method is used to send to the broker the list with virtual machines
     * that must be created.
     *
     * @param list the list
     * @param schedulerId the scheduler id
     */
    public void submitVmList(List<? extends Vm> list, int schedulerId) {
        getScheduler(schedulerId).submitVmList(list);
    }

    public void submitVmList(List<? extends Vm> list) {
        //bug here, not sure whether we should have different workflow schedulers
        getScheduler(0).submitVmList(list);
        setVmList(list);
    }
    
    public List<? extends Vm> getAllVmList(){
        if(this.vmList != null && !this.vmList.isEmpty()){
            return this.vmList;
        }
        else{
            List list = new ArrayList();
            for(int i = 0;i < getSchedulers().size();i ++){
                list.addAll(getScheduler(i).getVmList());
            }
            return list;
        }
    }

    /**
     * This method is used to send to the broker the list of cloudlets.
     *
     * @param list the list
     */
    public void submitCloudletList(List<? extends Cloudlet> list) {
        getJobsList().addAll(list);
    }

    /**
     * Processes events available for this Broker.
     *
     * @param ev a SimEvent object
     */
    @Override
    public void processEvent(SimEvent ev) {
//    	if(CloudSim.clock()>40)
//    	System.out.println(CloudSim.clock()+":"+CloudSim.getEntityName(ev.getSource())+"-> WorkflowEngine = "+ev.getTag());
        switch (ev.getTag()) {
            // Resource characteristics request
            case CloudSimTags.RESOURCE_CHARACTERISTICS_REQUEST:
                processResourceCharacteristicsRequest(ev);
                break;
            //this call is from workflow scheduler when all vms are created
            case CloudSimTags.CLOUDLET_SUBMIT:
                submitJobs();
                break;
			case CloudSimTags.CLOUDLET_RETURN:
				switch (Parameters.getSchedulingAlgorithm()) {
					case MS:
						try{
							processJobReturnForMS(ev);
						}
						catch (Exception e){
							e.printStackTrace();
						}
						break;
					case GWO:
						try{
							processJobReturnForGWO(ev);
						}
						catch (Exception e){
							e.printStackTrace();
						}
						break;
					case MOM:
						try{
							processJobReturnForMomentumSA(ev);
						}
						catch (Exception e){
							e.printStackTrace();
						}
						break;
					case GOA:
						try{
							processJobReturnForGOA(ev);
						}
						catch (Exception e){
							e.printStackTrace();
						}
						break;
					case ICSA:
						try{
							processJobReturnforICSA(ev);
						}
						catch (Exception e){
							e.printStackTrace();
						}
						break;
					case PSO:
						try {
							processJobReturnForPSO(ev);
						} catch (Exception e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
						break;
					case GA:
						try {
							processJobReturnForGA(ev);
						} catch (Exception e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
						break;

				case MINMIN:
				case MAXMIN:
				case FCFS:
				case MCT:
				case STATIC:
				case DATA:
				case ROUNDROBIN:
					processJobReturn(ev);
					break;
				default:
					break;
				}
            break;
            case CloudSimTags.END_OF_SIMULATION:
                shutdownEntity();
                break;
            case WorkflowSimTags.JOB_SUBMIT:
                processJobSubmit(ev);
                break;
            default:
                processOtherEvent(ev);
                break;
        }
    }

	/**
     * Process a request for the characteristics of a PowerDatacenter.
     *
     * @param ev a SimEvent object
     */
    protected void processResourceCharacteristicsRequest(SimEvent ev) {
        for (int i = 0; i < getSchedulerIds().size(); i++) {
            schedule(getSchedulerId(i), 0, CloudSimTags.RESOURCE_CHARACTERISTICS_REQUEST);
        }
    }

    /**
     * Binds a scheduler with a datacenter.
     *
     * @param datacenterId the data center id
     * @param schedulerId the scheduler id
     */
    public void bindSchedulerDatacenter(int datacenterId, int schedulerId) {
        getScheduler(schedulerId).bindSchedulerDatacenter(datacenterId);
    }

    /**
     * Binds a datacenter to the default scheduler (id=0)
     *
     * @param datacenterId dataceter Id
     */
    public void bindSchedulerDatacenter(int datacenterId) {
        bindSchedulerDatacenter(datacenterId, 0);
    }
   
    /**
     * Process a submit event
     *
     * @param ev a SimEvent object
     */
    protected void processJobSubmit(SimEvent ev) {
        List<? extends Cloudlet> list = (List) ev.getData();
        setJobsList(list);
        for(Object job1 : getJobsList()) {
        	Job job=(Job)job1;
        	jobList.add(job);
        }
        if(getoffloadingEngine().getOffloadingStrategy() != null){
        	long offloadingTime = getoffloadingEngine().run(jobList, DeadLine);
        	offloadingTimes.add(offloadingTime);
        }
    }

    /**
     * Process a job return event.
     *
     * @param ev a SimEvent object
     * @pre ev != $null
     * @post $none
     */
    protected void processJobReturn(SimEvent ev) {

        Job job = (Job) ev.getData();
        if (job.getCloudletStatus() == Cloudlet.FAILED) {
            // Reclusteringengine will add retry job to jobList
            int newId = getJobsList().size() + getJobsSubmittedList().size();
            getJobsList().addAll(ReclusteringEngine.process(job, newId));
        }

        getJobsReceivedList().add(job);
        jobsSubmitted--;
        if (getJobsList().isEmpty() && jobsSubmitted == 0) {
            //send msg to all the schedulers
            /*for (int i = 0; i < getSchedulerIds().size(); i++) {
                //sendNow(getSchedulerId(i), CloudSimTags.END_OF_SIMULATION, null);
            }*/
        	sendNow(controllerId, FogEvents.STOP_SIMULATION, null);
        } else {
            sendNow(this.getId(), CloudSimTags.CLOUDLET_SUBMIT, null);
        }
    }
    
    /**
     * Process a job return event.
     *数据中心处理完成以后 ，工作流引擎调用processJobReturn方法：若任务处理失败，则调用ReclusteringEngine中的方法，以便重新执行此任务
     *若任务处理成功，判断是否所有的任务都已经处理完成，若是，则通知调度机结束仿真，否则，通知自身继续提交任务
     * @param ev a SimEvent object
     * @throws Exception 
     * @pre ev != $null
     * @post $none
     */
    protected void processJobReturnForPSO(SimEvent ev) throws Exception {
    	Job job = (Job) ev.getData();
        if (job.getCloudletStatus() == Cloudlet.FAILED) {
            // Reclusteringengine will add retry job to jobList
        	System.out.println("failed");
            int newId = getJobsList().size() + getJobsSubmittedList().size();
            getJobsList().addAll(ReclusteringEngine.process(job, newId));
        }
        getJobsReceivedList().add(job);
        jobsSubmitted--;
        
        if(getJobsList().isEmpty() && jobsSubmitted == 0) {
    		//System.out.println("-------------------------------------------");
        	if(index1 != PsoScheduling.particleNum) {
        		already = 1;
    			//Processed a particle (the particle obtained by initialization)
             	FogBroker.count++;
             	fitness[index1++] = caculatefitness();
             	init();
                if(index1 == PsoScheduling.particleNum) {
	        		init();
                	updateFlag = 1;
                	updateFlag2 = 1;
             	}
             	sendNow(this.getId(), CloudSimTags.CLOUDLET_SUBMIT, null); 
    		}
        	else if(indexForUpdate != PsoScheduling.particleNum) {
 	        	//Processed all the particles obtained by initialization, and processed one particle (particles obtained after update)
        		int gbestIndex = 0;
    			for(int i = 0; i < index1; i++) {
    				if(fitness[i] < PsoScheduling.gbest_fitness) {
    					 PsoScheduling.gbest_fitness = fitness[i];
    					 gbestIndex = i;
    				}
    			}
    			PsoScheduling.gbest_schedule = PsoScheduling.pbest_schedule.get(gbestIndex);//Update the global optimal scheduling plan
    			
    			FogBroker.count2++;
	        	fitness2[indexForUpdate++] = caculatefitness();
	        	if(FogBroker.count2 != PsoScheduling.particleNum && PsoScheduling.iterateNum > iterateNum) {
	        		init();
	        		sendNow(this.getId(), CloudSimTags.CLOUDLET_SUBMIT, null);
	        	}
 		    }
            if(index1 == PsoScheduling.particleNum && indexForUpdate == PsoScheduling.particleNum) {
 	        	//Processed all the particles obtained after initialization and particles obtained after update
            	if(PsoScheduling.iterateNum > iterateNum) {
            		for(int i = 0; i < PsoScheduling.particleNum; i++) { 
            			//Update individual optimal
            			if(fitness[i] > fitness2[i]) {
            				int schedule1[] = PsoScheduling.pbest_schedule.get(i);
            				int schedule2[] = PsoScheduling.newSchedules.get(i);
            				for(int j = 0; j < schedule1.length; j++)
            					schedule1[j] = schedule2[j];
            				PsoScheduling.pbest_fitness[i] = fitness2[i]; 
            			}
            			else
            				PsoScheduling.pbest_fitness[i]=fitness[i];
	        		}
            		fitness = PsoScheduling.pbest_fitness;
            		for(int i = 0; i < PsoScheduling.particleNum; i++) {  //更新全局最优
            			if(PsoScheduling.pbest_fitness[i] < PsoScheduling.gbest_fitness) {
            				//index1=i;
            				PsoScheduling.gbest_fitness = PsoScheduling.pbest_fitness[i];
            				PsoScheduling.gbest_schedule = PsoScheduling.pbest_schedule.get(i);
            			}
            		}
            		iterateNum++;
//            		System.out.println("After "+iterateNum+" iterations:");
//            		System.out.println("======gbest_fitness:========"+PsoScheduling.gbest_fitness);
            		updatebest.add(PsoScheduling.gbest_fitness);
//	              	printindicators(PsoScheduling.gbest_fitness);
            		
            		if(PsoScheduling.iterateNum != iterateNum) {
    	        		indexForUpdate = 0;
    	        		FogBroker.count2 = 0;
    	        		getController().updateExecutionTime();
        	        	init(); 
         		    	updateFlag2 = 1;
         		    	PsoScheduling.newSchedules.removeAll(PsoScheduling.newSchedules);
         		    	sendNow(this.getId(), CloudSimTags.CLOUDLET_SUBMIT, null);
    	        	}
            	}
            }
            if(startlastSchedule == 1) {
            	double f = caculatefitness();
            	System.out.println("The last result : "+f);
            	for (int i = 0; i < getSchedulerIds().size(); i++) {
                    sendNow(getSchedulerId(i), CloudSimTags.END_OF_SIMULATION, null);
                }
     		}
            if(PsoScheduling.iterateNum == iterateNum && startlastSchedule == 0) {
            	for(int i = 0; i < PsoScheduling.particleNum; i++) {
            		if(PsoScheduling.pbest_fitness[i] == PsoScheduling.gbest_fitness) {
            			PsoScheduling.gbest_schedule = PsoScheduling.pbest_schedule.get(i);
            		}
            	}
            	//Record the end time of the pso
            	endTime = System.currentTimeMillis();
            	algorithmTime = endTime - getScheduler(0).startTime;
            	
            	startlastSchedule = 1;
            	caculatefitness();
            	init();
            	//sendNow(this.getSchedulerId(0), CloudSimTags.CLOUDLET_SUBMIT, submittedList);
            	sendNow(this.getId(), CloudSimTags.CLOUDLET_SUBMIT, null);
            }
        }
        else{
        	updateFlag2 = 0;
        	sendNow(this.getId(), CloudSimTags.CLOUDLET_SUBMIT, null);
        }
//      sendNow(getSchedulerId(0), CloudSimTags.END_OF_SIMULATION, null);
//    	sendNow(getcontrollerId(), FogEvents.STOP_SIMULATION,null);
    }

	protected void processJobReturnForMomentumSA(SimEvent ev) throws Exception {
		Job job = (Job) ev.getData();
		if (job.getCloudletStatus() == Cloudlet.FAILED) {
			// Reclusteringengine will add retry job to jobList
			System.out.println("failed");
			int newId = getJobsList().size() + getJobsSubmittedList().size();
			getJobsList().addAll(ReclusteringEngine.process(job, newId));
		}
		getJobsReceivedList().add(job);
		jobsSubmitted--;

		MomentumSA.current_iteration = iterateNum;

		if(getJobsList().isEmpty() && jobsSubmitted == 0) {
			//System.out.println("-------------------------------------------");
			if(initMomentumSAIndex != MomentumSA.popSize) {
				already = 1;
				//Processed a particle (the particle obtained by initialization)
				FogBroker.count++;
				fitnessMomentumSA[initMomentumSAIndex++] = caculatefitness();
				init();
				if(initMomentumSAIndex == MomentumSA.popSize) {
					init();
					updateFlag = 1;
					updateFlag2 = 1;
				}
				sendNow(this.getId(), CloudSimTags.CLOUDLET_SUBMIT, null);
			}
			else if(updateMomentumSAIndex != MomentumSA.popSize) {
				//Processed all the particles obtained by initialization, and processed one particle (particles obtained after update)
				int gbestIndex = -1;
				for(int i = 0; i < initMomentumSAIndex; i++) {
					if(fitnessMomentumSA[i] < MomentumSA.gbest_fitness) {
						MomentumSA.gbest_fitness = fitnessMomentumSA[i];
						gbestIndex = i;
					}
				}
				if(gbestIndex > -1) MomentumSA.gbest_schedule = MomentumSA.bodyPositions[gbestIndex];//Update the global optimal scheduling plan

				FogBroker.count2++;

				fitnessMomentumSA[updateMomentumSAIndex++] = caculatefitness();

				if(FogBroker.count2 != MomentumSA.popSize && MomentumSA.maxIter > iterateNum) {
					init();
					sendNow(this.getId(), CloudSimTags.CLOUDLET_SUBMIT, null);
				}
			}
			if(initMomentumSAIndex == MomentumSA.popSize && updateMomentumSAIndex == MomentumSA.popSize) {
				//Processed all the particles obtained after initialization and particles obtained after update
				if(MomentumSA.maxIter > iterateNum) {

					for(int i = 0; i < MomentumSA.popSize; i++) {  //更新全局最优
						if(fitnessMomentumSA[i] < MomentumSA.gbest_fitness) {
							MomentumSA.gbest_fitness = fitnessMomentumSA[i];
							MomentumSA.gbest_schedule = MomentumSA.bodyPositions[i];
						}
					}
					iterateNum++;

//					System.out.println("After "+iterateNum+" iterations:");
//					System.out.println("======gbest_fitness:========"+MomentumSA.gbest_fitness);

					updatebest.add(MomentumSA.gbest_fitness);
//	              	printindicators(MomentumSA.gbest_fitness);

					if(MomentumSA.maxIter != iterateNum) {
						updateMomentumSAIndex = 0;
						FogBroker.count2 = 0;
						getController().updateExecutionTime();
						init();
						updateFlag2 = 1;
						sendNow(this.getId(), CloudSimTags.CLOUDLET_SUBMIT, null);
					}
				}
			}
			if(startlastSchedule == 1) {
				double f = caculatefitness();
				System.out.println("The last result : "+f);
				for (int i = 0; i < getSchedulerIds().size(); i++) {
					sendNow(getSchedulerId(i), CloudSimTags.END_OF_SIMULATION, null);
				}
			}
			if(MomentumSA.maxIter == iterateNum && startlastSchedule == 0) {
				for(int i = 0; i < MomentumSA.popSize; i++) {
					if(fitnessMomentumSA[i] == MomentumSA.gbest_fitness) {
						MomentumSA.gbest_schedule = MomentumSA.bodyPositions[i];
					}
				}
				//Record the end time of the pso
				endTime = System.currentTimeMillis();
				algorithmTime = endTime - getScheduler(0).startTime;

				startlastSchedule = 1;
				caculatefitness();
				init();
				//sendNow(this.getSchedulerId(0), CloudSimTags.CLOUDLET_SUBMIT, submittedList);
				sendNow(this.getId(), CloudSimTags.CLOUDLET_SUBMIT, null);
			}
		}
		else{
			updateFlag2 = 0;
			sendNow(this.getId(), CloudSimTags.CLOUDLET_SUBMIT, null);
		}
//      sendNow(getSchedulerId(0), CloudSimTags.END_OF_SIMULATION, null);
//    	sendNow(getcontrollerId(), FogEvents.STOP_SIMULATION,null);
	}

	protected void processJobReturnForGWO(SimEvent ev) throws Exception {
		Job job = (Job) ev.getData();
		if (job.getCloudletStatus() == Cloudlet.FAILED) {
			// Reclusteringengine will add retry job to jobList
			System.out.println("failed");
			int newId = getJobsList().size() + getJobsSubmittedList().size();
			getJobsList().addAll(ReclusteringEngine.process(job, newId));
		}
		getJobsReceivedList().add(job);
		jobsSubmitted--;

		GWO.current_iteration = iterateNum;
		//System.out.println(iterateNum + " " + initGWOIndex + " " + updateGWOIndex);

		if(getJobsList().isEmpty() && jobsSubmitted == 0) {
			//System.out.println("-------------------------------------------");
			if(initGWOIndex != GWO.popSize) {
				already = 1;
				//Processed a particle (the particle obtained by initialization)
				FogBroker.count++;
				fitnessGWO[initGWOIndex] = caculatefitness();
				GWO.wolfFitness[initGWOIndex] = fitnessGWO[initGWOIndex];

				initGWOIndex++;
				init();
				if(initGWOIndex == GWO.popSize) {
					init();
					updateFlag = 1;
					updateFlag2 = 1;
				}
				sendNow(this.getId(), CloudSimTags.CLOUDLET_SUBMIT, null);
			}
			else if(updateGWOIndex != GWO.popSize) {
				//Processed all the particles obtained by initialization, and processed one particle (particles obtained after update)
				int gbestIndex = -1;
				for(int i = 0; i < initGWOIndex; i++) {
					if(fitnessGWO[i] < GWO.gbest_fitness) {
						GWO.gbest_fitness = fitnessGWO[i];
						gbestIndex = i;
					}
				}
				if (gbestIndex > -1) GWO.gbest_schedule = GWO.wolfPositions[gbestIndex];//Update the global optimal scheduling plan


				FogBroker.count2++;
				fitnessGWO[updateGWOIndex] = caculatefitness();

				//Updation Code
				GWO.wolfFitness[updateGWOIndex] = fitnessGWO[updateGWOIndex];

				updateGWOIndex++;

				if(FogBroker.count2 != GWO.popSize && GWO.maxIter > iterateNum) {
					init();
					sendNow(this.getId(), CloudSimTags.CLOUDLET_SUBMIT, null);
				}
			}
			if(initGWOIndex == GWO.popSize && updateGWOIndex == GWO.popSize) {
				//System.out.println(iterateNum);
				//Processed all the particles obtained after initialization and particles obtained after update
				if(GWO.maxIter > iterateNum) {

					for(int i = 0; i < GWO.popSize; i++) {  //更新全局最优
						if(fitnessGWO[i] < GWO.gbest_fitness) {
							//index1=i;
							GWO.gbest_fitness = fitnessGWO[i];
							GWO.gbest_schedule = GWO.wolfPositions[i];
						}
					}
					iterateNum++;
					//System.out.println("After "+iterateNum+" iterations:");
					//System.out.println("======gbest_fitness:========"+PsoScheduling.gbest_fitness);
					updatebest.add(GWO.gbest_fitness);
//	              	printindicators(PsoScheduling.gbest_fitness);

					if(GWO.maxIter != iterateNum) {
						updateGWOIndex = 0;
						FogBroker.count2 = 0;
						getController().updateExecutionTime();
						init();
						updateFlag2 = 1;
						//PsoScheduling.newSchedules.removeAll(PsoScheduling.newSchedules);
						sendNow(this.getId(), CloudSimTags.CLOUDLET_SUBMIT, null);
					}
				}
			}
			if(startlastSchedule == 1) {
				double f = caculatefitness();
				System.out.println("The last result : "+f);
				for (int i = 0; i < getSchedulerIds().size(); i++) {
					sendNow(getSchedulerId(i), CloudSimTags.END_OF_SIMULATION, null);
				}
			}
			if(GWO.maxIter == iterateNum && startlastSchedule == 0) {
				//System.out.println("here");
				for(int i = 0; i < GWO.popSize; i++) {
					if(fitnessGWO[i] == GWO.gbest_fitness) {
						GWO.gbest_schedule = GWO.wolfPositions[i];
					}
				}
				//Record the end time of the pso
				endTime = System.currentTimeMillis();
				algorithmTime = endTime - getScheduler(0).startTime;

				startlastSchedule = 1;
				caculatefitness();
				init();
				//sendNow(this.getSchedulerId(0), CloudSimTags.CLOUDLET_SUBMIT, submittedList);
				sendNow(this.getId(), CloudSimTags.CLOUDLET_SUBMIT, null);
			}
		}
		else{
			updateFlag2 = 0;
			sendNow(this.getId(), CloudSimTags.CLOUDLET_SUBMIT, null);
		}
//      sendNow(getSchedulerId(0), CloudSimTags.END_OF_SIMULATION, null);
//    	sendNow(getcontrollerId(), FogEvents.STOP_SIMULATION,null);
	}

	private void processJobReturnForMS(SimEvent ev) throws Exception{
		Job job = (Job) ev.getData();
		if (job.getCloudletStatus() == Cloudlet.FAILED) {
			// Reclusteringengine will add retry job to jobList
			System.out.println("failed");
			int newId = getJobsList().size() + getJobsSubmittedList().size();
			getJobsList().addAll(ReclusteringEngine.process(job, newId));
		}
		getJobsReceivedList().add(job);
		jobsSubmitted--;

		MSAlgorithm.t = iterateNum; // To get current iteration number

		if (getJobsList().isEmpty() && jobsSubmitted == 0){
			if(initMSIndex != MSAlgorithm.popSize) {
				already = 1;
				//Processed a particle (the particle obtained by initialization)
				FogBroker.count++;
				fitnessMS[initMSIndex] = caculatefitness();
				MSAlgorithm.schedules.get(initMSIndex).setFitness(fitnessMS[initMSIndex]);
				initMSIndex++;
				init();
				if(initMSIndex == MSAlgorithm.popSize) {
					init();
					updateFlag = 1;
					updateFlag2 = 1;
				}
				sendNow(this.getId(), CloudSimTags.CLOUDLET_SUBMIT, null);
			}

			else if(updateMSIndex != MSAlgorithm.popSize) {
				//Processed all the particles obtained by initialization, and processed one particle (particles obtained after update)
				FogBroker.count2++;
				fitnessMS[updateMSIndex] = caculatefitness();
				MSAlgorithm.schedules.get(updateMSIndex).setFitness(fitnessMS[updateMSIndex]);
				updateMSIndex++;

				if(FogBroker.count2 != MSAlgorithm.popSize && MSAlgorithm.iterations > iterateNum) {
					init();
					sendNow(this.getId(), CloudSimTags.CLOUDLET_SUBMIT, null);
				}
			}

			if(initMSIndex == MSAlgorithm.popSize && updateMSIndex == MSAlgorithm.popSize) {
				//Processed all the particles obtained after initialization and particles obtained after update
				if(MSAlgorithm.iterations > iterateNum) {
//					for(int i = 0; i < MSAlgorithm.popSize; i++) {  //更新全局最优
//						if(MSAlgorithm.schedules.get(i).getFitness() < MSAlgorithm.gBestFitness) {
//							MSAlgorithm.gBestFitness = MSAlgorithm.schedules.get(i).getFitness();
//							MSAlgorithm.gBestMoth = MSAlgorithm.schedules.get(i).getPosition();
//						}
//					}

					iterateNum++;
					//System.out.println("After "+iterateNum+" iterations:");
					//System.out.println("======gbest_fitness:========"+ICSA.bestFitness);
					gBestFitness.add(MSAlgorithm.gBestFitness);
//	              	printindicators(PsoScheduling.gbest_fitness);

					if(MSAlgorithm.iterations != iterateNum) {
						updateMSIndex = 0;
						FogBroker.count2 = 0;
						getController().updateExecutionTime();
						init();
						updateFlag2 = 1;
						//ICSA.newSchedules.removeAll(PsoScheduling.newSchedules);
						sendNow(this.getId(), CloudSimTags.CLOUDLET_SUBMIT, null);
					}
				}
			}

			if(startlastSchedule == 1) {
				double f = caculatefitness();
				//System.out.println("The last result : "+f);
				for (int i = 0; i < getSchedulerIds().size(); i++) {
					sendNow(getSchedulerId(i), CloudSimTags.END_OF_SIMULATION, null);
				}
			}
			if(MSAlgorithm.iterations == iterateNum && startlastSchedule == 0) {


				for(int i = 0; i < MSAlgorithm.popSize; i++) {
					if(MSAlgorithm.schedules.get(i).getFitness() == MSAlgorithm.gBestFitness) {
						MSAlgorithm.gBestMoth = MSAlgorithm.schedules.get(i).getPosition();
					}
				}
				//Record the end time of the ICSA
				endTime = System.currentTimeMillis();
				algorithmTime = endTime - getScheduler(0).startTime;

				startlastSchedule = 1;
				caculatefitness();
				init();
				//sendNow(this.getSchedulerId(0), CloudSimTags.CLOUDLET_SUBMIT, submittedList);
				sendNow(this.getId(), CloudSimTags.CLOUDLET_SUBMIT, null);
			}
		}
		else{
			updateFlag2 = 0;
			sendNow(this.getId(), CloudSimTags.CLOUDLET_SUBMIT, null);
		}
	}

	protected void processJobReturnForGOA(SimEvent ev) throws Exception {
		Job job = (Job) ev.getData();
		if (job.getCloudletStatus() == Cloudlet.FAILED) {
			// Reclusteringengine will add retry job to jobList
			System.out.println("failed");
			int newId = getJobsList().size() + getJobsSubmittedList().size();
			getJobsList().addAll(ReclusteringEngine.process(job, newId));
		}
		getJobsReceivedList().add(job);
		jobsSubmitted--;

		GOA.l = iterateNum;
		//System.out.println(iterateNum + " " + initGOAIndex + " " + updateGOAIndex);

		if(getJobsList().isEmpty() && jobsSubmitted == 0) {
			//System.out.println("-------------------------------------------");
			if(initGOAIndex != GOA.popSize) {
				already = 1;
				//Processed a particle (the particle obtained by initialization)
				FogBroker.count++;
				fitnessGOA[initGOAIndex] = caculatefitness();
				// Add fitness init code
				//GOA.fitnessHistory[initGOAIndex][0] = fitnessGOA[initGOAIndex];
				//GOA.positionHistory[initGOAIndex][0] = GOA.grassHopperPositions[initGOAIndex];
				//GOA.trajectories[initGOAIndex][0]= GOA.grassHopperPositions[initGOAIndex][0];
				if(fitnessGOA[initGOAIndex] < GOA.targetFitness){
					GOA.targetFitness = fitnessGOA[initGOAIndex];
					GOA.targetPosition = GOA.grassHopperPositions[initGOAIndex];
				}

				initGOAIndex++;
				init();
				if(initGOAIndex == GOA.popSize) {
					init();
					updateFlag = 1;
					updateFlag2 = 1;
				}
				sendNow(this.getId(), CloudSimTags.CLOUDLET_SUBMIT, null);
			}
			else if(updateGOAIndex != GOA.popSize) {
				//Processed all the particles obtained by initialization, and processed one particle (particles obtained after update)
//				int gbestIndex = -1;
//				for(int i = 0; i < initGOAIndex; i++) {
//					if(fitnessGOA[i] < GOA.gbest_fitness) {
//						GOA.gbest_fitness = fitnessGOA[i];
//						gbestIndex = i;
//					}
//				}
//				if (gbestIndex > -1) GOA.gbest_schedule = GOA.grassHopperPositions[gbestIndex];//Update the global optimal scheduling plan

				FogBroker.count2++;
				fitnessGOA[updateGOAIndex] = caculatefitness();

				//Updation Code
				if(fitnessGOA[updateGOAIndex] < GOA.targetFitness){
					GOA.targetFitness = fitnessGOA[updateGOAIndex];
					GOA.targetPosition = GOA.grassHopperPositions[updateGOAIndex];
				}

				updateGOAIndex++;

				if(FogBroker.count2 != GOA.popSize && GOA.maxIter > iterateNum) {
					init();
					sendNow(this.getId(), CloudSimTags.CLOUDLET_SUBMIT, null);
				}
			}
			if(initGOAIndex == GOA.popSize && updateGOAIndex == GOA.popSize) {
				//System.out.println(iterateNum);
				//Processed all the particles obtained after initialization and particles obtained after update
				if(GOA.maxIter > iterateNum) {
					for(int i = 0; i < GOA.popSize; i++) {
						//Update individual optimal
						if(fitnessGOA[i] < GOA.targetFitness){
							GOA.targetFitness = fitnessGOA[i];
							GOA.targetPosition = GOA.grassHopperPositions[i];
						}
					}

//					for(int i = 0; i < GOA.popSize; i++) {  //更新全局最优
//						if(fitnessGOA[i] < GOA.gbest_fitness) {
//							//index1=i;
//							GOA.gbest_fitness = fitnessGOA[i];
//							GOA.gbest_schedule = GOA.grassHopperPositions[i];
//						}
//					}
					iterateNum++;
					//System.out.println("After "+iterateNum+" iterations:");
					//System.out.println("======gbest_fitness:========"+PsoScheduling.gbest_fitness);
					updatebest.add(GOA.targetFitness);
//	              	printindicators(PsoScheduling.gbest_fitness);

					if(GOA.maxIter != iterateNum) {
						System.out.println(GOA.targetFitness);
						updateGOAIndex = 0;
						FogBroker.count2 = 0;
						getController().updateExecutionTime();
						init();
						updateFlag2 = 1;
						//PsoScheduling.newSchedules.removeAll(PsoScheduling.newSchedules);
						sendNow(this.getId(), CloudSimTags.CLOUDLET_SUBMIT, null);
					}
				}
			}
			if(startlastSchedule == 1) {
				System.out.println("last");
				double f = caculatefitness();
				System.out.println("The last result : "+f);
				for (int i = 0; i < getSchedulerIds().size(); i++) {
					sendNow(getSchedulerId(i), CloudSimTags.END_OF_SIMULATION, null);
				}
			}
			if(GOA.maxIter == iterateNum && startlastSchedule == 0) {
				//System.out.println("here");
//				for(int i = 0; i < GOA.popSize; i++) {
//					if(fitnessGOA[i] == GOA.gbest_fitness) {
//						GOA.gbest_schedule = GOA.grassHopperPositions[i];
//					}
//				}
				//Record the end time of the pso
				endTime = System.currentTimeMillis();
				algorithmTime = endTime - getScheduler(0).startTime;

				startlastSchedule = 1;
				caculatefitness();
				init();
				//sendNow(this.getSchedulerId(0), CloudSimTags.CLOUDLET_SUBMIT, submittedList);
				sendNow(this.getId(), CloudSimTags.CLOUDLET_SUBMIT, null);
			}
		}
		else{
			updateFlag2 = 0;
			sendNow(this.getId(), CloudSimTags.CLOUDLET_SUBMIT, null);
		}
//      sendNow(getSchedulerId(0), CloudSimTags.END_OF_SIMULATION, null);
//    	sendNow(getcontrollerId(), FogEvents.STOP_SIMULATION,null);
	}

	private void processJobReturnforICSA(SimEvent ev) throws Exception{
		Job job = (Job) ev.getData();
		if (job.getCloudletStatus() == Cloudlet.FAILED) {
			// Re-clustering Engine will add retry job to jobList
			System.out.println("failed");
			int newId = getJobsList().size() + getJobsSubmittedList().size();
			getJobsList().addAll(ReclusteringEngine.process(job, newId));
		}
		getJobsReceivedList().add(job);
		jobsSubmitted--;

		if (getJobsList().isEmpty() && jobsSubmitted == 0){
			if(initPopIndex != ICSA.popSize) {
				already = 1;
				//Processed a particle (the particle obtained by initialization)
				FogBroker.count++;
				fitnessICSA[initPopIndex] = caculatefitness();
				if (ICSA.worstFitness < fitnessICSA[initPopIndex]) ICSA.worstFitness = fitnessICSA[initPopIndex];
				if (fitnessICSA[initPopIndex] < ICSA.schedules.get(initPopIndex).getmFitness()){
					ICSA.schedules.get(initPopIndex).setMemory(ICSA.schedules.get(initPopIndex).getPosition());
					ICSA.schedules.get(initPopIndex).setmFitness(fitnessICSA[initPopIndex]);
				}
				initPopIndex++;
				init();
				if(initPopIndex == ICSA.popSize) {
					init();
					updateFlag = 1;
					updateFlag2 = 1;
				}
				sendNow(this.getId(), CloudSimTags.CLOUDLET_SUBMIT, null);
			}

			else if(indexToUpdate != ICSA.popSize) {
				//Processed all the particles obtained by initialization, and processed one particle (particles obtained after update)
				int gbestIndex = -1;
				for(int i = 0; i < index1; i++) {
					if(fitnessICSA[i] < ICSA.bestFitness) {
						ICSA.bestFitness = fitnessICSA[i];
						gbestIndex = i;
					}
				}
				if (gbestIndex >= 0) ICSA.bestSchedule = ICSA.schedules.get(gbestIndex).getPosition();//Update the global optimal scheduling plan

				FogBroker.count2++;
				fitnessICSA[indexToUpdate] = caculatefitness();
				if (ICSA.worstFitness < fitnessICSA[indexToUpdate]) ICSA.worstFitness = fitnessICSA[indexToUpdate];
				if (fitnessICSA[indexToUpdate] < ICSA.schedules.get(indexToUpdate).getmFitness()){
					ICSA.schedules.get(indexToUpdate).setMemory(ICSA.schedules.get(indexToUpdate).getPosition());
					ICSA.schedules.get(indexToUpdate).setmFitness(fitnessICSA[indexToUpdate]);
				}
				indexToUpdate++;

				if(FogBroker.count2 != ICSA.popSize && ICSA.iterations > iterateNum) {
					init();
					sendNow(this.getId(), CloudSimTags.CLOUDLET_SUBMIT, null);
				}
			}

			if(initPopIndex == ICSA.popSize && indexToUpdate == ICSA.popSize) {
				//Processed all the particles obtained after initialization and particles obtained after update
				if(ICSA.iterations > iterateNum) {
					for(int i = 0; i < ICSA.popSize; i++) {  //更新全局最优
						if(ICSA.schedules.get(i).getmFitness() < ICSA.bestFitness) {
							ICSA.bestFitness = ICSA.schedules.get(i).getmFitness();
							ICSA.bestSchedule = ICSA.schedules.get(i).getMemory();
						}
					}

					iterateNum++;
					//System.out.println("After "+iterateNum+" iterations:");
					//System.out.println("======gbest_fitness:========"+ICSA.bestFitness);
					gBestFitness.add(ICSA.bestFitness);
//	              	printindicators(PsoScheduling.gbest_fitness);

					if(ICSA.iterations != iterateNum) {
						indexToUpdate = 0;
						FogBroker.count2 = 0;
						getController().updateExecutionTime();
						init();
						updateFlag2 = 1;
						//ICSA.newSchedules.removeAll(PsoScheduling.newSchedules);
						sendNow(this.getId(), CloudSimTags.CLOUDLET_SUBMIT, null);
					}
				}
			}

			if(startlastSchedule == 1) {
				double f = caculatefitness();
				//System.out.println("The last result : "+f);
				for (int i = 0; i < getSchedulerIds().size(); i++) {
					sendNow(getSchedulerId(i), CloudSimTags.END_OF_SIMULATION, null);
				}
			}
			if(ICSA.iterations == iterateNum && startlastSchedule == 0) {
				

				for(int i = 0; i < ICSA.popSize; i++) {
					if(ICSA.schedules.get(i).getmFitness() == ICSA.bestFitness) {
						ICSA.bestSchedule = ICSA.schedules.get(i).getMemory();
					}
				}
				//Record the end time of the ICSA
				endTime = System.currentTimeMillis();
				algorithmTime = endTime - getScheduler(0).startTime;

				startlastSchedule = 1;
				caculatefitness();
				init();
				//sendNow(this.getSchedulerId(0), CloudSimTags.CLOUDLET_SUBMIT, submittedList);
				sendNow(this.getId(), CloudSimTags.CLOUDLET_SUBMIT, null);
			}
		}
		else{
			updateFlag2 = 0;
			sendNow(this.getId(), CloudSimTags.CLOUDLET_SUBMIT, null);
		}
	}
    
    public double caculatefitness(){
    	Controller controller = (Controller) CloudSim.getEntity(this.getcontrollerId());
    	controller.updateExecutionTime();
    	double energy = controller.TotalEnergy;  //单位J焦
    	double time = controller.TotalExecutionTime;
    	double cost = controller.TotalCost;
//    	System.out.println("controller.time = "+controller.TotalExecutionTime);
//    	System.out.println("------------------------------------------------------------------");
//    	System.out.println("clock = "+CloudSim.clock());
//    	System.out.println("------------------------------------------------------------------");
    	
    	if(time > DeadLine){
    		energy = 10*energy*(time/DeadLine);
    		cost = 10*cost*(time/DeadLine);
    	}
//    	maxTime = Double.max(maxTime,time);
//    	maxEnergy = Double.max(maxEnergy,energy);
//    	maxCost = Double.max(maxCost,cost);

    	//Double fit = ((time/maxTime) + (energy/maxEnergy) + (cost/maxCost))/((time/maxTime) * (energy/maxEnergy) * (cost/maxCost));
    	
    	double[] a ={time,energy,cost};
    	indicators.add(a);
    	//System.out.println("\t"+time+"\t"+energy+"\t"+cost);
    	switch (Parameters.getOptimization()) {
		case Time:
			//return time*0.2 + cost*0.2 + time*0.6;
			return (time - 279)/(475 - 279) + (energy - 9)/6 + (cost - 134)/(456 - 143);
		case Energy:
			return energy;
		case Cost:
			return cost;
		default:
			break;
		}
    	return 0;
    }
    
    private int printindicators(double gbest_fitness) {
    	@SuppressWarnings("resource")
		Formatter formatter = new Formatter(System.out);
//    	System.out.println("gbest_fitness:"+gbest_fitness);
    	for(double[] a:indicators){
    		switch (Parameters.getOptimization()) {
    		case Time:
//    			System.out.println(a[0]);
    			if(a[0]==gbest_fitness){
    				//System.out.println("\t"+a[0]+"\t"+a[1]+"\t"+a[2]);
    		    	formatter.format("%-20f\t%-20f\t%-20f",a[0],a[1],a[2]);
    		    	System.out.println();
    				return 1;
    			}
    			break;
    		case Energy:
    			if(a[1]==gbest_fitness){
    				//System.out.println("\t"+a[0]+"\t"+a[1]+"\t"+a[2]);
    				formatter.format("%-20f\t%-20f\t%-20f",a[0],a[1],a[2]);
    				System.out.println();
    				return 1;
    			}
    			break;
    		case Cost:
    			if(a[2]==gbest_fitness){
    				//System.out.println("\t"+a[0]+"\t"+a[1]+"\t"+a[2]);
    				formatter.format("%-20f\t%-20f\t%-20f",a[0],a[1],a[2]);
    				System.out.println();
    				return 1;
    			}
    			break;
    		}
    	}
    	indicators.clear();
    	return 0;
	}

    private void init() throws Exception {
		 for(Job job:jobList) {
			 getJobsList().add(job);
			 for(Task task : job.getTaskList()){
				 task.initlength();
				 Cloudlet cloudlet = (Cloudlet) task;
				 cloudlet.setCloudletLength(task.initlength());
				 //System.out.println("task#"+task.getCloudletId()+" length:"+task.getCloudletLength());
			 }
		 }
		 CloudSim.clock=0.1;
		 for (int i = 0; i < getSchedulerIds().size(); i++) {
             sendNow(getSchedulerId(i), CloudSimTags.CLEAR, null);
         }
		 for(int i=0;i<getJobsReceivedList().size();i++) {
			 Job job=(Job)getJobsReceivedList().get(i);
			 job.setCloudletStatus(Cloudlet.CREATED);
			 job.setTaskFinishTime(-1.0);
			 job.setExecStartTime(0);
			 job.setVmId(-1);
			 job.setInputsize(0);
			 job.setOutputsize(0);
		 }
		
		 getController().clear();
		 
		 for(Vm vm:getAllVmList()) {
			 vm.getCloudletScheduler().clear();
			// vm.updateVmProcessing(CloudSim.clock(), Datacenter.vmAllocationPolicy.getHost(vm).getVmScheduler()
						//.getAllocatedMipsForVm(vm));
		 }
		 getJobsReceivedList().removeAll(getJobsReceivedList());
//		 if(getJobsReceivedList().isEmpty())
//			 System.out.println("jobsReceivedList已清空");
	}
    
    protected void processJobReturnForGA(SimEvent ev) throws Exception {
    	Job job = (Job) ev.getData();
        if (job.getCloudletStatus() == Cloudlet.FAILED) {
            // Reclusteringengine will add retry job to jobList
            int newId = getJobsList().size() + getJobsSubmittedList().size();
            getJobsList().addAll(ReclusteringEngine.process(job, newId));
        }
        getJobsReceivedList().add(job);
        jobsSubmitted--;
        if(getJobsList().isEmpty() && jobsSubmitted == 0) {
    		if(findBestSchedule == 1) {
    			//按照最优调度方案去执行的结果，然后结束本次仿真
    			getController().updateExecutionTime();
    			sendNow(getSchedulerId(0), CloudSimTags.END_OF_SIMULATION, null);
    		}
    		if(initIndexForGA != GASchedulingAlgorithm.popsize) {
    			//处理完了一个粒子（初始化得到的粒子 ）
    			already = 1;
             	FogBroker.initIndexForGA++;
             	fitnessForGA[initIndexForGA++] = caculatefitness();
             	init();
                if(initIndexForGA == GASchedulingAlgorithm.popsize) {
                	gaFlag = 1;
                	gaFlag2 = 0;
                	double totalFitness = 0;
                	double bestFitness = Double.MAX_VALUE;
                	int bestIndex = 0;
                	for(int i = 0; i < fitnessForGA.length; i++) {
                		totalFitness += fitnessForGA[i];
                		if(bestFitness > fitnessForGA[i]) {
          					 bestFitness = fitnessForGA[i];
          					 bestIndex = i;
          				}
                	}
                	GASchedulingAlgorithm.bestParent = GASchedulingAlgorithm.schedules.get(bestIndex);//父代中最好的染色体
          			GASchedulingAlgorithm.bestParentFitness = bestFitness;//父代中最好的适应度值
          			GASchedulingAlgorithm.gbestSchedule = GASchedulingAlgorithm.bestParent;
//          			System.out.println("=======================================================第"+iterateNum+"次迭代前的最优适应度值bestFitness为："+bestFitness);
      				updatebest.add(bestFitness);
          			for(int i = 0; i < fitnessForGA.length; i++)
       					GASchedulingAlgorithm.probs.put(i, fitnessForGA[i] / totalFitness);//每个染色体被选中的概率
          			GASchedulingAlgorithm.getSegments();
          			init();
             	}
             	sendNow(this.getId(), CloudSimTags.CLOUDLET_SUBMIT, null); 
            }
    		else if(tempChildrenIndex != 4 && findBestSchedule == 0){
    			gaFlag2 = 1;
    			fitnessForGATempChildren[tempChildrenIndex++] = caculatefitness();
    			FogBroker.tempChildrenIndex++;
    			if(tempChildrenIndex == 4) {
    				gaFlag2 = 0;
    				tempChildrenIndex = 0;
    				FogBroker.tempChildrenIndex = 0;
    				if(fitnessForGATempChildren[0] < fitnessForGATempChildren[1]) {
    					GASchedulingAlgorithm.fitness.add(fitnessForGATempChildren[0]);
    					GASchedulingAlgorithm.children.add(GASchedulingAlgorithm.tempChildren.get(0));
    				}else {
    					GASchedulingAlgorithm.fitness.add(fitnessForGATempChildren[1]);
    					GASchedulingAlgorithm.children.add(GASchedulingAlgorithm.tempChildren.get(1));
    				}
    				if(fitnessForGATempChildren[2] < fitnessForGATempChildren[3]) {
    					GASchedulingAlgorithm.fitness.add(fitnessForGATempChildren[2]);
    					GASchedulingAlgorithm.children.add(GASchedulingAlgorithm.tempChildren.get(2));
    				}else {
    					GASchedulingAlgorithm.fitness.add(fitnessForGATempChildren[3]);
    					GASchedulingAlgorithm.children.add(GASchedulingAlgorithm.tempChildren.get(3));
    				}
    				GASchedulingAlgorithm.tempChildren.removeAll(GASchedulingAlgorithm.tempChildren);
    			}
    			if(GASchedulingAlgorithm.children.size() != GASchedulingAlgorithm.popsize) {
    				init();
    				sendNow(this.getId(), CloudSimTags.CLOUDLET_SUBMIT, null); 
          		}
    			else if(findBestSchedule == 0){
    				//完成了一次进化
    				if(GASchedulingAlgorithm.gmax != iterateNum) {
    					double bestFitness = Double.MAX_VALUE;//子代中最好的适应度值
      					double worstFitness = 0;//子代中最坏的适应度值
      					int worstIndex = 0;
      					for(int i = 0; i < GASchedulingAlgorithm.fitness.size(); i++) {
      						if(bestFitness > GASchedulingAlgorithm.fitness.get(i))
      							bestFitness = GASchedulingAlgorithm.fitness.get(i);
      						if(worstFitness < GASchedulingAlgorithm.fitness.get(i)) {
    							worstFitness = GASchedulingAlgorithm.fitness.get(i);
    							worstIndex = i;
    						}
    					}
     					iterateNum++;
     					GASchedulingAlgorithm.fitness.removeAll(GASchedulingAlgorithm.fitness);
     					GASchedulingAlgorithm.schedules.removeAll(GASchedulingAlgorithm.schedules);
     					GASchedulingAlgorithm.probSegments = new HashMap<Integer,double[]>();
     					GASchedulingAlgorithm.probs = new HashMap<Integer,Double>();
     					GASchedulingAlgorithm.tempParents.removeAll(GASchedulingAlgorithm.tempParents);
     					for(int i = 0; i < GASchedulingAlgorithm.children.size(); i++) {
     						if(i == worstIndex)
     							//用父代中最好的染色体替换掉子代中最差的染色体
     							GASchedulingAlgorithm.schedules.add(GASchedulingAlgorithm.bestParent);
     						else
     							GASchedulingAlgorithm.schedules.add(GASchedulingAlgorithm.children.get(i));
     					}
      					GASchedulingAlgorithm.children.removeAll(GASchedulingAlgorithm.children);
      					/*
      					 * 重置参数，以便执行下一次迭代
      					 * （进化前，需要计算每个染色体被选中的概率，故设置了这些参数）
      					 */
      					initIndexForGA = 0;
      					FogBroker.initIndexForGA = 0;
      					tempChildrenIndex = 0;
      					FogBroker.tempChildrenIndex = 0;
      					gaFlag = 0;
      					gaFlag2 = 0;
      					init();
      					sendNow(this.getId(), CloudSimTags.CLOUDLET_SUBMIT, null); 
      				}else {
      					//记录ga结束时间
      					endTime = System.currentTimeMillis();
      					algorithmTime = endTime - getScheduler(0).startTime;
//      					System.out.println("*************************************************************************按照最优调度方案执行");
      					findBestSchedule = 1;//去按照最优的调度方案执行
      					init();
      					sendNow(this.getId(), CloudSimTags.CLOUDLET_SUBMIT, null); 
      				}
    			}
    		 }
    	}else {
    		gaFlag2 = 1;
    		sendNow(this.getId(), CloudSimTags.CLOUDLET_SUBMIT, null);
    	}
    }

    /**
     * Overrides this method when making a new and different type of Broker.
     * This method is called by {@link #body()} for incoming unknown tags.
     *
     * @param ev a SimEvent object
     */
    protected void processOtherEvent(SimEvent ev) {
        if (ev == null) {
            Log.printLine(getName() + ".processOtherEvent(): " + "Error - an event is null.");
            return;
        }
        Log.printLine(getName() + ".processOtherEvent(): "
                + "Error - event unknown by this DatacenterBroker.");
    }

    /**
     * Checks whether a job list contains a id
     *
     * @param jobList the job list
     * @param id the job id
     * @return
     */
    private boolean hasJobListContainsID(List jobList, int id) {
        for (Iterator it = jobList.iterator(); it.hasNext();) {
            Job job = (Job) it.next();
            if (job.getCloudletId() == id) {
                return true;
            }
        }
        return false;
    }

    /**
     * Submit jobs to the created VMs.
     *
     * @pre $none
     * @post $none
     */
    protected void submitJobs() {
        List<Job> list = getJobsList();
        Map<Integer, List> allocationList = new HashMap<>();
        for (int i = 0; i < getSchedulers().size(); i++) {
            List<Job> submittedList = new ArrayList<>();
            allocationList.put(getSchedulerId(i), submittedList);
        }
        int num = list.size();
        for (int i = 0; i < num; i++) {
            //at the beginning
            Job job = list.get(i);
            //Dont use job.isFinished() it is not right

            if (!hasJobListContainsID(this.getJobsReceivedList(), job.getCloudletId())) {
            	//如果这个工作尚未提交，就检查它的父任务是否全部都已经提交
                List<Job> parentList = job.getParentList();
                boolean flag = true;
                for (Job parent : parentList) {
                    if (!hasJobListContainsID(this.getJobsReceivedList(), parent.getCloudletId())) {
                    	flag = false;
                        break;
                    }
                }
                /**
                 * This job's parents have all completed successfully. Should
                 * submit.如果这个未提交的任务的父任务都已经提交，那么就提交这个任务
                 */
                if (flag) {
                	
                    List submittedList = allocationList.get(job.getUserId());
                    submittedList.add(job);
                    jobsSubmitted++;
                    getJobsSubmittedList().add(job);
                    list.remove(job);
                    i--;
                    num--;
                }
            }

        }
        /**
         * If we have multiple schedulers. Divide them equally.
         */
        for (int i = 0; i < getSchedulers().size(); i++) {

            List submittedList = allocationList.get(getSchedulerId(i));
            //divide it into sublist

            int interval = Parameters.getOverheadParams().getWEDInterval();
            double delay = 0.0;
            if(Parameters.getOverheadParams().getWEDDelay()!=null){
                delay = Parameters.getOverheadParams().getWEDDelay(submittedList);
            }
            double delaybase = delay;
            int size = submittedList.size();
            if (interval > 0 && interval <= size) {
                int index = 0;
                List subList = new ArrayList();
                while (index < size) {
                    subList.add(submittedList.get(index));
                    index++;
                    if (index % interval == 0) {
                        //create a new one
                        schedule(getSchedulerId(i), delay, CloudSimTags.CLOUDLET_SUBMIT, subList);
                        delay += delaybase;
                        subList = new ArrayList();
                    }
                }
                if (!subList.isEmpty()) {
                    schedule(getSchedulerId(i), delay, CloudSimTags.CLOUDLET_SUBMIT, subList);
                }
            } else if (!submittedList.isEmpty()) {
                sendNow(this.getSchedulerId(i), CloudSimTags.CLOUDLET_SUBMIT, submittedList);
            }
        }
    }
   
    /*
     * (non-Javadoc)
     * @see cloudsim.core.SimEntity#shutdownEntity()
     */
    @Override
    public void shutdownEntity() {
        Log.printLine(getName() + " is shutting down...");
    }

    /*
     * (non-Javadoc)
     * @see cloudsim.core.SimEntity#startEntity()
     * Here we creata a message when it is started
     */
    @Override
    public void startEntity() {
        Log.printLine(getName() + " is starting...");
        schedule(getId(), 0, CloudSimTags.RESOURCE_CHARACTERISTICS_REQUEST);
    }

    /**
     * Gets the job list.
     *
     * @param <T> the generic type
     * @return the job list
     */
    @SuppressWarnings("unchecked")
    public <T extends Cloudlet> List<T> getJobsList() {
        return (List<T>) jobsList;
    }

    /**
     * Sets the job list.
     *
     * @param <T> the generic type
     * @param cloudletList the new job list
     */
    private <T extends Cloudlet> void setJobsList(List<T> jobsList) {
        this.jobsList = jobsList;
    }

    /**
     * Gets the job submitted list.
     *
     * @param <T> the generic type
     * @return the job submitted list
     */
    @SuppressWarnings("unchecked")
    public <T extends Cloudlet> List<T> getJobsSubmittedList() {
        return (List<T>) jobsSubmittedList;
    }

    /**
     * Sets the job submitted list.
     *
     * @param <T> the generic type
     * @param jobsSubmittedList the new job submitted list
     */
    private <T extends Cloudlet> void setJobsSubmittedList(List<T> jobsSubmittedList) {
        this.jobsSubmittedList = jobsSubmittedList;
    }

    /**
     * Gets the job received list.
     *
     * @param <T> the generic type
     * @return the job received list
     */
    @SuppressWarnings("unchecked")
    public <T extends Cloudlet> List<T> getJobsReceivedList() {
        return (List<T>) jobsReceivedList;
    }

    /**
     * Sets the job received list.
     *
     * @param <T> the generic type
     * @param cloudletReceivedList the new job received list
     */
    private <T extends Cloudlet> void setJobsReceivedList(List<T> jobsReceivedList) {
        this.jobsReceivedList = jobsReceivedList;
    }

    /**
     * Gets the vm list.
     *
     * @param <T> the generic type
     * @return the vm list
     */
    @SuppressWarnings("unchecked")
    public <T extends Vm> List<T> getVmList() {
        return (List<T>) vmList;
    }

    /**
     * Sets the vm list.
     *
     * @param <T> the generic type
     * @param vmList the new vm list
     */
    private <T extends Vm> void setVmList(List<T> vmList) {
        this.vmList = vmList;
    }

    /**
     * Gets the schedulers.
     *
     * @return the schedulers
     */
    public List<FogBroker> getSchedulers() {
        return this.scheduler;
    }

    /**
     * Sets the scheduler list.
     *
     * @param <T> the generic type
     * @param vmList the new scheduler list
     */
    private void setSchedulers(List list) {
        this.scheduler = list;
    }
    

    /**
     * Gets the scheduler id.
     *
     * @return the scheduler id
     */
    public List<Integer> getSchedulerIds() {
        return this.schedulerId;
    }

    /**
     * Sets the scheduler id list.
     *
     * @param <T> the generic type
     * @param vmList the new scheduler id list
     */
    private void setSchedulerIds(List list) {
        this.schedulerId = list;
    }

    /**
     * Gets the scheduler id list.
     *
     * @param index
     * @return the scheduler id list
     */
    public int getSchedulerId(int index) {
        if (this.schedulerId != null) {
            return this.schedulerId.get(index);
        }
        return 0;
    }

    /**
     * Gets the scheduler .
     *
     * @param schedulerId
     * @return the scheduler
     */
    public FogBroker getScheduler(int schedulerId) {
        if (this.scheduler != null) {
            return this.scheduler.get(schedulerId);
        }
        return null;
    }
    
    public int getcontrollerId() {
        return this.controllerId;
    }
    public void setcontrollerId(int controllerId) {
        this.controllerId = controllerId;
    }
    public void setDeadLine(double DeadLine) {
        this.DeadLine = DeadLine;
    }
    public OffloadingEngine getoffloadingEngine(){
    	return offloadingEngine;
    }
    public Controller getController(){
    	Controller controller = (Controller) CloudSim.getEntity(this.getcontrollerId());
    	return controller;
    }
    public double getAverageOffloadingTime(){
    	double sum = 0.0;
    	for(long time : offloadingTimes)
    		sum += time;
    	return sum / offloadingTimes.size();
    }

    public static void clearFlag() {
    	jobList.removeAll(jobList);
    	offloadingTimes.clear();
    	startlastSchedule=0;
    	updateFlag=0;
    	startlastSchedule=0;
    	FogBroker.count=0;
    	FogBroker.count2=0;
    	FogBroker.initIndexForGA=0;
    	FogBroker.tempChildrenIndex=0;
    	
    	index1=0;
        indexForUpdate=0;
        updateFlag2=0;
        
        already=0;
        initIndexForGA=0;
        gaFlag=0;
        gaFlag2=0;
        findBestSchedule=0;

        initPopIndex = 0;
        indexToUpdate = 0;

        maxCost = 0.0;
        maxEnergy = 0.0;
        maxTime = 0.0;

        initGOAIndex = 0;
        updateGOAIndex = 0;

        initMSIndex = 0;
        updateMSIndex = 0;

        initGWOIndex = 0;
        updateGWOIndex = 0;

        initMomentumSAIndex = 0;
        updateMomentumSAIndex = 0;

        GASchedulingAlgorithm.clear();
        PsoScheduling.clear();
        ICSA.clear();
        GOA.clear();
        MSAlgorithm.clear();
        GWO.clear();
        MomentumSA.clear();
    }

}
