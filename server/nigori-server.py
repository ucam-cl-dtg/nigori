from base64 import urlsafe_b64encode as b64enc, urlsafe_b64decode as b64dec
from django.utils import simplejson
from google.appengine.ext import webapp
from google.appengine.ext.webapp.util import run_wsgi_app
from google.appengine.ext import db

import codecs
import hashlib
import time

PROTOCOL_VERSION = 0

class MainPage(webapp.RequestHandler):
  def get(self):
    self.response.headers['Content-Type'] = 'text/html'
    self.response.out.write('<html><body><h1>Test Form</h1>')
        
    self.response.out.write("""
          <form action="/add-resource" method="post">
            <div>
              Key <input type="text" name="name" />
            </div>
            <div>
              Resource <input type="text" name="value" />
            <div><input type="submit" value="Send"></div>
          </form>
        </body>
      </html>""")
        
    self.response.out.write("""
          <form action="/list-resource" method="get">
            <div>
              Key <input type="text" name="name" />
            </div>
            <div><input type="submit" value="List"></div>
          </form>
        </body>
      </html>""")
    
    self.response.out.write("""
          <form action="/get-resource" method="get">
            <div>
              Key <input type="text" name="name" />
            </div>
            <div>
              Version <input type="text" name="version" />
            </div>
            <div><input type="submit" value="Get"></div>
          </form>
        </body>
      </html>""")

class Resource(db.Model):
  name = db.StringProperty()
  value = db.StringProperty()
  ctime = db.FloatProperty()

class AddResource(webapp.RequestHandler):
  def post(self):
    resource = Resource()
    resource.name = self.request.get('name')
    resource.value = self.request.get('value')
    import urllib
    print urllib.urlencode({"value": resource.value})
    resource.ctime = time.time()
    if resource.name == '':
      self.response.set_status(400, "Name must be supplied")
      return
    resource.put()

#    self.redirect('/list-resource?name=' + resource.name)

class ResourceLister(webapp.RequestHandler):
  def forEach(self, fn, which):
    totalVersions = Resource.all().filter('name =', which).count()
    # Database merges could cause this query to return more results
    # than the total indicated above on some architectures. The
    # protocol should tolerate that.
    resources = db.GqlQuery("SELECT * FROM Resource WHERE name = '"
                            + which + "' ORDER BY ctime")
#    resources = db.GqlQuery("SELECT * FROM Resource ORDER BY ctime")
    version = 0
    for resource in resources:
      if not fn(resource, version, totalVersions):
        break
      version += 1

class ListResource(ResourceLister):
  def get(self):
    self.response.headers['Content-Type'] = 'text/html'
    self.result = []
    self.forEach(self.list, self.request.get('name'))
    self.response.out.write(simplejson.dumps(self.result))

  def list(self, resource, version, totalVersions):
    self.result.append(dict(version = version, creationTime = resource.ctime,
                            value = resource.value, name = resource.name))
    return True

class ListResourceHTML(ResourceLister):
  def get(self):
    self.response.headers['Content-Type'] = 'text/html'

    self.response.out.write('<html><body><h1>Lookup Result</h1>')

    self.forEach(self.list, self.request.get('name'))

  def list(self, resource, version, totalVersions):
    write = self.response.out.write
    
    write('%04d/%04d ' % (version, totalVersions))
    write('%f ' % resource.ctime)
    write(' (%s) ' % time.strftime('%d/%m/%y %H:%M:%S',
                                   time.gmtime(resource.ctime)))
    write('<code>' + resource.name + ' = ' + resource.value + '</code><br />')
    return True

class GetResource(ResourceLister):
  def get(self):
    self.response.headers['Content-Type'] = 'text/plain'

    t = self.request.get('version')
    if t == '':
      t = -1
    else:
      t = int(t)
    self.targetVersion = t
    self.forEach(self.choose, self.request.get('name'))

  def choose(self, resource, version, totalVersions):
    if self.targetVersion == -1 and version != totalVersions - 1:
      return True
    if self.targetVersion != -1  and self.targetVersion != version:
      return True

    result = dict(version = version, totalVersions = totalVersions,
                  creationTime = resource.ctime, value = resource.value)
    
    self.response.out.write(simplejson.dumps(result))
    return False

class User(db.Model):
  user = db.StringProperty()
  publicKey = db.StringProperty()

class Register(webapp.RequestHandler):
  def post(self):
    user = User()
    user.user = self.request.get('user')
    user.publicKey = self.request.get('publicKey')
    user.put()

# FIXME: move to library, share with client
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

def lengthOf(x):
  l = len(x)
  t = int2bin(l)
  assert len(t) <= 4
  for i in range(len(t), 4):
    t = ("%c" % 0) + t
  return t

class HashWrapper:
  def __init__(self):
    self.h = hashlib.sha256()
    
  def add(self, x):
    self.h.update(lengthOf(x))
    self.h.update(x)

  def digest(self):
    return self.h.digest()

class SchnorrVerifier:
  def __init__(self, key):
    # FIXME: should be a shared library with the client
    self.p = int("fd7f53811d75122952df4a9c2eece4e7f611b7523cef4400c31e3f80b6512669455d402251fb593d8d58fabfc5f5ba30f6cb9b556cd7813b801d346ff26660b76b9950a5a49f9fe8047b1022c24fbba9d7feb7c61bf83b57e7c6a8a6150f04fb83f6d3c51ec3023554135a169132f675f3ae2b61d72aeff22203199dd14801c7", 16)
    self.q = int("9760508f15230bccb292b982a2eb840bf0581cf5", 16)
    self.g = int("f7e1a085d69b3ddecbbcab5c36b857b97994afbbfa3aea82f9574c0b3d0782675159578ebad4594fe67107108180b449167123e84c281613b7cf09328cc8a6e13c167a8b547c8d28e0a3ae1e2bb3a675916ea37f0bfa213562f1fb627a01243bcca4f1bea8519089a883dfe15ae59f06928b665e807b552564014c3bfecf492a", 16)
    self.publicKey = key

  def verify(self, message, s, e):
    r = (pow(self.g, s, self.p) * pow(self.publicKey, e, self.p)) % self.p
    h = HashWrapper()
    h.add(message)
    h.add(int2bin(r))
    e1 = h.digest()
    return bin2int(e1) == e

class Authenticate(webapp.RequestHandler):
  def post(self):
    ascii = codecs.lookup('ascii')

    user = self.request.get('user')
    users = db.GqlQuery("SELECT * FROM User WHERE user ='" + user +"'")
    assert users.count(2) == 1

    (key, l) = ascii.encode(users[0].publicKey)
    verifier = SchnorrVerifier(bin2int(b64dec(key)))
    
    message = self.request.get('message')
    # check message for replay, check is recent
    s = self.request.get('s')
    (s, l) = ascii.encode(s)
    e = self.request.get('e')
    (e, l) = ascii.encode(e)
    s = b64dec(s)
    e = b64dec(e)
    if not verifier.verify(message, bin2int(s), bin2int(e)):
      # FIXME: if I am going to issue this error, I should be getting
      # the signature in a WWW-Authenticate field
      self.response.set_status(401, "Signature doesn't verify")

application = webapp.WSGIApplication(
                                     [('/', MainPage),
                                      ('/add-resource', AddResource),
                                      ('/list-resource', ListResource),
                                      ('/list-resource-html', ListResourceHTML),
                                      ('/get-resource', GetResource),
                                      ('/register', Register),
                                      ('/authenticate', Authenticate)],
                                     
                                     debug=True)

def main():
  run_wsgi_app(application)

if __name__ == "__main__":
  main()
