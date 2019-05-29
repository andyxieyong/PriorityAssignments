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
import analysisWithPriority.OriginalTest;
import entity.Resource;
import entity.SporadicTask;
import generatorTools.SimpleSystemGenerator;
import utils.AnalysisUtils;
import utils.AnalysisUtils.RESOURCES_RANGE;
import utils.ResultReader;

public class Experiments {
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
		try {
			access(1000, true, true);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public static void EP2() {
		try {
			csl(1000, true, true);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public static void EP3() {
		int times = 10000;
		try {
			access(times, true, false);
			csl(times, false, true);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public static void access(int TOTAL_NUMBER_OF_SYSTEMS, boolean useMSRP, boolean useMrsP) throws Exception {
		int NoP = 16;
		int NoT = 4;
		int NoA = 1;
		double rsf = 0.4;
		int cslen = 1;

		int incrementor = 5;
		int times = 11;

		Experiments test = new Experiments();
		HolistictTest holistic = new HolistictTest();
		OriginalTest origin = new OriginalTest();

		int countTime = useMSRP && useMrsP ? times * 2 : times;

		final CountDownLatch holisticCD = new CountDownLatch(countTime);
		for (int i = 0; i < times; i++) {
			final int counter = i;
			if (useMSRP) {
				new Thread(new Runnable() {
					@Override
					public void run() {
						test.PriorityOrder(holistic, origin, NoP, NoT, NoA + incrementor * counter, rsf, cslen, true, TOTAL_NUMBER_OF_SYSTEMS);
						test.countDown(holisticCD);
					}
				}).start();
			}
			if (useMrsP) {
				new Thread(new Runnable() {
					@Override
					public void run() {
						test.PriorityOrder(holistic, origin, NoP, NoT, NoA + incrementor * counter, rsf, cslen, false, TOTAL_NUMBER_OF_SYSTEMS);
						test.countDown(holisticCD);
					}
				}).start();
			}

		}

		holisticCD.await();

		double[] range = new double[times];
		for (int i = 0; i < times; i++)
			range[i] = NoA + incrementor * i;

		ResultReader.priorityReader(SEED, NoP, NoT, -1, rsf, cslen, range);
	}

	public static void csl(int TOTAL_NUMBER_OF_SYSTEMS, boolean useMSRP, boolean useMrsP) throws Exception {
		int NoP = 16;
		int NoT = 3;
		int NoA = 10;
		double rsf = 0.4;

		int times = 8;
		Experiments test = new Experiments();
		HolistictTest holistic = new HolistictTest();
		OriginalTest origin = new OriginalTest();

		int countTime = useMSRP && useMrsP ? times * 2 : times;

		final CountDownLatch holisticCD = new CountDownLatch(countTime);
		for (int i = 1; i <= times; i++) {
			final int counter = i;
			if (useMSRP) {
				new Thread(new Runnable() {
					@Override
					public void run() {
						test.PriorityOrder(holistic, origin, NoP, NoT, NoA, rsf, counter, true, TOTAL_NUMBER_OF_SYSTEMS);
						test.countDown(holisticCD);
					}
				}).start();
			}

			if (useMrsP) {
				new Thread(new Runnable() {
					@Override
					public void run() {
						test.PriorityOrder(holistic, origin, NoP, NoT, NoA, rsf, counter, false, TOTAL_NUMBER_OF_SYSTEMS);
						test.countDown(holisticCD);
					}
				}).start();
			}
		}

		holisticCD.await();

		double[] range = new double[times];
		for (int i = 1; i <= times; i++)
			range[i - 1] = i;

		ResultReader.priorityReader(SEED, NoP, NoT, NoA, rsf, -1, range);
	}

	public void PriorityOrder(HolistictTest holistic, OriginalTest original, int NoP, int NoT, int NoA, double rsf, int cs_len, boolean isMSRP,
			int TOTAL_NUMBER_OF_SYSTEMS) {

		SimpleSystemGenerator generator = new SimpleSystemGenerator(MIN_PERIOD, MAX_PERIOD, NoP, NoP * NoT, true, cs_len, RESOURCES_RANGE.PARTITIONS, rsf, NoA,
				SEED);

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
