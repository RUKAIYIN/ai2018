package ai2018.group9;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import genius.core.boaframework.AcceptanceStrategy;
import genius.core.boaframework.Actions;
import genius.core.boaframework.BOAparameter;
import genius.core.boaframework.NegotiationSession;
import genius.core.boaframework.OfferingStrategy;
import genius.core.boaframework.OpponentModel;

public class Group9_AS extends AcceptanceStrategy {

	private double a; //a user-defined constant which is our utilityThreshold

	/**
	 * Empty constructor for the BOA framework.
	 */
	public Group9_AS() {
	}

	/**
	 * constructor for the BOA framework
	 * 
	 * @param negoSession
	 * @param strat
	 * @param alpha
	 */
	public Group9_AS(NegotiationSession negoSession, OfferingStrategy strat, double alpha) {
		this.negotiationSession = negoSession;
		this.offeringStrategy = strat;
		this.a = alpha;
	}

	/**
	 * Initialization for the BOA framework
	 * 
	 * @param negoSession
	 * @param strat
	 * @param opponentModel
	 * @param parameters
	 * @throws Exception
	 */
	@Override
	public void init(NegotiationSession negoSession, OfferingStrategy strat, OpponentModel opponentModel,
			Map<String, Double> parameters) throws Exception {
		this.negotiationSession = negoSession;
		this.offeringStrategy = strat;

		if (parameters.get("a") != null) {
			a = parameters.get("a");
		} else {
			a = 0.8;
		}
	}

	@Override
	public String printParameters() {
		String str = "[a: " + a + "]";
		return str;
	}

	@Override
	public Actions determineAcceptability() {
		double lastOpponentBidUtil = negotiationSession.getOpponentBidHistory().getLastBidDetails()
				.getMyUndiscountedUtil();
		double timeLeft = 1 - negotiationSession.getTime(); //ranges [0, 1]

		// Accept if our utility is above a or above the square root of time left 
		if (lastOpponentBidUtil >= Math.min(a, Math.sqrt(timeLeft))) {
			return Actions.Accept;
		}

		return Actions.Reject;
	}

	@Override
	public Set<BOAparameter> getParameterSpec() {

		Set<BOAparameter> set = new HashSet<BOAparameter>();
		set.add(new BOAparameter("a", 0.8,
				"Accept if our utility is above a or above the square root of time left "));

		return set;
	}

	@Override
	public String getName() {
		return "Group9 Acceptance Strategy";
	}
}
