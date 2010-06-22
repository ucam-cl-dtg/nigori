import gcd
import hashlib
import random

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

def pad_int2bin(x, n):
  t = int2bin(x)
  assert len(t) <= n
  for i in range(len(t), 4):
    t = ("%c" % 0) + t
  return t

def lengthOf(x):
  return pad_int2bin(len(x), 4)

def concat(strings):
  out = ""
  for string in strings:
    out = out + lengthOf(string) + string
  return out

def unconcat(string):
  out = []
  while string != "":
    l = bin2int(string[:4])
    t = string[4:l+4]
    assert len(t) == l
    out.append(t)
    string = string[l+4:]
  return out

class HashWrapper:
  def __init__(self):
    self.h = hashlib.sha256()
    
  def add(self, x):
    self.h.update(lengthOf(x))
    self.h.update(x)

  def digest(self):
    return self.h.digest()

class Schnorr:
    # Parameters cribbed from OpenSSL's J-PAKE implementation, for now
    p = int("fd7f53811d75122952df4a9c2eece4e7f611b7523cef4400c31e3f80b6512669455d402251fb593d8d58fabfc5f5ba30f6cb9b556cd7813b801d346ff26660b76b9950a5a49f9fe8047b1022c24fbba9d7feb7c61bf83b57e7c6a8a6150f04fb83f6d3c51ec3023554135a169132f675f3ae2b61d72aeff22203199dd14801c7", 16)
    q = int("9760508f15230bccb292b982a2eb840bf0581cf5", 16)
    g = int("f7e1a085d69b3ddecbbcab5c36b857b97994afbbfa3aea82f9574c0b3d0782675159578ebad4594fe67107108180b449167123e84c281613b7cf09328cc8a6e13c167a8b547c8d28e0a3ae1e2bb3a675916ea37f0bfa213562f1fb627a01243bcca4f1bea8519089a883dfe15ae59f06928b665e807b552564014c3bfecf492a", 16)

# FIXME: move to client library
class SchnorrSigner(Schnorr):
  def __init__(self, x):
    self.x = bin2int(x)

  def public(self):
    return int2bin(pow(self.g, self.x, self.p))

  def sign(self, message):
    k = random.SystemRandom().randrange(self.q)
    r = pow(self.g, k, self.p)
    h = HashWrapper()
    h.add(message)
    h.add(int2bin(r))
    e = h.digest()
    s = (k - self.x * bin2int(e)) % self.q
    signature = (e,  int2bin(s))
    return signature

class SchnorrVerifier(Schnorr):
  def __init__(self, key):
    self.publicKey = key

  def verify(self, message, s, e):
    r = (pow(self.g, s, self.p) * pow(self.publicKey, e, self.p)) % self.p
    h = HashWrapper()
    h.add(message)
    h.add(int2bin(r))
    e1 = h.digest()
    return bin2int(e1) == e
