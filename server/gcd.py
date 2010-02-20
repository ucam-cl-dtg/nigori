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
  def __init__(self, z, m):
    self.z = z
    self.m = m
  def __str__(self):
    return "cannot invert " + repr(self.z) + " modulo " + repr(self.m)

def mod_inverse(z, m):
  (a, b, v) = binary_extended_gcd(z, m)
  if v != 1:
    raise CannotInvertError(z, m)
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

  mod_inverse(256, 1024)

if __name__ == "__main__":
  main()
