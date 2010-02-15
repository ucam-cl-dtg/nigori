from base64 import urlsafe_b64encode as b64enc, urlsafe_b64decode as b64dec
from django.utils import simplejson
from google.appengine.ext import webapp
from google.appengine.ext.webapp.util import run_wsgi_app
from google.appengine.ext import db
from nigori import bin2int, SchnorrVerifier

import codecs
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
    if not wrapAuth(self.request, self.response):
      return
    
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

class ReplayError(Exception):
  pass

class VerifyError(Exception):
  pass

# AppEngine runs in Unicode, so we need to convert to ASCII
ascii = codecs.lookup('ascii')

def getb64(request, param):
  v = request.get(param)
  (v, l) = ascii.encode(v)
  return b64dec(v)

def authenticate(request):
  user = request.get('user')
  users = db.GqlQuery("SELECT * FROM User WHERE user ='" + user +"'")
  assert users.count(2) == 1

  (key, l) = ascii.encode(users[0].publicKey)
  verifier = SchnorrVerifier(bin2int(b64dec(key)))
    
  t = request.get('t')
  # FIXME: check t is recent, expire old tokens
  tokens = db.GqlQuery("SELECT * FROM Token WHERE token = '"
                       + t + "'")
  if tokens.count(1) > 0:
    raise ReplayError()

  token = Token()
  token.token = t
  token.put()
    
  s = getb64(request, 's')
  e = getb64(request, 'e')
    
  if not verifier.verify(t, bin2int(s), bin2int(e)):
    raise VerifyError()

def wrapAuth(request, response):
  try:
    authenticate(request)
  except VerifyError:
    # FIXME: if I am going to issue this error, I should be getting
    # the signature in a WWW-Authenticate field
    response.set_status(401, "Signature doesn't verify")
    return False
  except ReplayError:
    response.set_status(401, "This is a replay")
    return False
  return True

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
    if not wrapAuth(self.request, self.response):
      return
    
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

class Token(db.Model):
  token = db.StringProperty()

class Authenticate(webapp.RequestHandler):
  def post(self):
    wrapAuth(self.request, self.response)

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
