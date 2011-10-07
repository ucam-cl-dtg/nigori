import gcd
import random

prime4096 = int("ffffffffffffffffc90fdaa22168c234c4c6628b80dc1cd129024e088a67cc74020bbea63b139b22514a08798e3404ddef9519b3cd3a431b302b0a6df25f14374fe1356d6d51c245e485b576625e7ec6f44c42e9a637ed6b0bff5cb6f406b7edee386bfb5a899fa5ae9f24117c4b1fe649286651ece45b3dc2007cb8a163bf0598da48361c55d39a69163fa8fd24cf5f83655d23dca3ad961c62f356208552bb9ed529077096966d670c354e4abc9804f1746c08ca18217c32905e462e36ce3be39e772c180e86039b2783a2ec07a28fb5c55df06f4c52c9de2bcbf6955817183995497cea956ae515d2261898fa051015728e5a8aaac42dad33170d04507a33a85521abdf1cba64ecfb850458dbef0a8aea71575d060c7db3970f85a6e1e4c7abf5ae8cdb0933d71e8c94e04a25619dcee3d2261ad2ee6bf12ffa06d98a0864d87602733ec86a64521f2b18177b200cbbe117577a615d6c770988c0bad946e208e24fa074e5ab3143db5bfce0fd108e4b82d120a92108011a723c12a787e6d788719a10bdba5b2699c327186af4e23c1a946834b6150bda2583e9ca2ad44ce8dbbbc2db04de8ef92e8efc141fbecaa6287c59474e6bc05d99b2964fa090c3a2233ba186515be7ed1f612970cee2d7afb81bdd762170481cd0069127d5b05aa993b4ea988d8fddc186ffb7dc90a6c08f4df435c934063199ffffffffffffffff", 16)

def compute(a, x, p):
  t = 0
  for i in range(len(a)):
    t = (t + (a[i] * pow(x, i, p))) % p
  return t

def share(secret, k, n, p):
  assert p > secret
  assert p > n
  a = [secret]
  for i in range(1, k):
    a.append(random.SystemRandom().randrange(p))
  s = []
  for i in range(1, n+1):
    s.append(compute(a, i, p))
  return s

def recover(shares, p):
  secret = 0
  for i in shares.iterkeys():
    c = 1
    for j in shares.iterkeys():
      if i != j:
        # These could be cached, since p is fixed and i and j are small.
        inv = gcd.mod_inverse((j - i) % p, p)
        c = (c * j * inv) % p
    secret = (secret + c * shares[i]) % p
  return secret

def main():
  s = share(12, 2, 3, 17)
  r = recover({1: s[0], 2: s[1]}, 17)
  print "r =", r
  assert r == 12

  secret = random.SystemRandom().getrandbits(1000)
  s = share(secret, 3, 5, prime4096)
  r = recover({1: s[0], 3: s[2], 5: s[4]}, prime4096)
  print "secret =", secret, "r =", r
  assert secret == r

if __name__ == "__main__":
  main()
