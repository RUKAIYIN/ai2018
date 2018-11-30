package ai2018.group9;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import genius.core.Bid;
import genius.core.bidding.BidDetails;
import genius.core.boaframework.BOAparameter;
import genius.core.boaframework.NegotiationSession;
import genius.core.boaframework.OpponentModel;
import genius.core.issue.Issue;
import genius.core.issue.IssueDiscrete;
import genius.core.issue.Objective;
import genius.core.issue.Value;
import genius.core.issue.ValueDiscrete;
import genius.core.utility.AdditiveUtilitySpace;
import genius.core.utility.Evaluator;
import genius.core.utility.EvaluatorDiscrete;

/**
 * BOA framework implementation of the HardHeaded Frequecy Model.
 * 
 * Default: learning coef l = 0.2; learnValueAddition v = 1.0
 * 
 * paper: https://ii.tudelft.nl/sites/default/files/boa.pdf
 */
/**
 * @author mango
 *
 */
public class Group9_OM extends OpponentModel {

	/*
	 * the learning coefficient is the weight that is added each turn to the
	 * issue weights which changed. It's a trade-off between concession speed
	 * and accuracy.
	 */
	private double learnCoef;
	/*
	 * value which is added to a value if it is found. Determines how fast the
	 * value weights converge.
	 */
	private int learnValueAddition;
	private int amountOfIssues;
	private double goldenValue;
	private int bidsToCheck;

	@Override
	public void init(NegotiationSession negotiationSession, Map<String, Double> parameters) {
		this.negotiationSession = negotiationSession;
		if (parameters != null && parameters.get("l") != null) {
			learnCoef = parameters.get("l");
		} else {
			learnCoef = 0.2;
		}
		learnValueAddition = 1;
		opponentUtilitySpace = (AdditiveUtilitySpace) negotiationSession
				.getUtilitySpace().copy();
		bidsToCheck = countMaxIssueValues(opponentUtilitySpace);

		amountOfIssues = opponentUtilitySpace.getDomain().getIssues().size();
		/*
		 * This is the value to be added to weights of unchanged issues before
		 * normalization. Also the value that is taken as the minimum possible
		 * weight, (therefore defining the maximum possible also).
		 */
		goldenValue = learnCoef / amountOfIssues;

		initializeModel();

	}

	@Override
	public void updateModel(Bid opponentBid, double time) {
		if (negotiationSession.getOpponentBidHistory().size() < 2) {
			return;
		}
		int numberOfUnchanged = 0;
		BidDetails oppBid = negotiationSession.getOpponentBidHistory()
				.getHistory()
				.get(negotiationSession.getOpponentBidHistory().size() - 1);
		BidDetails prevOppBid = negotiationSession.getOpponentBidHistory()
				.getHistory()
				.get(negotiationSession.getOpponentBidHistory().size() - 2);
		HashMap<Integer, Integer> lastDiffSet = determineDifference(prevOppBid,	oppBid);

		// Get the last x bids the opponent made
		BidDetails[] lastBids = getLastBids(bidsToCheck);
		// Count how many distinct values have been bid for each issue in the last x bids
		HashMap<Integer, Integer> distinctBidsPerIssue = CountValues(lastBids);
		// HashMap to calculate how much to increment each issue according to last x bids
		HashMap<Integer, Double> addToIssue = new HashMap<Integer, Double>();

		double timeLeft = 1.0 - time;

		// count the number of changes in value
		for (Integer i : lastDiffSet.keySet()) {
			if (lastDiffSet.get(i) == 0) {
				numberOfUnchanged++;
				// Issue unchanged, we don't want to increment more
				addToIssue.put(i, 0.0);
			} else {
				// Issue i should be incremented by a value according to num distinct bids in last x bids
				// Also have time factor since more importance is in the beginning
				addToIssue.put(i, (1.0 - (double)(distinctBidsPerIssue.get(i))/amountOfIssues)*timeLeft);
			}
		}

		//Put more importance on the changes in the beginning, so multiply with timeleft
		double addValue = goldenValue*timeLeft;

		// The total sum of weights before normalization.
		double totalSum = 1D + addValue * numberOfUnchanged;
		// The maximum possible weight
		double maximumWeight = 1D - (amountOfIssues) * addValue / totalSum;

		// re-weighing issues while making sure that the sum remains 1
		for (Integer i : lastDiffSet.keySet()) {
			Objective issue = opponentUtilitySpace.getDomain()
					.getObjectivesRoot().getObjective(i);
			double weight = opponentUtilitySpace.getWeight(i);
			double newWeight;

			if (lastDiffSet.get(i) == 0 && weight < maximumWeight) {
				newWeight = (weight + addValue + addValue*addToIssue.get(i)) / totalSum;
			} else {
				newWeight = weight / totalSum;
			}
			opponentUtilitySpace.setWeight(issue, newWeight);
		}

		// Then for each issue value that has been offered last time, a constant
		// value is added to its corresponding ValueDiscrete.
		try {
			for (Entry<Objective, Evaluator> e : opponentUtilitySpace
					.getEvaluators()) {
				EvaluatorDiscrete value = (EvaluatorDiscrete) e.getValue();
				IssueDiscrete issue = ((IssueDiscrete) e.getKey());
				/*
				 * add constant learnValueAddition to the current preference of
				 * the value to make it more important
				 */
				ValueDiscrete issuevalue = (ValueDiscrete) oppBid.getBid()
						.getValue(issue.getNumber());
				Integer eval = value.getEvaluationNotNormalized(issuevalue);
				value.setEvaluation(issuevalue, (learnValueAddition + eval));
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}
	
	
	/**
	 * Returns the maximum # of values among all issues
	 * 
	 * @param oppSpace
	 * @return
	 */
	private int countMaxIssueValues(AdditiveUtilitySpace oppSpace) {
		int max = 0;
		try {
			for (Issue i : oppSpace.getDomain().getIssues()) {
				IssueDiscrete id = (IssueDiscrete) i;
				if (id.getNumberOfValues() > max) {
					max = id.getNumberOfValues();
				}
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		// In case we could not cast issues to issuediscrete, set max to 5
		if (max == 0) {
			max = 5;
		}
		return max;
	}

	/**
	 * Get last x bids opponent made
	 * @param x
	 * @return
	 */
	private BidDetails[]  getLastBids(Integer x) {
		BidDetails[] bidDetails = new BidDetails[x];
		int bidSize = negotiationSession.getOpponentBidHistory().size();
		if(bidSize < x + 1) {
			return new BidDetails[0];
		}
		for(int counter = 0; counter < x; counter++) {
			BidDetails bidDetail = negotiationSession.getOpponentBidHistory()
				.getHistory().get(bidSize - 1 - counter);
			bidDetails[counter] = bidDetail;
		}
		return bidDetails;
	}

	/**
	 * Count number of distinct values per issue for each BidDetail
	 * @param bidDetails
	 * @return
	 */
	private HashMap<Integer, Integer> CountValues(BidDetails[] bidDetails) {

		HashMap<Integer, Integer> count = new HashMap<Integer, Integer>();
		try {	
			for (Issue i : opponentUtilitySpace.getDomain().getIssues()) {
				Set<String> issueSet = new HashSet<String>();
				for(int counter = 0; counter < bidDetails.length; counter++) {
					Value val = bidDetails[counter].getBid().getValue(i.getNumber());
					issueSet.add(val.toString());
				}
				count.put(i.getNumber(), issueSet.size());
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		return count;
	}

	@Override
	public double getBidEvaluation(Bid bid) {
		double result = 0;
		try {
			result = opponentUtilitySpace.getUtility(bid);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return result;
	}

	@Override
	public String getName() {
		return "Group9 Opponent Model";
	}

	public double getLearnCoef() {
		return learnCoef;
	}

	public int getLearnValueAddition() {
		return learnValueAddition;
	}

	public int getAmountOfIssues() {
		return amountOfIssues;
	}

	public double getGoldenValue() {
		return goldenValue;
	}

	public int getBidsToCheck() {
		return bidsToCheck;
	}

	@Override
	public Set<BOAparameter> getParameterSpec() {
		Set<BOAparameter> set = new HashSet<BOAparameter>();
		set.add(new BOAparameter("l", 0.2,
				"The learning coefficient determines how quickly the issue weights are learned"));
		return set;
	}

	/**
	 * Init to flat weight and flat evaluation distribution
	 */
	private void initializeModel() {
		double commonWeight = 1D / amountOfIssues;

		for (Entry<Objective, Evaluator> e : opponentUtilitySpace
				.getEvaluators()) {

			opponentUtilitySpace.unlock(e.getKey());
			e.getValue().setWeight(commonWeight);
			try {
				// set all value weights to one (they are normalized when
				// calculating the utility)
				for (ValueDiscrete vd : ((IssueDiscrete) e.getKey())
						.getValues())
					((EvaluatorDiscrete) e.getValue()).setEvaluation(vd, 1);
			} catch (Exception ex) {
				ex.printStackTrace();
			}
		}
	}

	/**
	 * Determines the difference between bids. For each issue, it is determined
	 * if the value changed. If this is the case, a 1 is stored in a hashmap for
	 * that issue, else a 0.
	 * 
	 * @param a
	 *            bid of the opponent
	 * @param another
	 *            bid
	 * @return
	 */
	private HashMap<Integer, Integer> determineDifference(BidDetails first,
			BidDetails second) {

		HashMap<Integer, Integer> diff = new HashMap<Integer, Integer>();
		try {
			for (Issue i : opponentUtilitySpace.getDomain().getIssues()) {
				Value value1 = first.getBid().getValue(i.getNumber());
				Value value2 = second.getBid().getValue(i.getNumber());
				diff.put(i.getNumber(), (value1.equals(value2)) ? 0 : 1);
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}

		return diff;
	}

}