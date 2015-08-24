(ns clostack.glance
  (:require [clostack.utils :as utils]))

(defmacro defres [name url singular plural]
  `(utils/defres ~name "image" ~url ~singular ~plural))

(defres image "/v2/images" :image :images)

(defn versions [token]
  (utils/request nil :get (utils/endpoint-get token "image")))
