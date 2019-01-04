import json
import os
from pprint import pprint
from collections import Counter

training_dir = '../training_logs'
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
    raw_json=open(training_dir + '/' + file).read()
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

'''
  Usage: python learn.py
  Currently prints out the json file
'''
if __name__ == "__main__":
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
    
    pprint(parties)