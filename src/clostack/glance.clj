(ns clostack.glance
  (:require [clostack.utils :as utils]))

(defmacro defres [name url singular plural]
  `(utils/defres ~name "image" ~url ~singular ~plural))

(defres image "/v2/images" identity :images)

(defres version "" identity :versions)

(defn image-download [token id]
  (let [url (str (utils/endpoint-get token "image" "public") (:file (image-get token id)))]
    (:body
     (utils/request token :get url :as :stream :accept nil))))
