#!/usr/local/bin/python

from nigori_client_lib import NigoriClient

import simplejson
import sys

def main():
  client = NigoriClient(sys.argv[1], sys.argv[2], sys.argv[3], sys.argv[4])
  rsas = client.getValueList(client.TYPE_RSA, sys.argv[5])
  for rsa in rsas:
    key = simplejson.loads(client.keys.decrypt(rsa['value']))
    print key

if __name__ == "__main__":
  main()
