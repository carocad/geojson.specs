(ns hiposfer.geojson.specs
  "GeoJSON as Clojure Spec
   https://tools.ietf.org/html/rfc7946"
  (:require [clojure.spec.alpha :as s]
            [clojure.string :as str]))

(s/def ::lat (s/and number? #(<= -90 % 90)))
(s/def ::lon (s/and number? #(<= -180 % 180)))
(s/def ::position (s/or :2d (s/tuple ::lon ::lat)
                        :3d (s/tuple ::lon ::lat number?)))
(s/def ::properties  (s/map-of keyword? any?))
(s/def ::linear-ring (s/and (s/coll-of ::position :min-count 4)
                            #(= (first %) (last %))))
(s/def ::bbox (s/cat :west number? :south number?
                     :east number? :north number?))

;;TODO there doesnt seem to be a better way of doing this :(
;; https://groups.google.com/forum/#!topic/clojure-dev/eNN8NYj3CaA
;; the followind declarations should NOT be used in a normal workflow
;; so I think it is ok for them to be ugly here and not leak to the outside
(s/def :hiposfer.geojson.specs.point/type               #(= "Point" %))
(s/def :hiposfer.geojson.specs.multipoint/type          #(= "MultiPoint" %))
(s/def :hiposfer.geojson.specs.linestring/type          #(= "LineString" %))
(s/def :hiposfer.geojson.specs.multiline/type           #(= "MultiLineString" %))
(s/def :hiposfer.geojson.specs.polygon/type             #(= "Polygon" %))
(s/def :hiposfer.geojson.specs.multipolygon/type        #(= "MultiPolygon" %))
(s/def :hiposfer.geojson.specs.geometry-collection/type #(= "GeometryCollection" %))
(s/def :hiposfer.geojson.specs.feature/type             #(= "Feature" %))
(s/def :hiposfer.geojson.specs.feature-collection/type  #(= "FeatureCollection" %))

(s/def :hiposfer.geojson.specs.point/coordinates        ::position)
(s/def :hiposfer.geojson.specs.multipoint/coordinates   (s/coll-of ::position))
(s/def :hiposfer.geojson.specs.linestring/coordinates   (s/coll-of ::position :min-count 2))
(s/def :hiposfer.geojson.specs.multiline/coordinates    (s/coll-of :hiposfer.geojson.specs.linestring/coordinates))
(s/def :hiposfer.geojson.specs.polygon/coordinates      (s/coll-of ::linear-ring))
(s/def :hiposfer.geojson.specs.multipolygon/coordinates (s/coll-of :hiposfer.geojson.specs.polygon/coordinates))

;; --------------- geometry objects
(s/def ::point
  (s/keys :req-un [:hiposfer.geojson.specs.point/type
                   :hiposfer.geojson.specs.point/coordinates]
          :opt-un [::bbox]))

(s/def ::multipoint
  (s/keys :req-un [:hiposfer.geojson.specs.multipoint/type
                   :hiposfer.geojson.specs.multipoint/coordinates]
          :opt-un [::bbox]))

(s/def ::linestring
  (s/keys :req-un [:hiposfer.geojson.specs.linestring/type
                   :hiposfer.geojson.specs.linestring/coordinates]
          :opt-un [::bbox]))

(s/def ::multiline
  (s/keys :req-un [:hiposfer.geojson.specs.multiline/type
                   :hiposfer.geojson.specs.multiline/coordinates]
          :opt-un [::bbox]))

(s/def ::polygon
  (s/keys :req-un [:hiposfer.geojson.specs.polygon/type
                   :hiposfer.geojson.specs.polygon/coordinates]
          :opt-un [::bbox]))

(s/def ::multipolygon
  (s/keys :req-un [:hiposfer.geojson.specs.multipolygon/type
                   :hiposfer.geojson.specs.multipolygon/coordinates]
          :opt-un [::bbox]))

(s/def ::object (s/or :point ::point
                      :multipoint ::multipoint
                      :linestring ::linestring
                      :multiline ::multiline
                      :polygon ::polygon
                      :multipolygon ::multipolygon
                      :collection ::geometry-collection))

;TODO: FeatureCollection and Geometry objects, respectively
; MUST NOT contain a "geometry" or "properties" member.
(s/def :hiposfer.geojson.specs.geometry-collection/geometries   (s/coll-of ::object))
(s/def :hiposfer.geojson.specs.feature/geometry                 (s/nilable ::object))
(s/def :hiposfer.geojson.specs.feature-collection/features      (s/coll-of ::feature))

(s/def :feature/id (s/or :string string? :number number?))


;; -------- features/collections

(s/def ::geometry-collection
  (s/keys :req-un [:hiposfer.geojson.specs.geometry-collection/type
                   :hiposfer.geojson.specs.geometry-collection/geometries]))

(s/def ::feature
  (s/keys :req-un [:hiposfer.geojson.specs.feature/type
                   :hiposfer.geojson.specs.feature/geometry]
          :opt-un [:feature/id ::bbox]))

(s/def ::feature-collection
  (s/keys :req-un [:hiposfer.geojson.specs.feature-collection/type
                   :hiposfer.geojson.specs.feature-collection/features]
          :opt-un [::bbox]))

;; -------------- utility functions

(defn limited-feature
  "returns an feature spec that conforms only to the specified geometry type
  instead of any geometry object"
  [geo-spec]
  (s/keys :req-un [:hiposfer.geojson.specs.feature/type geo-spec]
          :opt-un [:feature/id ::bbox]))

(defn- bounds
  "computes a bounding box with [min-lon, min-lat, max-lon, max-lat]"
  [coordinates]
  (let [lons (map first coordinates)
        lats (map second coordinates)]
    [(apply min lons) (apply min lats)
     (apply max lons) (apply max lats)]))

(defn- super-bounds
  "computes the bounding box of bounding boxes"
  [boxes]
  [(apply min (map #(nth % 0) boxes)) (apply min (map #(nth % 1) boxes))
   (apply max (map #(nth % 2) boxes)) (apply max (map #(nth % 3) boxes))])

;The value of the bbox member MUST be an array of
;length 2*n where n is the number of dimensions represented in the
;contained geometries, with all axes of the most southwesterly point
;followed by all axes of the more northeasterly point.
(defn bbox
  "Returns the :bbox present in the geojson object. Otherwise computes a bounding
   box with [min-lon, min-lat, max-lon, max-lat]. Note: according to the rfc7946 the values of
   a bbox array are [west, south, east, north], not [minx, miny, maxx, maxy]

   However for simplicity we calculate them that way"
  [geojson]
  (if (:bbox geojson) (:bbox geojson)
    (case (:type geojson)
      "MultiPoint" (bounds (:coordinates geojson))
      "LineString" (bounds (:coordinates geojson))
      "MultiLineString" (super-bounds (map bounds (:coordinates geojson)))
      "Polygon"         (super-bounds (map bounds (:coordinates geojson)))
      "MultiPolygon"    (super-bounds (map super-bounds (map bounds (:coordinates geojson))))
      "GeometryCollection" (let [points (filter (comp #{"Point"} :type) (:geometries geojson))
                                 others (remove (comp #{"Point"} :type) (:geometries geojson))]
                             (super-bounds (concat (map bbox others) ;; duplicate point coordinates to fake bounds
                                                   (map #(vec (concat % %)) (map :coordinates points)))))
      "Feature"    (bbox (:geometry geojson))
      "FeatureCollection" (let [geometries (map :geometry (:features geojson))
                                points     (filter (comp #{"Point"} :type) geometries)
                                others     (remove (comp #{"Point"} :type) geometries)]
                            (super-bounds (concat (map bbox others) ;; duplicate point coordinates to fake bounds
                                                  (map #(vec (concat % %)) (map :coordinates points))))))))
;(bbox {:type "FeatureCollection"
;       :features [{:type "Feature"
;                   :geometry {:type "Point" :coordinates [1 2]}}
;                  {:type "Feature"
;                   :geometry {:type "Point" :coordinates [3 4]}}]})

;; Point is not supported
;; TODO: I think the bbox implementation of GeometryCollection and FeatureCollection might be wrong.
;; it should take into account single points as well. Probably it would be better to compute the bbox
;; of the other elements and then expand the bbox if necessary to include the points

(defn uri
  "takes a point or feature and concatenates the coordinates as {longitude},{latitude}"
  [geojson]
  (case (:type geojson)
    "Point" (str/join "," (:coordinates geojson))
    "Feature" (uri (:geometry geojson))))

(defn geo-uri
  "return a geoUri as specified by the rfc7946.
   Example: 'geo' URI: geo:lat,lon
   GeoJSON:{'type': 'Point', 'coordinates': [lon, lat]}"
  [geojson]
  (str "geo:" (uri geojson)))