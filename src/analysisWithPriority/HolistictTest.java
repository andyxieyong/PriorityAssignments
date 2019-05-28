package analysisWithPriority;

import java.util.ArrayList;

import entity.Resource;
import entity.SporadicTask;
import generatorTools.PriorityGeneator;
import utils.AnalysisUtils;

public class HolistictTest {

	boolean useCorrection = false;
	int extendCal = 5;

	public boolean getResponseTimeSPO(ArrayList<ArrayList<SporadicTask>> tasks, ArrayList<Resource> resources, boolean isMSRP) {
		if (tasks == null)
			return false;

		// assign priorities by Deadline Monotonic
		tasks = new PriorityGeneator().assignPrioritiesByDM(tasks);

		long[][] dummy_response_time = new long[tasks.size()][];
		for (int i = 0; i < dummy_response_time.length; i++) {
			dummy_response_time[i] = new long[tasks.get(i).size()];
			for (int j = 0; j < tasks.get(i).size(); j++) {
				dummy_response_time[i][j] = tasks.get(i).get(j).deadline;
			}
		}

		// now we check each task in each processor
		for (int i = 0; i < tasks.size(); i++) {
			int partition = i;
			ArrayList<SporadicTask> unassignedTasks = new ArrayList<>(tasks.get(partition));
			int sratingP = 500 - unassignedTasks.size() * 2;
			int prioLevels = tasks.get(partition).size();

			// For each priority level
			for (int currentLevel = 0; currentLevel < prioLevels; currentLevel++) {
				int startingIndex = unassignedTasks.size() - 1;

				for (int j = startingIndex; j >= 0; j--) {
					SporadicTask task = unassignedTasks.get(j);
					int originalP = task.priority;
					task.priority = sratingP;

					tasks.get(partition).sort((t1, t2) -> -Integer.compare(t1.priority, t2.priority));

					long timeBTB = getResponseTimeForSBPO(partition, tasks, resources, dummy_response_time, true, true, isMSRP, task, extendCal);
					// long time = getResponseTimeForOnePartition(partition,
					// tasks, resources, dummy_response_time, false, false,
					// isMSRP, task,)[0];

					task.priority = originalP;
					tasks.get(partition).sort((t1, t2) -> -Integer.compare(t1.priority, t2.priority));

					task.addition_slack_BTB = task.deadline - timeBTB;
					// task.addition_slack = task.deadline - time;
				}

				unassignedTasks.sort((t1, t2) -> -new AnalysisUtils().compareSlack(t1, t2, true));

				// if (unassignedTasks.get(0).addition_slack_BTB < 0) {
				//
				// unassignedTasks.sort((t1, t2) -> -new
				// AnalysisUtils().compareSlack(t1, t2, false));
				// if (unassignedTasks.get(0).addition_slack < 0) {
				// return false;
				// }
				// }

				unassignedTasks.get(0).priority = sratingP;
				tasks.get(partition).sort((t1, t2) -> -Integer.compare(t1.priority, t2.priority));
				unassignedTasks.remove(0);

				sratingP += 2;
			}

			tasks.get(partition).sort((t1, t2) -> -Integer.compare(t1.priority, t2.priority));
			dummy_response_time[partition] = getResponseTimeForOnePartition(partition, tasks, resources, dummy_response_time, true, true, isMSRP, 1);
		}

		boolean isEqual = false, missdeadline = false;
		long[][] response_time = new AnalysisUtils().initResponseTime(tasks);

		/* a huge busy window to get a fixed Ri */
		while (!isEqual) {
			isEqual = true;
			long[][] response_time_plus = busyWindow(tasks, resources, response_time, true, true, isMSRP);

			for (int i = 0; i < response_time_plus.length; i++) {
				for (int j = 0; j < response_time_plus[i].length; j++) {
					if (response_time[i][j] != response_time_plus[i][j])
						isEqual = false;
					if (response_time_plus[i][j] > tasks.get(i).get(j).deadline)
						missdeadline = true;
				}
			}

			new AnalysisUtils().cloneList(response_time_plus, response_time);

			if (missdeadline)
				break;
		}

		if (new AnalysisUtils().isSystemSchedulable(tasks, response_time))
			return true;
		else
			return false;
	}

	public boolean getResponseTimeExhaustive(ArrayList<ArrayList<SporadicTask>> tasks, ArrayList<Resource> resources, boolean isMSRP, boolean printDebug) {
		if (tasks == null)
			return false;

		ArrayList<ArrayList<ArrayList<SporadicTask>>> permutation = new ArrayList<>();
		for (int i = 0; i < tasks.size(); i++) {
			ArrayList<ArrayList<SporadicTask>> permutePartition = new AnalysisUtils().permutePartition(tasks.get(i));
			permutation.add(permutePartition);
		}

		ArrayList<Integer> lastIndexes = new ArrayList<>();
		for (int i = 0; i < tasks.size(); i++) {
			lastIndexes.add(0);
		}
		ArrayList<ArrayList<Integer>> combinations = new ArrayList<>();

		while (!isFinish(tasks, lastIndexes)) {
			ArrayList<Integer> indexes = new ArrayList<>(lastIndexes);
			combinations.add(indexes);

			int nextAdd = tasks.size();
			for (int i = lastIndexes.size() - 1; i >= 0; i--) {
				if (lastIndexes.get(i) != tasks.get(i).size() - 1) {
					nextAdd = i;
					break;
				}
			}

			if (nextAdd < tasks.size()) {
				lastIndexes.set(nextAdd, lastIndexes.get(nextAdd) + 1);
				for (int i = nextAdd + 1; i < tasks.size(); i++) {
					lastIndexes.set(i, 0);
				}
			}
		}
		combinations.add(lastIndexes);

		ArrayList<ArrayList<ArrayList<SporadicTask>>> possible = new ArrayList<>();

		for (int i = 0; i < combinations.size(); i++) {
			ArrayList<ArrayList<SporadicTask>> oneTasks = new ArrayList<>();
			for (int j = 0; j < combinations.get(i).size(); j++) {
				oneTasks.add(permutation.get(j).get(combinations.get(i).get(j)));
			}
			possible.add(oneTasks);
		}

		for (int index = 0; index < possible.size(); index++) {
			ArrayList<ArrayList<SporadicTask>> one = possible.get(index);

			for (int j = 0; j < one.size(); j++) {
				int initPrio = 1000;
				for (int k = 0; k < one.get(j).size(); k++) {
					one.get(j).get(k).priority = initPrio;
					initPrio -= 2;
				}
			}

			boolean isEqual = false, missdeadline = false;
			long[][] response_time = new AnalysisUtils().initResponseTime(one);

			/* a huge busy window to get a fixed Ri */
			while (!isEqual) {
				isEqual = true;
				long[][] response_time_plus = busyWindow(one, resources, response_time, true, true, isMSRP);

				for (int i = 0; i < response_time_plus.length; i++) {
					for (int j = 0; j < response_time_plus[i].length; j++) {
						if (response_time[i][j] != response_time_plus[i][j])
							isEqual = false;
						if (response_time_plus[i][j] > one.get(i).get(j).deadline)
							missdeadline = true;

					}
				}

				new AnalysisUtils().cloneList(response_time_plus, response_time);

				if (missdeadline)
					break;
			}

			if (new AnalysisUtils().isSystemSchedulable(one, response_time))
				return true;
		}

		return false;
	}

	private boolean isFinish(ArrayList<ArrayList<SporadicTask>> tasks, ArrayList<Integer> indexes) {
		for (int i = 0; i < indexes.size(); i++) {
			if (indexes.get(i) != tasks.get(i).size() - 1)
				return false;
		}

		return true;
	}

	public boolean getResponseTimeDM(ArrayList<ArrayList<SporadicTask>> tasks, ArrayList<Resource> resources, boolean isMSRP) {
		if (tasks == null)
			return false;

		// assign priorities by Deadline Monotonic
		tasks = new PriorityGeneator().assignPrioritiesByDM(tasks);

		boolean isEqual = false, missdeadline = false;
		long[][] response_time = new AnalysisUtils().initResponseTime(tasks);

		/* a huge busy window to get a fixed Ri */
		while (!isEqual) {
			isEqual = true;
			long[][] response_time_plus = busyWindow(tasks, resources, response_time, true, true, isMSRP);

			for (int i = 0; i < response_time_plus.length; i++) {
				for (int j = 0; j < response_time_plus[i].length; j++) {
					if (response_time[i][j] != response_time_plus[i][j])
						isEqual = false;
					if (response_time_plus[i][j] > tasks.get(i).get(j).deadline)
						missdeadline = true;

				}
			}

			new AnalysisUtils().cloneList(response_time_plus, response_time);

			if (missdeadline)
				break;
		}

		if (new AnalysisUtils().isSystemSchedulable(tasks, response_time))
			return true;
		else
			return false;
	}

	public boolean getResponseTimeRPA(ArrayList<ArrayList<SporadicTask>> tasks, ArrayList<Resource> resources, boolean isMSRP) {
		if (tasks == null)
			return false;

		// assign priorities by Deadline Monotonic
		tasks = new PriorityGeneator().assignPrioritiesByDM(tasks);

		long[][] dummy_response_time = new long[tasks.size()][];
		for (int i = 0; i < dummy_response_time.length; i++) {
			dummy_response_time[i] = new long[tasks.get(i).size()];
		}

		// now we check each task. we begin from the task with largest deadline
		for (int i = 0; i < tasks.size(); i++) {

			ArrayList<SporadicTask> unassignedTasks = new ArrayList<>(tasks.get(i));
			int sratingP = 500 - unassignedTasks.size() * 2;
			int prioLevels = tasks.get(i).size();

			for (int currentLevel = 0; currentLevel < prioLevels; currentLevel++) {
				int startingIndex = unassignedTasks.size() - 1;

				for (int j = startingIndex; j >= 0; j--) {
					SporadicTask task = unassignedTasks.get(j);
					int originalP = task.priority;
					task.priority = sratingP;

					tasks.get(i).sort((t1, t2) -> -Integer.compare(t1.priority, t2.priority));
					long time = getResponseTimeForOneTask(task, tasks, resources, dummy_response_time, true, false, isMSRP);
					task.priority = originalP;
					tasks.get(i).sort((t1, t2) -> -Integer.compare(t1.priority, t2.priority));

					task.addition_slack_BTB = task.deadline - time;
				}
				unassignedTasks.sort((t1, t2) -> -new AnalysisUtils().compareSlack(t1, t2, true));
				if (unassignedTasks.get(0).addition_slack_BTB < 0)
					return false;

				unassignedTasks.get(0).priority = sratingP;
				unassignedTasks.remove(0);
				tasks.get(i).sort((t1, t2) -> -Integer.compare(t1.priority, t2.priority));

				sratingP += 2;
			}
		}

		for (int i = 0; i < tasks.size(); i++) {
			tasks.get(i).sort((t1, t2) -> -Integer.compare(t1.priority, t2.priority));
		}

		return true;
	}

	public boolean getResponseTimeOPA(ArrayList<ArrayList<SporadicTask>> tasks, ArrayList<Resource> resources, boolean isMSRP) {
		if (tasks == null)
			return false;

		// assign priorities by Deadline Monotonic
		tasks = new PriorityGeneator().assignPrioritiesByDM(tasks);

		long[][] dummy_response_time = new long[tasks.size()][];
		for (int i = 0; i < dummy_response_time.length; i++) {
			dummy_response_time[i] = new long[tasks.get(i).size()];
		}

		// now we check each task. we begin from the task with largest deadline
		for (int i = 0; i < tasks.size(); i++) {

			ArrayList<SporadicTask> unassignedTasks = new ArrayList<>(tasks.get(i));
			int sratingP = 500 - unassignedTasks.size() * 2;
			int prioLevels = tasks.get(i).size();

			for (int currentLevel = 0; currentLevel < prioLevels; currentLevel++) {
				boolean isTaskSchedulable = false;
				int startingIndex = unassignedTasks.size() - 1;

				for (int j = startingIndex; j >= 0; j--) {
					SporadicTask task = unassignedTasks.get(j);
					int originalP = task.priority;
					task.priority = sratingP;

					tasks.get(i).sort((t1, t2) -> -Integer.compare(t1.priority, t2.priority));
					long time = getResponseTimeForOneTask(task, tasks, resources, dummy_response_time, true, false, isMSRP);
					boolean isSchedulable = time <= task.deadline;

					if (!isSchedulable) {
						task.priority = originalP;
						tasks.get(i).sort((t1, t2) -> -Integer.compare(t1.priority, t2.priority));
					} else {
						unassignedTasks.remove(task);
						isTaskSchedulable = true;
						break;
					}
				}

				if (!isTaskSchedulable) {
					return false;
				}
				sratingP += 2;
			}
		}

		for (int i = 0; i < tasks.size(); i++) {
			tasks.get(i).sort((t1, t2) -> -Integer.compare(t1.priority, t2.priority));
		}

		return true;
	}

	private long getResponseTimeForOneTask(SporadicTask caltask, ArrayList<ArrayList<SporadicTask>> tasks, ArrayList<Resource> resources,
			long[][] response_time, boolean useBTB, boolean useRi, boolean isMSRP) {
		SporadicTask task = caltask;
		long Ri = 0;
		long newRi = task.WCET + task.pure_resource_execution_time;

		while (Ri != newRi) {
			if (newRi > task.deadline)
				return newRi;

			Ri = newRi;
			task.spin = spinDelay(task, tasks, resources, response_time, newRi, useBTB, useRi);
			task.interference = highPriorityInterference(task, tasks, newRi);
			task.local = localBlocking(task, tasks, resources, response_time, newRi, useBTB, useRi, isMSRP);
			newRi = task.Ri = task.WCET + task.spin + task.interference + task.local;

			if (newRi > task.deadline) {
				return newRi;
			}
		}

		return newRi;
	}

	private long getResponseTimeForSBPO(int partition, ArrayList<ArrayList<SporadicTask>> tasks, ArrayList<Resource> resources, long[][] response_time,
			boolean btbHit, boolean useRi, boolean isMSRP, SporadicTask calTask, int extendCal) {

		long[][] response_time_new = new long[tasks.size()][];
		for (int i = 0; i < tasks.size(); i++) {
			response_time_new[i] = new long[tasks.get(i).size()];
		}
		new AnalysisUtils().cloneList(response_time, response_time_new);

		long[][] response_time_plus = new long[tasks.size()][];
		for (int i = 0; i < tasks.size(); i++) {
			response_time_plus[i] = new long[tasks.get(i).size()];
		}
		new AnalysisUtils().cloneList(response_time, response_time_plus);

		for (int i = 0; i < response_time_new[partition].length; i++) {
			response_time_plus[partition][i] = tasks.get(partition).get(i).WCET + tasks.get(partition).get(i).pure_resource_execution_time;
		}

		boolean isEqual = false;
		boolean shouldFinish = false;

		while (!isEqual && !shouldFinish) {
			isEqual = true;

			for (int i = 0; i < response_time_plus[partition].length; i++) {
				response_time_new[partition][i] = response_time_plus[partition][i];
			}

			for (int j = 0; j < tasks.get(partition).size(); j++) {
				SporadicTask task = tasks.get(partition).get(j);
				if (response_time_plus[partition][j] >= task.deadline * extendCal && task != calTask) {
					response_time_plus[partition][j] = task.deadline * extendCal;
					continue;
				}

				task.spin = spinDelay(task, tasks, resources, response_time_plus, response_time_plus[partition][j], btbHit, useRi);
				task.interference = highPriorityInterference(task, tasks, response_time_plus[partition][j]);
				task.local = localBlocking(task, tasks, resources, response_time_plus, response_time_plus[partition][j], btbHit, useRi, isMSRP);
				task.Ri = task.WCET + task.spin + task.interference + task.local;
				response_time_plus[partition][j] = task.Ri > task.deadline * extendCal ? task.deadline * extendCal : task.Ri;
			}

			for (int i = 0; i < response_time_plus[partition].length; i++) {
				if (response_time_plus[partition][i] != response_time_new[partition][i])
					isEqual = false;
				if (tasks.get(partition).get(i).Ri < tasks.get(partition).get(i).deadline * extendCal)
					shouldFinish = false;
			}

		}

		return calTask.Ri;
	}

	private long[] getResponseTimeForOnePartition(int partition, ArrayList<ArrayList<SporadicTask>> tasks, ArrayList<Resource> resources,
			long[][] response_time, boolean btbHit, boolean useRi, boolean isMSRP, int extendCal) {

		boolean isEqual = false;

		long[][] response_time_new = new long[tasks.size()][];
		for (int i = 0; i < tasks.size(); i++) {
			response_time_new[i] = new long[tasks.get(i).size()];
		}
		new AnalysisUtils().cloneList(response_time, response_time_new);

		long[][] response_time_plus = new long[tasks.size()][];
		for (int i = 0; i < tasks.size(); i++) {
			response_time_plus[i] = new long[tasks.get(i).size()];
		}
		new AnalysisUtils().cloneList(response_time, response_time_plus);

		for (int i = 0; i < response_time_new[partition].length; i++) {
			response_time_plus[partition][i] = tasks.get(partition).get(i).WCET + tasks.get(partition).get(i).pure_resource_execution_time;
		}

		while (!isEqual) {
			isEqual = true;

			for (int i = 0; i < response_time_plus[partition].length; i++) {
				response_time_new[partition][i] = response_time_plus[partition][i];
			}

			for (int j = 0; j < tasks.get(partition).size(); j++) {
				SporadicTask task = tasks.get(partition).get(j);
				if (response_time_plus[partition][j] >= task.deadline * extendCal) {
					response_time_plus[partition][j] = task.deadline;
					continue;
				}

				task.spin = spinDelay(task, tasks, resources, response_time_plus, response_time_plus[partition][j], btbHit, useRi);
				task.interference = highPriorityInterference(task, tasks, response_time_plus[partition][j]);
				task.local = localBlocking(task, tasks, resources, response_time_plus, response_time_plus[partition][j], btbHit, useRi, isMSRP);
				response_time_plus[partition][j] = task.Ri = task.WCET + task.spin + task.interference + task.local;
			}

			for (int i = 0; i < response_time_plus[partition].length; i++) {
				if (response_time_new[partition][i] != response_time_plus[partition][i])
					isEqual = false;
			}

		}

		return response_time_plus[partition];
	}

	private long[][] busyWindow(ArrayList<ArrayList<SporadicTask>> tasks, ArrayList<Resource> resources, long[][] response_time, boolean btbHit, boolean useRi,
			boolean isMSRP) {
		long[][] response_time_plus = new long[tasks.size()][];

		for (int i = 0; i < response_time.length; i++) {
			response_time_plus[i] = new long[response_time[i].length];
		}

		for (int i = 0; i < tasks.size(); i++) {
			for (int j = 0; j < tasks.get(i).size(); j++) {
				SporadicTask task = tasks.get(i).get(j);
				if (response_time[i][j] > task.deadline) {
					response_time_plus[i][j] = response_time[i][j];
					continue;
				}

				task.spin = spinDelay(task, tasks, resources, response_time, response_time[i][j], btbHit, useRi);
				task.interference = highPriorityInterference(task, tasks, response_time[i][j]);
				task.local = localBlocking(task, tasks, resources, response_time, response_time[i][j], btbHit, useRi, isMSRP);
				response_time_plus[i][j] = task.Ri = task.WCET + task.spin + task.interference + task.local;

				if (task.Ri > task.deadline) {
					return response_time_plus;
				}
			}
		}
		return response_time_plus;
	}

	/*
	 * Calculate the spin delay for a given task t.
	 */
	private long spinDelay(SporadicTask t, ArrayList<ArrayList<SporadicTask>> tasks, ArrayList<Resource> resources, long[][] Ris, long Ri, boolean btbHit,
			boolean useRi) {
		long spin_delay = 0;

		for (int k = 0; k < resources.size(); k++) {
			Resource resource = resources.get(k);
			int Rindex = getIndexRInTask(t, resource);
			int noqT = Rindex < 0 ? 0 : t.number_of_access_in_one_release.get(Rindex);
			int noqHT = getNoRFromHP(t, resource, tasks.get(t.partition), Ris[t.partition], btbHit, useRi, Ri);
			long spin = noqT + noqHT;

			for (int j = 0; j < tasks.size(); j++) {
				if (j != t.partition) {
					int noqRemote = getNoRRemote(resource, tasks.get(j), Ris[j], Ri, btbHit, useRi);
					spin += Math.min(noqT + noqHT, noqRemote);
				}
			}
			spin_delay += spin * resource.csl;
		}

		return spin_delay;
	}

	/*
	 * Calculate the local high priority tasks' interference for a given task t.
	 * CI is a set of computation time of local tasks, including spin delay.
	 */
	private long highPriorityInterference(SporadicTask t, ArrayList<ArrayList<SporadicTask>> allTasks, long Ri) {
		long interference = 0;
		int partition = t.partition;
		ArrayList<SporadicTask> tasks = allTasks.get(partition);

		for (int i = 0; i < tasks.size(); i++) {
			if (tasks.get(i).priority > t.priority) {
				SporadicTask hpTask = tasks.get(i);
				interference += Math.ceil((double) (Ri) / (double) hpTask.period) * (hpTask.WCET);
			}
		}
		return interference;
	}

	private long localBlocking(SporadicTask t, ArrayList<ArrayList<SporadicTask>> tasks, ArrayList<Resource> resources, long[][] Ris, long Ri, boolean btbHit,
			boolean useRi, boolean isMSRP) {
		ArrayList<Resource> LocalBlockingResources = isMSRP ? getLocalBlockingResourcesMSRP(t, resources, tasks.get(t.partition))
				: getLocalBlockingResourcesMrsP(t, resources, tasks.get(t.partition));
		ArrayList<Long> local_blocking_each_resource = new ArrayList<>();

		for (int i = 0; i < LocalBlockingResources.size(); i++) {
			Resource res = LocalBlockingResources.get(i);
			long local_blocking = res.csl;

			if (res.isGlobal) {
				for (int parition_index = 0; parition_index < res.partitions.size(); parition_index++) {
					int partition = res.partitions.get(parition_index);
					int norHP = getNoRFromHP(t, res, tasks.get(t.partition), Ris[t.partition], btbHit, useRi, Ri);
					int norT = t.resource_required_index.contains(res.id - 1)
							? t.number_of_access_in_one_release.get(t.resource_required_index.indexOf(res.id - 1))
							: 0;
					int norR = getNoRRemote(res, tasks.get(partition), Ris[partition], Ri, btbHit, useRi);

					if (partition != t.partition && (norHP + norT) < norR) {
						local_blocking += res.csl;
					}
				}
			}
			local_blocking_each_resource.add(local_blocking);
		}

		if (local_blocking_each_resource.size() >= 1) {
			local_blocking_each_resource.sort((l1, l2) -> -Double.compare(l1, l2));
		}

		return local_blocking_each_resource.size() > 0 ? local_blocking_each_resource.get(0) : 0;
	}

	private ArrayList<Resource> getLocalBlockingResourcesMSRP(SporadicTask task, ArrayList<Resource> resources, ArrayList<SporadicTask> localTasks) {
		ArrayList<Resource> localBlockingResources = new ArrayList<>();
		int partition = task.partition;

		for (int i = 0; i < resources.size(); i++) {
			Resource resource = resources.get(i);
			// local resources that have a higher ceiling
			if (resource.partitions.size() == 1 && resource.partitions.get(0) == partition && resource.getCeilingForProcessor(localTasks) >= task.priority) {
				for (int j = 0; j < resource.requested_tasks.size(); j++) {
					SporadicTask LP_task = resource.requested_tasks.get(j);
					if (LP_task.partition == partition && LP_task.priority < task.priority) {
						localBlockingResources.add(resource);
						break;
					}
				}
			}
			// global resources that are accessed from the partition
			if (resource.partitions.contains(partition) && resource.partitions.size() > 1) {
				for (int j = 0; j < resource.requested_tasks.size(); j++) {
					SporadicTask LP_task = resource.requested_tasks.get(j);
					if (LP_task.partition == partition && LP_task.priority < task.priority) {
						localBlockingResources.add(resource);
						break;
					}
				}
			}
		}

		return localBlockingResources;
	}

	private ArrayList<Resource> getLocalBlockingResourcesMrsP(SporadicTask task, ArrayList<Resource> resources, ArrayList<SporadicTask> localTasks) {
		ArrayList<Resource> localBlockingResources = new ArrayList<>();
		int partition = task.partition;

		for (int i = 0; i < resources.size(); i++) {
			Resource resource = resources.get(i);

			if (resource.partitions.contains(partition) && resource.getCeilingForProcessor(localTasks) >= task.priority) {
				for (int j = 0; j < resource.requested_tasks.size(); j++) {
					SporadicTask LP_task = resource.requested_tasks.get(j);
					if (LP_task.partition == partition && LP_task.priority < task.priority) {
						localBlockingResources.add(resource);
						break;
					}
				}
			}
		}

		return localBlockingResources;
	}

	/*
	 * gives that number of requests from HP local tasks for a resource that is
	 * required by the given task.
	 */
	private int getNoRFromHP(SporadicTask task, Resource resource, ArrayList<SporadicTask> tasks, long[] Ris, boolean btbHit, boolean useRi, long Ri) {
		int number_of_request_by_HP = 0;
		int priority = task.priority;

		for (int i = 0; i < tasks.size(); i++) {
			if (tasks.get(i).priority > priority && tasks.get(i).resource_required_index.contains(resource.id - 1)) {
				SporadicTask hpTask = tasks.get(i);
				int indexR = getIndexRInTask(hpTask, resource);
				if (useCorrection) {
					number_of_request_by_HP += Math.ceil((double) (Ri) / (double) hpTask.period) * hpTask.number_of_access_in_one_release.get(indexR);
				} else {
					number_of_request_by_HP += Math.ceil((double) (Ri + (btbHit ? (useRi ? Ris[i] : hpTask.deadline) : 0)) / (double) hpTask.period)
							* hpTask.number_of_access_in_one_release.get(indexR);
				}
			}
		}
		return number_of_request_by_HP;
	}

	private int getNoRRemote(Resource resource, ArrayList<SporadicTask> tasks, long[] Ris, long Ri, boolean btbHit, boolean useRi) {
		int number_of_request_by_Remote_P = 0;

		for (int i = 0; i < tasks.size(); i++) {
			if (tasks.get(i).resource_required_index.contains(resource.id - 1)) {
				SporadicTask remote_task = tasks.get(i);
				int indexR = getIndexRInTask(remote_task, resource);
				number_of_request_by_Remote_P += (Math
						.ceil((double) (Ri + (btbHit ? (useRi ? Ris[i] : remote_task.deadline) : 0)) / (double) remote_task.period))
						* remote_task.number_of_access_in_one_release.get(indexR);
			}
		}
		return number_of_request_by_Remote_P;
	}

	/*
	 * Return the index of a given resource stored in a task.
	 */
	private int getIndexRInTask(SporadicTask task, Resource resource) {
		int indexR = -1;
		if (task.resource_required_index.contains(resource.id - 1)) {
			for (int j = 0; j < task.resource_required_index.size(); j++) {
				if (resource.id - 1 == task.resource_required_index.get(j)) {
					indexR = j;
					break;
				}
			}
		}
		return indexR;
	}
}
