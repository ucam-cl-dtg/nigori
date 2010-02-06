#!/usr/local/bin/python

from base64 import urlsafe_b64encode as b64enc, urlsafe_b64decode as b64dec
from Crypto.Cipher import AES
from Crypto.Hash import HMAC
from Crypto.Hash import SHA256
from Crypto.Util import randpool

import httplib
import random
import simplejson
import sys
import time
import urllib

def bin2int(str):
  t = 0
  for i in range(len(str)):
    t = t * 256 + ord(str[i])
  return t

def int2bin(i):
  t = ""
  while i > 0:
    t = t + "%c" % (i % 256)
    i = i / 256
  # return t backwards
  return t[::-1]

def hexdump(str):
  t = ""
  for i in range(len(str)):
    c = hex(ord(str[i]))[2:]
    if len(c) == 1:
      c = "0" + c
    t = t + c
  return t

def lengthOf(x):
  l = len(x)
  t = int2bin(l)
  assert len(t) <= 4
  for i in range(len(t), 4):
    t = ("%c" % 0) + t
  return t

class HashWrapper:
  def __init__(self):
    self.h = SHA256.new()
    
  def add(self, x):
    self.h.update(lengthOf(x))
    self.h.update(x)

  def digest(self):
    return self.h.digest()

class SchnorrSigner:
  def __init__(self, x):
    # Parameters cribbed from OpenSSL's J-PAKE implementation, for now
    self.p = int("fd7f53811d75122952df4a9c2eece4e7f611b7523cef4400c31e3f80b6512669455d402251fb593d8d58fabfc5f5ba30f6cb9b556cd7813b801d346ff26660b76b9950a5a49f9fe8047b1022c24fbba9d7feb7c61bf83b57e7c6a8a6150f04fb83f6d3c51ec3023554135a169132f675f3ae2b61d72aeff22203199dd14801c7", 16)
    self.q = int("9760508f15230bccb292b982a2eb840bf0581cf5", 16)
    self.g = int("f7e1a085d69b3ddecbbcab5c36b857b97994afbbfa3aea82f9574c0b3d0782675159578ebad4594fe67107108180b449167123e84c281613b7cf09328cc8a6e13c167a8b547c8d28e0a3ae1e2bb3a675916ea37f0bfa213562f1fb627a01243bcca4f1bea8519089a883dfe15ae59f06928b665e807b552564014c3bfecf492a", 16)
    self.x = bin2int(x)

  def public(self):
    return b64enc(int2bin(pow(self.g, self.x, self.p)))

  def sign(self, message):
    k = random.SystemRandom().randrange(self.q)
    r = pow(self.g, k, self.p)
    h = HashWrapper()
    h.add(message)
    h.add(int2bin(r))
    e = h.digest()
    s = (k - self.x * bin2int(e)) % self.q
    signature = (b64enc(e),  b64enc(int2bin(s)))
    return signature

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
  message = "%d" % int(time.time())
  (e,s) = schnorr.sign(message)
  params = urllib.urlencode({"user": user, "message": message, "e": e, "s": s})
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
