from base64 import urlsafe_b64encode as b64enc, urlsafe_b64decode
from Crypto.Cipher import AES
from Crypto.Cipher import DES
from Crypto.Hash import HMAC
from Crypto.Hash import SHA256
from Crypto.Util import randpool

from crypto import gcd
from crypto.pbkdf2 import PBKDF2
from crypto.schnorr.signer import SchnorrSigner
from nigori.util import concat, int2bin, hexdump, bin2int

import codecs
import httplib
import random
try:
  import json  #available as standard on >= python2.6
except ImportError:
  import simplejson as json  #need 3rd-party lib for python2.5
import time
import urllib

# AppEngine runs in Unicode, and so may other things, so we need to
# convert to ASCII
ascii = codecs.lookup('ascii')

# Counts for PBKDF2
Nsalt = 1000
Nuser = 1001
Nenc = 1002
Nmac = 1003

def b64dec(b64):
  (v, l) = ascii.encode(b64)
  return urlsafe_b64decode(v)

class KeyDeriver:
  # use_des is a temporary measure so clients running on appspot can
  # be demoed. Yes, I am using it incorrectly (I am pretending it has
  # 16 byte blocks for padding)
  def __init__(self, username, servername, password, use_des = 0):
    s_user = PBKDF2(concat([username, servername]), "user salt", Nsalt, 8)
    self.user = PBKDF2(password, s_user, Nuser, 16)
    self.enc = PBKDF2(password, s_user, Nenc, 16)
    self.mac = PBKDF2(password, s_user, Nmac, 16)
    self.use_des = use_des

  def encryptWithIV(self, plain, iv):
    if self.use_des:
      crypter = DES.new(self.enc[:8], DES.MODE_CBC, iv)
    else:
      crypter = AES.new(self.enc, AES.MODE_CBC, iv)
    pad = 16 - len(plain) % 16
    c = '%c' % pad
    for i in range(pad):
      plain = plain + c
    crypted = crypter.encrypt(plain)
    hmac = HMAC.new(self.mac, crypted)
    # FIXME: HMAC returns 16 bytes even though we are using a 256 byte hash
    crypted = crypted + hmac.digest()
    return crypted

  def ivBytes(self):
    if self.use_des:
      return 8
    return 16

  def encrypt(self, plain):
    pool = randpool.RandomPool()
    iv = pool.get_bytes(self.ivBytes())
    return iv + self.encryptWithIV(plain, iv)

  def permute(self, plain):
    iv = ""
    for i in range(self.ivBytes()):
      iv = iv + ("%c" % 0)
    return b64enc(self.encryptWithIV(plain, iv))

  def decrypt(self, crypted):
    crypted = b64dec(crypted)
    l = len(crypted)
    if l < 32:
      raise ValueError("value too short")
    mac = crypted[l-16:]
    ivlen = self.ivBytes()
    iv = crypted[:ivlen]
    crypted = crypted[ivlen:l-16]
    hmac = HMAC.new(self.mac, crypted)
    if mac != hmac.digest():
      raise ValueError("mac doesn't match")
    if self.use_des:
      crypter = DES.new(self.enc[:8], DES.MODE_CBC, iv)
    else:
      crypter = AES.new(self.enc, AES.MODE_CBC, iv)
    plain = crypter.decrypt(crypted)
    c = plain[-1]
    for i in range(-1, -ord(c), -1):
      if plain[i] != c:
        raise ValueError("padding error")
    plain = plain[:-ord(c)]
    return plain

  def schnorr(self):
    return SchnorrSigner(self.user)

class NigoriClient:
  TYPE_RSA = 3
  def __init__(self, server, port, user, password):
    self.server = server
    self.port = port
    self.user = user
    self.keys = KeyDeriver(user, server, password, use_des = 1)

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
    res = response.read()
    return json.loads(res)
