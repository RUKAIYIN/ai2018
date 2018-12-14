import json
import sys

def read_json_file(file):
  raw_json=open(file).read()
  data = json.loads(raw_json)
  return data



'''
  Usage: python learn.py ../training_logs/conceder_conceder.json
  Currently prints out the json file
'''
if __name__ == "__main__":
    data = read_json_file(sys.argv[1])
    print(data)
