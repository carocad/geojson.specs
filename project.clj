(defproject hiposfer/geojson.specs "0.2.0"
  :description "a Geojson spec for validating data using Clojure's spec"
  :url "git@github.com:hiposfer/geojson.specs"
  :license {:name "GNU LESSER GENERAL PUBLIC LICENSE"
            :url "https://raw.githubusercontent.com/hiposfer/geojson.specs/master/LICENSE"}
  :dependencies [[org.clojure/clojure "1.9.0" :scope "provided"]
                 [org.clojure/clojurescript "1.9.946" :scope "provided"]]
  :plugins [[jonase/eastwood "0.2.9"]]
  :eastwood {:config-files ["resources/eastwood.clj"]}
  ;; deploy to clojars as - lein deploy releases
  :deploy-repositories [["releases" {:sign-releases false :url "https://clojars.org/repo"}]])
