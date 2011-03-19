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
