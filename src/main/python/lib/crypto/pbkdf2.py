from nigori.util import hexdump, pad_int2bin, bin2int, int2bin

import hashlib
import hmac

class DerivedKeyTooLongError(Exception):
  pass

def PBKDF2(p, s, c, dkLen):
  hash = hashlib.sha1()
  hLen = hash.digest_size
  if dkLen > (2 ** 32 - 1) * hLen:
    raise DerivedKeyTooLongError()
  l = (dkLen + hLen - 1) // hLen
  r = dkLen - (l - 1) * hLen
  out = ""
  for i in range(l):
    t = F(p, s, c, i + 1)
    if i == l - 1:
      t = t[:r]
    out = out + t
  assert len(out) == dkLen
  return out

def F(p, s, c, i):
  mac = hmac.new(p, digestmod = hashlib.sha1)
  m2 = mac.copy()
  m2.update(s + pad_int2bin(i, 4))
  prev = m2.digest()
  u = bin2int(prev)
  for i in range(c - 1):
    m2 = mac.copy()
    m2.update(prev)
    prev = m2.digest()
    u = u ^ bin2int(prev)
  return int2bin(u)

import unittest

class PBKDF2Test(unittest.TestCase):
  def test_vectors(self):
    tests = [
      # Test vectors from
      # http://www.ietf.org/id/draft-josefsson-pbkdf2-test-vectors-00.txt
      [ "password", "salt", 1, 20,
        "0c60c80f961f0e71f3a9b524af6012062fe037a6" ],
      [ "password", "salt", 2, 20,
        "ea6c014dc72d6f8ccd1ed92ace1d41f0d8de8957" ],
      [ "password", "salt", 4096, 20,
        "4b007901b765489abead49d926f721d065a429c1" ],
      # Skip very slow test vector for now
      #[ "password", "salt", 16777216, 20,
      #  "eefe3d61cd4da4e4e9945b3d6ba2158c2634e984" ],
      
      # Test vectors from RFC 3962
      [ "password", "ATHENA.MIT.EDUraeburn", 1, 16,
        "cdedb5281bb2f801565a1122b2563515" ],
      [ "password", "ATHENA.MIT.EDUraeburn", 2, 16,
        "01dbee7f4a9e243e988b62c73cda935d" ],
      [ "password", "ATHENA.MIT.EDUraeburn", 1200, 16,
        "5c08eb61fdf71e4e4ec3cf6ba1f5512b" ],
      [ "password", int2bin(int("1234567878563412", 16)), 5, 16,
        "d1daa78615f287e6a1c8b120d7062a49" ],
      [ "X" * 64, "pass phrase equals block size", 1200, 16,
        "139c30c0966bc32ba55fdbf212530ac9" ],
      [ "X" * 65, "pass phrase exceeds block size", 1200, 16,
        "9ccad6d468770cd51b10e6a68721be61" ],
      [ int2bin(int("f09d849e", 16)), "EXAMPLE.COMpianist", 50, 16,
        "6b9cf26d45455a43a5b8bb276a403b39", ],
      
      # Test vectors from
      # http://www.cryptosys.net/manapi/api_PBE_Kdf2.html
      [ "password", int2bin(int("78578e5a5d63cb06", 16)), 2048, 24,
        "bfde6be94df7e11dd409bce20a0255ec327cb936ffe93643" ],
      [ "password", int2bin(int("78578e5a5d63cb06", 16)), 2048, 64,
        "bfde6be94df7e11dd409bce20a0255ec327cb936ffe93643c4b150def77511224479994567f2e9b4e3bd0df7aeda3022b1f26051d81505c794f8940c04df1144" ],
      ]

    fails = 0
    for test in tests:
      pbkdf2 = hexdump(PBKDF2(test[0], test[1], test[2], test[3]))
      if pbkdf2 != test[4]:
        print "PBKDF2(%s, %s, %d, %d) = %s (expecting %s)" % (test[0], test[1],
                                                              test[2], test[3],
                                                              pbkdf2, test[4])
        fails = fails + 1
    self.assertEqual(fails, 0)

def main():
  unittest.main()

if __name__ == "__main__":
  main()
