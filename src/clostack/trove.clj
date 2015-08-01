(ns clostack.trove
  (:require [clostack.utils :as utils]))

(defmacro defres [name url singular plural & more]
  `(utils/defres ~name "database" ~url ~singular ~plural ~@more))

(defres instance "/instances" :instance :instances)
