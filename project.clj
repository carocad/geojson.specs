(defproject net.clojars.carocad/geojson.specs "0.3.0"
  :description "a Geojson spec for validating data using Clojure's spec"
  :url "https://github.com/carocad/geojson.specs"
  :license {:name "LGPL v3"
            :url  "https://github.com/carocad/geojson.specs/blob/master/LICENSE"}
  :dependencies [[org.clojure/clojure "1.10.1" :scope "provided"]
                 [org.clojure/clojurescript "1.10.597" :scope "provided"]]
  :profiles {:dev {:dependencies [[org.clojure/test.check "0.10.0"]  ;; generative testing
                                  [org.clojure/data.json "0.2.6"]]
                   :plugins [[jonase/eastwood "0.2.9"]]
                   :eastwood {:config-files ["resources/eastwood.clj"]}}}

  ;; deploy to clojars as - lein deploy releases
  :deploy-repositories [["clojars" {:url "https://clojars.org/repo"
                                    :username :env/clojars_username
                                    :password :env/clojars_password
                                    :sign-releases false}]])
