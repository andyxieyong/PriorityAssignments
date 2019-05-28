package entity;

import java.util.ArrayList;

public class Resource {

	public int id;
	public long csl;

	public ArrayList<SporadicTask> requested_tasks;
	public ArrayList<Integer> partitions;
	public ArrayList<Integer> ceiling;

	public boolean isGlobal = false;

	public Resource(int id, long cs_len) {
		this.id = id;
		this.csl = cs_len;
		requested_tasks = new ArrayList<>();
		partitions = new ArrayList<>();
		ceiling = new ArrayList<>();
	}

	@Override
	public String toString() {
		return "R" + this.id + " : cs len = " + this.csl + ", partitions: " + partitions.size() + ", tasks: " + requested_tasks.size() + ", isGlobal: "
				+ isGlobal;
	}

	public int getCeilingForProcessor(ArrayList<ArrayList<SporadicTask>> tasks, int partition) {
		int ceiling = -1;

		for (int k = 0; k < tasks.get(partition).size(); k++) {
			SporadicTask task = tasks.get(partition).get(k);

			if (task.resource_required_index.contains(this.id - 1)) {
				ceiling = task.priority > ceiling ? task.priority : ceiling;
			}
		}

		return ceiling;
	}

	public int getCeilingForProcessor(ArrayList<SporadicTask> tasks) {
		int ceiling = -1;

		for (int k = 0; k < tasks.size(); k++) {
			SporadicTask task = tasks.get(k);

			if (task.resource_required_index.contains(this.id - 1)) {
				ceiling = task.priority > ceiling ? task.priority : ceiling;
			}
		}

		return ceiling;
	}
}
