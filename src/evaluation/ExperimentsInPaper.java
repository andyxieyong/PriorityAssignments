package evaluation;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.concurrent.CountDownLatch;

import analysisWithPriority.HolistictTest;
import analysisWithPriority.MSRPOriginal;
import analysisWithPriority.MrsPOriginal;
import entity.Resource;
import entity.SporadicTask;
import generatorTools.SimpleSystemGenerator;
import utils.AnalysisUtils;
import utils.AnalysisUtils.RESOURCES_RANGE;
import utils.ResultReader;

public class ExperimentsInPaper {
	public static int MAX_PERIOD = 1000;
	public static int MIN_PERIOD = 1;
	public static int SEED = 1000;

	int count = 0;

	public synchronized void countDown(CountDownLatch cd) {
		cd.countDown();
		count++;
		System.out.println("!!: " + count);
	}

	public static void main(String args[]) throws Exception {
		EP3();
		
	}

	public static void EP1() {
		int times = 1000;
		try {
			access(times, 16, 4, 1, 0.4, 1, true);
			access(times, 16, 4, 1, 0.25, 3, false);
		} catch (Exception e) {
		}
	}

	public static void EP2() {
		int times = 1000;
		try {
			long[][] MSRPrange = { { 1, 15 }, { 15, 30 }, { 30, 50 }, { 50, 75 }, { 75, 100 }, { 100, 150 }, { 150, 200 }, { 1, 200 } };
			csl(times, 16, 3, 30, 0.45, true, MSRPrange);

			long[][] MrsPrange = { { 1, 15 }, { 15, 50 }, { 50, 100 }, { 100, 200 }, { 200, 300 }, { 300, 400 }, { 400, 500 }, { 1, 500 } };
			csl(times, 16, 3, 10, 0.4, false, MrsPrange);
		} catch (Exception e) {
		}
	}

	public static void EP3() {
		int times = 10000;
		try {
			access(times, 16, 4, 1, 0.4, 1, true);
			long[][] MrsPrange = { { 1, 15 }, { 15, 50 }, { 50, 100 }, { 100, 200 }, { 200, 300 }, { 300, 400 }, { 400, 500 }, { 1, 500 } };
			csl(times, 16, 3, 10, 0.4, false, MrsPrange);
		} catch (Exception e) {
		}
	}

	public static void access(int TOTAL_NUMBER_OF_SYSTEMS, int NoP, int NoT, int NoA, double rsf, int cslen, boolean useMSRP) throws Exception {

		int incrementor = 5;
		int times = 11;

		ExperimentsInPaper test = new ExperimentsInPaper();

		final CountDownLatch holisticCD = new CountDownLatch(times);
		for (int i = 0; i < times; i++) {
			final int counter = i;
			if (useMSRP) {
				new Thread(new Runnable() {
					@Override
					public void run() {
						test.PriorityOrder(NoP, NoT, NoA + incrementor * counter, rsf, cslen, true, null, TOTAL_NUMBER_OF_SYSTEMS);
						test.countDown(holisticCD);
					}
				}).start();
			} else {
				new Thread(new Runnable() {
					@Override
					public void run() {
						test.PriorityOrder(NoP, NoT, NoA + incrementor * counter, rsf, cslen, false, null, TOTAL_NUMBER_OF_SYSTEMS);
						test.countDown(holisticCD);
					}
				}).start();
			}

		}

		holisticCD.await();

		double[] range = new double[times];
		for (int i = 0; i < times; i++)
			range[i] = NoA + incrementor * i;

		ResultReader.priorityReader(SEED, useMSRP, NoP, NoT, -1, rsf, cslen, range);
	}

	public static void csl(int TOTAL_NUMBER_OF_SYSTEMS, int NoP, int NoT, int NoA, double rsf, boolean useMSRP, long[][] cslRnage) throws Exception {

		int times = 8;
		ExperimentsInPaper test = new ExperimentsInPaper();

		final CountDownLatch holisticCD = new CountDownLatch(times);
		for (int i = 1; i <= times; i++) {
			final int counter = i;
			if (useMSRP) {
				new Thread(new Runnable() {
					@Override
					public void run() {
						test.PriorityOrder(NoP, NoT, NoA, rsf, counter, true, cslRnage, TOTAL_NUMBER_OF_SYSTEMS);
						test.countDown(holisticCD);
					}
				}).start();
			} else {
				new Thread(new Runnable() {
					@Override
					public void run() {
						test.PriorityOrder(NoP, NoT, NoA, rsf, counter, false, cslRnage, TOTAL_NUMBER_OF_SYSTEMS);
						test.countDown(holisticCD);
					}
				}).start();
			}
		}

		holisticCD.await();

		double[] range = new double[times];
		for (int i = 1; i <= times; i++)
			range[i - 1] = i;

		ResultReader.priorityReader(SEED, useMSRP, NoP, NoT, NoA, rsf, -1, range);
	}

	public void PriorityOrder(int NoP, int NoT, int NoA, double rsf, int cs_len, boolean isMSRP, long[][] cslRnage, int TOTAL_NUMBER_OF_SYSTEMS) {

		HolistictTest holistic = new HolistictTest();

		SimpleSystemGenerator generator = new SimpleSystemGenerator(MIN_PERIOD, MAX_PERIOD, NoP, NoP * NoT, true, cs_len, RESOURCES_RANGE.PARTITIONS, rsf, NoA,
				SEED, cslRnage);

		String result = "";
		String name = isMSRP ? "MSRP" : "MrsP";
		int DM = 0;
		int OPA = 0;
		int RPA = 0;
		int SBPO = 0;
		int DMT = 0;

		int DMcannotOPAcan = 0;
		int DMcanOPAcannot = 0;

		int DMcannotSBPOcan = 0;
		int DMcanSBPOcannot = 0;

		int OPAcanSBPOcannot = 0;
		int OPAcannotSBPOcan = 0;

		for (int i = 0; i < TOTAL_NUMBER_OF_SYSTEMS; i++) {
			ArrayList<SporadicTask> tasksToAlloc = generator.generateTasks();
			ArrayList<Resource> resources = generator.generateResources();
			ArrayList<ArrayList<SporadicTask>> tasks = generator.generateResourceUsage(tasksToAlloc, resources);

			boolean DMok = false, OPAok = false, SBPOok = false;

			if (isMSRP) {
				long[][] Ris = new MSRPOriginal().getResponseTime(tasks, resources, false);
				if (new AnalysisUtils().isSystemSchedulable(tasks, Ris)) {
					DMT++;
				}
			} else {
				long[][] Ris = new MrsPOriginal().getResponseTime(tasks, resources, false);
				if (new AnalysisUtils().isSystemSchedulable(tasks, Ris)) {
					DMT++;
				}
			}

			if (holistic.getResponseTimeDM(tasks, resources, isMSRP)) {
				DM++;
				DMok = true;
			}

			if (holistic.getResponseTimeRPA(tasks, resources, isMSRP)) {
				RPA++;
			}

			if (holistic.getResponseTimeOPA(tasks, resources, isMSRP)) {
				OPA++;
				OPAok = true;
			}

			if (holistic.getResponseTimeSPO(tasks, resources, isMSRP)) {
				SBPO++;
				SBPOok = true;
			}

			if (!DMok && OPAok)
				DMcannotOPAcan++;

			if (DMok && !OPAok)
				DMcanOPAcannot++;

			if (!DMok && SBPOok)
				DMcannotSBPOcan++;

			if (DMok && !SBPOok)
				DMcanSBPOcannot++;

			if (OPAok && !SBPOok) {
				OPAcanSBPOcannot++;
			}

			if (!OPAok && SBPOok)
				OPAcannotSBPOcan++;

			System.out.println(name + " " + NoP + " " + NoT + " " + NoA + " " + rsf + " " + cs_len + " times: " + i);

		}

		result = name + " " + (double) SBPO / (double) TOTAL_NUMBER_OF_SYSTEMS + " " + (double) DM / (double) TOTAL_NUMBER_OF_SYSTEMS + " "
				+ (double) OPA / (double) TOTAL_NUMBER_OF_SYSTEMS + " " + (double) RPA / (double) TOTAL_NUMBER_OF_SYSTEMS + " "
				+ (double) DMT / (double) TOTAL_NUMBER_OF_SYSTEMS;

		result += " " + DMcannotSBPOcan + " " + DMcanSBPOcannot + " " + DMcannotOPAcan + " " + DMcanOPAcannot + " " + OPAcanSBPOcannot + " " + OPAcannotSBPOcan
				+ "\n";

		writeSystem((SEED + " " + name + " " + NoP + " " + NoT + " " + NoA + " " + rsf + " " + cs_len), result);
	}

	public static void writeSystem(String filename, String result) {
		PrintWriter writer = null;
		try {
			writer = new PrintWriter(new FileWriter(new File("result/" + filename + ".txt"), false));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		writer.println(result);
		writer.close();
	}
}
