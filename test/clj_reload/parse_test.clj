(ns clj-reload.parse-test
  (:require
    [clj-reload.core :as reload]
    [clj-reload.parse :as parse]
    [clj-reload.util :as util]
    [clojure.java.io :as io]
    [clojure.test :refer [is deftest testing use-fixtures]])
  (:import
    [java.io PushbackReader StringReader StringWriter]))

(defn read-str [s]
  (parse/read-file (PushbackReader. (StringReader. s)) nil))

(deftest read-file-test
  (is (= '{x {:requires
              #{a.b.c
                a.b.d
                a.b.e
                a.b.f
                a.b.g
                a.b.h
                a.b.i
                a.b.j
                a.b.k
                a.b.l}
              :keep {x {:tag defonce
                        :form (defonce x 1)}
                     y {:tag defprotocol
                        :form (defprotocol y 2)}}}}
        (read-str #ml "(ns x
                         (:require
                           a.b.c
                           [a.b.d]
                           [a.b.e :as e]
                           [a.b f g]
                           [a.b [h :as h]])
                         (:require
                           a.b.i)
                         (:use
                           a.b.j))
                       ...
                       (defonce x 1)
                       ...
                       (require 'a.b.k)
                       ...
                       ^:clj-reload/keep
                       (defprotocol y 2)
                       ...
                       (use 'a.b.l)")))
  
  (is (= '{x nil}
        (read-str "(ns x)")))
  
  (is (= '{x nil}
        (read-str "(in-ns 'x)"))))

(deftest read-file-errors-test
  (let [file #ml "(ns x
                    (:require 123)
                    (:require [345])
                    (:require [567 :as a])
                    (:require [789 a b c]))"
        out  (StringWriter.)
        res  (binding [*out* out]
               (read-str file))]
    (is (= '{x nil} res))
    (is (= "Unexpected :require form: 123
Unexpected :require form: [345]
Unexpected :require form: [567 :as a]
Unexpected :require form: [789 a b c]
" (str out)))))

(deftest scan-impl-test
  (let [{files :files'
         nses  :namespaces'} (binding [util/*log-fn* nil]
                               (@#'reload/scan-impl nil ["fixtures"] 0))]
    (is (= '#{}
          (get-in files [(io/file "fixtures/core_test/no_ns.clj") :namespaces])))
    
    (is (= '#{two-nses two-nses-second}
          (get-in files [(io/file "fixtures/core_test/two_nses.clj") :namespaces])))
    
    (is (= '#{split}
          (get-in files [(io/file "fixtures/core_test/split.clj") :namespaces])))
    
    (is (= '#{split}
          (get-in files [(io/file "fixtures/core_test/split_part.clj") :namespaces])))
        
    (is (= '#{clojure.string}
          (get-in nses ['two-nses :requires])))
    
    (is (= '#{clojure.set}
          (get-in nses ['two-nses-second :requires])))
    
    (is (= '#{clojure.string clojure.set}
          (get-in nses ['split :requires])))
    
    (is (= (io/file "fixtures/core_test/split.clj")
          (get-in nses ['split :main-file])))))