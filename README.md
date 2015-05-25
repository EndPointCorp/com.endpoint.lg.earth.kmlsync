Google Earth KML NetworkLinkUpdate Service
==========================================

Java package: com.endpoint.lg.earth.kmlsync

Liquid Galaxy Interactive Spaces activity providing an HTTP server that KML NetworkLinks can fetch new KML from. (KML is a file format used to display geographic data in Google Earth.) Each Google Earth Window is identified with a "slug" string, and this activity maintains a list of URLs that each Window should have loaded.


Configuration variables for LG-CMS activities
---------------------------------------------

```
earth.kmlsync
    # Expects a defined input route to receive director messages
    lg.earth.kmlsyncserver.updatePath       Path to "update" handler ("/network_link_update.kml")
    lg.earth.kmlsyncserver.masterPath       Path to "master" handler ("/master.kml")
    lg.earth.kmlsyncserver.modifyPath       Path to modification html page ("/modify.html")
    lg.earth.kmlsyncserver.indexPath        Path to index (test) page ("/Google%20Earth%20KMLSync/index.html")
    lg.earth.kmlsyncserver.queryPath        Path to page to modify query.txt files ("/query.html")
    space.activity.webapp.content.location  Path to webapp directory (should be "webapp")
    space.activity.webapp.web.server.port   What port should the web server listen on?
```


Copyright (C) 2015 Google Inc.
Copyright (C) 2015 End Point Corporation

Licensed under the Apache License, Version 2.0 (the "License"); you may not
use this file except in compliance with the License. You may obtain a copy of
the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
License for the specific language governing permissions and limitations under
the License.
