from google.appengine.ext import webapp
from google.appengine.ext.webapp.util import run_wsgi_app
from google.appengine.ext import db
import time

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
          <form action="/get-resource" method="get">
            <div>
              Key <input type="text" name="name" />
            </div>
            <div><input type="submit" value="Find"></div>
          </form>
        </body>
      </html>""")

class Resource(db.Model):
  name = db.StringProperty()
  value = db.StringProperty()
  ctime = db.FloatProperty()

class AddResource(webapp.RequestHandler):
  def post(self):
    self.response.headers['Content-Type'] = 'text/plain'

    resource = Resource()
    resource.name = self.request.get('name')
    resource.value = self.request.get('value')
    resource.ctime = time.time()
    resource.put()

    self.response.out.write('Saved')

class ResourceLister(webapp.RequestHandler):
  def forEach(self, fn, which):
    totalVersions = Resource.all().filter('name =', which).count()
    resources = db.GqlQuery("SELECT * FROM Resource WHERE name = '"
                            + which + "' ORDER BY ctime")
    version = 0
    for resource in resources:
      fn(resource, version, totalVersions)
      version += 1
        
class GetResource(ResourceLister):
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

application = webapp.WSGIApplication(
                                     [('/', MainPage),
                                      ('/add-resource', AddResource),
                                      ('/get-resource', GetResource)],
                                     debug=True)

def main():
  run_wsgi_app(application)

if __name__ == "__main__":
  main()
