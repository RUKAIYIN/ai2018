package ai2018.group9;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import genius.core.Bid;
import genius.core.Domain;
import genius.core.issue.Issue;
import genius.core.issue.IssueDiscrete;
import genius.core.issue.Objective;
import genius.core.issue.Value;
import genius.core.issue.ValueDiscrete;
import genius.core.uncertainty.AdditiveUtilitySpaceFactory;
import genius.core.uncertainty.BidRanking;
import genius.core.utility.AbstractUtilitySpace;
import genius.core.utility.AdditiveUtilitySpace;
import genius.core.utility.Evaluator;
import genius.core.utility.EvaluatorDiscrete;

/**
 * This class uses similar method in our OM to
 * estimate the utility space given uncertainty profile 
 * 
 * @author Group 9
 */
public class Group9_UtilitySpaceFactory{
	
	private AdditiveUtilitySpace u;
	
	/**
	 * parameters to estimate utility space under uncertainty
	 * @see Group9_OM
	 */
	private double learnCoef;
	private int learnValueAddition;
	private int amountOfIssues;
	private double goldenValue;
	private int bidsToCheck;
	
	public Group9_UtilitySpaceFactory(Domain domain) {
		// Generates an simple Utility Space on the domain, with equal weights and zero values.
		// Everything is zero-filled to already have all keys contained in the utility maps.
		List<Issue> issues = domain.getIssues();
		int noIssues = issues.size();
		Map<Objective, Evaluator> evaluatorMap = new HashMap<Objective, Evaluator>();
		for (Issue i : issues) {
			IssueDiscrete issue = (IssueDiscrete) i;
			EvaluatorDiscrete evaluator = new EvaluatorDiscrete();
			evaluator.setWeight(1.0 / noIssues);
			for (ValueDiscrete value : issue.getValues()) {
				evaluator.setEvaluationDouble(value, 0.0);
			}
			evaluatorMap.put(issue, evaluator);
		}
				
		u = new AdditiveUtilitySpace(domain, evaluatorMap);
		
		// Initiate utility space parameters
		initUSParameters();
	}
	
	
	/**
	 * Initialize parameters that are used to estimate utility space 
	 * Default values refer to those in Group9_OM
	 * 
	 * @param 
	 */
	private void initUSParameters() {
		learnCoef = 0.2;
		learnValueAddition = 1;
		amountOfIssues = u.getDomain().getIssues().size();
		goldenValue = learnCoef / amountOfIssues;
		bidsToCheck  = countMaxIssueValues(u);
	}
	
	/**
	 * Returns the maximum # of values among all issues
	 * 
	 * @param abstractUtilitySpace
	 * @return
	 */
	private int countMaxIssueValues(AbstractUtilitySpace abstractUtilitySpace) {
		int max = 0;
		for (Issue i : abstractUtilitySpace.getDomain().getIssues()) {
			IssueDiscrete id = (IssueDiscrete) i;
			if (id.getNumberOfValues() > max) {
				max = id.getNumberOfValues();
			}
		}
		return max;
	}

	/**
	 * Estimates the utility space given bid ranking which contains
	 * all possible bids from low to high utility
	 * 
	 * @param r
	 */
	public void estimateUsingBidRanks(BidRanking r) {
		if (r.getSize() < bidsToCheck) {
			simpleEstimateUsingBidRanks(r);
		}
		
		// From high-ranked bid to low-ranked bid,
		// add weight to issues that remain unchanged
		// and evaluate values
		for (int i = r.getSize() - 2; i >= 0; --i) {
			
			Bid bid = r.getBidOrder().get(i);
			Bid prevBid = r.getBidOrder().get(i + 1);

			int numberOfUnchanged = 0;
			
			HashMap<Integer, Integer> lastDiffSet = determineDifference(prevBid, bid);

			// Get the last x bids the opponent made
			Bid[] lastBids = getLastBids(i, bidsToCheck, r.getBidOrder());
			
			// Count how many distinct values have been bid for each issue in the last x bids
			HashMap<Integer, Integer> distinctBidsPerIssue = CountValues(lastBids);
			
			// HashMap to calculate how much to increment each issue according to last x bids
			HashMap<Integer, Double> addToIssue = new HashMap<Integer, Double>();
			
			//count # of changes in value
			for (int j : lastDiffSet.keySet()) {
				if (0 == lastDiffSet.get(j)) {
					numberOfUnchanged++;

					// Issue unchanged, we don't want to increment more
					addToIssue.put(j, 0.0);
				} else {
					try {
						// Incremented by a value according to # of distinct bids in last x bids
						addToIssue.put(j, 
								(1.0 - (double)(distinctBidsPerIssue.get(j)) / amountOfIssues));
					} catch (NullPointerException e) {
						// Do nothing
					}
				}
			}
			
			// The total sum of weights before normalization.
			double totalSum = 1D + goldenValue * numberOfUnchanged;
			// The maximum possible weight
			double maximumWeight = 1D - (amountOfIssues) * goldenValue / totalSum;
			
			// Re-weighing issues while making sure that the sum remains 1
			for (Integer k : lastDiffSet.keySet()) {
				Objective issue = u.getDomain().getObjectivesRoot().getObjective(k);
				double weight = u.getWeight(k);
				double newWeight;

				if (lastDiffSet.get(k) == 0 && weight < maximumWeight) {
					newWeight = (weight + goldenValue 
							+ goldenValue * addToIssue.get(k))
							/ totalSum;
				} else {
					newWeight = weight / totalSum;
				}
				u.setWeight(issue, newWeight);
			}
			
			// Then for each issue value that has been offered last time, a constant
			// value is added to its corresponding ValueDiscrete.
			try {
				for (Entry<Objective, Evaluator> e : u.getEvaluators()) {
					EvaluatorDiscrete value = (EvaluatorDiscrete) e.getValue();
					IssueDiscrete issue = ((IssueDiscrete) e.getKey());
					/*
					 * add constant learnValueAddition to the current preference of
					 * the value to make it more important
					 */
					ValueDiscrete issuevalue = (ValueDiscrete) bid.getValue(issue.getNumber());
					Integer eval = value.getEvaluationNotNormalized(issuevalue);
					value.setEvaluation(issuevalue, (learnValueAddition + eval));
				}
			} catch (Exception ex) {
				ex.printStackTrace();
			}
			
		} //end for loop of bid ranking
		
	}
	
	
	/**
	 * A simple heuristic for estimating a discrete {@link AdditiveUtilitySpace} from a {@link BidRanking}.
	 * Gives 0 points to all values occurring in the lowest ranked bid, 
	 * then 1 point to all values occurring in the second lowest bid, and so on.
	 * 
	 * @see AdditiveUtilitySpaceFactory
	 */
	public void simpleEstimateUsingBidRanks(BidRanking r)
	{
		double points = 0;
		for (Bid b : r.getBidOrder())
		{
			List<Issue> issues = b.getIssues();
			for (Issue i : issues)
			{
				int no = i.getNumber();
				ValueDiscrete v = (ValueDiscrete) b.getValue(no);
				double oldUtil = getUtility(i, v);
				setUtility(i, v, oldUtil + points);
			}
			points += 1;
		}
		normalizeWeightsByMaxValues();
	}
	
	/**
	 * Normalize issue weights by max values
	 * 
	 * @see AdditiveUtilitySpaceFactory
	 */
	private void normalizeWeightsByMaxValues()
	{
		for (Issue i : getIssues())
		{
			EvaluatorDiscrete evaluator = (EvaluatorDiscrete) u.getEvaluator(i);
			evaluator.normalizeAll();
		}
		for (Issue i : getIssues())
		{
			EvaluatorDiscrete evaluator = (EvaluatorDiscrete) u.getEvaluator(i);
			evaluator.scaleAllValuesFrom0To1();
		}
		u.normalizeWeights();
	}
	
	/**
	 * @see AdditiveUtilitySpaceFactory
	 * Sets e_i(v) := value 
	 * 
	 * @param i
	 * @param v
	 * @param value
	 */
	private void setUtility(Issue i, ValueDiscrete v, double value)
	{
		EvaluatorDiscrete evaluator = (EvaluatorDiscrete) u.getEvaluator(i);
		if (evaluator == null)
		{
			evaluator = new EvaluatorDiscrete();
			u.addEvaluator(i, evaluator);
		}
		evaluator.setEvaluationDouble(v, value);
	}
	
	/**
	 * Get the utility of a issue value
	 * @param i
	 * @param v
	 * @return
	 */
	private double getUtility(Issue i, ValueDiscrete v)
	{
		EvaluatorDiscrete evaluator = (EvaluatorDiscrete) u.getEvaluator(i);
		return evaluator.getDoubleValue(v);
	}
	
	/**
	 * Returns all issues in the domain
	 * @return List<Issue>
	 */
	private List<Issue> getIssues() 
	{
		return getDomain().getIssues();
	}
	
	private Domain getDomain() {
		return u.getDomain();
	}
	
	/**
	 * Count number of distinct values per issue for each Bid
	 * @param bids
	 * @return
	 */
	private HashMap<Integer, Integer> CountValues(Bid[] bids) {

		HashMap<Integer, Integer> count = new HashMap<Integer, Integer>();
		try {	
			for (Issue i : u.getDomain().getIssues()) {
				Set<String> issueSet = new HashSet<String>();
				for(int counter = 0; counter < bids.length; counter++) {
					if (null != bids[counter]) {
						Value val = bids[counter].getValue(i.getNumber());
						issueSet.add(val.toString());
					}
				}
				count.put(i.getNumber(), issueSet.size());
			}
		} catch (Exception ex) {
			// Do nothing
		}
		
		return count;
	}

	/**
	 * Get last x bids before base in bid ranking
	 * 
	 * @param x
	 * @param allBids
	 * @return Bid[]
	 */
	private Bid[] getLastBids(int base, int x, List<Bid> allBids) {
		int bidSize = allBids.size();
		
		if(bidSize < x + 1) {
			return new Bid[0];
		}

		Bid[] bids = new Bid[x];
		for(int counter = 0; 
				((counter < x) && (base + counter) <= (allBids.size() - 1)); 
				counter++) {
			Bid bid = allBids.get(base + counter);
			bids[counter] = bid;
		}
		return bids;
	}
	

	/**
	 * Determines the difference between bids. For each issue, it is determined
	 * if the value changed. If this is the case, a 1 is stored in a hashmap for
	 * that issue, else a 0.
	 * 
	 * @param first
	 * @param second
	 * @return HashMap<Integer, Integer>
	 */
	private HashMap<Integer, Integer> determineDifference(Bid first,
			Bid second) {

		HashMap<Integer, Integer> diff = new HashMap<Integer, Integer>();
		try {
			for (Issue i : u.getDomain().getIssues()) {
				Value value1 = first.getValue(i.getNumber());
				Value value2 = second.getValue(i.getNumber());
				diff.put(i.getNumber(), (value1.equals(value2)) ? 0 : 1);
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}

		return diff;
	}
	
	/**
	 * Returns the utility space that has been created.
	 */
	public AdditiveUtilitySpace getUtilitySpace() 
	{
		return u;
	}

	public void setLearnCoef(double learnCoef) {
		this.learnCoef = learnCoef;
	}


	public void setLearnValueAddition(int learnValueAddition) {
		this.learnValueAddition = learnValueAddition;
	}
}
