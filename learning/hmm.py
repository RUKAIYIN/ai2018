# import sys
import numpy as np
import json
import os
# from pprint import pprint
from collections import Counter



class AbstractHmm:

    def getTransitionMatrix(self):
        ''' Access the transition Matrix '''
        return self.transitionMatrix



    def getStateToIndex(self, stateName):
        ''' Return the index of the given state '''
        return self.states[stateName]


   
    def getObservationToIndex(self, observationName):
        ''' Return the index of the given observation'''
        return self.observations[observationName]


  
    def getObservationMatrix(self):
        ''' Return the matrix with the probabilities of observing each
            observation depending on each state
        The rows are the states and the columns the possible observations '''
       
        return self.observationMatrix


    
    def getInitialStates(self):
        ''' Return a vector with the probabilities of starting in each state
            Uniform for now, if we need more complex starting position,
               I will change it'''

        return self.initStates


    
    # Definition and creation of everything that is project-dependant

    
    def createMappingStateToIndex(self):
        ''' Create a mapping from the state name to an index '''
        
        pass
    
    

    def createMappingObservationToIndex(self):
        ''' Create a mapping from the state name to an index '''
        pass
        

    def createTransitionMatrix(self):
        ''' Create the transition matrix of the project '''
        pass
        
        
    def createObservationMatrix(self):
        ''' Create the observation matrix in a uniform way
            Rows are states and columns observations '''
        pass
        

    def createInitStates(self):
        ''' Create the vector of probabilities to start in each state '''
        pass


    def learningModel(self):
        ''' Learn the model necessary for the problem at hand '''
        pass




    



class Hmm(AbstractHmm):
    
    def __init__(self):  #, initStates, possibleObservations, transitionMatrix):
        ''' initStates is a dictionnary with {'state1':prob, 'state2':prob}
            possibleObservations is a list with the name of the possible
                            observations ['observation1','observation2']
            Transition matrix
            TO REDO BECAUSE BULLSHIT
        '''
        
        # Tell if the model has been trained or not
        self.trained = False

        # First we build a mapping for the States to indexes
        self.createMappingStateToIndex()
        #print(self.states)

        # Then we store the initial matrix state
        self.createInitStates()
        #print(self.initStates)

        # We create the transition Matrix
        self.createTransitionMatrix()


        # We know deals with the observations in the same way
        self.createMappingObservationToIndex()
        #print(self.observations)

        
        # We create an observation matrix that is uniformly distributed
        # The rows of the matrix correspond to the states
        # The columns correspond to the observations
        
        self.createObservationMatrix()
        #print(self.observationMatrix)



    # Definition and creation of everything that is project-dependant

    
    def createMappingStateToIndex(self):
        ''' Create a mapping from the state name to an index '''
        
        listStates = ['conceder', 'hardheaded', 'tft', 'random']
        self.states = {listStates[i]: i for i in range(len(listStates))}
    

    def createMappingObservationToIndex(self):
        ''' Create a mapping from the state name to an index '''

        listObservations = ['nice', 'fortunate', 'unfortunate', 'selfish', 'silent', 'concession']
        self.observations = {listObservations[i]: i for i in range(len(listObservations))}
        

    def createTransitionMatrix(self):
        ''' Create the transition matrix of the project '''
        self.transitionMatrix = np.identity(len(self.states))
        
        
    def createObservationMatrix(self):
        ''' Create the observation matrix in a uniform way
            Rows are states and columns observations '''

        self.observationMatrix = np.zeros((len(self.states),len(self.observations)))

        # We create the uniformly distributed matrix
        const = 1 / len(self.observations)
        for i in range(len(self.states)):
            for j in range(len(self.observations)):
                self.observationMatrix[i,j] = const
        

    def createInitStates(self):
        ''' Create the vector of probabilities to start in each state '''

        self.initStates = []
        for i in range(len(self.states)):
            self.initStates.append(1 / len(self.states))



            
    ###################### LEARNING ################################

            
    def learningModel(self,dirLogs):
        ''' Learn a model through training logs with a simple counting method and update the observation matrix '''

        training_dir = dirLogs
        parties = {'conceder': [], 'random': [], 'hardheaded': [], 'tft': []}
        thresh = 0.005

        def init_moves():
            return {
                'fortunate': 0,
                'selfish': 0,
                'concession': 0,
                'unfortunate': 0,
                'nice': 0,
                'silent': 0
            }

        def init_agent(name, prefs):
            return {
                'name': name,
                'prefs': prefs,
                'moves': init_moves(),
                'bids': [],
            }

        def read_json_files():
            sessions = []
            files = os.listdir(training_dir)
            for file in files:
                raw_json = open(training_dir + '/' + file).read()
                sessions.append({'id': file.replace('.json', ''), 'data': json.loads(raw_json)})
            return sessions

        def update_moves(agent, opponent):
            last_u1 = get_bid_util(agent['prefs'], agent['bids'][0])
            last_u2 = get_bid_util(opponent['prefs'], opponent['bids'][0])
            for bid in agent['bids'][1:]:
                u1 = get_bid_util(agent['prefs'], bid)
                u2 = get_bid_util(opponent['prefs'], bid)
                u1_diff = u1 - last_u1
                u2_diff = u2 - last_u2
                if abs(u1_diff) < thresh:
                    if abs(u2_diff) < thresh:
                        agent['moves']['silent'] += 1
                    elif u2_diff > 0:
                        agent['moves']['nice'] += 1
                    else:
                        if u1_diff >= 0:
                            agent['moves']['selfish'] += 1
                        else:
                            agent['moves']['unfortunate'] += 1
                elif u1_diff >= thresh:
                    if u2_diff >= 0:
                        agent['moves']['fortunate'] += 1
                    else:
                        agent['moves']['selfish'] += 1
                else:
                    if u2_diff >= 0:
                        agent['moves']['concession'] += 1
                    else:
                        agent['moves']['unfortunate'] += 1
                last_u1 = u1
                last_u2 = u2
    
            number_of_bids =len(agent['bids'])-1
            agent['moves']['silent'] = agent['moves']['silent']/number_of_bids
            agent['moves']['nice'] = agent['moves']['nice']/number_of_bids
            agent['moves']['fortunate'] = agent['moves']['fortunate']/number_of_bids
            agent['moves']['selfish'] = agent['moves']['selfish']/number_of_bids
            agent['moves']['concession'] = agent['moves']['concession']/number_of_bids
            agent['moves']['unfortunate'] = agent['moves']['unfortunate']/number_of_bids

        def calc_session(session):
            session_id = ''.join([i for i in session['id'] if not i.isdigit()])
            session_data = session['data']
            agent1_name, agent2_name = session_id.split('_')

            agent1 = init_agent(agent1_name, session_data['Utility1'])
            agent2 = init_agent(agent2_name, session_data['Utility2'])
            agent1['bids'] = [x['agent1'] for x in session_data['bids'] if 'agent1' in x]
            agent2['bids'] = [x['agent2'] for x in session_data['bids'] if 'agent2' in x]

            update_moves(agent1, agent2)
            update_moves(agent2, agent1)

            parties[agent1['name']].append(agent1['moves'])
            parties[agent2['name']].append(agent2['moves'])

        
        def get_bid_util(agent_util, bid):
            u = 0
            fruit, juice, topping1, topping2 = bid.split(',')
            u += agent_util['Fruit'][str(fruit)]*agent_util['Fruit']['weight']
            u += agent_util['Juice'][str(juice)]*agent_util['Juice']['weight']
            u += agent_util['Topping1'][str(topping1)]*agent_util['Topping1']['weight']
            u += agent_util['Topping2'][str(topping2)]*agent_util['Topping2']['weight']
            return u
        
        sessions = read_json_files()
        for session in sessions:
            calc_session(session)
        for agent in parties:
            tmp_moves = {}
            for moves in parties[agent]:
                tmp_moves = Counter(tmp_moves) + Counter(moves)
            avg_moves = init_moves()
            avg_moves.update(tmp_moves)
            parties[agent] = {k: v/len(parties[agent]) for k,v in avg_moves.items()}
        #pprint(parties)

        for strat in parties:
            for move in parties[strat]:
                #print(strat, move, parties[strat][move])
                self.observationMatrix[self.getStateToIndex(strat),self.getObservationToIndex(move)] = parties[strat][move]
        #print(self.observationMatrix)
        



            
########################### NO MORE LEARNING ################################


    ############### FILTERING ###############

    def filteringComputation(self, previousBeliefs, currentEvidence):
        ''' Calculate the filtering using forward message '''

        # Calculate P(X_{t+1}) from the results of previous calculation
        currentBelief = [0 for i in range(len(self.states))]
        for Xt in range(len(self.states)):
            # We generate the vector of going from one state to another
            transVector = self.getTransitionMatrix()[Xt,:]
            beliefOfXt = previousBeliefs[Xt]

            # We multiply each element of the vector by the belief of Xt (we use numpy for that)
            transVector * beliefOfXt

            # We add that the product to our beliefs
            currentBelief = [currentBelief[i] + transVector[i] for i in range(len(self.states))]

        # We now generate the probability of seeing the evidence for each state
        evidenceProba = self.getObservationMatrix()[:, self.getObservationToIndex(currentEvidence)]

        # We multiply one with the other and send the result.
        result = [evidenceProba[i] * currentBelief[i] for i in range(len(self.states))]

        #We normalize the results
        result = [result[i] / sum(result) for i in range(len(result))]

        return result





    def filteringHmm(self, logToFilter):
        ''' Apply filtering to an hmm, using test log '''

        def get_bid_util(agent_util, bid):
            u = 0
            fruit, juice, topping1, topping2 = bid.split(',')
            u += agent_util['Fruit'][str(fruit)]*agent_util['Fruit']['weight']
            u += agent_util['Juice'][str(juice)]*agent_util['Juice']['weight']
            u += agent_util['Topping1'][str(topping1)]*agent_util['Topping1']['weight']
            u += agent_util['Topping2'][str(topping2)]*agent_util['Topping2']['weight']
            return u

        def typeOfMoves(currentBid, previousBid, prefBiddingAgent, prefReceivingAgent):
            ''' Return the type of move of the bid according to the preferences of both agent '''

            # Compute twice the utility of each bid, not really nice but it will work for now
            thresh = 0.005
            u1 = get_bid_util(prefBiddingAgent, currentBid)
            u2 = get_bid_util(prefReceivingAgent, currentBid)
            u1_diff = u1 - get_bid_util(prefBiddingAgent, previousBid)
            u2_diff = u2 - get_bid_util(prefBiddingAgent, previousBid)
            if abs(u1_diff) < thresh:
                if abs(u2_diff) < thresh:
                    return 'silent'
                elif u2_diff > 0:
                    return 'nice'
                else:
                    if u1_diff >= 0:
                        return 'selfish'
                    else:
                        return 'unfortunate'
            elif u1_diff >= thresh:
                if u2_diff >= 0:
                    return 'fortunate'
                else:
                    return 'selfish'
            else:
                if u2_diff >= 0:
                    return 'concession'
                else:
                    return 'unfortunate'



        # We read the data
        raw_json = open(logToFilter,'r').read()
        sessionData = json.loads(raw_json)

        # We store the preferences of both agent
        prefAgent1 = sessionData['Utility1']
        prefAgent2 = sessionData['Utility2']

        # We initialise our filtering values, one for each agent
        filterAgent1 = self.initStates
        filterAgent2 = self.initStates

        # We go through all the bids
        bids = sessionData['bids']
        prevBidAgent1 = bids[0]['agent1']
        prevBidAgent2 = bids[0]['agent2']

        for bidRound in bids[1:-1]:

            # We get the bid of agent 1
            bidAgent1 = bidRound['agent1']
            evidence = typeOfMoves(bidAgent1, prevBidAgent1, prefAgent1, prefAgent2)
            prevBidAgent1 = bidAgent1

            # We update our beliefs
            filterAgent1 = self.filteringComputation(filterAgent1, evidence)
            print('filter Agent1, round ',bidRound['round'],' : ',filterAgent1)

             # We get the bid of agent 1
            bidAgent2 = bidRound['agent2']
            evidence = typeOfMoves(bidAgent2, prevBidAgent2, prefAgent2, prefAgent1)
            prevBidAgent2 = bidAgent2

            # We update our beliefs
            filterAgent2 = self.filteringComputation(filterAgent2, evidence)
            print('filter Agent2, round ',bidRound['round'],' : ',filterAgent2)
            


myHmm = Hmm()
myHmm.learningModel('training_logs')
myHmm.filteringHmm('training_logs/conceder_random1.json')
