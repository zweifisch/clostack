(ns clostack.glance
  (:require [clostack.utils :as utils]))

(defmacro defres [name url singular plural & more]
  `(utils/defres ~name "image" ~url ~singular ~plural ~@more))

(defres image "/v2/images" nil :images)

(defres version "" nil :versions :only [list])

(defn image-download [token id]
  (let [url (str (utils/endpoint-get token "image" "public") (:file (image-get token id)))]
    (:body
     (utils/request token :get url :as :stream :accept nil))))

(defn image-upload [token id stream]
  (let [url (str (utils/endpoint-get token "image" "public") (:file (image-get token id)))]
    (:body
     (utils/request token :put url :body stream :content-type :octet-stream))))
