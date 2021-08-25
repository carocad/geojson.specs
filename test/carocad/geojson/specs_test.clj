(ns carocad.geojson.specs-test
  (:require [clojure.test :as test]
            [clojure.spec.alpha :as spec]
            [clojure.test.check.generators :as generators]
            [clojure.test.check.properties :as properties]
            [carocad.geojson.specs :as geojson]
            [clojure.test.check :as check]
            [clojure.pprint :as pprint]))

(defn linear-rings
  [size]
  (for [_ (range 1 (Math/round ^float (/ size 2)))]         ;; keep size small
    (let [positions (generators/sample (spec/gen ::geojson/position) size)]
      (cons (last positions) positions))))

(defn polygon
  [size]
  (let [linear-rings (linear-rings size)
        result       {:type        "Polygon"
                      :coordinates linear-rings}]
    (merge result
           (when (even? size)
             (when-let [bbox (geojson/bbox result)]
               {:bbox bbox})))))

(def polygon-generator
  (generators/fmap polygon
                   (generators/such-that (fn [number] (>= number 3))
                                         generators/nat)))

(defn multipolygon
  [size]
  (let [coordinates (for [_ (range 1 size)] (linear-rings size))]
    {:type        "MultiPolygon"
     :coordinates coordinates}))

(def multipolygon-generator
  (generators/fmap multipolygon
                   (generators/such-that (fn [number] (>= number 3))
                                         ; keep size small, otherwise it takes too long
                                         (generators/resize 10 generators/nat))))

(def geometry-collection-generator
  (spec/gen ::geojson/geometry-collection
            {::geojson/polygon      #(do polygon-generator)
             ::geojson/multipolygon #(do multipolygon-generator)}))

(def feature-generator
  (spec/gen ::geojson/feature
            {::geojson/polygon             #(do polygon-generator)
             ::geojson/multipolygon        #(do multipolygon-generator)
             ::geojson/geometry-collection #(do geometry-collection-generator)}))

(def feature-collection-generator
  (spec/gen ::geojson/feature-collection
            {::geojson/polygon             #(do polygon-generator)
             ::geojson/multipolygon        #(do multipolygon-generator)
             ::geojson/geometry-collection #(do geometry-collection-generator)
             ::geojson/feature             #(do feature-generator)}))

(def boxable
  "Most geojson objects support bbox computation"
  (let [geo-generators (concat (map spec/gen [::geojson/multipoint
                                              ::geojson/linestring
                                              ::geojson/multiline])
                               [polygon-generator
                                multipolygon-generator
                                geometry-collection-generator
                                feature-generator
                                feature-collection-generator])]
    (properties/for-all [input (generators/one-of geo-generators)]
      (let [box (geojson/bbox input)]
        (or (nil? box) (and (sequential? box)
                            (= 4 (count box))))))))

(test/deftest geojson-bbox
  (let [result (check/quick-check 200 boxable)]
    (test/is (:pass? result)
             (str "read process failed at\n"
                  (with-out-str (pprint/pprint result))))))
