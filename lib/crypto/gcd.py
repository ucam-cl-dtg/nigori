def binary_extended_gcd(x, y):
  assert x > 0
  assert y > 0
  g = 1
  while (x&1) == 0 and (y&1) == 0:
    g = g << 1
    x = x >> 1
    y = y >> 1
  u = x
  v = y
  a = 1
  b = 0
  c = 0
  d = 1
  while u != 0:
   while (u&1) == 0:
     u = u >> 1
     if (a&1) == 0 and (b&1) == 0:
       a = a >> 1
       b = b >> 1
     else:
       a = (a + y) >> 1
       b = (b - x) >> 1
   while (v&1) == 0:
     v = v >> 1
     if (c&1) == 0 and (d&1) == 0:
       c = c >> 1
       d = d >> 1
     else:
       c = (c + y) >> 1
       d = (d - x) >> 1
   if u >= v:
     u = u - v
     a = a - c
     b = b - d
   else:
     v = v - u
     c = c - a
     d = d - b
  return (c, d, g * v)

class CannotInvertError(Exception):
  def __init__(self, z, m, gcd):
    self.z = z
    self.m = m
    self.gcd = gcd
  def __str__(self):
    return "cannot invert " + repr(self.z) + " modulo " + repr(self.m) + \
        " gcd is " + repr(self.gcd)

def mod_inverse(z, m):
  (a, b, v) = binary_extended_gcd(z, m)
  if v != 1:
    raise CannotInvertError(z, m, v)
  if a < 0:
    return a + m
  return a

def main():
  (a, b, v) = binary_extended_gcd(693, 609)
  print "a =", a, "b =", b, "v =", v
  assert a == -181
  assert b == 206
  assert v == 21

  a = mod_inverse(456, 65537)
  print "a =", a
  assert (456 * a) % 65537 == 1

  p4096 = int("ffffffffffffffffc90fdaa22168c234c4c6628b80dc1cd129024e088a67cc74020bbea63b139b22514a08798e3404ddef9519b3cd3a431b302b0a6df25f14374fe1356d6d51c245e485b576625e7ec6f44c42e9a637ed6b0bff5cb6f406b7edee386bfb5a899fa5ae9f24117c4b1fe649286651ece45b3dc2007cb8a163bf0598da48361c55d39a69163fa8fd24cf5f83655d23dca3ad961c62f356208552bb9ed529077096966d670c354e4abc9804f1746c08ca18217c32905e462e36ce3be39e772c180e86039b2783a2ec07a28fb5c55df06f4c52c9de2bcbf6955817183995497cea956ae515d2261898fa051015728e5a8aaac42dad33170d04507a33a85521abdf1cba64ecfb850458dbef0a8aea71575d060c7db3970f85a6e1e4c7abf5ae8cdb0933d71e8c94e04a25619dcee3d2261ad2ee6bf12ffa06d98a0864d87602733ec86a64521f2b18177b200cbbe117577a615d6c770988c0bad946e208e24fa074e5ab3143db5bfce0fd108e4b82d120a92108011a723c12a787e6d788719a10bdba5b2699c327186af4e23c1a946834b6150bda2583e9ca2ad44ce8dbbbc2db04de8ef92e8efc141fbecaa6287c59474e6bc05d99b2964fa090c3a2233ba186515be7ed1f612970cee2d7afb81bdd762170481cd0069127d5b05aa993b4ea988d8fddc186ffb7dc90a6c08f4df435c934063199ffffffffffffffff", 16)

  from nigori import hexdump, int2bin
  for n in range(-10, 11):
    if n != 0:
      print n, hexdump(int2bin(mod_inverse(n % p4096, p4096)))

  # expect an error :-)
  mod_inverse(256, 1024)

if __name__ == "__main__":
  main()
