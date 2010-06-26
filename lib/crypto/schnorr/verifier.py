from crypto.schnorr import Schnorr
from nigori.util import HashWrapper, int2bin, bin2int

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
