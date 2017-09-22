(defproject hiposfer/geojson.specs "0.1.0"
  :description "a Geojson spec for validating data using Clojure's spec"
  :url "git@github.com:hiposfer/geojson.specs"
  :license {:name "GNU LESSER GENERAL PUBLIC LICENSE"
            :url "https://raw.githubusercontent.com/hiposfer/geojson.specs/master/LICENSE"}
  :dependencies [[org.clojure/clojure "1.9.0-beta1"]]
  :global-vars {*warn-on-reflection* true}
  :plugins [[jonase/eastwood "0.2.3"]])
