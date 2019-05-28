package utils;

import java.util.ArrayList;

import entity.SporadicTask;

public class AnalysisUtils {
	public static final int MAX_PRIORITY = 1000;

	/* define how long the critical section can be */
	public static enum CS_LENGTH_RANGE {
		VERY_LONG_CSLEN, LONG_CSLEN, MEDIUM_CS_LEN, SHORT_CS_LEN, VERY_SHORT_CS_LEN, Random
	};

	/* define how many resources in the system */
	public static enum RESOURCES_RANGE {
		HALF_PARITIONS, /* partitions / 2 us */
		PARTITIONS, /* partitions us */
		DOUBLE_PARTITIONS, /* partitions * 2 us */
	};

	public ArrayList<ArrayList<SporadicTask>> permutePartition(ArrayList<SporadicTask> tasks) {
		ArrayList<ArrayList<SporadicTask>> list = new ArrayList<>();
		backtrack(list, new ArrayList<>(), tasks);
		return list;
	}

	// public ArrayList<ArrayList<SporadicTask>>
	// permuteSystem(ArrayList<SporadicTask> tasks) {
	// ArrayList<ArrayList<SporadicTask>> list = new ArrayList<>();
	// backtrack(list, new ArrayList<>(), tasks);
	// return list;
	// }

	private void backtrack(ArrayList<ArrayList<SporadicTask>> list, ArrayList<SporadicTask> tempList, ArrayList<SporadicTask> nums) {
		if (tempList.size() == nums.size()) {
			list.add(new ArrayList<>(tempList));
		} else {
			for (int i = 0; i < nums.size(); i++) {
				if (tempList.contains(nums.get(i)))
					continue; // element already exists, skip
				tempList.add(nums.get(i));
				backtrack(list, tempList, nums);
				tempList.remove(tempList.size() - 1);
			}
		}
	}

	public long[][] initResponseTime(ArrayList<ArrayList<SporadicTask>> tasks) {
		long[][] response_times = new long[tasks.size()][];

		for (int i = 0; i < tasks.size(); i++) {
			ArrayList<SporadicTask> task_on_a_partition = tasks.get(i);
			task_on_a_partition.sort((t1, t2) -> -Integer.compare(t1.priority, t2.priority));

			long[] Ri = new long[task_on_a_partition.size()];

			for (int j = 0; j < task_on_a_partition.size(); j++) {
				SporadicTask t = task_on_a_partition.get(j);
				Ri[j] = t.Ri = t.WCET + t.pure_resource_execution_time;
				t.total = 0;

			}
			response_times[i] = Ri;
		}
		return response_times;
	}

	public boolean isSystemSchedulable(ArrayList<ArrayList<SporadicTask>> tasks, long[][] Ris) {
		for (int i = 0; i < tasks.size(); i++) {
			for (int j = 0; j < tasks.get(i).size(); j++) {
				if (tasks.get(i).get(j).deadline < Ris[i][j])
					return false;
			}
		}
		return true;
	}

	public void cloneList(long[][] oldList, long[][] newList) {
		for (int i = 0; i < oldList.length; i++) {
			for (int j = 0; j < oldList[i].length; j++) {
				newList[i][j] = oldList[i][j];
			}
		}
	}

	public boolean isArrayContain(int[] array, int value) {

		for (int i = 0; i < array.length; i++) {
			if (array[i] == value)
				return true;
		}
		return false;
	}

	public void printResponseTime(long[][] Ris, ArrayList<ArrayList<SporadicTask>> tasks) {

		for (int i = 0; i < Ris.length; i++) {
			for (int j = 0; j < Ris[i].length; j++) {
				System.out.println("T" + tasks.get(i).get(j).id + " RT: " + Ris[i][j] + ", P: " + tasks.get(i).get(j).priority + ", D: "
						+ tasks.get(i).get(j).deadline + ", Blocking = " + tasks.get(i).get(j).total + ", WCET = " + tasks.get(i).get(j).WCET + ", Resource: "
						+ tasks.get(i).get(j).pure_resource_execution_time);

			}
			System.out.println();
		}
	}

	public int compareSlack(SporadicTask t1, SporadicTask t2, boolean withBTB) {
		long slack1 = withBTB ? t1.addition_slack_BTB : t1.addition_slack;
		long deadline1 = t1.deadline;

		long slack2 = withBTB ? t2.addition_slack_BTB : t2.addition_slack;
		long deadline2 = t2.deadline;

		if (slack1 < slack2) {
			return -1;
		}

		if (slack1 > slack2) {
			return 1;
		}

		if (slack1 == slack2) {
			if (deadline1 < deadline2)
				return -1;
			if (deadline1 > deadline2)
				return 1;
			if (deadline1 == deadline2)
				return 0;
		}

		System.err.println(
				"New OPA comparator error!" + " slack1:  " + slack1 + " deadline1:  " + deadline1 + " slack2:  " + slack2 + " deadline2:  " + deadline2);
		System.exit(-1);
		return 0;
	}

}
