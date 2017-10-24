package br.unb.cic.tdp1375.audit;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Arrays;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import bioinfo.auditor.Auditor;
import bioinfo.auditor.RearrangementAlgorithm;
import br.unb.cic.tdp1375.Sorting1375;
import br.unb.cic.tdp1375.permutations.Cycle;

/**
 *
 * @author luizaugusto
 */
public class GraauAuditingLMN implements RearrangementAlgorithm {

	private static final Logger logger = Logger.getLogger(GraauAuditingLMN.class);
	private String algorithmName;

	public GraauAuditingLMN(String algorithmName) {
		this.algorithmName = algorithmName;
	}

	public static void main(String args[]) {
		Logger log = Logger.getLogger("br.unb");
		log.setLevel(Level.OFF);
		log = Logger.getLogger("br.unb.cic.genomicrearragement.audit");
		log.setLevel(Level.ALL);
		log = Logger.getLogger("org.apache");
		log.setLevel(Level.ERROR);
		log = Logger.getLogger("httpclient");
		log.setLevel(Level.ERROR);

		// Instructions on http://mirza.ic.unicamp.br:8080/bioinfo/graau.jsf
		
		int threads = Integer.parseInt("4");
		String algorithmName = "LMN1375";
		String ticket = "d6017b1d34e64694b14e67e49703e613";

		while (true) {
			if (online()) {
				try {
					Auditor auditor = new Auditor("http://mirza.ic.unicamp.br:8080/axis2/services/BioinfoService");
					logger.info("auditor instanciado");
					auditor.audit(new GraauAuditingLMN(algorithmName), threads, ticket);
				} catch (Exception ex) {
					logger.error(ExceptionUtils.getFullStackTrace(ex));
				}
			}
		}
	}

	@Override
	public String getName() {
		return algorithmName;
	}

	@Override
	public String getRearrangementModelCode() {
		return "T";
	}

	@Override
	public int getDistance(int[] permutation) {
		int d = 0;

		try {
			Cycle pi = new Cycle("0," + StringUtils.join(ArrayUtils.toObject(permutation), ","));
			// In order to simulate the algorithm of Elias and Hartman, it is enough to simplify the permutation before sorting it
			d = Sorting1375.sort(pi);
		} catch (Exception e) {
			logger.error(String.format("erro ao ordenar %s", Arrays.toString(permutation)));
			logger.error(ExceptionUtils.getStackTrace(e));
		}

		return d;
	}

	private static boolean online() {
		try {
			Thread.sleep(10000);
			URL url = new URL("http://mirza.ic.unicamp.br:8080/axis2/services/BioinfoService");
			HttpURLConnection huc = (HttpURLConnection) url.openConnection();
			huc.setRequestMethod("HEAD");
			huc.getResponseCode();
			return true;
		} catch (Exception ex) {
			return false;
		}
	}
}
