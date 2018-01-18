(defproject hiposfer/geojson.specs "0.2.0"
  :description "a Geojson spec for validating data using Clojure's spec"
  :url "git@github.com:hiposfer/geojson.specs"
  :license {:name "GNU LESSER GENERAL PUBLIC LICENSE"
            :url "https://raw.githubusercontent.com/hiposfer/geojson.specs/master/LICENSE"}
  :dependencies [[org.clojure/clojure "1.9.0" :scope "provided"]
                 [org.clojure/clojurescript "1.9.946" :scope "provided"]]
  :plugins [[jonase/eastwood "0.2.5"]
            [lein-cljsbuild "1.1.7"]]
  :cljsbuild ;; TODO: add tests
  {:builds
   [{:id "dev"
     :source-paths ["src"]
     :compiler {:optimizations :none
                :pretty-print true}}
    {:id "simple"
     :source-paths ["src"]
     :compiler {:optimizations :simple
                :static-fns true}}
    {:id "advance"
     :source-paths ["src"]
     :compiler {:optimizations :advanced}}]})