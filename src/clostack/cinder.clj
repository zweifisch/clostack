(ns clostack.cinder
  (:require [clostack.utils :as utils]))

(defmacro defres [name url singular plural & more]
  `(utils/defres ~name "volumev2" ~url ~singular ~plural ~@more))

(defres volume "/volumes" :volume :volumes :custom-actions
  [[action :put "/action"]])

(defres volume-detail "/volumes/detail" :volume :volumes)

(defn volume-set-readonly [token id readonly]
  (-> (volume-action token id {:os-update_readonly_flag {:readonly readonly}}) :status (= 202)))
