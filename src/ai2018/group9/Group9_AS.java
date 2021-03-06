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
import genius.core.utility.AdditiveUtilitySpace;

/**
 * Acceptance Strategy of BOAFramework <br>
 * 
 * Accepts:
 * <ul>
 * <li>if we have uncertainty profile, and we receive an offer in our highest
 * 10% best bids.
 * <li>if we have normal utilityspace, and we receive offer with a utility
 * better than 90% of what we offered last.
 * </ul>
 * Discount is ignored.
 * 
 * @author Group 9
 */
public class Group9_AS extends AcceptanceStrategy {
	
	/**
	 * a user-defined constant as one of the parameters
	 * in the function of acceptance threshold
	 */
	private double a;
	
	/**
	 * our general Accept Strategy
	 */
	private static String ACCEPT_STRATEGY = "Accept if our utility is above a "
			+ "or above the square root of time left + 0.35";
	
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
		
		// under uncertainty
		if (null != negotiationSession.getUserModel()) {
			// 1. estimated utility space
			AdditiveUtilitySpace u = initUncertainty(negoSession);
			
			// 2. replace the utility space in negotiation session
			this.negotiationSession = new NegotiationSession(negoSession.getSessionData(), 
					u, negoSession.getTimeline(), negoSession.getOutcomeSpace(), 
					negoSession.getUserModel());
		}
	}
	
	/**
	 * Initializes and estimates utility space under uncertainty
	 * 
	 * @param negoSession
	 * @return AdditiveUtilitySpace
	 */
	private AdditiveUtilitySpace initUncertainty(NegotiationSession negoSession) {
		Group9_UtilitySpaceFactory factory = new Group9_UtilitySpaceFactory(negoSession.getDomain());
		
		// since we use similar strategy to that of Opponent Model
		// get the parameters from OM if possible
		if (opponentModel instanceof Group9_OM) {
			Group9_OM om = (Group9_OM)opponentModel;

			factory.setLearnCoef(om.getLearnCoef());
			factory.setLearnValueAddition(om.getLearnValueAddition());
		}
		
		// estimate the utility space
		factory.estimateUsingBidRanks(negotiationSession.getUserModel().getBidRanking());
		return factory.getUtilitySpace();
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
		
		double timeLeft = 1 - negotiationSession.getTime(); //ranges [0, 1]
		
		// Accept if our utility is above a or above the square root of time left + 0.35
		
		// Define the acceptThreshold as a function of time and a
		double acceptThreshold = Math.min(a, Math.sqrt(timeLeft) + 0.35);
		
		double lastOpponentBidUtil = negotiationSession.getOpponentBidHistory()
				.getLastBidDetails().getMyUndiscountedUtil();
		
		// Accept if the utility is higher than the calculated acceptThreshold
		if (lastOpponentBidUtil >= acceptThreshold) {
			
			// Under uncertainty
			// Apply ranking rule to make sure we accept a high-utility bid
			if (null != negotiationSession.getUserModel()) {
				List<Bid> bidOrder = 
						negotiationSession.getUserModel().getBidRanking().getBidOrder();
				
				if (bidOrder.contains(receivedBid)) {
					double percentile = bidOrder.indexOf(receivedBid)
							/ (double) bidOrder.size();
					// Reject if the bid ranks lower than acceptThreshold
					if (percentile < acceptThreshold) {
						return Actions.Reject;
					}
				}
			}
			
			return Actions.Accept;
		}
		
		return Actions.Reject;
	}
	
	@Override
	public Set<BOAparameter> getParameterSpec() {

		Set<BOAparameter> set = new HashSet<BOAparameter>();
		set.add(new BOAparameter("a", 0.8, ACCEPT_STRATEGY));

		return set;
	}

	@Override
	public String getName() {
		return "Group9 Acceptance Strategy";
	}
}