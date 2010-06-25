#!/usr/local/bin/python

from base64 import urlsafe_b64encode as b64enc, urlsafe_b64decode as b64dec
from Crypto.Cipher import AES
from Crypto.Hash import HMAC
from Crypto.Hash import SHA256
from Crypto.Util import randpool
from Crypto.PublicKey import RSA
from nigori import SchnorrSigner, concat, int2bin, unconcat, bin2int
# FIXME: use this library properly!
from nigori_client_lib import ShamirSplit, KeyDeriver

import httplib
import random
import simplejson
import sys
import time
import urllib

def connect():
  return httplib.HTTPConnection(server, port)

def servername():
  return server + ":" + str(port)

def register(user, password):
  keys = KeyDeriver(user, servername(), password)
  schnorr = keys.schnorr()
  public = b64enc(schnorr.public())
  params = urllib.urlencode({"user": user, "publicKey": public})
  headers = {"Content-Type": "application/x-www-form-urlencoded",
             "Accept": "text/plain" }
  conn = connect()
  conn.request("POST", "/register", params, headers)
  response = conn.getresponse()
  print response.status, response.reason
  print response.read()

def makeAuthParams(user, password):
  # FIXME: include server name, user name in t
  t = "%d:%d" % (int(time.time()), random.SystemRandom().getrandbits(20))
  keys = KeyDeriver(user, servername(), password)
  schnorr = keys.schnorr()
  (e,s) = schnorr.sign(t)
  params = {"user": user,
            "t": t,
            "e": b64enc(e),
            "s": b64enc(s)}
  return params

def do_auth(params):
  params = urllib.urlencode(params)
  headers = {"Content-Type": "application/x-www-form-urlencoded"}
  conn = connect()
  conn.request("POST", "/authenticate", params, headers)
  response = conn.getresponse()
  print response.status, response.reason
  print response.read()

def authenticate(user, password):
  params = makeAuthParams(user, password)
  do_auth(params)
  # test replay attack
  print "Replaying: this should fail"
  do_auth(params)

def baseGetList(user, password, type, name, use_des = 0):
  params = makeAuthParams(user, password)
  keys = KeyDeriver(user, servername(), password, use_des)
  params['name'] = keys.permute(concat([int2bin(type), name]))
  conn = connect()
  conn.request("GET", "/list-resource?" + urllib.urlencode(params))
  response = conn.getresponse()
  if response.status != 200:
    # FIXME: define a ProtocolError, perhaps?
    raise LookupError("HTTP error: %d %s" % (response.status, response.reason))
  json = response.read()
  return simplejson.loads(json)
  
def getList(user, password, name):
  records = baseGetList(user, password, 1, name)
  keys = KeyDeriver(user, servername(), password)
  for record in records:
    value = keys.decrypt(record['value'])
    print "%d at %f: %s" % (record['version'], record['creationTime'], value)

def add(user, password, type, name, value, use_des = 0):
  params = makeAuthParams(user, password)
  keys = KeyDeriver(user, servername(), password, use_des)
  params['name'] = keys.permute(concat([int2bin(type), name]))
  params['value'] = b64enc(keys.encrypt(value))
  params = urllib.urlencode(params)
  headers = {"Content-Type": "application/x-www-form-urlencoded",
             "Accept": "text/plain" }
  conn = connect()
  conn.request("POST", "/add-resource", params, headers)
  response = conn.getresponse()
  print response.status, response.reason
  print response.read()

def getRandomBytes(bytes):
  r = random.SystemRandom().getrandbits
  t = ""
  for n in range(bytes):
    t = t + chr(r(8))
  return t

def addNewRSA(user, password, name):
  rsa = RSA.generate(1024, getRandomBytes)
  key = {'n': rsa.n, 'e': rsa.e, 'd': rsa.d, 'p': rsa.p, 'q': rsa.q, 'u': rsa.u }
  value = simplejson.dumps(key)
  add(user, password, 3, name, value, use_des = 1)
  print "added", value

def getRSA(user, password, name):
  rsas = baseGetList(user, password, 3, name, use_des = 1)
  keys = KeyDeriver(password, 1)
  for rsa in rsas:
    key = simplejson.loads(keys.decrypt(rsa['value']))
    print key

def initSplit(user, password, splits):
  add(user, password, 2, "split servers", concat(splits))

def getSplits(user, password):
  records = baseGetList(user, password, 2, "split servers")
  record = records[-1]
  keys = KeyDeriver(password)
  splits = unconcat(keys.decrypt(record['value']))
  return splits

def splitAdd(user, password, name, value):
  splits = getSplits(user, password)
  k = int(splits[0])
  n = (len(splits) - 1)/2
  assert int(n) == n
  assert k <= n
  splitter = ShamirSplit()
  shares = splitter.share(value, k, n)
  for s in range(n):
    global host, port
    host = splits[2*s + 1]
    port = splits[2*s + 2]
    print "Sending split", s, "to", host + ":" + port
    add(user, password, 1, name, concat([int2bin(s + 1), int2bin(shares[s])]))

def splitGet(user, password, name):
  splits = getSplits(user, password)
  k = int(splits[0])
  n = (len(splits) - 1)/2
  assert int(n) == n
  assert k <= n
  
  keys = KeyDeriver(password)
  shares = {}
  # FIXME: obviously we should try all n until we get k splits
  for s in range(k):
    global host, port
    host = splits[2*s + 1]
    port = splits[2*s + 2]
    print "Getting split", s, "from", host + ":" + port
    records = baseGetList(user, password, 1, name)
    record = records[-1]
    share = unconcat(keys.decrypt(record['value']))
    assert len(share) == 2
    shares[bin2int(share[0])] = bin2int(share[1])

  splitter = ShamirSplit()
  secret = splitter.recover(shares)
  print "value =", secret

def main():
  global server, port
  server = sys.argv[1]
  port = int(sys.argv[2])
  action = sys.argv[3]
  if action == "get":
    getList(sys.argv[4], sys.argv[5], sys.argv[6])
  elif action == "add":
    add(sys.argv[4], sys.argv[5], 1, sys.argv[6], sys.argv[7])
  elif action == "register":
    register(sys.argv[4], sys.argv[5])
  elif action == "authenticate":
    authenticate(sys.argv[4], sys.argv[5])
  elif action == "create-split":
    initSplit(sys.argv[4], sys.argv[5], sys.argv[6:])
  elif action == "split-add":
    splitAdd(sys.argv[4], sys.argv[5], sys.argv[6], sys.argv[7])
  elif action == "split-get":
    splitGet(sys.argv[4], sys.argv[5], sys.argv[6])
  elif action == "add-rsa":
    addNewRSA(sys.argv[4], sys.argv[5], sys.argv[6])
  elif action == "get-rsa":
    getRSA(sys.argv[4], sys.argv[5], sys.argv[6])
  else:
    raise ValueError("Unrecognised action: " + action)

if __name__ == "__main__":
  main()
