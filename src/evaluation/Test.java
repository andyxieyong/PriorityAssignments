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
import entity.Resource;
import entity.SporadicTask;
import generatorTools.SimpleSystemGenerator;
import utils.AnalysisUtils.CS_LENGTH_RANGE;
import utils.AnalysisUtils.RESOURCES_RANGE;
import utils.ResultReader;

public class Test {
	public static int MAX_PERIOD = 1000;
	public static int MIN_PERIOD = 1;
	public static int TOTAL_NUMBER_OF_SYSTEMS = 1000;
	public static int TOTAL_PARTITIONS = 16;
	public static int SEED = 1000;

	public static void main(String[] args) throws Exception {
		Test test = new Test();
		HolistictTest holistic = new HolistictTest();
		final CountDownLatch holisticCD = new CountDownLatch(6 * 2);
		for (int i = 1; i < 7; i++) {
			final int cslen = i;
			new Thread(new Runnable() {
				@Override
				public void run() {
					test.PriorityOrder(holistic, cslen, true);
					holisticCD.countDown();
				}
			}).start();
			new Thread(new Runnable() {
				@Override
				public void run() {
					test.PriorityOrder(holistic, cslen, false);
					holisticCD.countDown();
				}
			}).start();
		}
		holisticCD.await();
		for (int i = 1000; i < 1001; i++)
			ResultReader.priorityReader(i);
	}

	public void PriorityOrder(HolistictTest analysis, int cs_len, boolean isMSRP) {
		int NoA = 2;
		int NoT = 3;
		double Rsf = 0.4;

		final CS_LENGTH_RANGE cs_range;
		switch (cs_len) {
		case 1:
			cs_range = CS_LENGTH_RANGE.VERY_SHORT_CS_LEN;
			break;
		case 2:
			cs_range = CS_LENGTH_RANGE.SHORT_CS_LEN;
			break;
		case 3:
			cs_range = CS_LENGTH_RANGE.MEDIUM_CS_LEN;
			break;
		case 4:
			cs_range = CS_LENGTH_RANGE.LONG_CSLEN;
			break;
		case 5:
			cs_range = CS_LENGTH_RANGE.VERY_LONG_CSLEN;
			break;
		case 6:
			cs_range = CS_LENGTH_RANGE.Random;
			break;
		default:
			cs_range = null;
			break;
		}

		SimpleSystemGenerator generator = new SimpleSystemGenerator(MIN_PERIOD, MAX_PERIOD, TOTAL_PARTITIONS, TOTAL_PARTITIONS * NoT, true,
				cs_range, RESOURCES_RANGE.PARTITIONS, Rsf, NoA, SEED);

		String result = "";
		String name = isMSRP ? "MSRP" : "MrsP";
		int DM = 0;
		int OPA = 0;
		int RPA = 0;
		int SBPO = 0;

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

			boolean DMok = false, OPAok = false, RPAok = false, SBPOok = false;

			if (analysis.getResponseTimeDM(tasks, resources, isMSRP)) {
				DM++;
				DMok = true;
			}

			// if (analysis.getResponseTimeRPA(tasks, resources, isMSRP)) {
			// RPA++;
			// RPAok = true;
			// }

			if (analysis.getResponseTimeOPA(tasks, resources, isMSRP)) {
				OPA++;
				OPAok = true;
			}

			if (analysis.getResponseTimeSPO(tasks, resources, isMSRP)) {
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

			System.out.println(name + " " + 2 + " " + 4 + " " + cs_len + " times: " + i);

		}

		result = name + " " + (double) SBPO / (double) TOTAL_NUMBER_OF_SYSTEMS + " " + (double) DM / (double) TOTAL_NUMBER_OF_SYSTEMS + " "
				+ (double) OPA / (double) TOTAL_NUMBER_OF_SYSTEMS + " " + (double) RPA / (double) TOTAL_NUMBER_OF_SYSTEMS;

		result += " " + DMcannotOPAcan + " " + DMcanOPAcannot + " " + DMcannotSBPOcan + " " + DMcanSBPOcannot + " " + OPAcanSBPOcannot + " "
				+ OPAcannotSBPOcan + "\n";

		writeSystem((SEED + " " + name + " " + 2 + " " + 4 + " " + cs_len), result);
	}

	public static boolean isSystemSchedulable(ArrayList<ArrayList<SporadicTask>> tasks, long[][] Ris) {
		for (int i = 0; i < tasks.size(); i++) {
			for (int j = 0; j < tasks.get(i).size(); j++) {
				if (tasks.get(i).get(j).deadline < Ris[i][j])
					return false;
			}
		}
		return true;
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
