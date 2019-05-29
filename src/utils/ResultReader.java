package utils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

public class ResultReader {

	// public static void main(String[] args) {
	//
	// // schedreader();
	// // migReader();
	// for (int i = 1000; i < 1001; i++)
	// priorityReader(i);
	// }

	public static void priorityReader(int seed, boolean isMSRP, int NoP, int NoT, int NoA, double rsf, int cs_len, double[] range) {
		String result = "";

		if (isMSRP) {
			result += "\nMSRP \n";

			for (int i = 0; i < range.length; i++) {
				String filepath = "result/" + seed + " " + "MSRP" + " " + (int) (NoP == -1 ? range[i] : NoP) + " " + (int) (NoT == -1 ? range[i] : NoT) + " "
						+ (int) (NoA == -1 ? range[i] : NoA) + " " + (rsf == -1 ? range[i] : rsf) + " " + (int) (cs_len == -1 ? range[i] : cs_len) + ".txt";

				List<String> lines = null;
				try {
					lines = Files.readAllLines(Paths.get(filepath), StandardCharsets.UTF_8);
				} catch (IOException e) {
				}
				if (lines != null)
					result += seed + " " + "MSRP" + " " + (NoP == -1 ? range[i] : NoP) + " " + (NoT == -1 ? range[i] : NoT) + " " + (NoA == -1 ? range[i] : NoA)
							+ " " + (rsf == -1 ? range[i] : rsf) + " " + (cs_len == -1 ? range[i] : cs_len) + " : " + lines.get(0) + "\n";
			}
		} else {
			result += "\n\nMrsP \n";

			for (int i = 0; i < range.length; i++) {
				String filepath = "result/" + seed + " " + "MrsP" + " " + (int) (NoP == -1 ? range[i] : NoP) + " " + (int) (NoT == -1 ? range[i] : NoT) + " "
						+ (int) (NoA == -1 ? range[i] : NoA) + " " + (rsf == -1 ? range[i] : rsf) + " " + (int) (cs_len == -1 ? range[i] : cs_len) + ".txt";

				List<String> lines = null;
				try {
					lines = Files.readAllLines(Paths.get(filepath), StandardCharsets.UTF_8);
				} catch (IOException e) {
				}
				if (lines != null)
					result += seed + " " + "MrsP" + " " + (NoP == -1 ? range[i] : NoP) + " " + (NoT == -1 ? range[i] : NoT) + " " + (NoA == -1 ? range[i] : NoA)
							+ " " + (rsf == -1 ? range[i] : rsf) + " " + (cs_len == -1 ? range[i] : cs_len) + " : " + lines.get(0) + "\n";
			}
		}

		System.out.println(result);

		PrintWriter writer = null;
		try {
			writer = new PrintWriter(new FileWriter(
					new File("result/All " + (isMSRP ? "MSRP" : "MrsP") + " " + seed + (NoP == -1 ? "" : " " + (int) NoP) + (NoT == -1 ? "" : " " + (int) NoT)
							+ (NoA == -1 ? "" : " " + (int) NoA) + (rsf == -1 ? "" : " " + rsf) + (cs_len == -1 ? "" : " " + (int) cs_len) + ".txt"),
					false));
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
