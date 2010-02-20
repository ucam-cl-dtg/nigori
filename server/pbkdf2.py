from nigori import hexdump, pad_int2bin, bin2int, int2bin

import hashlib

class DerivedKeyTooLongError(Exception):
  pass

class BadSaltLength(Exception):
  pass

def PBKDF2(p, s, c, dkLen):
  hash = hashlib.sha256()
  hLen = hash.digest_size
  if dkLen > (2 ** 32 - 1) * hLen:
    raise DerivedKeyTooLongError()
  if len(s) != 8:
    raise BadSaltLength()
  l = (dkLen + hLen - 1) // hLen
  r = dkLen - (l - 1) * hLen
  out = ""
  for i in range(l):
    t = F(p, s, c, i)
    if i == l - 1:
      t = t[:r]
    out = out + t
  assert len(out) == dkLen
  return out

def F(p, s, c, i):
  prev = hashlib.sha256(p + s + pad_int2bin(i, 4)).digest()
  u = bin2int(prev)
  for i in range(c - 1):
    prev = hashlib.sha256(p + prev).digest()
    u = u ^ bin2int(prev)
  return int2bin(u)

def main():
  print hexdump(PBKDF2("test", "12345678", 1000, 100))

if __name__ == "__main__":
  main()
