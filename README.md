# geojson.specs 

[![Build Status](https://app.travis-ci.com/carocad/geojson.specs.svg?branch=master)](https://app.travis-ci.com/carocad/geojson.specs)


a Geojson utility for validating data using Clojure(script)'s spec following RFC 7946.

```clojure
(ns example
  (:require [carocad.geojson.specs :as geojson]))
            [clojure.spec.alpha :as s] 

(s/valid? ::geojson/point {:type "Point" :coordinates [1 2 3]})

(s/valid? ::geojson/linestring {:type "LineString" :coordinates [[1 2 3]
                                                                 [4 5 6]]})
```

The `hiposfer.geojson.specs` namespace also contains some utility functions for
working with geojson. Currently, those are:
- `bbox`: get or compute (if not available) the bounding box of a geojson object. 
  Returns `nil` if it is not possible to compute a bbox from the geojson object.
- `uri`: takes a point (or feature-point) and returns a `lon,lat,height` string.
- `geo-uri` takes a point (or feature-point) and returns a `geo:lon,lat,height` string

---
Distributed under LGPL v3
