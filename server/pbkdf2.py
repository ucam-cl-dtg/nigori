from nigori import hexdump, pad_int2bin, bin2int, int2bin

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
  prev = hmac.new(p, s + pad_int2bin(i, 4), hashlib.sha1).digest()
  u = bin2int(prev)
  for i in range(c - 1):
    prev = hmac.new(p, prev, hashlib.sha1).digest()
    u = u ^ bin2int(prev)
  return int2bin(u)

def main():
  tests = [
            # Test vectors from
            # http://www.ietf.org/id/draft-josefsson-pbkdf2-test-vectors-00.txt
            [ "password", "salt", 1, 20,
              "0c60c80f961f0e71f3a9b524af6012062fe037a6" ],
            [ "password", "salt", 2, 20,
              "ea6c014dc72d6f8ccd1ed92ace1d41f0d8de8957" ],
            [ "password", "salt", 4096, 20,
              "4b007901b765489abead49d926f721d065a429c1" ],
#            [ "password", "salt", 16777216, 20,
#              "eefe3d61cd4da4e4e9945b3d6ba2158c2634e984" ],
          ]

  fails = 0
  for test in tests:
    pbkdf2 = hexdump(PBKDF2(test[0], test[1], test[2], test[3]))
    if pbkdf2 != test[4]:
      print "PBKDF2(%s, %s, %d, %d) = %s (expecting %s)" % (test[0], test[1],
                                                           test[2], test[3],
                                                           pbkdf2, test[4])
      fails = fails + 1
  assert fails == 0

if __name__ == "__main__":
  main()
