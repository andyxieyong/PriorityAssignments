package entity;

import java.text.DecimalFormat;
import java.util.ArrayList;

public class SporadicTask {
	public int priority;
	public long period;
	public long deadline;
	public long WCET;
	public int partition;
	public int id;
	public double util;

	public long pure_resource_execution_time = 0;
	public long Ri = 0, spin = 0, interference = 0, local = 0, total = 0;

	public ArrayList<Integer> resource_required_index;
	public ArrayList<Integer> number_of_access_in_one_release;

	public long addition_slack_BTB = 0;
	public long addition_slack = 0;
	

	public SporadicTask(int priority, long t, long d, long c, int partition, int id) {
		this(priority, t, d, c, partition, id, -1);
	}

	public SporadicTask(int priority, long t, long d, long c, int partition, int id, double util) {
		this.priority = priority;
		this.period = t;
		this.WCET = c;
		this.deadline = d;
		this.partition = partition;
		this.id = id;
		this.util = util;

		resource_required_index = new ArrayList<>();
		number_of_access_in_one_release = new ArrayList<>();

		Ri = 0;
		spin = 0;
		interference = 0;
		local = 0;
		total = 0;
	}

	@Override
	public String toString() {
		return "T" + this.id + " : T = " + this.period + ", C = " + this.WCET + ", PRET: " + this.pure_resource_execution_time + ", spin = " + spin + ", D = "
				+ this.deadline + ", Priority = " + this.priority + ", Partition = " + this.partition;
	}

	public String RTA() {
		return "T" + this.id + " : R = " + this.Ri + ", S = " + this.spin + ", I = " + this.interference + ", A = " + this.local + ", Total = " + this.total
				+ ". is schedulable: " + (Ri <= deadline);
	}

	public String getInfo() {
		DecimalFormat df = new DecimalFormat("#.#######");
		return "T" + this.id + " : T = " + this.period + ", C = " + this.WCET + ", PRET: " + this.pure_resource_execution_time + ", D = " + this.deadline
				+ ", Priority = " + this.priority + ", Partition = " + this.partition + ", Util: " + Double.parseDouble(df.format(util));
	}

}
