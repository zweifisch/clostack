(ns clostack.cinder
  (:require [clostack.utils :as utils]
            [clojure.string :as s]))

(defmacro defres [name url singular plural & more]
  `(utils/defres ~name "volumev2" ~url ~singular ~plural ~@more :put-for-update true))

(defres volume "/volumes" :volume :volumes :custom-actions
  [[action :put "/action"]])

(defres volume-detail "/volumes/detail" :volume :volumes)

(defn volume-set-readonly [token id readonly]
  (-> (volume-action token id {:os-update_readonly_flag {:readonly readonly}}) :status (= 202)))

(defres version "/" :version :versions)

(defn version-list [token]
  (let [url (s/replace-first (utils/endpoint-get token "volumev2") #"/v2.*" "")]
    (:versions
     (:body
      (utils/request token :get url)))))

(defres availability-zone "/os-availability-zone" nil :availabilityZoneInfo :only [list])
