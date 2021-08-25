(ns carocad.geojson.examples-test
  (:require [clojure.test :as test]
            [carocad.geojson.specs :as geojson]
            [clojure.data.json :as json]
            [clojure.java.io :as io]))

(test/deftest rfc-examples
  (doseq [file   (.listFiles (io/file "resources/examples"))]
    (with-open [reader (io/reader file)]
      (let [input (clojure.walk/keywordize-keys (json/read reader))]
        (test/testing (str "file: " (.getName file))
          (let [box (geojson/bbox input)]
            (test/is (or (nil? box) (and (sequential? box)
                                         (= 4 (count box))))
                     (str "failed bbox"))))))))

