(ns carocad.geojson.specs
  "GeoJSON as Clojure Spec
   https://tools.ietf.org/html/rfc7946"
  (:require [#?(:clj  clojure.spec.alpha
                :cljs cljs.spec.alpha) :as s]
            [clojure.string :as str]))

(s/def ::lat (s/and number? #(<= -90 % 90)))
(s/def ::lon (s/and number? #(<= -180 % 180)))
(s/def ::position (s/or :2d (s/tuple ::lon ::lat)
                        :3d (s/tuple ::lon ::lat number?)))
(s/def ::properties (s/map-of keyword? any?))
(s/def ::linear-ring (s/and (s/coll-of ::position :min-count 4)
                            #(= (first %) (last %))))
(s/def ::bbox (s/cat :west number? :south number?
                     :east number? :north number?))

;;TODO there doesn't seem to be a better way of doing this :(
;; https://groups.google.com/forum/#!topic/clojure-dev/eNN8NYj3CaA
;; the following declarations should NOT be used in a normal workflow
;; so I think it is ok for them to be ugly here and not leak to the outside
(s/def :carocad.geojson.specs.point/type #{"Point"})
(s/def :carocad.geojson.specs.multipoint/type #{"MultiPoint"})
(s/def :carocad.geojson.specs.linestring/type #{"LineString"})
(s/def :carocad.geojson.specs.multiline/type #{"MultiLineString"})
(s/def :carocad.geojson.specs.polygon/type #{"Polygon"})
(s/def :carocad.geojson.specs.multipolygon/type #{"MultiPolygon"})
(s/def :carocad.geojson.specs.geometry-collection/type #{"GeometryCollection"})
(s/def :carocad.geojson.specs.feature/type #{"Feature"})
(s/def :carocad.geojson.specs.feature-collection/type #{"FeatureCollection"})

(s/def :carocad.geojson.specs.point/coordinates ::position)
(s/def :carocad.geojson.specs.multipoint/coordinates (s/coll-of ::position))
(s/def :carocad.geojson.specs.linestring/coordinates (s/coll-of ::position :min-count 2))
(s/def :carocad.geojson.specs.multiline/coordinates (s/coll-of :carocad.geojson.specs.linestring/coordinates))
(s/def :carocad.geojson.specs.polygon/coordinates (s/coll-of ::linear-ring))
(s/def :carocad.geojson.specs.multipolygon/coordinates (s/coll-of :carocad.geojson.specs.polygon/coordinates))

;; --------------- geometry objects
(s/def ::point
  (s/keys :req-un [:carocad.geojson.specs.point/type
                   :carocad.geojson.specs.point/coordinates]
          :opt-un [::bbox]))

(s/def ::multipoint
  (s/keys :req-un [:carocad.geojson.specs.multipoint/type
                   :carocad.geojson.specs.multipoint/coordinates]
          :opt-un [::bbox]))

(s/def ::linestring
  (s/keys :req-un [:carocad.geojson.specs.linestring/type
                   :carocad.geojson.specs.linestring/coordinates]
          :opt-un [::bbox]))

(s/def ::multiline
  (s/keys :req-un [:carocad.geojson.specs.multiline/type
                   :carocad.geojson.specs.multiline/coordinates]
          :opt-un [::bbox]))

(s/def ::polygon
  (s/keys :req-un [:carocad.geojson.specs.polygon/type
                   :carocad.geojson.specs.polygon/coordinates]
          :opt-un [::bbox]))

(s/def ::multipolygon
  (s/keys :req-un [:carocad.geojson.specs.multipolygon/type
                   :carocad.geojson.specs.multipolygon/coordinates]
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
(s/def :carocad.geojson.specs.geometry-collection/geometries (s/coll-of ::object))
(s/def :carocad.geojson.specs.feature/geometry (s/nilable ::object))
(s/def :carocad.geojson.specs.feature-collection/features (s/coll-of ::feature))

(s/def ::id (s/or :string string? :number number?))


;; -------- features/collections

(s/def ::geometry-collection
  (s/keys :req-un [:carocad.geojson.specs.geometry-collection/type
                   :carocad.geojson.specs.geometry-collection/geometries]))

(s/def ::feature
  (s/keys :req-un [:carocad.geojson.specs.feature/type
                   :carocad.geojson.specs.feature/geometry]
          :opt-un [::id ::bbox ::properties]))

(s/def ::feature-collection
  (s/keys :req-un [:carocad.geojson.specs.feature-collection/type
                   :carocad.geojson.specs.feature-collection/features]
          :opt-un [::bbox]))

;; -------------- utility functions
(defn- bounds
  "computes a bounding box with [min-lon, min-lat, max-lon, max-lat]"
  [coordinates]
  (when (not-empty coordinates)
    (let [lons (map first coordinates)
          lats (map second coordinates)]
      [(apply min lons) (apply min lats)
       (apply max lons) (apply max lats)])))

(defn- super-bounds
  "computes the bounding box of bounding boxes"
  [maybe-boxes]
  (when-let [boxes (not-empty (remove nil? maybe-boxes))]
    [(apply min (map #(nth % 0) boxes)) (apply min (map #(nth % 1) boxes))
     (apply max (map #(nth % 2) boxes)) (apply max (map #(nth % 3) boxes))]))

(declare bbox)

(defn- geometries-bbox
  "for geojson collections the points contribute to the bbox, so we need special handling"
  [geometries]
  (let [points (filter (comp #{"Point"} :type) geometries)
        others (remove (comp #{"Point"} :type) geometries)]
    (super-bounds (concat (map bbox (remove nil? others))
                          ;; duplicate point coordinates to fake bounds
                          (map #(vec (concat % %)) (map :coordinates points))))))

;The value of the bbox member MUST be an array of
;length 2*n where n is the number of dimensions represented in the
;contained geometries, with all axes of the most southwesterly point
;followed by all axes of the more northeasterly point.
(defn bbox
  "Returns the :bbox present in the geojson object. Otherwise, computes a bounding
   box with [min-lon, min-lat, max-lon, max-lat]. Note: according to the rfc7946 the values of
   a bbox array are [west, south, east, north], not [minx, miny, maxx, maxy]

   However, for simplicity we calculate them that way"
  [geojson]
  (if (:bbox geojson)
    (:bbox geojson)
    (case (:type geojson)
      "Point" nil
      "MultiPoint" (bounds (:coordinates geojson))
      "LineString" (bounds (:coordinates geojson))
      "MultiLineString" (super-bounds (map bounds (:coordinates geojson)))
      "Polygon" (super-bounds (map bounds (:coordinates geojson)))
      "MultiPolygon" (super-bounds (for [polygon     (:coordinates geojson)
                                         coordinates polygon]
                                     (bounds coordinates)))
      "GeometryCollection" (geometries-bbox (:geometries geojson))
      "Feature" (when (some? (:geometry geojson))
                  (bbox (:geometry geojson)))
      "FeatureCollection" (geometries-bbox (map :geometry (:features geojson))))))
;(bbox {:type "FeatureCollection"
;       :features [{:type "Feature"
;                   :geometry {:type "Point" :coordinates [1 2]}}
;                  {:type "Feature"
;                   :geometry {:type "Point" :coordinates [3 4]}}]})

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
