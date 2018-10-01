package ai2018.group9;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import genius.core.bidding.BidDetails;
import genius.core.boaframework.BOAparameter;
import genius.core.boaframework.NegotiationSession;
import genius.core.boaframework.NoModel;
import genius.core.boaframework.OMStrategy;
import genius.core.boaframework.OfferingStrategy;
import genius.core.boaframework.OpponentModel;
import genius.core.boaframework.SortedOutcomeSpace;

public class Group9_BS extends OfferingStrategy {

	/** Minimum target utility */
	private double Pmin;
	
	private double Pmax;
	/** Minimum target utility */
	
	/** Concession factor, the exponent */
	private double e;
	
	/** 
	 * We divide the whole time t into two phase
	 * Phase I [0, t/n]: offer bids with increasing utility
	 * Phase II (t/n, 1]: offer bids with decreasing utility
	 */
	private double n;
	
	/** Outcome space */
	private SortedOutcomeSpace outcomespace;

	/**
	 * Method which initializes the agent by setting all parameters.
	 */
	@Override
	public void init(NegotiationSession negoSession, OpponentModel model, OMStrategy oms,
			Map<String, Double> parameters) throws Exception {
		super.init(negoSession, parameters);
		this.negotiationSession = negoSession;

		outcomespace = new SortedOutcomeSpace(negotiationSession.getUtilitySpace());
		negotiationSession.setOutcomeSpace(outcomespace);

		if (parameters.get("e") != null) {
			this.e = parameters.get("e");
		} else {
			this.e = 1/2;
		}

		if (parameters.get("n") != null) {
			this.n = parameters.get("n");
		} else {
			this.n = 3;
		}

		if (parameters.get("min") != null) {
			this.Pmin = parameters.get("min");
		} else {
			this.Pmin = negoSession.getMinBidinDomain().getMyUndiscountedUtil();
		}

		if (parameters.get("max") != null) {
			Pmax = parameters.get("max");
		} else {
			BidDetails maxBid = negoSession.getMaxBidinDomain();
			Pmax = maxBid.getMyUndiscountedUtil();
		}

		this.opponentModel = model;
		this.omStrategy = oms;
	}

	@Override
	public BidDetails determineOpeningBid() {
		return determineNextBid();
	}

	/**
	 * Simple offering strategy which retrieves the target utility and looks for
	 * the nearest bid if no opponent model is specified. If an opponent model
	 * is specified, then the agent return a bid according to the opponent model
	 * strategy.
	 */
	@Override
	public BidDetails determineNextBid() {
		double time = negotiationSession.getTime();
		double utilityGoal;
		utilityGoal = p(time);

		// System.out.println("[e=" + e + ", Pmin = " +
		// BilateralAgent.round2(Pmin) + "] t = " + BilateralAgent.round2(time)
		// + ". Aiming for " + utilityGoal);

		// if there is no opponent model available
		if (opponentModel instanceof NoModel) {
			nextBid = negotiationSession.getOutcomeSpace().getBidNearUtility(utilityGoal);
		} else {
			nextBid = omStrategy.getBid(outcomespace, utilityGoal);
		}
		return nextBid;
	}

	/**
	 * Makes sure the target utility with in the acceptable range according to
	 * the domain Goes from Pmax to Pmin
	 * 
	 * @param t
	 * @return double
	 */
	public double p(double t) {
		if ( t <= 1/n) {
			//for phase I, we offer bids with increasing utility towards Pmax
			return Pmin + (Pmax - Pmin) * n * t;
		} else {
			//for phase II, we offer bids with decreasing utility towards Pmin
			return Pmax - (Pmax - Pmin) * Math.pow(t, e);
		}
	}

	public NegotiationSession getNegotiationSession() {
		return negotiationSession;
	}

	@Override
	public Set<BOAparameter> getParameterSpec() {
		Set<BOAparameter> set = new HashSet<BOAparameter>();
		set.add(new BOAparameter("e", 0.5, "Concession rate"));
		set.add(new BOAparameter("n", 3.0, "Time divider"));
		set.add(new BOAparameter("min", 0.8, "Minimum utility"));
		set.add(new BOAparameter("max", 1.0, "Maximum utility"));

		return set;
	}

	@Override
	public String getName() {
		return "Group9 Bidding Strategy";
	}
}
