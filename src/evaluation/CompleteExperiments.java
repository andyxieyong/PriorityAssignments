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

public class CompleteExperiments {
	public static int MAX_PERIOD = 1000;
	public static int MIN_PERIOD = 1;
	public static int SEED = 1000;

	int count = 0;

	public synchronized void countDown(CountDownLatch cd) {
		cd.countDown();
		count++;
	}

	public static void main(String args[]) throws Exception {
		int[] NoP = { 8, 12, 16 };
		int[] NoT = { 3, 4, 5, 6 };
		int[] NoA = { 2, 5, 10, 15 };
		double[] RSF = { 0.2, 0.3, 0.4 };
		int[] CSL = { 1, 2, 3, 4, 5, 6, 7, 8 };
		int times = 1000;

		CompleteExperiments ep = new CompleteExperiments();

		for (int p = 0; p < NoP.length; p++) {
			for (int t = 0; t < NoT.length; t++) {
				for (int a = 0; a < NoA.length; a++) {
					CountDownLatch cd = new CountDownLatch(RSF.length * CSL.length * 2);

					for (int r = 0; r < RSF.length; r++) {
						for (int l = 0; l < CSL.length; l++) {

							final int nop = p, not = t, noa = a, rsf = r, csl = l;

							new Thread(new Runnable() {
								@Override
								public void run() {
									ep.PriorityOrder(NoP[nop], NoT[not], NoA[noa], RSF[rsf], CSL[csl], true, null, times);
									ep.countDown(cd);
								}
							}).start();

							new Thread(new Runnable() {
								@Override
								public void run() {
									ep.PriorityOrder(NoP[nop], NoT[not], NoA[noa], RSF[rsf], CSL[csl], false, null, times);
									ep.countDown(cd);
								}
							}).start();

						}
					}
					cd.await();
				}
			}
		}

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

			boolean DMok = false, OPAok = false, SBPOok = false, DMTok = false;

			if (isMSRP) {
				long[][] Ris = new MSRPOriginal().getResponseTime(tasks, resources, false);
				if (new AnalysisUtils().isSystemSchedulable(tasks, Ris)) {
					DMT++;
					DMTok = true;
				}
			} else {
				long[][] Ris = new MrsPOriginal().getResponseTime(tasks, resources, false);
				if (new AnalysisUtils().isSystemSchedulable(tasks, Ris)) {
					DMT++;
					DMTok = true;
				}
			}

			if (DMTok) {
				DM++;
				DMok = true;

				RPA++;

				OPA++;
				OPAok = true;

				SBPO++;
				SBPOok = true;
			} else {
				if (holistic.getResponseTimeOPA(tasks, resources, isMSRP)) {
					OPA++;
					OPAok = true;
				}

				if (OPAok) {
					DM++;
					DMok = true;

					RPA++;

					SBPO++;
					SBPOok = true;
				} else {
					if (holistic.getResponseTimeDM(tasks, resources, isMSRP)) {
						DM++;
						DMok = true;
					}

					if (holistic.getResponseTimeSPO(tasks, resources, isMSRP)) {
						SBPO++;
						SBPOok = true;
					}
				}
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
