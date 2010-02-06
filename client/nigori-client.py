#!/usr/local/bin/python

from base64 import urlsafe_b64encode as b64enc, urlsafe_b64decode as b64dec
from Crypto.Cipher import AES
from Crypto.Hash import HMAC
from Crypto.Hash import SHA256
from Crypto.Util import randpool
from nigori import SchnorrSigner

import httplib
import random
import simplejson
import sys
import time
import urllib

class KeyDeriver:
  def __init__(self, password):
    self.crypt = SHA256.new(password).digest()
    self.mac = SHA256.new(self.crypt).digest()
    self.authenticate = SHA256.new(self.mac).digest()

  def encrypt(self, plain):
    pool = randpool.RandomPool()
    iv = pool.get_bytes(16)
    crypter = AES.new(self.crypt, AES.MODE_CBC, iv)
    pad = 16 - len(plain) % 16
    c = '%c' % pad
    for i in range(pad):
      plain = plain + c
    crypted = crypter.encrypt(plain)
    hmac = HMAC.new(self.mac, crypted)
    crypted = b64enc(iv + crypted + hmac.digest())
    return crypted

  def decrypt(self, crypted):
    crypted = b64dec(crypted)
    l = len(crypted)
    if l < 32:
      raise ValueError("value too short")
    mac = crypted[l-16:]
    iv = crypted[:16]
    crypted = crypted [16:l-16]
    hmac = HMAC.new(self.mac, crypted)
    if mac != hmac.digest():
      raise ValueError("mac doesn't match")
    crypter = AES.new(self.crypt, AES.MODE_CBC, iv)
    plain = crypter.decrypt(crypted)
    c = plain[-1]
    for i in range(-1, -ord(c), -1):
      if plain[i] != c:
        raise ValueError("padding error")
    plain = plain[:-ord(c)]
    return plain

  def schnorr(self):
    return SchnorrSigner(self.authenticate)

def connect():
  return httplib.HTTPConnection("localhost", 8080)

def register(user, password):
  keys = KeyDeriver(password)
  schnorr = keys.schnorr()
  public = schnorr.public()
  params = urllib.urlencode({"user": user, "publicKey": public})
  headers = {"Content-Type": "application/x-www-form-urlencoded",
             "Accept": "text/plain" }
  conn = connect()
  conn.request("POST", "/register", params, headers)
  response = conn.getresponse()
  print response.status, response.reason
  print response.read()

def authenticate(user, password):
  keys = KeyDeriver(password)
  schnorr = keys.schnorr()
  # FIXME: include server name, user name in t
  t = "%d:%d" % (int(time.time()), random.SystemRandom().getrandbits(20))
  (e,s) = schnorr.sign(t)
  params = urllib.urlencode({"user": user,
                             "t": t,
                             "e": b64enc(e),
                             "s": b64enc(s)})
  headers = {"Content-Type": "application/x-www-form-urlencoded"}
  conn = connect()
  conn.request("POST", "/authenticate", params, headers)
  response = conn.getresponse()
  print response.status, response.reason
  print response.read()
  
def getList(password, name):
  conn = connect()
  conn.request("GET", "/list-resource?name=" + name)
  response = conn.getresponse()
#  print response.status, response.reason
  if response.status != 200:
    # FIXME: define a ProtocolError, perhaps?
    raise LookupError("HTTP error: %d %s" % (response.status, response.reason))
  json = response.read()
#  print json
  records = simplejson.loads(json)
  keys = KeyDeriver(password)
  for record in records:
#    print record
    value = keys.decrypt(record['value'])
    print "%d at %f: %s" % (record['version'], record['creationTime'], value)

def add(password, name, value):
  keys = KeyDeriver(password)
  params = urllib.urlencode({"name": name, "value": keys.encrypt(value)})
  headers = {"Content-Type": "application/x-www-form-urlencoded",
             "Accept": "text/plain" }
  conn = connect()
  conn.request("POST", "/add-resource", params, headers)
  response = conn.getresponse()
  print response.status, response.reason
  print response.read()

def main():
  action = sys.argv[1]
  if action == "get":
    getList(sys.argv[2], sys.argv[3])
  elif action == "add":
    add(sys.argv[2], sys.argv[3], sys.argv[4])
  elif action == "register":
    register(sys.argv[2], sys.argv[3])
  elif action == "authenticate":
    authenticate(sys.argv[2], sys.argv[3])
  else:
    raise ValueError("Unrecognised action: " + action)

if __name__ == "__main__":
  main()
