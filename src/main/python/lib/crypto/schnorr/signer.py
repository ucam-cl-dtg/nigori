from crypto.schnorr import Schnorr
from nigori.util import bin2int, int2bin, lengthOf, HashWrapper, hexdump

import os


class SchnorrSigner(Schnorr):

  def __init__(self, x):
    self.x = bin2int(x)

  def public(self):
    return int2bin(pow(self.g, self.x, self.p))

  def sign(self, message):
    k = self._getRand()
    r = pow(self.g, k, self.p)
    h = HashWrapper()
    h.add(message)
    h.add(int2bin(r))
    e = h.digest()
    s = (k - self.x * bin2int(e)) % self.q
    signature = (e,  int2bin(s))
    return signature

  def _getRand(self):
    # FIXME: Don't use a hardcoded upper limit (2^160) here
    k = int(hexdump(os.urandom(160 / 8)), 16)
    if k < self.q:
      return k
    else:
      return self._getRand()
