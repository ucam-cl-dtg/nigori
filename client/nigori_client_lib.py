from base64 import urlsafe_b64encode as b64enc, urlsafe_b64decode as b64dec
from Crypto.Cipher import AES
from Crypto.Hash import HMAC
from Crypto.Hash import SHA256
from Crypto.Util import randpool
from nigori import SchnorrSigner, concat, int2bin

import httplib
import random
import simplejson
import time
import urllib

class KeyDeriver:
  def __init__(self, password):
    self.crypt = SHA256.new(password).digest()
    self.mac = SHA256.new(self.crypt).digest()
    self.authenticate = SHA256.new(self.mac).digest()

  def encryptWithIV(self, plain, iv):
    crypter = AES.new(self.crypt, AES.MODE_CBC, iv)
    pad = 16 - len(plain) % 16
    c = '%c' % pad
    for i in range(pad):
      plain = plain + c
    crypted = crypter.encrypt(plain)
    hmac = HMAC.new(self.mac, crypted)
    crypted = crypted + hmac.digest()
    return crypted

  def encrypt(self, plain):
    pool = randpool.RandomPool()
    iv = pool.get_bytes(16)
    return iv + self.encryptWithIV(plain, iv)

  def permute(self, plain):
    iv = ""
    for i in range(16):
      iv = iv + ("%c" % 0)
    return b64enc(self.encryptWithIV(plain, iv))

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

class NigoriClient:
  TYPE_RSA = 3
  def __init__(self, server, port, user, password):
    self.server = server
    self.port = port
    self.user = user
    self.keys = KeyDeriver(password)

  def authParams(self):
    # FIXME: include server name, user name in t
    t = "%d:%d" % (int(time.time()), random.SystemRandom().getrandbits(20))
    schnorr = self.keys.schnorr()
    (e,s) = schnorr.sign(t)
    params = {"user": self.user,
              "t": t,
              "e": b64enc(e),
              "s": b64enc(s)}
    return params

  def connect(self):
    return httplib.HTTPConnection(self.server, self.port)

  def getValueList(self, type, name):
    params = self.authParams()
    params['name'] = self.keys.permute(concat([int2bin(type), name]))
    conn = self.connect()
    conn.request("GET", "/list-resource?" + urllib.urlencode(params))
    response = conn.getresponse()
    if response.status != 200:
      # FIXME: define a ProtocolError, perhaps?
      raise LookupError("HTTP error: %d %s" % (response.status, response.reason))
    json = response.read()
    return simplejson.loads(json)
