import json
import sys

def read_json_file(file):
  raw_json=open(file).read()
  data = json.loads(raw_json)
  return data


if __name__ == "__main__":
    print 'hello'
    data = read_json_file(sys.argv[1])
    print data
