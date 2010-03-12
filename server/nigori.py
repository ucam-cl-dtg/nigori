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

# FIXME: move to client library
class ShamirSplit:
  # FIXME: add a check that this is actually prime, we've already had
  # a typo in it once!
  p = int("ffffffffffffffffc90fdaa22168c234c4c6628b80dc1cd129024e088a67cc74020bbea63b139b22514a08798e3404ddef9519b3cd3a431b302b0a6df25f14374fe1356d6d51c245e485b576625e7ec6f44c42e9a637ed6b0bff5cb6f406b7edee386bfb5a899fa5ae9f24117c4b1fe649286651ece45b3dc2007cb8a163bf0598da48361c55d39a69163fa8fd24cf5f83655d23dca3ad961c62f356208552bb9ed529077096966d670c354e4abc9804f1746c08ca18217c32905e462e36ce3be39e772c180e86039b2783a2ec07a28fb5c55df06f4c52c9de2bcbf6955817183995497cea956ae515d2261898fa051015728e5a8aaac42dad33170d04507a33a85521abdf1cba64ecfb850458dbef0a8aea71575d060c7db3970f85a6e1e4c7abf5ae8cdb0933d71e8c94e04a25619dcee3d2261ad2ee6bf12ffa06d98a0864d87602733ec86a64521f2b18177b200cbbe117577a615d6c770988c0bad946e208e24fa074e5ab3143db5bfce0fd108e4b82d120a92108011a723c12a787e6d788719a10bdba5b2699c327186af4e23c1a946834b6150bda2583e9ca2ad44ce8dbbbc2db04de8ef92e8efc141fbecaa6287c59474e6bc05d99b2964fa090c3a2233ba186515be7ed1f612970cee2d7afb81bdd762170481cd0069127d5b05aa993b4ea988d8fddc186ffb7dc90a6c08f4df435c934063199ffffffffffffffff", 16)

  def compute(self, a, x):
    t = 0
    for i in range(len(a)):
      t = (t + (a[i] * pow(x, i, self.p))) % self.p
    return t

  def share(self, secret, k, n):
    assert self.p > secret
    assert self.p > n
    a = [secret]
    for i in range(1, k):
      a.append(random.SystemRandom().randrange(self.p))
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
    return secret
