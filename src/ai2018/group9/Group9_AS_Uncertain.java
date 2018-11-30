package ai2018.group9;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import genius.core.Bid;
import genius.core.boaframework.AcceptanceStrategy;
import genius.core.boaframework.Actions;
import genius.core.boaframework.BOAparameter;
import genius.core.boaframework.NegotiationSession;
import genius.core.boaframework.OfferingStrategy;
import genius.core.boaframework.OpponentModel;
import genius.core.uncertainty.UserModel;

/**
 * Accepts:
 * <ul>
 * <li>if we have uncertainty profile, and we receive an offer in our highest
 * 10% best bids.
 * <li>if we have normal utilityspace, and we receive offer with a utility
 * better than 90% of what we offered last.
 * </ul>
 * Discount is ignored.
 */
public class Group9_AS_Uncertain extends AcceptanceStrategy {
	
	private double a; //a user-defined constant which is our utilityThreshold
	
	/**
	 * Empty constructor for the BOA framework.
	 */
	public Group9_AS_Uncertain() {
	}

	/**
	 * constructor for the BOA framework
	 * 
	 * @param negoSession
	 * @param strat
	 * @param alpha
	 */
	public Group9_AS_Uncertain(NegotiationSession negoSession, OfferingStrategy strat, double alpha) {
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
		Bid receivedBid = negotiationSession.getOpponentBidHistory()
				.getLastBid();
		Bid lastOwnBid = negotiationSession.getOwnBidHistory().getLastBid();
		if (receivedBid == null || lastOwnBid == null) {
			return Actions.Reject;
		}

		UserModel userModel = negotiationSession.getUserModel();
		if (userModel != null) {
			List<Bid> bidOrder = userModel.getBidRanking().getBidOrder();
			if (bidOrder.contains(receivedBid)) {
				double percentile = (bidOrder.size()
						- bidOrder.indexOf(receivedBid))
						/ (double) bidOrder.size();
				if (percentile < 0.1)
					return Actions.Accept;
			}
		} else {
			/*// we have a normal utilityspace
			double otherLastUtil = negotiationSession.getUtilitySpace()
					.getUtility(receivedBid);
			double myLastUtil = negotiationSession.getUtilitySpace()
					.getUtility(lastOwnBid);
			if (otherLastUtil >= 0.9 * myLastUtil) {
				return Actions.Accept;
			}*/
			
			double lastOpponentBidUtil = negotiationSession.getOpponentBidHistory().getLastBidDetails()
					.getMyUndiscountedUtil();
			double timeLeft = 1 - negotiationSession.getTime(); //ranges [0, 1]

			// Accept if our utility is above a or above the square root of time left 
			//6.10.2018 changed from taking the root of t to  t to the power of 0.1 to make the descending slower.
			if (lastOpponentBidUtil >= Math.min(a,Math.pow(timeLeft,0.1))) {
				return Actions.Accept;
			}

			return Actions.Reject;
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
		return "Group9 Acceptance Strategy Uncertain";
	}
}