package ai2018.group9;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import genius.core.bidding.BidDetails;
import genius.core.boaframework.BOAparameter;
import genius.core.boaframework.NegotiationSession;
import genius.core.boaframework.OMStrategy;
import genius.core.boaframework.OpponentModel;
import genius.core.issue.Issue;
import genius.core.issue.Value;

/**
 * This class uses an opponent model to determine the next bid for the opponent,
 * while taking the opponent's preferences into account. The opponent model is
 * used to select the best bid.
 * 
 */
public class Group9_OMS extends OMStrategy {

	/**
	 * when to stop updating the opponentmodel. Note that this value is not
	 * exactly one as a match sometimes lasts slightly longer.
	 */
	double updateThreshold;
	
	
	/**
	 * weight of Hamming Distance
	 * Used to get a best bid
	 */
	double weightHammingDist;

	/**
	 * Initializes the opponent model strategy. If a value for the parameter t
	 * is given, then it is set to this value. Otherwise, the default value is
	 * used.
	 * 
	 * @param negotiationSession
	 *            state of the negotiation.
	 * @param model
	 *            opponent model used in conjunction with this opponent modeling
	 *            strategy.
	 * @param parameters
	 *            set of parameters for this opponent model strategy.
	 */
	@Override
	public void init(NegotiationSession negotiationSession, OpponentModel model, Map<String, Double> parameters) {
		super.init(negotiationSession, model, parameters);
		if (null != parameters.get("t")) {
			updateThreshold = parameters.get("t").doubleValue();
		} else {
			updateThreshold = 1.1;
		}
		
		if (null != parameters.get("w")) {
			weightHammingDist = parameters.get("w").doubleValue();
		} else {
			weightHammingDist = 3;
		}
	}

	/**
	 * Returns the best bid with lower Hamming distance and higher evaluation
	 * for the opponent given a set of similarly preferred bids.
	 * 
	 * @param list
	 *            of the bids considered for offering.
	 * @return bid to be offered to opponent.
	 */
	@Override
	public BidDetails getBid(List<BidDetails> allBids) {

		// 1. If there is only a single bid, return this bid
		if (allBids.size() == 1) {
			return allBids.get(0);
		}
		double bestUtil = -1;
		BidDetails bestBid = allBids.get(0);
		
		int amountOfIssues = model.getOpponentUtilitySpace().getDomain().getIssues().size();
		BidDetails oppBid = negotiationSession.getOpponentBidHistory()
							.getHistory()
							.get(negotiationSession.getOpponentBidHistory().size() - 1);
		
		
		// 2. Check that not all bids are assigned at utility of 0
		// to ensure that the opponent model works. If the opponent model
		// does not work, offer a random bid.
		boolean allWereZero = true;
		
		// 3. Determine the best bid
		for (BidDetails bid : allBids) {
			double evaluation = model.getBidEvaluation(bid.getBid());
			double hammingDist = calcHammingDist(bid, oppBid, amountOfIssues);
			double utility = (weightHammingDist * (1 -hammingDist) + evaluation)
							/ (weightHammingDist + 1);
			
			if (evaluation > 0.0001) {
				allWereZero = false;
			}
			if (utility > bestUtil) {
				bestBid = bid;
				bestUtil = utility;
			}
		}
		// 4. The opponent model did not work, therefore, offer a random bid.
		if (allWereZero) {
			Random r = new Random();
			return allBids.get(r.nextInt(allBids.size()));
		}
		return bestBid;
	}

	/**
	 * The opponent model may be updated, unless the time is higher than a given
	 * constant.
	 * 
	 * @return true if model may be updated.
	 */
	@Override
	public boolean canUpdateOM() {
		return negotiationSession.getTime() < updateThreshold;
	}

	@Override
	public Set<BOAparameter> getParameterSpec() {
		Set<BOAparameter> set = new HashSet<BOAparameter>();
		set.add(new BOAparameter("t", 1.1, "Time after which the OM should not be updated"));
		set.add(new BOAparameter("w", 3.0, "Weight of Hamming Distance in deciding the best bid"));
		return set;
	}

	@Override
	public String getName() {
		return "Group 9 OMS BestBid";
	}
	
	/**
	 * calculate the Hamming Distance between bid1 and bid2,
	 * which is how many different values among all issues between them
	 * 
	 * @param bid1
	 * @param bid2
	 * @param amountOfIssues
	 * @return
	 */
	private double calcHammingDist(BidDetails bid1, BidDetails bid2, int amountOfIssues) {
		int diff = 0;
		
		//1. calculate how many different values between two bids
		for (Issue i : model.getOpponentUtilitySpace().getDomain().getIssues()) {
			Value v1 = bid1.getBid().getValue(i.getNumber());
			Value v2 = bid2.getBid().getValue(i.getNumber());
			if (!v1.equals(v2)) {
				diff++;
			}
		}
		
		//2. normalize HammingDistance
		return (double)diff / amountOfIssues;
	}
}