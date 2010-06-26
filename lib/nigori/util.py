import hashlib

class HashWrapper:
  def __init__(self):
    self.h = hashlib.sha256()
    
  def add(self, x):
    self.h.update(lengthOf(x))
    self.h.update(x)

  def digest(self):
    return self.h.digest()

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

