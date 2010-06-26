from base64 import urlsafe_b64encode as b64enc, urlsafe_b64decode
from Crypto.Cipher import AES
from Crypto.Cipher import DES
from Crypto.Hash import HMAC
from Crypto.Hash import SHA256
from Crypto.Util import randpool
from nigori import SchnorrSigner, concat, int2bin, hexdump, bin2int
from crypto.pbkdf2 import PBKDF2

import codecs
import gcd
import httplib
import random
import simplejson
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
    json = response.read()
    return simplejson.loads(json)

class ShamirSplit:
  # FIXME: add a check that this is actually prime, we've already had
  # a typo in it once!
  p = int("ffffffffffffffffc90fdaa22168c234c4c6628b80dc1cd129024e088a67cc74020bbea63b139b22514a08798e3404ddef9519b3cd3a431b302b0a6df25f14374fe1356d6d51c245e485b576625e7ec6f44c42e9a637ed6b0bff5cb6f406b7edee386bfb5a899fa5ae9f24117c4b1fe649286651ece45b3dc2007cb8a163bf0598da48361c55d39a69163fa8fd24cf5f83655d23dca3ad961c62f356208552bb9ed529077096966d670c354e4abc9804f1746c08ca18217c32905e462e36ce3be39e772c180e86039b2783a2ec07a28fb5c55df06f4c52c9de2bcbf6955817183995497cea956ae515d2261898fa051015728e5a8aaac42dad33170d04507a33a85521abdf1cba64ecfb850458dbef0a8aea71575d060c7db3970f85a6e1e4c7abf5ae8cdb0933d71e8c94e04a25619dcee3d2261ad2ee6bf12ffa06d98a0864d87602733ec86a64521f2b18177b200cbbe117577a615d6c770988c0bad946e208e24fa074e5ab3143db5bfce0fd108e4b82d120a92108011a723c12a787e6d788719a10bdba5b2699c327186af4e23c1a946834b6150bda2583e9ca2ad44ce8dbbbc2db04de8ef92e8efc141fbecaa6287c59474e6bc05d99b2964fa090c3a2233ba186515be7ed1f612970cee2d7afb81bdd762170481cd0069127d5b05aa993b4ea988d8fddc186ffb7dc90a6c08f4df435c934063199ffffffffffffffff", 16)

  def compute(self, a, x):
    t = 0
    for i in range(len(a)):
      t = (t + (a[i] * pow(x, i, self.p))) % self.p
    return t

  def share(self, secret_bytes, k, n):
    # Prepend a 1 byte to prevent dropping of leading zeroes.
    secret_bytes = ("%c" % 1) + secret_bytes
    secret = bin2int(secret_bytes)
    
    assert self.p > secret
    assert self.p > n

    # generate random coefficients
    a = [secret]
    for i in range(1, k):
      a.append(random.SystemRandom().randrange(self.p))

    # and compute shares
    s = []
    for i in range(1, n+1):
      s.append(self.compute(a, i))
    return s

  def recover(self, shares):
    secret = 0
    for i in shares.iterkeys():
      c = 1
      for j in shares.iterkeys():
        if i != j:
          # These could be cached, since p is fixed and i and j are small.
          inv = gcd.mod_inverse((j - i) % self.p, self.p)
          c = (c * j * inv) % self.p
      secret = (secret + c * shares[i]) % self.p

    # convert to string, dropping leading 01 added by share()
    secret_bytes = int2bin(secret)
    assert secret_bytes[0] == "%c" % 1
    secret_bytes = secret_bytes[1:]
    
    return secret_bytes

import unittest

def hex2array(hex):
  t = ""
  for i in range(0, len(hex), 2):
    t = t + "%c" % int(hex[i:i+2], 16)
  return t

class TestShamirSplit(unittest.TestCase):
  def setUp(self):
    self.splitter = ShamirSplit()

  def test_splt(self):
    tests = [
      # Hex representation of string of arbitrary bytes
      "fffefdfcfbfaf9f8f7f6f5f4f3f2f0",
      "000102030405060708",
      ]

    for test in tests:
      secret = hex2array(test)
      s = self.splitter.share(secret, 2, 3)
      r = self.splitter.recover({1: s[0], 2: s[1]})
      recovered = hexdump(r)
      print "test =", hexdump(secret), "recovered =", recovered
      self.assertEqual(recovered, test)

# self test
def main():
  unittest.main()

if __name__ == "__main__":
  main()
