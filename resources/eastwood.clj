;; linter config

(disable-warning
  {:linter :constant-test
   :if-inside-macroexpansion-of #{'clojure.core/as->}
   :within-depth 2
   :reason "Allow as-> to have constant tests without warning"})

(disable-warning
  {:linter :constant-test
   :if-inside-macroexpansion-of #{'clojure.spec/every 'clojure.spec.alpha/every
                                  'clojure.spec/and 'clojure.spec.alpha/and
                                  'clojure.spec/keys 'clojure.spec.alpha/keys
                                  'clojure.spec/coll-of 'clojure.spec.alpha/coll-of}
   :within-depth 6
   :reason "clojure.spec's macros `keys`, `every`, and `and` often contain `clojure.core/and` invocations with only one argument."})
